/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tlumip.ct;

// FreightDemand3.java transforms PI's annual production-comsumption matrices
// into weekly commodity flow demand by commodity group and mode. It doesn't
// sound like much but there is a lot of work going on in this part of CT. It
// has to unravel each PI interchange, decide whether to keep it, assign a
// mode of transport to the flow, and then convert it from annual value to
// weekly tons.
// @author rdonnelly@pbtfsc.com
// @version "0.3, 14/08/04"

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixReader;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.model.WorldZoneExternalZoneUtil;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


public class FreightDemand3 {
    private static Logger logger = Logger.getLogger("com.pb.tlumip.ct");
    static int HIGHEST_BETA_ZONE;  //
    static double SMALLEST_ALLOWABLE_TONNAGE;  // interzonal interchange threshold- read in from properties file
    ModalDistributions md;
    ValueDensityFunction vdf;
    private final ValueDensityFunction_V2 vdf2;
   // Random rn;
    String inputPath;
    String outputPath;
    ResourceBundle ctRb;
    Set<String> commoditySet;

    long outputRecordsWritten = 0L;
    TableDataSet annualDemandTable = new TableDataSet();
    AlphaToBeta a2b;
    WorldZoneExternalZoneUtil wzUtil;
    ResourceBundle globalRb;
    int[] betaPlusWZsForCT;

    FreightDemand3(ResourceBundle appRb, ResourceBundle globalRb, String ctInputs, String ctOutputs){
        //set the ctRb to the ct.properties resource bundle and get the smallest allowable tonnage from there.
        this.ctRb = appRb;
        this.globalRb = globalRb;
        SMALLEST_ALLOWABLE_TONNAGE = Double.parseDouble(ResourceUtil.getProperty(ctRb, "SMALLEST_ALLOWABLE_TONNAGE"));

        //from global.properties file we need the path to the alpha2beta file so that we can initialize the
        //highest beta zone number.
        File alpha2beta = new File(ResourceUtil.getProperty(globalRb,"alpha2beta.file"));
        a2b = new AlphaToBeta(alpha2beta);
        HIGHEST_BETA_ZONE = a2b.getMaxBetaZone();


        this.inputPath = ctInputs;
        this.outputPath = ctOutputs;
        this.md = new ModalDistributions(inputPath+"ModalDistributionParameters.txt");
        this.vdf = new ValueDensityFunction(new File(inputPath + "ValueDensityParameters.txt"));
        vdf2 = new ValueDensityFunction_V2(new File(inputPath + "ValueDensityParameters.csv")); //todo: get from property file
       
        wzUtil = new WorldZoneExternalZoneUtil(globalRb);
        int[] betaZones = a2b.getBetaExternals0Based();
        int[] worldZonesForCt = wzUtil.getWorldZonesForCT();
        betaPlusWZsForCT = new int[(betaZones.length + worldZonesForCt.length + 1)];


        System.arraycopy(betaZones,0, betaPlusWZsForCT,1,betaZones.length);
        System.arraycopy(worldZonesForCt,0,betaPlusWZsForCT,betaZones.length+1,worldZonesForCt.length);
//        System.out.println("Length of betas: " + betaZones.length);
//        System.out.println("Length of worldss: " + worldZonesForCt.length);
//        System.out.println("Length of betaPlusWZs: " + betaPlusWZsForCT.length);
//        for(int zone : betaPlusWZsForCT){
//            System.out.println(zone);
//        }
    }

    public static class MyFilter implements FilenameFilter {
        String extension;
        String commodityPrefix = "SCTG";
        public MyFilter(String extension){
            this.extension = extension;
        }
        public boolean accept(File file, String name){
            return name.contains(commodityPrefix) && name.endsWith(extension);
            //return name.endsWith(extension);
        }
    }

    public void filterExternalData () {
        //We need to read in the list of Transportable Goods that
        //were produced by PI.
        File dir = new File(ResourceUtil.getProperty(ctRb,"pi.commodity.flows"));
        String extension = globalRb.getString("matrix.extension").trim();
        String[] fileNames = dir.list(new MyFilter(extension));

        if(fileNames.length == 0) {
            logger.info("There are no filenames containing SCTG and ending with " + extension + " in " + dir);
            throw new RuntimeException();
        }

        commoditySet = new TreeSet<String>();  //auto sorts by "natural order"
        for(String file : fileNames){
            commoditySet.add(file.substring(file.indexOf("SCTG"), file.indexOf(extension)));
        }

        //Next we need to associate the appropriate skim matrix with each commodity based
        //on what PI specified in the CommoditiesI.csv file
        TableDataSet tblCommoditiesI = null;
        CSVFileReader reader = new CSVFileReader();
        try {
            tblCommoditiesI = reader.readFile(new File(ctRb.getString("pi.commodity.properties")));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            throw new RuntimeException("An exception was thrown reading file: " + ctRb.getString("pi.commodity.properties"));
        }

        //We need to associate a commodity with the appropriate distance skim
        String commodityName;
        String skimName;
        HashMap<String, String> commoditySkimMap = new HashMap<String, String>();
        for(int r = 1 ; r < tblCommoditiesI.getRowCount(); r++){
            commodityName = tblCommoditiesI.getStringValueAt(r, "Commodity");
            if(commodityName.contains("SCTG")){
                skimName = tblCommoditiesI.getStringValueAt(r, "InterchangeName2");
                commoditySkimMap.put(commodityName, skimName);
            }
        }

        //For each element in the commodity list, we need to read in the buying_$commName matrix
        //and the selling_$commName matrix file.  The zip files will have the flows
        // in a matrix so we need to get the origin zone, the destination zone and the flow
        // We also need to read in the betapkdist so that we can determine the distance between the OD
        // pair.  Once we read in this info we will determine the mode of transport and then
        // write all this info into a text file ($commName.txt) in the ct tn directory.


        String consumptionFile; // buying_$commName
        String productionFile;  //selling_$commName
        Matrix cFlowMatrix; //consumption flows
        Matrix pFlowMatrix; //production flows
        PrintWriter pw; //used to write out the txt files.

        String skimPath = ResourceUtil.getProperty(ctRb, "ts.skim.path");
        String piInputPath = ResourceUtil.getProperty(ctRb,"pi.commodity.flows");
        String previousCommoditySkimName = "";  //set it to something for first loop
        String currentCommoditySkimName;
        ZipMatrixReader zipReader;
        Matrix distMatrix = null;
        for(String commodity : commoditySet){
            currentCommoditySkimName = commoditySkimMap.get(commodity);
            if(!currentCommoditySkimName.equals(previousCommoditySkimName)){
                zipReader = new ZipMatrixReader(new File(skimPath + currentCommoditySkimName + extension));
                distMatrix = zipReader.readMatrix();
            } //else use the same one as last time.

            //Read in the buying file and the selling file
            logger.info("Reading flow values for Commodity: " + commodity);

            productionFile = "selling_" + commodity + extension;
            zipReader = new ZipMatrixReader(new File(piInputPath + productionFile));
            pFlowMatrix = zipReader.readMatrix();

            consumptionFile = "buying_" + commodity + extension;
            zipReader = new ZipMatrixReader(new File(piInputPath + consumptionFile));
            cFlowMatrix = zipReader.readMatrix();

            //These are the values that we are going to read from the flow matrices.
            int origin;
            int destination;
            float cFlow;
            float pFlow;
            String modeOfTransport;
            double distance;
            //Go thru each matrix cell by cell.  Get the buying/consumption flow, the selling/production
            // flow, then get the distance, calculate the mode, and write that line to file.
            try {
                logger.info("Determing Mode and writing flow values to " + outputPath+commodity+".txt");
                pw = new PrintWriter(new BufferedWriter(new FileWriter(outputPath+commodity+".txt")));

                for(int r = 1; r < cFlowMatrix.getExternalNumbers().length; r++){
                    origin = cFlowMatrix.getExternalNumbers()[r];   //should be the same for both
                    for(int c = 1; c < cFlowMatrix.getExternalNumbers().length; c++){
                        destination = cFlowMatrix.getExternalNumbers()[c]; //production and consumption matrix
                        if(origin != pFlowMatrix.getExternalNumbers()[r] || destination != pFlowMatrix.getExternalNumbers()[c])
                            logger.fatal("Bad assumption regarding the production, consumption matrices having" +
                                    "the same external numbering scheme");
                        if(origin != wzUtil.LOCAL_MARKET_WORLD_ZONE && destination != wzUtil.LOCAL_MARKET_WORLD_ZONE){
                            cFlow = cFlowMatrix.getValueAt(origin, destination);
                            pFlow = pFlowMatrix.getValueAt(origin, destination);
                            distance = distMatrix.getValueAt(origin, destination);

                            modeOfTransport = md.selectMode(commodity, distance);

                            pw.println(origin+","+destination+","+cFlow+","+"C"+","+modeOfTransport);
                            pw.println(origin+","+destination+","+pFlow+","+"P"+","+modeOfTransport);

                            if(logger.isDebugEnabled()) {
                                logger.debug("\t"+origin+","+destination+","+cFlow+","+"C"+","+modeOfTransport+" (distance) " + distance);
                                logger.debug("\t"+origin+","+destination+","+pFlow+","+"P"+","+modeOfTransport);
                            }
                        }
                    }
                }
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


  public void calculateTonnage () {
    Matrix2d m = new Matrix2d(wzUtil.getHighestWZForCT()+1, wzUtil.getHighestWZForCT()+1, "");
    BufferedReader br;
    StringTokenizer st;
    String s, activity, modeOfTransport;
    int origin, destination;
    double flow;

    DecimalFormat dw = new DecimalFormat();
    dw.setGroupingSize(3);
    dw.setMaximumFractionDigits(0); dw.setMinimumFractionDigits(0);
    DecimalFormat df = new DecimalFormat();
    df.setGroupingSize(0);
    df.setMaximumFractionDigits(3); df.setMinimumFractionDigits(3);
    logger.info("Converting annual demand from PI to daily freight demand:");
    logger.info(CTHelper.rightAlign("Group",6)+CTHelper.rightAlign("Production",15)+
                CTHelper.rightAlign("Consumption",15)+CTHelper.rightAlign("Annual tons",15)+
                CTHelper.rightAlign("Weekly tons",15)+CTHelper.rightAlign("Dissolved",10));

    for (String commodity : commoditySet) {
      String printString = "";
      // Read the truck flows into a matrix, where we'll do the remainder of the work
      File f = new File(outputPath+commodity+".txt");
      f.deleteOnExit();
      double totalProduction = 0.0, totalConsumption = 0.0, totalUndefined = 0.0;
      double annualTons, weeklyTons, scalingFactor;

      m.fill(0.0);   // clear values from previous commodity

      try {
        br = new BufferedReader(new FileReader(f));
        while ((s = br.readLine()) != null) {
          st = new StringTokenizer(s,",");
          origin = Integer.parseInt(st.nextToken());
          destination = Integer.parseInt(st.nextToken());
          flow = Double.parseDouble(st.nextToken());
          activity = st.nextToken();
          modeOfTransport = st.nextToken();
          if (!modeOfTransport.equals("STK")) continue;   // keep only truck flows
          if (activity.equals("P")) totalProduction += flow;
          else if (activity.equals("C")) totalConsumption += flow;
          else totalUndefined += flow;
          m.cell[origin][destination] += flow;
        }
        br.close();
      } catch (IOException e) { e.printStackTrace(); }


      if (totalUndefined>0.0) printString += ("Error: flow="+totalUndefined+
        " with undefined activity for "+commodity);
      else printString += (commodity+
        CTHelper.rightAlign(dw.format(totalProduction),15)+
        CTHelper.rightAlign(dw.format(totalConsumption),15));
      if ((totalProduction+totalConsumption)==0.0) {     // Nothing transported by truck
          printString += ("");
          logger.info(printString);
          continue;
      }

      // Since the value density functions were derived at state level, we'll convert them
      // there first, and then convert each cell in the matrix on the fly
      annualTons = vdf.getAnnualTons(commodity+"STK", m.total());
      weeklyTons = annualTons/52.0;
      scalingFactor = weeklyTons/m.total();
      printString += (CTHelper.rightAlign(dw.format(annualTons),15)+
        CTHelper.rightAlign(dw.format(weeklyTons),15));

      // Calculate daily demand for each origin-destination interchange
      double residual = 0.0;
      int p, q;   // matrix indices
      for (p=0; p< m.getRowSize(); p++)
        for (q=0; q<m.getColumnSize(); q++) {
          m.cell[p][q] *= scalingFactor;
          // We need to eliminate extremely small shipments, which are an artifact of
          // equilibrium nature of PI rather than real demand at that level.
          if (m.cell[p][q]<SMALLEST_ALLOWABLE_TONNAGE) {
            residual += m.cell[p][q];
            m.cell[p][q] = 0.0;
          }
        }

      // If we found residuals we'll need to scale the remainder of the cells so that their
      // sum equals the original weekly tons
      printString += (CTHelper.rightAlign(dw.format(residual),10));
        logger.info(printString);
      if (residual>0.0) {
        for (p=0; p<m.getRowSize(); p++)
          for (q=0; q<m.getColumnSize(); q++)
            if (m.cell[p][q]>0.0) m.cell[p][q] *= weeklyTons/(weeklyTons-residual);
      }

      // And finally, let's write the data out to binary file that can be used later. We'll
      // store the demand matrices in semi-permanent file because we'll probably want to
      // create off-line queries and summaries later.
      int commodityNumber = Integer.parseInt(commodity.substring(4));
      System.out.println("Printing weekly tons for commodity: " + commodityNumber);
      writeWeeklyTons(commodityNumber, m);
      putIntoTableDataSet(commodity, m);
    }

  }

  public void calculateTonnage_V2 () {
    Matrix2d m = new Matrix2d(wzUtil.getHighestWZForCT()+1, wzUtil.getHighestWZForCT()+1, "");
    BufferedReader br;
    StringTokenizer st;
    String s, activity, modeOfTransport;
    int origin, destination;
    double flow;

    DecimalFormat dw = new DecimalFormat();
    dw.setGroupingSize(3);
    dw.setMaximumFractionDigits(0); dw.setMinimumFractionDigits(0);
    DecimalFormat df = new DecimalFormat();
    df.setGroupingSize(0);
    df.setMaximumFractionDigits(3); df.setMinimumFractionDigits(3);
    logger.info("Converting annual demand from PI to daily freight demand:");
    logger.info(CTHelper.rightAlign("Group",6)+CTHelper.rightAlign("Production",15)+
                CTHelper.rightAlign("Consumption",15)+CTHelper.rightAlign("Annual tons",15)+
                CTHelper.rightAlign("Weekly tons",15)+CTHelper.rightAlign("Dissolved",10));



    String modalDollarFile;
    try {
        modalDollarFile = globalRb.getString("ct.dollars.by.mode.flow.file");
    } catch (MissingResourceException e) {
        modalDollarFile = null;
    }
    final boolean recordDollarFlows = modalDollarFile != null;

    int[] ext = new int[betaPlusWZsForCT.length+1];
    System.arraycopy(betaPlusWZsForCT,0,ext,1,betaPlusWZsForCT.length);

    Map<String,Map<String,Map<String,Matrix>>> dollarFlows = new TreeMap<String,Map<String,Map<String,Matrix>>>(); //commodity->mode->activity->matrix

    Map<String,double[]> totalFlows = new LinkedHashMap<String,double[]>(); //commodity->[ie,ei,ii]

    for (String commodity : commoditySet) {
      String printString = "";
      // Read the truck flows into a matrix, where we'll do the remainder of the work
      File f = new File(outputPath+commodity+".txt");
      f.deleteOnExit();
      double totalProduction = 0.0, totalConsumption = 0.0, totalUndefined = 0.0;
      double annualTons, weeklyTons, scalingFactor;

      m.fill(0.0);   // clear values from previous commodity

      if (recordDollarFlows)
        dollarFlows.put(commodity,new HashMap<String,Map<String, Matrix>>());
      Map<String,Map<String, Matrix>> dollarFlowsMap = dollarFlows.get(commodity);

      try {
        br = new BufferedReader(new FileReader(f));
        while ((s = br.readLine()) != null) {
          st = new StringTokenizer(s,",");
          origin = Integer.parseInt(st.nextToken());
          destination = Integer.parseInt(st.nextToken());
          flow = Double.parseDouble(st.nextToken());
          activity = st.nextToken();
          modeOfTransport = st.nextToken();
          if (recordDollarFlows) {
              if (!dollarFlowsMap.containsKey(modeOfTransport))
                  dollarFlowsMap.put(modeOfTransport,new HashMap<String,Matrix>());
              Map<String,Matrix> nm = dollarFlowsMap.get(modeOfTransport);
              if (!nm.containsKey(activity)) {
                  Matrix mm = new Matrix(betaPlusWZsForCT.length,betaPlusWZsForCT.length);
                  mm.setExternalNumbers(ext);
                  nm.put(activity,mm);
              }
              Matrix mm = nm.get(activity);
              mm.setValueAt(origin,destination,(float) (mm.getValueAt(origin,destination)+flow));
          }
            
          if (!modeOfTransport.equals("STK"))
              continue;   // keep only truck flows
          if (activity.equals("P")) {
              totalProduction += flow;
          } else if (activity.equals("C")) {
              totalConsumption += flow;
              m.cell[origin][destination] += flow;
          } else {
              totalUndefined += flow;
          }
        }
        br.close();
      } catch (IOException e) {
          throw new RuntimeException(e);
      }


      if (totalUndefined>0.0) printString += ("Error: flow="+totalUndefined+
        " with undefined activity for "+commodity);
      else printString += (commodity+
        CTHelper.rightAlign(dw.format(totalProduction),15)+
        CTHelper.rightAlign(dw.format(totalConsumption),15));
      if ((totalProduction+totalConsumption)==0.0) {     // Nothing transported by truck
          printString += ("");
          logger.info(printString);
          continue;
      }

      //get external zones
      WorldZoneExternalZoneUtil wz = new WorldZoneExternalZoneUtil(globalRb);
      Set<Integer> externalBetas = new HashSet<Integer>();
      for (int i : wz.getWorldZonesForCT())
          externalBetas.add(i);

      double ieTotal = 0.0;
      double eiTotal = 0.0;
      double iiTotal = 0.0;

      for (int p = 0; p < m.getRowSize(); p++) {
          boolean pExternal = externalBetas.contains(p);  //is this alpha or beta?
          for (int q = 0; q <m.getColumnSize(); q++) {
              if (externalBetas.contains(q)) {
                  if (!pExternal) {
                      ieTotal += m.cell[p][q];
                  }
              } else {
                  if (pExternal) {
                      eiTotal += m.cell[p][q];
                  } else {
                      iiTotal += m.cell[p][q];
                  }
              }
          }
      }

      totalFlows.put(commodity,new double[] {ieTotal,eiTotal,iiTotal});

      String key = commodity+"STK";
      if (vdf2.getVAF(key) == 0.0) {
          m.fill(0.0);
      } else {
          double ieFactor = vdf2.getAnnualIETonsFactor(key);
          double eiFactor = vdf2.getAnnualEITonsFactor(key);
          double iiFactor = vdf2.getAnnualIITonsFactor(key);
          double smallestAllowableTonnage = vdf2.getSmallestAllowableTonnage(key);
          double ieResidual = 0.0;
          double eiResidual = 0.0;
          double iiResidual = 0.0;
          //ieFactor = ieTotal*ieFactor/52.0/ieTotal; - without intercept, total is only useful for residuals
          ieFactor /= 52.0;
          iiFactor /= 52.0;
          eiFactor /= 52.0;
          for (int p = 0; p < m.getRowSize(); p++) {
              boolean pExternal = externalBetas.contains(p);  //is this alpha or beta?
              for (int q = 0; q <m.getColumnSize(); q++) {
                  if (externalBetas.contains(q)) {
                      if (!pExternal) {
                          m.cell[p][q] *= ieFactor;
                          if (m.cell[p][q]<smallestAllowableTonnage) {
                            ieResidual += m.cell[p][q];
                            m.cell[p][q] = 0.0;
                          }
                      }
                  } else {
                      if (pExternal) {
                          m.cell[p][q] *= eiFactor;
                          if (m.cell[p][q]<smallestAllowableTonnage) {
                            eiResidual += m.cell[p][q];
                            m.cell[p][q] = 0.0;
                          }
                      } else {
                          m.cell[p][q] *= iiFactor;
                          if (m.cell[p][q]<smallestAllowableTonnage) {
                            iiResidual += m.cell[p][q];
                            m.cell[p][q] = 0.0;
                          }
                      }
                  }
              }
          }

          double t = m.total();
          logger.info("M total " + commodity + ": " + t);
          printString += CTHelper.rightAlign(dw.format(t*52.0),15);
          printString += CTHelper.rightAlign(dw.format(t),15);
          printString += CTHelper.rightAlign(dw.format(ieResidual+eiResidual+iiResidual),15);
          logger.info(printString);

          ieFactor = eiTotal > 0.0 ? ieTotal/(ieTotal - ieResidual) : 0.0;
          eiFactor = ieTotal > 0.0 ? eiTotal/(eiTotal - eiResidual) : 0.0;
          iiFactor = iiTotal > 0.0 ? iiTotal/(iiTotal - iiResidual) : 0.0;
          //redistribute residuals
          for (int p = 0; p < m.getRowSize(); p++) {
              boolean pExternal = externalBetas.contains(p);  //is this alpha or beta?
              for (int q = 0; q <m.getColumnSize(); q++) {
                  if (externalBetas.contains(q)) {
                      if (!pExternal && ieResidual > 0.0) {
                          m.cell[p][q] *= ieFactor;
                      }
                  } else {
                      if (pExternal) {
                          m.cell[p][q] *= eiFactor;
                      } else {
                          m.cell[p][q] *= iiFactor;
                      }
                  }
              }
          }
      }

      // And finally, let's write the data out to binary file that can be used later. We'll
      // store the demand matrices in semi-permanent file because we'll probably want to
      // create off-line queries and summaries later.
      int commodityNumber = Integer.parseInt(commodity.substring(4));
      System.out.println("Printing weekly tons for commodity: " + commodityNumber);
      writeWeeklyTons(commodityNumber, m);
      putIntoTableDataSet(commodity, m);
    }

    logger.info("Total flows by commodity:");
    String format = "%10s %15.2f %15.2f %15.2f";
    logger.info(String.format("%10s %15s %15s %15s","Commodity","IE Flow","EI Flow","II Flow"));
    for (String c : totalFlows.keySet()) {
        double[] flows = totalFlows.get(c);
        logger.info(String.format(format,c,flows[0],flows[1],flows[2]));
    }



    if (recordDollarFlows) {
        //it'll be a zipped csv file
        PrintWriter w = null;
        ZipOutputStream zos = null;
        try {
            zos  = new ZipOutputStream(new FileOutputStream(modalDollarFile + ".zip"));
            zos.putNextEntry(new ZipEntry(new File(modalDollarFile).getName() + ".zip"));

            w = new PrintWriter(zos);
            String header = "mode,activity";
            Set<String> modes = new TreeSet<String>();
            Set<String> activities = new TreeSet<String>();
            for (String commodity : dollarFlows.keySet()) {
                header += "," + commodity;
                for (String mode : dollarFlows.get(commodity).keySet()) {
                    modes.add(mode);
                    for (String act : dollarFlows.get(commodity).get(mode).keySet())
                        activities.add(act);
                }
            }
            for (String mode : modes) {
                for (String act : activities) {
                    for (int i : betaPlusWZsForCT) {
                        for (int j : betaPlusWZsForCT) {
                            StringBuilder sb = new StringBuilder(mode).append(",").append(act);
                            for (String commodity : dollarFlows.keySet()) {
                                sb.append(",");
                                if (dollarFlows.get(commodity).containsKey(mode)) {
                                    if (dollarFlows.get(commodity).get(mode).containsKey(act)) {
                                        sb.append(dollarFlows.get(commodity).get(mode).get(act).getValueAt(i,j));
                                    } else {
                                        sb.append("0.0");
                                    }
                                } else {
                                    sb.append("0.0");
                                }
                                w.println(sb);
                            }
                        }
                    }
                }
            }
            w.println(header);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (w != null)
                w.close();
            else if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    //swallow
                }
            }
        }
    }
  }

  public void writeWeeklyTons (int index, Matrix2d m) {
    boolean append = true;
    if (index==1) append = false;
    String filename = outputPath+"WeeklyDemand.binary";
    try {
      DataOutputStream ds = new DataOutputStream(new BufferedOutputStream
        (new FileOutputStream(filename, append)));
      for (int p=0; p<m.getRowSize(); p++)
        for (int q=0; q<m.getColumnSize(); q++)
          if (m.cell[p][q]>0.0) {
            ds.writeInt(index);
            ds.writeInt(p);
            ds.writeInt(q);
            ds.writeDouble(m.cell[p][q]);
            ++outputRecordsWritten;
          }
      ds.flush();
      ds.close();
    } catch (IOException e) { e.printStackTrace(); }
  }
  
    public void writeWeeklyTons(){
        CSVFileWriter writer = new CSVFileWriter();
        try {
            writer.writeFile(annualDemandTable, new File(outputPath+"AnnualDemandinTons.csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
  
    private void putIntoTableDataSet(String commodity, Matrix2d m){

      //define first 2 columns (i,j)
      if(annualDemandTable.getColumnCount()==0){
          int[] col1 = new int[betaPlusWZsForCT.length*betaPlusWZsForCT.length];
          int[] col2 = new int[betaPlusWZsForCT.length*betaPlusWZsForCT.length];
          
          int index = 0;
          for (int p=1; p<betaPlusWZsForCT.length; p++){
              for (int q=1; q<betaPlusWZsForCT.length; q++){
                  col1[index] = betaPlusWZsForCT[p];
                  col2[index] = betaPlusWZsForCT[q];
                  index++;
              }
          }
          annualDemandTable.appendColumn(col1, "i");
          annualDemandTable.appendColumn(col2, "j");
      }
      
      //build up the other columns as they come in and append them
      //to the Table
      float[] newColumn = new float[betaPlusWZsForCT.length*betaPlusWZsForCT.length];
      int index = 0;
      for (int p=1; p<betaPlusWZsForCT.length; p++){
          for (int q=1; q<betaPlusWZsForCT.length; q++){
              newColumn[index] = (float)(m.cell[betaPlusWZsForCT[p]][betaPlusWZsForCT[q]] * 52.0);
              index++;
          }
      }
      annualDemandTable.appendColumn(newColumn, commodity);
  }


  public void run () {
      logger.info("Starting conversion of PI $$ to CT tons (Freight Demand)");
    Date start = new Date();
    filterExternalData();
    Date next = new Date();
    if(logger.isDebugEnabled()) {
        logger.debug("filterExternalData() run time: "+CTHelper.elapsedTime(start, next));
    }
//    calculateTonnage();
    calculateTonnage_V2();
    writeWeeklyTons();  //this writes a CSV file that has all the commodities as columns.
    if(logger.isDebugEnabled()) {
        logger.debug(outputRecordsWritten+" OD records saved");
    }
    Date end = new Date();
    if(logger.isDebugEnabled()) {
        logger.debug("calculateTonnage() runtime: "+CTHelper.elapsedTime(next, end));
    }
    logger.info("total runtime for Freight Demand: "+CTHelper.elapsedTime(start, end));
  }


    public static void main(String[] args) {
        Date start = new Date();
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_aaaCurrentData/t1/ct/ct.properties"));
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_aaaCurrentData/t1/global.properties"));
        String inputPath = ResourceUtil.getProperty(rb,"ct.base.data");
        String outputPath = ResourceUtil.getProperty(rb,"ct.current.data");
       // long randomSeed = Long.parseLong(ResourceUtil.getProperty(rb, "randomSeed"));
        FreightDemand3 fd = new FreightDemand3(rb,globalRb,inputPath,outputPath);
        fd.run();
        logger.info("total time: "+CTHelper.elapsedTime(start, new Date()));
   }

}

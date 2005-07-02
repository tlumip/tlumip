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
package com.pb.tlumip.pt.estimation;
import com.pb.common.datastore.DataManager;
import com.pb.common.matrix.CollapsedMatrixCollection;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.InTextFile;
import com.pb.common.util.OutTextFile;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.model.TravelTimeAndCost;
import com.pb.tlumip.pt.DestinationChoiceLogsums;
import com.pb.tlumip.pt.TazData;
import com.pb.tlumip.pt.estimation.TazOld;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.File;
import java.util.*;

/** 
 * A class that creates an estimation
 * file for home location choice
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class CreateHomeLocationEstimationFile {
     
     DataManager dm = new DataManager();  //Create a data manager, connect to default data-store

     static final int TOTALSEGMENTS = 9;
     static final String[] purposes = {"w1","w2","w3","w4","c1","c2","c3","s","r","o","b"};  //work,school,shop,recreate,other,workbased
     static final String path = new String("/temp/matrix/");
     static int SIZE=5000;
     static final boolean debug=true;
     static final int originDebug = 2761;
     static final long sampnoDebug=407100;
     
     
    public static long startTime;
    public static long endTime;
    public static long elapsedTime;

    
     TravelTimeAndCost departCost;
     TravelTimeAndCost returnCost;

    ResourceBundle ptRb;

     public CreateHomeLocationEstimationFile(ResourceBundle appRb, ResourceBundle globalRb) throws FileNotFoundException{
                
          this.ptRb = appRb;

          //read the taz data from jDataStore; if it doesn't exist, write it to jDataStore from csv file first
          TazData taz = new TazData();
//          if(!dm.tableExists("TazData"))
          //     taz.addTazDataToJDataStore(dm);
               
          //might want to send the method the year
          taz.readData(appRb, globalRb, "tazData.file");
          
          //read the time and distance skims into memory
          if(debug) System.out.println("Reading time skims into CollapsedMatrices");
         Matrix m = (ZipMatrixReader.createReader(MatrixType.ZIP, new File(path+"pktime.zip")).readMatrix());
          CollapsedMatrixCollection skims=new CollapsedMatrixCollection(m);
          skims.addMatrix(ZipMatrixReader.createReader(MatrixType.ZIP, new File(path+"pkdist.zip")).readMatrix());
          skims.addMatrix(ZipMatrixReader.createReader(MatrixType.ZIP, new File(path+"optime.zip")).readMatrix());
          skims.addMatrix(ZipMatrixReader.createReader(MatrixType.ZIP, new File(path+"opdist.zip")).readMatrix());
          
          
          //now get work mode choice logsums for all 9 segments, and read them into skims HashMap class as well
          String[] mName = new String[TOTALSEGMENTS];
          
          for(int segment=0;segment<TOTALSEGMENTS;++segment){
               char thisPurpose='w';
               //create matrix for this segment
               mName[segment] = new String(path+ new Character(thisPurpose).toString() + new Integer(segment).toString()
                            + new String("ls.zip"));
                                  
               System.out.println("Reading file "+mName[segment]);
               skims.addMatrix(ZipMatrixReader.createReader(MatrixType.ZIP, new File(mName[segment])).readMatrix());
               
          };

          //read dc logsums from jDataStore;store them in a HashMap
          Hashtable dcLogsums = new Hashtable();
          DestinationChoiceLogsums dcl = new DestinationChoiceLogsums();
          for(int purpose=0;purpose<purposes.length;++purpose){
               for(int segment=0;segment<TOTALSEGMENTS;++segment){
               try{
                         dcl = new DestinationChoiceLogsums();
                    }catch(Exception e){
                         e.printStackTrace();  
                         System.exit(1);                                       
                  }                                                                

                    System.out.println("Reading DC LOGSUMS "+purposes[purpose]+" "+segment);
                    dcl.readFromJDataStore("DC LOGSUMS",purposes[purpose],segment);
                    dcLogsums.put((purposes[purpose]+new Integer(segment).toString()),dcl);
               }
          }
          
          
          
          //read records from data file into ArrayList
          System.out.println("Reading household records");
          ArrayList households = new ArrayList();
          InTextFile householdFile = new InTextFile();
          householdFile.open("/gen2/home location choice/allhouseholds.csv");

          String inHousehold=new String();
          
          try{
               while((inHousehold = householdFile.readLine())!=null){
                    if(inHousehold.length()==0)
                         break;
                    HomeLocationEstimationFileRecord thisRecord = new HomeLocationEstimationFileRecord();
                    households.add(thisRecord.parse(inHousehold));
               }
               householdFile.close();
          }catch(Exception e){
               System.out.println("Error parsing household:\n"+inHousehold);
               System.exit(1);
          }
               
          //open file for writing
          OutTextFile outFile = new OutTextFile();
          PrintWriter oFile = outFile.open("/gen2/home location choice/allhhsHLC.dat");
 
 
          RandomSampleByDistance rs = new RandomSampleByDistance();
          ArrayList chosenDistrictTazs = new ArrayList();
          
          //cycle through household ArrayList, generate random sample of 
          //tazs 
          Iterator hhIterator = households.iterator();
          while(hhIterator.hasNext()){
               HomeLocationEstimationFileRecord thisHousehold=(HomeLocationEstimationFileRecord)hhIterator.next();
               System.out.println("Household "+thisHousehold.sampno);
                //check if originTaz is within range; continue if not
                if(thisHousehold.homeTaz==0||thisHousehold.homeTaz==9999||thisHousehold.homeTaz==99999||thisHousehold.homeTaz==3156||
                     thisHousehold.homeTaz==3185||thisHousehold.homeTaz==3186){
                       continue;
                }else if(debug && sampnoDebug==thisHousehold.sampno)
                     System.out.println("Home taz: "+thisHousehold.homeTaz);
                
                DistrictTaz[] tazs = new DistrictTaz[taz.tazData.size()];
               
//               RandomSampleByDistance rs = new RandomSampleByDistance(36);
                if(thisHousehold.numberWorkers>0){
       
               if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("Setting market segments for record "+thisHousehold.sampno+","+
                    thisHousehold.numberWorkers+" workers");
               //set market segments for each worker
               int worker1Segment=calcWorkerSegment(thisHousehold.occupat1,thisHousehold.indstry1);
               int worker2Segment=0;
               int worker3Segment=0;
               if(thisHousehold.numberWorkers>=2)
                    worker2Segment=calcWorkerSegment(thisHousehold.occupat2,thisHousehold.indstry2);
               if(thisHousehold.numberWorkers==3)
                    worker3Segment=calcWorkerSegment(thisHousehold.occupat3,thisHousehold.indstry3);
                    
               //calculate household segments:: work=auto/worker and nonWork =autos/persons  (0->8)
               int workSegment=calcHouseholdSegment(thisHousehold.numVehic,(thisHousehold.partwrk+thisHousehold.fullwrk),
                    thisHousehold.income1);
               int nonWorkSegment=calcHouseholdSegment(thisHousehold.numVehic,thisHousehold.size,thisHousehold.income1);     
               
               //declare work destination choice logsums to the correct value, based on the worker segment for each worker
               if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("Setting worker dc logsums to correct values");
               DestinationChoiceLogsums worker1DcLogsums = new DestinationChoiceLogsums();
               DestinationChoiceLogsums worker2DcLogsums = new DestinationChoiceLogsums();
               DestinationChoiceLogsums worker3DcLogsums = new DestinationChoiceLogsums();
               worker1DcLogsums = (DestinationChoiceLogsums) dcLogsums.get("w"+new Integer(worker1Segment).toString()
                    +new Integer(workSegment).toString());
               if(thisHousehold.numberWorkers>=2)
                    worker2DcLogsums =(DestinationChoiceLogsums) dcLogsums.get("w"+new Integer(worker2Segment).toString()
                         +new Integer(workSegment).toString());
               if(thisHousehold.numberWorkers==3)
                    worker3DcLogsums = (DestinationChoiceLogsums) dcLogsums.get("w"+new Integer(worker3Segment).toString()
                         +new Integer(workSegment).toString());

               //set non work destination choice logsums
               if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("Setting non-work dc logsums to correct values");
               DestinationChoiceLogsums school1DcLogsums = (DestinationChoiceLogsums)dcLogsums.get("c1"
                    +new Integer(nonWorkSegment).toString());
               DestinationChoiceLogsums school2DcLogsums = (DestinationChoiceLogsums)dcLogsums.get("c2"
                    +new Integer(nonWorkSegment).toString());
               DestinationChoiceLogsums school3DcLogsums = (DestinationChoiceLogsums)dcLogsums.get("c3"
                    +new Integer(nonWorkSegment).toString());
               DestinationChoiceLogsums shopDcLogsums = (DestinationChoiceLogsums)dcLogsums.get("s"
                    +new Integer(nonWorkSegment).toString());
               DestinationChoiceLogsums recreateDcLogsums = (DestinationChoiceLogsums)dcLogsums.get("r"
                    +new Integer(nonWorkSegment).toString());
               DestinationChoiceLogsums otherDcLogsums = (DestinationChoiceLogsums)dcLogsums.get("o"
                    +new Integer(nonWorkSegment).toString());
               DestinationChoiceLogsums workbDcLogsums = (DestinationChoiceLogsums)dcLogsums.get("b"
                    +new Integer(nonWorkSegment).toString());
               
               if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("Setting taz data");
               //set distance and other variables for array of districtTazs
               int i=0;
               Enumeration tazEnum=taz.tazData.elements();
               while(tazEnum.hasMoreElements()){
                    
                    DistrictTaz thisTaz = new DistrictTaz();
                    thisTaz.setVariables( (TazOld) tazEnum.nextElement());
                    if(debug && sampnoDebug==thisHousehold.sampno) 
                        System.out.println("**Setting values for taz "+thisTaz.zoneNumber);
     
                    //set distance, work logsum, other logsums
                    thisTaz.distance=skims.getValue(thisHousehold.wrkTaz1,thisTaz.zoneNumber,path+"opdist.zip");
                    if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("Distance "+thisTaz.distance);
                    
                    thisTaz.mcLogsum1=skims.getValue(thisHousehold.wrkTaz1,thisTaz.zoneNumber,
                         path+"w"+new Integer(workSegment).toString()+"ls.zip");
                    if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("MC logsum worker 1: "+thisTaz.mcLogsum1);
                    thisTaz.workDcLogsum1=((Double) worker1DcLogsums.logsums.get(new Integer(thisTaz.zoneNumber))).floatValue();
                    if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("DC logsum worker 1: "+thisTaz.workDcLogsum1);
                    
                    if(thisHousehold.numberWorkers>=2){
                         thisTaz.mcLogsum2=skims.getValue(thisHousehold.wrkTaz2,thisTaz.zoneNumber,
                              path+"w"+new Integer(workSegment).toString()+"ls.zip");
                              if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("MC logsum worker 2: "+thisTaz.mcLogsum2);
                              thisTaz.workDcLogsum2=((Double) worker2DcLogsums.logsums.get(new Integer(thisTaz.zoneNumber))).floatValue();
                              if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("DC logsum worker 2: "+thisTaz.workDcLogsum2);
                    }

                    if(thisHousehold.numberWorkers==3){
                         thisTaz.mcLogsum3=skims.getValue(thisHousehold.wrkTaz3,thisTaz.zoneNumber,
                              path+"w"+new Integer(workSegment).toString()+"ls.zip");
                         if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("MC logsum worker 3: "+thisTaz.mcLogsum3);
                         thisTaz.workDcLogsum3=((Double) worker3DcLogsums.logsums.get(new Integer(thisTaz.zoneNumber))).floatValue();
                         if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("DC logsum worker 3: "+thisTaz.workDcLogsum3);
                    }
          
                    //following variables for each zone
                    thisTaz.schoolDcLogsum1 = ((Double)school1DcLogsums.logsums.get(new Integer(thisTaz.zoneNumber))).floatValue(); 
                    thisTaz.schoolDcLogsum2 = ((Double)school2DcLogsums.logsums.get(new Integer(thisTaz.zoneNumber))).floatValue(); 
                    thisTaz.schoolDcLogsum3 =((Double)school3DcLogsums.logsums.get(new Integer(thisTaz.zoneNumber))).floatValue(); 
                    thisTaz.shopDcLogsum = ((Double)shopDcLogsums.logsums.get(new Integer(thisTaz.zoneNumber))).floatValue(); 
                    thisTaz.recreateDcLogsum =  ((Double)recreateDcLogsums.logsums.get(new Integer(thisTaz.zoneNumber))).floatValue(); 
                    thisTaz.otherDcLogsum = ((Double)otherDcLogsums.logsums.get(new Integer(thisTaz.zoneNumber))).floatValue(); 
                    thisTaz.workbDcLogsum = ((Double)workbDcLogsums.logsums.get(new Integer(thisTaz.zoneNumber))).floatValue(); 
                    tazs[i]=thisTaz;
                    ++i;
               }
               //now sort the tazs, then add them to the districts, and get back an array of 
               //randomly-selected DistrictTazs
               if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("Sorting Tazs");
               Arrays.sort(tazs);
               chosenDistrictTazs = rs.chooseTAZsAccordingToReferenceTAZ(tazs,thisHousehold.homeTaz,thisHousehold.wrkTaz1,
                skims,(path+"opdist.zip"), true);
                }
               
               if(thisHousehold.numberWorkers==0)
                    chosenDistrictTazs = rs.chooseTAZsAccordingToReferenceTAZ(tazs,thisHousehold.homeTaz,thisHousehold.wrkTaz1,
                skims,(path+"opdist.zip"), false);
              
               
               if(debug && sampnoDebug==thisHousehold.sampno) System.out.println("Printing data");
               //print out the data
               thisHousehold.print(oFile);
               oFile.print(" ");
                for(int j=0;j<chosenDistrictTazs.size();++j){
                    DistrictTaz dt=(DistrictTaz) chosenDistrictTazs.get(j);
                    dt.print(oFile);
                    oFile.print(" ");
               }

               oFile.print("\n");
          }

          
     } //end constructor     
     public static void main (String[] args){
          ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
         ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
          System.out.println("creating home location choice estimation file");
          
          try{
               CreateHomeLocationEstimationFile homeEstimation = new CreateHomeLocationEstimationFile(rb, globalRb);
          }catch(Exception e){
               System.out.println("Error creating estimation file");
               System.exit(1);
          };
          
          System.out.println("created home location estimation file");
          System.exit(1);
                                            
    
} //end main


    public static void startTimer() {
        startTime = System.currentTimeMillis();
    }    

    public static void endTimer() {
        endTime = System.currentTimeMillis();
    }    

    public static long elapsedTime() {
        return (endTime-startTime);
    }    

     public int calcWorkerSegment(int occupation, int industry){
          
          int segment=4;
          if(occupation==1||(occupation==2 && industry!=7))           
               segment=1;
          else if(occupation==2 && industry==7)
               segment=2;
          else if(occupation==5||occupation==6)
               segment=3;
          
          return segment;
     }

     public int calcHouseholdSegment(int autos, int size, int income){
          
          int segment=0;
          //hh income - 0-15,15-30,30+
          boolean inclow=false;
          boolean incmed=false;
          boolean inchi=false;
          if(income>=1 && income<=3)
               inclow=true;
          else if(income>=7 && income<=13)
               inchi=true;
          else 
               incmed=true;
          
          if(inclow){
               if(autos==0) segment=0;
               else if(autos<size) segment=1;
               else segment=2;
          }
          if(incmed){
               if(autos==0) segment=3;
               else if(autos<size) segment=4;
               else segment=5;
          }
          if(inchi){
               if(autos==0) segment=6;
               else if(autos<size) segment=7;
               else segment=8;
          }
          

          return segment;
     }


}


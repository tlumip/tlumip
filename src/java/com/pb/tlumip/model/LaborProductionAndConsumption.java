package com.pb.tlumip.model;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.MatrixCollection;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pecas.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * LaborProductionAndConsum
 * 
 * @author Steve Hansen
 * @version 1.0
 * May 13, 2004
 * 
 */
public class LaborProductionAndConsumption {
    
    //output file names
    String laborProduction = "laborDollarProduction.csv";
    String laborConsumption = "laborDollarConsumption.csv";
    String laborProductionSum = "laborDollarProductionSum.csv";
    String laborConsumptionSum = "laborDollarConsumptionSum.csv";
    
    AlphaToBeta a2b;
    Collection occupationSet;
    Collection activitySet;
    Collection householdSegmentSet;
    HashMap production;
    HashMap consumption;
    MatrixCollection activityQuantity;
    
    protected static Logger logger = Logger.getLogger("com.pb.tlumip.model");
    ResourceBundle rb;

    
    String[] householdSegments =  { "HH0to5k1to2",
		                            "HH0to5k3plus",
		                            "HH5to10k1to2",
		                            "HH5to10k3plus",
                                    "HH10to15k1to2",
                                    "HH10to15k3plus",
                                    "HH15to20k1to2",
                                    "HH15to20k3plus",
                                    "HH20to30k1to2",
                                    "HH20to30k3plus",
                                    "HH30to40k1to2",
                                    "HH30to40k3plus",
                                    "HH40to50k1to2",
                                    "HH40to50k3plus",
                                    "HH50to70k1to2",
                                    "HH50to70k3plus",
                                    "HH70kUp1to2",
                                    "HH70kUp3plus"
                                   };
    
    String[] occupations = {"1_ManPro","1a_Health","2_PstSec","3_OthTchr","4_OthP&T","5_RetSls","6_OthR&C","7_NonOfc"};
    
    String[] activities = {"ACCOMMODATIONS",
                           "AGRICULTURE AND MINING-Agriculture",
                           "AGRICULTURE AND MINING-Office",
                           "COMMUNICATIONS AND UTILITIES-Light Industry",
                           "COMMUNICATIONS AND UTILITIES-Office",
                           "CONSTRUCTION",
                           "ELECTRONICS AND INSTRUMENTS-Light Industry",
                           "ELECTRONICS AND INSTRUMENTS-Office",
                           "FIRE BUSINESS AND PROFESSIONAL SERVICES",
                           "FOOD PRODUCTS-Heavy Industry",
                           "FOOD PRODUCTS-Light Industry",
                           "FOOD PRODUCTS-Office",
                           "FORESTRY AND LOGGING",
                           "GOVERNMENT ADMINISTRATION-Government Support",
                           "GOVERNMENT ADMINISTRATION-Office",
                           "HEALTH SERVICES-Hospital",
                           "HEALTH SERVICES-Institutional",
                           "HEALTH SERVICES-Office",
                           "HIGHER EDUCATION",
                           "HOMEBASED SERVICES",
                           "LOWER EDUCATION-Grade School",
                           "LOWER EDUCATION-Office",
                           "LUMBER AND WOOD PRODUCTS-Heavy Industry",
                           "LUMBER AND WOOD PRODUCTS-Office",
                           "OTHER DURABLES-Heavy Industry",
                           "OTHER DURABLES-Light Industry",
                           "OTHER DURABLES-Office",
                           "OTHER NON-DURABLES-Heavy Industry",
                           "OTHER NON-DURABLES-Light Industry",
                           "OTHER NON-DURABLES-Office",
                           "PERSONAL AND OTHER SERVICES AND AMUSEMENTS",
                           "PULP AND PAPER-Heavy Industry",
                           "PULP AND PAPER-Office",
                           "RETAIL TRADE-Office",
                           "RETAIL TRADE-Retail",
                           "TRANSPORT-Depot",
                           "TRANSPORT-Office",
                           "WHOLESALE TRADE-Office",
                           "WHOLESALE TRADE-Warehouse",
                           "Capitalists",
                           "GovInstitutions"
                          };      
            
    String[] columnHeaders = {"zoneNumber","occupation",
                              "HH0to5k0to2","HH0to5k2plus",
							  "HH5to10k0to2","HH5to10k2plus",
							  "HH10to15k0to2","HH10to15k2plus",
							  "HH15to20k0to2","HH15to20k2plus",
							  "HH20to30k0to2","HH20to30k2plus",
							  "HH30to40k0to2","HH30to40k2plus",
							  "HH40to50k0to2","HH40to50k2plus",
							  "HH50to70k0to2","HH50to70k2plus",
							  "HH70kUp0to2", "HH70kUp2plus"
                             };

    public LaborProductionAndConsumption(ResourceBundle propertyFile){
          this.rb = propertyFile;
    }

    public TableDataSet loadTableDataSet(String fileName, String source){
        
        String path = ResourceUtil.getProperty(rb, source);
        try {
            String fullPath = path+fileName;
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(fullPath));
            return table;
        } catch (IOException e) {
            logger.fatal("Error loading TableDataSet "+fileName);
            e.printStackTrace();
        }
        return null;
    }
    
    public MatrixCollection createMatrixCollection(int rows, int[] externalNumbers){
        MatrixCollection mc = new MatrixCollection();
        
        Iterator i = occupationSet.iterator();
        while(i.hasNext()){
            String occupationCode = (String)i.next();                   
            mc.addMatrix(occupationCode,rows,1);
            mc.getMatrix(occupationCode).setExternalNumbers(externalNumbers);      
        }
        return mc;
    }
    
     
    public void setZoneMap(TableDataSet table){

        a2b = new AlphaToBeta(table.getColumnAsInt(table.getColumnPosition("AZone")),
                              table.getColumnAsInt(table.getColumnPosition("BZone")));
    }
    
    public void setPopulation(TableDataSet table){
        int activityColumn = table.getColumnPosition("Activity");
        int zoneColumn = table.getColumnPosition("ZoneNumber");
        int quantityColumn = table.getColumnPosition("Quantity");
        
        activityQuantity = new MatrixCollection();
        for(int i=1;i<=table.getRowCount();i++){
            if(householdSegmentSet.contains(table.getStringValueAt(i,activityColumn))||activitySet.contains(table.getStringValueAt(i,activityColumn))){
                if(activityQuantity.getMatrix(table.getStringValueAt(i,activityColumn))==null){
                    activityQuantity.addMatrix(table.getStringValueAt(i,activityColumn),a2b.alphaSize(),1);
                    activityQuantity.getMatrix(table.getStringValueAt(i,activityColumn)).setExternalNumbers(a2b.getAlphaExternals());                   
                }
                int zone = (int)table.getValueAt(i,zoneColumn);
                activityQuantity.setValue(zone,1,table.getStringValueAt(i,activityColumn),
                            table.getValueAt(i,quantityColumn));
            }
        }
    }
    
   
    public void setProductionAndConsumption(){
        // this version uses in-memory PI objects instead of using a TableDataSet of ZonalMakeUse data.
        
        createProductionAndConsumptionHashMap();
        AbstractTAZ[] zones = AbstractTAZ.getAllZones();
        // go through all zonal make use coefficients in memory
        String occupation = null;
        String activity = null;
        Iterator it = ProductionActivity.getAllProductionActivities().iterator();
        while (it.hasNext()) {
            ProductionActivity p = (ProductionActivity)it.next();
            ConsumptionFunction cf = p.getConsumptionFunction();
            ProductionFunction pf = p.getProductionFunction();
            try {
                for (int z = 0; z < p.getMyDistribution().length; z++) {
                    double[] buyingZUtilities = new double[cf.size()];
                    for (int c = 0; c < cf.size(); c++) {
                        AbstractCommodity com = cf.commodityAt(c);
                        if (com == null) {
                            buyingZUtilities[c] = 0;
                        } else {
                            buyingZUtilities[c] = com.calcZUtility(zones[z], false);
                        }
                    } //CUSellc,z and CUBuyc,z have now been calculated for the commodites made or used by the activity
                    double[] consumptionAmounts = cf.calcAmounts(buyingZUtilities);
                    double[] sellingZUtilities = new double[pf.size()];
                    for (int c = 0; c < pf.size(); c++) {
                        AbstractCommodity com = pf.commodityAt(c);
                        if (com == null) {
                            sellingZUtilities[c] = 0;
                        } else {
                            sellingZUtilities[c] = com.calcZUtility(zones[z], true);
                        }
                    } //CUSellc,z and CUBuyc,z have now been calculated for the commodites made or used by the activity
                    double[] productionAmounts = pf.calcAmounts(sellingZUtilities);
                    for (int c = 0; c < cf.size(); c++) {
                        AbstractCommodity com = cf.commodityAt(c);
                        if (com!= null) {
                            occupation = com.getName();
                            activity = p.name;
                            if(activitySet.contains(activity) && occupationSet.contains(occupation)){
                                MatrixCollection mc = (MatrixCollection)consumption.get(activity);
                                mc.setValue(zones[z].getZoneUserNumber(),
                                        1,
                                        occupation,
                                        (float) consumptionAmounts[c]);
                                consumption.put(activity, mc);
                                
                            }
                        }
                    } 
                    for (int c = 0; c < pf.size(); c++) {
                        AbstractCommodity com = pf.commodityAt(c);
                        if (com!= null) {
                            occupation = com.getName();
                            activity = p.name;
                            if(householdSegmentSet.contains(activity)&& occupationSet.contains(occupation)){
                                MatrixCollection mc = (MatrixCollection)production.get(activity);
                                mc.setValue(zones[z].getZoneUserNumber(),
                                        1,
                                        occupation,
                                        (float) productionAmounts[c]);
                                production.put(activity, mc);
                            }
                        }
                    }
                } // end of zone loop
            } catch (OverflowException e) {
                logger.fatal("Can't write labour and production, "+e);
                throw new RuntimeException("Can't write labour production", e);
            }
        } // end of production activity loop
    }

    public void setProductionAndConsumption(TableDataSet makeUse ){
            
        int activityColumn = makeUse.getColumnPosition("Activity");
        int zoneColumn = makeUse.getColumnPosition("ZoneNumber");
        int commodityColumn = makeUse.getColumnPosition("Commodity");
        int mORuColumn =makeUse.getColumnPosition("MorU");
        int coefficientColumn = makeUse.getColumnPosition("Coefficient");

        for(int row=1;row<=makeUse.getRowCount();row++){
        	
            String occupation = makeUse.getStringValueAt(row,commodityColumn);
            if(makeUse.getStringValueAt(row,mORuColumn).equals("U")){
                if(activitySet.contains(makeUse.getStringValueAt(row,activityColumn)) && occupationSet.contains(occupation)){
                    MatrixCollection mc = (MatrixCollection)consumption.get(makeUse.getStringValueAt(row,activityColumn));
                    mc.setValue((int)makeUse.getValueAt(row,zoneColumn),
                                1,
                                makeUse.getStringValueAt(row,commodityColumn),
                                makeUse.getValueAt(row,coefficientColumn));
                    consumption.put(makeUse.getStringValueAt(row,activityColumn), mc);
                    
                }
            }
            else if(makeUse.getStringValueAt(row,mORuColumn).equals("M")){
                if(householdSegmentSet.contains(makeUse.getStringValueAt(row,activityColumn))&& occupationSet.contains(occupation)){
                    MatrixCollection mc = (MatrixCollection)production.get(makeUse.getStringValueAt(row,activityColumn));
                    mc.setValue((int)makeUse.getValueAt(row,zoneColumn),
                                1,
                                makeUse.getStringValueAt(row,commodityColumn),
                                makeUse.getValueAt(row,coefficientColumn));
                    production.put(makeUse.getStringValueAt(row,activityColumn), mc);
                }
            }
       }
    }       
    
    /**
     * 
     * @param s - String[] to convert to a list
     */
    public Collection toSet(String[] s){
        Collection c = new HashSet();
        for(int i=0;i<s.length;i++)
            c.add(s[i]);
        return c;
    }
    
    public void createProductionAndConsumptionHashMap(){
        production = new HashMap();
        consumption = new HashMap();
        for(int hhSeg = 0;hhSeg<householdSegments.length;hhSeg++){
            String segment = householdSegments[hhSeg];           
            production.put(segment,createMatrixCollection(a2b.betaSize(),a2b.getBetaExternals()));            
        }
        
        for(int acts = 0;acts<activities.length;acts++){
            String activity = activities[acts];            
            consumption.put(activity,createMatrixCollection(a2b.betaSize(),a2b.getBetaExternals()));   
        }
        
    }
    
    public void writeToCSV(){
        String pathName = ResourceUtil.getProperty(rb,"output.data");
        String fileName = pathName+laborProduction;
        PrintWriter pFile = open(fileName);

        fileName = pathName+laborConsumption;
        PrintWriter cFile = open(fileName);

        //Print header info
        for(int i=0;i<columnHeaders.length;i++){
            if(i<columnHeaders.length-1){
                pFile.print(columnHeaders[i]+",");
            } 
            else{
             pFile.print(columnHeaders[i]);
             pFile.println();                     
            }   
        }
        Iterator o = occupationSet.iterator();
        while(o.hasNext()){
            String occupation = (String)o.next();
            int[] alphaZones = a2b.getAlphaExternals();
            for(int taz=1;taz<alphaZones.length;taz++){
                pFile.print(alphaZones[taz]+",");
                pFile.print(occupation+",");
                for(int hhSeg=0;hhSeg<householdSegments.length;hhSeg++){
                       float population = activityQuantity.getValue(alphaZones[taz],1,householdSegments[hhSeg]);
                       int betaZone = a2b.getBetaZone(alphaZones[taz]);
                       float prodNumber = ((MatrixCollection)production.get(householdSegments[hhSeg])).getValue(betaZone,1,(String)occupation);
                       float householdProduction = population * prodNumber;
                       if(hhSeg<householdSegments.length-1){
                       	   pFile.print(householdProduction+",");
                       } 
                       else{
                        pFile.print(householdProduction);
                        pFile.println();            
                       }
                }
            }
        }
        pFile.close();
        
        
        //Print header info
        cFile.print("zoneNumber"+",");
        cFile.print("occupation"+",");
        for(int i=0;i<activities.length;i++){
            if(i<activities.length-1){
                cFile.print(activities[i]+",");
            } 
            else{
             cFile.print(activities[i]);
             cFile.println();                     
            }   
        }
        
        o = occupationSet.iterator();
        while(o.hasNext()){
            String occupation = (String)o.next();
            int[] alphaZones = a2b.getAlphaExternals();
            for(int taz=1;taz<alphaZones.length;taz++){
                cFile.print(alphaZones[taz]+",");
                cFile.print(occupation+",");
                for(int acts=0;acts<activities.length;acts++){
                       float population = activityQuantity.getValue(alphaZones[taz],1,activities[acts]);
                       int betaZone = a2b.getBetaZone(alphaZones[taz]);
                       float consumeNumber = ((MatrixCollection)consumption.get(activities[acts])).getValue(betaZone,1,(String)occupation);
                       float householdConsumption = population * consumeNumber;
                       if(acts<activities.length-1){
                           cFile.print(householdConsumption+",");
                       } 
                       else{
                        cFile.print(householdConsumption);
                        cFile.println();            
                       }
                }
            }
        }
        cFile.close();

        logger.info("\tlaborDollarProduction.csv and laborDollarConsumption.csv files have been written");

    }
    
    public static PrintWriter open(String textFileName){
        PrintWriter pwFile;
        try {
            pwFile = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(textFileName)));
            return pwFile;
        } catch (IOException e) {
            System.out.println("Could not open file " + textFileName + " for writing\n");           
            System.exit(1);
        }
        return null;
    
     }
    
    public float[] zoneFloatArray(){
        int[] externals = a2b.getAlphaExternals();
        float[] zoneArray = new float[a2b.alphaSize()];
        for(int z=1;z<=a2b.alphaSize();z++){
            zoneArray[z-1] = externals[z];
        }
        return zoneArray;
    }
    
    public void sumActivities(HashMap activities, Collection aSet, String fName){
        float population;
        int betaZone;
        float betaValue;
        float value;
        TableDataSet table = new TableDataSet();
        table.setName(fName);
        table.appendColumn(zoneFloatArray(),"zoneNumber");
        Iterator o = occupationSet.iterator();
        while(o.hasNext()){
            String occupation = (String)o.next();
            float[] tableColumn = new float[a2b.alphaSize()];
            Iterator a = aSet.iterator();
            while(a.hasNext()){
                String activity = (String)a.next();
                int[] alphaZones = a2b.getAlphaExternals();
                for(int z=1;z<=a2b.alphaSize();z++){
                    population = activityQuantity.getValue(alphaZones[z],1,activity);
                    betaZone = a2b.getBetaZone(alphaZones[z]);
                    betaValue = ((MatrixCollection)activities.get(activity)).getValue(betaZone,1,occupation);
                    value = population * betaValue;
                    tableColumn[z-1] = tableColumn[z-1]+value;
                }
            }
            table.appendColumn(tableColumn,occupation);
        }
        String pathName = ResourceUtil.getProperty(rb,"output.data");
        String fileName = pathName+fName;        
        try{
            CSVFileWriter cfw = new CSVFileWriter();
        	cfw.writeFile(table,new File(fileName));

        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void createOccupationSet() {
        occupationSet = toSet(occupations);
    }

    public void createHouseholdSegmentSet() {
        householdSegmentSet = toSet(householdSegments);
    }

    public void createActivitySet() {
        activitySet = toSet(activities);
    }

    public void sumProductionActivities(){
        sumActivities(production, householdSegmentSet, laborProductionSum);
        logger.info("\tlaborDollarConsumptionSum.csv has been written");
    }

    public void sumConsumptionActivities(){
        sumActivities(consumption, activitySet, laborConsumptionSum);
        logger.info("\tlaborDollarProductionSum.csv has been written");
    }


    public static void main(String[] args){
        LaborProductionAndConsumption labor = new LaborProductionAndConsumption(ResourceUtil.getResourceBundle("tlumip"));
        logger.info("test");
        TableDataSet householdQuantity = labor.loadTableDataSet("ActivityLocations2.csv","tlumip.input.data");
        TableDataSet makeUse = labor.loadTableDataSet("ZonalMakeUse.csv","tlumip.input.data");
        TableDataSet alphaToBeta = labor.loadTableDataSet("alpha2beta.csv","tlumip.input.data");
        logger.info("loaded data");
        labor.setZoneMap(alphaToBeta);
        
        labor.occupationSet = labor.toSet(labor.occupations);
        labor.activitySet = labor.toSet(labor.activities);
        labor.householdSegmentSet = labor.toSet(labor.householdSegments);

        labor.setPopulation(householdQuantity);        
        labor.setProductionAndConsumption(makeUse);
        labor.writeToCSV();
        
        labor.sumActivities(labor.production, labor.householdSegmentSet, labor.laborProductionSum);     
        labor.sumActivities(labor.consumption, labor.activitySet, labor.laborConsumptionSum);     
        logger.info("Finished setProductionAndConsumption()");
    }
}


package com.pb.despair.pt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.logging.Logger;


/**
 * Class to access TAZ information from CSV File
 *
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 *
 */
public class TazData implements Cloneable{

    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
    boolean debug = false;
    String tazDataTableName = "TazData";

    //a hashtable of taz objects
    public Hashtable tazData = new Hashtable();



    public void readData(ResourceBundle rb, String fileName) {
        if(debug) logger.fine("Getting table: " + tazDataTableName);

        TableDataSet table = loadTableDataSet(rb, fileName);
        int floorNameColumn = table.getColumnPosition("FLRName");
        int squareFeetColumn = table.getColumnPosition("BldgMSQFT");
        Taz thisTaz;

        for (int rowNumber = 1; rowNumber <= table.getRowCount();rowNumber++) {
            if (tazData.containsKey(
                        new Integer(
                            (int) table.getValueAt(rowNumber,
                                table.getColumnPosition("AZone"))))) {
                thisTaz = (Taz) tazData.get(new Integer(
                            (int) table.getValueAt(rowNumber,
                                table.getColumnPosition("AZone"))));
            } else {
                thisTaz = new Taz();
                thisTaz.zoneNumber = (int) table.getValueAt(rowNumber,table.getColumnPosition("AZone"));
                thisTaz.setName(new Integer(thisTaz.zoneNumber).toString());
                thisTaz.accommodation = table.getValueAt(rowNumber,"FLR.Accommodation");
                thisTaz.agriculture = table.getValueAt(rowNumber,"FLR.Agriculture");
                thisTaz.depot = table.getValueAt(rowNumber, "FLR.Depot");
                thisTaz.government = table.getValueAt(rowNumber, "FLR.Government.Support");
                thisTaz.gradeSchool = table.getValueAt(rowNumber,"FLR.Grade.school");
                thisTaz.heavyIndustry = table.getValueAt(rowNumber,"FLR.Heavy.Industry");
                thisTaz.hospital = table.getValueAt(rowNumber, "FLR.Hospital");
                thisTaz.institutional = table.getValueAt(rowNumber,"FLR.Institutional");
                thisTaz.lightIndustry = table.getValueAt(rowNumber,"FLR.Light.Industry");
                thisTaz.logging = table.getValueAt(rowNumber, "FLR.Logging");
                thisTaz.office = table.getValueAt(rowNumber, "FLR.Office");
                thisTaz.retail = table.getValueAt(rowNumber, "FLR.Retail");
                thisTaz.warehouse = table.getValueAt(rowNumber, "FLR.Warehouse");   
            }
            tazData.put(new Integer(thisTaz.zoneNumber), thisTaz);
        }
        setParkingCost(rb,"parkingCost.file");
        setAcres(rb,"alphatobeta.file");
    }
    
    public void setSchoolOccupation(int[] otherSchoolOccupation,int[] postSecSchoolOccupation){
        
        Enumeration tazEnum = tazData.elements();
        while (tazEnum.hasMoreElements()) {
            Taz thisTaz = (Taz) tazEnum.nextElement();
            thisTaz.otherSchoolOccupation = otherSchoolOccupation[thisTaz.zoneNumber];
            thisTaz.postSecondaryOccupation = postSecSchoolOccupation[thisTaz.zoneNumber];
        }
    }

    public void setParkingCost(ResourceBundle rb, String fileName) {
        TableDataSet table = loadTableDataSet(rb, fileName);
        int workColumn = table.getColumnPosition("WorkParkingCost");
        int nonWorkColumn = table.getColumnPosition("NonWorkParkingCost");
        int aZoneColumn = table.getColumnPosition("ZoneNumber");
        for(int i=1;i<=table.getRowCount();i++){
            if(tazData.containsKey(new Integer((int)table.getValueAt(i,aZoneColumn)))){
                Taz thisTaz = (Taz)tazData.get(new Integer((int)table.getValueAt(i,aZoneColumn)));
                thisTaz.workParkingCost = table.getValueAt(i,workColumn);
                thisTaz.nonWorkParkingCost = table.getValueAt(i,nonWorkColumn);
            }
        }
    }

    public void setAcres(ResourceBundle rb, String fileName){
        TableDataSet table = loadTableDataSet(rb, fileName);
        int acresColumn = table.getColumnPosition("GridAcres");
        int aZoneColumn = table.getColumnPosition("AZone");
        for(int i=1;i<=table.getRowCount();i++){
            if(tazData.containsKey(new Integer((int)table.getValueAt(i,aZoneColumn)))){
                Taz thisTaz = (Taz)tazData.get(new Integer((int)table.getValueAt(i,aZoneColumn)));
                thisTaz.acres = table.getValueAt(i,acresColumn);
            }
        }
    }
    
    public void setPopulation(int[] population){
        Enumeration tazEnum = tazData.elements();
        while (tazEnum.hasMoreElements()) {
            Taz thisTaz = (Taz) tazEnum.nextElement();
            thisTaz.households = population[thisTaz.zoneNumber];
            if(debug) logger.fine("population of zone "+thisTaz.zoneNumber+" = "+population[thisTaz.zoneNumber]);
        }
    }

    public void collapseEmployment(TourDestinationParametersData tdpd, StopDestinationParametersData sdpd) {
        //collapse employment categories for tazs
        logger.info("Collapsing employment");

        Enumeration tazEnum = tazData.elements();

        while (tazEnum.hasMoreElements()) {
            Taz thisTaz = (Taz) tazEnum.nextElement();
            thisTaz.collapseEmployment();
            thisTaz.setLnAcres();
            thisTaz.setTourSizeTerms(tdpd);
            thisTaz.setStopSizeTerms(sdpd);
        }
    }

    public int[] getExternalNumberArray() {
        //Set up external numbers         
        int tazCounter = 1;

        //create an array lookup to store all tazs
        int[] lookup = new int[tazData.size() + 1];

        //enumerate through all tazs and sort in lookup array 
        Enumeration tazEnum = tazData.elements();

        while (tazEnum.hasMoreElements()) {
            Taz thisTaz = (Taz) tazEnum.nextElement();
            lookup[tazCounter] = thisTaz.zoneNumber;
            tazCounter++;
        }

        //sort array        
        Arrays.sort(lookup);

        return lookup;
    }

    public boolean hasTaz(int zoneNumber) {
        return tazData.containsKey(new Integer(zoneNumber));
    }

    public static TableDataSet loadTableDataSet(ResourceBundle rb,
        String fileName) {
        try {
            String tazFile = ResourceUtil.getProperty(rb, fileName);
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(tazFile));
            logger.info("TazData: "+tazFile);

            return table;
        } catch (IOException e) {
            logger.severe("Can't find TazData input table " + fileName);
            e.printStackTrace();
        }

        return null;
    }
    public Object clone(){
        TazData newTazData;
        try {
            newTazData = (TazData) super.clone();
            newTazData.tazDataTableName = "TazData";
            newTazData.tazData = new Hashtable();
            Enumeration tazEnum = tazData.elements();
             while (tazEnum.hasMoreElements()) {
                 Taz thisTaz = (Taz) tazEnum.nextElement();
                 Taz tazCopy = (Taz) thisTaz.clone();
                 newTazData.tazData.put(new Integer(tazCopy.zoneNumber), tazCopy);
                 }

        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.toString());
        }



        return newTazData;
    }

    public void summarizeTAZs(){
        Enumeration tazEnum = tazData.elements();

        while (tazEnum.hasMoreElements()) {
            Taz thisTaz = (Taz) tazEnum.nextElement();
            thisTaz.summarizeTAZInfo();
        }
    }

    public static void main(String[] args) throws Exception {
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        TazData taz = new TazData();
        PTDataReader reader = new PTDataReader(rb);
        
        PTHousehold[] households = reader.readHouseholds("households.file");
        PTPerson[] persons = reader.readPersons("persons.file");
        taz.readData(rb, "tazData.file");
        logger.info("Finished reading TazData Table");
        
        for(int i=0;i<persons.length;i++){
               
        }
        //taz.setSchoolOccupation(persons);
        //taz.setPopulation(households);
        
        //read the tourDestinationParameters from csv to TableDataSet
        logger.info("Reading tour destination parameters");

        TourDestinationParametersData tdpd = new TourDestinationParametersData();
        tdpd.readData(rb, "tourDestinationParameters.file");

        //read the stopDestinationParameters from csv to TableDataSet
        logger.info("Reading stop destination parameters");

        StopDestinationParametersData sdpd = new StopDestinationParametersData();
        sdpd.readData(rb, "stopDestinationParameters.file");
        taz.collapseEmployment(tdpd, sdpd);

        TourDestinationParameters tdp = tdpd.getParameters(1, 3);
        Enumeration tazEnum = taz.tazData.elements();
        Taz testTaz = (Taz) tazEnum.nextElement();

        //Note - this takes 37 seconds with old utility calcs and 4 seconds with new utility calcs
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 100000000; i++) {
            testTaz.calcTourDestinationUtility(1, 3, tdp, 0.1,5.0);
        }

        logger.fine("Total time = " +
            ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
        System.exit(1);
    }
}

/** 
 * PTModel.java
 * Implements the person transport component of TLUMIP model
 * 
 * @author Joel Freedman
 * @version 1.0 October 2001
 * 
 */

package com.pb.despair.pt;

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.despair.model.ModelComponent;
import com.pb.despair.model.SkimsInMemory;

import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;


public class PTModelInputs extends ModelComponent implements Serializable{
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt");
    boolean debug = false;
    public static final boolean RUN_WEEKEND_MODEL = false;
    Random thisRandom;

    static String weekdayParameters;
    static String weekendParameters;
    static PatternChoiceParameters wkdayParams;
    static PatternChoiceParameters wkendParams;

    public static Patterns wkdayPatterns;
    public static Patterns wkendPatterns;

    public static TourModeParametersData tmpd;
    public static TourDestinationParametersData tdpd;

    public static StopDestinationParametersData sdpd;
    static TripModeParametersData smpd;
    static AllDestinationChoiceLogsums dcLogsums;

    static TableDataSet alphaToBeta;
    
    public static TazData tazs;    
    static SkimsInMemory skims;
    CSVFileReader reader = new CSVFileReader();
    ResourceBundle ptRb;
    ResourceBundle globalRb;

    private PTModelInputs(){
        //we don't want anyone to create a PTModelInputs without passing the parameters
        //listed in the other constructor
    }

    public PTModelInputs(ResourceBundle appRb, ResourceBundle globalRb){
        this.ptRb = appRb;
        this.globalRb = globalRb;
    }

    public void setSeed(int seed){
        
        thisRandom= new Random();
        thisRandom.setSeed(seed);
    }

    public void getPatterns(){
    //create instance of Patterns for weekdays
        logger.fine("Creating WeekdayPatterns Object");
        wkdayPatterns = new Patterns();
        wkdayPatterns.readData(ptRb,"weekdayPatterns.file");

        //create instance of Patterns for weekends
        logger.fine("Creating WeekdayPatterns Object");
        wkendPatterns = new Patterns();
        wkendPatterns.readData(ptRb,"weekendPatterns.file");
    }

    public void getParameters(){
 
        
        //read the taz data from csv to TableDataSet
        
        try{
            logger.fine("Adding AlphaToBetaTaz");
            String file = ResourceUtil.getProperty(globalRb, "alpha2beta.file");
        	alphaToBeta = reader.readFile(new File(file));
        }catch(IOException e) {
           	   logger.severe("Error reading alphazone to betazone file.");
           	   e.printStackTrace();
        }
                
        //create instance of PatternModelParameters for weekdays
        logger.fine("Creating Weekday PatternChoiceParameters Object");
        wkdayParams = new PatternChoiceParameters();
        wkdayParams.readData(ptRb,"weekdayParameters.file");


        //create instance of PatternModelParameters for weekends            
        logger.fine("Creating Weekend PatternChoiceParameters Object");
        wkendParams = new PatternChoiceParameters();
        wkendParams.readData(ptRb,"weekendParameters.file");
          


        //read the tourModeParameters from csv to TableDataSet
        logger.fine("Reading tour mode parameters");
        tmpd = new TourModeParametersData();
        tmpd.readData(ptRb,"tourModeParameters.file");
          
        //read the tourDestinationParameters from csv to TableDataSet
        logger.fine("Reading tour destination parameters");
        tdpd = new TourDestinationParametersData();
        tdpd.readData(ptRb,"tourDestinationParameters.file");
        //read the stopDestinationParameters from csv to TableDataSet
        logger.fine("Reading stop destination parameters");
        sdpd = new StopDestinationParametersData();
        sdpd.readData(ptRb,"stopDestinationParameters.file");

        //read the TripModeParameters from csv to TableDataSet
        logger.fine("Reading trip mode parameters");
        smpd = new TripModeParametersData();
        smpd.readData(ptRb,"tripModeParameters.file");
    }

    public static void readDCLogsums(ResourceBundle rb){
        dcLogsums= new AllDestinationChoiceLogsums();
//        dcLogsums.readDCLogsums(rb);        //BINARY-ZIP
        dcLogsums.readBinaryDCLogsums(rb);
    }
    
    public void readSkims(){
        
        //only read in if they haven't been read in already
        if(skims==null){

            //read skims into memory
            skims = new SkimsInMemory(globalRb); //global Rb sets some skim definitions such
                                                //as walk speed and peak times.
            skims.readSkims(ptRb);   //the pt.properties says where the skim files are located.
        }
        //logger.fine("Size of skims: "+ObjectUtil.sizeOf(skims));
    }
    public void setSkims(SkimsInMemory skimsInMemory){
           skims = skimsInMemory;
    }
    public void readTazData(){
        
        //only read tazdata if hasn't been read in already
        if(tazs==null){
            
            //read tazs into memory
            tazs = new TazData();
            tazs.readData(ptRb, globalRb, "tazData.file"); 
        }
    }
    public static SkimsInMemory getSkims(){
    	return skims;
    }
    public TableDataSet getTableDataSet(String resourceBundle, String pathName){
        
        ResourceBundle rb = ResourceUtil.getResourceBundle(resourceBundle);
        String path = ResourceUtil.getProperty(rb, pathName);
        
        try {
            String fullPath = path;
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(fullPath));
            return table;
        } catch (IOException e) {
            logger.severe("Can't find TazData input table " + "laborFlows");
            e.printStackTrace();
        }
        return null;
    }
    
    
    public PTHousehold[] getHouseholds(){
    
        //Read household and person data  
        PTDataReader dataReader = new PTDataReader(ptRb, globalRb);
        logger.info("Adding synthetic population from JDataStore"); 
        return dataReader.readHouseholds("households.file");

    }



    public void startModel(int timeInterval){};

}//end PTModel class



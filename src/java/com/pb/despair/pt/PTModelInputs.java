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
    ResourceBundle rb;

    public PTModelInputs(ResourceBundle rb){
        this.rb = rb;
    }

    public void setSeed(int seed){
        
        thisRandom= new Random();
        thisRandom.setSeed(seed);
    }

    public void getPatterns(){
    //create instance of Patterns for weekdays
        logger.fine("Creating WeekdayPatterns Object");
        wkdayPatterns = new Patterns();
        wkdayPatterns.readData(rb,"weekdayPatterns.file");

        //create instance of Patterns for weekends
        logger.fine("Creating WeekdayPatterns Object");
        wkendPatterns = new Patterns();
        wkendPatterns.readData(rb,"weekendPatterns.file");    
    }

    public void getParameters(){
 
        
        //read the taz data from csv to TableDataSet
        
        try{
            logger.fine("Adding AlphaToBetaTaz");
            String file = ResourceUtil.getProperty(rb, "alphatobeta.file");
        	alphaToBeta = reader.readFile(new File(file));
        }catch(IOException e) {
           	   logger.severe("Error reading alphazone to betazone file.");
           	   e.printStackTrace();
        }
                
        //create instance of PatternModelParameters for weekdays
        logger.fine("Creating Weekday PatternChoiceParameters Object");
        wkdayParams = new PatternChoiceParameters();
        wkdayParams.readData(rb,"weekdayParameters.file");


        //create instance of PatternModelParameters for weekends            
        logger.fine("Creating Weekend PatternChoiceParameters Object");
        wkendParams = new PatternChoiceParameters();
        wkendParams.readData(rb,"weekendParameters.file");
          


        //read the tourModeParameters from csv to TableDataSet
        logger.fine("Reading tour mode parameters");
        tmpd = new TourModeParametersData();
        tmpd.readData(rb,"tourModeParameters.file");
          
        //read the tourDestinationParameters from csv to TableDataSet
        logger.fine("Reading tour destination parameters");
        tdpd = new TourDestinationParametersData();
        tdpd.readData(rb,"tourDestinationParameters.file");
        //read the stopDestinationParameters from csv to TableDataSet
        logger.fine("Reading stop destination parameters");
        sdpd = new StopDestinationParametersData();
        sdpd.readData(rb,"stopDestinationParameters.file");

        //read the TripModeParameters from csv to TableDataSet
        logger.fine("Reading trip mode parameters");
        smpd = new TripModeParametersData();
        smpd.readData(rb,"tripModeParameters.file");
    }

    public static void readDCLogsums(ResourceBundle rb){
        dcLogsums= new AllDestinationChoiceLogsums();
        dcLogsums.readDCLogsums(rb);
    }
    
    public void readSkims(){
        
        //only read in if they haven't been read in already
        if(skims==null){

            //read skims into memory
            skims = new SkimsInMemory();
            skims.readSkims(rb);
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
            tazs.readData(rb,"tazData.file"); 
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
        PTDataReader dataReader = new PTDataReader(rb);
        logger.info("Adding synthetic population from JDataStore"); 
        return dataReader.readHouseholds("households.file");

    }



    public void startModel(int timeInterval){};

}//end PTModel class



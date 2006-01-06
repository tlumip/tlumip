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
/** 
 * PTModel.java
 * Implements the person transport component of TLUMIP model
 * 
 * @author Joel Freedman
 * @version 1.0 October 2001
 * 
 */

package com.pb.tlumip.pt;

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.tlumip.model.ModelComponent;
import com.pb.tlumip.model.SkimsInMemory;

import org.apache.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;


public class PTModelInputs extends ModelComponent implements Serializable{
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt");
    public static final boolean RUN_WEEKEND_MODEL = false;
    Random thisRandom;

    static String weekdayParameters;
    static String weekendParameters;
    static PatternChoiceParameters wkdayParams;
    static PatternChoiceParameters wkendParams;

    public static Patterns wkdayPatterns;
    public static Patterns wkendPatterns;

    public static DurationModelParametersData dmpd;

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
        if(logger.isDebugEnabled()) {
            logger.debug("Creating WeekdayPatterns Object");
        }
        wkdayPatterns = new Patterns();
        wkdayPatterns.readData(ptRb,"weekdayPatterns.file");

        //create instance of Patterns for weekends
        if(logger.isDebugEnabled()) {
            logger.debug("Creating WeekdayPatterns Object");
        }
        wkendPatterns = new Patterns();
        wkendPatterns.readData(ptRb,"weekendPatterns.file");
    }

    public void getParameters(){
 
        
        //read the taz data from csv to TableDataSet
        
        try{
            if(logger.isDebugEnabled()) {
                logger.debug("Adding AlphaToBetaTaz");
            }
            String file = ResourceUtil.getProperty(globalRb, "alpha2beta.file");
        	alphaToBeta = reader.readFile(new File(file));
        }catch(IOException e) {
           	   logger.fatal("Error reading alphazone to betazone file.");
           	   e.printStackTrace();
        }
                
        //create instance of PatternModelParameters for weekdays
        if(logger.isDebugEnabled()) {
            logger.debug("Creating Weekday PatternChoiceParameters Object");
        }
        wkdayParams = new PatternChoiceParameters();
        wkdayParams.readData(ptRb,"weekdayParameters.file");


        //create instance of PatternModelParameters for weekends
        if (PTModel.RUN_WEEKEND_MODEL) {
            if(logger.isDebugEnabled()) {
                logger.debug("Creating Weekend PatternChoiceParameters Object");
            }
            wkendParams = new PatternChoiceParameters();
            wkendParams.readData(ptRb,"weekendParameters.file");
        }
          
        //read the tourModeParameters from csv to TableDataSet
        if(logger.isDebugEnabled()) {
            logger.debug("Reading duration model parameters");
        }
        dmpd = new DurationModelParametersData();
        dmpd.readData(ptRb,"durationModelParameters.file");

        //read the tourModeParameters from csv to TableDataSet
        if(logger.isDebugEnabled()) {
            logger.debug("Reading tour mode parameters");
        }
        tmpd = new TourModeParametersData();
        tmpd.readData(ptRb,"tourModeParameters.file");
          
        //read the tourDestinationParameters from csv to TableDataSet
        if(logger.isDebugEnabled()) {
            logger.debug("Reading tour destination parameters");
        }
        tdpd = new TourDestinationParametersData();
        tdpd.readData(ptRb,"tourDestinationParameters.file");
        //read the stopDestinationParameters from csv to TableDataSet
        if(logger.isDebugEnabled()) {
            logger.debug("Reading stop destination parameters");
        }
        sdpd = new StopDestinationParametersData();
        sdpd.readData(ptRb,"stopDestinationParameters.file");

        //read the TripModeParameters from csv to TableDataSet
        if(logger.isDebugEnabled()) {
            logger.debug("Reading trip mode parameters");
        }
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
        //logger.debug("Size of skims: "+ObjectUtil.sizeOf(skims));
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
            logger.fatal("Can't find TazData input table " + "laborFlows");
            e.printStackTrace();
        }
        return null;
    }
    
    
    public PTHousehold[] getHouseholds(){
    
        //Read household and person data  
        
        // pass in the year of PUMS data from which the synthetic population was built
        PTDataReader dataReader = new PTDataReader(ptRb, globalRb, "1990");
        logger.info("Adding synthetic population from JDataStore"); 
        return dataReader.readHouseholds("households.file");

    }



    public void startModel(int timeInterval){};

}//end PTModel class



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
package com.pb.tlumip.pt;

import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ResourceBundle;
import java.util.Date;

/**
 * PTResults
 *
 * @author Freedman
 * @version Mar 3, 2004
 * 
 */
public class PTResults {
    
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.PTModel");
    PTTimer timer = new PTTimer();
    PTDataWriter ptWriter = new PTDataWriter();
    static PrintWriter debug;
    PrintWriter weekdayTour;
    PrintWriter weekdayPattern;
    PrintWriter weekdayTrip;
    PrintWriter weekendTour;
    PrintWriter weekendPattern;
    PrintWriter weekendTrip;
    PrintWriter householdData;

    static ResourceBundle rb;

    public PTResults(ResourceBundle rb){
        PTResults.rb = rb;
    }
    
    public static PrintWriter open(String textFileName){
        
        
        try {            
            //File fileName = new File(textFileName);
            //fileName.createNewFile();
            
            PrintWriter pwFile;
            pwFile = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(textFileName)));
            return pwFile;
        } catch (IOException e) {
            logger.fatal("Could not open file " + textFileName + " for writing\n");
            //TODO - log this exception to the node exception log file
            System.exit(1);
        }
        return null;
    
     }
    
    public void close(){
        logger.info("Closing tour, pattern and trip output files.");
        weekdayTour.close();
        weekdayPattern.close();
        weekdayTrip.close();
        householdData.close();
        if(PTModel.RUN_WEEKEND_MODEL){   
            weekendTour.close();
            weekendPattern.close();
            weekendTrip.close();
        }
    }
    
    public void createFiles(){
        weekdayTour = open(ResourceUtil.getProperty(rb, "weekdayTour.file"));
        weekdayTour.println(",,,,,,,Begin,,,,,,,IMStop1,,,,,,,PrimaryDestination,,,,,,,IMStop2,,,,,,,End");
        weekdayTour.println("hhID,personID,personAge,weekdayTour(yes/no),tourString,tour#,departDist," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "primaryMode");

        weekdayPattern = open(ResourceUtil.getProperty(rb, "weekdayPattern.file"));
        weekdayPattern.println("hhID,personID,personAge,weekdayTour(yes/no),patternLogsum,pattern,nHomeActivities,nWorkActivities,nSchoolActivities," +
                "nShopActivities,nRecreateActivities,nOtherActivities");

        weekdayTrip = open(ResourceUtil.getProperty(rb, "weekdayTrip.file"));
        weekdayTrip.println("hhID,personID,weekdayTour(yes/no),tour#,tourPurpose,tourSegment,tourMode,origin,destination,distance,time,tripStartTime,tripPurpose,tripMode");

        householdData = open(ResourceUtil.getProperty(rb, "householdData.file"));
        householdData.println("ID,size,autos,workers,income,singleFamily,multiFamily,homeTaz");        
        
        if(PTModel.RUN_WEEKEND_MODEL){
            weekendTour = open(ResourceUtil.getProperty(rb, "weekendTour.file"));
            weekendTour.println(",,,,,,,Begin,,,,,,,IMStop1,,,,,,,PrimaryDestination,,,,,,,IMStop2,,,,,,,End");
            weekendTour.println("hhID,personID,personAge,weekdayTour(yes/no),tourString,tour#,departDist," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "primaryMode");

            weekendPattern = open(ResourceUtil.getProperty(rb, "weekendPattern.file"));
            weekendPattern.println("hhID,personID,personAge,patternLogsum,pattern,nHomeActivities,nWorkActivities,nSchoolActivities," +
                "nShopActivities,nRecreateActivities,nOtherActivities");

            weekendTrip = open(ResourceUtil.getProperty(rb, "weekendTrip.file"));
            weekendTrip.println("hhID,personID,weekdayTour(yes/no),tour#,tourPurpose,tourSegment,tourMode,origin,destination,distance,time,tripStartTime,tripPurpose,tripMode");
        }
    }



    public void writeResults(PTHousehold[] households){
          
          logger.info("Writing patterns and tours to csv file");
          
          ptWriter.writeToursToTextFile(households, weekdayTour, true);   
          ptWriter.writeWeekdayPatternsToFile(households, weekdayPattern);
          ptWriter.writeWeekdayTripsToFile(households, weekdayTrip);
          
          for(int i=0;i<households.length;++i)
             households[i].printCSV(householdData);
          
          if(PTModel.RUN_WEEKEND_MODEL){     
              ptWriter.writeToursToTextFile(households, weekendTour, false);      
              ptWriter.writeWeekendPatternsToFile(households, weekendPattern);
              ptWriter.writeWeekendTripsToFile(households, weekendTrip);
          } 
          timer.endTimer();
                
  
     }

    public static PrintWriter createTourDebugFile(String fileName){
        String pathToDebugDir = ResourceUtil.getProperty(rb,"debugFiles.path");
        debug = open(pathToDebugDir + fileName);
        logger.info("Writing to " + pathToDebugDir + fileName);
        debug.println(",,,Begin,,,,,,,IMStop1,,,,,,,PrimaryDestination,,,,,,,IMStop2,,,,,,,End");
        debug.println("tourString,tour#,departDist," +
                    "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                    "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                    "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                    "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                    "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                    "primaryMode");
        debug.flush();
        return debug;
    }

    public static PrintWriter createTazDebugFile(String fileName){
        String pathToDebugDir = ResourceUtil.getProperty(rb,"debugFiles.path");
        //check to see if the file has already been written.  If so, we don't need to write it again.
        if(new File(pathToDebugDir + fileName).exists()) debug=null;
        //if file doesn't exists than create it and return the writer
        else {
            logger.info("Writing to " + pathToDebugDir + fileName);
            debug = open(pathToDebugDir + fileName);
            debug.println(",,,,,,,,,TourSizeTerms,,,,,,,,,,,,TourLnSizeTerms,,,,,,,,,,,,StopSizeTerms,,,,,,,StopLnSizeTerms");
            debug.println("zoneNumber,households,workParkingCost," +
                    "nonWorkParkingCost,acres,pricePerAcre,pricePerSqFtSFD,singleFamilyHH,multiFamilyHH," +
                    "h,w1,w2,w3,w4,b,c1,c2,c3,s,r,o"+
                    "h,w1,w2,w3,w4,b,c1,c2,c3,s,r,o"+
                    "h,w,b,c,s,r,o"+
                    "h,w,b,c,s,r,o");
            debug.flush();
        }

        return debug;
    }

    public static PrintWriter createTripModeDebugFile(String fileName){
        String pathToDebugDir = ResourceUtil.getProperty(rb,"debugFiles.path");
        debug = open(pathToDebugDir + fileName);
        logger.info("Writing to " + pathToDebugDir + fileName);
        debug.println("Trip Mode Choice Model Debug File");
        debug.println("Written: " + new Date());
        debug.println("No trip mode could be chosen.  Here is the summary followed by the details");
        debug.flush();
        return debug;
    }

    public static MatrixWriter createMatrixWriter(String fileName){
        String pathToDebugDir = ResourceUtil.getProperty(rb,"debugFiles.path");
        MatrixWriter mWriter = MatrixWriter.createWriter(MatrixType.BINARY, new File(pathToDebugDir + fileName));
        return mWriter;
    }


    public static void main(String[] args){
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
        PTResults results = new PTResults(rb);
        PTHousehold[] households; 
        
        // Read household and person data  
        PTDataReader dataReader = new PTDataReader(rb, globalRb);
        logger.info("Adding synthetic population from database"); 
        households = dataReader.readHouseholds("households.file");
        results.writeResults(households);
        
    }

}

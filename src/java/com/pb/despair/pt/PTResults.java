package com.pb.despair.pt;

import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.pb.common.util.ResourceUtil;

/**
 * PTResults
 *
 * @author Freedman
 * @version Mar 3, 2004
 * 
 */
public class PTResults {
    
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.PTModel");
    PTTimer timer = new PTTimer();
    PTDataWriter ptWriter = new PTDataWriter();
    PrintWriter weekdayTour;
    PrintWriter weekdayPattern;
    PrintWriter weekdayTrip;
    PrintWriter weekendTour;
    PrintWriter weekendPattern;
    PrintWriter weekendTrip;
    PrintWriter householdData;

    ResourceBundle rb;

    public PTResults(ResourceBundle rb){
        this.rb = rb;
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
            logger.severe("Could not open file " + textFileName + " for writing\n");           
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
    
    public void printHeader(PrintWriter pw, String header){
        
    }
    
    public void createFiles(){
        weekdayTour = open(ResourceUtil.getProperty(rb, "weekdayTour.file"));
        weekdayTour.println(",,,,,,Begin,,,,,,,IMStop1,,,,,,,PrimaryDestination,,,,,,,IMStop2,,,,,,,End");
        weekdayTour.println("hhID,personID,weekdayTour(yes/no),tourString,tour#,departDist," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "primaryMode");

        weekdayPattern = open(ResourceUtil.getProperty(rb, "weekdayPattern.file"));
        weekdayPattern.println("hhID,personID,weekdayTour(yes/no),patternLogsum,pattern,nHomeActivities,nWorkActivities,nSchoolActivities," +
                "nShopActivities,nRecreateActivities,nOtherActivities");

        weekdayTrip = open(ResourceUtil.getProperty(rb, "weekdayTrip.file"));
        weekdayTrip.println("hhID,personID,weekdayTour(yes/no),tour#,tourPurpose,tourMode,origin,destination,distance,time,tripStartTime,tripPurpose,tripMode");

        householdData = open(ResourceUtil.getProperty(rb, "householdData.file"));
        householdData.println("ID,size,autos,workers,income,singleFamily,multiFamily,homeTaz");        
        
        if(PTModel.RUN_WEEKEND_MODEL){
            weekendTour = open(ResourceUtil.getProperty(rb, "weekendTour.file"));
            weekendTour.println(",,,,,,Begin,,,,,,,IMStop1,,,,,,,PrimaryDestination,,,,,,,IMStop2,,,,,,,End");
            weekendTour.println("hhID,personID,weekdayTour(yes/no),tourString,tour#,departDist" +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                "primaryMode");

            weekendPattern = open(ResourceUtil.getProperty(rb, "weekendPattern.file"));
            weekendPattern.println("hhID,personID,patternLogsum,pattern,nHomeActivities,nWorkActivities,nSchoolActivities," +
                "nShopActivities,nRecreateActivities,nOtherActivities");

            weekendTrip = open(ResourceUtil.getProperty(rb, "weekendTrip.file"));
            weekendTrip.println("hhID,personID,tour#,tourPurpose,tripPurpose,origin,tripStartTime,distance,destination,tourMode,tripMode");
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
                
  
     }//end constructor    
     
    public static void main(String[] args){
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        PTResults results = new PTResults(rb);
        PTHousehold[] households; 
        
        // Read household and person data  
        PTDataReader dataReader = new PTDataReader(rb);
        logger.info("Adding synthetic population from database"); 
        households = dataReader.readHouseholds("households.file");
        results.writeResults(households);
        
    }

}

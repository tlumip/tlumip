package com.pb.despair.pt;

import com.pb.despair.model.Mode;
import com.pb.despair.model.UnitOfLand;
import org.apache.log4j.Logger;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;

/** A class that represents an activity, part of a tour
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 */

public class Activity implements Serializable{
    final static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
    // Attributes  (they are ints because they are multiplied by params in the Duration Model!)
    public Location location = new Location();

    public int activityNumber;
    public short activityPurpose;
    public short activityType;
    
    //public int startAM;
    //public int startMD;
    //public int startPM;
    //public int startEV;

    //military time
    public short duration;
    public short startTime;
    public short endTime;
    public short timeToActivity;
    //trip to activity
    public Mode tripMode;
    public float distanceToActivity;
    // Associations

    /**
     */
    public Activity() {
    }

    public void setNominalLocation() {
    }

    /**
    *
    * 
    */
    protected Vector myPersonTimeSlot;
    /**
    
    * 
    */
    protected UnitOfLand nominalLocation;

    // Operations
    public void print() {

        logger.info("Activity Number " + activityNumber);


        if (activityType==ActivityType.PRIMARY_DESTINATION)
            logger.info(", Primary Destination");
        else if (activityType==ActivityType.INTERMEDIATE_STOP)
            logger.info(", Intermediate Stop");
        else if (activityType==ActivityType.BEGIN)
            logger.info(", Tour Begin");
        else if (activityType==ActivityType.END)
            logger.info(", Tour End");

        if (activityPurpose==ActivityPurpose.HOME)
            logger.info("Activity Purpose:  Home");
        if (activityPurpose==ActivityPurpose.WORK||activityPurpose==ActivityPurpose.WORK_BASED)
            logger.info("Activity Purpose:  Work");
        if (activityPurpose==ActivityPurpose.SCHOOL)
            logger.info("Activity Purpose:  School");
        if (activityPurpose==ActivityPurpose.SHOP)
            logger.info("Activity Purpose:  Shop");
        if (activityPurpose==ActivityPurpose.RECREATE)
            logger.info("Activity Purpose:  Recreate");
        if (activityPurpose==ActivityPurpose.OTHER)
            logger.info("Activity Purpose:  Other");
        if (activityPurpose==ActivityPurpose.WORK_BASED)
            logger.info("Activity Purpose:  WorkBased");

//        if (startAM == 1)
//            logger.info("Activity Start Period:  AM");
//        if (startMD == 1)
//            logger.info("Activity Start Period:  MD");
//        if (startPM == 1)
//            logger.info("Activity Start Period:  PM");
//        if (startEV == 1)
//            logger.info("Activity Start Period:  EV");

        logger.info("Activity Duration:  " + duration);
        logger.info("       Start Time:  " + startTime);
        logger.info("         End Time:  " + endTime);

        logger.info("Location:");
        location.print();

        if (tripMode != null)
            logger.info("Mode:  " + tripMode.alternativeName);

    }

    //returns minutes left in day based on day ending 2:59 am, using ending time of activity
    public float minutesLeftInDay() {

        int endHour = endTime / 100;
        int endMinute = endTime - endHour * 100;

        int endHoursRemaining = 27 - endHour;

        int endMinutesRemaining = 60 - endMinute;

        if (endMinutesRemaining > 0)
            --endHoursRemaining;

        return new Integer((endHoursRemaining * 60) + endMinutesRemaining)
            .floatValue();
    }

    //calculates end time of activity
    void calculateEndTime() {

        short hours = (short)(duration / 60);
        short minutes = (short)(duration - (hours * 60));

        short startHours = (short)(startTime / 100);
        short startMinutes = (short)(startTime - (startHours * 100));

        if ((startMinutes + minutes) >= 60) {
            ++hours;
            minutes = (short)((startMinutes + minutes) - 60);
            startMinutes = 0;
        }
        endTime = (short)((startHours * 100) + (hours * 100) + startMinutes + minutes);
    }

    //calculates startTime of current activity based on time of last activity and time to get to this activity
    void calculateStartTime(Activity lastActivity) {
        //time to activity is broken down into hours and minutes
         int hours = timeToActivity / 60;
         int minutes = timeToActivity - (hours * 60);

         //prior to adding on the timeToActivity to the end time of the first activity, break
         //down the end time into hours and minutes
         int endHours = lastActivity.endTime/100;
         int endMinutes = lastActivity.endTime - (endHours*100);

         if(endMinutes + minutes >= 60){ //we need to increment the hours
             hours++;
             minutes = (endMinutes + minutes)-60;  //and fix the minutes
             endMinutes=0;
         }
         //return the correct time in military format.
         startTime=(short)((endHours*100) + (hours*100)+ endMinutes + minutes);
    }

    //to write to a text file, csv format
    void printCSV(PrintWriter file) {

 //       int activityType = 0;

  //      if (activityPurpose==ActivityPurpose.HOME)
 //           activityType = 1;
 //       if (activityPurpose==ActivityPurpose.WORK||activityPurpose==ActivityPurpose.WORK_BASED)
 //           activityType = 2;
//        if (activityPurpose==ActivityPurpose.SCHOOL)
//            activityType = 3;
//        if (activityPurpose==ActivityPurpose.SHOP)
//            activityType = 4;
 //       if (activityPurpose==ActivityPurpose.RECREATE)
 //           activityType = 5;
 //       if (activityPurpose==ActivityPurpose.OTHER)
 //           activityType = 6;
 //       if (activityPurpose==ActivityPurpose.WORK_BASED)
 //           activityType = 7;

        //int startPeriod = 0;

//        if (startAM == 1)
//            startPeriod = 1;
//        if (startMD == 1)
//            startPeriod = 2;
//        if (startPM == 1)
//            startPeriod = 3;
//        if (startEV == 1)
//            startPeriod = 4;

        int tripModeType = 0;
        if (tripMode != null)
            tripModeType = tripMode.type;
            
            
        file.print(
            ActivityPurpose.getActivityPurposeChar(activityPurpose)
                + ","
                + startTime
                + ","
                + endTime
                + ","
                + timeToActivity
                + ","
                + distanceToActivity
                + ","
                + tripModeType
                + ","
                + location.zoneNumber
                + ",");

    }
    //Returns a zero or one depending on  
    public int startTimePeriodCheck(int startTime, String timePeriod){
        int timePeriodCheck=0;
        if(startTime>=300 && startTime<1000 && timePeriod=="AM")
           timePeriodCheck=1;
        else if(startTime>=1000 && startTime<1530 && timePeriod=="MD")
           timePeriodCheck=1;
        else if(startTime>=1530 && startTime<1830 && timePeriod=="PM")
           timePeriodCheck=1;
        else if(startTime>=1830 && timePeriod=="EV")
           timePeriodCheck=1;
        return timePeriodCheck;
    }
    public String startTimePeriod(int startTime){
        String timePeriod;
        if(startTime>=300 && startTime<1000)timePeriod="AM";
        else if(startTime>=1000 && startTime<1530)timePeriod="MD";
        else if(startTime>=1530 && startTime<1830)timePeriod="PM";
        else if(startTime>=1830) timePeriod="EV";
        else {
            logger.warn("??? startTime < 300!!!");
            timePeriod="< 300";
        } 
        return timePeriod;
    }
    
    public int checkActivityType(short activityType, short testActivityType){
            int activityTypeReturn = 0;
            if (activityType==testActivityType) activityTypeReturn=1;
            return activityTypeReturn;
        }
        
    public int checkActivityPurpose(short activityPurpose, short testActivityPurpose){
            int activityPurposeReturn = 0;
            if (testActivityPurpose==ActivityPurpose.WORK && 
                (activityPurpose==ActivityPurpose.WORK     ||
                 activityPurpose==ActivityPurpose.WORK_BASED))
                     {activityPurposeReturn=1;
            } 
            else if (activityPurpose==testActivityPurpose)
                activityPurposeReturn=1;
                               
            return activityPurposeReturn;            
    }
} /* end class Activity */

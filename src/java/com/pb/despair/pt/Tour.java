
package com.pb.despair.pt;
import com.pb.despair.model.Mode;
import com.pb.despair.model.ModeType;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.logging.Logger;
import java.util.Date;

/** 
 * A class containing tour attributes 
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class Tour implements Serializable{
  
  // Attributes
      protected static Logger logger = Logger.getLogger("com.pb.despair.Tour");
      public Activity begin;
      public Activity primaryDestination;
      public Activity intermediateStop1;
      public Activity intermediateStop2;
      public Activity end;
      public String tourString;
      public Mode primaryMode;
      public float departDist;
      public float returnDist;
           
     //public Mode primaryMode;
     boolean hasPrimaryMode;
     public int tourNumber; //1->total tours
     
     //for work-based tours, based on tour mode to work
     public boolean driveToWork;
     public int tourDuration;
     
     //use militaryTime

/*
     public int timeLeavingOrigin;              //time leaving home or work
     public int timeArrivingDestination;          //timeLeavingOrigin + travel time
     public int timeLeavingDestination;          
     public int timeArrivingOrigin;               //timeLeavingDestination + travel time
*/     
     
     //Travel times (unweighted minutes)
     //public int timeToBegin;
     //public int timeToPrimaryDestination;
     //public int timeToIntermediateStop1;
     //public int timeToIntermediateStop2;
     //public int timeToEnd;
     
     //String tourString;
     
     
     //constructor to create empty Tour, to be used with workBasedTour
     public Tour(){

          //initialize the origin and primary destination activities
          begin=new Activity();
          //begin.isBegin=1;
          end=new Activity();
          //end.isEnd=1;
          primaryDestination=new Activity();
          //primaryDestination.isPrimaryDestination=1;
          
     
     };
     
     //contructor takes a pattern and an integer identifying the tourNumber and fills in attributes accordingly
     public Tour(String thisTourString){
          
          tourString=thisTourString;
          
          if(thisTourString.length()<3){
               logger.severe("Error: Less than 3 activities on tour");
               System.exit(1);
          }
          //initialize the origin and primary destination activities
          begin=new Activity();
          begin.activityType = ActivityType.BEGIN;
          begin.activityNumber=1;
          begin.activityPurpose=ActivityPurpose.getActivityPurposeValue(thisTourString.charAt(0));

          end=new Activity();
          end.activityType = ActivityType.END;
          end.activityNumber=thisTourString.length();
          end.activityPurpose=ActivityPurpose.getActivityPurposeValue(thisTourString.charAt(thisTourString.length()-1));


          primaryDestination=new Activity();
          primaryDestination.activityType = ActivityType.PRIMARY_DESTINATION;
         //The activity number and activity purpose will be determined below.

          //code activity numbers
          if(thisTourString.length()==5){     //there are two intermediate stops
               
               intermediateStop1 = new Activity();
               intermediateStop1.activityType=ActivityType.INTERMEDIATE_STOP;
               intermediateStop1.activityNumber=2;
               intermediateStop1.activityPurpose = ActivityPurpose.getActivityPurposeValue(thisTourString.charAt(1));

               primaryDestination.activityNumber=3;
               primaryDestination.activityPurpose=ActivityPurpose.getActivityPurposeValue(thisTourString.charAt(2));

               intermediateStop2 = new Activity();
               intermediateStop2.activityType=ActivityType.INTERMEDIATE_STOP;
               intermediateStop2.activityNumber=4;
               intermediateStop2.activityPurpose = ActivityPurpose.getActivityPurposeValue(thisTourString.charAt(3));
          }
          if(thisTourString.length()==4){     //there is one intermediate stop;
                                              // currently the default is that the stop will come after the primary
                                              //destination if the 2 stops are the same purpose.

               if(hasIntermediateStop1(thisTourString)==1){  //stop comes before primary destination
                    intermediateStop1=new Activity();
                    intermediateStop1.activityType=ActivityType.INTERMEDIATE_STOP;
                    intermediateStop1.activityNumber=2;
                    intermediateStop1.activityPurpose = ActivityPurpose.getActivityPurposeValue(thisTourString.charAt(1));

                    primaryDestination.activityNumber=3;
                    primaryDestination.activityPurpose = ActivityPurpose.getActivityPurposeValue(thisTourString.charAt(2));

               } else{ //stop comes after the primary destination
                   primaryDestination.activityNumber=2;
                   primaryDestination.activityPurpose = ActivityPurpose.getActivityPurposeValue(thisTourString.charAt(1));

                   intermediateStop2=new Activity();
                   intermediateStop2.activityType=ActivityType.INTERMEDIATE_STOP;
                   intermediateStop2.activityNumber=3;
                   intermediateStop2.activityPurpose = ActivityPurpose.getActivityPurposeValue(thisTourString.charAt(2));


               }
          } //end if one stop on tour
          if(thisTourString.length()==3){
              primaryDestination.activityNumber=2;
              primaryDestination.activityPurpose=ActivityPurpose.getActivityPurposeValue(thisTourString.charAt(1));
          }
     }//end Tour creation.
     
    //This method prints the Tour attributes to the logger.
     //Need to change this method to use times within activity rather than obsolete Tour variables
     public void print(Tour thisTour){
         logger.info("");
         logger.info("TOUR INFO: ");
         logger.info("");
          logger.info("Begin Activity");
          logger.info("****************************");
          begin.print();
          logger.info("Time To Begin Activity: "+thisTour.begin.startTime);
          if(intermediateStop1!=null){
              logger.info("");
               logger.info("Intermediate Stop 1 Activity");
               logger.info("****************************");
               intermediateStop1.print();
               logger.info("Time To Intermediate Stop 1 Activity: "+thisTour.intermediateStop1.timeToActivity);
          }
         logger.info("");
          logger.info("Primary Destination Activity");
          logger.info("****************************");
          primaryDestination.print();
          logger.info("Time To Primary Destination Activity: "+thisTour.primaryDestination.timeToActivity);
          if(intermediateStop2!=null){
              logger.info("");
               logger.info("Intermediate Stop 2 Activity");
               logger.info("****************************");
               intermediateStop2.print();
               logger.info("Time To Intermediate Stop 2 Activity: "+thisTour.intermediateStop2.timeToActivity);
          }
         logger.info("");
          logger.info("End Activity");
          logger.info("****************************");
          end.print();
          logger.info("Time To End Activity: "+thisTour.end.timeToActivity);
      
          if(hasPrimaryMode)     primaryMode.print();
     
     
     //Travel times (unweighted minutes)
//     public int timeToPrimaryDestination;
//     public int timeToBegin;
//     public int timeToEnd;
//     public int timeToIntermediateStop1;
//     public int timeToIntermediateStop2;


          
     }   
            
     //This method returns 1 if an intermediateStop occurs before the primary destination
     public int hasIntermediateStop1(String tourString){
         int intermediateStop1=0;
         if ((tourString.length()==5)||((tourString.length()==4) &&         
               (ActivityPurpose.getActivityPurposeValue(tourString.charAt(1))>
               ActivityPurpose.getActivityPurposeValue(tourString.charAt(2))))
         )
            intermediateStop1=1;
         return intermediateStop1;
     }

     //This method looks at a tour string and determines whether there is an 
     //intermediate stop before the primary destination
     public int hasIntermediateStop2(String tourString){
         int intermediateStop2=0;
         if ((tourString.length()==5)||(tourString.length()==4 &&         
               ActivityPurpose.getActivityPurposeValue(tourString.charAt(2))>
               ActivityPurpose.getActivityPurposeValue(tourString.charAt(1)))
         )
            intermediateStop2=1;
         return intermediateStop2;
     }
     
     public int iStopsCheck(int stops, String tourString){
         int iStopsCheckReturn=0;
         if (stops ==tourString.length()-3)
            iStopsCheckReturn=1;
         return iStopsCheckReturn;    
     }
     //this method sets up the workBased tour based on the attributes of the work tour
     public void setWorkBasedTourAttributes(Tour workTour){

          begin.activityType=ActivityType.BEGIN;
          begin.activityPurpose=ActivityPurpose.WORK;
          begin.activityNumber=1;
          begin.location= workTour.primaryDestination.location;
          begin.startTime = workTour.primaryDestination.startTime;
          begin.duration = workTour.primaryDestination.duration;

          primaryDestination.activityType=ActivityType.PRIMARY_DESTINATION;
          primaryDestination.activityPurpose=ActivityPurpose.OTHER;
          primaryDestination.activityNumber=2;

          end.activityType=ActivityType.END;
          end.activityPurpose=ActivityPurpose.WORK;
          end.activityNumber=3;
          end.location=workTour.primaryDestination.location;
          end.endTime = workTour.primaryDestination.endTime;
          tourString="b";
          
          if(workTour.primaryMode.type==ModeType.AUTODRIVER)
               driveToWork=true;
          else
               driveToWork=false;
          
          tourDuration=workTour.primaryDestination.duration;
     };               
     
     
//   to write tour characteristics to text file, csv
     /*void setDistance(PrintWriter file, Tour thisTour, SkimsInMemory skims){
          float distanceToIStop1;
          float distanceToPrimaryDestination;
          float distanceToIStop2;
          float distanceToEnd;
          
          if (hasIntermediateStop1(thisTour.tourString)==1){
              distanceToIStop1=skims.pkDist.getValueAt(thisTour.begin.location.zoneNumber,
                                                       thisTour.intermediateStop1.location.zoneNumber);
              distanceToPrimaryDestination=skims.pkDist.getValueAt(thisTour.intermediateStop1.location.zoneNumber,
                                                       thisTour.primaryDestination.location.zoneNumber);                               
          }
          else {
              distanceToIStop1=0;          
              distanceToPrimaryDestination=skims.pkDist.getValueAt(thisTour.begin.location.zoneNumber,
                                                                   thisTour.primaryDestination.location.zoneNumber); 
          }
                                                                              
          if (hasIntermediateStop2(thisTour.tourString)==1){
             distanceToIStop2=skims.pkDist.getValueAt(thisTour.primaryDestination.location.zoneNumber,
                                                      thisTour.intermediateStop2.location.zoneNumber);
             distanceToEnd=skims.pkDist.getValueAt(thisTour.intermediateStop2.location.zoneNumber,
                                                      thisTour.end.location.zoneNumber);                               
          }          
          else {
             distanceToIStop2=0;
             distanceToEnd=skims.pkDist.getValueAt(thisTour.primaryDestination.location.zoneNumber,
                                                                  thisTour.end.location.zoneNumber);                                                                   
          }
          file.print(thisTour.tourString+",");
          file.print(thisTour.tourNumber+",");
          begin.printCSV(file);
          if(hasIntermediateStop1(thisTour.tourString)==1)
               intermediateStop1.printCSV(file);
          else
               file.print("0,0,0,0,0,0,0,0,0,0,");
          primaryDestination.printCSV(file);
          if(hasIntermediateStop2(thisTour.tourString)==1)
               intermediateStop2.printCSV(file);
          else
               file.print("0,0,0,0,0,0,0,0,0,0,");
          end.printCSV(file);
          
          file.print(primaryMode.type);
          
     }*/
     
     
    /*  Write tour characteristics to text file, in csv format
     *  The following  fields will be written:
     *  tourString
     *  tourNumber
     *  begin.activityPurpose
     *  begin.startTime
     *  begin.endTime
     *  begin.timeToActivity
     *  begin.distanceToActivity
     *  begin.tripModeType
     *  begin.location.zoneNumber
     *  stop1.activityPurpose
     *  stop1.startTime
     *  stop1.endTime
     *  stop1.timeToActivity
     *  stop1.distanceToActivity
     *  stop1.tripModeType
     *  stop1.location.zoneNumber
     *  pdest.activityPurpose
     *  pdest.startTime
     *  pdest.endTime
     *  pdest.timeToActivity
     *  pdest.distanceToActivity
     *  pdest.tripModeType
     *  pdest.location.zoneNumber
     *  stop2.activityPurpose       
     *  stop2.startTime             
     *  stop2.endTime               
     *  stop2.timeToActivity        
     *  stop2.distanceToActivity    
     *  stop2.tripModeType          
     *  stop2.location.zoneNumber  
     *  end.activityPurpose        
     *  end.startTime             
     *  end.endTime               
     *  end.timeToActivity        
     *  end.distanceToActivity    
     *  end.tripModeType          
     *  end.location.zoneNumber   
     *  tourMode
     */
     void printCSV(PrintWriter file){
//          file.print(new Date()+",");
          file.print(tourString+",");
          file.print(tourNumber+",");
          begin.printCSV(file);
          if(intermediateStop1!=null)
               intermediateStop1.printCSV(file);
          else
               file.print("0,0,0,0,0,0,0,");
          primaryDestination.printCSV(file);
          if(intermediateStop2!=null)
               intermediateStop2.printCSV(file);
          else
               file.print("0,0,0,0,0,0,0,");
          end.printCSV(file);

        if (primaryMode != null) {
            file.print(primaryMode.type);
        }

        file.flush();

     }
     

} /* end class Tour */


package com.pb.despair.pt;

import com.pb.common.model.LogitModel;
import com.pb.common.matrix.Matrix;
import com.pb.despair.model.Mode;
import com.pb.despair.model.SkimsInMemory;
import com.pb.despair.model.TravelTimeAndCost;
import com.pb.common.util.SeededRandom;
import java.util.logging.Logger;
import java.util.Enumeration;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

/** 
 * This model implements a logit model to choose a tour destination
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
*/

 
public class TourDestinationModeChoiceModel{
    
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.TourDestinationModeChoiceModel"); 
     LogitModel destinationModel;
     double logsum;
     Taz chosenTaz;
     Mode chosenMode;
//     TazData allTazs;
    public static long startTime;
    public static long endTime;
    public static long elapsedTime;
    PersonTourModeAttributes personAttributes = new PersonTourModeAttributes();
    boolean writtenOutTheUtilitiesAlready = false;

     final boolean debug=false;
     final int debugID = -1;
     
     public void buildModel(TazData tazs){ //passsing in PTModel.tazs and adding to logit model as alternatives
//         allTazs=(TazData)tazs.clone();
         destinationModel = new LogitModel("destinationModel",tazs.tazData.size());
         int destination=0;
         Enumeration destinationEnum=tazs.tazData.elements();
         while(destinationEnum.hasMoreElements()){
             Taz destinationTaz = (Taz) destinationEnum.nextElement();
             destinationModel.addAlternative(destinationTaz);
         }           
     }

    public static void startTimer() {
        startTime = System.currentTimeMillis();
    }    

    public static void endTimer() {
        endTime = System.currentTimeMillis();
    }    

    public static long elapsedTime() {
        return (endTime-startTime);
    }    

     //passing in PTModel.tazs and using the method copy to calculate utilities
     public void calculateDestinationZone(PTHousehold thisHousehold, PTPerson thisPerson, Tour thisTour,
                                          SkimsInMemory skims, TourModeParametersData tmpd,
                                          TourDestinationParametersData tdpd, TazData tazs,
                                          DCExpUtilitiesManager um, TourModeChoiceModel tmcm){
                                              
          //long totalCalculateDestinationZoneTime = System.currentTimeMillis();

          Matrix expUtilities;

           //set up destination and mode choice parameters and logsums
          short activityPurpose = thisTour.primaryDestination.activityPurpose;
          TourModeParameters modeParams = tmpd.getTourModeParameters(activityPurpose);
          //set mode choice person attributes
          personAttributes.setAttributes(thisHousehold,thisPerson,thisTour);
          int purposeSegment=0;

          if (thisTour.primaryDestination.activityPurpose!=ActivityPurpose.WORK &&
                thisTour.primaryDestination.activityPurpose!=ActivityPurpose.WORK_BASED) {  //we need to choose a location TAZ
          	 int segment = thisPerson.getDCSegment(activityPurpose);
             TourDestinationParameters destParams = tdpd.getParameters(activityPurpose,segment);
          
             
             if(activityPurpose==ActivityPurpose.SCHOOL && thisPerson.age<18)
                purposeSegment=1;
                
                if(activityPurpose==ActivityPurpose.SCHOOL && thisPerson.age>=18)
                   purposeSegment=3;


             expUtilities = um.getMatrix(activityPurpose,purposeSegment);
             chosenTaz=null;

             //for destination choice model
              int destination=0;
              if(!writtenOutTheUtilitiesAlready && debug){
                  logger.info("Calculating Destination Zone for the following tour...");
                  logger.info("HHID " + thisHousehold.ID + ", Person " + thisPerson.ID + ", Tour " + thisTour.tourNumber
                          + ", ActivityPurpose " + ActivityPurpose.getActivityPurposeChar(activityPurpose)
                          + ", Origin " + thisTour.begin.location.zoneNumber);
                  thisHousehold.print();
                  thisPerson.print();
                  thisTour.print(thisTour);
                  writtenOutTheUtilitiesAlready = true;
              }
              
              //cycle through zones and compute total exponentiated utility
              float totalExpUtility=0;
              Enumeration tazEnum = tazs.tazData.elements();
              for(int i=0; i<tazs.tazData.size();i++){

              	  Taz destinationTaz = (Taz) tazEnum.nextElement();
                  
                  float expUtility=expUtilities.getValueAt(thisTour.begin.location.zoneNumber,destinationTaz.zoneNumber);
                  
                  totalExpUtility += expUtility;
                    
               } //end destinations

               //pick random number and choose a taz
               double rNumber=SeededRandom.getRandom();
               double culmProbability=0;
               tazEnum = tazs.tazData.elements();
               for(int i=0; i<tazs.tazData.size();i++){

                   Taz destinationTaz = (Taz) tazEnum.nextElement();
                  
                   float expUtility=expUtilities.getValueAt(thisTour.begin.location.zoneNumber,destinationTaz.zoneNumber);
                   culmProbability += (double)(expUtility)/totalExpUtility;
                  
                  if(culmProbability>rNumber){
                    chosenTaz=destinationTaz;
                    break;
                  }  
               } //end destinations
               if(chosenTaz==null){
                   logger.severe("Error in tour destination choice: no zones available for this household, tour");
                   logger.severe("Household "+thisHousehold.ID+" Person "+thisPerson.ID+" Tour "+thisTour.tourNumber);
                   try {
                	//TODO get debug filename from pt.properties
                       logger.severe("Writing Tour Debug info to the /models/tlumip/debug directory");
                       thisTour.printCSV(new PrintWriter(new FileWriter("/models/tlumip/debug/HH" + thisHousehold.ID + "Tour" + thisTour.tourNumber+".csv")));
                   } catch (IOException e1) {
                       e1.printStackTrace();  
                   }
                   tazEnum = tazs.tazData.elements();
                   expUtilities = um.getMatrix(activityPurpose,purposeSegment);
                   for(int i=0;i<tazs.tazData.size();++i){
                       Taz taz = (Taz)tazEnum.nextElement();
                       logger.severe("**** Attributes of destination "+taz.zoneNumber);
                       taz.print();
                       logger.severe("exputility "+expUtilities.getValueAt(thisTour.begin.location.zoneNumber,taz.zoneNumber));
                   }
                   logger.severe("TASK IS EXITING - FATAL ERROR");
                   System.exit(1);  //no sense in going on as we have a fundamental problem.
               }

              thisTour.primaryDestination.location.zoneNumber = chosenTaz.zoneNumber;

          }else {//the primary destination is work (or work-based) and therefore the chosenTaz is the workplace location
                chosenTaz = (Taz)tazs.tazData.get(new Integer(thisTour.primaryDestination.location.zoneNumber));
                if(chosenTaz == null){
                    logger.severe("Work (or Work-based) tour has a primary destination of 0 - not right!!");
                    logger.severe("HHID: " + thisHousehold.ID + " PersonID: " + thisPerson.ID);
                    logger.severe("TASK IS EXITING - FATAL ERROR");
                    System.exit(1);  //no sense in going on as we have a fundamental problem.
                }
          }
          //choose mode
          chosenMode=null;
          //set time and cost parameters for mode choice
          TravelTimeAndCost departCost = skims.setTravelTimeAndCost(thisTour.begin.location.zoneNumber, 
          thisTour.primaryDestination.location.zoneNumber, thisTour.begin.endTime);
          TravelTimeAndCost returnCost = skims.setTravelTimeAndCost(thisTour.primaryDestination.location.zoneNumber, 
               thisTour.end.location.zoneNumber, thisTour.primaryDestination.endTime);
          
          //Sets tour lengths
          thisTour.departDist = departCost.driveAloneDistance;
          thisTour.returnDist = returnCost.driveAloneDistance;
          thisTour.primaryDestination.distanceToActivity = departCost.driveAloneDistance;
          thisTour.end.distanceToActivity = returnCost.driveAloneDistance;
          
           //set mode choice taz attributes (only parking cost at this point)
           ZoneAttributes zone = new ZoneAttributes();
           if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK ||
                                                    thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED)
           	   zone.parkingCost=chosenTaz.workParkingCost;
           else
               zone.parkingCost=chosenTaz.nonWorkParkingCost;
            
           //mode choice model for this taz
           tmcm.calculateUtility(
           		                modeParams, 
								departCost, 
								returnCost, 
								personAttributes, 
								thisTour, 
								zone);
           tmcm.chooseMode();
           chosenMode = tmcm.chosenMode;
          //System.out.println("Chosen mode "+chosenTaz.modeModel.chosenMode.alternativeName);
          //set primaryDestination location zoneNumber
          
          thisTour.primaryMode=chosenMode;
          thisTour.hasPrimaryMode=true;
          if(debug && thisHousehold.ID==debugID) 
          	    logger.fine("Logsum for household "+thisHousehold.ID+"="+logsum);


               //endTimer();
//               logger.fine("Time to calculate probabilities and choose mode: "+elapsedTime());    
//               logger.fine("Total loop time: "+(System.currentTimeMillis()-totalCalculateDestinationZoneTime));
     }

}


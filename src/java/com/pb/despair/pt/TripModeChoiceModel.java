package com.pb.despair.pt;

import com.pb.common.model.LogitModel;
//import com.pb.common.util.SeededRandom;
import com.pb.despair.model.Mode;
import com.pb.despair.pt.tripmodes.*;
import com.pb.despair.model.SkimsInMemory;
import com.pb.despair.model.TravelTimeAndCost;
import com.pb.despair.model.ModeType;
import java.util.logging.Logger;

/** 
 * This class implements a logit model to choose a mode for a trip
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

 
public class TripModeChoiceModel{
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");

     final static boolean debug=false;
     final static int debugID = -1;
    boolean wroteOutNullTripMode = false;

     LogitModel autoNest = new LogitModel("Auto Nest");
     DriveAlone driveAlone = new DriveAlone();
     SharedRide2 sharedRide2 = new SharedRide2();
     SharedRide3Plus sharedRide3Plus = new SharedRide3Plus();
     
     public TripModeChoiceModel(){
         
         autoNest.addAlternative(driveAlone);
         autoNest.addAlternative(sharedRide2);
         autoNest.addAlternative(sharedRide3Plus);
         
     }
     
     public void calculateTripModes(PTHousehold thisHousehold, 
                                    PTPerson thisPerson, 
                                    Tour thisTour, 
                                    SkimsInMemory skims, 
                                    TripModeParametersData allParams,
                                    TazData tazs
                                    ){
               
          if(thisTour.primaryMode.type!=ModeType.AUTODRIVER &&
               thisTour.primaryMode.type!=ModeType.AUTOPASSENGER)
               return;


          //set person tour mode attributes
          PersonTripModeAttributes personAttributes = new PersonTripModeAttributes(thisHousehold,thisPerson);
                         
          //set tour mode choice attributes
          TripModeParameters params = new TripModeParameters();
          //char actPurpose = ActivityPurpose.ACTIVITY_PURPOSE[thisTour.primaryDestination.activityPurpose];
          params = (TripModeParameters) allParams.getTripModeParameters(thisTour.primaryDestination.activityPurpose);
               

          //check if intermediate stop 1, set attributes accordingly 
          if(thisTour.intermediateStop1!=null){

               //from begin activity -> intermediate Stop 1
               TravelTimeAndCost tc1 = skims.setTravelTimeAndCost(thisTour.begin.location.zoneNumber,
                                                                  thisTour.intermediateStop1.location.zoneNumber,              
                                                                  thisTour.begin.endTime
                                                                  );
               ZoneAttributes zone1 = new ZoneAttributes();
               
               if(debug) logger.info("thisTour.intermediateStop1.location.zoneNumber: "+thisTour.intermediateStop1.location.zoneNumber);
               zone1.parkingCost=((Taz)tazs.tazData.get(new Integer(thisTour.intermediateStop1.location.zoneNumber))).nonWorkParkingCost;
               calculateUtility(params,
                                tc1,
                                personAttributes,
                                thisTour.primaryMode,
                                zone1,
                                thisTour.intermediateStop1
                                );

               //from intermediate Stop 1 -> primary destination
               TravelTimeAndCost tc2 = skims.setTravelTimeAndCost(thisTour.intermediateStop1.location.zoneNumber,
                                                                  thisTour.primaryDestination.location.zoneNumber,
                                                                  thisTour.intermediateStop1.endTime);
               ZoneAttributes zone2 = new ZoneAttributes();
               zone2.parkingCost=((Taz)tazs.tazData.get(new Integer(thisTour.primaryDestination.location.zoneNumber))).nonWorkParkingCost;
               calculateUtility(params,
                                tc2,
                                personAttributes,
                                thisTour.primaryMode,
                                zone2,
                                thisTour.primaryDestination
                                );
          }else{
               //from begin -> primary destination
               TravelTimeAndCost tc = skims.setTravelTimeAndCost(thisTour.begin.location.zoneNumber,
                                                                 thisTour.primaryDestination.location.zoneNumber,
                                                                 thisTour.begin.endTime);
               ZoneAttributes zone = new ZoneAttributes();
              if (debug) {
                  logger.finer("thisTour.primaryDestination.location.zoneNumber"+thisTour.primaryDestination.location.zoneNumber);
                  logger.finer("thisTour.primaryDestination.location.zoneNumber"+thisTour.primaryDestination.location.zoneNumber);
              }
              zone.parkingCost=((Taz)tazs.tazData.get(new Integer(thisTour.primaryDestination.location.zoneNumber))).nonWorkParkingCost;
               calculateUtility(params,
                                tc,
                                personAttributes,
                                thisTour.primaryMode,
                                zone,
                                thisTour.primaryDestination
                                );
          
          }
     
     
          //check if intermediate stop 2, set attributes accordingly 
          if(thisTour.intermediateStop2!=null){
               //from primaryDestination activity -> intermediate Stop 2
               TravelTimeAndCost tc1 = skims.setTravelTimeAndCost(thisTour.primaryDestination.location.zoneNumber,
                                                                  thisTour.intermediateStop2.location.zoneNumber,
                                                                  thisTour.primaryDestination.endTime);
               ZoneAttributes zone1 = new ZoneAttributes();
               zone1.parkingCost=((Taz)tazs.tazData.get(new Integer(thisTour.intermediateStop2.location.zoneNumber))).nonWorkParkingCost;
               calculateUtility(params,
                                tc1,
                                personAttributes,
                                thisTour.primaryMode,
                                zone1,
                                thisTour.intermediateStop2
                                );
                                
               //from intermediate Stop 2 -> end
               TravelTimeAndCost tc2 = skims.setTravelTimeAndCost(thisTour.intermediateStop2.location.zoneNumber,
                                                                  thisTour.end.location.zoneNumber,
                                                                  thisTour.intermediateStop2.endTime
                                                                  );
               ZoneAttributes zone2 = new ZoneAttributes();
               //assume no parking cost at end of tour
//               zone2.parkingCost=((Taz)tazs.tazData.get(new Integer(thisTour.end.location.zoneNumber))).nonWorkParkingCost;
               calculateUtility(params,
                                tc2,
                                personAttributes,
                                thisTour.primaryMode,
                                zone2,
                                thisTour.end
                                );
          }else{
               //from primary destination -> end
               TravelTimeAndCost tc = skims.setTravelTimeAndCost(thisTour.primaryDestination.location.zoneNumber,
                                                                 thisTour.end.location.zoneNumber,
                                                                 thisTour.begin.endTime
                                                                 );
               ZoneAttributes zone = new ZoneAttributes();
               //assume no parking cost at end of tour
//               zone.parkingCost=((Taz)tazs.tazData.get(new Integer(thisTour.primaryDestination.location.zoneNumber))).nonWorkParkingCost;
               calculateUtility(params,
                                tc,
                                personAttributes,
                                thisTour.primaryMode,
                                zone,
                                thisTour.end
                                );        
          }
         if(debug && (thisTour.intermediateStop2!=null || thisTour.intermediateStop1!=null) && !wroteOutNullTripMode){
            logger.info("Processing hh "+thisHousehold.ID+" person "+thisPerson.ID+" pattern " + thisPerson.weekdayPattern.dayPattern.toString()+ " tour "+thisTour.tourString);
            thisTour.print(thisTour);
             wroteOutNullTripMode=true;
         }
     }

     void calculateUtility(TripModeParameters theseParameters, 
                           TravelTimeAndCost tc,
                           PersonTripModeAttributes thisPerson, 
                           Mode tourMode, 
                           ZoneAttributes thisZone, 
                           Activity destActivity){
          
          
          //set top level
          //LogitModel thisModel = new LogitModel("thisModel");

          //create modes
          //DriveAlone driveAlone = new DriveAlone();
          //SharedRide2 sharedRide2 = new SharedRide2();
          //SharedRide3Plus sharedRide3Plus = new SharedRide3Plus();

            driveAlone.setAvailability(true);
            sharedRide2.setAvailability(true);
            sharedRide3Plus.setAvailability(true);                 
            //calculate utilities
            driveAlone.calcUtility( tc, thisZone, theseParameters, thisPerson, tourMode, destActivity);                         
            sharedRide2.calcUtility( tc, thisZone, theseParameters, thisPerson, tourMode, destActivity);             
            sharedRide3Plus.calcUtility( tc, thisZone, theseParameters, thisPerson, tourMode, destActivity);
             

          //add alternatives, if they are available, to nesting structure
          //if(driveAlone.isAvailable){
           //    autoNest.addAlternative(driveAlone);
               
          //     if(debug) System.out.println("Drive Alone Utility "+driveAlone.getUtility());
          //}
          //if(sharedRide2.isAvailable){
          //    autoNest.addAlternative(sharedRide2);
          //     if(debug) System.out.println("Shared Ride 2 Utility "+sharedRide2.getUtility());
          //}
          //if(sharedRide3Plus.isAvailable){
          //    autoNest.addAlternative(sharedRide3Plus);
          //     if(debug) System.out.println("Shared Ride 3+ Utility "+sharedRide3Plus.getUtility());
          //}
          
          autoNest.computeAvailabilities();  
          double logsum = autoNest.getUtility();     
          autoNest.calculateProbabilities();
                               
          if(debug) logger.finer("Logsum "+logsum);

          try{ 
               destActivity.tripMode = (Mode)autoNest.chooseAlternative();
//                  if(debug){
//                      destActivity.tripMode.print();
////                      logger.finer("Chosen mode: "+destActivity.tripMode.alternativeName);
//                  }
          }catch(Exception e){
               System.out.println(e);
               logger.severe("Error in trip mode choice: no modes available ");
               System.exit(1);
          }


     }


}

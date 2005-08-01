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

import com.pb.common.model.LogitModel;
//import com.pb.common.util.SeededRandom;
import com.pb.tlumip.model.Mode;
import com.pb.tlumip.pt.tripmodes.*;
import com.pb.tlumip.model.SkimsInMemory;
import com.pb.tlumip.model.TravelTimeAndCost;
import com.pb.tlumip.model.ModeType;
import org.apache.log4j.Logger;

import java.io.PrintWriter;

/** 
 * This class implements a logit model to choose a mode for a trip
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

 
public class TripModeChoiceModel{
    final static Logger logger = Logger.getLogger(TripModeChoiceModel.class);

     final static int debugID = -1;
    boolean wroteOutNullTripMode = false;

     LogitModel autoNest = new LogitModel("Auto Nest");
     DriveAlone driveAlone = new DriveAlone();
     SharedRide2 sharedRide2 = new SharedRide2();
     SharedRide3Plus sharedRide3Plus = new SharedRide3Plus();

    WalkTrip walkTrip = new WalkTrip();
    BikeTrip bikeTrip = new BikeTrip();
    DriveTransitTrip driveTransitTrip = new DriveTransitTrip();
    WalkTransitTrip walkTransitTrip = new WalkTransitTrip();

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
               

         //All trips on WALK tours should be assigned WALKTRIP
         if(thisTour.primaryMode.type == ModeType.WALK){
             if(thisTour.intermediateStop1 != null) thisTour.intermediateStop1.tripMode = walkTrip;
             thisTour.primaryDestination.tripMode = walkTrip;
             if(thisTour.intermediateStop2 != null) thisTour.intermediateStop2.tripMode=walkTrip;
             thisTour.end.tripMode = walkTrip;
             return;
         }
         //All trips on BIKE tours should be assigned BIKETRIP
         if(thisTour.primaryMode.type == ModeType.BIKE){
             if(thisTour.intermediateStop1 != null) thisTour.intermediateStop1.tripMode = bikeTrip;
             thisTour.primaryDestination.tripMode = bikeTrip;
             if(thisTour.intermediateStop2 != null) thisTour.intermediateStop2.tripMode=bikeTrip;
             thisTour.end.tripMode = bikeTrip;
             return;
         }
         //All trips on WALKTRANSIT tours should be assigned WALKTRANSITTRIP
         if(thisTour.primaryMode.type == ModeType.WALKTRANSIT){
             if(thisTour.intermediateStop1 != null) thisTour.intermediateStop1.tripMode = walkTransitTrip;
             thisTour.primaryDestination.tripMode = walkTransitTrip;
             if(thisTour.intermediateStop2 != null) thisTour.intermediateStop2.tripMode=walkTransitTrip;
             thisTour.end.tripMode = walkTransitTrip;
             return;
         }
         //The non-drive-access portion of DRIVETRANSIT Tours should be assigned WALKTRANSITTRIP
         //The drive-access portion of DRIVETRANSIT tours (the first and last trip of these tours)
         //should be assigned DRIVETRANSTITRIP.
         if(thisTour.primaryMode.type == ModeType.DRIVETRANSIT){
             if(thisTour.intermediateStop1 != null) {
                 thisTour.intermediateStop1.tripMode = driveTransitTrip;
                 thisTour.primaryDestination.tripMode = walkTransitTrip;
             }else thisTour.primaryDestination.tripMode = driveTransitTrip;
             if(thisTour.intermediateStop2 != null) thisTour.intermediateStop2.tripMode = walkTransitTrip;
             thisTour.end.tripMode = driveTransitTrip;
             return;
         }

         //To get this far, the tour must either be an AutoDriver, AutoPassenger,
         // TransitPassenger or PassengerTransit tour.  If it is a passenger tour
         //then "DRIVEALONE" should not be available.

          //set person tour mode attributes
          PersonTripModeAttributes personAttributes = new PersonTripModeAttributes(thisHousehold,thisPerson);
                         
          //set tour mode choice attributes
          TripModeParameters params = new TripModeParameters();
          //char actPurpose = ActivityPurpose.ACTIVITY_PURPOSES[thisTour.primaryDestination.activityPurpose];
          params = (TripModeParameters) allParams.getTripModeParameters(thisTour.primaryDestination.activityPurpose);
          
          TravelTimeAndCost tc1 = new TravelTimeAndCost();
          TravelTimeAndCost tc2 = new TravelTimeAndCost();

          //check if intermediate stop 1, set attributes accordingly 
          if(thisTour.intermediateStop1!=null){

               //from begin activity -> intermediate Stop 1
              tc1 = skims.setTravelTimeAndCost(tc1, thisTour.begin.location.zoneNumber,
                                                                  thisTour.intermediateStop1.location.zoneNumber,
                                                                  thisTour.begin.endTime
                                                                  );
               ZoneAttributes zone1 = new ZoneAttributes();

               if(logger.isDebugEnabled()) {
                   logger.debug("thisTour.intermediateStop1.location.zoneNumber: "+thisTour.intermediateStop1.location.zoneNumber);
               }
               zone1.parkingCost=((Taz)tazs.tazData.get(new Integer(thisTour.intermediateStop1.location.zoneNumber))).nonWorkParkingCost;
               calculateUtility(params,
                                tc1,
                                personAttributes,
                                thisTour.primaryMode,
                                zone1,
                                thisTour.intermediateStop1,
                                thisTour.begin.location.zoneNumber,                        //for debug purposes.
                                thisTour.intermediateStop1.location.zoneNumber,     //for debug purposes.
                                thisHousehold.ID,                                                      //for debug purposes.
                                thisPerson.ID                                                             //for debug purposes.
                                );

               //from intermediate Stop 1 -> primary destination
               tc2 = skims.setTravelTimeAndCost(tc2, thisTour.intermediateStop1.location.zoneNumber,
                                                                  thisTour.primaryDestination.location.zoneNumber,
                                                                  thisTour.intermediateStop1.endTime);
               ZoneAttributes zone2 = new ZoneAttributes();
               zone2.parkingCost=((Taz)tazs.tazData.get(new Integer(thisTour.primaryDestination.location.zoneNumber))).nonWorkParkingCost;
               calculateUtility(params,
                                tc2,
                                personAttributes,
                                thisTour.primaryMode,
                                zone2,
                                thisTour.primaryDestination,
                                thisTour.begin.location.zoneNumber,
                                thisTour.primaryDestination.location.zoneNumber,
                                thisHousehold.ID,
                                thisPerson.ID
                                );
          }else{
               //from begin -> primary destination
               tc1 = skims.setTravelTimeAndCost(tc1, thisTour.begin.location.zoneNumber,
                                                                 thisTour.primaryDestination.location.zoneNumber,
                                                                 thisTour.begin.endTime);
               ZoneAttributes zone = new ZoneAttributes();
              if(logger.isDebugEnabled()) {
                  logger.debug("thisTour.primaryDestination.location.zoneNumber"+thisTour.primaryDestination.location.zoneNumber);
                  logger.debug("thisTour.primaryDestination.location.zoneNumber"+thisTour.primaryDestination.location.zoneNumber);
              }
              zone.parkingCost=((Taz)tazs.tazData.get(new Integer(thisTour.primaryDestination.location.zoneNumber))).nonWorkParkingCost;
              calculateUtility(params,
                                tc1,
                                personAttributes,
                                thisTour.primaryMode,
                                zone,
                                thisTour.primaryDestination,
                                thisTour.begin.location.zoneNumber,
                                thisTour.primaryDestination.location.zoneNumber,
                                thisHousehold.ID,
                                thisPerson.ID
                                );
          
          }
     
     
          //check if intermediate stop 2, set attributes accordingly 
          if(thisTour.intermediateStop2!=null){
               //from primaryDestination activity -> intermediate Stop 2
               tc1 = skims.setTravelTimeAndCost(tc1, thisTour.primaryDestination.location.zoneNumber,
                                                                  thisTour.intermediateStop2.location.zoneNumber,
                                                                  thisTour.primaryDestination.endTime);
               ZoneAttributes zone1 = new ZoneAttributes();
               zone1.parkingCost=((Taz)tazs.tazData.get(new Integer(thisTour.intermediateStop2.location.zoneNumber))).nonWorkParkingCost;
               calculateUtility(params,
                                tc1,
                                personAttributes,
                                thisTour.primaryMode,
                                zone1,
                                thisTour.intermediateStop2,
                                thisTour.begin.location.zoneNumber,
                                thisTour.intermediateStop2.location.zoneNumber,
                                thisHousehold.ID,
                                thisPerson.ID
                                );
                                
               //from intermediate Stop 2 -> end
               tc2 = skims.setTravelTimeAndCost(tc2, thisTour.intermediateStop2.location.zoneNumber,
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
                                thisTour.end,
                                thisTour.begin.location.zoneNumber,
                                thisTour.end.location.zoneNumber,
                                thisHousehold.ID,
                                thisPerson.ID
                                );
          }else{
               //from primary destination -> end
               tc1 = skims.setTravelTimeAndCost(tc1, thisTour.primaryDestination.location.zoneNumber,
                                                                 thisTour.end.location.zoneNumber,
                                                                 thisTour.begin.endTime
                                                                 );
               ZoneAttributes zone = new ZoneAttributes();
               //assume no parking cost at end of tour
//               zone.parkingCost=((Taz)tazs.tazData.get(new Integer(thisTour.primaryDestination.location.zoneNumber))).nonWorkParkingCost;
               calculateUtility(params,
                                tc1,
                                personAttributes,
                                thisTour.primaryMode,
                                zone,
                                thisTour.end,
                                thisTour.begin.location.zoneNumber,
                                thisTour.end.location.zoneNumber,
                                thisHousehold.ID,
                                thisPerson.ID
                                );        
          }
         if(logger.isDebugEnabled() && (thisTour.intermediateStop2!=null || thisTour.intermediateStop1!=null) && !wroteOutNullTripMode){
            logger.info("Processing hh "+thisHousehold.ID+" person "+thisPerson.ID+" pattern " + thisPerson.weekdayPattern.dayPattern.toString()+ " tour "+thisTour.tourString);
            thisTour.print(thisTour);
             wroteOutNullTripMode=true;
         }
     }

//     void calculateUtility(TripModeParameters theseParameters, 
//                           TravelTimeAndCost tc,
//                           PersonTripModeAttributes thisPerson, 
//                           Mode tourMode, 
//                           ZoneAttributes thisZone, 
//                           Activity destActivity){
//
//            // set availabilities and calculate utilities
//            driveAlone.calcUtility( tc, thisZone, theseParameters, thisPerson, tourMode, destActivity);                         
//            sharedRide2.calcUtility( tc, thisZone, theseParameters, thisPerson, tourMode, destActivity);             
//            sharedRide3Plus.calcUtility( tc, thisZone, theseParameters, thisPerson, tourMode, destActivity);
//             
//            autoNest.computeAvailabilities();
//            double logsum = autoNest.getUtility();
//            autoNest.calculateProbabilities();
//                               
//            if(logger.isDebugEnabled()) {
//                logger.debug("Logsum "+logsum);
//            }
//
//            try{
//                 destActivity.tripMode = (Mode)autoNest.chooseAlternative();
//
//            }catch(Exception e){
//                 System.out.println(e);
//                 logger.fatal("Error in trip mode choice: no modes available ");
//                 System.exit(1);
//            }
//     }

         void calculateUtility(TripModeParameters theseParameters,
                           TravelTimeAndCost tc,
                           PersonTripModeAttributes thisPerson,
                           Mode tourMode,
                           ZoneAttributes thisZone,
                           Activity destActivity,
                           int originZone,
                           int destinationZone,
                           int hhID,
                           int pID){

            // set availabilities and calculate utilities
            driveAlone.calcUtility( tc, thisZone, theseParameters, thisPerson, tourMode, destActivity);
            sharedRide2.calcUtility( tc, thisZone, theseParameters, thisPerson, tourMode, destActivity);
            sharedRide3Plus.calcUtility( tc, thisZone, theseParameters, thisPerson, tourMode, destActivity);

            autoNest.computeAvailabilities();
            double logsum = autoNest.getUtility();
            autoNest.calculateProbabilities();

            if(logger.isDebugEnabled()) {
                logger.debug("Logsum "+logsum);
            }

            try{
                 destActivity.tripMode = (Mode)autoNest.chooseAlternative();

            }catch(Exception e){
                //A trip mode could not be found.  Create a debug file in the debug directory with
                //pertinant information and then assign 'SharedRide2' as the trip mode so that the
                //program can continue running.  The PTDafMaster will check for the existence of debug files at the end
                //of the PT run and will write out a warning message and move the files into the t# directory.
                logger.warn("A trip mode could not be found, see P" + pID + "ActivityType" + destActivity.activityType + "TripModeDebugFile.txt.  Location of file is specfied in pt.properties");
                PrintWriter file = PTResults.createTripModeDebugFile("P" + pID + "ActivityType" + destActivity.activityType + "TripModeDebugFile.txt");  // will determine location of debug file and
                                                                                                    // add a header to the file.
                file.println("Summary:");
                file.println();
                file.println("HHID = " + hhID);
                file.println("PersonID = " + pID);
                file.println("ActivityType = " + destActivity.activityType);
                file.println("ActivityPurpose = " + destActivity.activityPurpose);
                file.println("Origin Zone = " + originZone);
                file.println("Destination Zone = " + destinationZone);
                file.println("TourMode = " + tourMode);   //toString() method returns alternative name.
                file.println();
                file.flush();

                file.println("Details: ");
                thisPerson.print(file);      //prints out the person trip mode attributes to the debug file
                thisZone.print(file);        //prints out the parking cost in the destination zone
                tc.print(file);                 //prints out the travel time and cost from origin zone to destination zone.
                theseParameters.print(file);          //prints out the trip mode parameters that are based on the activty purpose (ivt, etc)
                destActivity.print(file);    //prints out the dest activity atributes such as start time, end time, etc.

                file.close();

                //Now assign the trip mode to "SharedRide2" and return.
                destActivity.tripMode=sharedRide2;

            }


     }


}

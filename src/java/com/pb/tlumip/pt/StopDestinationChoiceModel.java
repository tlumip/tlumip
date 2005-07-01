package com.pb.tlumip.pt;

import com.pb.common.model.LogitModel;
import com.pb.tlumip.model.SkimsInMemory;
import com.pb.tlumip.model.ModeType;

import java.util.Enumeration;
import java.util.Iterator;
import org.apache.log4j.Logger;
import java.io.PrintWriter;

/** 
 * Model for choosing the intermediate stop destination on tours
 * 
 * @author Freedman
 * @version 1.0 12/01/2003
 *  
 */

 
public class StopDestinationChoiceModel{
     final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.StopDestinationChoiceModel");
     double utility;
     final static int debugID = 68313;
     LogitModel iStop1Model;
     LogitModel iStop2Model;
     //TazData allTazs;
     

     public void buildModel(TazData tazs){
         iStop1Model = new LogitModel("iStop1Model",tazs.tazData.size());
         iStop2Model = new LogitModel("iStop2Model",tazs.tazData.size());

         //create stop1 model with stop1 tazData
         Enumeration stop1DestinationEnum=tazs.tazData.elements();
         while(stop1DestinationEnum.hasMoreElements()){
             Taz destinationTaz = (Taz) stop1DestinationEnum.nextElement();
             iStop1Model.addAlternative(destinationTaz);
         }

         //create stop2 model with stop2 tazData
         Enumeration stop2DestinationEnum=tazs.tazData.elements();
         while(stop2DestinationEnum.hasMoreElements()){
             Taz destinationTaz = (Taz) stop2DestinationEnum.nextElement();
             iStop2Model.addAlternative(destinationTaz);
         }
     }
      
     public void calculateStopZones(PTHousehold thisHousehold, PTPerson thisPerson,Tour thisTour, SkimsInMemory skims, 
          StopDestinationParametersData sdpd, TazData tazs){
          
          //no stops on tour
          if(thisTour.intermediateStop1==null && thisTour.intermediateStop2==null){
            return;
          }

          //no begin taz, end Taz, or primaryDestination taz
          if(thisTour.begin.location.zoneNumber==0 || thisTour.end.location.zoneNumber==0 || thisTour.primaryDestination.location.zoneNumber==0 ){
              logger.error("Not running StopZones model for the following tour due to problems in the begin,end and primary dest TAZ numbers");
              logger.error("Error: begin taz : "+thisTour.begin.location.zoneNumber+ " end taz "+thisTour.end.location.zoneNumber+ " pd taz "+thisTour.primaryDestination.location.zoneNumber);

              //write the tour information into a debug file.  Path is specified in the pt.properties file
              logger.error("Writing Tour Debug info to the debug directory");
              PrintWriter file = PTResults.createTourDebugFile("HH" + thisHousehold.ID + "Tour" + thisTour.tourNumber+".csv");
              thisTour.printCSV(file);
              file.close();
              return;
          }
           //set up destination choice parameters
          StopDestinationParameters destParams = (StopDestinationParameters) sdpd.stopDestinationParameters[thisTour.primaryDestination.activityPurpose];

        //First run the stop1location model
        if (thisTour.intermediateStop1!=null) {
             //calculate utilities for each taz.  Use the ptModel.tazs that were passed into the method.
             Iterator alternatives = (iStop1Model.getAlternatives()).iterator();
             while(alternatives.hasNext()){
                 Taz  stop1Taz = (Taz)alternatives.next();

                 float autoTime=0;
                 float walkTime=0;
                 float bikeTime=0;
                 float transitGeneralizedCost=0;
                 float[] autoDists= new float[2]; //autoDists[0] = distance from begin to primary destination
                                                  //autoDists[1] = distance from begin to stop + stop to primary destination
                                                  //if autoDist[1] > 2 * autoDists[0] then that stop zone is not available.

                 autoDists = skims.getAdditionalAutoDistance(thisTour.begin.location.zoneNumber,
                                                                          thisTour.primaryDestination.location.zoneNumber,
                                                                          stop1Taz.zoneNumber,
                                                                          thisTour.begin.endTime);

                 if(thisTour.primaryMode.type==ModeType.WALK)
                         walkTime=skims.getAdditionalWalkTime(thisTour.begin.location.zoneNumber,
                                                              thisTour.primaryDestination.location.zoneNumber,
                                                              stop1Taz.zoneNumber,
                                                              thisTour.begin.endTime
                                                              );
                else if(thisTour.primaryMode.type==ModeType.BIKE)
                     bikeTime=skims.getAdditionalBikeTime(thisTour.begin.location.zoneNumber,
                                                          thisTour.primaryDestination.location.zoneNumber,
                                                          stop1Taz.zoneNumber,
                                                          thisTour.begin.endTime
                                                          );

                else if(thisTour.primaryMode.type==ModeType.WALKTRANSIT||
                        thisTour.primaryMode.type==ModeType.DRIVETRANSIT||
                        thisTour.primaryMode.type==ModeType.TRANSITPASSENGER)

                     transitGeneralizedCost=
                        skims.getAdditionalGeneralizedTransitCost(thisTour.begin.location.zoneNumber,
                                                                  thisTour.primaryDestination.location.zoneNumber,
                                                                  stop1Taz.zoneNumber,
                                                                  thisTour.begin.endTime
                                                                  );

                else
                    autoTime=skims.getAdditionalAutoTime(thisTour.begin.location.zoneNumber,
                                                          thisTour.primaryDestination.location.zoneNumber,
                                                          stop1Taz.zoneNumber,
                                                          thisTour.begin.endTime
                                                          );

                stop1Taz.calcStopDestinationUtility(thisTour.primaryDestination.activityPurpose, destParams,
                                                        thisTour.primaryMode,
                                                        autoTime,
                                                        walkTime,
                                                        bikeTime,
                                                        transitGeneralizedCost,
                                                        autoDists,
                                                        1
                                                        );
                if(logger.isDebugEnabled()) {
                     logger.debug("**** Attributes of destination "+stop1Taz.zoneNumber);
                     stop1Taz.print();
                      logger.debug("Utility for destination taz "+stop1Taz.zoneNumber+" = "
                           +stop1Taz.utility);
                 }
             } //calculated utilities for each destination TAZ

            iStop1Model.computeAvailabilities();
            iStop1Model.getUtility();
            iStop1Model.calculateProbabilities();

            try{

                 Taz chosenTaz = (Taz) iStop1Model.chooseElementalAlternative();
                 thisTour.intermediateStop1.location.zoneNumber=chosenTaz.zoneNumber;

            }catch(Exception e){
                logger.error("Error in stop destination choice: no zones available for this household, tour, stop 1");
                logger.error("Household "+thisHousehold.ID+" Person "+thisPerson.ID+" Tour "+thisTour.tourNumber);
                logger.error("A Stop1 Tour file and the TAZ info will be written out to the debug directory.");

                //write the tour information into a debug file.  Path is specified in the pt.properties file
                logger.error("Writing Tour Debug info to the debug directory");
                PrintWriter file = PTResults.createTourDebugFile("HH" + thisHousehold.ID + "Tour" + thisTour.tourNumber+"Stop1.csv");
                thisTour.printCSV(file);
                file.close();

                // if not already done, write the taz info into a debug file.  Path is specified in pt.properties
                PrintWriter file2 = PTResults.createTazDebugFile("HH" + thisHousehold.ID + "Tour" + thisTour.tourNumber+"Stop1AvailableAlternatives.csv");
                if(file2 != null){  //if it is null that means an earlier problem caused this file to be written already and there
                                       //is no reason to write it out twice
                    logger.error("Writing out Taz Info because the first intermediate stop on the tour couldn't find a destination");
                    Iterator alts = iStop1Model.getAlternatives().iterator();
                    while(alts.hasNext()){
                        Taz stop1Taz = (Taz)alts.next();
                        if (stop1Taz.isAvailable()) {
                            stop1Taz.printCSV(file2);
                        }
                    }
                    file2.close();

                }
                //in the interest of not stopping the run, we will just assign the stop location to be the
                //tour begin location.  A log report at the end of PT will notify the user of the erroneous data.
                thisTour.intermediateStop1.location.zoneNumber=thisTour.begin.location.zoneNumber;
            }
            thisTour.intermediateStop1.distanceToActivity = skims.getDistance(thisTour.begin.endTime,
                                                                                 thisTour.begin.location.zoneNumber,
                                                                                 thisTour.intermediateStop1.location.zoneNumber);

            thisTour.primaryDestination.distanceToActivity = skims.getDistance(thisTour.intermediateStop1.endTime,
               		                                                              thisTour.intermediateStop1.location.zoneNumber,
																				  thisTour.primaryDestination.location.zoneNumber);
         }//end of stop1location model

         //Now do the stop2location destination choice model
         if(thisTour.intermediateStop2!=null){

            Iterator alts2 = iStop2Model.getAlternatives().iterator();
            while(alts2.hasNext()){

                Taz stop2Taz = (Taz) alts2.next();

                 float autoTime=0;
                 float walkTime=0;
                 float bikeTime=0;
                 float transitGeneralizedCost=0;
                 float[] autoDists= new float[2];  //autoDist[0] = dist. from prim. dest to end
                                                   //autoDist[1] = dist from prim dest to stop + stop to end

                 autoDists = skims.getAdditionalAutoDistance(thisTour.primaryDestination.location.zoneNumber,
                                                                          thisTour.end.location.zoneNumber,
                                                                          stop2Taz.zoneNumber,
                                                                          thisTour.primaryDestination.endTime);

                 if(thisTour.primaryMode.type==ModeType.WALK)
                         walkTime=skims.getAdditionalWalkTime(thisTour.primaryDestination.location.zoneNumber,
                                                              thisTour.end.location.zoneNumber,
                                                              stop2Taz.zoneNumber,
                                                              thisTour.primaryDestination.endTime
                                                              );
                 else if(thisTour.primaryMode.type==ModeType.BIKE)
                         bikeTime=skims.getAdditionalBikeTime(thisTour.primaryDestination.location.zoneNumber,
                                                              thisTour.end.location.zoneNumber,
                                                              stop2Taz.zoneNumber,
                                                              thisTour.primaryDestination.endTime
                                                              );
                 else if(thisTour.primaryMode.type==ModeType.WALKTRANSIT||
                            thisTour.primaryMode.type==ModeType.DRIVETRANSIT||
                            thisTour.primaryMode.type==ModeType.PASSENGERTRANSIT)

                         transitGeneralizedCost=
                            skims.getAdditionalGeneralizedTransitCost(thisTour.primaryDestination.location.zoneNumber,
                                                                      thisTour.end.location.zoneNumber,
                                                                      stop2Taz.zoneNumber,
                                                                      thisTour.primaryDestination.endTime
                                                                      );
                 else
                    autoTime=skims.getAdditionalAutoTime(thisTour.primaryDestination.location.zoneNumber,
                                                              thisTour.end.location.zoneNumber,
                                                              stop2Taz.zoneNumber,
                                                              thisTour.primaryDestination.endTime
                                                              );
                 //destination choice model for this taz
                 stop2Taz.calcStopDestinationUtility(thisTour.primaryDestination.activityPurpose, destParams,
                                                        thisTour.primaryMode,
                                                        autoTime,
                                                        walkTime,
                                                        bikeTime,
                                                        transitGeneralizedCost,
                                                        autoDists,
                                                        2
                                                        );
                 if(logger.isDebugEnabled()) {
                      logger.debug("**** Attributes of destination "+stop2Taz.zoneNumber);
                      stop2Taz.print();
                       logger.debug("Utility for destination taz "+stop2Taz.zoneNumber+" = "
                            +stop2Taz.utility);
                 }

             } //utilties have been calculated for each taz

             iStop2Model.computeAvailabilities();
             iStop2Model.getUtility();
             iStop2Model.calculateProbabilities();
             try{

                 Taz chosenTaz = (Taz) iStop2Model.chooseElementalAlternative();
                 thisTour.intermediateStop2.location.zoneNumber=chosenTaz.zoneNumber;

             }catch(Exception e){
                 logger.error("Error in stop destination choice: no zones available for this household, tour, stop 2");
                 logger.error("Household "+thisHousehold.ID+" Person "+thisPerson.ID+" Tour "+thisTour.tourNumber);
                 logger.error("A Stop2 Tour file and the TAZ info will be written out to the debug directory.");

                 //write the tour information into a debug file.  Path is specified in the pt.properties file
                 logger.error("Writing Tour Debug info to the debug directory");
                 PrintWriter file = PTResults.createTourDebugFile("HH" + thisHousehold.ID + "Tour" + thisTour.tourNumber+"Stop2.csv");
                 thisTour.printCSV(file);
                 file.close();

                 // if not already done, write the taz info into a debug file.  Path is specified in pt.properties
                 PrintWriter file2 = PTResults.createTazDebugFile("HH" + thisHousehold.ID + "Tour" + thisTour.tourNumber+"Stop2AvailableAlternatives.csv");
                 if(file2 != null){  //if it is null that means an earlier problem caused this file to be written already and there
                                       //is no reason to write it out twice
                     logger.error("Writing out Taz Info because the second stop on this tour couldn't find a destination");
                     Iterator alts = iStop2Model.getAlternatives().iterator();
                     while(alts.hasNext()){
                         Taz stop2Taz = (Taz)alts.next();
                         if (stop2Taz.isAvailable()) {
                             stop2Taz.printCSV(file2);
                         }
                     }
                     file2.close();
                 }
                 //in the interest of not stopping the run, we will just assign the stop location to be the
                 //tour begin location.  A log report at the end of PT will notify the user of the erroneous data.
                 thisTour.intermediateStop1.location.zoneNumber=thisTour.begin.location.zoneNumber;
             }
             thisTour.intermediateStop2.distanceToActivity = skims.getDistance(thisTour.primaryDestination.endTime,
                                                                    thisTour.primaryDestination.location.zoneNumber,
                                                                    thisTour.intermediateStop2.location.zoneNumber);

             thisTour.primaryDestination.distanceToActivity = skims.getDistance(thisTour.intermediateStop2.endTime,
                                                                    thisTour.intermediateStop2.location.zoneNumber,
                                                                    thisTour.end.location.zoneNumber);
        } //end of stop2location model

    } //end of calculateStopZones method.

}


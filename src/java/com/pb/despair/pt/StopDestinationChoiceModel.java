package com.pb.despair.pt;

import com.pb.common.model.LogitModel;
import com.pb.despair.model.SkimsInMemory;
import com.pb.despair.model.ModeType;

import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Logger;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

/** 
 * Model for choosing the intermediate stop destination on tours
 * 
 * @author Freedman
 * @version 1.0 12/01/2003
 *  
 */

 
public class StopDestinationChoiceModel{
     protected static Logger logger = Logger.getLogger("com.pb.despair.pt.StopDestinationChoiceModel");
     double utility;
     final static boolean debug=false;
     final static int debugID = 68313;
     LogitModel iStop1Model;
     LogitModel iStop2Model;
     //TazData allTazs;
     

     public void buildModel(TazData tazs){
         //allTazs=tazs;
         iStop1Model = new LogitModel("iStop1Model",tazs.tazData.size());
         iStop2Model = new LogitModel("iStop2Model",tazs.tazData.size());
         int destination=0;
        
         //create stop1 model with stop1 tazData
         //   first need a copy of the TazData for stop1...
//         TazData stop1Tazs = (TazData) tazs.clone();
         Enumeration stop1DestinationEnum=tazs.tazData.elements();
         while(stop1DestinationEnum.hasMoreElements()){
             Taz destinationTaz = (Taz) stop1DestinationEnum.nextElement();
             iStop1Model.addAlternative(destinationTaz);
         }
         //create stop2 model with stop2 tazData
         //   first need a copy of the TazData for stop2...
//         TazData stop2Tazs = (TazData) tazs.clone();
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
              logger.severe("Not running StopZones model for the following tour due to problems in the begin,end and primary dest TAZ numbers");
              logger.severe("Error: begin taz : "+thisTour.begin.location.zoneNumber+ " end taz "+thisTour.end.location.zoneNumber+ " pd taz "+thisTour.primaryDestination.location.zoneNumber);
              thisTour.print(thisTour);
              return;
          }
           //set up destination choice parameters
          StopDestinationParameters destParams = (StopDestinationParameters) sdpd.stopDestinationParameters[thisTour.primaryDestination.activityPurpose];

        //First run the stop1location model
        if (thisTour.intermediateStop1!=null) {
             //calculate utilities for each taz.  Use the ptModel.tazs that were passed into the method.
//            Enumeration enum = tazs.tazData.elements();
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
//                    if(debug && thisHousehold.ID==debugID){
                if(debug){
                     logger.info("**** Attributes of destination "+stop1Taz.zoneNumber);
                     stop1Taz.print();
                      logger.info("Utility for destination taz "+stop1Taz.zoneNumber+" = "
                           +stop1Taz.utility);
                 }
//                   logger.fine("Time to get destination Choice: "+(System.currentTimeMillis()-destChoiceTime));
             } //calculated utilities for each destination TAZ

            iStop1Model.computeAvailabilities();
            iStop1Model.getUtility();
            iStop1Model.calculateProbabilities();
            try{

                 Taz chosenTaz = (Taz) iStop1Model.chooseElementalAlternative();
            //      logger.finest("Chose iStop1 taz: "+chosenTaz.zoneNumber);
                 thisTour.intermediateStop1.location.zoneNumber=chosenTaz.zoneNumber;

            }catch(Exception e){
                 logger.severe("Error in destination choice: no zones available for this household, tour, stop 1");
                 logger.severe("Household "+thisHousehold.ID+" Person "+thisPerson.ID+" Tour "+thisTour.tourNumber);
                 Iterator alts = iStop1Model.getAlternatives().iterator();
                 while(alts.hasNext()){
                     Taz stop1Taz = (Taz)alts.next();
                     logger.severe("**** Attributes of destination "+stop1Taz.zoneNumber);
                     stop1Taz.print();
                     logger.severe("Utility for destination taz "+stop1Taz.zoneNumber+" = "
                                                      +stop1Taz.utility);
                 }
                try {
                	//TODO get debug filename from pt.properties
                    logger.severe("Writing Tour Debug info to the /models/tlumip/debug directory");
                    PrintWriter file = new PrintWriter(new FileWriter("/models/tlumip/debug/HH" + thisHousehold.ID + "Tour" + thisTour.tourNumber+"Stop1.csv"));
                    file.println("tourString,tour#," +
                        "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                        "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                        "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                        "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                        "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                        "primaryMode");
                    thisTour.printCSV(file);
                } catch (IOException e1) {
                    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
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
//             Enumeration enum = tazs.tazData.elements();
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
//               if(debug && thisHousehold.ID==debugID){
                 if(debug ){
                      logger.info("**** Attributes of destination "+stop2Taz.zoneNumber);
                      stop2Taz.print();
                       logger.info("Utility for destination taz "+stop2Taz.zoneNumber+" = "
                            +stop2Taz.utility);
                 }

             } //utilties have been calculated for each taz

             iStop2Model.computeAvailabilities();
             iStop2Model.getUtility();
             iStop2Model.calculateProbabilities();
             try{
                  Taz chosenTaz = (Taz) iStop2Model.chooseElementalAlternative();
//                logger.finest("Chose iStop2 taz: "+chosenTaz.zoneNumber);
                  thisTour.intermediateStop2.location.zoneNumber=chosenTaz.zoneNumber;
                  logger.finer("istop2 taz: "+thisTour.intermediateStop2.location.zoneNumber);
             }catch(Exception e){
                 logger.severe("Error in destination choice: no zones available for this household, tour, stop 2");
                 logger.severe("Household "+thisHousehold.ID+" Person "+thisPerson.ID+" Tour "+thisTour.tourNumber);
//                 enum = tazs.tazData.elements();
                    alts2 = iStop2Model.getAlternatives().iterator();
                    while(alts2.hasNext()){
                     Taz stop2Taz = (Taz)alts2.next();
                     logger.severe("**** Attributes of destination "+stop2Taz.zoneNumber);
                     stop2Taz.print();
                     logger.severe("Utility for destination taz "+stop2Taz.zoneNumber+" = "
                                                      +stop2Taz.utility);
                 }
                 try {
                	//TODO get debug filename from pt.properties
                     logger.severe("Writing tour debug info to /models/tlumip/debug ");
                     PrintWriter file = new PrintWriter(new FileWriter("/models/tlumip/debug/HH" + thisHousehold.ID + "Tour" + thisTour.tourNumber+"Stop1.csv"));
                     file.println("tourString,tour#," +
                        "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                        "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                        "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                        "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                        "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                        "primaryMode");
                    thisTour.printCSV(file);
                  } catch (IOException e1) {
                     e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                 }
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


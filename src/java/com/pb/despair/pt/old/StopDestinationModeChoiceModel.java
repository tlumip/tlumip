package com.pb.despair.pt.old;

import com.pb.common.model.LogitModel;
import com.pb.despair.model.SkimsInMemory;
import com.pb.despair.model.ModeType;
import com.pb.despair.pt.ActivityPurpose;
import com.pb.despair.pt.PTHousehold;
import com.pb.despair.pt.PTPerson;
import com.pb.despair.pt.StopDestinationParameters;
import com.pb.despair.pt.StopDestinationParametersData;
import com.pb.despair.pt.Taz;
import com.pb.despair.pt.TazData;
import com.pb.despair.pt.Tour;

import java.util.Enumeration;


/** 
 * Model for choosing the destination and mode choice of 
 * intermediate stops
 * 
 * @author Freedman
 * @version 1.0 12/01/2003
 *  
 */

 
public class StopDestinationModeChoiceModel{

     double logsum;
     final static boolean debug=false;
     final static int debugID = 1;
     
     //constructor
     public void calculateDestinationZone(PTHousehold thisHousehold, PTPerson thisPerson, int tourNumber, SkimsInMemory skims, 
          StopDestinationParametersData sdpd, TazData allTazs){
          
          Tour thisTour = thisPerson.weekdayTours[tourNumber];

          //no stops on tour
          if(thisTour.hasIntermediateStop1(thisTour.tourString)+
             thisTour.hasIntermediateStop2(thisTour.tourString)==0)     
               return;

          //no begin taz, end Taz, or primaryDestination taz
          if(thisTour.begin.location.zoneNumber==0 || thisTour.end.location.zoneNumber==0 || thisTour.primaryDestination.location.zoneNumber==0 )
               return;

           //set up destination choice parameters
          StopDestinationParameters destParams = new StopDestinationParameters();
          char actPurpose = ActivityPurpose.ACTIVITY_PURPOSE[thisTour.primaryDestination.activityPurpose];          
          destParams = (StopDestinationParameters) sdpd.stopDestinationParameters[thisTour.primaryDestination.activityPurpose];

          //for destination choice model
          LogitModel iStop1Model = new LogitModel("iStop1Model");
          LogitModel iStop2Model = new LogitModel("iStop2Model");
          
          Enumeration destinationEnum=allTazs.tazData.elements();
          while(destinationEnum.hasMoreElements()){

               Taz stop1Taz = (Taz) destinationEnum.nextElement();
               Taz stop2Taz = stop1Taz;
               stop1Taz.setName(new Integer(stop1Taz.zoneNumber).toString());
               stop2Taz.setName(new Integer(stop2Taz.zoneNumber).toString());

               float autoTime=0;
               float walkTime=0;
               float bikeTime=0;
               float transitGeneralizedCost=0;
               
               if(thisTour.hasIntermediateStop1(thisTour.tourString)==1){
                    if(thisTour.primaryMode.type==ModeType.WALK)
                         walkTime=skims.getAdditionalWalkTime(thisTour.begin.location.zoneNumber, thisTour.primaryDestination.location.zoneNumber,
                              stop1Taz.zoneNumber, thisTour.begin.endTime);
                    else if(thisTour.primaryMode.type==ModeType.BIKE)          
                         bikeTime=skims.getAdditionalBikeTime(thisTour.begin.location.zoneNumber, thisTour.primaryDestination.location.zoneNumber,
                              stop1Taz.zoneNumber, thisTour.begin.endTime);
                    else if(thisTour.primaryMode.type==ModeType.WALKTRANSIT||thisTour.primaryMode.type==ModeType.DRIVETRANSIT||thisTour.primaryMode.type==ModeType.TRANSITPASSENGER)          
                         transitGeneralizedCost=skims.getAdditionalGeneralizedTransitCost(thisTour.begin.location.zoneNumber, 
                              thisTour.primaryDestination.location.zoneNumber, stop1Taz.zoneNumber, thisTour.begin.endTime);
                    else
                         autoTime=skims.getAdditionalAutoTime(thisTour.begin.location.zoneNumber, thisTour.primaryDestination.location.zoneNumber,
                              stop1Taz.zoneNumber, thisTour.begin.endTime);
                              
                    //destination choice model for this taz                         
                    //stop1Taz.oldCalcStopDestinationUtility(destParams,thisTour.primaryMode,autoTime, walkTime, bikeTime,transitGeneralizedCost,1);
                    stop1Taz.calcStopDestinationUtility(actPurpose,destParams,thisTour.primaryMode,autoTime, walkTime, bikeTime,transitGeneralizedCost,1);
                    if(stop1Taz.isAvailable())
                         iStop1Model.addAlternative(stop1Taz);
                    if(debug && thisHousehold.ID==debugID){
                         System.out.println("**** Attributes of destination "+stop1Taz.zoneNumber);
                         stop1Taz.print();
                          System.out.println("Utility for destination taz "+stop1Taz.zoneNumber+" = "
                               +stop1Taz.utility);
                     }
               }
               

               if(thisTour.hasIntermediateStop2(thisTour.tourString)==1){
                    if(thisTour.primaryMode.type==ModeType.WALK)
                         walkTime=skims.getAdditionalWalkTime(thisTour.primaryDestination.location.zoneNumber, thisTour.end.location.zoneNumber,
                              stop2Taz.zoneNumber, thisTour.primaryDestination.endTime);
                    else if(thisTour.primaryMode.type==ModeType.BIKE)          
                         bikeTime=skims.getAdditionalBikeTime(thisTour.primaryDestination.location.zoneNumber, thisTour.end.location.zoneNumber,
                              stop2Taz.zoneNumber, thisTour.primaryDestination.endTime);
                    else if(thisTour.primaryMode.type==ModeType.WALKTRANSIT||thisTour.primaryMode.type==ModeType.DRIVETRANSIT||thisTour.primaryMode.type==ModeType.PASSENGERTRANSIT)          
                         transitGeneralizedCost=skims.getAdditionalGeneralizedTransitCost(thisTour.primaryDestination.location.zoneNumber, 
                              thisTour.end.location.zoneNumber, stop2Taz.zoneNumber, thisTour.primaryDestination.endTime);
                    else
                         autoTime=skims.getAdditionalAutoTime(thisTour.primaryDestination.location.zoneNumber, thisTour.end.location.zoneNumber,
                              stop2Taz.zoneNumber, thisTour.primaryDestination.endTime);
                              
                    //destination choice model for this taz                         
                    //stop2Taz.oldCalcStopDestinationUtility(destParams,thisTour.primaryMode,autoTime, walkTime, bikeTime,transitGeneralizedCost,2);
                    stop2Taz.calcStopDestinationUtility(actPurpose, destParams,thisTour.primaryMode,autoTime, walkTime, bikeTime,transitGeneralizedCost,2);
                    
                    if(stop2Taz.isAvailable())
                         iStop2Model.addAlternative(stop2Taz);
                    if(debug && thisHousehold.ID==debugID){
                         System.out.println("**** Attributes of destination "+stop2Taz.zoneNumber);
                         stop2Taz.print();
                          System.out.println("Utility for destination taz "+stop2Taz.zoneNumber+" = "
                               +stop2Taz.utility);
                     }
               }
         
              iStop1Model.computeAvailabilities();
              logsum = iStop1Model.getUtility();
              iStop1Model.calculateProbabilities();
         
               
          }; //destinations
          if(thisTour.hasIntermediateStop1(thisTour.tourString)==1){
               try{
                    Taz chosenTaz = (Taz) iStop1Model.chooseElementalAlternative();
                    //if(debug) System.out.println("Chose taz: "+chosenTaz.zoneNumber);
                    thisTour.intermediateStop1.location.zoneNumber=chosenTaz.zoneNumber;
               }catch(Exception e){
                    System.out.println("Error in destination choice: no zones available for this household, tour, stop 1");
                    System.out.println("Household "+thisHousehold.ID+" Person "+thisPerson.ID+" Tour "+thisTour.tourNumber);
                    System.exit(1);
               }
          }  //stop1
          
          iStop2Model.computeAvailabilities();
          logsum = iStop2Model.getUtility();
          iStop2Model.calculateProbabilities();
          
          if(thisTour.hasIntermediateStop2(thisTour.tourString)==1){
               try{
                    Taz chosenTaz = (Taz) iStop2Model.chooseElementalAlternative();
                    //if(debug) System.out.println("Chose taz: "+chosenTaz.zoneNumber);
                    thisTour.intermediateStop2.location.zoneNumber=chosenTaz.zoneNumber;
               }catch(Exception e){
                    System.out.println("Error in destination choice: no zones available for this household, tour, stop 2");
                    System.out.println("Household "+thisHousehold.ID+" Person "+thisPerson.ID+" Tour "+thisTour.tourNumber);
                    System.exit(1);
               }
          } //stop2
          
     }
}


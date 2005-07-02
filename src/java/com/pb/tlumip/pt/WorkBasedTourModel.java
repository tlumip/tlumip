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
import com.pb.common.util.SeededRandom;
import com.pb.common.math.MathUtil;
import com.pb.common.matrix.Matrix;
import com.pb.tlumip.model.Mode;
import com.pb.tlumip.model.SkimsInMemory;
import com.pb.tlumip.model.TravelTimeAndCost;

import java.util.Enumeration;
import org.apache.log4j.Logger;

/**  
 * This class implements a logit model to choose a work based tour mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

 
public class WorkBasedTourModel{
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.WorkBasedTourModel");
    boolean writtenOutTheUtilitiesAlready = false;
     final static int debugID = -1;

     LogitModel destinationModel;
     double logsum;
     Taz chosenTaz;
     Mode chosenMode;
//     TazData allTazs;
     PersonTourModeAttributes personAttributes = new PersonTourModeAttributes();
     
     

     Long longVar = new Long(0);
     Double doubleVar = new Double(0);

    public void buildModel(TazData tazs){ 
        destinationModel = new LogitModel("destinationModel",tazs.tazData.size());
        Enumeration destinationEnum=tazs.tazData.elements();
        while(destinationEnum.hasMoreElements()){
            Taz destinationTaz = (Taz) destinationEnum.nextElement();
            destinationModel.addAlternative(destinationTaz);
        }           
    }


     
     //constructor
     //  TODO This method needs to be updated due to changes in TourModeChoiceModel
     public void calculateWorkBasedTour(PTHousehold thisHousehold, 
     		                            PTPerson thisPerson, 
										Tour thisTour, SkimsInMemory skims, 
										TourModeParametersData tmpd,     
                                        TourDestinationParametersData tdpd, 
                                        TazData tazs,
                                        DCExpUtilitiesManager um,
                                        TourModeChoiceModel tmcm){
          
          if(logger.isDebugEnabled() && thisHousehold.ID==debugID){
               logger.debug("Work-Based Tour found for household "+thisHousehold.ID);
          }

         if(logger.isDebugEnabled() && !writtenOutTheUtilitiesAlready){
               logger.debug("Work-Based Tour found for household "+thisHousehold.ID);
          }

          //set up destination and mode choice parameters
          TourModeParameters modeParams = (TourModeParameters) tmpd.getTourModeParameters(ActivityPurpose.WORK_BASED);

          Matrix expUtilities = um.getMatrix(ActivityPurpose.WORK_BASED,0);
          //set mode choice person attributes
          personAttributes.setAttributes(thisHousehold,thisPerson,thisTour);

 
          //calculate primary destination duration using logistic distribution
          //Logistic Model: y=a/(1+b*exp(-cx))     
          //Coefficient Data:                    
          double a =     -0.061715469;                  
          double b =     -3.6215795;                    
          double c =     1.2192908;                     
          double percentPrimaryDestinationTime = a/(1.0+b*MathUtil.exp(-c*SeededRandom.getRandom()));
          
          int totalMinutesAvailable = thisTour.begin.duration;
          
          thisTour.primaryDestination.duration=new Long(Math.round(percentPrimaryDestinationTime*totalMinutesAvailable)).shortValue();

          int minutesAvailable = totalMinutesAvailable-thisTour.primaryDestination.duration;
          
          //calculate first work activity duration using 4th degree polynomial
          //4th Degree Polynomial Fit:  y=a+bx+cx^2+dx^3+ex^4
          //Coefficient Data:
          a =     0.071716912;
          b =     2.6959682;
          c =     -6.0296104;
          double d =     5.4314903;
          double e =     -1.2171757;
          double rNumber=SeededRandom.getRandom();
          
          thisTour.begin.duration=new Double(minutesAvailable*(a+b*rNumber+c*Math.pow(rNumber,2)+d*Math.pow(rNumber,3)+e*Math.pow(rNumber,4))).shortValue();
          thisTour.begin.calculateEndTime();
          thisTour.primaryDestination.calculateStartTime(thisTour.begin);
          thisTour.primaryDestination.calculateEndTime();
          thisTour.end.calculateStartTime(thisTour.primaryDestination);
          thisTour.end.duration= (short)(totalMinutesAvailable-(thisTour.primaryDestination.duration+thisTour.begin.duration));
          thisTour.end.calculateEndTime();
          
          if(logger.isDebugEnabled() && !writtenOutTheUtilitiesAlready){
               logger.debug("Percent primary destination time "+percentPrimaryDestinationTime);
               logger.debug("Total tour time (based on first activity "+thisTour.begin.duration);
               logger.debug("Primary destination duration "+thisTour.primaryDestination.duration);
               logger.debug("Minutes available for tour begin and end activities"+minutesAvailable);
               logger.debug("Tour begin activity duration "+thisTour.begin.duration);
               logger.debug("Tour end activity duration "+thisTour.end.duration);
               logger.debug("Minutes available for travel "+minutesAvailable);
               writtenOutTheUtilitiesAlready = true;
          }


          
         if(logger.isDebugEnabled() && !writtenOutTheUtilitiesAlready){
                  logger.debug("Here are the utilities for the tazs passed into the 'WorkBasedTour.calculateWorkBaseTour' method");
                  logger.debug("for HHID " + thisHousehold.ID + ", Person " + thisPerson.ID + ", Tour " + thisTour.tourNumber
                          + ", ActivityPurpose b"
                          + ", Origin " + thisTour.begin.location.zoneNumber);
                  logger.debug("ZoneNumber,Utility,Logsum");
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
           rNumber=SeededRandom.getRandom();
           double culmProbability=0;
           tazEnum = tazs.tazData.elements();
           for(int i=0; i<tazs.tazData.size();i++){

               Taz destinationTaz = (Taz) tazEnum.nextElement();
                  
               float expUtility=expUtilities.getValueAt(thisTour.begin.location.zoneNumber,destinationTaz.zoneNumber);
               culmProbability += (double) (expUtility/totalExpUtility);
                  
              if(culmProbability>rNumber){
                chosenTaz=destinationTaz;
                break;
              }  
           } //end destinations
              

          //choose mode
          //set time and cost parameters for mode choice
          TravelTimeAndCost departCost = skims.setTravelTimeAndCost(thisTour.begin.location.zoneNumber, 
               chosenTaz.zoneNumber, thisTour.begin.endTime);
          TravelTimeAndCost returnCost = skims.setTravelTimeAndCost(chosenTaz.zoneNumber, 
               thisTour.end.location.zoneNumber, thisTour.primaryDestination.endTime);

         //Sets tour lengths
          thisTour.departDist = departCost.driveAloneDistance;
          thisTour.returnDist = returnCost.driveAloneDistance;
          thisTour.primaryDestination.distanceToActivity = departCost.driveAloneDistance;
          thisTour.end.distanceToActivity = returnCost.driveAloneDistance;

          //set mode choice taz attributes (only parking cost at this point)
          ZoneAttributes zone = new ZoneAttributes();
          zone.parkingCost=chosenTaz.nonWorkParkingCost;
          
          //set number of autos for this person to 0 if they didn't drive to work
          if(thisTour.driveToWork==false){
            personAttributes.autos=0;
            personAttributes.aupr0=1;
            personAttributes.aupr1=0;
            personAttributes.aupr2=0;
            personAttributes.auwk0=1;
            personAttributes.auwk1=0;
            personAttributes.auwk2=0;
          }
                   
          //mode choice model for chosen taz
          tmcm.calculateUtility(modeParams, departCost, returnCost, personAttributes, thisTour, zone);
               
          tmcm.chooseMode();
          chosenMode = tmcm.chosenMode;
          //set primaryDestination location zoneNumber
          thisTour.primaryDestination.location.zoneNumber=chosenTaz.zoneNumber;
          thisTour.primaryMode=chosenMode;

          thisTour.hasPrimaryMode=true;
          if(logger.isDebugEnabled() && thisHousehold.ID==debugID) {
              logger.debug("Logsum for household "+thisHousehold.ID+"="+logsum);
          }
     }


}


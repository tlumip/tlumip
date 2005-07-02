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
import com.pb.common.matrix.Matrix;
import com.pb.tlumip.model.Mode;
import com.pb.tlumip.model.SkimsInMemory;
import com.pb.tlumip.model.TravelTimeAndCost;
import com.pb.common.util.SeededRandom;
import org.apache.log4j.Logger;
import java.util.Enumeration;
import java.io.PrintWriter;

/** 
 * This model implements a logit model to choose a tour destination
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
*/

 
public class TourDestinationModeChoiceModel{
    
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.TourDestinationModeChoiceModel");

    LogitModel destinationModel;
    Taz chosenTaz;
    Mode chosenMode;

    PersonTourModeAttributes personAttributes = new PersonTourModeAttributes();
    boolean writtenOutTheUtilitiesAlready = false;


    public void buildModel(TazData tazs){ //passsing in PTModel.tazs and adding to logit model as alternatives
         destinationModel = new LogitModel("destinationModel",tazs.tazData.size());
         Enumeration destinationEnum=tazs.tazData.elements();
         while(destinationEnum.hasMoreElements()){
             Taz destinationTaz = (Taz) destinationEnum.nextElement();
             destinationModel.addAlternative(destinationTaz);
         }           
    }

    //passing in PTModel.tazs and using the method copy to calculate utilities
    public void calculateDestinationZone(PTHousehold thisHousehold, PTPerson thisPerson, Tour thisTour,
                                          SkimsInMemory skims, TourModeParametersData tmpd, TazData tazs,
                                          DCExpUtilitiesManager um, TourModeChoiceModel tmcm){

        chosenTaz=null;
        short activityPurpose = thisTour.primaryDestination.activityPurpose;

        if (activityPurpose !=ActivityPurpose.WORK && activityPurpose !=ActivityPurpose.WORK_BASED) { //choose destination zone

            //first figure out what purpose segment we are using so that we can pull the correct
            //expUtilities matrix
            int purposeSegment=0;
            if(activityPurpose==ActivityPurpose.SCHOOL && thisPerson.age<=18) purposeSegment=1;
            else if(activityPurpose==ActivityPurpose.SCHOOL && thisPerson.age>18) purposeSegment=3;
            Matrix expUtilities = um.getMatrix(activityPurpose,purposeSegment);

            //some debug logging statements used when things go wrong.
            if(!writtenOutTheUtilitiesAlready && logger.isDebugEnabled()) {
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

            if(chosenTaz==null){  //no destination zone could be found.  We have a real problem so write out debug info
                logger.error("Error in tour destination choice: no zones available for this household, tour");
                logger.error("Household "+thisHousehold.ID+" Person "+thisPerson.ID+" Tour "+thisTour.tourNumber);
                logger.error("A Primary Destination Tour file and the TAZ info will be written out to the debug directory." +
                        "\nYou should also look at "+ expUtilities.getName()+" located in the pt directory " +
                        "for the current year");

                //write the tour information into a debug file.  Path is specified in the pt.properties file
                logger.error("Writing Tour Debug info to the debug directory");
                PrintWriter file = PTResults.createTourDebugFile("HH" + thisHousehold.ID + "Tour" + thisTour.tourNumber+"PrimaryDestination.csv");
                thisTour.printCSV(file);
                file.close();

                // if not already done, write the taz info into a debug file.  Path is specified in pt.properties
                PrintWriter file2 = PTResults.createTazDebugFile("TazInfo.csv");
                if(file2 != null){  //if it is null that means an earlier problem caused this file to be written already and there
                                       //is no reason to write it out twice
                    logger.error("Writing out Taz Info because the primary destination on this tour couldn't find a destination zone");
                    tazEnum = tazs.tazData.elements();
                    for(int i=0;i<tazs.tazData.size();++i){
                        Taz taz = (Taz)tazEnum.nextElement();
                        taz.printCSV(file2);
                    }
                    file2.close();

                }
                //We need to do something here so that the program can continue but somehow mark this tour as
                //pathological.  Right now I am just letting the chosenTaz be the taz where the tour is starting from.
                chosenTaz = (Taz) tazs.tazData.get(new Integer(thisTour.begin.location.zoneNumber));
            }
            //set the primaryDestination zone number.
            thisTour.primaryDestination.location.zoneNumber = chosenTaz.zoneNumber;

        }else {//the primary destination is work (or work-based) and therefore the chosenTaz is the workplace location
            chosenTaz = (Taz)tazs.tazData.get(new Integer(thisTour.primaryDestination.location.zoneNumber));
            if(chosenTaz == null){
                logger.error("Work (or Work-based) tour has a primary destination of 0 - not right!!");
                logger.error("HHID: " + thisHousehold.ID + " PersonID: " + thisPerson.ID);
                chosenTaz = (Taz) tazs.tazData.get(new Integer(thisTour.begin.location.zoneNumber));;  
            }
        }

        //Now we need to choose the mode.

        chosenMode=null;

        //set time and cost parameters for mode choice
        TravelTimeAndCost departCost = skims.setTravelTimeAndCost(thisTour.begin.location.zoneNumber,
        thisTour.primaryDestination.location.zoneNumber, thisTour.begin.endTime);
        TravelTimeAndCost returnCost = skims.setTravelTimeAndCost(thisTour.primaryDestination.location.zoneNumber,
               thisTour.end.location.zoneNumber, thisTour.primaryDestination.endTime);
        TourModeParameters modeParams = tmpd.getTourModeParameters(activityPurpose);

        //set mode choice person attributes
        personAttributes.setAttributes(thisHousehold,thisPerson,thisTour);

        //Sets tour lengths
        thisTour.departDist = departCost.driveAloneDistance;
        thisTour.returnDist = returnCost.driveAloneDistance;
        thisTour.primaryDestination.distanceToActivity = departCost.driveAloneDistance;
        thisTour.end.distanceToActivity = returnCost.driveAloneDistance;
          
        //set mode choice taz attributes (only parking cost at this point)
        ZoneAttributes zone = new ZoneAttributes();
        if(activityPurpose == ActivityPurpose.WORK || activityPurpose == ActivityPurpose.WORK_BASED)
            zone.parkingCost=chosenTaz.workParkingCost;
        else
            zone.parkingCost=chosenTaz.nonWorkParkingCost;
            
        //mode choice model for this taz
        tmcm.calculateUtility(modeParams,departCost,returnCost,personAttributes,thisTour,zone);
        tmcm.chooseMode();
        chosenMode = tmcm.chosenMode;

        if(chosenMode == null) logger.error("The chosen mode is null");

        thisTour.primaryMode=chosenMode;
        thisTour.hasPrimaryMode=true;


    }//end of choosing destination zone

}


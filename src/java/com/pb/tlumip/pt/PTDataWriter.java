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


import org.apache.log4j.Logger;
import java.io.PrintWriter;


/** 
 *  PTDataWriter.java
 *  writes PT data to output device
 * 
 *  @author Joel Freedman
 *  @version 1.0 October 2002
 *
 */

public class PTDataWriter {

     final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
     public PTDataWriter(){
          
     }

     public void writeToursToTextFile(PTHousehold[] households, PrintWriter oFile,
                                      boolean weekday){
                    
          //open file for writing

          
          for(int hhNumber=0;hhNumber<households.length;++hhNumber){
               PTHousehold thisHousehold = households[hhNumber];
               for(int personNumber=0;personNumber<households[hhNumber].persons.length;++personNumber){
                  PTPerson thisPerson = households[hhNumber].persons[personNumber];
                  
                  if(weekday){
                  
                    //print weekday home-based tours first
                    for(int tourNumber=0;tourNumber<thisPerson.weekdayTours.length;++tourNumber){
                         Tour thisTour = thisPerson.weekdayTours[tourNumber];
                         
                         oFile.print(thisHousehold.ID+",");
                         oFile.print(thisPerson.ID+",");
                         oFile.print(thisPerson.age+",");
                         oFile.print("1,"); //weekdayTour=1 (TRUE)
                         thisTour.printCSV(oFile);
                         oFile.println();
                         
                    }

                    //print weekday work-based tours next
                    for(int tourNumber=0;tourNumber<thisPerson.weekdayWorkBasedTours.length;++tourNumber){
                         Tour thisTour = thisPerson.weekdayWorkBasedTours[tourNumber];
                        
                         oFile.print(thisHousehold.ID+",");
                         oFile.print(thisPerson.ID+",");
                         oFile.print(thisPerson.age+",");
                         oFile.print("1,"); //weekdayTour=1 (TRUE)
                         thisTour.printCSV(oFile);
                         oFile.println();
                        
                    }
                  }
                  else{
                  
                    //print weekend home-based tours next
                    for(int tourNumber=0;tourNumber<thisPerson.weekendTours.length;++tourNumber){
                         Tour thisTour = thisPerson.weekendTours[tourNumber];
                        
                         oFile.print(thisHousehold.ID+",");
                         oFile.print(thisPerson.ID+",");
                         oFile.print(thisPerson.age+",");
                         oFile.print("0,"); //weekdayTour=0 (FALSE)
                         thisTour.printCSV(oFile);
                         oFile.println();
                        
                    }

                    //print weekend work-based tours next
                    for(int tourNumber=0;tourNumber<thisPerson.weekendWorkBasedTours.length;++tourNumber){
                         Tour thisTour = thisPerson.weekendWorkBasedTours[tourNumber];
                         
                         oFile.print(thisHousehold.ID+",");
                         oFile.print(thisPerson.ID+",");
                         oFile.print(thisPerson.age+",");
                         oFile.print("0,"); //weekdayTour=0 (FALSE)
                         thisTour.printCSV(oFile);
                         oFile.println();
                        
                    }
                  }
               }//end persons
          }//end households
     }
    
    /**
     * writeWeekdayTripsToFile
     *
     * Outputs a trip table with the following format:
     * "hhID,personID,tour#,tourPurpose,tourMode,origin,destination,distance,time,tripStartTime,tripPurpose,tripMode"
     * Origin TAZ
     * Trip Start Time
     * Trip Travel Distance
     * Trip Travel Time
     * Destination TAZ
     * Tour Mode (See ModeType.java for codes)
     * Trip Mode (See ModeType.java for codes)
     * 
     */
    
    public void writeWeekdayTripsToFile(PTHousehold[] households, PrintWriter oFile){
        logger.info("Writing weekday trips to file.");
          
         for(int hhNumber=0;hhNumber<households.length;++hhNumber){
              for(int personNumber=0;personNumber<households[hhNumber].persons.length;++personNumber){
                   PTPerson thisPerson = households[hhNumber].persons[personNumber];
                   //print weekday home-based tours first
                   for(int tourNumber=0;tourNumber<thisPerson.weekdayTours.length;++tourNumber){
                       Tour thisTour = thisPerson.weekdayTours[tourNumber];
                        if(thisTour.intermediateStop1!=null){
                            oFile.print(households[hhNumber].ID+",");
                            oFile.print(thisPerson.ID + ",");
                            oFile.print("1,"); //weekdayTour=1 (TRUE)
                            oFile.print(thisTour.tourNumber+",");
                            oFile.print("0,"); //denotes that this is a sub-tour taken as the work-based tour.
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'c'){
                                if(thisPerson.age <= 18) oFile.print("1,");  //indicates a K-12 school tour
                                else oFile.print("3,"); //indicates a college school tour
                            }else if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'w'){
                                oFile.print(thisPerson.occupation+",");
                            }else oFile.print("0,"); //no segment associated with this tour
                            oFile.print(thisTour.primaryMode.type+",");
                            oFile.print(thisTour.begin.location.zoneNumber+",");
                            oFile.print(thisTour.intermediateStop1.location.zoneNumber+",");
                            oFile.print(thisTour.intermediateStop1.distanceToActivity+",");
                            oFile.print(thisTour.intermediateStop1.timeToActivity+",");
                            oFile.print(thisTour.begin.endTime+",");
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.intermediateStop1.activityPurpose)+",");
                            oFile.println(thisTour.intermediateStop1.tripMode.type);

                            oFile.print(households[hhNumber].ID+",");
                            oFile.print(thisPerson.ID + ",");
                            oFile.print("1,"); //weekdayTour=1 (TRUE)
                            oFile.print(thisTour.tourNumber+",");
                            oFile.print("0,"); //denotes that this is a sub-tour taken as the work-based tour.
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'c'){
                                if(thisPerson.age <= 18) oFile.print("1,");  //indicates a K-12 school tour
                                else oFile.print("3,"); //indicates a college school tour
                            }else if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'w'){
                                oFile.print(thisPerson.occupation+",");
                            }else oFile.print("0,"); //no segment associated with this tour
                            oFile.print(thisTour.primaryMode.type+",");
                            oFile.print(thisTour.intermediateStop1.location.zoneNumber+",");
                            oFile.print(thisTour.primaryDestination.location.zoneNumber+",");
                            oFile.print(thisTour.primaryDestination.distanceToActivity+",");
                            oFile.print(thisTour.primaryDestination.timeToActivity+",");
                            oFile.print(thisTour.intermediateStop1.endTime+",");
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            oFile.println(thisTour.primaryDestination.tripMode.type);
                        }
                        else{
                            oFile.print(households[hhNumber].ID+",");
                            oFile.print(thisPerson.ID + ",");
                            oFile.print("1,"); //weekdayTour=1 (TRUE)
                            oFile.print(thisTour.tourNumber+",");
                            oFile.print("0,"); //denotes that this is a sub-tour taken as the work-based tour.
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'c'){
                                if(thisPerson.age <= 18) oFile.print("1,");  //indicates a K-12 school tour
                                else oFile.print("3,"); //indicates a college school tour
                            }else if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'w'){
                                oFile.print(thisPerson.occupation+",");
                            }else oFile.print("0,"); //no segment associated with this tour
                            oFile.print(thisTour.primaryMode.type+",");
                            oFile.print(thisTour.begin.location.zoneNumber+",");
                            oFile.print(thisTour.primaryDestination.location.zoneNumber+",");
                            oFile.print(thisTour.primaryDestination.distanceToActivity+",");
                            oFile.print(thisTour.primaryDestination.timeToActivity+",");
                            oFile.print(thisTour.begin.endTime+",");
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            oFile.println(thisTour.primaryDestination.tripMode.type);
                        }

                        if(thisTour.intermediateStop2!=null){
                            oFile.print(households[hhNumber].ID+",");
                            oFile.print(thisPerson.ID + ",");
                            oFile.print("1,"); //weekdayTour=1 (TRUE)
                            oFile.print(thisTour.tourNumber+",");
                            oFile.print("0,"); //denotes that this is a sub-tour taken as the work-based tour.
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'c'){
                                if(thisPerson.age <= 18) oFile.print("1,");  //indicates a K-12 school tour
                                else oFile.print("3,"); //indicates a college school tour
                            }else if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'w'){
                                oFile.print(thisPerson.occupation+",");
                            }else oFile.print("0,"); //no segment associated with this tour
                            oFile.print(thisTour.primaryMode.type+",");
                            oFile.print(thisTour.primaryDestination.location.zoneNumber+",");
                            oFile.print(thisTour.intermediateStop2.location.zoneNumber+",");
                            oFile.print(thisTour.intermediateStop2.distanceToActivity+",");
                            oFile.print(thisTour.intermediateStop2.timeToActivity+",");
                            oFile.print(thisTour.primaryDestination.endTime+",");
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.intermediateStop2.activityPurpose)+",");
                            oFile.println(thisTour.intermediateStop2.tripMode.type);

                            oFile.print(households[hhNumber].ID+",");
                            oFile.print(thisPerson.ID + ",");
                            oFile.print("1,"); //weekdayTour=1 (TRUE)
                            oFile.print(thisTour.tourNumber+",");
                            oFile.print("0,"); //denotes that this is a sub-tour taken as the work-based tour.
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'c'){
                                if(thisPerson.age <= 18) oFile.print("1,");  //indicates a K-12 school tour
                                else oFile.print("3,"); //indicates a college school tour
                            }else if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'w'){
                                oFile.print(thisPerson.occupation+",");
                            }else oFile.print("0,"); //no segment associated with this tour
                            oFile.print(thisTour.primaryMode.type+",");
                            oFile.print(thisTour.intermediateStop2.location.zoneNumber+",");
                            oFile.print(thisTour.end.location.zoneNumber+",");
                            oFile.print(thisTour.end.distanceToActivity+",");
                            oFile.print(thisTour.end.timeToActivity+",");
                            oFile.print(thisTour.intermediateStop2.endTime+",");
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.end.activityPurpose)+",");
                            oFile.println(thisTour.end.tripMode.type);
                        }
                        else{
                            oFile.print(households[hhNumber].ID+",");
                            oFile.print(thisPerson.ID + ",");
                            oFile.print("1,"); //weekdayTour=1 (TRUE)
                            oFile.print(thisTour.tourNumber+",");
                            oFile.print("0,"); //denotes that this is a sub-tour taken as the work-based tour.
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'c'){
                                if(thisPerson.age <= 18) oFile.print("1,");  //indicates a K-12 school tour
                                else oFile.print("3,"); //indicates a college school tour
                            }else if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'w'){
                                oFile.print(thisPerson.occupation+",");
                            }else oFile.print("0,"); //no segment associated with this tour
                            oFile.print(thisTour.primaryMode.type+",");
                            oFile.print(thisTour.primaryDestination.location.zoneNumber+",");
                            oFile.print(thisTour.end.location.zoneNumber+",");
                            oFile.print(thisTour.end.distanceToActivity+",");
                            oFile.print(thisTour.end.timeToActivity+",");
                            oFile.print(thisTour.primaryDestination.endTime+",");
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.end.activityPurpose)+",");
                            oFile.println(thisTour.end.tripMode.type);
                        }
                         
                    }
                  //Now print the weekday work based tours - if there are any.
                  if (thisPerson.weekdayWorkBasedTours != null) {
                      for(int tourNumber=0;tourNumber<thisPerson.weekdayWorkBasedTours.length;++tourNumber){
                          Tour thisTour = thisPerson.weekdayWorkBasedTours[tourNumber];
                          oFile.print(households[hhNumber].ID+",");
                          oFile.print(thisPerson.ID + ",");
                          oFile.print("1,"); //weekdayTour=1 (TRUE)
                          oFile.print(thisTour.tourNumber+",");  //unique tour number
                          oFile.print("1,"); //denotes that this is a sub-tour and is necessary for accounting.
                          oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                          oFile.print(thisPerson.occupation+",");
                          oFile.print(thisTour.primaryMode.type+",");
                          oFile.print(thisTour.begin.location.zoneNumber+",");
                          oFile.print(thisTour.primaryDestination.location.zoneNumber+",");
                          oFile.print(thisTour.primaryDestination.distanceToActivity+",");
                          oFile.print(thisTour.primaryDestination.timeToActivity+",");
                          oFile.print(thisTour.begin.endTime+",");
                          oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                          //we currently do not calculate a trip mode so we will get a null pointer if
                          //we use the line that is commented out.  So I am just printing out the tour mode as if
                          //it were the trip mode (which it might be for all I know)
//                          oFile.println(thisTour.primaryMode.type);
                          oFile.println(thisTour.primaryDestination.tripMode.type);

                          oFile.print(households[hhNumber].ID+",");
                          oFile.print(thisPerson.ID + ",");
                          oFile.print("1,"); //weekdayTour=1 (TRUE)
                          oFile.print(thisTour.tourNumber+",");
                          oFile.print("1,"); //denotes that this is a sub-tour and is necessary for accounting.
                          oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                          oFile.print(thisPerson.occupation+",");
                          oFile.print(thisTour.primaryMode.type+",");
                          oFile.print(thisTour.primaryDestination.location.zoneNumber+",");
                          oFile.print(thisTour.end.location.zoneNumber+",");
                          oFile.print(thisTour.end.distanceToActivity+",");
                          oFile.print(thisTour.end.timeToActivity+",");
                          oFile.print(thisTour.primaryDestination.endTime+",");
                          oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.end.activityPurpose)+",");
                          //we currently do not calculate a trip mode so we will get a null pointer if
                          //we use the line that is commented out.  So I am just printing out the tour mode as if
                          //it were the trip mode (which it might be for all I know)
//                          oFile.println(thisTour.primaryMode.type);
                          oFile.println(thisPerson.weekdayTours[tourNumber].end.tripMode.type);
                      }
                  }
              }//end persons
         }//end households
         logger.info("Trips written to file.");
    }

    /**
     * writeWeekdayTripsToFile
     *
     * Outputs a trip table with the following format:
     * Origin TAZ
     * Trip Start Time
     * Trip Travel Time
     * Destination TAZ
     * Tour Mode (See ModeType.java for codes)
     * Trip Mode (See ModeTypr.java for codes)
     * 
     */
    
    public void writeWeekendTripsToFile(PTHousehold[] households, PrintWriter oFile){
        for(int hhNumber=0;hhNumber<households.length;++hhNumber){
              for(int personNumber=0;personNumber<households[hhNumber].persons.length;++personNumber){
                   PTPerson thisPerson = households[hhNumber].persons[personNumber];
                   //print weekend tours
                   for(int tourNumber=0;tourNumber<thisPerson.weekdayTours.length;++tourNumber){
                       Tour thisTour = thisPerson.weekendTours[tourNumber];
                        if(thisTour.intermediateStop1!=null){
                            oFile.print(households[hhNumber].ID+",");
                            oFile.print(thisPerson.ID + ",");
                            oFile.print("0,"); //weekdayTour=0 (FALSE)
                            oFile.print(thisTour.tourNumber+",");
                            oFile.print("0,"); //denotes that this is a sub-tour taken as the work-based tour.
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'c'){
                                if(thisPerson.age <= 18) oFile.print("1,");  //indicates a K-12 school tour
                                else oFile.print("3,"); //indicates a college school tour
                            }else if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'w'){
                                oFile.print(thisPerson.occupation+",");
                            }else oFile.print("0,"); //no segment associated with this tour
                            oFile.print(thisTour.primaryMode.type+",");
                            oFile.print(thisTour.begin.location.zoneNumber+",");
                            oFile.print(thisTour.intermediateStop1.location.zoneNumber+",");
                            oFile.print(thisTour.intermediateStop1.distanceToActivity+",");
                            oFile.print(thisTour.intermediateStop1.timeToActivity+",");
                            oFile.print(thisTour.begin.endTime+",");
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.intermediateStop1.activityPurpose)+",");
                            oFile.println(thisTour.intermediateStop1.tripMode.type);

                            oFile.print(households[hhNumber].ID+",");
                            oFile.print(thisPerson.ID + ",");
                            oFile.print("0,"); //weekdayTour=0 (FALSE)
                            oFile.print(thisTour.tourNumber+",");
                            oFile.print("0,"); //denotes that this is a sub-tour taken as the work-based tour.
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'c'){
                                if(thisPerson.age <= 18) oFile.print("1,");  //indicates a K-12 school tour
                                else oFile.print("3,"); //indicates a college school tour
                            }else if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'w'){
                                oFile.print(thisPerson.occupation+",");
                            }else oFile.print("0,"); //no segment associated with this tour
                            oFile.print(thisTour.primaryMode.type+",");
                            oFile.print(thisTour.intermediateStop1.location.zoneNumber+",");
                            oFile.print(thisTour.primaryDestination.location.zoneNumber+",");
                            oFile.print(thisTour.primaryDestination.distanceToActivity+",");
                            oFile.print(thisTour.primaryDestination.timeToActivity+",");
                            oFile.print(thisTour.intermediateStop1.endTime+",");
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            oFile.println(thisTour.primaryDestination.tripMode.type);
                        }
                        else{
                            oFile.print(households[hhNumber].ID+",");
                            oFile.print(thisPerson.ID + ",");
                            oFile.print("0,"); //weekdayTour=0 (FALSE)
                            oFile.print(thisTour.tourNumber+",");
                            oFile.print("0,"); //denotes that this is a sub-tour taken as the work-based tour.
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'c'){
                                if(thisPerson.age <= 18) oFile.print("1,");  //indicates a K-12 school tour
                                else oFile.print("3,"); //indicates a college school tour
                            }else if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'w'){
                                oFile.print(thisPerson.occupation+",");
                            }else oFile.print("0,"); //no segment associated with this tour
                            oFile.print(thisTour.primaryMode.type+",");
                            oFile.print(thisTour.begin.location.zoneNumber+",");
                            oFile.print(thisTour.primaryDestination.location.zoneNumber+",");
                            oFile.print(thisTour.primaryDestination.distanceToActivity+",");
                            oFile.print(thisTour.primaryDestination.timeToActivity+",");
                            oFile.print(thisTour.begin.endTime+",");
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            oFile.println(thisTour.primaryDestination.tripMode.type);
                        }

                        if(thisTour.intermediateStop2!=null){
                            oFile.print(households[hhNumber].ID+",");
                            oFile.print(thisPerson.ID + ",");
                            oFile.print("0,"); //weekdayTour=0 (FALSE)
                            oFile.print(thisTour.tourNumber+",");
                            oFile.print("0,"); //denotes that this is a sub-tour taken as the work-based tour.
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'c'){
                                if(thisPerson.age <= 18) oFile.print("1,");  //indicates a K-12 school tour
                                else oFile.print("3,"); //indicates a college school tour
                            }else if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'w'){
                                oFile.print(thisPerson.occupation+",");
                            }else oFile.print("0,"); //no segment associated with this tour
                            oFile.print(thisTour.primaryMode.type+",");
                            oFile.print(thisTour.primaryDestination.location.zoneNumber+",");
                            oFile.print(thisTour.intermediateStop2.location.zoneNumber+",");
                            oFile.print(thisTour.intermediateStop2.distanceToActivity+",");
                            oFile.print(thisTour.intermediateStop2.timeToActivity+",");
                            oFile.print(thisTour.primaryDestination.endTime+",");
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.intermediateStop2.activityPurpose)+",");
                            oFile.println(thisTour.intermediateStop2.tripMode.type);

                            oFile.print(households[hhNumber].ID+",");
                            oFile.print(thisPerson.ID + ",");
                            oFile.print("0,"); //weekdayTour=0 (FALSE)
                            oFile.print(thisTour.tourNumber+",");
                            oFile.print("0,"); //denotes that this is a sub-tour taken as the work-based tour.
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'c'){
                                if(thisPerson.age <= 18) oFile.print("1,");  //indicates a K-12 school tour
                                else oFile.print("3,"); //indicates a college school tour
                            }else if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'w'){
                                oFile.print(thisPerson.occupation+",");
                            }else oFile.print("0,"); //no segment associated with this tour
                            oFile.print(thisTour.primaryMode.type+",");
                            oFile.print(thisTour.intermediateStop2.location.zoneNumber+",");
                            oFile.print(thisTour.end.location.zoneNumber+",");
                            oFile.print(thisTour.end.distanceToActivity+",");
                            oFile.print(thisTour.end.timeToActivity+",");
                            oFile.print(thisTour.intermediateStop2.endTime+",");
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.end.activityPurpose)+",");
                            oFile.println(thisTour.end.tripMode.type);
                        }
                        else{
                            oFile.print(households[hhNumber].ID+",");
                            oFile.print(thisPerson.ID + ",");
                            oFile.print("0,"); //weekdayTour=0 (FALSE)
                            oFile.print(thisTour.tourNumber+",");
                            oFile.print("0,"); //denotes that this is a sub-tour taken as the work-based tour.
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose)+",");
                            if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'c'){
                                if(thisPerson.age <= 18) oFile.print("1,");  //indicates a K-12 school tour
                                else oFile.print("3,"); //indicates a college school tour
                            }else if(ActivityPurpose.getActivityPurposeChar(thisTour.primaryDestination.activityPurpose) == 'w'){
                                oFile.print(thisPerson.occupation+",");
                            }else oFile.print("0,"); //no segment associated with this tour
                            oFile.print(thisTour.primaryMode.type+",");
                            oFile.print(thisTour.primaryDestination.location.zoneNumber+",");
                            oFile.print(thisTour.end.location.zoneNumber+",");
                            oFile.print(thisTour.end.distanceToActivity+",");
                            oFile.print(thisTour.end.timeToActivity+",");
                            oFile.print(thisTour.primaryDestination.endTime+",");
                            oFile.print(ActivityPurpose.getActivityPurposeChar(thisTour.end.activityPurpose)+",");
                            oFile.println(thisTour.end.tripMode.type);
                        }

                    } //end tours
              }//end persons
         }//end households

    }
    
    public void writeWeekdayPatternsToFile(PTHousehold[] households,PrintWriter oFile){
          
        for(int hhNumber=0;hhNumber<households.length;++hhNumber){
            PTHousehold thisHousehold = households[hhNumber];
            for(int personNumber=0;personNumber<households[hhNumber].persons.length;++personNumber){
                PTPerson thisPerson = households[hhNumber].persons[personNumber];
                Pattern thisWeekdayPattern = households[hhNumber].persons[personNumber].weekdayPattern;
               
                oFile.print(thisHousehold.ID+","+
                            thisPerson.ID+","+
                            thisPerson.age+","+
                            "1," + //weekdayTour=1 (TRUE)
                            thisPerson.weekdayPatternLogsum+",");
                thisWeekdayPattern.printCSV(oFile);
                oFile.println();
               }
           }
    }

    public void writeWeekendPatternsToFile(PTHousehold[] households,PrintWriter oFile){
          
        for(int hhNumber=0;hhNumber<households.length;++hhNumber){
            PTHousehold thisHousehold = households[hhNumber];
            for(int personNumber=0;personNumber<households[hhNumber].persons.length;++personNumber){
                PTPerson thisPerson = households[hhNumber].persons[personNumber];
                Pattern thisWeekendPattern = households[hhNumber].persons[personNumber].weekendPattern;
                oFile.print(thisHousehold.ID+","+
                            thisPerson.ID+","+
                            thisPerson.age+","+
                            thisPerson.weekendPatternLogsum+",");
                thisWeekendPattern.printCSV(oFile);
                oFile.println();
               }
           }
    }
  /*  public void writeSkimsToTextFile(SkimsInMemory skims,String textFileName){
                    
         //open file for writing
         OutTextFile outFile = new OutTextFile();
         PrintWriter oFile = outFile.open(textFileName);
          
         for(int i=0;i<households.length;++hhNumber){
              PTHousehold thisHousehold = households[hhNumber];
              for(int personNumber=0;personNumber<households[hhNumber].persons.length;++personNumber){
                   PTPerson thisPerson = households[hhNumber].persons[personNumber];
                   Pattern thisWeekdayPattern = households[hhNumber].persons[personNumber].weekdayPattern;
                   Pattern thisWeekendPattern = households[hhNumber].persons[personNumber].weekendPattern;
                       oFile.print(thisHousehold.ID+","+
                                   thisPerson.ID+",");
                       thisWeekdayPattern.printCSV(oFile);
                       oFile.print(",");
                       thisWeekendPattern.printCSV(oFile);
                       oFile.println();
              }
          }
     }*/
}

package com.pb.despair.pt;
import com.pb.despair.model.ModeType;
import java.util.logging.Logger;
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
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
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
                         oFile.print("1,"); //weekdayTour=1 (TRUE)
                         thisTour.printCSV(oFile);
                         oFile.println();
                         
                    }

                    //print weekday work-based tours next
                    for(int tourNumber=0;tourNumber<thisPerson.weekdayWorkBasedTours.length;++tourNumber){
                         Tour thisTour = thisPerson.weekdayWorkBasedTours[tourNumber];
                        
                         oFile.print(thisHousehold.ID+",");
                         oFile.print(thisPerson.ID+",");
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
                         oFile.print("0,"); //weekdayTour=0 (FALSE)
                         thisTour.printCSV(oFile);
                         oFile.println();
                        
                    }

                    //print weekend work-based tours next
                    for(int tourNumber=0;tourNumber<thisPerson.weekendWorkBasedTours.length;++tourNumber){
                         Tour thisTour = thisPerson.weekendWorkBasedTours[tourNumber];
                         
                         oFile.print(thisHousehold.ID+",");
                         oFile.print(thisPerson.ID+",");
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
     * @author Hansen
     *
     * Outputs a trip table with the following format:
     * Origin TAZ
     * Trip Start Time
     * Trip Travel Time
     * Destination TAZ
     * Tour Mode (See ModeType.java for codes)
     * Trip Mode (See ModeType.java for codes)
     * 
     */
    
    public void writeWeekdayTripsToFile(PTHousehold[] households, PrintWriter oFile){
        //TODO Check that it is OK to print tripMode for non-auto trips
                 
        logger.info("Writing weekday trips to file.");
          
         for(int hhNumber=0;hhNumber<households.length;++hhNumber){
              PTHousehold thisHousehold = households[hhNumber];
              for(int personNumber=0;personNumber<households[hhNumber].persons.length;++personNumber){
                   PTPerson thisPerson = households[hhNumber].persons[personNumber];
                    
                   //print weekday home-based tours first
                   for(int tourNumber=0;tourNumber<thisPerson.weekdayTours.length;++tourNumber){
                       Tour thisTour = thisPerson.weekdayTours[tourNumber];
                            if(thisTour.intermediateStop1!=null){
                                oFile.print(thisPerson.weekdayTours[tourNumber].begin.location.zoneNumber+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].begin.endTime+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].intermediateStop1.distanceToActivity+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].intermediateStop1.location.zoneNumber+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryMode.type+",");
                                oFile.println(thisPerson.weekdayTours[tourNumber].intermediateStop1.tripMode.type);

                                oFile.print(thisPerson.weekdayTours[tourNumber].intermediateStop1.location.zoneNumber+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].intermediateStop1.endTime+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryDestination.distanceToActivity+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryDestination.location.zoneNumber+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryMode.type+",");
                                oFile.println(thisPerson.weekdayTours[tourNumber].primaryDestination.tripMode.type);
                            }
                            else{
                                oFile.print(thisPerson.weekdayTours[tourNumber].begin.location.zoneNumber+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].begin.endTime+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryDestination.distanceToActivity+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryDestination.location.zoneNumber+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryMode.type+",");
                              	oFile.println(thisPerson.weekdayTours[tourNumber].primaryDestination.tripMode.type);
                            }

                            if(thisTour.intermediateStop2!=null){
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryDestination.location.zoneNumber+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryDestination.endTime+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].intermediateStop2.distanceToActivity+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].intermediateStop2.location.zoneNumber+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryMode.type+",");
                                oFile.println(thisPerson.weekdayTours[tourNumber].intermediateStop2.tripMode.type);

                                oFile.print(thisPerson.weekdayTours[tourNumber].intermediateStop2.location.zoneNumber+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].intermediateStop2.endTime+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].end.distanceToActivity+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].end.location.zoneNumber+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryMode.type+",");
                                oFile.println(thisPerson.weekdayTours[tourNumber].end.tripMode.type);
                            }
                            else{
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryDestination.location.zoneNumber+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryDestination.endTime+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].end.distanceToActivity+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].end.location.zoneNumber+",");
                                oFile.print(thisPerson.weekdayTours[tourNumber].primaryMode.type+",");
                                oFile.println(thisPerson.weekdayTours[tourNumber].end.tripMode.type);
                            }
                         
                    }
              }//end persons
         }//end households
         logger.info("Trips written to file.");
    }

    /**
     * writeWeekdayTripsToFile
     * @author Hansen
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
        //TODO Check that it is OK to print tripMode for non-auto trips

          
         for(int hhNumber=0;hhNumber<households.length;++hhNumber){
              PTHousehold thisHousehold = households[hhNumber];
              for(int personNumber=0;personNumber<households[hhNumber].persons.length;++personNumber){
                   PTPerson thisPerson = households[hhNumber].persons[personNumber];
                    
                   //print weekday home-based tours first
                   for(int tourNumber=0;tourNumber<thisPerson.weekdayTours.length;++tourNumber){
                       Tour thisTour = thisPerson.weekendTours[tourNumber];
                       int tourLength = thisPerson.weekendTours[tourNumber].tourString.length();
                            if(thisTour.intermediateStop1!=null){
                                oFile.print(thisPerson.weekendTours[tourNumber].begin.location.zoneNumber+",");
                                oFile.print(thisPerson.weekendTours[tourNumber].begin.endTime+",");
                                //oFile.print(thisPerson.weekendTours[tourNumber].intermediateStop1.timeToActivity+",");    
                                oFile.print(thisPerson.weekendTours[tourNumber].intermediateStop1.location.zoneNumber+",");
                                oFile.print(thisPerson.weekendTours[tourNumber].primaryMode.type+",");
                                //oFile.print(thisPerson.weekendTours[tourNumber].intermediateStop1.tripMode.type+",");
                                oFile.println();
                                oFile.print(thisPerson.weekendTours[tourNumber].intermediateStop1.location.zoneNumber+",");
                                oFile.print(thisPerson.weekendTours[tourNumber].intermediateStop1.endTime+",");
                                //oFile.print(thisPerson.weekendTours[tourNumber].primaryDestination.timeToActivity+",");    
                                oFile.print(thisPerson.weekendTours[tourNumber].primaryDestination.location.zoneNumber+",");
                                oFile.print(thisPerson.weekendTours[tourNumber].primaryMode.type+",");
                                //oFile.print(thisPerson.weekendTours[tourNumber].primaryDestination.tripMode.type+",");
                                oFile.println();
                            }
                            else{
                                oFile.print(thisPerson.weekendTours[tourNumber].begin.location.zoneNumber+",");
                                oFile.print(thisPerson.weekendTours[tourNumber].begin.endTime+",");
                                //oFile.print(thisPerson.weekendTours[tourNumber].primaryDestination.timeToActivity+",");    
                                oFile.print(thisPerson.weekendTours[tourNumber].primaryDestination.location.zoneNumber+",");
                                oFile.print(thisPerson.weekendTours[tourNumber].primaryMode.type+",");
                                //oFile.print(thisPerson.weekendTours[tourNumber].primaryDestination.tripMode.type+",");
                                oFile.println();
                            }
                            if(thisTour.intermediateStop2!=null){
                                oFile.print(thisPerson.weekendTours[tourNumber].primaryDestination.location.zoneNumber+",");
                                oFile.print(thisPerson.weekendTours[tourNumber].primaryDestination.endTime+",");
                                //oFile.print(thisPerson.weekendTours[tourNumber].intermediateStop2.timeToActivity+",");    
                                oFile.print(thisPerson.weekendTours[tourNumber].intermediateStop2.location.zoneNumber+",");
                                oFile.print(thisPerson.weekendTours[tourNumber].primaryMode.type+",");
                                //oFile.print(thisPerson.weekendTours[tourNumber].intermediateStop2.tripMode.type+",");
                                oFile.println();
                                oFile.print(thisPerson.weekendTours[tourNumber].intermediateStop2.location.zoneNumber+",");
                                oFile.print(thisPerson.weekendTours[tourNumber].intermediateStop2.endTime+",");
                                //oFile.print(thisPerson.weekendTours[tourNumber].end.timeToActivity+",");    
                                oFile.print(thisPerson.weekendTours[tourNumber].end.location.zoneNumber+",");
                                oFile.print(thisPerson.weekendTours[tourNumber].primaryMode.type+",");
                                //oFile.print(thisPerson.weekendTours[tourNumber].end.tripMode.type+",");
                                oFile.println();
                            }
                            else{
                                oFile.print(thisPerson.weekendTours[tourNumber].primaryDestination.location.zoneNumber+",");
                                oFile.print(thisPerson.weekendTours[tourNumber].primaryDestination.endTime+",");
                                //oFile.print(thisPerson.weekendTours[tourNumber].end.timeToActivity+",");    
                                oFile.print(thisPerson.weekendTours[tourNumber].end.location.zoneNumber+",");
                                oFile.print(thisPerson.weekendTours[tourNumber].primaryMode.type+",");
                                //oFile.print(thisPerson.weekendTours[tourNumber].end.tripMode.type+",");
                                oFile.println();
                            }
                         
                    }
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

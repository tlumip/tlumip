/** 
 * PTModel.java
 * Implements the person transport component of TLUMIP model
 * 
 * @author Joel Freedman
 * @version 1.0 October 2001
 * 
 */

package com.pb.despair.pt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.model.ModelComponent;
import com.pb.despair.model.SkimsInMemory;

import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class PTModel extends ModelComponent implements Serializable{
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt");
    public static final boolean RUN_WEEKEND_MODEL = false;
    public TazData tazs;
    Patterns wkdayPatterns;
    Patterns wkendPatterns;
    public TourModeChoiceModel tmcm;
    public TourDestinationModeChoiceModel destinationModeChoiceModel;          
    public StopDestinationChoiceModel stopDestinationChoiceModel;
    public TripModeChoiceModel tripModeChoiceModel;
    public WorkBasedTourModel workBasedTourModel;
    public DurationModel durationModel;
    public PatternModel weekdayPatternModel;
    public PatternModel weekendPatternModel;
    ResourceBundle rb;
    boolean debug = false;

    public PTModel(){

    }

    //This constructor should no longer be called as
    //we are moving the TazData to the PTModelInputs class
    //to keep all of the inputs together. 9/7/04
    public PTModel(ResourceBundle rb){
      this.rb = rb;
      logger.info("Reading TazData into PTModel");
      tazs = new TazData();
      tazs.readData(rb,"tazData.file");

    }

public void buildLogitModels(){
        //create instance of Patterns for weekdays
        if(debug) logger.fine("Creating WeekdayPatterns Object");
        wkdayPatterns = new Patterns();
        wkdayPatterns.readData(rb,"weekdayPatterns.file");

        //create instance of Patterns for weekends
        if(debug) logger.fine("Creating WeekdayPatterns Object");
        wkendPatterns = new Patterns();
        wkendPatterns.readData(rb,"weekendPatterns.file");

        weekdayPatternModel = new PatternModel();
        weekdayPatternModel.buildModel(wkdayPatterns, PTModelInputs.wkdayParams);

        weekendPatternModel = new PatternModel();
        weekendPatternModel.buildModel(wkendPatterns,PTModelInputs.wkendParams);

        tmcm = new TourModeChoiceModel();

        //update local tazData object with the static PTModelInputs.tazs data
        Enumeration tazEnum = tazs.tazData.elements();
        while (tazEnum.hasMoreElements()) {
            Taz thisTaz = (Taz) tazEnum.nextElement();
            thisTaz.households = ((Taz)PTModelInputs.tazs.tazData.get(new Integer(thisTaz.zoneNumber))).households;
            thisTaz.otherSchoolOccupation = ((Taz)PTModelInputs.tazs.tazData.get(new Integer(thisTaz.zoneNumber))).otherSchoolOccupation;
            thisTaz.postSecondaryOccupation = ((Taz)PTModelInputs.tazs.tazData.get(new Integer(thisTaz.zoneNumber))).postSecondaryOccupation;
        }
        tazs.collapseEmployment(PTModelInputs.tdpd,PTModelInputs.sdpd);

        destinationModeChoiceModel = new TourDestinationModeChoiceModel();
        destinationModeChoiceModel.buildModel(tazs); //does not clone in this version

        stopDestinationChoiceModel = new StopDestinationChoiceModel();
        stopDestinationChoiceModel.buildModel(tazs);

        tripModeChoiceModel = new TripModeChoiceModel();

        workBasedTourModel = new WorkBasedTourModel();
        workBasedTourModel.buildModel(tazs);

        durationModel = new DurationModel();
    }
    /**
     * Choose a weekday pattern for each household
     * @param households An array of PTHousehold objects
     */
    public PTHousehold[] runWeekdayPatternModel(PTHousehold[] households){    
        long startTime = System.currentTimeMillis();

          
        for(int hhNumber=0;hhNumber<households.length;++hhNumber){
            PTHousehold thisHousehold = (PTHousehold)households[hhNumber];
               
            if(hhNumber==0||hhNumber==households.length-1||hhNumber % 10000==0){                                     
                if(debug) logger.fine("Creating Pattern for hhNumber: "+hhNumber);
            }         
                
            //set dc Logsums for all persons in hh
            //Set the household logsum attributes with logsum values stored in the PTModelInputs.dcLogsums
            //data member.
            PTModelInputs.dcLogsums.setDCLogsums(thisHousehold);
            for(int persNumber=0;persNumber<thisHousehold.persons.length;++persNumber){
                PTPerson thisPerson = thisHousehold.persons[persNumber];
                    
                thisPerson.weekdayPattern=weekdayPatternModel.choosePattern(thisHousehold,
                                                                     thisPerson,
                                                                     wkdayPatterns,
                                                /*weekday boolean*/   true);
                                                                                

            }
        }
        if(debug) logger.fine("Elapsed time to create patterns: "+(System.currentTimeMillis()-startTime)/1000);
            
        return households;            
    }
    
    /**
     * Choose a weekend pattern for each household
     * @param households An array of PTHousehold objects
     */
    public PTHousehold[] runWeekendPatternModel(PTHousehold[] households){    
             
        long startTime = System.currentTimeMillis();
 
          
        for(int hhNumber=0;hhNumber<households.length;++hhNumber){
            PTHousehold thisHousehold = (PTHousehold)households[hhNumber];
               
            if(hhNumber==0||hhNumber==households.length-1||hhNumber % 1000==0){                                     
                if(debug) logger.info("Creating Pattern for hhNumber: "+hhNumber);
            }         
                
            //set dc Logsums for all persons in hh
            PTModelInputs.dcLogsums.setDCLogsums(thisHousehold);               
            for(int persNumber=0;persNumber<thisHousehold.persons.length;++persNumber){
                PTPerson thisPerson = thisHousehold.persons[persNumber];
                    
                thisPerson.weekendPattern=weekendPatternModel.choosePattern(thisHousehold,
                                                                     thisPerson,
                                                                     wkendPatterns,
                                                /*weekday boolean*/   false);
                                                                                

            }
        }
        if(debug) logger.info("Elapsed time to create patterns: "+(System.currentTimeMillis()-startTime)/1000);
            
        return households;            
    }

    /**
     * Generate tours from the patterns chosen for each household
     * @param households  An array of PTHouseholds
     */
    public PTHousehold[] generateWeekdayTours(PTHousehold[] households){
            //generate tours for each person based on pattern, set home and work tazs
            for(int hhNumber=0;hhNumber<households.length;++hhNumber){
                PTHousehold thisHousehold = (PTHousehold) households[hhNumber];
               
                for(int personNumber=0;personNumber<thisHousehold.persons.length;++personNumber){                    
                    PTPerson thisPerson = thisHousehold.persons[personNumber];
                    
                    Pattern wkdayPattern = thisPerson.weekdayPattern;
                    
                    thisPerson.weekdayTours = new Tour[(wkdayPattern.homeActivities-1)];
                    thisPerson.weekdayWorkBasedTours = new Tour[(thisPerson.weekdayPattern.workBasedTours)];
                    int job=0;
                    //weekday tours
                    for(int tourNumber=0;tourNumber<thisHousehold.persons[personNumber].weekdayTours.length;++tourNumber){                                               
                        thisHousehold.persons[personNumber].weekdayTours[tourNumber]  = new Tour(wkdayPattern.getTourString(tourNumber+1));
                        Tour thisWeekdayTour = thisHousehold.persons[personNumber].weekdayTours[tourNumber];
                         
                        thisWeekdayTour.tourNumber=tourNumber+1;
                        thisWeekdayTour.tourString = wkdayPattern.getTourString(tourNumber+1);                      
                        //set taz numbers, first for home activities
                        if(thisWeekdayTour.tourString.charAt(0)==ActivityPurpose.ACTIVITY_PURPOSE[ActivityPurpose.HOME])
                            thisWeekdayTour.begin.location.zoneNumber=thisHousehold.homeTaz;
                        if(thisWeekdayTour.tourString.charAt(
                            thisWeekdayTour.tourString.length()-1)==ActivityPurpose.ACTIVITY_PURPOSE[ActivityPurpose.HOME])
                              thisWeekdayTour.end.location.zoneNumber=thisHousehold.homeTaz;

                        //now for work activities
                        if(((thisWeekdayTour.primaryDestination.activityPurpose==ActivityPurpose.WORK)||
                            (thisWeekdayTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED)) && job<2){
                              ++job;
                              thisWeekdayTour.primaryDestination.location.zoneNumber
                                   =thisHousehold.persons[personNumber].workTaz;
                            if(thisWeekdayTour.primaryDestination.location.zoneNumber == 0){
                                logger.severe("the primary destination of this work tour (" + thisWeekdayTour.tourString + ") tour is 0 - this should not be");
                                logger.severe("tour pd activity purpose: " + thisWeekdayTour.primaryDestination.activityPurpose);
                                logger.severe("activity purpose work: " + ActivityPurpose.WORK);
                                logger.severe("activity purpose work cast as short " + (short)ActivityPurpose.WORK);
                                logger.severe("test: " + (thisWeekdayTour.primaryDestination.activityPurpose==ActivityPurpose.WORK));
                                thisPerson.print();
                                thisWeekdayTour.print(thisWeekdayTour);
                            }
                        }
                        if(((thisWeekdayTour.primaryDestination.activityPurpose==ActivityPurpose.WORK)||
                            (thisWeekdayTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED)) && job>=2){
                              ++job;
                              thisWeekdayTour.primaryDestination.location.zoneNumber
                                   =thisHousehold.persons[personNumber].workTaz2;
                        }

                    }

                }//end persons for loop    
            }//end households for loop
            return households;          
    }//end generate tours

    /**
     * Generate weekend tours from the patterns chosen for each household
     * @param households  An array of PTHouseholds
     */
    public PTHousehold[] generateWeekendTours(PTHousehold[] households){
            logger.info("Generating Tours based on Weekday,Weekend Patterns");
            //generate tours for each person based on pattern, set home and work tazs
            for(int hhNumber=0;hhNumber<households.length;++hhNumber){
                PTHousehold thisHousehold = (PTHousehold) households[hhNumber];
               
                for(int personNumber=0;personNumber<thisHousehold.persons.length;++personNumber){                    
                    PTPerson thisPerson = thisHousehold.persons[personNumber];
                    
                    Pattern wkendPattern = thisPerson.weekendPattern;
                    
                    thisPerson.weekendTours = new Tour[(wkendPattern.homeActivities-1)];
                    thisPerson.weekendWorkBasedTours = new Tour[(thisPerson.weekendPattern.workBasedTours)];
                    int job=0;

                    //weekend tours
                    for(int tourNumber=0;tourNumber<thisHousehold.persons[personNumber].weekendTours.length;++tourNumber){
                        thisHousehold.persons[personNumber].weekendTours[tourNumber]  = new Tour(wkendPattern.getTourString(tourNumber+1));                         
                        Tour thisWeekendTour = thisHousehold.persons[personNumber].weekendTours[tourNumber];
                         
                        thisWeekendTour.tourNumber=tourNumber+1;
                        thisWeekendTour.tourString = wkendPattern.getTourString(tourNumber+1);                      
                         
                        //set taz numbers, first for home activities
                        if(thisWeekendTour.tourString.charAt(0)==ActivityPurpose.ACTIVITY_PURPOSE[ActivityPurpose.HOME])
                              thisWeekendTour.begin.location.zoneNumber=thisHousehold.homeTaz;
                        if(thisWeekendTour.tourString.charAt(
                            thisWeekendTour.tourString.length()-1)==ActivityPurpose.ACTIVITY_PURPOSE[ActivityPurpose.HOME])
                              thisWeekendTour.end.location.zoneNumber=thisHousehold.homeTaz;

                         //now for work activities
                        if((thisWeekendTour.primaryDestination.activityPurpose==ActivityPurpose.WORK)||
                            (thisWeekendTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED)&& job<2){
                              ++job;
                              thisWeekendTour.primaryDestination.location.zoneNumber
                                   =thisHousehold.persons[personNumber].workTaz;
                        }
                        if((thisWeekendTour.primaryDestination.activityPurpose==ActivityPurpose.WORK)||
                            (thisWeekendTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED)&& job>=2){
                              ++job;
                              thisWeekendTour.primaryDestination.location.zoneNumber
                                   =thisHousehold.persons[personNumber].workTaz2;
                        }                         
                    }//end weekend for loop 
                }//end persons for loop    
            }//end households for loop
            return households;          
    }//end generate tours
    


    /**
     * Generate durations for activities, choose tour primary destination and mode,
     * intermediate stop locations, and trip mode for auto trips for all weekday tours.
     *
     * @param thisHousehold
     */
    public List runWeekdayDurationDestinationModeChoiceModels(PTHousehold thisHousehold,
                                            DCExpUtilitiesManager um){
                //We want to be able to return the duration, primary and secondary times as well as
                //the households so we will put the household and a double array into a list
                //which will be returned by this method.
                List returnValues = new ArrayList();
                double[] times = new double[3];  //duration model, primary tour model and secondary tour model times
                double[] primaryTimes = null;
                double[] accumPrimaryTimes = new double[3];
                for(int personNumber=0;personNumber<thisHousehold.persons.length;++personNumber){
                    PTPerson thisPerson = thisHousehold.persons[personNumber];
                    //logger.fine("Getting pattern for Household Number: "+thisHousehold.ID+" Person Number:  "+thisPerson.ID);
                    Pattern thisPattern = thisPerson.weekdayPattern;

                    int workBasedTours=0;
                    //home-based tours

                    for(int tourNumber=0; tourNumber<thisHousehold.persons[personNumber].weekdayTours.length;++tourNumber){
                        Tour thisWeekdayTour = thisPerson.weekdayTours[tourNumber];
                        //duration model
                        long durationTime = System.currentTimeMillis();
                        durationModel.setAttributes(thisHousehold,thisPerson,thisPattern);
                        thisWeekdayTour = durationModel.calculateTourDuration(thisPerson.weekdayTours, tourNumber);
                        times[0] += (System.currentTimeMillis()-durationTime)/1000.0;

                        long primaryTime = System.currentTimeMillis();
                        primaryTimes = runModelsForOneTour(thisHousehold,
                                            thisPerson,
                                            thisWeekdayTour,
                                            personNumber,
                                            um
                                            );
                        times[1] += (System.currentTimeMillis()-primaryTime)/1000.0;

                        for(int i=0; i<primaryTimes.length; i++){
                            accumPrimaryTimes[i] += primaryTimes[i];
                        }

                        adjustStartEndTimesOfTour(tourNumber, thisWeekdayTour, thisPerson.weekdayTours, PTModelInputs.skims);

                        // ask joel; is this block for secondary tours based at work locations?
                        long secondaryTime = System.currentTimeMillis();
                        if(thisWeekdayTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED){
                            ++workBasedTours;
                            thisHousehold.persons[personNumber].weekdayWorkBasedTours[workBasedTours-1]  = new Tour();
                            Tour thisWeekdayWorkBasedTour = thisHousehold.persons[personNumber].weekdayWorkBasedTours[workBasedTours-1];
                            thisWeekdayWorkBasedTour.tourNumber=thisPerson.weekdayTours.length + workBasedTours;
                            thisWeekdayWorkBasedTour.setWorkBasedTourAttributes(thisWeekdayTour);
                            workBasedTourModel.calculateWorkBasedTour(thisHousehold,
                                                              thisPerson,
                                                              thisWeekdayWorkBasedTour,
                                                              PTModelInputs.skims,
                                                              PTModelInputs.tmpd,
                                                              PTModelInputs.tdpd,
                                                              tazs,
                                                              um,
                                                              tmcm
                                                              );

                            tripModeChoiceModel.calculateTripModes(thisHousehold,
                                               thisPerson,
                                               thisWeekdayWorkBasedTour,
                                               PTModelInputs.skims,
                                               PTModelInputs.smpd,
                                               tazs
                                               );
                            
                            adjustStartEndTimeOfWorkBasedTour(thisWeekdayWorkBasedTour, PTModelInputs.skims);
                        }
                        times[2] += (System.currentTimeMillis()-secondaryTime)/1000.0;

                    } //end searching tours
               } //end persons
            returnValues.add(0,thisHousehold);
            returnValues.add(1, times);
            returnValues.add(2, accumPrimaryTimes);
            return returnValues;
    }//end weekday model

    public double[] runModelsForOneTour(PTHousehold thisHousehold, PTPerson thisPerson, Tour thisTour,
                                    int personNumber, DCExpUtilitiesManager um){
        double[] times = new double[3];
//        logger.info("Currently running destination, stop dest and trip mode for the following tour: " + thisTour.tourString);
//        thisTour.print(thisTour);
        //tour destination/mode choice model
        long startTime = System.currentTimeMillis();
        destinationModeChoiceModel.calculateDestinationZone(thisHousehold,
                                                            thisPerson,
                                                            thisTour, 
                                                            PTModelInputs.skims, 
                                                            PTModelInputs.tmpd, 
                                                            PTModelInputs.tdpd,
                                                            tazs,
                                                            um,
                                                            tmcm
                                                            );
        times[0] = ((System.currentTimeMillis() - startTime)/1000.0);

        //Intermediate Stop Destination choice Model
        //logger.fine("Running Tour/Intermediate stop model for Household Number: "+thisHousehold.ID+" Person Number:  "+thisPerson.ID);
        startTime = System.currentTimeMillis();
        stopDestinationChoiceModel.calculateStopZones(thisHousehold,
                                                      thisPerson,
                                                      thisTour, 
                                                      PTModelInputs.skims, 
                                                      PTModelInputs.sdpd,
                                                      tazs);

        times[1] =  ((System.currentTimeMillis() - startTime)/1000.0);

        /*
        //print results thus far
        System.out.println();
        logger.info("Weekday Home-Based Tour Number:  "+(tourNumber+1));
        households[hhNumber].persons[personNumber].weekdayTours[tourNumber].print();     
        System.out.println("Trip mode choice");
        */
        
        
        
        //logger.fine("Running Trip mode choice model for Household Number: "+thisHousehold.ID+" Person Number:  "+thisPerson.ID);
        startTime = System.currentTimeMillis();
        tripModeChoiceModel.calculateTripModes(thisHousehold,
                                               thisPerson,
                                               thisTour,
                                               PTModelInputs.skims,
                                               PTModelInputs.smpd,
                                               tazs
                                               );

        times[2] = ((System.currentTimeMillis() - startTime)/1000.0);
        /*
        //print results thus far
        System.out.println();
        logger.info("Weekday Home-Based Tour Number:  "+(tourNumber+1));
        households[hhNumber].persons[personNumber].weekdayTours[tourNumber].print();
        */          
         //end home-based tours



        return times;
    }
    
   


    /**
     * Generate durations for activities, choose tour primary destination and mode,
     * intermediate stop locations, and trip mode for auto trips for all weekday tours.
     * 
     * @param thisHousehold
     */
    public PTHousehold runWeekendDurationDestinationModeChoiceModels(PTHousehold thisHousehold,DCExpUtilitiesManager um){ 

            
               //for each hh member, and each tour, calculate duration, tour destination & mode for weekends
               for(int personNumber=0;personNumber<thisHousehold.persons.length;++personNumber){
                    PTPerson thisPerson = thisHousehold.persons[personNumber];
                    //logger.fine("Household Number: "+thisHousehold.ID+" Person Number:  "+thisPerson.ID);
                    Pattern thisPattern = thisPerson.weekendPattern;
                    for(int tourNumber=0; tourNumber<thisHousehold.persons[personNumber].weekendTours.length;++tourNumber){
                         Tour thisWeekendTour = thisHousehold.persons[personNumber].weekendTours[tourNumber];
                         
                         //duration model
                         durationModel.setAttributes(thisHousehold,thisPerson,thisPattern);
                         thisWeekendTour = durationModel.calculateTourDuration(thisPerson.weekendTours, tourNumber);
                         
                        runModelsForOneTour(thisHousehold,
                                            thisPerson,
                                            thisWeekendTour,
                                            personNumber,
                                            um
                                            );
                         
                    } //end searching tours
                         
               } //end persons
        //} //end households
        return thisHousehold;
    }//end weekend model


    public PTHousehold[] generateRandomHouseholds(int numberOfHouseholds,TazData taz){
          
         PTHousehold[] households = new PTHousehold[numberOfHouseholds];
          
         for(int i=0;i<numberOfHouseholds;++i){
              PTHousehold hh = new PTHousehold();
              hh.createRandomHousehold(taz);
              households[i] = hh;
         }
          
         return households;
    }

    public static TableDataSet loadTableDataSet(ResourceBundle rb, String pathName){
        
            String path = ResourceUtil.getProperty(rb, pathName);
            
            try {
                String fullPath = path;
                CSVFileReader reader = new CSVFileReader();
                TableDataSet table = reader.readFile(new File(fullPath));
                return table;
            } catch (IOException e) {
                logger.severe("Can't find TazData input table laborFlows");
                e.printStackTrace();
            }
            return null;
       }
    /*
    *  When the start and end times of the various activities on the tour
    *  were originally calculated the "timeToActivity" attribute was 0
    *  because the activity destination was not known.  So now that
    *  the destination has been chosen we will go back and adjust the
    *  times to account for travel time.
    */
    private void adjustStartEndTimesOfTour(int tourNumber, Tour thisTour, Tour[] tours, SkimsInMemory skims){

        if(tourNumber == 0){ //the first tour in the pattern
            //begin.startTime does not change
            //being.endTime does not change
        }else{
            thisTour.begin.startTime = tours[tourNumber-1].end.startTime;
            durationModel.calculateEndTime(thisTour.begin);
        }

        //adjust the intermediateStop1 and primary destination times
        if(thisTour.intermediateStop1 != null){ //this tour has an intermediateStop1
            //set intStop1 timeToActivity
            thisTour.intermediateStop1.timeToActivity = (short)skims.opTime.getValueAt(thisTour.begin.location.zoneNumber,
                                                                                thisTour.intermediateStop1.location.zoneNumber);
            //adjust intStop1 start time
            durationModel.calculateStartTime(thisTour.begin, thisTour.intermediateStop1);
            //adjust intStop1 end time
            durationModel.calculateEndTime(thisTour.intermediateStop1);
            //set primary destination timeToActivity
            if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK ||
                    thisTour.primaryDestination.activityPurpose == ActivityPurpose.WORK_BASED){
                thisTour.primaryDestination.timeToActivity = (short) skims.pkTime.getValueAt(thisTour.intermediateStop1.location.zoneNumber,
                                                                                             thisTour.primaryDestination.location.zoneNumber);
            } else thisTour.primaryDestination.timeToActivity = (short) skims.opTime.getValueAt(thisTour.intermediateStop1.location.zoneNumber,
                                                                                                thisTour.primaryDestination.location.zoneNumber);
            //adjust primaryDestination start time
            durationModel.calculateStartTime(thisTour.intermediateStop1,thisTour.primaryDestination);
        } else { //this tour has no intermediateStop1 and goes from begin to primary destination
            //set primary destination timeToActivity
            if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK ||
                    thisTour.primaryDestination.activityPurpose == ActivityPurpose.WORK_BASED){
                thisTour.primaryDestination.timeToActivity = (short) skims.pkTime.getValueAt(thisTour.begin.location.zoneNumber,
                                                                                             thisTour.primaryDestination.location.zoneNumber);
            } else thisTour.primaryDestination.timeToActivity = (short) skims.opTime.getValueAt(thisTour.begin.location.zoneNumber,
                                                                                                thisTour.primaryDestination.location.zoneNumber);
            //adjust primaryDestination start time
            durationModel.calculateStartTime(thisTour.begin, thisTour.primaryDestination);
        } // we have now calculated start and end time for intStop1 and startTime for primaryDestination

        //adjust primaryDestination end time
        durationModel.calculateEndTime(thisTour.primaryDestination);

        //Now do the end of the chain, the intermediateStop2 and the end
        if(thisTour.intermediateStop2 != null){ //this tour has an intermediateStop2
            //set intStop2 timeToActivity
            thisTour.intermediateStop2.timeToActivity = (short)skims.opTime.getValueAt(thisTour.primaryDestination.location.zoneNumber,
                                                                                thisTour.intermediateStop2.location.zoneNumber);
            //adjust intStop2 start time
            durationModel.calculateStartTime(thisTour.primaryDestination, thisTour.intermediateStop2);
            //adjust intStop2 end time
            durationModel.calculateEndTime(thisTour.intermediateStop2);

            //set end timeToActivity
            thisTour.end.timeToActivity = (short) skims.opTime.getValueAt(thisTour.intermediateStop2.location.zoneNumber,
                                                                           thisTour.end.location.zoneNumber);
            //adjust end start time
            durationModel.calculateStartTime(thisTour.intermediateStop2,thisTour.end);
        } else { //this tour has no intermediateStop2 and goes from primary destination to end
            //set end timeToActivity
            thisTour.end.timeToActivity = (short) skims.opTime.getValueAt(thisTour.primaryDestination.location.zoneNumber,
                                                                          thisTour.end.location.zoneNumber);
            //adjust end start time
            durationModel.calculateStartTime(thisTour.primaryDestination, thisTour.end);
        } // we have now calculated start and end time for intStop1 and startTime for primaryDestination

    }

    /*
    *  When the start and end times of work based tour ('w-o-w')
    *  were originally calculated the "timeToActivity" attribute was 0
    *  because the primary destination was not known.  So now that
    *  the destination has been chosen we will go back and adjust the
    *  primary destination start and end time and the end activity start time to account for travel time.
    *  The begin start and end time were set based on the adjusted tour times in the
    *  previous call to 'adjustStartEndTimeOfTour' so these won't change.
    */
    private void adjustStartEndTimeOfWorkBasedTour(Tour thisWorkBasedTour,  SkimsInMemory skims){

        //adjust the primary destination times
        thisWorkBasedTour.primaryDestination.timeToActivity = (short) skims.opTime.getValueAt(thisWorkBasedTour.begin.location.zoneNumber,
                                                                                                thisWorkBasedTour.primaryDestination.location.zoneNumber);
        //adjust primaryDestination start time
        durationModel.calculateStartTime(thisWorkBasedTour.begin,thisWorkBasedTour.primaryDestination);
        //adjust primaryDestination end time
        durationModel.calculateEndTime(thisWorkBasedTour.primaryDestination);

        //Now do the end of the chain, the primaryDestination to the end
        thisWorkBasedTour.end.timeToActivity = (short) skims.opTime.getValueAt(thisWorkBasedTour.primaryDestination.location.zoneNumber,
                                                                          thisWorkBasedTour.end.location.zoneNumber);
        //adjust end start time
        durationModel.calculateStartTime(thisWorkBasedTour.primaryDestination, thisWorkBasedTour.end);

    }

     /**
      * Run PT Model
      * @param args
      */
     public static void main(String[] args){
         //can't be done right now, sorry.
     }


    public void startModel(int timeInterval){};

}//end PTModel class



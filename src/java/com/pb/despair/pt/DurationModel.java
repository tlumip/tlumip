package com.pb.despair.pt;

import com.pb.common.math.MathUtil;
import com.pb.common.util.SeededRandom;
import org.apache.log4j.Logger;

/** 
 * DurationModel takes a day-pattern, durationmodel 
 * person attributes, and tour number and predicts 
 * the time of each activity
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

public class DurationModel{
    final static Logger logger = Logger.getLogger(DurationModel.class);
     //attributes
     Pattern thisPattern;
     PersonDurationAttributes thisPersonAttributes = new PersonDurationAttributes();
     private Tour thisTour;
     public static final int randomDraws=1;
     final static boolean constrained=true;
     DurationModelParametersData dmpd;
    
    boolean firstHomeLogged = false,
            intermedHomeLogged = false,
            workLogged = false,
            shopLogged = false,
            recreateLogged = false,
            otherLogged = false,
            schoolLogged = false;

     /**
      * Constructor with no arguments.
      *
      */
     public DurationModel(DurationModelParametersData dmpd){
        this.dmpd = dmpd;
     }
     
     /**
      * DurationModel Constructor
      *
      * @param thisHousehold PTHousehold
      * @param thisPerson PTPerson
      * @param pattern Pattern
      */
     public void setAttributes(PTHousehold thisHousehold, PTPerson thisPerson, Pattern pattern){
          
          thisPattern=pattern;
          
          //from PTHousehold , PTPerson
          thisPersonAttributes.setAttributes(thisHousehold,thisPerson);
          
     }
      
     /**
      * Calculates duration of specified tour
      * activity number is the activity to predict duration, returns an integer (duration in minutes) 
      * @param tours
      * @param tourNumber
      */

     public Tour calculateTourDuration(Tour[] tours, int tourNumber){
          
          //get the tour for this tourNumber     
          thisTour=tours[tourNumber];

          //calculate duration for first activity, depends on if first tour of day or second+ tour of day     
          if(tourNumber==0){
               thisTour.begin.startTime = 300;
               thisTour.begin.duration = calculateFirstHomeDuration(thisTour.begin);
          }else{
               thisTour.begin.startTime = tours[tourNumber-1].end.startTime;
               thisTour.begin.duration = calculateIntermediateHomeDuration(thisTour.begin);
          }
          calculateEndTime(thisTour.begin);
          
          //IntermediateStop1
          if(thisTour.intermediateStop1!=null){
               calculateStartTime(thisTour.begin,thisTour.intermediateStop1);
               thisTour.intermediateStop1.duration = calculateDuration(thisTour.intermediateStop1);
               calculateEndTime(thisTour.intermediateStop1);
               calculateStartTime(thisTour.intermediateStop1,thisTour.primaryDestination);
          }else{
               calculateStartTime(thisTour.begin,thisTour.primaryDestination);
          }          
          
          //Primary Destination duration
          thisTour.primaryDestination.duration = calculateDuration(thisTour.primaryDestination);
          calculateEndTime(thisTour.primaryDestination);
          //IntermediateStop2
          if(thisTour.intermediateStop2!=null){
               calculateStartTime(thisTour.primaryDestination,thisTour.intermediateStop2);
               thisTour.intermediateStop2.duration = calculateDuration(thisTour.intermediateStop2);
               calculateEndTime(thisTour.intermediateStop2);
               calculateStartTime(thisTour.intermediateStop2,thisTour.end);
          }else{
               calculateStartTime(thisTour.primaryDestination,thisTour.end);
          }
          return thisTour;
     }
          
     /** this function merely calls the appropriate method based on the type
     of activity passed.  Not to be used with at-home activities 
     */
     public short calculateDuration(Activity thisActivity){
          
          short duration=0;
          
          //call appropriate method
          if(thisActivity.activityPurpose==ActivityPurpose.WORK || thisActivity.activityPurpose==ActivityPurpose.WORK_BASED)
               duration=calculateWorkDuration(thisActivity);
          else if(thisActivity.activityPurpose==ActivityPurpose.SCHOOL)
               duration=calculateSchoolDuration(thisActivity);
          else if(thisActivity.activityPurpose==ActivityPurpose.SHOP)
               duration=calculateShopDuration(thisActivity);
          else if(thisActivity.activityPurpose==ActivityPurpose.RECREATE)
               duration=calculateRecreateDuration(thisActivity);
          else if(thisActivity.activityPurpose==ActivityPurpose.OTHER)
               duration=calculateOtherDuration(thisActivity);
          
          return duration;
     }

    float solveForT(double shape,double expression, Activity thisActivity){
          
         //constraint: the activity must be at least 5 minutes and at most x minutes left in day
         double maxRandom=MathUtil.exp(-Math.pow(5,(1/shape))*MathUtil.exp(-1/shape*expression));
         double minRandom=MathUtil.exp(-Math.pow((thisActivity.minutesLeftInDay()-30),(1/shape))*MathUtil.exp(-1/shape*expression));
          
         double totalRandom=0;
         double randomNumber=0;


        if(logger.isDebugEnabled()) {
            logger.debug("Expression:  "+expression);
            logger.debug("Shape:  "+shape);
            logger.debug("Min Random number:  "+minRandom);
            logger.debug("Max Random number:  "+maxRandom);
        }

        if(constrained){
              do{
                   totalRandom=0;
                   for(int i=0;i<randomDraws;++i){
                        totalRandom+=SeededRandom.getRandom();
                   }
                   randomNumber=totalRandom/randomDraws;
                   if(logger.isDebugEnabled()) {
                       logger.debug("Random number:  "+randomNumber);
                   }
              }while(randomNumber<minRandom || randomNumber>=maxRandom);
         }else{
              totalRandom=0;
              for(int i=0;i<randomDraws;++i){
                   totalRandom+=SeededRandom.getRandom();
              }
              randomNumber=totalRandom/randomDraws;
              if(logger.isDebugEnabled()) {
                  logger.debug("Random number:  "+randomNumber);
              }
         }
          
          
         double epsilon=MathUtil.log(-MathUtil.log(1-randomNumber));
         double minutes = MathUtil.exp(expression + (epsilon*shape));


        if(logger.isDebugEnabled()) {
            logger.debug("Numerator: "+epsilon);
            logger.debug("Minutes "+minutes);
        }

        return (new Double(minutes)).floatValue();
    }

     
     float oldSolveForT(double shape,double expression, Activity thisActivity){
          
          //constraint: the activity must be at least 5 minutes and at most x minutes left in day
          double maxRandom=MathUtil.exp(-Math.pow(5,(1/shape))*MathUtil.exp(-1/shape*expression));
          double minRandom=MathUtil.exp(-Math.pow((thisActivity.minutesLeftInDay()-30),(1/shape))*MathUtil.exp(-1/shape*expression));
          
          double totalRandom=0;
          double randomNumber=0;


         if(logger.isDebugEnabled()) {
             logger.debug("Expression:  "+expression);
             logger.debug("Shape:  "+shape);
             logger.debug("Min Random number:  "+minRandom);
             logger.debug("Max Random number:  "+maxRandom);
         }

         if(constrained){
               do{
                    totalRandom=0;
                    for(int i=0;i<randomDraws;++i){
                         totalRandom+=SeededRandom.getRandom();
                    }
                    randomNumber=totalRandom/randomDraws;
                    if(logger.isDebugEnabled()) {
                        logger.debug("Random number:  "+randomNumber);
                    }
               }while(randomNumber<minRandom || randomNumber>=maxRandom);
          }else{
               totalRandom=0;
               for(int i=0;i<randomDraws;++i){
                    totalRandom+=SeededRandom.getRandom();
               }
               randomNumber=totalRandom/randomDraws;
               if(logger.isDebugEnabled()) {
                   logger.debug("Random number:  "+randomNumber);
               }
          }
          
          
          double numerator=MathUtil.log(randomNumber);
          double denominator=((-1/shape)*(expression));
          double minutes = Math.pow((-numerator/MathUtil.exp(denominator)),shape);


         if(logger.isDebugEnabled()) {
             logger.debug("Numerator: "+numerator);
             logger.debug("Denominator: "+denominator);
             logger.debug("Minutes "+minutes);
         }

         return (new Double(minutes)).floatValue();
     }
          
     public short calculateFirstHomeDuration(Activity thisActivity){
          DurationModelParameters param = getParametersFromParametersDataArray("firstHome");
          double expression=          
                 param.IStopsInPatternEquals1*thisPattern.IStopsEquals1           //One intermediate stop in pattern
               + param.IStopsInPatternEquals2Plus*thisPattern.IStopsEquals2Plus     //Two or more intermediate stops in pattern
               + param.toursEquals2*thisPattern.toursEquals2          //Two tours in pattern
               + param.toursEquals3Plus*thisPattern.toursEquals3Plus     //Three or more tours in pattern
               + param.singleAdultWithOnePlusChildren*thisPersonAttributes.singleAdultWithOnePlusChildren//Single adult with 1+ children
               + param.autos0*thisPersonAttributes.autos0     //0 Autos
               + param.age19to21*thisPersonAttributes.age19to21 //Age 19to21
               + param.shopOnlyInPattern*thisPattern.shopOnly                //Only shop activities in pattern
               + param.recreateOnlyInPattern*thisPattern.recreateOnly           //Only recreate activities in pattern
               + param.otherOnlyInPattern*thisPattern.otherOnly           //Only other activities in pattern
               + param.tour1IsWork*thisPattern.tour1IsWork          //First tour is work
               + param.tour1IsSchool*thisPattern.tour1IsSchool          //First tour is school
               + param.constant;                                        //constant
               
          double shape= (double) param.shape;
          if(logger.isDebugEnabled() && !firstHomeLogged) {
              logger.debug("Shape Param for First Home: " + param.shape);
              firstHomeLogged = true;
          }
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));
     }

     public short calculateIntermediateHomeDuration(Activity thisActivity){
          DurationModelParameters param = getParametersFromParametersDataArray("intermedHome");
          double expression=
                 param.toursEquals3*thisPattern.toursEquals3          //Three Tours
               + param.toursEquals4*thisPattern.toursEquals4          //Four Tours
               + param.toursEquals5Plus*thisPattern.toursEquals5Plus     //Five or More Tours
               + param.mdActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"MD"))     //Start MD
               + param.pmActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"PM"))    //Start PM
               + param.evActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"EV"))       //Start EV
               + param.constant;                                         //Constant
          
          double shape= (double) param.shape;
          if(logger.isDebugEnabled() && !intermedHomeLogged) {
              logger.debug("Shape Param for Intermediate Home: " + param.shape);
              intermedHomeLogged = true;
          }
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));
     }

     public short calculateWorkDuration(Activity thisActivity){
          DurationModelParameters param = getParametersFromParametersDataArray("work");
          double expression=
                 param.IStopsInPatternEquals1*thisPattern.IStopsEquals1          //One Daily Intermediate Stop
               + param.IStopsInPatternEquals2*thisPattern.IStopsEquals2          //Two Daily Intermediate Stops
               + param.IStopsInPatternEquals3*thisPattern.IStopsEquals3          //Three Daily Intermediate Stops
               + param.IStopsInPatternEquals4Plus*thisPattern.IStopsEquals4Plus     //Four + Daily Intermediate Stops
               + param.toursEquals2*thisPattern.toursEquals2          //Two Tours
               + param.toursEquals3*thisPattern.toursEquals3          //Three Tours
               + param.toursEquals4*thisPattern.toursEquals4          //Four Tours
               + param.toursEquals5Plus*thisPattern.toursEquals5Plus     //Five or More Tours
               + param.industryEqualsRetail*thisPersonAttributes.industryEqualsRetail //Retail Industry
               + param.industryEqualsPersonServices*thisPersonAttributes.industryEqualsPersonServices//Personal Services
               + param.worksTwoJobs*thisPersonAttributes.worksTwoJobs//Works Two Jobs
               + param.schDummy*thisPattern.schDummy               //School Activity
               + param.amActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"AM"))    //Activity Start AM
               + param.mdActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"MD"))   //Activity Start MD
               + param.pmActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"PM"))  //Activity Start PM
               + param.evActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"EV"))  //Activity Start EV
               + param.constant;                                         //Constant

          double shape= (double) param.shape;
         if(logger.isDebugEnabled() && !workLogged) {
              logger.debug("Shape Param for Work: " + param.shape);
              workLogged = true;
          }
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));
     }

     public short calculateSchoolDuration(Activity thisActivity){
          DurationModelParameters param = getParametersFromParametersDataArray("school");
          double expression=
                 param.toursEquals2*thisPattern.toursEquals2          //Two Tours
               + param.toursEquals3*thisPattern.toursEquals3          //Three Tours
               + param.toursEquals4*thisPattern.toursEquals4          //Four Tours
               + param.toursEquals5Plus*thisPattern.toursEquals5Plus     //Five or More Tours
               + param.amActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"AM"))   //Start AM
               + param.mdActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"MD"))     //Start MD
               + param.pmActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"PM"))    //Start PM
               + param.evActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"EV"))     //Start EV
               + param.IStopsInPatternEquals1*thisPattern.IStopsEquals1          //One intermediate stop in pattern
               + param.IStopsInPatternEquals2*thisPattern.IStopsEquals2          //Two intermediate stops in pattern
               + param.IStopsInPatternEquals3*(thisPattern.IStopsEquals3
               + param.IStopsInPatternEquals4Plus*thisPattern.IStopsEquals4Plus)//Three or More Intermediate Stops in Pattern
               + param.IStopsOnTourEquals1*thisTour.iStopsCheck(1,thisTour.tourString) //One intermediate stop on Tour
               + param.IStopsOnTourEquals2*thisTour.iStopsCheck(2,thisTour.tourString) //Two Intermediate Stops on Tour
               + param.wkDummy*thisPattern.wrkDummy               //Work Activity in Pattern
               + param.age25Plus*thisPersonAttributes.age25Plus//Age is >=25 years
               + param.constant;                                         //Constant
          
          double shape=(double) param.shape;
         if(logger.isDebugEnabled() && !schoolLogged) {
              logger.debug("Shape Param for School: " + param.shape);
              schoolLogged = true;
          }
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));
     }
     public short calculateShopDuration(Activity thisActivity){
          DurationModelParameters param = getParametersFromParametersDataArray("shop");
          double expression=
                 param.toursEquals2*thisPattern.toursEquals2          //Two Tours
               + param.toursEquals3*thisPattern.toursEquals3          //Three Tours
               + param.toursEquals4*thisPattern.toursEquals4          //Four Tours
               + param.toursEquals5Plus*thisPattern.toursEquals5Plus      //Five or More Tours
               + param.amActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"AM"))  //Activity Start AM
               + param.mdActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"MD"))  //Activity Start MD
               + param.pmActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"PM")) //Activity Start PM
               + param.evActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"EV"))   //Activity Start EV
               + param.IStopsInPatternEquals1*thisPattern.IStopsEquals1          //One Daily Intermediate Stop
               + param.IStopsInPatternEquals2*thisPattern.IStopsEquals2          //Two Daily Intermediate Stops
               + param.IStopsInPatternEquals3*thisPattern.IStopsEquals3       //Three Daily Intermediate Stops
               + param.IStopsInPatternEquals4Plus*thisPattern.IStopsEquals4Plus     //Four + Daily Intermediate Stops
               + param.activityIsPrimaryDestination*(thisActivity.checkActivityType(thisActivity.activityType,ActivityType.PRIMARY_DESTINATION))    //Primary Destination
               + param.activityIsIntermediateStopOnShopTour*(thisActivity.checkActivityType(thisActivity.activityType,ActivityType.INTERMEDIATE_STOP))
               *(thisTour.primaryDestination.checkActivityPurpose(thisActivity.activityPurpose,ActivityPurpose.SHOP)) //Intermediate Stop on Shop Tour
               + param.income60Plus*thisPersonAttributes.income60Plus //Income > $60k
               + param.female*thisPersonAttributes.female      //Female
               + param.householdSize3Plus*thisPersonAttributes.householdSize3Plus //HH Size 3+
               + param.constant;                                         //Constant
 
          double shape=param.shape;
         if(logger.isDebugEnabled() && !shopLogged) {
              logger.debug("Shape Param for Shop: " + param.shape);
              shopLogged = true;
          }
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));
     }
     public short calculateRecreateDuration(Activity thisActivity){
          DurationModelParameters param = getParametersFromParametersDataArray("recreate");
          double expression=    
                      param.toursEquals2*thisPattern.toursEquals2          //Two Tours
                    + param.toursEquals3*thisPattern.toursEquals3          //Three Tours
                    + param.toursEquals4*thisPattern.toursEquals4          //Four Tours
                    + param.toursEquals5Plus*thisPattern.toursEquals5Plus      //Five or More Tours
                    + param.amActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"AM"))  //Activity Start AM
                    + param.mdActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"MD"))    //Activity Start MD
                    + param.pmActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"PM"))   //Activity Start PM
                    + param.evActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"EV"))  //Activity Start EV
                    + param.IStopsInPatternEquals1*thisPattern.IStopsEquals1          //One Daily Intermediate Stop
                    + param.IStopsInPatternEquals2*thisPattern.IStopsEquals2          //Two Daily Intermediate Stops
                    + param.IStopsInPatternEquals3*thisPattern.IStopsEquals3       //Three Daily Intermediate Stops
                    + param.IStopsInPatternEquals4Plus*thisPattern.IStopsEquals4Plus     //Four + Daily Intermediate Stops
                    + param.IStopsOnTourEquals1*thisTour.iStopsCheck(1,thisTour.tourString)  //One intermediate stop on Tour
                    + param.IStopsOnTourEquals2*thisTour.iStopsCheck(2,thisTour.tourString) //Two Intermediate Stops on Tour
                    + param.activityIsIntermediateStopOnWorkTour*(thisActivity.checkActivityType(thisActivity.activityType,ActivityType.INTERMEDIATE_STOP))
                                    *(thisTour.primaryDestination.checkActivityPurpose(thisActivity.activityPurpose,ActivityPurpose.WORK)) //Intermediate Stop on Work Tour
                    + param.isWeekend*thisPattern.isWeekend               //Weekend
                    + param.constant;                                        //Constant
          
          double shape=param.shape;
         if(logger.isDebugEnabled() && !recreateLogged) {
              logger.debug("Shape Param for recreate: " + param.shape);
              recreateLogged = true;
          }
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));
     }

     public short calculateOtherDuration(Activity thisActivity){
          DurationModelParameters param = getParametersFromParametersDataArray("other");
          double expression=    
                 param.toursEquals2*thisPattern.toursEquals2          //Two Tours
               + param.toursEquals3*thisPattern.toursEquals3          //Three Tours
               + param.toursEquals4*thisPattern.toursEquals4          //Four Tours
               + param.toursEquals5Plus*thisPattern.toursEquals5Plus      //Five or More Tours
               + param.amActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"AM"))               //Activity Start AM
               + param.mdActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"MD"))  //Activity Start MD
               + param.pmActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"PM"))   //Activity Start PM
               + param.evActivityStartTime*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"EV"))   //Activity Start EV
               + param.IStopsInPatternEquals1*thisPattern.IStopsEquals1     //One Daily Intermediate Stop
               + param.IStopsInPatternEquals2*thisPattern.IStopsEquals2          //Two Daily Intermediate Stops
               + param.IStopsInPatternEquals3Plus*thisPattern.IStopsEquals3Plus     //Three or More Daily Intermediate Stops
               + param.IStopsOnTourEquals1*thisTour.iStopsCheck(1,thisTour.tourString)          //One intermediate stop on Tour
               + param.IStopsOnTourEquals2*thisTour.iStopsCheck(2,thisTour.tourString)          //Two Intermediate Stops on Tour
               + param.activityIsIntermediateStopOnWorkTour*(thisActivity.checkActivityType(thisActivity.activityType,ActivityType.INTERMEDIATE_STOP))
                        *(thisTour.primaryDestination.checkActivityPurpose(thisActivity.activityPurpose,ActivityPurpose.WORK)) //Work Tour Intermediate Stop
               + param.activityIsIntermediateStopOnSchoolTour*(thisActivity.checkActivityType(thisActivity.activityType,ActivityType.INTERMEDIATE_STOP))
                        *(thisTour.primaryDestination.checkActivityPurpose(thisActivity.activityPurpose,ActivityPurpose.SCHOOL))//School Tour Intermediate Stop
               + param.householdSize3Plus*thisPersonAttributes.householdSize3Plus     //Household Size >=3
               + param.singleAdultWithOnePlusChildren*thisPersonAttributes.singleAdultWithOnePlusChildren     //Single Adult with Child
               + param.income60Plus*thisPersonAttributes.income60Plus//Household Income >=$60k
               + param.constant;                                         //Constant

          double shape=param.shape;
         if(logger.isDebugEnabled() && !otherLogged) {
              logger.debug("Shape Param for Other: " + param.shape);
              otherLogged = true;
          }
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));
     }

     void calculateEndTime(Activity thisActivity){
          
          int hours=thisActivity.duration / 60;
          int minutes = thisActivity.duration  - (hours*60);
          
          int startHours = thisActivity.startTime/100;
          int startMinutes = thisActivity.startTime - (startHours*100);
          
          if((startMinutes+minutes)>=60){
               ++hours;
               minutes=(startMinutes+minutes)-60;
               startMinutes=0;
          }     
          thisActivity.endTime=(short)((startHours*100)+(hours*100)+startMinutes+minutes);
     }          

          
     void calculateStartTime(Activity firstActivity, Activity nextActivity){
         //time to activity is broken down into hours and minutes
         int hours = nextActivity.timeToActivity / 60;
         int minutes = nextActivity.timeToActivity - (hours*60);

         //prior to adding on the timeToActivity to the end time of the first activity, break
         //down the end time into hours and minutes
         int endHours = firstActivity.endTime/100;
         int endMinutes = firstActivity.endTime - (endHours*100);

         if(endMinutes + minutes >= 60){ //we need to increment the hours
             hours++;
             minutes = (endMinutes + minutes)-60;  //and fix the minutes
             endMinutes=0;
         }
         //return the correct time in military format.
         nextActivity.startTime=(short)((endHours*100) + (hours*100)+ endMinutes + minutes);
          
    }

    DurationModelParameters getParametersFromParametersDataArray(String activityPurpose){

        int i=0;
        while( i<dmpd.parameters.length ){
            //try to find the correct element in the array - if you do, return the object
            if(dmpd.parameters[i].purpose.equalsIgnoreCase(activityPurpose)) return dmpd.parameters[i];

            //otherwise go to next array element
            i++;
        }
        //if you get to this line, then something is wrong because you should have found the activity
        //you were looking for in the array
        logger.error(activityPurpose + " was not found in parameters array - check parameters file for activity" +
                " names - returning null");
        return null;
    }




}

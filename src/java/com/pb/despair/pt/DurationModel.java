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
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.DurationModel");
     //attributes
     Pattern thisPattern;
     PersonDurationAttributes thisPersonAttributes = new PersonDurationAttributes();
     private Tour thisTour;
     public static final int randomDraws=1;
     final static boolean constrained=true;
     
     /**
      * Constructor with no arguments.
      *
      */
     public DurationModel(){
         
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
               //if second tour, set time to get to first activity as total travel time of last tour
//               tours[tourNumber-1].end.duration=calculateDuration(tours[tourNumber-1].end);
//               calculateEndTime(tours[tourNumber-1].end);
               //thisTour.begin.timeToActivity=new Double(tours[tourNumber-1].primaryMode.time).shortValue();
//               thisTour.begin.timeToActivity=(short)tours[tourNumber-1].primaryMode.time;
//               calculateStartTime(tours[tourNumber-1].end,thisTour.begin);
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
          if(thisActivity.activityPurpose==ActivityPurpose.WORK||thisActivity.activityPurpose==ActivityPurpose.WORK_BASED)
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
          
          double expression=          
                 -0.0537*thisPattern.IStopsEquals1           //One intermediate stop in pattern
               + -0.0740*thisPattern.IStopsEquals2Plus     //Two or more intermediate stops in pattern
               + -0.1115*thisPattern.toursEquals2          //Two tours in pattern
               + -0.1972*thisPattern.toursEquals3Plus     //Three or more tours in pattern
               + -0.1157*thisPersonAttributes.singleAdultWithOnePlusChildren//Single adult with 1+ children
               + -0.0475*thisPersonAttributes.autos0     //0 Autos
               +  0.1535*thisPersonAttributes.age18to20 //Age 18to20
               +  0.0860*thisPattern.shopOnly                //Only shop activities in pattern
               +  0.0563*thisPattern.recreateOnly           //Only recreate activities in pattern
               +  0.0201*thisPattern.otherOnly           //Only other activities in pattern
               + -0.2349*thisPattern.tour1IsWork          //First tour is work
               + -0.2295*thisPattern.tour1IsSchool          //First tour is school     
               +  5.9243;                                        //constant
               
          double shape= 0.5826;
     
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));
     }

     public short calculateIntermediateHomeDuration(Activity thisActivity){
          
          double expression=
                 -0.3001*thisPattern.toursEquals3          //Three Tours 
               + -0.4694*thisPattern.toursEquals4          //Four Tours
               + -0.6910*thisPattern.toursEquals5Plus     //Five or More Tours
               + -0.4126*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"MD"))     //Start MD
               + -0.6741*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"PM"))    //Start PM
//               + -0.2943*thisActivity.startEV               //Start EV
               + -0.8*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"EV"))       //Start EV
                    +  5.2806;                                         //Constant
          
          double shape=1.0342;
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));
     }

     public short calculateWorkDuration(Activity thisActivity){
          
          double expression=
                 -0.0948*thisPattern.IStopsEquals1          //One Daily Intermediate Stop
               + -0.1330*thisPattern.IStopsEquals2          //Two Daily Intermediate Stops
               + -0.2472*thisPattern.IStopsEquals3          //Three Daily Intermediate Stops
               + -0.3718*thisPattern.IStopsEquals4Plus     //Four + Daily Intermediate Stops
               + -0.0656*thisPattern.toursEquals2          //Two Tours
               + -0.1667*thisPattern.toursEquals3          //Three Tours
               + -0.2242*thisPattern.toursEquals4          //Four Tours
               + -0.3444*thisPattern.toursEquals5Plus     //Five or More Tours
               +  0.0199*thisPersonAttributes.industryEqualsRetail //Retail Industry
               + -0.0484*thisPersonAttributes.industryEqualsPersonServices//Personal Services
               + -0.3078*thisPersonAttributes.worksTwoJobs//Works Two Jobs
               + -0.1063*thisPattern.schDummy               //School Activity
               + -0.0625*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"AM"))    //Activity Start AM
               + -0.2484*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"MD"))   //Activity Start MD
               + -0.4032*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"PM"))  //Activity Start PM
               + -0.6898*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"EV"))  //Activity Start EV
               +  6.3855;                                         //Constant

          double shape=(1/3.3986);
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));
     }

     public short calculateSchoolDuration(Activity thisActivity){
          
          double expression=
                 -0.1013*thisPattern.toursEquals2          //Two Tours
               + -0.2356*thisPattern.toursEquals3          //Three Tours
               + -0.3167*thisPattern.toursEquals4          //Four Tours
               + -0.4125*thisPattern.toursEquals5Plus     //Five or More Tours
               + -0.2045*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"AM"))   //Start AM
               + -0.5112*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"MD"))     //Start MD
               + -0.8908*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"PM"))    //Start PM
               + -1.1053*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"EV"))     //Start EV
               + -0.0792*thisPattern.IStopsEquals1          //One intermediate stop in pattern
               + -0.0966*thisPattern.IStopsEquals2          //Two intermediate stops in pattern
               + -0.1749*(thisPattern.IStopsEquals3+thisPattern.IStopsEquals4Plus)//Three or More Intermediate Stops in Pattern
               + -0.1257*thisTour.iStopsCheck(1,thisTour.tourString) //One intermediate stop on Tour
               + -0.1356*thisTour.iStopsCheck(2,thisTour.tourString) //Two Intermediate Stops on Tour
               + -0.0711*thisPattern.wrkDummy               //Work Activity in Pattern
               + -0.0857*thisPersonAttributes.age25Plus//Age is >=25 years
               +  6.3140;                                         //Constant
          
          double shape=(1/3.1369);
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));          
     }
     public short calculateShopDuration(Activity thisActivity){
          
          double expression=
                 -0.1429*thisPattern.toursEquals2          //Two Tours
               + -0.2949*thisPattern.toursEquals3          //Three Tours
               + -0.4102*thisPattern.toursEquals4          //Four Tours
               + -0.5177*thisPattern.toursEquals5Plus      //Five or More Tours
               + -0.3274*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"AM"))  //Activity Start AM
               + -0.5239*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"MD"))  //Activity Start MD
               + -0.8438*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"PM")) //Activity Start PM
               + -1.0081*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"EV"))   //Activity Start EV
               + -0.2146*thisPattern.IStopsEquals1          //One Daily Intermediate Stop
               + -0.2659*thisPattern.IStopsEquals2          //Two Daily Intermediate Stops
               + -0.3860*thisPattern.IStopsEquals3       //Three Daily Intermediate Stops
               + -0.2977*thisPattern.IStopsEquals4Plus     //Four + Daily Intermediate Stops
               +  0.1570*(thisActivity.checkActivityType(thisActivity.activityType,ActivityType.PRIMARY_DESTINATION))    //Primary Destination
               + -0.0633*(thisActivity.checkActivityType(thisActivity.activityType,ActivityType.INTERMEDIATE_STOP))
               *(thisTour.primaryDestination.checkActivityPurpose(thisActivity.activityPurpose,ActivityPurpose.SHOP)) //Intermediate Stop on Shop Tour
               +  0.0284*thisPersonAttributes.income60Plus //Income > $60k
               +  0.1042*thisPersonAttributes.female      //Female 
               +  0.0676*thisPersonAttributes.householdSize3Plus //HH Size 3+
               +  4.7248;                                         //Constant
 
          double shape=1.1938;
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));     
     }
     public short calculateRecreateDuration(Activity thisActivity){
          
          double expression=    
                    -0.3123*thisPattern.toursEquals2          //Two Tours
               + -0.4793*thisPattern.toursEquals3          //Three Tours
               + -0.6116*thisPattern.toursEquals4          //Four Tours
               + -0.6341*thisPattern.toursEquals5Plus      //Five or More Tours
               + -0.1879*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"AM"))  //Activity Start AM
               + -0.5474*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"MD"))    //Activity Start MD
               + -0.6692*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"PM"))   //Activity Start PM
               + -0.7487*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"EV"))  //Activity Start EV
               + -0.0858*thisPattern.IStopsEquals1          //One Daily Intermediate Stop
               + -0.1617*thisPattern.IStopsEquals2          //Two Daily Intermediate Stops
               + -0.2072*thisPattern.IStopsEquals3       //Three Daily Intermediate Stops
               + -0.2452*thisPattern.IStopsEquals4Plus     //Four + Daily Intermediate Stops
               + -0.4296*thisTour.iStopsCheck(1,thisTour.tourString)  //One intermediate stop on Tour
               + -0.2238*thisTour.iStopsCheck(2,thisTour.tourString) //Two Intermediate Stops on Tour
               + -0.0579*(thisActivity.checkActivityType(thisActivity.activityType,ActivityType.INTERMEDIATE_STOP))
               *(thisTour.primaryDestination.checkActivityPurpose(thisActivity.activityPurpose,ActivityPurpose.WORK)) //Intermediate Stop on Work Tour
               +  0.0785*thisPattern.isWeekend               //Weekend
               +  5.9603;                                        //Constant
          
          double shape=1.5357;
          return (short)(Math.round(solveForT(shape,expression,thisActivity)));     
     }

     public short calculateOtherDuration(Activity thisActivity){
          
          double expression=    
                 -0.3500*thisPattern.toursEquals2          //Two Tours
               + -0.7273*thisPattern.toursEquals3          //Three Tours
               + -1.0913*thisPattern.toursEquals4          //Four Tours
               + -1.4537*thisPattern.toursEquals5Plus      //Five or More Tours
               + -0.5729*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"AM"))               //Activity Start AM
               + -0.9544*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"MD"))  //Activity Start MD
               + -1.0905*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"PM"))   //Activity Start PM
               + -1.0664*(thisActivity.startTimePeriodCheck(thisActivity.startTime,"EV"))   //Activity Start EV
               + -0.1807*thisPattern.IStopsEquals1     //One Daily Intermediate Stop
               + -0.5475*thisPattern.IStopsEquals2          //Two Daily Intermediate Stops
               + -0.6421*thisPattern.IStopsEquals3Plus     //Three or More Daily Intermediate Stops
               + -0.5545*thisTour.iStopsCheck(1,thisTour.tourString)          //One intermediate stop on Tour
               + -0.1122*thisTour.iStopsCheck(2,thisTour.tourString)          //Two Intermediate Stops on Tour
               + -0.2401*(thisActivity.checkActivityType(thisActivity.activityType,ActivityType.INTERMEDIATE_STOP))
               *(thisTour.primaryDestination.checkActivityPurpose(thisActivity.activityPurpose,ActivityPurpose.WORK)) //Work Tour Intermediate Stop
               + -0.6859*(thisActivity.checkActivityType(thisActivity.activityType,ActivityType.INTERMEDIATE_STOP))
               *(thisTour.primaryDestination.checkActivityPurpose(thisActivity.activityPurpose,ActivityPurpose.SCHOOL))//School Tour Intermediate Stop
               + -0.2703*thisPersonAttributes.householdSize3Plus     //Household Size >=3
               + -0.0880*thisPersonAttributes.singleAdultWithOnePlusChildren     //Single Adult with Child
               +  0.1370*thisPersonAttributes.income60Plus//Household Income >=$60k
               +  5.8489;                                         //Constant

          double shape=1.3229;
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

     
    

}

package com.pb.despair.pt;

import com.pb.common.model.Alternative;
import java.io.PrintWriter;
import java.io.Serializable;
import org.apache.log4j.Logger;
/** 
 * Pattern.java stores the out-of-home activity pattern for each person-day,
 * as well as summary information about the pattern that can
 * be used in model estimation.
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class Pattern implements Alternative, Serializable, Cloneable {
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
     //To hold the pattern
     public StringBuffer dayPattern = new StringBuffer();

     //Simple number of activities by type in pattern variables
     public int homeActivities;
    public int workActivities;
     public int schoolActivities;
     public int shopActivities;
     public int recreateActivities;
     public int otherActivities;
     public int t1Dummy;
     public int t2Dummy;
     public int t3Dummy;
     public int t4Dummy;
     public int t5pDummy;  
     public int wrkDummy;     
     public int schDummy;    
     public int shpDummy;     
     public int recDummy;     
     public int othDummy;     
     public int wkbDummy;
     public int toursEquals1;
     public int toursEquals2;
     public int toursEquals3Plus;
     public int toursEquals3;
     public int toursEquals4;
     public int toursEquals5Plus;
     public int workOnly;
     public int schoolOnly;
     public int shopOnly;
     public int recreateOnly;
     public int otherOnly;
     public int isWeekend;
     public int workTours;
     public int schoolTours;
     public int shopTours;
     public int recreateTours;
     public int otherTours;
     public int workBasedTours;
     public int numberOfToursGT2;
     
     //Number of intermediate stop variables     
     public int tour1IStops;
     public int tour2IStops;
     public int tour3IStops;
     public int tour4PIStops;
      public int workTourIStops;
     public int nonWorkTourIStops;
     public int totalIStops;
     public int IStopsEquals1;
     public int IStopsEquals2Plus;
     public int IStopsEquals2;
     public int IStopsEquals3;
     public int IStopsEquals3Plus;
     public int IStopsEquals4Plus;
     
     //Combination of activities on tour variables
     public int workPSchool;
     public int workPShop;
      public int workPRecreate;
     public int workPOther;
     public int schoolPShop;
     public int schoolPRecreate;
     public int schoolPOther;
     public int shopPRecreate;
     public int shopPOther;
     public int recreatePOther;

     //stops variables
     public int stopsOnWorkTours;
     public int stopsOnSchoolTours;
     public int stopsOnShopTours;
     public int stopsOnRecreateTours;
     public int stopsOnOtherTours;

     //Sequence variables
     public int tour1Purpose;
     public int tour2Purpose;
     public int tour3Purpose;
     public int tour4Purpose;
     public int tour1IsWork;
     public int tour1IsSchool;
     public int workTourNotFirst;
     public int schoolTourNotFirst;
     
     //pattern file variables
     public int observed=1;

     //added from Pattern Alternative//
       public boolean isAvailable=true;
       public boolean hasUtility=false;     
       double utility=0;
       double constant;
       double expConstant;
       String name;
    //the base pattern is homeAllDay
    //private static final Pattern homeAllDay=new Pattern("h");

     
/** Pattern class constructor 
@param word A day-pattern encoded as a String of any length 
with the following character values:  h=home,w=work(no work-based tour),
b=work(work-based tour),c=school,s=shop,r=social/recreation,o=other.
*/
     public Pattern(String word){

          dayPattern.append(word);

          countActivitiesbyPurpose();

          countIntermediateStops();

          countActivityCombinations();
          
          countTours();

     }

     /** counts the number of activities, tours by purpose, stores results in class variables. */
     void countActivitiesbyPurpose(){
          //search through word, count number of activities by purpose
          for(int i=0;i<dayPattern.length();++i){

               char thisChar=dayPattern.charAt(i);

               if(thisChar=='h')
                    ++homeActivities;
               if(thisChar=='w'){
                    ++workActivities;
                    wrkDummy=1;
               }
               if(thisChar=='c'){
                    ++schoolActivities;
                    schDummy=1;
               }
               if(thisChar=='s'){
                    ++shopActivities;
                    shpDummy=1;
               }
               if(thisChar=='r'){
                    ++recreateActivities;
                    recDummy=1;
               }
               if(thisChar=='o'){
                    ++otherActivities;
                    othDummy=1;
               }
               if(thisChar=='b'){
                    ++workActivities;
                    ++workBasedTours;
                    wkbDummy=1;
               }
          }

          if(homeActivities>=2)
               t1Dummy=1;
          if(homeActivities>=3)
               t2Dummy=1;
          if(homeActivities>=4)
               t3Dummy=1;
          if(homeActivities>=5)
               t4Dummy=1;
          if(homeActivities>=6)
               t5pDummy=1;
               
          if(homeActivities==2)
               toursEquals1=1;
          if(homeActivities==3)
               toursEquals2=1;
          if(homeActivities>=4)
               toursEquals3Plus=1;
          if(homeActivities==4)
               toursEquals3=1;
          if(homeActivities==5)
               toursEquals4=1;
          if(homeActivities>=6)
               toursEquals5Plus=1;
               
          
          if(wrkDummy==1 && schDummy==0 && shpDummy==0 && recDummy==0 && othDummy==0)
               workOnly=1;
          else if(wrkDummy==0 && schDummy==1 && shpDummy==0 && recDummy==0 && othDummy==0)
               schoolOnly=1;
          else if(wrkDummy==0 && schDummy==0 && shpDummy==1 && recDummy==0 && othDummy==0)
               shopOnly=1;
          else if(wrkDummy==0 && schDummy==0 && shpDummy==0 && recDummy==1 && othDummy==0)
               recreateOnly=1;
          else if(wrkDummy==0 && schDummy==0 && shpDummy==0 && recDummy==0 && othDummy==1)
               otherOnly=1;
               

     } //end countActivitiesbyPurpose()

     public String getTourString(int tourNumber){
          
          if(homeActivities<2){
               logger.error("Error: less than 2 home activities on pattern "+dayPattern);
               logger.error("Cannot return tour number "+tourNumber+ ". Returning null");
               return null;
          }

          String dayString = new String(dayPattern.toString());
          StringBuffer tourString = new StringBuffer();
          //the following indices are used to locate the at-home activities on either end of a tour
          int lastHomeActivityIndex=0;
          int firstHomeActivityIndex;
          int n=0;
          //get desired tour
          while(dayString.length()>lastHomeActivityIndex+1){
               ++n;
               firstHomeActivityIndex=lastHomeActivityIndex;
               lastHomeActivityIndex=dayString.indexOf("h",firstHomeActivityIndex+1);
               if(n==tourNumber){
                    tourString.append(dayString.substring(firstHomeActivityIndex,lastHomeActivityIndex+1));
                    break;          
               }
          }
//          System.out.println("Got string "+tourString);
          return tourString.toString();
     }


     /** counts the number of intermediate stops in this word, stores results in class variables. */
     void countIntermediateStops(){
          if(homeActivities>=2)
               tour1IStops=getTourString(1).length()-3;
          if(homeActivities>=3)
               tour2IStops=getTourString(2).length()-3;
          if(homeActivities>=4)
               tour3IStops=getTourString(3).length()-3;
               
          if(homeActivities>=5)
               for(int i=5;i<=homeActivities;++i)
                    tour4PIStops += getTourString(i-1).length()-3;
               
          totalIStops=tour1IStops+tour2IStops+tour3IStops+tour4PIStops;
          if(totalIStops==1)
               IStopsEquals1=1;
          if(totalIStops>=2)
               IStopsEquals2Plus=1;
          if(totalIStops==2)
               IStopsEquals2=1;
          if(totalIStops==3)
               IStopsEquals3=1;
          if(totalIStops>=3)
               IStopsEquals3Plus=1;
          if(totalIStops>=4)
               IStopsEquals4Plus=1;



     }

     /** counts the number of activity combinations in this word, stores results in class variables */
     void countActivityCombinations(){ 
          int tourNumber=1;
          if(homeActivities>1){
               while(homeActivities>=(tourNumber+1)){
//                    System.out.println("pattern "+dayPattern.toString()+" has "+homeActivities+" homeActivities");
                    String thisTour=getTourString(tourNumber);
                    boolean workActivity=false;
                    boolean schoolActivity=false;
                    boolean shopActivity=false;
                    boolean recreateActivity=false;
                    boolean otherActivity=false;
                    //cycle through letters on this tour between two home locations
                    for(int i=1;i<(thisTour.length()-1);++i){
                         if(thisTour.charAt(i)=='w'||thisTour.charAt(i)=='b')
                              workActivity=true;
                         if(thisTour.charAt(i)=='c')
                              schoolActivity=true;
                         if(thisTour.charAt(i)=='s')
                              shopActivity=true;
                         if(thisTour.charAt(i)=='r')
                              recreateActivity=true;
                         if(thisTour.charAt(i)=='o')
                              otherActivity=true;
                    } //end cycling through letters of this tour
                    //number of stops
                    if(workActivity && thisTour.length()>3)
                         stopsOnWorkTours += thisTour.length()-3;
                    else if(schoolActivity && thisTour.length()>3)
                         stopsOnSchoolTours += thisTour.length()-3;
                    else if(shopActivity && thisTour.length()>3)
                         stopsOnShopTours += thisTour.length()-3;
                    else if(recreateActivity && thisTour.length()>3)
                         stopsOnRecreateTours += thisTour.length()-3;
                    else if(otherActivity && thisTour.length()>3)
                         stopsOnOtherTours += thisTour.length()-3;



                    //combinations
                    if(workActivity && schoolActivity)
                         ++workPSchool;
                    if(workActivity && shopActivity)
                         ++workPShop;
                    if(workActivity && recreateActivity)
                         ++workPRecreate;
                    if(workActivity && otherActivity)
                         ++workPOther;
                    if(schoolActivity && shopActivity)
                         ++schoolPShop;
                    if(schoolActivity && recreateActivity)
                         ++schoolPRecreate;
                    if(schoolActivity && otherActivity)
                         ++schoolPOther;
                    if(shopActivity && recreateActivity)
                         ++shopPRecreate;
                    if(shopActivity && otherActivity)          
                         ++shopPOther;
                    if(recreateActivity && otherActivity)
                         ++recreatePOther;


                    //sequence
                    if(tourNumber==1){  //these purposes do NOT correspond to the values listed in "ActivityPurpose" but
                         if(workActivity)   //they are not used outside this class so it is irrelevant.
                              tour1Purpose=1;
                         else if(schoolActivity)
                              tour1Purpose=2;
                         else if(shopActivity)
                              tour1Purpose=3;
                         else if(recreateActivity)
                              tour1Purpose=4;
                         else if(otherActivity)
                              tour1Purpose=5;
                    }else if(tourNumber==2){
                         if(workActivity)
                              tour2Purpose=1;
                         else if(schoolActivity)
                              tour2Purpose=2;
                         else if(shopActivity)
                              tour2Purpose=3;
                         else if(recreateActivity)
                              tour2Purpose=4;
                         else if(otherActivity)
                              tour2Purpose=5;
                    }else if(tourNumber==3){
                         if(workActivity)
                              tour3Purpose=1;
                         else if(schoolActivity)
                              tour3Purpose=2;
                         else if(shopActivity)
                              tour3Purpose=3;
                         else if(recreateActivity)
                              tour3Purpose=4;
                         else if(otherActivity)
                              tour3Purpose=5;
                    }else if(tourNumber==4){
                         if(workActivity)
                              tour4Purpose=1;
                         else if(schoolActivity)
                              tour4Purpose=2;
                         else if(shopActivity)
                              tour4Purpose=3;
                         else if(recreateActivity)
                              tour4Purpose=4;
                         else if(otherActivity)
                              tour4Purpose=5;
                    }
                    ++tourNumber;
               } //end this tour     
          }
          if(tour1Purpose==1)
               tour1IsWork=1;
          else if(tour1Purpose==2)
               tour1IsSchool=1;
     }
     
          /*
     * Creates a pattern that is just the current tour, then creates a Tour object
     * using that pattern.  Counts number of tours for each tour purpose using the
     * primaryDestination activity of the Tour
     */
     public void countTours(){
          
          if(homeActivities>1){
               for(int i=0;i<(homeActivities-1);++i){
                    String tourString = new String();
                    tourString =getTourString(i+1);
                    Tour thisTour = new Tour(tourString);
                    
                    //increment tour count variables
                    if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK || 
                            thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED)
                         ++workTours;
                    else if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.SCHOOL)
                         ++schoolTours;
                    else if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.SHOP)
                         ++shopTours;
                    else if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.RECREATE)
                         ++recreateTours;
                    else if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.OTHER)
                         ++otherTours;
                    else if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED)
                         ++workBasedTours;
                         
                    if((thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK || 
                   thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED) && i>1)
                         workTourNotFirst=1;
                    
                    if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.SCHOOL && i>1)
                         schoolTourNotFirst=1;
                         
                    if(i>2)
                         ++numberOfToursGT2;
               }
          }
               
          
     }
     public void print(){
               //To hold the pattern
          logger.info("***************************");
          logger.info("dayPattern = "+ dayPattern);

          //Simple number of activities by type in pattern variables
          logger.info("homeActivities =       "+ homeActivities);
          logger.info("workActivities =       "+ workActivities);
          logger.info("schoolActivities =     "+ schoolActivities);
          logger.info("shopActivities =       "+ shopActivities);
          logger.info("recreateActivities =   "+ recreateActivities);
          logger.info("otherActivities =      "+ otherActivities);
          logger.info("t1Dummy =              "+ t1Dummy);
          logger.info("t2Dummy =              "+ t2Dummy);
          logger.info("t3Dummy =              "+ t3Dummy);
          logger.info("t4Dummy =              "+ t4Dummy);
          logger.info("t5pDummy =             "+ t5pDummy);
          logger.info("wrkDummy =             "+ wrkDummy);
          logger.info("schDummy =             "+ schDummy);
          logger.info("shpDummy =             "+ shpDummy);
          logger.info("recDummy =             "+ recDummy);
          logger.info("othDummy =             "+ othDummy);
          logger.info("wkbDummy =             "+ wkbDummy);
          logger.info("toursEquals1 =         "+ toursEquals1);
          logger.info("toursEquals2 =         "+ toursEquals2);
          logger.info("toursEquals3Plus =     "+ toursEquals3Plus);
          logger.info("toursEquals3 =         "+ toursEquals3);
          logger.info("toursEquals4 =         "+ toursEquals4);
          logger.info("toursEquals5Plus =     "+ toursEquals5Plus);
          logger.info("workOnly =             "+ workOnly);
          logger.info("schoolOnly =           "+ schoolOnly);
          logger.info("shopOnly =             "+ shopOnly);
          logger.info("recreateOnly =         "+ recreateOnly);
          logger.info("otherOnly =            "+ otherOnly);
          logger.info("isWeekend =            "+ isWeekend);
          logger.info("workTours =            "+ workTours);
          logger.info("schoolTours =          "+ schoolTours);
          logger.info("shopTours =            "+ shopTours);
          logger.info("recreateTours =        "+ recreateTours);
          logger.info("otherTours =           "+ otherTours);
          logger.info("workBasedTours =       "+ workBasedTours);
          logger.info("numberOfToursGT2 =     "+ numberOfToursGT2);
                                                                                                               
                                                                                                               
          //Number of intermediate stop variables                        
          logger.info("tour1IStops =          "+ tour1IStops);
          logger.info("tour2IStops =          "+ tour2IStops);
          logger.info("tour3IStops =          "+ tour3IStops);
          logger.info("tour4PIStops =         "+ tour4PIStops);
          logger.info("workTourIStops =       "+ workTourIStops);
          logger.info("nonWorkTourIStops =    "+ nonWorkTourIStops);
          logger.info("totalIStops =          "+ totalIStops);
          logger.info("IStopsEquals1 =        "+ IStopsEquals1);
          logger.info("IStopsEquals2Plus =    "+ IStopsEquals2Plus);
          logger.info("IStopsEquals2 =        "+ IStopsEquals2);
          logger.info("IStopsEquals3 =        "+ IStopsEquals3);
          logger.info("IStopsEquals3Plus =    "+ IStopsEquals3Plus);
          logger.info("IStopsEquals4Plus =    "+ IStopsEquals4Plus);
                                                                                                               
          //Combination of activities on tour variables                   
          logger.info("workPSchool =          "+ workPSchool);
          logger.info("workPShop =            "+ workPShop);
          logger.info("workPRecreate =        "+ workPRecreate);
          logger.info("workPOther =           "+ workPOther);
          logger.info("schoolPShop =          "+ schoolPShop);
          logger.info("schoolPRecreate =      "+ schoolPRecreate);
          logger.info("schoolPOther =         "+ schoolPOther);
          logger.info("shopPRecreate =        "+ shopPRecreate);
          logger.info("shopPOther =           "+ shopPOther);
          logger.info("recreatePOther =       "+ recreatePOther);
                                                                                         
          //stops variables                                                               
          logger.info("stopsOnWorkTours =     "+ stopsOnWorkTours);
          logger.info("stopsOnSchoolTours =   "+ stopsOnSchoolTours);
          logger.info("stopsOnShopTours =     "+ stopsOnShopTours);
          logger.info("stopsOnRecreateTours = "+ stopsOnRecreateTours);
          logger.info("stopsOnOtherTours =    "+ stopsOnOtherTours);
                                                                                                              
          //Sequence variables                                                              
          logger.info("tour1Purpose =         "+ tour1Purpose);
          logger.info("tour2Purpose =         "+ tour2Purpose);
          logger.info("tour3Purpose =         "+ tour3Purpose);
          logger.info("tour4Purpose =         "+ tour4Purpose);
          logger.info("tour1IsWork =          "+ tour1IsWork);
          logger.info("tour1IsSchool =        "+ tour1IsSchool);
          logger.info("workTourNotFirst =     "+ workTourNotFirst);
          logger.info("schoolTourNotFirst =   "+ schoolTourNotFirst);
                                                                                                               
     }
     
     
     public void print(PrintWriter f){
          f.print(     
               homeActivities+" "+
              workActivities+" "+
               schoolActivities+" "+
               shopActivities+" "+
               recreateActivities+" "+
               otherActivities+" "+
               workBasedTours+" "+
               tour1IStops+" "+
               tour2IStops+" "+
               tour3IStops+" "+
               tour4PIStops+" "+
                workTourIStops+" "+
               nonWorkTourIStops+" "+
               workPSchool+" "+
               workPShop+" "+
                workPRecreate+" "+
               workPOther+" "+
               schoolPShop+" "+
               schoolPRecreate+" "+
               schoolPOther+" "+
               shopPRecreate+" "+
               shopPOther+" "+
               recreatePOther+" "+
               stopsOnWorkTours+" "+
               stopsOnSchoolTours+" "+
               stopsOnShopTours+" "+
               stopsOnRecreateTours+" "+
               stopsOnOtherTours+" "+
               tour1Purpose+" "+
               tour2Purpose+" "+
               tour3Purpose+" "+
               tour4Purpose+" "+
               t1Dummy+" "+
               t2Dummy+" "+
               t3Dummy+" "+
               t4Dummy+" "+
               t5pDummy+" "+
               wrkDummy+" "+     
               schDummy+" "+     
               shpDummy+" "+     
               recDummy+" "+     
               othDummy+" "+     
               wkbDummy+" "     
          );
     }
     
//to write to a text file, csv format
     public void printCSV(PrintWriter file){

          file.print(
               dayPattern+","
               +homeActivities+","
               +workActivities+","
               +schoolActivities+","
               +shopActivities+","
               +recreateActivities+","
               +otherActivities

                                                                                                                                                         
          );
     }
     
     public boolean equals(Object obj){
          
          Pattern comparePattern = (Pattern)obj;
          boolean tf=false;
        String compareString=comparePattern.dayPattern.toString();
          String thisString=this.dayPattern.toString();
          if(compareString.compareTo(thisString)==0)
               tf=true;
          return tf;
     }

/////////////////////////////////
//Added from PatternAlternative//
/////////////////////////////////
    public void setConstant(double constant){
        this.constant = constant;
    }
    
    public double getConstant(){
        return constant;
    }
    public void setExpConstant(double expConstant){
        this.expConstant =expConstant;
    }
    public double getExpConstant(){
         return expConstant;
    }
    /**
    Set the utility of the alternative.
    @param util  Utility value.
    */
    public void setUtility(double util){
        utility=util;
    }    /**

    /** 
    Get the name of this alternative.
    @return The name of the alternative
    */
    public String getName(){
        return name;
    }
    /** 
    Set the name of this alternative.
    @param name The name of the alternative
    */
    public void setName(String name){
        this.name=name;
    }
    /** 
    Get the availability of this alternative.
    @return True if alternative is available
    */
    public boolean isAvailable(){
        return isAvailable;
    }
    /** 
    Set the availability of this alternative.
    @param available True if alternative is available
    */
    public void setAvailability(boolean available){
        isAvailable=available;
    }
    



     /**
     *
     * To print the pattern to the screen
     *
     */
//     public void print(){
//          thisPattern.print();
//          logger.info("Utility = "+utility);
//     }

     /** This method calculates the utility of a given pattern for a given person
*/
     public void calcUtility(PatternChoiceParameters params, PersonPatternChoiceAttributes persAttr){
         utility=0;
         hasUtility=true;

         if(homeActivities==1){
           return;
         }


          //work tours not available to non-workers
          if(persAttr.worker==0){
               if(wrkDummy==1 || wkbDummy==1){
                    isAvailable=false;
                   return;
               }
          }
          //school tours not available to non-students
          if(persAttr.student==0){
               if(schDummy==1){
                    isAvailable=false;
                   return;
               }
          }

          if(persAttr.worksTwoJobs==0){
               if(workActivities>1){
                    isAvailable=false;
                   return;
               }
          }

               utility=(
                 params.workWorker                              *  wrkDummy * persAttr.worker
               + params.schoolStudent                           *  schDummy * persAttr.student
               + params.shopActivityDummyFemale                 *  shpDummy * persAttr.female
               + params.shopActivityDummyUnemployed             *  shpDummy * persAttr.unemployed
               + params.shopActivityDummySize1                  *  shpDummy * persAttr.householdSize1
               + params.shopActivityDummySize2                  *  shpDummy * persAttr.householdSize2
               + params.shopActivityDummySize3p                 *  shpDummy * persAttr.householdSize3Plus
               + params.shopActivitiesSingleParentWithChild0_5  *  shopActivities * persAttr.singleWithChild0_5
               + params.recreateActivityDummyAge0_21            *  recDummy * persAttr.age00To21
               + params.recreateActivityDummyAge21_25           *  recDummy * persAttr.age21To25
               + params.recreateActivityDummyAge25_60           *  recDummy * persAttr.age25To60
               + params.recreateActivityDummyWorker             *  recDummy * persAttr.worker
               + params.recreateActivityDummyUnemployed         *  recDummy * persAttr.unemployed
               + params.numberToursAge0_5                       *  (homeActivities-1) * persAttr.age00To05
               + params.numberToursAge5_15                      *  (homeActivities-1) * persAttr.age05To15
               + params.numberToursAge15_25                     *  (homeActivities-1) * persAttr.age15To25
               + params.numberToursAge25_50                     *  (homeActivities-1) * persAttr.age25To50
               + params.numberToursAge50_70                     * (homeActivities-1) * persAttr.age50To70
               + params.numberToursAge70_80                     * (homeActivities-1) * persAttr.age70To80
               + params.numberToursAge80p                       * (homeActivities-1) * persAttr.age80Plus
               + params.workStopsChildrenAge0_15                * workTourIStops * persAttr.age00To15
               + params.workStopsIncome0_15k                    * workTourIStops * persAttr.householdIncome00To15k
               + params.workStopsIncome50kp                     * workTourIStops * persAttr.householdIncome50kPlus
               + params.workStopsAutos0                         * workTourIStops * persAttr.autos0
               + params.workStops_by_NWKTours                   * workTourIStops * ((homeActivities-1)-workActivities)
               + params.workTourNotFirst                        * workTourNotFirst
               + params.schoolToursNotFirst                     * schoolTourNotFirst
               + params.workStops                               * stopsOnWorkTours
               + params.schoolStops                             * stopsOnSchoolTours
               + params.shopStops                               * stopsOnShopTours
               + params.nonWorkStopsChildrenAge0_15             * nonWorkTourIStops * persAttr.childlt15
               + params.nonWorkStopsIncome50kp                  * nonWorkTourIStops * persAttr.householdIncome50kPlus 
               + params.nonWorkStopsAutos0                      * nonWorkTourIStops * persAttr.autos0                
               + params.shopActivities                          * shopActivities                                           
               + params.recreateActivities                      * recreateActivities                                       
               + params.otherActivities                         * otherActivities                                          
               + params.workBasedWorker                         * workBasedTours * persAttr.worker        
//               + params.shopTours                               * shopTours  
//               + params.otherTours                              * otherTours                   
               + params.workBasedTours                          * workBasedTours                   
               + params.shopToursByShopDCLogsum                 * shopTours * persAttr.shopDCLogsum                   
               + params.otherToursByOtherDCLogsum               * otherTours * persAttr.otherDCLogsum
               + params.workBasedToursByWorkBasedDCLogsum       * workBasedTours * persAttr.workBasedDCLogsum
               + params.homeBasedToursGT2ByWorkBasedTours       * numberOfToursGT2*workBasedTours
               + params.shopToursAutos0                         * shopTours * persAttr.autos0
               + params.otherToursAutos0                        * otherTours * persAttr.autos0
               + params.workBasedToursAutos0                    * workBasedTours * persAttr.autos0
               + params.numberToursIfChildren0_5                * (homeActivities-1) * persAttr.child00To05
               + params.numberToursIfChildren5_15               * (homeActivities-1) * persAttr.child05To10
               + params.numberToursIfChildren5_15               * (homeActivities-1) * persAttr.child10To15
               + params.numberToursIncome0_15k                  * (homeActivities-1) * persAttr.householdIncome00To15k
               + params.numberToursIncome15_25k                 * (homeActivities-1) * persAttr.householdIncome15To25k
               + params.numberToursIncome25_55k                 * (homeActivities-1) * persAttr.householdIncome25To55k
               + params.numberToursIncome55kp                   * (homeActivities-1) * persAttr.householdIncome50kPlus
               + params.numberToursAutos0                       * (homeActivities-1) * persAttr.autos0
               + params.numberToursAutosLtAdults                * (homeActivities-1) * persAttr.autosLessThanAdults
               + params.otherActivityDummy                      * othDummy
               + params.workBasedDummy                          * wkbDummy  
               + params.recreateDummyAge5_10                    * recDummy * persAttr.age05To10
               + params.shopActivitiesIncome0_15k               * shopActivities * persAttr.householdIncome00To15k
               + params.shopActivitiesIncome15_25k              * shopActivities * persAttr.householdIncome15To25k
               + params.shopActivitiesIncome25_55k              * shopActivities * persAttr.householdIncome25To55k
               + params.shopActivitiesIncome55kp                * shopActivities * persAttr.householdIncome50kPlus
               + params.recreateActivitiesAge10_20              * recreateActivities * persAttr.age10To20
               + params.recreateActivitiesAge20_30              * recreateActivities * persAttr.age20To30
               + params.recreateActivitiesAge30_50              * recreateActivities * persAttr.age30To50
               + params.recreateStops                           * stopsOnRecreateTours
               + params.otherStops                              * stopsOnOtherTours
               + params.workDummyIndustryRetail                 * wrkDummy * persAttr.industryEqualsRetail          
               + params.workDummyIndustryPersonalServices         * wrkDummy * persAttr.industryEqualsPersonalServices

          );

         //moving this to the beginning of the method to avoid
         //unneccessary utility calculations.
//          if(homeActivities==1)
//               utility=0;
//
//          //work tours not available to non-workers
//          if(persAttr.worker==0)
//               if(wrkDummy==1 || wkbDummy==1)
//                    isAvailable=false;
//
//          //school tours not available to non-students
//          if(persAttr.student==0)
//               if(schDummy==1)
//                    isAvailable=false;
//
//          if(persAttr.worksTwoJobs==0)
//               if(workActivities>1)
//                    isAvailable=false;
//
//          hasUtility=true;
     
     }
     public double getUtility(){
          if(!hasUtility){
               logger.fatal("Error: Utility not calculated for "+dayPattern+"\n");
              //TODO - throw a runtime exception instead and log to the node exception files
               System.exit(1);
          };
          return utility;
     };
     
     public void printDebug(PatternChoiceParameters params, PersonPatternChoiceAttributes persAttr){
                                                                                                                            
          logger.info("params.workWorker  * wrkDummy * persAttr.worker                                                                       "+(params.workWorker                              *  wrkDummy * persAttr.worker                                                       ));         
          logger.info("params.schoolStudent  * schDummy * persAttr.student                                                                   "+(params.schoolStudent                           *  schDummy * persAttr.student                                                      ));        
          logger.info("params.shopActivityDummyFemale  * shpDummy * persAttr.female                                                          "+(params.shopActivityDummyFemale                 *  shpDummy * persAttr.female                                                       ));        
          logger.info("params.shopActivityDummyUnemployed * shpDummy * persAttr.unemployed                                                   "+(params.shopActivityDummyUnemployed             *  shpDummy * persAttr.unemployed                                                   ));        
          logger.info("params.shopActivityDummySize1  * shpDummy * persAttr.householdSize1                                                   "+(params.shopActivityDummySize1                  *  shpDummy * persAttr.householdSize1                                               ));        
          logger.info("params.shopActivityDummySize2  * shpDummy * persAttr.householdSize2                                                   "+(params.shopActivityDummySize2                  *  shpDummy * persAttr.householdSize2                                               ));        
          logger.info("params.shopActivityDummySize3p  * shpDummy * persAttr.householdSize3Plus                                              "+(params.shopActivityDummySize3p                 *  shpDummy * persAttr.householdSize3Plus                                           ));        
          logger.info("params.shopActivitiesSingleParentWithChild0_5 * shopActivities * persAttr.singleWithChild0_5                          "+(params.shopActivitiesSingleParentWithChild0_5  *  shopActivities * persAttr.singleWithChild0_5                                     ));        
          logger.info("params.recreateActivityDummyAge0_21 * recDummy * persAttr.age00To21                                                   "+(params.recreateActivityDummyAge0_21            *  recDummy * persAttr.age00To21                                                    ));        
          logger.info("params.recreateActivityDummyAge21_25 * recDummy * persAttr.age21To25                                                  "+(params.recreateActivityDummyAge21_25           *  recDummy * persAttr.age21To25                                                    ));        
          logger.info("params.recreateActivityDummyAge25_60 * recDummy * persAttr.age25To60                                                  "+(params.recreateActivityDummyAge25_60           *  recDummy * persAttr.age25To60                                                    ));        
          logger.info("params.recreateActivityDummyWorker * recDummy * persAttr.worker                                                       "+(params.recreateActivityDummyWorker             *  recDummy * persAttr.worker                                                       ));        
          logger.info("params.recreateActivityDummyUnemployed * recDummy * persAttr.unemployed                                               "+(params.recreateActivityDummyUnemployed         *  recDummy * persAttr.unemployed                                                   ));        
          logger.info("params.numberToursAge0_5  * (homeActivities-1) * persAttr.age00To05                                                   "+(params.numberToursAge0_5                       *  (homeActivities-1) * persAttr.age00To05                                          ));        
          logger.info("params.numberToursAge5_15  * (homeActivities-1) * persAttr.age05To15                                                  "+(params.numberToursAge5_15                      *  (homeActivities-1) * persAttr.age05To15                                          ));        
          logger.info("params.numberToursAge15_25  * (homeActivities-1) * persAttr.age15To25                                                 "+(params.numberToursAge15_25                     *  (homeActivities-1) * persAttr.age15To25                                          ));        
          logger.info("params.numberToursAge25_50  * (homeActivities-1) * persAttr.age25To50                                                 "+(params.numberToursAge25_50                     *  (homeActivities-1) * persAttr.age25To50                                          ));        
          logger.info("params.numberToursAge50_70  * (homeActivities-1) * persAttr.age50To70                                                 "+(params.numberToursAge50_70                     * (homeActivities-1) * persAttr.age50To70                                           ));        
          logger.info("params.numberToursAge70_80  * (homeActivities-1) * persAttr.age70To80                                                 "+(params.numberToursAge70_80                     * (homeActivities-1) * persAttr.age70To80                                           ));        
          logger.info("params.numberToursAge80p  * (homeActivities-1) * persAttr.age80Plus                                                   "+(params.numberToursAge80p                       * (homeActivities-1) * persAttr.age80Plus                                           ));        
          logger.info("params.workStopsChildrenAge0_15 * workTourIStops * persAttr.age00To15                                                 "+(params.workStopsChildrenAge0_15                * workTourIStops * persAttr.age00To15                                               ));        
          logger.info("params.workStopsIncome0_15k  * workTourIStops * persAttr.householdIncome00To15k                                       "+(params.workStopsIncome0_15k                    * workTourIStops * persAttr.householdIncome00To15k                                  ));        
          logger.info("params.workStopsIncome50kp  * workTourIStops * persAttr.householdIncome50kPlus                                        "+(params.workStopsIncome50kp                     * workTourIStops * persAttr.householdIncome50kPlus                                  ));        
          logger.info("params.workStopsAutos0  * workTourIStops * persAttr.autos0                                                            "+(params.workStopsAutos0                         * workTourIStops * persAttr.autos0                                                  ));        
          logger.info("params.workStops_by_NWKTours  * workTourIStops * ((homeActivities-1)-workActivities)          "+(params.workStops_by_NWKTours                   * workTourIStops * ((homeActivities-1)-workActivities)      ));        
          logger.info("params.workTourNotFirst  * workTourNotFirst                                                                           "+(params.workTourNotFirst                        * workTourNotFirst                                                                  ));        
          logger.info("params.schoolToursNotFirst  * schoolTourNotFirst                                                                      "+(params.schoolToursNotFirst                     * schoolTourNotFirst                                                                ));        
          logger.info("params.workStops  * stopsOnWorkTours                                                                                  "+(params.workStops                               * stopsOnWorkTours                                                                  ));        
          logger.info("params.schoolStops  * stopsOnSchoolTours                                                                              "+(params.schoolStops                             * stopsOnSchoolTours                                                                ));        
          logger.info("params.shopStops  * stopsOnShopTours                                                                                  "+(params.shopStops                               * stopsOnShopTours                                                                  ));        
          logger.info("params.nonWorkStopsChildrenAge0_15 * nonWorkTourIStops * persAttr.childlt15                                           "+(params.nonWorkStopsChildrenAge0_15             * nonWorkTourIStops * persAttr.childlt15                                            ));        
          logger.info("params.nonWorkStopsIncome50kp  * nonWorkTourIStops * persAttr.householdIncome50kPlus                                  "+(params.nonWorkStopsIncome50kp                  * nonWorkTourIStops * persAttr.householdIncome50kPlus                               ));        
          logger.info("params.nonWorkStopsAutos0  * nonWorkTourIStops * persAttr.autos0                                                      "+(params.nonWorkStopsAutos0                      * nonWorkTourIStops * persAttr.autos0                                               ));        
          logger.info("params.shopActivities  * shopActivities                                                                               "+(params.shopActivities                          * shopActivities                                                                    ));        
          logger.info("params.recreateActivities  * recreateActivities                                                                       "+(params.recreateActivities                      * recreateActivities                                                                ));        
          logger.info("params.otherActivities  * otherActivities                                                                             "+(params.otherActivities                         * otherActivities                                                                   ));        
          logger.info("params.workBasedWorker  * workBasedTours * persAttr.worker                                                            "+(params.workBasedWorker                         * workBasedTours * persAttr.worker                                                  ));        
//          logger.info("params.shopTours  * shopTours                                                                                         "+(params.shopTours                               * shopTours                                                                         ));        
//          logger.info("params.otherTours  * otherTours                                                                                       "+(params.otherTours                              * otherTours                                                                        ));        
          logger.info("params.workBasedTours  * workBasedTours                                                                               "+(params.workBasedTours                          * workBasedTours                                                                    ));        
          logger.info("params.shopToursByShopDCLogsum  * shopTours * persAttr.shopDCLogsum                                                   "+(params.shopToursByShopDCLogsum                 * shopTours * persAttr.shopDCLogsum                                                 ));        
          logger.info("params.otherToursByOtherDCLogsum * otherTours * persAttr.otherDCLogsum                                                "+(params.otherToursByOtherDCLogsum               * otherTours * persAttr.otherDCLogsum                                               ));        
          logger.info("params.workBasedToursByWorkBasedDCLogsum * workBasedTours * persAttr.workBasedDCLogsum                                "+(params.workBasedToursByWorkBasedDCLogsum       * workBasedTours * persAttr.workBasedDCLogsum                                       ));        
          logger.info("params.homeBasedToursGT2ByWorkBasedTours * numberOfToursGT2*workBasedTours                                "+(params.homeBasedToursGT2ByWorkBasedTours       * numberOfToursGT2*workBasedTours                                       ));        
          logger.info("params.shopToursAutos0  * shopTours * persAttr.autos0                                                                 "+(params.shopToursAutos0                         * shopTours * persAttr.autos0                                                       ));        
          logger.info("params.otherToursAutos0  * otherTours * persAttr.autos0                                                               "+(params.otherToursAutos0                        * otherTours * persAttr.autos0                                                      ));        
          logger.info("params.workBasedToursAutos0  * workBasedTours * persAttr.autos0                                                       "+(params.workBasedToursAutos0                    * workBasedTours * persAttr.autos0                                                  ));        
     }

    public Object clone(){
        Pattern newPattern;
        try {
            newPattern = (Pattern) super.clone();
            newPattern.dayPattern = dayPattern;

            //Simple number of activities by type in pattern variables
            newPattern.homeActivities = homeActivities;
            newPattern.workActivities = workActivities;
            newPattern.schoolActivities = schoolActivities;
            newPattern.shopActivities=shopActivities;
            newPattern.recreateActivities=recreateActivities;
            newPattern.otherActivities=otherActivities;
            newPattern.t1Dummy=t1Dummy;
            newPattern.t2Dummy=t2Dummy;
            newPattern.t3Dummy=t3Dummy;
            newPattern.t4Dummy=t4Dummy;
             newPattern.t5pDummy=t5pDummy;
             newPattern.wrkDummy=wrkDummy;
             newPattern.schDummy=schDummy;
             newPattern.shpDummy=shpDummy;
             newPattern.recDummy=recDummy;
             newPattern.othDummy=othDummy;
             newPattern.wkbDummy=wkbDummy;
             newPattern.toursEquals1=toursEquals1;
             newPattern.toursEquals2=toursEquals2;
             newPattern.toursEquals3Plus=toursEquals3Plus;
             newPattern.toursEquals3=toursEquals3;
             newPattern.toursEquals4=toursEquals4;
             newPattern.toursEquals5Plus=toursEquals5Plus;
             newPattern.workOnly=workOnly;
             newPattern.schoolOnly=schoolOnly;
             newPattern.shopOnly=shopOnly;
             newPattern.recreateOnly=recreateOnly;
             newPattern.otherOnly=otherOnly;
             newPattern.isWeekend=isWeekend;
             newPattern.workTours=workTours;
             newPattern.schoolTours=schoolTours;
             newPattern.shopTours=shopTours;
             newPattern.recreateTours=recreateTours;
             newPattern.otherTours=otherTours;
             newPattern.workBasedTours=workBasedTours;
             newPattern.numberOfToursGT2=numberOfToursGT2;

             //Number of intermediate stop variables
             newPattern.tour1IStops=tour1IStops;
             newPattern.tour2IStops=tour2IStops;
             newPattern.tour3IStops=tour3IStops;
             newPattern.tour4PIStops=tour4PIStops;
             newPattern.workTourIStops=workTourIStops;
             newPattern.nonWorkTourIStops=nonWorkTourIStops;
             newPattern.totalIStops=totalIStops;
             newPattern.IStopsEquals1=IStopsEquals1;
             newPattern.IStopsEquals2Plus=IStopsEquals2Plus;
             newPattern.IStopsEquals2=IStopsEquals2;
             newPattern.IStopsEquals3=IStopsEquals3;
             newPattern.IStopsEquals3Plus=IStopsEquals3Plus;
             newPattern.IStopsEquals4Plus=IStopsEquals4Plus;

             //Combination of activities on tour variables
             newPattern.workPSchool=workPSchool;
             newPattern.workPShop=workPShop;
              newPattern.workPRecreate=workPRecreate;
             newPattern.workPOther=workPOther;
             newPattern.schoolPShop=schoolPShop;
             newPattern.schoolPRecreate=schoolPRecreate;
             newPattern.schoolPOther=schoolPOther;
             newPattern.shopPRecreate=shopPRecreate;
             newPattern.shopPOther=shopPOther;
             newPattern.recreatePOther=recreatePOther;

             //stops variables
             newPattern.stopsOnWorkTours=stopsOnWorkTours;
             newPattern.stopsOnSchoolTours=stopsOnSchoolTours;
             newPattern.stopsOnShopTours=stopsOnShopTours;
             newPattern.stopsOnRecreateTours=stopsOnRecreateTours;
             newPattern.stopsOnOtherTours=stopsOnOtherTours;

             //Sequence variables
             newPattern.tour1Purpose=tour1Purpose;
             newPattern.tour2Purpose=tour2Purpose;
             newPattern.tour3Purpose=tour3Purpose;
             newPattern.tour4Purpose=tour4Purpose;
             newPattern.tour1IsWork=tour1IsWork;
             newPattern.tour1IsSchool=tour1IsSchool;
             newPattern.workTourNotFirst=workTourNotFirst;
             newPattern.schoolTourNotFirst=schoolTourNotFirst;

             //pattern file variables
             newPattern.observed=observed;

             //added from Pattern Alternative//
             newPattern.isAvailable=isAvailable;
             newPattern.hasUtility=hasUtility;
             newPattern.utility=utility;
             newPattern.constant=constant;
             newPattern.expConstant=expConstant;
             newPattern.name=name;
             }
             catch (CloneNotSupportedException e) {
                throw new RuntimeException(e.toString());
             }
               return newPattern;
            }

    public static void main(String[] args) {
        Pattern testPattern = new Pattern("hsssh");
        testPattern.print();
    }
}

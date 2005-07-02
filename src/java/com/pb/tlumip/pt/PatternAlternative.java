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

import com.pb.common.model.Alternative;
/**
 * Class to set availability and utility of Pattern alternatives
 * 
 * @author Joel Freedman
 * @version 1.0 12/1/2003
 * 
 */
public class PatternAlternative implements Alternative{

     public boolean isAvailable=true;
     public boolean hasUtility=false;
     
     double utility=0;
     double constant;
     double expConstant;
     String name;
     
     Pattern thisPattern;
     
    // all coefficients for weekend and weekday pattern choice
//    private static final double[] allTheCoefficientsForPatternChoice=null; 

     //the base pattern is homeAllDay
     private static final Pattern homeAllDay=new Pattern("h");


     //constructor takes Pattern argument
     PatternAlternative(Pattern thisPat){

          thisPattern = thisPat;
                    
     }
     
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
     public void print(){
          thisPattern.print();
          System.out.println("Utility = "+utility);
     }

     /** This method calculates the utility of a given pattern for a given person
*/
     public void calcUtility(PatternChoiceParameters params, PersonPatternChoiceAttributes persAttr){

               utility=(
                 params.workWorker                              *  thisPattern.wrkDummy * persAttr.worker
               + params.schoolStudentK12                           *  thisPattern.schDummy * persAttr.studentK12
               + params.schoolStudentPostSec                           *  thisPattern.schDummy * persAttr.studentPostSec
               + params.shopActivityDummyFemale                 *  thisPattern.shpDummy * persAttr.female
               + params.shopActivityDummyUnemployed             *  thisPattern.shpDummy * persAttr.unemployed
               + params.shopActivityDummySize1                  *  thisPattern.shpDummy * persAttr.householdSize1
               + params.shopActivityDummySize2                  *  thisPattern.shpDummy * persAttr.householdSize2
               + params.shopActivityDummySize3p                 *  thisPattern.shpDummy * persAttr.householdSize3Plus
               + params.shopActivitiesSingleParentWithChild0_5  *  thisPattern.shopActivities * persAttr.singleWithChild0_5
               + params.recreateActivityDummyAge0_21            *  thisPattern.recDummy * persAttr.age00To21
               + params.recreateActivityDummyAge21_25           *  thisPattern.recDummy * persAttr.age21To25
               + params.recreateActivityDummyAge25_60           *  thisPattern.recDummy * persAttr.age25To60
               + params.recreateActivityDummyWorker             *  thisPattern.recDummy * persAttr.worker
               + params.recreateActivityDummyUnemployed         *  thisPattern.recDummy * persAttr.unemployed
               + params.numberToursAge0_5                       *  (thisPattern.homeActivities-1) * persAttr.age00To05
               + params.numberToursAge5_15                      *  (thisPattern.homeActivities-1) * persAttr.age05To15
               + params.numberToursAge15_25                     *  (thisPattern.homeActivities-1) * persAttr.age15To25
               + params.numberToursAge25_50                     *  (thisPattern.homeActivities-1) * persAttr.age25To50
               + params.numberToursAge50_70                     * (thisPattern.homeActivities-1) * persAttr.age50To70
               + params.numberToursAge70_80                     * (thisPattern.homeActivities-1) * persAttr.age70To80
               + params.numberToursAge80p                       * (thisPattern.homeActivities-1) * persAttr.age80Plus
               + params.workStopsChildrenAge0_15                * thisPattern.workTourIStops * persAttr.age00To15
               + params.workStopsIncome0_15k                    * thisPattern.workTourIStops * persAttr.householdIncome00To15k
               + params.workStopsIncome50kp                     * thisPattern.workTourIStops * persAttr.householdIncome50kPlus
               + params.workStopsAutos0                         * thisPattern.workTourIStops * persAttr.autos0
               + params.workStops_by_NWKTours                   * thisPattern.workTourIStops * ((thisPattern.homeActivities-1)-thisPattern.workActivities)
               + params.workTourNotFirst                        * thisPattern.workTourNotFirst
               + params.schoolToursNotFirst                     * thisPattern.schoolTourNotFirst
               + params.workStops                               * thisPattern.stopsOnWorkTours
               + params.schoolStops                             * thisPattern.stopsOnSchoolTours
               + params.shopStops                               * thisPattern.stopsOnShopTours
               + params.nonWorkStopsChildrenAge0_15             * thisPattern.nonWorkTourIStops * persAttr.childlt15
               + params.nonWorkStopsIncome50kp                  * thisPattern.nonWorkTourIStops * persAttr.householdIncome50kPlus 
               + params.nonWorkStopsAutos0                      * thisPattern.nonWorkTourIStops * persAttr.autos0                
               + params.shopActivities                          * thisPattern.shopActivities                                           
               + params.recreateActivities                      * thisPattern.recreateActivities                                       
               + params.otherActivities                         * thisPattern.otherActivities                                          
               + params.workBasedWorker                         * thisPattern.nWorkBasedTours * persAttr.worker
//               + params.nShopTours                               * thisPattern.nShopTours
//               + params.nOtherTours                              * thisPattern.nOtherTours
               + params.nWorkBasedTours                          * thisPattern.nWorkBasedTours
               + params.shopToursByShopDCLogsum                 * thisPattern.nShopTours * persAttr.shopDCLogsum
               + params.otherToursByOtherDCLogsum               * thisPattern.nOtherTours * persAttr.otherDCLogsum
               + params.workBasedToursByWorkBasedDCLogsum       * thisPattern.nWorkBasedTours * persAttr.workBasedDCLogsum
               + params.homeBasedToursGT2ByWorkBasedTours       * thisPattern.numberOfToursGT2*thisPattern.nWorkBasedTours
               + params.shopToursAutos0                         * thisPattern.nShopTours * persAttr.autos0
               + params.otherToursAutos0                        * thisPattern.nOtherTours * persAttr.autos0
               + params.workBasedToursAutos0                    * thisPattern.nWorkBasedTours * persAttr.autos0
               + params.numberToursIfChildren0_5                * (thisPattern.homeActivities-1) * persAttr.child00To05
               + params.numberToursIfChildren5_15               * (thisPattern.homeActivities-1) * persAttr.child05To10
               + params.numberToursIfChildren5_15               * (thisPattern.homeActivities-1) * persAttr.child10To15
               + params.numberToursIncome0_15k                  * (thisPattern.homeActivities-1) * persAttr.householdIncome00To15k
               + params.numberToursIncome15_25k                 * (thisPattern.homeActivities-1) * persAttr.householdIncome15To25k
               + params.numberToursIncome25_55k                 * (thisPattern.homeActivities-1) * persAttr.householdIncome25To55k
               + params.numberToursIncome55kp                   * (thisPattern.homeActivities-1) * persAttr.householdIncome50kPlus
               + params.numberToursAutos0                       * (thisPattern.homeActivities-1) * persAttr.autos0
               + params.numberToursAutosLtAdults                * (thisPattern.homeActivities-1) * persAttr.autosLessThanAdults
               + params.otherActivityDummy                      * thisPattern.othDummy
               + params.workBasedDummy                          * thisPattern.wkbDummy  
               + params.recreateDummyAge5_10                    * thisPattern.recDummy * persAttr.age05To10
               + params.shopActivitiesIncome0_15k               * thisPattern.shopActivities * persAttr.householdIncome00To15k
               + params.shopActivitiesIncome15_25k              * thisPattern.shopActivities * persAttr.householdIncome15To25k
               + params.shopActivitiesIncome25_55k              * thisPattern.shopActivities * persAttr.householdIncome25To55k
               + params.shopActivitiesIncome55kp                * thisPattern.shopActivities * persAttr.householdIncome50kPlus
               + params.recreateActivitiesAge10_20              * thisPattern.recreateActivities * persAttr.age10To20
               + params.recreateActivitiesAge20_30              * thisPattern.recreateActivities * persAttr.age20To30
               + params.recreateActivitiesAge30_50              * thisPattern.recreateActivities * persAttr.age30To50
               + params.recreateStops                           * thisPattern.stopsOnRecreateTours
               + params.otherStops                              * thisPattern.stopsOnOtherTours
               + params.workDummyIndustryRetail                 * thisPattern.wrkDummy * persAttr.industryEqualsRetail          
               + params.workDummyIndustryPersonalServices         * thisPattern.wrkDummy * persAttr.industryEqualsPersonalServices

          );
          
          if(thisPattern.homeActivities==1)
               utility=0;
               
          //work tours not available to non-workers
          if(persAttr.worker==0)
               if(thisPattern.wrkDummy==1)
                    isAvailable=false;
                    
          //school tours not available to non-students
          if(persAttr.studentK12==0 && persAttr.studentPostSec==0)
               if(thisPattern.schDummy==1)
                    isAvailable=false;
                    
          if(persAttr.worksTwoJobs==0)
               if(thisPattern.workActivities>1)
                    isAvailable=false;
          
          hasUtility=true;
     
     }
     public double getUtility(){
          if(!hasUtility){
               System.out.println("Error: Utility not calculated for "+thisPattern.dayPattern+"\n");
               System.exit(1);
          };
          return utility;
     };
     
     public void printDebug(PatternChoiceParameters params, PersonPatternChoiceAttributes persAttr){
                                                                                                                            
          System.out.println("params.workWorker  * thisPattern.wrkDummy * persAttr.worker                                                                       "+(params.workWorker                              *  thisPattern.wrkDummy * persAttr.worker                                                       ));         
          System.out.println("params.schoolStudentK12  * schDummy * persAttr.studentK12                                                                   "+(params.schoolStudentK12                           *  thisPattern.schDummy * persAttr.studentK12                                                      ));
          System.out.println("params.schoolStudentPostSec  * schDummy * persAttr.studentPostSec                                                                   "+(params.schoolStudentPostSec                           * thisPattern.schDummy * persAttr.studentPostSec                                                      ));
          System.out.println("params.shopActivityDummyFemale  * thisPattern.shpDummy * persAttr.female                                                          "+(params.shopActivityDummyFemale                 *  thisPattern.shpDummy * persAttr.female                                                       ));        
          System.out.println("params.shopActivityDummyUnemployed * thisPattern.shpDummy * persAttr.unemployed                                                   "+(params.shopActivityDummyUnemployed             *  thisPattern.shpDummy * persAttr.unemployed                                                   ));        
          System.out.println("params.shopActivityDummySize1  * thisPattern.shpDummy * persAttr.householdSize1                                                   "+(params.shopActivityDummySize1                  *  thisPattern.shpDummy * persAttr.householdSize1                                               ));        
          System.out.println("params.shopActivityDummySize2  * thisPattern.shpDummy * persAttr.householdSize2                                                   "+(params.shopActivityDummySize2                  *  thisPattern.shpDummy * persAttr.householdSize2                                               ));        
          System.out.println("params.shopActivityDummySize3p  * thisPattern.shpDummy * persAttr.householdSize3Plus                                              "+(params.shopActivityDummySize3p                 *  thisPattern.shpDummy * persAttr.householdSize3Plus                                           ));        
          System.out.println("params.shopActivitiesSingleParentWithChild0_5 * thisPattern.shopActivities * persAttr.singleWithChild0_5                          "+(params.shopActivitiesSingleParentWithChild0_5  *  thisPattern.shopActivities * persAttr.singleWithChild0_5                                     ));        
          System.out.println("params.recreateActivityDummyAge0_21 * thisPattern.recDummy * persAttr.age00To21                                                   "+(params.recreateActivityDummyAge0_21            *  thisPattern.recDummy * persAttr.age00To21                                                    ));        
          System.out.println("params.recreateActivityDummyAge21_25 * thisPattern.recDummy * persAttr.age21To25                                                  "+(params.recreateActivityDummyAge21_25           *  thisPattern.recDummy * persAttr.age21To25                                                    ));        
          System.out.println("params.recreateActivityDummyAge25_60 * thisPattern.recDummy * persAttr.age25To60                                                  "+(params.recreateActivityDummyAge25_60           *  thisPattern.recDummy * persAttr.age25To60                                                    ));        
          System.out.println("params.recreateActivityDummyWorker * thisPattern.recDummy * persAttr.worker                                                       "+(params.recreateActivityDummyWorker             *  thisPattern.recDummy * persAttr.worker                                                       ));        
          System.out.println("params.recreateActivityDummyUnemployed * thisPattern.recDummy * persAttr.unemployed                                               "+(params.recreateActivityDummyUnemployed         *  thisPattern.recDummy * persAttr.unemployed                                                   ));        
          System.out.println("params.numberToursAge0_5  * (thisPattern.homeActivities-1) * persAttr.age00To05                                                   "+(params.numberToursAge0_5                       *  (thisPattern.homeActivities-1) * persAttr.age00To05                                          ));        
          System.out.println("params.numberToursAge5_15  * (thisPattern.homeActivities-1) * persAttr.age05To15                                                  "+(params.numberToursAge5_15                      *  (thisPattern.homeActivities-1) * persAttr.age05To15                                          ));        
          System.out.println("params.numberToursAge15_25  * (thisPattern.homeActivities-1) * persAttr.age15To25                                                 "+(params.numberToursAge15_25                     *  (thisPattern.homeActivities-1) * persAttr.age15To25                                          ));        
          System.out.println("params.numberToursAge25_50  * (thisPattern.homeActivities-1) * persAttr.age25To50                                                 "+(params.numberToursAge25_50                     *  (thisPattern.homeActivities-1) * persAttr.age25To50                                          ));        
          System.out.println("params.numberToursAge50_70  * (thisPattern.homeActivities-1) * persAttr.age50To70                                                 "+(params.numberToursAge50_70                     * (thisPattern.homeActivities-1) * persAttr.age50To70                                           ));        
          System.out.println("params.numberToursAge70_80  * (thisPattern.homeActivities-1) * persAttr.age70To80                                                 "+(params.numberToursAge70_80                     * (thisPattern.homeActivities-1) * persAttr.age70To80                                           ));        
          System.out.println("params.numberToursAge80p  * (thisPattern.homeActivities-1) * persAttr.age80Plus                                                   "+(params.numberToursAge80p                       * (thisPattern.homeActivities-1) * persAttr.age80Plus                                           ));        
          System.out.println("params.workStopsChildrenAge0_15 * thisPattern.workTourIStops * persAttr.age00To15                                                 "+(params.workStopsChildrenAge0_15                * thisPattern.workTourIStops * persAttr.age00To15                                               ));        
          System.out.println("params.workStopsIncome0_15k  * thisPattern.workTourIStops * persAttr.householdIncome00To15k                                       "+(params.workStopsIncome0_15k                    * thisPattern.workTourIStops * persAttr.householdIncome00To15k                                  ));        
          System.out.println("params.workStopsIncome50kp  * thisPattern.workTourIStops * persAttr.householdIncome50kPlus                                        "+(params.workStopsIncome50kp                     * thisPattern.workTourIStops * persAttr.householdIncome50kPlus                                  ));        
          System.out.println("params.workStopsAutos0  * thisPattern.workTourIStops * persAttr.autos0                                                            "+(params.workStopsAutos0                         * thisPattern.workTourIStops * persAttr.autos0                                                  ));        
          System.out.println("params.workStops_by_NWKTours  * thisPattern.workTourIStops * ((thisPattern.homeActivities-1)-thisPattern.workActivities)          "+(params.workStops_by_NWKTours                   * thisPattern.workTourIStops * ((thisPattern.homeActivities-1)-thisPattern.workActivities)      ));        
          System.out.println("params.workTourNotFirst  * thisPattern.workTourNotFirst                                                                           "+(params.workTourNotFirst                        * thisPattern.workTourNotFirst                                                                  ));        
          System.out.println("params.schoolToursNotFirst  * thisPattern.schoolTourNotFirst                                                                      "+(params.schoolToursNotFirst                     * thisPattern.schoolTourNotFirst                                                                ));        
          System.out.println("params.workStops  * thisPattern.stopsOnWorkTours                                                                                  "+(params.workStops                               * thisPattern.stopsOnWorkTours                                                                  ));        
          System.out.println("params.schoolStops  * thisPattern.stopsOnSchoolTours                                                                              "+(params.schoolStops                             * thisPattern.stopsOnSchoolTours                                                                ));        
          System.out.println("params.shopStops  * thisPattern.stopsOnShopTours                                                                                  "+(params.shopStops                               * thisPattern.stopsOnShopTours                                                                  ));        
          System.out.println("params.nonWorkStopsChildrenAge0_15 * thisPattern.nonWorkTourIStops * persAttr.childlt15                                           "+(params.nonWorkStopsChildrenAge0_15             * thisPattern.nonWorkTourIStops * persAttr.childlt15                                            ));        
          System.out.println("params.nonWorkStopsIncome50kp  * thisPattern.nonWorkTourIStops * persAttr.householdIncome50kPlus                                  "+(params.nonWorkStopsIncome50kp                  * thisPattern.nonWorkTourIStops * persAttr.householdIncome50kPlus                               ));        
          System.out.println("params.nonWorkStopsAutos0  * thisPattern.nonWorkTourIStops * persAttr.autos0                                                      "+(params.nonWorkStopsAutos0                      * thisPattern.nonWorkTourIStops * persAttr.autos0                                               ));        
          System.out.println("params.shopActivities  * thisPattern.shopActivities                                                                               "+(params.shopActivities                          * thisPattern.shopActivities                                                                    ));        
          System.out.println("params.recreateActivities  * thisPattern.recreateActivities                                                                       "+(params.recreateActivities                      * thisPattern.recreateActivities                                                                ));        
          System.out.println("params.otherActivities  * thisPattern.otherActivities                                                                             "+(params.otherActivities                         * thisPattern.otherActivities                                                                   ));        
          System.out.println("params.workBasedWorker  * thisPattern.nWorkBasedTours * persAttr.worker                                                            "+(params.workBasedWorker                         * thisPattern.nWorkBasedTours * persAttr.worker                                                  ));
//          System.out.println("params.nShopTours  * thisPattern.nShopTours                                                                                         "+(params.nShopTours                               * thisPattern.nShopTours                                                                         ));
//          System.out.println("params.nOtherTours  * thisPattern.nOtherTours                                                                                       "+(params.nOtherTours                              * thisPattern.nOtherTours                                                                        ));
          System.out.println("params.nWorkBasedTours  * thisPattern.nWorkBasedTours                                                                               "+(params.nWorkBasedTours                          * thisPattern.nWorkBasedTours                                                                    ));
          System.out.println("params.shopToursByShopDCLogsum  * thisPattern.nShopTours * persAttr.shopDCLogsum                                                   "+(params.shopToursByShopDCLogsum                 * thisPattern.nShopTours * persAttr.shopDCLogsum                                                 ));
          System.out.println("params.otherToursByOtherDCLogsum * thisPattern.nOtherTours * persAttr.otherDCLogsum                                                "+(params.otherToursByOtherDCLogsum               * thisPattern.nOtherTours * persAttr.otherDCLogsum                                               ));
          System.out.println("params.workBasedToursByWorkBasedDCLogsum * thisPattern.nWorkBasedTours * persAttr.workBasedDCLogsum                                "+(params.workBasedToursByWorkBasedDCLogsum       * thisPattern.nWorkBasedTours * persAttr.workBasedDCLogsum                                       ));
          System.out.println("params.homeBasedToursGT2ByWorkBasedTours * thisPattern.numberOfToursGT2*thisPattern.nWorkBasedTours                                "+(params.homeBasedToursGT2ByWorkBasedTours       * thisPattern.numberOfToursGT2*thisPattern.nWorkBasedTours                                       ));
          System.out.println("params.shopToursAutos0  * thisPattern.nShopTours * persAttr.autos0                                                                 "+(params.shopToursAutos0                         * thisPattern.nShopTours * persAttr.autos0                                                       ));
          System.out.println("params.otherToursAutos0  * thisPattern.nOtherTours * persAttr.autos0                                                               "+(params.otherToursAutos0                        * thisPattern.nOtherTours * persAttr.autos0                                                      ));
          System.out.println("params.workBasedToursAutos0  * thisPattern.nWorkBasedTours * persAttr.autos0                                                       "+(params.workBasedToursAutos0                    * thisPattern.nWorkBasedTours * persAttr.autos0                                                  ));
     }




}

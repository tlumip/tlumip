package com.pb.despair.pt;


/** 
 * A class that contains Duration Model Parameters
 * 
 * @author Christi Willison
 * @version 1.0 1/21/05
 * 
 */
public class DurationModelParameters {

    public String purpose;                                      /* firstHome, intermedHome, work, school
                                                                   shop, recreate, other */
    public float IStopsInPatternEquals1,                        /* pattern has 1 intermediate stop total */
                 IStopsInPatternEquals2,                        /* pattern has 2 intermediate stops total */
                 IStopsInPatternEquals2Plus,                    /* pattern has 2+ intermediate stops total */
                 IStopsInPatternEquals3,                        /* pattern has 3 intermediate stops total */
                 IStopsInPatternEquals3Plus,                    /* pattern has 3+ intermediate stops total */
                 IStopsInPatternEquals4Plus,                    /* pattern has 4+ intermediate stops total */
                 IStopsOnTourEquals1,                           /* tour has 1 intermediate stop total */
                 IStopsOnTourEquals2,                           /* tour has 2 intermediate stops total */
                 toursEquals2,                                  /* pattern has 2 tours total */
                 toursEquals3,                                  /* pattern has 3 tours total */
                 toursEquals3Plus,                              /* pattern has 3+ tours total */
                 toursEquals4,                                  /* pattern has 4 tours total */
                 toursEquals5Plus,                              /* pattern has 5+ tours total */
                 singleAdultWithOnePlusChildren,                /* person is single adult with 1+ children */
                 householdSize3Plus,                            /* person lives in hh of size 3+ */
                 female,                                        /* person is female */
                 income60Plus,                                  /* person has income > 60,000 */
                 worksTwoJobs,                                  /* person works 2 jobs */
                 age19to21,                                     /* person is age 19-21 */
                 age25Plus,                                     /* person is age 25 or older */
                 autos0,                                        /* person has no car */
                 industryEqualsRetail,                          /* person works in retail */
                 industryEqualsPersonServices,                  /* person works in a service industry */
                 schDummy,                                      /* school dummy variable */
                 wkDummy,                                       /* work dummy variable */
                 amActivityStartTime,                           /* activity starts in the morning */
                 mdActivityStartTime,                           /* activity starts in mid-day */
                 pmActivityStartTime,                           /* activity starts in the afternoon */
                 evActivityStartTime,                           /* activity starts in the evening */
                 isWeekend,                                     /* weekend pattern */
                 activityIsPrimaryDestination,                  /* activity is primary destination */
                 activityIsIntermediateStopOnShopTour,          /* activity is intermediate stop on shop tour */
                 activityIsIntermediateStopOnWorkTour,          /* activity is intermediate stop on work tour */
                 activityIsIntermediateStopOnSchoolTour,        /* activity is intermediate stop on school tour */
                 shopOnlyInPattern,                             /* pattern involves only shopping */
                 recreateOnlyInPattern,                         /* pattern involves only recreation */
                 otherOnlyInPattern,                            /* pattern involves only other */
                 tour1IsWork,                                   /* first tour in pattern is a work tour */
                 tour1IsSchool,                                 /* first tour in pattern is a school tour */
                 constant,
                 shape;




     
    public DurationModelParameters(){};   /* pattern will be initialized to null
                                             and all float variables will be
                                             initialized to 0 on construction. */

    public void print(){

        System.out.print(
              "\n"+ purpose
             +"\n"+ IStopsInPatternEquals1
             +"\n"+ IStopsInPatternEquals2
             +"\n"+ IStopsInPatternEquals2Plus
             +"\n"+ IStopsInPatternEquals3
             +"\n"+ IStopsInPatternEquals3Plus
             +"\n"+ IStopsInPatternEquals4Plus
             +"\n"+ IStopsOnTourEquals1
             +"\n"+ IStopsOnTourEquals2
             +"\n"+ toursEquals2
             +"\n"+ toursEquals3
             +"\n"+ toursEquals3Plus
             +"\n"+ toursEquals4
             +"\n"+ toursEquals5Plus
             +"\n"+ singleAdultWithOnePlusChildren
             +"\n"+ householdSize3Plus
             +"\n"+ female
             +"\n"+ income60Plus
             +"\n"+ worksTwoJobs
             +"\n"+ age19to21
             +"\n"+ age25Plus
             +"\n"+ autos0
             +"\n"+ industryEqualsRetail
             +"\n"+ industryEqualsPersonServices
             +"\n"+ schDummy
             +"\n"+ wkDummy
             +"\n"+ amActivityStartTime
             +"\n"+ mdActivityStartTime
             +"\n"+ pmActivityStartTime
             +"\n"+ evActivityStartTime
             +"\n"+ isWeekend
             +"\n"+ activityIsPrimaryDestination
             +"\n"+ activityIsIntermediateStopOnShopTour
             +"\n"+ activityIsIntermediateStopOnWorkTour
             +"\n"+ activityIsIntermediateStopOnSchoolTour
             +"\n"+ shopOnlyInPattern
             +"\n"+ recreateOnlyInPattern
             +"\n"+ otherOnlyInPattern
             +"\n"+ tour1IsWork
             +"\n"+ tour1IsSchool
             +"\n"+ constant
             +"\n"+ shape

        );
    }

}




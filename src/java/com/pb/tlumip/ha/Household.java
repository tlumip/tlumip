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
package com.pb.tlumip.ha;

import com.pb.models.pecas.AbstractZone;
import com.pb.models.pecas.Alternative;
import com.pb.models.pecas.ChoiceModelOverflowException;
import com.pb.models.pecas.DevelopmentTypeInterface;
import com.pb.models.pecas.DisaggregateActivity;
import com.pb.models.pecas.EconomicUnit;
import com.pb.models.pecas.FixedUtilityAlternative;
import com.pb.models.pecas.LogitModel;
import com.pb.models.pecas.NoAlternativeAvailable;
import com.pb.models.pecas.PeakAutoSkims;
import com.pb.tlumip.ld.DevelopmentType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/* import com.pb.tlumip.model.TravelPreferencesInterface;
import com.pb.tlumip.ld.DevelopmentType;
import com.pb.tlumip.ts.SummaryOfTravelConditions;
import com.pb.tlumip.model.*;
import com.pb.tlumip.pi.TAZ;
import com.pb.tlumip.model.AbstractTAZ; */

/**
 * This class is a subclass of Economic Unit, which means it makes decisions as a unit and has a number
 * of regular locations that it locates in.
 * @see com.pb.models.pecas.EconomicUnit
 * @author John Abraham
 */
public class Household extends EconomicUnit implements Cloneable {

    // TODO: compositionChanged	shouldn't be static
	static boolean compositionChanged; // shouldn't be static!!  

    public static final int[] countWorkersAgeBands = {};
    public int calcNonWorkLogsumSegment(){
          
          int segment=0;
          //hh income - 0-15,15-30,30+
          boolean inclow=false;
          boolean incmed=false;
          boolean inchi=false;
          int workers =0;
          for (int pc=0;pc<myPeople.size();pc++) {
              Person p = (Person)myPeople.get(pc);
              if (p.employStatus >0) workers++;
          }
          if(income<15000)
               inclow=true;
          else if(income>=15000 && income<30000)
               incmed=true;
          else 
               inchi=true;
          
          if(inclow){
               if(autos==0) segment=0;
               else if(autos<workers) segment=1;
               else segment=2;
          }
          if(incmed){
               if(autos==0) segment=3;
               else if(autos<workers) segment=4;
               else segment=5;
          }
          if(inchi){
               if(autos==0) segment=6;
               else if(autos<workers) segment=7;
               else segment=8;
          }
          
    
          return segment;
     }
     

    // Attributes

    /** the income of the household in constant dollars*/
    private float income = 0;
    /** the floorspace occupied by the household*/
    private float floorspace[] = new float[2];
    /** the unique identifier for the household. */
    public final int id;
    static private int maxId=0;
    //static final double birthRate = (float)0.05;
    static final double moveOrStayOrLeaveDispersionParameter = 1.0;
    static final double dwellingPriceCoefficient = -0.01632;
    static final double dwellingChoiceDispersionParameter = 1.0;
    static final double dwellingSizeParameter = 0.4799;
    private static final float secondaryLocationMovingRate = (float)0.0;
    private static final double movingOutsideTheRegionUtility = -0.0;
    private static final double movingConstant = -26;
    static AllHouseholds allHouseholds = null;
    /** number of autos owned by the household */
    private short autos;
    /** the last time the household moved */
    private short yearsSinceMove = 0;
    

    public double getMovingOutsideTheRegionProbability() {
        final HouseholdLocationLogit tazLocations = allHouseholds.tazLocations;
        double chanceOfLeavingOrStaying[] = null;
        synchronized(tazLocations) {
            tazLocations.setHousehold(this);
            tazLocations.setDispersionParameter(getHomeLocationDispersionParameter());
            LogitModel leaveOrStay = new LogitModel();
            Alternative stayAlternative = new Alternative() {
                public double getUtility(double higherLevelDispersionParameter) {
                    double utility=0;
                    try {
                        utility = tazLocations.getUtility(1);
                    } catch (ChoiceModelOverflowException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // adjust for age of hhmembers and time since last move
                    return utility + movingConstant;
                }
            };
            leaveOrStay.addAlternative(stayAlternative);
            Alternative leave = new FixedUtilityAlternative(movingOutsideTheRegionUtility);
            leaveOrStay.addAlternative(leave);
            leaveOrStay.setDispersionParameter(moveOrStayOrLeaveDispersionParameter);
            try {
                chanceOfLeavingOrStaying = leaveOrStay.getChoiceProbabilities();
            } catch (ChoiceModelOverflowException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return chanceOfLeavingOrStaying[1];
    }

    public static void setAllHouseholds(AllHouseholds ahh) {
        if (allHouseholds == null || allHouseholds == ahh) {
            allHouseholds = ahh;
        } else
            throw new Error("We have more than one AllHouseholds objects");
    }

    /** place to keep the ZUtilities. There is a buying ZUtility for each household member, commodity and zone */
    //private CommodityZUtilityCache[][][] buyingZUtilities = null;

    /** place to keep the ZUtilities. There is a selling ZUtility for each household member, commodity and zone */
    //private CommodityZUtilityCache[][][] sellingZUtilities = null;
    // Associations

    /**
     * A household is really just a group of people who are (perhaps temporarily) living together and working as a unit.
     * This is a Vector for now, but for performance reasons we might need to change it to be an int[] of indexes into a big
     * array of numbers that describe all the people.
     * associates <{com.pb.tlumip.ha.Person}>
     * supplierCardinality 1..*
     */
    private ArrayList myPeople;

    /**
     * A household has some of its own preferences, independent of the preferences of
     * the individual household members and independent of the consumption and production functions.
     */
   // private HouseholdPreferences houseHoldPreferences = new HouseholdPreferences();
    private static final double homeLocationDispersionParameter = 1.0;
    // Operations

    /**
     * This takes the household through its births and deaths Functional Spec D:1.1.1.1, D:1.1.1.2, D:1.1.2, D:1.2(only
     * preferences part) (for new household members.. it might make sense to keep the Preference information from time period
     * to time period for existing household members) This needs to pass any people who leave the household back to the
     * AllHouseholds class, so that the AllHouseholds class can match them with other households or with other individuals to
     * start new households.  This can be done either through a parameter (a Vector of individuals who are leaving) or a
     * function call to the DisaggregateActivity.
     * @param elapsedTime the amount of time that has elapsed
     * @return returns true if the household disappears because of demographic changes (last person moves out or dies)
     */
    protected boolean demographicChanges(double elapsedTime) {
        // message #1.2.1 to existingHHMember:com.pb.tlumip.ha.Person
        // existingHHMember.demographicChanges(float);
        // need this to deal with people who die while we're iterating through
        // the people.
        Person[] tempHolder = new Person[myPeople.size()];
        boolean movingOut[] = new boolean[myPeople.size()];
        tempHolder = (Person[]) myPeople.toArray(tempHolder);
        yearsSinceMove++;
        boolean homeless = (this.getHomeZone()==null);
        for (int p = 0; p < tempHolder.length; p++) {
            
            // first check to make sure they have a job location -- all
            // employed people should have a job location but if we're just
            // starting out from the synthetic population they won't yet.
            if (!homeless && tempHolder[p].employStatus>0 && tempHolder[p].myJobTAZ==0) tempHolder[p].homeAnchoredJobLocationChoice();
            // do demographic changes
            movingOut[p] = tempHolder[p].demographicChanges(elapsedTime);
        }
        for (int p=0; p<tempHolder.length; p++) {
        	if (movingOut[p]) tempHolder[p].removeFromHousehold();
        }
        if (myPeople.size() == 0) {
            // everyone's gone!
            return true;
        }
        
        // do the long distance job search
        for (int pnum=0; pnum<myPeople.size(); pnum++) {
            Person p = (Person) myPeople.get(pnum);
            if (p.employStatus>0 && p.myJobTAZ==0) {
                if (allHouseholds.theRandom.nextDouble()<p.getLongDistanceJobSearchProbability()) {
                    p.nonHomeAnchoredJobLocationChoice();
                    
                    // erase any other jobs so that the home location choice only considers the new one
                    for (int pnum2=0;pnum2<myPeople.size(); pnum2++) {
                        if (pnum2!=pnum) {
                            Person p2 = (Person) myPeople.get(pnum2);
                            p2.myJobTAZ = 0;
                        }
                    }
                    break;
                }
            }
        
        }
        
        secondSetOfDemographicChanges(elapsedTime);
        compositionChanged=true;
        return false;
    }
    
    protected void secondSetOfDemographicChanges(double elapsedTime) {
        // iterate through the household members to see if anyone has a child
        int numNewKids = 0;
        boolean malePresent=false;
        for (int pc=0;pc<myPeople.size();pc++) {
            Person p = (Person)myPeople.get(pc);
            if (p.female == false && p.age >17) {
            	malePresent = true;
            	break;
            }
        }
       	boolean married= false;
       	if (malePresent && allHouseholds.theRandom.nextDouble()<0.5) married = true;     
        for (int pc=0;pc<myPeople.size();pc++) {
            Person p = (Person)myPeople.get(pc);
            numNewKids += p.haveBabiesThisYear(married);
        }
        for (int kidNum = 0; kidNum < numNewKids; kidNum++) {
            Person newKid = new Person();
            newKid.addToHousehold(this);
        }
        for (int pc=0;pc<myPeople.size();pc++) {
            Person p = (Person)myPeople.get(pc);
            p.workStatusTransition();
        }
        sampleIncome();
        compositionChanged=true;
    }
    
    
    
    void countMembersByAgeAndWorkStatus(int[] ageBands, int[][] results) {
        for (int pc=0;pc<myPeople.size();pc++) {
            Person p = (Person)myPeople.get(pc);
            if (ageBands.length==0) {
            	results[0][0]++;
            	if (p.employStatus >0) results[0][1]++;
            }
            else if (p.age < ageBands[0]) {
            	results[0][0]++;
                if (p.employStatus >0) results[0][1]++;
            }
            else if (p.age > ageBands[ageBands.length-1]) {
            	results[ageBands.length][0]++;
            	if (p.employStatus >0) results[ageBands.length][1]++;
            }
            else if (ageBands.length>=2) {
	            for (int i=1; i< ageBands.length; i++) {
    	        	if(p.age >=ageBands[i-1] && p.age <ageBands[i]) {
	            		results[i][0]++;
	            		if (p.employStatus >0) 
	            			results[i][1]++;
    	        	}
	            }
            }
        }
    }


    /** Pick an income based on other attributes */
    void sampleIncome() {
    	income = (float) 5550.42603;
    	boolean under5=false;
    	for (int pnum =0; pnum<myPeople.size(); pnum++) {
    		Person p = (Person) myPeople.get(pnum);
	        if (p.employStatus>0 && p.occupation <=82) income+=25762;
	        if (p.employStatus>0 && p.occupation >82 && p.occupation <=112) income+=24230;
	        if (p.employStatus>0 && p.occupation >112 && p.occupation <=154) income+=19715;
	        if (p.employStatus>0 && p.occupation >154 && p.occupation <=162) income+=21237;
	        if (p.employStatus>0 && p.occupation >162 && p.occupation <=262) income+=18396;
	        if (p.employStatus>0 && p.occupation >262 && p.occupation <=282) income+=10065;
	        if (p.employStatus>0 && p.occupation >282 && p.occupation <=402) income+=18544;
	        if (p.employStatus>0 && p.occupation >402 ) income+=9724;
	        if (p.yearsSchool>12 && p.schoolStatus>=2) income+=4561;
	        if (p.yearsSchool<=12 && p.schoolStatus >=2) income+=1072;
	        if (p.age<5) under5=true;
	        if (p.employStatus==0 && p.schoolStatus <2) {
	        	if (p.age >=65) income+=8119.68;
	        	else income += -1835;
	        }
    	}
    	if (under5==false) income-=937;
    	compositionChanged=true;
    }

    /**
     * This is the function that takes the household through its spatial
     * transitions.  This part does the vacating spatial transitions, which may
     * put the household into the AllHouseholds moving pool.
     */
    protected void decideActionsRegardingLocations(double elapsedTime) {
        // message #1.7.1 to aHousehold:com.pb.tlumip.ha.Household
        this.decideWhetherToMove();
        // message #1.7.2 to aHousehold:com.pb.tlumip.ha.Household
        this.secondaryLocationDecision();
    }

    /** Pick household preferences and preferences for each household member based on attributes */
    /*void sampleInitialCharacteristics() {
        // message #1.2.4.1 to existingHHMember:com.pb.tlumip.ha.Person
        // existingHHMember.samplePreferences();
        houseHoldPreferences.sample(this);
        Iterator it = myPeople.listIterator();
        while (it.hasNext()) {
            Person someone = (Person)it.next();
            someone.sampleInitialCharacteristics();
        }
        sampleIncome();
    }*/

    /**
     * this method does not maintain the two-way links between Households
     * and Persons.  Use Person.removeFromHousehold() instead. Person.removeFromHousehold() calls this method.
     */
    void removePerson(Person imOutaHere) {
        myPeople.remove(imOutaHere);
        compositionChanged = true;
    }

    /**
     * This method does not maintain the two-way links between Households
     * and Persons.  Use Person.addToHousehold(Household h) instead. Person.addToHousehold(household h) calls this method.
     */
    void addPerson(Person signMeUp) {
        if (!myPeople.contains(signMeUp)) myPeople.add(signMeUp);
        compositionChanged=true;
    }

    /**  */
    private synchronized void decideWhetherToMove() {
        if (primaryAndSecondaryLocation[0] == null) return; // can't move if we don't live anywhere
        final HouseholdLocationLogit tazLocations = allHouseholds.tazLocations;
        Alternative choice = null;
        Alternative stay = null;
        synchronized(tazLocations) {
            tazLocations.setDispersionParameter(getHomeLocationDispersionParameter());
            tazLocations.setHousehold(this);
            LogitModel moveOrStayOrLeave = new LogitModel();
            Alternative moveAlternative = new Alternative() {
                public double getUtility(double higherLevelDispersionParameter) {
                    double utility=0;
                    try {
                        utility = tazLocations.getUtility(1);
                    } catch (ChoiceModelOverflowException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // adjust for age of hhmembers and time since last move
                    return utility + movingConstant;
                }
            };
            moveOrStayOrLeave.addAlternative(moveAlternative);
            stay = new FixedUtilityAlternative(utilityOfCurrentProperty(getHomeZone() /*, dwellingPriceCoefficient*/));
            moveOrStayOrLeave.addAlternative(stay);
            Alternative leave = new FixedUtilityAlternative(movingOutsideTheRegionUtility);
            moveOrStayOrLeave.addAlternative(leave);
            moveOrStayOrLeave.setDispersionParameter(moveOrStayOrLeaveDispersionParameter);
            try {
                choice = moveOrStayOrLeave.monteCarloChoice();
            } catch (NoAlternativeAvailable m) {
                throw new Error("No move/no-move alternative available -- wtf?");
            } catch (ChoiceModelOverflowException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (choice != stay) {
        	this.getReadyToMoveOut(0); 
        	yearsSinceMove = 0;
	        compositionChanged=true;
        }
    }

    private void secondaryLocationDecision() {
        double uSecondaryLocation = income/10000.0-7;
        boolean leavingSecondaryHome = false;
        boolean obtainingSecondaryHome = false;
        if (primaryAndSecondaryLocation[1] != null) uSecondaryLocation += 1;
        double temp = Math.exp(uSecondaryLocation);
        if (allHouseholds.theRandom.nextDouble()<temp/(1+temp)) {
            // going to have a secondary home next year
            if (primaryAndSecondaryLocation[1]==null) {
                // easy answer -- just get one
                obtainingSecondaryHome = true;
            } else {
                // do we swap our secondary homes?
                if (allHouseholds.theRandom.nextDouble() < 0.10) {
                    obtainingSecondaryHome = true;
                    leavingSecondaryHome = true; 
                } else {
                    // nothing happens
                }
            }
        } else {
            // not going to have a secondary home next year
            if (primaryAndSecondaryLocation[1]==null) {
                // nothing happens
            } else {
                leavingSecondaryHome = true;
            }
        }
        if (leavingSecondaryHome) getReadyToMoveOut(1);
        if (obtainingSecondaryHome) allHouseholds.addToSecondaryLocationMovingPool(this);
        compositionChanged = true;
    }
   

    /** This is the combination of a household and a TAZ zone, for inserting as an alternative into a LogitModel */
    static class HouseholdLocation implements Alternative {
        Household hh;
        AbstractZone location;
        //        double priceCoefficient = 1.0;
        double constantUtility = 0.0;

        public void setHousehold(Household h) {
            this.hh = h;
        }

        public Household getHousehold() {
            return hh;
        }

        HouseholdLocation(Household h, AbstractZone t) {
            hh = h;
            location = t;
        }

        HouseholdLocation(Household h, AbstractZone t,
            //              double priceCoefficient,
            double constantUtility) {
                //   this.priceCoefficient = priceCoefficient;
                this.constantUtility = constantUtility;
        }

        public double getUtility(double higherLevelDispersionParameter) {
            double bob;
            bob = hh.utilityOfAvailableVacantProperties(location, higherLevelDispersionParameter);
            if (Double.isNaN(bob)) {
                System.out.println("hmm, household location utility is Nan");
            }
            return bob + constantUtility;
        }

        AbstractZone getTAZ() { return location; }
    };


    private void takeANewSecondaryResidence() throws AbstractZone.CantFindRoomException {
        VacationLocationLogit vll = allHouseholds.vacationLocations;
        vll.h = this;
        AbstractZone ourNewVacationCommunity = null;
        try {
            VacationLocationLogit.VacationLocation vl = (VacationLocationLogit.VacationLocation) vll.monteCarloChoice();
            ourNewVacationCommunity = vl.z;
        } catch (NoAlternativeAvailable e) {
            throw new com.pb.models.pecas.AbstractZone.CantFindRoomException("no TAZ's available for " + this);
        } catch (ChoiceModelOverflowException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        findLocationInZone(ourNewVacationCommunity, 1);
        compositionChanged=true;
    }

    /** This needs to figure out the utility of locating in a zone. */
    private synchronized double utilityOfAvailableVacantProperties(AbstractZone t, double higherLevelDispersionParameter) {
        double bob = utilityOfLocationIndependentOfPriceAndAvailability(t);
        synchronized(allHouseholds.spaceChoiceLogit) {
            allHouseholds.spaceChoiceLogit.h = this;
            allHouseholds.spaceChoiceLogit.z = t;
            try {
                bob += allHouseholds.spaceChoiceLogit.getUtility(1);
            } catch (ChoiceModelOverflowException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return bob;
    }
    
    static int[][]ageCount = new int[5][2];

    double utilityOfSpaceAndPrice(AbstractZone t, DevelopmentTypeInterface dt, boolean useSizeTerm, double higherLevelDispersionParameter) {
        AbstractZone.PriceVacancy priceVacancy = t.getPriceVacancySize(dt);
        //int[][] ageCount = new int[5][2];  // use static instead to avoid garbage collection
        double utility = 0;
        synchronized (ageCount) {
	        this.countMembersByAgeAndWorkStatus(ageBandsForLocationUtility,ageCount);
	        
	        if (dt == sfd) {
	        	if (income >30000 && income <=50000) utility +=0.4861;
	        	if (income >50000 && income <=60000) utility +=1.203;
	        	if (income >60000) utility +=1.367;
	        	if (myPeople.size()==2) utility +=0.5211;
	        	if (myPeople.size()>=3) utility +=0.3564;
	        	if (ageCount[1][0]+ageCount[2][0] > 0) utility+=0.1686;
	        	utility += 1.642;
	        }
	        if (dt == rrsfd) {
	        	if (income >30000 && income <=50000) utility +=0.4861;
	        	if (income >50000 && income <=60000) utility +=1.203;
	        	if (income >60000) utility +=1.367;
	        	if (myPeople.size()==2) utility +=0.5211;
	        	if (myPeople.size()>=3) utility +=0.3564;
	        	if (ageCount[1][0]+ageCount[2][0] > 0) utility+=0.1686;
	        	utility += 1.642;
	        }
	        if (dt == at) {
	        	if (myPeople.size()==2) utility += -0.7340;
	        	if (myPeople.size()==3 | myPeople.size()==4) utility += -1.852;
	        	if (myPeople.size() >=5) utility += -2.352;
	        	if (ageCount[0][0]+ageCount[1][0] >0) utility += 1.052;
	        	utility += 1.277;
	        }
	        if (dt == mf) {
	        	if (myPeople.size()==2) utility += -0.7340;
	        	if (myPeople.size()==3 | myPeople.size()==4) utility += -1.852;
	        	if (myPeople.size() >=5) utility += -2.352;
	        	if (ageCount[0][0]+ageCount[1][0] >0) utility += 1.052;
	        	utility += 1.277;
	        }
        }
        utility += dwellingPriceCoefficient * priceVacancy.getPrice();
        if (useSizeTerm) {
	        if (priceVacancy.getVacancy() > 0) {
	            return utility + 1 / higherLevelDispersionParameter *
	                dwellingSizeParameter * Math.log(priceVacancy.getVacancy());
	        } else {
	            return Double.NEGATIVE_INFINITY;
	        }
        } else return utility;
    }

 /*  LogitModel makeSpaceChoiceModel(AbstractZone t) {
        final double dwellingChoiceDispersionParameter = 100;
        AbstractZone.PriceVacancy priceVacancy = null;
        DevelopmentTypeInterface[] typesOfSpace = getAllowedInDevelopmentTypes();
        LogitModel spaceChoiceModel = new LogitModel();
        for (int dt = 0; dt < typesOfSpace.length; dt++) {
            priceVacancy = t.getPriceVacancySize(typesOfSpace[dt]);
            if (priceVacancy.vacancy != 0) {
                double utility = - dwellingPriceCoefficient * priceVacancy.price + 1 / dwellingChoiceDispersionParameter *
                    Math.log(priceVacancy.vacancy);
                if (Double.isNaN(utility)) {
                    //System.out.println("Vacancy alternative in zone is NaN for zone "+t+" Development Type "+typesOfSpace[dt]);
                } else {
	                SpaceTypeAlternative sta = new SpaceTypeAlternative(typesOfSpace[dt],utility);
	                spaceChoiceModel.addAlternative(sta);
                }
            }
        }
        spaceChoiceModel.setDispersionParameter(.01);
        return spaceChoiceModel  ;
   }      */

    private synchronized double utilityOfCurrentProperty(AbstractZone t) {
        double bob = utilityOfLocationIndependentOfPriceAndAvailability(t);
        if (primaryAndSecondaryLocation[0] == null) return bob;
        DevelopmentTypeInterface myHomeType = getHomeGridCell().getCurrentDevelopment();
        return bob+utilityOfSpaceAndPrice(t, myHomeType, false, dwellingChoiceDispersionParameter);
    }
    
    private static AllDestinationChoiceLogsums dcLogsums = null;
    static {
        dcLogsums = new AllDestinationChoiceLogsums();
        dcLogsums.readFromJDataStore();
    }

	static DevelopmentTypeInterface rrmh=null;
	static DevelopmentTypeInterface rrsfd=null;
	static DevelopmentTypeInterface sfd;
	static DevelopmentTypeInterface at;
	static DevelopmentTypeInterface mh;
	static DevelopmentTypeInterface mf;
	static final int[] ageBandsForLocationUtility = {5,10,15,18};
	
	static final double[] locationChoiceIntegerCoefficients = {
		0, // household size
		0, // housingtype
		0, // kids
		0, // kids
		0, // kids
		0, // kids
		0, // adults 
		0, // workers
		0,// female1
		0,0,0,0,  // employStatus schoolStatus yearsSchool myJobTAZ
		0,// female1
		0,0,0,0,  // employStatus schoolStatus yearsSchool myJobTAZ
		0,// female1
		0,0,0,0};  // employStatus schoolStatus yearsSchool myJobTAZ
	static final double[] locationChoiceDoubleCoefficients = {
        0, // income
        0, 0, 0, // age1, age2, age3
        -1, // price
        -.1, -0.1, -0.1, // distance to workplaces
        0.2,0.2,0.2, // destination choice logsums to workplaces
        .1, .1, .1, .1, .1  // other logsums
	};
        

    /**
     * This function uses the ZUtilities calcuated for each commodity and person for a zone
     * and combines them to get the overall household ZUtility for a residence in that zone.
     * The household ZUtility for each commodity is taken as the minimum of the Person's
     * ZUtility for that commodity, reflecting an assumption that the household can dispath
     * any household member to purchase/supply that commodity.
     */
    private double utilityOfLocationIndependentOfPriceAndAvailability(AbstractZone t) {
        // use pt.AllDestinationChoiceLogSums class to get the correct destination choice logsums
        // base on the code in pt.PatternModel constructor
        // copy code from AllDestinationChoiceLogSums.setDCLogSums()
        // where setDCLogSums() calls pt.PTHousehold or pt.PTPerson duplicate code?
        double utility = 0;
        int zoneNumber = t.getZoneUserNumber();
        int workSegment = calcWorkLogsumSegment();
        int nonWorkSegment = calcNonWorkLogsumSegment();
//        int[] ia = new int[23]; // integer attributes
//        double[] da = new double[16]; // double attributes
//        ia[0] = myPeople.size();
        if (rrsfd== null) {
        	rrsfd = DevelopmentType.getAlreadyCreatedDevelopmentType("RRSFD");
        	rrmh = DevelopmentType.getAlreadyCreatedDevelopmentType("RRMH");
        	sfd = DevelopmentType.getAlreadyCreatedDevelopmentType("SFD");
        	at = DevelopmentType.getAlreadyCreatedDevelopmentType("AT");
        	mh = DevelopmentType.getAlreadyCreatedDevelopmentType("MH");
        	mf = DevelopmentType.getAlreadyCreatedDevelopmentType("MF");
        }
//        da[0] = income;
//        int[][] ageCount = {{0,0},{0,0},{0,0},{0,0},{0,0}};
//        this.countMembersByAgeAndWorkStatus(ageBandsForLocationUtility,ageCount);
//        ia[2] = ageCount[0][0];
 //       ia[3] = ageCount[1][0];
 //       ia[4] = ageCount[2][0];
 //       ia[5] = ageCount[3][0];
 //       ia[6] = ageCount[4][0];
  //      ia[7] = /* ageCount[0][1] + ageCount[1][1] + */ ageCount[2][1] + ageCount[3][1]+ ageCount[4][1];
  		int jobs = 0;
        for (int pnum = 0; pnum < myPeople.size() && jobs < 3; pnum++) {
        	Person p = (Person) myPeople.get(pnum);
 //       	if (p.female) ia[8+pnum*5] = 1; else ia[8+pnum*5] = 0;
//        	da[1+pnum] = p.age;
 //       	ia[9+pnum*5] = p.employStatus;
  //      	ia[10+pnum*5] = p.schoolStatus;
  //      	ia[11+pnum*5] = p.yearsSchool;
  //      	ia[12+pnum*5] = p.myJobTAZ;
        	if(p.myJobTAZ!=0) {
        		jobs ++;
//        		ModeChoiceLogsums mcls = Person.getModeChoiceLogsums(workSegment);
    
//        		da[5+pnum] = skims.getDistance(zoneNumber,p.myJobTAZ);
        		// TODO: fix NaN in skims
//		        utility += 0.4491 *  mcls.getLogsum(zoneNumber, p.myJobTAZ);
        	} 
        }
//        AbstractZone.PriceVacancy priceVacancy = t.getPriceVacancySize(sfd);
 //       da[4] = priceVacancy.getPrice();
//        da[11] = dcLogsums.getDCLogsum('c','1',nonWorkSegment,zoneNumber);
//        da[12] = dcLogsums.getDCLogsum('c','3',nonWorkSegment,zoneNumber);
          utility += 1.0* dcLogsums.getDCLogsum('s',' ',nonWorkSegment,zoneNumber);
//        da[14] = dcLogsums.getDCLogsum('r',' ',nonWorkSegment,zoneNumber);
//        da[15] = dcLogsums.getDCLogsum('o',' ',nonWorkSegment,zoneNumber);
 //       double utility = 0;
  //      for (int i=0; i< ia.length; i++) {
  //      	utility += locationChoiceIntegerCoefficients[i]*ia[i];
  //      }
 //       for (int i =0; i< da.length; i++) {
 //       	utility += locationChoiceDoubleCoefficients[i]*da[i];
//    }
        // TODO: remove check for NaN on utility
        if (Double.isNaN(utility)) {
        	System.out.println("location utility is NaN: ");
        }
        return utility;
    }
    
    static final double vacationHomeDistanceCoefficient = -.05;
    static final double vacationHomePriceCoefficient = -1;
    
    public double utilityOfVacationAlternative(AbstractZone t) {
        int zoneNumber = t.getZoneUserNumber();
        if (rrsfd== null) {      	
        	rrmh = DevelopmentType.getAlreadyCreatedDevelopmentType("RRMH");
            rrsfd = DevelopmentType.getAlreadyCreatedDevelopmentType("RRSFD");
            sfd = DevelopmentType.getAlreadyCreatedDevelopmentType("SFD");
            at = DevelopmentType.getAlreadyCreatedDevelopmentType("AT");
            mh = DevelopmentType.getAlreadyCreatedDevelopmentType("MH");
            mf = DevelopmentType.getAlreadyCreatedDevelopmentType("MF");
        }
        AbstractZone homeZone = this.getHomeZone();
        double size = allHouseholds.getVacationHomeSizeTerm(zoneNumber);
        double utility =0;
        if (size <=0) utility = Double.NEGATIVE_INFINITY;
        else {
	        utility = Math.log(size);
	        if (homeZone !=null) {
	            utility += vacationHomeDistanceCoefficient * skims.getDistance(homeZone.getZoneUserNumber(),zoneNumber);
	        }
	        utility += utilityOfSpaceAndPrice(t,sfd,false, dwellingChoiceDispersionParameter);
        }
        if (Double.isNaN(utility)) {
        	System.out.println("NaN vacation home utility for zone "+t+" from home "+homeZone);
        	System.out.println("size "+size+" distance "+skims.getDistance(homeZone.getZoneUserNumber(),zoneNumber));
        	System.out.println("space price utility = "+utilityOfSpaceAndPrice(t,sfd,false, dwellingChoiceDispersionParameter));
        }
        return utility;
    }
    
    static PeakAutoSkims skims = new PeakAutoSkims();

    private void takeANewResidence() throws AbstractZone.CantFindRoomException {
       /* new way  -- avoids creating a new "logit model" object for each household */

        /* but doesn't support multithreading */

        final HouseholdLocationLogit tazLocations = allHouseholds.tazLocations;
        AbstractZone ourNewCommunity = null;
        synchronized(tazLocations) {
            tazLocations.setDispersionParameter(getHomeLocationDispersionParameter());
            tazLocations.setHousehold(this);

            /* old way
        AbstractZone[] zones = TAZ.getAllZones();
        LogitModel tazLocations = new LogitModel();
        for (int z = 0; z < zones.length; z++) {
            HouseholdLocation hl = new HouseholdLocation(this, zones[z]);
            tazLocations.addAlternative(hl);
        }
        tazLocations.setDispersionParameter(getHomeLocationDispersionParameter());
        */

            try {
                ourNewCommunity = ((HouseholdLocation)tazLocations.monteCarloChoice()).getTAZ();
            } catch (NoAlternativeAvailable e) {
                throw new com.pb.models.pecas.AbstractZone.CantFindRoomException("no TAZ's available for " + this);
            } catch (ChoiceModelOverflowException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        findLocationInZone(ourNewCommunity, 0);
        compositionChanged=true;
    }

    /**
     * Check to see if the household is in the moving pool or the secondary residence moving pool and, if so, try to find them
     * new homes and remove them from the pools.
     *
     */
    void findNewLocations() {
        if (allHouseholds.movingPoolContains(this)) {
            try {
                takeANewResidence();
                if (primaryAndSecondaryLocation[0] == null) {
                    throw new Error("Can't find new location --- why wasn't an exception thrown!");
                }
                allHouseholds.removeFromMovingPool(this);
            } catch (com.pb.models.pecas.AbstractZone.CantFindRoomException e) {
                // System.out.println("Can't find home for "+this);
                // oh well, just leave them in the movingPool for next time
            }
            compositionChanged=true;
        }
        if (allHouseholds.secondaryLocationMovingPoolContains(this)) {
            try {
                takeANewSecondaryResidence();
                allHouseholds.removeFromSecondaryLocationMovingPool(this);
                // oops this was (mistakenly?) :
                // allHouseholds.addToSecondaryLocationMovingPool(this);
            } catch (com.pb.models.pecas.AbstractZone.CantFindRoomException e) {
                //    System.out.println("Can't find vacation home for " + this);
                // oh well, just leave them in the secondaryResidenceMovingPool for next time
            }
            compositionChanged=true;
        }
    }

    /**
     * find a grid cell in the zone for a household
     * 
     */
    void findLocationInZone(AbstractZone z, int primaryOrSecondary)
        throws com.pb.models.pecas.AbstractZone.CantFindRoomException
    {
        HouseholdSpaceChoiceLogit scl = allHouseholds.spaceChoiceLogit;
        DevelopmentTypeInterface dt=null;
        synchronized(scl) {
            scl.h = this;
            scl.z = z;
            try {
                dt = scl.monteCarloSpaceChoice();
            } catch (NoAlternativeAvailable e) {
                throw new com.pb.models.pecas.AbstractZone.CantFindRoomException(this +
                    " isn't allowed in any development types in zone " + z);
            } catch (ChoiceModelOverflowException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (primaryAndSecondaryLocation[primaryOrSecondary] != null) throw new Error("Forgot to move out of old location before moving in to new location:"+this);
        primaryAndSecondaryLocation[primaryOrSecondary] = z.assignAGridCell(this, dt);
        floorspace[primaryOrSecondary] = primaryAndSecondaryLocation[primaryOrSecondary].getSize();
		compositionChanged=true;
    }

    void findLocationInZone(AbstractZone z, int primaryOrSecondary, DevelopmentTypeInterface dt)
        throws com.pb.models.pecas.AbstractZone.CantFindRoomException
    {
        if (primaryAndSecondaryLocation[primaryOrSecondary] != null) throw new Error("Forgot to move out of old location before moving in to new location:"+this);
        primaryAndSecondaryLocation[primaryOrSecondary] = z.assignAGridCell(this, dt);
        floorspace[primaryOrSecondary] = primaryAndSecondaryLocation[primaryOrSecondary].getSize();
		compositionChanged=true;
    }


/*    private void buildZUtilities() {
        AbstractZone[] zones = TAZ.getAllZones();
        buyingZUtilities = new CommodityZUtilityCache[myConsumptionFunction.size()][myPeople.size()][zones.length];
        for (int c = 0; c < myConsumptionFunction.size(); c++) {
            Commodity cm = (Commodity) myConsumptionFunction.commodityAt(c);
            for (int p = 0; p < myPeople.size(); p++) {
                buyingZUtilities[c][p] = ((Person)myPeople.elementAt(p)).calcBuyingCommodityZUtilities(cm);
            }
        }
        sellingZUtilities = new CommodityZUtilityCache[myProductionFunction.size()][myPeople.size()][zones.length];
        for (int c = 0; c < myProductionFunction.size(); c++) {
            Commodity cm = (Commodity) myProductionFunction.commodityAt(c);
            for (int p = 0; p < myPeople.size(); p++) {
                sellingZUtilities[c][p] = ((Person)myPeople.elementAt(p)).calcSellingCommodityZUtilities(cm);
            }
        }
    }   */

/*    void forgetZUtilities() {
        //   buyingZUtilities = null;
        //   sellingZUtilities = null;
    } */

    public DisaggregateActivity getMyDisaggregateActivity() { return allHouseholds; };

    static final int[] ageBands = {18};
    public float spaceNeeded(DevelopmentTypeInterface dtype) {
        if (rrsfd== null) {      	
        	rrmh = DevelopmentType.getAlreadyCreatedDevelopmentType("RRMH");
            rrsfd = DevelopmentType.getAlreadyCreatedDevelopmentType("RRSFD");
            sfd = DevelopmentType.getAlreadyCreatedDevelopmentType("SFD");
            at = DevelopmentType.getAlreadyCreatedDevelopmentType("AT");
            mh = DevelopmentType.getAlreadyCreatedDevelopmentType("MH");
            mf = DevelopmentType.getAlreadyCreatedDevelopmentType("MF");
        }
        float numRooms = (float) 4.87261;
        numRooms += income/1000*.01726;
        int[][] counter = {{0,0},{0,0}};
        this.countMembersByAgeAndWorkStatus(ageBands,counter);
        int workers = counter[0][1]+counter[1][1];
        if (workers <=1 ) numRooms += 0.46774 ;
        if (workers ==2 ) numRooms += 0.52855;
        if (workers ==3) numRooms += 0.63528;
        if (counter[0][0]==1) numRooms += -0.03912;
        if (counter[0][0]==2) numRooms += 0.20862;
        if (counter[0][0]==3) numRooms += 0.395760;
        if (counter[0][0]>=4) numRooms += 0.52400;
        if (dtype == mh || dtype == rrmh) numRooms += (3.84994-4.87261);
        if (dtype == mf) numRooms += (2.86790-4.87261);
        if (dtype == at) numRooms += (3.55029-4.87261);
        
        return roomsToSqft(numRooms,dtype);
        
    }

    /**
     * This function returns a vector of the RegularActivities that the EconomicUnit has to do.  These RegularActivities have
     * locations associated with them.  Thus the EconomicUnit generally tends to locate within a reasonable distance of the
     * RegularActivity locations.
     * associates <{com.pb.tlumip.pi.RegularActivity}>
     * @return a Vector of the regular activities
     */
    public Vector getRegularActivities() {
        Vector regact = new Vector();
        Iterator it = myPeople.iterator();
        while (it.hasNext()) {
            Person p = (Person)it.next();
            if (p.myJobTAZ != 0) regact.add(AbstractZone.findZoneByUserNumber(p.myJobTAZ));
            if (p.mySchoolingTAZ != 0) regact.add(AbstractZone.findZoneByUserNumber(p.mySchoolingTAZ));
        }
        return regact;
    }

    public double getHomeLocationDispersionParameter() { return homeLocationDispersionParameter; }

    Household(int newid) {
    	id = newid;
    	if (maxId <=id) maxId=id+1;
        if (allHouseholds == null) throw new Error("Didn't set allHouseholds static variable");
        myPeople = new ArrayList(2);
    }
    
    Household()  {
    	this(maxId);
    }

    /** Test function */

/*    public static void main(String[] args) {
        final int numTests = 100;
        TAZ.createTestTazAndExchange();
        AbstractZone[] zones = TAZ.getAllZones();
        AllHouseholds ahh = AllHouseholds.getAllHouseholds(zones);
        Household.setAllHouseholds(ahh);
        Person.setAllHouseholds(ahh);
        ahh.allowIn(DevelopmentType.getAlreadyCreatedDevelopmentType(0));
        ahh.setRepresentativeConsumptionFunction(new LinearConsumptionFunction(Commodity.createOrRetrieveCommodity(String.valueOf(0)), 10, 0, 0));
        ahh.setRepresentativeProductionFunction(new LinearProductionFunction(Commodity.createOrRetrieveCommodity(String.valueOf(1)), 10, 0, 0));
        TransportKnowledge.createTestMatricesForPAAndHA();
        for (int h = 0; h < numTests; h++) {
            Household testHH = new Household();
            testHH.myConsumptionFunction = new LinearConsumptionFunction(Commodity.createOrRetrieveCommodity(String.valueOf(0)), 10, 0, 0);
            testHH.myProductionFunction = new LinearProductionFunction(Commodity.createOrRetrieveCommodity(String.valueOf(1)), 1, 0, 0);
            Person p = new Person();
            p.age = allHouseholds.theRandom.nextInt(90) + 5;
            p.myTravelPreferences = new com.pb.tlumip.pt.FixedVOTTravelPreferences(10.0);
            p.addToHousehold(testHH);
            p = new Person();
            p.age = allHouseholds.theRandom.nextInt(90) + 5;
            p.myTravelPreferences = new com.pb.tlumip.pt.FixedVOTTravelPreferences(10.0);
            p.addToHousehold(testHH);
            testHH.sampleIncome();
            testHH.samplePreferences();
            ahh.addToMovingPool(testHH);
            if (ahh.theRandom.nextFloat() < 0.25) ahh.addToSecondaryLocationMovingPool(testHH);
            testHH.findNewLocations(); // normally, vacateLocations is called first.  But we need to find them an initial home for startup
            testHH.demographicChanges(1.0);
            testHH.vacateLocations(1.0);
            testHH.findNewLocations();
        }
//        System.out.println("Size of moving pool =" + ahh.movingPool.size());
        System.out.println("Size of new households pool =" + ahh.newHouseholdsPool.size());
//        System.out.println("Size of secondary residence pool =" + ahh.secondaryResidenceMovingPool.size());
        TAZ.PriceVacancy pv = new TAZ.PriceVacancy();
        for (int z = 0; z < zones.length; z++) {
            zones[z].getPriceVacancySize(pv, DevelopmentType.getAlreadyCreatedDevelopmentType(0));
            System.out.println("TAZ " + zones[z] + " price:" + pv.price + " vacancy:" + pv.vacancy);
        }
    } */

    public String toString() {
        StringBuffer gendersAndAges = new StringBuffer();
        Iterator it = myPeople.iterator();
        while (it.hasNext()) {
            Person p = (Person)it.next();
            if (p.female) gendersAndAges.append("F"); else gendersAndAges.append("M");
            gendersAndAges.append(p.age);
        }
        return "Household comp: " + gendersAndAges + " income: " + income;
    };

 /*   public int hashCode() {
        Iterator it = myPeople.iterator();
        int thecode = 0;
        while (it.hasNext()) {
            thecode ^= it.next().hashCode();
        }
        return thecode;
    }; */

/*    public boolean equals(Object o) {
        Household other = (Household)o;
        if (other == null) {
            return false;
        }
        if (myPeople.size() != other.myPeople.size()) {
            return false;
        }
        Iterator it = myPeople.iterator();
        while (it.hasNext()) {
            Person p = (Person)it.next();
            if (!other.myPeople.contains(p)) {
                return false;
            }
        }
        return true;
    } */

    public Household tempCopy() {
        try {
            Household hh = (Household)super.clone();
            hh.myPeople = (ArrayList)myPeople.clone();
            return hh;
        } catch (CloneNotSupportedException e) { throw new Error("tempCopy of Household didn't work"); }
    }

    void dissolveInto(Collection whereThePeopleGo) {
        whereThePeopleGo.addAll(myPeople);
        Person[] p = new Person[myPeople.size()];
        p = (Person[]) myPeople.toArray(p);
        for (int i = 0; i < p.length; i++) {
            p[i].removeFromHousehold();
        }
		compositionChanged=true;
    }

    public float getIncome() { return income; }

    public void setIncome(float income) { 
    	this.income = income; 
		compositionChanged=true;
    }

    public List getPeople() { return Collections.unmodifiableList(myPeople); }

	public int calcWorkLogsumSegment(){

        int size = myPeople.size();
		
		int segment=0;
		//hh income - 0-15,15-30,30+
		boolean inclow=false;
		boolean incmed=false;
		boolean inchi=false;
		if(income<15000)
			inclow=true;
		else if(income>=15000 && income<30000)
			incmed=true;
		else 
			inchi=true;
		
		if(inclow){
			if(autos==0) segment=0;
			else if(autos<size) segment=1;
			else segment=2;
		}
		if(incmed){
			if(autos==0) segment=3;
			else if(autos<size) segment=4;
			else segment=5;
		}
		if(inchi){
			if(autos==0) segment=6;
			else if(autos<size) segment=7;
			else segment=8;
		}
		

		return segment;
	}
    /**
     * @return
     */
    public short getAutos() {
        return autos;
    }

    /**
     * @return
     */
    public short getYearsSinceMove() {
        return yearsSinceMove;
    }

    /**
     * @param s
     */
    public void setAutos(short s) {
        autos = s;
    }

    /**
     * @param s
     */
    public void setYearsSinceMove(short s) {
        yearsSinceMove = s;
    }

    /**
     * 
     */
    public void eraseAllButOneJob() {
        int numWorkplaces = 0;
        for (int pnum=0;pnum<myPeople.size();pnum++) {
            Person p = (Person) myPeople.get(pnum);
            if (p.employStatus>0 && p.myJobTAZ!=0) numWorkplaces++;
        }
        if (numWorkplaces >1) {
            int workPlaceToKeep = allHouseholds.theRandom.nextInt(numWorkplaces);
            for (int pnum=0;pnum<myPeople.size();pnum++) {
                Person p = (Person) myPeople.get(pnum);
                if (p.employStatus>0 && p.myJobTAZ!=0 && pnum !=workPlaceToKeep) p.myJobTAZ=0;
            }
            
        }
    }

    /**
     * Method roomsToSqft.
     * @param numRooms
     * @return float
     */
    public float roomsToSqft(float numRooms, DevelopmentTypeInterface dtype) {
        if (rrsfd== null) {         
            rrmh = DevelopmentType.getAlreadyCreatedDevelopmentType("RRMH");
            rrsfd = DevelopmentType.getAlreadyCreatedDevelopmentType("RRSFD");
            sfd = DevelopmentType.getAlreadyCreatedDevelopmentType("SFD");
            at = DevelopmentType.getAlreadyCreatedDevelopmentType("AT");
            mh = DevelopmentType.getAlreadyCreatedDevelopmentType("MH");
            mf = DevelopmentType.getAlreadyCreatedDevelopmentType("MF");
        }
        if (dtype == sfd || dtype == rrsfd) {
            if (numRooms < 1 ) return 598             *numRooms;
            if (numRooms < 2 ) return 598 +(617 - 598)*(numRooms- 1);
            if (numRooms < 3 ) return 617 +(807 - 617)*(numRooms- 2);
            if (numRooms < 4 ) return 807 +(1152- 807)*(numRooms- 3);
            if (numRooms < 5 ) return 1152+(1464-1152)*(numRooms- 4);
            if (numRooms < 6 ) return 1464+(1723-1464)*(numRooms- 5);
            if (numRooms < 7 ) return 1723+(2025-1723)*(numRooms- 6);
            if (numRooms < 8 ) return 2025+(2371-2025)*(numRooms- 7);
            if (numRooms < 10) return 2371+(2668-2371)*(numRooms- 8)/2;
            return                    2668+(2668-2371)*(numRooms-10)/2;
            // assumes the average house with 9+ rooms has 10 rooms, and that rooms above 8
            // are all the same size
        }
        if (dtype == mf) {
            if (numRooms < 1 ) return 351             *numRooms;
            if (numRooms < 2 ) return 351 +(488 -351 )*(numRooms- 1);
            if (numRooms < 3 ) return 488 +(681 -488 )*(numRooms- 2);
            if (numRooms < 4 ) return 681 +(909 -681 )*(numRooms- 3);
            if (numRooms < 5 ) return 909 +(1078-909 )*(numRooms- 4);
            if (numRooms < 6 ) return 1078+(1300-1078)*(numRooms- 5);
            if (numRooms < 7 ) return 1300+(1717-1300)*(numRooms- 6);
            if (numRooms < 8 ) return 1717+(1255-1717)*(numRooms- 7);
            if (numRooms < 10) return 1255+(1056-1255)*(numRooms- 8)/2;
            return                    1056;
            // assumes over 10 rooms are same size as the average for 9+ rooms.
       }
        if (dtype == at) {               
            if (numRooms < 1 ) return 796                 *numRooms;
            if (numRooms < 2 ) return 796 +(552 -796 )*(numRooms- 1);
            if (numRooms < 3 ) return 552 +(741 -552 )*(numRooms- 2);
            if (numRooms < 4 ) return 741 +(971 -741 )*(numRooms- 3);
            if (numRooms < 5 ) return 971 +(1067-971 )*(numRooms- 4);
            if (numRooms < 6 ) return 1067+(1251-1067)*(numRooms- 5);
            if (numRooms < 7 ) return 1251+(1510-1251)*(numRooms- 6);
            if (numRooms < 8 ) return 1510+(1816-1510)*(numRooms- 7);
            if (numRooms < 10) return 1816+(1995-1816)*(numRooms- 8)/2;
            return                    1995+(1995-1816)*(numRooms-10)/2;
            // assumes the average house with 9+ rooms has 10 rooms, and that rooms above 8
            // are all the same size
        }
        if (dtype == mh || dtype == rrmh) {
            if (numRooms < 1 ) return 350         *numRooms;
            if (numRooms < 2 ) return 350 +(450 -350 )*(numRooms- 1);
            if (numRooms < 3 ) return 450 +(605 -450 )*(numRooms- 2);
            if (numRooms < 4 ) return 605 +(887 -605 )*(numRooms- 3);
            if (numRooms < 5 ) return 887 +(1117-887 )*(numRooms- 4);
            if (numRooms < 6 ) return 1117+(1349-1117)*(numRooms- 5);
            if (numRooms < 7 ) return 1349+(1650-1349)*(numRooms- 6);
            if (numRooms < 8 ) return 1650+(1943-1650)*(numRooms- 7);
            if (numRooms < 10) return 1943+(1798-1943)*(numRooms- 8)/2;
            return                    1798;
            // assumes over 10 rooms are same size as the average for 9+ rooms.
        }
            
        throw new Error("Incorrect development type for household "+dtype.getName());
    }

}

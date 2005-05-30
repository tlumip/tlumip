package com.pb.despair.ha;

import com.pb.despair.model.ModeChoiceLogsums;
import com.pb.despair.model.PersonInterface;
import com.pb.despair.model.SimulationDays;
import com.pb.despair.pt.Pattern;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pecas.AbstractTAZ;
import com.pb.models.pecas.CommodityZUtility;
import com.pb.models.pecas.LogitModel;

import java.util.*;

/**
 * A class that represents a person
 * @author John E. Abraham
 */
public class Person implements PersonInterface {
    
   
    static final double jobChangeProbability=0.2;
    static final double longDistanceJobSearchProbability=0.1;
    
    /*
     * for the Mode/DC Logsums market segment
     *
     */
     public int calcWorkerLogsumSegment(){
          
          int segment=4;
          if(occupation<=262)           // Manager/Professional
               segment=1;
          else if(occupation>=263 && occupation <= 282) // retail
               segment=2;
          else if(occupation>402) // Production/Fabrication
               segment=3;
          
          return segment;
     }
          

    /*
     * for the Mode/DC Logsums market segment
     *
     */
     public int calcStudentLogsumSegment(){
          
          int segment=1;
          
          if(age>18)          //College +
               segment=3;
               
          return segment;
     }
     

    // Attributes
    static private AllHouseholds allHouseholds;
    public static final float resamplePreferencesProbability = (float)0.2;
    static private int maxId;
    /** the unique identifier for the person */
    final int id;
    public Person(int newId) {
        id = newId;
        if (maxId <=id) maxId=id+1;
        // a new life!
        if (allHouseholds.theRandom.nextInt(2) == 1) female = true; else female = false;
    }
    
    public Person() {
        this(maxId);
    }

    /** An attribute that represents the person's age */
    float age = 0;

    /** An attribute that represents the person's gender */
    boolean female = false;

    /** Person's current or most recent occupation in Census Occupation Code*/
    short occupation = 0;

    /** Person's employment status.  2 is fulltime, 1 is parttime, 0 is not working */
    short employStatus = 0;
    // Associations
    
    /** Person's school status.  <2 is not in school, >=2 is in school (Census codes) */
    short schoolStatus = 0;
    /** Number of years of schooling completed */
    short yearsSchool = 0;
    /** Number of children ever born +1 (1 = no children ever, 2 = 1 child ... census codes ) */
    short fertil = 0;

    /** supplierCardinality 0..1  to be used to keep track of school location */
    int mySchoolingTAZ;

    /**
     * associates <{com.pb.despair.ha.Job}>
     * supplierCardinality 0..2
     */
    int myJobTAZ;
   // Pattern myActivityPattern = null;
   // TravelUtilityCalculatorInterface myTravelPreferences;
    private Household lnkHousehold;

    public static final double[] occupationPriceCoefficients = {1,1,1,1,1,1,1,1};
    public static final double[] occupationSizeCoefficients = {1,1,1,1,1,1,1,1};
    
    
    
    void nonHomeAnchoredJobLocationChoice() {
        int occupationIndex =7;
        if (occupation <=82) occupationIndex=0;
         if (occupation >82 && occupation <=112) occupationIndex=1;
         if (occupation >112 && occupation <=154) occupationIndex=2;
         if (occupation >154 && occupation <=162) occupationIndex=3;
         if (occupation >162 && occupation <=262) occupationIndex=4;
         if (occupation >262 && occupation <=282) occupationIndex=5;
         if (occupation >282 && occupation <=402) occupationIndex=6;
         if (occupation >402 ) occupationIndex=7;
         double utilities[] = new double[allHouseholds.commodityPrices[occupationIndex].length];
         double sum =0;
         for (int z=0;z<utilities.length;z++){
             if (allHouseholds.commoditySizes[occupationIndex][z]<=0) {
                 utilities[z]=0;
             } else {
                 utilities[z]=allHouseholds.commodityPrices[occupationIndex][z]*occupationPriceCoefficients[occupationIndex]+
                 Math.log(allHouseholds.commoditySizes[occupationIndex][z])*occupationSizeCoefficients[occupationIndex];
                 utilities[z]=Math.exp(utilities[z]);
                 sum += utilities[z];
             }
         }
         if (sum>0) {
             double selector = allHouseholds.theRandom.nextDouble()*sum;
             sum =0;
             int z1;
             for (z1=0;z1<utilities.length;z1++) {
                 sum+= utilities[z1];
                 if (sum>=selector) break;
             }
             myJobTAZ=AbstractTAZ.getZone(z1).getZoneUserNumber();
         }
    }
	static final int maxOccupationCategories = 10;
    
    static double[] nonHomeAnchoredJobCompositeUtilities = new double[maxOccupationCategories];
    static boolean nonHomeAnchoredJobCompositeUtilitiesCalculated = false;

    double nonHomeAnchoredJobCompositeUtility(int occupation) {
    	if (!nonHomeAnchoredJobCompositeUtilitiesCalculated) {
    		for (int occupationIndex = 1; occupationIndex <maxOccupationCategories && occupationIndex < allHouseholds.commodityPrices.length; occupationIndex++ ) { 
		         double utilities[] = new double[allHouseholds.commodityPrices[occupationIndex].length];
		         double sum =0;
		         for (int z=0;z<utilities.length;z++){
		             if (allHouseholds.commoditySizes[occupationIndex][z]<=0) {
		                 utilities[z]=0;
		             } else {
		                 utilities[z]=allHouseholds.commodityPrices[occupationIndex][z]*occupationPriceCoefficients[occupationIndex]+
		                 Math.log(allHouseholds.commoditySizes[occupationIndex][z])*occupationSizeCoefficients[occupationIndex];
		                 utilities[z]=Math.exp(utilities[z]);
		                 sum += utilities[z];
		             }
		         }
		         nonHomeAnchoredJobCompositeUtilities[occupationIndex] = Math.log(sum);
    		}
    		nonHomeAnchoredJobCompositeUtilitiesCalculated = true;
    	}
    	return nonHomeAnchoredJobCompositeUtilities[occupation];
    }


   static final ModeChoiceLogsums allWorkModeChoiceLogsums[] = new ModeChoiceLogsums[100];


    int getOccupationIndex() {
        int occupationIndex =7;
        if (occupation <=82) occupationIndex=0;
         if (occupation >82 && occupation <=112) occupationIndex=1;
         if (occupation >112 && occupation <=154) occupationIndex=2;
         if (occupation >154 && occupation <=162) occupationIndex=3;
         if (occupation >162 && occupation <=262) occupationIndex=4;
         if (occupation >262 && occupation <=282) occupationIndex=5;
         if (occupation >282 && occupation <=402) occupationIndex=6;
         if (occupation >402 ) occupationIndex=7;
         return occupationIndex;
    }
    
    double journeyToWorkUtility(int occupationIndex, int workSegment, int homeZone, AbstractTAZ destinationZone) {
    	 int z = destinationZone.getZoneIndex();
         double totalEmployment = 0;
         ModeChoiceLogsums mcls = getModeChoiceLogsums(workSegment);
         for (int i=0;i<allHouseholds.commoditySizes.length;i++) {totalEmployment+=allHouseholds.commoditySizes[occupationIndex][z];}
         return 0.54*mcls.getLogsum(homeZone, AbstractTAZ.getZone(z).getZoneUserNumber())+
         Math.log(allHouseholds.commoditySizes[occupationIndex][z]) + 
         0.1831* Math.log(totalEmployment - allHouseholds.commoditySizes[occupationIndex][z]);
    }
    
    double journeyToWorkUtility(AbstractTAZ destinationZone) {
    	return journeyToWorkUtility(getOccupationIndex(), calcWorkerLogsumSegment(),lnkHousehold.getHomeZone().getZoneUserNumber(), destinationZone);
    }

    
    static ModeChoiceLogsums getModeChoiceLogsums(int workSegment) {
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
         ModeChoiceLogsums mcls =  allWorkModeChoiceLogsums[workSegment];
         if (mcls == null) {
             mcls = new ModeChoiceLogsums(rb);
             mcls.readLogsums('w',workSegment);
             allWorkModeChoiceLogsums[workSegment]= mcls;
         }
         return mcls;
    }
    	

    void homeAnchoredJobLocationChoice() {
    	
        int occupationIndex =getOccupationIndex();
         double utilities[] = new double[allHouseholds.commodityPrices[occupationIndex].length];
         int workSegment= calcWorkerLogsumSegment();
         ModeChoiceLogsums mcls = getModeChoiceLogsums(workSegment);
         double sum =0;
         int homeZone = lnkHousehold.getHomeZone().getZoneUserNumber();
         for (int z=0;z<utilities.length;z++){
             if (allHouseholds.commoditySizes[occupationIndex][z]<=0) {
                 utilities[z]=0;
             } else {
                 utilities[z] = Math.exp(journeyToWorkUtility(occupationIndex,workSegment,homeZone,AbstractTAZ.getZone(z)));
                 utilities[z]=Math.exp(utilities[z]);
                 sum += utilities[z];
             }
         }
         if (sum>0) {
             double selector = allHouseholds.theRandom.nextDouble()*sum;
             sum =0;
             int z1;
             for (z1=0;z1<utilities.length;z1++) {
                 sum+= utilities[z1];
                 if (sum>=selector) break;
             }
             myJobTAZ=AbstractTAZ.getZone(z1).getZoneUserNumber();
         }
    }

    static Hashtable[] jobUtilities = new Hashtable[8];;
    /**
     * This returns the utility of jobs from the current home location,
     * or from all available home location
     * @param zone zone to calculate job utility from, -1 to get from all model zones
     */
    double homeAnchoredJobLocationChoiceUtility(int zone, int occupationIndex) {
    	// duplicates code in homeAnchoredJobLocationChoice to avoid excessive new()'s for
    	// passing temporary variables from a common helper method.
         if (jobUtilities[occupationIndex] == null) jobUtilities[occupationIndex] = new Hashtable();
         Double utilityDouble = (Double) jobUtilities[occupationIndex].get(new Integer(zone));
         if (utilityDouble !=null) return utilityDouble.doubleValue();
         else {
         	 if (zone == -1) {
         	 	AbstractTAZ[] zones = AbstractTAZ.getAllZones();
         	 	double sum = 0;
         	 	for (int i=0;i<zones.length;i++) {
         	 		sum += Math.exp(homeAnchoredJobLocationChoiceUtility(zones[i].getZoneUserNumber(),occupationIndex));
         	 	}
         	 	utilityDouble = new Double(Math.log(sum));
         	 } else {
	         	 		
		         int length = allHouseholds.commodityPrices[occupationIndex].length;
		         int workSegment= calcWorkerLogsumSegment();
		         ModeChoiceLogsums mcls = getModeChoiceLogsums(workSegment);
		         double sum =0;
		         int homeZone = zone;
		         for (int z=0;z<length;z++){
		             if (allHouseholds.commoditySizes[occupationIndex][z]>0) {
		                 double totalEmployment = 0;
		                 for (int i=0;i<allHouseholds.commoditySizes.length;i++) {totalEmployment+=allHouseholds.commoditySizes[occupationIndex][z];}
		                 double utility=0.54*mcls.getLogsum(homeZone, AbstractTAZ.getZone(z).getZoneUserNumber())+
		                 Math.log(allHouseholds.commoditySizes[occupationIndex][z]) + 
		                 0.1831* Math.log(totalEmployment - allHouseholds.commoditySizes[occupationIndex][z]);
		                 utility=Math.exp(utility);
		                 sum += utility;
		             }
		         }
		         utilityDouble = new Double(Math.log(sum));
         	 }
	         jobUtilities[occupationIndex].put(new Integer(zone),utilityDouble);
	         return utilityDouble.doubleValue();
         }
    }
    
    
    /**
     * This takes the person through their changes; aging, dieing, leaving home. etc.
     * Functional Spec D:1.1.1.1, D:1.1.1.2, D:1.1.2, D:1.2 (for new household members.. it might make sense to keep the
     * Preference information from time period to time period for existing household members)
     * If the person leaves home, then the person needs to go out and find other people to make a new home.  this means that
     * this function needs a method to communicate with the "all households" class.  It could do this by returning a value
     * that indicates it left home, or it could be passed a pointer to the "all households" class and it could send a message
     * to the "allhouseholds", or some other method. This needs to pass any people who leave the household back to the
     * AllHouseholds class, so that the AllHouseholds class can match them with other households or with other individuals to
     * start new households.  This can be done either through a parameter (a Vector of individuals who are leaving) or a
     * function call to the DisaggregateActivity.
     * @param elapsedTime the amount of time that has elapsed
     */
    boolean demographicChanges(double elapsedTime) {
        age += elapsedTime;
        if (schoolStatus >=2) yearsSchool++;
        boolean leavingHome = false;
        if (allHouseholds.theRandom.nextFloat() < this.getChanceOfDyingInPastYear() * elapsedTime) {
            // message #1.2.1.2 to aHousehold:com.pb.despair.ha.Household
            // aHousehold.removePerson(com.pb.despair.ha.Person);
            leavingHome = true;
            age = -1;
        } else {
            if (allHouseholds.theRandom.nextFloat() < this.getLeaveHomeProbability() * elapsedTime) {
                // message #1.2.1.1 to newHouseholdsPool:java.util.Vector
                // newHouseholdsPool.addElement(java.lang.Object);
                allHouseholds.newHouseholdsPool.add(this);
                leavingHome = true;
            }
        }
        if (allHouseholds.theRandom.nextDouble()<getJobChangeProbability()) myJobTAZ=0;
        
//        if (allHouseholds.theRandom.nextFloat() < resamplePreferencesProbability) samplePreferences();
        if (leavingHome) {
        	return true;
        }
        return false;
    }

	static final double keepJobConstant = 6.0;
	static final double[] keepOccupationConstants = {0,0,0,0,0,0,0,0};
	static final double keepJobUtilityParameter = 1.0;
	static final double anchoredConstant = -2.0;
	static final double jobChangeDispersionParameter =1.0;
    public double getJobChangeProbability() {
    	double keepU = keepJobConstant;
    	AbstractTAZ home = lnkHousehold.getHomeZone();
        int occupationIndex =getOccupationIndex();
    	if (home!=null) keepU += keepJobUtilityParameter*homeAnchoredJobLocationChoiceUtility(home.getZoneUserNumber(),occupationIndex);
         keepU += keepOccupationConstants[occupationIndex];
         
         double anchoredU = anchoredConstant + homeAnchoredJobLocationChoiceUtility(-1,occupationIndex);
         
         double nonAnchoredU = nonHomeAnchoredJobCompositeUtility(occupationIndex);
         
         return (Math.exp(jobChangeDispersionParameter* anchoredU) + Math.exp(jobChangeDispersionParameter * nonAnchoredU))/
         (Math.exp(jobChangeDispersionParameter* anchoredU) + Math.exp(jobChangeDispersionParameter * nonAnchoredU)+ Math.exp(jobChangeDispersionParameter * keepU));
    }
    public double getLongDistanceJobSearchProbability() {
    	int occupationIndex = getOccupationIndex();
         double anchoredU = anchoredConstant + homeAnchoredJobLocationChoiceUtility(-1,occupationIndex);
         
         double nonAnchoredU = nonHomeAnchoredJobCompositeUtility(occupationIndex);
         
         return (Math.exp(jobChangeDispersionParameter * nonAnchoredU))/
         (Math.exp(jobChangeDispersionParameter* anchoredU) + Math.exp(jobChangeDispersionParameter * nonAnchoredU));
    }
    
     public void addToHousehold(Household h) {
        lnkHousehold = h;
        h.addPerson(this);
    }

    public void removeFromHousehold() {
        lnkHousehold.removePerson(this);
        lnkHousehold = null;
    }

    void samplePreferences() {
        // to be used to track individual travel preferences -- utility function coefficients
/*        if (age < 5) myTravelPreferences = new com.pb.despair.pt.CantTravelAtAll();
        else
            myTravelPreferences = new TimeAndDistanceTravelUtilityCalculator(allHouseholds.theRandom.nextDouble() *
                20, 0.06); */
    } 

    public float getChanceOfDyingInPastYear() {
        // age is incremented BEFORE this routine is called, so rates
        // are based on the previous year's age.  This is only really
        // necessary to capture infant mortality in the first year of life
        if (female) {
            if (age <= (float) 1) return (float)(661.1 / 100000);
            if (age <= (float) 5) return (float)(31.8 / 100000);
            if (age <= (float) 10) return (float)(16.6 / 100000);
            if (age <= (float) 15) return (float)(18.3 / 100000);
            if (age <= (float) 20) return (float)(43.4 / 100000);
            if (age <= (float) 25) return (float)(49.5 / 100000);
            if (age <= (float) 30) return (float)(58.7 / 100000);
            if (age <= (float) 35) return (float)(80.0 / 100000);
            if (age <= (float) 40) return (float)(115.5 / 100000);
            if (age <= (float) 45) return (float)(168.6 / 100000);
            if (age <= (float) 50) return (float)(251.6 / 100000);
            if (age <= (float) 55) return (float)(394.0 / 100000);
            if (age <= (float) 60) return (float)(637.3 / 100000);
            if (age <= (float) 65) return (float)(1019.9 / 100000);
            if (age <= (float) 70) return (float)(1527.7 / 100000);
            if (age <= (float) 75) return (float)(2422.8 / 100000);
            if (age <= (float) 80) return (float)(3760.6 / 100000);
            if (age <= (float) 85) return (float)(6321.6 / 100000);
            return (float)(14492.3 / 100000);
        } else {
            // male
            if (age <= (float) 1) return (float)(812.8 / 100000);
            if (age <= (float) 5) return (float)(39.7 / 100000);
            if (age <= (float) 10) return (float)(20.2 / 100000);
            if (age <= (float) 15) return (float)(27.9 / 100000);
            if (age <= (float) 20) return (float)(104.5 / 100000);
            if (age <= (float) 25) return (float)(145.3 / 100000);
            if (age <= (float) 30) return (float)(145.3 / 100000);
            if (age <= (float) 35) return (float)(173.6 / 100000);
            if (age <= (float) 40) return (float)(222.2 / 100000);
            if (age <= (float) 45) return (float)(312.0 / 100000);
            if (age <= (float) 50) return (float)(456.8 / 100000);
            if (age <= (float) 55) return (float)(665.6 / 100000);
            if (age <= (float) 60) return (float)(1048.3 / 100000);
            if (age <= (float) 65) return (float)(1679.6 / 100000);
            if (age <= (float) 70) return (float)(2550.6 / 100000);
            if (age <= (float) 75) return (float)(3941.9 / 100000);
            if (age <= (float) 80) return (float)(5824.6 / 100000);
            if (age <= (float) 85) return (float)(9313.5 / 100000);
            return (float)(17461.9 / 100000);
        }
    }

    public double getLeaveHomeProbability() {
        double uLeave = 0;
        if (age <=17) uLeave+=0.490;
        if (age>17 && age <=21) uLeave += 1.261;
        if (age>21 && age <=30) uLeave +=0.147;
        if (age>30 && age <=40) uLeave += -0.106;
        if (age>40 && age <=50) uLeave += -0.707;
        if (age>50 && age <=65) uLeave += -1.547;
        if (age>65) uLeave += -1.547;
        
        int[][] typeCount = {{0,0},{0,0},{0,0}};
        final int[] ageBands = {5,18};
        
        lnkHousehold.countMembersByAgeAndWorkStatus(ageBands,typeCount);
        if (typeCount[0][0]>0) uLeave += -0.277;
        if (typeCount[1][0]>0) uLeave += -0.334;
        if (yearsSchool<12) uLeave += -2.162;
        if (yearsSchool==12) uLeave += -1.295;
        if (yearsSchool>12 && yearsSchool<16) uLeave += -1.700;
        if (yearsSchool==16) uLeave += -1.754;
        
        int hhSize = typeCount[0][0]+typeCount[1][0]+typeCount[2][0];
        if (hhSize == 2) uLeave += -2.825;
        if (hhSize == 3) uLeave += -3.748;
        if (hhSize >= 4) uLeave += -4.464;
        if (this.employStatus>0) uLeave -= 0.770;
        if (this.schoolStatus>=2) uLeave += 0.331;
        double expLeave = Math.exp(uLeave);
        return  expLeave/(1+expLeave); 
    }

 /*   CommodityZUtilityCache[] calcSellingCommodityZUtilities(Commodity cm) {
        if (myTravelPreferences instanceof ReusedTravelUtilityCalculator) {
            return ((ReusedTravelUtilityCalculator)myTravelPreferences).calcBuyingCommodityZUtilities(cm);
        } else {
            AbstractTAZ[] zones = TAZ.getAllZones();
            CommodityZUtilityCache[] czuc = new CommodityZUtilityCache[zones.length];
            for (int z = 0; z < zones.length; z++) {
                TAZ azone = (TAZ)zones[z];
                czuc[z] = new CommodityZUtilityCache(cm.retrieveSellingZUtilityForZone(azone));
                czuc[z].calcZUtilityForPreferences(myTravelPreferences, false);
            }
            return czuc;
        }
    } */

  /*  CommodityZUtilityCache[] calcBuyingCommodityZUtilities(Commodity cm) {
        if (myTravelPreferences instanceof ReusedTravelUtilityCalculator) {
            return ((ReusedTravelUtilityCalculator)myTravelPreferences).calcBuyingCommodityZUtilities(cm);
        } else {
            AbstractTAZ[] zones = TAZ.getAllZones();
            CommodityZUtilityCache[] czuc = new CommodityZUtilityCache[zones.length];
            for (int z = 0; z < zones.length; z++) {
                TAZ azone = (TAZ)zones[z];
                czuc[z] = new CommodityZUtilityCache(cm.retrieveBuyingZUtilityForZone(azone));
                czuc[z].calcZUtilityForPreferences(myTravelPreferences, false);
            }
            return czuc;
        }
    } */

    public String toString() {
        String gender = "man";
        if (female) gender = "woman";
        return "Hi, I'm a " + age + "yo " + gender;
    };

    public static void setAllHouseholds(AllHouseholds ahh) {
        if (allHouseholds == null || allHouseholds == ahh) {
            allHouseholds = ahh;
        } else
            throw new Error("We have more than one AllHouseholds objects");
    }

    public void generateTours(SimulationDays d) {
    }

    /* not yet implemented
    */

    public Pattern choosePattern() {
        return new Pattern("hwh");
    }

 /*   public CommodityFlow retrieveCommodityFlowForTripType(int work1work2school3) {
        UnitOfLand location = null;
        Object activity = null;
        Commodity com = null;
        switch (work1work2school3) {
            case 1:
                activity = myJob.elementAt(0);
                break;
            case 2:
                activity = myJob.elementAt(1);
                break;
            case 3:
                activity = mySchooling;
                break;
        }
        if (activity instanceof RegularActivity) {
            location = ((RegularActivity)activity).getLocation();
            com = Commodity.retrieveCommodity("labour"); // this is obviously just a placeholder.  need to
            // tie into production function...
            TAZ home = (TAZ)lnkHousehold.getHomeZone();
            SellingZUtility szu = (SellingZUtility)home.getSellingCommodityZUtilities().get(com); // not tested yet.
            return szu.findExistingCommodityFlow((TAZ)TAZ.findZone(location));
        }
        return null;
    } */

    public CommodityZUtility retrieveCommodityZUtility() {
        return null;
    }


    static final double[][] marriedBirthProbs = {
		{0.0000,149.1198,88.7771,52.7183,26.0937,8.7425 ,1.5247,0.0948},
		{0.0000,30.7080 ,70.8222,52.9770,33.2212,12.8700,1.9473,0.1027},
		{0.0000,4.5863  ,26.0003,28.9927,19.6132,8.6816 ,1.6573,0.0474},
		{0.0000,0.3988  ,6.1158 ,11.3455,8.9271 ,4.4496 ,0.9778,0.0474},
		{0.0000,0.0332  ,1.4881 ,3.5036 ,3.4980 ,2.2988 ,0.6215,0.0158},
		{0.0000,0.0665  ,0.3598 ,0.8935 ,1.6277 ,1.1407 ,0.4309,0.0237},
		{0.0000,0.0000  ,0.0491 ,0.3762 ,0.5763 ,0.7576 ,0.2403,0.0158},
		{0.0000,0.0000  ,0.0000 ,0.0705 ,0.2022 ,0.4093 ,0.1574,0.0000},
		{0.0000,0.0000  ,0.0000 ,0.0705 ,0.2325 ,0.3309 ,0.2403,0.0158}    	
    };

    static final double[][] unMarriedBirthProbs = {
		{0.8184,38.9203,54.5091,43.4926,22.5971,7.6235 ,1.3356,0.0948},
		{0.0176,8.0148 ,43.4848,43.7060,28.7695,11.2226,1.7059,0.1027},
		{0.0000,1.1970 ,15.9642,23.9190,16.9851,7.5704 ,1.4518,0.0474},
		{0.0000,0.1041 ,3.7551 ,9.3600 ,7.7308 ,3.8801 ,0.8566,0.0474},
		{0.0000,0.0087 ,0.9137 ,2.8905 ,3.0293 ,2.0046 ,0.5444,0.0158},
		{0.0000,0.0173 ,0.2209 ,0.7372 ,1.4096 ,0.9947 ,0.3775,0.0237},
		{0.0000,0.0000 ,0.0301 ,0.3104 ,0.4990 ,0.6606 ,0.2105,0.0158},
		{0.0000,0.0000 ,0.0000 ,0.0582 ,0.1751 ,0.3569 ,0.1379,0.0000},
		{0.0000,0.0000 ,0.0000 ,0.0582 ,0.2014 ,0.2885 ,0.2105,0.0158}   	
    };
    
    static final double[][] multipleBirthsPer1000 = {
    	{9.2, 15.3, 22.0, 28.3, 35.8, 42.6, 44.5, 163.3},
    	{0.0, 0.1, 0.5,   1.5,  3.3,   4.1,  4.3,  22.1}
    };
    
    public int haveBabiesThisYear(boolean married) {
        if (!female) return 0;
        if (age < 10 || age >= 50) return 0;
        if (fertil ==0) fertil = 1; // women 10 and over get fertil =1,
        double[][] birthProbs = null;
        if (married) birthProbs = marriedBirthProbs;
        else birthProbs = unMarriedBirthProbs;
        int ageBand = (int) ((age-10)/5);
        int fertilToUse = Math.min(9,Math.max(fertil,1))-1;
        double births1000 = birthProbs[fertilToUse][ageBand];
        double selector = allHouseholds.theRandom.nextDouble()*1000;
        if (selector > births1000) return 0;
        // ok, we're having a baby!
        int numBabies = 1;
        
        selector = allHouseholds.theRandom.nextDouble()*1000;
        if (selector < multipleBirthsPer1000[0][ageBand]) {
        	numBabies = 2;
        }
        else if (selector < multipleBirthsPer1000[0][ageBand]+multipleBirthsPer1000[1][ageBand]) {
        	numBabies = 3;
        }
        
        fertil += numBabies;
        return numBabies;
        
        
    }
    /**
     * Method sampleInitialOccupation.
     */
    public void sampleInitialOccupation() {
        double[] theAttributes = new double[16];
        double[][] theCoefficients = {
            {-3.050,-2.250,-2.052,-2.085,-0.858,-0.136,-0.013,0.171 ,0.266 ,0.129 ,0,0,0.818,1.802,3.459,4.370}, 
            {-3.047,-2.408,-2.226,-2.079,-2.401,0.116 ,0.059 ,0.109 ,0.053 ,-0.042,0,0,0.226,1.490,2.757,4.445}, 
            {-7.820,-8.237,-7.816,-7.578,-1.729,-0.131,-0.218,0.110 ,0.281 ,0.212 ,0,0,1.910,3.735,6.086,8.921}, 
            {-5.131,-4.648,-4.091,-4.429,-2.457,-0.166,0.276 ,-0.246,-0.132,-0.090,0,0,1.346,2.385,5.632,6.867}, 
            {-4.864,-4.382,-4.727,-5.250,-0.245,-0.114,-0.178,0.128 ,0.256 ,0.041 ,0,0,0.642,1.928,3.029,3.417}, 
            {-1.224,-1.986,-1.884,-1.601,-1.576,-0.182,-0.078,-0.014,0.063 ,0.060 ,0,0,0.352,0.668,1.243,0.972}, 
            {-1.595,-1.353,-1.126,-1.093,-1.695,-0.155,-0.033,0.069 ,0.198 ,0.127 ,0,0,1.012,1.700,2.493,2.444}, 
            {0     ,0     ,0     ,0     ,0     ,0     ,0     ,0     ,0     ,0     ,0,0,0    ,0    ,0    ,0    }  
        };
        if (age <=17) theAttributes[0] = 1;
        if (age>17 && age <=25) theAttributes[1] = 1;
        if (age>25 && age <=65) theAttributes[2] = 1;
        if (age>65) theAttributes[3] = 1;
        if (!female) theAttributes[4] = 1;
        int typeCount[][] = {{0,0},{0,0},{0,0}};
        int ageBands[] = {5,18};
        lnkHousehold.countMembersByAgeAndWorkStatus(ageBands,typeCount);
        if (typeCount[0][0] > 0 ) theAttributes[5] = 1;
        if (typeCount[1][0] > 0) theAttributes[6]=1;
        int hhSize = typeCount[0][0]+typeCount[1][0]+typeCount[2][0];
        if (hhSize == 1) theAttributes[7] = 1;
        if (hhSize == 2) theAttributes[8] = 1;
        if (hhSize == 3) theAttributes[9] = 1;
        if (yearsSchool==12) theAttributes[12] = 1;
        if (yearsSchool>12 && yearsSchool<16) theAttributes[13] = 1;
        if (yearsSchool==16) theAttributes[14] = 1;
        if (yearsSchool>16) theAttributes[15] = 1;
        int choice = LogitModel.arrayCoefficientSimplifiedChoice(theCoefficients,theAttributes);
        switch (choice) {
        	case 0: this.occupation=1; break;
        	case 1: this.occupation=83; break;
        	case 2: this.occupation=113; break;
        	case 3: this.occupation=155; break;
        	case 4: this.occupation=163; break;
        	case 5: this.occupation=263; break;
        	case 6: this.occupation=283; break;
        	default: this.occupation=500; break;
        }
        
      }


    private static final int[] ageBandsForWorkStatus = {5,18};
    /**
     * Method workStatusTransition.
     */
    public void workStatusTransition() {
        if (age <=5) {
            employStatus = 0;
            schoolStatus = 0;
            return;
        }
        if (age <=15) {
            employStatus = 0;
            schoolStatus = 2;
            return;
        }
        int ageBands[] = {5,18};
        int typeCount[][] = {{0,0},{0,0},{0,0}};
        lnkHousehold.countMembersByAgeAndWorkStatus(ageBands,typeCount);
        double uws = 0;
        double uwns = 0;
        double unws = 0;
        double unwns = 0;
        if (age<=17) {
            unwns += -3.106;
        }
        if (age>17 && age <=25) {
            uwns += 1.461;
            unws += -0.102;
            unwns += -0.798;
        }
        if (age>25 && age <= 65) {
            uwns += 3.967;
            unws += -1.679;
            unwns += 1.746;
        }
        if (age>65) {
            uwns += -5;
            unws += 0.065;
            unwns += 0;
        }
        if (typeCount[0][0]>0) {
            uwns += 1.338;
            unws += 0.333;
            unwns += 1.476;
        }
        if (typeCount[1][0]>0) {
            uwns += -0.015;
            unws += 0.198;
            unwns += -0.226;
        }
        if (yearsSchool<12) {
            uwns += 2.753;
            unws += 0.964;
            unwns += 5.976;
        }
        if (yearsSchool==12) {
            uwns += 1.857;
            unws += -.400;
            unwns += 4.735;
        }
        if (yearsSchool>12 && yearsSchool <16) {
            uwns += 0.734;
            unws += -.0190;
            unwns += 3.277;
        }
        if (yearsSchool==16) {
            uwns += 1.494;
            unws += -.726;
            unwns += 3.483;
        }
        int persons = typeCount[0][0]+typeCount[1][0]+typeCount[2][0];
        if (persons == 2) {
            uwns += 0.204;
            unws += 0.487;
            unwns += 0.568;
        }
        if (persons == 3) {
            uwns += -1.790;
            unws += 0.495;
            unwns += -1.516;
        }
        if (persons >= 4) {
            uwns += -2.296;
            unws += 0.406;
            unwns += -1.968;
        }
        if (schoolStatus >=2 && employStatus >=1) {
            uwns += -0.861;
            unws += -0.599;
            unwns += -4.125;
        }
        if (schoolStatus <2 && employStatus >=1) {
            uwns += -0.861;
            unws += -0.599;
            unwns += -4.125;
        }
        if (schoolStatus >=2 && employStatus ==0) {
            uwns += -2.689;
            unws += 0.502;
            unwns += -3.073;
        }
        double expuws = Math.exp(uws);
        double expuwns = Math.exp(uwns);
        double expunws = Math.exp(unws);
        double expunwns = Math.exp(unwns);
        double denom = expuws+expuwns+expunws+expunwns;
        short oldEmployStatus = employStatus;
        double selector = allHouseholds.theRandom.nextDouble() * denom;
        if (selector < expuws) {
            schoolStatus = 2;
            employStatus = 2;
        }
        else if (selector < expuws + expuwns) {
            schoolStatus = 1;
            employStatus = 2;
        }
        else if (selector < expuws + expuwns + expunws) {
            schoolStatus = 2;
            employStatus = 0;
        }
        else {
            schoolStatus = 1;
            employStatus = 0;
        }
        if (employStatus >=1 && oldEmployStatus == 0) {
            this.sampleInitialOccupation();
        }
        if (employStatus >=1 && oldEmployStatus != 0) {
        	this.occupationTransition();
        }
        if (employStatus ==0) occupation=0;
    }

    /**
     * Method occupationTransition.
     */
    private void occupationTransition() {
        double[] theAttributes = new double[24];
        final double[][] theCoefficients = {
             {-6.708,-6.408,-6.631,-6.387,0.769 ,-0.004,-0.072,0.198 ,0.270 ,0.244 ,0,0,-0.703,-0.310,-0.134,0.514 ,6.385,3.062,3.959,3.080,3.812,3.503,3.442,0},
             {-7.291,-7.670,-7.392,-8.518,0.903 ,-0.027,-0.032,-0.257,0.004 ,0.070 ,0,0,-0.619,-0.180,0.503 ,0.272 ,3.356,7.238,3.371,2.580,3.178,2.316,0.581,0},
             {-9.024,-8.884,-8.912,-8.699,1.684 ,-0.038,0.205 ,0.372 ,0.580 ,0.563 ,0,0,-1.568,-0.927,-0.070,0.527 ,3.876,3.591,7.660,3.968,3.472,2.506,2.139,0},
             {-7.140,-7.207,-7.177,-7.180,0.663 ,-0.192,0.115 ,0.019 ,-0.029,-0.084,0,0,-0.786,0.005 ,0.260 ,-0.076,3.373,3.964,4.888,6.990,2.687,1.098,1.955,0},
             {-4.349,-4.927,-5.822,-15   ,-1.061,-1.581,-2.088,-4.009,-3.740,-1.994,0,0,0.889 ,1.821 ,1.417 ,1.718 ,2.776,-15  ,-15  ,2.895,7.925,-15  ,2.266,0},
             {-7.288,-7.220,-7.501,-7.326,0.167 ,-0.099,-0.293,-0.085,-0.019,0.102 ,0,0,-0.223,0.074 ,0.338 ,0.264 ,3.763,3.274,2.696,2.802,3.042,7.157,3.949,0},
             {-6.483,-6.736,-6.679,-6.254,-0.760,-0.018,-0.033,-0.290,-0.112,-0.136,0,0,0.266 ,0.213 ,-0.389,-0.182,4.191,-15  ,3.477,3.356,4.039,4.287,6.549,0},
             {0     ,0     ,0     ,0     ,0     ,0     ,0     ,0     ,0     ,0     ,0,0,0     ,0     ,0     ,0     ,0    ,0    ,0    ,0    ,0    ,0    ,0    ,0} 
        };
        if (age <=17) theAttributes[0] = 1;
        if (age>17 && age <=25) theAttributes[1] = 1;
        if (age>25 && age <=65) theAttributes[2] = 1;
        if (age>65) theAttributes[3] = 1;
        if (!female) theAttributes[4] = 1;
        int ageBands[] = {5,18};
        int typeCount[][] = {{0,0},{0,0},{0,0}};
        lnkHousehold.countMembersByAgeAndWorkStatus(ageBands,typeCount);
        if (typeCount[0][0] > 0 ) theAttributes[5] = 1;
        if (typeCount[1][0] > 0) theAttributes[6]=1;
        int hhSize = typeCount[0][0]+typeCount[1][0]+typeCount[2][0];
        if (hhSize == 1) theAttributes[7] = 1;
        if (hhSize == 2) theAttributes[8] = 1;
        if (hhSize == 3) theAttributes[9] = 1;
        if (yearsSchool==12) theAttributes[12] = 1;
        if (yearsSchool>12 && yearsSchool<16) theAttributes[13] = 1;
        if (yearsSchool==16) theAttributes[14] = 1;
        if (yearsSchool>16) theAttributes[15] = 1;
        if (occupation <=82) theAttributes[16] = 1;
        if (occupation >82 && occupation <=112) theAttributes[17]=1;
        if (occupation >112 && occupation <=154) theAttributes[18]=1;
        if (occupation >154 && occupation <=162) theAttributes[19]=1;
        if (occupation >162 && occupation <=262) theAttributes[20]=1;
        if (occupation >262 && occupation <=282) theAttributes[21]=1;
        if (occupation >282 && occupation <=402) theAttributes[22]=1;
        if (occupation >402 ) theAttributes[23]=1;
        
        int choice = LogitModel.arrayCoefficientSimplifiedChoice(theCoefficients,theAttributes);
        switch (choice) {
        	case 0: this.occupation=1; break;
        	case 1: this.occupation=83; break;
        	case 2: this.occupation=113; break;
        	case 3: this.occupation=155; break;
        	case 4: this.occupation=163; break;
        	case 5: this.occupation=263; break;
        	case 6: this.occupation=283; break;
        	default: this.occupation=500; break;
        }
        
    }


}

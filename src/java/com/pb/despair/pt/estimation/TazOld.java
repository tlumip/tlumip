package com.pb.despair.pt.estimation;

import com.pb.common.math.MathUtil;
import com.pb.common.model.Alternative;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.model.Mode;
import com.pb.despair.model.ModeType;
import com.pb.despair.pt.ActivityPurpose;
import com.pb.despair.pt.PTModelInputs;
import com.pb.despair.pt.StopDestinationParameters;
import com.pb.despair.pt.StopDestinationParametersData;
import com.pb.despair.pt.TazData;
import com.pb.despair.pt.TourDestinationParameters;
import com.pb.despair.pt.TourDestinationParametersData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

import java.io.PrintWriter;

/**
 * A class containing all the necessary information about a TAZ
 *
 * @author J Freedman
 * @version 1.0 12/01/2003
 *
 */
public class TazOld implements Alternative{
    final static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
    private boolean NEW_UTILITY_METHOD = true;

    //attributes
   public int zoneNumber;
   public float population;
   public float households;
   public float agriculture_Office;
   public float agriculture_Agriculture;
   public float fFM_Office;
   public float fFM_Forest;
   public float lightIndustry_Office;
   public float lightIndustry_LightIndustrial;
   public float heavyIndustry_Office;
   public float heavyIndustry_HeavyIndustrial;
   public float wholesale_Office;
   public float wholesale_Warehouse;
   public float retail_Office;
   public float retail_Retail;
   public float hotel_Hotel;
   public float construction_Construction;
   public float healthCare_Office;
   public float healthCare_Hospital;
   public float healthCare_Institutional;
   public float transportation_Office;
   public float transportation_DepotSpace;
   public float otherServices_Office;
   public float otherServices_LightIndustrial;
   public float otherServices_Retail;
   public float gradeSchool_Office;
   public float gradeSchool_GradeSchool;
   public float postSecondary_Institutional;
   public float government_Office;
   public float government_GovernmentSupport;
   public float government_Institutional;
   public float workParkingCost;
   public float nonWorkParkingCost;
   public float acres;
   public double lnAcres;
   public float pricePerAcre;
   public float pricePerSqFtSFD;
   public float singleFamilyHH;
   public float multiFamilyHH;
   public float totalRetail;
   public float nonRetail;
   public float totalOffice;
   public float nonOffice;
   public float totalIndustrial;
   public float nonIndustrial;
   public float otherWork;
   public double sizeTerm;

   public boolean collapsedEmployment;
   public boolean hasUtility;
   public float totalEmployment;
   public double utility;
   public float retailEmploymentWithin30MinutesTransit;
   public double[][] tourLnSizeTerm = new double[ActivityPurpose.ACTIVITY_PURPOSES.length][];
   public double[] stopLnSizeTerm = new double[ActivityPurpose.ACTIVITY_PURPOSES.length];
   public double[][] tourSizeTerm = new double[ActivityPurpose.ACTIVITY_PURPOSES.length][];
   public double[] stopSizeTerm = new double[ActivityPurpose.ACTIVITY_PURPOSES.length];
   public double constant;
   public double expConstant;
   public String name;
   public boolean isAvailable;
    ArrayList alternativeObservers;

     public TazOld(){

          population=0;
          households=0;
          agriculture_Office=0;
          agriculture_Agriculture=0;
          fFM_Office=0;
          fFM_Forest=0;
          lightIndustry_Office=0;
          lightIndustry_LightIndustrial=0;
          heavyIndustry_Office=0;
          heavyIndustry_HeavyIndustrial=0;
          wholesale_Office=0;
          wholesale_Warehouse=0;
          retail_Office=0;
          retail_Retail=0;
          hotel_Hotel=0;
          construction_Construction=0;
          healthCare_Office=0;
          healthCare_Hospital=0;
          healthCare_Institutional=0;
          transportation_Office=0;
          transportation_DepotSpace=0;
          otherServices_Office=0;
          otherServices_LightIndustrial=0;
          otherServices_Retail=0;
          gradeSchool_Office=0;
          gradeSchool_GradeSchool=0;
          postSecondary_Institutional=0;
          government_Office=0;
          government_GovernmentSupport=0;
          government_Institutional=0;
          workParkingCost=0;
          nonWorkParkingCost=0;
          acres=0;
          pricePerAcre=0;
          pricePerSqFtSFD=0;
          singleFamilyHH=0;
          multiFamilyHH=0;

          //following variables specifically for tour destination choice
          collapsedEmployment=false;
          totalEmployment=0;
          totalRetail=0;
          nonRetail=0;
          totalOffice=0;
          nonOffice=0;
          totalIndustrial=0;
          nonIndustrial=0;
          otherWork=0;

          sizeTerm=0;
          hasUtility=false;
          utility=0;
          isAvailable=true;
     };

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
    Get the list of observers for this alternative.
    @return ao  A list of alternative observers.
    */
    public ArrayList getAlternativeObservers(){
        return alternativeObservers;
    }


     /**
     * Returns the collection of alternative observers or null if no observers
     * have been attached.
     */
    public Collection getObservers(){
        return alternativeObservers;
    }


     /**
     * To create aggregates of employment for destination
     * choice.   Use before calculating utility
     *
     **/
     void collapseEmployment(){

          totalEmployment=agriculture_Office+
               agriculture_Agriculture+
               fFM_Office+
               fFM_Forest+
               lightIndustry_Office+
               lightIndustry_LightIndustrial+
               heavyIndustry_Office+
               heavyIndustry_HeavyIndustrial+
               wholesale_Office+
               wholesale_Warehouse+
               retail_Office+
               retail_Retail+
               hotel_Hotel+
               construction_Construction+
               healthCare_Office+
               healthCare_Hospital+
               healthCare_Institutional+
               transportation_Office+
               transportation_DepotSpace+
               otherServices_Office+
               otherServices_LightIndustrial+
               otherServices_Retail+
               gradeSchool_Office+
               gradeSchool_GradeSchool+
               postSecondary_Institutional+
               government_Office+
               government_GovernmentSupport+
               government_Institutional;

          totalRetail=retail_Office+retail_Retail;
          nonRetail=totalEmployment - totalRetail;

          totalOffice=agriculture_Office+
               fFM_Office+
               lightIndustry_Office+
               heavyIndustry_Office+
               wholesale_Office+
               retail_Office+
               healthCare_Office+
               transportation_Office+
               otherServices_Office+
               gradeSchool_Office+
               government_Office;
          nonOffice= totalEmployment-totalOffice;
          totalIndustrial     = lightIndustry_LightIndustrial+
               heavyIndustry_HeavyIndustrial+
               otherServices_LightIndustrial+
               nonIndustrial;
          nonIndustrial = totalEmployment - totalIndustrial;

          otherWork=totalEmployment-(totalRetail+totalOffice+totalIndustrial);

          collapsedEmployment=true;
     };

    public void setLnAcres(){
        lnAcres = MathUtil.log(acres);
    }

    public double getParkingCost(char actPurpose){
        if(actPurpose=='w')
            return this.workParkingCost;
        else
            return this.nonWorkParkingCost;
    }

    public void initSizeTermArray(){
        tourSizeTerm[ActivityPurpose.WORK] = new double[ActivityPurpose.WORK_SEGMENTS];
        tourSizeTerm[ActivityPurpose.SCHOOL] = new double[ActivityPurpose.SCHOOL_SEGMENTS];
        tourSizeTerm[ActivityPurpose.SHOP] = new double[1];
        tourSizeTerm[ActivityPurpose.RECREATE] = new double[1];
        tourSizeTerm[ActivityPurpose.OTHER] = new double[1];
        tourSizeTerm[ActivityPurpose.WORK_BASED] = new double[1];

        tourLnSizeTerm[ActivityPurpose.WORK] = new double[ActivityPurpose.WORK_SEGMENTS];
        tourLnSizeTerm[ActivityPurpose.SCHOOL] = new double[ActivityPurpose.SCHOOL_SEGMENTS];
        tourLnSizeTerm[ActivityPurpose.SHOP] = new double[1];
        tourLnSizeTerm[ActivityPurpose.RECREATE] = new double[1];
        tourLnSizeTerm[ActivityPurpose.OTHER] = new double[1];
        tourLnSizeTerm[ActivityPurpose.WORK_BASED] = new double[1];

    }
    // New method calculates the size terms in constructor to save run time.
    public void setTourSizeTerms(TourDestinationParametersData tdpd){
        initSizeTermArray();
        TourDestinationParameters destParams = new TourDestinationParameters();
        float sizeTerm;
        //start at 1 because don't need for home
        //TODO fix this segment thing
        for(int i=1;i<ActivityPurpose.ACTIVITY_PURPOSES.length;i++){
            int purpose = ActivityPurpose.getActivityPurposeValue(ActivityPurpose.ACTIVITY_PURPOSES[i]);
            for(int segmentMinusOne=0;segmentMinusOne<ActivityPurpose.getDCSegments(purpose);segmentMinusOne++){
                destParams = (TourDestinationParameters) tdpd.getParameters(purpose,segmentMinusOne+1);
                sizeTerm = (destParams.retail         *  retail_Retail
                            +destParams.nonRetail          * nonRetail
                            +destParams.gradeSchool        * gradeSchool_GradeSchool
                            +destParams.secondarySchool    * 1
                            +destParams.postSecondarySchool* postSecondary_Institutional
                            +destParams.households         * households
                            +destParams.office             * totalOffice
                            +destParams.nonOffice          * nonOffice
                            +destParams.industrial         * totalIndustrial
                            +destParams.nonIndustrial      * nonIndustrial
                            +destParams.otherWork          * otherWork);

                tourSizeTerm[purpose][segmentMinusOne] = sizeTerm;
                tourLnSizeTerm[purpose][segmentMinusOne] = MathUtil.log(sizeTerm);
            }
        }
    }//end setTourDestinationSizeTerms

    public void setStopSizeTerms(StopDestinationParametersData sdpd){
        StopDestinationParameters stopdestinationparameters = new StopDestinationParameters();
        float sizeTerm;
        for(int i=1;i<ActivityPurpose.ACTIVITY_PURPOSES.length;i++){
            int purpose = ActivityPurpose.getActivityPurposeValue(ActivityPurpose.ACTIVITY_PURPOSES[i]);
        	stopdestinationparameters =
                (StopDestinationParameters) sdpd.stopDestinationParameters[purpose];

            sizeTerm =  stopdestinationparameters.retail  * retail_Retail
                        + stopdestinationparameters.nonRetail * nonRetail
                        + stopdestinationparameters.gradeSchool * gradeSchool_GradeSchool
                        + stopdestinationparameters.hhs * households;

            stopSizeTerm[purpose] = sizeTerm;
            stopLnSizeTerm[purpose] = MathUtil.log(sizeTerm);
        }
    }

     /** to calculate utility for tour destination choice
     *   this method is still used in CreateDestinationChoiceLogsums
     *   all others use the alternative version
     *@param tdp:  TourDestinationChoiceParameters (see class)
     *@param logsum:  the mode choice logsum
     *
     **/

    private void calcTourDestinationUtility(TourDestinationParameters  tdp, double logsum){

         if(collapsedEmployment==false){
               collapseEmployment();
               collapsedEmployment=true;
          }

         sizeTerm = (tdp.retail         * retail_Retail
                    +tdp.nonRetail          * nonRetail
                    +tdp.gradeSchool        * gradeSchool_GradeSchool
                    +tdp.secondarySchool    * 1
                    +tdp.postSecondarySchool* postSecondary_Institutional
                    +tdp.households         * households
                    +tdp.office             * totalOffice
                    +tdp.nonOffice          * nonOffice
                    +tdp.industrial         * totalIndustrial
                    +tdp.nonIndustrial      * nonIndustrial
                    +tdp.otherWork          * otherWork
          );

         if(sizeTerm>0 && acres>0){
                  utility= tdp.logsum*(logsum-10.0) + lnAcres
                       + MathUtil.log(sizeTerm);
                  hasUtility=true;
                  isAvailable=true;
         }
         else {
             hasUtility=false;
             isAvailable=false;
         }

     }

    /* Testing New Method.  Once implemented, this should save significant time.*/
    void calcTourDestinationUtility(int activityPurpose, int segment,
                                    TourDestinationParameters tdp,
                                    double logsum){
        if(NEW_UTILITY_METHOD)
            calcNewTourDestinationUtility(activityPurpose,segment,tdp,logsum);
        else
            calcTourDestinationUtility(tdp,logsum);
    }

    void calcNewTourDestinationUtility(int activityPurpose, int segment,
            TourDestinationParameters tdp,
            double logsum){
         //if(collapsedEmployment=false){
         //      collapseEmployment();
         //      collapsedEmployment=true;
         // }

         if(tourSizeTerm[activityPurpose][segment-1]>0 && acres>0){
                utility= tdp.logsum*(logsum-10.0) + lnAcres
                    + tourLnSizeTerm[activityPurpose][segment-1];
                hasUtility=true;
                isAvailable=true;
         }
                  //if(utilityNew!=utility){
                  //    logger.info("New Utility= "+utilityNew);
                  //    logger.info("Old Utility= "+utility);
                  //    logger.info("Different Utilities");
                  //}
         else {
                hasUtility=false;
                isAvailable=false;
         }

     }


    void calcStopDestinationUtility(int actPurpose,
                                    StopDestinationParameters stopdestinationparameters,
									Mode mode,
									float autoTime,
									float walkTime,
									float bikeTime,
									float transitGeneralizedCost,
									int stopNumber){
    	if(NEW_UTILITY_METHOD)
    		calcNewStopDestinationUtility(actPurpose,
                                          stopdestinationparameters,
										  mode,
										  autoTime,
										  walkTime,
										  bikeTime,
										  transitGeneralizedCost,
										  stopNumber);
    	else
            calcStopDestinationUtility(stopdestinationparameters,
                                       mode,
									   autoTime,
									   walkTime,
									   bikeTime,
									   transitGeneralizedCost,
									   stopNumber);
    }


     /** to calculate utility for stop destination choice
      *
      * @param stopdestinationparameters
      * @param mode
      * @param autoTime
      * @param walkTime
      * @param bikeTime
      * @param transitGeneralizedCost
      * @param stopNumber
      */

    void calcStopDestinationUtility(StopDestinationParameters stopdestinationparameters,
                                       Mode mode,
                                       float autoTime,
                                       float walkTime,
                                       float bikeTime,
                                       float transitGeneralizedCost,
                                       int stopNumber) {
        if(mode.type == ModeType.WALK && walkTime > 120)
            return;
        if(mode.type == ModeType.BIKE && bikeTime > 120)
            return;
        boolean flag = false;
        if(mode.type == ModeType.WALKTRANSIT || mode.type == ModeType.DRIVETRANSIT)
            flag = true;
        if(mode.type == ModeType.TRANSITPASSENGER && stopNumber == 1)
            flag = true;
        if(mode.type == ModeType.PASSENGERTRANSIT && stopNumber == 2)
            flag = true;
        if(flag && transitGeneralizedCost == 0.0 && walkTime > 120)
            return;
        if(!collapsedEmployment) {
            collapseEmployment();
            collapsedEmployment = true;
        }
        sizeTerm = stopdestinationparameters.retail *  retail_Retail
             + stopdestinationparameters.nonRetail * nonRetail
             + stopdestinationparameters.gradeSchool * gradeSchool_GradeSchool
             + stopdestinationparameters.hhs * households;

        if(sizeTerm > 0.0 && acres > 0.0) {
            if(mode.type == ModeType.WALK)
                utility = stopdestinationparameters.timeWalk * walkTime
                + stopdestinationparameters.intraNonMotor * lnAcres
                + MathUtil.log(sizeTerm);
            else
            if(mode.type == ModeType.BIKE)
                utility = stopdestinationparameters.timeBike * bikeTime
                + stopdestinationparameters.intraNonMotor * lnAcres
                + MathUtil.log(sizeTerm);
            else
            if(flag)
                utility = stopdestinationparameters.timeTransit * transitGeneralizedCost
                + stopdestinationparameters.intraTransit * lnAcres
                + MathUtil.log(sizeTerm);
            else
                utility = stopdestinationparameters.timeAuto * autoTime
                + stopdestinationparameters.intraAuto * lnAcres
                + MathUtil.log(sizeTerm);
            hasUtility = true;
            isAvailable = true;

        }
        else {
            hasUtility = false;
            isAvailable = false;
        }
    }

    //TODO testing changing actPurpose from char to int
    private void calcNewStopDestinationUtility(int actPurpose,
                                       StopDestinationParameters stopdestinationparameters,
                                       Mode mode,
                                       float autoTime,
                                       float walkTime,
                                       float bikeTime,
                                       float transitGeneralizedCost,
                                       int stopNumber)
                                       {
        //int actPurposeValue = ActivityPurpose.getActivityPurposeValue(actPurpose);
        if(mode.type == ModeType.WALK && walkTime > 120)
            return;
        if(mode.type == ModeType.BIKE && bikeTime > 120)
            return;
        boolean flag = false;
        if(mode.type == ModeType.WALKTRANSIT || mode.type == ModeType.DRIVETRANSIT)
            flag = true;
        if(mode.type == ModeType.TRANSITPASSENGER && stopNumber == 1)
            flag = true;
        if(mode.type == ModeType.PASSENGERTRANSIT && stopNumber == 2)
            flag = true;
        if(flag && transitGeneralizedCost == 0.0 && walkTime > 120)
            return;
        //if(!collapsedEmployment) {
        //    collapseEmployment();
        //    collapsedEmployment = true;
        //}
        int purpose = actPurpose;//ActivityPurpose.getActivityPurposeValue(actPurpose);
        if(stopSizeTerm[purpose] > 0.0 && acres > 0.0) {
            if(mode.type == ModeType.WALK)
            utility = stopdestinationparameters.timeWalk * walkTime
                + stopdestinationparameters.intraNonMotor * lnAcres
                + stopLnSizeTerm[purpose];
            else
            if(mode.type == ModeType.BIKE)
            utility = stopdestinationparameters.timeBike * bikeTime
                + stopdestinationparameters.intraNonMotor * lnAcres
                + stopLnSizeTerm[purpose];
            else
            if(flag)
            utility = stopdestinationparameters.timeTransit * transitGeneralizedCost
                + stopdestinationparameters.intraTransit * lnAcres
                + stopLnSizeTerm[purpose];
            else
            utility = stopdestinationparameters.timeAuto * autoTime
                + stopdestinationparameters.intraAuto * lnAcres
                + stopLnSizeTerm[purpose];
            hasUtility = true;
            isAvailable = true;
            //if(utilityNew!=utility){
           //     logger.info("New Utility= "+utilityNew);
           //     logger.info("Old Utility= "+utility);
           //     logger.info("Different Utilities");}
        }
        else {
            hasUtility = false;
            isAvailable = false;
        }
    }

     public double getUtility(){
          if(!hasUtility){
          	   Exception e = new Exception();
               logger.fatal("Error: Utility not calculated for zone "+zoneNumber+"\n");
               e.printStackTrace();
               System.exit(1);
          };
          return utility;
     };



     public void print(PrintWriter p){

          p.print(
                " "+ zoneNumber
               +" "+ population
               +" "+ households
                +" "+ agriculture_Office
               +" "+ agriculture_Agriculture
               +" "+ fFM_Office
               +" "+ fFM_Forest
               +" "+ lightIndustry_Office
               +" "+ lightIndustry_LightIndustrial
               +" "+ heavyIndustry_Office
               +" "+ heavyIndustry_HeavyIndustrial
               +" "+ wholesale_Office
               +" "+ wholesale_Warehouse
               +" "+ retail_Office
               +" "+ retail_Retail
               +" "+ hotel_Hotel
               +" "+ construction_Construction
               +" "+ healthCare_Office
               +" "+ healthCare_Hospital
               +" "+ healthCare_Institutional
               +" "+ transportation_Office
               +" "+ transportation_DepotSpace
               +" "+ otherServices_Office
               +" "+ otherServices_LightIndustrial
               +" "+ otherServices_Retail
               +" "+ gradeSchool_Office
               +" "+ gradeSchool_GradeSchool
               +" "+ postSecondary_Institutional
               +" "+ government_Office
               +" "+ government_GovernmentSupport
               +" "+ government_Institutional
               +" "+ workParkingCost
               +" "+ nonWorkParkingCost
               +" "+ acres
               +" "+ pricePerAcre
               +" "+ pricePerSqFtSFD
               +" "+ singleFamilyHH
               +" "+ multiFamilyHH
          );


     };
     /**
     *print():For printing zone attributes to the screen
     *
     *
     **/
     public void print(){

          System.out.print(
                "\n"+ zoneNumber
               +"\n"+ population
               +"\n"+ households
                +"\n"+ agriculture_Office
               +"\n"+ agriculture_Agriculture
               +"\n"+ fFM_Office
               +"\n"+ fFM_Forest
               +"\n"+ lightIndustry_Office
               +"\n"+ lightIndustry_LightIndustrial
               +"\n"+ heavyIndustry_Office
               +"\n"+ heavyIndustry_HeavyIndustrial
               +"\n"+ wholesale_Office
               +"\n"+ wholesale_Warehouse
               +"\n"+ retail_Office
               +"\n"+ retail_Retail
               +"\n"+ hotel_Hotel
               +"\n"+ construction_Construction
               +"\n"+ healthCare_Office
               +"\n"+ healthCare_Hospital
               +"\n"+ healthCare_Institutional
               +"\n"+ transportation_Office
               +"\n"+ transportation_DepotSpace
               +"\n"+ otherServices_Office
               +"\n"+ otherServices_LightIndustrial
               +"\n"+ otherServices_Retail
               +"\n"+ gradeSchool_Office
               +"\n"+ gradeSchool_GradeSchool
               +"\n"+ postSecondary_Institutional
               +"\n"+ government_Office
               +"\n"+ government_GovernmentSupport
               +"\n"+ government_Institutional
               +"\n"+ workParkingCost
               +"\n"+ nonWorkParkingCost
               +"\n"+ acres
               +"\n"+ pricePerAcre
               +"\n"+ pricePerSqFtSFD
               +"\n"+ singleFamilyHH
               +"\n"+ multiFamilyHH


          );
     }

     public static void main(String[] args){
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
         ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
        //read the tourDestinationParameters from csv to TableDataSet
        logger.info("Reading tour destination parameters");
        TourDestinationParametersData tdpd = new TourDestinationParametersData();
        tdpd.readData(rb,"tourDestinationParameters.file");
        //read the stopDestinationParameters from csv to TableDataSet
        logger.info("Reading stop destination parameters");
        StopDestinationParametersData sdpd = new StopDestinationParametersData();
        sdpd.readData(rb,"stopDestinationParameters.file");

        logger.info("Adding TazData");
        TazData tazs = new TazData();
        tazs.readData(rb,globalRb,"tazData.file");
        tazs.collapseEmployment(PTModelInputs.tdpd, PTModelInputs.sdpd);
     }
}























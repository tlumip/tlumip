package com.pb.despair.pt;

import com.pb.common.math.MathUtil;
import com.pb.common.model.Alternative;
import com.pb.common.util.ResourceUtil;
//import com.pb.common.model.ConcreteAlternative;
import com.pb.despair.model.Mode;
import com.pb.despair.model.ModeType;

import java.util.ResourceBundle;
import java.util.logging.Logger;
//import java.util.Iterator;
import java.io.PrintWriter;
import java.lang.RuntimeException;

/** 
 * A class containing all the necessary information about a TAZ
 * 
 * @author J Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class Taz implements Alternative, Cloneable{
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");

     //attributes
     public int zoneNumber;
     float households;       
     float accommodation;
     float agriculture;
     float depot;
     float government;
     float gradeSchool;
     float heavyIndustry;
     float hospital;
     float institutional;
     float lightIndustry;
     float logging;
     float office;
     float retail;
     float warehouse;
     float postSecondaryOccupation;
     float otherSchoolOccupation;
     float workParkingCost;
     float nonWorkParkingCost;
     float acres;
     double lnAcres;
     float pricePerAcre;
     float pricePerSqFtSFD;    
     float singleFamilyHH;
     float multiFamilyHH;       
     float nonRetail;
     float totalOffice;
     float nonOffice;
     float totalIndustrial;
     float nonIndustrial;
     float otherWork;                         
     double sizeTerm;
     
     boolean collapsedEmployment;
     boolean tourSizeTermsSet;
     boolean stopSizeTermsSet;
     float totalEmployment;
     public double utility;
     float retailEmploymentWithin30MinutesTransit;

     double[][] tourSizeTerm = new double[ActivityPurpose.ACTIVITY_PURPOSE.length][];
     double[] stopSizeTerm = new double[ActivityPurpose.ACTIVITY_PURPOSE.length];

    double[][] tourLnSizeTerm = new double[ActivityPurpose.ACTIVITY_PURPOSE.length][];
    double[] stopLnSizeTerm = new double[ActivityPurpose.ACTIVITY_PURPOSE.length];

    double constant;
    double expConstant;
    String name;
    boolean isAvailable;
  
     public Taz(){      
          
          //following variables specifically for tour destination choice
          collapsedEmployment=false;
          tourSizeTermsSet=false;
         stopSizeTermsSet = false;
          totalEmployment=0;
          nonRetail=0;
          totalOffice=0;
          nonOffice=0;
          totalIndustrial=0;
          nonIndustrial=0;        
          otherWork=0;                 
          
          sizeTerm=0;
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
     * To create aggregates of employment for destination
     * choice.   Use before calculating utility
     *
     **/
     void collapseEmployment(){
          
          totalEmployment= accommodation+
                           agriculture+
						   depot+
						   government+
						   heavyIndustry+
						   hospital+
						   institutional+
						   lightIndustry+
						   logging+
						   office+
						   retail+
						   warehouse+
						   postSecondaryOccupation+
						   otherSchoolOccupation;
          
                    
          nonRetail=totalEmployment - retail;
          
          totalOffice=office;                        
          nonOffice= totalEmployment-totalOffice;                                
          totalIndustrial     = lightIndustry+heavyIndustry;                            
          nonIndustrial = totalEmployment - totalIndustrial;
          
          otherWork=totalEmployment-(retail+totalOffice+totalIndustrial);                       

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
 
     public Object clone(){
         Taz newTaz;
          try {
             newTaz = (Taz) super.clone();

            //clone arrays
             newTaz.tourSizeTerm = new double[this.tourSizeTerm.length][];
             newTaz.tourLnSizeTerm = new double[this.tourLnSizeTerm.length][];
             
             for(int i=0;i<this.tourSizeTerm.length;++i){
                 newTaz.tourSizeTerm[i] = new double[this.tourSizeTerm[i].length];
                 newTaz.tourLnSizeTerm[i] = new double[this.tourLnSizeTerm[i].length];
                for(int j=0;j<this.tourSizeTerm[i].length;++j){
                    newTaz.tourSizeTerm[i][j]=this.tourSizeTerm[i][j];
                    newTaz.tourLnSizeTerm[i][j]=this.tourLnSizeTerm[i][j];
                }
            }
            //stop size terms are single dimension, ok to use clone method
             newTaz.stopSizeTerm = (double[]) this.stopSizeTerm.clone();
             newTaz.stopLnSizeTerm = (double[]) stopLnSizeTerm.clone();
             if(this.name!=null)
                newTaz.name = new String(this.name);
             else
                newTaz.name = new String();
         }
         catch (CloneNotSupportedException e) {
             throw new RuntimeException(e.toString());
         }
          
         return newTaz;
      }
 
     public void initSizeTermArray(){
        tourSizeTerm[ActivityPurpose.HOME] = new double[1];
        tourSizeTerm[ActivityPurpose.WORK] = new double[ActivityPurpose.WORK_SEGMENTS];
        tourSizeTerm[ActivityPurpose.SCHOOL] = new double[ActivityPurpose.SCHOOL_SEGMENTS];
        tourSizeTerm[ActivityPurpose.SHOP] = new double[1];
        tourSizeTerm[ActivityPurpose.RECREATE] = new double[1];
        tourSizeTerm[ActivityPurpose.OTHER] = new double[1];
        tourSizeTerm[ActivityPurpose.WORK_BASED] = new double[1];

        tourLnSizeTerm[ActivityPurpose.HOME] = new double[1];
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
        for(int i=1;i<ActivityPurpose.ACTIVITY_PURPOSE.length;i++){
            int purpose = ActivityPurpose.getActivityPurposeValue(ActivityPurpose.ACTIVITY_PURPOSE[i]);
            for(int segmentMinusOne=0;segmentMinusOne<ActivityPurpose.getDCSegments(purpose);segmentMinusOne++){
                destParams = (TourDestinationParameters) tdpd.getParameters(purpose,segmentMinusOne+1);
                sizeTerm = (destParams.retail           * retail
                            +destParams.nonRetail          * nonRetail
                            +destParams.gradeSchool        * otherSchoolOccupation
                            +destParams.secondarySchool    * 0
                            +destParams.postSecondarySchool* postSecondaryOccupation
                            +destParams.households         * households
                            +destParams.office             * totalOffice
                            +destParams.nonOffice          * nonOffice
                            +destParams.industrial         * totalIndustrial
                            +destParams.nonIndustrial      * nonIndustrial
                            +destParams.otherWork          * otherWork);
                
                tourSizeTerm[purpose][segmentMinusOne] = sizeTerm;
                if(sizeTerm>0)
                    tourLnSizeTerm[purpose][segmentMinusOne] = MathUtil.log(sizeTerm);
            }
        }
        tourSizeTermsSet = true;
    }//end setTourDestinationSizeTerms
    
    public void setStopSizeTerms(StopDestinationParametersData sdpd){
        StopDestinationParameters stopdestinationparameters = new StopDestinationParameters();
        float sizeTerm;
        for(int i=1;i<ActivityPurpose.ACTIVITY_PURPOSE.length;i++){
            int purpose = ActivityPurpose.getActivityPurposeValue(ActivityPurpose.ACTIVITY_PURPOSE[i]);
        	stopdestinationparameters = 
                (StopDestinationParameters) sdpd.stopDestinationParameters[purpose];
            
            sizeTerm =  stopdestinationparameters.retail * retail
                        + stopdestinationparameters.nonRetail * nonRetail 
                        + stopdestinationparameters.gradeSchool * gradeSchool
                        + stopdestinationparameters.hhs * households;
            
            stopSizeTerm[purpose] = sizeTerm;
            if(sizeTerm>0)
                stopLnSizeTerm[purpose] = MathUtil.log(sizeTerm);
        }
        stopSizeTermsSet = true;
    }
    
    
    public void calcTourDestinationUtility(int activityPurpose, int subPurpose,
            TourDestinationParameters tdp, double logsum){
         
         utility=-999;
         if(tourSizeTerm[activityPurpose][subPurpose-1]>0 && acres>0){
                utility= tdp.logsum*(logsum-10.0) + lnAcres + tourLnSizeTerm[activityPurpose][subPurpose-1];
                isAvailable=true;
         }
         else {
                isAvailable=false;
         }
     }
    
     public void calcStopDestinationUtility(int actPurpose, 
                                       StopDestinationParameters stopdestinationparameters, 
                                       Mode mode, 
                                       float autoTime,
                                       float walkTime, 
                                       float bikeTime, 
                                       float transitGeneralizedCost,
                                       float[] autoDists,
                                       int stopNumber)
                                       {
        utility=-999;
        isAvailable = false;

        //int actPurposeValue = ActivityPurpose.getActivityPurposeValue(actPurpose);
        if(mode.type == ModeType.WALK && walkTime > 120)
            return;     //if you are walking and this zone is more than 2 hours away, it is unavailable
        if(mode.type == ModeType.BIKE && bikeTime > 120)
            return;     //if you are biking and this zone is more than 2 hours away, it is unavailable
         if((mode.type == ModeType.AUTODRIVER || mode.type == ModeType.AUTOPASSENGER) && autoDists[0] > 2*autoDists[1])
            return;  //if you are driving and this zone takes you more than twice the distance you would have
                     //traveled if you didn't stop, it is unavailable.
        boolean flag = false;
        if(mode.type == ModeType.WALKTRANSIT || mode.type == ModeType.DRIVETRANSIT)
            flag = true;
        if(mode.type == ModeType.TRANSITPASSENGER && stopNumber == 1)
            flag = true;
        if(mode.type == ModeType.PASSENGERTRANSIT && stopNumber == 2)
            flag = true;
        if(flag && transitGeneralizedCost == 0.0 && walkTime > 120)
            return;
            
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
            isAvailable = true;
        }
    }

     public double getUtility(){
 
          return utility;
     };

    //This method will be called from the household workers to check and see if the
    //Taz has been updated and what the size terms and the acres have been set to.
    public void summarizeTAZInfo(){
        if(collapsedEmployment == true){
            if(tourSizeTermsSet == true){
                if(stopSizeTermsSet == true){
                    logger.info("");
                    logger.info("TAZ Data has been set, here is a summary: ");
                    logger.info("TAZ #: " + zoneNumber);
                    logger.info("TAZ acres: " + acres);
                    logger.info("TAZ tour size terms");
                    for(int i=1;i<ActivityPurpose.ACTIVITY_PURPOSE.length;i++){
                        int purpose = ActivityPurpose.getActivityPurposeValue(ActivityPurpose.ACTIVITY_PURPOSE[i]);
                        String output = new String();
                        output = ActivityPurpose.ACTIVITY_PURPOSE[i] + ": \t";
                        for(int segmentMinusOne=0;segmentMinusOne<ActivityPurpose.getDCSegments(purpose);segmentMinusOne++){
                            output += tourSizeTerm[purpose][segmentMinusOne] + "\t";
                        }
                        logger.info("\t" + output);
                    }
                }else logger.severe("Stop Size Terms have not been set - this problem must be corrected before a summary can be " +
                                    "produced");
            }else logger.severe("Tour Size Terms have not been set - this problem must be corrected before a summary can be " +
                                "produced");
        }else logger.severe("Employment has not been collapsed - this problem must be corrected before a summary can be " +
                            "produced");
        logger.info("");
        return;
    }



     public void print(PrintWriter p){

          p.print(
                " "+ zoneNumber
               +" "+ households       
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

          logger.info(
                "\nzoneNumber: "+ zoneNumber
               +"\nhouseholds: "+ households
               +"\nworkParkingCost: "+ workParkingCost
               +"\nnonWorkParkingCost: "+ nonWorkParkingCost
               +"\nacres: "+ acres
               +"\npricePerAcre: "+ pricePerAcre
               +"\npricePerSqFtSFD: "+ pricePerSqFtSFD
               +"\nsingleFamilyHH: "+ singleFamilyHH
               +"\nmultiFamilyHH: "+ multiFamilyHH    
               +"\ntoursizeTerms: ");
               for(int i=0;i<tourSizeTerm.length;++i)
                    for(int j=0;j<tourSizeTerm[i].length;++j)
                        logger.info("\n ["+i+","+j+"] : "+tourSizeTerm[i][j]+" ln : "+tourLnSizeTerm[i][j]);
          logger.info("\nstopsizeTerms: ");
               for(int i=0;i<stopSizeTerm.length;++i)
                  logger.info("\n "+i+" : "+stopSizeTerm[i]+" ln : "+stopLnSizeTerm[i]);
     }
     
     public static void main(String[] args){
         ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
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
        tazs.readData(rb,"tazData.file");
        tazs.collapseEmployment(PTModelInputs.tdpd, PTModelInputs.sdpd);
     }
}   
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    


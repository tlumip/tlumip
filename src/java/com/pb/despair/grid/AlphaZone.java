package com.pb.despair.grid;

import org.apache.log4j.Logger;
import java.util.ArrayList;

/**
 * Keeps track of residential SQFT and the typFARs for
 * each developement type within each Land Use Intensity and calculates demand
 * in grid cells.
 *
 * @author Christi Willison
 * @version Oct 29, 2003
 * Created by IntelliJ IDEA.
 */
public class AlphaZone {
    private Logger log = Logger.getLogger("com.pb.despair.grid");
    private static String[] countyNameIndex = new String[Ref.NUM_ALPHA_ZONES];
    private int number;
    private String landUseIntensity;
    float[] maxFARs=new float[2]; //corresponds to the LUI, will be initialzed by setFARs method called by constructor
    double[] typFARs=new double[17];// corresponds to the LUI, will be initialized by setFARs method called by constructor
    double[] factors=new double[2];//result of Stage 2a (will not be less than .7);
    double[] bldgSQFTDemand = new double[Ref.RDEVTYPES.length+Ref.NRDEVTYPES.length];
    double[] bldgSQFTPerCell = new double[Ref.RDEVTYPES.length+Ref.NRDEVTYPES.length];
    int[] cellsRequired = new int[Ref.RDEVTYPES.length+Ref.NRDEVTYPES.length];
    double[] cellsDiff = new double[Ref.RDEVTYPES.length+Ref.NRDEVTYPES.length]; //will be set in the calculateCellsRequired method
    double[] totalDifference;//  totalDifference[0] = totalRDiff, totalDifference[1] = totalNRDiff
    //RR variable to keep track of
    double[] RRbldgSQFTDemand = new double[2]; //RRbldgSQFTDemand[0]=RRSFD, RRbldgSQFTDemand[1]=RRMH
    double RRMHonFAVSQFTPerCell=0.0;  //will be assigned a value in Stage2a if we borrow any FAV to satisfy demand.
    double RRSFDonFAVSQFTPerCell=0.0;  //will be assigned a value in Stage2a if we borrow any FAV to satisfy demand.

    AlphaZone(int az,String countyName,String luc, double[] SQFTdemand){
        this.number = az;
        this.countyNameIndex[az]=countyName;
        this.landUseIntensity = luc;
        this.bldgSQFTDemand = SQFTdemand;
        setFARs();
        calculateCellsRequired(); //this converts bldgSQFTdemand to cellsRequired using the typFARs
        calculateBldgSQFTPerCell();
    }

     private void setFARs(){
        for(int i=0;i<typFARs.length;i++) this.typFARs[i] = Ref.TYPFAR[Ref.getSubscript(landUseIntensity,Ref.INTENSITIES)][i];
        for(int i=0;i<maxFARs.length;i++) this.maxFARs[i]=Ref.MAXFAR[Ref.getSubscript(landUseIntensity,Ref.INTENSITIES)][i];
        return;
     }

    public void calculateCellsRequired(){
        totalDifference=new double[2];
        for(int i=0;i<cellsRequired.length;i++){
            double cellsDouble = (bldgSQFTDemand[i]/typFARs[i])/(double)Ref.SQFT_PER_GRIDCELL;//(bldg. sqft/(bldg. sqft/land sqft)/( land sqft/gridcell)=#gridCells (nr)
            int cellsInt = (int) Math.ceil(cellsDouble);
            cellsRequired[i] = cellsInt;
            cellsDiff[i] = (double)cellsInt-cellsDouble;
        }
        calculateTotalDifference();
        return;
    }

    private void calculateTotalDifference(){
        for(int i=0;i<Ref.RDEVTYPES.length;i++) totalDifference[0]+=cellsDiff[i];
        for(int i=0;i<Ref.NRDEVTYPES.length;i++) totalDifference[1]+=cellsDiff[i+Ref.RDEVTYPES.length];
    }

    public int getMaxCellsRequired(String glc){ //used in Stage1 comparison of supply and demand
        int sum=0;
        if(glc=="R") {
            for(int i=0;i<Ref.RDEVTYPES.length;i++)
                sum+=(int)Math.ceil(bldgSQFTDemand[i]/maxFARs[0]/Ref.SQFT_PER_GRIDCELL);
        }
        if(glc=="NR") {
            for(int i=0;i<Ref.NRDEVTYPES.length;i++){
                sum+=(int)Math.ceil(bldgSQFTDemand[i+Ref.RDEVTYPES.length]/maxFARs[1]/Ref.SQFT_PER_GRIDCELL);
            }
        }
        return sum;
    }

    public int getCellsRequired(String devType_glc){ //called in Stage2a when we look more closely at supply and demand
        int sum=0;
        if(devType_glc.equals("R")){ //total R cells required, need to add the R devType elements
            for(int i=0;i<Ref.RDEVTYPES.length;i++){
                sum+=cellsRequired[i];
            }
        }else if(devType_glc.equals("NR")){ // total NR cells required, need to add the NR devType elements
            for(int i=0;i<Ref.NRDEVTYPES.length;i++){
                sum+=cellsRequired[i+Ref.RDEVTYPES.length];
            }
        }else sum=cellsRequired[Ref.getSubscript(devType_glc,Ref.DEVTYPES)]; //only need the cells required for devType

        return sum;
    }

    public void updateTypFARs(double[] factor,int newRSupply,int newNRSupply){//called in Stage2a after we have balanced the R/NR demand and supply
        //We want to multiply the FARs by a number no smaller than .7 so
        //if the factor < .7, set factor = .7
        factor[0]= Math.max(factor[0],.70);
        factor[1] = Math.max(factor[1],.70);
        this.factors[0]=factor[0];
        for(int i=0;i<Ref.RDEVTYPES.length;i++)
            typFARs[i]*=factor[0];
//            typFARs[i]*=(factor[0]-(totalDifference[0]/newRSupply));


        this.factors[1]=factor[1];
        for(int i=Ref.RDEVTYPES.length;i<typFARs.length;i++)
            typFARs[i]*=factor[1];
//           typFARs[i]*=(factor[1]-(totalDifference[1]/newNRSupply));
        return;
    }

    public void updateCellsRequired(int newRSupply, int newNRSupply){ //called in Stage2b after the typFARs are updated.
        //first update the R cells required.  In all cases except where the factor was set to .7, the new R demand should equal the
        //new R supply.  If the factor is .7 then the new R demand is equal to Rdemand/.7
        //before moving on the the NR case we need to be sure that we didn't bucket round a cell to 0 that has SQFT to satisfy
        int rDemand = getCellsRequired("R");
        if(factors[0]==.7) newRSupply=(int)((rDemand/0.7)+.5);
        double remainder=0.0;
        for(int i=0;i<Ref.RDEVTYPES.length;i++){ //calculate the # cells required for the R case using bucket rounding.
            if(i==0){
                double numerator = (double)newRSupply*getCellsRequired(Ref.RDEVTYPES[i]);
                double cellsDouble = numerator/rDemand;
                int cellsInt = (int) Math.ceil(cellsDouble);
                cellsRequired[i] = cellsInt;
                remainder = ((double)rDemand*cellsInt)-(numerator);
                cellsDiff[i]=cellsInt-cellsDouble;
            }else{
                double numerator = ((double)newRSupply*getCellsRequired(Ref.RDEVTYPES[i]))-remainder;
                double cellsDouble = numerator/rDemand;
                int cellsInt = (int) Math.ceil(cellsDouble);
                cellsRequired[i]=cellsInt;
                remainder=((double)rDemand*cellsInt)-numerator;
                cellsDiff[i]=cellsInt-cellsDouble;
            }
        }
        int offset = Ref.RDEVTYPES.length;
        int nrDemand = getCellsRequired("NR");
        if(factors[1] ==.7) newNRSupply=(int)((nrDemand/0.7)+.5);
        remainder=0.0;
        for(int i=0;i<Ref.NRDEVTYPES.length;i++){ //calculate the # cells required for the NR case using bucket rounding.
            if(i==0){
                double numerator = (double)newNRSupply*getCellsRequired(Ref.NRDEVTYPES[i]);
                double cellsDouble = numerator/nrDemand;
                int cellsInt = (int) Math.ceil(cellsDouble);
                cellsRequired[i+offset] = cellsInt;
                remainder = ((double)cellsInt*nrDemand)-numerator;
                cellsDiff[i+offset]=cellsInt-cellsDouble;
            }else{
                double numerator = ((double)newNRSupply*getCellsRequired(Ref.NRDEVTYPES[i]))-remainder;
                double cellsDouble = numerator/nrDemand;
                int cellsInt = (int) Math.ceil(cellsDouble);
                cellsRequired[i+offset]=cellsInt;
                remainder=((double)cellsInt*nrDemand)-numerator;
                cellsDiff[i+offset]=cellsInt-cellsDouble;
            }
        }
        checkForZero(cellsRequired);
        return;
    }

    private void checkForZero(int[] cellsRequired){
        //first look at the R cells
        ArrayList posOfRZeros=new ArrayList();
        for(int i=0;i<Ref.RDEVTYPES.length;i++){
            if(cellsRequired[i]==0 && bldgSQFTDemand[i]>0) posOfRZeros.add(new Integer(i));
       }
        int numOfRZeros=posOfRZeros.size(); //at most it will be 6
        if(numOfRZeros==0) {
//            log.info("No R bucket rounding problems in alphaZone "+getNumber());
            //go to the NR case
        }else{
            //else: we rounded cells down to zero that still have sqft to be satisfied so we need to borrow some from the
            //other cells that are not 0.
            //now we need to sort the number of cells demanded and if the first max isn't enough to
            //cover all the cells that are zero but should be 1 then we will need to use the next highest
            //cell count so that we can substract one from that.
            int[] indexes={0,1,2,3,4,5};
            int maxIndex;
            int maxPosition;
            int temp;
            //sort indexes so that we know which element is the biggest, next biggest, etc.
            for(int passCount=0;passCount<Ref.RDEVTYPES.length-1;passCount++){
                maxIndex=indexes[passCount];
                maxPosition=passCount;
                for(int placeCount=passCount+1;placeCount<Ref.RDEVTYPES.length;placeCount++){
                    if(cellsRequired[indexes[placeCount]]>cellsRequired[indexes[maxPosition]]){
                        maxIndex=indexes[placeCount];
                        maxPosition=placeCount;
                    }
                }
                temp=maxIndex;
                indexes[maxPosition]=indexes[passCount];
                indexes[passCount]=temp;
            }
            if(cellsRequired[indexes[0]]<2) {
                log.info("Major Res bucket rounding problems in alphaZone "+getNumber()+".  "+posOfRZeros.size()+" cells can not be fixed");
                //we have rounded all (or all but 1) cells out of existence, so there is nothing to borrow
                //go on to NR case
            }else{
                int position=0;
                while(numOfRZeros>0 && cellsRequired[indexes[position]]>1){
                    int cells = Math.min(numOfRZeros,cellsRequired[indexes[position]]-1);
                    numOfRZeros-=cells;
                    cellsRequired[indexes[position]]-=cells;
                    position++;
                }
                int cellsCorrected = posOfRZeros.size()-numOfRZeros;
                if(numOfRZeros != 0) log.info("Could only fix "+ cellsCorrected + "R cells");
                else log.info("Fixed the Res bucket rounding problem in alphaZone "+ getNumber());
                for(int i=0;i<posOfRZeros.size()-numOfRZeros;i++)
                    cellsRequired[((Integer)posOfRZeros.get(i)).intValue()]++;
            }
        }
        //now do the same thing for the NR case.
        int offset = Ref.RDEVTYPES.length;
        ArrayList posOfNRZeros =  new ArrayList();
        for(int i=0;i<Ref.NRDEVTYPES.length;i++){
            if(cellsRequired[i+offset]==0 && bldgSQFTDemand[i+offset]>0) posOfNRZeros.add(new Integer(i+offset));
        }
        int numOfNRZeros=posOfNRZeros.size(); //at most it will be 11
        if(numOfNRZeros==0) {
//            log.info("No NR bucket rounding problems in alphaZone "+getNumber());
            return; //don't need to go on because we didn't bucket round any cells out of existence.
        }
        //else: we rounded cells down to zero that still have sqft to be satisfied so we need to borrow some from the
        //other cells that are not 0.
        //now we need to sort the number of cells demanded and if the first max isn't enough to
        //cover all the cells that are zero but should be 1 then we will need to use the next highest
        //cell count so that we can substract one from that.
        int[] NRindexes={0,1,2,3,4,5,6,7,8,9,10};
        int maxIndex;
        int maxPosition;
        int temp;
        //sort indexes so that we know which element is the biggest, next biggest, etc.
        for(int passCount=0;passCount<Ref.NRDEVTYPES.length-1;passCount++){
            maxIndex=NRindexes[passCount];
            maxPosition=passCount;
            for(int placeCount=passCount+1;placeCount<Ref.NRDEVTYPES.length;placeCount++){
                if(cellsRequired[NRindexes[placeCount]+offset]>cellsRequired[NRindexes[maxPosition]+offset]){
                    maxIndex=NRindexes[placeCount];
                    maxPosition=placeCount;
                }
            }
            temp=maxIndex;
            NRindexes[maxPosition]=NRindexes[passCount];
            NRindexes[passCount]=temp;
        }
        if(cellsRequired[NRindexes[0]+offset]<2) {
            log.info("Major NonRes bucket rounding problems in alphaZone "+getNumber()+".  "+posOfNRZeros.size()+" cells can not be fixed");
            return; //we have rounded all (or all but 1) cells out of existence, so there is nothing to borrow
        }
        int position=0;
        while(numOfNRZeros>0 && cellsRequired[NRindexes[position]+offset]>1){
            int cells = Math.min(numOfNRZeros,cellsRequired[NRindexes[position]+offset]-1);
            numOfNRZeros-=cells;
            cellsRequired[NRindexes[position]+offset]-=cells;
            position++;
        }
        int cellsCorrected = posOfNRZeros.size()-numOfNRZeros;
        if(numOfNRZeros != 0) log.info("Could only fix "+ cellsCorrected + "NR cells");
        else log.info("Fixed the NRes bucket rounding problem in alphaZone "+ getNumber());
        for(int i=0;i<posOfNRZeros.size()-numOfNRZeros;i++)
            cellsRequired[((Integer)posOfNRZeros.get(i)).intValue()]++;

        return;

    }

    public int[] getCellsRequiredArray(String glc){//used in Stage3 when we need to choose a devType
        int[] cellsReq;
        int offset;
        if(glc=="R") {
            cellsReq=new int[Ref.RDEVTYPES.length];
            offset=0;
        }else {
            cellsReq=new int[Ref.NRDEVTYPES.length];
            offset=Ref.RDEVTYPES.length;

        }
        for(int i=0;i<cellsReq.length;i++)
                cellsReq[i]=cellsRequired[i+offset];
        return cellsReq;
    }

    public void decrementCellsRequired(String devType, int value){//called in Stage3 after devType has been chosen
        cellsRequired[Ref.getSubscript(devType,Ref.DEVTYPES)]-=value;
    }

    public void decrementBldgSQFT(String devType, double value){ //used by Stage 2a to remove any rural res. sqft from
                                                                //sqft array since it will be satisfied by FOR/AG/Vagfor cells
        bldgSQFTDemand[Ref.getSubscript(devType,Ref.DEVTYPES)]-=value;
     }

    public void decrementRRBldgSQFT(String devType,double value){
        if(devType.equals("RRSFD")) RRbldgSQFTDemand[0]-=value;
        else RRbldgSQFTDemand[1]-=value;
        return;
    }

//Getters for the various AlphaZone attributes
    public float getMaxFAR(String glc){
        float FAR = Float.NaN;
        if(glc=="R") return maxFARs[0];
        if(glc=="NR") return maxFARs[1];
        return FAR;
    }

    public double getDemandInBldgSQFT(String devType_glc){
        double sum=0.0;
        if(devType_glc.equals("R")){ //total R demand in SQFT, need to add the R devType elements
            for(int i=0;i<Ref.RDEVTYPES.length;i++){
                sum+=bldgSQFTDemand[i];
            }
        }else if(devType_glc.equals("NR")){ // total NR demand in SQFT, need to add the NR devType elements
            for(int i=0;i<Ref.NRDEVTYPES.length;i++){
                sum+=bldgSQFTDemand[i+Ref.RDEVTYPES.length];
            }
        }else sum=bldgSQFTDemand[Ref.getSubscript(devType_glc,Ref.DEVTYPES)]; //only need the demand for devType
        return sum;
    }

    public void calculateBldgSQFTPerCell(){
        for(int i=0;i<bldgSQFTDemand.length;i++){
            if(getCellsRequired(Ref.DEVTYPES[i]) <= 0) bldgSQFTPerCell[i]=0; //don't do a calculation if there are no cells required for a particular devType
            else bldgSQFTPerCell[i]=Math.ceil(bldgSQFTDemand[i]/getCellsRequired(Ref.DEVTYPES[i])); //round up
        }
    }

    public double getRRMHonFAVSQFTPerCell() {
        return RRMHonFAVSQFTPerCell;
    }

    public double getRRSFDonFAVSQFTPerCell() {
        return RRSFDonFAVSQFTPerCell;
    }

    public void setRRMHonFAVSQFTPerCell(double RRMHonFAVSQFTPerCell) {
        this.RRMHonFAVSQFTPerCell = RRMHonFAVSQFTPerCell;
    }

    public void setRRSFDonFAVSQFTPerCell(double RRSFDonFAVSQFTPerCell) {
        this.RRSFDonFAVSQFTPerCell = RRSFDonFAVSQFTPerCell;
    }

    public double getBldgSQFTPerCell(int index){
        return bldgSQFTPerCell[index];
    }

    public double[] getTypFARs() {
        return typFARs;
    }

    public double getTypFAR(int position){
        return typFARs[position];
    }

    public double[] getFactors(){
        return factors;
    }

    public String getLandUseIntensity() {
        return landUseIntensity;
    }

    public int getNumber() {
        return number;
    }

    public static String getCountyName(int zoneNumber) {
        return countyNameIndex[zoneNumber];
    }

    public double getCellsDiff(int index) {
        return cellsDiff[index];
    }

    public String toString () {
        return "az="+number+" landUseIntensity="+landUseIntensity+"\n";
    }

    public static void main(String[] args) {
         int[] cellsRequired={1,5,0,0,6,2,10,11,12,13,14,15,16,17,18,19,20};
//        checkForZero(cellsRequired);

//        System.out.println(System.getProperty("user.dir"));
////        int result=0;
////        result=(50004*118963)-52572;
////        System.out.println("Result: "+ result);
////        float[] SQFTdemand= {16745f,20492f,14885f,98162f,1567f,35372f,187223f,0f,3530f,
////                          0f,25940f,0f,530f,580f,0f,314120f,169560f,15060f,529320f};
//        float[] SQFTdemand= {3071f,61866.90009f,10729f,152178f,4541f,36811.5f,0f,0f,0f,
//                          0f,0f,0f,0f,0f,0f,0f,0f,0f,0f};
//
//        AlphaZone az = new AlphaZone(159,"Washington","Medium",SQFTdemand);
//        //print out az data
//        System.out.println("AlphaZone:"+az.number+", LUI:"+az.landUseIntensity);
//        System.out.println("R demand "+ az.getDemandInBldgSQFT("R"));
//        System.out.println("NR demand " + az.getDemandInBldgSQFT("NR"));
//        System.out.print("FARs: ");
//        for(int i=0;i<az.typFARs.length;i++) System.out.print(az.typFARs[i]+", ");
//        System.out.println("\nR cells req: ");
//        for(int i=0;i<Ref.RDEVTYPES.length;i++) System.out.print(az.cellsRequired[i]+" ");
//        System.out.println("\nR cell diff: ");
//        for(int i=0;i<Ref.RDEVTYPES.length;i++) System.out.print(az.cellsDiff[i]+" ");
//        System.out.println("\nTotal R difference: "+ az.totalDifference[0]);
//        System.out.println("\nNR cells req: ");
//        for(int i=0;i<Ref.NRDEVTYPES.length;i++) System.out.print(az.cellsRequired[i+Ref.RDEVTYPES.length]+" ");
//
//
//        System.out.println("\nTotal R cells req "+ az.getCellsRequired("R"));
//        System.out.println("Total NR cells req " + az.getCellsRequired("NR"));
//
////        az.decrementCellsRequired("MF",1);
//////        az.decrementCellsRequired("HvyInd",1);
////        System.out.println("\nNew R cells req: ");
////        for(int i=0;i<Ref.RDEVTYPES.length;i++) System.out.print(az.cellsRequired[i]+", ");
////        System.out.println("\nNR cells req: ");
////        for(int i=0;i<Ref.NRDEVTYPES.length;i++) System.out.print(az.cellsRequired[i+Ref.RDEVTYPES.length]+", ");
////        System.out.println();
////
////        System.out.println("\nNew Total R cells req "+ az.getCellsRequired("R"));
////        System.out.println("New Total NR cells req " + az.getCellsRequired("NR"));
//
//        int Rcells_req = 0;
//        double[] factors = new double[2];
////
//        Rcells_req = az.getCellsRequired("R");
////
//        int cellsAvail = 87;
//        System.out.println("R cells Avail "+cellsAvail);
//        factors[0] = Rcells_req/(double)cellsAvail;
//        System.out.println("factor "+factors[0]);
//        az.updateTypFARs(factors,cellsAvail,0);
//        az.updateCellsRequired(cellsAvail,0);
//        System.out.print("New FARs: ");
//        for(int i=0;i<az.typFARs.length;i++) System.out.print(az.typFARs[i]+", ");
//        System.out.println("\nNew R cells req: ");
//        for(int i=0;i<Ref.RDEVTYPES.length;i++) System.out.print(az.cellsRequired[i]+", ");
//        System.out.println("\nTotal R cells req "+ az.getCellsRequired("R"));
////        System.out.println("\nNR cells req: ");
////        for(int i=0;i<Ref.NRDEVTYPES.length;i++) System.out.print(az.cellsRequired[i+Ref.RDEVTYPES.length]+", ");
////        System.out.println();
//
////          System.out.println("sqft = "+ (short)(az.getTypFAR(0)*Ref.SQFT_PER_GRIDCELL));


    }
}

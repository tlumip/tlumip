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
package com.pb.tlumip.grid;

import org.apache.log4j.Logger;

/**
 * Represents a gridcell to be synthesized.  Will have static methods
 * that will pick a devType, assign bldgSQFT, and pick a yr. built
 * @author  Christi Willison
 * @version Dec 17, 2003
 * Created by IntelliJ IDEA.
 */
public class GridCell {
    static Logger log = Logger.getLogger("com/pb/tlumip/grid");

    static void getAttributes(County currCounty, AlphaZone currAZ, Distribution3D d3d, int luc, String glc, float[] matchCoeffs,int[] result){
        float[] demandArray = new float[Ref.DEVTYPES.length];
        float[] weightedMatchCoeffs = new float[Ref.DEVTYPES.length];
        int az=currAZ.getNumber();
        String devType=null;
        short devTypeCode=0;
        int sqft = 0;

        if(glc.equals("Vdev")){//cell will either be an R type, an NR type or Undeveloped.
            float VdevForR = GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)];
            float VdevForNR = GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)];
            int RcellsReq=currAZ.getCellsRequired("R");
            int NRcellsReq=currAZ.getCellsRequired("NR");
            if((VdevForR*RcellsReq>0 || VdevForNR*NRcellsReq>0)){//Vdev was used for to satisfy existing R and/or NR demand
                int[] RcellsReqArray=currAZ.getCellsRequiredArray("R");
                int[] NRcellsReqArray=currAZ.getCellsRequiredArray("NR");
                //match coefficients will be weighted by number of cells required and the number borrowed
                for(int i=0;i<Ref.RDEVTYPES.length;i++) demandArray[i]=RcellsReqArray[i]*VdevForR;
                for(int i=0;i<Ref.NRDEVTYPES.length;i++) demandArray[i+Ref.RDEVTYPES.length]=NRcellsReqArray[i]*VdevForNR;
                for(int i=0;i<Ref.DEVTYPES.length;i++) weightedMatchCoeffs[i]=matchCoeffs[i]*demandArray[i];
                devType=chooseDevType(weightedMatchCoeffs);
                GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript(Ref.getDemandType(devType),Ref.DEMANDTOSATISFY)]-=1;
                currAZ.decrementCellsRequired(devType,1);
                devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                sqft=getBldgSQFT(currAZ,devTypeCode);
            }else {
                devTypeCode = GridSynthesizer.undevelopedDevTypes[luc];
                sqft=0;
            }
        }else if(glc.equals("VTp")){//right now we don't account for any VTp that AG/LOG might have borrowed (it didn't)
                float VTpForR = GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)];
                float VTpForNR = GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)];
                int RcellsReq=currAZ.getCellsRequired("R");
                int NRcellsReq=currAZ.getCellsRequired("NR");
                if((VTpForR*RcellsReq>0 || VTpForNR*NRcellsReq>0)){
                    int[] RcellsReqArray=currAZ.getCellsRequiredArray("R");
                    int[] NRcellsReqArray=currAZ.getCellsRequiredArray("NR");
                    //match coefficients will be weighted by number of cells required and the number borrowed
                    for(int i=0;i<Ref.RDEVTYPES.length;i++) demandArray[i]=RcellsReqArray[i]*VTpForR;
                    for(int i=0;i<Ref.NRDEVTYPES.length;i++) demandArray[i+Ref.RDEVTYPES.length]=NRcellsReqArray[i]*VTpForNR;
                    for(int i=0;i<Ref.DEVTYPES.length;i++) weightedMatchCoeffs[i]=matchCoeffs[i]*demandArray[i];
                    devType=chooseDevType(weightedMatchCoeffs);
                    GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript(Ref.getDemandType(devType),Ref.DEMANDTOSATISFY)]-=1;
                    currAZ.decrementCellsRequired(devType,1);
                    devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                    sqft=getBldgSQFT(currAZ,devTypeCode);
                 }else {
                    devTypeCode = GridSynthesizer.undevelopedDevTypes[luc];
                    sqft=0;
                }
        }else if(glc.equals("R")){
                if(currAZ.getCellsRequired("R")>0){//true if there are R demands that haven't been met.
                    int[] RcellsReqArray=currAZ.getCellsRequiredArray("R");
                    //match coefficients will be weighted by number of cells required
                    for(int i=0;i<Ref.RDEVTYPES.length;i++) demandArray[i]=RcellsReqArray[i];
                    for(int i=0;i<Ref.DEVTYPES.length;i++) weightedMatchCoeffs[i]=matchCoeffs[i]*demandArray[i];
                    devType=chooseDevType(weightedMatchCoeffs);
                    currAZ.decrementCellsRequired(devType,1);
                    devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                    sqft=getBldgSQFT(currAZ,devTypeCode);
                }else if(currAZ.getCellsRequired("NR")>0 &&
                        GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("R",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]>0){
                //this is true if we had to borrow R cells to satisfy NR demand and there is no more R demand.
                        int[] NRcellsReqArray=currAZ.getCellsRequiredArray("NR");
                        //match coefficients will be weighted by number of cells required
                        for(int i=0;i<Ref.NRDEVTYPES.length;i++) demandArray[i+Ref.RDEVTYPES.length]=NRcellsReqArray[i];
                        for(int i=0;i<Ref.DEVTYPES.length;i++) weightedMatchCoeffs[i]=matchCoeffs[i]*demandArray[i];
                        devType=chooseDevType(weightedMatchCoeffs);
                        GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("R",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]-=1;
                        currAZ.decrementCellsRequired(devType,1);
                        devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                        sqft=getBldgSQFT(currAZ,devTypeCode);
                }else {
                    devTypeCode = GridSynthesizer.undevelopedDevTypes[luc];//cell is not needed to satisfy demand
                    sqft=0;
                }
        }else if(glc.equals("NR")){
                if(currAZ.getCellsRequired("NR")>0){//true if there are NR demands that haven't been met.
                    int[] NRcellsReqArray=currAZ.getCellsRequiredArray("NR");
                    //match coefficients will be weighted by number of cells required
                    for(int i=0;i<Ref.NRDEVTYPES.length;i++) demandArray[i+Ref.RDEVTYPES.length]=NRcellsReqArray[i];
                    for(int i=0;i<Ref.DEVTYPES.length;i++) weightedMatchCoeffs[i]=matchCoeffs[i]*demandArray[i];
                    devType=chooseDevType(weightedMatchCoeffs);
                    currAZ.decrementCellsRequired(devType,1);
                    devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                    sqft=getBldgSQFT(currAZ,devTypeCode);
                }else if(currAZ.getCellsRequired("R")>0 &&
                    GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("NR",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]>0){
                        //this is true if we had to borrow NR cells to satisfy R demand and there is no more NR demand.
                        int[] RcellsReq=currAZ.getCellsRequiredArray("R");
                        //match coefficients will be weighted by number of cells required
                        for(int i=0;i<Ref.RDEVTYPES.length;i++) demandArray[i]=RcellsReq[i];
                        for(int i=0;i<Ref.DEVTYPES.length;i++) weightedMatchCoeffs[i]=matchCoeffs[i]*demandArray[i];
                        devType=chooseDevType(weightedMatchCoeffs);
                        GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("NR",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]-=1;
                        currAZ.decrementCellsRequired(devType,1);
                        devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                        sqft=getBldgSQFT(currAZ,devTypeCode);
                }else {
                    devTypeCode = GridSynthesizer.undevelopedDevTypes[luc];//cell is not needed to satisfy demand
                    sqft=0;
                }
        }else if(glc.equals("FOR")){
                if(currCounty.getCellsDemanded("LOG")>0){
                    int[] FORcellsDemanded=currCounty.getCellsDemandedArray();
                    //match coefficients will be weighted by FOR demand only (elements 0-3)
                    int offset = Ref.RDEVTYPES.length+Ref.NRDEVTYPES.length;
                    for(int i=0;i<Ref.FORAGDEVTYPES.length-1;i++) demandArray[i+offset]=FORcellsDemanded[i];
                    for(int i=0;i<Ref.DEVTYPES.length;i++) weightedMatchCoeffs[i]=matchCoeffs[i]*demandArray[i];
                    devType=chooseDevType(weightedMatchCoeffs);
                    currCounty.decrementCellsDemanded(devType,1);
                    devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                    sqft=(int)(Ref.SQFT_PER_GRIDCELL+0.5f);
//                }else if(currCounty.FORBorrowedForAG>0 && currCounty.getCellsDemanded("AG")>0) { //as of 2/5/04 AG does not borrow
//                    devType="AG";                                                                //from FOR(Federal Forrest)
//                    currCounty.FORBorrowedForAG-=1;
//                    currCounty.decrementCellsDemanded(devType,1);
//                    devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
//                    sqft=(int)(Ref.SQFT_PER_GRIDCELL+.5);
                }else {
                    devTypeCode = GridSynthesizer.undevelopedDevTypes[luc];
                    sqft=0;
                }
        }else if(glc.equals("FOROther")){
                int FOROtherforRRSFD=GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("FOROther",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)];
                int FOROtherforRRMH=GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("FOROther",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)];
                if(FOROtherforRRSFD+FOROtherforRRMH>0 ){//we borrowed some FOROther to satisfy RR demand
                    if(FOROtherforRRSFD>=FOROtherforRRMH) {
                        devType="RRSFD";
                        GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("FOROther",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)]-=1;
                        sqft=(int)Math.min(currAZ.getRRSFDonFAVSQFTPerCell(),currAZ.RRbldgSQFTDemand[0]);
                        currAZ.decrementRRBldgSQFT(devType,sqft);
                    }
                    else {
                        devType="RRMH";
                        GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("FOROther",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)]-=1;
                        sqft=(int)Math.min(currAZ.getRRMHonFAVSQFTPerCell(),currAZ.RRbldgSQFTDemand[1]);
                        currAZ.decrementRRBldgSQFT(devType,sqft);
                    }
                    devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                }else if(currCounty.getCellsDemanded("LOG")>0 && currCounty.getFOROtherUsedForLog()>0){//still have LOG demand to satisfy
                    int[] FORcellsDemanded=currCounty.getCellsDemandedArray();
                    //match coefficients will be weighted by FOR demand only (elements 0-3)
                    int offset = Ref.RDEVTYPES.length+Ref.NRDEVTYPES.length;
                    for(int i=0;i<Ref.FORAGDEVTYPES.length-1;i++) demandArray[i+offset]=FORcellsDemanded[i];
                    for(int i=0;i<Ref.DEVTYPES.length;i++) weightedMatchCoeffs[i]=matchCoeffs[i]*demandArray[i];
                    devType=chooseDevType(weightedMatchCoeffs);
                    currCounty.decrementCellsDemanded(devType,1);
                    currCounty.FOROtherUsedForLog--;
                    devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                    sqft=(int)(Ref.SQFT_PER_GRIDCELL+0.5f);
                }else if(currCounty.FOROtherBorrowedForAG>0 && currCounty.getCellsDemanded("AG")>0){//all LOG demand has been satisfied
                    devType="AG";
                    currCounty.FOROtherBorrowedForAG-=1;
                    currCounty.decrementCellsDemanded(devType,1);
                    devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                    sqft=(int)(Ref.SQFT_PER_GRIDCELL+0.5f);
                }else {
                    devTypeCode = GridSynthesizer.undevelopedDevTypes[luc];
                    sqft=0;
                }
        }else if(glc.equals("Vagfor")){
                int VagforforRRSFD=GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("Vagfor",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)];
                int VagforforRRMH=GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("Vagfor",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)];
                float Vagfor_LOG = currCounty.VagforBorrowedForLOG;
                float Vagfor_AG = currCounty.VagforBorrowedForAG;
                if(VagforforRRSFD+VagforforRRMH>0){//we borrowed some Vagfor to satisfy RR demand
                    if(VagforforRRSFD>=VagforforRRMH) {
                        devType="RRSFD";
                        GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("Vagfor",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)]-=1;
                        sqft=(int)Math.min(currAZ.getRRSFDonFAVSQFTPerCell(),currAZ.RRbldgSQFTDemand[0]);
                        currAZ.decrementRRBldgSQFT(devType,sqft);
                    }
                    else {
                        devType="RRMH";
                        GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("Vagfor",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)]-=1;
                        sqft=(int)Math.min(currAZ.getRRMHonFAVSQFTPerCell(),currAZ.RRbldgSQFTDemand[1]);
                        currAZ.decrementRRBldgSQFT(devType,sqft);
                    }
                    devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                }else if((currCounty.getCellsDemanded("LOG")*Vagfor_LOG>0 || currCounty.getCellsDemanded("AG")*Vagfor_AG>0)){
                //we borrowed Vagfor to satisfy LOG and/or AG demand
                        int[] FORAGcellsDemanded = currCounty.getCellsDemandedArray();
                        //match coefficients will be weighted by FOR/AG demand and by the amount borrowed
                        int offset = Ref.RDEVTYPES.length+Ref.NRDEVTYPES.length;
                        for(int i=0;i<Ref.FORAGDEVTYPES.length-1;i++) demandArray[i+offset]=FORAGcellsDemanded[i]*Vagfor_LOG;
                        demandArray[demandArray.length-1]=FORAGcellsDemanded[FORAGcellsDemanded.length-1]*Vagfor_AG;
                        for(int i=0;i<Ref.DEVTYPES.length;i++) weightedMatchCoeffs[i]=matchCoeffs[i]*demandArray[i];
                        devType=chooseDevType(weightedMatchCoeffs);
                        if(devType.equals("AG"))currCounty.VagforBorrowedForAG-=1;
                        else currCounty.VagforBorrowedForLOG-=1;
                        currCounty.decrementCellsDemanded(devType,1);
                        devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                        sqft=(int)(Ref.SQFT_PER_GRIDCELL+0.5f);
                }else {
                    devTypeCode = GridSynthesizer.undevelopedDevTypes[luc];
                    sqft=0;
                }
        }else if(glc.equals("AG")){
                int AGforRRSFD=GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("AG",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)];
                int AGforRRMH=GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("AG",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)];
                if(AGforRRSFD+AGforRRMH>0){//we borrowed some AG to satisfy RR demand
                    if(AGforRRSFD>=AGforRRMH) {
                        devType="RRSFD";
                        GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("AG",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)]-=1;
                        sqft=(int)Math.min(currAZ.getRRSFDonFAVSQFTPerCell(),currAZ.RRbldgSQFTDemand[0]);
                        currAZ.decrementRRBldgSQFT(devType,sqft);
                    }
                    else {
                        devType="RRMH";
                        GridSynthesizer.numCellsBorrowed[az][Ref.getSubscript("AG",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)]-=1;
                        sqft=(int)Math.min(currAZ.getRRMHonFAVSQFTPerCell(),currAZ.RRbldgSQFTDemand[1]);
                        currAZ.decrementRRBldgSQFT(devType,sqft);
                    }
                    devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                }else if(currCounty.getCellsDemanded("AG")>0){
                    devType="AG";
                    currCounty.decrementCellsDemanded(devType,1);
                    devTypeCode=Ref.DEVTYPECODES[Ref.getSubscript(devType,Ref.DEVTYPES)];
                    sqft=(int)(Ref.SQFT_PER_GRIDCELL+0.5f);
                }else {
                    devTypeCode = GridSynthesizer.undevelopedDevTypes[luc];
                    sqft=0;
                }
         }
        result[0]=devTypeCode;
        result[1]=sqft;
        result[2]=getYrBuilt(devTypeCode,d3d,currAZ);
        return;
     }

    private static String chooseDevType(float[] weightedCoeffs){
        float max=0.0f;
        int posOfMax=-1;
        for(int i=0;i<weightedCoeffs.length;i++){
            if(max<weightedCoeffs[i]){
                max=weightedCoeffs[i];
                posOfMax=i;
            }
        }
        return Ref.DEVTYPES[posOfMax];
    }

    private static int getBldgSQFT(AlphaZone currAZ, short devTypeCode){
        if(devTypeCode>10 && devTypeCode<32){//implies that the devTypeCode is an R or NR type
            int index = Ref.getPosition(devTypeCode,Ref.DEVTYPECODES);
            double SQFTCalc = currAZ.getBldgSQFTPerCell(index); //already rounded up to int
            double SQFTToBeAssigned = currAZ.getDemandInBldgSQFT(Ref.getDevType(devTypeCode)); //rounded to nearest int
            double choice = Math.min(SQFTCalc,SQFTToBeAssigned);
            currAZ.decrementBldgSQFT(Ref.getDevType(devTypeCode),choice);
            return (int)choice;
        }
        //else the devTypeCode is an AG/FOR type so the sqft = SQFT_PER_GRIDCELL
        if(devTypeCode==33 || devTypeCode==32) return (int)(Ref.SQFT_PER_GRIDCELL+0.5f);
        //else the devTypeCode is Undeveloped so return 0
        return 0;
    }


    public static short getYrBuilt(short devTypeCode, Distribution3D d3d, AlphaZone currAZ){
        int index=Ref.getPosition(devTypeCode,Ref.DEVTYPECODES); //returns -1 if code is not one of the developed codes
        if(index==-1) return -1; //the code is an undeveloped or undevelopable code
        if(index<Ref.RDEVTYPES.length+Ref.NRDEVTYPES.length){//implies that the devTypeCode is an R or NR type
            int year =(int)Math.rint(d3d.drawSample(currAZ.getNumber(),Math.random()));
            if(year<1900 || year>2000) year=(short)1970;
            return (short)year;
        }
        //else if it is AG or FOR return 1859, the year Oregon became a state
        return (short)1859;
    }


    public static boolean isAllZeroes(float[] array){
        boolean zeroes=false;
        float sum=0.0f;
        for(int i=0;i<array.length;i++)
            sum+=array[i];
        if(sum==0) zeroes=true;
        return zeroes;
    }

}

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

import java.util.ArrayList;

/** Stores county attributes used in Grid Synthesis
 *
 *
 * @author Christi Willison
 * @version Oct 29, 2003
 * Created by IntelliJ IDEA.
 */
public class County {

    String name;
    String stateCode;

    double[] ForAg_acreDemand; // read in from S1LOG+AG.csv in GridSynthesizer constructor - length 5
    int[] ForAgcellsDemanded; // calculated from the acreDemand , initialized in County constuctor - length 5


    int AG_supply=0,
        FOR_supply=0,
        FOROther_supply=0,
        Vagfor_supply=0,
        VTp_supply=0;  //county supply which is given at the alpha zone level, summed up in Stage 1

    int VagforBorrowedForAG=0,
        VagforBorrowedForLOG=0,
//        VTpBorrowedForAG=0,     //we don't actually do this so I am commenting it out (2/5/04)
//        VTpBorrowedForLOG=0,
        FOROtherBorrowedForAG=0; //cells borrowed to satisfy demand, determined in Stage 1

    int AGUsedForAg=0,   //need this is Stage 3 to see if I can put RR on AG land
        FORUsedForLog=0, //am keeping track of this so I can output it in "CheckSupplyandDemand.java"
        FOROtherUsedForLog=0,
        AGExcess=0,
        VagforExcess=0,
        FOROtherExcess=0,
        FORExcess=0;

    ArrayList azones = new ArrayList(); //holds the county's alphazones

    //Constructor to use
    County(String name, String stateCode, double[] demandInAcres){
        this.name = name;
        this.stateCode = stateCode;
        this.ForAg_acreDemand = demandInAcres;
        calculateCellsDemanded();//will convert acres to sqft and then sqft to gridcells
    }

    private void calculateCellsDemanded(){ //used by constructor to initialize the cellsRequired array.
        ForAgcellsDemanded = new int[Ref.FORAGDEVTYPES.length];//elements 0,1,2,3 are FOR demand and 4 is AG demand
        for(int i=0;i<ForAgcellsDemanded.length;i++)
            ForAgcellsDemanded[i] = (int)Math.ceil((ForAg_acreDemand[i]*Ref.SQFT_PER_ACRE)/(Ref.SQFT_PER_GRIDCELL+0.5f));
    }

    public int getCellsDemanded(String type){
        int sum=0;
        if(type.equals("LOG")){
            for(int i=0;i<ForAgcellsDemanded.length-1;i++)//add up all FOR elements, but not the AG.
                sum+=ForAgcellsDemanded[i];
        }else sum=ForAgcellsDemanded[ForAgcellsDemanded.length-1]; //last element of the array
        return sum;
    }

    public int[] getCellsDemandedArray (){
        return ForAgcellsDemanded;
    }

    public void decrementCellsDemanded(String type,int value){
        ForAgcellsDemanded[Ref.getSubscript(type,Ref.FORAGDEVTYPES)]-=value;
        return;
    }

    public String getName() {
        return name;
    }

    public int getVagforBorrowedForAG() {
        return VagforBorrowedForAG;
    }

    public int getVagforBorrowedForLOG() {
        return VagforBorrowedForLOG;
    }

//    public int getVTpBorrowedForAG() {
//        return VTpBorrowedForAG;
//    }
//
//    public int getVTpBorrowedForLOG() {
//        return VTpBorrowedForLOG;
//    }

    public int getAGUsedForAg() {
        return AGUsedForAg;
    }

    public int getFORUsedForLog() {
        return FORUsedForLog;
    }

    public int getFOROtherUsedForLog() {
        return FOROtherUsedForLog;
    }

    public int getFOROtherBorrowedForAG() {
        return FOROtherBorrowedForAG;
    }

    public int getVagforExcess() {
        return VagforExcess;
    }

    public int getFOROtherExcess() {
        return FOROtherExcess;
    }

    public int getAGExcess() {
        return AGExcess;
    }

    public ArrayList getAzones() {
        return azones;
    }

    public String toString () {
        return "county="+name+" LOG_demand="+getCellsDemanded("LOG")+" AG_demand="+getCellsDemanded("AG")+"\n"+azones;
    }

    public static void main(String[] args) {
        double[] cellsDemanded={0,16.4,24.6,41.1,770922};
        County madeUpCounty= new County("Christi","ABC",cellsDemanded);
        String AgForDmd="";
        for(int i=0;i<madeUpCounty.getCellsDemandedArray().length;i++) AgForDmd+=madeUpCounty.getCellsDemandedArray()[i]+"\t";
        int totalLog=madeUpCounty.getCellsDemanded("LOG");

        System.out.println(AgForDmd);
        System.out.println(totalLog);
    }
}

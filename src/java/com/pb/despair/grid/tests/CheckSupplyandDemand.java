package com.pb.despair.grid.tests;

import com.pb.despair.grid.County;
import com.pb.despair.grid.GridSynthesizer;
import com.pb.despair.grid.AlphaZone;
import com.pb.despair.grid.Ref;
import com.pb.common.util.OutTextFile;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;

/**
 * 
 * 
 * @author  Christi Willison
 * @version Dec 18, 2003
 * Created by IntelliJ IDEA.
 */
public class CheckSupplyandDemand {

    private static Logger log = Logger.getLogger("com/pb/despair/grid/tests");


    public static void checkSupplyandDemand(){
        GridSynthesizer s12 = new GridSynthesizer();
        s12.runStage1(false);
        s12.runStage2a(false);
        s12.runStage2b(false);
        OutTextFile demandFile = new OutTextFile();
        try {
            demandFile.open("c:/temp/Demand_PostStage12a2b.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //gives Demand output file
        try {
            demandFile.println("\t\t\t\tResDmd\t\t\t\t\t\t\tRResDmdOnFAV\t\t\t\tNRDmd\t\t\t\t\t\t\t\t\t\t\t\tAgForDmd");
            demandFile.println("AZone\tCounty\tLUI\tRfactor\tRdMH\tRdMF\tRdAT\tRdSFD\tRdRRMH\tRdRRSFD\tRdTotal"+
                    "\tRRdRRMH\tRRdRRSFD\tRRdTotal\tNRfactor\tNRdAccom\tNRdDepot\tNRdGovSppt\tNRdGschool"+
                    "\tNRdHvyInd\tNRdHospital\tNRdInst\tNRdLtInd\tNRdOffice\tNRdRetail\tNRdWhse\tNRdTotal"+
                    "\tFORFed\tFORState\tFORInd\tFOROther\tLOGTotal\tAg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Iterator c = s12.countyMap.keySet().iterator();
        while (c.hasNext()) {
            County currCounty = (County)s12.countyMap.get(c.next());
            Iterator aZIter = currCounty.getAzones().iterator();
            while (aZIter.hasNext()) {
                // Create aZIter pointer to the current alpha zone
                AlphaZone currAZ = ((AlphaZone)aZIter.next());
                int az = currAZ.getNumber();
                String ResDmd = "";
                for(int i=0;i<currAZ.getCellsRequiredArray("R").length;i++) ResDmd+= currAZ.getCellsRequiredArray("R")[i]+"\t";
                int totalR = currAZ.getCellsRequired("R");
                int RRSFDemand=s12.numCellsBorrowed[az][Ref.getSubscript("FOROther",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)]+
                        s12.numCellsBorrowed[az][Ref.getSubscript("AG",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)]+
                        s12.numCellsBorrowed[az][Ref.getSubscript("Vagfor",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)];
                int RRMHDemand=s12.numCellsBorrowed[az][Ref.getSubscript("AG",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)]+
                        s12.numCellsBorrowed[az][Ref.getSubscript("Vagfor",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)]+
                        s12.numCellsBorrowed[az][Ref.getSubscript("FOROther",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)];
                int totalRR=RRSFDemand+RRMHDemand;
                String NRDmd="";
                for(int i=0;i<currAZ.getCellsRequiredArray("NR").length;i++) NRDmd+=currAZ.getCellsRequiredArray("NR")[i]+"\t";
                int totalNR = currAZ.getCellsRequired("NR");
                String ForDmd="";
                for(int i=0;i<currCounty.getCellsDemandedArray().length-1;i++) ForDmd+=currCounty.getCellsDemandedArray()[i]+"\t";
                int AgDmd=currCounty.getCellsDemanded("AG");
                int totalLog=currCounty.getCellsDemanded("LOG");
                try {
                    demandFile.println(az+"\t"+currCounty.getName()+"\t"+currAZ.getLandUseIntensity()+
                             "\t"+currAZ.getFactors()[0]+"\t"+ResDmd+totalR+"\t"+RRMHDemand+"\t"+RRSFDemand+"\t"+totalRR+
                             "\t"+currAZ.getFactors()[1]+"\t"+NRDmd+totalNR+"\t"+ForDmd+totalLog+"\t"+AgDmd);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            demandFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //gives the Supply Output file
        OutTextFile supplyFile = new OutTextFile();
        try {
            supplyFile.open("c:/temp/Supply_PostStage12a2b.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            supplyFile.println("\t\t\tCell counts\t\t\t\t\t\t\t\tResSupply\t\t\t\t\tRResSupply\t\t\t\t\t\t\t\t\tNRSupply\t\t\t\t\tAgSupply\t" +
                    "\t\tForSupply");
            supplyFile.println("AZone\tCounty\tLUI" +
                    "\tRs\tNRs\tVdevS\tVTpS\tVagforS\tFORs\tFOROtherS\tAGs" +
                    "\t*R4R\tVdev4R\tVTp4R\tNR4R\ts4RTotal" +
                    "\tA4RRMH\tV4RRMH\tFO4RRMH\ts4RRMHTotal\tA4RRSFD\tV4RRSFD\tFO4RRSFD\ts4RRSFDTotal\ts4RRTotal" +
                    "\t*NR4NR\tVdev4NR\tVTp4NR\tR4NR\ts4NRTotal"+
                    "\tAG4Ag\tVagfor4AG\tFOROther4Ag" +
                    "\tFOR4Log\tFOROther4Log\tVagfor4Log");
        } catch (IOException e) {
            e.printStackTrace();
        }


        Iterator c2 = s12.countyMap.keySet().iterator();
        while (c2.hasNext()) {
            County currCounty = (County)s12.countyMap.get(c2.next());
            Iterator aZIter = currCounty.getAzones().iterator();
            while (aZIter.hasNext()) {
                // Create aZIter pointer to the current alpha zone
                AlphaZone currAZ = ((AlphaZone)aZIter.next());
                int az = currAZ.getNumber();
                //Cell Counts
                int Rs=s12.totalSupplyByGLC[az][Ref.getSubscript("R",Ref.GLCS)];
                int NRs=s12.totalSupplyByGLC[az][Ref.getSubscript("NR",Ref.GLCS)];
                int VdevS=s12.totalSupplyByGLC[az][Ref.getSubscript("Vdev",Ref.GLCS)];
                int VTpS=s12.totalSupplyByGLC[az][Ref.getSubscript("VTp",Ref.GLCS)];
                int VagforS=s12.totalSupplyByGLC[az][Ref.getSubscript("Vagfor",Ref.GLCS)];
                int FORs=s12.totalSupplyByGLC[az][Ref.getSubscript("FOR",Ref.GLCS)];
                int FOROtherS=s12.totalSupplyByGLC[az][Ref.getSubscript("FOROther",Ref.GLCS)];
                int AGs=s12.totalSupplyByGLC[az][Ref.getSubscript("AG",Ref.GLCS)];
                //ResSupply
                int Vdev4R=s12.numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)];
                int VTp4R=s12.numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)];
                int NR4R=s12.numCellsBorrowed[az][Ref.getSubscript("NR",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)];

                //RResSupply
                int AG4RRSFD=s12.numCellsBorrowed[az][Ref.getSubscript("AG",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)];
                int Vagfor4RRSFD=s12.numCellsBorrowed[az][Ref.getSubscript("Vagfor",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)];
                int FOROther4RRSFD=s12.numCellsBorrowed[az][Ref.getSubscript("FOROther",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)];
                int RRSFDTotal=AG4RRSFD+Vagfor4RRSFD+FOROther4RRSFD;
                int AG4RRMH=s12.numCellsBorrowed[az][Ref.getSubscript("AG",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)];
                int Vagfor4RRMH=s12.numCellsBorrowed[az][Ref.getSubscript("Vagfor",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)];
                int FOROther4RRMH=s12.numCellsBorrowed[az][Ref.getSubscript("FOROther",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)];
                int RRMHTotal=AG4RRMH+Vagfor4RRMH+FOROther4RRMH;
                int totalFAV4RR=AG4RRSFD+Vagfor4RRSFD+FOROther4RRSFD+AG4RRMH+Vagfor4RRMH+FOROther4RRMH;
                //NRSupply
                int Vdev4NR=s12.numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)];
                int VTp4NR=s12.numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)];
                int R4NR=s12.numCellsBorrowed[az][Ref.getSubscript("R",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)];

                //AGSupply
                int AG4AG=currCounty.getAGUsedForAg();
                int FOROther4Ag=currCounty.getFOROtherBorrowedForAG();
                int Vagfor4Ag=currCounty.getVagforBorrowedForAG();
//                int AgTotal=FOR4Ag+Vagfor4Ag+Ag4Ag;
                //FORSupply
                int FOR4LOG=currCounty.getFORUsedForLog();
                int FOROther4LOG=currCounty.getFOROtherUsedForLog();
                int Vagfor4LOG=currCounty.getVagforBorrowedForLOG();
//                int ForTotal=Vagfor4FOR+FOR4FOR;


                int R4R = Rs-R4NR;
                int NR4NR = NRs-NR4R;
                int totalR=Vdev4R+VTp4R+NR4R+R4R;
                int totalNR=Vdev4NR+VTp4NR+R4NR+NR4NR;



                try {
                    supplyFile.println(az+"\t"+currCounty.getName()+"\t"+currAZ.getLandUseIntensity()+
                             "\t"+Rs+"\t"+NRs+"\t"+VdevS+"\t"+VTpS+"\t"+VagforS+
                             "\t"+FORs+"\t"+FOROtherS+"\t"+AGs+
                             "\t"+R4R+"\t"+Vdev4R+"\t"+VTp4R+"\t"+NR4R+"\t"+totalR+
                             "\t"+AG4RRMH+"\t"+Vagfor4RRMH+"\t"+FOROther4RRMH+"\t"+RRMHTotal+
                             "\t"+AG4RRSFD+"\t"+Vagfor4RRSFD+"\t"+FOROther4RRSFD+"\t"+RRSFDTotal+"\t"+totalFAV4RR+
                             "\t"+NR4NR+"\t"+Vdev4NR+"\t"+VTp4NR+"\t"+R4NR+"\t"+totalNR+
                             "\t"+AG4AG+"\t"+Vagfor4Ag+"\t"+FOROther4Ag+
                             "\t"+FOR4LOG+"\t"+FOROther4LOG+"\t"+Vagfor4LOG);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            supplyFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




        public static void main(String[] args) {
            checkSupplyandDemand();
        }


}

package com.pb.despair.grid;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.*;
import com.pb.common.grid.*;
import com.pb.common.util.Format;
import com.pb.common.util.OutTextFile;

/**
 * Produces DevType.grid, SQFT.grid, and YRBuilt.grid
 * to be used as starting point in TLUMIP
 *
 * @author Christi Willison
 * @version Nov 17, 2003
 * Created by IntelliJ IDEA.
 */
public class GridSynthesizer {
    private static Logger log = Logger.getLogger("com.pb.despair.grid");
    //used in Stage1, 1b and 2a
    public static int[][] totalSupplyByGLC = new int[Ref.NUM_ALPHA_ZONES][Ref.GLCS.length];//initialized by readTotalCellsByGLC method.
    public static HashMap countyMap = new HashMap();//initialized in the readCountyData and readAlphaZoneData methods.
    //used in Stage1b, 2a and 2b
    public static int[][][] numCellsBorrowed = new int[Ref.NUM_ALPHA_ZONES][Ref.SUPPLYTOBORROW.length][Ref.DEMANDTOSATISFY.length];
    //needed for Stage 2b
    String[] glcForLandUseCode = new String[Ref.NUM_LUCS]; //initialized in the readTotalCellsByGLC method.
    float[][] matchCoefficients = new float[Ref.NUM_LUCS][Ref.DEVTYPES.length]; //initialized in readMatchCoefficients method.
    static short[] undevelopedDevTypes = new short[Ref.NUM_LUCS]; //initialized in readMatchCoefficients method.

    public GridSynthesizer() {
        readTotalCellsByGLC("c:/Project_Files/tlumip/input_files/TotalCellsByGLC.csv");
        readCountyData("c:/Project_Files/tlumip/input_files/S1LOG+AG.csv");
        readAlphaZoneData("c:/Project_Files/tlumip/input_files/S1R+NR.csv");
        readMatchCoefficients("c:/Project_Files/tlumip/input_files/MatchCoefficients.csv");
    }

    private void readTotalCellsByGLC(String fileName){
        readGrossLandCategoryDefinitions("c:/Project_Files/tlumip/input_files/S1GLC.csv");
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String s;
            StringTokenizer st;
            int azone;
            br.readLine();    // Skip the header, stupid
            while ((s = br.readLine()) != null) {
                st = new StringTokenizer(s, ",");
                azone = Integer.parseInt(st.nextToken());     // alpha zone number
                for(int i=0;i<Ref.GLCS.length;i++)
                    totalSupplyByGLC[azone][i]=Integer.parseInt(st.nextToken());
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readGrossLandCategoryDefinitions (String filename){
        // Create a table to hold the gross land category codes, indexed by the more specific land use codes found in
    // the LU5 grid coverage. We'll use null to indicate a missing value.
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            StringTokenizer st;
            String g, s;
            int lc;
            br.readLine();    // Skip the header, stupid
            while ((s = br.readLine()) != null) {
                st = new StringTokenizer(s, ",");
                g = st.nextToken();   // gross land category
                lc = Integer.parseInt(st.nextToken());
                glcForLandUseCode[lc] = g;
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readCountyData (String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            StringTokenizer st;
            String s, sc, cn;
            double[] ForAgDemand = new double[Ref.FORAGDEVTYPES.length];

            br.readLine();    // Skip the header, stupid
            while ((s = br.readLine()) != null) {
                st = new StringTokenizer(s, ",");
                st.nextToken();     // skip the county FIPS code
                cn = st.nextToken();   // county name
                sc = st.nextToken();   // state postal code
                for(int i=0;i<ForAgDemand.length;i++){ //demand in acres
                    ForAgDemand[i] = Double.parseDouble(st.nextToken());
                }
                County c = new County(cn, sc, ForAgDemand);

                // Okay, now add this county to the hashmap
                if (!countyMap.containsKey(cn)) countyMap.put(cn,c);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readAlphaZoneData (String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            StringTokenizer st;
            String s, luc, cn;
            br.readLine();    // Skip the header, stupid
            int az;
            while ((s = br.readLine()) != null) {
                double[] SQFTdemand=new double[Ref.RDEVTYPES.length+Ref.NRDEVTYPES.length];
                st = new StringTokenizer(s, ",");
                az = Integer.parseInt(st.nextToken());     // alpha zone number
                st.nextToken();   // Skip the county FIPS code
                luc = st.nextToken();      // land use intensity code
                cn = st.nextToken();      // county name
                for(int i=0;i<Ref.RDEVTYPES.length;i++){
                    SQFTdemand[i]=Double.parseDouble(st.nextToken());
                }
                st.nextToken(); //skip the TotalR column
                for(int i=0;i<Ref.NRDEVTYPES.length;i++){
                    SQFTdemand[i+Ref.RDEVTYPES.length]=Double.parseDouble(st.nextToken());
                }
                AlphaZone a = new AlphaZone(az, cn, luc, SQFTdemand);
                // Okay, now add this alpha zone to the county-level hashmap
                if (!countyMap.containsKey(cn)) log.severe("County "+cn+" for zone "+az+" does not exist, stupid");
                else ((County) countyMap.get(cn)).azones.add(a);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMatchCoefficients(String fileName){
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String s;
            int landCoverValue;
            br.readLine(); //skip the header row
            while((s=br.readLine()) != null){
                StringTokenizer st = new StringTokenizer(s,",");
                landCoverValue = Integer.parseInt(st.nextToken());
                undevelopedDevTypes[landCoverValue] = Short.parseShort(st.nextToken());
                for(int i=0;i<matchCoefficients[0].length;i++)
                     matchCoefficients[landCoverValue][i] = Float.parseFloat(st.nextToken());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runStage1(boolean writeTextFile){
        log.info("Beginning Stage 1");
        OutTextFile Stage1az = null;
        if (writeTextFile) {
            Stage1az = new OutTextFile();
            Stage1az.open("c:/temp/Stage1az_output.txt");
            try {
                Stage1az.println("AZone\tCounty\tLUI\tR_demand\tNR_demand\tR_supply\tNR_supply" +
                        "\tVdevSupply\tVTpSupply\tR_excess\tNR_excess\tVdev_excess\tVTp_excess\tTotalExcess");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Loop through each alpha zone in each county and do the Stage 1 evaluations for the following cases
        Iterator countyIter = countyMap.keySet().iterator();
        while (countyIter.hasNext()) {
            County currCounty = (County)countyMap.get(countyIter.next());
            // Loop through each zone in the county
            Iterator aZIter = currCounty.azones.iterator();
            while (aZIter.hasNext()) {
                AlphaZone currAZ = ((AlphaZone)aZIter.next());
                int az = currAZ.getNumber();
                String[] glcCase ={"R","NR"};
                String glc;
                int[] excess = new int[4]; //excess[0]=R cells left, excess[1]=NR cells left, excess[2] = Vdev cells, excess[3]= VTp
                // Sum up the Vagfor, FOR, and AG supply for each alpha zone so we know total at the county level
                currCounty.Vagfor_supply += totalSupplyByGLC[az][Ref.getSubscript("Vagfor",Ref.GLCS)];
                currCounty.FOR_supply += totalSupplyByGLC[az][Ref.getSubscript("FOR",Ref.GLCS)];
                currCounty.AG_supply += totalSupplyByGLC[az][Ref.getSubscript("AG",Ref.GLCS)];
                currCounty.FOROther_supply+= totalSupplyByGLC[az][Ref.getSubscript("FOROther",Ref.GLCS)];

                int Vdev_supply = totalSupplyByGLC[az][Ref.getSubscript("Vdev",Ref.GLCS)];
                int VTp_supply = totalSupplyByGLC[az][Ref.getSubscript("VTp",Ref.GLCS)];

                for(int c=0;c<2;c++){
                //glcCase[0] == "R".  Comparing Rcell_demand with Rcell_supply (calc. from bldg. sq. feet demand using maxFARs)
                //glcCase[1] == "NR". Comparing available NR cells with NR cells required (calc. from bldg. sq. feet demand using maxFARs)
                    glc= glcCase[c];
                    int cellsSupplied = totalSupplyByGLC[az][Ref.getSubscript(glc,Ref.GLCS)]; //Total number of R or NR cells avail
                    int cellsDemanded = currAZ.getMaxCellsRequired(glc);
                    if(cellsDemanded !=0){
                        int residual = cellsDemanded - cellsSupplied;
                        if(residual > 0){ //cellsDemanded > cellsSupplied => steal from VTp
                            int i = Math.min(VTp_supply,residual);
                            residual -= i;
                            VTp_supply -= i;
                            if(residual > 0){ //cellsDemanded > cellsSupplied + VTp_supply => steal from Vdev
                                i=Math.min(Vdev_supply,residual);
                                residual -= i;
                                Vdev_supply -= i;
                                if(residual > 0) { //cellsDemanded > cellsSupplied + VTp_supply + Vdev_supply
//                                    log.info("\t"+az+"\t"+currCounty.countyName+"\t"+currAZ.landUseIntensity+
//                                             "\t"+glcForLandUseCode+"\t"+residual);
                                    excess[c]=-residual;
                                }//cellsDemanded <= cellsSupplied + all of VTp + all or some of Vdev. excess[c]==0
                            }//cellsDemanded <= cellsSupplied + all or some of VTp. excess[c]==0
                        } else{ //cellsDemanded <= cellsSupplied
                            excess[c]=-residual;  //will be negative
                        }
                    } //no cells demanded, unsatisfiedDemand[c]==0
                }//next case (NR)
                currCounty.VTp_supply += VTp_supply;  //add any excess VTp_supply to the county total to be borrowed to satisfy
                                                       //LOG or AG demand
                excess[2] = Vdev_supply;
                excess[3] = VTp_supply;
                int totalExcess =0;
                for(int i=0;i<excess.length;i++) totalExcess+=excess[i];
                if (writeTextFile) {
                    try {
                        Stage1az.println(az+"\t"+currCounty.name+"\t"+currAZ.getLandUseIntensity()+
                                "\t"+currAZ.getMaxCellsRequired("R")+"\t"+currAZ.getMaxCellsRequired("NR")+
                                "\t"+totalSupplyByGLC[az][Ref.getSubscript("R",Ref.GLCS)]+"\t"+totalSupplyByGLC[az][Ref.getSubscript("NR",Ref.GLCS)]+
                                "\t"+totalSupplyByGLC[az][Ref.getSubscript("Vdev",Ref.GLCS)]+
                                "\t"+totalSupplyByGLC[az][Ref.getSubscript("VTp",Ref.GLCS)]+
                                "\t"+excess[0]+"\t"+excess[1]+"\t"+excess[2]+"\t"+excess[3]+"\t"+totalExcess);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }//next alpha zone
        }//next county
        if (writeTextFile) {
            try {
                Stage1az.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //we could have done this all within the same county loop but we'd like to produce
        //separate output.  This will produce the County output.
        OutTextFile Stage1county = null;
        if (writeTextFile) {
            Stage1county = new OutTextFile();
            Stage1county.open("c:/temp/Stage1county_output.txt");
            try {
                Stage1county.println("County\tLOG_demand\tAG_demand\tFOR_supply\tFOROther_supply\tAG_supply\tVagfor_supply"+
                        "\tFOR_excess\tFOROther_excess\tAG_excess\tVagfor_excess\tFOAVCounty_excess");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        countyIter = countyMap.keySet().iterator();
        while (countyIter.hasNext()) {
            County currCounty = (County)countyMap.get(countyIter.next());
            int FOROthersupply=currCounty.FOROther_supply; //+currCounty.FOR_supply (we took this out (2/5/04)
            int FORsupply=currCounty.FOR_supply;
            int AGsupply=currCounty.AG_supply;
            int Vagforsupply=currCounty.Vagfor_supply;
            int FOROtherExcess;
//            int VTpsupply=currCounty.VTp_supply;
            //Now do the LOG and AG at the county level
            //First do AG
            int residual = currCounty.getCellsDemanded("AG") - AGsupply;
            if (residual>0) {
                currCounty.AGUsedForAg=AGsupply;
                AGsupply=0;
                int i = Math.min(residual,Vagforsupply);
                residual -= i;
                Vagforsupply -= i;
                currCounty.VagforBorrowedForAG+=i;
                if(residual>0){
                    i = Math.min(residual,FOROthersupply);
                    residual -=i;
                    FOROthersupply -= i;
                    currCounty.FOROtherBorrowedForAG+=i;

//                    if(residual>0) {
//                        i = Math.min(residual,VTpsupply);
//                        residual -=i;
//                        VTpsupply -= i;
//                        currCounty.VTpBorrowedForAG+=i;
//                    }
                }
            }else {
                AGsupply-=currCounty.getCellsDemanded("AG");
                currCounty.AGUsedForAg=currCounty.getCellsDemanded("AG");
            }
            FOROtherExcess=FOROthersupply;

            //Now do LOG
            residual = currCounty.getCellsDemanded("LOG") - FORsupply;
            if (residual>0) {//LOG demand > FOR supply
                currCounty.FORUsedForLog=FORsupply;
                FORsupply=0;
                int i = Math.min(residual,FOROtherExcess);
                residual -= i;
                FOROtherExcess -= i;
                currCounty.FOROtherUsedForLog+=i;
                if(residual>0){
                    i = Math.min(residual,Vagforsupply);
                    residual -=i;
                    Vagforsupply -= i;
                    currCounty.VagforBorrowedForLOG +=i;
                }
            }else {
                FORsupply-=currCounty.getCellsDemanded("LOG"); //LOG demand <= FOR supply so there will be some FOR leftover
                currCounty.FORUsedForLog=currCounty.getCellsDemanded("LOG");
            }
            currCounty.FORExcess= FORsupply;
            currCounty.FOROtherExcess=FOROtherExcess;
            currCounty.AGExcess=AGsupply;
            currCounty.VagforExcess = Vagforsupply;

            int foavExcess=FOROtherExcess+AGsupply+Vagforsupply;
            if (writeTextFile) {
                try {
                    Stage1county.println(currCounty.name+"\t"+currCounty.getCellsDemanded("LOG")+"\t"+currCounty.getCellsDemanded("AG")+
                            "\t"+currCounty.FOR_supply+"\t"+currCounty.FOROther_supply+"\t"+currCounty.AG_supply+
                            "\t"+currCounty.Vagfor_supply+"\t"+currCounty.FORExcess+"\t"+currCounty.FOROtherExcess+
                            "\t"+currCounty.AGExcess+"\t"+currCounty.VagforExcess+"\t"+foavExcess);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }//next county
        if (writeTextFile) {
            try {
                Stage1county.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.info("End of Stage 1");
    }//end method


    public void runStage2a(boolean writeTextFile){ //trying to resolve RR demand using Vagfor/AG/FOR land.  Stage 1 must be run first.
        log.info("Starting Stage 2a");
        OutTextFile Stage2a = null;
        if (writeTextFile) {
            Stage2a = new OutTextFile();
            Stage2a.open("c:/temp/Stage2aRR_output.txt");
            try {
                Stage2a.println("County/AZ\tFOAVCountyExcess\tAGinAZ\tVagforInAZ\tFOROtherInAZ\tTotalFOAVinAZ" +
                        "\tRRSFD_D\tRRMH_D\tTotalRR_D\tRRSFD_AG\tRRSFD_Vagfor\tRRSFD_FO\tRRMH_A\tRRMH_V\tRRMH_FO\tTotalRR_FOAV");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Iterator countyIter = countyMap.keySet().iterator();
        while (countyIter.hasNext()) {
            County currCounty = (County)countyMap.get(countyIter.next());
            int AGCountyExcess=currCounty.getAGExcess();
            int VagforCountyExcess=currCounty.getVagforExcess();
            int FOROtherCountyExcess= currCounty.getFOROtherExcess();
            int countyExcess = FOROtherCountyExcess+AGCountyExcess+VagforCountyExcess;
            if (writeTextFile) {
                    try {
                        Stage2a.println(currCounty.name+"\t"+countyExcess);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
            // Loop through each zone in the county
            Iterator aZIter = currCounty.azones.iterator();
            while (aZIter.hasNext() && countyExcess>0) {
                AlphaZone currAZ = ((AlphaZone)aZIter.next());
                    int az = currAZ.getNumber();
                    String lui = currAZ.getLandUseIntensity();
                    if(lui.equals("VeryLow")){//we only want to do this for zones with VeryLow land use
                        int RNRresidual = (currAZ.getCellsRequired("R")+currAZ.getCellsRequired("NR"))-
                                (totalSupplyByGLC[az][Ref.getSubscript("R",Ref.GLCS)]+totalSupplyByGLC[az][Ref.getSubscript("NR",Ref.GLCS)]+
                                totalSupplyByGLC[az][Ref.getSubscript("Vdev",Ref.GLCS)]+totalSupplyByGLC[az][Ref.getSubscript("VTp",Ref.GLCS)]); //cellsReq - cellsAvail
                        if (RNRresidual > 0) {
                            int RRSFDemand= currAZ.getCellsRequired("RRSFD");
                            int RRMHDemand=currAZ.getCellsRequired("RRMH");
                            int totalRRDemand = RRSFDemand + RRMHDemand;
                            if(totalRRDemand>=countyExcess) {
                                float factor=countyExcess/(float)totalRRDemand;
                                totalRRDemand=countyExcess;  //can't use up more cells than the county has available
                                RRSFDemand=(int)Math.ceil(RRSFDemand*factor);
                                RRMHDemand=countyExcess-RRSFDemand;
                            }
                            if (totalRRDemand >0) {
                                int AsupplyInAlphaZone = Math.min(AGCountyExcess,totalSupplyByGLC[az][Ref.getSubscript("AG",Ref.GLCS)]);
                                int VsupplyInAlphaZone = Math.min(VagforCountyExcess,totalSupplyByGLC[az][Ref.getSubscript("Vagfor",Ref.GLCS)]);
                                int FOsupplyInAlphaZone = Math.min(FOROtherCountyExcess,totalSupplyByGLC[az][Ref.getSubscript("FOROther",Ref.GLCS)]);
                                int FOAVsupplyInAlphaZone=FOsupplyInAlphaZone+AsupplyInAlphaZone+VsupplyInAlphaZone;
                                if(FOAVsupplyInAlphaZone > 0){//try to satisfy some RRDemand with AG, FOR, then Vagfor
                                    if (writeTextFile) {
                                        try {
                                            Stage2a.print(az+"\t"+countyExcess+"\t"+AsupplyInAlphaZone+"\t"+VsupplyInAlphaZone+"\t"+FOsupplyInAlphaZone+
                                                    "\t"+FOAVsupplyInAlphaZone+"\t"+RRSFDemand+"\t"+RRMHDemand+"\t"+totalRRDemand);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    int[] RRcellsSatisfiedByAG=new int[2];  //RRcellsSatisfiedByAG[0] = RRSFDemand, RRcellsSatisfiedByAG[1] = RRMHDemand
                                    int[] RRcellsSatisfiedByVagfor=new int[2];  //RRcellsSatisfiedByVagfor[0] = RRSFDemand, RRcellsSatisfiedByVagfor[1] = RRMHDemand
                                    int[] RRcellsSatisfiedByFOROther=new int[2];  //RRcellsSatisfiedByFOROther[0] = RRSFDemand, RRcellsSatisfiedByFOROther[1] = RRMHDemand
                                    int residual = totalRRDemand-AsupplyInAlphaZone;
                                    if(residual > 0){ //totalRRDemand > AsupplyInAlphaZone
                                        //satisfy as much RRSFD as you can
                                        int sfaz=Math.min(RRSFDemand,AsupplyInAlphaZone);
                                        RRSFDemand-=sfaz;
                                        AsupplyInAlphaZone-=sfaz;
                                        AGCountyExcess-=sfaz;
                                        RRcellsSatisfiedByAG[0] = sfaz;
                                        numCellsBorrowed[az][Ref.getSubscript("AG",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)]=sfaz;
                                        //satisfy as much RRMH as you can
                                        int mhaz=Math.min(RRMHDemand,AsupplyInAlphaZone);
                                        RRMHDemand-=mhaz;
                                        AsupplyInAlphaZone-=mhaz;
                                        AGCountyExcess-=mhaz;
                                        RRcellsSatisfiedByAG[1] = mhaz;
                                        numCellsBorrowed[az][Ref.getSubscript("AG",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)]=mhaz;

                                        //then try to satisfy the rest with Vagfor
                                        int i=Math.min(residual,VsupplyInAlphaZone);
                                        residual-=i;
                                        //satisfy as much RRSFD as you can
                                        sfaz=Math.min(RRSFDemand,VsupplyInAlphaZone);
                                        RRSFDemand-=sfaz;
                                        VsupplyInAlphaZone-=sfaz;
                                        VagforCountyExcess-=sfaz;
                                        RRcellsSatisfiedByVagfor[0] = sfaz;
                                        numCellsBorrowed[az][Ref.getSubscript("Vagfor",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)]=sfaz;
                                        //satisfy as much RRMH as you can
                                        mhaz=Math.min(RRMHDemand,VsupplyInAlphaZone);
                                        RRMHDemand-=mhaz;
                                        VsupplyInAlphaZone-=mhaz;
                                        VagforCountyExcess-=mhaz;
                                        RRcellsSatisfiedByVagfor[1] = mhaz;
                                        numCellsBorrowed[az][Ref.getSubscript("Vagfor",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)]=mhaz;
                                        //check to see if Vagfor was enough
                                        if(residual > 0){ //Vagfor supply wasn't enough, borrow any FOROther supply in alphazone if we have it.
                                            //satisfy as much RRSFDemand as you can
                                            sfaz=Math.min(RRSFDemand,FOsupplyInAlphaZone);
                                            RRSFDemand-=sfaz;
                                            FOsupplyInAlphaZone-=sfaz;
                                            FOROtherCountyExcess-=sfaz;
                                            RRcellsSatisfiedByFOROther[0] = sfaz;
                                            numCellsBorrowed[az][Ref.getSubscript("FOROther",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)]=sfaz;
                                            //satisfy as much RRMH as you can
                                            mhaz=Math.min(RRMHDemand,FOsupplyInAlphaZone);
                                            RRMHDemand-=mhaz;
                                            FOsupplyInAlphaZone-=mhaz;
                                            FOROtherCountyExcess-=mhaz;
                                            RRcellsSatisfiedByFOROther[1] = mhaz;
                                            numCellsBorrowed[az][Ref.getSubscript("FOROther",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)]=mhaz;
                                        }
                                    }else{ //Ag supply is enough to satisfy all the RR demand
                                        RRcellsSatisfiedByAG[0]=RRSFDemand;
                                        numCellsBorrowed[az][Ref.getSubscript("AG",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRSFD",Ref.DEMANDTOSATISFY)]=RRSFDemand;
                                        RRcellsSatisfiedByAG[1]=RRMHDemand;
                                        numCellsBorrowed[az][Ref.getSubscript("AG",Ref.SUPPLYTOBORROW)][Ref.getSubscript("RRMH",Ref.DEMANDTOSATISFY)]=RRMHDemand;
                                        AGCountyExcess-=totalRRDemand;
                                   }//end if-else
//                        if(RRcellsSatisfiedByFAV[0]==currAZ.getCellsRequired("RRSFD")) currAZ.decrementBldgSQFT("RRSFD",currAZ.getDemandInBldgSQFT("RRSFD"));
//                       else {
//                            float RRSFDBldgSqft=RRcellsSatisfiedByFAV[0]*Ref.SQFT_PER_GRIDCELL*.03f;
//                            currAZ.decrementBldgSQFT("RRSFD",RRSFDBldgSqft);
//                        }
//                        if(RRcellsSatisfiedByFAV[1]==currAZ.getCellsRequired("RRMH")) currAZ.decrementBldgSQFT("RRMH",currAZ.getDemandInBldgSQFT("RRMH"));
//                       else{
//                            float RRMHBldgSqft=RRcellsSatisfiedByFAV[1]*Ref.SQFT_PER_GRIDCELL*0.02f;
//                            currAZ.decrementBldgSQFT("RRMH",RRMHBldgSqft);
                                    //Now add up how many cells we of each RR devType we satisfied
                                    int SFDcellsSatisfied=RRcellsSatisfiedByAG[0]+RRcellsSatisfiedByVagfor[0]+RRcellsSatisfiedByFOROther[0];
                                    int MHcellsSatisfied=RRcellsSatisfiedByAG[1]+RRcellsSatisfiedByVagfor[1]+RRcellsSatisfiedByFOROther[1];
                                    //To avoid rounding issues, we need to mulitiply the number of cells borrowed times the
                                    //actual  bldg. sq. footage for that devType.  (1-(cellDiff/totalCells))*newFAR*SQFT_PER_GRIDCELL
                                    double RRSFDBldgSqft=Math.rint((SFDcellsSatisfied*
                                            (1.0-(currAZ.getCellsDiff(Ref.getSubscript("RRSFD",Ref.DEVTYPES))/currAZ.getCellsRequired("RRSFD")))*
                                            currAZ.getTypFAR(Ref.getSubscript("RRSFD",Ref.DEVTYPES))*Ref.SQFT_PER_GRIDCELL));
                                    currAZ.RRbldgSQFTDemand[0]=RRSFDBldgSqft;
                                    currAZ.setRRSFDonFAVSQFTPerCell(Math.ceil(RRSFDBldgSqft/SFDcellsSatisfied));
                                    currAZ.decrementBldgSQFT("RRSFD",RRSFDBldgSqft);

                                    double RRMHBldgSqft=Math.rint((MHcellsSatisfied*
                                            (1.0-(currAZ.getCellsDiff(Ref.getSubscript("RRMH",Ref.DEVTYPES))/currAZ.getCellsRequired("RRMH")))*
                                            currAZ.getTypFAR(Ref.getSubscript("RRMH",Ref.DEVTYPES))*Ref.SQFT_PER_GRIDCELL));
                                    currAZ.RRbldgSQFTDemand[1]=RRMHBldgSqft;
                                    currAZ.setRRMHonFAVSQFTPerCell(Math.ceil(RRMHBldgSqft/MHcellsSatisfied));
                                    currAZ.decrementBldgSQFT("RRMH",RRMHBldgSqft);

                                    currAZ.calculateCellsRequired();
                                    int totalRRSatisfiedByFAV= SFDcellsSatisfied+MHcellsSatisfied;
                                    countyExcess=AGCountyExcess+VagforCountyExcess+FOROtherCountyExcess;
                                    if (writeTextFile) {
                                        try {
                                            Stage2a.println("\t"+RRcellsSatisfiedByAG[0]+"\t"+RRcellsSatisfiedByVagfor[0]+"\t"+RRcellsSatisfiedByFOROther[0]+
                                                    "\t"+RRcellsSatisfiedByAG[1]+"\t"+RRcellsSatisfiedByVagfor[1]+"\t"+RRcellsSatisfiedByFOROther[1]+
                                                    "\t"+totalRRSatisfiedByFAV);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }//we don't have any AVFO supply so we can't borrow from AG, Vagfor, or FOROther lands
                            }//no RR demand so no need to borrow
                        }//there is enough R and NR supply to satisfy R and NR demand so no need to borrow AVFO for RR
                    }//alpha zone is not "very low"
            }//next alpha zone
            currCounty.FOROtherExcess=FOROtherCountyExcess;
            currCounty.AGExcess=AGCountyExcess;
            currCounty.VagforExcess=VagforCountyExcess;
       }//next county
        if (writeTextFile) {
            try {
                Stage2a.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.info("End of Stage 2a");
    }

    public void runStage2b(boolean writeTextFile){
        log.info("Starting Stage 2b");
        OutTextFile Stage2b = null;
        if (writeTextFile) {
            Stage2b = new OutTextFile();
            Stage2b.open("c:/temp/Stage2bBalance_output.txt");
            try {
                Stage2b.println("AZone\tCounty\tLUI\tR_factor\tNR_factor\tUpdatedR_factor\tUpdatedNR_factor\tMH\tMF\tAT\tSFD\tRRMH\tRRSFD\t" +
                                "Accom\tDepot\tGovSppt\tGschool\tHvyInd\tHospital\tInst\tLtInd\tOffice\tRetail\tWhse");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String[] glcCase ={"R","NR"};
        Iterator countyIter = countyMap.keySet().iterator();
        while (countyIter.hasNext()) {
            County currCounty = (County)countyMap.get(countyIter.next());
//            log.info("\tEvaluating supply and demand in "+ currCounty.countyName + " county.");
            Iterator azIter = currCounty.azones.iterator();
            while (azIter.hasNext()) {
                // Create aZIter pointer to the current alpha zone
                AlphaZone currAZ = ((AlphaZone)azIter.next());
                int az = currAZ.getNumber();
                int VTpcellsAvail = totalSupplyByGLC[az][Ref.getSubscript("VTp",Ref.GLCS)];
                int VdevcellsAvail = totalSupplyByGLC[az][Ref.getSubscript("Vdev",Ref.GLCS)];
                String glc;
                //initialized to 1 which implies no change to the FARs which is the default case.
                double[] factors = {1.0,1.0};//factors[0]=="R_factor", factors[1]=="NR_factor"
                double[] updatedFactors = {1.0,1.0};//used in Part II
                int[] unsatisfiedCells = new int[2];//unsatisfiedCells[0]=="unsatisfiedRcells", unsatisfiedCells[1]=="unsatisfiedNRcells"
                                                    //will only be populated when cellsReq>cellsAvail+allVTpcellsAvail+allVdevcellsAvail
                                                    //and will be compared against any excess R or NR cells
                int[] cellsExcess = new int[2]; //cellsExcess[0]=RcellsExcess, cellsExcess[1]=NRcellsExcess -unused supply
 // PART I
                for(int c=0;c<2;c++){
                //glcCase[0] == "R".  Comparing availability of R cells with R cells required (calc. from bldg. sq. feet demand)
                //glcCase[1] == "NR". Comparing available NR cells with NR cells required (calc. from bldg. sq. feet demand)
                glc= glcCase[c];
                int cellsAvail = totalSupplyByGLC[az][Ref.getSubscript(glc,Ref.GLCS)]; //Total number of R or NR cells avail
                int cellsReq = currAZ.getCellsRequired(glc);
                int residual = cellsReq-cellsAvail; //cells req - cells avail
                if (cellsReq != 0) {
                    if(residual>0){//cellsReq>cellsAvail
                        int i = Math.min(VTpcellsAvail,residual);
                        residual-=i;
                        VTpcellsAvail-=i;
                        numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript(glc,Ref.DEMANDTOSATISFY)]+=i;
                        if(residual>0){//cellsReq>cellsAvail+allVTpcellsAvail
                            i = Math.min(VdevcellsAvail,residual);
                            residual-=i;
                            VdevcellsAvail-=i;
                            numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript(glc,Ref.DEMANDTOSATISFY)]+=i;
                            if(residual>0){//cellsReq>cellsAvail+allVTpcellsAvail+allVdevcellsAvail
                                unsatisfiedCells[c] = residual;
                                int totalCellsAvail = (cellsAvail +
                                        (VTpcellsAvail+numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript(glc,Ref.DEMANDTOSATISFY)])
                                        + (VdevcellsAvail+numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript(glc,Ref.DEMANDTOSATISFY)]));
                                if (totalCellsAvail != 0) {//factor will be greater than 1.
                                    factors[c] = (double)cellsReq/totalCellsAvail;

                                }else{//there are no cells to borrow so the FARs must be infinitely large to satisfy
                                        //thus factor==arbitrary large number (in this case 100000)
                                        factors[c] = 100000.0;
                                }
                            }//cellsReq = cellsAvail + allVTpcellsAvail + someVdevcellsAvail (had to borrow allVTp and someVdev cells) -FARs are still OK
                            //OR cellsReq=cellsAvail + allVTpcellsAvail + allVdevcellsAvail - FARs are OK.

                        }//cellsReq = cellsAvail + someVTpcellsAvail (had to borrow some VTp cells) - FARs are OK
                        //OR cellsReq = cellsAvail + allVTpcellsAvail - FARs are OK.

                    }//cellsReq < cellsAvail so FARs are too high, factor<1
                    //OR cellsReq = cellsAvail, factor==1
                    else{
                        factors[c] = (double)cellsReq/cellsAvail;
                    }
                }//no RcellsReq or NRcellsReq in this alpha zone, so FARs are unchanged (factors[c]==1) and go to next case (or next zone if c==1).
                cellsExcess[c]=-residual;
                }//next GLC case
//PART II
                //Now that we have looked at the R and NR case separately and borrowed as much vague as possible
                //we need to try to balance the factors by sharing Rsupply=RcellsAvail+borrowedVague or NR
                //supply=NRcellsAvail+borrowedVague.  Otherwise, updatedFactors=factors
                //
                int numBorrowed=0;
                int newRSupply;
                int newNRSupply; //is needed for "newFAR" calculation
                int Rd=currAZ.getCellsRequired("R");
                int NRd=currAZ.getCellsRequired("NR");
                int VTpR = numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)];
                int VdevR = numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)];
                int VTpNR = numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)];
                int VdevNR = numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)];
                int Rs=totalSupplyByGLC[az][Ref.getSubscript("R",Ref.GLCS)]+VTpR+VdevR;
                int NRs=totalSupplyByGLC[az][Ref.getSubscript("NR",Ref.GLCS)]+VTpNR+VdevNR;
                //Case 1: (R_factor-NR_factor>0.5f ) => borrow from NRexcess(if any), then borrowedVTp, then borrowedVdev, then NRcellsAvail
                if(factors[0]-factors[1]>.50 && Rd>0 && NRs != 0){ //there needs to be Rdemand and NRsupply, otherwise there is no way to balance.
                    int x=(int)Math.rint((((double)(Rd*NRs)-(Rs*NRd))/(Rd+NRd))+0.5); //We want Rd/(Rs + x) = NRd/(NRs-x).  Solving for x gives us the equation
                    if(x>=NRs){//implies that we need to borrow all of the NRs (and more if we could if x>NRs) to balance the factors.
                                //Since we don't want to borrow all of the NRs, we will give all but 1 cell of it to R.
                        numBorrowed=NRs-1;
                        //increase the values in the numCellsBorrowed array for R demand
                        numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]+=VTpNR;
                        numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]+=VdevNR;
                        numCellsBorrowed[az][Ref.getSubscript("NR",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]+=
                                totalSupplyByGLC[az][Ref.getSubscript("NR",Ref.GLCS)]-1;
                        //decrease the values in the numCellsBorrowed array for NR demand
                        numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]-=VTpNR;
                        numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]-=VdevNR;
                    }else{//(x<NRs) implies that we only need to borrow some of the NRs, in which we have 4 pools:  NRexcess, borrowedVTp
                        //borrowedVdev, or NRcellsAvail(not excess).  We need to borrow in that order.
                        int i=0;
                        if (cellsExcess[1]>0) {
                            i = Math.min(x,cellsExcess[1]);
                            x-=i;
                            numBorrowed+=i;
                            numCellsBorrowed[az][Ref.getSubscript("NR",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]+=i;
                        }
                        if(x>0){//we need to borrow some/all of the VTpNR
                            i=Math.min(x,VTpNR);
                            x-=i;
                            numBorrowed+=i;
                            numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]+=i;
                            numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]-=i;
                            if(x>0){//keep borrowing, we are now into the VdevNR
                                i=Math.min(x,VdevNR);
                                x-=i;
                                numBorrowed+=i;
                                numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]+=i;
                                numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]-=i;
                                if(x>0){//since x<NRs we know that we just need to borrow x number of NRcellsAvail to finally balance
                                    numBorrowed+=x;
                                    numCellsBorrowed[az][Ref.getSubscript("NR",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]+=x;
                                }//we have corrected the imbalance with NRexcess + VTpNR + VdevNR
                            }//we have corrected the imbalance with NRexcess + VTpNR
                        }//we have corrected the imbalance with NRexcess
                    }
                    newRSupply = Rs+numBorrowed;
                    newNRSupply = NRs-numBorrowed;
                    updatedFactors[0]=(double)Rd/(Rs+numBorrowed);
                    if(NRd>0) updatedFactors[1]=(double)NRd/(NRs-numBorrowed);
                    else updatedFactors[1]=1.0; //no NRdemand so the FAR remains the same (ie. factors=1)

                //Case 2: (NR_factor-R_factor>0.5f ) => borrow from Rexcess(if any), then borrowedVTp, then borrowedVdev, then RcellsAvail
                }else if(factors[1]-factors[0] >.50 && NRd>0 && Rs!=0){//NR_factor>.5+R_factor and there is some NRdemand and some Rsupply, otherwise x will be negative (or 0).
                    numBorrowed=0;
                    int x=(int)Math.rint(((double)((Rs*NRd)-(Rd*NRs))/(Rd+NRd))+0.5); //We want NRd/(NRs + x) = Rd/(Rs-x).  Solving for x gives us the equation
                    if(x>=Rs){//implies we need to borrow all of Rs but we don't want to borrow all the Rs so we will borrow all but 1 cell;
                        numBorrowed=Rs-1;
                        //increase the NR cells borrowed
                        numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]+=VdevR;
                        numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]+=VTpR;
                        numCellsBorrowed[az][Ref.getSubscript("R",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]+=
                                totalSupplyByGLC[az][Ref.getSubscript("R",Ref.GLCS)]-1;
                        //decrease the Rcells borrowed
                        numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]-=VdevR;
                        numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]-=VTpR;
                    }else{//(x<Rs) implies we need to borrow some of the Rs but not all so borrow in this order: Rexcess, VTpR, VdevR, then RcellsAvail
                        int i=0;
                        if (cellsExcess[0]>0) {
                            i = Math.min(x,cellsExcess[0]);
                            x-=i;
                            numBorrowed+=i;
                            numCellsBorrowed[az][Ref.getSubscript("R",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]+=i;
                        }
                        if(x>0){//we need to borrow some/all of the VTpR
                            i=Math.min(x,VTpR);
                            x-=i;
                            numBorrowed+=i;
                            numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]+=i;
                            numCellsBorrowed[az][Ref.getSubscript("VTp",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]-=i;
                            if(x>0){//keep borrowing, we are now into the VdevR
                                i=Math.min(x,VdevR);
                                x-=i;
                                numBorrowed+=i;
                                numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]+=i;
                                numCellsBorrowed[az][Ref.getSubscript("Vdev",Ref.SUPPLYTOBORROW)][Ref.getSubscript("R",Ref.DEMANDTOSATISFY)]-=i;
                                if(x>0){//since x<Rs we know that we just need to borrow x number of RcellsAvail to finally balance
                                    numBorrowed+=x;
                                    numCellsBorrowed[az][Ref.getSubscript("R",Ref.SUPPLYTOBORROW)][Ref.getSubscript("NR",Ref.DEMANDTOSATISFY)]+=x;
                                }//we have corrected the imbalance with Rexcess + VTpR + VdevR
                            }//we have corrected the imbalance with Rexcess + VTpR
                        }//we have corrected the imbalance with Rexcess

                    }
                    newRSupply = Rs-numBorrowed;
                    newNRSupply = NRs+numBorrowed;
                    if(Rd>0) updatedFactors[0]=(double)Rd/(Rs-numBorrowed);//should increase R_factor
                    else updatedFactors[0]=1.0;//No Rdemand so FARs remain the same (ie. factor=1)
                    updatedFactors[1]=(double)NRd/(NRs+numBorrowed);//should decrease NR_factor

                    //Case 3:  (No balancing necessary so the the updatedFactors are the same as the factors calculated
                    //in Part I.
                }else{
                    newRSupply = Rs;
                    newNRSupply = NRs;
                    updatedFactors[0] = factors[0];
                    updatedFactors[1] = factors[1];
                }
                //update alphazone to reflect the final changes.
                currAZ.updateTypFARs(updatedFactors, newRSupply , newNRSupply);
                currAZ.updateCellsRequired(newRSupply,newNRSupply);
                currAZ.calculateBldgSQFTPerCell();


                if (writeTextFile) {
                    try {
                        Stage2b.println(az+"\t"+currCounty.name+"\t"+currAZ.getLandUseIntensity()+
                                "\t"+factors[0]+"\t"+factors[1]+"\t"+currAZ.getFactors()[0]+"\t"+currAZ.getFactors()[1]+
                                "\t"+currAZ.typFARs[0]+"\t"+currAZ.typFARs[1]+"\t"+currAZ.typFARs[2]+
                                "\t"+currAZ.typFARs[3]+"\t"+currAZ.typFARs[4]+"\t"+currAZ.typFARs[5]+
                                "\t"+currAZ.typFARs[6]+"\t"+currAZ.typFARs[7]+"\t"+currAZ.typFARs[8]+
                                "\t"+currAZ.typFARs[9]+"\t"+currAZ.typFARs[10]+"\t"+currAZ.typFARs[11]+
                                "\t"+currAZ.typFARs[12]+"\t"+currAZ.typFARs[13]+"\t"+currAZ.typFARs[14]+
                                "\t"+currAZ.typFARs[15]+"\t"+currAZ.typFARs[16]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }//next alpha zone
        }//next county
        if (writeTextFile) {
            try {
                Stage2b.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.info("End of Stage 2b");
    }//end of method

    public void runStage3(Level level){ //takes approx. 1 hour 15 minutes.
        log.info("Starting Stage 3");
        //For analysis of Stage 3 output, run Analysis.  This will summarize the sqft and the devType grid files.
        int numRows = Ref.TLUMIP_GRID_NROWS;
        int numCols = Ref.TLUMIP_GRID_NCOLS;
        GridFile devTypeGrid = null;
        GridFile sqftGrid=null;
        GridFile yrBuiltGrid=null;
        GridFile landUseGrid = null;
        GridFile alphaZoneGrid = null;
        try {
            devTypeGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/DEVTYPE.grid"));
            sqftGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/SQFT.grid"));
            yrBuiltGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/YRBUILT.grid"));
            landUseGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/LU9.grid"));
            alphaZoneGrid = GridFile.open(new File("c:/Project_Files/tlumip/grids/AZONE.grid"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Distribution3D d3d = new Distribution3D("YearBuilt"); //need a Dist3D object to make the year built choice for R and NR
        d3d.readData("c:/Project_Files/tlumip/input_files/YearBuilt.txt");
        try {
            for(int r=1;r<=numRows;r++){
                int[] lucDataRow = new int[numCols]; //temporary holding spot for the lucs read in from file and the outgoing devTypeCode
                int[] sqftDataRow = new int[numCols]; //temporary holding array for the SQFT data, must be an integer array because
                                                        //values are potentially bigger than 32000.
                int[] yrBltDataRow = new int[numCols];  //temporary holding spot for the yrBuilt assignment
                int[] aZoneDataRow = new int[numCols];
                landUseGrid.getRow(r,lucDataRow);  //lucDataRow now holds the lucs from the LandUse grid file.
                alphaZoneGrid.getRow(r,aZoneDataRow);
                for(int c=0;c<lucDataRow.length;c++){ //process each cell in the row before getting the next row
                    String glc=null;
                    int landUseCode = lucDataRow[c];
                    if(landUseCode==-1){//all cells in gridfile are initialized to -1 so devType, sqft, and yrbuilt will be -1.
                        continue;
                    }
                    glc = glcForLandUseCode[landUseCode];
                    if(glc==null){  //implies that the landUseCode is not one of our glcs so it has a pre-defined
                                    //devType that we should assign and go on to the next cell
                        if(undevelopedDevTypes[landUseCode]==32){
                            lucDataRow[c]=undevelopedDevTypes[landUseCode];
                            sqftDataRow[c]=(int)(Ref.SQFT_PER_GRIDCELL+0.5f);
                            yrBltDataRow[c]=1859;
                        }else{
                            lucDataRow[c]=undevelopedDevTypes[landUseCode];
                            sqftDataRow[c]=0;
                            yrBltDataRow[c]=-1;
                        }
                        continue;
                    }
                    int[] result = new int[3]; //result[0]=devTypeCode, result[1]=bldgSQFT , result[2]=YrBuilt
                    short devTypeCode=0;
                    int bldgSQFT=0; //might be bigger than the biggest short (31999)
                    short yrBuilt=0;
                    int azNum = aZoneDataRow[c];
                    //get the alpha zone object from the county hashmap that we want to work with.
                    String countyName = AlphaZone.getCountyName(azNum);//static method in AlphaZone class
                    County currCounty = ((County)countyMap.get(countyName));
                    ArrayList alphazones = currCounty.azones;
                    Iterator azIter = alphazones.iterator();
                    AlphaZone currAZ=null;
                    while(azIter.hasNext()){
                        currAZ = (AlphaZone)azIter.next();
                        if(currAZ.getNumber()==azNum) break;
                    }

                    GridCell.getAttributes(currCounty,currAZ,d3d,landUseCode,glc,matchCoefficients[landUseCode],result);
                    devTypeCode=(short)result[0];
                    bldgSQFT=result[1];
                    yrBuilt=(short)result[2];

                    lucDataRow[c]=devTypeCode; //after we read the luc, we can replace with the devTypeCode
                    sqftDataRow[c]=bldgSQFT;
                    yrBltDataRow[c]=yrBuilt;
                }//next column
                devTypeGrid.putRow(r,lucDataRow);
                sqftGrid.putRow(r,sqftDataRow);
                yrBuiltGrid.putRow(r,yrBltDataRow);
            }//next row
            devTypeGrid.close();
            sqftGrid.close();
            yrBuiltGrid.close();
            landUseGrid.close();
            alphaZoneGrid.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("End of Stage 3");
    }


    public static void main(String[] args) {
        log.info("\tstart time\t"+ new Date().toString());
        GridSynthesizer s12 = new GridSynthesizer();
        s12.runStage1(true);
        s12.runStage2a(true);
        s12.runStage2b(true);
        s12.runStage3(Level.parse("INFO"));
        log.info("\tend time\t"+ new Date().toString());
    }// end of main
}

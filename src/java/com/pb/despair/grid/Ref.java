package com.pb.despair.grid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Holds all of the static variable references to be used
 * for grid synthesis
 * @author  Christi Willison
 * @version Dec 17, 2003
 * Created by IntelliJ IDEA.
 */
public class Ref {
    //General references used in the com/pb/despair/grid package.
    public static final float SQFT_PER_GRIDCELL = 9712.5f;   // assumes a 30-meter grid cell
    public static final float SQFT_PER_ACRE = 43560.0f;
    public static final int NUM_ALPHA_ZONES = 4142; //max alphazone number + 1
    public static final int NUM_COUNTIES = 75; //don't need to index by county number so don't need +1
    public static final int NUM_LUCS = 816;//max landUseCode + 1
    public static final int TLUMIP_GRID_NROWS = 30141;
    public static final int TLUMIP_GRID_NCOLS = 26848;
    public static final double TLUMIP_GRID_XLL = 221891.972108;
    public static final double TLUMIP_GRID_YLL = -943468.337502;
    public static final double TLUMIP_GRID_CELLSIZE = 98.552015;

    //Static variable references used in AlphaZone class
    public static final String[] INTENSITIES = { "High", "Medium", "Low", "VeryLow" };
    public static final String[] RDEVTYPES={"MH","MF","AT","SFD","RRMH","RRSFD"};
    public static final String[] NRDEVTYPES={"Accom","Depot","GovSppt","Gschool","HvyInd","Hospital",
                                       "Inst","LtInd","Office","Retail","Whse"};
    // Typical floor area ratios for residential and non-residential development types.
    public static final double[][] TYPFAR =
             //  MH,   MF,  AT,  SFD, RRMH,RRSFD, Accom, Depot,GovSppt,Gschool,HvyInd,Hospital, Inst, LtInd,Office,Retail, Whse
            {{  .50, 2.00, .70, .370,  .10, .150,  5.00,  1.00,   3.00,    .30,  3.00,    5.00, 2.00,  3.00, 10.00,  3.00, 1.00},
             {.2430,1.380,.410,.2620,  .10, .150,2.3750, .7310,  1.6670,  .260, 1.250,    3.00,.4930,1.3820,1.6170, .8250,.7310},
             { .170, .770,.260, .160,  .10, .150, 1.690,  .520,   1.330,  .210,  .880,    2.00, .350,  .840,  .880,  .490, .520},
             { .130, .580, .20, .150,  .020, .030,  1.00,   .30,    1.00,  .150,   .50,    1.00,  .20,   .30,  .150,  .150,  .30}};
    public static final float RRMH_FAR = .02F; //used in Stage 2a when satisfying Rural demand with For/Ag/Vagfor (FAV)
    public static final float RRSFD_FAR = .03F; //used in Stage 2a when satisfying Rural demand with For/Ag/Vagfor (FAV)
    //  Maximum floor area ratios (FARs) for residential and non-residential land uses.
    public static final float[][] MAXFAR =
              // R  ,  NR
            { { 1.5F, 5.6F },  // High
              { 0.4F, 1.0F },  // Medium
              { 0.2F, 0.6F },  // Low
              { 0.2F, 0.3F } };  // Very low

    //Static variable references used in County class
    public static final String[] FORAGDEVTYPES={"FORFed","FORState","FORInd","FOROther","AG"};

    //Static variable references used in GridSynthesizer
    public static final String[] DEVTYPES={"MH","MF","AT","SFD","RRMH","RRSFD","Accom","Depot","GovSppt",
                              "Gschool","HvyInd","Hospital","Inst","LtInd","Office","Retail","Whse",
                              "FORFed","FORState","FORInd","FOROther","AG"};
    public static final short[] DEVTYPECODES={11,12,13,14,15,16,21,22,23,24,25,26,27,28,29,30,31,
                                              33,33,33,33,32};
    //Static variable references used in Stage 3
    public static final String[] UNDEVELOPEDDEVTYPES={"UNDEV","UNDEVFor","UNDEVAg","TP","WATER","OSA"};
    public static final short[] UNDEVELOPEDDEVTYPECODES={0,1,2,41,42,43};

    public static final String[] GLCS={ "R", "NR", "Vdev", "VTp", "Vagfor","AG","FOR", "FOROther"};
    //used as index into numCellsBorrowed that is used in Stage 2b.
    public static final String[] SUPPLYTOBORROW = {"VTp","Vdev","Vagfor","FOR","R","NR","FOROther","AG"};
    public static final String[] DEMANDTOSATISFY={"R","NR","AG","LOG","RRSFD","RRMH"};

    //Data structures used in Analyzer code to produce summaries
    public static final String[] SUMMARYDEVTYPES={"MH","MF","AT","SFD","RRMH","RRSFD","Accom","Depot","GovSppt",
                              "Gschool","HvyInd","Hospital","Inst","LtInd","Office","Retail","Whse",
                              "LOG","AG"};
    public static final short[] SUMMARYDEVTYPECODES={11,12,13,14,15,16,21,22,23,24,25,26,27,28,29,30,31,33,32};


    public static String getDemandType(String devType){
        String glc=null;
        int pos=getSubscript(devType,DEVTYPES);
        if(pos<RDEVTYPES.length) glc="R";
        else if(pos<NRDEVTYPES.length+RDEVTYPES.length) glc="NR";
        else if(pos<((FORAGDEVTYPES.length-1)+RDEVTYPES.length+NRDEVTYPES.length)) glc="LOG";
        else glc="AG";
        return glc;
    }

    public static String getDevType(short devTypeCode){
        int devTypePos = getPosition(devTypeCode,DEVTYPECODES);
        if(devTypePos == -1) {
            devTypePos=getPosition(devTypeCode,UNDEVELOPEDDEVTYPECODES);
            return UNDEVELOPEDDEVTYPES[devTypePos];
        }
        return DEVTYPES[devTypePos];
    }

    public static int getSubscript (String s, String[] subscriptType){
       int position=-1;
       for (int p=0;p<subscriptType.length;p++)
          if(subscriptType[p].equals(s)){
             position = p;
             break;
          }
       return position;
    }

    public static int getPosition (short x, short[] array){
        int position=-1;
        for (int p=0;p<array.length;p++)
          if(array[p]==x){
             position = p;
             break;
          }
       return position;
    }

    public static void main(String[] args) {
        Ref ref = new Ref();
        for(int i=0;i<DEVTYPES.length;i++){
            System.out.print(DEVTYPES[i]+" ");
            System.out.print(getDemandType(DEVTYPES[i]));
            System.out.println();

        }


    }


}

package com.pb.despair.grid.tests;

import com.pb.despair.grid.County;
import com.pb.despair.grid.AlphaZone;
import com.pb.despair.grid.GridSynthesizer;
import com.pb.despair.grid.Ref;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Method to output the results from Stage 2a
 * 
 * @author  Christi Willison
 * @version Dec 16, 2003
 * Created by IntelliJ IDEA.
 */
public class CheckStage2aResults {
    private static Logger log = Logger.getLogger("com/pb/despair/grid/tests");


    public static void checkStage2aResults(){
        Level level = Level.parse("SEVERE");

        //Look at a particular alpha zone by specifying the county and the alpha zone number
           ArrayList alphazones = ((County)GridSynthesizer.countyMap.get("Multnomah")).getAzones();
           Iterator azIter = alphazones.iterator();
           AlphaZone az=null;
           while(azIter.hasNext()){
               az = (AlphaZone)azIter.next();
               if(az.getNumber()==10) break;
           }
           log.log(level,"\tAlphaZone\t"+az.getNumber()+"\tLUI\t"+az.getLandUseIntensity());
           log.log(level,"\tR_SQFTdemand\t"+ az.getDemandInBldgSQFT("R"));
           log.log(level,"\tNR_SQFTdemand\t" + az.getDemandInBldgSQFT("NR"));
           String beginning = "\tFARs\t";
           String end = "";
           for(int i=0;i<az.getTypFARs().length;i++) end += az.getTypFARs()[i]+"\t";
           log.log(level,beginning + end);

           log.log(level,"\tMH\tMF\tAT\tSF\tRRMH\tRRSFD");
           end = "";
           for(int i=0;i<az.getCellsRequiredArray("R").length;i++) end += az.getCellsRequiredArray("R")[i]+"\t";
           log.log(level,"\t"+end);
           log.log(level,"\tAccom\tDepot\tGovSppt\tGschool\tHvyInd\tHosp\tInst\tLtInd\tOffice\tRetail\tWhse");
           end = "";
           for(int i=0;i<az.getCellsRequiredArray("NR").length;i++) end += az.getCellsRequiredArray("NR")[i]+"\t";
           log.log(level,"\t"+end);
           log.log(level,"\tR cells req "+ az.getCellsRequired("R"));
           log.log(level,"\tNR cells req " + az.getCellsRequired("NR"));
           log.log(level,"\tR supply "+GridSynthesizer.totalSupplyByGLC[az.getNumber()][Ref.getSubscript("R",Ref.GLCS)]);
           log.log(level,"\tNR supply "+GridSynthesizer.totalSupplyByGLC[az.getNumber()][Ref.getSubscript("NR",Ref.GLCS)]);
    }




    public static void main(String[] args) {
        GridSynthesizer s12 = new GridSynthesizer();
        checkStage2aResults();
        s12.runStage1(false);
        checkStage2aResults();
        s12.runStage2a(false);
        checkStage2aResults();
        s12.runStage2b(false);
        checkStage2aResults();
    }


}

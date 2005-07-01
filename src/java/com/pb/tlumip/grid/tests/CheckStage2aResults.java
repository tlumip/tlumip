package com.pb.tlumip.grid.tests;

import com.pb.tlumip.grid.County;
import com.pb.tlumip.grid.AlphaZone;
import com.pb.tlumip.grid.GridSynthesizer;
import com.pb.tlumip.grid.Ref;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Method to output the results from Stage 2a
 * 
 * @author  Christi Willison
 * @version Dec 16, 2003
 * Created by IntelliJ IDEA.
 */
public class CheckStage2aResults {
    private static Logger log = Logger.getLogger("com/pb/tlumip/grid/tests");


    public static void checkStage2aResults(){

        //Look at a particular alpha zone by specifying the county and the alpha zone number
           ArrayList alphazones = ((County)GridSynthesizer.countyMap.get("Multnomah")).getAzones();
           Iterator azIter = alphazones.iterator();
           AlphaZone az=null;
           while(azIter.hasNext()){
               az = (AlphaZone)azIter.next();
               if(az.getNumber()==10) break;
           }
           log.info("\tAlphaZone\t"+az.getNumber()+"\tLUI\t"+az.getLandUseIntensity());
           log.info("\tR_SQFTdemand\t"+ az.getDemandInBldgSQFT("R"));
           log.info("\tNR_SQFTdemand\t" + az.getDemandInBldgSQFT("NR"));
           String beginning = "\tFARs\t";
           String end = "";
           for(int i=0;i<az.getTypFARs().length;i++) end += az.getTypFARs()[i]+"\t";
           log.info(beginning + end);

           log.info("\tMH\tMF\tAT\tSF\tRRMH\tRRSFD");
           end = "";
           for(int i=0;i<az.getCellsRequiredArray("R").length;i++) end += az.getCellsRequiredArray("R")[i]+"\t";
           log.info("\t"+end);
           log.info("\tAccom\tDepot\tGovSppt\tGschool\tHvyInd\tHosp\tInst\tLtInd\tOffice\tRetail\tWhse");
           end = "";
           for(int i=0;i<az.getCellsRequiredArray("NR").length;i++) end += az.getCellsRequiredArray("NR")[i]+"\t";
           log.info("\t"+end);
           log.info("\tR cells req "+ az.getCellsRequired("R"));
           log.info("\tNR cells req " + az.getCellsRequired("NR"));
           log.info("\tR supply "+GridSynthesizer.totalSupplyByGLC[az.getNumber()][Ref.getSubscript("R",Ref.GLCS)]);
           log.info("\tNR supply "+GridSynthesizer.totalSupplyByGLC[az.getNumber()][Ref.getSubscript("NR",Ref.GLCS)]);
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

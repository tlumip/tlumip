package com.pb.tlumip.grid.tests;

import com.pb.tlumip.grid.County;
import com.pb.tlumip.grid.AlphaZone;
import com.pb.tlumip.grid.GridSynthesizer;
import com.pb.tlumip.grid.Ref;

import org.apache.log4j.Logger;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * 
 * 
 * @author  Christi Willison
 * @version Dec 16, 2003
 * Created by IntelliJ IDEA.
 */
public class CheckInputAndInitializations {

    private static Logger log = Logger.getLogger("com/pb/tlumip/grid/test");

    public static void checkInputDataAndInitializations(){
        new GridSynthesizer();
        log.info("\tCounty\tAZone\tLUI\tRcellsReq\tNRcellsReq\tRcellsReq\tNRcellsReq" +
                "\tLOGcellsReq\tAGcellsReq\tRcellsAvail\tNRcellsAvail\tVTpcellsAvail\t" +
                "VdevcellsAvail\tVagforcellsAvail");
        log.info("\t\t\t\t(using maxFARs)\t(using maxFARs)\t(using typFARs)\t(using typFARs)");
        Iterator c = GridSynthesizer.countyMap.keySet().iterator();
        while (c.hasNext()) {
            County currCounty = (County)GridSynthesizer.countyMap.get(c.next());
            Iterator aZIter = currCounty.getAzones().iterator();
            while (aZIter.hasNext()) {
                // Create aZIter pointer to the current alpha zone
                AlphaZone currAZ = ((AlphaZone)aZIter.next());
                int az = currAZ.getNumber();
                int R_cellsDem = currAZ.getMaxCellsRequired("R");
                int NR_cellsDem = currAZ.getMaxCellsRequired("NR");
                int R_cellsReq = currAZ.getCellsRequired("R");
                int NR_cellsReq = currAZ.getCellsRequired("NR");
                int LOG_cellsDem = currCounty.getCellsDemanded("LOG");
                int AG_cellsDem = currCounty.getCellsDemanded("AG");
                int RcellsAvail = GridSynthesizer.totalSupplyByGLC[az][Ref.getSubscript("R",Ref.GLCS)];
                int NRcellsAvail = GridSynthesizer.totalSupplyByGLC[az][Ref.getSubscript("NR",Ref.GLCS)];
                int VTpcellsAvail = GridSynthesizer.totalSupplyByGLC[az][Ref.getSubscript("VTp",Ref.GLCS)];
                int VdevcellsAvail = GridSynthesizer.totalSupplyByGLC[az][Ref.getSubscript("Vdev",Ref.GLCS)];
                int VagforcellsAvail = GridSynthesizer.totalSupplyByGLC[az][Ref.getSubscript("Vagfor",Ref.GLCS)];

                log.info("\t"+currCounty.getName()+"\t"+az+"\t"+currAZ.getLandUseIntensity()+
                         "\t"+R_cellsDem+"\t"+NR_cellsDem+"\t"+R_cellsReq+"\t"+NR_cellsReq+
                         "\t"+LOG_cellsDem+"\t"+AG_cellsDem+
                         "\t"+RcellsAvail+"\t"+NRcellsAvail+"\t"+VTpcellsAvail+
                         "\t"+VdevcellsAvail+"\t"+VagforcellsAvail);
            }
        }
            //Look at a particular alpha zone by specifying the county and the alpha zone number
           ArrayList alphazones = ((County)GridSynthesizer.countyMap.get("Multnomah")).getAzones();
           Iterator azIter = alphazones.iterator();
           AlphaZone az=null;
           while(azIter.hasNext()){
               az = (AlphaZone)azIter.next();
               if(az.getNumber()==1) break;
           }
           log.info("AlphaZone:"+az.getNumber()+", LUI:"+az.getLandUseIntensity());
           String beginning = "FARs:";
           String end = " ";
           for(int i=0;i<az.getTypFARs().length;i++) end += az.getTypFARs()[i]+"  ";
           log.info(beginning + end);
           log.info("R demand "+ az.getDemandInBldgSQFT("R"));
           log.info("NR demand " + az.getDemandInBldgSQFT("NR"));
           log.info("MH\tMF\tAT\tSF\tRRMH\tRRSFD\tTotalR");
           end = "";
           for(int i=0;i<az.getCellsRequiredArray("R").length;i++) end += az.getCellsRequiredArray("R")[i]+"     ";
            end+=az.getCellsRequired("R");
           log.info(end);
           log.info("Accom\tDepot\tGovSppt\tGschool\tHvyInd\tHosp\tInst\tLtInd\tOffice\tRetail\tWhse\tTotalNR");
           end = "";
           for(int i=0;i<az.getCellsRequiredArray("NR").length;i++) end += az.getCellsRequiredArray("NR")[i]+"     ";
        end+=az.getCellsRequired("NR");
           log.info(end);
           log.info("R cells req "+ az.getCellsRequired("R"));
           log.info("NR cells req " + az.getCellsRequired("NR"));


//            log.info("County name for AlphaZone 4074 should be Owyhee");
//            log.info("countyNameForAlphaZone[4074] is: "+ countyNameForAlphaZone[4074]);
//            log.info("and .getCountyName(4074) method returns: "+ getCountyName(4074));
//
//            log.info("Match coefficients for alphazone 2894 are: ");
//            end="";
//            for(int i=0;i<matchCoefficientsByDevType[567].length;i++) end += matchCoefficientsByDevType[567][i]+"     ";
//            log.info(end);
//            log.info("LCV 0 should have an unassigned devType of 43: "+unassignedDevTypes[0]);
//            log.info("LCV 131 should have an unassigned devType of 0: "+unassignedDevTypes[131]);
//
//            for(int i=0;i<glcForLandUseCode.length;i++)
//                if(glcForLandUseCode[i] != null)
//                    log.info("\t"+i+"\t"+glcForLandUseCode[i]);

        }//end of method

    public static void main(String[] args) {
        checkInputDataAndInitializations();
    }

}

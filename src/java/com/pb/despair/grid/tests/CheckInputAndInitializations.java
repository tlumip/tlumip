package com.pb.despair.grid.tests;

import com.pb.despair.grid.County;
import com.pb.despair.grid.AlphaZone;
import com.pb.despair.grid.GridSynthesizer;
import com.pb.despair.grid.Ref;

import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static Logger log = Logger.getLogger("com/pb/despair/grid/test");

    public static void checkInputDataAndInitializations(){
        GridSynthesizer s12 = new GridSynthesizer();
        Level level=Level.parse("SEVERE");
        log.log(level,"\tCounty\tAZone\tLUI\tRcellsReq\tNRcellsReq\tRcellsReq\tNRcellsReq" +
                "\tLOGcellsReq\tAGcellsReq\tRcellsAvail\tNRcellsAvail\tVTpcellsAvail\t" +
                "VdevcellsAvail\tVagforcellsAvail");
        log.log(level,"\t\t\t\t(using maxFARs)\t(using maxFARs)\t(using typFARs)\t(using typFARs)");
        Iterator c = s12.countyMap.keySet().iterator();
        while (c.hasNext()) {
            County currCounty = (County)s12.countyMap.get(c.next());
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
                int RcellsAvail = s12.totalSupplyByGLC[az][Ref.getSubscript("R",Ref.GLCS)];
                int NRcellsAvail = s12.totalSupplyByGLC[az][Ref.getSubscript("NR",Ref.GLCS)];
                int VTpcellsAvail = s12.totalSupplyByGLC[az][Ref.getSubscript("VTp",Ref.GLCS)];
                int VdevcellsAvail = s12.totalSupplyByGLC[az][Ref.getSubscript("Vdev",Ref.GLCS)];
                int VagforcellsAvail = s12.totalSupplyByGLC[az][Ref.getSubscript("Vagfor",Ref.GLCS)];

                log.log(level,"\t"+currCounty.getName()+"\t"+az+"\t"+currAZ.getLandUseIntensity()+
                         "\t"+R_cellsDem+"\t"+NR_cellsDem+"\t"+R_cellsReq+"\t"+NR_cellsReq+
                         "\t"+LOG_cellsDem+"\t"+AG_cellsDem+
                         "\t"+RcellsAvail+"\t"+NRcellsAvail+"\t"+VTpcellsAvail+
                         "\t"+VdevcellsAvail+"\t"+VagforcellsAvail);
            }
        }
            //Look at a particular alpha zone by specifying the county and the alpha zone number
           ArrayList alphazones = ((County)s12.countyMap.get("Multnomah")).getAzones();
           Iterator azIter = alphazones.iterator();
           AlphaZone az=null;
           while(azIter.hasNext()){
               az = (AlphaZone)azIter.next();
               if(az.getNumber()==1) break;
           }
           log.log(level,"AlphaZone:"+az.getNumber()+", LUI:"+az.getLandUseIntensity());
           String beginning = "FARs:";
           String end = " ";
           for(int i=0;i<az.getTypFARs().length;i++) end += az.getTypFARs()[i]+"  ";
           log.log(level,beginning + end);
           log.log(level,"R demand "+ az.getDemandInBldgSQFT("R"));
           log.log(level,"NR demand " + az.getDemandInBldgSQFT("NR"));
           log.log(level,"MH\tMF\tAT\tSF\tRRMH\tRRSFD\tTotalR");
           end = "";
           for(int i=0;i<az.getCellsRequiredArray("R").length;i++) end += az.getCellsRequiredArray("R")[i]+"     ";
            end+=az.getCellsRequired("R");
           log.log(level,end);
           log.log(level,"Accom\tDepot\tGovSppt\tGschool\tHvyInd\tHosp\tInst\tLtInd\tOffice\tRetail\tWhse\tTotalNR");
           end = "";
           for(int i=0;i<az.getCellsRequiredArray("NR").length;i++) end += az.getCellsRequiredArray("NR")[i]+"     ";
        end+=az.getCellsRequired("NR");
           log.log(level,end);
           log.log(level,"R cells req "+ az.getCellsRequired("R"));
           log.log(level,"NR cells req " + az.getCellsRequired("NR"));


//            log.log(level,"County name for AlphaZone 4074 should be Owyhee");
//            log.log(level,"countyNameForAlphaZone[4074] is: "+ countyNameForAlphaZone[4074]);
//            log.log(level,"and .getCountyName(4074) method returns: "+ getCountyName(4074));
//
//            log.log(level,"Match coefficients for alphazone 2894 are: ");
//            end="";
//            for(int i=0;i<matchCoefficientsByDevType[567].length;i++) end += matchCoefficientsByDevType[567][i]+"     ";
//            log.log(level,end);
//            log.log(level,"LCV 0 should have an unassigned devType of 43: "+unassignedDevTypes[0]);
//            log.log(level,"LCV 131 should have an unassigned devType of 0: "+unassignedDevTypes[131]);
//
//            for(int i=0;i<glcForLandUseCode.length;i++)
//                if(glcForLandUseCode[i] != null)
//                    log.log(level,"\t"+i+"\t"+glcForLandUseCode[i]);

        }//end of method

    public static void main(String[] args) {
        checkInputDataAndInitializations();
    }

}

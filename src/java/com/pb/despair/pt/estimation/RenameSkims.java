package com.pb.despair.pt.estimation;

/** 
 * Renaming skims
 *
 * @author    Steve Hansen
 * @version   1.0, may 17, 2004
 *
 */

import com.pb.common.util.ResourceUtil;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.matrix.MatrixType;

import java.util.logging.Logger;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.util.ResourceBundle;

public class RenameSkims {
    
     static Logger logger = Logger.getLogger("com.pb.despair.pt");    

     //emme2 matrix names
    public static String[] mfName = {"mf01", /*Peak Drive Time (pktime.zip)*/
                                    "mf02", /*Peak distance (pkdist.zip)*/
                                    "mf03", /*Off Peak Drive Time (optime.zip)*/
                                    "mf04", /*Off Peak Distance (opdist.zip)*/                                   
                                    "mf05", /*  Peak Best Walk-Transit In-vehicle Time */
                                    "mf06", /*  Peak Best Walk-Transit First Wait */
                                    "mf07", /*  Peak Best Walk-Transit Total Wait */
                                    "mf08", /*  Peak Best Walk-Transit Auxilary Transit (WALK) Time */
                                    "mf09", /*  Peak Best Walk-Transit Number Boardings */
                                    "mf10", /*  Peak Best Walk-Transit Fare (Batched-in and modified for salem, eug, med) */
                                    "mf15", /*  Off-Peak Best Walk-Transit In-vehicle Time */
                                    "mf16", /*  Off-Peak Best Walk-Transit First Wait */
                                    "mf17", /*  Off-Peak Best Walk-Transit Total Wait */
                                    "mf18", /*  Off-Peak Best Walk-Transit Auxilary Transit (WALK) Time */
                                    "mf19", /*  Off-Peak Best Walk-Transit Number Boardings */
                                    "mf20", /*  Off-Peak Best Walk-Transit Fare (Batched-in and modified for salem, eug, med) */
                                    "mf21", /*  Peak Best Drive-Transit In-vehicle Time */
                                    "mf22", /*  Peak Best Drive-Transit First Wait */
                                    "mf23", /*  Peak Best Drive-Transit Total Wait */
                                    "mf24", /*  Peak Best Drive-Transit Auxilary Transit (WALK) Time */
                                    "mf25", /*  Peak Best Drive-Transit Number Boardings */
                                    "mf26", /*  Peak Best Drive-Transit Drive Time */
                                    "mf10", /*  Peak Best Walk-Transit Fare (Batched-in and modified for salem, eug, med) */
                                    "mf31", /*  Off-Peak Best Drive-Transit In-vehicle Time */
                                    "mf32", /*  Off-Peak Best Drive-Transit First Wait */
                                    "mf33", /*  Off-Peak Best Drive-Transit Total Wait */
                                    "mf34", /*  Off-Peak Best Drive-Transit Auxilary Transit (WALK) Time */
                                    "mf35", /*  Off-Peak Best Drive-Transit Number Boardings */
                                    "mf36",
                                    "mf20", /*  Off-Peak Best Walk-Transit Fare (Batched-in and modified for salem, eug, med) */
                                    }; /*  Off-Peak Best Drive- Drive Time */

     //output compressed skims
    public static String[] mName = {"pktime",
                                    "pkdist",
                                    "optime",
                                    "opdist",
                                    "pwtivt",
                                    "pwtfwt",
                                    "pwttwt",
                                    "pwtaux",
                                    "pwtbrd",
                                    "pwtfar",
                                    "owtivt",
                                    "owtfwt",
                                    "owttwt",
                                    "owtaux",
                                    "owtbrd",
                                    "owtfar",
                                    "pdtivt",
                                    "pdtfwt",
                                    "pdttwt",
                                    "pdtwlk",
                                    "pdtbrd",
                                    "pdtdrv",
                                    "pdtfar",
                                    "odtivt",
                                    "odtfwt",
                                    "odttwt",
                                    "odtwlk",
                                    "odtbrd",
                                    "odtdrv",
                                    "odtfar"};
     

    
    public static void main (String[] args) throws FileNotFoundException, IOException {
        System.out.println("Importing matrices from databank.");
        ResourceBundle rb = ResourceUtil.getResourceBundle( "despair" );
        String pathName = ResourceUtil.getProperty(rb, "Model.skimPath");
        String writePathName = ResourceUtil.getProperty(rb,"Model.writeSkimPath");
            
        for(int i = 0; i<mfName.length;i++){
        	String readFileName = pathName+mName[i]+".zip";
            logger.info("Reading matrix: "+readFileName);
            MatrixReader mr = MatrixReader.createReader(MatrixType.ZIP,new File(readFileName));
            Matrix m = mr.readMatrix();
            m.setName(mName[i]);
            String writeFileName = writePathName+mName[i]+".zip";
            MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP,new File(writeFileName));
            logger.info("Writing matrix: "+writeFileName);
            mw.writeMatrix(m);            
        }

     }  //endmain
}     //end class

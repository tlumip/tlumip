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
package com.pb.tlumip.pt.estimation;

/** 
 * Imports emme2 matrices, Stores them in compressed matrix format.  
 *
 * @author    Joel Freedman
 * @version   1.0, 8/2001
 *
 */

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.matrix.MatrixType;
import com.pb.tlumip.pt.AlphaToBetaData;

import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.File;
import java.util.ResourceBundle;

public class ImportEmme2MatricesFromDatabank {
    
     static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
     static final int MAXTAZ=4141;
     static final int MAX_SEQUENTIAL_TAZ=2986;
     static final boolean CREATE_BETA_ZONES = false;
     static TableDataSet alphaToBeta;

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
     
    public static TableDataSet loadTableDataSet(ResourceBundle rb, String getProperty) {
        
        String tazToDistrictFile = "d:/tlumip_data/azonebzone";/*ResourceUtil.getProperty(rb, getProperty);*/
        
        try {
            String fileName = tazToDistrictFile + ".csv";
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(fileName));
            return table;
        } catch (IOException e) {
            logger.fatal("Can't find taz to district file");
            e.printStackTrace();
        }
        return null;
    }
    
    public static void main (String[] args) {
        System.out.println("Importing matrices from databank.");
        ResourceBundle rb = ResourceUtil.getResourceBundle( "pt" );
        String readPathName = ResourceUtil.getProperty(rb, "emme2reader.file");
        String writePathName = ResourceUtil.getProperty(rb, "emme2writer.file");
        MatrixReader mr = MatrixReader.createReader(MatrixType.EMME2,new File(readPathName));
        
            
    for(int i = 0; i<mfName.length;i++){
        System.out.println("Importing  "+mName[i]);        
        logger.info("Adding table "+mName[i]);

        Matrix alphaZoneMatrix = mr.readMatrix(mfName[i]);
        alphaZoneMatrix.setName(mName[i]);
        String fullPath = writePathName+mName[i]+".zip";
        MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP,new File(fullPath));
        mw.writeMatrix(alphaZoneMatrix);
        
        if(CREATE_BETA_ZONES){
            AlphaToBetaData aToB = new AlphaToBetaData();
            alphaToBeta = loadTableDataSet(rb, "alphatobeta");
            Matrix betaZoneMatrix = aToB.getSqueezedMatrix(alphaToBeta,alphaZoneMatrix,"MEAN");
            String betaPath = "beta"+writePathName+mName[i]+".zip";
            MatrixWriter betamw = MatrixWriter.createWriter(MatrixType.ZIP,new File(betaPath));
            betamw.writeMatrix(betaZoneMatrix);
        }
        
    }

     }  //endmain
}     //end class

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
package com.pb.tlumip.pi.test;

import java.io.File;

import org.apache.log4j.Logger;

import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;

/**
 * This code will read in a buying_$commodityName.zipMatrix which
 * has beta to beta flows and squeeze it into a county by county
 * matrix.
 * 
 * Created on Aug 16, 2005 
 * @author Christi
 */
public class CountySqueeze {
    protected static Logger logger = Logger.getLogger(CountySqueeze.class);

    public static void main(String[] args) {
        File a2bFile = new File("/models/tlumip/scenario_aaaCurrentData/reference/alpha2beta.csv");
        AlphaToBeta a2bMap = new AlphaToBeta(a2bFile,"Bzone","FIPS");
        
        File buyComm = new File("/models/tlumip/scenario_aaaCurrentData/t1/pi/buying_SCTG_01.zipMatrix");
        MatrixReader buyReader = MatrixReader.createReader(MatrixType.ZIP, buyComm);
        Matrix buyMatrix = buyReader.readMatrix();
        
        MatrixCompression compressor = new MatrixCompression(a2bMap);
        Matrix countySqueeze = compressor.getCompressedMatrix(buyMatrix,"SUM");
        
        File output = new File("/models/tlumip/scenario_aaaCurrentData/t1/pi/CountyFlows_Value_SCTG01.zip");
        MatrixWriter writer = MatrixWriter.createWriter(MatrixType.ZIP, output);
        writer.writeMatrix(countySqueeze);
        
        logger.info("SCTG_01: Buying");
        logger.info("\tBetaZone 4005 Row Sum, " + buyMatrix.getRowSum(4005));
        logger.info("\tBetaZone 4005 Col Sum, " + buyMatrix.getColumnSum(4005));
        logger.info("");
        logger.info("\tBetaZone 4017 Row Sum, " + buyMatrix.getRowSum(4017));
        logger.info("\tBetaZone 4017 Col Sum, " + buyMatrix.getColumnSum(4017));
        logger.info("");
        logger.info("\t4005RowSum + 4017RowSum, " + (buyMatrix.getRowSum(4005)+buyMatrix.getRowSum(4017)));
        logger.info("\t4005ColSum + 4017ColSum, " + (buyMatrix.getColumnSum(4005)+buyMatrix.getColumnSum(4017)));
        logger.info("");
        logger.info("\tCounty 6015 Row Sum, " + countySqueeze.getRowSum(6015));
        logger.info("\tCounty 6015 Col Sum, " + countySqueeze.getColumnSum(6015));
        logger.info("");
        logger.info("");
        File sellComm = new File("/models/tlumip/scenario_aaaCurrentData/t1/pi/selling_SCTG_01.zipMatrix");
        MatrixReader sellReader = MatrixReader.createReader(MatrixType.ZIP, sellComm);
        Matrix sellMatrix = sellReader.readMatrix();
        logger.info("SCTG_01: Selling");
        logger.info("\tBetaZone 4005 Row Sum, " + sellMatrix.getRowSum(4005));
        logger.info("\tBetaZone 4005 Col Sum, " + sellMatrix.getColumnSum(4005));
        logger.info("");
        logger.info("\tBetaZone 4017 Row Sum, " + sellMatrix.getRowSum(4017));
        logger.info("\tBetaZone 4017 Col Sum, " + sellMatrix.getColumnSum(4017));
        logger.info("");
    }
}

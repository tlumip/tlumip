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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;

/**
 * 
 * 
 * Created on Aug 12, 2005 
 * @author Christi
 */
public class IntrazonalPercentFileTest {
    protected static Logger logger = Logger.getLogger(IntrazonalPercentFileTest.class);

    public static void main(String[] args) {
        File buyComm = new File("/models/tlumip/scenario_aaaCurrentData/t1/pi/buying_SCTG01.zipMatrix"); 
        File sellComm = new File("/models/tlumip/scenario_aaaCurrentData/t1/pi/selling_SCTG01.zipMatrix");
        
        MatrixReader buyReader = MatrixReader.createReader(MatrixType.ZIP, buyComm);
        Matrix buyMatrix = buyReader.readMatrix();
        
        MatrixReader sellReader = MatrixReader.createReader(MatrixType.ZIP, sellComm);
        Matrix sellMatrix = sellReader.readMatrix();
        
        File fileName = null;
        try {            
            fileName = new File("/models/tlumip/scenario_aaaCurrentData/t1/pi/PctIntrazonalxCommodityxBzone.csv");
            
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
            writer.println("Bzone,Commodity,BuyIntra,BuyFrom,BuyTo,SellIntra,SellFrom,SellTo");
            for(int i=0; i<buyMatrix.getRowCount(); i++){
                int betaZone = buyMatrix.getExternalNumber(i);
                writer.print(betaZone + ",");
                writer.print("SCTG01,");
                writer.print(buyMatrix.getValueAt(betaZone,betaZone)+",");
                writer.print(buyMatrix.getSum()+",");
                writer.print(buyMatrix.getSum() + ",");
                writer.print(sellMatrix.getValueAt(betaZone,betaZone)+",");
                writer.print(sellMatrix.getSum()+",");
                writer.println(sellMatrix.getSum());
            }
            writer.close();
        } catch (IOException e) {
            logger.fatal("Could not open file " + fileName + " for writing\n");
            System.exit(1);
        }
    }
}

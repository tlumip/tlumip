/*
 * Copyright 2006 PB Consult Inc.
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
package com.pb.tlumip.pt.daf;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.models.pt.daf.HouseholdWorker;
import com.pb.tlumip.pt.PTOccupation;

import java.io.File;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Dec 12, 2006
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class TLUMIPHouseholdWorker extends HouseholdWorker {

    public void onStart(){
        super.onStart(PTOccupation.NONE);
    }

    public static void main(String[] args) {
        HouseholdWorker worker = new TLUMIPHouseholdWorker();
        worker.onStart();



        Matrix logsums = MatrixReader.readMatrix(new File("/models/tlumip/scenario_90All_PTRefactor/t1/pt/w0mcls.zmx"), null);
        Matrix distance = skims.pkDist;
        Matrix propensity = laborFlows.calculatePropensityMatrix(logsums, distance);
        writeMatrixForDebug(propensity, "Prop", ".zmx");

//        ColumnVector production = laborFlows.alphaProduction[PTOccupation.MANAGER.ordinal()];
//        ptLogger.info("Value in zone 4299 is " + production.getValueAt(4299));
//        ptLogger.info("Should be 23395300");
//        RowVector consumption = laborFlows.alphaConsumption[PTOccupation.MANAGER.ordinal()];
//        ptLogger.info("Value in zone 4299 is " + consumption.getValueAt(4299));
//        ptLogger.info("Should be 8203550");
//        ptLogger.info("Value in selling_Managers matrix is " + laborFlows.betaLaborFlows.getMatrix("MANAGER").getValueAt(183,10));
//        ptLogger.info("Should be 1166.95");
//        ptLogger.info("Value in selling_Managers matrix is " + laborFlows.betaLaborFlows.getMatrix("MANAGER").getValueAt(183,409));
//        ptLogger.info("Should be 10819859");
//        ptLogger.info("Value in distance matrix from 4299 to 2155 is " + distance.getValueAt(4299,2155));
//        ptLogger.info("Should be 5.49");
//        ptLogger.info("Value in mc logsum matrix from 4299 to 2155 is " + logsums.getValueAt(4299,2155));
//        ptLogger.info("Should be 5.49");
//
//        Matrix flows = laborFlows.calculateAlphaLaborFlowsMatrix(logsums, distance,
//                0, PTOccupation.MANAGER);
//        writeMatrixForDebug(flows,"TAZFlows_", "0.zmx");
//
//        float rowSum = 0.0f;
//        for(int r=1; r < flows.getRowCount(); r++){
//            rowSum = flows.getRowSum(flows.getExternalNumber(r));
//            if(rowSum != 1.0f){
//                ptLogger.info("Zone " + flows.getExternalNumber(r) + " probabilities" +
//                        " don't add up to one: " + rowSum);
//            }
//        }
    }

    public static void writeMatrixForDebug(Matrix m, String prefix, String suffix){
        File mFile = new File(debugDirPath + prefix + m.getName() + suffix);
        MatrixWriter mWriter = MatrixWriter.createWriter(MatrixType.ZIP, mFile);
        mWriter.writeMatrix(m);

    }
}

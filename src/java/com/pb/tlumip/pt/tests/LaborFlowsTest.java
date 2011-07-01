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
package com.pb.tlumip.pt.tests;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetLoader;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.PTOccupationReferencer;
import com.pb.models.pt.tests.PTOccupation;
import com.pb.models.reference.IndustryOccupationSplitIndustryReference;
import com.pb.tlumip.pt.LaborFlows;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ResourceBundle;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Oct 15, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class LaborFlowsTest {

    protected static Logger logger = Logger.getLogger(LaborFlowsTest.class);

    public static void main(String args[]) {
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");

//        IndustryOccupationSplitIndustryReference indOccRef =
//                new IndustryOccupationSplitIndustryReference( ResourceUtil.getProperty(globalRb,
//                "industry.occupation.to.split.industry.correspondence"));
        IndustryOccupationSplitIndustryReference indOccRef = new IndustryOccupationSplitIndustryReference(IndustryOccupationSplitIndustryReference.getSplitCorrespondenceFilepath(globalRb));


        String[] occupationLabels = indOccRef.getOccupationLabelsByIndex();

        PTOccupationReferencer occReferencer = PTOccupation.NO_OCCUPATION;

        LaborFlows laberFlows = new LaborFlows(globalRb, rb, occReferencer);
        logger.info("Reading alphaZone to betaZone mapping.");
        String fileName = ResourceUtil.getProperty(globalRb, "alpha2beta.file");
        String alphaName = globalRb.getString("alpha.name");
        String betaName = globalRb.getString("beta.name");
        logger.info("Creating alphaZone to betaZone mapping using " + fileName);

        AlphaToBeta alphaToBeta = new AlphaToBeta(new File(fileName), alphaName, betaName);
        laberFlows.setZoneMap(alphaToBeta);

        logger.info("Reading alpha production and consumption numbers.");
        TableDataSet productionValues = TableDataSetLoader.loadTableDataSet(rb,
                "productionValues.file");
        TableDataSet consumptionValues = TableDataSetLoader.loadTableDataSet(
                rb, "consumptionValues.file");
        // logger.info("Reading betaZone labor flows");
        // TableDataSet betaLaborFlowsTable =
        // loadTableDataSet("pt","betaFlows.file");

        logger.info("Put production and consumption numbers by alphaZone "
                + "into MatrixCollection alphaProductionAndConsumption.");
        laberFlows.readAlphaValues(productionValues, consumptionValues);
        logger.info("Put betaZone labor flows into MatrixCollection"
                + " betaLaborFlows.");
        laberFlows.readBetaLaborFlows(2000, occupationLabels);

        String path = ResourceUtil.getProperty(rb, "mcLogsum.path");

        String distanceMatrix = ResourceUtil.getProperty(rb, "skims.path");
        distanceMatrix = distanceMatrix + "carPkDist.zip";
        MatrixReader mr = MatrixReader.createReader(MatrixType.ZIP,
                new File(distanceMatrix));
        Matrix d = mr.readMatrix(distanceMatrix);

        // Loop through each work market segment
        for (int segment = 1; segment < 2; segment++) {

            logger.info("Reading work MC logsums for segment " + segment);
            mr = MatrixReader.createReader(MatrixType.ZIP,
                    new File(path + "w" + segment + "ls.zip"));
            Matrix m = mr.readMatrix(path + "w" + segment + "ls.zip");
            laberFlows.calculatePropensityMatrix(m,d);
            // Loop through each occupation type
            for (int occupation = 1; occupation <= 5; occupation++) {
                // lf.calculateAlphaLaborFlowsMatrix(m,
                // segment, occupation);
                // lf.writeLaborFlowProbabilities(lfMatrix);
            }
        }
        // logger.info("SizeOf(laborFlowProbabilities) = " + ObjectUtil.sizeOf(
        // laborFlowProbabilities ) + " bytes");
    }
}


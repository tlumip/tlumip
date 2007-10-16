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
package com.pb.tlumip.pt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetLoader;
import com.pb.common.math.MathUtil;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCollection;
import com.pb.common.matrix.MatrixException;
import com.pb.common.matrix.MatrixExpansion2;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.RowVector;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.PTOccupationReferencer;
import com.pb.models.utils.Tracer;
import com.pb.tlumip.model.WorldZoneExternalZoneUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Oct 15, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class LaborFlows {

    static Logger logger = Logger.getLogger(LaborFlows.class);

    private Tracer tracer = Tracer.getTracer();

    private AlphaToBeta alphaToBeta;

    public MatrixCollection betaLaborFlows = new MatrixCollection();

    public ColumnVector[] alphaProduction;

    public RowVector[] alphaConsumption;

    // this is the default value but it is also set in the properties file
    private double dispersionParameter = 0.54;
    private double distance_0_5 = 0.0;
    private double distance_5_15 = 0.0;
    private double distance_15_30 = 0.0;
    private double distance_30_50 = 0.0;
    private double distance_50Plus = 0.0;
    private double intrazonalParameter = 0.0;

    private PTOccupationReferencer occRef;
    private WorldZoneExternalZoneUtil wzUtil;

    String matrixFormat;
    ResourceBundle rb;

    public LaborFlows(ResourceBundle globalRb, ResourceBundle appRb, PTOccupationReferencer occRef) {
        rb = appRb;
        dispersionParameter = Double.parseDouble(ResourceUtil.getProperty(rb,
                "sdt.labor.flow.dispersion.parameter"));
        distance_0_5 = Double.parseDouble(ResourceUtil.getProperty(rb,
        "sdt.labor.flow.distance_0_5.parameter"));
        distance_5_15 = Double.parseDouble(ResourceUtil.getProperty(rb,
        "sdt.labor.flow.distance_5_15.parameter"));
        distance_15_30 = Double.parseDouble(ResourceUtil.getProperty(rb,
        "sdt.labor.flow.distance_15_30.parameter"));
        distance_30_50 = Double.parseDouble(ResourceUtil.getProperty(rb,
        "sdt.labor.flow.distance_30_50.parameter"));
        distance_50Plus = Double.parseDouble(ResourceUtil.getProperty(rb,
        "sdt.labor.flow.distance_50Plus.parameter"));
        intrazonalParameter = Double.parseDouble(ResourceUtil.getProperty(rb,
        "sdt.labor.flow.intrazonal.parameter"));

        File file = new File(ResourceUtil.getProperty(globalRb, "alpha2beta.file"));
        String alphaName = globalRb.getString("alpha.name");
        String betaName = globalRb.getString("beta.name");
        CSVFileReader reader = new CSVFileReader();
        try {
            TableDataSet table = reader.readFile(file);
            alphaToBeta = new AlphaToBeta(table, alphaName, betaName);
        } catch (IOException e) {
            String msg = "Unable to read " + file.getAbsolutePath();
            logger.fatal(msg);
            throw new RuntimeException(msg);
        }

        this.occRef = occRef;
        matrixFormat = globalRb.getString("matrix.extension");

        this.wzUtil = new WorldZoneExternalZoneUtil(globalRb);
    }

    /**
     * Call this method before reading alpha matrices.
     *
     * Why not pass the taz manager?
     *
     * @param alphaToBeta
     *            taz to amz table
     */
    public void setZoneMap(AlphaToBeta alphaToBeta) {
        this.alphaToBeta = alphaToBeta;
    }

    /**
     * Create a travel propensity matrix p from the logsum matrix m and distance matrix d as:
     *
     * p(i,j) = exp[dispersionParameter * m(i,j) + distanceParameter_d*d(0,1) + intrazonalParameter*i(0,1)]
     *
     * where d =1 if distance in distance range d and 0 if not
     *       i =1 if intrazonal interchange and 0 if not
     *
     * @param m
     *            A matrix, presumably of mode choice logsums.
     * @param d  distance Skim matrix?
     * @return The propensity matrix.
     */
    public Matrix calculatePropensityMatrix(Matrix m, Matrix d) {
        logger.debug("Set travel propensity Matrix.");

        Matrix alphaPropensityMatrix = new Matrix(m.getRowCount(), m
                .getColumnCount());
        String name = m.getName();
        int lsIndex = name.indexOf("ls");
        name = name.substring(0, lsIndex) + "propensity";
        alphaPropensityMatrix.setName(name);
        alphaPropensityMatrix.setExternalNumbers(m.getExternalNumbers());
        int origin;
        int destination;
        float propensity;

        for (int i = 0; i < m.getRowCount(); i++) {
            for (int j = 0; j < m.getColumnCount(); j++) {
                origin = m.getExternalNumber(i);
                destination = m.getExternalNumber(j);
                double intrazonal = 0.0;
                double distanceParameter = 0.0;

                float distance = d.getValueAt(origin,destination);
                if(distance<5)
                    distanceParameter =  distance_0_5;
                else if(distance<15)
                    distanceParameter =  distance_5_15;
                else if(distance<30)
                    distanceParameter =  distance_15_30;
                else if(distance<50)
                    distanceParameter =  distance_30_50;
                else
                    distanceParameter =  distance_50Plus;

                if(origin==destination)
                    intrazonal=intrazonalParameter;

                propensity = (float) MathUtil.exp(dispersionParameter
                        * m.getValueAt(origin, destination)
                        + distanceParameter
                        + intrazonal);

                if (tracer.isTraceZonePair(origin, destination)) {
                    logger.info("Trace of " + name
                            + " calculation for zone pair " + origin + ", "
                            + destination + ": " + propensity + " = exp("
                            + dispersionParameter + " * "
                            + m.getValueAt(origin, destination)
                            + " + " + distanceParameter + ")"
                            + " + " + intrazonal + ")");
                }

                alphaPropensityMatrix.setValueAt(origin, destination,
                        propensity);
            }
        }
        return alphaPropensityMatrix;
    }

    public void readBetaLaborFlows(int baseYear, String[] occupations) {
        String path = ResourceUtil.getProperty(rb, "pi.beta.labor.flows");
        String suffix = ResourceUtil.getProperty(rb, "matrix.extension");

        logger.info("Reading labor flow matrices.");
        for (String occupation : occupations) {

            if (occupation.startsWith("No Occupation") || occupation.startsWith("0_NoOccupation")) {
                continue;
            }

            File file = new File(path + occupation + suffix);

            Matrix matrix = MatrixReader.readMatrix(file, occupation);
            matrix = matrix.getSubMatrix(alphaToBeta.getBetaExternals1Based());

            matrix.setName(occRef.getOccupation(occupation).name());
            logger.info("Reading labor flows in from " + file.getAbsolutePath() +
                        " and adding to betaLaborFlows as " + matrix.getName());
            betaLaborFlows.addMatrix(matrix);
        }
    }

    public void readAlphaValues() {
        TableDataSet consumption = TableDataSetLoader.loadTableDataSet(rb,
                "pecas.consumption.sum");
        TableDataSet production = TableDataSetLoader.loadTableDataSet(rb,
                "pecas.production.sum");

        readAlphaValues(production, consumption);
    }

    public void readAlphaValues(TableDataSet production,
            TableDataSet consumption) {
        logger.info("Reading alpha values");
        int taz;

        int tazColumn = production.getColumnPosition("zoneNumber");

        // create matrix collections tailored to the alpha tables
        String[] labels = production.getColumnLabels();
        //Getting the first occupation enum is arbitrary - just have to get one of them so that
        //you can use the getClass.getEnumConstants method.
        alphaProduction = new ColumnVector[occRef.getOccupation(1).getClass().getEnumConstants().length];
        alphaConsumption = new RowVector[occRef.getOccupation(1).getClass().getEnumConstants().length];

        // index off of one to avoid the zoneNumber column
        for (int i = 1; i < labels.length; ++i) {
            Enum occupation = occRef.getOccupation(labels[i]);
            int j = occupation.ordinal();

            logger.info("Adding labor production / consumption totals "
                    + occupation.name());
            alphaProduction[j] = new ColumnVector(alphaToBeta.alphaSize());
            alphaProduction[j].setName(occupation.name());
            alphaProduction[j].setExternalNumbers(alphaToBeta.getAlphaExternals1Based());

            alphaConsumption[j] = new RowVector(alphaToBeta.alphaSize());
            alphaConsumption[j].setName(occupation.name());
            alphaConsumption[j].setExternalNumbers(alphaToBeta.getAlphaExternals1Based());
        }

        // convert the TableDataSet to ColumnVectors
        for (int rowNumber = 1; rowNumber <= production.getRowCount(); rowNumber++) {
            taz = (int) production.getValueAt(rowNumber, tazColumn);

            if (!wzUtil.isWorldZone(taz)) {
                for (int col = 2; col <= production.getColumnCount(); ++col) {
                    String label = production.getColumnLabel(col);
                    Enum occupation = occRef.getOccupation(label);

                    try {
                        alphaProduction[occupation.ordinal()].setValueAt(taz,
                                production.getValueAt(rowNumber, col));
                    } catch (Exception e) {
                        String msg = "Problems finding " + label + ", aka "
                                + occupation.name() + ", in row " + rowNumber
                                + ", aka taz " + taz;
                        logger.fatal(msg);

                        throw new RuntimeException(msg + "\n", e);
                    }
                }
            }
        }

        // convert the TableDataSet to RowVectors
        for (int rowNumber = 1; rowNumber <= consumption.getRowCount(); rowNumber++) {
            taz = (int) consumption.getValueAt(rowNumber, tazColumn);
            for (int col = 2; col <= consumption.getColumnCount(); ++col) {
                String label = consumption.getColumnLabel(col);
                Enum occupation = occRef.getOccupation(label);

                alphaConsumption[occupation.ordinal()].setValueAt(taz,
                        consumption.getValueAt(rowNumber, label));
            }
        }
    }

    public Matrix calculateAlphaLaborFlowsMatrix(Matrix mcLogsum, Matrix distance, int segment,
            Enum occupation) {
        MatrixExpansion2 me = new MatrixExpansion2(alphaToBeta);
        me.setPropensityMatrix(calculatePropensityMatrix(mcLogsum,distance));


        Matrix expandedLaborFlows = betaLaborFlows.getMatrix(occupation.name());

        logger.debug("Setting the beta flow.");
        me.setBetaFlowMatrix(expandedLaborFlows);

        logger.debug("Setting the consumption.");
        me.setConsumptionMatrix(alphaConsumption[occupation
                .ordinal()]);

        try {
            logger.debug("Setting the production.");
            me.setProductionMatrix(alphaProduction[occupation
                    .ordinal()]);
        } catch (MatrixException e) {
            Matrix matrix = alphaProduction[occupation.ordinal()];
            logger.fatal("Matrix " + matrix.getName() + " is "
                    + matrix.getRowCount() + " x " + matrix.getColumnCount());
            throw e;
        } catch (Exception e) {
            String msg = "Unhandled exception calculating alpha flows.";
            logger.fatal(msg);
            throw new RuntimeException(msg, e);
        }

        me.setAlphaFlowMatrix(alphaToBeta);

        Matrix alphaFlow = me.getAlphaFlowMatrix();
        String name = "TAZLaborFlow_" + occupation.name() + segment + matrixFormat;
        me.setProbabilityMatrix(alphaFlow, name);

        return me.getAlphaFlowProbabilityMatrix();
    }

    public Matrix calculateAlphaLaborFlowsMatrix(Matrix propensity, Matrix mcLogsum, Matrix distance, int segment,
            Enum occupation) {
        MatrixExpansion2 me = new MatrixExpansion2(alphaToBeta);
        me.setPropensityMatrix(propensity);


        Matrix expandedLaborFlows = betaLaborFlows.getMatrix(occupation.name());

        logger.debug("Setting the beta flow.");
        me.setBetaFlowMatrix(expandedLaborFlows);

        logger.debug("Setting the consumption.");
        me.setConsumptionMatrix(alphaConsumption[occupation
                .ordinal()]);

        try {
            logger.debug("Setting the production.");
            me.setProductionMatrix(alphaProduction[occupation
                    .ordinal()]);
        } catch (MatrixException e) {
            Matrix matrix = alphaProduction[occupation.ordinal()];
            logger.fatal("Matrix " + matrix.getName() + " is "
                    + matrix.getRowCount() + " x " + matrix.getColumnCount());
            throw e;
        } catch (Exception e) {
            String msg = "Unhandled exception calculating alpha flows.";
            logger.fatal(msg);
            throw new RuntimeException(msg, e);
        }

        me.setAlphaFlowMatrix(alphaToBeta);

        Matrix alphaFlow = me.getAlphaFlowMatrix();
        String name = "TAZLaborFlow_" + occupation.name() + segment + matrixFormat;
        me.setProbabilityMatrix(alphaFlow, name);

        return me.getAlphaFlowProbabilityMatrix();
    }

    /**
     * Log the total values across columns for each row in the matrix.
     *
     * @param m
     *            The matrix to debug.
     */
    public void debugMatrix(Matrix m) {
        for (int r = 0; r < m.getRowCount(); r++) {
            double rowSum = m.getSum();
            if (rowSum < 1)
                logger.info("rowsum for occupation " + m.getName() + " taz "
                        + m.getExternalNumber(r) + " = " + rowSum);
        }
    }

}

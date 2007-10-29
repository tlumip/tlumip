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
package com.pb.tlumip.et;

import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.FileType;
import com.pb.common.datafile.TableDataFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixBalancerRM;
import com.pb.common.matrix.RowVector;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.model.WorldZoneExternalZoneUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ResourceBundle;


/**
 * EEModel is a class that generates the Thru trips for the ET model.
 *
 * @author Kimberly Grommes
 * @version 1.0, Jul 3, 2007
 *          Created by IntelliJ IDEA.
 */
public class EEModel {

    Logger logger = Logger.getLogger(EEModel.class);

    private ResourceBundle appRb;
    private ResourceBundle globalRb;

    private Matrix mtxSeed;
    private Matrix mtxOutput;

    private RowVector rvColumnTargets;
    private ColumnVector cvRowTargets;

    private double dblRelativeError;
    private int intMaxIterations;

    private Double dblLargeRoadGrowthRate;
    private Double dblSmallRoadGrowthRate;

    private ArrayList alLargeRoads;
    //private ArrayList alSmallRoads;

    private int[] intsExternalStations;
    
    private int intTimeInterval;
    private ExternalStationParameters externalStationParameters;
   String strMatrixExtension;
    
    public EEModel(ResourceBundle appRb, ResourceBundle globalRb, int intTimeInterval, ExternalStationParameters externalStationParameters) {
        this.appRb = appRb;
        this.globalRb = globalRb;
        this.intTimeInterval = intTimeInterval;
        this.externalStationParameters = externalStationParameters;
    }

    public void runModel(ArrayList<ShipmentDetail> alTrucks) {

        defineModel();

        String[] strsTripTypes = ResourceUtil.getArray(appRb, "EE.trip.types");

        for (String strTripType: strsTripTypes) {
            defineInputs(strTripType);

            MatrixBalancerRM mbBalancer = new MatrixBalancerRM(mtxSeed, cvRowTargets, rvColumnTargets,
                    dblRelativeError, intMaxIterations, MatrixBalancerRM.ADJUST.BOTH_USING_AVERAGE);
            this.mtxOutput = mbBalancer.balance();

            addShipments(strTripType, alTrucks);

            //writeMatrix(mtxOutput, strTripType);
            //outputDiagnosticFile(strTripType);
            outputFile(strTripType);
        }

    }


    private void defineModel() {

        dblRelativeError = ResourceUtil.getDoubleProperty(appRb, "ee.relative.error");
        intMaxIterations = ResourceUtil.getIntegerProperty(appRb, "ee.max.iterations");
        strMatrixExtension = ResourceUtil.getProperty(globalRb, "matrix.extension");

        WorldZoneExternalZoneUtil wzUtil = new WorldZoneExternalZoneUtil(globalRb);
        intsExternalStations = wzUtil.getExternalZonesForET();

        dblLargeRoadGrowthRate = ResourceUtil.getDoubleProperty(appRb, "large.road.growth.rate");
        dblSmallRoadGrowthRate = ResourceUtil.getDoubleProperty(appRb, "small.road.growth.rate");

        alLargeRoads = ResourceUtil.getList(appRb, "large.roads");
        //alSmallRoads = ResourceUtil.getList(appRb, "small.roads");

    }

    private void defineInputs(String strTripType) {

        defineSeed(strTripType);
        defineTargets(strTripType);

    }

    private void addShipments(String strTripType, ArrayList<ShipmentDetail> alTrucks) {

        String strStartTime;
        int intTruckClass;

        if (strTripType.startsWith("AM")) {
            strStartTime = ResourceUtil.getProperty(globalRb, "am.peak.start");
        } else {
            strStartTime = ResourceUtil.getProperty(globalRb, "offpeak.start");
        }

        if (strTripType.endsWith("LT")) {
            intTruckClass = ResourceUtil.getIntegerProperty(appRb, "LT.truck.class");
        } else {
            intTruckClass = ResourceUtil.getIntegerProperty(appRb, "HT.truck.class");
        }

        for (int intOrigin: intsExternalStations) {
            for (int intDestination: intsExternalStations) {

                float fltValue = mtxOutput.getValueAt(intOrigin, intDestination);
                alTrucks.add(new ShipmentDetail(strTripType, intOrigin, intDestination,
                        strStartTime, fltValue, intTruckClass));

            }
        }
    }

    private void defineSeed(String strTripType) {

        String strInputFileName =  ResourceUtil.getProperty(appRb, "EE.file.prefix") + strTripType + ".csv";
        String strInputPath = ResourceUtil.getProperty(appRb, "et.base.data");

        TableDataSet tblTripData;

        TableDataFileReader rdrReader = TableDataFileReader.createReader(FileType.CSV);
        try {
            tblTripData = rdrReader.readFile(new File(strInputPath + strInputFileName));
        } catch (IOException e) {
            tblTripData = null;
            logger.error(e);
            System.exit(-1);
        }
        tblTripData.buildIndex(1);

        float[][] fltsTripData = new float[intsExternalStations.length][intsExternalStations.length];
        for (int r = 0; r < intsExternalStations.length; r++) {
            for (int c = 0; c < intsExternalStations.length; c++) {
                float fltValue = tblTripData.getIndexedValueAt(intsExternalStations[r], Integer.toString(intsExternalStations[c]));
                fltsTripData[r][c] = fltValue;

            }
        }

        mtxSeed = new Matrix(strTripType, "", fltsTripData);
        mtxSeed.setExternalNumbersZeroBased(intsExternalStations);


    }

    private void defineTargets(String strTripType) {

        String strTimePrefix;
        String strTruckSuffix;

        if (strTripType.startsWith("AM")) {
            strTimePrefix = "AM_";
        } else {
            strTimePrefix = "MD_";
        }

        if (strTripType.endsWith("LT")) {
            strTruckSuffix = "Light";
        } else {
            strTruckSuffix = "Heavy";
        }

        rvColumnTargets = new RowVector(intsExternalStations.length);
        cvRowTargets = new ColumnVector(intsExternalStations.length);
        rvColumnTargets.setExternalNumbersZeroBased(intsExternalStations);
        cvRowTargets.setExternalNumbersZeroBased(intsExternalStations);

        for (int intExternalStation: intsExternalStations) {

            //inbound = rowTargets, outbound = columnTargets
            float fltInTimeFactor;
            float fltOutTimeFactor;

            float fltInboundTraffic = externalStationParameters.getValue(intExternalStation, "IBTruck" + strTruckSuffix);
            float fltInThruFactor = externalStationParameters.getValue(intExternalStation, "IBThruFactor_" + strTruckSuffix + "Truck");

            float fltOutboundTraffic = externalStationParameters.getValue(intExternalStation, "OBTruck" + strTruckSuffix);
            float fltOutThruFactor = externalStationParameters.getValue(intExternalStation, "OBThruFactor_" + strTruckSuffix + "Truck");

            fltInTimeFactor = externalStationParameters.getValue(intExternalStation, strTimePrefix + "IB" + strTruckSuffix + "TruckFactor");
            fltOutTimeFactor = externalStationParameters.getValue(intExternalStation, strTimePrefix + "OB" + strTruckSuffix + "TruckFactor");

            double dblGrowthFactor;
            if (alLargeRoads.contains(intExternalStation)) {
                dblGrowthFactor =  dblLargeRoadGrowthRate;
            } else {
                dblGrowthFactor = dblSmallRoadGrowthRate;
            }

            cvRowTargets.setValueAt(intExternalStation, calculateValue(fltInboundTraffic * fltInThruFactor * fltInTimeFactor, dblGrowthFactor));
            rvColumnTargets.setValueAt(intExternalStation, calculateValue(fltOutboundTraffic * fltOutThruFactor * fltOutTimeFactor, dblGrowthFactor));

        }

    }

    private float calculateValue(float fltInputValue, double dblGrowthRate) {

        double value = fltInputValue * Math.pow(1f + (dblGrowthRate/100f), intTimeInterval);
        return (float) value;

    }

    private void outputFile(String strTripType) {

        TableDataSet tblOutput = new TableDataSet();
        tblOutput.appendColumn(intsExternalStations, "TAZ");
        for (int intZone: intsExternalStations) {
            tblOutput.appendColumn(mtxOutput.getColumn(intZone).copyValues1D(), Integer.toString(intZone));
        }

        String strOutputPath = ResourceUtil.getProperty(appRb, "et.current.data");
        String strFileName = ResourceUtil.getProperty(appRb, "EE.file.prefix") + strTripType + ".csv";

        try{
            CSVFileWriter cfwWriter = new CSVFileWriter();
            cfwWriter.writeFile(tblOutput, new File(strOutputPath + strFileName));
            cfwWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    //The following is commented out because it was very useful for debugging during development,
    // but it's not actually needed for production. I'm expecting that there will be some additional debugging work
    // so I don't want to just delete this.

    /*
    private void writeMatrix(Matrix mtxOutput, String strTripType) {
        //simply for diagnostic purposes.
        String strOutputPath = ResourceUtil.getProperty(appRb, "et.current.data");
        String strOutputFileName = strOutputPath + strTripType + ".csv";
        logger.info("Writing output file (" + strOutputFileName + ").");
        MatrixWriter writer = MatrixWriter.createWriter(strOutputFileName);
        writer.writeMatrix(mtxOutput);

    }

    private void outputDiagnosticFile(String strTripType) {

        ArrayList<Integer> alOrigin = new ArrayList<Integer>();
        ArrayList<Integer> alDestination = new ArrayList<Integer>();
        ArrayList<String> alStartTime = new ArrayList<String>();
        ArrayList<Integer> alTruckClass = new ArrayList<Integer>();

        String strStartTime;
        int intTruckClass;

        if (strTripType.startsWith("AM")) {
            strStartTime = ResourceUtil.getProperty(globalRb, "AM_PEAK_START");
        } else {
            strStartTime = ResourceUtil.getProperty(globalRb, "OFF_PEAK_START");
        }

        if (strTripType.endsWith("LT")) {
            intTruckClass = ResourceUtil.getIntegerProperty(appRb, "LT.truck.class");
        } else {
            intTruckClass = ResourceUtil.getIntegerProperty(appRb, "HT.truck.class");
        }

        TableDataSet tblOutput = new TableDataSet();

        float fltRemainder = 0f;

        //Iterate over all values in balanced matrix and write 1 row to the table for each trip.
        for (int intOrigin: intsExternalStations) {
            for (int intDestination: intsExternalStations) {
                float fltValue = mtxOutput.getValueAt(intOrigin, intDestination);
                fltRemainder += (fltValue - ((int) fltValue));
                int intValue = (int) fltValue;
                while (intValue > 0) {
                    alOrigin.add(intOrigin);
                    alDestination.add(intDestination);
                    alStartTime.add(strStartTime);
                    alTruckClass.add(intTruckClass);
                    intValue--;
                }
                float fltRemainderToDisperse = (int) fltRemainder;
                fltRemainder -= fltRemainderToDisperse;

                while (fltRemainderToDisperse > 0) {
                    alOrigin.add(intOrigin);
                    alDestination.add(intDestination);
                    alStartTime.add(strStartTime);
                    alTruckClass.add(intTruckClass);
                    fltRemainderToDisperse--;
                }
                fltRemainder = 0f;
            }
        }

        int[] intsOrigin = new int[alOrigin.size()];
        int[] intsDestination = new int[alDestination.size()];
        String[] strsStartTime = new String[alStartTime.size()];
        int[] intsTruckClass = new int[alTruckClass.size()];

        for (int i = 0; i < alOrigin.size(); i++) {
            intsOrigin[i] = alOrigin.get(i);
            intsDestination[i] = alDestination.get(i);
            strsStartTime[i] = alStartTime.get(i);
            intsTruckClass[i] = alTruckClass.get(i);
        }

        tblOutput.appendColumn(intsOrigin, "Origin");
        tblOutput.appendColumn(intsDestination, "Destination");
        tblOutput.appendColumn(strsStartTime, "StartTime");
        tblOutput.appendColumn(intsTruckClass, "TruckClass");

        String strOutputPath = ResourceUtil.getProperty(appRb, "et.current.data");
        String strFileName = ResourceUtil.getProperty(appRb, "EE.file.prefix") + strTripType + ".csv";

        try{
            CSVFileWriter cfwWriter = new CSVFileWriter();
            cfwWriter.writeFile(tblOutput, new File(strOutputPath + strFileName));
            cfwWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
    */


}

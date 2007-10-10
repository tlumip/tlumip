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

import com.pb.common.datafile.CSVFileReader;
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
 * EEModel is a class that ...
 *
 * @author Kimberly Grommes
 * @version 1.0, Jul 3, 2007
 *          Created by IntelliJ IDEA.
 */
public class EEModel {

    Logger logger = Logger.getLogger(EEModel.class);

    ResourceBundle appRb;
    ResourceBundle globalRb;

    Matrix mtxSeed;
    Matrix mtxOutput;

    RowVector rvColumnTargets;
    ColumnVector cvRowTargets;

    double dblRelativeError;
    int intMaxIterations;

    int intNumberOfYears;

    Double dblLargeRoadGrowthRate;
    Double dblSmallRoadGrowthRate;

    ArrayList alLargeRoads;
    ArrayList alSmallRoads;

    int[] intsExternalStations;
    int[] intsExternalStationsZeroBased;

    public EEModel(ResourceBundle appRb, ResourceBundle globalRb) {
        this.appRb = appRb;
        this.globalRb = globalRb;
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

            //writeMatrix(mtxOutput, strOutputPath + strTripType + ".zmx");
            //outputFiles(strTripType);
        }

    }


    private void defineModel() {

        dblRelativeError = ResourceUtil.getDoubleProperty(appRb, "relative.error");

        WorldZoneExternalZoneUtil wzUtil = new WorldZoneExternalZoneUtil(globalRb);
        intsExternalStations = wzUtil.getExternalZonesForET();

        intsExternalStationsZeroBased = new int[intsExternalStations.length + 1];
        intsExternalStationsZeroBased[0] = 0;
        System.arraycopy(intsExternalStations, 0, intsExternalStationsZeroBased, 1, intsExternalStations.length);

        dblLargeRoadGrowthRate = ResourceUtil.getDoubleProperty(appRb, "large.road.growth.rate");
        dblSmallRoadGrowthRate = ResourceUtil.getDoubleProperty(appRb, "small.road.growth.rate");

        intNumberOfYears = ResourceUtil.getIntegerProperty(appRb, "number.of.years");

        alLargeRoads = ResourceUtil.getList(appRb, "large.roads");
        alSmallRoads = ResourceUtil.getList(appRb, "small.roads");

    }

    private void defineInputs(String strTripType) {

        defineSeed(strTripType);
        defineTargets(strTripType);

    }    

    private void addShipments(String strTripType, ArrayList<ShipmentDetail> alTrucks) {

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
        mtxSeed.setExternalNumbers(intsExternalStationsZeroBased);


    }

    private void defineTargets(String strTripType) {

        String strExternalStationFile = ResourceUtil.getProperty(appRb, "external.station.parameter.file");
        TableDataSet tblExternalStationParameters = null;
        TableDataFileReader rdrReader = CSVFileReader.createReader(new File(strExternalStationFile));

        try {
            tblExternalStationParameters = rdrReader.readFile(new File(strExternalStationFile));
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        tblExternalStationParameters.buildIndex(1);

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

        rvColumnTargets = new RowVector(intsExternalStations.length + 1);
        cvRowTargets = new ColumnVector(intsExternalStations.length + 1);
        rvColumnTargets.setExternalNumbers(intsExternalStationsZeroBased);
        cvRowTargets.setExternalNumbers(intsExternalStationsZeroBased);

        for (int intExternalStation: intsExternalStations) {

            //inbound = rowTargets, outbound = columnTargets
            float fltInTimeFactor;
            float fltOutTimeFactor;

            float fltInboundTraffic = tblExternalStationParameters.getIndexedValueAt(intExternalStation, "IBTruck" + strTruckSuffix);
            float fltInThruFactor = tblExternalStationParameters.getIndexedValueAt(intExternalStation, "IBThruFactor_" + strTruckSuffix + "Truck");

            float fltOutboundTraffic = tblExternalStationParameters.getIndexedValueAt(intExternalStation, "OBTruck" + strTruckSuffix);
            float fltOutThruFactor = tblExternalStationParameters.getIndexedValueAt(intExternalStation, "OBThruFactor_" + strTruckSuffix + "Truck");

            fltInTimeFactor = tblExternalStationParameters.getIndexedValueAt(intExternalStation, strTimePrefix + "IB" + strTruckSuffix + "TruckFactor");
            fltOutTimeFactor = tblExternalStationParameters.getIndexedValueAt(intExternalStation, strTimePrefix + "OB" + strTruckSuffix + "TruckFactor");

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

        double value = fltInputValue * Math.pow(1f + (dblGrowthRate/100f), intNumberOfYears);
        return (float) value;

    }

    /*
    private void writeMatrix(Matrix mtxOutput, String strTripType) {
        //don't actually need to write out the matrices, this is for debugging purposes
        String strOutputPath = ResourceUtil.getProperty(appRb, "et.current.data");
        String strOutputFileName = strOutputPath + strTripType + ".zmx";
        logger.info("Writing output file (" + strOutputFileName + ").");
        MatrixWriter writer = MatrixWriter.createWriter(strOutputFileName);
        writer.writeMatrix(mtxOutput);

    }
    */

    /*
    private void outputFiles(String strTripType) {

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

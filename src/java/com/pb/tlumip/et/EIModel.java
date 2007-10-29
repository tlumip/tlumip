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
import com.pb.common.datafile.TableDataFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixException;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.model.WorldZoneExternalZoneUtil;
import com.pb.tlumip.model.ZoneMap;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * EIModel is a class that converts PI $/yr by commodity to trucks/year.
 *
 * @author Kimberly Grommes
 * @version 1.0, Aug 8, 2007
 * Created by IntelliJ IDEA.
 */
public class EIModel {

    Logger logger = Logger.getLogger(EIModel.class);

    private WorldZoneExternalZoneUtil wzUtil;

    private ResourceBundle appRb;
    private ResourceBundle globalRb;

    private AlphaToBeta a2b;

    private HashMap<String, Float> hshCommodityTruckData;
    private HashMap<String, Float> hshCommodityVehicleType;

    private String[] strsCommodities;

    private double dblSmallestAllowableTonnage;

    String strMatrixExtension;

    //private ArrayList<ShipmentDetail> alTrucks;
    private float fltAMLightPercentage;
        private float fltAMHeavyPercentage;
        private float fltMDLightPercentage;
        private float fltMDHeavyPercentage;


    private int intLightTruckClass;
    private int intHeavyTruckClass;

    private String strAMTime;
    private String strMDTime;

    private float alpha;
    private  float gamma;
    private  float beta;

    private Matrix mtxDistanceSkim;

    private ZoneMap zones;

    private int intLightTonsPerTruck;
    private int intHeavyTonsPerTruck;

    private ExternalStationParameters externalStationParameters;


    // take beta to 6006 (and vice versa) dollar flows and convert them to alpha to 5000 (and vice versa) zone truck flows.
    // to pick the alpha flow from the beta flow, I need to use the intensity map stuff from CT.
    // to pick the 5000 zoneS, I need to use a gravity model to choose the percentage to go to the various zones.

    //Pj = (zeta * e ^ (- beta * Dj)) / sumofJ (alpha * e ^(- beta * Dj))
    // where Dj is the distance and J is the 5000 zone. capish?



    public EIModel(ResourceBundle appRb, ResourceBundle globalRb, ExternalStationParameters externalStationParameters) {
        this.appRb = appRb;
        this.globalRb = globalRb;
        a2b = new AlphaToBeta(new File(ResourceUtil.getProperty(globalRb, "alpha2beta.file")),
                ResourceUtil.getProperty(globalRb, "alpha.name"), ResourceUtil.getProperty(globalRb, "beta.name"));
        this.externalStationParameters = externalStationParameters;
    }

    private void defineStationParameters() {

        //TODO: if I'm only using this table here for a few values, maybe it would be better to put these values into a properties file.
        // They don't really belong in the external station file in any case.
        fltAMLightPercentage = externalStationParameters.getValue(9999, "AM_OBLightTruckFactor") +
                externalStationParameters.getValue(9999, "AM_IBLightTruckFactor");
        fltAMHeavyPercentage = externalStationParameters.getValue(9999, "AM_OBHeavyTruckFactor") +
                externalStationParameters.getValue(9999, "AM_IBHeavyTruckFactor");
        fltMDLightPercentage = externalStationParameters.getValue(9999, "MD_OBLightTruckFactor") +
                externalStationParameters.getValue(9999, "MD_IBLightTruckFactor");
        fltMDHeavyPercentage = externalStationParameters.getValue(9999, "MD_OBHeavyTruckFactor") +
                externalStationParameters.getValue(9999, "MD_IBHeavyTruckFactor");
    }

    public void runModel(ArrayList<ShipmentDetail> alTrucks) {

        defineModel();

        for (String strCommodity: strsCommodities) {

            Matrix mtxOriginalCommodityFlow;
            String strFileName = ResourceUtil.getProperty(appRb, "pi.output.dir") +
                        "buying_" + strCommodity + strMatrixExtension;
            try {

                mtxOriginalCommodityFlow = readMatrix(strFileName, strCommodity);

            } catch (MatrixException ex) {
                logger.info("Skipping commodity " + strCommodity + ". Can't find matrix: " + strFileName + ".");
                continue;
            }

            int[] intsExternalZoneNumbers1Based = new int[wzUtil.getNumberOfExternalZones()+1];
            System.arraycopy(wzUtil.getExternalZonesForET(), 0, intsExternalZoneNumbers1Based, 1, wzUtil.getExternalZonesForET().length);

            Matrix mtxUpdatedCommodityImportFlow = new Matrix(strCommodity + "Import_Updated", "", wzUtil.getNumberOfExternalZones(), a2b.getNumAlphaZones() );
            mtxUpdatedCommodityImportFlow.setExternalNumbers( intsExternalZoneNumbers1Based, a2b.getAlphaExternals1Based());

            Matrix mtxUpdatedCommodityExportFlow = new Matrix(strCommodity + "Export_Updated", "", wzUtil.getNumberOfExternalZones(), a2b.getNumAlphaZones());
            mtxUpdatedCommodityExportFlow.setExternalNumbers(intsExternalZoneNumbers1Based, a2b.getAlphaExternals1Based());

            float fltImportResidual = 0f;
            float fltExportResidual = 0f;

            //this first loop converts the beta zones to alpha zones and the 6006 zone to external zones and the PI$/year to tons/week
            for (int intOriginZone: a2b.getBetaExternals0Based()) {
                int intAlphaZone = zones.getAlphaZone(intOriginZone, strCommodity);
                float fltPIExportValue = mtxOriginalCommodityFlow.getValueAt(intOriginZone, wzUtil.LOCAL_MARKET_WORLD_ZONE);
                float fltPIImportValue = mtxOriginalCommodityFlow.getValueAt(wzUtil.LOCAL_MARKET_WORLD_ZONE, intOriginZone);

                float fltImportDollarsPerWeek = fltPIImportValue/52f;
                float fltExportDollarsPerWeek = fltPIExportValue/52f;

                float fltDollarPerTon = hshCommodityTruckData.get(strCommodity);

                float fltImportTonsPerWeek = fltImportDollarsPerWeek*(1f/fltDollarPerTon);
                float fltExportTonsPerWeek = fltExportDollarsPerWeek*(1f/fltDollarPerTon);

                float fltDistanceSum = 0f;

                for (int intExternalZone: wzUtil.getExternalZonesForET()){
                    fltDistanceSum += alpha * Math.exp(-beta * mtxDistanceSkim.getValueAt(intAlphaZone, intExternalZone));
                }

                for (int intExternalZone: wzUtil.getExternalZonesForET()) {
                    float fltProportion = (float) (gamma * Math.exp(-beta *
                            mtxDistanceSkim.getValueAt(intAlphaZone, intExternalZone)))/fltDistanceSum;

                    float fltImportAmount = fltImportTonsPerWeek * fltProportion;
                    float fltExportAmount = fltExportTonsPerWeek * fltProportion;

                    if (fltImportAmount < dblSmallestAllowableTonnage) {
                        fltImportResidual += fltImportAmount;
                        mtxUpdatedCommodityImportFlow.setValueAt(intExternalZone, intAlphaZone, 0f);
                    } else {
                        mtxUpdatedCommodityImportFlow.setValueAt(intExternalZone, intAlphaZone, fltImportAmount);
                    }

                    if (fltExportAmount < dblSmallestAllowableTonnage) {
                        fltExportResidual += fltExportAmount;
                        mtxUpdatedCommodityExportFlow.setValueAt(intExternalZone, intAlphaZone, 0f);
                    } else {
                        mtxUpdatedCommodityExportFlow.setValueAt(intExternalZone, intAlphaZone, fltExportAmount);
                    }
                }
            }

            float fltTotalImports = 0f;
            float fltTotalExports = 0f;

            for (int i: intsExternalZoneNumbers1Based) {
                fltTotalImports += mtxUpdatedCommodityImportFlow.getRowSum(i);
                fltTotalExports += mtxUpdatedCommodityExportFlow.getRowSum(i);
            }

            //calculation below could be replaced with something like:
            //m.cell[p][q] *= weeklyTons/(weeklyTons-residual);
            //which is from CT/FreightDemand3/line 335ish
            //calculation works out same
            for (int intAlphaZone: a2b.getAlphaExternals0Based()) {
                for (int intExternalZone: wzUtil.getExternalZonesForET()) {

                    if (fltImportResidual > 0) {
                        float fltImportValue = mtxUpdatedCommodityImportFlow.getValueAt(intExternalZone, intAlphaZone);
                        if (fltImportValue > 0) {
                            mtxUpdatedCommodityImportFlow.setValueAt(intExternalZone, intAlphaZone, fltImportValue += (fltImportResidual/fltTotalImports) * fltImportValue);
                        }
                    }
                    if (fltExportResidual > 0) {
                        float fltExportValue = mtxUpdatedCommodityExportFlow.getValueAt(intExternalZone, intAlphaZone);

                        if (fltExportValue > 0) {
                            mtxUpdatedCommodityExportFlow.setValueAt(intExternalZone, intAlphaZone, fltExportValue += (fltExportResidual/fltTotalExports) * fltExportValue);
                        }
                    }
                }
            }
            convertTonsPerWeektoTrucksPerPeak(mtxUpdatedCommodityImportFlow, mtxUpdatedCommodityExportFlow, strCommodity, alTrucks);


        }

        //outputDiagnosticFile();
        //outputFile();

    }

    private void convertTonsPerWeektoTrucksPerPeak(Matrix mtxUpdatedCommodityImportFlow, Matrix mtxUpdatedCommodityExportFlow, String strCommodity, ArrayList<ShipmentDetail> alTrucks) {
        //this third loop converts the tons/week amount to trucks/peak period
        for (int intAlphaZone: a2b.getAlphaExternals0Based()) {
            for (int intExternalZone: wzUtil.getExternalZonesForET()) {

                float fltImportTonsPerWeek = mtxUpdatedCommodityImportFlow.getValueAt(intExternalZone, intAlphaZone);
                float fltExportTonsPerWeek = mtxUpdatedCommodityExportFlow.getValueAt(intExternalZone, intAlphaZone);

                float fltLightCommodityPercentage = hshCommodityVehicleType.get(strCommodity + "_Light")/100f;
                float fltHeavyCommodityPercentage = hshCommodityVehicleType.get(strCommodity + "_Heavy")/100f;

                float fltImportLightTrucksPerWeek = (fltImportTonsPerWeek * fltLightCommodityPercentage)/intLightTonsPerTruck;
                float fltExportLightTrucksPerWeek = (fltExportTonsPerWeek * fltLightCommodityPercentage)/intLightTonsPerTruck;
                float fltImportHeavyTrucksPerWeek = (fltImportTonsPerWeek * fltHeavyCommodityPercentage)/intHeavyTonsPerTruck;
                float fltExportHeavyTrucksPerWeek = (fltExportTonsPerWeek * fltHeavyCommodityPercentage)/intHeavyTonsPerTruck;

                float fltAMImportLightTrucksPerWeek = fltImportLightTrucksPerWeek * fltAMLightPercentage;
                float fltMDImportLightTrucksPerWeek = fltImportLightTrucksPerWeek * fltMDLightPercentage;
                float fltAMExportLightTrucksPerWeek = fltExportLightTrucksPerWeek * fltAMLightPercentage;
                float fltMDExportLightTrucksPerWeek = fltExportLightTrucksPerWeek * fltMDLightPercentage;
                float fltAMImportHeavyTrucksPerWeek = fltImportHeavyTrucksPerWeek * fltAMHeavyPercentage;
                float fltMDImportHeavyTrucksPerWeek = fltImportHeavyTrucksPerWeek * fltMDHeavyPercentage;
                float fltAMExportHeavyTrucksPerWeek = fltExportHeavyTrucksPerWeek * fltAMHeavyPercentage;
                float fltMDExportHeavyTrucksPerWeek = fltExportHeavyTrucksPerWeek * fltMDHeavyPercentage;

                if (fltAMImportLightTrucksPerWeek > 0)
                    alTrucks.add(new ShipmentDetail(strCommodity, intExternalZone, intAlphaZone, strAMTime, fltAMImportLightTrucksPerWeek/7, intLightTruckClass));
                if (fltMDImportLightTrucksPerWeek > 0)
                    alTrucks.add(new ShipmentDetail(strCommodity, intExternalZone, intAlphaZone, strMDTime, fltMDImportLightTrucksPerWeek/7, intLightTruckClass));
                if (fltAMExportLightTrucksPerWeek > 0)
                    alTrucks.add(new ShipmentDetail(strCommodity, intAlphaZone, intExternalZone, strAMTime, fltAMExportLightTrucksPerWeek/7, intLightTruckClass));
                if (fltMDExportLightTrucksPerWeek > 0)
                    alTrucks.add(new ShipmentDetail(strCommodity, intAlphaZone, intExternalZone, strMDTime, fltMDExportLightTrucksPerWeek/7, intLightTruckClass));
                if (fltAMImportHeavyTrucksPerWeek > 0)
                    alTrucks.add(new ShipmentDetail(strCommodity, intExternalZone, intAlphaZone, strAMTime, fltAMImportHeavyTrucksPerWeek/7, intHeavyTruckClass));
                if (fltMDImportHeavyTrucksPerWeek > 0)
                    alTrucks.add(new ShipmentDetail(strCommodity, intExternalZone, intAlphaZone, strMDTime, fltMDImportHeavyTrucksPerWeek/7, intHeavyTruckClass));
                if (fltAMExportHeavyTrucksPerWeek > 0)
                    alTrucks.add(new ShipmentDetail(strCommodity, intAlphaZone, intExternalZone, strAMTime, fltAMExportHeavyTrucksPerWeek/7, intHeavyTruckClass));
                if (fltMDExportHeavyTrucksPerWeek > 0)
                    alTrucks.add(new ShipmentDetail(strCommodity, intAlphaZone, intExternalZone, strMDTime, fltMDExportHeavyTrucksPerWeek/7, intHeavyTruckClass));
            }
        }
    }

    private void defineModel() {
        strMatrixExtension = ResourceUtil.getProperty(globalRb, "matrix.extension");
        wzUtil = new WorldZoneExternalZoneUtil(globalRb);
        //AlphaToBeta a2b = new AlphaToBeta(new File(ResourceUtil.getProperty(globalRb, "alpha2beta.file")));
        int[] intsBetaZones = a2b.getBetaExternals0Based();
        int[] intsWorldZones = wzUtil.getWorldZones();
        int[] intsBetaPlusWorldZones = new int[intsBetaZones.length + intsWorldZones.length];
        System.arraycopy(intsBetaZones, 0, intsBetaPlusWorldZones, 0, intsBetaZones.length);
        System.arraycopy(intsWorldZones, 0, intsBetaPlusWorldZones, intsBetaZones.length, intsWorldZones.length);

        dblSmallestAllowableTonnage = ResourceUtil.getDoubleProperty(appRb, "smallest.allowable.tonnage");

        defineCommodityTruckData();

        defineCommodityVehicleTypes();

        defineStationParameters();

        intLightTruckClass = ResourceUtil.getIntegerProperty(appRb, "LT.truck.class");
        intHeavyTruckClass = ResourceUtil.getIntegerProperty(appRb, "HT.truck.class");

        strAMTime = ResourceUtil.getProperty(globalRb, "AM_PEAK_START");
        strMDTime = ResourceUtil.getProperty(globalRb, "OFF_PEAK_START");

        defineGravityModelParameters();

        mtxDistanceSkim = readMatrix(ResourceUtil.getProperty(appRb, "distance.skim"), "Distance_Skim");

        zones = new ZoneMap(new File(ResourceUtil.getProperty(globalRb, "alpha2beta.file")), 1990);

        intLightTonsPerTruck = ResourceUtil.getIntegerProperty(appRb, "light.tons.per.truck");
        intHeavyTonsPerTruck = ResourceUtil.getIntegerProperty(appRb, "heavy.tons.per.truck");
    }

    private void defineGravityModelParameters() {
        alpha = (float) ResourceUtil.getDoubleProperty(appRb, "ei.gravitymodel.alpha");
        gamma = (float) ResourceUtil.getDoubleProperty(appRb, "ei.gravitymodel.gamma");
        beta = (float) ResourceUtil.getDoubleProperty(appRb, "ei.gravitymodel.beta");
    }

    private void defineCommodityTruckData() {

        hshCommodityTruckData = new HashMap<String, Float>();
        TableDataSet tblCommodityTruckData;
        String strCommodityTruckDataFile = ResourceUtil.getProperty(appRb, "commodity.truck.data.file");
        TableDataFileReader rdrReader = CSVFileReader.createReader(new File(strCommodityTruckDataFile));

        try {
            tblCommodityTruckData = rdrReader.readFile(new File(strCommodityTruckDataFile));
        }
        catch (IOException e) {
            tblCommodityTruckData = null;
            e.printStackTrace();
            System.exit(1);
        }

        strsCommodities = tblCommodityTruckData.getColumnAsString("Commodity");

        for (int row = 1; row <= tblCommodityTruckData.getRowCount(); row++) {
            String strCommodity = tblCommodityTruckData.getStringValueAt(row, "Commodity");
            float fltDollarPerTon = tblCommodityTruckData.getValueAt(row, "1990$perTon");
            hshCommodityTruckData.put(strCommodity, fltDollarPerTon);
        }
    }

    private void defineCommodityVehicleTypes() {

        hshCommodityVehicleType = new HashMap<String, Float>();

        TableDataSet tblCommodityVehicleData;
        String strFileName = ResourceUtil.getProperty(appRb, "commodity.vehicle.types.file");
        TableDataFileReader rdrReader = CSVFileReader.createReader(new File(strFileName));

        try {
            tblCommodityVehicleData = rdrReader.readFile(new File(strFileName));
        }
        catch (IOException e) {
            tblCommodityVehicleData = null;
            e.printStackTrace();
            System.exit(1);
        }

        for (int row = 1; row <= tblCommodityVehicleData.getRowCount(); row++) {
            int intCommodity = (int) tblCommodityVehicleData.getValueAt(row, "sctg");

            String strSCTG = Integer.toString(intCommodity);
            if (strSCTG.length() == 1) {
                strSCTG = "SCTG0" + strSCTG;
            } else {
                strSCTG = "SCTG" + strSCTG;
            }
            float fltLightPercentage = tblCommodityVehicleData.getValueAt(row, "pTRK1") +
                    tblCommodityVehicleData.getValueAt(row, "pTRK2") +
                    tblCommodityVehicleData.getValueAt(row, "pTRK3");
            float fltHighPercentage = tblCommodityVehicleData.getValueAt(row, "pTRK4") +
                    tblCommodityVehicleData.getValueAt(row, "pTRK5");
            hshCommodityVehicleType.put(strSCTG + "_Light", fltLightPercentage);
            hshCommodityVehicleType.put(strSCTG + "_Heavy", fltHighPercentage);

        }

    }

    private Matrix readMatrix(String strFilePath, String strName) {
        //logger.info("Reading matrix at " + strFilePath);
        return MatrixReader.readMatrix(new File(strFilePath), strName);
    }
/*
    private void outputFile() {

        int intTotalTrucks = 0;
        TableDataSet tblOutput = new TableDataSet();

        ArrayList<Integer> alOrigin = new ArrayList<Integer>();
        ArrayList<Integer> alDestination = new ArrayList<Integer>();
        ArrayList<String> alStartTime = new ArrayList<String>();
        ArrayList<Integer> alTruckClass = new ArrayList<Integer>();

        float fltRemainder = 0f;

        //Iterate over all entries in alTrucks and write 1 row to the table for each trip.
        for (ShipmentDetail objDetail : alTrucks) {
            float fltValue = objDetail.getNumberOfTrucks();
            fltRemainder += (fltValue - Math.floor(fltValue));

            int intValue = (int) fltValue;
            while (intValue > 0) {
                intTotalTrucks++;
                alOrigin.add(objDetail.getOrigination());
                alDestination.add(objDetail.getDestination());
                alStartTime.add(objDetail.getTimeOfDay());
                alTruckClass.add(objDetail.getTruckClass());
                intValue--;
            }
            float fltRemainderToDisperse = (float) Math.floor(fltRemainder);
            fltRemainder -= fltRemainderToDisperse;

            while (fltRemainderToDisperse > 0) {
                intTotalTrucks++;
                alOrigin.add(objDetail.getOrigination());
                alDestination.add(objDetail.getDestination());
                alStartTime.add(objDetail.getTimeOfDay());
                alTruckClass.add(objDetail.getTruckClass());
                fltRemainderToDisperse--;
            }
        }

        if (fltRemainder > 0) {
            ShipmentDetail objDetail = alTrucks.get(alTrucks.size()-1);
            intTotalTrucks++;
            alOrigin.add(objDetail.getOrigination());
            alDestination.add(objDetail.getDestination());
            alStartTime.add(objDetail.getTimeOfDay());
            alTruckClass.add(objDetail.getTruckClass());
        }

        logger.info("Total trucks: " + intTotalTrucks);

        int[] intsOrigin = new int[alOrigin.size()];
        int[] intsDestination = new int[alOrigin.size()];
        String[] strsStartTime = new String[alOrigin.size()];
        int[] intsTruckClass = new int[alOrigin.size()];

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
        String strFileName = ResourceUtil.getProperty(appRb, "EI.file.prefix") + "TruckTrips" + ".csv";

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
    /*
    private void outputDiagnosticFile() {

        TableDataSet tblOutput = new TableDataSet();

        int[] intsOrigin = new int[alTrucks.size()];
        int[] intsDestination = new int[alTrucks.size()];
        String[] strsStartTime = new String[alTrucks.size()];
        int[] intsTruckClass = new int[alTrucks.size()];
        float[] fltsNumberOfTrucks = new float[alTrucks.size()];
        String[] strsCommodity = new String[alTrucks.size()];

        float fltNumberOfTrucks = 0f;


        for (int i = 0; i < alTrucks.size(); i++) {
            ShipmentDetail objDetail = alTrucks.get(i);
            intsOrigin[i] = objDetail.getOrigination();
            intsDestination[i] = objDetail.getDestination();
            strsStartTime[i] = objDetail.getTimeOfDay();
            strsCommodity[i] = objDetail.getCommodity();
            intsTruckClass[i] = objDetail.getTruckClass();
            fltsNumberOfTrucks[i] = objDetail.getNumberOfTrucks();
            fltNumberOfTrucks += objDetail.getNumberOfTrucks();
        }

        tblOutput.appendColumn(intsOrigin, "Origin");
        tblOutput.appendColumn(intsDestination, "Destination");
        tblOutput.appendColumn(strsStartTime, "StartTime");
        tblOutput.appendColumn(intsTruckClass, "TruckClass");
        tblOutput.appendColumn(fltsNumberOfTrucks, "NumberOfTrucks");
        tblOutput.appendColumn(strsCommodity, "Commodity");

        String strOutputPath = ResourceUtil.getProperty(appRb, "et.current.data");
        String strFileName = ResourceUtil.getProperty(appRb, "EI.file.prefix") + "DiagnosticTruckTrips" + ".csv";

        logger.info("Total Number of trucks: " + fltNumberOfTrucks);

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

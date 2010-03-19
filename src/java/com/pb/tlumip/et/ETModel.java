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
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.models.reference.ModelComponent;
import com.pb.models.utils.StatusLogger;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * ETModel is a class that generates the external trips for the TLUMIP model, primarily by calling EIModel and EEModel.
 *
 * Do not use this code; it was replaced by Michalis’ python code, which was replaced by ET EE component implementation only
 *
 * @author Kimberly Grommes
 * @version 1.0, Jan 23, 2007
 * Created by IntelliJ IDEA.
 */
@Deprecated
public class ETModel extends ModelComponent {

    Logger logger = Logger.getLogger(ETModel.class);

    private ExternalStationParameters externalStationParameters;

    private ArrayList<ShipmentDetail> alTrucks;


    public ETModel(ResourceBundle appRb, ResourceBundle globalRb){
		setResourceBundles(appRb, globalRb);    //creates a resource bundle as a class attribute called appRb.

    }

    private void defineExternalStationParameters() {
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

        externalStationParameters = new ExternalStationParameters(tblExternalStationParameters);
    }

    public void startModel(int baseYear, int intTimeInterval){
        logger.info("Starting ET Model.");
        StatusLogger.logText("et","ET started for year t" + intTimeInterval);

        defineExternalStationParameters();

        alTrucks = new ArrayList<ShipmentDetail> ();

        logger.info("Starting Thru-Trips Model.");
        EEModel eeModel = new EEModel(appRb, globalRb, intTimeInterval, baseYear, externalStationParameters);
        eeModel.runModel(alTrucks);

        logger.info("Starting ExternalInternal Model.");
        EIModel eiModel = new EIModel(appRb, globalRb, externalStationParameters);
        eiModel.runModel(alTrucks);

        StatusLogger.logText("et","Writing output file");
        outputFile();

        logger.info("Finishing ET Model.");
        StatusLogger.logText("et","ET Done");
    }

    private void outputFile() {

        int intTotalTrucks = 0;
        TableDataSet tblOutput = new TableDataSet();

        ArrayList<Integer> alOrigin = new ArrayList<Integer>();
        ArrayList<Integer> alDestination = new ArrayList<Integer>();
        ArrayList<String> alStartTime = new ArrayList<String>();
        ArrayList<String> alTruckClass = new ArrayList<String>();

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

        logger.info("ET Total trucks: " + intTotalTrucks);

        int[] intsOrigin = new int[alOrigin.size()];
        int[] intsDestination = new int[alOrigin.size()];
        String[] strsStartTime = new String[alOrigin.size()];
        String[] strsTruckClass = new String[alOrigin.size()];

        for (int i = 0; i < alOrigin.size(); i++) {
            intsOrigin[i] = alOrigin.get(i);
            intsDestination[i] = alDestination.get(i);
            strsStartTime[i] = alStartTime.get(i);
            strsTruckClass[i] = alTruckClass.get(i);
        }

        tblOutput.appendColumn(intsOrigin, "Origin");
        tblOutput.appendColumn(intsDestination, "Destination");
        tblOutput.appendColumn(strsStartTime, "TripStartTime");
        tblOutput.appendColumn(strsTruckClass, "TruckClass");

        //need to share property with TS
        String strOutputFile = globalRb.getString("et.truck.trips");

        try{
            CSVFileWriter cfwWriter = new CSVFileWriter();
            cfwWriter.writeFile(tblOutput, new File(strOutputFile));
            cfwWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}

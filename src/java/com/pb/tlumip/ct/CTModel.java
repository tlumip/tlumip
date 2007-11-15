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
package com.pb.tlumip.ct;

import com.pb.common.util.ResourceUtil;
import com.pb.models.reference.ModelComponent;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * @author donnellyr
 *
 * 
 */
public class CTModel extends ModelComponent {
	
	Logger logger = Logger.getLogger(CTModel.class);
    Logger statusLogger = Logger.getLogger("status");
    long randomSeed;           //will be read from properties files and passed to
    String inputPath;    //other methods that
    String outputPath;   //run the CTModel.

	public CTModel(ResourceBundle appRb, ResourceBundle globalRb){
		setResourceBundles(appRb, globalRb);    //creates a resource bundle as a class attribute called appRb.
        this.inputPath = ResourceUtil.getProperty(appRb, "ct.base.data");
        logger.info("inputPath: " + inputPath);
        this.outputPath = ResourceUtil.getProperty(appRb, "ct.current.data");
        logger.info("outputPath: " + outputPath);
        this.randomSeed = Long.parseLong(ResourceUtil.getProperty(appRb, "randomSeed"));
        logger.info("random seed: " + randomSeed);
    }
	
	public void startModel(int baseYear, int t){
        Date start = new Date();

        // This translates PI output (annual dollar flows at beta zone level) to
        // weekly tons by commodity class (SCTG01-SCTG43), and writes output in
        // binary format.
        statusLogger.info("ct.status,Converting dollars to tons");
        FreightDemand3 fd = new FreightDemand3(appRb,globalRb,inputPath,outputPath,randomSeed);
	    fd.run();  //writes WeeklyDemand.binary

        // Translates weekly demand from beta zones into discrete daily shipments in
        // alpha zone, and writes text file of shipments
        statusLogger.info("ct.status,Converting betazone demand into daily shipments by alphazone");
        DiscreteShipments2 ds = new DiscreteShipments2(appRb,globalRb,inputPath,randomSeed);
        ds.run(new File(outputPath + "WeeklyDemand.binary"));   // input from FreightDemand
        ds.writeShipments(new File(outputPath + "DailyShipments.csv"));   // output file

        // Loads discrete shipments at alpha zone level onto individual trucks, optimizes
        // their itinerary, and writes to a CSV file in format required by TS
        // Next line prevents writing intrazonal (at alpha level) trips
        statusLogger.info("ct.status,Placing shipments on trucks");
        boolean collapseIntrazonalTrips = true;
        TruckTours4 truckTours = new TruckTours4(appRb, globalRb, inputPath,randomSeed);
        truckTours.run(new File(outputPath + "DailyShipments.csv"));   // input from DiscreteShipments
        truckTours.writeTours(new File(globalRb.getString("ct.truck.trips")), collapseIntrazonalTrips);  //output

        logger.info("total time of CT: "+CTHelper.elapsedTime(start, new Date()));
        statusLogger.info("ct.status,ct done");
    }
	
	public static void main (String args[]){
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_aaaCurrentData/t1/ct/ct.properties"));
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_aaaCurrentData/t1/global.properties"));
        CTModel ctModel = new CTModel(rb, globalRb);
		ctModel.startModel(1990, 1);	
	}

}

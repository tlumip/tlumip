package com.pb.despair.ct;

import java.io.File;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.Random;
import java.util.logging.Logger;

import com.pb.common.util.ResourceUtil;
import com.pb.despair.model.ModelComponent;

/**
 * @author donnellyr
 *
 * 
 */
public class CTModel extends ModelComponent {
	
	Logger logger = Logger.getLogger("com.pb.despair.ct");
    long randomSeed;           //will be read from properties files and passed to
    String inputPath;    //other methods that
    String outputPath;   //run the CTModel.
    boolean debug;      //in properties file

	public CTModel(ResourceBundle appRb, ResourceBundle globalRb){
		setResourceBundles(appRb, globalRb);    //creates a resource bundle as a class attribute called appRb.
        this.debug = (Boolean.valueOf(ResourceUtil.getProperty(appRb, "debug"))).booleanValue();
        logger.info("debug: " + debug);
        this.inputPath = ResourceUtil.getProperty(appRb, "ct.base.data");
        logger.info("inputPath: " + inputPath);
        this.outputPath = ResourceUtil.getProperty(appRb, "ct.current.data");
        logger.info("outputPath: " + outputPath);
        this.randomSeed = Long.parseLong(ResourceUtil.getProperty(appRb, "randomSeed"));
        logger.info("random seed: " + randomSeed);
    }
	
	public void startModel(int t){
        Date start = new Date();

        // This translates PI output (annual dollar flows at beta zone level) to
        // weekly tons by commodity class (SCTG01-SCTG43), and writes output in
        // binary format.
        FreightDemand3 fd = new FreightDemand3(appRb,globalRb,debug,inputPath,outputPath,randomSeed);
	    fd.run();  //writes WeeklyDemand.binary

        // Translates weekly demand from beta zones into discrete daily shipments in
        // alpha zone, and writes text file of shipments
        DiscreteShipments2 ds = new DiscreteShipments2(appRb,inputPath,randomSeed);
        ds.run(new File(outputPath + "WeeklyDemand.binary"));   // input from FreightDemand
        ds.writeShipments(new File(outputPath + "DailyShipments.csv"));   // output file

        // Loads discrete shipments at alpha zone level onto individual trucks, optimizes
        // their itinerary, and writes to a CSV file in format required by TS
        // Next line prevents writing intrazonal (at alpha level) trips
        boolean collapseIntrazonalTrips = true;
        TruckTours4 truckTours = new TruckTours4(appRb,inputPath,randomSeed);
        truckTours.run(new File(outputPath + "DailyShipments.csv"));   // input from DiscreteShipments
        truckTours.writeTours(new File(outputPath + "TruckTrips.csv"), collapseIntrazonalTrips);  //output

        logger.info("total time of CT: "+CTHelper.elapsedTime(start, new Date()));
	}
	
	public static void main (String args[]){
		CTModel ctModel = new CTModel(ResourceUtil.getResourceBundle("ct"), ResourceUtil.getResourceBundle("global"));
		ctModel.startModel(1);	
	}

}

package com.pb.despair.ts.assign.tests;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 5/7/2004
 */


import com.pb.despair.ts.assign.Network;
import com.pb.despair.ts.assign.Skims;
import com.pb.despair.ts.assign.FW;
import com.pb.despair.ts.assign.TripDataGenerator;

import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Logger;



public class HighwayAssignTest {

	protected static Logger logger = Logger.getLogger("com.pb.despair.ts.assign.tests");

	HashMap propertyMap;

	double[][][] multiclassTripTable = new double[2][][];
	
	Network g = null;
	
	
	
	public HighwayAssignTest() {

	    propertyMap = ResourceUtil.getResourceBundleAsHashMap("ts");

	}
    
	
	
    public static void main (String[] args) {
        
		HighwayAssignTest test = new HighwayAssignTest();

		test.runTest("peak");
		
    }

    
    
    private void runTest (String period) {
        
		long totalTime = 0;
		long startTime = System.currentTimeMillis();
		

		int totalTrips;
		int linkCount;
		String myDateString;

		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating Network object at: " + myDateString);
		g = new Network( propertyMap, period );
		
		
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW( propertyMap, g );


		// create highway skims object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating hwy skims object at: " + myDateString);
		Skims highwaySkims = new Skims(g, propertyMap);

		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("generating trips with gravity model at: " + myDateString);
		TripDataGenerator tdm = new TripDataGenerator ( highwaySkims.getSovDistSkims() );


		//Compute Frank-Wolfe solution
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("starting fw at: " + myDateString);
		multiclassTripTable[0] = tdm.getOdTable(); 
		fw.iterate ( multiclassTripTable );

        
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done at: " + myDateString);
		totalTime = (System.currentTimeMillis() - startTime);
		logger.info ("done with Frank-Wolfe assignment: " + myDateString);
		logger.info ("total time: " + totalTime);


		logger.info("HwyDistSkimsTest() finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");

    }
    
}

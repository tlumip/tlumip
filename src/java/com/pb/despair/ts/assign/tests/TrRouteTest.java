package com.pb.despair.ts.assign.tests;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 5/7/2004
 */


import com.pb.despair.ts.assign.Network;
import com.pb.despair.ts.transit.TrRoute;

//import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.util.Date;
import java.text.DateFormat;
import java.util.logging.Logger;



public class TrRouteTest {

	protected static Logger logger = Logger.getLogger("com.pb.despair.ts.tests");

	public static final String PK_TRANSIT_ROUTES_D221 = "c:\\jim\\tlumip\\TLUMIPEmme2\\pktran.in";
	public static final String ROUTE_REPORT_FILE = "c:\\jim\\tlumip\\Tlumip_Routes.report";
	

	
	HashMap tsPropertyMap;
    HashMap globalPropertyMap;

	Network g = null;
	
	
	
	public TrRouteTest() {

	    //tsPropertyMap = ResourceUtil.getResourceBundleAsHashMap("ts");
        //globalPropertyMap = ResourceUtil.getResourceBundleAsHashMap("global");


	}
    
	
	
    public static void main (String[] args) {
        
		TrRouteTest test = new TrRouteTest();

		test.runTest("peak");
		
    }

    
    
    private void runTest (String period) {
        
		long totalTime = 0;
		long startTime = System.currentTimeMillis();
		String myDateString;
		

		// create a highway network oject
		Network g = new Network( tsPropertyMap, globalPropertyMap, period );

		// create transit routes object with max 50 routes
		TrRoute tr = new TrRoute (500);

		//read transit route info from Emme/2 for d221 file
		tr.readTransitRoutes ( PK_TRANSIT_ROUTES_D221 );

		// associate transit segment node sequence with highway link indices
		tr.getLinkIndices (g);

		// print route summary file
		tr.printTransitRoutes ( ROUTE_REPORT_FILE );

        
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done at: " + myDateString);


		logger.info("TrRouteTest() finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");

    }
    
}

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
package com.pb.tlumip.ts.assign.tests;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 5/7/2004
 */


import com.pb.tlumip.ts.assign.Network;
import com.pb.tlumip.ts.assign.Skims;
import com.pb.tlumip.ts.assign.FW;
import com.pb.tlumip.ts.assign.TripDataGenerator;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.text.DateFormat;
import java.util.Date;
import org.apache.log4j.Logger;



public class HighwayAssignTest {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.assign.tests");

	HashMap tsPropertyMap;
    HashMap globalPropertyMap;

	double[][][] multiclassTripTable = new double[2][][];
	
	Network g = null;
	
	
	
	public HighwayAssignTest() {

	    tsPropertyMap = ResourceUtil.getResourceBundleAsHashMap("ts");
        globalPropertyMap = ResourceUtil.getResourceBundleAsHashMap("global");

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

		float peakFactor = Float.parseFloat( (String)globalPropertyMap.get("AM_PEAK_VOL_FACTOR") );
		
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating Network object at: " + myDateString);
		g = new Network( tsPropertyMap, globalPropertyMap, period, peakFactor );
		
		
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW();
		fw.initialize( tsPropertyMap, g );


		// create highway skims object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating hwy skims object at: " + myDateString);
		Skims highwaySkims = new Skims(g, tsPropertyMap, globalPropertyMap);

		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("generating trips with gravity model at: " + myDateString);
		Matrix m = highwaySkims.getHwySkimMatrix( period, "dist", 'a' );
		TripDataGenerator tdm = new TripDataGenerator ( m.getValues() );


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

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


import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.NetworkHandlerIF;
import com.pb.tlumip.ts.assign.Skims;
import com.pb.tlumip.ts.assign.TripDataGenerator;
import com.pb.tlumip.ts.assign.FW;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.util.ResourceBundle;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import org.apache.log4j.Logger;



public class HighwayAssignTest {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.assign.tests");

    ResourceBundle rb;
    ResourceBundle globalRb;
    HashMap tsPropertyMap;
    HashMap globalPropertyMap;

	
	
	public HighwayAssignTest() {

        rb = ResourceUtil.getPropertyBundle( new File("/jim/tlumip/data/test/tpTest.properties") );
        globalRb = ResourceUtil.getPropertyBundle(new File("/jim/util/svn_workspace/projects/tlumip/config/global.properties"));
        
        tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

	}
    
	
	
    public static void main (String[] args) {
        
		HighwayAssignTest test = new HighwayAssignTest();

		test.runTest("peak");
		
    }

    
    
    private void runTest (String period) {
        
		long totalTime = 0;
		long startTime = System.currentTimeMillis();
		

		String myDateString;

        double[][][] multiclassTripTable = new double[2][][];
        

        
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating Network object at: " + myDateString);
		NetworkHandlerIF nh = NetworkHandler.getInstance();
        nh.setup( rb, globalRb, period );
        logger.info ("done building Network object.");

		
		
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW();
		fw.initialize( tsPropertyMap, nh );


		// create highway skims object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating hwy skims object at: " + myDateString);
		Skims highwaySkims = new Skims( nh, tsPropertyMap, globalPropertyMap );

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

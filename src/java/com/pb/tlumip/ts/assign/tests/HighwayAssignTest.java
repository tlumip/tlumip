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


import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.NetworkHandlerIF;
import com.pb.tlumip.ts.assign.FW;
import com.pb.tlumip.ts.assign.Skims;
import com.pb.tlumip.ts.assign.TripDataGenerator;
import org.apache.log4j.Logger;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.ResourceBundle;


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
        setupNetwork( nh, tsPropertyMap, globalPropertyMap, period );
        logger.info ("done building Network object.");

		
		
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW();
		fw.initialize( tsPropertyMap, nh );


		// create highway skims object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating hwy skims object at: " + myDateString);
		Skims highwaySkims = new Skims( nh, rb, globalRb );

		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("generating trips with gravity model at: " + myDateString);
        double[][] linkAttribs = new double[1][];
        linkAttribs[0] = nh.getCongestedTime();
        
        String[] matrixName = new String[1];
        matrixName[0] = "time";
        
        String[] matrixDescription = new String[1];
        matrixDescription[0] = "Skims.main() test time matrix";
        
        Matrix[] m = highwaySkims.getHwySkimMatrices ( "peak", linkAttribs, matrixName, matrixDescription, 'a' );

        TripDataGenerator tdm = new TripDataGenerator ( m[0].getValues() );


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
    
    private void setupNetwork ( NetworkHandlerIF nh, HashMap appMap, HashMap globalMap, String timePeriod ) {
        
        String networkFileName = (String)appMap.get("d211.fileName");
        String networkDiskObjectFileName = (String)appMap.get("NetworkDiskObject.file");
        
        String turnTableFileName = (String)appMap.get( "d231.fileName" );
        String networkModsFileName = (String)appMap.get( "d211Mods.fileName" );
        
        String vdfFileName = (String)appMap.get("vdf.fileName");
        String vdfIntegralFileName = (String)appMap.get("vdfIntegral.fileName");
        
        String a2bFileName = (String) globalMap.get( "alpha2beta.file" );
        
        // get peak or off-peak volume factor from properties file
        String volumeFactor="";
        if ( timePeriod.equalsIgnoreCase( "peak" ) )
            volumeFactor = (String)globalMap.get("am.peak.volume.factor");
        else if ( timePeriod.equalsIgnoreCase( "offpeak" ) )
            volumeFactor = (String)globalMap.get("offpeak.volume.factor");
        else {
            logger.error ( "time period specifed as: " + timePeriod + ", but must be either 'peak' or 'offpeak'." );
            System.exit(-1);
        }
        
        String userClassesString = (String)appMap.get("userClass.modes");
        String truckClass1String = (String)appMap.get( "truckClass1.modes" );
        String truckClass2String = (String)appMap.get( "truckClass2.modes" );
        String truckClass3String = (String)appMap.get( "truckClass3.modes" );
        String truckClass4String = (String)appMap.get( "truckClass4.modes" );
        String truckClass5String = (String)appMap.get( "truckClass5.modes" );

        String walkSpeed = (String)globalMap.get( "sdt.walk.mph" );
        
        
        String[] propertyValues = new String[NetworkHandler.NUMBER_OF_PROPERTY_VALUES];
        
        propertyValues[NetworkHandlerIF.NETWORK_FILENAME_INDEX] = networkFileName;
        propertyValues[NetworkHandlerIF.NETWORK_DISKOBJECT_FILENAME_INDEX] = networkDiskObjectFileName;
        propertyValues[NetworkHandlerIF.VDF_FILENAME_INDEX] = vdfFileName;
        propertyValues[NetworkHandlerIF.VDF_INTEGRAL_FILENAME_INDEX] = vdfIntegralFileName;
        propertyValues[NetworkHandlerIF.ALPHA2BETA_FILENAME_INDEX] = a2bFileName;
        propertyValues[NetworkHandlerIF.TURNTABLE_FILENAME_INDEX] = turnTableFileName;
        propertyValues[NetworkHandlerIF.NETWORKMODS_FILENAME_INDEX] = networkModsFileName;
        propertyValues[NetworkHandlerIF.VOLUME_FACTOR_INDEX] = volumeFactor;
        propertyValues[NetworkHandlerIF.USER_CLASSES_STRING_INDEX] = userClassesString;
        propertyValues[NetworkHandlerIF.TRUCKCLASS1_STRING_INDEX] = truckClass1String;
        propertyValues[NetworkHandlerIF.TRUCKCLASS2_STRING_INDEX] = truckClass2String;
        propertyValues[NetworkHandlerIF.TRUCKCLASS3_STRING_INDEX] = truckClass3String;
        propertyValues[NetworkHandlerIF.TRUCKCLASS4_STRING_INDEX] = truckClass4String;
        propertyValues[NetworkHandlerIF.TRUCKCLASS5_STRING_INDEX] = truckClass5String;
        propertyValues[NetworkHandlerIF.WALK_SPEED_INDEX] = walkSpeed;
        
        nh.setupHighwayNetworkObject ( timePeriod, propertyValues );
        
    }

}

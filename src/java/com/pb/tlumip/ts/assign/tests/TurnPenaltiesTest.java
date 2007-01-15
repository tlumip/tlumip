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
import com.pb.tlumip.ts.assign.ShortestPathTreeH;
import com.pb.common.util.ResourceUtil;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;



public class TurnPenaltiesTest {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.assign.tests");

    
    ResourceBundle rb;
    ResourceBundle globalRb;
	
	
	
	public TurnPenaltiesTest() {

        rb = ResourceUtil.getPropertyBundle( new File("/jim/tlumip/data/test/tpTest.properties") );
        globalRb = ResourceUtil.getPropertyBundle(new File("/jim/util/svn_workspace/projects/tlumip/config/global.properties"));

	}
    
	
	private void runTest (String period) {
        
        NetworkHandlerIF nh = NetworkHandler.getInstance();
        setupNetwork( nh, ResourceUtil.changeResourceBundleIntoHashMap(rb), ResourceUtil.changeResourceBundleIntoHashMap(globalRb), period );
		logger.info ("done building Network object.");

		
		// build shortest path tree object and set cost and valid link attributes for this user class.
		ShortestPathTreeH sp = new ShortestPathTreeH( nh );

		// set the highway network attribute on which to skim the network
		double[] linkCost = nh.getCongestedTime();
		
		// set all links as valid for this test
		boolean[] validLinks = new boolean[nh.getLinkCount()];
		Arrays.fill (validLinks, true);
   
		sp.setLinkCost( linkCost );
		sp.setValidLinks( validLinks );
		

		// build and print shortest path from 1 to 2
		sp.printPath( 1, 2 );
		
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
            volumeFactor = (String)globalMap.get("AM_PEAK_VOL_FACTOR");
        else if ( timePeriod.equalsIgnoreCase( "offpeak" ) )
            volumeFactor = (String)globalMap.get("OFF_PEAK_VOL_FACTOR");
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

        String walkSpeed = (String)globalMap.get( "WALK_MPH" );
        
        
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
        
        nh.buildNetworkObject ( timePeriod, propertyValues );
        
    }
    
    
	public static void main (String[] args) {
        
		long startTime = System.currentTimeMillis();
		
		TurnPenaltiesTest test = new TurnPenaltiesTest();
		test.runTest("peak");
        
		logger.info("TurnPenaltiesTest() finished in " +
			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");
	}


}

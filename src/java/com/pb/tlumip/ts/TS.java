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
package com.pb.tlumip.ts;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 6/30/2004
 */


import com.pb.tlumip.ts.assign.Skims;
import com.pb.tlumip.ts.assign.FW;


import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.util.Arrays;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;



public class TS {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts");


	final char[] highwayModeCharacters = { 'a', 'd', 'e', 'f', 'g', 'h' };

	HashMap tsPropertyMap;
    HashMap globalPropertyMap;

    ResourceBundle appRb;
    ResourceBundle globalRb;
    
    NetworkHandler g = null;
	double[][][] multiclassTripTable = new double[highwayModeCharacters.length][][];


	
	
	
	public TS( String appPropertyName, String globalPropertyName ) {

        tsPropertyMap = ResourceUtil.getResourceBundleAsHashMap( appPropertyName );
        globalPropertyMap = ResourceUtil.getResourceBundleAsHashMap( globalPropertyName );
		
	}

    public TS(ResourceBundle appRb, ResourceBundle globalRb) {

        this.appRb = appRb;
        this.globalRb = globalRb;
        
        tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

	}


    public void runHighwayAssignment( String assignmentPeriod ) {
    	
    	// define assignment related variables dependent on the assignment period
    	initializeHighwayAssignment ( assignmentPeriod );

    	// load the trips from PT and CT trip lists into multiclass o/d demand matrices for assignment
		createMulticlassDemandMatrices ( assignmentPeriod );
		
		// run the multiclass assignment for the time period
    	multiclassEquilibriumHighwayAssignment ( assignmentPeriod );
		
    	// write the auto time and distance highway skim matrices to disk
    	writeHighwaySkimMatrices ( assignmentPeriod, 'a' );
		
    	// if at some point in time we want to have truck specific highway skims,
    	// we'd create them here and would modify the the properties file to include
    	// class specific naming in skims file properties file keynames.  We'd also
    	// modify the method above to distinguish the class id in addition to period
    	// and skim types.
    	
		
    }
    
    
    
    private void initializeHighwayAssignment ( String assignmentPeriod ) {
        
        String myDateString = DateFormat.getDateTimeInstance().format(new Date());

        logger.info ("creating peak Highway Network object for assignment at: " + myDateString);
        g = new NetworkHandler();
        g.setup( appRb, globalRb, assignmentPeriod );

    }
	
	
    private void createMulticlassDemandMatrices ( String assignmentPeriod ) {
        
		DemandHandler d = new DemandHandler();
        d.setNetworkAttributes( g.getNumCentroids(), g.getNumUserClasses(), g.getNodeIndex(), g.getAssignmentGroupMap(), g.userClassesIncludeTruck() );
        d.setup( appRb, globalRb, assignmentPeriod );
        
        multiclassTripTable = d.getMulticlassTripTables();
        
    }

    
	
    private void multiclassEquilibriumHighwayAssignment ( String assignmentPeriod ) {
        
		long startTime = System.currentTimeMillis();
		
		String myDateString;

		
		
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating + " + assignmentPeriod + " FW object at: " + myDateString);
		FW fw = new FW();
		fw.initialize( tsPropertyMap, g );


		// Compute Frank-Wolfe solution
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("starting + " + assignmentPeriod + " fw at: " + myDateString);
		fw.iterate ( multiclassTripTable );
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done with + " + assignmentPeriod + " fw at: " + myDateString);

        logger.info( assignmentPeriod + " highway assignment finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes.");

        
        
        
        String assignmentResultsFileName = null;

        if ( assignmentPeriod.equalsIgnoreCase( "peak" ) ) {
            // get peak period definitions from property files
            assignmentResultsFileName = (String)tsPropertyMap.get("peakOutput.fileName");
        }
        else if ( assignmentPeriod.equalsIgnoreCase( "offpeak" ) ) {
            // get off-peak period definitions from property files
            assignmentResultsFileName = (String)tsPropertyMap.get("offpeakOutput.fileName");
        }

        
		logger.info("Writing results file with " + assignmentPeriod + " assignment results.");
        g.writeNetworkAttributes( assignmentResultsFileName );

		
        
        logger.info( "\ndone with " + assignmentPeriod + " period assignment."); 
        
    }
	

    
    public void checkNetworkForIsolatedLinks () {
		g.checkForIsolatedLinks ();
    }
    
    
    
    public void checkAllODPairsForNetworkConnectivity () {
    	
        double[][][] dummyTripTable = new double[g.getUserClasses().length][g.getNumCentroids()+1][g.getNumCentroids()+1];
		for(int i=0; i < g.getUserClasses().length - 1; i++) {
			for(int j=0; j < g.getNumCentroids() + 1; j++) {
				Arrays.fill(dummyTripTable[i][j], 1.0);
			}
		}
		g.checkODConnectivity(dummyTripTable);

    }
    
    
    
    public void checkODPairsWithTripsForNetworkConnectivity () {
		g.checkODConnectivity(multiclassTripTable);
    }

    
    
    
    public void writeHighwaySkimMatrix ( String assignmentPeriod, String skimType, char modeChar ) {

		logger.info("Writing " + assignmentPeriod + " time skim matrix for highway mode " + modeChar + " to disk...");
        long startTime = System.currentTimeMillis();
        
    	Skims skims = new Skims(g, tsPropertyMap, globalPropertyMap);
    	
        skims.writeHwySkimMatrix ( assignmentPeriod, skimType, modeChar);

        logger.info("wrote the " + assignmentPeriod + " " + skimType + " skims for mode " + modeChar + " in " +
    			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

    }


    
    public void writeHighwaySkimMatrices ( String assignmentPeriod, char modeChar ) {

    	String[] skimTypeArray = { "time", "dist" };
    	
    	
		logger.info("Writing " + assignmentPeriod + " time and dist skim matrices for highway mode " + modeChar + " to disk...");
        long startTime = System.currentTimeMillis();
        
    	Skims skims = new Skims(g, tsPropertyMap, globalPropertyMap);
    	
        skims.writeHwySkimMatrices ( assignmentPeriod, skimTypeArray, modeChar);

        logger.info("wrote the " + assignmentPeriod + " time and dist skims for mode " + modeChar + " in " +
    			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

    }


    
    public static void main (String[] args) {
        
        TS tsTest = new TS( ResourceBundle.getBundle("ts"), ResourceBundle.getBundle ("global") );

        // run peak highway assignment
		tsTest.runHighwayAssignment( "peak" );
//		tsTest.runHighwayAssignment( "offpeak" );
		
		logger.info ("\ndone with TS run.");
		
    }

}

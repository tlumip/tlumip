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


import com.pb.tlumip.model.ModeType;
import com.pb.tlumip.ts.assign.FW;
import com.pb.tlumip.ts.assign.Network;
import com.pb.tlumip.ts.assign.Skims;


import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;



public class TS {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts");


	final char[] highwayModeCharacters = { 'a', 'd', 'e', 'f', 'g', 'h' };

	HashMap tsPropertyMap;
    HashMap globalPropertyMap;

	String ptFileName = null;
	String ctFileName = null;
	String assignmentResultsFileName = null;
	
	double[][][] multiclassTripTable = new double[highwayModeCharacters.length][][];
	Network g = null;
	int startHour;
	int endHour;
	float volumeFactor;
	

	
	
	
	public TS() {

        tsPropertyMap = ResourceUtil.getResourceBundleAsHashMap("ts");
		
	}

    public TS(ResourceBundle appRb, ResourceBundle globalRb) {

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
		
    }
    
    
    
    private void initializeHighwayAssignment ( String assignmentPeriod ) {
        
    	String myDateString = null;
		long startTime = System.currentTimeMillis();
		
	    // get trip list filenames from property file
		ptFileName = (String)tsPropertyMap.get("pt.fileName");
		ctFileName = (String)tsPropertyMap.get("ct.fileName");

		

		if ( assignmentPeriod.equalsIgnoreCase( "peak" ) ) {
			// get peak period definitions from property files
			startHour = Integer.parseInt( (String)globalPropertyMap.get("AM_PEAK_START") );
			endHour = Integer.parseInt( (String)globalPropertyMap.get("AM_PEAK_END") );
			volumeFactor = Float.parseFloat( (String)globalPropertyMap.get("AM_PEAK_VOL_FACTOR") );
			assignmentResultsFileName = (String)tsPropertyMap.get("peakOutput.fileName");
		}
		else if ( assignmentPeriod.equalsIgnoreCase( "offpeak" ) ) {
			// get off-peak period definitions from property files
			startHour = Integer.parseInt( (String)globalPropertyMap.get("OFF_PEAK_START") );
			endHour = Integer.parseInt( (String)globalPropertyMap.get("OFF_PEAK_END") );
			volumeFactor = Float.parseFloat( (String)globalPropertyMap.get("OFF_PEAK_VOL_FACTOR") );
			assignmentResultsFileName = (String)tsPropertyMap.get("offpeakOutput.fileName");
		}

		
        myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating peak Highway Network object for assignment at: " + myDateString);
		g = new Network( tsPropertyMap, globalPropertyMap, assignmentPeriod, volumeFactor );

    }
	
	
    private void createMulticlassDemandMatrices ( String assignmentPeriod ) {
        
		long startTime = System.currentTimeMillis();
		
		int totalTrips;
		int linkCount;
		String myDateString;

		// check that at least one valid user class has been defined
		if ( g.getNumUserClasses() == 0 ) {
			logger.error ( "No valid user classes defined in ts.properties file.", new RuntimeException() );
		}
		
		HashMap assignmentGroupMap = g.getAssignmentGroupMap();
		
		// read PT trip list into o/d trip matrix if auto user class was defined
		if ( assignmentGroupMap.containsKey( String.valueOf('a') ) ) {
			int assignmentGroupIndex = ((Integer)assignmentGroupMap.get( String.valueOf('a') )).intValue(); 
			myDateString = DateFormat.getDateTimeInstance().format(new Date());
			logger.info ("reading " + assignmentPeriod + " PT trip list at: " + myDateString);
			multiclassTripTable[0] = getAutoTripTableFromPTList ( g, ptFileName, startHour, endHour );
		}
		else {
			logger.info ("no auto class defined, so " + assignmentPeriod + " PT trip list was not read." );
		}

        
		// read CT trip list into o/d trip matrix if at least one truck class was defined
		if ( g.userClassesIncludeTruck() ) {
			myDateString = DateFormat.getDateTimeInstance().format(new Date());
			logger.info ("reading " + assignmentPeriod + " CT trip list at: " + myDateString);
			double[][][] truckTripTables = getTruckAssignmentGroupTripTableFromCTList ( g, ctFileName, startHour, endHour );

			for(int i=0; i < truckTripTables.length - 1; i++)
				multiclassTripTable[i+1] = truckTripTables[i];
		}


		
    }
	
	
    private void multiclassEquilibriumHighwayAssignment ( String assignmentPeriod ) {
        
		long startTime = System.currentTimeMillis();
		
		int totalTrips;
		int linkCount;
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

        
		logger.info("Writing results file with " + assignmentPeriod + " assignment results.");
        writeAssignmentResults(g, assignmentResultsFileName);

		
        
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


    
    private double[][] getAggregateTripTableFromCsvFile ( Network g, String fileName, int numCentroids ) {
        
        int orig;
        int dest;
        int o;
        int d;
        float tripCount=0;
        float trips;
        
        int[] nodeIndex = null;
        
        double[][] tripTable = new double[g.getNumCentroids()+1][g.getNumCentroids()+1];

        
		// read the aggregate trip table into a TableDataSet
		CSVFileReader reader = new CSVFileReader();
        
		TableDataSet table = null;
		try {
			table = reader.readFile(new File( fileName ));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    

		
		nodeIndex = g.getNodeIndex();
		
		
		// traverse the trip list in the TableDataSet and aggregate trips to an o/d trip table
		for (int i=0; i < table.getRowCount(); i++) {
		    
		    // csv file has orig, dest, trips
			orig = (int)table.getValueAt( i+1, 1 );
			dest = (int)table.getValueAt( i+1, 2 );
			trips = table.getValueAt( i+1, 3 );
			
			o = nodeIndex[orig];
			d = nodeIndex[dest];
			
			// accumulate all peak period highway mode trips
		    tripTable[o][d] += trips;
			tripCount += trips;
			
		}
		
		// done with trip list TabelDataSet
		table = null;

		logger.info (tripCount + " trips read from csv file.");

		return tripTable;
		    
    }
    
    
    
    private double[][] getAutoTripTableFromPTList ( Network g, String fileName, int startPeriod, int endPeriod ) {
        
        int orig;
        int dest;
        int startTime;
        int mode;
        int o;
        int d;
        int tripCount=0;
        
        int[] nodeIndex = null;
        
        double[][] tripTable = new double[g.getNumCentroids()+1][g.getNumCentroids()+1];
        

        
		// read the PT output person trip list file into a TableDataSet
		CSVFileReader reader = new CSVFileReader();

		String[] columnsToRead = { "origin", "destination", "tripStartTime", "tripMode" };
		TableDataSet table = null;
		try {
			if ( fileName != null) {

				table = reader.readFile(new File( fileName ), columnsToRead);

				nodeIndex = g.getNodeIndex();
				
				// traverse the trip list in the TableDataSet and aggregate trips to an o/d trip table
				for (int i=0; i < table.getRowCount(); i++) {
				    
					orig = (int)table.getValueAt( i+1, "origin" );
					dest = (int)table.getValueAt( i+1, "destination" );
					startTime = (int)table.getValueAt( i+1, "tripStartTime" );
					mode = (int)table.getValueAt( i+1, "tripMode" );
					
					o = nodeIndex[orig];
					d = nodeIndex[dest];
					
					// accumulate all peak period highway mode trips
					if ( (mode == ModeType.AUTODRIVER || mode == ModeType.AUTOPASSENGER) && (startTime >= startPeriod && startTime <= endPeriod) ) {
	
					    tripTable[o][d] ++;
						tripCount++;
	
					}
					
				}
				
				// done with trip list TabelDataSet
				table = null;

			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}


		logger.info (tripCount + " total auto network trips read from PT file for period " + startPeriod +
                " to " + endPeriod);

		return tripTable;
		    
    }
    

    private double[][][] getTruckAssignmentGroupTripTableFromCTList ( Network g, String fileName, int startPeriod, int endPeriod ) {

        int orig;
        int dest;
        int startTime;
        int mode;
        int o;
        int d;
        int group;
        char modeChar;
        String truckType;
        double tripFactor = 1.0;
        

        HashMap assignmentGroupMap = g.getAssignmentGroupMap();
        int numAssignmentGroups = assignmentGroupMap.keySet().size();
		if ( assignmentGroupMap.containsKey( String.valueOf('a') ) )
			numAssignmentGroups--;
        
        int[] nodeIndex = null;

        
        double[] tripsByUserClass = new double[highwayModeCharacters.length];
        double[] tripsByAssignmentGroup = new double[numAssignmentGroups];
        double[][][] tripTable = new double[numAssignmentGroups][g.getNumCentroids()+1][g.getNumCentroids()+1];



		// read the PT output person trip list file into a TableDataSet
		CSVFileReader reader = new CSVFileReader();

		TableDataSet table = null;
		int tripRecord = 0;
		try {
			if ( fileName != null ) {

				table = reader.readFile(new File( fileName ));

				nodeIndex = g.getNodeIndex();

				// traverse the trip list in the TableDataSet and aggregate trips to an o/d trip table
				for (int i=0; i < table.getRowCount(); i++) {
					tripRecord = i+1;
					
					orig = (int)table.getValueAt( i+1, "origin" );
					dest = (int)table.getValueAt( i+1, "destination" );
					startTime = (int)table.getValueAt( i+1, "tripStartTime" );
					truckType = (String)table.getStringValueAt( i+1, "truckType" );
					tripFactor = (int)table.getValueAt( i+1, "tripFactor" );
	
					mode = Integer.parseInt( truckType.substring(3) );
					modeChar = highwayModeCharacters[mode];
					group = ((Integer)assignmentGroupMap.get( String.valueOf( modeChar ) )).intValue();
					
					o = nodeIndex[orig];
					d = nodeIndex[dest];
	
					// accumulate all peak period highway mode trips
					if ( startTime >= startPeriod && startTime <= endPeriod ) {
	
					    tripTable[group-1][o][d] += tripFactor;
					    tripsByUserClass[mode-1] += tripFactor;
					    tripsByAssignmentGroup[group-1] += tripFactor;
	
					}
	
				}
	
				// done with trip list TabelDataSet
				table = null;

			}
			
		} catch (Exception e) {
			logger.error ("exception caught reading CT truck trip record " + tripRecord, e);
			System.exit(-1);
		}

		
		logger.info ("trips by truck user class read from CT file from " + startPeriod + " to " + endPeriod + ":");
		for (int i=0; i < tripsByUserClass.length; i++)
			if (tripsByUserClass[i] > 0)
				logger.info ( tripsByUserClass[i] + " truck trips with user class " + highwayModeCharacters[i+1] );

		logger.info ("trips by truck assignment groups read from CT file from " + startPeriod + " to " + endPeriod + ":");
		for (int i=0; i < tripsByAssignmentGroup.length; i++)
			if (tripsByAssignmentGroup[i] > 0)
				logger.info ( tripsByAssignmentGroup[i] + " truck trips in assignment group " + (i+1) );

		return tripTable;

    }



    private void writeAssignmentResults( Network g, String fileName ) {
    	
    	g.writeNetworkAttributes(fileName);
    	
    }




    public static void main (String[] args) {
        
        TS tsTest = new TS( ResourceBundle.getBundle("ts"), ResourceBundle.getBundle ("global") );

		tsTest.runHighwayAssignment( "peak" );
//		tsTest.runHighwayAssignment( "offpeak" );
		
		logger.info ("\ndone with TS run.");
		
    }

}

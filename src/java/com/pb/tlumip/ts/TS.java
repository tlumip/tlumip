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
import com.pb.common.util.ObjectUtil;

import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;



public class TS {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts");


	HashMap tsPropertyMap;
    HashMap globalPropertyMap;

	String ptFileName = null;
	String ctFileName = null;
	String peakOutputFileName = null;
	String offpeakOutputFileName = null;
	
	
	
	public TS() {

        tsPropertyMap = ResourceUtil.getResourceBundleAsHashMap("ts");

		initTS();
		
	}

    public TS(ResourceBundle appRb, ResourceBundle globalRb) {

        tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);
		initTS();

	}



    public static void main (String[] args) {
        
        TS tsTest = new TS( ResourceBundle.getBundle("ts"), ResourceBundle.getBundle ("global") );

		tsTest.assignPeakAuto();
//		tsTest.assignOffPeakAuto();

		logger.info ("\ndone with TS run.");
    }


    


	private void initTS() {

	    // get trip list filenames from property file
		ptFileName = (String)tsPropertyMap.get("pt.fileName");
		ctFileName = (String)tsPropertyMap.get("ct.fileName");

		peakOutputFileName = (String)tsPropertyMap.get("peakOutput.fileName");
		offpeakOutputFileName = (String)tsPropertyMap.get("offpeakOutput.fileName");
	}

	
    public void assignPeakAuto () {
        
		long startTime = System.currentTimeMillis();
		
		double[][][] multiclassTripTable = null;
		double[][][] truckTripTables = null;
		double[][] autoTripTable = null;
		
		Network g = null;
		

		int totalTrips;
		int linkCount;
		String myDateString;

		// get peak period definitions from global property file
		int peakStart = Integer.parseInt( (String)globalPropertyMap.get("AM_PEAK_START") );
		int peakEnd = Integer.parseInt( (String)globalPropertyMap.get("AM_PEAK_END") );
		float peakFactor = Float.parseFloat( (String)globalPropertyMap.get("AM_PEAK_VOL_FACTOR") );
		
        myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating peak Highway Network object for assignment at: " + myDateString);
		g = new Network( tsPropertyMap, globalPropertyMap, "peak", peakFactor );

	    long size = ObjectUtil.sizeOf( g );
	    logger.info("Approximate size of " + g + " object :" + ((float)size/(1024.0*1024.0)) + " MB.");
		
	
	
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW();
		fw.initialize( tsPropertyMap, g );

		// read PT trip list into o/d trip matrix
        myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("reading PT trip list at: " + myDateString);
		autoTripTable = getAutoTripTableFromPTList ( g, ptFileName, peakStart, peakEnd );

        
		// read CT trip list into o/d trip matrix
		if ( g.getUserClasses().length > 1 ) {
			myDateString = DateFormat.getDateTimeInstance().format(new Date());
			logger.info ("reading CT trip list at: " + myDateString);
			truckTripTables = getTruckTripTableFromCTList ( g, ctFileName, peakStart, peakEnd );
		}

		
		multiclassTripTable = new double[g.getUserClasses().length][][];
		multiclassTripTable[0] = autoTripTable;
		for(int i=0; i < g.getUserClasses().length - 1; i++)
			multiclassTripTable[i+1] = truckTripTables[i];

		
		// do some checks on network connectivity.
//		g.checkForIsolatedLinks ();
//        double[][][] dummyTripTable = new double[g.getUserClasses().length][g.getNumCentroids()+1][g.getNumCentroids()+1];
//		for(int i=0; i < g.getUserClasses().length - 1; i++) {
//			for(int j=0; j < g.getNumCentroids() + 1; j++) {
//				Arrays.fill(dummyTripTable[i][j], 1.0);
//			}
//		}
//		g.checkODConnectivity(dummyTripTable);
//		g.checkODConnectivity(multiclassTripTable);
		


		//Compute Frank-Wolfe solution
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("starting fw at: " + myDateString);
		fw.iterate ( multiclassTripTable );
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done with fw at: " + myDateString);

        logger.info("assignPeakAuto() finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");

        
		logger.info("Writing network file with peak assignment results");
        writeAssignmentResults(g, peakOutputFileName);

		
		
		logger.info("Writing Peak Auto (class 0) Time and Distance skims to disk");
        startTime = System.currentTimeMillis();
        writePeakSkims(g, tsPropertyMap, globalPropertyMap, g.getValidLinksForClass(0));
        logger.info("wrote the (class 0) peak skims in " +
    			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");


        
        // log the average sov trip travel distance and travel time for this assignment
        logger.info("Generating Time and Distance peak skims to use to calcluate average time and distance...");
        Skims skims = new Skims(g, tsPropertyMap, globalPropertyMap);
        double[] skimSummaries = skims.getAvgSovTripSkims(multiclassTripTable[0], g.getValidLinksForClass(0));
        logger.info( "Average Peak auto (class 0) trip travel distance = " + skimSummaries[0] + " miles."); 
        logger.info( "Average Peak auto (class 0) trip travel time = " + skimSummaries[1] + " minutes."); 

        logger.info( "\ndone with peak assignment."); 
        
    }
	
	
    public void assignOffPeakAuto () {

		long startTime = System.currentTimeMillis();

		double[][][] multiclassTripTable = null;
		double[][][] truckTripTables = null;
		double[][] autoTripTable = null;
		
		Network g = null;
		
        int totalTrips;
		int linkCount;
		String myDateString;

		
		
        // get off-peak period definitions from property file
		int offPeakStart = Integer.parseInt( (String)globalPropertyMap.get("OFF_PEAK_START") );
		int offPeakEnd = Integer.parseInt( (String)globalPropertyMap.get("OFF_PEAK_END") );
		float offPeakFactor = Float.parseFloat( (String)globalPropertyMap.get("OFF_PEAK_VOL_FACTOR") );

        myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating peak Highway Network object for assignment at: " + myDateString);
		g = new Network( tsPropertyMap, globalPropertyMap, "offpeak", offPeakFactor );

	    long size = ObjectUtil.sizeOf( g );
	    logger.info("Approximate size of " + g + " object :" + ((float)size/(1024.0*1024.0)) + " MB.");
		
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW();
		fw.initialize( tsPropertyMap, g );

		// read PT trip list into o/d trip matrix
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("reading PT trip list at: " + myDateString);
		autoTripTable = getAutoTripTableFromPTList ( g, ptFileName, offPeakStart, offPeakEnd );

		// read CT trip list into o/d trip matrix
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("reading CT trip list at: " + myDateString);
		truckTripTables = getTruckTripTableFromCTList ( g, ctFileName, offPeakStart, offPeakEnd );

		multiclassTripTable = new double[g.getUserClasses().length][][];
		multiclassTripTable[0] = autoTripTable;
		for(int i=0; i < g.getUserClasses().length - 1; i++)
			multiclassTripTable[i+1] = truckTripTables[i];

		
		//Compute Frank-Wolfe solution
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("starting fw at: " + myDateString);
		fw.iterate ( multiclassTripTable );
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done with fw at: " + myDateString);

        logger.info("assignOffPeakAuto() finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");


		logger.info("Writing network file with off-peak assignment results");
        writeAssignmentResults(g, offpeakOutputFileName);
		
		
        logger.info("Writing Off-Peak Auto (class 0) Time and Distance skims to disk...");
        startTime = System.currentTimeMillis();
        writeOffPeakSkims(g, tsPropertyMap, globalPropertyMap, g.getValidLinksForClass(0));
        logger.info("wrote the Off-Peak skims in " +
			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

        // log the average sov trip travel distance and travel time for this assignment
        logger.info("Generating Time and Distance off-peak skims to use to calcluate average time and distance...");
        Skims skims = new Skims(g, tsPropertyMap, globalPropertyMap);
        double[] skimSummaries = skims.getAvgSovTripSkims(multiclassTripTable[0], g.getValidLinksForClass(0));
        logger.info( "Average Off-Peak auto (class 0) trip travel distance = " + skimSummaries[0] + " miles."); 
        logger.info( "Average Off-Peak auto (class 0) trip travel time = " + skimSummaries[1] + " minutes."); 
        
        logger.info( "\ndone with off-peak assignment."); 
    }

    public void writePeakSkims(Network g, HashMap tsMap, HashMap globalMap, boolean[] validLinks){
        Skims skims = new Skims(g, tsMap, globalMap);
        logger.info ("skimming network and creating pk time and distance matrices.");
//        skims.writePeakSovTimeSkimMatrices(validLinks);  //writes the alpha and beta pktime skims
        skims.writeSovDistSkimMatrices(validLinks);     //writes alpha and beta pkdist  and
                                                // off-peak distance skims
    }

    public void writeOffPeakSkims(Network g, HashMap map, HashMap globalMap, boolean[] validLinks){
        Skims skims = new Skims(g, map, globalMap);
        logger.info ("skimming network and creating off-pk time matrices.");
        skims.writeOffPeakSovTimeSkimMatrices(validLinks);    //writes the alpha off-peak time skim
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
    

    private double[][][] getTruckTripTableFromCTList ( Network g, String fileName, int startPeriod, int endPeriod ) {

        int orig;
        int dest;
        int startTime;
        int mode;
        int o;
        int d;
        String truckType;
        double tripFactor = 1.0;

        int[] nodeIndex = null;

        char[] userClasses = g.getUserClasses();
        
        // the first userclass is auto, and is not relevant for truck trips, so there are userClasses.length - 1 truck classes
        double[] tripCount = new double[userClasses.length - 1];
        double[][][] tripTable = new double[userClasses.length - 1][g.getNumCentroids()+1][g.getNumCentroids()+1];



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
					o = nodeIndex[orig];
					d = nodeIndex[dest];
	
					// accumulate all peak period highway mode trips
					if ( startTime >= startPeriod && startTime <= endPeriod ) {
	
					    tripTable[mode-1][o][d] += tripFactor;
						tripCount[mode-1] += tripFactor;
	
					}
	
				}
	
				// done with trip list TabelDataSet
				table = null;

			}
			
		} catch (Exception e) {
			logger.error ("exception caught reading CT truck trip record " + tripRecord, e);
			System.exit(-1);
		}

		
		logger.info ("truck network trips read from CT file from " + startPeriod + " to " + endPeriod + ":");
		for (int i=0; i < tripCount.length; i++)
			if (tripCount[i] > 0)
				logger.info (tripCount[i] + " truck trips with user class " + userClasses[i+1] );

		return tripTable;

    }



    private void writeAssignmentResults( Network g, String fileName ) {
    	
    	g.writeNetworkAttributes(fileName);
    	
    }
}

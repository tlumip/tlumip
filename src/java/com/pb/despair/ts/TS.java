package com.pb.despair.ts;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 6/30/2004
 */


import com.pb.despair.model.ModeType;
import com.pb.despair.ts.assign.FW;
import com.pb.despair.ts.assign.Network;
import com.pb.despair.ts.assign.Skims;


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

	protected static Logger logger = Logger.getLogger("com.pb.despair.ts");


	HashMap tsPropertyMap;
    HashMap globalPropertyMap;

	String ptFileName = null;
	String ctFileName = null;
	String peakOutputFileName = null;
	String offpeakOutputFileName = null;
	
	int peakStart;
	int peakEnd;
	float peakFactor;

    int offPeakStart;
	int offPeakEnd;
	float offPeakFactor;

	double[][][] multiclassTripTable = new double[2][][];
	
	Network g = null;
	
	
	
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
//		TS tsTest = new TS();

		tsTest.assignPeakAuto();
		tsTest.assignOffPeakAuto();

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
		

		int totalTrips;
		int linkCount;
		String myDateString;

        myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating peak Highway Network object for assignment at: " + myDateString);
		g = new Network( tsPropertyMap, globalPropertyMap, "peak" );

	    long size = ObjectUtil.sizeOf( g );
	    logger.info("Approximate size of " + g + " object :" + ((float)size/(1024.0*1024.0)) + " MB.");
		
		// get peak period definitions from global property file
		peakStart = Integer.parseInt( (String)globalPropertyMap.get("AM_PEAK_START") );
		peakEnd = Integer.parseInt( (String)globalPropertyMap.get("AM_PEAK_END") );
		peakFactor = Float.parseFloat( (String)globalPropertyMap.get("AM_PEAK_VOL_FACTOR") );
		g.setVolumeFactor(peakFactor);

	
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW( tsPropertyMap, g );

	
		// read PT trip list into o/d trip matrix
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("reading PT trip list at: " + myDateString);
		multiclassTripTable[0] = getAutoTripTableFromPTList ( ptFileName, peakStart, peakEnd );

		// read CT trip list into o/d trip matrix
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("reading CT trip list at: " + myDateString);
		multiclassTripTable[1] = getTruckTripTableFromCTList ( ctFileName, peakStart, peakEnd );

		//Compute Frank-Wolfe solution
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("starting fw at: " + myDateString);
		fw.iterate ( multiclassTripTable );
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done with fw at: " + myDateString);

        logger.info("assignPeakAuto() finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");

        
		logger.info("Writing network file with peak assignment results");
        writeAssignmentResults(peakOutputFileName);
		
		
		logger.info("Writing Peak Time and Distance skims to disk");
        startTime = System.currentTimeMillis();
        writePeakSkims(g, tsPropertyMap, globalPropertyMap);
        logger.info("wrote the peak skims in " +
			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

    }
	
	
    public void assignOffPeakAuto () {

		long startTime = System.currentTimeMillis();

        int totalTrips;
		int linkCount;
		String myDateString;

		
		
        // get off-peak period definitions from property file
		offPeakStart = Integer.parseInt( (String)globalPropertyMap.get("OFF_PEAK_START") );
		offPeakEnd = Integer.parseInt( (String)globalPropertyMap.get("OFF_PEAK_END") );
		offPeakFactor = Float.parseFloat( (String)globalPropertyMap.get("OFF_PEAK_VOL_FACTOR") );
		g.setVolumeFactor(offPeakFactor);

		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW( tsPropertyMap, g );

		// read PT trip list into o/d trip matrix
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("reading PT trip list at: " + myDateString);
		multiclassTripTable[0] = getAutoTripTableFromPTList ( ptFileName, offPeakStart, offPeakEnd );

		// read CT trip list into o/d trip matrix
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("reading CT trip list at: " + myDateString);
		multiclassTripTable[1] = getTruckTripTableFromCTList ( ctFileName, offPeakStart, offPeakEnd );

		//Compute Frank-Wolfe solution
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("starting fw at: " + myDateString);
		fw.iterate ( multiclassTripTable );
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done with fw at: " + myDateString);

        logger.info("assignOffPeakAuto() finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");


		logger.info("Writing network file with off-peak assignment results");
        writeAssignmentResults(offpeakOutputFileName);
		
		
        logger.info("Writing Off-Peak Time and Distance skims to disk");
        startTime = System.currentTimeMillis();
        writeOffPeakSkims(g, tsPropertyMap, globalPropertyMap);
        logger.info("wrote the Off-Peak skims in " +
			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

    }

    public void writePeakSkims(Network g, HashMap tsMap, HashMap globalMap){
        Skims skims = new Skims(g, tsMap, globalMap);
        logger.info ("skimming network and creating pk time and distance matrices.");
        skims.writePeakSovTimeSkimMatrices();  //writes the alpha and beta pktime skims
        skims.writeSovDistSkimMatrices();     //writes alpha and beta pkdist  and
                                                // off-peak distance skims
    }

    public void writeOffPeakSkims(Network g, HashMap map, HashMap globalMap){
        Skims skims = new Skims(g, map, globalMap);
        logger.info ("skimming network and creating off-pk time matrices.");
        skims.writeOffPeakSovTimeSkimMatrices();    //writes the alpha off-peak time skim
    }





    private double[][] getAggregateTripTableFromCsvFile ( String fileName ) {
        
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
    
    
    
    private double[][] getAutoTripTableFromPTList ( String fileName, int startPeriod, int endPeriod ) {
        
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
    

    private double[][] getTruckTripTableFromCTList ( String fileName, int startPeriod, int endPeriod ) {

        int orig;
        int dest;
        int startTime;
        int mode;
        int o;
        int d;
        int tripCount=0;
        double tripFactor;

        int[] nodeIndex = null;

        double[][] tripTable = new double[g.getNumCentroids()+1][g.getNumCentroids()+1];



		// read the PT output person trip list file into a TableDataSet
		CSVFileReader reader = new CSVFileReader();

		TableDataSet table = null;
		try {
			if ( fileName != null ) {

				table = reader.readFile(new File( fileName ));

				nodeIndex = g.getNodeIndex();

				// traverse the trip list in the TableDataSet and aggregate trips to an o/d trip table
				for (int i=0; i < table.getRowCount(); i++) {
	
					orig = (int)table.getValueAt( i+1, "origin" );
					dest = (int)table.getValueAt( i+1, "destination" );
					startTime = (int)table.getValueAt( i+1, "tripStartTime" );
					mode = (int)table.getValueAt( i+1, "tripMode" );
					tripFactor = (int)table.getValueAt( i+1, "tripFactor" );
	
					o = nodeIndex[orig];
					d = nodeIndex[dest];
	
					// accumulate all peak period highway mode trips
					if ( startTime >= startPeriod && startTime <= endPeriod ) {
	
					    tripTable[o][d] += tripFactor;
						tripCount += tripFactor;
	
					}
	
				}
	
				// done with trip list TabelDataSet
				table = null;

			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		logger.info (tripCount + " truck network trips read from CT file from " +
                startPeriod + " to " + endPeriod);

		return tripTable;

    }



    private void writeAssignmentResults( String fileName ) {
    	
    	g.writeNetworkAttributes(fileName);
    	
    }
}

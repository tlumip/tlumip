package com.pb.despair.ts;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 6/30/2004
 */


import com.pb.despair.model.ModeType;


import com.pb.common.assign.Network;
import com.pb.common.assign.FW;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Logger;



public class TS {

	protected static Logger logger = Logger.getLogger("com.pb.despair.ts");


	HashMap propertyMap;
	
	String ptFileName = null;
	String ctFileName = null;
	
	int peakStart;
	int peakEnd;
	float peakFactor;
	
	double[][][] multiclassTripTable = new double[2][][];
	
	Network g = null;
	
	
	
	public TS() {

        propertyMap = ResourceUtil.getResourceBundleAsHashMap("ts");


	    // get trip list filenames from property file
		ptFileName = (String)propertyMap.get("pt.fileName");
		ctFileName = (String)propertyMap.get("ct.fileName");

		// get peak period definitions from property file
		peakStart = Integer.parseInt( (String)propertyMap.get("amPeak.start") );
		peakEnd = Integer.parseInt( (String)propertyMap.get("amPeak.end") );
		peakFactor = Float.parseFloat( (String)propertyMap.get("amPeak.volumeFactor") );
		
	}

    public TS(ResourceBundle rb) {

        propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);


	    // get trip list filenames from property file
		ptFileName = (String)propertyMap.get("pt.fileName");
		ctFileName = (String)propertyMap.get("ct.fileName");

		// get peak period definitions from property file
		peakStart = Integer.parseInt( (String)propertyMap.get("amPeak.start") );
		peakEnd = Integer.parseInt( (String)propertyMap.get("amPeak.end") );
		peakFactor = Float.parseFloat( (String)propertyMap.get("amPeak.volumeFactor") );

	}



    public static void main (String[] args) {
        
		TS tsTest = new TS();

		tsTest.assignPeakAuto();
		
    }


    


    public void assignPeakAuto () {
        
		long startTime = System.currentTimeMillis();
		

		int totalTrips;
		int linkCount;
		String myDateString;

		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating Highway Network object at: " + myDateString);
		g = new Network( propertyMap );
		
		
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW( propertyMap, g );

		// read PT trip list into o/d trip matrix
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("reading PT trip list at: " + myDateString);
		multiclassTripTable[0] = getPeakAutoTripTableFromPTList ( ptFileName );

		// read CT trip list into o/d trip matrix
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("reading CT trip list at: " + myDateString);
		multiclassTripTable[1] = getPeakTruckTripTableFromCTList ( ctFileName );

		//Compute Frank-Wolfe solution
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("starting fw at: " + myDateString);
		fw.iterate ( multiclassTripTable );
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done with fw at: " + myDateString);


        
		logger.info("assignPeakAuto() finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");
		
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
    
    
    
    private double[][] getPeakAutoTripTableFromPTList ( String fileName ) {
        
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
        
		TableDataSet table = null;
		try {
			table = reader.readFile(new File( fileName ));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    

		
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
			if ( (mode == ModeType.AUTODRIVER || mode == ModeType.AUTOPASSENGER) && (startTime >= peakStart && startTime <= peakEnd) ) {

			    tripTable[o][d] ++;
				tripCount++;

			}
			
		}
		
		// done with trip list TabelDataSet
		table = null;

		logger.info (tripCount + " peak period auto network trips read from PT file.");

		return tripTable;
		    
    }
    

    private double[][] getPeakTruckTripTableFromCTList ( String fileName ) {
        
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
			table = reader.readFile(new File( fileName ));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    

		
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
			if ( startTime >= peakStart && startTime <= peakEnd ) {

			    tripTable[o][d] += tripFactor;
				tripCount += tripFactor;
			
			}
			
		}
		
		// done with trip list TabelDataSet
		table = null;

		logger.info (tripCount + " peak period truck network trips read from CT file.");

		return tripTable;
		    
    }

    
}

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
import com.pb.common.matrix.Matrix;

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

    int offPeakStart;
	int offPeakEnd;
	float offPeakFactor;

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

        // get off-peak period definitions from property file
		offPeakStart = Integer.parseInt( (String)propertyMap.get("offPeak.start") );
		offPeakEnd = Integer.parseInt( (String)propertyMap.get("offPeak.end") );
		offPeakFactor = Float.parseFloat( (String)propertyMap.get("offPeak.volumeFactor") );

        String myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating Highway Network object at: " + myDateString);
		g = new Network( propertyMap );

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

		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW( propertyMap, g );

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

        logger.info("Writing Peak Time and Distance skims to disk");
        startTime = System.currentTimeMillis();
        writePeakSkims(g, propertyMap);
        logger.info("wrote the peak skims in " +
			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

    }

    public void assignOffPeakAuto () {

		long startTime = System.currentTimeMillis();

        int totalTrips;
		int linkCount;
		String myDateString;

		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW( propertyMap, g );

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

        logger.info("Writing Off-Peak Time and Distance skims to disk");
        startTime = System.currentTimeMillis();
        writeOffPeakSkims(g, propertyMap);
        logger.info("wrote the Off-Peak skims in " +
			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

    }

    public void writePeakSkims(Network g, HashMap map){
        Skims skims = new Skims(g, map);
        logger.info ("skimming network and creating pk time and distance matrices.");
        skims.writePeakSovTimeSkimMatrices();  //writes the alpha and beta pktime skims
        skims.writeSovDistSkimMatrices();     //writes alpha and beta pkdist  and
                                                // off-peak distance skims
    }

    public void writeOffPeakSkims(Network g, HashMap map){
        Skims skims = new Skims(g, map);
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
			if ( (mode == ModeType.AUTODRIVER || mode == ModeType.AUTOPASSENGER) && (startTime >= startPeriod && startTime <= endPeriod) ) {

			    tripTable[o][d] ++;
				tripCount++;

			}
			
		}
		
		// done with trip list TabelDataSet
		table = null;

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
			if ( startTime >= startPeriod && startTime <= endPeriod ) {

			    tripTable[o][d] += tripFactor;
				tripCount += tripFactor;

			}

		}

		// done with trip list TabelDataSet
		table = null;

		logger.info (tripCount + " truck network trips read from CT file from " +
                startPeriod + " to " + endPeriod);

		return tripTable;

    }

    
}

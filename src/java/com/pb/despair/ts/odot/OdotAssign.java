package com.pb.despair.ts.odot;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 9/30/2004
 * 
 * This model implements an aggregate equilibrium traffic assignment procedure
 * and is intended for use with the ODOT R-based travel demand model package.
 * The traffic assignment class written in java is to be executed by an R program. 
 */



import com.pb.despair.ts.assign.Network;
import com.pb.despair.ts.assign.FW;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Logger;



public class OdotAssign {

	protected static Logger logger = Logger.getLogger("com.pb.despair.ts.odot");

	protected static Network g = null;

	
	
	public OdotAssign() {
	}
    
	
	
    public static void main (String[] args) {
        
       System.out.println ( assignAggregateTrips( args[0] ) );
		
    }

    
    
    public static String assignAggregateTrips ( String propertyFileName ) {
        
		long startTime = System.currentTimeMillis();
		
		String period = "peak";
		
		HashMap propertyMap;
		String tripFileName = null;
		double[][][] multiclassTripTable = new double[2][][];
		
		String loggerMessage=null;
		
		int totalTrips;
		int linkCount;
		String myDateString;

		// create a HashMap of properties values
	    int dotIndex = propertyFileName.indexOf("."); 
	    String subString = propertyFileName.substring( 0, dotIndex ); 
	    String propertyName = subString; 
        propertyMap = ResourceUtil.getResourceBundleAsHashMap( propertyName );
        
		
	    // get trip list filenames from property file
		tripFileName = (String)propertyMap.get("pt.fileName");


		
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating Highway Network object at: " + myDateString);
		g = new Network( propertyMap, period );
		
		
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW( propertyMap, g );

		// read PT trip list into o/d trip matrix
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("reading trip list at: " + myDateString);
		multiclassTripTable[0] = getAggregateTripTableFromCsvFile ( tripFileName );

		//Compute Frank-Wolfe solution
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("starting fw at: " + myDateString);
		fw.iterate ( multiclassTripTable );
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done with fw at: " + myDateString);


        loggerMessage = "assignAggregateTrips() finished in " + ( System.currentTimeMillis() - startTime ) / 60000.0  + " minutes";
		logger.info( loggerMessage );
		
		return loggerMessage;

    }
    
    
    
    private static double[][] getAggregateTripTableFromCsvFile ( String fileName ) {
        
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
    
}

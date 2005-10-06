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
package com.pb.tlumip.ts.odot;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 9/30/2004
 * 
 * This model implements an aggregate equilibrium traffic assignment procedure
 * and is intended for use with the ODOT R-based travel demand model package.
 * The traffic assignment class written in java is to be executed by an R program. 
 */



import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.assign.FW;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import org.apache.log4j.Logger;



public class OdotAssign {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.odot");

	protected static NetworkHandler g = null;

	
	
	public OdotAssign() {
	}
    
	
	
    public static void main (String[] args) {
        
       System.out.println ( assignAggregateTrips( args[0], args[1] ) );
		
    }

    
    
    public static String assignAggregateTrips ( String tsPropertyFileName, String globalPropertyFileName ) {
        
		long startTime = System.currentTimeMillis();
		
		String period = "peak";
		
		HashMap tsPropertyMap;
		String tripFileName = null;
		double[][][] multiclassTripTable = new double[2][][];
		
		String loggerMessage=null;
		
		String myDateString;

		// create a HashMap of ts properties values
	    int dotIndex = tsPropertyFileName.indexOf(".");
	    String subString = tsPropertyFileName.substring( 0, dotIndex );
	    String appPropertyName = subString;
        tsPropertyMap = ResourceUtil.getResourceBundleAsHashMap( appPropertyName );

        // create a HashMap of global properties values
	    dotIndex = globalPropertyFileName.indexOf(".");
	    subString = globalPropertyFileName.substring( 0, dotIndex );
        String globalPropertyName = subString;
        globalPropertyName = subString;


	    // get trip list filenames from property file
		tripFileName = (String)tsPropertyMap.get("pt.fileName");


		
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating Highway Network object at: " + myDateString);
        g = new NetworkHandler();
        g.setup( appPropertyName, globalPropertyName, period );
        logger.info ("done building Network object.");
		
		
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW();
		fw.initialize( tsPropertyMap, g );

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

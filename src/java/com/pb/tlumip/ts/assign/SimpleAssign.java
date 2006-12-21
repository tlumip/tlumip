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
package com.pb.tlumip.ts.assign;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 9/30/2004
 * 
 * This model implements an aggregate equilibrium traffic assignment procedure
 * and is intended for use with the ODOT R-based travel demand model package.
 * The traffic assignment class written in java is to be executed by an R program. 
 */



import com.pb.tlumip.model.ModeType;
import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.NetworkHandlerIF;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.util.ResourceBundle;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import org.apache.log4j.Logger;



public class SimpleAssign {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.odot");

	protected static NetworkHandlerIF nh = null;

    HashMap tsPropertyMap = null;
    HashMap globalPropertyMap = null;
    
	
	public SimpleAssign() {
	}
    
	
	
    public static void main (String[] args) {
        
        SimpleAssign a = new SimpleAssign();
        System.out.println ( a.assignAggregateTrips( args[0], args[1] ) );
		
    }

    
    
    public String assignAggregateTrips ( String tsPropertyFileName, String globalPropertyFileName ) {
        
		long startTime = System.currentTimeMillis();
		
		String period = "peak";
		
		double[][][] multiclassTripTable = new double[2][][];
		
		String loggerMessage=null;
		
		String myDateString;

		// create a HashMap of ts properties values
        tsPropertyMap = ResourceUtil.getResourceBundleAsHashMap( tsPropertyFileName );

        // create a HashMap of global properties values
        globalPropertyMap = ResourceUtil.getResourceBundleAsHashMap( globalPropertyFileName );


		
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating Highway Network object at: " + myDateString);
        nh = NetworkHandler.getInstance();
        nh.setup( ResourceBundle.getBundle(tsPropertyFileName), ResourceBundle.getBundle(globalPropertyFileName), period );
        logger.info ("done building Network object.");
		
		
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating FW object at: " + myDateString);
		FW fw = new FW();
		fw.initialize( tsPropertyMap, nh );

		// read PT trip list into o/d trip matrix
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("reading trip list at: " + myDateString);
		multiclassTripTable[0] = createMulticlassDemandMatrices()[0];

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
    
    
    
    private double[][][] createMulticlassDemandMatrices () {
        
        String myDateString;
        
        int networkNumUserClasses = nh.getNumUserClasses();
        
        double[][][] multiclassTripTable = new double[networkNumUserClasses][][];
        

        int startHour = 0;
        int endHour = 0;

        // get trip list filenames from property file
        String ptFileName = (String)tsPropertyMap.get("pt.fileName");

        

        startHour = Integer.parseInt( (String)globalPropertyMap.get("AM_PEAK_START") );
        endHour = Integer.parseInt( (String)globalPropertyMap.get("AM_PEAK_END") );


        // check that at least one valid user class has been defined
        if ( networkNumUserClasses == 0 ) {
            logger.error ( "No valid user classes defined in the application properties file.", new RuntimeException() );
        }
        
        HashMap assignmentGroupMap = nh.getAssignmentGroupMap();
        
        
        // read PT trip list into o/d trip matrix if auto user class was defined
        if ( assignmentGroupMap.containsKey( String.valueOf('a') ) ) {
            myDateString = DateFormat.getDateTimeInstance().format(new Date());
            logger.info ("reading peak PT trip list at: " + myDateString);
            multiclassTripTable[0] = getAutoTripTableFromPTList ( ptFileName, startHour, endHour );
        }
        else {
            logger.info ("no auto class defined, so peak PT trip list was not read." );
        }

        
        return multiclassTripTable;
        
    }
    
    

    
    private double[][] getAutoTripTableFromPTList ( String fileName, int startPeriod, int endPeriod ) {
        
        int orig;
        int dest;
        int startTime;
        int mode;
        int o;
        int d;
        int allAutoTripCount=0;
        int tripCount=0;
        
        int networkNumCentroids = nh.getNumCentroids();
        int[] networkNodeIndexArray = nh.getNodeIndex();
        
        
        double[][] tripTable = new double[networkNumCentroids+1][networkNumCentroids+1];
        

        
        // read the PT output person trip list file into a TableDataSet
        OLD_CSVFileReader reader = new OLD_CSVFileReader();

        String[] columnsToRead = { "origin", "destination", "tripStartTime", "tripMode" };
        TableDataSet table = null;
        try {
            if ( fileName != null) {

                table = reader.readFile(new File( fileName ), columnsToRead);

                // traverse the trip list in the TableDataSet and aggregate trips to an o/d trip table
                for (int i=0; i < table.getRowCount(); i++) {
                    
                    orig = (int)table.getValueAt( i+1, "origin" );
                    dest = (int)table.getValueAt( i+1, "destination" );
                    startTime = (int)table.getValueAt( i+1, "tripStartTime" );
                    mode = (int)table.getValueAt( i+1, "tripMode" );
                    
                    o = networkNodeIndexArray[orig];
                    d = networkNodeIndexArray[dest];
                    
                    // accumulate all peak period highway mode trips
                    if ( (mode == ModeType.AUTODRIVER || mode == ModeType.AUTOPASSENGER) ) {
                        
                        if ( (startTime >= startPeriod && startTime <= endPeriod) ) {
    
                            tripTable[o][d]++;
                            tripCount++;
                        
                        }

                        allAutoTripCount++;
                    }
                    
                }
                
                // done with trip list TabelDataSet
                table = null;

            }
            
        } catch (IOException e) {
            logger.error ( "", e );
        }


        logger.info (allAutoTripCount + " total auto network trips read from PT entire file");
        logger.info (tripCount + " total auto network trips read from PT file for period " + startPeriod +
                " to " + endPeriod);

        return tripTable;
            
    }
    

}

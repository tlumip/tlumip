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


import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import com.pb.common.rpc.DafNode;

import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

import org.apache.log4j.Logger;



public class DemandHandler implements DemandHandlerIF, Serializable {

	protected static transient Logger logger = Logger.getLogger(DemandHandler.class);

    public static final String[] modeLabels = { "", "DA", "SR2", "SR3+", "WK", "BK", "WT", "DT" }; 
    
    int networkNumCentroids;
    int networkNumUserClasses;
    int[] networkNodeIndexArray;
    boolean networkUserClassesIncludeTruck;
    char[] highwayModeCharacters;
    char[][] networkAssignmentGroupChars;
    
    String ptFileName;
    String ctFileName;
    String timePeriod;
    int startHour;
    int endHour;
    
	double[][][] multiclassTripTable = null;
    
    double[][] multiclassTripTableRowSums = null;

	


    public DemandHandler() {
    }

    
    // Factory Method to return either local or remote instance
    public static DemandHandlerIF getInstance( String rpcConfigFile ) {
    
        if ( rpcConfigFile == null ) {

            // if rpc config file is null, then all handlers are local, so return local instance
            return new DemandHandler();

        }
        else {
            
            // return either a local instance or an rpc instance depending on how the handler was defined.
            Boolean isLocal = DafNode.getInstance().isHandlerLocal( HANDLER_NAME );

            if ( isLocal == null )
                // handler name not found in config file, so create a local instance.
                return new DemandHandler();
            else 
                // handler name found in config file but is not local, so create an rpc instance.
                return new DemandHandlerRpc( rpcConfigFile );

        }
        
    }
    

    // Factory Method to return local instance only
    public static DemandHandlerIF getInstance() {
        return new DemandHandler();
    }
    

   
    public boolean setup( String ptFileName, String ctFileName, int startHour, int endHour, String timePeriod, int numCentroids, int numUserClasses, int[] nodeIndexArray, char[][] assignmentGroupChars, char[] highwayModeCharacters, boolean userClassesIncludeTruck ) {
        
        networkNumCentroids = numCentroids;
        networkNumUserClasses = numUserClasses;
        networkAssignmentGroupChars = assignmentGroupChars;
        networkNodeIndexArray = nodeIndexArray;
        networkUserClassesIncludeTruck = userClassesIncludeTruck;
        this.highwayModeCharacters = highwayModeCharacters;
        this.timePeriod = timePeriod;
        this.ptFileName = ptFileName;
        this.ctFileName = ctFileName;
        this.startHour = startHour;
        this.endHour = endHour;

        return true;
        
    }

    
    
    // this method called by methods running in a different VM and thus making a remote method call to setup this object
    public boolean setupRpc( String ptFileName, String ctFileName, int startHour, int endHour, String timePeriod, int numCentroids, int numUserClasses, int[] nodeIndexArray, char[][] assignmentGroupChars, char[] highwayModeCharacters, boolean userClassesIncludeTruck ) {
        
        return setup( ptFileName, ctFileName, startHour, endHour, timePeriod, numCentroids, numUserClasses, nodeIndexArray, assignmentGroupChars, highwayModeCharacters, userClassesIncludeTruck );
    
    }
    
    
    
    public boolean buildDemandObject() {
        
        int i=0;
        int j=0;
        int k=0;
        
        // load the trips from PT and CT trip lists into multiclass o/d demand matrices for assignment
        try {

            logger.info ( "creating demand trip tables from " + ptFileName + " and " + ctFileName + " for the " + timePeriod + " period." );
            
            // read in the trip lists
            multiclassTripTable = createMulticlassDemandMatrices ();
            
            
            multiclassTripTableRowSums = new double[multiclassTripTable.length][multiclassTripTable[0].length];
            
            // summarize the trip table rows for each user class
            for (i=0; i < multiclassTripTable.length; i++)
                for (j=0; j < multiclassTripTable[i].length; j++)
                    for (k=0; k < multiclassTripTable[i][j].length; k++)
                        multiclassTripTableRowSums[i][j] += multiclassTripTable[i][j][k];
            
            
            return true;
            
        }
        catch (Exception e) {
            
            logger.error ("error building multiclass od demand matrices for " + timePeriod + " period.");
            logger.error ("multiclassTripTable.length=" + multiclassTripTable.length);
            logger.error ("multiclassTripTable[0].length=" + multiclassTripTable[0].length);
            logger.error ("multiclassTripTable[0][0].length=" + multiclassTripTable[0][0].length);
            logger.error ("multiclassTripTableRowSums.length=" + multiclassTripTableRowSums.length);
            logger.error ("multiclassTripTableRowSums[0].length=" + multiclassTripTableRowSums[0].length);
            logger.error ("i=" + i + ", j=" + j + ", k=" + k, e);
            return false;
            
        }
        
    }
    
    
    public double[] getTripTableRow ( int userClass, int row ) {
        return multiclassTripTable[userClass][row];
    }
    
    
    public double[] getTripTableRowRpc ( int userClass, int row ) {
        double[] tripRow = getTripTableRow ( userClass, row );
        return tripRow;
    }
    
    
    public double[][][] getMulticlassTripTables () {
        return multiclassTripTable;
    }
    
    
    public double[][][] getMulticlassTripTablesRpc () {
        double[][][] tripTables = getMulticlassTripTables();
        return tripTables;
    }
    
    
    public double[][] getTripTableRowSums () {
        return multiclassTripTableRowSums;
    }
    
    
    public double[][] getTripTableRowSumsRpc () {
        double[][] rowSums = getTripTableRowSums();
        return rowSums;
    }
    
    
    public double[][] getWalkTransitTripTable () {
        
        return getWalkTransitTripTableFromPTList ();
        
    }

    
    
    public double[][] getDriveTransitTripTable () {
        
        return getDriveTransitTripTableFromPTList ();
        
    }

    
    
    
    private double[][][] createMulticlassDemandMatrices () {
        
		String myDateString;
		
    	double[][][] multiclassTripTable = new double[networkNumUserClasses][][];
		

		// check that at least one valid user class has been defined
		if ( networkNumUserClasses == 0 ) {
			logger.error ( "No valid user classes defined in the application properties file.", new RuntimeException() );
		}
		
		
        
		// read PT trip list into o/d trip matrix if auto user class was defined
        boolean assignmentGroupContainsAuto = false;
        for (int i=0; i < networkAssignmentGroupChars[0].length; i++) {
            if ( networkAssignmentGroupChars[0][i] == 'a' ) {
                assignmentGroupContainsAuto = true;
                break;
            }
        }
                
		if ( assignmentGroupContainsAuto ) {
			myDateString = DateFormat.getDateTimeInstance().format(new Date());
			logger.info ("reading " + timePeriod + " PT trip list at: " + myDateString);
			multiclassTripTable[0] = getAutoTripTableFromPTList ();
		}
		else {
			logger.info ("no auto class defined, so " + timePeriod + " PT trip list was not read." );
		}

        
		// read CT trip list into o/d trip matrix if at least one truck class was defined
		if ( networkUserClassesIncludeTruck ) {
			myDateString = DateFormat.getDateTimeInstance().format(new Date());
			logger.info ("reading " + timePeriod + " CT trip list at: " + myDateString);
			double[][][] truckTripTables = getTruckAssignmentGroupTripTableFromCTList ();

			for(int i=0; i < truckTripTables.length - 1; i++)
				multiclassTripTable[i+1] = truckTripTables[i];
		}


        return multiclassTripTable;
		
    }
	
	

    
    private double[][] getAutoTripTableFromPTList () {
        
        int orig;
        int dest;
        int startTime;
        int mode;
        int o;
        int d;
        int allAutoTripCount=0;
        int tripCount=0;
        int allTrips = 0;

        BufferedReader in = null;
        String fileHeader = null;
        String s;
        
        double[][] tripTable = new double[networkNumCentroids+1][networkNumCentroids+1];

        TreeMap freqMap = new TreeMap();

        

        // open the PT trip list file for reading
        try {
            if ( ptFileName != null && ! ptFileName.equals("") ) {
                in = new BufferedReader(new FileReader(ptFileName));
                fileHeader = in.readLine();
            }
            else {
                new IOException("null input file name.");
            }
        }
        catch (IOException e) {
            logger.error ( "error opening PT person trip list: " + ptFileName, e );
        }
        
        

        // get the field indices for the fields we want to read
        StringTokenizer st = new StringTokenizer(fileHeader, ",\n\r");
        int[] fieldFlags = new int[st.countTokens()];

        int i =0;
        while ( st.hasMoreTokens() ) {
            s = st.nextToken();
            if ( s.equals("origin") )
                fieldFlags[i] = 1;
            else if ( s.equals("destination") )
                fieldFlags[i] = 2;
            else if ( s.equals("tripStartTime") )
                fieldFlags[i] = 3;
            else if ( s.equals("tripMode") )
                fieldFlags[i] = 4;
            i++;
        }
        
        int[] values = new int[4+1];
                

        
        // parse fields for the values we were interested in.
        try {
            while ( (s = in.readLine()) != null) {
                    
                i = 0;
                st = new StringTokenizer(s, ",\n\r");
                while ( st.hasMoreTokens() ) {
                    String stringValue = st.nextToken();
                    if ( fieldFlags[i] > 0 )
                        values[fieldFlags[i]] = Integer.parseInt(stringValue);
                    i++;
                }
                
                orig = values[1];
                dest = values[2];
                startTime = values[3];
                mode = values[4];
                
                // accumulate a frequency table of all trips within period by mode
                if ( (startTime >= startHour && startTime <= endHour) ) {
                    allTrips++;
                        
                    int value = 0;
                    if ( freqMap.containsKey(mode) )
                        value = (Integer)freqMap.get(mode);
                    freqMap.put ( mode, (value+1) );

                }
                
                
                o = networkNodeIndexArray[orig];
                d = networkNodeIndexArray[dest];
                        
                // accumulate all peak period highway mode trips
                if ( (mode == ModeType.AUTODRIVER || mode == ModeType.AUTOPASSENGER) ) {
                    
                    if ( (startTime >= startHour && startTime <= endHour) ) {

                        tripTable[o][d]++;
                        tripCount++;
                    
                    }

                    allAutoTripCount++;
                }
                
            }
        }
        catch (NumberFormatException e) {
            logger.error ( "reading PT trip list file.", e);
        }
        catch (IOException e) {
            logger.error ( "reading PT trip list file.", e);
        }
            


        logger.info (allAutoTripCount + " total auto network trips read from PT entire file");
        logger.info (tripCount + " total auto network trips read from PT file for period " + startHour + " to " + endHour);
        
        Set keys = freqMap.keySet();
        Iterator it = keys.iterator();
        logger.info ( "");
        logger.info ( "Frequency Table of PT " + startHour + " to " + endHour + " Period Trips by Mode");
        logger.info ( String.format ( "%-6s %12s %16s %16s", "mode", "freq", "pct", "cumPct" ) );
        logger.info ( "-----------------------------------------------------" );
        double cumPct = 0.0;
        while ( it.hasNext() ) {
            mode = (Integer)it.next();
            int value = (Integer)freqMap.get(mode);
            double pct = value*100.0/allTrips;
            cumPct += pct;
            logger.info ( String.format ( "%-6s %12d %16.2f %16.2f", modeLabels[mode], value, pct, cumPct ) );
        }
        logger.info ( "-----------------------------------------------------" );
        logger.info ( String.format ( "%-6s %12d %16.2f %16.2f", "Total", allTrips, 100.0, cumPct ) );
        logger.info ( "");
        
        
        return tripTable;
            
    }
    

    private double[][] getWalkTransitTripTableFromPTList () {
        
        int orig;
        int dest;
        int startTime;
        int mode;
        int o;
        int d;
        int allTripsCount=0;
        int tripCount=0;
        
        
        double[][] tripTable = new double[networkNumCentroids+1][networkNumCentroids+1];
        

        
        // read the PT output person trip list file into a TableDataSet
        OLD_CSVFileReader reader = new OLD_CSVFileReader();

        String[] columnsToRead = { "origin", "destination", "tripStartTime", "tripMode" };
        TableDataSet table = null;
        try {
            if ( ptFileName != null && ! ptFileName.equals("") ) {

                table = reader.readFile(new File( ptFileName ), columnsToRead);

                // traverse the trip list in the TableDataSet and aggregate trips to an o/d trip table
                for (int i=0; i < table.getRowCount(); i++) {
                    
                    orig = (int)table.getValueAt( i+1, "origin" );
                    dest = (int)table.getValueAt( i+1, "destination" );
                    startTime = (int)table.getValueAt( i+1, "tripStartTime" );
                    mode = (int)table.getValueAt( i+1, "tripMode" );
                    
                    o = networkNodeIndexArray[orig];
                    d = networkNodeIndexArray[dest];
                    
                    // accumulate all peak period highway mode trips
                    if ( (mode == ModeType.WALKTRANSIT || mode == ModeType.TRANSITPASSENGER) ) {
                        
                        if ( (startTime >= startHour && startTime <= endHour) ) {
    
                            tripTable[o][d]++;
                            tripCount++;
                        
                        }

                        allTripsCount++;
                    }
                    
                }
                
                // done with trip list TableDataSet
                table = null;

            }
            
        } catch (IOException e) {
            logger.error ( "", e );
        }


        logger.info (allTripsCount + " total walk transit trips read from PT file");
        logger.info (tripCount + " total walk transit trips read from PT file for period " + startHour +
                " to " + endHour);

        return tripTable;
            
    }
    

    private double[][] getDriveTransitTripTableFromPTList () {
        
        int orig;
        int dest;
        int startTime;
        int mode;
        int o;
        int d;
        int allTripsCount=0;
        int tripCount=0;
        
        
        double[][] tripTable = new double[networkNumCentroids+1][networkNumCentroids+1];
        

        
        // read the PT output person trip list file into a TableDataSet
        OLD_CSVFileReader reader = new OLD_CSVFileReader();

        String[] columnsToRead = { "origin", "destination", "tripStartTime", "tripMode" };
        TableDataSet table = null;
        try {
            if ( ptFileName != null && ! ptFileName.equals("") ) {

                table = reader.readFile(new File( ptFileName ), columnsToRead);

                // traverse the trip list in the TableDataSet and aggregate trips to an o/d trip table
                for (int i=0; i < table.getRowCount(); i++) {
                    
                    orig = (int)table.getValueAt( i+1, "origin" );
                    dest = (int)table.getValueAt( i+1, "destination" );
                    startTime = (int)table.getValueAt( i+1, "tripStartTime" );
                    mode = (int)table.getValueAt( i+1, "tripMode" );
                    
                    o = networkNodeIndexArray[orig];
                    d = networkNodeIndexArray[dest];
                    
                    // accumulate all peak period highway mode trips
                    if ( (mode == ModeType.DRIVETRANSIT || mode == ModeType.PASSENGERTRANSIT) ) {
                        
                        if ( (startTime >= startHour && startTime <= endHour) ) {
    
                            tripTable[o][d]++;
                            tripCount++;
                        
                        }

                        allTripsCount++;
                    }
                    
                }
                
                // done with trip list TableDataSet
                table = null;

            }
            
        } catch (IOException e) {
            logger.error ( "", e );
        }


        logger.info (allTripsCount + " total drive transit trips read from PT file");
        logger.info (tripCount + " total drive transit trips read from PT file for period " + startHour +
                " to " + endHour);

        return tripTable;
            
    }
    

    private double[][][] getTruckAssignmentGroupTripTableFromCTList () {

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
        int allTruckTripCount=0;
        

        double[] tripsByUserClass = new double[highwayModeCharacters.length];
        double[] tripsByAssignmentGroup = new double[networkNumUserClasses];
        double[][][] tripTable = new double[networkNumUserClasses][networkNumCentroids+1][networkNumCentroids+1];



		// read the CT output file into a TableDataSet
		OLD_CSVFileReader reader = new OLD_CSVFileReader();

		TableDataSet table = null;
		int tripRecord = 0;
		try {
			if ( ctFileName != null && ! ctFileName.equals("") ) {

				table = reader.readFile(new File( ctFileName ));

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
                    group = -1;
                    for (int j=1; j < networkAssignmentGroupChars.length; j++) {
                        for (int k=0; k < networkAssignmentGroupChars[j].length; k++) {
                            if ( networkAssignmentGroupChars[j][k] == modeChar ) {
                                group = j;
                                break;
                            }
                        }
                    }
                    if ( group < 0 ) {
                        logger.error ( "modeChar = " + modeChar + " associated with CT integer mode = " + mode + " not found in any asignment group." );
                        System.exit(-1);
                    }
					
					o = networkNodeIndexArray[orig];
					d = networkNodeIndexArray[dest];
	
					// accumulate all peak period highway mode trips
					if ( startTime >= startHour && startTime <= endHour ) {
	
					    tripTable[group-1][o][d] += tripFactor;
					    tripsByUserClass[mode-1] += tripFactor;
					    tripsByAssignmentGroup[group-1] += tripFactor;
	
					}
                    
                    allTruckTripCount += tripFactor;
	
				}
	
				// done with trip list TabelDataSet
				table = null;

			}
			
		} catch (Exception e) {
			logger.error ("exception caught reading CT truck trip record " + tripRecord, e);
			System.exit(-1);
		}

		
        logger.info (allTruckTripCount + " total trips by all truck user classes read from CT file.");
        logger.info ("trips by truck user class read from CT file from " + startHour + " to " + endHour + ":");
		for (int i=0; i < tripsByUserClass.length; i++)
			if (tripsByUserClass[i] > 0)
				logger.info ( tripsByUserClass[i] + " truck trips with user class " + highwayModeCharacters[i+1] );

		logger.info ("trips by truck assignment groups read from CT file from " + startHour + " to " + endHour + ":");
		for (int i=0; i < tripsByAssignmentGroup.length; i++)
			if (tripsByAssignmentGroup[i] > 0)
				logger.info ( tripsByAssignmentGroup[i] + " truck trips in assignment group " + (i+1) );

		return tripTable;

    }
    
}

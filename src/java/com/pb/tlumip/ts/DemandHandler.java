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

import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcHandler;
import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Logger;



public class DemandHandler implements RpcHandler {

    public static String remoteHandlerName = "demandHandler";
    
	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.DemandHandler");

    
    RpcClient networkHandlerClient;    

    final char[] highwayModeCharacters = { 'a', 'd', 'e', 'f', 'g', 'h' };

    String componentPropertyName;
    String globalPropertyName;
    
	HashMap componentPropertyMap;
    HashMap globalPropertyMap;

    ResourceBundle appRb;
    ResourceBundle globalRb;
    
    int networkNumCentroids;
    int networkNumUserClasses;
    int[] networkNodeIndexArray;
    boolean networkUserClassesIncludeTruck;
    HashMap networkAssignmentGroupMap;

    
	double[][][] multiclassTripTable = null;
    
    double[][] multiclassTripTableRowSums = null;

	


	public DemandHandler() {

        String handlerName = null;
        
        try {
            
            //Create RpcClients this class connects to
            try {

                handlerName = NetworkHandler.remoteHandlerName;
                networkHandlerClient = new RpcClient( handlerName );
                
            }
            catch (MalformedURLException e) {
                logger.error ( "MalformedURLException caught in DemandHandler() while defining RpcClients.", e );
            }

        }
        catch ( Exception e ) {
            logger.error ( "Exception caught in DemandHandler().", e );
            System.exit(1);
        }

	}


    
    public Object execute (String methodName, Vector params) throws Exception {
                  
        if ( methodName.equalsIgnoreCase( "setup" ) ) {
            
            // get the network attributes needed by demandHandler from the networkHandler
            int numCentroids = networkHandlerGetNumCentroidsRpcCall();
            int numUserClasses = networkHandlerGetNumUserClassesRpcCall();
            int[] nodeIndex = networkHandlerGetNodeIndexRpcCall();
            HashMap assignmentGroupMap = networkHandlerGetAssignmentGroupMapRpcCall();
            boolean userClassesIncludeTruck = networkHandlerUserClassesIncludeTruckRpcCall();
            setNetworkAttributes( numCentroids, numUserClasses, nodeIndex, assignmentGroupMap, userClassesIncludeTruck );
            
            // set the property objects from client that initialized this object and build the demand matrices
            HashMap componentPropertyMap = (HashMap)params.get(0);
            HashMap globalPropertyMap = (HashMap)params.get(1);
            String timePeriod = (String)params.get(2);
            return setup( componentPropertyMap, globalPropertyMap, timePeriod );

        }
        else if ( methodName.equalsIgnoreCase( "getTripTableRowSums" ) ) {
            return multiclassTripTableRowSums;
        }
        else if ( methodName.equalsIgnoreCase( "getTripTableRow" ) ) {
            int userClass = (Integer)params.get(0);
            int row = (Integer)params.get(1);
            double[] tripTableRow = getTripTableRow( userClass, row );
            return tripTableRow;
        }
        else {
            logger.error ( "method name " + methodName + " called from remote client is not registered for remote method calls.", new Exception() );
            return null;
        }
        
    }
    

    
    
    public boolean setup( HashMap componentPropertyMap, HashMap globalPropertyMap, String timePeriod ) {
        
        this.componentPropertyMap = componentPropertyMap;
        this.globalPropertyMap = globalPropertyMap; 
        
        return buildDemandObject( timePeriod );
        
    }
    
    
    public boolean setup( ResourceBundle componentRb, ResourceBundle globalRb, String timePeriod ) {
        

        this.appRb = componentRb;
        this.globalRb = globalRb;
        
        this.componentPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( componentRb );
        this.globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( globalRb );

        return buildDemandObject( timePeriod );
        
    }
    
    
    public void setNetworkAttributes( int numCentroids, int numUserClasses, int[] nodeIndexArray, HashMap assignmentGroupMap, boolean userClassesIncludeTruck ) {
        
        networkNumCentroids = numCentroids;
        networkNumUserClasses = numUserClasses;
        networkAssignmentGroupMap = assignmentGroupMap;
        networkNodeIndexArray = nodeIndexArray;
        networkUserClassesIncludeTruck = userClassesIncludeTruck;
        networkAssignmentGroupMap = assignmentGroupMap;
        
    }
    
    
    public double[] getTripTableRow ( int userClass, int row ) {
        
        return multiclassTripTable[userClass][row];
        
    }
    
    
    public double[][][] getMulticlassTripTables () {
        
        return multiclassTripTable;
        
    }
    
    
    
    private boolean buildDemandObject( String timePeriod ) {
    	
        int i=0;
        int j=0;
        int k=0;
        
    	// load the trips from PT and CT trip lists into multiclass o/d demand matrices for assignment
        try {

            // read in the trip lists
            multiclassTripTable = createMulticlassDemandMatrices ( timePeriod );
            
            
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
    
    
    
    private double[][][] createMulticlassDemandMatrices ( String timePeriod ) {
        
		String myDateString;
		
    	double[][][] multiclassTripTable = new double[networkNumUserClasses][][];
		

    	int startHour = 0;
    	int endHour = 0;

        // get trip list filenames from property file
        String ptFileName = (String)componentPropertyMap.get("pt.fileName");
        String ctFileName = (String)componentPropertyMap.get("ct.fileName");

        

        if ( timePeriod.equalsIgnoreCase( "peak" ) ) {
            // get peak period definitions from property files
            startHour = Integer.parseInt( (String)globalPropertyMap.get("AM_PEAK_START") );
            endHour = Integer.parseInt( (String)globalPropertyMap.get("AM_PEAK_END") );
        }
        else if ( timePeriod.equalsIgnoreCase( "offpeak" ) ) {
            // get off-peak period definitions from property files
            startHour = Integer.parseInt( (String)globalPropertyMap.get("OFF_PEAK_START") );
            endHour = Integer.parseInt( (String)globalPropertyMap.get("OFF_PEAK_END") );
        }


		// check that at least one valid user class has been defined
		if ( networkNumUserClasses == 0 ) {
			logger.error ( "No valid user classes defined in " + componentPropertyName + " file.", new RuntimeException() );
		}
		
		HashMap assignmentGroupMap = networkAssignmentGroupMap;
		
        
		// read PT trip list into o/d trip matrix if auto user class was defined
		if ( assignmentGroupMap.containsKey( String.valueOf('a') ) ) {
			myDateString = DateFormat.getDateTimeInstance().format(new Date());
			logger.info ("reading " + timePeriod + " PT trip list at: " + myDateString);
			multiclassTripTable[0] = getAutoTripTableFromPTList ( networkNodeIndexArray, ptFileName, startHour, endHour );
		}
		else {
			logger.info ("no auto class defined, so " + timePeriod + " PT trip list was not read." );
		}

        
		// read CT trip list into o/d trip matrix if at least one truck class was defined
		if ( networkUserClassesIncludeTruck ) {
			myDateString = DateFormat.getDateTimeInstance().format(new Date());
			logger.info ("reading " + timePeriod + " CT trip list at: " + myDateString);
			double[][][] truckTripTables = getTruckAssignmentGroupTripTableFromCTList ( assignmentGroupMap, networkNodeIndexArray, ctFileName, startHour, endHour );

			for(int i=0; i < truckTripTables.length - 1; i++)
				multiclassTripTable[i+1] = truckTripTables[i];
		}


        return multiclassTripTable;
		
    }
	
	

    
    private double[][] getAutoTripTableFromPTList ( int[] nodeIndex, String fileName, int startPeriod, int endPeriod ) {
        
        int orig;
        int dest;
        int startTime;
        int mode;
        int o;
        int d;
        int allAutoTripCount=0;
        int tripCount=0;
        
        
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
					
					o = nodeIndex[orig];
					d = nodeIndex[dest];
					
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
    

    private double[][][] getTruckAssignmentGroupTripTableFromCTList ( HashMap assignmentGroupMap, int[] nodeIndex, String fileName, int startPeriod, int endPeriod ) {

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
			if ( fileName != null ) {

				table = reader.readFile(new File( fileName ));

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
   


    private int networkHandlerGetNumUserClassesRpcCall() throws Exception {
        // g.getNumUserClasses()
        return (Integer)networkHandlerClient.execute("networkHandler.getNumUserClasses", new Vector() );
    }

    private int networkHandlerGetNumCentroidsRpcCall() throws Exception {
        // g.getNumCentroids()
        return (Integer)networkHandlerClient.execute("networkHandler.getNumCentroids", new Vector());
    }

    private int[] networkHandlerGetNodeIndexRpcCall() throws Exception {
        // g.getNodeIndex()
        return (int[])networkHandlerClient.execute("networkHandler.getNodeIndex", new Vector() );
    }

    private HashMap networkHandlerGetAssignmentGroupMapRpcCall() throws Exception {
        // g.getAssignmentGroupMap()
        return (HashMap)networkHandlerClient.execute("networkHandler.getAssignmentGroupMap", new Vector() );
    }

    private boolean networkHandlerUserClassesIncludeTruckRpcCall() throws Exception {
        // g.userClassesIncludeTruck()
        return (Boolean)networkHandlerClient.execute("networkHandler.userClassesIncludeTruck", new Vector() );
    }
    
}

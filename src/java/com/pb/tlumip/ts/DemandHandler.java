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

import com.pb.common.rpc.NodeConfig;
import com.pb.common.rpc.RPC;
import com.pb.common.util.Convert;
import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.Echo;
import org.apache.xmlrpc.SystemHandler;
import org.apache.xmlrpc.WebServer;



public class DemandHandler {

    public static String remoteHandlerAddress = "http://localhost:7001";
    
	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.DemandHandler");

    public static String nodeName;
    public static int webPort = 7001;
    public static int tcpPort = 7002;

    
    
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

	


	public DemandHandler() {
	}


    
    public Object execute (String methodName, Vector params) throws Exception {
                  
        if ( methodName.equalsIgnoreCase( "setup" ) ) {
            HashMap componentPropertyMap = (HashMap)Convert.toObject((byte[])params.get(0));
            HashMap globalPropertyMap = (HashMap)Convert.toObject((byte[])params.get(1));
            String timePeriod = (String)params.get(2);
            return setup( componentPropertyMap, globalPropertyMap, timePeriod );
        }
        else if ( methodName.equalsIgnoreCase( "setNetworkAttributes" ) ) {
            int numCentroids = (Integer)params.get(0);
            int numUserClasses = (Integer)params.get(1);
            int[] nodeIndexArray = (int[])Convert.toObject((byte[])params.get(2));
            HashMap assignmentGroupMap = (HashMap)Convert.toObject((byte[])params.get(3));
            boolean userClassesIncludeTruck = (Boolean)params.get(4);
            setNetworkAttributes( numCentroids, numUserClasses, nodeIndexArray, assignmentGroupMap, userClassesIncludeTruck );
            return null;
        }
        else if ( methodName.equalsIgnoreCase( "getMulticlassTripTables" ) ) {
            return Convert.toBytes( getMulticlassTripTables() );
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
    
    
    
    public double[][][] getMulticlassTripTables () {
        
        return multiclassTripTable;
        
    }
    
    
    
    private boolean buildDemandObject( String timePeriod ) {
    	
    	// load the trips from PT and CT trip lists into multiclass o/d demand matrices for assignment
        try {

            multiclassTripTable = createMulticlassDemandMatrices ( timePeriod );
            return true;
        }
        catch (Exception e) {
            
            logger.error ("error building multiclass od demand matrices for " + timePeriod + " period.", e);
            return false;
            
        }
		
    }
    
    
    
    private double[][][] createMulticlassDemandMatrices ( String timePeriod ) {
        
		String myDateString;
		
    	double[][][] multiclassTripTable = new double[highwayModeCharacters.length][][];
		

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
			multiclassTripTable[0] = getAutoTripTableFromPTList ( networkNumCentroids, networkNodeIndexArray, ptFileName, startHour, endHour );
		}
		else {
			logger.info ("no auto class defined, so " + timePeriod + " PT trip list was not read." );
		}

        
		// read CT trip list into o/d trip matrix if at least one truck class was defined
		if ( networkUserClassesIncludeTruck ) {
			myDateString = DateFormat.getDateTimeInstance().format(new Date());
			logger.info ("reading " + timePeriod + " CT trip list at: " + myDateString);
			double[][][] truckTripTables = getTruckAssignmentGroupTripTableFromCTList ( assignmentGroupMap, networkNumCentroids, networkNodeIndexArray, ctFileName, startHour, endHour );

			for(int i=0; i < truckTripTables.length - 1; i++)
				multiclassTripTable[i+1] = truckTripTables[i];
		}


        return multiclassTripTable;
		
    }
	
	

    
    private double[][] getAutoTripTableFromPTList ( int numCentroids, int[] nodeIndex, String fileName, int startPeriod, int endPeriod ) {
        
        int orig;
        int dest;
        int startTime;
        int mode;
        int o;
        int d;
        int allAutoTripCount=0;
        int tripCount=0;
        
        
        double[][] tripTable = new double[numCentroids+1][numCentroids+1];
        

        
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
    

    private double[][][] getTruckAssignmentGroupTripTableFromCTList ( HashMap assignmentGroupMap, int numCentroids, int[] nodeIndex, String fileName, int startPeriod, int endPeriod ) {

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
        

        int numAssignmentGroups = assignmentGroupMap.keySet().size();
		if ( assignmentGroupMap.containsKey( String.valueOf('a') ) )
			numAssignmentGroups--;
        
        
        double[] tripsByUserClass = new double[highwayModeCharacters.length];
        double[] tripsByAssignmentGroup = new double[numAssignmentGroups];
        double[][][] tripTable = new double[numAssignmentGroups][numCentroids+1][numCentroids+1];



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


    public static void main(String[] args) {

        if (args.length < 2) {
            logger.error ("usage: java " + DemandHandler.class.getName() + " <node-name> <config-file>");
            return;
        }

        nodeName = args[0];

        RPC.init();
        //RPC.setDebug(true);

        try {
            
            //Read config file
            logger.info ("reading config file: " + args[1]);
            NodeConfig nodeConfig = new NodeConfig();
            nodeConfig.readConfig(new File(args[1]));

            //Create webserver - register default handlers
            WebServer webserver = new WebServer(webPort);
            webserver.addHandler("math", Math.class);
            webserver.addHandler("$default", new Echo());

            //Add SystemHandler, for multicall
            SystemHandler system = new SystemHandler();
            system.addDefaultSystemHandlers();
            webserver.addHandler("system", system);

            //Register handlers only for this node
            for (int i=0; i < nodeConfig.nHandlers; i++) {
                String name = nodeConfig._handlers[i].name;
                String node = nodeConfig._handlers[i].node;

                if (nodeName.equalsIgnoreCase(node)) {
                    Class clazz = Class.forName(nodeConfig._handlers[i].className);

                    logger.info ( "handler["+i+"]: " + name + "::" + clazz.getName() );
                    webserver.addHandler(name, clazz.newInstance());
                }
            }

            //Create webserver
            webserver.start();
            logger.info ( "Web server listening on " + webPort + "..." );
            
        }
        catch (Exception e) {
            logger.error ( "Exception caught in DemandHandler.main().", e );
        }
    }
    
}

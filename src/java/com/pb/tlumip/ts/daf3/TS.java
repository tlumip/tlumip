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
package com.pb.tlumip.ts.daf3;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 6/30/2004
 */


import com.pb.tlumip.ts.DemandHandler;
import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.daf3.Skims;


import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcException;
import com.pb.common.util.Convert;
import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.util.Vector;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;



public class TS {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.daf3");


	final char[] highwayModeCharacters = { 'a', 'd', 'e', 'f', 'g', 'h' };

	HashMap tsPropertyMap;
    HashMap globalPropertyMap;

    ResourceBundle appRb;
    ResourceBundle globalRb;

    String tsPropertyName;
    String globalPropertyName;
    
    String assignmentPeriod;
    
    RpcClient networkHandlerClient;    
    RpcClient demandHandlerClient;    
    
    int numCentroids = 0;
    int numUserClasses = 0;
    int[] nodeIndexArray = null;
    HashMap assignmentGroupMap = null;
    boolean userClassesIncludeTruck = false;

    double[][][] multiclassTripTable = new double[highwayModeCharacters.length][][];


	
	
	
	public TS( String appPropertyName, String globalPropertyName ) {

        tsPropertyName = appPropertyName;
        this.globalPropertyName = globalPropertyName;
        
        tsPropertyMap = ResourceUtil.getResourceBundleAsHashMap( appPropertyName );
        globalPropertyMap = ResourceUtil.getResourceBundleAsHashMap( globalPropertyName );
		
        try {
            setupRpcClients();
        }
        catch ( RpcException e ) {
            logger.error ( "RpcException caught in TS() establishing " + NetworkHandler.remoteHandlerAddress + " as the remote machine for running the NetworkHandler object.", e );
            System.exit(1);
        }
        catch ( IOException e ) {
            logger.error ( "IOException caught in TS() establishing " + NetworkHandler.remoteHandlerAddress + " as the remote machine for running the NetworkHandler object.", e );
            System.exit(1);
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught in TS().", e );
            System.exit(1);
        }
       
	}

    public TS(ResourceBundle appRb, ResourceBundle globalRb) {

        this.appRb = appRb;
        this.globalRb = globalRb;
        
        tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

        try {
            setupRpcClients();
        }
        catch ( RpcException e ) {
            logger.error ( "RpcException caught in TS() establishing " + NetworkHandler.remoteHandlerAddress + " as the remote machine for running the NetworkHandler object.", e );
            System.exit(1);
        }
        catch ( IOException e ) {
            logger.error ( "IOException caught in TS() establishing " + NetworkHandler.remoteHandlerAddress + " as the remote machine for running the NetworkHandler object.", e );
            System.exit(1);
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught in TS().", e );
            System.exit(1);
        }
       
    }


    public void runHighwayAssignment( String assignmentPeriod ) {
        
        this.assignmentPeriod = assignmentPeriod;
    	
    	// define assignment related variables dependent on the assignment period
    	initializeHighwayAssignment ( assignmentPeriod );

    	// load the trips from PT and CT trip lists into multiclass o/d demand matrices for assignment
		createMulticlassDemandMatrices ( assignmentPeriod );
		
		// run the multiclass assignment for the time period
    	multiclassEquilibriumHighwayAssignment ( assignmentPeriod );
		
    	// write the auto time and distance highway skim matrices to disk
    	writeHighwaySkimMatrices ( assignmentPeriod, 'a' );
		
    	// if at some point in time we want to have truck specific highway skims,
    	// we'd create them here and would modify the the properties file to include
    	// class specific naming in skims file properties file keynames.  We'd also
    	// modify the method above to distinguish the class id in addition to period
    	// and skim types.
    	
		
    }
    
    
    
    private void initializeHighwayAssignment ( String assignmentPeriod ) {
        
        logger.info ( "creating " + assignmentPeriod + " period NetworkHandler object for highway assignment at: " + DateFormat.getDateTimeInstance().format(new Date()) );

        try {
            networkHandlerSetupRpcCall();
        }
        catch ( RpcException e ) {
            logger.error ( "RpcException caught.", e );
            System.exit(1);
        }
        catch ( IOException e ) {
            logger.error ( "IOException caught.", e );
            System.exit(1);
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }

        logger.info ( "NetworkHandler object successfully created at: " + DateFormat.getDateTimeInstance().format(new Date()) );

    }
	
	
    private void createMulticlassDemandMatrices ( String assignmentPeriod ) {
        
        try {
            
            numCentroids = networkHandlerGetNumCentroidsRpcCall();
            numUserClasses = networkHandlerGetNumUserClassesRpcCall();
            nodeIndexArray = networkHandlerGetNodeIndexRpcCall();
            assignmentGroupMap = networkHandlerGetAssignmentGroupMapRpcCall();
            userClassesIncludeTruck = networkHandlerUserClassesIncludeTruckRpcCall();
            
            demandHandlerSetNetworkAttributesRpcCall();
            demandHandlerSetupRpcCall();

            
            multiclassTripTable = demandHandlerGetMulticlassTripTablesRpcCall();
            
        }
        catch ( RpcException e ) {
            logger.error ( "RpcException caught.", e );
            System.exit(1);
        }
        catch ( IOException e ) {
            logger.error ( "IOException caught.", e );
            System.exit(1);
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }
        
    }

    
	
    private void multiclassEquilibriumHighwayAssignment ( String assignmentPeriod ) {
        
		long startTime = System.currentTimeMillis();
		
		String myDateString;

		
		
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating + " + assignmentPeriod + " FW object at: " + myDateString);
		FW fw = new FW();
		fw.initialize( tsPropertyMap, networkHandlerClient );


		// Compute Frank-Wolfe solution
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("starting + " + assignmentPeriod + " fw at: " + myDateString);
		fw.iterate ( multiclassTripTable );
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done with + " + assignmentPeriod + " fw at: " + myDateString);

        logger.info( assignmentPeriod + " highway assignment finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes.");

        
        
        
        String assignmentResultsFileName = null;

        if ( assignmentPeriod.equalsIgnoreCase( "peak" ) ) {
            // get peak period definitions from property files
            assignmentResultsFileName = (String)tsPropertyMap.get("peakOutput.fileName");
        }
        else if ( assignmentPeriod.equalsIgnoreCase( "offpeak" ) ) {
            // get off-peak period definitions from property files
            assignmentResultsFileName = (String)tsPropertyMap.get("offpeakOutput.fileName");
        }
        

        
        try {
    		logger.info("Writing results file with " + assignmentPeriod + " assignment results.");
            networkHandlerWriteNetworkAttributesRpcCall( assignmentResultsFileName );
        }
        catch ( RpcException e ) {
            logger.error ( "RpcException caught.", e );
            System.exit(1);
        }
        catch ( IOException e ) {
            logger.error ( "IOException caught.", e );
            System.exit(1);
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }

		
        
        logger.info( "\ndone with " + assignmentPeriod + " period assignment."); 
        
    }
	

    
    public void writeHighwaySkimMatrix ( String assignmentPeriod, String skimType, char modeChar ) {

		logger.info("Writing " + assignmentPeriod + " time skim matrix for highway mode " + modeChar + " to disk...");
        long startTime = System.currentTimeMillis();
        
    	Skims skims = new Skims( tsPropertyMap, globalPropertyMap, networkHandlerClient );
    	
        skims.writeHwySkimMatrix ( assignmentPeriod, skimType, modeChar);

        logger.info("wrote the " + assignmentPeriod + " " + skimType + " skims for mode " + modeChar + " in " +
    			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

    }


    
    public void writeHighwaySkimMatrices ( String assignmentPeriod, char modeChar ) {

    	String[] skimTypeArray = { "time", "dist" };
    	
    	
		logger.info("Writing " + assignmentPeriod + " time and dist skim matrices for highway mode " + modeChar + " to disk...");
        long startTime = System.currentTimeMillis();
        
    	Skims skims = new Skims( tsPropertyMap, globalPropertyMap, networkHandlerClient );
    	
        skims.writeHwySkimMatrices ( assignmentPeriod, skimTypeArray, modeChar);

        logger.info("wrote the " + assignmentPeriod + " time and dist skims for mode " + modeChar + " in " +
    			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

    }


    

    
    private void setupRpcClients () throws RpcException, IOException {

        try {
            
            networkHandlerClient = new RpcClient( NetworkHandler.remoteHandlerAddress );
            demandHandlerClient = new RpcClient( DemandHandler.remoteHandlerAddress );
            
        } catch (MalformedURLException e) {
            
            logger.error ( "MalformedURLException caught in TS.setupRpcClients().", e );
            
        }
        
    }

    
    private void networkHandlerSetupRpcCall () throws Exception {
        // g.buildNetworkObject();
        
        Vector params = new Vector();
        params.addElement( Convert.toBytes(tsPropertyMap) );
        params.addElement( Convert.toBytes(globalPropertyMap) );
        params.addElement( assignmentPeriod );

        networkHandlerClient.execute("networkHandler.buildNetworkObject", params);
    }


    private int networkHandlerGetNumCentroidsRpcCall() throws Exception {
        // g.getNumCentroids()
        return (Integer)networkHandlerClient.execute("networkHandler.getNumCentroids", new Vector());
    }


    private int networkHandlerGetNumUserClassesRpcCall() throws Exception {
        // g.getNumUserClasses()
        return (Integer)networkHandlerClient.execute("networkHandler.getNumUserClasses", new Vector() );
    }


    private boolean networkHandlerUserClassesIncludeTruckRpcCall() throws Exception {
        // g.userClassesIncludeTruck()
        return (Boolean)networkHandlerClient.execute("networkHandler.userClassesIncludeTruck", new Vector() );
    }


    private HashMap networkHandlerGetAssignmentGroupMapRpcCall() throws Exception {
        // g.getAssignmentGroupMap()
        return (HashMap)Convert.toObject( (byte[])networkHandlerClient.execute("networkHandler.getAssignmentGroupMap", new Vector() ) );
    }


    private int[] networkHandlerGetNodeIndexRpcCall() throws Exception {
        // g.getNodeIndex()
        
        return (int[])Convert.toObject( (byte[])networkHandlerClient.execute("networkHandler.getNodeIndex", new Vector() ) );
    }

    
    private void networkHandlerWriteNetworkAttributesRpcCall( String assignmentResultsFileName ) throws Exception {
        // g.writeNetworkAttributes(String assignmentResultsFileName)
        Vector params = new Vector();
        params.addElement( assignmentResultsFileName );
        networkHandlerClient.execute("networkHandler.writeNetworkAttributes", params );
    }


    
    
    
    
    private void demandHandlerSetupRpcCall() throws Exception {
        // d.setup();
        
        Vector params = new Vector();
        params.addElement( Convert.toBytes(tsPropertyMap) );
        params.addElement( Convert.toBytes(globalPropertyMap) );
        params.addElement( assignmentPeriod );

        demandHandlerClient.execute("demandHandler.setup", params);
    }


    private void demandHandlerSetNetworkAttributesRpcCall() throws Exception {
        // d.setup();
        
        Vector params = new Vector();
        params.addElement( numCentroids );
        params.addElement( numUserClasses );
        params.addElement( Convert.toBytes(nodeIndexArray) );
        params.addElement( Convert.toBytes(assignmentGroupMap) );
        params.addElement( userClassesIncludeTruck );

        demandHandlerClient.execute("demandHandler.setNetworkAttributes", params);
    }


    private double[][][] demandHandlerGetMulticlassTripTablesRpcCall() throws Exception {
        // d.getMulticlassTripTables();
        
//        int i=0;
//        int j=0;
//        int k=0;
//        double[] tripsByClass = new double[numUserClasses];
//        
//        double[] tripVector = (double[])Convert.toObject( (byte[])demandHandlerClient.execute("demandHandler.getMulticlassTripTables", new Vector() ) );
//        double[][][] tripArray = new double[numUserClasses][numCentroids][numCentroids];
//        
//        for (int n=0; n < tripVector.length; n++) {
//
//            i = n / (numCentroids*numCentroids);
//            j = (n - i*numCentroids*numCentroids) / numCentroids;
//            k = (n - i*numCentroids*numCentroids - j*numCentroids);
//            
//            tripArray[i][j][k] = tripVector[n];
//            
//            tripsByClass[i] += tripArray[i][j][k];
//
//        }
//        
//        for (i=0; i < numUserClasses; i++)
//            logger.info( "class index " + i + " has " + tripsByClass[i] + " trips in the trip table returned by DemandHandler." );
//        
//        
//        return tripArray;
        

      double[][][] tripArray = (double[][][])Convert.toObject( (byte[])demandHandlerClient.execute("demandHandler.getMulticlassTripTables", new Vector() ) );
      return tripArray;
    
    }


    
    
    

    public static void main (String[] args) {
        
        TS tsTest = new TS( "ts", "global" );

        // run peak highway assignment
		tsTest.runHighwayAssignment( "peak" );
//		tsTest.runHighwayAssignment( "offpeak" );
		
		logger.info ("\ndone with TS run.");
		
    }

}

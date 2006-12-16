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



import com.pb.tlumip.ts.FW;
import com.pb.tlumip.ts.assign.Skims;
import com.pb.tlumip.ts.assign.TransitSkimManager;
import com.pb.tlumip.ts.transit.AuxTrNet;
import com.pb.tlumip.ts.transit.OptimalStrategy;
import com.pb.tlumip.ts.transit.TrRoute;


import com.pb.common.datafile.DataReader;
import com.pb.common.datafile.DataWriter;
import com.pb.common.matrix.Matrix;
import com.pb.common.rpc.DafNode;
import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.util.Arrays;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;



public class TS {

	protected static Logger logger = Logger.getLogger(TS.class);


    static final boolean CREATE_NEW_NETWORK = true;

    final char[] highwayModeCharacters = { 'a', 'd', 'e', 'f', 'g', 'h' };

    ResourceBundle appRb;
    ResourceBundle globalRb;
    

    
    public TS(ResourceBundle appRb, ResourceBundle globalRb) {

        this.appRb = appRb;
        this.globalRb = globalRb;
        
	}


    public void runHighwayAssignment( NetworkHandlerIF nh, String assignmentPeriod ) {
    	
    	// define assignment related variables dependent on the assignment period
    	initializeHighwayAssignment ( nh, assignmentPeriod );
        logger.info("TS main - highway network initialized\n\n");

    	// load the trips from PT and CT trip lists into multiclass o/d demand matrices for assignment
		//createMulticlassDemandMatrices ( nh, assignmentPeriod );
        //logger.info("TS main - demand matrices created\n\n");
		
		// run the multiclass assignment for the time period
    	multiclassEquilibriumHighwayAssignment ( nh, assignmentPeriod );
        logger.info("TS main - equilibrium assignment done\n\n");
		
    	// if at some point in time we want to have truck specific highway skims,
    	// we'd create them here and would modify the the properties file to include
    	// class specific naming in skims file properties file keynames.  We'd also
    	// modify the method above to distinguish the class id in addition to period
    	// and skim types.
    	
    }
    
    
    
    private void initializeHighwayAssignment ( NetworkHandlerIF nh, String assignmentPeriod ) {
        
        String myDateString = DateFormat.getDateTimeInstance().format(new Date());

        int returnValue = nh.setup( appRb, globalRb, assignmentPeriod );

        logger.info ("set up " + assignmentPeriod + " highway network object with " + returnValue + " links for highway assignment at: " + myDateString);
    }
	
	
    private void multiclassEquilibriumHighwayAssignment ( NetworkHandlerIF nh, String assignmentPeriod ) {
        
		long startTime = System.currentTimeMillis();
		
        HashMap tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        HashMap globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

		String myDateString;

		
		
		// create Frank-Wolfe Algortihm Object
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating + " + assignmentPeriod + " FW object at: " + myDateString);
		FW fw = new FW();
		fw.initialize( appRb, globalRb, nh, highwayModeCharacters );


		// Compute Frank-Wolfe solution
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("starting + " + assignmentPeriod + " fw at: " + myDateString);
		fw.iterate ();
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

        
		logger.info("Writing results file with " + assignmentPeriod + " assignment results.");
        nh.writeNetworkAttributes( assignmentResultsFileName );

		
        
        logger.info( "\ndone with " + assignmentPeriod + " period assignment."); 
        
    }
	

    
    public void checkNetworkForIsolatedLinks (NetworkHandlerIF nh) {
		nh.checkForIsolatedLinks ();
    }
    
    
    
    public void checkAllODPairsForNetworkConnectivity (NetworkHandlerIF nh) {
    	
        double[][][] dummyTripTable = new double[nh.getUserClasses().length][nh.getNumCentroids()+1][nh.getNumCentroids()+1];
		for(int i=0; i < nh.getUserClasses().length - 1; i++) {
			for(int j=0; j < nh.getNumCentroids() + 1; j++) {
				Arrays.fill(dummyTripTable[i][j], 1.0);
			}
		}
		checkODConnectivity(nh, dummyTripTable);

    }
    
    
    
    public void checkODPairsWithTripsForNetworkConnectivity (NetworkHandlerIF nh) {
        DemandHandler d = new DemandHandler();
        d.setup( appRb, globalRb, "peak", nh.getNumCentroids(), nh.getNumUserClasses(), nh.getNodeIndex(), nh.getAssignmentGroupMap(), highwayModeCharacters, nh.userClassesIncludeTruck() );
        d.buildDemandObject();

        double[][][] multiclassTripTable = d.getMulticlassTripTables();
		checkODConnectivity(nh, multiclassTripTable);
    }

    
    
    
    public void checkODConnectivity ( NetworkHandlerIF nh, double[][][] trips ) {

        HashMap tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        HashMap globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

        String timePeriod = "peak";
        
        double[][] linkAttributes = new double[2][];
        linkAttributes[0] = nh.getDist();
        linkAttributes[1] = nh.getCongestedTime();
        
        char[] userClasses = nh.getUserClasses();

        Skims skims = new Skims( nh, tsPropertyMap, globalPropertyMap );

        
        for (int m=0; m < userClasses.length; m++) {

            double total = 0.0;
            for (int i=0; i < trips[m].length; i++)
                for (int j=0; j < trips[m][i].length; j++)
                    total += trips[m][i][j];
            
                    
            // log the average sov trip travel distance and travel time for this assignment
            logger.info("Generating Time and Distance " + timePeriod + " skims for subnetwork " + userClasses[m] + " (class " + m + ") ...");
            
            if (total > 0.0) {

                Matrix[] skimMatrices = skims.getHwySkimMatrices( timePeriod, linkAttributes, userClasses[m] );

                logger.info( "Total " + timePeriod + " demand for subnetwork " + userClasses[m] + " (class " + m + ") = " + total + " trips."); 

                double[] distSummaries = skims.getAvgTripSkims ( trips[m], skimMatrices[0] );
                
                logger.info( "Average subnetwork " + userClasses[m] + " (class " + m + ") " + timePeriod + " trip travel distance = " + distSummaries[0] + " miles."); 
                logger.info( "Number of disconnected O/D pairs in subnetwork " + userClasses[m] + " (class " + m + ") based on distance = " + distSummaries[1]);

                double[] timeSummaries = skims.getAvgTripSkims ( trips[m], skimMatrices[1] );
                
                logger.info( "Average subnetwork " + userClasses[m] + " (class " + m + ") " + timePeriod + " trip travel time = " + timeSummaries[1] + " minutes."); 
                logger.info( "Number of disconnected O/D pairs in subnetwork " + userClasses[m] + " (class " + m + ") based on time = " + timeSummaries[1]);
                
            }
            else {
                
                logger.info("No demand for subnetwork " + userClasses[m] + " (class " + m + ") therefore, no average time or distance calculated.");
                
            }
                    
        }

    }
    
    
    
    
    public void writeHighwaySkimMatrix ( NetworkHandlerIF nh, String assignmentPeriod, String skimType, char modeChar ) {

        HashMap tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        HashMap globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

		logger.info("Writing " + assignmentPeriod + " time skim matrix for highway mode " + modeChar + " to disk...");
        long startTime = System.currentTimeMillis();
        
    	Skims skims = new Skims(nh, tsPropertyMap, globalPropertyMap);
    	
        skims.writeHwySkimMatrix ( assignmentPeriod, skimType, modeChar);

        logger.info("wrote the " + assignmentPeriod + " " + skimType + " skims for mode " + modeChar + " in " +
    			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

    }


    
    public void writeHighwaySkimMatrices ( NetworkHandlerIF nh, String assignmentPeriod, char modeChar ) {

        HashMap tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        HashMap globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

    	String[] skimTypeArray = { "time", "dist" };
    	
    	
		logger.info("Writing " + assignmentPeriod + " time and dist skim matrices for highway mode " + modeChar + " to disk...");
        long startTime = System.currentTimeMillis();
        
    	Skims skims = new Skims(nh, tsPropertyMap, globalPropertyMap);
    	
        skims.writeHwySkimMatrices ( assignmentPeriod, skimTypeArray, modeChar);

        logger.info("wrote the " + assignmentPeriod + " time and dist skims for mode " + modeChar + " in " +
    			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

    }


    
    private double[] runWalkTransitAssignment ( NetworkHandlerIF nh, AuxTrNet ag, String assignmentPeriod ) {
        
        // define assignment related variables dependent on the assignment period
        initializeHighwayAssignment ( nh, assignmentPeriod );
        
        // get the transit trip table to be assigned 
        DemandHandler d = new DemandHandler();
        d.setup( appRb, globalRb, assignmentPeriod, nh.getNumCentroids(), nh.getNumUserClasses(), nh.getNodeIndex(), nh.getAssignmentGroupMap(), highwayModeCharacters, nh.userClassesIncludeTruck() );
        d.buildDemandObject();
        
        double[][] tripTable = d.getWalkTransitTripTable ();
        
        // load the triptable on walk access transit network
        double[] rteBoardings = optimalStrategyNetworkLoading ( assignmentPeriod, "walk", nh, ag, tripTable );

        return rteBoardings;
    }
    
    
    private double[] runDriveTransitAssignment ( NetworkHandlerIF nh, AuxTrNet ag, String assignmentPeriod ) {
        
        // define assignment related variables dependent on the assignment period
        initializeHighwayAssignment ( nh, assignmentPeriod );
        
        // get the transit trip table to be assigned 
        DemandHandler d = new DemandHandler();
        d.setup( appRb, globalRb, assignmentPeriod, nh.getNumCentroids(), nh.getNumUserClasses(), nh.getNodeIndex(), nh.getAssignmentGroupMap(), highwayModeCharacters, nh.userClassesIncludeTruck() );
        d.buildDemandObject();
        
        double[][] tripTable = d.getDriveTransitTripTable ();
        
        // load the triptable on drive access transit network
        double[] rteBoardings = optimalStrategyNetworkLoading ( assignmentPeriod, "drive", nh, ag, tripTable );

        return rteBoardings;
        
    }
    
    
    private double[] optimalStrategyNetworkLoading ( String assignmentPeriod, String accessMode, NetworkHandlerIF nh, AuxTrNet ag, double[][] tripTable ) {
        
        // create an optimal strategy object for this highway and transit network
        OptimalStrategy os = new OptimalStrategy( ag );

        double[] routeBoardings = new double[ag.getMaxRoutes()];

        double tripTableColumn[] = new double[tripTable[0].length];

        double intrazonal = 0;
        double totalTrips = 0;
        double notLoadedTrips = 0;
        for ( int d=0; d < nh.getNumCentroids(); d++ ) {
            
            if ( d % 100 == 0 )
                logger.info( "loading " + assignmentPeriod + " period " + accessMode + " transit trips for destination zone " + d);
            
            os.buildStrategy( d );
            
            double tripSum = 0.0;
            for (int o=0; o < tripTable.length; o++) {

                // don't assign intra-zonal trips
                if ( o == d ) {
                    intrazonal += tripTable[o][d];
                    tripTableColumn[o] = 0.0; 
                    continue;
                }
                else {
                    tripTableColumn[o] = tripTable[o][d]; 
                    tripSum += tripTable[o][d];
                }
                
            }
                
            double destBoardings = 0.0;
            if ( tripSum > 0 ) {

                double[] routeBoardingsToDest = os.loadOptimalStrategyDest( tripTableColumn );
                
                for (int r=0; r < routeBoardings.length; r++) {
                    routeBoardings[r] += routeBoardingsToDest[r];
                    destBoardings += routeBoardingsToDest[r];
                }
                
                totalTrips += tripSum;
                notLoadedTrips += os.getTripsNotLoaded();
                
            }
            
        }

        logger.info( intrazonal + " " + assignmentPeriod + " period intrazonal " + accessMode + " transit trips, " + totalTrips + " total, " + notLoadedTrips + " not loaded." );
        
        return routeBoardings;
    }
    
   
    private AuxTrNet getTransitNetwork( NetworkHandlerIF nh, String period, String accessMode ) {
        
        HashMap tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);

        boolean create_new_network = CREATE_NEW_NETWORK;

        AuxTrNet ag = null; 
        String diskObjectFileName = null;
        
        // create a new transit network from d211 highway network file and d221 transit routes file, or read it from DiskObject.
        String key = period + accessMode + "TransitNetwork";
        String path = (String) tsPropertyMap.get( "diskObject.pathName" );
        if ( path.endsWith("/") || path.endsWith("\\") )
            diskObjectFileName = path + key + ".diskObject";
        else
            diskObjectFileName = path + "/" + key + ".diskObject";
        
        if ( create_new_network ) {
            ag = createTransitNetwork ( nh, period, accessMode );
            DataWriter.writeDiskObject ( ag, diskObjectFileName, key );
        }
        else {
            ag = (AuxTrNet) DataReader.readDiskObject ( diskObjectFileName, key );
        }

        
        return ag;
        
    }
    
    
    private AuxTrNet createTransitNetwork ( NetworkHandlerIF nh, String period, String accessMode ) {
        
        HashMap tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);

        String auxTransitNetworkListingFileName = (String)tsPropertyMap.get("AUX_TRANSIT_NETWORK_LISTING");

        // get the filename for the route files
        String d221File = null;
        if ( period.equalsIgnoreCase( "peak" ) )
            d221File = (String) tsPropertyMap.get( "d221.pk.fileName" );
        else if ( period.equalsIgnoreCase( "offpeak" ) )
            d221File = (String) tsPropertyMap.get( "d221.op.fileName" );

        if ( d221File == null ) {
            RuntimeException e = new RuntimeException();
            logger.error ( "Error reading routes file for specified " + period + " period.", e );
            throw e;
        }
        
        // read parameter for maximum number of transit routes
        int maxRoutes = Integer.parseInt ( (String)tsPropertyMap.get("MAX_TRANSIT_ROUTES") );

        // create transit routes object
        TrRoute tr = new TrRoute ( maxRoutes );

        //read transit route info from Emme/2 for d221 file for the specified time period
        tr.readTransitRoutes ( d221File );
            
        // associate transit segment node sequence with highway link indices
        tr.getLinkIndices (nh.getNetwork());



        // create an auxilliary transit network object
        AuxTrNet ag = new AuxTrNet(nh, tr);

        // build the auxilliary links for the given transit routes object
        ag.buildAuxTrNet ( accessMode );
        
        // define the forward star index arrays, first by anode then by bnode
        ag.setForwardStarArrays ();
        ag.setBackwardStarArrays ();

        
//      ag.printAuxTrLinks (24, tr);
        if ( auxTransitNetworkListingFileName != null )
            ag.printAuxTranNetwork( auxTransitNetworkListingFileName );
//      ag.printTransitNodePointers();

        String myDateString = DateFormat.getDateTimeInstance().format(new Date());
        logger.info ("done creating transit network AuxTrNetTest: " + myDateString);

        return ag;
    }

    
    
    public void logTransitBoardingsReport ( AuxTrNet ag, String periodHeadingLabel, double[] routeBoardings ) {
        
        TrRoute tr = ag.getTrRoute();
        
        // construct a format string for the description field
        int maxStringLength = 0;
        for (int rte=0; rte < tr.getLineCount(); rte++)
            if ( tr.getDescription(rte).length() > maxStringLength )
                maxStringLength = tr.getDescription(rte).length();
        String descrFormat = "%-" + (maxStringLength+4) + "s";

        logger.info ( String.format("%-10s", "Count") + String.format("%-10s", "Line") + String.format(descrFormat, "Description") + String.format("%-8s", "Mode") + String.format("%18s", (periodHeadingLabel + " Boardings")) );
        logger.info ( String.format("%-10s", "-----") + String.format("%-10s", "----") + String.format(descrFormat, "-----------") + String.format("%-8s", "----") + String.format("%18s", "-----------------") );

        float total = 0.0f;
        for (int rte=0; rte < tr.getLineCount(); rte++) { 
            logger.info ( String.format("%-10s", (rte+1)) + String.format("%-10s", tr.getLine(rte)) + String.format(descrFormat, tr.getDescription(rte)) + String.format("%-8c", tr.getMode(rte)) + String.format("%18.2f", routeBoardings[rte]) );
            total += routeBoardings[rte];
        }
        logger.info ( String.format("%-20s", "Total Boardings") + String.format(descrFormat, "") + String.format("%-8s", "") + String.format("%18.2f", total) );
        
    }
    
    
    private void assignAndSkimHighway ( NetworkHandlerIF nh, String assignmentPeriod ) {

        // run peak highway assignment
        runHighwayAssignment( nh, assignmentPeriod );
        logger.info ("done with " + assignmentPeriod + " highway assignment.");

        // write the auto time and distance highway skim matrices to disk
        writeHighwaySkimMatrices ( nh, assignmentPeriod, 'a' );
        logger.info ("done writing " + assignmentPeriod + " highway skims files.");
        
        logger.info ("done with " + assignmentPeriod + " highway skimming and loading.");
        
    }


    private void assignAndSkimTransit ( NetworkHandlerIF nh, String assignmentPeriod, ResourceBundle appRb, ResourceBundle globalRb ) {

        // generate walk transit network
        String accessMode = "walk";
        AuxTrNet ag = getTransitNetwork( nh, assignmentPeriod, accessMode );
        logger.info ("done generating " + assignmentPeriod + " " + accessMode + " transit network.");
        
        // generate walk transit skim matrices
        TransitSkimManager tsm = new TransitSkimManager( ag, appRb, globalRb );     
        if ( assignmentPeriod.equalsIgnoreCase("peak") )
            tsm.writePeakWalkTransitSkims();
        else
            tsm.writeOffPeakWalkTransitSkims();
        logger.info ("done writing " + assignmentPeriod + " " + accessMode + " transit skims files.");

        // load walk transit trips
        double[] walkTransitBoardings = runWalkTransitAssignment ( nh, ag, assignmentPeriod );
        logger.info ("done with " + assignmentPeriod + " " + accessMode + " transit assignment.");
        
        
        // generate drive transit network
        accessMode = "drive";
        ag = getTransitNetwork( nh, assignmentPeriod, accessMode );
        logger.info ("done generating " + assignmentPeriod + " " + accessMode + " transit network.");
        
        // generate drive transit skim matrices
        tsm = new TransitSkimManager( ag, appRb, globalRb );     
        if ( assignmentPeriod.equalsIgnoreCase("peak") )
            tsm.writePeakDriveTransitSkims();
        else
            tsm.writeOffPeakDriveTransitSkims();
        logger.info ("done writing " + assignmentPeriod + " " + accessMode + " transit skims files.");

        // load drive transit trips
        double[] driveTransitBoardings = runDriveTransitAssignment ( nh, ag, assignmentPeriod );
        logger.info ("done with " + assignmentPeriod + " " + accessMode + " transit assignment.");
        
        
        TrRoute tr = ag.getTrRoute();
        double[] totalBoardings = new double[tr.getLineCount()]; 
        
        for (int rte=0; rte < totalBoardings.length; rte++) { 
            totalBoardings[rte] = walkTransitBoardings[rte] + driveTransitBoardings[rte];
        }
        
        logTransitBoardingsReport ( ag, assignmentPeriod, totalBoardings );
        
        logger.info ("done with " + assignmentPeriod + " transit skimming and loading.");
        
    }


    public static void main (String[] args) {

        TS tsMain = new TS( ResourceBundle.getBundle(args[0]), ResourceBundle.getBundle (args[1]) );

        String rpcConfigFileName = (args.length == 3 ? args[2] : null);
        
        
        // Need a DafNode instance to read a config file and initialize a DafNode.
        if ( rpcConfigFileName != null ) {
            
            try {
                DafNode.getInstance().initClient(rpcConfigFileName);
            }
            catch (MalformedURLException e) {
                logger.error( "MalformedURLException caught in TS.main() initializing a DafNode.", e);
            }
            catch (Exception e) {
                logger.error( "Exception caught in TS.main() initializing a DafNode.", e);
            }
            
        }

        
        // generate a NetworkHandler object to use for peak period assignments and skimming
        NetworkHandlerIF nhPeak = NetworkHandler.getInstance( rpcConfigFileName );

        tsMain.runHighwayAssignment(nhPeak, "peak");
        
//        tsMain.assignAndSkimHighway ( nhPeak, "peak" );
//        tsMain.assignAndSkimTransit ( nhPeak, "peak", ResourceBundle.getBundle(args[1]), ResourceBundle.getBundle(args[2]) );
// 
//        nhPeak = null;
//        
//        // generate a NetworkHandler object to use for off-peak period assignments and skimming
//        NetworkHandlerIF nhOffPeak = NetworkHandler.getInstance( rpcConfigFileName );
//
//        tsMain.assignAndSkimHighway ( nhOffPeak, "offpeak" );
//        tsMain.assignAndSkimTransit ( nhOffPeak, "offpeak", ResourceBundle.getBundle(args[1]), ResourceBundle.getBundle(args[2]) );
//        
//        nhOffPeak = null;

        logger.info ("TS.main() is finished.");
 
    }

}

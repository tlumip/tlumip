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


import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.rpc.DafNode;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.ts.assign.Skims;
import com.pb.tlumip.ts.assign.TransitAssignAndSkimManager;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


public class TS {

	protected static Logger logger = Logger.getLogger(TS.class);
    

    static final boolean CREATE_NEW_NETWORK = true;
    public boolean SKIM_ONLY = false;
    

    ResourceBundle appRb;
    ResourceBundle globalRb;
    

    
    public TS(ResourceBundle appRb, ResourceBundle globalRb) {

        this.appRb = appRb;
        this.globalRb = globalRb;
        
        try {
            String skimsOnlyFlag = this.appRb.getString("skimsOnly.flag");
            if ( skimsOnlyFlag != null ) {
                if ( skimsOnlyFlag.equalsIgnoreCase("true") )
                    SKIM_ONLY = true;
            }
        }
        catch (MissingResourceException e) {
            // if this exception is caught, no flag was set, so continue on with SKIM_ONLY value false.
        }
        
	}


    public void runHighwayAssignment( NetworkHandlerIF nh ) {

        String assignmentPeriod = nh.getTimePeriod();
        // run the multiclass assignment for the time period
    	multiclassEquilibriumHighwayAssignment ( nh, assignmentPeriod );
        logger.info("TS main - equilibrium assignment done\n\n");
		
    	// if at some point in time we want to have truck specific highway skims,
    	// we'd create them here and would modify the the properties file to include
    	// class specific naming in skims file properties file keynames.  We'd also
    	// modify the method above to distinguish the class id in addition to period
    	// and skim types.
    	
        logger.info ("done with " + assignmentPeriod + " highway assignment.");
        
    }
    
    
    
    private void multiclassEquilibriumHighwayAssignment ( NetworkHandlerIF nh, String assignmentPeriod ) {
        
		long startTime = System.currentTimeMillis();
		
		// create Frank-Wolfe Algortihm Object
        String myDateString;
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("creating and initializing a " + assignmentPeriod + " FW object at: " + myDateString);
		FW fw = new FW();
		fw.initialize( appRb, globalRb,nh, nh.getHighwayModeCharacters() );


		// Compute Frank-Wolfe solution
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("starting " + assignmentPeriod + " FW iterations at: " + myDateString);
		fw.iterate ();
		myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("end of " + assignmentPeriod + " FW iterations at: " + myDateString);

        logger.info( assignmentPeriod + " highway assignment finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes.");

        
        
        
        String assignmentResultsTarget = String.format( "%s.output.fileName", assignmentPeriod.toLowerCase() );
        String assignmentResultsFileName = appRb.getString( assignmentResultsTarget );

        
		logger.info("Writing results file with " + assignmentPeriod + " assignment results.");
        nh.writeNetworkAttributes( assignmentResultsFileName );

		
        logger.info("Checking network connectivity and computing TLDs after " + assignmentPeriod + " period assignment.");
        checkODPairsWithTripsForNetworkConnectivity(nh);
        

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
		checkODConnectivity(nh, dummyTripTable, "amPeak");

    }
    
    
    
    public void checkODPairsWithTripsForNetworkConnectivity (NetworkHandlerIF nh) {
        
        HashMap globalMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

        String timePeriod = nh.getTimePeriod();
        int startHour = -1;
        int endHour = -1;
        
        if ( timePeriod.equalsIgnoreCase("ampeak")) {
            startHour = Integer.parseInt((String)globalMap.get("am.peak.start"));
            endHour = Integer.parseInt( (String)globalMap.get("am.peak.end") );
        }
        else if ( timePeriod.equalsIgnoreCase("pmpeak")) {
            startHour = Integer.parseInt((String)globalMap.get("pm.peak.start"));
            endHour = Integer.parseInt( (String)globalMap.get("pm.peak.end") );
        }
        else if ( timePeriod.equalsIgnoreCase("mdoffpeak")) {
            startHour = Integer.parseInt((String)globalMap.get("md.offpeak.start"));
            endHour = Integer.parseInt( (String)globalMap.get("md.offpeak.end") );
        }
        else if ( timePeriod.equalsIgnoreCase("ntoffpeak")) {
            startHour = Integer.parseInt((String)globalMap.get("nt.offpeak.start"));
            endHour = Integer.parseInt( (String)globalMap.get("nt.offpeak.end") );
        }
        else {
            logger.error ( "time period specifed as: " + timePeriod + ", but must be either 'ampeak', 'mdoffpeak', 'pmpeak', or 'ntoffpeak'." );
            System.exit(-1);
        }
        

        logger.info( "requesting that demand matrices get built." );
        DemandHandlerIF d = DemandHandler.getInstance();
        d.setup( nh.getUserClassPces(), (String)globalMap.get("sdt.person.trips"), (String)globalMap.get("ldt.vehicle.trips"), Double.parseDouble((String)globalMap.get("pt.sample.rate")), (String)globalMap.get("ct.truck.trips"), (String)globalMap.get("et.truck.trips"), startHour, endHour, timePeriod, nh.getNumCentroids(), nh.getNumUserClasses(), nh.getNodeIndex(), nh.getAlphaDistrictIndex(), nh.getDistrictNames(), nh.getAssignmentGroupChars(), nh.getHighwayModeCharacters(), nh.userClassesIncludeTruck() );
        d.buildHighwayDemandObject();

        double[][][] multiclassTripTable = d.getMulticlassTripTables();
		checkODConnectivity(nh, multiclassTripTable, timePeriod);
    }

    
    
    
    public void checkODConnectivity ( NetworkHandlerIF nh, double[][][] trips, String timePeriod ) {

        double[][] linkAttributes = new double[2][];
        linkAttributes[0] = nh.getDist();
        linkAttributes[1] = nh.getCongestedTime();
        
        String[] names = new String[2];
        names[0] = "test1";
        names[1] = "test1";

        String[] description = new String[2];
        description[0] = "checkODConnectivity dist matrix";
        description[1] = "checkODConnectivity time matrix";

        char[] userClasses = nh.getUserClasses();

        Skims skims = new Skims( nh, appRb, globalRb );

        double[] distFreqs = null;
        double[] timeFreqs = null;
        
        
            
        for (int m=0; m < userClasses.length; m++) {

            double total = 0.0;
            for (int i=0; i < trips[m].length; i++)
                for (int j=0; j < trips[m][i].length; j++)
                    total += trips[m][i][j];
            
                    
            // log the average sov trip travel distance and travel time for this assignment
            logger.info("Generating Time and Distance " + timePeriod + " skims for subnetwork " + userClasses[m] + " (class " + m + ") ...");
            
            if (total > 0.0) {

                Matrix[] skimMatrices = skims.getHwySkimMatrices( timePeriod, linkAttributes, names, description, userClasses[m] );

                logger.info( "Total " + timePeriod + " demand for subnetwork " + userClasses[m] + " (class " + m + ") = " + total + " trips."); 

                double[] distSummaries = skims.getAvgTripSkims ( trips[m], skimMatrices[0], nh.getIndexNode() );
                distFreqs = skims.getSkimTripFreqs();
                
                logger.info( "Average subnetwork " + userClasses[m] + " (class " + m + ") " + timePeriod + " trip travel distance = " + distSummaries[0] + " miles."); 
                logger.info( "Number of disconnected O/D pairs in subnetwork " + userClasses[m] + " (class " + m + ") based on distance = " + distSummaries[1]);

                double[] timeSummaries = skims.getAvgTripSkims ( trips[m], skimMatrices[1], nh.getIndexNode() );
                timeFreqs = skims.getSkimTripFreqs();
                
                logger.info( "Average subnetwork " + userClasses[m] + " (class " + m + ") " + timePeriod + " trip travel time = " + timeSummaries[0] + " minutes."); 
                logger.info( "Number of disconnected O/D pairs in subnetwork " + userClasses[m] + " (class " + m + ") based on time = " + timeSummaries[1]);

                
                
                HashMap appMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
                String fileName = (String)appMap.get("timeDistTripFreqs.fileName");
                
                if ( fileName != null ) {

                    int index = fileName.indexOf(".csv");
                    if ( index < 0 )
                        fileName += "_" + String.valueOf(userClasses[m]) + "_" + timePeriod;
                    else {
                        fileName = fileName.substring(0, index);
                        fileName += "_" + String.valueOf(userClasses[m]) + "_" + timePeriod + ".csv";
                    }
                    

                    try {
                        
                        PrintWriter outStream =  new PrintWriter(new BufferedWriter( new FileWriter( fileName ) ) );

                        outStream.println( "interval,minuteTrips,mileTrips" );
                        
                        for (int i=0; i < Math.max( timeFreqs.length, distFreqs.length); i++) {
                            String record = String.format("%d,%.1f,%.1f", i, ( i < timeFreqs.length ? timeFreqs[i] : 0.0 ), ( i < distFreqs.length ? distFreqs[i] : 0.0 ) );
                            outStream.println( record );
                        }
                            
                        outStream.close();

                    }
                    catch (IOException e) {
                        logger.fatal("IO Exception writing trip length frequencies for time and distance to file: " + fileName, e );
                    }
                }
                
            }
            else {
                
                logger.info("No demand for subnetwork " + userClasses[m] + " (class " + m + ") therefore, no average time or distance calculated.");
                
            }
                    
        }

    }
    
    
    
    
    public void writeHighwaySkimMatrices ( NetworkHandlerIF nh, char[] hwyModeChars ) {
        
        Skims skims = new Skims(nh, appRb, globalRb);
        
        String assignmentPeriod = nh.getTimePeriod();

        for ( char mode : hwyModeChars ) {
            logger.info( String.format("Compute shortest generalized cost trees for skimming %s time, dist and toll skim matrices for highway mode '%c' ...", assignmentPeriod, mode) );
            String[] skimTypeArray = { "time", "dist", "toll", "fftime" };
            skims.writeHwySkimMatrices ( assignmentPeriod, skimTypeArray, mode );
        }
        
    }


    
    public void assignAndSkimTransit ( NetworkHandlerIF nh, ResourceBundle appRb, ResourceBundle globalRb ) {

        String assignmentPeriod = nh.getTimePeriod();
        
        // generate transit load and skim manager object, then load and skim all networks
        TransitAssignAndSkimManager tsm = new TransitAssignAndSkimManager( nh, appRb, globalRb );
        
        if ( SKIM_ONLY ) {
            tsm.assignAndSkimTransit ( nh, assignmentPeriod, true );
            logger.info ("done with " + assignmentPeriod + " period transit skimming.");
        }
        else {
            tsm.assignAndSkimTransit ( nh, assignmentPeriod, false);
            logger.info ("done with " + assignmentPeriod + " period transit loading and skimming.");
        }
        
    }

    

    public void loadAssignmentResults ( NetworkHandlerIF nh, ResourceBundle appRb ) {
        
        // get the filename where highway assignment total link flows and times are stored from the property file. 
        String fileNameKey = String.format( "%s.output.fileName", nh.getTimePeriod() );
        String fileName = appRb.getString( fileNameKey );
        
        // read the link data from the assignment results csv file into a TableDataSet 
        TableDataSet assignmentResults = null;
        
        OLD_CSVFileReader csvReader = new OLD_CSVFileReader();
        try {
            assignmentResults = csvReader.readFile( new File(fileName) );
        } catch (IOException e) {
            logger.error ( "IOException reading loaded link data from assignment results file: " + fileName, e );
            System.exit(-1);
        }

        
        
        // get the column names of the userclass flow vectors in the results file.
        String resultsString = nh.getAssignmentResultsString();
        String[] columnNames = assignmentResults.getColumnLabels();
        
        ArrayList flowColumnNames = new ArrayList();
        for (int i=0; i < columnNames.length; i++) {
            if ( columnNames[i].startsWith( resultsString ) )
                flowColumnNames.add( columnNames[i] );
        }
        int numFlowFields = flowColumnNames.size();

        if ( numFlowFields > nh.getNumUserClasses() )
            numFlowFields = nh.getNumUserClasses();
        

        // update the multiclass flow fields and congested time field in NetworkHandler
        double[][] flows = new double[numFlowFields][];
        for (int i=0; i < numFlowFields; i++)
            flows[i] = assignmentResults.getColumnAsDouble( (String)flowColumnNames.get(i) );
            
        nh.setFlows(flows);
        
        
        int[] an = assignmentResults.getColumnAsInt( nh.getAssignmentResultsAnodeString() );
        int[] bn = assignmentResults.getColumnAsInt( nh.getAssignmentResultsBnodeString() );
        double[] times = assignmentResults.getColumnAsDouble( nh.getAssignmentResultsTimeString() );
        double[] timau = new double[times.length];
        
        for (int i=0; i < an.length; i++) {
            int k = nh.getLinkIndex(an[i], bn[i]);
            timau[k] = times[i];
        }
        
        nh.setTimau(timau);
        
    }
    
    
    
    public int setupHighwayNetwork ( NetworkHandlerIF nh, HashMap appMap, HashMap globalMap, String timePeriod ) {
        
        String networkFileName = (String)appMap.get("d211.fileName");
        String networkDiskObjectFileName = (String)appMap.get("NetworkDiskObject.file");
        
        String turnTableFileName = (String)appMap.get( "d231.fileName" );
        String networkModsFileName = (String)appMap.get( "d211Mods.fileName" );
        String extraAtribsFileName = (String)appMap.get( "extraAttribs.fileName" );
        
        String vdfFileName = (String)appMap.get("vdf.fileName");
        String vdfIntegralFileName = (String)appMap.get("vdfIntegral.fileName");
        
        String a2bFileName = (String) globalMap.get( "alpha2beta.file" );
        
        // get peak or off-peak volume factor from properties file
        String volumeFactor="";
        if ( timePeriod.equalsIgnoreCase( "ampeak" ) )
            volumeFactor = (String)globalMap.get("am.peak.volume.factor");
        else if ( timePeriod.equalsIgnoreCase( "mdoffpeak" ) )
            volumeFactor = (String)globalMap.get("md.offpeak.volume.factor");
        else if ( timePeriod.equalsIgnoreCase( "pmpeak" ) )
            volumeFactor = (String)globalMap.get("pm.peak.volume.factor");
        else if ( timePeriod.equalsIgnoreCase( "ntoffpeak" ) )
            volumeFactor = (String)globalMap.get("nt.offpeak.volume.factor");
        else {
            logger.error ( "time period specifed as: " + timePeriod + ", but must be either 'ampeak', 'mdoffpeak', 'pmpeak', or 'ntoffpeak'." );
            System.exit(-1);
        }
        
        String userClassesString = (String)appMap.get("userClass.modes");
        String userClassPCEsString = (String)appMap.get("userClass.pces");
        String truckClass1String = (String)appMap.get( "truckClass1.modes" );
        String truckClass2String = (String)appMap.get( "truckClass2.modes" );
        String truckClass3String = (String)appMap.get( "truckClass3.modes" );
        String truckClass4String = (String)appMap.get( "truckClass4.modes" );
        String truckClass5String = (String)appMap.get( "truckClass5.modes" );

        String walkSpeed = (String)globalMap.get( "sdt.walk.mph" );
        
        
        String[] propertyValues = new String[NetworkHandler.NUMBER_OF_PROPERTY_VALUES];
        Arrays.fill(propertyValues, "");
        
        
        if ( networkFileName != null ) propertyValues[NetworkHandlerIF.NETWORK_FILENAME_INDEX] = networkFileName;
        if ( networkDiskObjectFileName != null ) propertyValues[NetworkHandlerIF.NETWORK_DISKOBJECT_FILENAME_INDEX] = networkDiskObjectFileName;
        if ( vdfFileName != null ) propertyValues[NetworkHandlerIF.VDF_FILENAME_INDEX] = vdfFileName;
        if ( vdfIntegralFileName != null ) propertyValues[NetworkHandlerIF.VDF_INTEGRAL_FILENAME_INDEX] = vdfIntegralFileName;
        if ( a2bFileName != null ) propertyValues[NetworkHandlerIF.ALPHA2BETA_FILENAME_INDEX] = a2bFileName;
        if ( turnTableFileName != null ) propertyValues[NetworkHandlerIF.TURNTABLE_FILENAME_INDEX] = turnTableFileName;
        if ( networkModsFileName != null ) propertyValues[NetworkHandlerIF.NETWORKMODS_FILENAME_INDEX] = networkModsFileName;
        if ( extraAtribsFileName != null ) propertyValues[NetworkHandlerIF.EXTRA_ATTRIBS_FILENAME_INDEX] = extraAtribsFileName;
        if ( volumeFactor != null ) propertyValues[NetworkHandlerIF.VOLUME_FACTOR_INDEX] = volumeFactor;
        if ( userClassesString != null ) propertyValues[NetworkHandlerIF.USER_CLASSES_STRING_INDEX] = userClassesString;
        if ( userClassPCEsString != null ) propertyValues[NetworkHandlerIF.USER_CLASS_PCES_STRING_INDEX] = userClassPCEsString;
        if ( truckClass1String != null ) propertyValues[NetworkHandlerIF.TRUCKCLASS1_STRING_INDEX] = truckClass1String;
        if ( truckClass2String != null ) propertyValues[NetworkHandlerIF.TRUCKCLASS2_STRING_INDEX] = truckClass2String;
        if ( truckClass3String != null ) propertyValues[NetworkHandlerIF.TRUCKCLASS3_STRING_INDEX] = truckClass3String;
        if ( truckClass4String != null ) propertyValues[NetworkHandlerIF.TRUCKCLASS4_STRING_INDEX] = truckClass4String;
        if ( truckClass5String != null ) propertyValues[NetworkHandlerIF.TRUCKCLASS5_STRING_INDEX] = truckClass5String;
        if ( walkSpeed != null ) propertyValues[NetworkHandlerIF.WALK_SPEED_INDEX] = walkSpeed;
        
        
        return nh.setupHighwayNetworkObject ( timePeriod, propertyValues );
        
    }
    
    
    
    public void bench ( ResourceBundle appRb, ResourceBundle globalRb, String rpcConfigFileName ) {

        long startTime = System.currentTimeMillis();
        
        String period = "ampeak";

        // generate a NetworkHandler object to use for peak period assignments and skimming
        logger.info( "TS.bench() getting a NetworkHandler instance and setting the config file name value." );
        
        NetworkHandlerIF nhPeak = NetworkHandler.getInstance( rpcConfigFileName );
        nhPeak.setRpcConfigFileName( rpcConfigFileName );
        setupHighwayNetwork( nhPeak, ResourceUtil.changeResourceBundleIntoHashMap(appRb), ResourceUtil.changeResourceBundleIntoHashMap(globalRb), period );
        logger.info ("created " + period + " Highway NetworkHandler object: " + nhPeak.getNodeCount() + " highway nodes, " + nhPeak.getLinkCount() + " highway links." );

//        nhPeak.startDataServer();

        runHighwayAssignment(nhPeak);

        logger.info ("TS.bench() finished peak highway assignment in " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");

//        nhPeak.stopDataServer();

    }

    
    
    
    public static void main (String[] args) {

        TS tsMain = new TS( ResourceBundle.getBundle(args[0]), ResourceBundle.getBundle (args[1]) );

        long startTime = System.currentTimeMillis();


        
        // An rpc config file can be used to define a cluster and the distribution of objects to multiple machines.
        // If this file is not specified as the 3rd command line argument, the application runs entirely in this main jvm.
        String rpcConfigFileName = (args.length == 3 ? args[2] : null);

        
        // Need a DafNode instance to read a config file and initialize a DafNode.
        if ( rpcConfigFileName != null ) {
            
            try {
                logger.info( "TS.main() using DafNode.initClient() to initialize a DafNode for the objects TS creates." );
                DafNode.getInstance().initClient(rpcConfigFileName);
            }
            catch (MalformedURLException e) {
                logger.error( "MalformedURLException caught in TS.main() initializing a DafNode.", e);
            }
            catch (Exception e) {
                logger.error( "Exception caught in TS.main() initializing a DafNode.", e);
            }
            
        }



/*        
        // generate a NetworkHandler object to use for peak period assignments and skimming
        NetworkHandlerIF nhPeak = NetworkHandler.getInstance( rpcConfigFileName );
        tsMain.setupNetwork( nhPeak, ResourceUtil.getResourceBundleAsHashMap(args[0]), ResourceUtil.getResourceBundleAsHashMap(args[1]), "ampeak" );
        
        nhPeak.checkForIsolatedLinks();

        tsMain.loadAssignmentResults ( nhPeak, ResourceBundle.getBundle(args[0]));
*/        

        


/*        
        // TS Example 1 - Run a peak highway assignment:
        
        // run peak highway assignment
        NetworkHandlerIF nhPeak = NetworkHandler.getInstance( rpcConfigFileName );
        nhPeak.setRpcConfigFileName( rpcConfigFileName );
        tsMain.setupHighwayNetwork( nhPeak, ResourceUtil.getResourceBundleAsHashMap(args[0]), ResourceUtil.getResourceBundleAsHashMap(args[1]), "ampeak" );
//        nhPeak.checkForIsolatedLinks();
//        tsMain.multiclassEquilibriumHighwayAssignment( nhPeak, "ampeak" );
        tsMain.loadAssignmentResults ( nhPeak, ResourceBundle.getBundle(args[0]) );
        nhPeak.startDataServer();

        // write the auto time and distance highway skim matrices to disk based on attribute values in NetworkHandler after assignment
//        char[] hwyModeChars = nhPeak.getHighwayModeCharacters();
        char[] hwyModeChars = { 'a', 'd' };
        tsMain.writeHighwaySkimMatrices ( nhPeak, hwyModeChars );
*/

        
        
        
      
        // TS Example 2 - Read peak highway assignment results into NetworkHandler, then load and skim transit network
        NetworkHandlerIF nhPeak = NetworkHandler.getInstance( rpcConfigFileName );
        nhPeak.setRpcConfigFileName( rpcConfigFileName );
        tsMain.setupHighwayNetwork( nhPeak, ResourceUtil.getResourceBundleAsHashMap(args[0]), ResourceUtil.getResourceBundleAsHashMap(args[1]), "ampeak" );
//        nhPeak.checkForIsolatedLinks();
        nhPeak.startDataServer();
        //tsMain.multiclassEquilibriumHighwayAssignment( nhPeak, nhPeak.getTimePeriod() );
        char[] hwyModeChars = { 'a', 'd', 'e', 'f' };
        tsMain.writeHighwaySkimMatrices ( nhPeak, hwyModeChars );
        tsMain.loadAssignmentResults ( nhPeak, ResourceBundle.getBundle(args[0]) );
        logger.info ("Network data server running...");
        
        tsMain.assignAndSkimTransit ( nhPeak, ResourceBundle.getBundle(args[0]), ResourceBundle.getBundle(args[1]) );
      
       
        
        
        // test capability to get list of highway network link ids from a transit route
//        String name = "B_1334";
//        int[] linkIds = nhPeak.getTransitRouteLinkIds( name );
//        String linksString = String.format("%d", linkIds[0]);
//        for (int i=1; i < linkIds.length; i++)
//            linksString += String.format(", %d", linkIds[i]);
//        logger.info ( String.format( "link ids for route %s : %s", name, linksString) );

        
        // run the benchmark highway assignment procedure
//        tsMain.bench ( tsMain.appRb, tsMain.globalRb, rpcConfigFileName );
        

        // run the benchmark highway assignment procedure
//        ApplicationOrchestrator ao = new ApplicationOrchestrator(null);
//        ao.runTSModel(tsMain.appRb, tsMain.globalRb);
        

        logger.info ("TS.main() finished in " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
        nhPeak.stopDataServer();
        //logger.info ("Network data server stopped.");
        logger.info ("TS.main() exiting.");

    }

}

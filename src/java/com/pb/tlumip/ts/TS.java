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
import com.pb.models.pt.TripModeType;
import com.pb.models.pt.ldt.LDTripModeType;
import com.pb.tlumip.ts.assign.Skims;
import com.pb.tlumip.ts.assign.TransitSkimManager;
import com.pb.tlumip.ts.transit.OptimalStrategy;
import com.pb.tlumip.ts.transit.TrRoute;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.*;



public class TS {

	protected static Logger logger = Logger.getLogger(TS.class);


    static final boolean CREATE_NEW_NETWORK = true;
    

    ResourceBundle appRb;
    ResourceBundle globalRb;
    

    
    public TS(ResourceBundle appRb, ResourceBundle globalRb) {

        this.appRb = appRb;
        this.globalRb = globalRb;
        
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
		
        HashMap tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);

		String myDateString;

		
		
		// create Frank-Wolfe Algortihm Object
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
        
        String timePeriod = "peak";
        int startHour = 600;
        int endHour = 900;
        

        logger.info( "requesting that demand matrices get built." );
        DemandHandlerIF d = DemandHandler.getInstance();
        d.setup( (String)ResourceUtil.changeResourceBundleIntoHashMap(appRb).get("sdt.fileName"), (String)ResourceUtil.changeResourceBundleIntoHashMap(appRb).get("ldt.fileName"), (String)ResourceUtil.changeResourceBundleIntoHashMap(appRb).get("ct.fileName"), startHour, endHour, timePeriod, nh.getNumCentroids(), nh.getNumUserClasses(), nh.getNodeIndex(), nh.getAssignmentGroupChars(), nh.getHighwayModeCharacters(), nh.userClassesIncludeTruck() );
        d.buildHighwayDemandObject();

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


    
    public void writeHighwaySkimMatrices ( NetworkHandlerIF nh, char modeChar ) {

        String assignmentPeriod = nh.getTimePeriod();
        long startTime = System.currentTimeMillis();
        logger.info("Writing " + assignmentPeriod + " time, dist and toll skim matrices for highway mode " + modeChar + " to disk...");

        
        HashMap tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        HashMap globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);
    	Skims skims = new Skims(nh, tsPropertyMap, globalPropertyMap);
        
        String[] skimTypeArray = { "time", "dist", "toll" };
        skims.writeHwySkimMatrices ( assignmentPeriod, skimTypeArray, modeChar);

        
        logger.info("wrote the " + assignmentPeriod + " time, dist and toll skims for mode " + modeChar + " in " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");

    }


    
    private double[] runTransitAssignment ( NetworkHandlerIF nh, String accessMode, String routeType, ArrayList tripModeList ) {
        
        String assignmentPeriod = nh.getTimePeriod();
        
        int startHour = 0;
        int endHour = 0;
        if ( assignmentPeriod.equalsIgnoreCase( "peak" ) ) {
            // get peak period definitions from property files
            startHour = Integer.parseInt( (String)ResourceUtil.changeResourceBundleIntoHashMap(globalRb).get("AM_PEAK_START") );
            endHour = Integer.parseInt( (String)ResourceUtil.changeResourceBundleIntoHashMap(globalRb).get("AM_PEAK_END") );
        }
        else if ( assignmentPeriod.equalsIgnoreCase( "offpeak" ) ) {
            // get off-peak period definitions from property files
            startHour = Integer.parseInt( (String)ResourceUtil.changeResourceBundleIntoHashMap(globalRb).get("OFF_PEAK_START") );
            endHour = Integer.parseInt( (String)ResourceUtil.changeResourceBundleIntoHashMap(globalRb).get("OFF_PEAK_END") );
        }
        
        
        // get the transit trip table to be assigned 
        DemandHandler d = new DemandHandler();
        d.setup( (String)ResourceUtil.changeResourceBundleIntoHashMap(appRb).get("sdt.fileName"), (String)ResourceUtil.changeResourceBundleIntoHashMap(appRb).get("ldt.fileName"), (String)ResourceUtil.changeResourceBundleIntoHashMap(appRb).get("ct.fileName"), startHour, endHour, assignmentPeriod, nh.getNumCentroids(), nh.getNumUserClasses(), nh.getNodeIndex(), nh.getAssignmentGroupChars(), nh.getHighwayModeCharacters(), nh.userClassesIncludeTruck() );
        
        double[][] tripTable = d.getTripTablesForModes ( tripModeList );
        
        
        // load the triptable on walk access transit network
        double[] rteBoardings = optimalStrategyNetworkLoading ( nh, accessMode, routeType, tripTable );

        return rteBoardings;
    }
    
    
    private double[] optimalStrategyNetworkLoading ( NetworkHandlerIF nh, String accessMode, String routeType, double[][] tripTable ) {
        
        String assignmentPeriod = nh.getTimePeriod();
        
        // create an optimal strategy object for this highway and transit network
        OptimalStrategy os = new OptimalStrategy( nh );

        double[] routeBoardings = new double[nh.getMaxRoutes()];

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
    
    
    
    public void  saveTransitBoardings ( NetworkHandlerIF nh, String accessMode, String routeType, double[] transitBoardings, HashMap routeBoardings ) {
        
        TrRoute tr = nh.getTrRoute();

        
        int accessIndex = -1;
        if ( accessMode.equalsIgnoreCase("walk") )
            accessIndex = 0;
        else if ( accessMode.equalsIgnoreCase("drive") )
            accessIndex = 1;
        
        
        int routeTypeIndex = -1;
        if ( routeType.equalsIgnoreCase("air") )
            routeTypeIndex = 0;
        else if ( routeType.equalsIgnoreCase("hsr") )
            routeTypeIndex = 1;
        else if ( routeType.equalsIgnoreCase("intercity") )
            routeTypeIndex = 2;
        else if ( routeType.equalsIgnoreCase("intracity") )
            routeTypeIndex = 3;
        
        
        SavedRouteInfo savedInfo = null;
        
        // routeBoardings is a hashMap containg a double[] of boardings walk/drive for each routeType:
        // wAir/dAir  wHsr/dHsr  wIc/dIc  wt/dt
        int index = -1;
        if ( routeTypeIndex >= 0 && accessIndex >= 0 ) {
            
            for (int rte=0; rte < tr.getLineCount(); rte++) {
                String rteName = tr.getLine(rte);
                
                if ( routeBoardings.containsKey(rteName) )
                    savedInfo = (SavedRouteInfo)routeBoardings.get(rteName);
                else
                    savedInfo = new SavedRouteInfo(tr.getDescription(rte), tr.getMode(rte));
                
                index = 2*routeTypeIndex + accessIndex;
                savedInfo.boardings[index] += transitBoardings[rte];
                
                routeBoardings.put(rteName, savedInfo);
            }

        }
        else {
            logger.error ("error trying to save boardings - invalid routeType = " + routeType + " or accessMode = " + accessMode );
            System.exit(-1);
        }
        
    }

    
   
    public void logTransitBoardingsReport ( HashMap savedBoardings, String periodHeadingLabel ) {
        
        Set routeNames = null;
        Iterator it = null; 
            
            
        logger.info ("");
        logger.info ("");
        logger.info ("Transit Network Loading Report for " + periodHeadingLabel + " period Trips");
        logger.info ("");

        
        
        // construct a format string for the description field from the longest route description
        int maxStringLength = 0;
        routeNames = savedBoardings.keySet();
        it = routeNames.iterator();
        while ( it.hasNext() ) {
            String name = (String)it.next();
            SavedRouteInfo info = (SavedRouteInfo)savedBoardings.get(name);
            if ( info.description.length() > maxStringLength )
                maxStringLength = info.description.length();
        }
        String descrFormat = "%-" + (maxStringLength+4) + "s";


        logger.info( String.format("%-6s %-9s " + descrFormat + " %-6c %8s %8s    %8s %8s    %8s %8s    %8s %8s    %8s %8s    %8s", "Count", "Route", "Description", "Mode", "wAir", "dAir", "wHsr", "dHsr", "wIc", "dIc", "wt", "dt", "wTot", "dTot", "Total") );
        
        int lineCount = 0;
        double[] totals = new double[11];
        it = routeNames.iterator();
        while ( it.hasNext() ) {
            String name = (String)it.next();
            SavedRouteInfo info = (SavedRouteInfo)savedBoardings.get(name);
            double wTot = info.boardings[0] + info.boardings[2] + info.boardings[4] + info.boardings[6];
            double dTot = info.boardings[1] + info.boardings[3] + info.boardings[5] + info.boardings[7];
            logger.info( String.format("%-6d %-9s " + descrFormat + " %-6c %8s %8.2f    %8.2f %8.2f    %8.2f %8.2f    %8.2f %8.2f    %8.2f %8.2f    %8.2f", ++lineCount, name, info.description, info.mode, "N/A", info.boardings[1], info.boardings[2], info.boardings[3], info.boardings[4], info.boardings[5], info.boardings[6], info.boardings[7], wTot, dTot, (wTot+dTot)) );
            for (int i=0; i < 8; i++)
                totals[i] += info.boardings[i];
            totals[8] += wTot;
            totals[9] += dTot;
            totals[10] += (wTot + dTot);
        }
        
        logger.info ( String.format("%-16s " + descrFormat + " %7s %8.2f %8.2f    %8.2f %8.2f    %8.2f %8.2f    %8.2f %8.2f    %8.2f %8.2f    %8.2f", "Total Boardings", "", "", totals[0], totals[1], totals[2], totals[3], totals[4], totals[5], totals[6], totals[7], totals[8], totals[9], totals[10]));
        
    }
    
    
    
    public void assignAndSkimTransit ( NetworkHandlerIF nh, ResourceBundle appRb, ResourceBundle globalRb ) {

        String assignmentPeriod = nh.getTimePeriod();
        
        double[] transitBoardings = null;
        
        ArrayList tripModeList = null;
        HashMap savedBoardings = new HashMap();
        
        
        // generate transit skim matrices and load trips
        TransitSkimManager tsm = new TransitSkimManager( nh, appRb, globalRb );

/*        
        // drive access air skims
        tsm.setupTransitNetwork( nh, assignmentPeriod, "drive", "air" );
        tsm.writeTransitSkims( assignmentPeriod, "drive", "air" );

        // drive access air assignment
        tripModeList = new ArrayList();
        tripModeList.add( LDTripModeType.AIR.name() );
        transitBoardings = runTransitAssignment ( nh, "drive", "air", tripModeList );
        saveTransitBoardings ( nh, "drive", "air", transitBoardings, savedBoardings );

        
        
        // drive access hsr skims
        tsm.setupTransitNetwork( nh, assignmentPeriod, "drive", "hsr" );
        tsm.writeTransitSkims( assignmentPeriod, "drive", "hsr" );

        // drive access hsr assignment
        tripModeList = new ArrayList();
        tripModeList.add( LDTripModeType.HSR_DRIVE.name() );
        transitBoardings = runTransitAssignment ( nh, "drive", "hsr", tripModeList );
        saveTransitBoardings ( nh, "drive", "hsr", transitBoardings, savedBoardings );

        
        
        // drive access intercity skims
        tsm.setupTransitNetwork( nh, assignmentPeriod, "drive", "intercity" );
        tsm.writeTransitSkims( assignmentPeriod, "drive", "intercity" );

        // drive access intercity assignment
        tripModeList = new ArrayList();
        tripModeList.add( LDTripModeType.TRANSIT_DRIVE.name() );
        transitBoardings = runTransitAssignment ( nh, "drive", "intercity", tripModeList );
        saveTransitBoardings ( nh, "drive", "intercity", transitBoardings, savedBoardings );
*/        
        
        
        // drive access intracity skims
        tsm.setupTransitNetwork( nh, assignmentPeriod, "drive", "intracity" );
        tsm.writeTransitSkims( assignmentPeriod, "drive", "intracity" );

        // drive access intracity assignment
        tripModeList = new ArrayList();
        tripModeList.add( TripModeType.DR_TRAN.name() );
        transitBoardings = runTransitAssignment ( nh, "drive", "intracity", tripModeList );
        saveTransitBoardings ( nh, "drive", "intracity", transitBoardings, savedBoardings );
        
        
        
        // walk access intracity skims - these are used as walk access skims for hsr, and intercity as well
        tsm.setupTransitNetwork( nh, assignmentPeriod, "walk", "intracity" );
        tsm.writeTransitSkims( assignmentPeriod, "walk", "intracity" );
        
        // load walk access transit trips - (all route types use the same network object; no walk access air)
        tripModeList = new ArrayList();
        tripModeList.add( LDTripModeType.HSR_WALK.name() );
        transitBoardings = runTransitAssignment ( nh, "walk", "hsr", tripModeList );
        saveTransitBoardings ( nh, "walk", "hsr", transitBoardings, savedBoardings );

        tripModeList = new ArrayList();
        tripModeList.add( LDTripModeType.TRANSIT_WALK.name() );
        transitBoardings = runTransitAssignment ( nh, "walk", "intercity", tripModeList );
        saveTransitBoardings ( nh, "walk", "intercity", transitBoardings, savedBoardings );

        tripModeList = new ArrayList();
        tripModeList.add( TripModeType.WK_TRAN.name() );
        transitBoardings = runTransitAssignment ( nh, "walk", "intracity", tripModeList );
        saveTransitBoardings ( nh, "walk", "intracity", transitBoardings, savedBoardings );

        
        
        logTransitBoardingsReport ( savedBoardings, assignmentPeriod );
        
        
        logger.info ("done with " + assignmentPeriod + " period transit skimming and loading.");
        
    }

    
    
    public void loadAssignmentResults ( NetworkHandlerIF nh, ResourceBundle appRb ) {
        
        // get the filename where highway assignment total link flows and times are stored from the property file. 
        HashMap propertyMap = ResourceUtil.changeResourceBundleIntoHashMap( appRb );
        
        String fileNameKey = "";
        String assignmentPeriod = nh.getTimePeriod();
        if ( assignmentPeriod.equalsIgnoreCase("peak") )
            fileNameKey = "peakOutput.fileName";
        else
            fileNameKey = "offpeakOutput.fileName";
                
        String fileName = (String)propertyMap.get( fileNameKey );
        
        
        
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


        // update the multiclass flow fields and congested time field in NetworkHandler
        double[][] flows = new double[numFlowFields][];
        for (int i=0; i < numFlowFields; i++)
            flows[i] = assignmentResults.getColumnAsDouble( (String)flowColumnNames.get(i) );
            
        nh.setFlows(flows);
        
        
        double[] timau = assignmentResults.getColumnAsDouble( nh.getAssignmentResultsTimeString() );
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
        if ( timePeriod.equalsIgnoreCase( "peak" ) )
            volumeFactor = (String)globalMap.get("AM_PEAK_VOL_FACTOR");
        else if ( timePeriod.equalsIgnoreCase( "offpeak" ) )
            volumeFactor = (String)globalMap.get("OFF_PEAK_VOL_FACTOR");
        else {
            logger.error ( "time period specifed as: " + timePeriod + ", but must be either 'peak' or 'offpeak'." );
            System.exit(-1);
        }
        
        String userClassesString = (String)appMap.get("userClass.modes");
        String truckClass1String = (String)appMap.get( "truckClass1.modes" );
        String truckClass2String = (String)appMap.get( "truckClass2.modes" );
        String truckClass3String = (String)appMap.get( "truckClass3.modes" );
        String truckClass4String = (String)appMap.get( "truckClass4.modes" );
        String truckClass5String = (String)appMap.get( "truckClass5.modes" );

        String walkSpeed = (String)globalMap.get( "WALK_MPH" );
        
        
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
        
        String period = "peak";

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

    
    
    
    
    public class SavedRouteInfo {
        
        static final int NUM_ROUTE_TYPES = 4;
        
        String description;
        char mode;
        double[] boardings;
        
        private SavedRouteInfo(String description, char mode) {
            this.description = description;
            this.mode = mode;
            boardings = new double[2*NUM_ROUTE_TYPES];
        }
        
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
        tsMain.setupNetwork( nhPeak, ResourceUtil.getResourceBundleAsHashMap(args[0]), ResourceUtil.getResourceBundleAsHashMap(args[1]), "peak" );
        
        nhPeak.checkForIsolatedLinks();

        tsMain.loadAssignmentResults ( nhPeak, ResourceBundle.getBundle(args[0]));
*/        

        


/*        
        // TS Example 1 - Run a peak highway assignment:
        
        // run peak highway assignment
        NetworkHandlerIF nhPeak = NetworkHandler.getInstance( rpcConfigFileName );
        nhPeak.setRpcConfigFileName( rpcConfigFileName );
        tsMain.setupNetwork( nhPeak, ResourceUtil.getResourceBundleAsHashMap(args[0]), ResourceUtil.getResourceBundleAsHashMap(args[1]), "peak" );
        nhPeak.checkForIsolatedLinks();
        tsMain.multiclassEquilibriumHighwayAssignment( nhPeak, "peak" );

        // write the auto time and distance highway skim matrices to disk based on attribute values in NetworkHandler after assignment
        tsMain.writeHighwaySkimMatrices ( nhPeak, 'a' );
*/

        
        
        
       
        // TS Example 2 - Read peak highway assignment results into NetworkHandler, then load and skim transit network
        NetworkHandlerIF nhPeak = NetworkHandler.getInstance( rpcConfigFileName );
        nhPeak.setRpcConfigFileName( rpcConfigFileName );
        tsMain.setupHighwayNetwork( nhPeak, ResourceUtil.getResourceBundleAsHashMap(args[0]), ResourceUtil.getResourceBundleAsHashMap(args[1]), "peak" );
        //tsMain.multiclassEquilibriumHighwayAssignment( nhPeak, "peak" );
        tsMain.loadAssignmentResults ( nhPeak, ResourceBundle.getBundle(args[0]) );
        nhPeak.startDataServer();
        logger.info ("Network data server running...");
        
        tsMain.assignAndSkimTransit ( nhPeak, ResourceBundle.getBundle(args[0]), ResourceBundle.getBundle(args[1]) );

        
        // test capability to get list of highway network link ids from a transit route
        String name = "B_1334";
        int[] linkIds = nhPeak.getTransitRouteLinkIds( name );
        String linksString = String.format("%d", linkIds[0]);
        for (int i=1; i < linkIds.length; i++)
            linksString += String.format(", %d", linkIds[i]);
        logger.info ( String.format( "link ids for route %s : %s", name, linksString) );

        
        // run the benchmark highway assignment procedure
//        tsMain.bench ( tsMain.appRb, tsMain.globalRb, rpcConfigFileName );
        

        // run the benchmark highway assignment procedure
//        ApplicationOrchestrator ao = new ApplicationOrchestrator(null);
//        ao.runTSModel(tsMain.appRb, tsMain.globalRb);
        

        logger.info ("TS.main() finished in " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
        nhPeak.stopDataServer();
        logger.info ("Network data server stopped.");
        logger.info ("TS.main() exiting.");

    }

}

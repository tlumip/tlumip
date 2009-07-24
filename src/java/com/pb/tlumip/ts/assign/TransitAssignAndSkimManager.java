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
 * @version   1.0, 7/1/2004
 */


import com.pb.models.pt.TripModeType;
import com.pb.models.pt.ldt.LDTripModeType;
import com.pb.tlumip.ts.DemandHandler;
import com.pb.tlumip.ts.DemandHandlerIF;
import com.pb.tlumip.ts.NetworkHandlerIF;
import com.pb.tlumip.ts.transit.AuxTrNet;
import com.pb.tlumip.ts.transit.OptimalStrategy;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;


public class TransitAssignAndSkimManager {

    private Logger logger = Logger.getLogger(TransitAssignAndSkimManager.class);
    private Logger skimLogger = Logger.getLogger( "skimLogger" );

    static final boolean CREATE_NEW_NETWORK = true;
    boolean SKIM_ONLY = false;

    // make TEST_ORIG and TEST_DEST a negative number to skip debugging an od.
    public static final int TEST_ORIG = -1;
    public static final int TEST_DEST = -1;

    // make TEST_ORIG and TEST_DEST a positive number to trigger debugging the strategy for the od.
    //public static final int TEST_ORIG = 1;
    //public static final int TEST_DEST = 18;
	
    static final String OUT_OF_AREA_FARE_ZONE = "NONE"; 
    
    
    protected static Object objLock = new Object();
    //private static final int MAX_NUMBER_OF_THREADS = 1;
    private static final int MAX_NUMBER_OF_THREADS = 2;
    
    DemandHandlerIF dh = null;
    
    ResourceBundle appRb = null;
    ResourceBundle globalRb = null;
    
    String skimFileExtension = null;
    String skimFileDirectory = null;
    
    String transitNetworkListings = null;
    String transitRoutesDirectory = null;
    int maxRoutes = 0;

    String assignmentPeriod;
    
    HashMap<Integer,String> fareZones = null;
    HashMap<String,Float> intracityFareTable = null;
    
    int[] zonesToSkim = null;
    int[] externalToAlphaInternal = null;
    int[] alphaExternalNumbers = null;

    HashMap<String,SavedRouteInfo> savedBoardings = null;


    
	
    public TransitAssignAndSkimManager(NetworkHandlerIF nh, ResourceBundle appRb, ResourceBundle globalRb) {

        this.appRb = appRb;
        this.globalRb = globalRb;
        
        try {
            transitNetworkListings = appRb.getString( "transitNetworkListings.directory" );
        }
        catch ( MissingResourceException e ) {
            transitNetworkListings = null;
        }
        
        transitRoutesDirectory = appRb.getString( "transitRoutes.directory" );
        
        maxRoutes = Integer.parseInt ( appRb.getString( "MAX_TRANSIT_ROUTES" ) );

        skimFileExtension = globalRb.getString( "matrix.extension" );
        skimFileDirectory = appRb.getString( "transitSkims.directory" );

        intracityFareTable = readIntracityTransitFareCsvFile();
        
        savedBoardings = new HashMap<String,SavedRouteInfo>();
        
        assignmentPeriod = nh.getTimePeriod();
        
        // create demand handler to get trips to assign if SKIM_ONLY == false
        if ( ! SKIM_ONLY ) {
            String rpcConfigFile = nh.getRpcConfigFileName();
            setupDemandObject ( rpcConfigFile, assignmentPeriod, nh );
        }
            
        initSkimMatrices ( nh, globalRb.getString( "alpha2beta.file" ) );
        
    }    
    
    
    private void setupDemandObject ( String rpcConfigFile, String timePeriod, NetworkHandlerIF nh ) {
        
        double ptSampleRate = 1.0;
        String rateString = globalRb.getString( "pt.sample.rate" );
        if ( rateString != null )
            ptSampleRate = Double.parseDouble( rateString );

        
        int startHour = 0;
        int endHour = 0;
        if ( timePeriod.equalsIgnoreCase( "ampeak" ) ) {
            // get am peak period definitions from property files
            startHour = Integer.parseInt( globalRb.getString( "am.peak.start") );
            endHour = Integer.parseInt( globalRb.getString( "am.peak.end" ) );
        }
        else if ( timePeriod.equalsIgnoreCase( "pmpeak" ) ) {
            // get pm peak period definitions from property files
            startHour = Integer.parseInt( globalRb.getString( "pm.peak.start") );
            endHour = Integer.parseInt( globalRb.getString( "pm.peak.end" ) );
        }
        else if ( timePeriod.equalsIgnoreCase( "mdoffpeak" ) ) {
            // get md off-peak period definitions from property files
            startHour = Integer.parseInt( globalRb.getString( "md.offpeak.start") );
            endHour = Integer.parseInt( globalRb.getString( "md.offpeak.end" ) );
        }
        else if ( timePeriod.equalsIgnoreCase( "ntoffpeak" ) ) {
            // get nt off-peak period definitions from property files
            startHour = Integer.parseInt( globalRb.getString( "nt.offpeak.start") );
            endHour = Integer.parseInt( globalRb.getString( "nt.offpeak.end" ) );
        }
        
        String sdtFileName = globalRb.getString("sdt.person.trips");
        String ldtFileName = globalRb.getString("ldt.vehicle.trips");
        String ctFileName = globalRb.getString("ct.truck.trips");
        String etFileName = globalRb.getString("et.truck.trips");
        
        
        dh = DemandHandler.getInstance( rpcConfigFile );
        dh.setup( nh.getUserClassPces(), sdtFileName, ldtFileName, ptSampleRate, ctFileName, etFileName, startHour, endHour, timePeriod, nh.getNumCentroids(), nh.getNumUserClasses(), nh.getNodeIndex(), nh.getAlphaDistrictIndex(), nh.getDistrictNames(), nh.getAssignmentGroupChars(), nh.getHighwayModeCharacters(), nh.userClassesIncludeTruck() );
        
    }

    
    public void assignAndSkimTransit ( NetworkHandlerIF nh, String period ) {
        assignAndSkimTransit ( nh, period, false );
    }

    
    public void assignAndSkimTransit ( NetworkHandlerIF nh, String period, boolean skimOnlyFlag ) {
        
        SKIM_ONLY = skimOnlyFlag;
        
        
        int numberOfThreads = java.lang.Runtime.getRuntime().availableProcessors();
        if ( numberOfThreads > MAX_NUMBER_OF_THREADS )
            numberOfThreads = MAX_NUMBER_OF_THREADS;
        
        ExecutorService exec = Executors.newFixedThreadPool(numberOfThreads);
        ArrayList<Future<String>> results = new ArrayList<Future<String>>();



        
        
        // check for existence of a route file targets in the properties file for each service type, access mode, period.
        // if resource is missing, then no need to create an AssignSkimTask -- no assignment or skimming is required for the service type.
        
        
        String driveAccessIdentifier = "";
        try {
            driveAccessIdentifier = appRb.getString("drive.identifier");        
        }
        catch ( MissingResourceException e ) {
        }

        String walkAccessIdentifier = "";
        try {
            walkAccessIdentifier = appRb.getString("walk.identifier");        
        }
        catch ( MissingResourceException e ) {
        }

        
        
        String intracityTransitIdentifier = "";
        try {
            intracityTransitIdentifier = appRb.getString("intracityTransit.identifier");        
        }
        catch ( MissingResourceException e ) {
        }

        String intercityTransitIdentifier = "";
        try {
            intercityTransitIdentifier = appRb.getString("intercityTransit.identifier");        
        }
        catch ( MissingResourceException e ) {
        }

        String hsrTransitTransitIdentifier = "";
        try {
            hsrTransitTransitIdentifier = appRb.getString("hsrTransit.identifier");        
        }
        catch ( MissingResourceException e ) {
        }

        String airTransitIdentifier = "";
        try {
            airTransitIdentifier = appRb.getString("airTransit.identifier");        
        }
        catch ( MissingResourceException e ) {
        }

        

        // drive access air loading and skims
        String[] drAirTypesIn = { "air" }; 
        String[] drAirTypes = getServiceTypeRouteFilesSpecified( drAirTypesIn, period );
        if ( drAirTypes.length > 0 && driveAccessIdentifier.equalsIgnoreCase("d") && airTransitIdentifier.equalsIgnoreCase("air") )
            results.add ( exec.submit( new AssignSkimTask( nh, drAirTypes, period, "driveLdt", "drive", "air", LDTripModeType.AIR.name() ) ) );
        
        // drive access hsr loading and skims
        String[] drHsrTypesIn = { "hsr", "intercity" }; 
        String[] drHsrTypes = getServiceTypeRouteFilesSpecified( drHsrTypesIn, period );
        if ( drHsrTypes.length > 0 && driveAccessIdentifier.equalsIgnoreCase("d") && hsrTransitTransitIdentifier.equalsIgnoreCase("hsr") )
            results.add ( exec.submit( new AssignSkimTask( nh, drHsrTypes, period, "driveLdt", "drive", "hsr", LDTripModeType.HSR_DRIVE.name() ) ) );
        
        // drive access intercity bus/rail loading and skims
        String[] drIcTypesIn = { "intercity" }; 
        String[] drIcTypes = getServiceTypeRouteFilesSpecified( drIcTypesIn, period );
        if ( drIcTypes.length > 0 && driveAccessIdentifier.equalsIgnoreCase("d") && intercityTransitIdentifier.equalsIgnoreCase("ic") )
            results.add ( exec.submit( new AssignSkimTask( nh, drIcTypes, period, "driveLdt", "drive", "intercity", LDTripModeType.TRANSIT_DRIVE.name() ) ) );

        // drive access intracity transit loading and skims
        String[] drTrTypesIn = { "intracity" }; 
        String[] drTrTypes = getServiceTypeRouteFilesSpecified( drTrTypesIn, period );
        if ( drTrTypes.length > 0 && driveAccessIdentifier.equalsIgnoreCase("d") && intracityTransitIdentifier.equalsIgnoreCase("t") )
            results.add ( exec.submit( new AssignSkimTask( nh, drTrTypes, period, "drive", "drive", "intracity", TripModeType.DR_TRAN.name() ) ) );

        // walk access hsr loading and skims
        String[] wkHsrTypesIn = { "hsr", "intercity", "intracity" }; 
        String[] wkHsrTypes = getServiceTypeRouteFilesSpecified( wkHsrTypesIn, period );
        if ( wkHsrTypes.length > 0 && walkAccessIdentifier.equalsIgnoreCase("w") && hsrTransitTransitIdentifier.equalsIgnoreCase("hsr") )
            results.add ( exec.submit( new AssignSkimTask( nh, wkHsrTypes, period, "walk", "walk", "hsr", LDTripModeType.HSR_WALK.name() ) ) );

        // walk access intercity loading and skims
        String[] wkIcTypesIn = { "intercity", "intracity" }; 
        String[] wkIcTypes = getServiceTypeRouteFilesSpecified( wkIcTypesIn, period );
        if ( wkIcTypes.length > 0 && walkAccessIdentifier.equalsIgnoreCase("w") && intercityTransitIdentifier.equalsIgnoreCase("ic") )
            results.add ( exec.submit( new AssignSkimTask( nh, wkIcTypes, period, "walk", "walk", "intercity", LDTripModeType.TRANSIT_WALK.name() ) ) );

        // walk access intracity loading and skims
        String[] wkTrTypesIn = { "intracity" }; 
        String[] wkTrTypes = getServiceTypeRouteFilesSpecified( wkTrTypesIn, period );
        if ( wkTrTypes.length > 0 && walkAccessIdentifier.equalsIgnoreCase("w") && intracityTransitIdentifier.equalsIgnoreCase("t") )
            results.add ( exec.submit( new AssignSkimTask( nh, wkTrTypes, period, "walk", "walk", "intracity", TripModeType.WK_TRAN.name() ) ) );

        
        
        
/*        
        // walk access hsr loading and skims
        String[] wkTrTypesIn = { "intracity" }; 
        String[] wkTrTypes = getServiceTypeRouteFilesSpecified( wkTrTypesIn, period );
        results.add ( exec.submit( new AssignSkimTask( nh, wkTrTypes, period, "walk", "walk", "intracity", TripModeType.WK_TRAN.name() ) ) );

        String[] drTrTypesIn = { "intracity" }; 
        String[] drTrTypes = getServiceTypeRouteFilesSpecified( drTrTypesIn, period );
        results.add ( exec.submit( new AssignSkimTask( nh, drTrTypes, period, "drive", "drive", "intracity", TripModeType.DR_TRAN.name() ) ) );
*/        
        
        
        
        
        for ( Future<String> fs : results ) {
            
            try {
                String result = fs.get();
                logger.info( String.format( "%s task finished.", result ) );
            }
            catch (InterruptedException e) {
                logger.error( "", e );
            }
            catch (ExecutionException e) {
                logger.error( "", e );
            }
            finally {
                exec.shutdown();
            }
            
        }
        
        
        
        String csvFileName = null;
        String csvFileTarget = String.format("%s.TransitLoadings.fileName", period);
        try {
            csvFileName = appRb.getString( csvFileTarget );
        }
        catch ( MissingResourceException e ){
            // do nothing, filename can be null.
        }
        
        String rptFileName = null;
        String rptFileTarget = String.format("%s.TransitReport.fileName", period);
        try {
            rptFileName = appRb.getString( rptFileTarget );
        }
        catch ( MissingResourceException e ){
            // do nothing, filename can be null.
        }
        
        logTransitBoardingsReport ( csvFileName, rptFileName, period );
        
    }


    
    // check the service type strings for the period specified for their specification in the proprties file.
    // if the target name is defined in the properties file, include that service type in the return array.
    private String[] getServiceTypeRouteFilesSpecified( String[] types, String period ) {

        ArrayList<String> typeList = new ArrayList<String>();
        
        for ( int i=0; i < types.length; i++ ) {
            try {
                // if the target for the service type is specified in the property file, add the service type name to the list.
                // don't need to do anything with the filename at this point, just saving the value in case we need to debug this and see aht is returned.
                String fileName = appRb.getString( String.format("%s.%s.fileName", types[i], getPeriodIdentifier(period)) );
                typeList.add( types[i] );
            }
            catch ( MissingResourceException e ){
                // if the first element in the types array, the primary service type, is not defined, then
                // don't bother checking any other secondary types -- the service is not to be assigned.
                if ( i == 0 )
                    return new String[0];
            }
        }
        
        String[] returnArray = new String[typeList.size()];
        for ( int i=0; i < returnArray.length; i++ )
            returnArray[i] = typeList.get(i);

        return returnArray;
        
    }
    
    /**
     * setup transit network from route files specified:
     * 
     * Examples:
     *      "ampeak", "walk", ["intracity"]
     *      "mdoffpeak", "drive", ["intracity"]
     *      "mdoffpeak", "driveLDT", ["air"]
     *      "mdoffpeak", "driveLDT", ["hsr", "intercity"]
     *      "ampeak", "walk", ["intercity", "hsr", "intracity"]
     * @param nh
     * @param period
     * @param accessMode
     * @param rteTypes
     * @return
     */
    private int setupTransitNetwork ( String identifier, NetworkHandlerIF nh, String period, String accessMode, String[] rteTypes ) {
        
        // construct a network listing output filename for the transit network being setup - assume primary route type is the first listed
        String listingName = null;
        try {
            listingName = appRb.getString( "transitNetworkListings.directory" ) + period + "_" + accessMode + "_" + rteTypes[0] + ".listing";
        }
        catch ( MissingResourceException e ) {
        }
        
        
        // transit network setup method expects arrays of filenames and types:
        String[] d221Files = new String[rteTypes.length];

        
        // check route files
        for (int i=0; i < rteTypes.length; i++) {
            
            // check for valid specification and combination of arguments
            checkArguments( period, accessMode, rteTypes[i] );
            
            // construct a target for the route file (e.g. "air.pk.fileName")
            String d221Target = String.format("%s.%s.fileName", rteTypes[i], ( period.equalsIgnoreCase("ampeak") || period.equalsIgnoreCase("pmpeak") ? "pk" : "op" ) );
            
            // get the file name for the target from the propoerties file
            d221Files[i] = transitRoutesDirectory + appRb.getString( d221Target );
            
            // check existence of route file
            checkRouteFile( period, accessMode, rteTypes[i], d221Files[i]);

        }

        return nh.setupTransitNetworkObject ( identifier, period, accessMode, listingName, transitNetworkListings, d221Files, rteTypes, maxRoutes );
        
    }

    

    private HashMap<String,Float> readIntracityTransitFareCsvFile () {

        final String OrigDistLabel = "OFareDistrict";
        final String DestDistLabel = "DFareDistrict";
        final String Fare1990Label = "Fare_1990$";

        HashMap<String,Float> fareTable = null;

        String filename = appRb.getString( "fareZoneFares.file" );
        
        // read the extra link attributes file and update link attributes table values.
        if ( filename != null && ! filename.equals("") ) {

            try {
                
                OLD_CSVFileReader reader = new OLD_CSVFileReader();
                TableDataSet table = reader.readFile( new File(filename) );

                fareTable = new HashMap<String,Float>();
                
                for (int i=0; i < table.getRowCount(); i++) {
                    
                    String oDist = table.getStringValueAt( i+1, OrigDistLabel );
                    String dDist = table.getStringValueAt( i+1, DestDistLabel );
                    float fare = (float)table.getValueAt( i+1, Fare1990Label );
                    
                    String key = String.format( "%s_%s", oDist, dDist );
                    fareTable.put( key, fare );
                }

            }
            catch (IOException e) {
                logger.error ( "exception caught intracity transit fare district fares file: " + filename );
                throw new RuntimeException(e);
            }
                        
        }
        
        return fareTable;

    }



    /**
     * write a set of zip format drive access air skim matrix files for the period specified
     */
    private void writeDriveAirSkims ( Matrix[] skimMatrices, String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        
        // if period is "nul", no period identifier associated with period, so don't need skims files to be written
        if ( periodIdentifier.equalsIgnoreCase( "nul" ) ) {
            logger.info ( String.format( "no drive air skims files were written for %s period.", period ) );
            return;
        }
        
        
        String accessIdentifier = getAccessIdentifier("drive");
        String routeTypeIdentifier = getRouteTypeIdentifier("air");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "ivt" + skimFileExtension;
        String fwtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "fwt" + skimFileExtension;
        String drvFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "drv" + skimFileExtension;
        String farFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "far" + skimFileExtension;
        
        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );
        skimLogger.info( ivtFilename );
        skimMatrices[SkimType.IVT.ordinal()].logMatrixStatsToInfo( skimLogger );
        
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );
        skimLogger.info( fwtFilename );
        skimMatrices[SkimType.FWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AIR$.ordinal()] );
        skimLogger.info( farFilename );
        skimMatrices[SkimType.AIR$.ordinal()].logMatrixStatsToInfo( skimLogger );

        Matrix m = skimMatrices[SkimType.ACC.ordinal()].add(skimMatrices[SkimType.EGR.ordinal()] );   // for drive air, drv is acc + egr drive time
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(drvFilename) );
        mw.writeMatrix( m );
        skimLogger.info( drvFilename );
        m.logMatrixStatsToInfo( skimLogger );

        logger.info ("done writing " + period + " drive air skims files.");
            
    }

    
    /**
     * write a set of zip format drive access high speed rail skim matrix files for the period specified
     */
    private void writeDriveHsrSkims ( Matrix[] skimMatrices, String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        
        // if period is "nul", no period identifier associated with period, so don't need skims files to be written
        if ( periodIdentifier.equalsIgnoreCase( "nul" ) ) {
            logger.info ( String.format( "no drive high speed rail skims files were written for %s period.", period ) );
            return;
        }
        
        
        String accessIdentifier = getAccessIdentifier("drive");
        String routeTypeIdentifier = getRouteTypeIdentifier("hsr");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "ivt" + skimFileExtension;
        String fwtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "fwt" + skimFileExtension;
        String twtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "twt" + skimFileExtension;
        String xwkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "xwk" + skimFileExtension;
        String drvFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "drv" + skimFileExtension;
        String farFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "far" + skimFileExtension;
        String hsrBrdFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "hsr_brd" + skimFileExtension;
        String hsrIvtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "hsr_ivt" + skimFileExtension;

        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );
        skimLogger.info( ivtFilename );
        skimMatrices[SkimType.IVT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );
        skimLogger.info( fwtFilename );
        skimMatrices[SkimType.FWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(twtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TWT.ordinal()] );
        skimLogger.info( twtFilename );
        skimMatrices[SkimType.TWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(xwkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AUX.ordinal()] );
        skimLogger.info( xwkFilename );
        skimMatrices[SkimType.AUX.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(hsrIvtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.HSR_IVT.ordinal()] );
        skimLogger.info( hsrIvtFilename );
        skimMatrices[SkimType.HSR_IVT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(hsrBrdFilename) );
        mw.writeMatrix( skimMatrices[SkimType.BRD_HSR.ordinal()] );
        skimLogger.info( hsrBrdFilename );
        skimMatrices[SkimType.BRD_HSR.ordinal()].logMatrixStatsToInfo( skimLogger );

        Matrix m = skimMatrices[SkimType.HSR$.ordinal()].add( skimMatrices[SkimType.BUS$.ordinal()].add( skimMatrices[SkimType.RAIL$.ordinal()] ) );   // for drive hsr, far combines hsr$, bus$, and rail$
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( m );
        skimLogger.info( farFilename );
        m.logMatrixStatsToInfo( skimLogger );

        m = skimMatrices[SkimType.ACC.ordinal()].add( skimMatrices[SkimType.EGR.ordinal()] );   // for drive hsr, drv is acc + egr drive time
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(drvFilename) );
        mw.writeMatrix( m );
        skimLogger.info( drvFilename );
        m.logMatrixStatsToInfo( skimLogger );

        logger.info ("done writing " + period + " drive high speed rail skims files.");
            
    }

    
    /**
     * write a set of zip format drive access intercity transit skim matrix files for the period specified
     */
    private void writeDriveIntercitySkims ( Matrix[] skimMatrices, String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        
        // if period is "nul", no period identifier associated with period, so don't need skims files to be written
        if ( periodIdentifier.equalsIgnoreCase( "nul" ) ) {
            logger.info ( String.format( "no drive intercity bus/rail skims files were written for %s period.", period ) );
            return;
        }
        
        
        String accessIdentifier = getAccessIdentifier("drive");
        String routeTypeIdentifier = getRouteTypeIdentifier("intercity");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "ivt" + skimFileExtension;
        String fwtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "fwt" + skimFileExtension;
        String twtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "twt" + skimFileExtension;
        String xwkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "xwk" + skimFileExtension;
        String drvFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "drv" + skimFileExtension;
        String farFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "far" + skimFileExtension;
        String busIvtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "bus_ivt" + skimFileExtension;
        String railIvtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "rail_ivt" + skimFileExtension;
        
        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );
        skimLogger.info( farFilename );
        skimMatrices[SkimType.IVT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );
        skimLogger.info( fwtFilename );
        skimMatrices[SkimType.FWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(twtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TWT.ordinal()] );
        skimLogger.info( twtFilename );
        skimMatrices[SkimType.TWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(xwkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AUX.ordinal()] );
        skimLogger.info( xwkFilename );
        skimMatrices[SkimType.AUX.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(busIvtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.BUS_IVT.ordinal()] );
        skimLogger.info( busIvtFilename );
        skimMatrices[SkimType.BUS_IVT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(railIvtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.RAIL_IVT.ordinal()] );
        skimLogger.info( railIvtFilename );
        skimMatrices[SkimType.RAIL_IVT.ordinal()].logMatrixStatsToInfo( skimLogger );

        Matrix m = skimMatrices[SkimType.BUS$.ordinal()].add( skimMatrices[SkimType.RAIL$.ordinal()] );   // for drive intercity, far combines bus$ and rail$
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( m );
        skimLogger.info( farFilename );
        m.logMatrixStatsToInfo( skimLogger );

        m = skimMatrices[SkimType.ACC.ordinal()].add( skimMatrices[SkimType.EGR.ordinal()] );   // for drive intercity, drv is acc + egr drive time
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(drvFilename) );
        mw.writeMatrix( m );
        skimLogger.info( drvFilename );
        m.logMatrixStatsToInfo( skimLogger );

        logger.info ("done writing " + period + " drive intercity bus/rail skims files.");
            
    }

    

    /**
     * write a set of zip format drive access intracity transit skim matrix files for the period specified
     */
    private void writeDriveIntracitySkims ( Matrix[] skimMatrices, String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        
        // if period is "nul", no period identifier associated with period, so don't need skims files to be written
        if ( periodIdentifier.equalsIgnoreCase( "nul" ) ) {
            logger.info ( String.format( "no drive intracity transit skims files were written for %s period.", period ) );
            return;
        }
        
        
        String accessIdentifier = getAccessIdentifier("drive");
        String routeTypeIdentifier = getRouteTypeIdentifier("intracity");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "ivt" + skimFileExtension;
        String fwtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "fwt" + skimFileExtension;
        String twtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "twt" + skimFileExtension;
        String drvFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "drv" + skimFileExtension;
        String xwkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "xwk" + skimFileExtension;
        String ewkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "ewk" + skimFileExtension;
        String brdFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "brd" + skimFileExtension;
        String farFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "far" + skimFileExtension;
        
        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );
        skimLogger.info( ivtFilename );
        skimMatrices[SkimType.IVT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );
        skimLogger.info( fwtFilename );
        skimMatrices[SkimType.FWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(twtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TWT.ordinal()] );
        skimLogger.info( twtFilename );
        skimMatrices[SkimType.TWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(drvFilename) );
        mw.writeMatrix( skimMatrices[SkimType.ACC.ordinal()] );
        skimLogger.info( drvFilename );
        skimMatrices[SkimType.ACC.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(xwkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AUX.ordinal()] );
        skimLogger.info( xwkFilename );
        skimMatrices[SkimType.AUX.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ewkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.EGR.ordinal()] );
        skimLogger.info( ewkFilename );
        skimMatrices[SkimType.EGR.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(brdFilename) );
        mw.writeMatrix( skimMatrices[SkimType.BRD.ordinal()] );
        skimLogger.info( brdFilename );
        skimMatrices[SkimType.BRD.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TRAN$.ordinal()] );
        skimLogger.info( farFilename );
        skimMatrices[SkimType.TRAN$.ordinal()].logMatrixStatsToInfo( skimLogger );

        logger.info ("done writing " + period + " drive intracity transit skims files.");
            
    }

    

    /**
     * write a set of zip format walk access high speed rail skim matrix files for the period specified
     */
    private void writeWalkHsrSkims ( Matrix[] skimMatrices, String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        
        // if period is "nul", no period identifier associated with period, so don't need skims files to be written
        if ( periodIdentifier.equalsIgnoreCase( "nul" ) ) {
            logger.info ( String.format( "no walk high speed rail skims files were written for %s period.", period ) );
            return;
        }
        
        
        String accessIdentifier = getAccessIdentifier("walk");
        String routeTypeIdentifier = getRouteTypeIdentifier("hsr");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "ivt" + skimFileExtension;
        String fwtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "fwt" + skimFileExtension;
        String twtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "twt" + skimFileExtension;
        String awkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "awk" + skimFileExtension;
        String xwkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "xwk" + skimFileExtension;
        String ewkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "ewk" + skimFileExtension;
        String farFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "far" + skimFileExtension;
        String hsrBrdFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "hsr_brd" + skimFileExtension;
        String hsrIvtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "hsr_ivt" + skimFileExtension;
        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );
        skimLogger.info( ivtFilename );
        skimMatrices[SkimType.IVT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );
        skimLogger.info( fwtFilename );
        skimMatrices[SkimType.FWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(twtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TWT.ordinal()] );
        skimLogger.info( twtFilename );
        skimMatrices[SkimType.TWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(awkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.ACC.ordinal()] );
        skimLogger.info( awkFilename );
        skimMatrices[SkimType.ACC.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(xwkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AUX.ordinal()] );
        skimLogger.info( xwkFilename );
        skimMatrices[SkimType.AUX.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ewkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.EGR.ordinal()] );
        skimLogger.info( ewkFilename );
        skimMatrices[SkimType.EGR.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(hsrIvtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.HSR_IVT.ordinal()] );
        skimLogger.info( hsrIvtFilename );
        skimMatrices[SkimType.HSR_IVT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(hsrBrdFilename) );
        mw.writeMatrix( skimMatrices[SkimType.BRD_HSR.ordinal()] );
        skimLogger.info( hsrBrdFilename );
        skimMatrices[SkimType.BRD_HSR.ordinal()].logMatrixStatsToInfo( skimLogger );

        Matrix m = skimMatrices[SkimType.HSR$.ordinal()].add( skimMatrices[SkimType.BUS$.ordinal()] ).add( skimMatrices[SkimType.RAIL$.ordinal()] ).add( skimMatrices[SkimType.TRAN$.ordinal()] );   // for walk hsr, far combines hsr$, bus$, $rail and tran$
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( m );
        skimLogger.info( farFilename );
        m.logMatrixStatsToInfo( skimLogger );

        logger.info ("done writing " + period + " walk high speed rail skims files.");
            
    }

    

    /**
     * write a set of zip format walk access intercity bus/rail skim matrix files for the period specified
     */
    private void writeWalkIntercitySkims ( Matrix[] skimMatrices, String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        
        // if period is "nul", no period identifier associated with period, so don't need skims files to be written
        if ( periodIdentifier.equalsIgnoreCase( "nul" ) ) {
            logger.info ( String.format( "no walk intercity bus/rail skims files were written for %s period.", period ) );
            return;
        }
        
        
        String accessIdentifier = getAccessIdentifier("walk");
        String routeTypeIdentifier = getRouteTypeIdentifier("intercity");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "ivt" + skimFileExtension;
        String fwtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "fwt" + skimFileExtension;
        String twtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "twt" + skimFileExtension;
        String awkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "awk" + skimFileExtension;
        String xwkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "xwk" + skimFileExtension;
        String ewkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "ewk" + skimFileExtension;
        String brdFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "brd" + skimFileExtension;
        String farFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "far" + skimFileExtension;
        String busIvtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "bus_ivt" + skimFileExtension;
        String railIvtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "rail_ivt" + skimFileExtension;
        
        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );
        skimLogger.info( ivtFilename );
        skimMatrices[SkimType.IVT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );
        skimLogger.info( fwtFilename );
        skimMatrices[SkimType.FWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(twtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TWT.ordinal()] );
        skimLogger.info( twtFilename );
        skimMatrices[SkimType.TWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(awkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.ACC.ordinal()] );
        skimLogger.info( awkFilename );
        skimMatrices[SkimType.ACC.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(xwkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AUX.ordinal()] );
        skimLogger.info( xwkFilename );
        skimMatrices[SkimType.AUX.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ewkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.EGR.ordinal()] );
        skimLogger.info( ewkFilename );
        skimMatrices[SkimType.EGR.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(brdFilename) );
        mw.writeMatrix( skimMatrices[SkimType.BRD.ordinal()] );
        skimLogger.info( brdFilename );
        skimMatrices[SkimType.BRD.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(busIvtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.BUS_IVT.ordinal()] );
        skimLogger.info( busIvtFilename );
        skimMatrices[SkimType.BUS_IVT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(railIvtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.RAIL_IVT.ordinal()] );
        skimLogger.info( railIvtFilename );
        skimMatrices[SkimType.RAIL_IVT.ordinal()].logMatrixStatsToInfo( skimLogger );

        Matrix m = skimMatrices[SkimType.BUS$.ordinal()].add( skimMatrices[SkimType.RAIL$.ordinal()] ).add( skimMatrices[SkimType.TRAN$.ordinal()] );   // for walk intercity, far combines bus$, $rail and tran$
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( m );
        skimLogger.info( farFilename );
        m.logMatrixStatsToInfo( skimLogger );

        logger.info ("done writing " + period + " walk intercity bus/rail skims files.");
            
    }

    

    /**
     * write a set of zip format walk access intracity transit skim matrix files for the period specified
     */
    private void writeWalkIntracitySkims ( Matrix[] skimMatrices, String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        
        // if period is "nul", no period identifier associated with period, so don't need skims files to be written
        if ( periodIdentifier.equalsIgnoreCase( "nul" ) ) {
            logger.info ( String.format( "no walk intracity transit skims files were written for %s period.", period ) );
            return;
        }
        
        
        String accessIdentifier = getAccessIdentifier("walk");
        String routeTypeIdentifier = getRouteTypeIdentifier("intracity");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "ivt" + skimFileExtension;
        String fwtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "fwt" + skimFileExtension;
        String twtFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "twt" + skimFileExtension;
        String awkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "awk" + skimFileExtension;
        String xwkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "xwk" + skimFileExtension;
        String ewkFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "ewk" + skimFileExtension;
        String brdFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "brd" + skimFileExtension;
        String farFilename = skimFileDirectory + periodIdentifier + accessIdentifier + routeTypeIdentifier + "far" + skimFileExtension;
        
        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );
        skimLogger.info( ivtFilename );
        skimMatrices[SkimType.IVT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );
        skimLogger.info( fwtFilename );
        skimMatrices[SkimType.FWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(twtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TWT.ordinal()] );
        skimLogger.info( twtFilename );
        skimMatrices[SkimType.TWT.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(awkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.ACC.ordinal()] );
        skimLogger.info( awkFilename );
        skimMatrices[SkimType.ACC.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(xwkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AUX.ordinal()] );
        skimLogger.info( xwkFilename );
        skimMatrices[SkimType.AUX.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ewkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.EGR.ordinal()] );
        skimLogger.info( ewkFilename );
        skimMatrices[SkimType.EGR.ordinal()].logMatrixStatsToInfo( skimLogger );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(brdFilename) );
        mw.writeMatrix( skimMatrices[SkimType.BRD.ordinal()] );
        skimLogger.info( brdFilename );
        skimMatrices[SkimType.BRD.ordinal()].logMatrixStatsToInfo( skimLogger );

        Matrix m = skimMatrices[SkimType.TRAN$.ordinal()];   // for walk intracity, far is tran$
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( m );
        skimLogger.info( farFilename );
        m.logMatrixStatsToInfo( skimLogger );

        logger.info ("done writing " + period + " walk intracity transit skims files.");
            
    }

    

    
    
    private Matrix[] runTransitAssignment ( String identifier, NetworkHandlerIF nh, String timePeriod, String accessMode, String routeType, String tripMode ) {
        
        double[][] tripTable = null;  
        double[] tripTableColumn = null;
        
        int numCentroids = nh.getNumCentroids();
        
        // get trip table from demand handler for trips to assign if SKIM_ONLY == false
        if ( ! SKIM_ONLY ) {
            tripTable = dh.getTripTableForMode ( tripMode );

            tripTableColumn = new double[tripTable[0].length];
        }
        
        
                
        // load the triptable on walk access transit network
        // create an optimal strategy object for this highway and transit network
        OptimalStrategy os = new OptimalStrategy( nh, identifier );
        os.setTransitFareTables ( intracityFareTable, fareZones ); 

        // arrays for skim values into 0-based double[][] dimensioned to number of actual zones including externals (2983)
        double[][][] zeroBasedDoubleArray = new double[OptimalStrategy.NUM_SKIMS][numCentroids][numCentroids];
                
        double intrazonal = 0;
        double totalTrips = 0;
        double notLoadedTrips = 0;
        
        int[] nodeIndex = nh.getNodeIndex();
        int[] indexNode = nh.getIndexNode();

        double[] routeBoardings = new double[maxRoutes];

        for ( int dest=0; dest < numCentroids; dest++ ) {

            
            if ( TEST_DEST >= 0 && dest != nodeIndex[TEST_DEST] )
                continue;

            
            if ( dest % 100 == 0 ) {
                if ( SKIM_ONLY )
                    logger.info( String.format( "building %s %s %s optimal strategy for destination index %d for writing skim tables.", assignmentPeriod, accessMode, routeType, dest) );
                else
                    logger.info( String.format( "building %s %s %s optimal strategy for destination index %d for loading network and writing skim tables.", assignmentPeriod, accessMode, routeType, dest) );
            }
            
            
            // prepare trip array to assign if SKIM_ONLY == false
            double tripSum = 0.0;
            if ( ! SKIM_ONLY ) {
            
                for (int orig=0; orig < tripTable.length; orig++) {
    
                    // don't assign intra-zonal trips on network, but keep track of the total.
                    if ( orig == dest ) {
                        intrazonal += tripTable[orig][dest];
                        tripTableColumn[orig] = 0.0; 
                        continue;
                    }
                    else {
                        tripTableColumn[orig] = tripTable[orig][dest]; 
                        tripSum += tripTable[orig][dest];
                    }
                    
                }
                
            }
            
            
            // build optimal strategy for this network
            os.buildStrategy( dest, accessMode );

            
            // load trips onto strategy unless SKIM_ONLY == true
            if ( ! SKIM_ONLY ) {

                double destBoardings = 0.0;
                if ( tripSum > 0 ) {
    
                    double[] routeBoardingsToDest = os.loadOptimalStrategyDest( tripTableColumn, maxRoutes );
                    
                    for (int r=0; r < routeBoardings.length; r++) {
                        routeBoardings[r] += routeBoardingsToDest[r];
                        destBoardings += routeBoardingsToDest[r];
                    }
                    
                    totalTrips += tripSum;
                    notLoadedTrips += os.getTripsNotLoaded();
                    
                }
            
            }
            else if ( TEST_ORIG >= 0 && TEST_DEST >= 0 && dest == nodeIndex[TEST_DEST] ) {
                
                os.getOptimalStrategyLinks ( nodeIndex[TEST_ORIG] );
                os.testLoadOptimalStrategyDest();
                //return null;
                
            }
            
            // calculate skim matrices for strategy
            double[][] odSkimValues = os.getOptimalStrategySkimsDest();
            
            
            // save skim table values
            for (int k=0; k < OptimalStrategy.NUM_SKIMS; k++) {
                for (int orig=0; orig < numCentroids; orig++) { 
                    zeroBasedDoubleArray[k][orig][dest] = odSkimValues[k][orig];
                }
            }

        }


        // save loaded trips for summarizing unless SKIM_ONLY == true
        if ( ! SKIM_ONLY ) {
            saveTransitBoardings ( nh, identifier, accessMode, routeType, routeBoardings );
        }
        
        
        // save skim Matrix objects        
        float[][][] zeroBasedFloatArrays = new float[OptimalStrategy.NUM_SKIMS][][];

        for (int k=0; k < OptimalStrategy.NUM_SKIMS; k++) {
            zeroBasedFloatArrays[k] = getZeroBasedFloatArray ( nh.getIndexNode(), zeroBasedDoubleArray[k], numCentroids );
            zeroBasedDoubleArray[k] = null;
        }
        
        Matrix[] skimMatrices = new Matrix[OptimalStrategy.NUM_SKIMS];
        

        
        String nameQualifier = null;
        String descQualifier = null;
        if ( timePeriod.equalsIgnoreCase("ampeak") ) {
            nameQualifier = "p";
            descQualifier = "ampeak";
        }
        else {
            nameQualifier = "o";
            descQualifier = "mdoffpeak";
        }

        if ( accessMode.equalsIgnoreCase("walk") ) {
            nameQualifier += "w";
            descQualifier += " walk";
        }
        else {
            nameQualifier += "d";
            descQualifier += " drive";
        }
        
        if ( routeType.equalsIgnoreCase("air") ) {
            nameQualifier += "air";
            descQualifier += " air";
        }
        else if ( routeType.equalsIgnoreCase("hsr") ) {
            nameQualifier += "icr";
            descQualifier += " icr";
        }
        else if ( routeType.equalsIgnoreCase("intercity") ) {
            nameQualifier += "ic";
            descQualifier += " ic";
        }
        else if ( routeType.equalsIgnoreCase("intracity") ) {
            nameQualifier += "t";
            descQualifier += " tran";
        }
        

        skimMatrices[SkimType.IVT.ordinal()] = new Matrix( nameQualifier + "ivt", descQualifier + " in-vehicle time skims", zeroBasedFloatArrays[SkimType.IVT.ordinal()] );
        zeroBasedFloatArrays[SkimType.IVT.ordinal()] = null;
        skimMatrices[SkimType.FWT.ordinal()] = new Matrix( nameQualifier + "fwt", descQualifier + " first wait time skims", zeroBasedFloatArrays[SkimType.FWT.ordinal()] );
        zeroBasedFloatArrays[SkimType.FWT.ordinal()] = null;
        skimMatrices[SkimType.TWT.ordinal()] = new Matrix( nameQualifier + "twt", descQualifier + " total wait time skims", zeroBasedFloatArrays[SkimType.TWT.ordinal()] );
        zeroBasedFloatArrays[SkimType.TWT.ordinal()] = null;
        skimMatrices[SkimType.ACC.ordinal()] = new Matrix( nameQualifier + "acc", descQualifier + " access time skims", zeroBasedFloatArrays[SkimType.ACC.ordinal()] );
        zeroBasedFloatArrays[SkimType.ACC.ordinal()] = null;
        skimMatrices[SkimType.AUX.ordinal()] = new Matrix( nameQualifier + "aux", descQualifier + " other walk time skims", zeroBasedFloatArrays[SkimType.AUX.ordinal()] );
        zeroBasedFloatArrays[SkimType.AUX.ordinal()] = null;
        skimMatrices[SkimType.EGR.ordinal()] = new Matrix( nameQualifier + "egr", descQualifier + " egress walk time skims", zeroBasedFloatArrays[SkimType.EGR.ordinal()] );
        zeroBasedFloatArrays[SkimType.EGR.ordinal()] = null;
        skimMatrices[SkimType.BRD.ordinal()] = new Matrix( nameQualifier + "brd", descQualifier + " boardings skims", zeroBasedFloatArrays[SkimType.BRD.ordinal()] );
        zeroBasedFloatArrays[SkimType.BRD.ordinal()] = null;
        skimMatrices[SkimType.BRD_HSR.ordinal()] = new Matrix( nameQualifier + "brd_hsr", descQualifier + " hsr boardings skims", zeroBasedFloatArrays[SkimType.BRD_HSR.ordinal()] );
        zeroBasedFloatArrays[SkimType.BRD_HSR.ordinal()] = null;
        skimMatrices[SkimType.HSR_IVT.ordinal()] = new Matrix( nameQualifier + "hsr_ivt", descQualifier + " hsr in-vehicle time skims", zeroBasedFloatArrays[SkimType.HSR_IVT.ordinal()] );
        zeroBasedFloatArrays[SkimType.HSR_IVT.ordinal()] = null;
        skimMatrices[SkimType.HSR$.ordinal()] = new Matrix( nameQualifier + "hsr$", descQualifier + " hsr fare skims", zeroBasedFloatArrays[SkimType.HSR$.ordinal()] );
        zeroBasedFloatArrays[SkimType.HSR$.ordinal()] = null;
        skimMatrices[SkimType.AIR$.ordinal()] = new Matrix( nameQualifier + "air$", descQualifier + " air fare skims", zeroBasedFloatArrays[SkimType.AIR$.ordinal()] );
        zeroBasedFloatArrays[SkimType.AIR$.ordinal()] = null;
        skimMatrices[SkimType.RAIL_IVT.ordinal()] = new Matrix( nameQualifier + "cr_ivt", descQualifier + " cr in-vehicle time skims", zeroBasedFloatArrays[SkimType.RAIL_IVT.ordinal()] );
        zeroBasedFloatArrays[SkimType.RAIL_IVT.ordinal()] = null;
        skimMatrices[SkimType.RAIL$.ordinal()] = new Matrix( nameQualifier + "rail$", descQualifier + " ic rail fare skims", zeroBasedFloatArrays[SkimType.RAIL$.ordinal()] );
        zeroBasedFloatArrays[SkimType.RAIL$.ordinal()] = null;
        skimMatrices[SkimType.BUS_IVT.ordinal()] = new Matrix( nameQualifier + "bus_ivt", descQualifier + " ic bus in-vehicle time skims", zeroBasedFloatArrays[SkimType.BUS_IVT.ordinal()] );
        zeroBasedFloatArrays[SkimType.BUS_IVT.ordinal()] = null;
        skimMatrices[SkimType.BUS$.ordinal()] = new Matrix( nameQualifier + "bus$", descQualifier + " ic bus fare skims", zeroBasedFloatArrays[SkimType.BUS$.ordinal()] );
        zeroBasedFloatArrays[SkimType.BUS$.ordinal()] = null;
        skimMatrices[SkimType.TRAN$.ordinal()] = new Matrix( nameQualifier + "tran$", descQualifier + " local transit fare skims", zeroBasedFloatArrays[SkimType.TRAN$.ordinal()] );
        zeroBasedFloatArrays[SkimType.TRAN$.ordinal()] = null;
        
        for (int k=0; k < OptimalStrategy.NUM_SKIMS; k++)
            skimMatrices[k].setExternalNumbers( alphaExternalNumbers );
        
        
        return skimMatrices;
        
    }
    

    
    private void  saveTransitBoardings ( NetworkHandlerIF nh, String identifier, String accessMode, String routeType, double[] transitBoardings ) {
        
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
            
            int numRoutes = nh.getAuxNumRoutes(identifier);
            
            for (int rte=0; rte < numRoutes; rte++) {
               
                String rteName = nh.getAuxRouteName(identifier, rte);
                
                synchronized (objLock) {
                
                    if ( savedBoardings.containsKey(rteName) )
                        savedInfo = (SavedRouteInfo)savedBoardings.get(rteName);
                    else
                        savedInfo = new SavedRouteInfo(nh.getAuxRouteDescription(identifier, rte), nh.getAuxRouteMode(identifier, rte), nh.getAuxRouteType(identifier, rte) );
                    
                    index = 2*routeTypeIndex + accessIndex;
                    savedInfo.boardings[index] += transitBoardings[rte];
                    
                    savedBoardings.put(rteName, savedInfo);

                }
                
            }

        }
        else {
            logger.error ("error trying to save boardings - invalid routeType = " + routeType + " or accessMode = " + accessMode );
            System.exit(-1);
        }
        
    }

    

    
    private String getPeriodIdentifier ( String period ) {
        
        if ( period.equalsIgnoreCase( "ampeak" ) )
            return "pk";
        else if ( period.equalsIgnoreCase( "mdoffpeak" ) )
            return "op";
        else
            return "nul";
    }

    private String getAccessIdentifier ( String accessType ) {
        if ( accessType.equalsIgnoreCase( "walk" ) )
            return "w";
        else if ( accessType.equalsIgnoreCase( "drive" ) )
            return "d";
        else
            return "nul";
    }

    private String getRouteTypeIdentifier ( String routeType ) {
        if ( routeType.equalsIgnoreCase( "air" ) )
            return "air";
        else if ( routeType.equalsIgnoreCase( "hsr" ) )
            return "icr";
        else if ( routeType.equalsIgnoreCase( "intercity" ) )
            return "ic";
        else if ( routeType.equalsIgnoreCase( "intracity" ) )
            return "t";
        else
            return "nul";
    }

    /**
     * check that the route file exists and can be read.
     */
    private void checkRouteFile( String period, String accessMode, String rteType, String d221File) {

        try {
            if ( d221File == null ) {
                throw new RuntimeException();
            }
            else {
                File check = new File(d221File);
                if ( ! check.canRead() )
                    throw new RuntimeException();
            }
        }
        catch ( RuntimeException e ) {
            logger.error ( "Error while checking existence of route file specified for:");
            logger.error ( String.format( "period=%s, accessMode=%s, routeType=%s", period, accessMode, rteType) );
            logger.error ( String.format( "Filename for file not found=%s", d221File) );
            logger.error ( "" );
            throw e;
        }

    }

    /**
     * check for valid arguments for specifying a route file.
     * @param period
     * @param accessMode
     * @param rteType
     */
    private void checkArguments(String period, String accessMode, String rteType) {
        
        boolean fails = true;
        
        if ( period.equalsIgnoreCase("ampeak") || period.equalsIgnoreCase("mdoffpeak") || period.equalsIgnoreCase("pmpeak") || period.equalsIgnoreCase("ntoffpeak") ) {
            // air is not allowed for walk
            if ( accessMode.equalsIgnoreCase("walk") ) {
                if ( rteType.equalsIgnoreCase("hsr") || rteType.equalsIgnoreCase("intercity") || rteType.equalsIgnoreCase("intracity") ) {
                    fails = false;
                }
            }
            // intracity is not allowed for drive ldt
            else if ( accessMode.equalsIgnoreCase("driveLdt") ) {
                if ( rteType.equalsIgnoreCase("air") || rteType.equalsIgnoreCase("hsr") || rteType.equalsIgnoreCase("intercity") ) {
                    fails = false;
                }
            }
            // intracity is only allowed for drive
            else if ( accessMode.equalsIgnoreCase("drive") ) {
                if ( rteType.equalsIgnoreCase("intracity") ) {
                    fails = false;
                }
            }
        }
        if ( fails ) {
            invalidArgs ( period, accessMode, rteType );
        }

    }

    
    private void invalidArgs ( String period, String accessMode, String routeType ) {
        logger.error ( "Skims cannot be built for the combination of arguments specified:");
        logger.error ( String.format( "period=%s, accessMode=%s, routeType=%s", period, accessMode, routeType) );
        throw new RuntimeException();
    }
        

    
    private float[][] getZeroBasedFloatArray ( int[] skimsInternalToExternal, double[][] zeroBasedDoubleArray, int numCentroids ) {

        // convert the zero-based double[alphas+externals][alphas+externals] produced by the skimming procedure, with network centroid/zone index mapping
        // to a zero-based float[alphas+externals][alphas+externals] with indexZone mapping to be written to skims file.
        float[][] zeroBasedFloatArray = new float[numCentroids][numCentroids];
        
        int exRow;
        int exCol;
        int inRow;
        int inCol;
        for (int i=0; i < zeroBasedDoubleArray.length; i++) {
            exRow = skimsInternalToExternal[i];
            if ( zonesToSkim[exRow] == 1 ) {
                inRow = externalToAlphaInternal[exRow];
                for (int j=0; j < zeroBasedDoubleArray[i].length; j++) {
                    exCol = skimsInternalToExternal[j];
                    if ( zonesToSkim[exCol] == 1 ) {
                        inCol = externalToAlphaInternal[exCol];
                        zeroBasedFloatArray[inRow][inCol] = (float)zeroBasedDoubleArray[i][j];
                    }
                }
            }
        }

        zeroBasedDoubleArray = null;

        return zeroBasedFloatArray;

    }


    
    private void initSkimMatrices ( NetworkHandlerIF nh, String zoneCorrespondenceFile ) {

        int[] alphaNumberArray = null;
        String[] fareZoneLabels = null;
        
        // take a column of alpha zone numbers from a TableDataSet and puts them into an array for
        // purposes of setting external numbers.         */
        try {
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            TableDataSet table = reader.readFile(new File(zoneCorrespondenceFile));
            alphaNumberArray = table.getColumnAsInt( 1 );
            fareZoneLabels = table.getColumnAsString( "Fare" );
        } catch (IOException e) {
            logger.fatal("Can't get zone numbers from zonal correspondence file");
            e.printStackTrace();
        }

        // get the list of externals from the NetworkHandler.
        int[] externals = nh.getExternalZoneLabels();

    
        // define which of the total set of centroids are within the Halo area and should have skim trees built
        // include external zones (5000s)
        zonesToSkim = new int[nh.getMaxCentroid()+1];
        externalToAlphaInternal = new int[nh.getMaxCentroid()+1];
        alphaExternalNumbers = new int[nh.getNumCentroids()+1];
        Arrays.fill ( zonesToSkim, 0 );
        Arrays.fill ( externalToAlphaInternal, -1 );
        for (int i=0; i < alphaNumberArray.length; i++) {
            zonesToSkim[alphaNumberArray[i]] = 1;
            externalToAlphaInternal[alphaNumberArray[i]] = i;
            alphaExternalNumbers[i+1] = alphaNumberArray[i];
        }
        for (int i=0; i < externals.length; i++) {
            zonesToSkim[alphaNumberArray.length+i] = 1;
            externalToAlphaInternal[externals[i]] = alphaNumberArray.length+i;
            alphaExternalNumbers[alphaNumberArray.length+i+1] = externals[i];
        }

        
        fareZones = new HashMap<Integer,String>();
        for (int i=0; i < alphaNumberArray.length; i++ ) {
            fareZones.put(alphaNumberArray[i], fareZoneLabels[i]);
        }
        
        for (int i=0; i < externals.length; i++ ) {
            fareZones.put(externals[i], OUT_OF_AREA_FARE_ZONE);
        }
        
    }
    
    
    
    

    public void logTransitBoardingsReport ( String csvFileName, String repFileName, String periodHeadingLabel ) {
                
        
        // write results to csv file, if one was named in properties file
        if ( csvFileName != null ) {
        
            // open csv file for saving transit assignment route boardings summary information 
            PrintWriter outStream = null;
            try {
                outStream = new PrintWriter (new BufferedWriter( new FileWriter(csvFileName) ) );
            }
            catch (IOException e) {
                logger.fatal ( String.format("I/O exception opening transit boardings csv file=%s.", csvFileName), e);
                System.exit(-1);
            }

            
            // write formatted lines to file here as first line in csv file
            ArrayList<String> outputLines = formatCsvHeaderLines();
            Iterator<String> it = outputLines.iterator();
            while ( it.hasNext() ) {
                outStream.write( it.next() );
            }                


            // get set of route boardings results for all route types
            int lineCount = 0;
            String[] typeList = { "air", "hsr", "intercity", "intracity" };
            for ( String type : typeList ) {

                outputLines = getCsvOutputLines ( savedBoardings, type, ++lineCount );
                
                // write formatted lines to file
                it = outputLines.iterator();
                while ( it.hasNext() ) {
                    outStream.write( (String)it.next() );
                }                
                
            }
            
            outStream.close();

        }   


        
        
        
        // likewise for a report file, if one was named in properties file
        if ( repFileName != null ) {
        
            // open log file for saving transit assignment route boardings summary information 
            PrintWriter outStream = null;
            try {
                outStream = new PrintWriter (new BufferedWriter( new FileWriter(repFileName) ) );
            }
            catch (IOException e) {
                logger.fatal ( String.format("I/O exception opening transit boardings report file=%s.", csvFileName), e);
                System.exit(-1);
            }

            
            
            
            // get set of route boardings results for all route types
            int lineCount = 0;
            String[] typeList = { "air", "hsr", "intercity", "intracity" };
            for ( String type : typeList ) {

                ArrayList<String> outputLines = getLogOutputLines ( savedBoardings, type, periodHeadingLabel, ++lineCount );
                
                // write formatted lines to file
                Iterator<String> it = outputLines.iterator();
                while ( it.hasNext() ) {
                    outStream.write( it.next() );
                }                
                
            }
            
            outStream.close();

        }   

    }
    
    
        
    private ArrayList<String> formatLogHeaderLines( String periodHeadingLabel, String routeType, String descrFormat ) {
        
        ArrayList<String> outputLines = new ArrayList<String>();
        
        String title = String.format ( "Transit Network Boardings Report for %s Period %s Trips\n", periodHeadingLabel, routeType );
        String dashes = "";
        for (int i=0; i < title.length(); i++)
            dashes += "-";
        dashes += "\n";
        
        outputLines.add( dashes );
        outputLines.add( title );
        outputLines.add( dashes );
        outputLines.add( "\n" );
        outputLines.add( "\n" );
        
        String outputString = String.format("%-6s %-9s " + descrFormat + " %-10s %-6s %8s %8s    %8s %8s    %8s %8s    %8s %8s    %8s %8s    %8s\n", "Count", "Route", "Description", "RouteType", "Mode", "wAir", "dAir", "wHsr", "dHsr", "wIc", "dIc", "wt", "dt", "wTot", "dTot", "Total") ;

        dashes = "";
        for (int i=0; i < outputString.length(); i++)
            dashes += "-";
        dashes += "\n";
        
        outputLines.add( outputString );
        outputLines.add( dashes );
        
        return outputLines;
    }


    private String formatLogRecord( String name, SavedRouteInfo info, String descrFormat, int lineCount ) {
        double wTot = info.boardings[0] + info.boardings[2] + info.boardings[4] + info.boardings[6];
        double dTot = info.boardings[1] + info.boardings[3] + info.boardings[5] + info.boardings[7];
        String outputString = String.format("%-6d %-9s " + descrFormat + " %-10s  %-6c %8.2f %8.2f    %8.2f %8.2f    %8.2f %8.2f    %8.2f %8.2f    %8.2f %8.2f    %8.2f\n", lineCount, name, info.description, info.routeType, info.mode, info.boardings[0], info.boardings[1], info.boardings[2], info.boardings[3], info.boardings[4], info.boardings[5], info.boardings[6], info.boardings[7], wTot, dTot, (wTot+dTot));
        return outputString;
    }

    
    private String formatLogTotalsRecord( double[] totals, String descrFormat ) {
        String outputString = String.format("%-16s " + descrFormat + " %-10s  %-6s %8.2f %8.2f    %8.2f %8.2f    %8.2f %8.2f    %8.2f %8.2f    %8.2f %8.2f    %8.2f\n", "Total Boardings", "", "", "", totals[0], totals[1], totals[2], totals[3], totals[4], totals[5], totals[6], totals[7], totals[8], totals[9], totals[10]) ;
        return outputString;
    }

        
    private ArrayList<String> formatCsvHeaderLines() {
        ArrayList<String> outputLines = new ArrayList<String>();
        outputLines.add("Count,Route,Description,RouteType,Mode,wAir,dAir,wHsr,dHsr,wIc,dIc,wt,dt,wTot,dTot,Total\n");
        return outputLines;
    }

    
    private String formatCsvRecord( String name, SavedRouteInfo info, int lineCount ) {
        double wTot = info.boardings[0] + info.boardings[2] + info.boardings[4] + info.boardings[6];
        double dTot = info.boardings[1] + info.boardings[3] + info.boardings[5] + info.boardings[7];
        String outputString = String.format("%d,%s,%s,%s,%c,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n", lineCount, name, info.description, info.routeType, info.mode, info.boardings[0], info.boardings[1], info.boardings[2], info.boardings[3], info.boardings[4], info.boardings[5], info.boardings[6], info.boardings[7], wTot, dTot, (wTot+dTot));
        return outputString;
    }


    private double[] updateTotals( SavedRouteInfo info, double[] totals ) {
    
        double wTot = info.boardings[0] + info.boardings[2] + info.boardings[4] + info.boardings[6];
        double dTot = info.boardings[1] + info.boardings[3] + info.boardings[5] + info.boardings[7];

        for (int i=0; i < 8; i++)
            totals[i] += info.boardings[i];
        
        totals[8] += wTot;
        totals[9] += dTot;
        totals[10] += (wTot + dTot);
        
        return totals;
        
    }
    
    
    private ArrayList<String> getCsvOutputLines ( HashMap<String,SavedRouteInfo> savedBoardings, String type, int lineCount ) {
        
        ArrayList<String> outputLines = new ArrayList<String>();
        double[] totals = new double[11];

        // get the subset of route names that match type in sorted order
        SortedSet< String > nameSet = new TreeSet< String >();
        Iterator<String> it = savedBoardings.keySet().iterator();
        while ( it.hasNext() ) {
            String name = it.next();
            SavedRouteInfo info = (SavedRouteInfo)savedBoardings.get(name);
            if ( info.routeType.equalsIgnoreCase( type ) )
                nameSet.add(name);
        }
        

        // generate an output record for each route in the selected subset
        it = nameSet.iterator();
        while ( it.hasNext() ) {
            String name = it.next();
            SavedRouteInfo info = (SavedRouteInfo)savedBoardings.get(name);
            String outputString = formatCsvRecord( name, info, ++lineCount );
            totals = updateTotals( info, totals );
            outputLines.add( outputString );
        }

        return outputLines;
        
    }
    
    
    private ArrayList<String> getLogOutputLines ( HashMap<String,SavedRouteInfo> savedBoardings, String type, String periodHeadingLabel, int lineCount ) {
        
        double[] totals = new double[11];


        // construct a format string for the description field from the longest route description of any route
        int maxStringLength = 0;
        Iterator<String> it = savedBoardings.keySet().iterator();
        while ( it.hasNext() ) {
            String name = it.next();
            SavedRouteInfo info = (SavedRouteInfo)savedBoardings.get(name);
            if ( info.description.length() > maxStringLength )
                maxStringLength = info.description.length();
        }
        String descrFormat = "%-" + (maxStringLength+4) + "s";

        

        
        // add header lines to output list
        ArrayList<String> outputLines = formatLogHeaderLines( periodHeadingLabel, type, descrFormat );


        // get the subset of route names that match type in sorted order
        SortedSet< String > nameSet = new TreeSet< String >();
        it = savedBoardings.keySet().iterator();
        while ( it.hasNext() ) {
            String name = (String)it.next();
            SavedRouteInfo info = (SavedRouteInfo)savedBoardings.get(name);
            if ( info.routeType.equalsIgnoreCase( type ) )
                nameSet.add(name);
        }
        

        // generate an output record for each route in the selected subset and add to output list
        it = nameSet.iterator();
        while ( it.hasNext() ) {
            String name = (String)it.next();
            SavedRouteInfo info = (SavedRouteInfo)savedBoardings.get(name);
            String outputString = formatLogRecord( name, info, descrFormat, ++lineCount );
            totals = updateTotals( info, totals );
            outputLines.add( outputString );
        }

        // add the summary totals record to output list
        String outputString = formatLogTotalsRecord( totals, descrFormat );
        String dashes = "";
        for (int i=0; i < outputString.length(); i++)
            dashes += "-";
        dashes += "\n";

        outputLines.add( dashes );
        outputLines.add( outputString );

        // add some white space to report
        outputLines.add( "\n" );
        outputLines.add( "\n" );
        outputLines.add( "\n" );

        return outputLines;
        
    }
    
    
    
    
    
    
    
    public class SavedRouteInfo {
        
        static final int NUM_ROUTE_TYPES = 4;
        
        String routeType;
        String description;
        char mode;
        double[] boardings;
        
        private SavedRouteInfo(String description, char mode, String routeType) {
            this.routeType = routeType;
            this.description = description;
            this.mode = mode;
            boardings = new double[2*NUM_ROUTE_TYPES];
        }
        
    }
    


    public enum SkimType {
        IVT,
        FWT,
        TWT,
        ACC,
        EGR,
        AUX,
        BRD,
        BRD_HSR,
        HSR_IVT,
        HSR$,
        AIR$,
        RAIL_IVT,
        RAIL$,
        BUS_IVT,
        BUS$,
        TRAN$
    }

    
    public class AssignSkimTask implements Callable<String> {
        
        private Logger logger = Logger.getLogger(AssignSkimTask.class);
        
        private NetworkHandlerIF nh;
        private String[] serviceTypes;      // combinations of air, hsr, intercity, intracity
        private String specificServiceType; // air, hsr, intercity, or intracity
        private String period;              // amPeak, mdOffPeak, pmPeak, or ntOffPeak 
        private String accessMode;          // driveLdt, drive, or walk
        private String accessModeType;      // drive or walk
        private String tripModeName;        // LDTripModeType or TripModeType trip mode name
        
        private String identifier;          // unique String to distinguish the 7 types of transit assignments.
        
        public AssignSkimTask( NetworkHandlerIF nh, String[] serviceTypes, String period, String accessMode, String accessModeType, String specificServiceType, String tripModeName ) {
            this.nh = nh;
            this.serviceTypes = serviceTypes;
            this.specificServiceType = specificServiceType;
            this.period = period;
            this.accessMode = accessMode;
            this.accessModeType = accessModeType;
            this.tripModeName = tripModeName;
            
            this.identifier = accessMode + "_" + specificServiceType;
            
            logger.info( String.format( "task created to load and skim %s period %s access %s transit using %s access network for %s trips", period, accessModeType, specificServiceType, accessMode ,tripModeName ) );
        }
        
        public String call() {
            
            try {
                
                // transit loading and skims
                setupTransitNetwork( identifier, nh, period, accessMode, serviceTypes );
                Matrix[] skims = runTransitAssignment ( identifier, nh, period, accessModeType, specificServiceType, tripModeName );
                writeSkims ( skims, period, accessModeType, specificServiceType );
                
            }
            catch (RuntimeException e) {
                logger.fatal ( String.format( "exception caught loading and skimming %s period %s access %s transit using %s access network for %s trips", period, accessModeType, specificServiceType, accessMode ,tripModeName ) );
                throw e;
            }
            
            return String.format( "%s period %s access %s", period, accessModeType, specificServiceType );
        }
        
        
        private void writeSkims( Matrix[] skims, String period, String access, String serviceType ) {
            
            if ( access.equalsIgnoreCase("drive") ) {

                if ( serviceType.equalsIgnoreCase("air") )
                    writeDriveAirSkims ( skims, period );
                else if ( serviceType.equalsIgnoreCase("hsr") )
                    writeDriveHsrSkims ( skims, period );
                else if ( serviceType.equalsIgnoreCase("intercity") )
                    writeDriveIntercitySkims ( skims, period );
                else if ( serviceType.equalsIgnoreCase("intracity") )
                    writeDriveIntracitySkims ( skims, period );
                else {
                    logger.fatal ( String.format( "'%s' is not a valid service type for writing %s period drive transit skim files", serviceType, period ) );
                    throw new RuntimeException();
                }
                
            }
            else if ( access.equalsIgnoreCase("walk") ) {

                // no walk access air trips assigned or skimmed
                if ( serviceType.equalsIgnoreCase("hsr") )
                    writeWalkHsrSkims ( skims, period );
                else if ( serviceType.equalsIgnoreCase("intercity") )
                    writeWalkIntercitySkims ( skims, period );
                else if ( serviceType.equalsIgnoreCase("intracity") )
                    writeWalkIntracitySkims ( skims, period );
                else {
                    logger.fatal ( String.format( "'%s' is not a valid service type for writing %s period walk transit skim files", serviceType, period ) );
                    throw new RuntimeException();
                }
                
            }
            else {
                logger.fatal ( String.format( "'%s' is not a valid access mode for writing %s period %s transit skim files", access, period, serviceType ) );
                throw new RuntimeException();
            }
                
        }
        
    }
    
    
}

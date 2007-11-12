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
import com.pb.tlumip.ts.NetworkHandlerIF;
import com.pb.tlumip.ts.transit.OptimalStrategy;
import com.pb.tlumip.ts.transit.TrRoute;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;


import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;


public class TransitAssignAndSkimManager {

	protected static Logger logger = Logger.getLogger(TransitAssignAndSkimManager.class);

    static final boolean CREATE_NEW_NETWORK = true;
	
	
    NetworkHandlerIF nh = null;
	
	HashMap tsPropertyMap = null;
    HashMap globalPropertyMap = null;
	
    ResourceBundle appRb = null;
    ResourceBundle globalRb = null;
    
    String skimFileExtension = null;
    String skimFileDirectory = null;
    
    String transitRouteDataFilesDirectory = null;
    String transitRoutesDirectory = null;
    int maxRoutes = 0;

    
    HashMap fareZones = null;
    
    int[] indexNode = null;
    int[] alphaNumberArray = null;
    int[] zonesToSkim = null;
    int[] externalToAlphaInternal = null;
    int[] alphaExternalNumbers = null;

    Matrix[] skimMatrices = null;
    HashMap savedBoardings = null;
    HashMap intracityFareTable = null;

    
	
    public TransitAssignAndSkimManager(NetworkHandlerIF nh, ResourceBundle appRb, ResourceBundle globalRb) {

        // get the items in the properties file
        tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap ( appRb );
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap ( globalRb );

        this.nh = nh;
        this.appRb = appRb;
        this.globalRb = globalRb;
        
        // get some information from the properties file
        transitRouteDataFilesDirectory = (String)tsPropertyMap.get("transitRouteDataFiles.directory");
        transitRoutesDirectory = (String)tsPropertyMap.get("transitRoutes.directory");
        maxRoutes = Integer.parseInt ( (String)tsPropertyMap.get("MAX_TRANSIT_ROUTES") );

        skimFileExtension = (String)globalPropertyMap.get("matrix.extension");
        skimFileDirectory = (String)tsPropertyMap.get("transitSkims.directory");

        intracityFareTable = readIntracityTransitFareCsvFile();
        
        savedBoardings = new HashMap();
        
        indexNode = nh.getIndexNode();

    }    
    

    
    public void assignAndSkimTransit ( String period ) {
        
        // drive access air loading and skims
        String[] drAirTypes = { "air" }; 
        setupTransitNetwork( nh, period, "driveLdt", drAirTypes );
        runTransitAssignment ( nh, "drive", "air", LDTripModeType.AIR.name() );
        writeDriveAirSkims ( period );
    
        
        // drive access hsr loading and skims
        String[] drHsrTypes = { "hsr", "intercity" }; 
        setupTransitNetwork( nh, period, "driveLdt", drHsrTypes );
        runTransitAssignment ( nh, "drive", "hsr", LDTripModeType.HSR_DRIVE.name() );
        writeDriveHsrSkims ( period );
    
        
        // drive access intercity bus/rail loading and skims
        String[] drIcTypes = { "intercity" }; 
        setupTransitNetwork( nh, period, "driveLdt", drIcTypes );
        runTransitAssignment ( nh, "drive", "intercity", LDTripModeType.TRANSIT_DRIVE.name() );
        writeDriveIntercitySkims ( period );
    
        
        // drive access intracity transit loading and skims
        String[] drTrTypes = { "intracity" }; 
        setupTransitNetwork( nh, period, "drive", drTrTypes );
        runTransitAssignment ( nh, "drive", "intracity", TripModeType.DR_TRAN.name() );
        writeDriveIntracitySkims ( period );
    
        
        // walk access hsr loading and skims
        String[] wkHsrTypes = { "hsr", "intercity", "intracity" }; 
        setupTransitNetwork( nh, period, "walk", wkHsrTypes );
        runTransitAssignment ( nh, "walk", "hsr", LDTripModeType.HSR_WALK.name() );
        writeWalkHsrSkims ( period );

        
        // walk access hsr loading and skims
        String[] wkIcTypes = { "intercity", "intracity" }; 
        setupTransitNetwork( nh, period, "walk", wkIcTypes );
        runTransitAssignment ( nh, "walk", "intercity", LDTripModeType.TRANSIT_WALK.name() );
        writeWalkIntercitySkims ( period );

        
        // walk access hsr loading and skims
        String[] wkTrTypes = { "intracity" }; 
        setupTransitNetwork( nh, period, "walk", wkTrTypes );
        runTransitAssignment ( nh, "walk", "intracity", TripModeType.WK_TRAN.name() );
        writeWalkIntracitySkims ( period );
    
    }

    
    
    public HashMap getSavedBoardings () {
        return savedBoardings;
    }
    
    

    /**
     * setup transit network from route files specified:
     * 
     * Examples:
     *      "peak", "walk", ["intracity"]
     *      "offpeak", "drive", ["intracity"]
     *      "offpeak", "driveLDT", ["air"]
     *      "offpeak", "driveLDT", ["hsr", "intercity"]
     *      "peak", "walk", ["intercity", "hsr", "intracity"]
     * @param nh
     * @param period
     * @param accessMode
     * @param rteTypes
     * @return
     */
    private int setupTransitNetwork ( NetworkHandlerIF nh, String period, String accessMode, String[] rteTypes ) {
        
        // construct a network listing output filename for the transit network being setup - assume primary route type is the first listed
        String listingName = (String)tsPropertyMap.get("transitNetworkListings.directory") + period + "_" + accessMode + "_" + rteTypes[0] + ".listing";

        // transit network setup method expects arrays of filenames and types:
        String[] d221Files = new String[rteTypes.length];

        
        // check route files
        for (int i=0; i < rteTypes.length; i++) {
            
            // check for valid specification and combination of arguments
            checkArguments( period, accessMode, rteTypes[i] );
            
            // construct a target for the route file (e.g. "air.pk.fileName")
            String d221Target = String.format("%s.%s.fileName", rteTypes[i], ( period.equalsIgnoreCase("peak") ? "pk" : "op" ) );
            
            // get the file name for the target from the propoerties file
            d221Files[i] = transitRoutesDirectory + (String)tsPropertyMap.get(d221Target);
            
            // check existence of route file
            checkRouteFile( period, accessMode, rteTypes[i], d221Files[i]);

        }

        return nh.setupTransitNetworkObject ( period, accessMode, listingName, transitRouteDataFilesDirectory, d221Files, rteTypes, maxRoutes );
        
    }

    

    private HashMap readIntracityTransitFareCsvFile () {

        final String OrigDistLabel = "OFareDistrict";
        final String DestDistLabel = "DFareDistrict";
        final String Fare2007Label = "Fare_2007$";
        final String Fare1990Label = "Fare_1990$";

        HashMap fareTable = null;

        String filename = (String)tsPropertyMap.get("fareZoneFares.file");
        
        // read the extra link attributes file and update link attributes table values.
        if ( filename != null && ! filename.equals("") ) {

            try {
                
                OLD_CSVFileReader reader = new OLD_CSVFileReader();
                TableDataSet table = reader.readFile( new File(filename) );

                fareTable = new HashMap();
                
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
    private void writeDriveAirSkims ( String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        String accessIdentifier = getAccessIdentifier("drive");
        String routeTypeIdentifier = getRouteTypeIdentifier("air");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "ivt" + periodIdentifier + skimFileExtension;
        String drvFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "drv" + periodIdentifier + skimFileExtension;
        String farFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "far" + periodIdentifier + skimFileExtension;
        
        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AIR$.ordinal()] );

        Matrix m = skimMatrices[SkimType.ACC.ordinal()].add(skimMatrices[SkimType.EGR.ordinal()] );   // for drive air, drv is acc + egr drive time
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(drvFilename) );
        mw.writeMatrix( m );

        logger.info ("done writing " + period + " drive air skims files.");
            
    }

    
    /**
     * write a set of zip format drive access high speed rail skim matrix files for the period specified
     */
    private void writeDriveHsrSkims ( String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        String accessIdentifier = getAccessIdentifier("drive");
        String routeTypeIdentifier = getRouteTypeIdentifier("hsr");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "ivt" + periodIdentifier + skimFileExtension;
        String fwtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "fwt" + periodIdentifier + skimFileExtension;
        String xwkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "xwk" + periodIdentifier + skimFileExtension;
        String drvFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "drv" + periodIdentifier + skimFileExtension;
        String farFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "far" + periodIdentifier + skimFileExtension;

        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(xwkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AUX.ordinal()] );

        Matrix m = skimMatrices[SkimType.HSR$.ordinal()].add( skimMatrices[SkimType.BUS$.ordinal()].add( skimMatrices[SkimType.RAIL$.ordinal()] ) );   // for drive hsr, far combines hsr$, bus$, and rail$
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( m );

        m = skimMatrices[SkimType.ACC.ordinal()].add( skimMatrices[SkimType.EGR.ordinal()] );   // for drive hsr, drv is acc + egr drive time
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(drvFilename) );
        mw.writeMatrix( m );

        logger.info ("done writing " + period + " drive high speed rail skims files.");
            
    }

    
    /**
     * write a set of zip format drive access intercity transit skim matrix files for the period specified
     */
    private void writeDriveIntercitySkims ( String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        String accessIdentifier = getAccessIdentifier("drive");
        String routeTypeIdentifier = getRouteTypeIdentifier("intercity");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "ivt" + periodIdentifier + skimFileExtension;
        String fwtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "fwt" + periodIdentifier + skimFileExtension;
        String twtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "twt" + periodIdentifier + skimFileExtension;
        String xwkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "xwk" + periodIdentifier + skimFileExtension;
        String drvFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "drv" + periodIdentifier + skimFileExtension;
        String farFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "far" + periodIdentifier + skimFileExtension;
        
        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(twtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TWT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(xwkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AUX.ordinal()] );

        Matrix m = skimMatrices[SkimType.BUS$.ordinal()].add( skimMatrices[SkimType.RAIL$.ordinal()] );   // for drive intercity, far combines bus$ and rail$
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( m );

        m = skimMatrices[SkimType.ACC.ordinal()].add( skimMatrices[SkimType.EGR.ordinal()] );   // for drive intercity, drv is acc + egr drive time
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(drvFilename) );
        mw.writeMatrix( m );

        logger.info ("done writing " + period + " drive intercity bus/rail skims files.");
            
    }

    

    /**
     * write a set of zip format drive access intracity transit skim matrix files for the period specified
     */
    private void writeDriveIntracitySkims ( String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        String accessIdentifier = getAccessIdentifier("drive");
        String routeTypeIdentifier = getRouteTypeIdentifier("intracity");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "ivt" + periodIdentifier + skimFileExtension;
        String fwtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "fwt" + periodIdentifier + skimFileExtension;
        String twtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "twt" + periodIdentifier + skimFileExtension;
        String drvFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "drv" + periodIdentifier + skimFileExtension;
        String xwkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "xwk" + periodIdentifier + skimFileExtension;
        String ewkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "ewk" + periodIdentifier + skimFileExtension;
        String farFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "far" + periodIdentifier + skimFileExtension;
        
        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(twtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TWT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(drvFilename) );
        mw.writeMatrix( skimMatrices[SkimType.ACC.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(xwkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AUX.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ewkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.EGR.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TRAN$.ordinal()] );

        logger.info ("done writing " + period + " drive intracity transit skims files.");
            
    }

    

    /**
     * write a set of zip format walk access high speed rail skim matrix files for the period specified
     */
    private void writeWalkHsrSkims ( String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        String accessIdentifier = getAccessIdentifier("walk");
        String routeTypeIdentifier = getRouteTypeIdentifier("hsr");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "ivt" + periodIdentifier + skimFileExtension;
        String fwtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "fwt" + periodIdentifier + skimFileExtension;
        String twtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "twt" + periodIdentifier + skimFileExtension;
        String awkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "awk" + periodIdentifier + skimFileExtension;
        String xwkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "xwk" + periodIdentifier + skimFileExtension;
        String ewkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "ewk" + periodIdentifier + skimFileExtension;
        String farFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "far" + periodIdentifier + skimFileExtension;
        
        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(twtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TWT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(awkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.ACC.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(xwkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AUX.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ewkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.EGR.ordinal()] );

        Matrix m = skimMatrices[SkimType.HSR$.ordinal()].add( skimMatrices[SkimType.BUS$.ordinal()] ).add( skimMatrices[SkimType.RAIL$.ordinal()] ).add( skimMatrices[SkimType.TRAN$.ordinal()] );   // for walk hsr, far combines hsr$, bus$, $rail and tran$
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( m );

        logger.info ("done writing " + period + " walk high speed rail skims files.");
            
    }

    

    /**
     * write a set of zip format walk access intercity bus/rail skim matrix files for the period specified
     */
    private void writeWalkIntercitySkims ( String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        String accessIdentifier = getAccessIdentifier("walk");
        String routeTypeIdentifier = getRouteTypeIdentifier("intercity");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "ivt" + periodIdentifier + skimFileExtension;
        String fwtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "fwt" + periodIdentifier + skimFileExtension;
        String twtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "twt" + periodIdentifier + skimFileExtension;
        String awkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "awk" + periodIdentifier + skimFileExtension;
        String xwkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "xwk" + periodIdentifier + skimFileExtension;
        String ewkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "ewk" + periodIdentifier + skimFileExtension;
        String farFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "far" + periodIdentifier + skimFileExtension;
        
        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(twtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TWT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(awkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.ACC.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(xwkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AUX.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ewkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.EGR.ordinal()] );

        Matrix m = skimMatrices[SkimType.BUS$.ordinal()].add( skimMatrices[SkimType.RAIL$.ordinal()] ).add( skimMatrices[SkimType.TRAN$.ordinal()] );   // for walk intercity, far combines bus$, $rail and tran$
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( m );

        logger.info ("done writing " + period + " walk intercity bus/rail skims files.");
            
    }

    

    /**
     * write a set of zip format walk access intracity transit skim matrix files for the period specified
     */
    private void writeWalkIntracitySkims ( String period ) {

        String periodIdentifier = getPeriodIdentifier(period);
        String accessIdentifier = getAccessIdentifier("walk");
        String routeTypeIdentifier = getRouteTypeIdentifier("intracity");
        
        // generate filenames
        String ivtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "ivt" + periodIdentifier + skimFileExtension;
        String fwtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "fwt" + periodIdentifier + skimFileExtension;
        String twtFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "twt" + periodIdentifier + skimFileExtension;
        String awkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "awk" + periodIdentifier + skimFileExtension;
        String xwkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "xwk" + periodIdentifier + skimFileExtension;
        String ewkFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "ewk" + periodIdentifier + skimFileExtension;
        String farFilename = skimFileDirectory + accessIdentifier + routeTypeIdentifier + "far" + periodIdentifier + skimFileExtension;
        
        
        // aggregate skim tables if necessary and prepare final Matrix objects to be written out
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ivtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.IVT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fwtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.FWT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(twtFilename) );
        mw.writeMatrix( skimMatrices[SkimType.TWT.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(awkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.ACC.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(xwkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.AUX.ordinal()] );

        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(ewkFilename) );
        mw.writeMatrix( skimMatrices[SkimType.EGR.ordinal()] );

        Matrix m = skimMatrices[SkimType.TRAN$.ordinal()];   // for walk intracity, far is tran$
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(farFilename) );
        mw.writeMatrix( m );

        logger.info ("done writing " + period + " walk intracity transit skims files.");
            
    }

    

    
    
    private void runTransitAssignment ( NetworkHandlerIF nh, String accessMode, String routeType, String tripMode ) {
        
        HashMap globalMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

        String assignmentPeriod = nh.getTimePeriod();
        
        int startHour = 0;
        int endHour = 0;
        if ( assignmentPeriod.equalsIgnoreCase( "peak" ) ) {
            // get peak period definitions from property files
            startHour = Integer.parseInt((String)globalMap.get("am.peak.start"));
            endHour = Integer.parseInt( (String)globalMap.get("am.peak.end") );
        }
        else if ( assignmentPeriod.equalsIgnoreCase( "offpeak" ) ) {
            // get off-peak period definitions from property files
            startHour = Integer.parseInt((String)globalMap.get("offpeak.start"));
            endHour = Integer.parseInt( (String)globalMap.get("offpeak.end") );
        }
        
        
        // get the transit trip table to be assigned 
        DemandHandler d = new DemandHandler();
        double sampleRate = 1.0;
        String rateString = (String)globalMap.get("pt.sample.rate");
        if ( rateString != null )
            sampleRate = Double.parseDouble( rateString );
        d.setup( (String)globalMap.get("sdt.person.trips"), (String)globalMap.get("ldt.vehicle.trips"), sampleRate, (String)globalMap.get("ct.truck.trips"), startHour, endHour, assignmentPeriod, nh.getNumCentroids(), nh.getNumUserClasses(), nh.getNodeIndex(), nh.getAlphaDistrictIndex(), nh.getDistrictNames(), nh.getAssignmentGroupChars(), nh.getHighwayModeCharacters(), nh.userClassesIncludeTruck() );
        
        double[][] tripTable = d.getTripTablesForMode ( tripMode );

        
        
        
        
        // load the triptable on walk access transit network
        // create an optimal strategy object for this highway and transit network
        OptimalStrategy os = new OptimalStrategy( nh );
        os.setTransitFareTables ( intracityFareTable, fareZones ); 

        // arrays for skim values into 0-based double[][] dimensioned to number of actual zones including externals (2983)
        double[][][] zeroBasedDoubleArray = new double[OptimalStrategy.NUM_SKIMS][nh.getNumCentroids()][nh.getNumCentroids()];
                
        double intrazonal = 0;
        double totalTrips = 0;
        double notLoadedTrips = 0;

        double[] routeBoardings = new double[nh.getMaxRoutes()];
        double[] tripTableColumn = new double[tripTable[0].length];

        for ( int dest=0; dest < nh.getNumCentroids(); dest++ ) {
            
            if ( dest % 100 == 0 )
                logger.info( "loading " + assignmentPeriod + " " + accessMode + " " + routeType + " transit trips for destination zone " + dest + "." );
            
            double tripSum = 0.0;
            for (int orig=0; orig < tripTable.length; orig++) {

                // don't assign intra-zonal trips
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
            
            os.buildStrategy( dest );

            
            // load trips onto strategy
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
            
            
            
            // calculate skim matrices for strategy
            double[][] odSkimValues = os.getOptimalStrategySkimsDest();
            
            // save skim table values
            for (int k=0; k < OptimalStrategy.NUM_SKIMS; k++) {
                for (int orig=0; orig < nh.getNumCentroids(); orig++)
                    zeroBasedDoubleArray[k][orig][dest] = odSkimValues[k][orig];
                
            }

        }

        saveTransitBoardings ( nh, accessMode, routeType, routeBoardings );

        
        
        // save skim Matrix objects        
        float[][][] zeroBasedFloatArrays = new float[OptimalStrategy.NUM_SKIMS][][];

        initSkimMatrices ( (String)globalPropertyMap.get("alpha2beta.file") );
        
        for (int k=0; k < OptimalStrategy.NUM_SKIMS; k++) {
            zeroBasedFloatArrays[k] = getZeroBasedFloatArray ( zeroBasedDoubleArray[k] );
        }
        
        skimMatrices = new Matrix[OptimalStrategy.NUM_SKIMS];
        

        
        String nameQualifier = null;
        String descQualifier = null;
        if ( nh.getTimePeriod().equalsIgnoreCase("peak") ) {
            nameQualifier = "p";
            descQualifier = "peak";
        }
        else {
            nameQualifier = "o";
            descQualifier = "offpeak";
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
        skimMatrices[SkimType.FWT.ordinal()] = new Matrix( nameQualifier + "fwt", descQualifier + " first wait time skims", zeroBasedFloatArrays[SkimType.FWT.ordinal()] );
        skimMatrices[SkimType.TWT.ordinal()] = new Matrix( nameQualifier + "twt", descQualifier + " total wait time skims", zeroBasedFloatArrays[SkimType.TWT.ordinal()] );
        skimMatrices[SkimType.ACC.ordinal()] = new Matrix( nameQualifier + "acc", descQualifier + " access time skims", zeroBasedFloatArrays[SkimType.ACC.ordinal()] );
        skimMatrices[SkimType.AUX.ordinal()] = new Matrix( nameQualifier + "aux", descQualifier + " other walk time skims", zeroBasedFloatArrays[SkimType.AUX.ordinal()] );
        skimMatrices[SkimType.EGR.ordinal()] = new Matrix( nameQualifier + "egr", descQualifier + " egress walk time skims", zeroBasedFloatArrays[SkimType.EGR.ordinal()] );
        skimMatrices[SkimType.HSR$.ordinal()] = new Matrix( nameQualifier + "hsr$", descQualifier + " hsr fare skims", zeroBasedFloatArrays[SkimType.HSR$.ordinal()] );
        skimMatrices[SkimType.AIR$.ordinal()] = new Matrix( nameQualifier + "air$", descQualifier + " air fare skims", zeroBasedFloatArrays[SkimType.AIR$.ordinal()] );
        skimMatrices[SkimType.RAIL$.ordinal()] = new Matrix( nameQualifier + "rail$", descQualifier + " ic rail fare skims", zeroBasedFloatArrays[SkimType.RAIL$.ordinal()] );
        skimMatrices[SkimType.BUS$.ordinal()] = new Matrix( nameQualifier + "bus$", descQualifier + " ic bus fare skims", zeroBasedFloatArrays[SkimType.BUS$.ordinal()] );
        skimMatrices[SkimType.TRAN$.ordinal()] = new Matrix( nameQualifier + "tran$", descQualifier + " local transit fare skims", zeroBasedFloatArrays[SkimType.TRAN$.ordinal()] );
        
        for (int k=0; k < OptimalStrategy.NUM_SKIMS; k++)
            skimMatrices[k].setExternalNumbers( alphaExternalNumbers );
        
    }
    
    
    private void  saveTransitBoardings ( NetworkHandlerIF nh, String accessMode, String routeType, double[] transitBoardings ) {
        
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
                
                if ( savedBoardings.containsKey(rteName) )
                    savedInfo = (SavedRouteInfo)savedBoardings.get(rteName);
                else
                    savedInfo = new SavedRouteInfo(tr.getDescription(rte), tr.getMode(rte), tr.getRouteType(rte) );
                
                index = 2*routeTypeIndex + accessIndex;
                savedInfo.boardings[index] += transitBoardings[rte];
                
                savedBoardings.put(rteName, savedInfo);
            }

        }
        else {
            logger.error ("error trying to save boardings - invalid routeType = " + routeType + " or accessMode = " + accessMode );
            System.exit(-1);
        }
        
    }

    
    
    private String getPeriodIdentifier ( String period ) {
        if ( period.equalsIgnoreCase( "peak" ) )
            return "pk";
        else if ( period.equalsIgnoreCase( "offpeak" ) )
            return "op";
        else
            return "nul";
    }

    private String getAccessIdentifier ( String period ) {
        if ( period.equalsIgnoreCase( "walk" ) )
            return "w";
        else if ( period.equalsIgnoreCase( "drive" ) )
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
        
        if ( period.equalsIgnoreCase("peak") || period.equalsIgnoreCase("offpeak") )
            // air is not allowed for walk
            if ( accessMode.equalsIgnoreCase("walk") )
                if ( rteType.equalsIgnoreCase("hsr") || rteType.equalsIgnoreCase("intercity") || rteType.equalsIgnoreCase("intracity") )
                    fails = false;
            // intracity is not allowed for drive ldt
            else if ( accessMode.equalsIgnoreCase("driveLdt") )
                if ( rteType.equalsIgnoreCase("air") || rteType.equalsIgnoreCase("hsr") || rteType.equalsIgnoreCase("intercity") )
                    fails = false;
            // intracity is only allowed for drive
            else if ( accessMode.equalsIgnoreCase("drive") )
                if ( rteType.equalsIgnoreCase("intracity") )
                    fails = false;

        if ( fails )
            invalidArgs ( period, accessMode, rteType );

    }

    
    private void invalidArgs ( String period, String accessMode, String routeType ) {
        logger.error ( "Skims cannot be built for the combination of arguments specified:");
        logger.error ( String.format( "period=%s, accessMode=%s, routeType=%s", period, accessMode, routeType) );
        throw new RuntimeException();
    }
        

    
    private float[][] getZeroBasedFloatArray ( double[][] zeroBasedDoubleArray ) {

        int[] skimsInternalToExternal = indexNode;

        // convert the zero-based double[alphas+externals][alphas+externals] produced by the skimming procedure, with network centroid/zone index mapping
        // to a zero-based float[alphas+externals][alphas+externals] with indexZone mapping to be written to skims file.
        float[][] zeroBasedFloatArray = new float[nh.getNumCentroids()][nh.getNumCentroids()];
        
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


    
    private void initSkimMatrices ( String zoneCorrespondenceFile ) {

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

        
        fareZones = new HashMap();
        for (int i=0; i < alphaNumberArray.length; i++ ) {
            fareZones.put(alphaNumberArray[i], fareZoneLabels[i]);
        }
        
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
        HSR$,
        AIR$,
        RAIL$,
        BUS$,
        TRAN$
    }

}

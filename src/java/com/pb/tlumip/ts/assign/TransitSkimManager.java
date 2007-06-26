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


import com.pb.tlumip.ts.NetworkHandlerIF;
import com.pb.tlumip.ts.transit.OptimalStrategy;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.io.File;
import org.apache.log4j.Logger;



public class TransitSkimManager {

	protected static Logger logger = Logger.getLogger(TransitSkimManager.class);

    static final boolean CREATE_NEW_NETWORK = true;
	
	
    NetworkHandlerIF nh = null;
	
	HashMap tsPropertyMap = null;
    HashMap globalPropertyMap = null;
	
    ResourceBundle appRb = null;
    ResourceBundle globalRb = null;
    
    String ivtString;
    String fwtString;
    String twtString;
    String accString;
    String egrString;
    String auxString;
    String brdString;
    String farString;
    String frqString;
    
    HashMap skimTablesOrder = null;
    
	
    public TransitSkimManager(NetworkHandlerIF nh, ResourceBundle appRb, ResourceBundle globalRb) {

        // get the items in the properties file
        tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap ( appRb );
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap ( globalRb );

        this.nh = nh;
        this.appRb = appRb;
        this.globalRb = globalRb;
        
        ivtString = (String)tsPropertyMap.get("invehtime.identifier");
        fwtString = (String)tsPropertyMap.get("firstwait.identifier");
        twtString = (String)tsPropertyMap.get("totalwait.identifier");
        accString = (String)tsPropertyMap.get("accesswalk.identifier");
        egrString = (String)tsPropertyMap.get("egresswalk.identifier");
        auxString = (String)tsPropertyMap.get("otherwalk.identifier");
        brdString = (String)tsPropertyMap.get("boardings.identifier");
        farString = (String)tsPropertyMap.get("fare.identifier");
        frqString = (String)tsPropertyMap.get("freq.identifier");

        skimTablesOrder = new HashMap();
        skimTablesOrder.put(ivtString, 0);
        skimTablesOrder.put(fwtString, 1);
        skimTablesOrder.put(twtString, 2);
        skimTablesOrder.put(accString, 3);
        skimTablesOrder.put(egrString, 4);
        skimTablesOrder.put(auxString, 5);
        skimTablesOrder.put(brdString, 6);
        skimTablesOrder.put(farString, 7);
        skimTablesOrder.put(frqString, 8);
        
    }    
    

    
    public int setupTransitNetwork ( NetworkHandlerIF nh, String period, String accessMode, String routeType ) {
        
        // get some information from the properties file
        String transitRoutesDirectory = (String)tsPropertyMap.get("transitRoutes.directory");
        int maxRoutes = Integer.parseInt ( (String)tsPropertyMap.get("MAX_TRANSIT_ROUTES") );

        // construct a network listing output filename for the transit network being setup
        String listingName = (String)tsPropertyMap.get("transitNetworkListings.directory") + period + "_" + accessMode + "_" + routeType + ".listing";
        

        // construct a list of d211 files to combine and convert to an array
        ArrayList d221FileList = new ArrayList();
        ArrayList routeTypeList = new ArrayList();
        if ( period.equalsIgnoreCase("peak") ) {
            d221FileList.add( transitRoutesDirectory + (String)tsPropertyMap.get("intracity.pk.fileName") );
            routeTypeList.add( "intracity" );
            d221FileList.add( transitRoutesDirectory + (String)tsPropertyMap.get("intercity.pk.fileName") );
            routeTypeList.add( "intercity" );
            d221FileList.add( transitRoutesDirectory + (String)tsPropertyMap.get("hsr.pk.fileName") );
            routeTypeList.add( "hsr" );
        }
        else if ( period.equalsIgnoreCase("offpeak") ) {
            d221FileList.add( transitRoutesDirectory + (String)tsPropertyMap.get("intracity.op.fileName") );
            routeTypeList.add( "intracity" );
            d221FileList.add( transitRoutesDirectory + (String)tsPropertyMap.get("intercity.op.fileName") );
            routeTypeList.add( "intercity" );
            d221FileList.add( transitRoutesDirectory + (String)tsPropertyMap.get("hsr.op.fileName") );
            routeTypeList.add( "hsr" );
        }
        else {
            invalidArgs ( period, accessMode, routeType );
        }

        
        // use this if transit network is to be built with combined route files
        String[] d221Files = new String[d221FileList.size()];
        String[] rteTypes = new String[routeTypeList.size()];
        for (int i=0; i < d221Files.length; i++) {
            d221Files[i] = (String)d221FileList.get(i);
            rteTypes[i] = (String)routeTypeList.get(i);
        }
        

        // use this if transit network is to be built with single route file
        String[] d221File = new String[1];
        String[] rteType = new String[1];

        // set this to whichever string was determined by combination of arguments
        String[] routeFiles = null;
        String[] routeTypes = null;
        
        
        
        // determine routes to used based on method argument values
        if ( accessMode.equalsIgnoreCase("walk")) {

            if ( routeType.equalsIgnoreCase("air") ) {
                invalidArgs ( period, accessMode, routeType );
            }
            else if ( routeType.equalsIgnoreCase("hsr") ) {
                routeFiles = d221Files;
                routeTypes = rteTypes;
            }
            else if ( routeType.equalsIgnoreCase("intercity") ) {
                routeFiles = d221Files;
                routeTypes = rteTypes;
            }
            else if ( routeType.equalsIgnoreCase("intracity") ) {
                routeFiles = d221Files;
                routeTypes = rteTypes;
            }
            else {
                invalidArgs ( period, accessMode, routeType );
            }
            
        }        
        else if ( accessMode.equalsIgnoreCase("drive")) {
            
            if ( routeType.equalsIgnoreCase("air") ) {
                accessMode = "driveLdt";
                if ( period.equalsIgnoreCase("peak") ) {
                    d221File[0] = transitRoutesDirectory + (String)tsPropertyMap.get("air.pk.fileName");
                    rteType[0] = "air";
                }
                else if ( period.equalsIgnoreCase("offpeak") ) {
                    d221File[0] = transitRoutesDirectory + (String)tsPropertyMap.get("air.op.fileName");
                    rteType[0] = "air";
                }
                else {
                    invalidArgs ( period, accessMode, routeType );
                }
                routeFiles = d221File;
            }
            else if ( routeType.equalsIgnoreCase("hsr") ) {
                accessMode = "driveLdt";
                if ( period.equalsIgnoreCase("peak") ) {
                    d221File[0] = transitRoutesDirectory + (String)tsPropertyMap.get("hsr.pk.fileName");
                    rteType[0] = "hsr";
                }
                else if ( period.equalsIgnoreCase("offpeak") ) {
                    d221File[0] = transitRoutesDirectory + (String)tsPropertyMap.get("hsr.op.fileName");
                    rteType[0] = "hsr";
                }
                else {
                    invalidArgs ( period, accessMode, routeType );
                }
                routeFiles = d221File;
            }
            else if ( routeType.equalsIgnoreCase("intercity") ) {
                accessMode = "driveLdt";
                if ( period.equalsIgnoreCase("peak") ) {
                    d221File[0] = transitRoutesDirectory + (String)tsPropertyMap.get("intercity.pk.fileName");
                    rteType[0] = "intercity";
                }
                else if ( period.equalsIgnoreCase("offpeak") ) {
                    d221File[0] = transitRoutesDirectory + (String)tsPropertyMap.get("intercity.op.fileName");
                    rteType[0] = "intercity";
                }
                else {
                    invalidArgs ( period, accessMode, routeType );
                }
                routeFiles = d221File;
            }
            else if ( routeType.equalsIgnoreCase("intracity") ) {
                routeFiles = d221Files;
                routeTypes = rteTypes;
            }
            else {
                invalidArgs ( period, accessMode, routeType );
            }

        }
           

        // check that all the route files to be used exist and can be read.
        for ( int i=0; i < routeFiles.length; i++ ) {
            try {
                if ( routeFiles[i] == null ) {
                    throw new RuntimeException();
                }
                else {
                    File check = new File(routeFiles[i]);
                    if ( ! check.canRead() )
                        throw new RuntimeException();
                }
            }
            catch ( RuntimeException e ) {
                logger.error ( "Error while checking existence of route files specified for:");
                logger.error ( String.format( "period=%s, accessMode=%s, routeType=%s", period, accessMode, routeType) );
                logger.error ( "", e );
                System.exit(-1);
            }
        }

        
        
        return nh.setupTransitNetworkObject ( period, accessMode, listingName, routeFiles, routeTypes, maxRoutes );
        
    }

    

    private void invalidArgs ( String period, String accessMode, String routeType ) {
        
        logger.error ( "Skims cannot be built for the combination of arguments specified:");
        logger.error ( String.format( "period=%s, accessMode=%s, routeType=%s", period, accessMode, routeType) );
        System.exit(-1);
        
    }
        
    
    /*
     * write a set of zip fomrmat transit skim matrices to the file names determined by the set of arguments.
     */
    public void writeTransitSkims ( String period, String accessMode, String routeType ) {
        
        // get the filenames for the period, accessMode, and type of routes (hsr, air, intercity, or intracity)
        String[] skimFileNames = getTransitSkimsFileNames( period, accessMode, routeType );

        
        // generate a set of output zip format peak walk transit skims
        Matrix[] skims = getTransitSkims ( period, accessMode );
        
        if ( skims.length > skimFileNames.length ) {
            logger.error( "fewer skims filenames were generated than skim tables were produced." );
            logger.error( skims.length + " " + period + " " + accessMode + " " + routeType + " skim tables were created, but only " + skimFileNames.length + " filenames were generated:" );
            for (int i=0; i < skimFileNames.length; i++)
                logger.error ( "    " + skimFileNames[i] );
            System.exit(-9);
        }
        else if ( skims.length < skimFileNames.length ) {
            logger.error( "more skims filenames were generated than skim tables were produced." );
            logger.error( skims.length + " " + period + " " + accessMode + " " + routeType + " skim tables were created, but " + skimFileNames.length + " filenames were generated:" );
            for (int i=0; i < skimFileNames.length; i++)
                logger.error ( "    " + skimFileNames[i] );
            System.exit(-9);
        }
        
        
        writeZipTransitSkims( skimFileNames, skims );
        logger.info ("done writing " + period + " " + accessMode + " " + routeType + " transit skims files.");
        
    }
    
    
    
    
    
    /*
     * construct the skim filenames from the directory and skim identifiers specified in the properties file and the arguments passed in.
     * 
     */
    private String[] getTransitSkimsFileNames ( String period, String accessMode, String routeType ) {

        String firstPart = (String)tsPropertyMap.get("transitSkims.directory") + period + accessMode + routeType;
        
        String[] skimFileNames = new String[9];
        skimFileNames[0] = firstPart + ivtString + ".zip";
        skimFileNames[1] = firstPart + fwtString + ".zip";
        skimFileNames[2] = firstPart + twtString + ".zip";
        skimFileNames[3] = firstPart + accString + ".zip";
        skimFileNames[4] = firstPart + egrString + ".zip";
        skimFileNames[5] = firstPart + auxString + ".zip";
        skimFileNames[6] = firstPart + brdString + ".zip";
        skimFileNames[7] = firstPart + farString + ".zip";
        skimFileNames[8] = firstPart + frqString + ".zip";
        

        return skimFileNames;
        
    }
    
    
    
    private void writeZipTransitSkims ( String[] transitSkimFileNames, Matrix[] skims ) {
        
        for (int i=0; i < transitSkimFileNames.length; i++) {
            MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP, new File(transitSkimFileNames[i]));
            mw.writeMatrix( skims[i] );
        }
        
    }
        
	private Matrix[] getTransitSkims ( String period, String accessMode ) {
        
		
		// create an optimal strategy object for this highway and transit network
		OptimalStrategy os = new OptimalStrategy( nh );

        os.initSkimMatrices ( (String)globalPropertyMap.get("alpha2beta.file"), skimTablesOrder );
        
        os.computeOptimalStrategySkimMatrices();
        
        Matrix[] transitSkims = os.getSkimMatrices();
		
		return transitSkims;
        
	}
    

}

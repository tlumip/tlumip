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


import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.NetworkHandlerIF;
import com.pb.tlumip.ts.transit.AuxTrNet;
import com.pb.tlumip.ts.transit.OptimalStrategy;
import com.pb.tlumip.ts.transit.TrRoute;
import com.pb.common.datafile.DataReader;
import com.pb.common.datafile.DataWriter;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.io.File;
import java.text.DateFormat;
import org.apache.log4j.Logger;



public class TransitSkimManager {

	protected static Logger logger = Logger.getLogger(TransitSkimManager.class);

    static final boolean CREATE_NEW_NETWORK = true;
	
	
    AuxTrNet ag = null;
	
	HashMap tsPropertyMap = null;
    HashMap globalPropertyMap = null;
	
	
	
    public TransitSkimManager( String period, String walkAccessMode ) {

        // default constructor used if object is created from an object that did not run an assignment first and dose not have a loaded highway network.
        
        
        // get the items in the properties file
        ResourceBundle appRb = ResourceUtil.getPropertyBundle( new File( "ts" ) );
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle( new File( "global" ) );
        
        tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

        
        // generate a NetworkHandler object to use for skimming
        NetworkHandlerIF nh = NetworkHandler.getInstance();
        setupNetwork( nh, tsPropertyMap, globalPropertyMap, period );
        logger.info ("TransitSkimManager created " + period + " Highway NetworkHandler object: " + nh.getNodeCount() + " highway nodes, " + nh.getLinkCount() + " highway links." );
        
        // generate a transit network using the highway network just created
        ag = getTransitNetwork( nh, period, walkAccessMode );
        logger.info ("\nTransitSkimManager created  " + period + " " + walkAccessMode + " transit network.");
        
    }
    
    
    private void setupNetwork ( NetworkHandlerIF nh, HashMap appMap, HashMap globalMap, String timePeriod ) {
        
        String networkFileName = (String)appMap.get("d211.fileName");
        String networkDiskObjectFileName = (String)appMap.get("NetworkDiskObject.file");
        
        String turnTableFileName = (String)appMap.get( "d231.fileName" );
        String networkModsFileName = (String)appMap.get( "d211Mods.fileName" );
        
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
        
        propertyValues[NetworkHandlerIF.NETWORK_FILENAME_INDEX] = networkFileName;
        propertyValues[NetworkHandlerIF.NETWORK_DISKOBJECT_FILENAME_INDEX] = networkDiskObjectFileName;
        propertyValues[NetworkHandlerIF.VDF_FILENAME_INDEX] = vdfFileName;
        propertyValues[NetworkHandlerIF.VDF_INTEGRAL_FILENAME_INDEX] = vdfIntegralFileName;
        propertyValues[NetworkHandlerIF.ALPHA2BETA_FILENAME_INDEX] = a2bFileName;
        propertyValues[NetworkHandlerIF.TURNTABLE_FILENAME_INDEX] = turnTableFileName;
        propertyValues[NetworkHandlerIF.NETWORKMODS_FILENAME_INDEX] = networkModsFileName;
        propertyValues[NetworkHandlerIF.VOLUME_FACTOR_INDEX] = volumeFactor;
        propertyValues[NetworkHandlerIF.USER_CLASSES_STRING_INDEX] = userClassesString;
        propertyValues[NetworkHandlerIF.TRUCKCLASS1_STRING_INDEX] = truckClass1String;
        propertyValues[NetworkHandlerIF.TRUCKCLASS2_STRING_INDEX] = truckClass2String;
        propertyValues[NetworkHandlerIF.TRUCKCLASS3_STRING_INDEX] = truckClass3String;
        propertyValues[NetworkHandlerIF.TRUCKCLASS4_STRING_INDEX] = truckClass4String;
        propertyValues[NetworkHandlerIF.TRUCKCLASS5_STRING_INDEX] = truckClass5String;
        propertyValues[NetworkHandlerIF.WALK_SPEED_INDEX] = walkSpeed;
        
        nh.buildNetworkObject ( timePeriod, propertyValues );
        
    }
    
    
    public TransitSkimManager(AuxTrNet ag, ResourceBundle appRb, ResourceBundle globalRb) {

        // get the items in the properties file
        tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap ( appRb );
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap ( globalRb );

        this.ag = ag;
    }
    
    
	/*
	 * return a peak period walk-transit skims Matrix[] including the following elements:
	 * 
	 * 0: in-vehicle time Matrix
	 * 1: first wait Matrix
	 * 2: total wait Matrix
     * 3: access time Matrix
     * 4: walk time Matrix
	 * 5: boardings Matrix
     * 6: cost Matrix
     * 7: high-speed/Amtrak rail ivt Matrix
	 */
	public Matrix[] getPeakWalkTransitSkims () {
		return getTransitSkims ( "peak", "Walk" );
	}
	
	
	/*
	 * write a set of peak period walk-transit skims in zip format to the
	 * files specified in the properties file.
	 * 
	 */
	public void writePeakWalkTransitSkims () {

		String variableString;
		ArrayList variableList;
		StringTokenizer st;
		
		// get the filenames for the peak walk transit output skims files
		variableString = (String)tsPropertyMap.get("pkWtSkim.fileNames");
		variableList = new ArrayList();
		st = new StringTokenizer(variableString, ", |");
		while (st.hasMoreTokens()) {
			variableList.add(st.nextElement());
		}
		String[] skimFileNames = new String[variableList.size()];
		for (int i=0; i < skimFileNames.length; i++)
		    skimFileNames[i] = (String)variableList.get(i);

		writeZipTransitSkims( "peak", "Walk", skimFileNames );
	}
	
	
	
	/*
	 * return a peak period drive-transit skims Matrix[] including the following elements:
	 * 
	 * 0: in-vehicle time Matrix
	 * 1: first wait Matrix
	 * 2: total wait Matrix
	 * 3: access time Matrix
	 * 4: boardings Matrix
	 * 5: cost Matrix
	 */
	public Matrix[] getPeakDriveTransitSkims () {
		return getTransitSkims ( "peak", "Drive" );
	}
	
	
	/*
	 * write a set of peak period walk-transit skims in zip format to the
	 * files specified in the properties file.
	 * 
	 */
	public void writePeakDriveTransitSkims () {

		String variableString;
		ArrayList variableList;
		StringTokenizer st;
		
		// get the filenames for the peak walk transit output skims files
		variableString = (String)tsPropertyMap.get("pkDtSkim.fileNames");
		variableList = new ArrayList();
		st = new StringTokenizer(variableString, ", |");
		while (st.hasMoreTokens()) {
			variableList.add(st.nextElement());
		}
		String[] skimFileNames = new String[variableList.size()];
		for (int i=0; i < skimFileNames.length; i++)
			skimFileNames[i] = (String)variableList.get(i);

		writeZipTransitSkims( "peak", "Drive", skimFileNames );
	}
	
	
	
	/*
	 * return a offpeak period walk-transit skims Matrix[] including the following elements:
	 * 
	 * 0: in-vehicle time Matrix
	 * 1: first wait Matrix
	 * 2: total wait Matrix
	 * 3: access time Matrix
	 * 4: boardings Matrix
	 * 5: cost Matrix
	 */
	public Matrix[] getOffPeakWalkTransitSkims () {
		return getTransitSkims ( "offpeak", "Walk" );
	}
	
	
	/*
	 * write a set of offpeak period walk-transit skims in zip format to the
	 * files specified in the properties file.
	 * 
	 */
	public void writeOffPeakWalkTransitSkims () {

		String variableString;
		ArrayList variableList;
		StringTokenizer st;
		
		// get the filenames for the offpeak walk transit output skims files
		variableString = (String)tsPropertyMap.get("opWtSkim.fileNames");
		variableList = new ArrayList();
		st = new StringTokenizer(variableString, ", |");
		while (st.hasMoreTokens()) {
			variableList.add(st.nextElement());
		}
		String[] skimFileNames = new String[variableList.size()];
		for (int i=0; i < skimFileNames.length; i++)
		skimFileNames[i] = (String)variableList.get(i);

		writeZipTransitSkims( "offpeak", "Walk", skimFileNames );
	}
	
	
	
	/*
	 * return a offpeak period drive-transit skims Matrix[] including the following elements:
	 * 
	 * 0: in-vehicle time Matrix
	 * 1: first wait Matrix
	 * 2: total wait Matrix
	 * 3: access time Matrix
	 * 4: boardings Matrix
	 * 5: cost Matrix
	 */
	public Matrix[] getOffPeakDriveTransitSkims () {
		return getTransitSkims ( "offpeak", "Drive" );
	}
	
	
	/*
	 * write a set of offpeak period walk-transit skims in zip format to the
	 * files specified in the properties file.
	 * 
	 */
	public void writeOffPeakDriveTransitSkims () {

		String variableString;
		ArrayList variableList;
		StringTokenizer st;
		
		// get the filenames for the offpeak walk transit output skims files
		variableString = (String)tsPropertyMap.get("opDtSkim.fileNames");
		variableList = new ArrayList();
		st = new StringTokenizer(variableString, ", |");
		while (st.hasMoreTokens()) {
			variableList.add(st.nextElement());
		}
		String[] skimFileNames = new String[variableList.size()];
		for (int i=0; i < skimFileNames.length; i++)
			skimFileNames[i] = (String)variableList.get(i);

		writeZipTransitSkims( "offpeak", "Drive", skimFileNames );
	}
	
	
	
    private void writeZipTransitSkims ( String period, String accessMode, String[] transitSkimFileNames ) {
        
        // generate a set of output zip format peak walk transit skims
        Matrix[] skims = getTransitSkims ( period, accessMode );
        for (int i=0; i < skims.length; i++) {
            MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP, new File(transitSkimFileNames[i]));
            mw.writeMatrix( skims[i] );
        }
        
    }
        
	private Matrix[] getTransitSkims ( String period, String accessMode ) {
        
		
		// create an optimal strategy object for this highway and transit network
		OptimalStrategy os = new OptimalStrategy( ag );

        os.initSkimMatrices ( (String)globalPropertyMap.get("alpha2beta.file") );
        
		Matrix[] transitSkims = os.getOptimalStrategySkimMatrices();
		
		String myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done with getTransitSkims(): " + myDateString);

		return transitSkims;
        
	}
    

    private AuxTrNet getTransitNetwork( NetworkHandlerIF nh, String period, String accessMode ) {
        
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
        
        logger.info (nh.getLinkCount() + " highway links");
		logger.info (nh.getNodeCount() + " highway nodes");


		// get the filenames for the peak and off-peak route files
		String d221PeakFile = (String) tsPropertyMap.get( "d221.pk.fileName" );
		String d221OffPeakFile = (String) tsPropertyMap.get( "d221.op.fileName" );

		
		// read parameter for maximum number of transit routes
		int maxRoutes = Integer.parseInt ( (String)tsPropertyMap.get("MAX_TRANSIT_ROUTES") );

		
		
		// create transit routes object with max 50 routes
		TrRoute tr = new TrRoute (maxRoutes);

		//read transit route info from Emme/2 for d221 file for the specified time period
	    tr.readTransitRoutes ( period.equalsIgnoreCase("peak") ? d221PeakFile : d221OffPeakFile );
		    

		// associate transit segment node sequence with highway link indices
		tr.getLinkIndices (nh);



		// create an auxilliary transit network object
		ag = new AuxTrNet( nh, tr);

		
		// build the auxilliary links for the given transit routes object
		ag.buildAuxTrNet ( accessMode );
		
		
		// define the forward star index arrays, first by anode then by bnode
		ag.setForwardStarArrays ();
		ag.setBackwardStarArrays ();


		String myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done creating transit network AuxTrNetTest: " + myDateString);

		return ag;
	}
    
	
	
}

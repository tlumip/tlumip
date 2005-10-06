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


import com.pb.tlumip.ts.assign.Network;
import com.pb.tlumip.ts.transit.AuxTrNet;
import com.pb.tlumip.ts.transit.OpStrategy;
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
import java.util.StringTokenizer;
import java.io.File;
import java.text.DateFormat;
import org.apache.log4j.Logger;



public class TransitSkimManager {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.assign");

	
	static final boolean CREATE_NEW_NETWORK = true;
	
	
	public static final String AUX_TRANSIT_NETWORK_LISTING = "c:\\jim\\tlumip\\aux_transit_net.listing";

	float peakFactor;
	float offPeakFactor;

	AuxTrNet ag = null;	
	
	HashMap tsPropertyMap = null;
    HashMap globalPropertyMap = null;


	int MAX_ROUTES;
	
	
	
	public TransitSkimManager() {

		// get the items in the properties file
		tsPropertyMap = ResourceUtil.getResourceBundleAsHashMap ("ts" );
        globalPropertyMap = ResourceUtil.getResourceBundleAsHashMap ("global" );

		peakFactor = Float.parseFloat( (String)globalPropertyMap.get("AM_PEAK_VOL_FACTOR") );
		offPeakFactor = Float.parseFloat( (String)globalPropertyMap.get("OFF_PEAK_VOL_FACTOR") );

	}
    
	
	
	/*
	 * return a peak period walk-transit skims Matrix[] including the following elements:
	 * 
	 * 0: in-vehicle time Matrix
	 * 1: first wait Matrix
	 * 2: total wait Matrix
	 * 3: access time Matrix
	 * 4: boardings Matrix
	 * 5: cost Matrix
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
	
	
	
	private Matrix[] getTransitSkims ( String period, String accessMode ) {
        
		
		String diskObjectFileName = null;
		
		// create a new transit network from d211 highway network file and d221 transit routes file, or read it from DiskObject.
		String key = period + accessMode + "TransitNetwork";
		String path = (String) tsPropertyMap.get( "diskObject.pathName" );
		if ( path.endsWith("/") || path.endsWith("\\") )
		    diskObjectFileName = path + key + ".diskObject";
		else
		    diskObjectFileName = path + "/" + key + ".diskObject";
		if ( CREATE_NEW_NETWORK ) {
			ag = createTransitNetwork ( period, accessMode );
			DataWriter.writeDiskObject ( ag, diskObjectFileName, key );
		}
		else {
			ag = (AuxTrNet) DataReader.readDiskObject ( diskObjectFileName, key );
		}

		

		
		
		// create an optimal strategy object for this highway and transit network
		OpStrategy os = new OpStrategy( ag );

		// generate the walk transit skims to zone 1 and print values in tabular report
		int dest = 1;
		if ( os.buildStrategy( dest ) >= 0 ) {
		    
			// compute skims for this O/D pair for use in stop/station choice
			os.initSkims();
			os.wtSkimsFromDest();

//			for (int i=0; i < ag.getHighwayNodeCount(); i++)
//				os.getOptimalStrategySkimsFromOrig(i);


			os.printTransitSkimsTo ( dest );
			
		}

	
		Matrix[] transitSkims = os.getOptimalStrategySkimMatrices();
		
		
        
		String myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done with getTransitSkims(): " + myDateString);


		return transitSkims;
	}
    

	
	private AuxTrNet createTransitNetwork ( String period, String accessMode ) {
        
		long totalTime = 0;
		long startTime = System.currentTimeMillis();
		
		float volumeFactor;
		
		if ( period.compareToIgnoreCase("peak") == 1 )
			volumeFactor = peakFactor;
		else
			volumeFactor = offPeakFactor;

		// create a highway network oject
		Network g = new Network( tsPropertyMap, globalPropertyMap, period );
		logger.info (g.getLinkCount() + " highway links");
		logger.info (g.getNodeCount() + " highway nodes");


		// get the filenames for the peak and off-peak route files
		String d221PeakFile = (String) tsPropertyMap.get( "d221.pk.fileName" );
		String d221OffPeakFile = (String) tsPropertyMap.get( "d221.op.fileName" );

		
		// read parameter for maximum number of transit routes
		MAX_ROUTES = Integer.parseInt ( (String)tsPropertyMap.get("MAX_TRANSIT_ROUTES") );

		
		
		// create transit routes object with max 50 routes
		TrRoute tr = new TrRoute (MAX_ROUTES);

		//read transit route info from Emme/2 for d221 file for the specified time period
	    tr.readTransitRoutes ( period.equalsIgnoreCase("peak") ? d221PeakFile : d221OffPeakFile );
		    

		// associate transit segment node sequence with highway link indices
		tr.getLinkIndices (g);



		// create an auxilliary transit network object
		ag = new AuxTrNet(g.getLinkCount() + 3*tr.getTotalLinkCount() + 2*MAX_ROUTES, g, tr);

		
		// build the auxilliary links for the given transit routes object
		ag.buildAuxTrNet ( accessMode );
		
		
		// define the forward star index arrays, first by anode then by bnode
		ag.setForwardStarArrays ();
		ag.setBackwardStarArrays ();


		String myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done creating transit network AuxTrNetTest: " + myDateString);

		return ag;
	}
    
	
	
	private void writeZipTransitSkims ( String period, String accessMode, String[] transitSkimFileNames ) {
	    
		// generate a set of output zip format peak walk transit skims
		Matrix[] skims = getTransitSkims ( period, accessMode );
		for (int i=0; i < skims.length; i++) {
			MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP, new File(transitSkimFileNames[i]));
			mw.writeMatrix( skims[i] );
		}
		
	}
		
	
}

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

package com.pb.tlumip.ts.assign.tests;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 5/7/2004
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

//import com.pb.common.util.ResourceUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.io.File;
import java.text.DateFormat;

import org.apache.log4j.Logger;



public class AuxTrNetTest {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.assign.tests");

	
	static final boolean CREATE_NEW_NETWORK = true;
	static final int START_NODE = 24944;
	static final int END_NODE = 10;
	
	
	public static final String AUX_TRANSIT_NETWORK_LISTING = "c:\\jim\\tlumip\\aux_transit_net.listing";

//	public static final int PK_WT_IVT = 0;
//	public static final int PK_WT_FWT = 1;
//	public static final int PK_WT_TWT = 2;
//	public static final int PK_WT_AUX = 3;
//	public static final int PK_WT_BRD = 4;
//	public static final int PK_WT_FAR = 5;
//	public static final int PK_WT_SKIMS = 6;
	
	AuxTrNet ag = null;	
	
	HashMap tsPropertyMap = null;
    HashMap globalPropertyMap = null;

	String d221PeakFile = null;
	String d221OffPeakFile = null;
	
	String[] PeakWalkTransitSkimFileNames = null;
	String[] PeakDriveTransitSkimFileNames = null;
	String[] OffPeakWalkTransitSkimFileNames = null;
	String[] OffPeakDriveTransitSkimFileNames = null;
	
	int MAX_ROUTES;
	
	
	
	public AuxTrNetTest( HashMap tsPropertyMap, HashMap globalPropertyMap ) {
		
		this.tsPropertyMap = tsPropertyMap;
        this.globalPropertyMap = globalPropertyMap;


		// get the filenames for the peak and off-peak route files
		d221PeakFile = (String) tsPropertyMap.get( "d221.pk.fileName" );
		d221OffPeakFile = (String) tsPropertyMap.get( "d221.op.fileName" );

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
		PeakWalkTransitSkimFileNames = new String[variableList.size()];
		for (int i=0; i < PeakWalkTransitSkimFileNames.length; i++)
			PeakWalkTransitSkimFileNames[i] = (String)variableList.get(i);

		// get the filenames for the peak drive transit output skims files
		variableString = (String)tsPropertyMap.get("pkDtSkim.fileNames");
		variableList.clear();
		st = new StringTokenizer(variableString, ", |");
		while (st.hasMoreTokens()) {
			variableList.add(st.nextElement());
		}
		PeakDriveTransitSkimFileNames = new String[variableList.size()];
		for (int i=0; i < PeakDriveTransitSkimFileNames.length; i++)
		PeakDriveTransitSkimFileNames[i] = (String)variableList.get(i);

		// get the filenames for the peak walk transit output skims files
		variableString = (String)tsPropertyMap.get("opWtSkim.fileNames");
		variableList.clear();
		st = new StringTokenizer(variableString, ", |");
		while (st.hasMoreTokens()) {
			variableList.add(st.nextElement());
		}
		OffPeakWalkTransitSkimFileNames = new String[variableList.size()];
		for (int i=0; i < OffPeakWalkTransitSkimFileNames.length; i++)
		OffPeakWalkTransitSkimFileNames[i] = (String)variableList.get(i);

		// get the filenames for the peak drive transit output skims files
		variableString = (String)tsPropertyMap.get("opDtSkim.fileNames");
		variableList.clear();
		st = new StringTokenizer(variableString, ", |");
		while (st.hasMoreTokens()) {
			variableList.add(st.nextElement());
		}
		OffPeakDriveTransitSkimFileNames = new String[variableList.size()];
		for (int i=0; i < OffPeakDriveTransitSkimFileNames.length; i++)
		OffPeakDriveTransitSkimFileNames[i] = (String)variableList.get(i);

		
		// read parameter for maximum number of transit routes
		MAX_ROUTES = Integer.parseInt ( (String)tsPropertyMap.get("MAX_TRANSIT_ROUTES") );

	}
    
	
	
    public static void main (String[] args) {

// Use to write out all transit skims files    	
//    	ResourceBundle rb = ResourceUtil.getPropertyBundle( new File("/jim/util/svn_workspace/projects/tlumip/config/Network.properties") );
//    	HashMap propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb); 
//
//		AuxTrNetTest test = new AuxTrNetTest(propertyMap);
//		test.runWriteFilesTest();
		
		
		
    	ResourceBundle rb = ResourceUtil.getPropertyBundle( new File("/jim/util/svn_workspace/projects/tlumip/config/ts.properties") );
    	HashMap propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

        ResourceBundle globalRb = ResourceUtil.getPropertyBundle( new File("/jim/util/svn_workspace/projects/tlumip/config/global.properties") );
    	HashMap globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

    	logger.info ("building transit network");
		AuxTrNetTest test = new AuxTrNetTest(propertyMap, globalPropertyMap);
		
		/*
		 * specify which period, accessmode and matrix element to view:
		 * 
		 * period: "peak" or "offpeak"
		 * accessMode: "walk" or "drive"
		 *
		 * 0: in-vehicle time Matrix
		 * 1: first wait Matrix
		 * 2: total wait Matrix
		 * 3: access time Matrix
		 * 4: boardings Matrix
		 * 5: cost Matrix
		 *  
		 */
		
    }

    
    
	private void runWriteFilesTest () {
	    
		long startTime = System.currentTimeMillis();

        // get off-peak period definitions from property file
		float peakFactor = Float.parseFloat( (String)globalPropertyMap.get("AM_PEAK_VOL_FACTOR") );
		float offPeakFactor = Float.parseFloat( (String)globalPropertyMap.get("OFF_PEAK_VOL_FACTOR") );

		writeZipTransitSkims( "peak", "Walk", peakFactor, PeakWalkTransitSkimFileNames );
		writeZipTransitSkims( "peak", "Drive", peakFactor, PeakDriveTransitSkimFileNames );
		writeZipTransitSkims( "offpeak", "Walk", offPeakFactor, OffPeakWalkTransitSkimFileNames );
		writeZipTransitSkims( "offpeak", "Drive", offPeakFactor, OffPeakDriveTransitSkimFileNames );
		
		logger.info("AuxTrNetTest.runTest() finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");

		
	}

	
	
	public void writeZipTransitSkims ( String period, String accessMode, float volumeFactor, String[] transitSkimFileNames ) {
	    
		// generate a set of output zip format peak walk transit skims
		Matrix[] skims = getTransitSkims ( period, accessMode, volumeFactor );
		for (int i=0; i < skims.length; i++) {
			writeZipMatrix( skims[i], transitSkimFileNames[i] );
		}
		
	}
		
	
	
	public Matrix[] getTransitSkims ( String period, String accessMode, float volumeFactor ) {
        
		
		String diskObjectFileName = null;
		
		// create a new transit network from d211 highway network file and d221 transit routes file, or read it from DiskObject.
		String key = period + accessMode + "TransitNetwork";
		String path = (String) tsPropertyMap.get( "diskObject.pathName" );
		if ( path.endsWith("/") || path.endsWith("\\") )
		    diskObjectFileName = path + key + ".diskObject";
		else
		    diskObjectFileName = path + "/" + key + ".diskObject";
		if ( CREATE_NEW_NETWORK ) {
			ag = createTransitNetwork ( period, accessMode, volumeFactor );
			DataWriter.writeDiskObject ( ag, diskObjectFileName, key );
		}
		else {
			ag = (AuxTrNet) DataReader.readDiskObject ( diskObjectFileName, key );
		}

		

		
		
		// create an optimal strategy object for this highway and transit network
		OpStrategy os = new OpStrategy( ag );

		int[] nodeIndex = ag.getHighwayNetwork().getNodeIndex();
		os.buildStrategy( nodeIndex[END_NODE] );
		os.getOptimalStrategyWtSkimsFromOrig( START_NODE, END_NODE );
		System.exit(1);
		
		
//		// generate the walk transit skims to zone 1 and print values in tabular report
//		int dest = 0;
//		if ( os.buildStrategy( dest ) >= 0 ) {
//		    
//			// compute skims for this O/D pair for use in stop/station choice
//			os.initSkims();
//			os.wtSkimsFromDest();
//
//			for (int i=0; i < ag.getHighwayNodeCount(); i++)
//				os.getOptimalStrategySkimsFromOrig(i);
//
//
//			os.printTransitSkimsTo ( dest );
//			
//		}
//
//		System.exit(1);
		
	
		Matrix[] transitSkims = os.getOptimalStrategySkimMatrices();
		
		
		
/*
		// load a walk transit trip on the O/D strategy
		os.loadWalkTransit(orig,dest);

		// print the node skims values for all links in this strategy assigned flow.
		System.out.println ("Loading a walk transit trip on the current transit strategy and dumping skims");
		System.out.println ("");
//		os.printTransitSkims();
	  os.printTransitSkimsTo (71020);
		System.out.println ("");
		System.out.println ("");
		System.out.println ("");
*/


/*
		// compute skims for this O/D pair for use in stop/station choice
		os.initSkims(dest);
		os.skimsFromDest();
		System.out.println ("done with skims toward the destination.");


		for (int i=1; i < g.getNodeCount(); i++)
		  os.driveTransitSkimsFromOrig(i);
		System.out.println ("done with skims from origin nodes.");

		// load a drive transit trip on the O/D strategy
		os.loadDriveTransit(orig);
		System.out.println ("loading.");

		// print the node skims values for all links in this strategy assigned flow.
		System.out.println ("Loading a drive transit trip on the current transit strategy and dumping skims");
		System.out.println ("");
		os.printTransitSkims();
*/

        
		String myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done with getTransitSkims(): " + myDateString);


		return transitSkims;
	}
    

	
	private AuxTrNet createTransitNetwork ( String period, String accessMode, float volumeFactor ) {
        
		long totalTime = 0;
		long startTime = System.currentTimeMillis();
		

		// create a highway network oject
		Network g = new Network( tsPropertyMap, globalPropertyMap, period, volumeFactor );
		logger.info (g.getLinkCount() + " highway links");
		logger.info (g.getNodeCount() + " highway nodes");


		// create transit routes object
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

//		ag.printAuxTrLinks (24, tr);
		ag.printAuxTranNetwork( AUX_TRANSIT_NETWORK_LISTING );
//		ag.printTransitNodePointers();

		String myDateString = DateFormat.getDateTimeInstance().format(new Date());
		logger.info ("done creating transit network AuxTrNetTest: " + myDateString);

		return ag;
	}
    
	
	
	/**
	 *  Write the matrix out to a new zip file
	 */
	public static void writeZipMatrix(Matrix m, String fileName) {
		MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP, new File(fileName));
		mw.writeMatrix( m );
	}


}

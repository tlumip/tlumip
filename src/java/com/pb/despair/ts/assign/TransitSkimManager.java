package com.pb.despair.ts.assign;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 7/1/2004
 */


import com.pb.despair.ts.assign.Network;
import com.pb.despair.ts.transit.AuxTrNet;
import com.pb.despair.ts.transit.OpStrategy;
import com.pb.despair.ts.transit.TrRoute;
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
import java.util.logging.Logger;



public class TransitSkimManager {

	protected static Logger logger = Logger.getLogger("com.pb.despair.ts.assign");

	
	static final boolean CREATE_NEW_NETWORK = true;
	
	
	public static final String AUX_TRANSIT_NETWORK_LISTING = "c:\\jim\\tlumip\\aux_transit_net.listing";

	AuxTrNet ag = null;	
	
	HashMap propertyMap = null;

	
	int MAX_ROUTES;
	
	
	
	public TransitSkimManager() {

		// get the items in the properties file
		propertyMap = ResourceUtil.getResourceBundleAsHashMap ("ts" );
        
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
		variableString = (String)propertyMap.get("pkWtSkim.fileNames");
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
		variableString = (String)propertyMap.get("pkDtSkim.fileNames");
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
		variableString = (String)propertyMap.get("opWtSkim.fileNames");
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
		variableString = (String)propertyMap.get("opDtSkim.fileNames");
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
		String path = (String) propertyMap.get( "diskObject.pathName" );
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
			os.skimsFromDest();

			for (int i=0; i < ag.getHighwayNodeCount(); i++)
				os.getOptimalStrategySkimsFromOrig(i);


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
		

		// create a highway network oject
		Network g = new Network( propertyMap, period );
		logger.info (g.getLinkCount() + " highway links");
		logger.info (g.getNodeCount() + " highway nodes");


		// get the filenames for the peak and off-peak route files
		String d221PeakFile = (String) propertyMap.get( "d221.pk.fileName" );
		String d221OffPeakFile = (String) propertyMap.get( "d221.op.fileName" );

		
		// read parameter for maximum number of transit routes
		MAX_ROUTES = Integer.parseInt ( (String)propertyMap.get("MAX_TRANSIT_ROUTES") );

		
		
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
package com.pb.despair.ts.assign;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import com.pb.common.calculator.LinkCalculator;
import com.pb.common.calculator.LinkFunction;
import com.pb.common.datafile.D211FileReader;
import com.pb.common.datafile.D231FileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.IndexSort;
import com.pb.despair.model.Halo;

/**
 * This Network class contains the link and node data tables and
 * index arrays for representing the network in forward star notation.
 *
 */

public class Network implements Serializable {

	protected static transient Logger logger = Logger.getLogger("com.pb.despair.ts.assign");

	int minCentroidLabel;
	int maxCentroidLabel;
	int NUM_AUTO_CLASSES;
	
	String assignmentPeriod;

	float volumeFactor;
	
	double WALK_SPEED;

	
	int numAlphazones;
	int maxCentroid;
	int numCentroids;
	int maxNode;

	boolean[][] validLink = null;
    int[] indexNode = null;
    int[] nodeIndex = null;
	int[] sortedLinkIndexA;
	int[] ipa;
	int[] ia;
	int[] ib;

	String d211File = null;
	String d231File = null;
	String d211ModsFile = null;
	String hwyVdfFile = null;
	String hwyVdfIntegralFile = null;

	TableDataSet nodeTable = null;
	TableDataSet linkTable = null;
	TableDataSet linkModsTable = null;
	TableDataSet derivedLinkTable = null;

	LinkFunction lf = null;
	LinkFunction lfi = null;
	LinkCalculator fdLc = null;
	LinkCalculator fpLc = null;
	LinkCalculator fdiLc = null;
	LinkCalculator fpiLc = null;

	float[][][] turnTable = null;

	HashMap propertyMap = null;
	
	
    public Network ( HashMap propertyMap, String period ) {

		float[][] turnDefs = null;

        this.propertyMap = propertyMap;
        
        
        // read the Network.properties file into a HashMap, and get the values.
		readPropertyFile(period);
		

		// read the node and link tables
		D211FileReader d211 = new D211FileReader();
		D231FileReader d231 = new D231FileReader();
		try {
			nodeTable = d211.readNodeTable( new File(d211File) );
			linkTable = d211.readLinkTable( new File(d211File) );
			
			if ( d211ModsFile != null )
			    linkModsTable = d211.readLinkTableMods( new File(d211ModsFile) );

			if ( d231File != null )
			    turnDefs = d231.readTurnTable( new File(d231File) );
		
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// read link function definitions files (delay functions and integrals functions)		
		lf = new LinkFunction ( (String)propertyMap.get("vdf.fileName"), "vdf");
		lfi = new LinkFunction ( (String)propertyMap.get("vdfIntegral.fileName"), "vdf");
		
		
		// set the internal numbering for nodes and their correspondence to external numbering.
		setInternalNodeNumbering ();

		// define the forward star index arrays, first by anode then by bnode
		ia = linkTable.getColumnAsInt( "ia" );
		ib = linkTable.getColumnAsInt( "ib" );
		ipa = setForwardStarArrays ();
		
		// calculate the derived link attributes for the network
		derivedLinkTable = deriveLinkAttributes();


		// merge the derived link attributes into the linkTable TableDataSet,
		// then we're done with the derived table.
		linkTable.merge ( derivedLinkTable );
		derivedLinkTable = null;
		
		
		// calculate the congested link travel times based on the vdf functions defined
		fdLc = new LinkCalculator ( linkTable, lf.getFunctionStrings( "fd" ), "vdf" );
		applyVdfs();
		logLinkTimeFreqs();
		


		// add turn defintions and turn penalty indices into linkTable
		if (turnDefs != null) {
			fpLc = new LinkCalculator ( linkTable, lf.getFunctionStrings( "fp" ), "turnIndex" );
			setTurnPenalties( turnDefs );
		}


		// define link calculators for use in computing objective function and lambda vales
		fdiLc = new LinkCalculator ( linkTable, lfi.getFunctionStrings( "fd" ), "vdf" );
		fpiLc = new LinkCalculator ( linkTable, lfi.getFunctionStrings( "fp" ), "turnIndex" );
		
		
		
    }


    
    
	public String getTimePeriod () {
		return assignmentPeriod;
	}

	public int getMaxCentroid () {
		return maxCentroid;
	}

	public int getNumCentroids () {
		return numCentroids;
	}

    public int getLinkCount () {
        return linkTable.getRowCount();
    }

    public int getNodeCount () {
        return nodeTable.getRowCount();
    }

	public double getWalkTime (float dist) {
		return(60.0*dist/WALK_SPEED);
	}

	public boolean isCentroid ( int node ) {
		return ( node >= minCentroidLabel && node <= maxCentroidLabel );
	}
	
	
	public int[] getIa () {
		return linkTable.getColumnAsInt( "ia" );
	}

	public int[] getIb () {
		return linkTable.getColumnAsInt( "ib" );
	}

	public int[] getIpa () {
		return ipa;
	}

	public int[] getSortedLinkIndexA () {
		return sortedLinkIndexA;
	}

	public int[] getIndexNode () {
		return indexNode;
	}

	public int[] getNodeIndex () {
		return nodeIndex;
	}

	public boolean[] getCentroid () {
		return linkTable.getColumnAsBoolean( "centroid" );
	}

	public double[] getCapacity () {
		return linkTable.getColumnAsDouble( "capacity" );
	}

	public double[] getCongestedTime () {
		return linkTable.getColumnAsDouble( "congestedTime" );
	}

	public float getSumOfVdfIntegrals () {
		return linkTable.getColumnTotal( linkTable.getColumnPosition("vdfIntegral") );
	}

	public double[] getFreeFlowTime () {
		return linkTable.getColumnAsDouble( "freeFlowTime" );
	}

	public double[] getDist () {
		return linkTable.getColumnAsDouble( "dist" );
	}

	public int[] getLinkType () {
		return linkTable.getColumnAsInt( "type" );
	}

	public String[] getMode () {
		return linkTable.getColumnAsString( "mode" );
	}

    public double[][] getFlows () {
         
        double[][] flows = new double[NUM_AUTO_CLASSES][];
         
        for (int m=0; m < NUM_AUTO_CLASSES; m++)
            flows[m] = linkTable.getColumnAsDouble( "flow_" + m );
        
        return flows;
    }
    
	public double[] getNodeX () {
		return nodeTable.getColumnAsDouble( "x" );
	}
    
	public double[] getNodeY () {
		return nodeTable.getColumnAsDouble( "y" );
	}
    
    
    public boolean[] getValidLink ( int userClass ) {
        return validLink[userClass];
    }

    public void setVolau ( double[] volau ) {
        linkTable.setColumnAsDouble( linkTable.getColumnPosition("volau"), volau );
    }

    public void setFlows (double[][] flow) {
        for (int m=0; m < flow.length; m++)
            linkTable.setColumnAsDouble( linkTable.getColumnPosition("flow_" + m), flow[m] );
    }
    

    private void readPropertyFile ( String period ) {
        
    	this.assignmentPeriod = period;
    	

    	// get the filename for the alpha/beta zone correspomdence file
		String zoneIndexFile = (String) propertyMap.get( "zoneIndex.fileName" );

		// create a Halo object for defining the extent of the study area
		Halo halo = new Halo( zoneIndexFile );
        
    	// get the filename for the network node and link table
		this.d211File = (String) propertyMap.get( "d211.fileName" );
		
		// get the filename for the turn table
		this.d231File = (String) propertyMap.get( "d231.fileName" );
		
		// get the filename for the network node and link modifications table
		this.d211ModsFile = (String) propertyMap.get( "d211Mods.fileName" );
		
		// get the filename for the highway link volume delay function definition file
		this.hwyVdfFile = (String) propertyMap.get(	"vdf.fileName" );
		
		// get the filename for the highway link volume delay function integrals definition file
		this.hwyVdfIntegralFile = (String) propertyMap.get(	"vdfIntegral.fileName" );
		
		// get network properties
		this.minCentroidLabel = 1;
		this.maxCentroidLabel = halo.getMaxZoneNumber();
		this.numAlphazones = halo.getNumberOfZones();
		this.NUM_AUTO_CLASSES = Integer.parseInt ( (String)propertyMap.get( "NUM_AUTO_CLASSES" ) );

		if ( (String)propertyMap.get( "WALK_SPEED" ) != null )
		    this.WALK_SPEED = Double.parseDouble ( (String)propertyMap.get( "WALK_SPEED" ) );

		
		if ( period == "peak" ) {
			if ( (String)propertyMap.get( "amPeak.volumeFactor" ) != null )
				this.volumeFactor = Float.parseFloat ( (String)propertyMap.get( "amPeak.volumeFactor" ) );
		}
		else {
			if ( (String)propertyMap.get( "offPeak.volumeFactor" ) != null )
				this.volumeFactor = Float.parseFloat ( (String)propertyMap.get( "offPeak.volumeFactor" ) );
		}
		
    }
    
	/**
	 * Use this method to get the largest node value
	 */
	private int getMaxNode () {

		maxNode = 0;
		for (int i=0; i < linkTable.getRowCount(); i++) {
	        
			int an = (int)linkTable.getValueAt( i+1, "anode" );
			int bn = (int)linkTable.getValueAt( i+1, "bnode" );
	        
			// set the value for the largest node number in the network
			if (an > maxNode)
				maxNode = an;
			if (bn > maxNode)
				maxNode = bn;

		}

		return maxNode;
		
	}


	
	/**
	 * Use this method to read the Emme2 d211 text file format network into
	 * a simple data table.
	 */
	private TableDataSet deriveLinkAttributes () {

		int[] turnPenaltyIndex = new int[linkTable.getRowCount()];
		float[] length = new float[linkTable.getRowCount()];
		double[] capacity = new double[linkTable.getRowCount()];
		double[] freeFlowSpeed = new double[linkTable.getRowCount()];
		double[] congestedTime = new double[linkTable.getRowCount()];
		double[] vdfIntegral = new double[linkTable.getRowCount()];
		double[] freeFlowTime = new double[linkTable.getRowCount()];
		double[] oldTime = new double[linkTable.getRowCount()];
		double[] volau = new double[linkTable.getRowCount()];
		double[][] flow = new double[NUM_AUTO_CLASSES][linkTable.getRowCount()];
		boolean[] centroid = new boolean[linkTable.getRowCount()];
		String[] centroidString = new String[linkTable.getRowCount()];
		validLink = new boolean[NUM_AUTO_CLASSES][linkTable.getRowCount()];


		for (int i=0; i < linkTable.getRowCount(); i++) {
		    
			int an = (int)linkTable.getValueAt( i+1, "anode" );
			int bn = (int)linkTable.getValueAt( i+1, "bnode" );
			if ( isCentroid(an) || isCentroid(bn) ) {
					centroid[i] = true;
					centroidString[i] = "true";
			}
			else {
				centroid[i] = false;
				centroidString[i] = "false";
			}
    

			// set speed and capacity fields based on values in ul1; set minimum link distance.
			float ul1 = linkTable.getValueAt( i+1, "ul1" );
			float ul2 = linkTable.getValueAt( i+1, "ul2" );

			
			if ( centroid[i] )
				capacity[i] = 9999;
			else if (ul1 > 15 && ul1 <= 30)
				capacity[i] = 800;
			else if (ul1 > 30 && ul1 <= 40)
				capacity[i] = 1200;
			else if (ul1 > 40 && ul1 <= 50)
				capacity[i] = 1400;
			else if (ul1 > 50 && ul1 <= 60)
				capacity[i] = 1600;
			else if (ul1 > 60 && ul1 <= 70)
				capacity[i] = 1800;
			else if (ul1 > 70)
				capacity[i] = 2000;
			else
				capacity[i] = 600;

			capacity[i] *= linkTable.getValueAt( i+1, "lanes" );
			capacity[i] /= volumeFactor;

			float dist = linkTable.getValueAt( i+1, "dist" );
			if (dist == 0.0)
			    dist = 0.001f;
			length[i] = dist;

			
			// initialize the flow by user class fields to zero
			for (int j=0; j < NUM_AUTO_CLASSES; j++) {
				flow[j][i] = 0.0f;
			}
	        
			
			
			// initialize the valid links for shortest paths flags
			for (int j=0; j < NUM_AUTO_CLASSES; j++) {
				validLink[j][i] = true;
			}
	        

			String mode = linkTable.getStringValueAt( i+1, "mode" );

			
			
			// redefine link vdf attributes - 1=non-centroid, 2=centroid
			if ( centroid[i] )
			    linkTable.setValueAt( i+1, linkTable.getColumnPosition("vdf"), 2);
			else
			    linkTable.setValueAt( i+1, linkTable.getColumnPosition("vdf"), 1);

			
			// define times and speeds as was done for PT calibration (see times.mac)
			if ( ul1 >= 0 && ul1 <= 6 )
			    ul1 = 30;
			
			if ( !centroid[i] && (ul2 >= 0.9 && ul2 <= 6) && mode.indexOf('a') >= 0 && (ul1 >= 5 && ul1 <= 99) )
			    ul1 -= 5;
			
			float ul3 = (float)((dist/ul1)*60.0);

			
			freeFlowSpeed[i] = ul1;
			congestedTime[i] = ul3;
			freeFlowTime[i] = congestedTime[i];
			oldTime[i] = congestedTime[i];

			linkTable.setValueAt( i+1, linkTable.getColumnPosition("ul1"), ul1);
			linkTable.setValueAt( i+1, linkTable.getColumnPosition("ul3"), ul3);

		}
	    
	    
		// apply any link mods for ul3 as done for PT (see times.mac)
		if (linkModsTable != null) {

		    for (int i=0; i < linkModsTable.getRowCount(); i++) {
		    
			    int[] ib = getIb();
				int an = (int)linkModsTable.getValueAt( i+1, "anode" );
				int bn = (int)linkModsTable.getValueAt( i+1, "bnode" );
				int ia = nodeIndex[an];
				float ul3 = linkModsTable.getValueAt( i+1, "ul3" );
				
				for (int j=ipa[ia]; j < ipa[ia+1]; j++) {
					int k = sortedLinkIndexA[j];
					if (indexNode[ib[k]] == bn) {
						linkTable.setValueAt( k+1, linkTable.getColumnPosition("ul3"), ul3);
						linkTable.setValueAt( k+1, linkTable.getColumnPosition("vdf"), 3);
						congestedTime[k] = ul3;
					    break;
					}
				}
	
			}
			
			linkModsTable = null;
		
		}
		


		TableDataSet derivedTable = new TableDataSet();
	    
		derivedTable.appendColumn(centroidString, "centroid");
		derivedTable.appendColumn(turnPenaltyIndex, "turnPenaltyIndex");
		derivedTable.appendColumn(capacity, "capacity");
		derivedTable.appendColumn(freeFlowSpeed, "freeFlowSpeed");
		derivedTable.appendColumn(congestedTime, "congestedTime");
		derivedTable.appendColumn(volau, "volau");
		derivedTable.appendColumn(vdfIntegral, "vdfIntegral");
		derivedTable.appendColumn(freeFlowTime, "freeFlowTime");
		derivedTable.appendColumn(length, "length");
		derivedTable.appendColumn(oldTime, "oldTime");
		for (int j=0; j < NUM_AUTO_CLASSES; j++) {
			derivedTable.appendColumn(flow[j], "flow" + "_" + j);
		}

		
		return derivedTable;

	}

	
	
	public void applyVdfs () {
	
		double[] results = fdLc.solve();
		linkTable.setColumnAsDouble( linkTable.getColumnPosition("congestedTime"), results );

	}
	
	
	
	public void applyVdfIntegrals () {
	
		double[] results = fdiLc.solve();
		linkTable.setColumnAsDouble( linkTable.getColumnPosition("vdfIntegral"), results );

	}
	
	
	
	private void setInternalNodeNumbering () {

		int[] ia = new int[linkTable.getRowCount()];
		int[] ib = new int[linkTable.getRowCount()];

		maxNode = getMaxNode();
		
		indexNode = new int[nodeTable.getRowCount()+1];
		nodeIndex = new int[maxNode+1];
		int[] usedNodes = new int[maxNode+1];
        
		Arrays.fill ( indexNode, 0 );
		Arrays.fill ( nodeIndex, 0 );
		Arrays.fill ( usedNodes, -1 );

		int[] an = linkTable.getColumnAsInt( "anode" );
		int[] bn = linkTable.getColumnAsInt( "bnode" );


		// renumber centroid nodes first
		int nextNode = 0;
		numCentroids = 0;
		maxCentroid = 0;
		for (int i=0; i < linkTable.getRowCount(); i++) {
            
			if ( isCentroid(an[i]) && usedNodes[an[i]] < 0) {
			    if (an[i] > maxCentroid)
			        maxCentroid = an[i];
				usedNodes[an[i]] = nextNode++;
			    numCentroids++;
			}

			if ( isCentroid(bn[i]) && usedNodes[bn[i]] < 0) {
			    if (bn[i] > maxCentroid)
			        maxCentroid = bn[i];
				usedNodes[bn[i]] = nextNode++;
			    numCentroids++;
			}

		}

		
		// now renumber regular nodes and set the internal node number array and correspondence array values
		for (int i=0; i < linkTable.getRowCount(); i++) {
            
			if (usedNodes[an[i]] < 0)
				usedNodes[an[i]] = nextNode++;
			if (usedNodes[bn[i]] < 0)
				usedNodes[bn[i]] = nextNode++;

			ia[i] = usedNodes[an[i]];
			ib[i] = usedNodes[bn[i]];

			indexNode[ia[i]] = an[i];
			nodeIndex[an[i]] = ia[i];
			indexNode[ib[i]] = bn[i];
			nodeIndex[bn[i]] = ib[i];

		}
        
        
		// add new link node fields to TableDataSet
		linkTable.appendColumn( ia, "ia" );
		linkTable.appendColumn( ib, "ib" );

	}
    
    
	private int[] setForwardStarArrays () {
	    
		int k;
		int old;

		int[] ip = new int[nodeTable.getRowCount()+1];
		sortedLinkIndexA = IndexSort.indexSort( ia );
		
		old = ia[sortedLinkIndexA[0]];
		ip[old] = 0;
		for (int i=0; i < ia.length; i++) {
			k = sortedLinkIndexA[i];

			if ( ia[k] != old ) {
				ip[ia[k]] = i;
				old = ia[k];
			}
		}
		ip[old+1] = ia.length;
		
		return ip;

	}

	
	
	private void setTurnPenalties( 	float[][] turnDefs ) {

		int k=0;
		int index=0;

		ArrayList[] turnLists = new ArrayList[linkTable.getRowCount()];
		turnTable = new float[linkTable.getRowCount()][][];
		

		// set the turn penalty index record for each link that is downstream in the turning movement.
		// this record provides the turn penalty function information used to define the turn penalty/prohibition.
		for (int i=0; i < turnDefs.length; i++) {
            
			// turning movement i->j->k is coded in file as j i k
			k = getLinkIndex ( (int)turnDefs[i][0], (int)turnDefs[i][2] );

			if (turnLists[k] == null)
				turnLists[k] = new ArrayList();

			turnLists[k].add(turnDefs[i]);
			
		}


		for (k=0; k < linkTable.getRowCount(); k++) {

			if (turnLists[k] != null) {
				turnTable[k] = new float[turnLists[k].size()][((float[])turnLists[k].get(0)).length];
				for (int j=0; j < turnLists[k].size(); j++) 
					turnTable[k][j] = (float[])turnLists[k].get(j);
			}
		}

	}

	
	public double getTurnPenalty ( int jn, int in, int kn ) {

		double returnValue = 0;
		
		// if turnTable is null, no penalties or prohibitions at all are defined
		if (turnTable == null)
		    return 0;
	    
		
		// get the turn penalty index from the turn penalty record if one exists
		// if index == -1, no penalty 
		// if index ==  0, turn prohibited 
		// if index >=  1, turn penalty function index (functions defined in d411 format file) 
		int fpIndex = getTurnPenaltyIndex ( jn, in, kn );
		
		// if this downstream link is not part of a penalized or prohibited turn, return penalty is zero.
		if ( fpIndex < 0 )
			return 0;
		// else, if this downstream link is part of a prohibited turn, return penalty is -1.
		else if ( fpIndex == 0 )
			return -1;
		
		
		// otherwise, this downstream link is part of a penalized turn so return the turn penalty.
		returnValue = fpLc.solve( fpIndex );

		
		return returnValue;
		
	}
	
	
	private int getTurnPenaltyIndex ( int jn, int in, int kn ) {

		int returnValue = -1;
	    
		int k = getLinkIndex ( jn, kn );
		
		// if this downstream link is not in turn table, the turn is non penalized,
		// so return -1.
		if ( turnTable[k] == null )
			return returnValue;
		
		
		// if this downstream link is in turn table, check to see if the turn is penalized or prohibited.
		// if prohibited, return 0.  if penalized, return the turn penalty function index.
		float[][] linkTurnTable = turnTable[k];
		
		for (int i=0; i < linkTurnTable.length; i++) {

			if ( (int)linkTurnTable[i][0] == jn && (int)linkTurnTable[i][1] == in && (int)linkTurnTable[i][2] == kn ) {
				returnValue = (int)linkTurnTable[i][3];
				break;
			}
		    
		}

		return returnValue;
		
	}
	
	
	public int getLinkIndex( int an, int bn ) {

		// takes external node numbers and returns link index

	    boolean debug = false;
	    
		int k = 0;
		int returnValue = 0;
		
		for (int i=ipa[nodeIndex[an]]; i < ipa[nodeIndex[an]+1]; i++) {
            
			k = sortedLinkIndexA[i];
            
			if (debug) {
				logger.info ("i=" + i + ", k=" + k + ", an=" + nodeIndex[an] + ", bn=" + nodeIndex[bn] + ", ia[k=" + k + "]=" + ia + ", ib[k=" + k + "]=" + ib );
			}
			
			if ( ib[k] == nodeIndex[bn] ) {
				returnValue = k;
				break;
			}
		}

		return returnValue;
	    
	}

	
	
	public void logLinkTimeFreqs () {
	    
		int[] ia = getIa();
		int[] ib = getIb();

		double[] congestedTime = (double[])linkTable.getColumnAsDouble( "congestedTime" );
		int[] buckets = new int[8];

		int count = 0;
		for (int i=0; i < congestedTime.length; i++) {
			if ( congestedTime[i] > 0 && congestedTime[i] <= 1.0 )
				buckets[0]++;
			else if ( congestedTime[i] > 1.0 && congestedTime[i] <= 10.0 )
				buckets[1]++;
			else if ( congestedTime[i] > 10.0 && congestedTime[i] <= 100.0 )
				buckets[2]++;
			else if ( congestedTime[i] > 100.0 && congestedTime[i] <= 1000.0 )
				buckets[3]++;
			else if ( congestedTime[i] > 1000.0 )
				buckets[4]++;
			else if ( congestedTime[i] == 0.0 ) {
				buckets[5]++;
//				int k = sortedLinkIndexA[i];
				int k = i;
				logger.info ( indexNode[ia[k]] + "," + indexNode[ib[k]] + " has congested time = " + congestedTime[i] );
			}
			else {
				buckets[6]++;
//				int k = sortedLinkIndexA[i];
				int k = i;
				logger.info ( indexNode[ia[k]] + "," + indexNode[ib[k]] + " has congested time = " + congestedTime[i] );
			}
		}

		logger.info ("");
		logger.info ( "frequency table of free flow link travel times");
		logger.info ( buckets[5] + " == 0");
		logger.info ( "0 < " + buckets[0] + " <= 1.0");
		logger.info ( "1.0 < " + buckets[1] + " <= 10.0");
		logger.info ( "10.0 < " + buckets[2] + " <= 100.0");
		logger.info ( "100.0 < " + buckets[3] + " <= 1,000.0");
		logger.info ( buckets[4] + " > 1,000.0");
		logger.info ( buckets[6] + " other");
		logger.info ("");
		
	}
}

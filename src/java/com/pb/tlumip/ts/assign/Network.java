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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.pb.common.calculator.LinkCalculator;
import com.pb.common.calculator.LinkFunction;
import com.pb.common.datafile.D211FileReader;
import com.pb.common.datafile.D231FileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.IndexSort;
import com.pb.common.matrix.AlphaToBeta;

/**
 * This Network class contains the link and node data tables and
 * index arrays for representing the network in forward star notation.
 *
 */

public class Network implements Serializable {

	//TODO these values should be set in a method from values determined in tour mode choice
	static final float TIME_PARAMETER = 1.0f;
	static final float COST_PARAMETER = 0.0f;
	static final float OPERATING_COST = 0.0f;

	
	protected static transient Logger logger = Logger.getLogger("com.pb.tlumip.ts.assign.Network");

	
	HashMap userClassMap = new HashMap();
	HashMap assignmentGroupMap = new HashMap();
	

	int minCentroidLabel;
	int maxCentroidLabel;
	
	String assignmentPeriod;

    float volumeFactor;
    
	double WALK_SPEED;

	
	int numAlphazones;
	int maxCentroid;
	int numCentroids;
	int maxNode;
    int numLinks;
    int numUserClasses;

	char[] userClasses = null;
	
    double[] volau;
    double[] totAonFlow;

    boolean[] validLinks = null;
	boolean[][] validLinksForClass = null;
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
	LinkCalculator ftLc = null;

    int[][] turnPenaltyIndices = null;
    float[][] turnPenaltyArray = null;
	float[][][] turnTable = null;

    
	HashMap tsPropertyMap = null;
    HashMap globalPropertyMap = null;


    public Network ( HashMap tsPropertyMap, HashMap globalPropertyMap, String period ) {

		float[][] turnDefs = null;

        this.tsPropertyMap = tsPropertyMap;
        this.globalPropertyMap = globalPropertyMap;


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
		lf = new LinkFunction ( (String)tsPropertyMap.get("vdf.fileName"), "vdf");
		lfi = new LinkFunction ( (String)tsPropertyMap.get("vdfIntegral.fileName"), "vdf");
		
		
		// set the internal numbering for nodes and their correspondence to external numbering.
		setInternalNodeNumbering ();

		// define the forward star index arrays, first by anode then by bnode
		ia = linkTable.getColumnAsInt( "ia" );
		ib = linkTable.getColumnAsInt( "ib" );
		sortedLinkIndexA = IndexSort.indexSort( ia );
		ipa = setForwardStarArrays ( ia, sortedLinkIndexA );
		
		// calculate the derived link attributes for the network
		derivedLinkTable = deriveLinkAttributes( volumeFactor );

		
		// merge the derived link attributes into the linkTable TableDataSet,
		// then we're done with the derived table.
		linkTable.merge ( derivedLinkTable );
		derivedLinkTable = null;
		
        numLinks = linkTable.getRowCount();
		
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

		
		ftLc = new LinkCalculator ( linkTable, lf.getFunctionStrings( "ft" ), "vdf" );

		
		
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
        return numLinks;
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

	public double[] getOriginalCapacity () {
		return linkTable.getColumnAsDouble( "originalCapacity" );
	}

    public double[] getCongestedTime () {
        return linkTable.getColumnAsDouble( "congestedTime" );
    }

    public double[] getLinkGeneralizedCost () {
        return linkTable.getColumnAsDouble( "generalizedCost" );
    }

	public double[] getTransitTime () {
		return linkTable.getColumnAsDouble( "transitTime" );
	}

    public double getSumOfVdfIntegrals () {
        double[] integrals = linkTable.getColumnAsDouble("vdfIntegral");
        
        double sum = 0.0;
        for (int k=0; k < integrals.length; k++)
            if ( validLinks[k] )
                sum += integrals[k];
            
        return sum;
    }

    public double getSumOfGcIntegrals () {
        
        double[] integrals = linkTable.getColumnAsDouble("vdfIntegral");
        double[] toll = getToll();
        double[] dist = getDist();
        
        double sum = 0.0;
        for (int k=0; k < integrals.length; k++)
            if ( validLinks[k] )
                sum += TIME_PARAMETER*integrals[k] + volau[k]*COST_PARAMETER*(toll[k] + dist[k]*OPERATING_COST);
            
        return sum;
    }

	public double[] getFreeFlowTime () {
		return linkTable.getColumnAsDouble( "freeFlowTime" );
	}

	public double[] getToll () {
		return linkTable.getColumnAsDouble( "toll" );
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

	public char[] getUserClasses () {
		return userClasses;
	}

	public int getNumUserClasses () {
		return numUserClasses;
	}
	
	public boolean userClassesIncludeAuto () {
		// return true if the auto class is included in the list of user classes to assign
		
		boolean autoIncluded = false;
		for (int i=0; i < userClasses.length; i++) {
			if ( userClasses[i] == 'a' ) {
				autoIncluded = true;
				break;
			}
		}
			
		return autoIncluded;
	}

	public boolean userClassesIncludeTruck () {
		// return true if at least one truck class is included in the list of user classes to assign

		boolean truckIncluded = false;
		for (int i=0; i < userClasses.length; i++) {
			if ( userClasses[i] == 'd' || userClasses[i] == 'e' || userClasses[i] == 'f' || userClasses[i] == 'g' || userClasses[i] == 'h' ) {
				truckIncluded = true;
				break;
			}
		}
			
		return truckIncluded;
	}

	public double getWalkSpeed () {
		return WALK_SPEED;
	}

    public double[][] getFlows () {
         
        double[][] flows = new double[userClasses.length][];
         
        for (int m=0; m < userClasses.length; m++)
            flows[m] = linkTable.getColumnAsDouble( "flow_" + m );
        
        return flows;
    }
    
	public double[] getNodeX () {
		return nodeTable.getColumnAsDouble( "x" );
	}
    
	public double[] getNodeY () {
		return nodeTable.getColumnAsDouble( "y" );
	}
    	
	public HashMap getAssignmentGroupMap () {
		return assignmentGroupMap;
	}
	
	public int getUserClassIndex ( char modeChar ) {
		return ((Integer)userClassMap.get( String.valueOf(modeChar) )).intValue();
	}
    
    public int[][] getTurnPenaltyIndices () {
        return turnPenaltyIndices;
    }
    
    public float[][] getTurnPenaltyArray () {
        return turnPenaltyArray;
    }
    
    public boolean[][] getValidLinksForAllClasses () {
        return validLinksForClass;
    }

    public boolean[] getValidLinksForClass ( int userClass ) {
        return validLinksForClass[userClass];
    }

    public boolean[] getValidLinksForClass ( char modeChar ) {
		int userClassIndex = getUserClassIndex(modeChar);
        return validLinksForClass[userClassIndex];
    }

    public double[] setLinkGeneralizedCost () {
    	double[] ctime = getCongestedTime();
    	double[] toll = getToll();
    	double[] dist = getDist();
    	
    	double[] gc = new double[ctime.length];
    	
    	for (int i=0; i < ctime.length; i++)
    		gc[i] = TIME_PARAMETER*ctime[i] + COST_PARAMETER*(toll[i] + dist[i]*OPERATING_COST);
    	
		linkTable.setColumnAsDouble( linkTable.getColumnPosition("generalizedCost"), gc );
		
		return gc;
    }

    public void setVolau ( double[] volau ) {
        this.volau = volau;
		linkTable.setColumnAsDouble( linkTable.getColumnPosition("volau"), volau );
    }

    public void setVolCapRatios () {
		
    	double[] totalVolCapRatio = linkTable.getColumnAsDouble( "totalVolCapRatio" );
    	double[] totalCapacity = linkTable.getColumnAsDouble( "totalCapacity" );
    	double[] volad = linkTable.getColumnAsDouble( "volad" );

		for (int i=0; i < linkTable.getRowCount(); i++) {
			totalVolCapRatio[i] = (volau[i]+volad[i])/totalCapacity[i];
		}

		linkTable.setColumnAsDouble( linkTable.getColumnPosition("totalVolCapRatio"), totalVolCapRatio );
		
    }

    public void setFlows (double[][] flow) {
        for (int m=0; m < flow.length; m++)
            linkTable.setColumnAsDouble( linkTable.getColumnPosition("flow_" + m), flow[m] );
    }
    
    public void setTtf ( int[] ttf ) {
        linkTable.setColumnAsInt( linkTable.getColumnPosition("ttf"), ttf );
    }
      
    private char[] getUserClassesFromProperties () {

		// get the mode codes that identify user classes
		String userClassPropertyString = (String)tsPropertyMap.get("userClass.modes");
		ArrayList userClassList = new ArrayList();
		StringTokenizer st = new StringTokenizer(userClassPropertyString, ", |");
		while (st.hasMoreTokens()) {
			userClassList.add(st.nextElement());
		}
		
		// copy the valid mode codes into an array
		char tempUserClass;
		char[] tempUserClasses = new char[userClassList.size()]; 
		int j = 0;
		for (int i=0; i < tempUserClasses.length; i++) {
			tempUserClass = ((String)userClassList.get(i)).charAt(0);
			if ( tempUserClass == 'a' || tempUserClass == 'd' || tempUserClass == 'e' || tempUserClass == 'f' || tempUserClass == 'g' || tempUserClass == 'h' )
				tempUserClasses[j++] = tempUserClass;
		}

		// create an array of valid mode codes to return
		char[] finalUserClasses = new char[j]; 
		for (int i=0; i < finalUserClasses.length; i++) {
			finalUserClasses[i] = tempUserClasses[i];
			
			userClassMap.put ( String.valueOf(finalUserClasses[i]), Integer.valueOf(i) );
		}
		
		return finalUserClasses;
		
    }
    
    
    
    private void setAssignmentGroups () {
    
		// get the mode codes associated with each of the possible 5 truck groups and put into a HashMap
		char tempUserClass;
		String propertyKeyWordString = null;
		String truckClassPropertyString = null;
		ArrayList truckClassList = new ArrayList();
    	for  (int i=1; i <= 5; i++ ) {
    		propertyKeyWordString = "truckClass" + i + ".modes";
    		truckClassPropertyString = (String)tsPropertyMap.get( propertyKeyWordString );
    		StringTokenizer st = new StringTokenizer( truckClassPropertyString, ", |" );
    		while (st.hasMoreTokens()) {
    			tempUserClass = ((String)st.nextElement()).charAt(0);
    			
    			// if the mode code is a valid truck code, add it to the assignment group map.
    			if ( tempUserClass == 'd' || tempUserClass == 'e' || tempUserClass == 'f' || tempUserClass == 'g' || tempUserClass == 'h' )
    				assignmentGroupMap.put ( String.valueOf(tempUserClass), Integer.valueOf(1) );
    		}
    	}

    	// if auto is in the list of user classes, add auto as assignment group 0.
    	if ( userClassesIncludeAuto () )
			assignmentGroupMap.put ( String.valueOf('a'), Integer.valueOf(0) );

    }
    
    

    private void readPropertyFile ( String period ) {
        
    	this.assignmentPeriod = period;
    	
        if ( assignmentPeriod.equalsIgnoreCase( "peak" ) ) {
            // get peak period definitions from property files
            volumeFactor = Float.parseFloat( (String)globalPropertyMap.get("AM_PEAK_VOL_FACTOR") );
        }
        else if ( assignmentPeriod.equalsIgnoreCase( "offpeak" ) ) {
            // get off-peak period definitions from property files
            volumeFactor = Float.parseFloat( (String)globalPropertyMap.get("OFF_PEAK_VOL_FACTOR") );
        }
        

    	// get the filename for the alpha/beta zone correspomdence file
		String zoneIndexFile = (String) globalPropertyMap.get( "alpha2beta.file" );

		// create a Halo object for defining the extent of the study area
		AlphaToBeta a2b = new AlphaToBeta(new File(zoneIndexFile));

    	// get the filename for the network node and link table
		this.d211File = (String) tsPropertyMap.get( "d211.fileName" );
		
		// get the filename for the turn table
		this.d231File = (String) tsPropertyMap.get( "d231.fileName" );
		
		// get the filename for the network node and link modifications table
		this.d211ModsFile = (String) tsPropertyMap.get( "d211Mods.fileName" );
		
		// get the filename for the highway link volume delay function definition file
		this.hwyVdfFile = (String) tsPropertyMap.get(	"vdf.fileName" );
		
		// get the filename for the highway link volume delay function integrals definition file
		this.hwyVdfIntegralFile = (String) tsPropertyMap.get(	"vdfIntegral.fileName" );
		
		// get network properties
		this.minCentroidLabel = 1;
		this.maxCentroidLabel = a2b.getMaxAlphaZone();
		this.numAlphazones = a2b.alphaSize();
		
		

		// get user classes to assign and assignment groups (which classes are combined together for assigning)
		this.userClasses = getUserClassesFromProperties ();
        this.numUserClasses = this.userClasses.length;
		setAssignmentGroups();
		
		
		logger.info ( "Mode codes for user classes in multiclass assignment:" );
		for (int i=0; i < userClasses.length; i++)
			logger.info ( "     " + i + ": " + userClasses[i] );
		

		if ( (String)globalPropertyMap.get( "WALK_MPH" ) != null )
		    this.WALK_SPEED = Double.parseDouble ( (String)globalPropertyMap.get( "WALK_MPH" ) );

		
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
	private TableDataSet deriveLinkAttributes ( float volumeFactor ) {

		int[] turnPenaltyIndex = new int[linkTable.getRowCount()];
		int[] ttf = new int[linkTable.getRowCount()];
		float[] length = new float[linkTable.getRowCount()];
		double[] lanes = new double[linkTable.getRowCount()];
		double[] totalVolCapRatio = new double[linkTable.getRowCount()];
		double[] totalCapacity = new double[linkTable.getRowCount()];
		double[] capacity = new double[linkTable.getRowCount()];
		double[] originalCapacity = new double[linkTable.getRowCount()];
		double[] freeFlowSpeed = new double[linkTable.getRowCount()];
		double[] congestedTime = new double[linkTable.getRowCount()];
		double[] transitTime = new double[linkTable.getRowCount()];
		double[] vdfIntegral = new double[linkTable.getRowCount()];
		double[] freeFlowTime = new double[linkTable.getRowCount()];
		double[] oldTime = new double[linkTable.getRowCount()];
		double[] volad = new double[linkTable.getRowCount()];
		double[] toll = new double[linkTable.getRowCount()];
		double[] gc = new double[linkTable.getRowCount()];
		double[][] flow = new double[userClasses.length][linkTable.getRowCount()];
		boolean[] centroid = new boolean[linkTable.getRowCount()];
		String[] centroidString = new String[linkTable.getRowCount()];
		validLinksForClass = new boolean[userClasses.length][linkTable.getRowCount()];
		validLinks = new boolean[linkTable.getRowCount()];
        volau = new double[linkTable.getRowCount()];
        totAonFlow = new double[linkTable.getRowCount()];

		Arrays.fill (volad, 0.0);
		Arrays.fill (validLinks, false);

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
			String mode = linkTable.getStringValueAt( i+1, "mode" );
			float ul1 = linkTable.getValueAt( i+1, "ul1" );

			
			// can't have zero speed for highway links, so fix those here for now
			if ( ul1 == 0 && mode.indexOf('a') >= 0 )
				ul1 = 15;
			
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

			lanes[i] = linkTable.getValueAt( i+1, "lanes" );
			
			originalCapacity[i] = capacity[i];
			capacity[i] /= volumeFactor;

			
			// the following variables are needed for the VDF Integrals definitions
			totalCapacity[i] = 0.75 * capacity[i] * lanes[i];
			totalVolCapRatio[i] = 0.0;

			
			float dist = linkTable.getValueAt( i+1, "dist" );
			if (dist == 0.0)
			    dist = 0.001f;
			length[i] = dist;

			
			// initialize the flow by user class fields to zero
			for (int j=0; j < userClasses.length; j++) {
				flow[j][i] = 0.0f;
			}
	        
			
			// The following modes are valid for TLUMIP Statewide network and multiclass assignment (6 classes).
			// Auto trips ares available on all highway links.
			// Truck trips are classed by weight, d being lightest to h being heaviest.
			// Lighter trucks are valid on all heavier class links 
			// a	auto
			// d	truck below 34k lbs.
			// e	truck between 64k lbs. and 34k lbs.
			// f	truck between 80k lbs. and 64k lbs.
			// g	truck between 105.5k lbs. and 80k lbs.
			// h	truck greater than 105.5k lbs.
			
			// initialize the valid links for multiclass network used in determining shortest paths by class
			for (int j=0; j < userClasses.length; j++) {
			    if ( mode.indexOf( userClasses[j] ) >= 0 ) {
			    	validLinksForClass[j][i] = true;
			    	validLinks[i] = true;
			    }
			    else {
			    	validLinksForClass[j][i] = false;
			    }
			}
	        


			


			
			freeFlowSpeed[i] = ul1;
			congestedTime[i] = (float)((dist/ul1)*60.0);
			freeFlowTime[i] = congestedTime[i];
			oldTime[i] = congestedTime[i];
			float ul3 = (float)((dist/ul1)*60.0);

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
		derivedTable.appendColumn(ttf, "ttf");
		derivedTable.appendColumn(totalVolCapRatio, "totalVolCapRatio");
		derivedTable.appendColumn(totalCapacity, "totalCapacity");
		derivedTable.appendColumn(capacity, "capacity");
		derivedTable.appendColumn(originalCapacity, "originalCapacity");
		derivedTable.appendColumn(freeFlowSpeed, "freeFlowSpeed");
		derivedTable.appendColumn(congestedTime, "congestedTime");
		derivedTable.appendColumn(congestedTime, "transitTime");
		derivedTable.appendColumn(volau, "volau");
		derivedTable.appendColumn(volad, "volad");
		derivedTable.appendColumn(toll, "toll");
		derivedTable.appendColumn(gc, "generalizedCost");
		derivedTable.appendColumn(vdfIntegral, "vdfIntegral");
		derivedTable.appendColumn(freeFlowTime, "freeFlowTime");
		derivedTable.appendColumn(length, "length");
		derivedTable.appendColumn(oldTime, "oldTime");
		for (int j=0; j < userClasses.length; j++) {
			derivedTable.appendColumn(flow[j], "flow" + "_" + j);
		}

		
		return derivedTable;

	}

	
	
//	public void applyVdfs () {
//		
//			double[] results = fdLc.solve();
//			
//			for (int i=0 ; i < results.length; i++) {
//				if ( results[i] < 0 || results[i] == Double.NaN ) {
//					logger.error ( "invalid result in Network.applyVdfs().   results[i=" + i + "] = " + results[i] );
//					logger.error ( "anode = " + indexNode[ia[i]] + ", bnode = " + indexNode[ib[i]] );
//					System.exit(-1);
//				}
//			}
//				
//			linkTable.setColumnAsDouble( linkTable.getColumnPosition("congestedTime"), results );
//
//		}
		
		
		
	public void applyVdfs () {
		
        double[] congestedTime = (double[])linkTable.getColumnAsDouble( "congestedTime" );
        
		double[] results = fdLc.solve(validLinks);
		
		for (int i=0 ; i < results.length; i++) {
            // if link calculater returns a negative or NaN result for a valid link, report the error
            if ( validLinks[i] ) {
                if ( results[i] < 0 || results[i] == Double.NaN ) {
                    logger.error ( "invalid result in Network.applyVdfs(boolean[] validLinks).   results[i=" + i + "] = " + results[i] );
                    logger.error ( "anode = " + indexNode[ia[i]] + ", bnode = " + indexNode[ib[i]] );
                    System.exit(-1);
                }
            }
            // use the default travel times calcualted for non-valid links.  A non-valid link might be a bus-only link that's not included in highway assignment network, for example.
            else {
                results[i] = congestedTime[i];
            }
		}
			
		linkTable.setColumnAsDouble( linkTable.getColumnPosition("congestedTime"), results );

	}
		
		
		
	public void applyVdfIntegrals () {
	
		double[] results = fdiLc.solve(validLinks);
		
		for (int i=0 ; i < results.length; i++) {
            // if link calculater returns a negative or NaN result for a valid link, report the error
            if ( validLinks[i] ) {
                if ( results[i] == Double.NaN ) {
                    logger.error ( "invalid result in Network.applyVdfIntegrals(boolean[] validLinks).   results[i=" + i + "] = " + results[i] );
                    logger.error ( "anode = " + indexNode[ia[i]] + ", bnode = " + indexNode[ib[i]] );
                    System.exit(-1);
                }
			}
            // the integral of a non-valid link is the constant, default time times the highway flow, which is zero, so the result is 0. 
            else {
                results[i] = 0.0;
            }
		}
			
		linkTable.setColumnAsDouble( linkTable.getColumnPosition("vdfIntegral"), results );

	}
	
	
	public double applyLinkTransitVdf ( int hwyLinkIndex, int transitVdfIndex ) {
		
		// calculate the link in-vehicle travel times based on the transit vdf index for the link passed in
		double result = ftLc.solve(hwyLinkIndex, transitVdfIndex);
		
		if ( result < 0 || result == Double.NaN ) {
			logger.error ( "invalid result in Network.applyLinkTransitVdf(int hwyLinkIndex, int transitVdfIndex).   hwyLinkIndex=" + hwyLinkIndex + ", transitVdfIndex = " + transitVdfIndex + ", result = "+ result );
			System.exit(-1);
		}
			
		return result;
		
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
    
    
	private int[] setForwardStarArrays ( int[] nodes, int[] indexArray ) {
	    
		int k;
		int old;

		int[] ip = new int[nodeTable.getRowCount()+1];
		
		old = nodes[indexArray[0]];
		ip[old] = 0;
		for (int i=0; i < nodes.length; i++) {
			k = indexArray[i];

			if ( nodes[k] != old ) {
				ip[nodes[k]] = i;
				old = nodes[k];
			}
		}
		ip[old+1] = nodes.length;
		
		return ip;

	}

	
	
	public void checkForIsolatedLinks () {
	    
		int start;
		int end;
		int aExit;
		int bExit;
		boolean errorsFound = false;
		
		
		for (int userClass = 0; userClass < userClasses.length; userClass++) {
			
			logger.info ( "checking network connectivity for userClass " + userClass + " (" + userClasses[userClass] + ") subnetwork.");
			
			// for each anode that has only one bnode, flag links where bnode only goes back to anode.
			for (int i=0; i < ia.length; i++) {
				
				if ( !validLinksForClass[userClass][i] )
					continue;
				
				aExit = -1;
				bExit = -1;

				start = ipa[ia[i]];
				end = ipa[ia[i]+1];
				
				// if anode has only one exiting link
				if (start == end - 1) {
					
					aExit = sortedLinkIndexA[start];
					
					start = ipa[ib[i]];
					end = ipa[ib[i]+1];
					
					// if bnode has only one exiting link and 
					if (start == end - 1) {
						
						bExit = sortedLinkIndexA[start];
						
						// if the node pair is same in each direction, this is an isolated link
						if (ia[aExit] == ib[bExit] && ib[aExit] == ia[bExit]) {
							logger.error ( "node pair [" + indexNode[ia[aExit]] + "," + indexNode[ib[aExit]] + "] is disconnected.");
							errorsFound = true;
						}
					
					}
					
				}
				
			}

		}
			
		if (errorsFound) {
			logger.error ( "errors identified above in constructing the internal network representations.");
			logger.error ( "the node pairs identified above are only connected to each other for the specified subnetwork.");
			logger.error ( "no subnetwork paths may therefore be built through these nodes and links.\n\n");
		}

	}

	
	




	private void setTurnPenalties( 	float[][] turnDefs ) {

		int k=0;

		ArrayList[] turnLists = new ArrayList[linkTable.getRowCount()];
        turnTable = new float[linkTable.getRowCount()][][];
        turnPenaltyIndices = new int[linkTable.getRowCount()][];
        turnPenaltyArray = new float[linkTable.getRowCount()][];
		

		// set the turn penalty index record for each link that is downstream in the turning movement.
		// this record provides the turn penalty function information used to define the turn penalty/prohibition.
		for (int i=0; i < turnDefs.length; i++) {
            
			// turning movement i->j->k is coded in file as j i k
			k = getLinkIndex ( (int)turnDefs[i][0], (int)turnDefs[i][2] );

			if (turnLists[k] == null)
				turnLists[k] = new ArrayList();

			turnLists[k].add(turnDefs[i]);
			
		}

        
        // turnLists[k] is an ArrayList of the turn penalty file records with link k as the downstream link
        // the elements in the ArrayList are arrays of node sequence and penalty for a turn into link k
        // the node sequence is: j, i, k for a turn defined as i->j->k.
        // therfore, turnLists[k][n][0] = the j node for the nth turn record going into link k
        //           turnLists[k][n][1] = the i node for the nth turn record going into link k
        //           turnLists[k][n][2] = the k node for the nth turn record going into link k
        //           turnLists[k][n][3] = the penalty for turn i->j->k.
		for (k=0; k < linkTable.getRowCount(); k++) {

			if (turnLists[k] != null) {
                
				turnTable[k] = new float[turnLists[k].size()][((float[])turnLists[k].get(0)).length];
                turnPenaltyIndices[k] = new int[turnLists[k].size()];
                turnPenaltyArray[k] = new float[turnLists[k].size()];
                for (int j=0; j < turnLists[k].size(); j++) {
                    
                    turnTable[k][j] = (float[])turnLists[k].get(j);
//                    int link2Index = getLinkIndex ( (int)turnTable[k][j][0], (int)turnTable[k][j][2] );
                    turnPenaltyIndices[k][j] = getLinkIndex ( (int)turnTable[k][j][1], (int)turnTable[k][j][0] );
                    turnPenaltyArray[k][j] = turnTable[k][j][3];
                                                       
                }
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
		int k = getLinkIndex ( jn, kn );
		returnValue = fpLc.solve( k, fpIndex );

		
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
		int k = 0;
		int returnValue = 0;
		
		for (int i=ipa[nodeIndex[an]]; i < ipa[nodeIndex[an]+1]; i++) {
            
			k = sortedLinkIndexA[i];
            
			if(logger.isDebugEnabled()) {
				logger.debug ("i=" + i + ", k=" + k + ", an=" + nodeIndex[an] + ", bn=" + nodeIndex[bn] + ", ia[k=" + k + "]=" + ia + ", ib[k=" + k + "]=" + ib );
			}
			
			if ( ib[k] == nodeIndex[bn] ) {
				returnValue = k;
				break;
			}
		}

		return returnValue;
	    
	}

	
	
    //Bisection routine to calculate opitmal lambdas during each frank-wolfe iteration.
    public double bisect ( int iter, double[][] aonFlow, double[][] flow ) {
        
        String myDateString = DateFormat.getDateTimeInstance().format(new Date());
        logger.info ("starting FW.bisect()" + myDateString);
 
        double x=0.0, xleft=0.0, xright=1.0, gap=0.0;

        int numBisectIterations = (int)(Math.log(1.0e-07)/Math.log(0.5) + 1.5);
        
        gap = ofGap( aonFlow, flow );
        
        if (Math.abs(gap) <= 1.0e-07) {
            x = -1.0;
            return( x );
        }
        else {
            if (gap <= 0)
                xleft = x;
            else
                xright = x;
            x = (xleft + xright)/2.0;
            if(logger.isDebugEnabled()) {
                logger.debug ("iter=" + iter + ", gap=" + gap + ", xleft=" + xleft + ", xright=" + xright + ", x=" + x);
            }

            for (int n=0; n < numBisectIterations; n++) {
                gap = bisectGap(x, aonFlow, flow );
                if (gap <= 0)
                    xleft = x;
                else
                    xright = x;
                x = (xleft + xright)/2.0;
                if(logger.isDebugEnabled()) {
                    logger.debug ("iter=" + iter + ", n=" + n + ", gap=" + gap + ", xleft=" + xleft + ", xright=" + xright + ", x=" + x);
                }
            }

            return( x );
            
        }
    }



    public double ofValue ( double[][] flow )  {

        double returnValue = -1;
        
        try {
        
            double[] volau = new double[numLinks];
            
            String myDateString = DateFormat.getDateTimeInstance().format(new Date());
            logger.info ("starting FW.ofValue()" + myDateString);
     
            // sum total flow over all user classes for each link 
            for (int k=0; k < volau.length; k++) {
                volau[k] = 0.0;
                for (int m=0; m < numUserClasses; m++)
                    volau[k] += flow[m][k];
            }
            
            setVolau(volau);
            setVolCapRatios();
            applyVdfIntegrals();
    
            returnValue = getSumOfGcIntegrals();
        
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }

        return returnValue;
        
    }



    public double ofGap ( double[][] aonFlow, double[][] flow ) {

        double returnValue = -1;
        
        try {
            
            String myDateString = DateFormat.getDateTimeInstance().format(new Date());
            logger.info ("starting FW.ofGap()" + myDateString);
     
            // sum total flow over all user classes for each link 
            for (int k=0; k < volau.length; k++) {
                volau[k] = 0.0;
                totAonFlow[k] = 0.0;
                for (int m=0; m < numUserClasses; m++) {
                    volau[k] += flow[m][k];
                    totAonFlow[k] += aonFlow[m][k];
                }
            }
            
            setVolau(volau);
            applyVdfs();
            double[] cTime = setLinkGeneralizedCost();
            
            
            double gap = 0.0;
            for (int k=0; k < volau.length; k++) {
                if ( validLinks[k] ) {
                    
                    if ( ! isValidDoubleValue(cTime[k]) )
                        logger.error( "invalid value for loaded link travel time on link " + k );
                    else if ( ! isValidDoubleValue(totAonFlow[k]) )
                        logger.error( "invalid value for all-or-nothing link flow on link " + k );
                    else if ( ! isValidDoubleValue(volau[k]) )
                        logger.error( "invalid value for loaded link flow on link " + k );
                
                    gap += cTime[k]*(totAonFlow[k] - volau[k]);
                }
            }
    
            returnValue = gap;
        
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }

        return returnValue;
        
    }


    public double bisectGap (double x, double[][] aonFlow, double[][] flow ) {

        double returnValue = -1;
        
        try {
            
            String myDateString = DateFormat.getDateTimeInstance().format(new Date());
            logger.info ("starting FW.bisectGap()" + myDateString);
     
            // sum total flow over all user classes for each link 
            for (int k=0; k < volau.length; k++) {
                volau[k] = 0.0;
                totAonFlow[k] = 0.0;
                for (int m=0; m < numUserClasses; m++) {
                    volau[k] += flow[m][k];
                    totAonFlow[k] += aonFlow[m][k];
                }
                volau[k] = volau[k] + x*(totAonFlow[k] - volau[k]);
            }
            
            setVolau(volau);
            applyVdfs();
            double[] cTime = setLinkGeneralizedCost();
    
            double gap = 0.0;
            for (int k=0; k < cTime.length; k++)
                if ( validLinks[k] )
                    gap += cTime[k]*(totAonFlow[k] - volau[k]);
    
    
            returnValue = gap;
        
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }

        return returnValue;
        
    }


    public void linkSummaryReport ( double[][] flow ) {
        
        double totalVol;
        double[][] volumeSum = new double[numUserClasses][Constants.MAX_LINK_TYPE];
        int[] linkType = getLinkType();

        for (int k=0; k < numLinks; k++)
            for (int m=0; m < numUserClasses; m++)
                volumeSum[m][linkType[k]] += flow[m][k];

        logger.info("");
        logger.info("");
        logger.info("");
        String logRecord = String.format("%-8s", "LinkType");
        for (int m=0; m < numUserClasses; m++)
            logRecord.concat ( String.format("%12s", ("class " + userClasses[m])) );
        logger.info( logRecord );
        for (int i=0; i < Constants.MAX_LINK_TYPE; i++) {
            totalVol = 0.0;
            for (int m=0; m < numUserClasses; m++)
                totalVol += volumeSum[m][i];
            if (totalVol > 0.0) {
                logRecord = String.format("%-8d", i);
                for (int m=0; m < numUserClasses; m++)
                    logRecord.concat ( String.format("%12.2f", volumeSum[m][i]) );
                logger.info( logRecord );
            }
        }
    }

    
    
    public void logLinkTimeFreqs () {
	    
		int[] ia = getIa();
		int[] ib = getIb();

		double[] congestedTime = (double[])linkTable.getColumnAsDouble( "congestedTime" );
		int[] buckets = new int[8];

		for (int i=0; i < congestedTime.length; i++) {
			
			if ( !validLinks[i] )
				continue;
			
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
		logger.info ( "for links valid for at least one user class");
		logger.info ( buckets[5] + " == 0");
		logger.info ( "0 < " + buckets[0] + " <= 1.0");
		logger.info ( "1.0 < " + buckets[1] + " <= 10.0");
		logger.info ( "10.0 < " + buckets[2] + " <= 100.0");
		logger.info ( "100.0 < " + buckets[3] + " <= 1,000.0");
		logger.info ( buckets[4] + " > 1,000.0");
		logger.info ( buckets[6] + " other");
		logger.info ("");
		
	}
	
	

	public void writeNetworkAttributes ( String fileName ) {

		int[] ia = getIa();
		int[] ib = getIb();
		double[] congestedTime = (double[])linkTable.getColumnAsDouble( "congestedTime" );
		double[] distance = (double[])linkTable.getColumnAsDouble( "length" );
		double[] capacity = (double[])linkTable.getColumnAsDouble( "capacity" );

		double[][] flow = new double[userClasses.length][];
		for (int j=0; j < userClasses.length; j++) {
			flow[j] = (double[])linkTable.getColumnAsDouble( "flow_" + j );
		}
		
		
		PrintWriter outStream = null;

        // open output stream for network attributes file
		try {
			
			outStream = new PrintWriter (new BufferedWriter( new FileWriter(fileName) ) );

			
			outStream.print ("anode,bnode,capacity,assignmentTime,");
			for (int j=0; j < userClasses.length-1; j++)
				outStream.print( "assignmentFlow_" + j + "," );
			
			outStream.println( "assignmentFlow_" + (userClasses.length-1) );
			

			
			for (int k=0; k < ia.length; k++) {
			
				outStream.print( indexNode[ia[k]] + ","
								+ indexNode[ib[k]] + ","
								+ String.format("%.4f", capacity[k]) + ","
								+ String.format("%.4f", congestedTime[k]) + ","
								);
								
				for (int j=0; j < userClasses.length-1; j++)
					outStream.print( String.format("%.4f", flow[j][k]) + "," );
	
				outStream.println( String.format("%.4f", flow[userClasses.length-1][k]) );

			}
		
			outStream.close();

		
		}
		catch (IOException e) {
			logger.fatal ("I/O exception writing network attributes file.");
			e.printStackTrace();
		}

	}
	

    private boolean isValidDoubleValue( double value ) {
        return ( value >= -Double.MAX_VALUE && value <= Double.MAX_VALUE );
    }

}

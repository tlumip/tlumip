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
package com.pb.tlumip.ts.transit;

import com.pb.tlumip.ts.NetworkHandlerIF;
import com.pb.tlumip.ts.assign.ShortestPathTreeH;

import com.pb.common.util.IndexSort;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;




public class AuxTrNet implements Serializable {

	protected static transient Logger logger = Logger.getLogger( AuxTrNet.class );

    public static final double UNCONNECTED = 0.0;
    public static final double INFINITY = 1.0e+30;
    public static final double NEG_INFINITY = Float.NEGATIVE_INFINITY;
	
	public static final int BOARDING_TYPE = 0;
	public static final int IN_VEHICLE_TYPE = 1;
	public static final int ALIGHTING_TYPE = 2;
	public static final int LAYOVER_TYPE = 3;
	public static final int AUXILIARY_TYPE = 4;

	static final int MAX_ROUTES = 500;

	static final double ALPHA = 0.5;
	static final double FARE = 0.75;
    static final double TRANSFER_FARE = 0.25;

	static final double MAX_WALK_ACCESS_DIST = 1.0;   // miles
    static final double MAX_DRIVE_ACCESS_DIST = 25.0; // miles
    static final int MAX_DRIVE_ACCESS_LINKS = 3;

  
	// OVT_COEFF is used as both a scale variable and as a utility coefficient.
	// It's value is assumed positive for use as a scale variable, and
	// whenever it is used as a coefficient, the resulting value is subtracted from utility,
	// thus using an implied negative sign.
//	static final double VALUE_OF_TIME		= 12.0;							//  $/hour
//	static final double IVT_COEFF			= 0.025;						// 	1/min
//	static final double OVT_COEFF			= 2.0*IVT_COEFF;				//  1/min (assumes IVT_COEFF is negative)
//	static final double COST_COEFF			= IVT_COEFF*60/VALUE_OF_TIME;	//	1/$
//	static final double FIRST_WAIT_COEFF	= 2.5*IVT_COEFF;				//  1/min
//	static final double XFR_WAIT_COEFF		= 2.0*FIRST_WAIT_COEFF;			//	1/min
//	static final double DRIVE_ACCESS_COEFF  = 4.0*IVT_COEFF;				//	1/min
//	static final double WALK_ACCESS_COEFF	= 2.0*IVT_COEFF;				//	1/min
//	static final double WALK_EGRESS_COEFF	= 2.0*IVT_COEFF;				//	1/min
//	static final double WALK_XFR_COEFF    	= 2.0*IVT_COEFF;				//  1/min
//	static final double TRANSFER_COEFF		= 2.0*IVT_COEFF;				//  1/xfrs

	static final double VALUE_OF_TIME		= 12.0;							//  $/hour
	static final double IVT_COEFF			= 1.0;	 						// 	1/min
	static final double OVT_COEFF			= 2.0;							//  1/min (assumes IVT_COEFF is negative)
	static final double COST_COEFF			= 0.0;							//	1/$
	static final double FIRST_WAIT_COEFF	= 2.0;							//  1/min
	static final double WAIT_COEFF			= 2.0;							//	1/min
	static final double DRIVE_ACCESS_COEFF  = 2.0;							//	1/min
	static final double WALK_ACCESS_COEFF	= 2.0;							//	1/min
	static final double WALK_EGRESS_COEFF	= 2.0;							//	1/min
	static final double WALK_XFR_COEFF    	= 2.0;							//  1/min
	static final double TRANSFER_COEFF		= 2.0;							//  1/xfrs

    NetworkHandlerIF nh = null;
	TrRoute tr;

	private int auxLinks, auxNodes;


	int[] hwyLink;
	int[] trRoute;
	int[] ia;
	int[] ib;
	int[] linkType;
	int[] ttf;
	double[] freq;
	double[] cost;
	double[] invTime;
	double[] dwellTime;
	double[] walkTime;
	double[] waitTime;
	double[] layoverTime;
	double[] flow;
	double[] driveAccTime;



	int[] ipa;
	int[] ipb;
	int[] indexa;
	int[] indexb;


    boolean[] boardingNode;
    
    Set[] nodeRoutes = null;
    
	int[] indexNode;
	int[] gia;
	int[] gib;
	String[] gMode;
    double[] gNodeX;
    double[] gNodeY;
    double[] gDist;
	double[] gCongestedTime;

	ArrayList[] gSegs = null;
	String accessMode = null;
    String period = null;


    
	public AuxTrNet (NetworkHandlerIF nh, TrRoute tr) {

        int maxAuxLinks = 300000;
//        int maxAuxLinks = nh.getLinkCount() + 3*tr.getTotalLinkCount() + 2*tr.getMaxRoutes();
		logger.info ("maxAuxLinks = " + maxAuxLinks + " in AuxTrNet() constructor.");

		hwyLink = new int[maxAuxLinks];
		trRoute = new int[maxAuxLinks];
		ia = new int[maxAuxLinks];
		ib = new int[maxAuxLinks];
		linkType = new int[maxAuxLinks];
		freq = new double[maxAuxLinks];
		cost = new double[maxAuxLinks];
		invTime = new double[maxAuxLinks];
		dwellTime = new double[maxAuxLinks];
		walkTime = new double[maxAuxLinks];
		waitTime = new double[maxAuxLinks];
		layoverTime = new double[maxAuxLinks];
		flow = new double[maxAuxLinks];
		driveAccTime = new double[maxAuxLinks];

        boardingNode = new boolean[nh.getNodeCount()+1];
        
        nodeRoutes = new Set[nh.getNodeCount()+1];
        
        
		gia = nh.getIa();
		gib = nh.getIb();
		gMode = nh.getMode();
		indexNode = nh.getIndexNode();
        gNodeX = nh.getNodeX();
        gNodeY = nh.getNodeY();
		gDist = nh.getDist();
		gCongestedTime = nh.getTransitTime();
		gSegs = new ArrayList[nh.getLinkCount()];
		
		for (int i=0; i < gSegs.length; i++)
			gSegs[i] = new ArrayList();
		
        this.nh = nh;
		this.tr = tr;
        this.period = nh.getTimePeriod();
	}


	public void buildAuxTrNet ( String accessMode ) {

	    this.accessMode = accessMode;
	    
		TrSegment ts;
		TrSegment tsNext;
		boolean debug = false;

		int aux = 0;
		int nextNode;
		int startNode;
		int startAuxNode;
		int anode=0;
		int bnode=0;


		// add boarding, in-vehicle, and alighting transit links for each link in a transit route
		nextNode = nh.getNodeCount() + 1;
		for (int rte=0; rte < tr.getLineCount(); rte++) {
			ts = (TrSegment)tr.transitPath[rte].get(0);
			startNode = gia[ts.link];
			startAuxNode = nextNode;
			if (debug) logger.info ("rte=" + rte + ", startNode=" + indexNode[startNode] + ", startAuxNode=" + startAuxNode);
			for (int seg=0; seg < tr.transitPath[rte].size(); seg++) {
				
				try {
					
					// get the current transit segment and the following segment in this route.
					// dwell time is determined for the following link, and added as in-vehicle time for the current in-vehicle link.
				    ts = (TrSegment)tr.transitPath[rte].get(seg);
				    if (seg < tr.transitPath[rte].size() - 2)
				    	tsNext = (TrSegment)tr.transitPath[rte].get(seg+1);
				    else
				    	tsNext = null;
				    
				    anode = gia[ts.link];
				    bnode = gib[ts.link];
				    
				    // Keep a list of transit lines using each network link.  Save using xxxyyy where xxx is rte and yyy is seg.
				    // A link will therefore be able to look up all transit routes serving the link using tr.transitPaths[rte].get(seg)
				    // for all the rteseg values stored for the link.
				    gSegs[ts.link].add( new Integer(rte*1000 + seg) );
				    
					if (debug) logger.info ("anode=" + indexNode[anode] + ", bnode=" + indexNode[bnode] + ", ttf=" + ts.getTtf() );
	
					    // add auxilliary links for route segment
				    if (ts.layover) {
				        if (bnode == startNode) {
				            nextNode++;
				        }
				        else {
				        	if (debug) logger.info ("mid-line layover:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + anode + ", bnode=" + bnode + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
				       	   	addAuxLayoverLink (aux++, nextNode, nextNode + 1, ts, tr.headway[rte], rte);
							nextNode++;
				        }
				    }
				    else {
				        if (ts.board) {
				            if (debug) logger.info ("regular board:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + anode + ", bnode=" + bnode + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
				           	addAuxBoardingLink (aux++, anode, nextNode, ts, tr.headway[rte], rte);
                            if ( nodeRoutes[anode] == null )
                                nodeRoutes[anode] = new HashSet();
                            nodeRoutes[anode].add(rte);
                            boardingNode[anode] = true;
				        }
				        if (debug) logger.info ("regular in-veh:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + anode + ", bnode=" + bnode + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
				       	addAuxInVehicleLink (aux++, nextNode, ts, tsNext, tr.headway[rte], tr.speed[rte], rte);
				   	   	if (ts.alight) {
							if (debug) logger.info ("regular alight:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + anode + ", bnode=" + bnode + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
							addAuxAlightingLink (aux++, nextNode, bnode, ts, tr.headway[rte], rte);
						}
						nextNode++;
					}
			    
				}
				catch (Exception e) {

					logger.fatal( "exception thrown while processing:");
					logger.fatal( "      rte = " + rte);
					logger.fatal( "      line = " + tr.getLine(rte));
					logger.fatal( "      description = " + tr.getDescription(rte));
					logger.fatal( "      anode = " + indexNode[anode]);
					logger.fatal( "      bnode = " + indexNode[bnode]);
                    logger.fatal ( "", e );
					System.exit(1);
				}
			}
		}

		auxNodes = nextNode;
		logger.info (aux + " transit links added.");
		logger.info (auxNodes + " is max transit node.");

        // add walk links first
        auxLinks =  getAccessLinks( accessMode, aux );
        logger.info ( (auxLinks-aux) + " " + accessMode + " access and walk egress links added.");
        logger.info ( auxLinks + " total transit and access links.");


		resizeAuxNetLinkAttributes ();
		
	}


	
	
	public void resizeAuxNetLinkAttributes () {

		int[] tempi1 = new int[auxLinks];
		int[] tempi2 = new int[auxLinks];
		int[] tempi3 = new int[auxLinks];
		int[] tempi4 = new int[auxLinks];
		int[] tempi5 = new int[auxLinks];
		double[] tempd1 = new double[auxLinks];
		double[] tempd2 = new double[auxLinks];
		double[] tempd3 = new double[auxLinks];
		double[] tempd4 = new double[auxLinks];
		double[] tempd5 = new double[auxLinks];
		double[] tempd6 = new double[auxLinks];
		double[] tempd7 = new double[auxLinks];


        System.arraycopy(hwyLink, 0, tempi1, 0, auxLinks);
        System.arraycopy(trRoute, 0, tempi2, 0, auxLinks);
        System.arraycopy(ia, 0, tempi3, 0, auxLinks);
        System.arraycopy(ib, 0, tempi4, 0, auxLinks);
        System.arraycopy(linkType, 0, tempi5, 0, auxLinks);
        System.arraycopy(freq, 0, tempd1, 0, auxLinks);
        System.arraycopy(cost, 0, tempd2, 0, auxLinks);
        System.arraycopy(invTime, 0, tempd3, 0, auxLinks);
        System.arraycopy(walkTime, 0, tempd4, 0, auxLinks);
        System.arraycopy(layoverTime, 0, tempd5, 0, auxLinks);
        System.arraycopy(flow, 0, tempd6, 0, auxLinks);
        System.arraycopy(driveAccTime, 0, tempd7, 0, auxLinks);


		hwyLink = tempi1;
		trRoute = tempi2;
		ia = tempi3;
		ib = tempi4;
		linkType = tempi5;
		freq = tempd1;
		cost = tempd2;
		invTime = tempd3;
		walkTime = tempd4;
		layoverTime = tempd5;
		flow = tempd6;
		driveAccTime = tempd7;

	}


	public void setForwardStarArrays () {
	    
		int k;
		int old;

		ipa = new int[auxNodes+1];
		Arrays.fill (ipa, -1);
		
		indexa = IndexSort.indexMergeSort( ia );
		logger.info ( "check on order of ia array returned " + IndexSort.checkAscendingOrder(ia, indexa) );
		
		old = ia[indexa[0]];
		ipa[old] = 0;
		for (int i=0; i < ia.length; i++) {
			k = indexa[i];

			if ( ia[k] != old ) {
				ipa[ia[k]] = i;
				old = ia[k];
			}
		}

		if ( old < auxNodes)
			ipa[old+1] = ia.length;
		else
			ipa[old] = ia.length;
		
	}

	
	public void setBackwardStarArrays () {
	    
		int k;
		int old;

		ipb = new int[auxNodes+1];
		Arrays.fill (ipb, -1);

		indexb = IndexSort.indexMergeSort( ib );
		logger.info ( "check on order of ib array returned " + IndexSort.checkAscendingOrder(ib, indexb) );
		
		old = ib[indexb[0]];
		ipb[old] = 0;
		for (int i=0; i < ib.length; i++) {

			k = indexb[i];

			if ( ib[k] != old ) {
				ipb[ib[k]] = i;
				old = ib[k];
			}
			
		}
		
		if ( old < auxNodes)
			ipb[old+1] = ib.length;
		else
			ipb[old] = ib.length;
		
	}

	
	private int getAccessLinks ( String accessMode, int aux ) {

        Iterator it;
        
        ArrayList walkAccessLinkList = null;
        ArrayList driveAccessLinkList = null;

        
        // get walk access links no matter the accessMode.
        walkAccessLinkList = getWalkAccessLinks();
        
        
        // get drive access links if the accessMode is "drive".
        if ( accessMode.equalsIgnoreCase("drive") )
            driveAccessLinkList = getDriveAccessLinks();
        
        
        // add the access links depending on value of accessMode.
        if ( accessMode.equalsIgnoreCase("walk") ) {

            it = walkAccessLinkList.iterator();
            while ( it.hasNext() ) {
                double[] linkInfo = (double[])it.next();
        
                hwyLink[aux] = -1;
                trRoute[aux] = -1;
                ia[aux] = (int)linkInfo[0];
                ib[aux] = (int)linkInfo[1];
                freq[aux] = INFINITY;
                cost[aux] = 0.0;
                invTime[aux] = 0.0;
                walkTime[aux] = (float)(60.0*linkInfo[2]/nh.getWalkSpeed());
                driveAccTime[aux] = 0.0;
                layoverTime[aux] = 0.0;
                linkType[aux] = AUXILIARY_TYPE;
                aux++;
        
            }

        }
        else {
        
            it = driveAccessLinkList.iterator();
            while ( it.hasNext() ) {
                double[] linkInfo = (double[])it.next();
        
                hwyLink[aux] = -1;
                trRoute[aux] = -1;
                ia[aux] = (int)linkInfo[0];
                ib[aux] = (int)linkInfo[1];
                freq[aux] = INFINITY;
                cost[aux] = 0.0;
                invTime[aux] = 0.0;
                walkTime[aux] = 0.0;
                driveAccTime[aux] = (float)linkInfo[2];
                layoverTime[aux] = 0.0;
                linkType[aux] = AUXILIARY_TYPE;
                aux++;
        
            }
        }
        
            
        // add the egress links which are walk for either accessMode.
        it = walkAccessLinkList.iterator();
        while ( it.hasNext() ) {
            double[] linkInfo = (double[])it.next();

            hwyLink[aux] = -1;
            trRoute[aux] = -1;
            ia[aux] = (int)linkInfo[1];
            ib[aux] = (int)linkInfo[0];
            freq[aux] = INFINITY;
            cost[aux] = 0.0;
            invTime[aux] = 0.0;
            walkTime[aux] = (float)(60.0*linkInfo[2]/nh.getWalkSpeed());
            driveAccTime[aux] = 0.0;
            layoverTime[aux] = 0.0;
            linkType[aux] = AUXILIARY_TYPE;
            aux++;
        }
            
		return aux;

    }
	
    
	
    private ArrayList getWalkAccessLinks() {
        
        ArrayList walkAccessLinkList = new ArrayList();
        
        // update the link costs based on current flows
        double[] linkCost = gDist;
        boolean[] validLinks = new boolean[nh.getLinkCount()];

        // build shortest path tree object and set cost and valid link attributes for this user class.
        ShortestPathTreeH sp = new ShortestPathTreeH( nh );
        
        // let any link in the network be used in shortest paths from origin to boarding nodes.
        Arrays.fill(validLinks, true);
        sp.setValidLinks( validLinks );
        sp.setLinkCost( linkCost );

        for (int origin=0; origin < nh.getNumCentroids(); origin++) {

            Set connectedRoutes = new HashSet();

            // build a shortest path tree from the origin and get a list of boarding nodes ordered by distance from origin and within walking distance or origin.
            sp.buildTree ( origin );
            ArrayList endPoints = sp.getNodesWithinCost ( MAX_WALK_ACCESS_DIST, boardingNode );
            
            
            // get a list of node pairs (origin,bnode) with shortest path distances to bnode to use to create walk access links.
            // the node pairs will only be selected for bnodes that serve different transit routes from previously selected node pairs.
            Iterator it = endPoints.iterator();
            while ( it.hasNext() ) {
                // get the node info for nodes within walk distance (ia, cumDist).
                double[] nodeInfo = (double[])it.next();

                // add each element in nodeRoutes to connectedRoutes.  The add method will only add the element to the connectedRoute set if it isn't already contained.
                int oldSize = connectedRoutes.size();
                Iterator rt = nodeRoutes[(int)nodeInfo[0]].iterator();
                while ( rt.hasNext() )
                    connectedRoutes.add( rt.next() );
                
                // if the updated connectedRoutes set has increased in size, create new walk access links to this boarding node
                if ( connectedRoutes.size() > oldSize ) {
                    
                    // make an array to hold ia, ib, dist for new walk access link, then store in ArrayList
                    double[] linkList = new double[3];
                    
                    linkList[0] = origin;
                    linkList[1] = nodeInfo[0];
                    linkList[2] = nodeInfo[1];
    
                    walkAccessLinkList.add(linkList);
                }

            }

        }

        return walkAccessLinkList;
    }
    
    
    
    private ArrayList getDriveAccessLinks() {
        
        ArrayList driveAccessLinkList = new ArrayList();
        
        // update the link costs based on current flows
        double[] linkCost = gCongestedTime;
        boolean[][] validLinksForClasses = nh.getValidLinksForAllClasses();

        // use auto userclass (m=0) for building paths
        int m = 0;

        double avgSpeed = 20.0;
        double increment = 3.0;
        
        // build shortest path tree object and set cost and valid link attributes for this user class.
        ShortestPathTreeH sp = new ShortestPathTreeH( nh );
        sp.setValidLinks( validLinksForClasses[m] );
        sp.setLinkCost( linkCost );

        for (int origin=0; origin < nh.getNumCentroids(); origin++) {

            sp.buildTree ( origin );
            
            // we'll add 3 miles incrementally to MAX_WALK_ACCESS_DIST until the desired number of drive access links are found
            double distanceIncrement = MAX_WALK_ACCESS_DIST + 3.0;
            
            // determine time bands based on incremental distances and an assumed average speed.
            // we'll get nodes from the shortest congested drive time tree that are within this band
            double minTime = 60.0*MAX_WALK_ACCESS_DIST/avgSpeed;
            double maxTime = 60.0*distanceIncrement/avgSpeed;
            
            // add distance incrementally until at least MIN_DRIVE_ACCESS_LINKS drive access links are found or MAX_DRIVE_ACCESS_DIST is reached
            ArrayList endPoints = sp.getNodesWithinCosts ( minTime, maxTime, boardingNode );
            while ( endPoints.size() < MAX_DRIVE_ACCESS_LINKS  && distanceIncrement < MAX_DRIVE_ACCESS_DIST ) {
                distanceIncrement += increment;
                minTime = maxTime;
                maxTime = 60.0*distanceIncrement/avgSpeed;
                endPoints.addAll( sp.getNodesWithinCosts ( minTime, maxTime, boardingNode ) );
            }

            
            // if no end points were found, not drive access links will be created for this origin zone
            if ( endPoints.size() > 0 ) {
            
                // select MIN_DRIVE_ACCESS_LINKS drive access links randomly from the set available
                int randomIndex;
                Set indexSet = new HashSet();
                while ( indexSet.size() < MAX_DRIVE_ACCESS_LINKS ) {
                    
                    randomIndex = (int)(Math.random()*endPoints.size());
                    while ( indexSet.contains(randomIndex) )
                        randomIndex = (int)(Math.random()*endPoints.size());
                    indexSet.add(randomIndex);
                    
                    double[] nodeInfo = (double[])endPoints.get(randomIndex);
    
                    // make an array to hold ia, ib, dist for new walk access link, then store in ArrayList
                    double[] linkList = new double[3];
                    linkList[0] = origin;
                    linkList[1] = nodeInfo[0];
                    linkList[2] = nodeInfo[1];
                    
                    driveAccessLinkList.add(linkList);
                }
            
            }
            
        }

        return driveAccessLinkList;
    }
    
    
    
    
	public void printAuxTrLinks (int rte, TrRoute tr) {

		int i, k, start=0, end;


		// find first transit segment from given route in auxilliary links array
		for (i=0; i < auxLinks; i++) {
			if (trRoute[i] == rte) {
				start = i;
				break;
			}
		}

		// find last transit segment from given route in auxilliary links array
		i = start;
		while(trRoute[i] == rte)
			i++;
		end = i;

		// print report of all auxilliary links associated with this route
		logger.info ("Transit route " + tr.getLine(rte) + ", " + tr.getDescription(rte));
		logger.info ( String.format( "%6s%6s%6s%6s%6s%6s%8s%8s%8s%8s%8s", "i", "link", "an", "bn", "ia", "ib", "freq", "cost", "invT", "walkT", "layT" ) );
		for (i=start; i < end; i++) {
			k = hwyLink[i];
            logger.info ( String.format( "%6d%6d%6d%6d%6d%6d%8s%8.2f%8.2f%8.2f%8.2f",
				i, k, indexNode[gia[k]], indexNode[gib[k]],ia[i],ib[i], (freq[i] == INFINITY ? String.format("%8s", "Inf") : String.format("%8.2f", freq[i])), cost[i], invTime[i], walkTime[i], layoverTime[i] ) );
		}
        
	}


	public void printAuxTranNetwork (String fileName) {

		int i, j, k, start, end, inB, outB, outI;

		try {
			PrintWriter out = new PrintWriter (
				new BufferedWriter (
					new FileWriter (fileName)));

		  	out.println ("--------------------------");
			out.println ("Auxilliary Transit Network");
		  	out.println ("--------------------------");
            out.format( "%8s%8s%8s%8s%8s%8s%8s%8s%8s%8s%8s%10s%10s%10s%10s%10s%10s\n", "i", "ia", "ib", "type", "link", "an", "bn", "inB", "outB", "outI", "rte", "freq", "cost", "invT", "walkT", "layT", "hwyT" );

		  	for (i=0; i < auxLinks; i++) {
				k = hwyLink[i];
				
				start = ipb[ib[i]];
				j = ib[i] + 1;
				if (j < ipb.length-1) {
					while (ipb[j] == -1 && j < ipb.length-1)
						j++;
				}
				end = ipb[j];
				inB = end - start;
				
				
				start = ipa[ib[i]];
				j = ib[i] + 1;
				if (j < ipa.length-1) {
					while (ipa[j] == -1 && j < ipa.length-1)
						j++;
				}
				end = ipa[j];
				outB = end - start;

				if (inB == 1 && outB == 1)
					outI = indexa[start];
				else
					outI = -1;
				
				
                out.format( "%8d%8d%8d%8d%8d%8s%8s%8d%8d%8d%8d%10s%10.2f%10.2f%10.2f%10.2f%10s\n",
                        i, ia[i], ib[i], linkType[i], k, (k >= 0 ? Integer.toString(indexNode[gia[k]]) : "accA"), (k >= 0 ? Integer.toString(indexNode[gib[k]]) : "accB"), inB, outB, outI, trRoute[i], (freq[i] == INFINITY ? String.format("%10s", "Inf") : String.format("%10.2f", freq[i])), cost[i], invTime[i], walkTime[i], layoverTime[i], (k >= 0 ? String.format("%10.2f", gCongestedTime[k]) : "acc time") );
	  		}

			out.close();

		} catch (IOException e) {
			logger.info ("Transit auxiliiary network file: " + fileName + ", could not be opened for writing,");
			logger.info ("or some other IO exception ocurred");
		}
	}


	boolean routeContainsNode (int rte, int node) {

		int i, start=0, end;

		// find first transit segment from given route in auxilliary links array
		for (i=0; i < auxLinks; i++) {
			if (trRoute[i] == rte) {
				start = i;
				break;
			}
		}

		// find last transit segment from given route in auxilliary links array
		i = start;
		while(trRoute[i] == rte)
			i++;
		end = i;

		// return true if specified node is contained in this route
		for (i=start; i < end; i++) {
			if (ia[i] == node || ib[i] == node)
			  return true;
	  }

	  return false;
	}


	void addAuxBoardingLink (int aux, int anode, int nextNode, TrSegment ts, double headway, int rte)	{
		// add boarding link to auxilliary link table
		hwyLink[aux] = ts.link;
		trRoute[aux] = rte;
		ia[aux] = anode;
		ib[aux] = nextNode;
		// WAIT_COEFF is assumed positive, so it is used here just to scale the link frequency
		freq[aux] = 1.0/(WAIT_COEFF*headway);
		cost[aux] = getLinkFare();
		invTime[aux] = 0.0;
		walkTime[aux] = 0.0;
		waitTime[aux] = 0.0;
		layoverTime[aux] = 0.0;
		linkType[aux] = BOARDING_TYPE;
	}


	void addAuxInVehicleLink (int aux, int nextNode, TrSegment ts, TrSegment tsNext, double headway, double speed, int rte) {
		// add in-vehicle link to auxilliary link table
		
		hwyLink[aux] = ts.link;
		trRoute[aux] = rte;
		ia[aux] = nextNode;
		ib[aux] = nextNode + 1;
		freq[aux] = INFINITY;
		cost[aux] = 0.0;
		walkTime[aux] = 0.0;
		waitTime[aux] = 0.0;
		
		if (ts.ttf > 0)
			invTime[aux] = nh.applyLinkTransitVdf( ts.link, ts.ttf );
		else
			invTime[aux] = (60.0*gDist[ts.link]/speed);
		
		layoverTime[aux] = 0.0;
		linkType[aux] = IN_VEHICLE_TYPE;
		
		if (tsNext != null ) {
			dwellTime[aux] = tsNext.getDwt();
		}
		else {
			dwellTime[aux] = -ts.getDwt()/gDist[ts.link];
		}
		
	}


	void addAuxAlightingLink (int aux, int nextNode, int bnode, TrSegment ts, double headway, int rte) {
		// add alighting link to auxilliary link table
		hwyLink[aux] = ts.link;
		trRoute[aux] = rte;
		ia[aux] = nextNode + 1;
		ib[aux] = bnode;
		freq[aux] = INFINITY;
		cost[aux] = 0.0;
		walkTime[aux] = 0.0;
		waitTime[aux] = 0.0;
		invTime[aux] = 0.0;
		layoverTime[aux] = 0.0;
		linkType[aux] = ALIGHTING_TYPE;
	}


	void addAuxLayoverLink (int aux, int nextNode, int layoverNode, TrSegment ts, double headway, int rte) {
		// add layover link to auxilliary link table
		hwyLink[aux] = ts.link;
		trRoute[aux] = rte;
		ia[aux] = nextNode;
		ib[aux] = layoverNode;
		freq[aux] = INFINITY;
		cost[aux] = 0.0;
		walkTime[aux] = 0.0;
		waitTime[aux] = 0.0;
		invTime[aux] = 0.0;
		layoverTime[aux] = ts.lay;
		linkType[aux] = LAYOVER_TYPE;
	}


	public void printTransitNodePointers () {

		int i, j, k;
		int start, end;


		// print report of all transit links and their node pointer attributes
		logger.info ("");
		logger.info ("------------------------------------");
		logger.info ("Transit Network Bnode Pointer Arrays");
		logger.info ("------------------------------------");
		logger.info ( String.format ("%10s%10s%10s%10s%10s%10s%10s", "i", "start", "end", "j", "k", "ia", "ib" ) );
	  	for (i=1; i < auxNodes; i++) {
			start = ipb[i];
			if (start >= 0) {
				j = i + 1;
				while (ipb[j] == -1)
		  			j++;
				end = ipb[j];
		  		for (j=start; j < end; j++) {
					k = indexb[j];
                    logger.info ( String.format ("%10d%10d%10d%10d%10d%10d%10d", i, start, end, j, k, ia[k], ib[k] ) );
				}
			}
		}

		logger.info ("-----------------------------------");
		logger.info ("");
		logger.info ("");
	}


	double getLinkInVehicleTime (int k) {
		return gCongestedTime[k];
	}


	// linkImped in optimal strategy is generalized cost, not including wait time.
	double getLinkImped (int k) {
		return (IVT_COEFF*(invTime[k] + dwellTime[k] + layoverTime[k]) + OVT_COEFF*walkTime[k] + COST_COEFF*cost[k]);
			
	}


	// for debugging purposes only
	// linkImped in optimal strategy is generalized cost, not including wait time.
	double getLinkImped (int k, int temp) {
		logger.info ("IVT_COEFF*(invTime[k] + dwellTime[k] + layoverTime[k])=" + String.format("%.4f", IVT_COEFF) + "*(" + String.format("%.4f", invTime[k]) + " + " + String.format("%.4f", dwellTime[k]) + " + " + String.format("%.4f", layoverTime[k]) + ")" );
		logger.info ("OVT_COEFF*(walkTime[k])=" + String.format("%.4f", OVT_COEFF) + "*(" + String.format("%.4f", walkTime[k]) + ")" );
		logger.info ("COST_COEFF*(cost[k])=" + String.format("%.4f", COST_COEFF) + "*(" + String.format("%.4f", cost[k]) + ")");

		return (IVT_COEFF*(invTime[k] + dwellTime[k] + layoverTime[k]) + OVT_COEFF*walkTime[k] + COST_COEFF*cost[k]);
	}

	
	double getMaxWalkAccessTime() {
	// return time in minutes to walk the maximum walk access distance
		return (60.0*(MAX_WALK_ACCESS_DIST/nh.getWalkSpeed()));
	}

	
	double getLinkFare () {
		return FARE;
	}


	int getAuxLinkCount () {
		return auxLinks;
	}


	int getAuxNodeCount () {
		return auxNodes;
	}


	public int getHighwayNodeCount () {
		return nh.getNodeCount();
	}


	public NetworkHandlerIF getHighwayNetworkHandler () {
	    return this.nh;
	}

    public int[] getHighwayNetworkNodeIndex() {
        return nh.getNodeIndex();
    }
    
    public int[] getHighwayNetworkIndexNode() {
        return nh.getIndexNode();
    }
    
    public String getAccessMode() {
        return accessMode;
    }

    public String getTimePeriod() {
        return period;
    }

    public int getMaxRoutes() {
        return MAX_ROUTES;
    }
    
    public TrRoute getTrRoute() {
        return tr;
    }
}

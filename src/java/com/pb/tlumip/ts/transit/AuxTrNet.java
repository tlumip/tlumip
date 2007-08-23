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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;




public class AuxTrNet implements Serializable {

	protected static transient Logger logger = Logger.getLogger( AuxTrNet.class );

    public static final double INFINITY = 1.0e+30;
    public static final double NEG_INFINITY = Float.NEGATIVE_INFINITY;
    public static final double UNCONNECTED = 0.0;
	
	public static final int BOARDING_TYPE = 0;
	public static final int IN_VEHICLE_TYPE = 1;
	public static final int ALIGHTING_TYPE = 2;
	public static final int LAYOVER_TYPE = 3;
	public static final int AUXILIARY_TYPE = 4;

	static final int MAX_ROUTES = 500;

    static final String[] routeTypeStrings = { "air", "hsr", "intercity", "intracity" };

	static final double ALPHA = 0.5;
	static final double FARE = 0.75;
    static final double TRANSFER_FARE = 0.25;

	static final double MAX_WALK_ACCESS_DIST = 1.0;   // miles
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
    private int maxHwyInternalNode;


	int[] hwyLink;
	int[] trRoute;
    int[] ia;
    int[] ib;
    int[] an;
    int[] bn;
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
	char[] rteMode;
	String[] routeType;
    

	int[] ipa;
	int[] ipb;
	int[] indexa;
	int[] indexb;


    boolean[][] boardingNode;
    
    Set[] nodeRoutes = null;
    
    int[] indexNode;
    int[] nodeIndex;
	int[] gia;
	int[] gib;
	String[] gMode;
    int[] gInternalNodeToNodeTableRow;
    double[] gNodeX;
    double[] gNodeY;
    double[] gDist;
	double[] gCongestedTime;

	ArrayList[] gSegs = null;
	String accessMode = null;
    String period = null;

    HashMap routeTypeMap = new HashMap();

    
    
    
	public AuxTrNet (NetworkHandlerIF nh, TrRoute tr) {

        int maxAuxLinks = 300000;
//        int maxAuxLinks = nh.getLinkCount() + 3*tr.getTotalLinkCount() + 2*tr.getMaxRoutes();
		logger.info ("maxAuxLinks = " + maxAuxLinks + " in AuxTrNet() constructor.");

		hwyLink = new int[maxAuxLinks];
		trRoute = new int[maxAuxLinks];
        ia = new int[maxAuxLinks];
        ib = new int[maxAuxLinks];
        an = new int[maxAuxLinks];
        bn = new int[maxAuxLinks];
        linkType = new int[maxAuxLinks];
        rteMode = new char[maxAuxLinks];
		freq = new double[maxAuxLinks];
		cost = new double[maxAuxLinks];
		invTime = new double[maxAuxLinks];
		dwellTime = new double[maxAuxLinks];
		walkTime = new double[maxAuxLinks];
		waitTime = new double[maxAuxLinks];
		layoverTime = new double[maxAuxLinks];
		flow = new double[maxAuxLinks];
		driveAccTime = new double[maxAuxLinks];

        boardingNode = new boolean[routeTypeStrings.length][nh.getNodeCount()+1];

        routeType = new String[maxAuxLinks];
        
        nodeRoutes = new Set[nh.getNodeCount()+1];
        
        maxHwyInternalNode = nh.getNodeCount();
        
		gia = nh.getIa();
		gib = nh.getIb();
		gMode = nh.getMode();
        indexNode = nh.getIndexNode();
        nodeIndex = nh.getNodeIndex();
        gInternalNodeToNodeTableRow = nh.getInternalNodeToNodeTableRow();
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
        int routeTypeIndex;
        
        for (int i=0; i < routeTypeStrings.length; i++)
            routeTypeMap.put(routeTypeStrings[i], i);
        

		// add boarding, in-vehicle, and alighting transit links for each link in a transit route
//        nextNode = nh.getNodeCount() + 1;
        nextNode = nh.getNodeCount();
		for (int rte=0; rte < tr.getLineCount(); rte++) {
			ts = (TrSegment)tr.transitPath[rte].get(0);
            
            String routeType = tr.routeType[rte];
            routeTypeIndex = (Integer)routeTypeMap.get(routeType);
            
			startNode = gia[ts.link];
			startAuxNode = nextNode;
			if (debug) logger.info ("rte=" + rte + ", startNode(ia)=" + startNode + ", startNode(an)=" + ts.an + ", startAuxNode=" + startAuxNode);
			
            
            int dummy=0;
            if ( rte == 318 ) {
                dummy = 1;
            }
            
            for (int seg=0; seg < tr.transitPath[rte].size(); seg++) {
				
				try {
					
                    if ( seg == 161 ) {
                        dummy = 2;
                    }
                    
					// get the current transit segment and the following segment in this route.
					// dwell time is determined for the following link, and added as in-vehicle time for the current in-vehicle link.
				    ts = (TrSegment)tr.transitPath[rte].get(seg);
				    if (seg < tr.transitPath[rte].size() - 2)
				    	tsNext = (TrSegment)tr.transitPath[rte].get(seg+1);
				    else
				    	tsNext = null;
				    
				    
				    // Keep a list of transit lines using each network link.  Save using xxxyyy where xxx is rte and yyy is seg.
				    // A link will therefore be able to look up all transit routes serving the link using tr.transitPaths[rte].get(seg)
				    // for all the rteseg values stored for the link.
                    // If link is a layover, there's no index, so skip it.
                    if ( ts.link >= 0 )
                        gSegs[ts.link].add( new Integer(rte*1000 + seg) );
				    
					if (debug) logger.info ("anode=" + ts.an + ", bnode=" + ts.bn + ", ttf=" + ts.getTtf() );
	
					    // add auxilliary links for route segment
				    if (ts.layover) {
				        if (nodeIndex[ts.bn] == startNode) {
				            nextNode++;
				        }
				        else {
				        	if (debug) logger.info ("mid-line layover:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + ts.an + ", bnode=" + ts.bn + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
				       	   	addAuxLayoverLink (aux++, nextNode, nextNode + 1, ts, tr.headway[rte], rte, tr.mode[rte]);
							nextNode++;
				        }
				    }
				    else {
				        if (ts.board) {
                            int inA = nodeIndex[ts.an];
				            if (debug) logger.info ("regular board:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + ts.an + ", bnode=" + ts.bn + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
				           	addAuxBoardingLink (aux++, inA, nextNode, ts, tr.headway[rte], rte, tr.mode[rte]);
                            if ( nodeRoutes[inA] == null )
                                nodeRoutes[inA] = new HashSet();
                            nodeRoutes[inA].add(rte);
                            boardingNode[routeTypeIndex][inA] = true;
				        }
				        if (debug) logger.info ("regular in-veh:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + ts.an + ", bnode=" + ts.bn + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
				       	addAuxInVehicleLink (aux++, nextNode, ts, tsNext, tr.headway[rte], tr.speed[rte], rte, tr.mode[rte]);
				   	   	if (ts.alight) {
							if (debug) logger.info ("regular alight:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + ts.an + ", bnode=" + ts.bn + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
                            int inB = nodeIndex[ts.bn];
							addAuxAlightingLink (aux++, nextNode, inB, ts, tr.headway[rte], rte, tr.mode[rte]);
						}
						nextNode++;
					}
			    
				}
				catch (Exception e) {

					logger.fatal( "exception thrown while processing:");
					logger.fatal( "      rte = " + rte);
					logger.fatal( "      line = " + tr.getLine(rte));
					logger.fatal( "      description = " + tr.getDescription(rte));
					logger.fatal( "      anode = " + ts.an);
                    logger.fatal( "      bnode = " + ts.bn);
                    logger.fatal( "      linkId = " + ts.link);
                    logger.fatal( "      alight = " + ts.alight);
                    logger.fatal( "      board = " + ts.board);
                    logger.fatal( "      layover = " + ts.layover);
                    logger.fatal ( "", e );
					System.exit(1);
				}
			}
		}

		logger.info (aux + " transit links added.");

        auxNodes = nextNode;
        logger.info (auxNodes + " is the max transit node.");

        // add walk links first
        logger.info ( "generating transit network access links..." );
        auxLinks =  getAuxilliaryLinks( accessMode, aux );
        logger.info ( (auxLinks-aux) + " " + accessMode + " transit total access, walk, and egress links added.");
        
        // the value of auxLinks is set so it can be the next index assigned, so it is one more than it needs to be; therefore decrement it.
        auxLinks--;
        
        logger.info ( auxLinks + " total transit and access links.");


		resizeAuxNetLinkAttributes ();
		
	}


	
	
	public void resizeAuxNetLinkAttributes () {

		int[] tempi1 = new int[auxLinks];
		int[] tempi2 = new int[auxLinks];
		int[] tempi3 = new int[auxLinks];
		int[] tempi4 = new int[auxLinks];
        int[] tempi5 = new int[auxLinks];
        int[] tempi6 = new int[auxLinks];
        int[] tempi7 = new int[auxLinks];
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
        System.arraycopy(an, 0, tempi6, 0, auxLinks);
        System.arraycopy(bn, 0, tempi7, 0, auxLinks);
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
        an = tempi6;
        bn = tempi7;
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
	    
        int i = -1;
        int k = -1;
		int old = -1;
        
        try {
            
            // auxNodes is the value of the highest internal node number, so auxNodes+1 is the number of internal node numbers
            // dimension ipa to the number of internal nodes + 1. i.e. auxNodes+2.
            ipa = new int[auxNodes+2];
            Arrays.fill (ipa, -1);
            
            indexa = IndexSort.indexMergeSort( ia );
            logger.info ( "check on order of ia array returned " + IndexSort.checkAscendingOrder(ia, indexa) );
            
            old = ia[indexa[0]];
            ipa[old] = 0;
            for (i=0; i < ia.length; i++) {
                k = indexa[i];

                if ( ia[k] != old ) {
                    ipa[ia[k]] = i;
                    old = ia[k];
                }
            }

            // set ipa of the highest node number + 1 to the number of links
            ipa[auxNodes+1] = ia.length;


            if ( logger.isDebugEnabled() ) {
                int count = 0;
                for (i=0; i < ipa.length; i++)
                    if ( ipa[i] < 0 ) {
                        count++;
                        logger.debug ( String.format("%-6d  node %d has no exiting links.", count, i) );
                    }
                
                logger.debug( String.format("forward star pointer array ipa created with: %d  -1 values,", count) );
            }
            
        }
        catch (Exception e) {
            logger.error (String.format("k=%d, old=%d, i=%d, auxNodes=%d, ia[k]=%s, ia.length=%d, ipa.length=%d, indexa=.length=%d", k, old, i, auxNodes, (k >= 0 && k < ia.length ? Integer.toString(ia[k]) : "index error"), ia.length, ipa.length, indexa.length), e);
        }

        
	}

	
	public void setBackwardStarArrays () {
	    
		int i = -1;
        int k = -1;
		int old = -1;

        try {
            
            // auxNodes is the value of the highest internal node number, so auxNodes+1 is the number of internal node numbers
            // dimension ipb to the number of internal nodes + 1. i.e. auxNodes+2.
    		ipb = new int[auxNodes+2];
    		Arrays.fill (ipb, -1);
    
    		indexb = IndexSort.indexMergeSort( ib );
    		logger.info ( "check on order of ib array returned " + IndexSort.checkAscendingOrder(ib, indexb) );
    		
    		old = ib[indexb[0]];
    		ipb[old] = 0;
    		for (i=1; i < ib.length; i++) {
                
    			k = indexb[i];
                
    			if ( ib[k] != old ) {
    				ipb[ib[k]] = i;
    				old = ib[k];
    			}
    			
    		}

            // set ipb of the highest node number + 1 to the number of links
            ipb[auxNodes+1] = ib.length;
            

            if ( logger.isDebugEnabled() ) {
                int count = 0;
                for (i=0; i < ipb.length; i++)
                    if ( ipb[i] < 0 ) {
                        count++;
                        logger.debug ( String.format("%-6d  node %d has no entering links.", count, i) );
                    }
                
                logger.debug( String.format("backward star pointer array ipb created with: %d  -1 values,", count) );
            }
		
        }
        catch (Exception e) {
            logger.error (String.format("k=%d, old=%d, i=%d, auxNodes=%d, ib[k]=%s, ib.length=%d, ipb.length=%d, indexb.length=%d", k, old, i, auxNodes, (k >= 0 && k < ib.length ? Integer.toString(ib[k]) : "index error"), ib.length, ipb.length, indexb.length), e);
        }
        
	}

	
	private int getAuxilliaryLinks ( String accessMode, int aux ) {

        Iterator it;
        
        ArrayList walkAccessLinkList = null;
        ArrayList driveAccessLinkList = null;

        int startAux = aux;
        
        
        // add the access links depending on value of accessMode.
        // if acces mode is walk, add walk access and walk egress.
        // if acces mode is drive, add drive local access and walk egress.
        // if acces mode is driveLdt, add drive long distance access and drive long distance egress.
        if ( accessMode.equalsIgnoreCase("walk") || accessMode.equalsIgnoreCase("drive") ) {

            // get walk access links no matter the accessMode.
            walkAccessLinkList = getWalkAccessLinks();
            
            
            if ( accessMode.equalsIgnoreCase("walk") ) {

                it = walkAccessLinkList.iterator();
                while ( it.hasNext() ) {
                    double[] linkInfo = (double[])it.next();
            
                    hwyLink[aux] = -1;
                    trRoute[aux] = -1;
                    routeType[aux] = routeTypeStrings[(int)linkInfo[3]];
                    ia[aux] = (int)linkInfo[0];
                    ib[aux] = (int)linkInfo[1];
                    an[aux] = indexNode[(int)linkInfo[0]];
                    bn[aux] = indexNode[(int)linkInfo[1]];
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
            
                // get drive access links if the accessMode is "drive".
                if ( accessMode.equalsIgnoreCase("drive") )
                    driveAccessLinkList = getDriveAccessLinks( 25.0 );      // max dist = 25 miles for drive to local transit

                it = driveAccessLinkList.iterator();
                while ( it.hasNext() ) {
                    double[] linkInfo = (double[])it.next();
            
                    hwyLink[aux] = -1;
                    trRoute[aux] = -1;
                    routeType[aux] = routeTypeStrings[(int)linkInfo[3]];
                    ia[aux] = (int)linkInfo[0];
                    ib[aux] = (int)linkInfo[1];
                    an[aux] = indexNode[(int)linkInfo[0]];
                    bn[aux] = indexNode[(int)linkInfo[1]];
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
        
            logger.info ( (aux-startAux) + " " + accessMode + " transit access links added.");
            startAux = aux;
            
            
            // add the egress links which are walk for either accessMode.
            it = walkAccessLinkList.iterator();
            while ( it.hasNext() ) {
                double[] linkInfo = (double[])it.next();
    
                hwyLink[aux] = -1;
                trRoute[aux] = -1;
                routeType[aux] = routeTypeStrings[(int)linkInfo[3]];
                ia[aux] = (int)linkInfo[1];
                ib[aux] = (int)linkInfo[0];
                an[aux] = indexNode[(int)linkInfo[1]];
                bn[aux] = indexNode[(int)linkInfo[0]];
                freq[aux] = INFINITY;
                cost[aux] = 0.0;
                invTime[aux] = 0.0;
                walkTime[aux] = (float)(60.0*linkInfo[2]/nh.getWalkSpeed());
                driveAccTime[aux] = 0.0;
                layoverTime[aux] = 0.0;
                linkType[aux] = AUXILIARY_TYPE;
                aux++;
            }

            logger.info ( (aux-startAux) + " " + accessMode + " transit walk egress links added.");

        }
        else if ( accessMode.equalsIgnoreCase("driveLdt") ) {
            
            driveAccessLinkList = getDriveAccessLinks( 50.0 );      // max dist = 50 miles for drive to long distance transit

            it = driveAccessLinkList.iterator();
            while ( it.hasNext() ) {
                double[] linkInfo = (double[])it.next();
        
                hwyLink[aux] = -1;
                trRoute[aux] = -1;
                routeType[aux] = routeTypeStrings[(int)linkInfo[3]];
                ia[aux] = (int)linkInfo[0];
                ib[aux] = (int)linkInfo[1];
                an[aux] = indexNode[(int)linkInfo[0]];
                bn[aux] = indexNode[(int)linkInfo[1]];
                freq[aux] = INFINITY;
                cost[aux] = 0.0;
                invTime[aux] = 0.0;
                walkTime[aux] = 0.0;
                driveAccTime[aux] = (float)linkInfo[2];
                layoverTime[aux] = 0.0;
                linkType[aux] = AUXILIARY_TYPE;
                aux++;
        
                hwyLink[aux] = -1;
                trRoute[aux] = -1;
                routeType[aux] = routeTypeStrings[(int)linkInfo[3]];
                ia[aux] = (int)linkInfo[1];
                ib[aux] = (int)linkInfo[0];
                an[aux] = indexNode[(int)linkInfo[1]];
                bn[aux] = indexNode[(int)linkInfo[0]];
                freq[aux] = INFINITY;
                cost[aux] = 0.0;
                invTime[aux] = 0.0;
                walkTime[aux] = 0.0;
                driveAccTime[aux] = (float)linkInfo[2];
                layoverTime[aux] = 0.0;
                linkType[aux] = AUXILIARY_TYPE;
                aux++;
        
            }
            
            logger.info ( (aux-startAux) + " " + accessMode + " transit drive access and drive egress links added.");

        }
        
        startAux = aux;
        
        // add the highway links that have mode s(sidewalk) or w(walk) to allow transfer possibilities.
        // these are the only walk links added at this time besides access links
        for (int k=0; k < gMode.length; k++){
        
            if ( gMode[k].indexOf('w') >= 0 || gMode[k].indexOf('s') >= 0 ) {
                
                hwyLink[aux] = k;
                trRoute[aux] = -1;
                routeType[aux] = "auxWalk";
                ia[aux] = gia[k];
                ib[aux] = gib[k];
                an[aux] = indexNode[gia[k]];
                bn[aux] = indexNode[gib[k]];
                freq[aux] = INFINITY;
                cost[aux] = 0.0;
                invTime[aux] = 0.0;
                walkTime[aux] = (float)(60.0*gDist[k]/nh.getWalkSpeed());
                driveAccTime[aux] = 0.0;
                layoverTime[aux] = 0.0;
                linkType[aux] = AUXILIARY_TYPE;
                aux++;
            }

        }
        
        logger.info ( (aux-startAux) + " transfer walk links added.");
        
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

            // build a shortest path tree from the origin.
            sp.buildTree ( origin );
            
            ArrayList[] endPoints = new ArrayList[routeTypeStrings.length];
            
            // get a list of boarding nodes ordered by distance from the origin and within walking distance of origin.
            // get one list for each routeType of transit routes defined
            for ( int i=0; i < routeTypeStrings.length; i++ ) {
                
                endPoints[i] = sp.getNodesWithinCost ( MAX_WALK_ACCESS_DIST, boardingNode[i] );
                
                // get a list of node pairs (origin,bnode) with shortest path distances to bnode to use to create walk access links.
                // the node pairs will only be selected for bnodes that serve different transit routes from previously selected node pairs within the specified routeType.
                Iterator it = endPoints[i].iterator();
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
                        double[] linkList = new double[4];
                        
                        linkList[0] = origin;
                        linkList[1] = nodeInfo[0];
                        linkList[2] = nodeInfo[1];
                        linkList[3] = (Integer)routeTypeMap.get( routeTypeStrings[i] );
        
                        walkAccessLinkList.add(linkList);
                    }

                }
                
            }

        }

        return walkAccessLinkList;
    }
    
    
    
    private ArrayList getDriveAccessLinks( double maxDriveAccessDist ) {
        
        ArrayList driveAccessLinkList = new ArrayList();
        
        // update the link costs based on current flows
        double[] linkCost = gCongestedTime;
        boolean[][] validLinksForClasses = nh.getValidLinksForAllClasses();

        // use auto userclass (m=0) for building paths
        int m = 0;

        double avgSpeed = 30.0;
        double increment = 2.0;
        double distanceIncrement = 0;
        // build shortest path tree object and set cost and valid link attributes for this user class.
        ShortestPathTreeH sp = new ShortestPathTreeH( nh );
        sp.setValidLinks( validLinksForClasses[m] );
        sp.setLinkCost( linkCost );

        for (int origin=0; origin < nh.getNumCentroids(); origin++) {

            int dummy = 0;
            if ( indexNode[origin] == 1000 ) {
                dummy = 1;
            }
            
            sp.buildTree ( origin );
            
            ArrayList[] endPoints = new ArrayList[routeTypeStrings.length];
            Set[] indexSet = new HashSet[routeTypeStrings.length];
            
            for ( int i=0; i < routeTypeStrings.length; i++ ) {

                // we'll add 3 miles incrementally to MAX_WALK_ACCESS_DIST until the desired number of drive access links are found
                distanceIncrement = MAX_WALK_ACCESS_DIST;
                
                
                // determine time bands based on incremental distances and an assumed average speed.
                // we'll get nodes from the shortest congested drive time tree that are within this band
                endPoints[i] = new ArrayList();
                double maxTime = 60.0*distanceIncrement/avgSpeed;
                double minTime = 0.0;
                
                // add distance incrementally until at least MIN_DRIVE_ACCESS_LINKS drive access links for this routeType
                // are found or MAX_DRIVE_ACCESS_DIST is reached.
                int selectedNodes = 0;
                while ( selectedNodes < MAX_DRIVE_ACCESS_LINKS  && distanceIncrement < maxDriveAccessDist ) {
                    distanceIncrement += increment;
                    minTime = maxTime;
                    maxTime = 60.0*distanceIncrement/avgSpeed;
                    endPoints[i] = sp.getNodesWithinCosts ( minTime, maxTime, boardingNode[i] );
                    indexSet[i] = new HashSet();

                    // if no end points were found, no drive access links will be created for this origin zone
                    if ( endPoints[i].size() > 0 ) {
                    
                        // select MIN_DRIVE_ACCESS_LINKS drive access links randomly from the set available
                        // or all drive access links if the set available is less than MIN_DRIVE_ACCESS_LINKS.
                        int randomIndex;
                        while ( selectedNodes < MAX_DRIVE_ACCESS_LINKS && indexSet[i].size() < MAX_DRIVE_ACCESS_LINKS && indexSet[i].size() < endPoints[i].size() ) {
                            
                            randomIndex = (int)(Math.random()*endPoints[i].size());
                            while ( indexSet[i].contains(randomIndex) )
                                randomIndex = (int)(Math.random()*endPoints[i].size());
                            indexSet[i].add(randomIndex);
                            
                            double[] nodeInfo = (double[])endPoints[i].get(randomIndex);
            
                            // make an array to hold ia, ib, dist, and routeTypeIndex for new walk access link, then store in ArrayList
                            double[] linkList = new double[4];
                            linkList[0] = origin;
                            linkList[1] = nodeInfo[0];
                            linkList[2] = nodeInfo[1];
                            linkList[3] = (Integer)routeTypeMap.get( routeTypeStrings[i] );
                            
                            selectedNodes++;
                            
                            driveAccessLinkList.add(linkList);
                        }

                    }

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
				i, k, an[k], bn[k], ia[i] ,ib[i], (freq[i] == INFINITY ? "Inf" : String.format("%8.2f", freq[i])), cost[i], invTime[i], walkTime[i], layoverTime[i] ) );
		}
        
	}


	public void printAuxTranNetwork (String fileName) {

		int i, k, m;
        double hwyTime;

		try {
			PrintWriter out = new PrintWriter (
				new BufferedWriter (
					new FileWriter (fileName)));

		  	out.println ("--------------------------");
			out.println ("Auxilliary Transit Network");
		  	out.println ("--------------------------");
            out.format( "%8s%8s%8s%8s%8s%8s%8s%8s%8s%8s%10s%10s%10s%10s%10s\n", "i", "k", "ia", "ib", "type", "link", "an", "bn", "rte", "freq", "cost", "invT", "walkT", "layT", "hwyT" );

            int[] linkIndices = getTransitLinkIndices();
		  	for (i=0; i < linkIndices.length; i++) {
                
                k = linkIndices[i];
                m = hwyLink[k];
                
                // get the equivalent highway network nodes, unless they don't exist (e.g. they are for a transit in-vehicle link)
                hwyTime = -1.0;
                if ( m >= 0 && m < maxHwyInternalNode ) {
                    hwyTime = gCongestedTime[m]; 
                }
                
                out.format( "%8d%8d%8d%8d%8d%8d%8d%8d%8s%8s%10.2f%10.2f%10.2f%10.2f%10.2f\n",
                        i, k, ia[k], ib[k], linkType[k], m, an[k], bn[k], trRoute[k], (freq[k] == INFINITY ? "Inf" : String.format("%10.2f", freq[k])), cost[k], invTime[k], walkTime[k], layoverTime[k], hwyTime );
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


    public int[] getRouteLinkIds (int rte) {
        return getHwyLinkIds (rte);
    }
    
    public int[] getRouteLinkIds (String rteName) {
        int rte = tr.getId(rteName);
        return getHwyLinkIds (rte);
    }
    
    // get an array of highway neytwork linkIds for the transit route index provided
    private int[] getHwyLinkIds (int rte) {

        ArrayList segList = tr.transitPath[rte];
        int[] tempIds = new int[segList.size()];
        
        int k = 0;
        for (int i=0; i < segList.size(); i++) {
            TrSegment ts = (TrSegment)segList.get(i);
            
            if ( ts.link > 0 && ts.layover == false )
                tempIds[k++] = ts.link;
        }

        int[] linkIds = new int[k];
        for (int i=0; i < linkIds.length; i++)
            linkIds[i] = tempIds[i];
        
        return linkIds;
        
    }


	void addAuxBoardingLink (int aux, int anode, int nextNode, TrSegment ts, double headway, int rte, char mode)	{
		// add boarding link to auxilliary link table
		hwyLink[aux] = ts.link;
		trRoute[aux] = rte;
		ia[aux] = anode;
		ib[aux] = nextNode;
        an[aux] = ts.an;
        bn[aux] = ts.bn;
		// WAIT_COEFF is assumed positive, so it is used here just to scale the link frequency
		freq[aux] = 1.0/(WAIT_COEFF*headway);
		cost[aux] = getLinkFare();
		invTime[aux] = 0.0;
		walkTime[aux] = 0.0;
		waitTime[aux] = 0.0;
		layoverTime[aux] = 0.0;
		linkType[aux] = BOARDING_TYPE;
        rteMode[aux] = mode;
	}


	void addAuxInVehicleLink (int aux, int nextNode, TrSegment ts, TrSegment tsNext, double headway, double speed, int rte, char mode) {
		// add in-vehicle link to auxilliary link table
		
		hwyLink[aux] = ts.link;
		trRoute[aux] = rte;
		ia[aux] = nextNode;
		ib[aux] = nextNode + 1;
        an[aux] = ts.an;
        bn[aux] = ts.bn;
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
		
        rteMode[aux] = mode;
	}


	void addAuxAlightingLink (int aux, int nextNode, int bnode, TrSegment ts, double headway, int rte, char mode) {
		// add alighting link to auxilliary link table
		hwyLink[aux] = ts.link;
		trRoute[aux] = rte;
		ia[aux] = nextNode + 1;
		ib[aux] = bnode;
        an[aux] = ts.an;
        bn[aux] = ts.bn;
		freq[aux] = INFINITY;
		cost[aux] = 0.0;
		walkTime[aux] = 0.0;
		waitTime[aux] = 0.0;
		invTime[aux] = 0.0;
		layoverTime[aux] = 0.0;
		linkType[aux] = ALIGHTING_TYPE;
        rteMode[aux] = mode;
	}


	void addAuxLayoverLink (int aux, int nextNode, int layoverNode, TrSegment ts, double headway, int rte, char mode) {
		// add layover link to auxilliary link table
		hwyLink[aux] = ts.link;
		trRoute[aux] = rte;
		ia[aux] = nextNode;
		ib[aux] = layoverNode;
        an[aux] = ts.an;
        bn[aux] = ts.bn;
		freq[aux] = INFINITY;
		cost[aux] = 0.0;
		walkTime[aux] = 0.0;
		waitTime[aux] = 0.0;
		invTime[aux] = 0.0;
		layoverTime[aux] = ts.lay;
		linkType[aux] = LAYOVER_TYPE;
        rteMode[aux] = mode; 
	}


	private int[] getTransitLinkIndices () {

		int i, j, k;
		int start, end;
        int count=0;
        
        boolean debug = false;


		// print report of all transit links and their node pointer attributes for debugging purposes
        if ( debug ) {
            logger.info ("");
            logger.info ("------------------------------------");
            logger.info ("Transit Network Bnode Pointer Arrays");
            logger.info ("------------------------------------");
            logger.info ( String.format ("%10s%10s%10s%10s%10s%10s%10s%10s%10s%10s%10s%10s%10s%10s", "count", "i", "links", "start", "end", "j", "k", "ia", "ib", "an", "bn", "linkType", "rte", "rteName" ) );
        }
        
        ArrayList linkIndicesList = new ArrayList();
        
        // auxNodes is the max internal node number
	  	for (i=0; i <= auxNodes; i++) {
			start = ipb[i];
			if (start >= 0) {
				j = i + 1;
				while (ipb[j] == -1)
		  			j++;
				end = ipb[j];
		  		for (j=start; j < end; j++) {
					k = indexb[j];
                    
                    linkIndicesList.add(k);
                    
                    count++;
                    if (debug)
                        logger.info ( String.format ("%10d%10d%10d%10d%10d%10d%10d%10d%10d%10d%10d%10d%10d%10s", count, i, linkIndicesList.size(), start, end, j, k, ia[k], ib[k], an[k], bn[k], linkType[k], trRoute[k], (trRoute[k] >= 0 ? tr.line[trRoute[k]] : "N/A") ) );
				}
			}
		}

        int[] linkIndices = new int[linkIndicesList.size()];
        for (i=0; i < linkIndices.length; i++)
            linkIndices[i] = (Integer)linkIndicesList.get(i);
        
        // the number of link indices determined should match auxLinks, so check that it does.
        if ( linkIndices.length != auxLinks ) {
            logger.warn( "Error detected while getting network indices." );
            logger.warn( String.format("linkIndices[] length = %d and auxLinks = %d, but they should be equal.", linkIndices.length, auxLinks) );
        }
        
        
        if ( debug ) {
    		logger.info ("-----------------------------------");
    		logger.info ("");
    		logger.info ("");
        }
        
        return linkIndices;
        
	}
    


	public double getLinkInVehicleTime (int k) {
		return gCongestedTime[k];
	}


	// linkImped in optimal strategy is generalized cost, not including wait time.
	public double getLinkImped (int k) {
		return (IVT_COEFF*(invTime[k] + dwellTime[k] + layoverTime[k]) + OVT_COEFF*walkTime[k] + COST_COEFF*cost[k]);
	}


	// for debugging purposes only
	// linkImped in optimal strategy is generalized cost, not including wait time.
	double getLinkImped (int k, int temp) {
        
        if ( logger.isDebugEnabled() ) {
    		logger.debug ("IVT_COEFF*(invTime[k] + dwellTime[k] + layoverTime[k])=" + String.format("%.4f", IVT_COEFF) + "*(" + String.format("%.4f", invTime[k]) + " + " + String.format("%.4f", dwellTime[k]) + " + " + String.format("%.4f", layoverTime[k]) + ")" );
    		logger.debug ("OVT_COEFF*(walkTime[k])=" + String.format("%.4f", OVT_COEFF) + "*(" + String.format("%.4f", walkTime[k]) + ")" );
    		logger.debug ("COST_COEFF*(cost[k])=" + String.format("%.4f", COST_COEFF) + "*(" + String.format("%.4f", cost[k]) + ")");
        }

		return (IVT_COEFF*(invTime[k] + dwellTime[k] + layoverTime[k]) + OVT_COEFF*walkTime[k] + COST_COEFF*cost[k]);
	}

	
	double getMaxWalkAccessTime() {
	// return time in minutes to walk the maximum walk access distance
		return (60.0*(MAX_WALK_ACCESS_DIST/nh.getWalkSpeed()));
	}

	
	double getLinkFare () {
		return FARE;
	}


	public int getAuxLinkCount () {
		return auxLinks;
	}


	public int getAuxNodeCount () {
		return auxNodes+1;
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
    
    public int getNumRoutes() {
        return tr.getLineCount();
    }
    
    public String getRouteName(int rte) {
        return tr.getLine(rte);
    }
    
    public String[] getRouteNames() {
        return tr.getRouteNames();
    }
    
    public String[] getRouteTypes() {
        return tr.getRouteTypes();
    }
    
    public TrRoute getTrRoute() {
        return tr;
    }
    
    public int[] getLinkTrRoute() {
        return trRoute;
    }
    
    public double[] getWaitTime() {
        return waitTime;
    }
    
    public double[] getWalkTime() {
        return walkTime;
    }
    
    public double[] getDriveAccTime() {
        return driveAccTime;
    }

    public double[] getDwellTime() {
        return dwellTime;
    }

    public double[] getLayoverTime() {
        return layoverTime;
    }

    public double[] getInvTime() {
        return invTime;
    }

    public double[] getFreq() {
        return freq;
    }

    public double[] getFlow() {
        return flow;
    }

    public int[] getLinkType() {
        return linkType;
    }

    public int[] getIa() {
        return ia;
    }
    
    public int[] getIb() {
        return ib;
    }
    
    public int[] getIpa() {
        return ipa;
    }
    
    public int[] getIpb() {
        return ipb;
    }
    
    public int[] getIndexa() {
        return indexa;
    }
    
    public int[] getIndexb() {
        return indexb;
    }
    
    public int[] getHwyLink() {
        return hwyLink;
    }
    
    public char[] getRteMode() {
        return rteMode;
    }
    
    public int[] getStationDriveAccessNodes(int stationNode) {
        
        ArrayList startNodeList = new ArrayList();
        
        for (int i=0; i < ib.length; i++) {
            if ( driveAccTime[i] > 0.0 && indexNode[ib[i]] == stationNode )
                startNodeList.add( indexNode[ia[i]] );
        }
        
        int[] startNodes = new int[startNodeList.size()];
        for (int i=0; i < startNodes.length; i++)
            startNodes[i] = (Integer)startNodeList.get(i);
        
        return startNodes;
        
    }

    public Vector getDriveAccessLinkCoords(Vector routeNames, Vector linkIds) {

        int i = 0;
        int j = 0;
        int k = 0;
        int m = 0;
        int r = 0;
        
        Vector linkInfoList = new Vector();
        
        try {
            
            // get a list of link ids for drive access links that connect to stations with boarding allowed on routes in the names list
            ArrayList validAccessNodeIds = new ArrayList();
            
            for (m=0; m < linkIds.size(); m++) {
                int id = (Integer)linkIds.get(m);
                
                int inA = gia[id];
                int start = ipa[inA];
                
                // node pointer array values can be -1 if node i has no links entering.
                // this can happen if i is a hwy node in a route determined by the shortest path between 2 hwy nodes.
                // boarding/alighting only occur at the 2 hwy node endpoints, and these are the only nodes with a direct relationship to the hwy ia and ib.
                // the internal nodes in this case are all auxilliary transit nodes for in-vehicle links with no direct association to a hwy node in the transit network representation.
                if ( start >= 0 ) {
                    
                    j = inA + 1;
                    while ( ipa[j] < 0 )
                        j++;
                    
                    int end = ipa[j];
                        
                    for(i=start; i < end; i++) {
                        
                        k = indexa[i];
                        
                        if ( linkType[k] == BOARDING_TYPE ) {
                            
                            for (r=0; r < routeNames.size(); r++) {
                                String name = (String)routeNames.get(r);
                                if ( tr.line[trRoute[k]].equalsIgnoreCase(name) ) {
                                    validAccessNodeIds.add(inA);
                                    break;
                                }
                            }

                        }
                        
                    }
                    
                }
                
            }
            
            
            
            Iterator it = validAccessNodeIds.iterator();
            while ( it.hasNext() ) {

                int inA = (Integer)it.next();
                           
                int start = ipb[inA];

                // get the start and end pointers for links ending at inA.
                j = inA + 1;
                while ( ipb[j] < 0 )
                    j++;
                
                int end = ipb[j];
                                    
                for(j=start; j < end; j++) {
                    
                    k = indexb[j];
                    
                    if ( linkType[k] == AUXILIARY_TYPE && driveAccTime[k] > 0.0 ) {
                        Vector linkInfo = new Vector();
                        
                        r = gInternalNodeToNodeTableRow[ia[k]];
                        linkInfo.add(indexNode[ia[k]]);
                        linkInfo.add(gNodeX[r]);
                        linkInfo.add(gNodeY[r]);
                        r = gInternalNodeToNodeTableRow[ib[k]];
                        linkInfo.add(indexNode[ib[k]]);
                        linkInfo.add(gNodeX[r]);
                        linkInfo.add(gNodeY[r]);
                        linkInfo.add(routeType[k]);
                        
                        linkInfoList.add(linkInfo);
                    }
                        
                }
                
            }
                
        }
        catch (Exception e) {
            logger.error ( String.format( "Exception in AuxTrNet.getDriveAccessLinkCoords(): m=%d, i=%d, j=%d, k=%d, startA=%d, endA=%d, startB=%d, endB=%d.", m, i, j, k, ipa[i], ipa[i+1], ipb[j], ipb[j+1]) );
            logger.error ( String.format( "     r=%d, rte=%d, rteName=%s.", r, trRoute[k], (trRoute[k] >= 0 ? tr.line[trRoute[k]] : "N/A") ) );
            for (int n=0; n < routeNames.size(); n++)
                logger.error ( String.format( "     %d   %-20s.", n, (String)routeNames.get(n) ) );
            logger.error ("", e);
        }
            
            
        return linkInfoList;
        
    }
    
    public Vector getCentroidTransitDriveAccessLinkCoords(Vector zones) {
        
        // zones is a Vector of external zone centroid numbers for which to get drive access links and coords.

        int j = 0;
        int k = 0;
        int r = 0;
        int inA = -1;
        int start = -1;
        int end = -1;
        Vector linkInfoList = new Vector();
        
        try {
            
            Iterator it = zones.iterator();
            while ( it.hasNext() ) {

                int exA = (Integer)it.next();
                inA = nodeIndex[exA];
                start = ipa[inA];

                if ( start < 0 )
                    continue;
                
                
                // get the start and end pointers for links exiting inA.
                j = inA + 1;
                while ( ipa[j] < 0 )
                    j++;
                
                end = ipa[j];
                                    
                for(j=start; j < end; j++) {
                    
                    k = indexa[j];
                    
                    if ( linkType[k] == AUXILIARY_TYPE && driveAccTime[k] > 0.0 ) {
                        Vector linkInfo = new Vector();
                        
                        r = gInternalNodeToNodeTableRow[ia[k]];
                        linkInfo.add(indexNode[ia[k]]);
                        linkInfo.add(gNodeX[r]);
                        linkInfo.add(gNodeY[r]);
                        r = gInternalNodeToNodeTableRow[ib[k]];
                        linkInfo.add(indexNode[ib[k]]);
                        linkInfo.add(gNodeX[r]);
                        linkInfo.add(gNodeY[r]);
                        linkInfo.add(routeType[k]);
                        
                        linkInfoList.add(linkInfo);
                    }
                        
                }
                
            }
                
        }
        catch (Exception e) {
            logger.error ( String.format( "Exception in AuxTrNet.getCentroidTransitDriveAccessLinkCoords(): j=%d, k=%d, start=%d, end=%d, inA=%d.", j, k, start, end, inA) );
            logger.error ("", e);
        }
            
            
        return linkInfoList;
        
    }
    
}

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

import com.pb.tlumip.ts.assign.Network;

import com.pb.common.util.IndexSort;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.log4j.Logger;




public class AuxTrNet implements Serializable {

	protected static transient Logger logger = Logger.getLogger( AuxTrNet.class );

	public static final double INFINITY = 1.0e+30;
	
	public static final int BOARDING_TYPE = 0;
	public static final int IN_VEHICLE_TYPE = 1;
	public static final int ALIGHTING_TYPE = 2;
	public static final int LAYOVER_TYPE = 3;
	public static final int AUXILIARY_TYPE = 4;

	static final int MAX_ROUTES = 500;

	static final double ALPHA = 0.5;
	static final double FARE = 0.75;

	static final double MAX_WALK_ACCESS_DIST = 2.0;   // miles

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

	Network g;
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


	int[] indexNode;
	int[] gia;
	int[] gib;
	String[] gMode;
	double[] gDist;
	double[] gCongestedTime;

	ArrayList[] gSegs = null;
	String accessMode = null;



	public AuxTrNet (int maxAuxLinks, Network g, TrRoute tr) {

		logger.info ("maxAuxLinks =" + maxAuxLinks + " in AuxTrNet() constructor.");

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


		gia = g.getIa();
		gib = g.getIb();
		gMode = g.getMode();
		indexNode = g.getIndexNode();
		gDist = g.getDist();
		gCongestedTime = g.getTransitTime();
		gSegs = new ArrayList[g.getLinkCount()];
		
		for (int i=0; i < gSegs.length; i++)
			gSegs[i] = new ArrayList();
		
		this.g = g;
		this.tr = tr;
	}


	public void buildAuxTrNet ( String accessMode ) {

	    this.accessMode = accessMode;
	    
		TrSegment ts;
		TrSegment tsNext;
		boolean debug = false;

		int aux;
		int nextNode;
		int startNode;
		int startAuxNode;
		int anode=0;
		int bnode=0;


		// add walk links first
		aux = getAccessLinks( accessMode );
		logger.info ( aux + " " + accessMode + " access links added.");


		// now add auxiliary transit links
		nextNode = g.getNodeCount() + 1;
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

		auxLinks = aux;
		auxNodes = nextNode;
		logger.info (auxLinks + " auxilliary transit links added.");
		logger.info (auxNodes + " is max auxilliary transit node.");

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

	
	private int getAccessLinks ( String accessMode ) {
	  
	    // add auxilliary links (walk or drive access, and walk links) to transit network
		int aux = 0;
		for (int i=0; i < g.getLinkCount(); i++) {
		    
			if ( accessMode.equalsIgnoreCase("walk") ) {

			    if ( (gia[i] >= g.getNumCentroids() && gib[i] >= g.getNumCentroids()) || gDist[i] > MAX_WALK_ACCESS_DIST || gMode[i].indexOf('w') >= 0 || gMode[i].indexOf('s') >= 0 ) {
			        // not a walk access link
			        continue;
			    }
				else {
					hwyLink[aux] = i;
					trRoute[aux] = -1;
					ia[aux] = gia[i];
					ib[aux] = gib[i];
					freq[aux] = INFINITY;
					cost[aux] = 0.0;
					invTime[aux] = 0.0;
					walkTime[aux] = g.getWalkTime( (float)gDist[i] );
					driveAccTime[aux] = 0.0;
					layoverTime[aux] = 0.0;
					linkType[aux] = AUXILIARY_TYPE;
					aux++;
				}
			
			}
			else if ( accessMode.equalsIgnoreCase("drive") ) {

				if ( (gia[i] >= g.getNumCentroids() && gib[i] >= g.getNumCentroids()) || gMode[i].indexOf('w') >= 0 || gMode[i].indexOf('s') >= 0 || gMode[i].indexOf('p') >= 0 || gMode[i].indexOf('k') >= 0 ) {
					// keep link only if it's a pnr, knr, or walk (but no walks at origin)
					continue;
				}
				else {
				    if (gia[i] < g.getNumCentroids() && gMode[i].indexOf('w') >= 0) {
				        continue;
				    }
				    
				    
					hwyLink[aux] = i;
					trRoute[aux] = -1;
					ia[aux] = gia[i];
					ib[aux] = gib[i];
					freq[aux] = INFINITY;
					cost[aux] = 0.0;
					invTime[aux] = 0.0;
					if (gMode[i].indexOf('w') >= 0) {
					    walkTime[aux] = g.getWalkTime( (float)gDist[i] );
					    driveAccTime[aux] = 0.0;
					}
					else {
						walkTime[aux] = 0.0;
					    driveAccTime[aux] = gCongestedTime[i];
					}
					layoverTime[aux] = 0.0;
					linkType[aux] = AUXILIARY_TYPE;
					aux++;
				}
			
			}
		}
		
		return aux;
	}
	
	
	
//	private void calculateInVehicleTimes() {
//		
//		for (int i=0; i < gSegs.length; i++) {
//			
//			// if this link doesn't serve any transit routes, skip to next link
//			if (gSegs[i].size() == 0)
//				continue;
//			
//			
//			// loop through the transit lines served by this link and accumualte travel time and headway
//			double[] times = new double[gSegs[i].size()];
//			double[] hdwys = new double[gSegs[i].size()];
//			double totalTime = 0.0;
//			double totalHeadway = 0.0;
//			for (int j=0; j < gSegs[i].size(); j++) {
//				Integer rteSeg = (Integer)gSegs[i].get(j);
//				int rte = rteSeg.intValue()/1000;
//				int seg = rteSeg.intValue() - 1000*rte;
//				TrSegment ts = (TrSegment)tr.transitPath[rte].get(seg);
//				times[j] = g.applyLinkTransitVdf( ts.link, ts.ttf );
//				hdwys[j] = tr.getHeadway(rte);
//				totalTime += times[j];
//				totalHeadway += hdwys[j];
//			}
//
//		}
//		
//	}


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
				
				
                out.format( "%8d%8d%8d%8d%8d%8d%8d%8d%8d%8d%8d%10s%10.2f%10.2f%10.2f%10.2f%10.2f\n",
                        i, ia[i], ib[i], linkType[i], k, indexNode[gia[k]], indexNode[gib[k]], inB, outB, outI, trRoute[i], (freq[i] == INFINITY ? String.format("%10s", "Inf") : String.format("%10.2f", freq[i])), cost[i], invTime[i], walkTime[i], layoverTime[i], gCongestedTime[k] );
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
			invTime[aux] = g.applyLinkTransitVdf( ts.link, ts.ttf );
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
		return (60.0*(MAX_WALK_ACCESS_DIST/g.getWalkSpeed()));
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
		return g.getNodeCount();
	}


	public Network getHighwayNetwork () {
	    return this.g;
	}

    public int[] getHighwayNetworkNodeIndex() {
        return g.getNodeIndex();
    }
    
    public int[] getHighwayNetworkIndexNode() {
        return g.getIndexNode();
    }
    
	public String getAccessMode() {
	    return accessMode;
	}

    public int getMaxRoutes() {
        return MAX_ROUTES;
    }
    
    public TrRoute getTrRoute() {
        return tr;
    }
}

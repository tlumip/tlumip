package com.pb.despair.ts.transit;

import com.pb.despair.ts.assign.Network;

import com.pb.common.util.IndexSort;
import com.pb.common.util.Justify;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;




public class AuxTrNet implements Serializable {

	protected static transient Logger logger = Logger.getLogger("com.pb.despair.ts.transit");

	public static final double INFINITY = 1.0e+30;

	static final int MAX_ROUTES = 500;

	static final float ALPHA = 0.5f;
	static final float FARE = 0.75f;

//	static final double MAX_WALK_ACCESS_DIST = 2.0;   // miles
	static final double MAX_WALK_ACCESS_DIST = 100.0;   // miles

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
	static final double OVT_COEFF			= 3.0;							//  1/min (assumes IVT_COEFF is negative)
	static final double COST_COEFF			= 0.0;							//	1/$
	static final double FIRST_WAIT_COEFF	= 3.0;							//  1/min
	static final double XFR_WAIT_COEFF		= 3.0;							//	1/min
	static final double DRIVE_ACCESS_COEFF  = 3.0;							//	1/min
	static final double WALK_ACCESS_COEFF	= 3.0;							//	1/min
	static final double WALK_EGRESS_COEFF	= 3.0;							//	1/min
	static final double WALK_XFR_COEFF    	= 3.0;							//  1/min
	static final double TRANSFER_COEFF		= 3.0;							//  1/xfrs

	Network g;
	TrRoute tr;
	Justify myFormat = new Justify();

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
	double[] walkTime;
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
		walkTime = new double[maxAuxLinks];
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
	    
		double headway;
		TrSegment ts;
		boolean debug = false;

		int aux;
		int nextNode;
		int startNode;
		int startAuxNode;
		int anode;
		int bnode;


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
			    ts = (TrSegment)tr.transitPath[rte].get(seg);

			    anode = gia[ts.link];
			    bnode = gib[ts.link];
			    
			    // Keep a list of transit lines using each network link.  Save using xxxyyy where xxx is rte and yyy is seg.
			    // A link will therefore be able to look up all transit routes serving the link using tr.transitPaths[rte].get(seg)
			    // for all the rteseg values stored for the link.
			    gSegs[ts.link].add( Integer.valueOf(rte*1000 + seg) );
			    
				if (debug) logger.info ("anode=" + indexNode[anode] + ", bnode=" + indexNode[bnode] + ", ttf=" + ts.getTtf() );

				    // add auxilliary links for route segment
			    if (ts.layover) {
			        if (anode == startNode) {
			            if (debug) logger.info ("layover at startnode:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + anode + ", bnode=" + bnode + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
			           	addAuxLayoverLink (aux++, nextNode, startAuxNode, ts, rte);
			            nextNode++;
			        }
			        else {
			        	if (debug) logger.info ("mid-line layover:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + anode + ", bnode=" + bnode + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
			       	   	addAuxLayoverLink (aux++, nextNode, nextNode + 1, ts, rte);
						nextNode++;
			        	if (ts.board) {
							if (debug) logger.info ("board after layover:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + anode + ", bnode=" + bnode + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
			       	       	addAuxBoardingLink (aux++, anode, nextNode, ts, tr.headway[rte], rte);
			       	   	}
						if (debug) logger.info ("in-veh after layover:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + anode + ", bnode=" + bnode + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
			       	  	addAuxInVehicleLink (aux++, nextNode, ts, rte);
			        	if (ts.alight) {
			        	    if (debug) logger.info ("alight after layover:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + anode + ", bnode=" + bnode + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
			       	    	addAuxAlightingLink (aux++, nextNode, bnode, ts, rte);
			        	}
			        	nextNode++;
			        }
			    }
			    else {
			        if (ts.board) {
			            if (debug) logger.info ("regular board:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + anode + ", bnode=" + bnode + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
			           	addAuxBoardingLink (aux++, anode, nextNode, ts, tr.headway[rte], rte);
			        }
			        if (debug) logger.info ("regular in-veh:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + anode + ", bnode=" + bnode + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
			       	addAuxInVehicleLink (aux++, nextNode, ts, rte);
			   	   	if (ts.alight) {
						if (debug) logger.info ("regular alight:  aux=" + aux + ", nextNode=" + nextNode + ", anode=" + anode + ", bnode=" + bnode + ", ts.board=" + ts.board + ", ts.alight=" + ts.alight + ", ts.layover=" + ts.layover);
						addAuxAlightingLink (aux++, nextNode, bnode, ts, rte);
					}
					nextNode++;
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
		ipa[old+1] = ia.length;
		
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
		ipb[old+1] = ib.length;
		
	}

	
	private int getAccessLinks ( String accessMode ) {
	  
	    // add highway access (walk or drive) links to auxilliary transit network
		int aux = 0;
		for (int i=0; i < g.getLinkCount(); i++) {
		    

			if ( accessMode.equalsIgnoreCase("walk") ) {

			    if ( gMode[i].indexOf('w') < 0 && gMode[i].indexOf('s') < 0 ) {
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
					linkType[aux] = 4;
					aux++;
				}
			
			}
			else if ( accessMode.equalsIgnoreCase("drive") ) {

				if ( gMode[i].indexOf('w') < 0 && gMode[i].indexOf('p') < 0 && gMode[i].indexOf('k') < 0 ) {
					// keep link only if it's a pnr, knr, or walk (but no walks at origin)
					continue;
				}
				else {
				    if (gia[i] < g.getNumCentroids() && gMode[i].indexOf('w') >= 0) {
				        continue;
				    }
				    
				    int dummy=0;
				    if (aux == 4130) {
				        dummy = 1;
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
					linkType[aux] = 4;
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
		logger.info (
			myFormat.right("i", 6) +
			myFormat.right("link", 6) +
			myFormat.right("an", 6) +
			myFormat.right("bn", 6) +
			myFormat.right("ia", 6) +
			myFormat.right("ib", 6) +
			myFormat.right("freq", 8) +
			myFormat.right("cost", 8) +
			myFormat.right("invT", 8) +
			myFormat.right("walkT", 8) +
			myFormat.right("layT", 8));
		for (i=start; i < end; i++) {
			k = hwyLink[i];
			logger.info (
				myFormat.right(i, 6) +
				myFormat.right(k, 6) +
				myFormat.right(indexNode[gia[k]], 6) +
				myFormat.right(indexNode[gib[k]], 6) +
				myFormat.right(ia[i], 6) +
				myFormat.right(ib[i], 6) + 
				(freq[i] == INFINITY ? myFormat.right ("Inf",8) : myFormat.right(myFormat.df2.format(freq[i]), 8)) +
				myFormat.right(myFormat.df2.format(cost[i]), 8) +
				myFormat.right(myFormat.df2.format(invTime[i]), 8) +
				myFormat.right(myFormat.df2.format(walkTime[i]), 8) +
				myFormat.right(myFormat.df2.format(layoverTime[i]), 8));
	  }
	}


	public void printAuxTranNetwork (String fileName) {

		int i, k, start=0, end;

		try {
			PrintWriter out = new PrintWriter (
				new BufferedWriter (
					new FileWriter (fileName)));

		  	out.println ("");
			out.println ("--------------------------");
			out.println ("Auxilliary Transit Network");
		  	out.println ("--------------------------");
			out.println (
				myFormat.right("i", 8) +
			 	myFormat.right("ia", 8) +
				myFormat.right("ib", 8) +
				myFormat.right("type", 8) +
				myFormat.right("link", 8) +
		  		myFormat.right("an", 8) +
				myFormat.right("bn", 8) +
				myFormat.right("rte", 8) +
				myFormat.right("freq", 10) +
		  		myFormat.right("cost", 10) +
				myFormat.right("invT", 10) +
				myFormat.right("walkT", 10) +
				myFormat.right("layT", 10) +
			  	myFormat.right("hwyT", 10));
		  	for (i=0; i < auxLinks; i++) {
				k = hwyLink[i];
				out.println (
					myFormat.right(i, 8) +
					myFormat.right(ia[i], 8) +
					myFormat.right(ib[i], 8) +
					myFormat.right(linkType[i], 8) +
					myFormat.right(k, 8) +
			  		myFormat.right(indexNode[gia[k]], 8) +
					myFormat.right(indexNode[gib[k]], 8) +
					myFormat.right(trRoute[i], 8) +
					(freq[i] == INFINITY ? myFormat.right ("Inf", 10) : myFormat.right(freq[i], 10, 2)) +
					myFormat.right(cost[i], 10, 2) +
			  		myFormat.right(invTime[i], 10, 2) +
					myFormat.right(walkTime[i], 10, 2) +
					myFormat.right(layoverTime[i], 10, 2) +
					myFormat.right(gCongestedTime[k], 10, 2));
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
		// OVT_COEFF is assumed positive, so it is used here just to scale the link frequency
		freq[aux] = 1.0/(headway*ALPHA*OVT_COEFF);
		cost[aux] = getLinkFare();
		invTime[aux] = 0.0;
		walkTime[aux] = 0.0;
		layoverTime[aux] = 0.0;
		linkType[aux] = 0;
	}


	void addAuxInVehicleLink (int aux, int nextNode, TrSegment ts, int rte) {
		// add in-vehicle link to auxilliary link table
		hwyLink[aux] = ts.link;
		trRoute[aux] = rte;
		ia[aux] = nextNode;
		ib[aux] = nextNode + 1;
		freq[aux] = INFINITY;
		cost[aux] = 0.0;
		walkTime[aux] = 0.0;
		invTime[aux] = g.applyLinkTransitVdf( ts.link, ts.ttf );
		layoverTime[aux] = 0.0;
		linkType[aux] = 1;
	}


	void addAuxAlightingLink (int aux, int nextNode, int bnode, TrSegment ts, int rte) {
		// add alighting link to auxilliary link table
		hwyLink[aux] = ts.link;
		trRoute[aux] = rte;
		ia[aux] = nextNode + 1;
		ib[aux] = bnode;
		freq[aux] = INFINITY;
		cost[aux] = 0.0;
		walkTime[aux] = 0.0;
		invTime[aux] = 0.0;
		layoverTime[aux] = 0.0;
		linkType[aux] = 2;
	}


	void addAuxLayoverLink (int aux, int nextNode, int layoverNode, TrSegment ts, int rte) {
		// add layover link to auxilliary link table
		hwyLink[aux] = ts.link;
		trRoute[aux] = rte;
		ia[aux] = nextNode;
		ib[aux] = layoverNode;
		freq[aux] = INFINITY;
		cost[aux] = 0.0;
		walkTime[aux] = 0.0;
		invTime[aux] = 0.0;
		layoverTime[aux] = ts.lay;
		linkType[aux] = 3;
	}


	public void printTransitNodePointers () {

		int i, j, k;
		int start, end;


		// print report of all transit links and their node pointer attributes
		logger.info ("");
		logger.info ("------------------------------------");
		logger.info ("Transit Network Bnode Pointer Arrays");
		logger.info ("------------------------------------");
		logger.info (
			myFormat.right("i", 10) +
			myFormat.right("start", 10) +
			myFormat.right("end", 10) +
			myFormat.right("j", 10) +
			myFormat.right("k", 10) +
			myFormat.right("ia", 10) +
			myFormat.right("ib", 10));
	  	for (i=1; i < auxNodes; i++) {
			start = ipb[i];
			if (start >= 0) {
				j = i + 1;
				while (ipb[j] == -1)
		  			j++;
				end = ipb[j];
		  		for (j=start; j < end; j++) {
					k = indexb[j];
					logger.info (
						myFormat.right(i, 10) +
						myFormat.right(start, 10) +
						myFormat.right(end, 10) +
						myFormat.right(j, 10) +
						myFormat.right(k, 10) +
						myFormat.right(ia[k], 10) +
						myFormat.right(ib[k], 10));
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


	double getUtility (int k) {
		return (IVT_COEFF*(invTime[k]) + COST_COEFF*cost[k] + OVT_COEFF*(walkTime[k] + layoverTime[k]));
	}


	// for debugging purposes only
	double getUtility (int k, int temp) {
		logger.info ("IVT_COEFF*(invTime[k])=" + myFormat.df4.format(IVT_COEFF) + "*(" + myFormat.df4.format(invTime[k]) + ")");
		logger.info ("COST_COEFF*(cost[k])=" + myFormat.df4.format(COST_COEFF) + "*(" + myFormat.df4.format(cost[k]) + ")");
		logger.info ("OVT_COEFF*(walkTime[k] + layoverTime[k])=" + myFormat.df4.format(OVT_COEFF) + "*(" + myFormat.df4.format(walkTime[k]) + " + " + myFormat.df4.format(layoverTime[k]) + ")");

		return (IVT_COEFF*(invTime[k]) + COST_COEFF*cost[k] + OVT_COEFF*(walkTime[k] + layoverTime[k]));
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

	
	public String getAccessMode() {
	    return accessMode;
	}

}

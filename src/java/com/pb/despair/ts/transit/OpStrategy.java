package com.pb.despair.ts.transit;

import com.pb.despair.ts.assign.Network;
import com.pb.despair.ts.assign.ShortestPath;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.IndexSort;
import com.pb.common.util.Justify;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;




public class OpStrategy {


	protected static Logger logger = Logger.getLogger("com.pb.despair.ts.transit");

	public static final int IVT = 0;
	public static final int FWT = 1;
	public static final int TWT = 2;
	public static final int AUX = 3;
	public static final int BRD = 4;
	public static final int FAR = 5;
	public static final int NUM_SKIMS = 6;

	static final double COMPARE_EPSILON = 1.0e-07;

	static final double MIN_ALLOCATED_FLOW = 0.00001;
	
	static final int MAX_BOARDING_LINKS = 100;
	static final double DRIVE_STOP_PCTS = 1;
	static final double MIN_EXP = -308;
	static final double MAX_EXP =  308;

//	static final double LATITUDE_PER_FEET  = 2.7;
//	static final double LONGITUDE_PER_FEET = 3.6;
	static final double LATITUDE_PER_FEET  = 1.0;
	static final double LONGITUDE_PER_FEET = 1.0;

	Justify myFormat = new Justify();

	AuxTrNet ag;
	Network g;

	int dest;
	
	Heap candidateHeap;
	int[] heapContents;

//	int orig, dest;
	double[] nodeLabel, nodeFreq, linkLabel;
	boolean[] tested, inStrategy;
	int[] orderInStrategy;
	int[] strategyOrderForLink;
	
	double[] nodeAccWalkTime;
	double[] nodeEgrWalkTime;
	double[] nodeTotWalkTime;
	double[] nodeFirstWaitTime;
	double[] nodeTotalWaitTime;
	double[] nodeInVehTime;
	double[] nodeDriveAccTime;
	double[] nodeCost;
	double[] nodeBoardings;
	double[] nodeFlow;
	
	double[] accessTime = null;
	
	int[] gia;
	int[] gib;
	int[] indexNode;
	int[] nodeIndex;
	double[] gNodeX;
	double[] gNodeY;

	int inStrategyCount;
	
	boolean classDebug = false;
	
	
	
	public OpStrategy ( AuxTrNet ag ) {

		this.ag = ag;
		this.g = ag.getHighwayNetwork();

		nodeFlow = new double[ag.getAuxNodeCount()+1];
		nodeLabel = new double[ag.getAuxNodeCount()+1];
		nodeFreq = new double[ag.getAuxNodeCount()+1];
		linkLabel = new double[ag.getAuxLinkCount()+1];
		tested = new boolean[ag.getAuxLinkCount()+1];
		inStrategy = new boolean[ag.getAuxLinkCount()+1];
		orderInStrategy = new int[ag.getAuxLinkCount()+1];
		strategyOrderForLink = new int[ag.getAuxLinkCount()+1];
		
		//node skims
		nodeAccWalkTime = new double[ag.getAuxNodeCount()+1];
		nodeEgrWalkTime = new double[ag.getAuxNodeCount()+1];
		nodeTotWalkTime = new double[ag.getAuxNodeCount()+1];
		nodeFirstWaitTime = new double[ag.getAuxNodeCount()+1];
		nodeTotalWaitTime = new double[ag.getAuxNodeCount()+1];
		nodeInVehTime = new double[ag.getAuxNodeCount()+1];
		nodeDriveAccTime = new double[ag.getAuxNodeCount()+1];
		nodeCost = new double[ag.getAuxNodeCount()+1];
		nodeBoardings = new double[ag.getAuxNodeCount()+1];

		//Create a new heap structure to sort candidate node labels
		candidateHeap = new Heap(ag.getAuxNodeCount()+1);
		heapContents = new int[ag.getAuxNodeCount()+1];

		
		gia = g.getIa();
		gib = g.getIb();
		indexNode = g.getIndexNode();
		nodeIndex = g.getNodeIndex();
		gNodeX = g.getNodeX();
		gNodeY = g.getNodeY();
		
	}


	private void initData() {
		Arrays.fill(nodeLabel, AuxTrNet.INFINITY);
		Arrays.fill(nodeFlow, 0.0);
		Arrays.fill(nodeFreq, 0.0);
		Arrays.fill(linkLabel, 0.0);
		Arrays.fill(tested, false);
		Arrays.fill(inStrategy, false);
		Arrays.fill(orderInStrategy, 0);
		Arrays.fill(strategyOrderForLink, -1);

		inStrategyCount = 0;
		candidateHeap.clear();
	}



	public void initSkims () {

	    boolean debug = classDebug;
	    
	    if (debug) {
	        logger.info("");
	        logger.info("initializing node skims arrays for dest=" + dest);
	    }

		Arrays.fill (nodeEgrWalkTime, -AuxTrNet.INFINITY);
		Arrays.fill (nodeTotWalkTime, -AuxTrNet.INFINITY);
		Arrays.fill (nodeTotalWaitTime, -AuxTrNet.INFINITY);
		Arrays.fill (nodeInVehTime, -AuxTrNet.INFINITY);
		Arrays.fill (nodeDriveAccTime, -AuxTrNet.INFINITY);
		Arrays.fill (nodeCost, -AuxTrNet.INFINITY);
		Arrays.fill (nodeBoardings, -AuxTrNet.INFINITY);

		Arrays.fill (nodeAccWalkTime, 0.0);
		Arrays.fill (nodeFirstWaitTime, 0.0);

		nodeAccWalkTime[dest] = 0.0;
		nodeEgrWalkTime[dest] = 0.0;
		nodeTotWalkTime[dest] = 0.0;
		nodeFirstWaitTime[dest] = 0.0;
		nodeTotalWaitTime[dest] = 0.0;
		nodeInVehTime[dest] = 0.0;
		nodeDriveAccTime[dest] = 0.0;
		nodeCost[dest] = 0.0;
		nodeBoardings[dest] = 0.0;
	}


	/**
	 * 
	 * This method builds the optimal strategy sub-network for the destination taz passed in.
	 * The sub-network is represented by the boolean link field inStrategy[] where true indicates
	 * the link is part of the strategy and false indicates it is not.
	 */
	public int buildStrategy (int dest) {
		// dest is an internal node number from highway network (g).

		int k, m;
		int count = 0;
		boolean debug = classDebug;
//		boolean debug = true;

		this.dest = dest;
		initData();

		
		nodeLabel[dest] = 0;
		nodeFreq[dest] = 0;
		updateEnteringLabels (dest);

		
		// set the access time array based on access mode
		accessTime = new double[ag.walkTime.length];
		if (ag.getAccessMode().equalsIgnoreCase("walk")) {
			accessTime = ag.walkTime; 
		}
		else {
			accessTime = ag.driveAccTime; 
		}

		
		


		if (debug)
		    logger.info ("building optimal strategy to " + dest + "(" + indexNode[dest] + ")");
		
		while ((k = candidateHeap.remove()) != -1) {

			count ++;

			// get the highway network link index for the given transit network link index
			m = ag.hwyLink[k];
			
			int dummy = 0;
			if ( indexNode[gia[m]] == 24923 ) {
				dummy = 1;
//				debug = true;
			}
			
			if (! tested[k]) {
				tested[k] = true;
				if (ag.ia[k] != dest) {

					// log some information about the starting condition of the candidate link being examined
					if (debug) {
						logger.info ("");
						
						logger.info ("k=" + k + ", ag.ia[k]=" + ag.ia[k] + "(g.an=" + indexNode[gia[m]] + "), ag.ib[k]=" + ag.ib[k] + "(g.bn=" + indexNode[gib[m]] + "), linkType=" + ag.linkType[k]);
						logger.info ("nodeLabel[ag.ia=" + ag.ia[k] + "]=" + nodeLabel[ag.ia[k]]);
						logger.info ("nodeLabel[ag.ib=" + ag.ib[k] + "]=" + nodeLabel[ag.ib[k]]);
						logger.info ("nodeFreq[ag.ia=" + ag.ia[k] + "]=" + nodeFreq[ag.ia[k]]);
						logger.info ("nodeFreq[ag.ib=" + ag.ib[k] + "]=" + nodeFreq[ag.ib[k]]);
						logger.info ("ag.freq[k=" + k + "]=" + ag.freq[k]);
						logger.info ("ag.getUtility(k=" + k + ")=" + ag.getUtility(k, 0));
					}

					
					// if the anode's label is at least as big as bnode's label + lik's utility, the link is a candidate to be added to strategy; otherwise get next link. 
					if (nodeLabel[ag.ia[k]] >= (nodeLabel[ag.ib[k]] + ag.getUtility(k))) {

						if ((nodeLabel[ag.ia[k]] == AuxTrNet.INFINITY) && (nodeFreq[ag.ia[k]] == 0.0)) { // first time anode is encountered
							if (debug) logger.info ("first time anode encountered");
							if (ag.freq[k] == AuxTrNet.INFINITY) { // non-boarding link
								if (debug) logger.info ("link freq is Infinity, boarding not allowed for this link");
								nodeLabel[ag.ia[k]] = nodeLabel[ag.ib[k]] + ag.getUtility(k);
							}
							else {  // first transit boarding link considered from the current node
								if (debug) logger.info ("link freq is not Infinity, boarding is allowed for link, examined for first route using link");
								nodeLabel[ag.ia[k]] = (1.0 + ag.freq[k]*(nodeLabel[ag.ib[k]] + ag.getUtility(k)))/(nodeFreq[ag.ia[k]] + ag.freq[k]);
							}
							nodeFreq[ag.ia[k]] = ag.freq[k];
							inStrategy[k] = true;
							strategyOrderForLink[k] = inStrategyCount;
							orderInStrategy[inStrategyCount++] = k;
							if ( updateEnteringLabels(ag.ia[k]) < 0 )
							  ;
							  //return -1;
						}
						else if (nodeFreq[ag.ia[k]] != AuxTrNet.INFINITY) { // second or subsequent time anode is encountered (must be boarding)
							if (debug) logger.info ("second+ time anode encountered (must be boarding link)");
							nodeLabel[ag.ia[k]] = (nodeFreq[ag.ia[k]]*nodeLabel[ag.ia[k]] + ag.freq[k]*(nodeLabel[ag.ib[k]] + ag.getUtility(k)))/(nodeFreq[ag.ia[k]] + ag.freq[k]);
							nodeFreq[ag.ia[k]] += ag.freq[k];
							inStrategy[k] = true;
							strategyOrderForLink[k] = inStrategyCount;
							orderInStrategy[inStrategyCount++] = k;
							if ((updateEnteringLabels (ag.ia[k])) < 0)
							  ;
							  //return -2;
						}
						else {
							if (debug) logger.info ("link not included in strategy");
							inStrategy[k] = false;
						}


						// log some information about the ending condition of the candidate link being examined
						if (debug) {
							logger.info ("");
							logger.info ("nodeLabel[ag.ia=" + ag.ia[k] + "]=" + nodeLabel[ag.ia[k]]);
							logger.info ("nodeLabel[ag.ib=" + ag.ib[k] + "]=" + nodeLabel[ag.ib[k]]);
							logger.info ("nodeFreq[ag.ia=" + ag.ia[k] + "]=" + nodeFreq[ag.ia[k]]);
							logger.info ("nodeFreq[ag.ib=" + ag.ib[k] + "]=" + nodeFreq[ag.ib[k]]);
							logger.info ("ag.freq[k=" + k + "]=" + ag.freq[k]);
							logger.info ("inStrategy[k=" + k + "]=" + inStrategy[k]);
						}

					}

				}
				
			}
			
			if (debug) candidateHeap.dataPrint();
			
		} // end of while heap not empty

		return 0;
	}



	private boolean nodeInStrategy (int node) {
		// node is an internally numbered node.

		int i=0;


		// all from destination attributes must be connected or all must be unconnected.  Any combination is an error.
		if (nodeEgrWalkTime[node] > -AuxTrNet.INFINITY) i++;
		if (nodeTotWalkTime[node] > -AuxTrNet.INFINITY) i++;
		if (nodeTotalWaitTime[node] > -AuxTrNet.INFINITY) i++;
		if (nodeDriveAccTime[node] > -AuxTrNet.INFINITY) i++;
		if (nodeInVehTime[node] > -AuxTrNet.INFINITY) i++;
		if (nodeCost[node] > -AuxTrNet.INFINITY) i++;
		if (nodeBoardings[node] > -AuxTrNet.INFINITY) i++;

		if (i == 0) {
			return false;
		}
		else if (i == 7) {
			return true;
		}
		else {
			logger.info ("Invalid skims for node " + node + ".  Exiting.");
			logger.info ("nodeAccWalkTime[" + node + "]=    " + myFormat.right (nodeAccWalkTime[node], 10, 5));
			logger.info ("nodeEgrWalkTime[" + node + "]=    " + myFormat.right (nodeEgrWalkTime[node], 10, 5));
			logger.info ("nodeTotWalkTime[" + node + "]=    " + myFormat.right (nodeTotWalkTime[node], 10, 5));
			logger.info ("nodeFirstWaitTime[" + node + "]=  " + myFormat.right (nodeFirstWaitTime[node], 10, 5));
			logger.info ("nodeTotalWaitTime[" + node + "]=  " + myFormat.right (nodeTotalWaitTime[node], 10, 5));
			logger.info ("nodeDriveAccTime[" + node + "]=   " + myFormat.right (nodeDriveAccTime[node], 10, 5));
			logger.info ("nodeInVehTime[" + node + "]=      " + myFormat.right (nodeInVehTime[node], 10, 5));
			logger.info ("nodeCost[" + node + "]=           " + myFormat.right (nodeCost[node], 10, 5));
			logger.info ("nodeBoardings[" + node + "]=      " + myFormat.right (nodeBoardings[node], 10, 5));
			System.exit (-1);
			return (false);
		}
	}



	private int updateEnteringLabels (int currentNode) {
		// calculate linkLabels[] for use in ordering the contents of the heap.
		// linkLabel[k] is the cumulative utility from ia[k] to dest.

		int i, j, k, m;
		int start, end;
		boolean debug = classDebug;
//		boolean debug = true;

		if (debug) {
			logger.info ("");
			logger.info ("updateEnteringLabels(): currentNode = " + currentNode);
	  	}

		start = ag.ipb[currentNode];
		if (start == -1) {
			return -1;
		}


		
		if (debug)
			  logger.info ("start=" + start + ", indexb[start]=" + ag.indexb[start] + ", ia=" + ag.ia[ag.indexb[start]] + ", ib=" + ag.ib[ag.indexb[start]] + ", an=" + (ag.ia[ag.indexb[start]] < indexNode.length ? indexNode[ag.ia[ag.indexb[start]]] : 0) + ", bn=" + (ag.ib[ag.indexb[start]] < indexNode.length ? indexNode[ag.ib[ag.indexb[start]]] : 0));
		j = currentNode + 1;
		while (ag.ipb[j] == -1)
			j++;
		end = ag.ipb[j];
		if (debug) {
			logger.info ("end=" + end + ", j=" + j);
			logger.info ("end=" + end + ", indexb[end]=" + ag.indexb[end] + ", ia=" + ag.ia[ag.indexb[end]] + ", ib=" + ag.ib[ag.indexb[end]]);
		  	logger.info ("");
		}
		for (i=start; i < end; i++) {
			k = ag.indexb[i];
			m = ag.hwyLink[k];
			
			linkLabel[k] = nodeLabel[ag.ib[k]] + ag.getUtility(k);
			if (debug)
				logger.info ("adding   " + i + ", indexb[i] or k=" + k + ", linkType=" + ag.linkType[k] + ", ia=" + ag.ia[k] + "(" + indexNode[gia[m]] + "), ib=" + ag.ib[k] + "(" + indexNode[gib[m]] + "), linkLabel[k]=" + myFormat.right(linkLabel[k], 15, 6));
			candidateHeap.add(k);

		}

		if (debug) candidateHeap.dataPrint();
			
		return 0;
	}



	public void wtSkimsFromDest () {

		int i, k, m;
		int ia, ib;
		double wait;

		boolean[] first = new boolean[ag.getAuxNodeCount()+1];
		Arrays.fill (first, true);

 		boolean debug = classDebug;
//		boolean debug = true;


		for (i=0; i < inStrategyCount; i++) {
			
			// get the transit network link index associated with the ith entry in the optimal strategy
			k = orderInStrategy[i];

			// get the highway network link index for the given transit network link index
			m = ag.hwyLink[k];
			
			ia = ag.ia[k];
			ib = ag.ib[k];

			int dummy = 0;
//			if ( indexNode[gia[m]] == 24923 && ag.linkType[k] == 0 ) {
			if ( ia == 15470 ) {
				dummy = 1;
				debug = true;
			}
			

			if (debug) {
				logger.info ("");
				logger.info ("=====================================================================================================");
				logger.info ("k=" + k + ", i=" + i + ", m=" + m + ", ag.ia[k]=" + ag.ia[k] + ", ag.ib[k]=" + ag.ib[k] + ", ag.an[k]=" + (ag.ia[k] < indexNode.length ? indexNode[ag.ia[k]] : 0) + ", ag.bn[k]=" + (ag.ib[k] < indexNode.length ? indexNode[ag.ib[k]] : 0) + ", g.an[k]=" + indexNode[gia[m]] + ", g.bn[k]=" + indexNode[gib[m]] );
				logger.info ("=====================================================================================================");
				logger.info ("");
				logger.info (myFormat.left(("first[ia]="),25)                     + myFormat.right (first[ia],15));
				logger.info (myFormat.left(("ag.linkType[" + k + "]="),25)        + myFormat.right (ag.linkType[k],15));
				logger.info (myFormat.left(("ag.walkTime[" + k + "]="),25)        + (ag.walkTime[k]        == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (ag.walkTime[k]        == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (ag.walkTime[k],15, 4))));
				logger.info (myFormat.left(("ag.invTime[" + k + "]="),25)         + (ag.invTime[k]         == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (ag.invTime[k]         == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (ag.invTime[k],15, 4))));
				logger.info (myFormat.left(("ag.driveAccTime[" + k + "]="),25)    + (ag.driveAccTime[k]    == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (ag.driveAccTime[k]    == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (ag.driveAccTime[k],15, 4))));
				logger.info (myFormat.left(("ag.cost[" + k + "]="),25)            + (ag.cost[k]            == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (ag.cost[k]            == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (ag.cost[k],15, 4))));
				logger.info (myFormat.left(("ag.freq[" + k + "]="),25)            + (ag.freq[k]            == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (ag.freq[k]            == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (ag.freq[k],15, 4))));
				logger.info (myFormat.left(("nodeFreq[" + ia + "]="),25)          + (nodeFreq[ia]          == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeFreq[ia]          == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeFreq[ia],15, 4))));
				logger.info (myFormat.left(("nodeFreq[" + ib + "]="),25)          + (nodeFreq[ib]          == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeFreq[ib]          == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeFreq[ib],15, 4))));
			}


			if (first[ia]) {
			    if (nodeFreq[ia] == AuxTrNet.INFINITY) {
			        if (ag.freq[k] == AuxTrNet.INFINITY) {
			            nodeEgrWalkTime[ia]   = nodeEgrWalkTime[ib]   + (nodeBoardings[ib] == 0 ? ag.walkTime[k] : 0);
			            nodeTotWalkTime[ia]   = nodeTotWalkTime[ib]   + ag.walkTime[k];
			            nodeTotalWaitTime[ia] = nodeTotalWaitTime[ib] + (ag.linkType[k] == 0 ? 1.0/ag.freq[k] : 0) + ag.layoverTime[k];
			            nodeCost[ia]          = nodeCost[ib]          + ag.cost[k];
			            nodeInVehTime[ia]     = nodeInVehTime[ib]     + ag.invTime[k];
			            nodeAccWalkTime[ia]   = nodeAccWalkTime[ib]   + ag.walkTime[k];
			            nodeBoardings[ia]     = nodeBoardings[ib]     + (ag.linkType[k] == 0 ? 1 : 0);
			            first[ia] = false;
			        }
			        else {
			            nodeEgrWalkTime[ia]   = 0.0;
			            nodeTotWalkTime[ia]   = 0.0;
			            nodeTotalWaitTime[ia] = 0.0;
			            nodeCost[ia]          = 0.0;
			            nodeInVehTime[ia]     = 0.0;
			            nodeDriveAccTime[ia]  = 0.0;
			            nodeBoardings[ia]     = 0.0;
			            first[ia] = false;
			        }
			    }
			    else {
			        nodeEgrWalkTime[ia]   = ag.freq[k]*(nodeEgrWalkTime[ib]   + (nodeBoardings[ib] == 0 ? ag.walkTime[k] : 0)) / nodeFreq[ia];
			        nodeTotWalkTime[ia]   = ag.freq[k]*(nodeTotWalkTime[ib]   + ag.walkTime[k])                                / nodeFreq[ia];
			        nodeTotalWaitTime[ia] = ag.freq[k]*(nodeTotalWaitTime[ib] + (ag.linkType[k] == 0 ? 1.0/ag.freq[k] : 0) + ag.layoverTime[k])    / nodeFreq[ia];
			        nodeCost[ia]          = ag.freq[k]*(nodeCost[ib]          + ag.cost[k])                                    / nodeFreq[ia];
			        nodeInVehTime[ia]     = ag.freq[k]*(nodeInVehTime[ib]     + ag.invTime[k])                                 / nodeFreq[ia];
			        nodeAccWalkTime[ia]   = ag.freq[k]*(nodeAccWalkTime[ib]   + ag.walkTime[k])                                / nodeFreq[ia];
			        nodeBoardings[ia]     = ag.freq[k]*(nodeBoardings[ib]     + (ag.linkType[k] == 0 ? 1 : 0))                 / nodeFreq[ia];
			        first[ia] = false;
			    }
			}
			else {
			    if (nodeFreq[ia] == AuxTrNet.INFINITY) {
			        if (ag.freq[k] == AuxTrNet.INFINITY) {
			            nodeEgrWalkTime[ia]   += nodeEgrWalkTime[ib]   + (nodeBoardings[ib] == 0 ? ag.walkTime[k] : 0);
			            nodeTotWalkTime[ia]   += nodeTotWalkTime[ib]   + ag.walkTime[k];
			            nodeTotalWaitTime[ia] += nodeTotalWaitTime[ib] + (ag.linkType[k] == 0 ? 1.0/ag.freq[k] : 0) + ag.layoverTime[k];
			            nodeCost[ia]          += nodeCost[ib]          + ag.cost[k];
			            nodeInVehTime[ia]     += nodeInVehTime[ib]     + ag.invTime[k];
			            nodeAccWalkTime[ia]   += nodeAccWalkTime[ib]   + ag.walkTime[k];
			            nodeBoardings[ia]     += nodeBoardings[ib]     + (ag.linkType[k] == 0 ? 1 : 0);
			        }
			    }
			    else {
			        nodeEgrWalkTime[ia]   += ag.freq[k]*(nodeEgrWalkTime[ib]   + (nodeBoardings[ib] == 0 ? ag.walkTime[k] : 0)) / nodeFreq[ia];
			        nodeTotWalkTime[ia]   += ag.freq[k]*(nodeTotWalkTime[ib]   + ag.walkTime[k])                                / nodeFreq[ia];
			        nodeTotalWaitTime[ia] += ag.freq[k]*(nodeTotalWaitTime[ib] + (ag.linkType[k] == 0 ? 1.0/ag.freq[k] : 0) + ag.layoverTime[k])    / nodeFreq[ia];
			        nodeCost[ia]          += ag.freq[k]*(nodeCost[ib]          + ag.cost[k])                                    / nodeFreq[ia];
		          	nodeInVehTime[ia]     += ag.freq[k]*(nodeInVehTime[ib]     + ag.invTime[k])                                 / nodeFreq[ia];
		          	nodeAccWalkTime[ia]   += ag.freq[k]*(nodeAccWalkTime[ib]   + ag.walkTime[k])                                / nodeFreq[ia];
		          	nodeBoardings[ia]     += ag.freq[k]*(nodeBoardings[ib]     + (ag.linkType[k] == 0 ? 1 : 0))                 / nodeFreq[ia];
		      	}
		  	}

			
			if (debug) {
				logger.info (myFormat.left(("nodeAccWalkTime[" + ia + "]="),25)   + (nodeAccWalkTime[ia]   == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeAccWalkTime[ia]   == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeAccWalkTime[ia],15, 4))));
				logger.info (myFormat.left(("nodeAccWalkTime[" + ib + "]="),25)   + (nodeAccWalkTime[ib]   == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeAccWalkTime[ib]   == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeAccWalkTime[ib],15, 4))));
				logger.info (myFormat.left(("nodeEgrWalkTime[" + ia + "]="),25)   + (nodeEgrWalkTime[ia]   == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeEgrWalkTime[ia]   == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeEgrWalkTime[ia],15, 4))));
				logger.info (myFormat.left(("nodeEgrWalkTime[" + ib + "]="),25)   + (nodeEgrWalkTime[ib]   == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeEgrWalkTime[ib]   == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeEgrWalkTime[ib],15, 4))));
				logger.info (myFormat.left(("nodeTotWalkTime[" + ia + "]="),25)   + (nodeTotWalkTime[ia]   == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeTotWalkTime[ia]   == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeTotWalkTime[ia],15, 4))));
				logger.info (myFormat.left(("nodeTotWalkTime[" + ib + "]="),25)   + (nodeTotWalkTime[ib]   == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeTotWalkTime[ib]   == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeTotWalkTime[ib],15, 4))));
				logger.info (myFormat.left(("nodeFirstWaitTime[" + ia + "]="),25) + (nodeFirstWaitTime[ia] == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeFirstWaitTime[ia] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeFirstWaitTime[ia],15, 4))));
				logger.info (myFormat.left(("nodeFirstWaitTime[" + ib + "]="),25) + (nodeFirstWaitTime[ib] == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeFirstWaitTime[ib] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeFirstWaitTime[ib],15, 4))));
				logger.info (myFormat.left(("nodeTotalWaitTime[" + ia + "]="),25) + (nodeTotalWaitTime[ia] == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeTotalWaitTime[ia] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeTotalWaitTime[ia],15, 4))));
				logger.info (myFormat.left(("nodeTotalWaitTime[" + ib + "]="),25) + (nodeTotalWaitTime[ib] == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeTotalWaitTime[ib] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeTotalWaitTime[ib],15, 4))));
				logger.info (myFormat.left(("nodeDriveAccTime[" + ia + "]="),25)  + (nodeDriveAccTime[ia]  == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeDriveAccTime[ia]  == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeDriveAccTime[ia],15, 4))));
				logger.info (myFormat.left(("nodeDriveAccTime[" + ib + "]="),25)  + (nodeDriveAccTime[ib]  == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeDriveAccTime[ib]  == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeDriveAccTime[ib],15, 4))));
				logger.info (myFormat.left(("nodeInVehTime[" + ia + "]="),25)     + (nodeInVehTime[ia]     == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeInVehTime[ia]     == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeInVehTime[ia],15, 4))));
				logger.info (myFormat.left(("nodeInVehTime[" + ib + "]="),25)     + (nodeInVehTime[ib]     == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeInVehTime[ib]     == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeInVehTime[ib],15, 4))));
				logger.info (myFormat.left(("nodeCost[" + ia + "]="),25)          + (nodeCost[ia]          == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeCost[ia]          == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeCost[ia],15, 4))));
				logger.info (myFormat.left(("nodeCost[" + ib + "]="),25)          + (nodeCost[ib]          == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeCost[ib]          == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeCost[ib],15, 4))));
				logger.info (myFormat.left(("nodeBoardings[" + ia + "]="),25)     + (nodeBoardings[ia]     == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeBoardings[ia]     == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeBoardings[ia],15, 4))));
				logger.info (myFormat.left(("nodeBoardings[" + ib + "]="),25)     + (nodeBoardings[ib]     == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeBoardings[ib]     == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right (nodeBoardings[ib],15, 4))));
				logger.info ((nodeBoardings[ia] == 0) + ", " + nodeBoardings[ia]);
				logger.info ((nodeBoardings[ib] == 0) + ", " + nodeBoardings[ib]);
			}

		}

	}



	public void getOptimalStrategyWtSkimsFromOrig (int fromNode) {

		int i, j, k, m;
		int start, end, check;
		int fromNodeIndex=0;
		int count;
		
		double waitTime, flow;
		
//		boolean debug = classDebug;
		boolean debug = true;
		
		boolean firstBoard = false;


		if (fromNode == dest) return;


		// find the link index of the optimal strategy link exiting fromNode
		// allocate 1 trip to routes between fromNode and dest to track proportions allocated to multiple paths in strategy
		for (i=inStrategyCount - 1; i >= 0; i--) {
			k = orderInStrategy[i];
			m = ag.hwyLink[k];
			if ( ag.ia[k] == fromNode ) {
				fromNodeIndex = i;
				nodeFlow[ag.ia[k]] = 1.0;
				if (debug) {
				    logger.info ("");
				    logger.info ("fromNode=" + fromNode + "(" + indexNode[fromNode] + "), fromNodeIndex=" + fromNodeIndex + ", i=" + i + ", k=" + k + ", m=" + m + ", ag.ia=" + ag.ia[k] + ", ag.ib=" + ag.ib[k] + ", g.an=" + indexNode[gia[m]] + ", g.bn=" + indexNode[gib[m]]);
				}
				break;
			}
		}
		

		
		// loop through links in optimal strategy starting at fromNode, stopping at dest
		count = 0;
		for (i=fromNodeIndex; i >= 0; i--) {
		    
			k = orderInStrategy[i];
			m = ag.hwyLink[k];

			if (nodeFlow[ag.ia[k]] > 0.0 || ag.ia[k] == fromNode) {
				if (debug) {
					logger.info ("count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", ag.ia=" + ag.ia[k] + ", ag.ib="  + ag.ib[k] + ", g.an=" + indexNode[gia[m]] + ", g.bn=" + indexNode[gib[m]] + ", ag.linkType=" + ag.linkType[k] + ", ag.walkTime=" + ag.walkTime[k] + ", ag.invTime=" + ag.invTime[k] + ", nodeFlow[ag.ia[k]]=" + nodeFlow[ag.ia[k]] + ", ag.freq[k]=" + ag.freq[k] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ag.ia[k]]);
				}

			    flow = (ag.freq[k]/nodeFreq[ag.ia[k]])*nodeFlow[ag.ia[k]];
			    
			    if (ag.ia[orderInStrategy[i]] == ag.ia[orderInStrategy[fromNodeIndex]]) {
			    	ag.flow[k] = 1.0;
			    	nodeFlow[ag.ib[k]] = 1.0;
			    	nodeFlow[ag.ia[k]] = 0.0;
			    }
			    else if (flow > MIN_ALLOCATED_FLOW) {
			    	ag.flow[k] = flow;
			    	nodeFlow[ag.ib[k]] = flow;
			    	nodeFlow[ag.ia[k]] = 0.0;
			    }
				if (debug) {
				    logger.info ("flow=" + flow + ", ag.flow[k]=" + ag.flow[k] + ", nodeFlow[ag.ib[k]]=" + nodeFlow[ag.ib[k]]);
				}
				
				count++;
			}
		}
		

/*		
		
		// find link indices exiting fromNode
		start = ag.ipa[fromNode];
		if (start == -1) {
			if (debug)
				logger.info ("no links exiting fromNode= " + fromNode + "(" + indexNode[fromNode] + ").  fromNode=" + fromNode + " is unconnected.");
			nodeEgrWalkTime[fromNode]   = 0.0;
			nodeTotWalkTime[fromNode]   = 0.0;
			nodeTotalWaitTime[fromNode] = 0.0;
			nodeCost[fromNode]          = 0.0;
			nodeInVehTime[fromNode]     = 0.0;
			nodeBoardings[fromNode]     = 0.0;
			return;
		}
		
		j = fromNode + 1;
		while (ag.ipa[j] == -1)
			j++;
		end = ag.ipa[j];


		// 	accumulate access time on walk links in strategy until either a boarding link or dest is encountered
		while (!firstBoard) {
			check = 0;
			for (i=start; i < end; i++) {

				k = ag.indexa[i];
				m = ag.hwyLink[k];
				
				if (debug)
					logger.info ("fromNode=" + fromNode + ", dest=" + dest + ", i=" + i + ", k=" + k + ", m=" + m + ", ag.ia[k]=" + ag.ia[k] + ", ag.ib[k]=" + ag.ib[k] + ", ag.an[k]=" + (ag.ia[k] < indexNode.length ? indexNode[ag.ia[k]] : 0) + ", ag.bn[k]=" + (ag.ib[k] < indexNode.length ? indexNode[ag.ib[k]] : 0) + ", g.an[k]=" + indexNode[gia[m]] + ", g.an[k]=" + indexNode[gib[m]] + ", ag.linkType[k]=" + ag.linkType[k] + ", inStrategy[k]=" + inStrategy[k]);
				
				if (ag.linkType[k] == 0 || ag.ib[k] == dest) {
					firstBoard = true;
					break;
				}
				
				if (inStrategy[k] && ((nodeAccWalkTime[fromNode] + accessTime[k]) < ag.getMaxWalkAccessTime())) {
					check++;
					nodeAccWalkTime[fromNode] += ag.walkTime[k];
					if (debug)
						logger.info ("ag.walkTime[k]=" + ag.walkTime[k] + ", ag.accessTime[k]=" + accessTime[k] + ", nodeAccWalkTime[fromNode]=" + nodeAccWalkTime[fromNode] + ", check=" + check);
					break;
				}
			}
			if (check == 0) {
				nodeEgrWalkTime[fromNode]   = 0.0;
				nodeTotWalkTime[fromNode]   = 0.0;
				nodeTotalWaitTime[fromNode] = 0.0;
				nodeCost[fromNode]          = 0.0;
				nodeInVehTime[fromNode]     = 0.0;
				nodeBoardings[fromNode]     = 0.0;
				return;
			}

			// 	find link indices exiting bnode
			start = ag.ipa[ag.ib[k]];
			if (start == -1) {
				if (debug) {
					logger.info ("start == -1 for fromNode = " + fromNode + " in skimsFromOrig(fromNode=" + fromNode + "):  links exiting bnodes");
					logger.info ("no links exiting bnode= " + ag.ib[k] + "(" + indexNode[ag.ib[k]] + ").  fromNode is unconnected because bnode is unconnected.");
				}
				nodeEgrWalkTime[fromNode]   = 0.0;
				nodeTotWalkTime[fromNode]   = 0.0;
				nodeTotalWaitTime[fromNode] = 0.0;
				nodeCost[fromNode]          = 0.0;
				nodeInVehTime[fromNode]     = 0.0;
				nodeDriveAccTime[fromNode]  = 0.0;
				nodeBoardings[fromNode]     = 0.0;
				return;
			}
			j = ag.ib[k] + 1;
			while (ag.ipa[j] == -1)
				j++;
			end = ag.ipa[j];
		}

		if (!firstBoard)
			nodeAccWalkTime[fromNode] = 0.0;

		if (nodeAccWalkTime[fromNode] == nodeTotWalkTime[fromNode] && nodeEgrWalkTime[fromNode] > 0.0)
			nodeAccWalkTime[fromNode] = 0.0;



		// find link indices exiting first boarding node from fromNode
		start = ag.ipa[ag.ia[k]];
		if (start == -1) {
			logger.info ("start == -1 for fromNode = " + fromNode + " in skimsFromOrig(fromNode=" + fromNode + "):  links exiting first boarding node");
			System.exit(-1);
		}
		j = ag.ia[k] + 1;
		while (ag.ipa[j] == -1)
			j++;
		end = ag.ipa[j];


		waitTime = 0.0;
		for (i=start; i < end; i++) {
			k = ag.indexa[i];
			if (inStrategy[k] && ag.linkType[k] == 0)
				waitTime += 1.0/nodeFreq[ag.ia[k]];
			if (debug)
				logger.info ("nodeFreq[ag.ia[k]]=" + nodeFreq[ag.ia[k]] + ", waitTime=" + waitTime);
		}
		nodeFirstWaitTime[fromNode] = waitTime;
		if (debug) {
			logger.info ("nodeFirstWaitTime[fromNode]=" + nodeFirstWaitTime[fromNode]);
			logger.info ("");
		}

*/

	}



	public Matrix[] getOptimalStrategySkimMatrices () {

		int j, k, m;
		int accessLinkIndex=-1;
		int start, end, check;
		double waitTime;
		
//		boolean debug = true;
		boolean debug = classDebug;
		
		boolean firstBoard = false;


		Matrix[] skims = new Matrix[NUM_SKIMS];
		for (int i=0; i < skims.length; i++)
		    skims[i] = new Matrix( g.getNumCentroids(), g.getNumCentroids() );
		
		for (int dest=0; dest < g.getNumCentroids(); dest++) {
		    
		    if ( dest % 100 == 0 ) {
		        logger.info ( "generating skims to zone " + dest + " at " + DateFormat.getDateTimeInstance().format(new Date()) );
		    }
		    
		    
			// build an optimal strategy for the specified destination node.
			if ( buildStrategy(dest) >= 0 ) {
			    
				// compute skims for this destination
				initSkims();
				wtSkimsFromDest();

				for (int orig=0; orig < g.getNumCentroids(); orig++) {

				    // if orig is destination, it's intrazonal interchange, so go to next origin zone.
					if ( orig == dest )
					    continue;



					// find start of link indices exiting orig
					start = ag.ipa[orig];
					if (start == -1) {
						if (debug)
							logger.info ("no links exiting orig= " + orig + "(" + indexNode[orig] + ").  orig=" + orig + " is unconnected.");
						nodeEgrWalkTime[orig]   = 0.0;
						nodeTotWalkTime[orig]   = 0.0;
						nodeTotalWaitTime[orig] = 0.0;
						nodeCost[orig]          = 0.0;
						nodeInVehTime[orig]     = 0.0;
						nodeDriveAccTime[orig]  = 0.0;
						nodeBoardings[orig]     = 0.0;
						continue;
					}

					// find end of link indices exiting orig
					j = orig + 1;
					while (ag.ipa[j] == -1)
						j++;
					end = ag.ipa[j];

					
					// 	find the access link from the origin to a boarding link in the strategy
					check = 0;
					for (int i=start; i < end; i++) {
						k = ag.indexa[i];
						m = ag.hwyLink[k];

						if (inStrategy[k] && ((nodeAccWalkTime[orig] + accessTime[k]) < ag.getMaxWalkAccessTime())) {
							check++;
							nodeAccWalkTime[orig] += ag.walkTime[k];
							nodeDriveAccTime[orig] += ag.driveAccTime[k];
							if (debug) {
								logger.info ("orig=" + orig + ", dest=" + dest + ", i=" + i + ", k=" + k + ", ag.ia=" + ag.ia[k] + "(g.an=" + indexNode[gia[m]] + ") , ag.ib=" + ag.ib[k] + "(" + indexNode[gib[m]] + ") , ag.linkType[k]=" + ag.linkType[k] + ", inStrategy[k]=" + inStrategy[k]);
								logger.info ("ag.walkTime[k]=" + ag.walkTime[k] + ", ag.driveAccTime[k]=" + ag.driveAccTime[k] + ", ag.accessTime[k]=" + accessTime[k] + ", nodeAccWalkTime[orig]=" + nodeAccWalkTime[orig] + ", nodeDriveAccTime[orig]=" + nodeDriveAccTime[orig] + ", check=" + check);
							}
							accessLinkIndex = k;
							break;
						}
					}
					if (check == 0) {
//							logger.info ("orig=" + orig + " is not connected to " + dest + "(" + indexNode[dest] + ").");
						nodeEgrWalkTime[orig]   = 0.0;
						nodeTotWalkTime[orig]   = 0.0;
						nodeTotalWaitTime[orig] = 0.0;
						nodeCost[orig]          = 0.0;
						nodeInVehTime[orig]     = 0.0;
						nodeDriveAccTime[orig]  = 0.0;
						nodeBoardings[orig]     = 0.0;
						continue;
//							logger.info ("rerun with debug = false in skimsFromOrig");
//							System.exit(-1);
					}

						
					// 	find start of link indices exiting the bnode of this transit access link
					start = ag.ipa[ag.ib[accessLinkIndex]];
					if (start == -1) {
						if (debug) {
							m = ag.hwyLink[accessLinkIndex];
							logger.info ("start == -1 for orig = " + orig + " in getOptimalStrategySkimMatrices(orig=" + orig + "):  links exiting bnodes");
							logger.info ("accessLinkIndex=" + accessLinkIndex + ", no links exiting bnode= " + ag.ib[accessLinkIndex] + "(g.bn=" + indexNode[gib[m]] + ").  orig is unconnected because bnode is unconnected.");
						}
						nodeEgrWalkTime[orig]   = 0.0;
						nodeTotWalkTime[orig]   = 0.0;
						nodeTotalWaitTime[orig] = 0.0;
						nodeCost[orig]          = 0.0;
						nodeInVehTime[orig]     = 0.0;
						nodeDriveAccTime[orig]  = 0.0;
						nodeBoardings[orig]     = 0.0;
						continue;
					}

					// 	find end of link indices exiting this bnode
					j = ag.ib[accessLinkIndex] + 1;
					while (ag.ipa[j] == -1)
						j++;
					end = ag.ipa[j];

					


					waitTime = 0.0;
					for (int i=start; i < end; i++) {
						k = ag.indexa[i];
						m = ag.hwyLink[k];
						if (inStrategy[k] && ag.linkType[k] == 0) {
							waitTime += 1.0/nodeFreq[ag.ia[k]];
							if (debug) {
								logger.info ("orig=" + orig + ", dest=" + dest + ", i=" + i + ", k=" + k + ", ag.ia=" + ag.ia[k] + "(g.an=" + indexNode[gia[m]] + ") , ag.ib=" + ag.ib[k] + "(" + indexNode[gib[m]] + ") , ag.linkType[k]=" + ag.linkType[k]);
								logger.info ("nodeFreq[ag.ia[k]]=" + nodeFreq[ag.ia[k]] + ", waitTime=" + waitTime);
							}
						}
					}
					nodeFirstWaitTime[orig] = waitTime;
					if (debug) {
						logger.info ("nodeFirstWaitTime[orig]=" + nodeFirstWaitTime[orig]);
						logger.info ("");
					}
					
					
					skims[IVT].setValueAt( orig, dest, (float)nodeInVehTime[orig] );
					skims[FWT].setValueAt( orig, dest, (float)nodeFirstWaitTime[orig] );
					skims[TWT].setValueAt( orig, dest, (float)nodeTotalWaitTime[orig] );
					skims[AUX].setValueAt( orig, dest, ag.getAccessMode().equalsIgnoreCase("walk") ? (float)nodeTotWalkTime[orig] : (float)nodeDriveAccTime[orig] );
					skims[BRD].setValueAt( orig, dest, (float)nodeBoardings[orig] );
					skims[FAR].setValueAt( orig, dest, (float)nodeCost[orig] );
				
				}
				
			}
			
		}

		return skims;
		
	}



	public void loadWalkTransitProportions (int orig) {

		// two possible cases for walk transit transit:
		//		1) orig has no exiting boarding links, only walk links in strategy -- do station choice
		//		2) orig has exiting boarding links that are part of strategy and thus no walk links

		int i, k = 0, origIndex = 0, stopLink;
		double flow;

//		boolean debug = true;
		boolean debug = classDebug;

		logger.info ("");
		logger.info ("loading Walk-Transit, inStrategyCount=" + inStrategyCount);

		for (i=inStrategyCount - 1; i >= 0; i--) {
			k = orderInStrategy[i];
			if (ag.ia[k] == orig) {
				origIndex = i;
				nodeFlow[orig] = 1.0;
				break;
			}
		}
		if (debug) {
		    logger.info ("");
		    logger.info ("orig=" + orig + ", origIndex=" + origIndex + ", nodeFreq[orig]=" + nodeFreq[orig]);
		}

		if (nodeFreq[orig] >= AuxTrNet.INFINITY) {
		    // no boarding at origin,  station (stop) choice required
			stopLink = walkStopChoice(orig);
			ag.flow[stopLink] = 1.0;
//			nodeFlow[ag.ib[stopLink]] = 1.0;
//			nodeFlow[orig] = 0.0;
			nodeFlow[orig] = 1.0;
			if (debug) {
				int m = ag.hwyLink[stopLink];
			    logger.info ("stopLink=" + stopLink + ", ag.ia=" + ag.ia[stopLink] + ", ag.ib=" + ag.ib[stopLink] + ", g.an[k]=" + indexNode[gia[m]] + ", g.bn[k]=" + indexNode[gib[m]] + ", nodeFlow[ag.ib[stopLink]]=" + nodeFlow[ag.ib[stopLink]]);
			}
		}

		for (i=origIndex; i >= 0; i--) {
		    
			k = orderInStrategy[i];
			
			if (nodeFlow[ag.ia[k]] > 0.0 || ag.ia[k] == orig) {
				int m = ag.hwyLink[k];
				if (debug) {
					logger.info ("loading " + "i=" + i + ", k=" + k + ", ag.ia=" + ag.ia[k] + ", ag.ib="  + ag.ib[k] + ", ag.linkType=" + ag.linkType[k] + ", ag.walkTime=" + ag.walkTime[k] + ", ag.invTime=" + ag.invTime[k] + ", g.an[k]=" + indexNode[gia[m]] + ", g.bn[k]=" + indexNode[gib[m]] + ", nodeFlow[ag.ia[k]]=" + nodeFlow[ag.ia[k]] + ", ag.freq[k]=" + ag.freq[k] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ag.ia[k]]);
				}
			    flow = (ag.freq[k]/nodeFreq[ag.ia[k]])*nodeFlow[ag.ia[k]];
			    ag.flow[k] += flow;
				nodeFlow[ag.ib[k]] += flow;
				if (debug) {
				    logger.info ("flow=" + flow + ", ag.flow[k]=" + ag.flow[k] + ", nodeFlow[ag.ib[k]]=" + nodeFlow[ag.ib[k]]);
				}
				k = 0;
			}
		}
		
	}


	public void loadWalkTransit (int orig) {

		// two possible cases for walk transit transit:
		//		1) orig has no exiting boarding links, only walk links in strategy -- do station choice
		//		2) orig has exiting boarding links that are part of strategy and thus no walk links

		int i, j, k = 0, stopIndex=0;
		int start, end, currentNode, count=0;
		double rnum, compositeFreq;
		int[] links = new int[MAX_BOARDING_LINKS];
		double[] props = new double[MAX_BOARDING_LINKS];
		double[] cdf = new double[MAX_BOARDING_LINKS];
		boolean[] notVisited = new boolean[ag.getAuxNodeCount()+1];

		boolean debug = classDebug;

		Arrays.fill(notVisited, true);

		logger.info ("");
		logger.info ("loading Walk-Transit, inStrategyCount=" + inStrategyCount);


		if (nodeFreq[orig] >= AuxTrNet.INFINITY) {
		// no boarding at origin,  station (stop) choice required
			stopIndex = walkStopChoice(orig);
		  if (debug) {
			logger.info ("");
			logger.info ("orig=" + orig + ", stopIndex=" + stopIndex + ", ag.ia[stopIndex]=" + ag.ia[stopIndex] + ", ag.ib[stopIndex]=" + ag.ib[stopIndex]);
	  }
		}
		else {
		for (i=inStrategyCount - 1; i >= 0; i--) {
			k = orderInStrategy[i];
			if (ag.ia[k] == orig) {
				  stopIndex = k;
				break;
			}
		  }
		if (debug) {
			logger.info ("");
			logger.info ("orig=" + orig + ", stopIndex=" + stopIndex + ", ag.ia[stopIndex]=" + ag.ia[stopIndex] + ", ag.ib[stopIndex]=" + ag.ib[stopIndex]);
		}
		}


// load trip onto transit routes by monte carlo selection of possible routes through strategy with proportions according to boarding frequencies.
	currentNode = ag.ia[stopIndex];
	while (currentNode != dest) {

	  notVisited[currentNode] = false;
		start = ag.ipa[currentNode];
		j = currentNode + 1;
		  while (ag.ipa[j] == -1)
			  j++;
		  end = ag.ipa[j];

	  //composite node frequency
	  compositeFreq = 0.0;
		  for (i=start; i < end; i++) {
			k = ag.indexa[i];
			if (debug) {
			  logger.info ("i=" + i + ", k=" + k + ", ia=" + ag.ia[k] + "(" + indexNode[ag.ia[k]] + "), ib=" + ag.ib[k] + "(" + indexNode[ag.ib[k]] + "), hwy_an=" + indexNode[gia[ag.hwyLink[k]]] + ", hwy_bn=" + indexNode[gib[ag.hwyLink[k]]] + ", inStrategy=" + inStrategy[k] + ", linkType=" + ag.linkType[k] + ", notVisited=" + notVisited[ag.ib[k]] + ", freq=" + ag.freq[k] + ", compositeFreq=" + compositeFreq);
		}
		  if (ag.linkType[k] == 0 && inStrategy[k] && notVisited[ag.ib[k]])
			compositeFreq += ag.freq[k];
		}

		if (debug) {
		  logger.info ("currentNode=" + currentNode + "(" + indexNode[currentNode] + "), start=" + start + ", end=" + end + ", compositeFreq=" + compositeFreq);
		}

	  if (compositeFreq > 0.0) {
		  j = 0;
		  cdf[0] = 0.0;
			for (i=start; i < end; i++) {
			k = ag.indexa[i];
			  if (ag.ib[k] == dest) {
				cdf[0] = 0.0;
			  break;
			}

		  if (ag.linkType[k] == 0 && inStrategy[k] && notVisited[ag.ib[k]]) {
			  props[j] = ag.freq[k]/compositeFreq;
			  links[j] = k;
			  if (j == 0)
				cdf[j] = props[j];
			else
			  cdf[j] = cdf[j-1] + props[j];
				if (debug) {
				  logger.info ("i=" + i + ", j=" + j + ", ia=" + ag.ia[k] + "(" + indexNode[ag.ia[k]] + "), ib=" + ag.ib[k] + "(" + indexNode[ag.ib[k]] + "), hwy_an=" + indexNode[gia[ag.hwyLink[k]]] + ", hwy_bn=" + indexNode[gib[ag.hwyLink[k]]] + ", inStrategy=" + inStrategy[k] + ", linkType=" + ag.linkType[k] + ", freq=" + ag.freq[k] + ", nodeFreq=" + nodeFreq[ag.ia[k]] + ", props=" + props[j] + ", cdf=" + cdf[j] + ", links=" + links[j] + ", ivt=" + nodeInVehTime[ag.ib[k]]);
			}
			  j++;
		  }
		  else {
			  if (debug) {
				  logger.info ("i=" + i + ", j=" + j + ", ia=" + ag.ia[k] + "(" + indexNode[ag.ia[k]] + "), ib=" + ag.ib[k] + "(" + indexNode[ag.ib[k]] + "), hwy_an=" + indexNode[gia[ag.hwyLink[k]]] + ", hwy_bn=" + indexNode[gib[ag.hwyLink[k]]] + ", inStrategy=" + inStrategy[k] + ", linkType=" + ag.linkType[k] + ", freq=" + ag.freq[k] + ", nodeFreq=" + nodeFreq[ag.ia[k]] + ", props=" + props[j] + ", cdf=" + cdf[j] + ", links=" + links[j] + ", ivt=" + nodeInVehTime[ag.ib[k]]);
				}
			}
		}

		if (cdf[0] > 0.0) {
			// Monte Carlo selection of boarding link to load
		  rnum = Math.random();			//  0 <= rnum < 1.0
		  for (i=0; i < j; i++) {
				if (debug) {
				  logger.info ("i=" + i + ", j=" + j + ", rnum=" + rnum + ", cdf[i]=" + cdf[i] + ", links[i]=" + links[i]);
				}
				if (rnum <= cdf[i]) {
					k =  links[i];
					  break;
				  }
				}
		}

			  ag.flow[k] += 1;

			  currentNode = ag.ib[k];
			}
			else {
			for (i=start; i < end; i++) {
			k = ag.indexa[i];

			if (debug) {
			  logger.info ("i=" + i + ", inStrategy[k]=" + inStrategy[k] + ", ia=" + ag.ia[k] + "(" + indexNode[ag.ia[k]] + "), ib=" + ag.ib[k] + "(" + indexNode[ag.ib[k]] + "), hwy_an=" + indexNode[gia[ag.hwyLink[k]]] + ", hwy_bn=" + indexNode[gib[ag.hwyLink[k]]] + ", inStrategy=" + inStrategy[k] + ", linkType=" + ag.linkType[k] + ", freq=" + ag.freq[k] + ", nodeFreq=" + nodeFreq[ag.ia[k]] + ", ivt=" + nodeInVehTime[ag.ib[k]]);
			}

			  if (ag.ib[k] == dest) {
			  break;
			}

		  if (inStrategy[k] && notVisited[ag.ib[k]]) {
			break;
			}
		}

		  ag.flow[k] += 1;
		currentNode = ag.ib[k];

	  }

	  if (debug) {
		if (count++ > 500)
		  System.exit(-1);
	  }
	}
	}


	int walkStopChoice (int orig) {

		int j, jj, k, kk, count;
		int start, end;
		int start2, end2;
		boolean stopAvail;

		boolean debug = classDebug;


		start = ag.ipa[orig];
		if (start == -1) {
			logger.info ("start == -1 for orig = " + orig + " in walkStopChoice(orig=" + orig + ")");
			System.exit(-1);
		}
		j = orig + 1;
		while (ag.ipa[j] == -1)
			j++;
		end = ag.ipa[j];

		boolean[] avail = new boolean[end - start];
		double[] stopUtil = new double[end - start];

		count = 0;
		for (j=start; j < end; j++) {
		  k = ag.indexa[j];
			avail[count] = false;
			stopUtil[count] = 0.0;
		  if (debug)
			logger.info ("orig=" + orig + ", j=" + j + ", k=" + k + ", ag.ia[k]=" + ag.ia[k] + ", ag.ib[k]=" + ag.ib[k] + ", accessTime[k]=" + accessTime[k] +", nodeInStrategy(ag.ib[k])=" + nodeInStrategy(ag.ib[k]));
			if (accessTime[k] > 0.0) {
				if (nodeInStrategy(ag.ib[k])) {
					// check for special case that opposite direction link returns to orig
					start2 = ag.ipa[ag.ib[k]];
			if (start2 == -1) {
			  logger.info ("start2 == -1 for ag.ib[k] = " + ag.ib[k] + " in walkStopChoice(orig=" + orig + ")");
				System.exit(-1);
		  }
					jj = ag.ib[k] + 1;
					while (ag.ipa[jj] == -1)
						jj++;
					end2 = ag.ipa[jj];
					if (debug)
			logger.info ("start2=" + start2 + ", end2=" + end2);
		  stopAvail = true;
					for (jj=start2; jj < end2; jj++) {
			  kk = ag.indexa[jj];
					  if (debug)
						logger.info ("jj=" + jj + ", kk=" + kk + ", count=" + count + ", inStrategy[kk]=" + inStrategy[kk] + ", ag.ia[kk]=" + ag.ia[kk] + ", ag.ib[kk]=" + ag.ib[kk]);
						if (inStrategy[kk] && ag.ib[kk] == orig) {
							stopAvail = false;
							break;
						}
					}
					if (stopAvail) {
					  // get first wait and walk access skims for this ag.ib[k], then add accessTime[k] to have walk access time for stop choice
					  // skimsFromOrig(ag.ib[k]);
						avail[count] = true;
						stopUtil[count] = getStopUtility(k);
					}
				  if (debug)
					logger.info ("count=" + count + ", avail[count]=" + avail[count] + ", stopUtil[count]=" + stopUtil[count]);
				}
			}
	  count++;
		}

	if (count == 0) {
	  logger.info ("count == 0 in walkStopChoice(orig=" + orig + ")");
	  logger.info ("start=" + start + ", end=" + end);
	  System.exit(-1);
	}
		return (ag.indexa[start + mnlMonteCarlo(stopUtil, avail, count)]);
	}



	public void loadDriveTransit (int orig) {

		int i, j, k = 0, stopIndex = 0, stopNode;
		int start, end, currentNode, count=0;
		double flow, rnum, compositeFreq;
		int[] links = new int[MAX_BOARDING_LINKS];
		double[] props = new double[MAX_BOARDING_LINKS];
		double[] cdf = new double[MAX_BOARDING_LINKS];
		boolean[] notVisited = new boolean[ag.getAuxNodeCount()+1];

		boolean debug = classDebug;

		Arrays.fill(notVisited, true);

		logger.info ("");
		logger.info ("loading Drive-Transit, inStrategyCount=" + inStrategyCount + ", orig=" + orig + "(" + indexNode[orig] + ")");

		stopNode = driveStopChoice(DRIVE_STOP_PCTS/100.0, orig);

		for (i=inStrategyCount - 1; i >= 0; i--) {
			k = orderInStrategy[i];
			if (ag.ia[k] == stopNode) {
				stopIndex = i;
//				nodeFlow[ag.ia[k]] = 1.0;
				break;
			}
		}


/*
//		load trip onto transit routes by apportioning 1 unit of flow to links according to boarding frequencies.
		for (i=stopIndex; i >= 0; i--) {
			k = orderInStrategy[i];
			if (debug)
				logger.info ("loading " + "i=" + i + ", k=" + k + " (" + ag.ia[k] + "," + ag.ib[k] + ")  nodeFlow[ag.ia[k]]=" + nodeFlow[ag.ia[k]] + ", ag.freq[k]=" + ag.freq[k] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ag.ia[k]]);
			if (nodeFlow[ag.ia[k]] > 0.0) {
				flow = (ag.freq[k]/nodeFreq[ag.ia[k]])*nodeFlow[ag.ia[k]];
				ag.flow[k] += flow;
				nodeFlow[ag.ib[k]] += flow;
				if (debug)
					logger.info (", flow=" + flow + ", ag.flow[k]=" + ag.flow[k] + ", nodeFlow[ag.ib[k]]=" + nodeFlow[ag.ib[k]]);
			}
			else {
				if (debug)
					logger.info ("");
			}
		}
*/


// load trip onto transit routes by monte carlo selection of possible routes through strategy with proportions according to boarding frequencies.

		k = orderInStrategy[stopIndex];
		currentNode = ag.ia[k];
		while (currentNode != dest) {

	  		notVisited[currentNode] = false;
			start = ag.ipa[currentNode];
			j = currentNode + 1;
		  	while (ag.ipa[j] == -1)
				  j++;
		  	end = ag.ipa[j];

		  	//composite node frequency
		  	compositeFreq = 0.0;
		  	for (i=start; i < end; i++) {
				k = ag.indexa[i];
		  		if (ag.linkType[k] == 0 && inStrategy[k] && notVisited[ag.ib[k]])
					compositeFreq += ag.freq[k];
			}

			if (debug) {
		  		logger.info ("currentNode=" + currentNode + "(" + indexNode[currentNode] + "), start=" + start + ", end=" + end + ", compositeFreq=" + compositeFreq);
			}

	  		if (compositeFreq > 0.0) {
		  		j = 0;
		  		cdf[0] = 0.0;
				for (i=start; i < end; i++) {
					k = ag.indexa[i];
			  		if (ag.ib[k] == dest) {
						cdf[0] = 0.0;
			  			break;
					}

		  			if (ag.linkType[k] == 0 && inStrategy[k] && notVisited[ag.ib[k]]) {
			  			props[j] = ag.freq[k]/compositeFreq;
			  			links[j] = k;
			  			if (j == 0)
							cdf[j] = props[j];
						else
			  				cdf[j] = cdf[j-1] + props[j];
						if (debug) {
				  			logger.info ("i=" + i + ", j=" + j + ", ia=" + ag.ia[k] + "(" + (ag.ia[k] < indexNode.length ? indexNode[ag.ia[k]] : 0 ) + "), ib=" + ag.ib[k] + "(" + (ag.ib[k] < indexNode.length ? indexNode[ag.ib[k]] : 0) + "), hwy_an=" + indexNode[gia[ag.hwyLink[k]]] + ", hwy_bn=" + indexNode[gib[ag.hwyLink[k]]] + ", inStrategy=" + inStrategy[k] + ", linkType=" + ag.linkType[k] + ", freq=" + ag.freq[k] + ", nodeFreq=" + nodeFreq[ag.ia[k]] + ", props=" + props[j] + ", cdf=" + cdf[j] + ", links=" + links[j] + ", ivt=" + nodeInVehTime[ag.ib[k]]);
						}
			  			j++;
		  			}
		  			else {
			  			if (debug) {
				  			logger.info ("i=" + i + ", j=" + j + ", ia=" + ag.ia[k] + "(" + (ag.ia[k] < indexNode.length ? indexNode[ag.ia[k]] : 0 ) + "), ib=" + ag.ib[k] + "(" + (ag.ib[k] < indexNode.length ? indexNode[ag.ib[k]] : 0) + "), hwy_an=" + indexNode[gia[ag.hwyLink[k]]] + ", hwy_bn=" + indexNode[gib[ag.hwyLink[k]]] + ", inStrategy=" + inStrategy[k] + ", linkType=" + ag.linkType[k] + ", freq=" + ag.freq[k] + ", nodeFreq=" + nodeFreq[ag.ia[k]] + ", props=" + props[j] + ", cdf=" + cdf[j] + ", links=" + links[j] + ", ivt=" + nodeInVehTime[ag.ib[k]]);
						}
					}
				}

				if (cdf[0] > 0.0) {
					// 	Monte Carlo selection of boarding link to load
					rnum = Math.random();			//  0 <= rnum < 1.0
		  			for (i=0; i < j; i++) {
						if (debug) {
				  			logger.info ("i=" + i + ", j=" + j + ", rnum=" + rnum + ", cdf[i]=" + cdf[i] + ", links[i]=" + links[i]);
						}
						if (rnum <= cdf[i]) {
							k =  links[i];
					  		break;
				  		}
					}
				}

			  	ag.flow[k] += 1;

			  	currentNode = ag.ib[k];
			}
			else {
				for (i=start; i < end; i++) {
					k = ag.indexa[i];

					if (debug) {
			  			logger.info ("i=" + i + ", inStrategy[k]=" + inStrategy[k] + ", ia=" + ag.ia[k] + "(" + (ag.ia[k] < indexNode.length ? indexNode[ag.ia[k]] : 0) + "), ib=" + ag.ib[k] + "(" + (ag.ib[k] < indexNode.length ? indexNode[ag.ib[k]] : 0) + "), hwy_an=" + indexNode[gia[ag.hwyLink[k]]] + ", hwy_bn=" + indexNode[gib[ag.hwyLink[k]]] + ", inStrategy=" + inStrategy[k] + ", linkType=" + ag.linkType[k] + ", freq=" + ag.freq[k] + ", nodeFreq=" + nodeFreq[ag.ia[k]] + ", ivt=" + nodeInVehTime[ag.ib[k]]);
					}

			  		if (ag.ib[k] == dest) {
			  			break;
					}

		  			if (inStrategy[k] && notVisited[ag.ib[k]]) {
						break;
					}
				}

		  		ag.flow[k] += 1;
				currentNode = ag.ib[k];

	  		}

	  		if (debug) {
				if (count++ > 500)
		  			System.exit(-1);
	  		}
		}
	}


	public int driveStopChoice (double driveStopPcts, int fromNode) {

		int i, k;
		int stopCount = 0, stopNum;
		int[] stopNodeOrder = new int[ag.getAuxNodeCount()+1];
		int[] sortData = new int[ag.getAuxNodeCount()+1];
		int[] stopNodes = new int[ag.getAuxNodeCount()+1];
		int[] nodeCount = new int[ag.getAuxNodeCount()+1];
		int[] mnlNodes = new int[ag.getAuxNodeCount()+1];
		boolean[] stopAvail = new boolean[ag.getAuxNodeCount()+1];
		double[] stopTimes = new double[ag.getAuxNodeCount()+1];
		double[] cumStopTimes = new double[ag.getAuxNodeCount()+1];
		double[] fullCumPcts = new double[ag.getAuxNodeCount()+1];
		double[] mnlTimes = new double[ag.getAuxNodeCount()+1];

		boolean debug = classDebug;



		// count the number of stop nodes at boarding links in strategy and save.
		// These are the stop choice alternatives.
		for (i=0; i < inStrategyCount; i++) {
			k = orderInStrategy[i];
			if (nodeCount[ag.ia[k]] == 0 && ag.linkType[k] == 0 && ag.ia[k] != fromNode && nodeInVehTime[ag.ia[k]] > 0.0)
				stopNodes[stopCount++] = ag.ia[k];
			nodeCount[ag.ia[k]] ++;
		}
		if (debug) {
		logger.info ("");
		logger.info ("driveStopChoice(" + myFormat.left(driveStopPcts, 5, 2) + "," + fromNode + "), stopCount=" + stopCount);
	  }


	  // calculate shortest path travel times over auto network from fromNode to each stop --  Drive access time.
	  // sort nodes by utility.
		ShortestPath sp = new ShortestPath(g);

		double totalTime = 0.0;
		for (i=0; i < stopCount; i++) {
//			sp.buildPath(fromNode, stopNodes[i]);
//			stopTimes[i] = sp.getPathCost(fromNode, stopNodes[i]);
//			nodeDriveAccTime[fromNode] = stopTimes[i];
//		stopNodeOrder[i] = i;
		// get an in-strategy link who's b-node is stopnode[i].  This link index will be used to get the stop node's utility.
//		  k = ag.indexb[ag.ipb[stopNodes[i]]];
		stopTimes[i] = airlineDistance(fromNode, stopNodes[i]);
	  totalTime += stopTimes[i];
		stopNodeOrder[i] = i;
		sortData[i] = (int)(stopTimes[i]*1000000);
	  }


	if (debug) {
	  logger.info ("");
	  logger.info ("");
	  logger.info ("fromNode=" + fromNode);
	  for (i=0; i < stopCount; i++)
		logger.info (myFormat.right(i, 5) + myFormat.right(stopNodes[i], 8) + myFormat.right(indexNode[stopNodes[i]], 8) + myFormat.right(stopTimes[i], 12, 3));
	}


	stopNodeOrder = IndexSort.indexSort ( sortData );


	// calculate shortest path time to closest in-strategy drive stop node to get drive access attributes
	if (driveStopPcts == 0.0)
	  sp.buildPath(fromNode, stopNodes[stopNodeOrder[0]]);


	  // consider only stops in the DRIVE_STOP_PCTSth percentile of highway shortest path travel times.
	  mnlTimes[0] = stopTimes[stopNodeOrder[0]];
	  mnlNodes[0] = stopNodes[stopNodeOrder[0]];
	  cumStopTimes[0] = stopTimes[stopNodeOrder[0]];
	  fullCumPcts[0] = cumStopTimes[0]/totalTime;
	  stopAvail[0] = true;
		for (i=1; i < stopCount; i++) {
		  if (fullCumPcts[i-1] > driveStopPcts) {
			stopCount = i;
			break;
		  }
		  else {
			  mnlTimes[i] = stopTimes[stopNodeOrder[i]];
			  mnlNodes[i] = stopNodes[stopNodeOrder[i]];
			stopAvail[i] = true;
				cumStopTimes[i] = stopTimes[stopNodeOrder[i]] + cumStopTimes[i-1];
			fullCumPcts[i] = cumStopTimes[i]/totalTime;
		  }
		}

	if (debug) {
	  logger.info ("");
	  logger.info ("");
	  for (i=0; i < stopCount; i++)
		logger.info (myFormat.right(i, 5) + myFormat.right(mnlNodes[i], 8) + "(" + indexNode[mnlNodes[i]] + ")" + myFormat.right(mnlTimes[i], 12, 3));
	}

		stopNum =  mnlMonteCarlo (mnlTimes, stopAvail, stopCount);

		if (debug) {
		logger.info ("");
		  logger.info ("stopNum=" + stopNum + ", mnlNodes[stopNum]=" + mnlNodes[stopNum] + "(" + indexNode[mnlNodes[stopNum]] + ")");
	  }

		return (mnlNodes[stopNum]);
	}



	public void driveTransitSkimsFromOrig (int fromNode) {

		int i, j, k = 0, stop;
		int start, end, check;
		double waitTime;
		boolean debug = classDebug;
		boolean firstBoard = false;


		// get the drive stop which is closest by highway shortest path to fromNode
		stop = driveStopChoice(0.0, fromNode);
		if (debug)
		    logger.info ("drive stop choice for fromNode=" + fromNode + "(" + indexNode[fromNode] + ") is " + stop + "(" + indexNode[stop] + ")");


		// find link indices exiting stop node
		for (i=inStrategyCount - 1; i >= 0; i--) {
			k = orderInStrategy[i];
			if (ag.ia[k] == stop)
				break;
		}

		start = ag.ipa[ag.ia[k]];
		if (start == -1 && debug) {
		    if (debug)
		        logger.info ("no links exiting drive stop node= " + stop + "(" + indexNode[stop] + ").  drive stop node=" + stop + " is unconnected.");
		    nodeEgrWalkTime[fromNode]   = 0.0;
		    nodeTotWalkTime[fromNode]   = 0.0;
		    nodeTotalWaitTime[fromNode] = 0.0;
		    nodeCost[fromNode]          = 0.0;
		    nodeInVehTime[fromNode]     = 0.0;
		    nodeDriveAccTime[fromNode]  = 0.0;
		    nodeBoardings[fromNode]     = 0.0;
		    return;
		}
		j = ag.ia[k] + 1;
		while (ag.ipa[j] == -1)
		    j++;
		end = ag.ipa[j];


		waitTime = 0.0;
		for (i=start; i < end; i++) {
		    k = ag.indexa[i];
		    if (debug)
		        logger.info ("ia=" + ag.ia[k] + ", ib=" + ag.ib[k]);
		    if (inStrategy[k] && ag.linkType[k] == 0)
		        waitTime += 1.0/nodeFreq[ag.ia[k]];
		}
		nodeFirstWaitTime[fromNode] = waitTime;

		if (debug)
		    logger.info ("done with driveTransitSkimsFromOrig()");
		
	}



	private double getStopUtility (int k) {
		// k is the auxiliary transit network link index.

		int node = ag.ib[k];

		return (-AuxTrNet.IVT_COEFF*nodeInVehTime[node]
			 + -AuxTrNet.DRIVE_ACCESS_COEFF*nodeDriveAccTime[node]
			   + (-AuxTrNet.WALK_ACCESS_COEFF/AuxTrNet.OVT_COEFF)*(nodeAccWalkTime[node] + ag.walkTime[k])    // add this link's walkTime to it's bnode skim for utility calculation
				 + (-AuxTrNet.WALK_EGRESS_COEFF/AuxTrNet.OVT_COEFF)*nodeEgrWalkTime[node]
				 + (-AuxTrNet.WALK_XFR_COEFF/AuxTrNet.OVT_COEFF)*(nodeTotWalkTime[node] - nodeAccWalkTime[node] - nodeEgrWalkTime[node])
				 + (-AuxTrNet.FIRST_WAIT_COEFF/AuxTrNet.OVT_COEFF)*nodeFirstWaitTime[node]
				 + (-AuxTrNet.XFR_WAIT_COEFF/AuxTrNet.OVT_COEFF)*(nodeTotalWaitTime[node] - nodeFirstWaitTime[node])
				 + -AuxTrNet.COST_COEFF*nodeCost[node]
				 + -AuxTrNet.TRANSFER_COEFF*(nodeBoardings[node] - 1));
	}



	private int mnlMonteCarlo (double[] util, boolean[] avail, int count) {
		// Calculate MNL cumulative distribution function using the utilities and
		// available alternatives arrays passed.  Returns Monte Carlo selection [0,count) from CDF.

		double[] cdf = new double[count];
		double[] expUtil = new double[count];
		double denominator = 0.0, rnum;

		// Sum the exponentiated utilities
		for (int i=0; i < count; i++) {
			if (avail[i]) {
				if (util[i] > MAX_EXP)
					expUtil[i] = Math.exp(MAX_EXP);
				else if (util[i] < MIN_EXP)
					expUtil[i] = Math.exp(MIN_EXP);
				else
					expUtil[i] = Math.exp(util[i]);
				denominator += expUtil[i];
			}
			else {
				expUtil[i] = 0.0;
			}
		}

		// calculate MNL CDF
		cdf[0] = expUtil[0] / denominator;
		for (int i=1; i < count; i++)
			cdf[i] = cdf[i-1] + (expUtil[i] / denominator);

		// Monte Carlo selection
		rnum = Math.random();			//  0 <= rnum < 1.0
		for (int i=0; i < count; i++)
			if (rnum <= cdf[i])
				return i;

		logger.info ("Error: random number [0,1) not located in MNL CDF in mnlMonteCarlo().");
		logger.info ("rnum =" + rnum);
		logger.info (myFormat.right("i", 6) + myFormat.right("avail[i]", 10) + myFormat.right("util[i]", 10) + myFormat.right("prop[i]", 10) + myFormat.right("cdf[i]", 10));
		for (int i=0; i < count; i++)
			logger.info (myFormat.right(i, 6) +
				myFormat.right(avail[i], 10) +
				myFormat.right(util[i], 10, 3) +
				myFormat.right(expUtil[i]/denominator, 10, 3) +
				myFormat.right(cdf[i], 10, 3));
		
		logger.info ("exiting.");
		System.exit(-1);

		return (-1);
	}


	public void printTransitSkimsTo (int dest) {

		int i, k = 0, m, rte;
		int ia, ib;

		boolean debug = classDebug;


		logger.info ("");
		logger.info ("");
		logger.info ("Transit Skims from all origin zones to " + dest);

		logger.info ( myFormat.right("int orig", 9) +
			myFormat.right("ext orig", 9) +
			myFormat.right("walk access", 15) +
			myFormat.right("walk egress", 15) +
			myFormat.right("walk total", 15) +
			myFormat.right("first wait", 15) +
			myFormat.right("total wait", 15) +
			myFormat.right("drive access", 15) +
			myFormat.right("in vehicle", 15) +
			myFormat.right("cost", 15) +
			myFormat.right("boardings", 15) );

		for (i=0; i < g.getNumCentroids(); i++) {
		    ia = i;
		    if (ia > 0) {
				logger.info (myFormat.right(ia, 9) +
					(ia <= g.getNodeCount() ? myFormat.right (indexNode[ia], 9) : myFormat.right(" ", 9)) +
					(nodeAccWalkTime[ia]   == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeAccWalkTime[ia] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right(nodeAccWalkTime[ia],15, 6))) +
					(nodeEgrWalkTime[ia]   == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeEgrWalkTime[ia] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right(nodeEgrWalkTime[ia],15, 6))) +
					(nodeTotWalkTime[ia]   == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeTotWalkTime[ia] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right(nodeTotWalkTime[ia],15, 6))) +
					(nodeFirstWaitTime[ia] == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeFirstWaitTime[ia] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right(nodeFirstWaitTime[ia]/AuxTrNet.OVT_COEFF,15, 6))) +
					(nodeTotalWaitTime[ia] == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeTotalWaitTime[ia] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right(nodeTotalWaitTime[ia]/AuxTrNet.OVT_COEFF,15, 6))) +
					(nodeDriveAccTime[ia]  == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeDriveAccTime[ia] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right(nodeDriveAccTime[ia],15, 6))) +
					(nodeInVehTime[ia]     == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeInVehTime[ia] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right(nodeInVehTime[ia],15, 6))) +
					(nodeCost[ia]          == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeCost[ia] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right(nodeCost[ia],15, 6))) +
					(nodeBoardings[ia]     == -AuxTrNet.INFINITY ? myFormat.right("-Infinity",15) : (nodeBoardings[ia] == AuxTrNet.INFINITY ? myFormat.right("Infinity",15) : myFormat.right(nodeBoardings[ia],15, 6))) );
		    }
		}
	}


	double airlineDistance(int fromNode, int toNode) {

	  double horizMiles, vertMiles, distMiles;
	  boolean debug = classDebug;


	  horizMiles = ((gNodeX[toNode] - gNodeX[fromNode])/LONGITUDE_PER_FEET)/5280.0;
	  vertMiles  = ((gNodeY[toNode] - gNodeY[fromNode])/LONGITUDE_PER_FEET)/5280.0;
	  distMiles = Math.sqrt(horizMiles*horizMiles + vertMiles*vertMiles);


	  if (debug) {
	      logger.info ("fromNode=" + fromNode + ", toNode=" + toNode +
	              ", ax=" + gNodeX[fromNode] + ", ay=" + gNodeY[fromNode] + ", bx=" + gNodeX[toNode] + ", by=" + gNodeY[toNode] +
	              ", horizMiles=" + horizMiles + ", vertMiles=" + vertMiles + ", distMiles=" + distMiles);
	  }


	  return distMiles;
	}



	/*-------------------- Inner class --------------------*/

		public class Heap  {

			public static final boolean DEBUG = false;
			static final double COMPARE_EPSILON = 1.0e-09;


			private int size;
			private int data[];
			private int last;


			public Heap(int size) {
//			if (DEBUG) {
				System.out.println ("creating a heap of size " + (size) );
				System.out.flush();
//			}
				data = new int[size];
				last = -1;
			}


			public Heap(int initData[]) {
			if (DEBUG) {
				System.out.println ("creating a heap of size " + (initData.length) );
				System.out.flush();
			}
				data = new int[initData.length];
				for (int i = 0; i < initData.length; i++) {
					data[i] = initData[i];
				}
				last = initData.length - 1;
				heapify();
			}


			public int peek() {
				if (last == -1) return -1;   // no item left
					return data[0];              // return element at top of heap
			}


			public void clear() {
				last = -1;
                for (int i=0; i < heapContents.length; i++)
                    heapContents[i] = 0;
			}


			//Rearrange current data into a heap
			public void heapify() {
				for (int i = (last - 1) / 2; i >= 0; i--) {
					percolateDown(i);
				}
			}


			public void add(int x) {

				int addIndex;
				
				int m = ag.hwyLink[x];
                if (DEBUG) logger.info("adding " + x + ", last=" + last + ", ag.ia[x]= " + ag.ia[x] +", g.an= " + indexNode[gia[m]] + ", ag.ib[x]= " + ag.ib[x] + ", g.bn= " + indexNode[gib[m]] + ", linkLabel[x]=" + linkLabel[x]);
      
                if (heapContents[ag.ia[x]] == 1) {
                	addIndex = -1;
    				for (int i = last; i >= 0; i--) {
    				    if ( ag.ia[data[i]] == ag.ia[x] ) {
    				    	addIndex = i;
    				    	break;
    				    }
    				}

    				if (addIndex < 0) {
        				// not in the heap any longer, so add it at end
	                	last++;
	                	addIndex = last;
	    				data[last] = x;
    				}
                }
                else {
                	last++;
                	addIndex = last;
    				data[last] = x;
                }

				percolateUp(addIndex);
				heapContents[ag.ia[x]] = 1;
				
			}


			public int remove() {
				if (last == -1) return -1;   // no item left
				int min = data[0];           // remove element at top of heap
				data[0] = data[last];        // move last element to top of heap

				if (last == 0) {
					last = -1;
					if (DEBUG) {
						System.out.println("remove " + min + ", last=" + last);
						System.out.flush();
					}
					return min;
				}

				last--;                      // reduce heap size
				percolateDown(0);            // move element at top down

				if (DEBUG) {
					System.out.println("remove " + min + ", last=" + last);
					System.out.flush();
				}
				return min;
			}


    		/**
    		 * remove element i from the heap. 
    		 * 
    		 */
    		public int remove(int i) {
    			if (last == -1) return -1;   // no item left
    			int min = data[i];           // remove element at top of heap
    			data[i] = data[last];        // move last element to top of heap
    
    			if (last == 0) {
    				last = -1;
    				if (DEBUG) logger.info("remove " + min + ", last=" + last);
    				return min;
    			}
    
    			last--;                      // reduce heap size
    			percolateDown(i);            // move element at top down
    
    			if (DEBUG) logger.info("remove " + min + ", last=" + last);
    			return min;
    		}


			//Let element move up and settle
			public void percolateUp(int idx) {
				if (DEBUG) {
					System.out.println("pu " + idx);
					System.out.flush();
				}
				if (idx == 0) return;
				int parentIdx = (idx - 1) / 2;
				int k = data[idx];									// added
				int kParent = data[parentIdx];			// added
				if (linkLabel[k] - linkLabel[kParent] < -COMPARE_EPSILON) {			// added
//					if (data[parentIdx] > data[idx]) {
					if (DEBUG) {
						System.out.println ("pu: first if true");
						System.out.flush();
					}
					swap(parentIdx, idx);           // move larger parent down
					percolateUp(parentIdx);
				}
				else if ((linkLabel[k] - linkLabel[kParent] <= COMPARE_EPSILON) && (ag.ia[k] <= ag.ia[kParent]) && (ag.ib[k] < ag.ib[kParent])) {			// added
//					if (data[parentIdx] > data[idx]) {
					if (DEBUG) {
						System.out.println ("pu: first else if true");
						System.out.flush();
					}
					swap(parentIdx, idx);           // move larger parent down
					percolateUp(parentIdx);
				}
			}


			public void percolateDown(int idx) {
				if (DEBUG) {
					System.out.println("pd " + idx);
					System.out.flush();
				}
				int childIdx = idx * 2 + 1;
				if (childIdx > last) return;
				int k = data[idx];									// added
				int kChild = data[childIdx];				// added
				int kChildp1 = data[childIdx+1];		// added

				if ((childIdx + 1) <= last && (linkLabel[kChildp1] - linkLabel[kChild] < -COMPARE_EPSILON)) {			// added
//					if (childIdx + 1 <= last && data[childIdx+1] < data[childIdx]) {
					if (DEBUG) {
						System.out.println ("pd: first if true");
						System.out.flush();
					}
					childIdx = childIdx + 1;
					kChild = data[childIdx];				// added
				}
				else if ((childIdx + 1) <= last && (linkLabel[kChildp1] - linkLabel[kChild] <= COMPARE_EPSILON) && (ag.ia[kChildp1] <= ag.ia[kChild]) && (ag.ib[kChildp1] < ag.ib[kChild])) {			// added
//					if (childIdx + 1 <= last && data[childIdx+1] < data[childIdx]) {
					if (DEBUG) {
						System.out.println ("pd: first else if true, kChild=" + kChild + ", ag.ib[kChild]=" + ag.ib[kChild] + ", kChildp1=" + kChildp1 + ", ag.ib[kChildp1]=" + ag.ib[kChildp1]);
						System.out.flush();
					}
					childIdx = childIdx + 1;
					kChild = data[childIdx];				// added
				}

				if (linkLabel[kChild] - linkLabel[k] < -COMPARE_EPSILON) {			// added
//					if (data[idx] > data[childIdx]) {
					if (DEBUG) {
						System.out.println ("pd: second if true");
						System.out.flush();
					}
					swap(idx, childIdx);
					percolateDown(childIdx);
				}
				else if ((linkLabel[kChild] - linkLabel[k] <= COMPARE_EPSILON) && (ag.ia[kChild] <= ag.ia[k]) && (ag.ib[kChild] < ag.ib[k])) {			// added
//					if (data[idx] > data[childIdx]) {
					if (DEBUG) {
						System.out.println ("pd: second else if true");
						System.out.flush();
					}
					swap(idx, childIdx);
					percolateDown(childIdx);
				}
			}


			public void swap(int idx1, int idx2) {
				int temp = data[idx1];
				data[idx1] = data[idx2];
				data[idx2] = temp;
			}


			//Print heap contents to console (not in sorted order)
			public void dataPrint() {
				int k, m;

				for (int i = 0; i <= last; i++) {
					k = data[i];
					m = ag.hwyLink[k];
					logger.info ("i=" + i + ",k=" + k + ", ag.ia[k]=" + ag.ia[k] + "(g.an=" + indexNode[gia[m]] + "), ag.ib[k]=" + ag.ib[k] + "(g.bn=" + indexNode[gib[m]] + "), linkType=" + ag.linkType[k] + ", linkLabel[k]=" + myFormat.right(linkLabel[k], 10, 6));
				}
			}
		}

}	
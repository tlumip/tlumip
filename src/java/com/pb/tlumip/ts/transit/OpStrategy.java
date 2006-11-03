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
import com.pb.tlumip.ts.assign.ShortestPath;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.IndexSort;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.Logger;



public class OpStrategy {


	protected static Logger logger = Logger.getLogger(OpStrategy.class);

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
	double[] gDist;

	int inStrategyCount;
    double tripsNotLoaded;
	
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
		gDist = g.getDist();
		
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

		int j, k, m, start, end;
        
//        boolean debug = classDebug;
        boolean debug = true;
		
		double linkImped = 0.0;

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

            int dummy = 0;
            if ( ag.ia[k] == 17219 ) {
                dummy = 1;
            }
            
            if ( ag.ib[k] == 17219 ) {
                dummy = 1;
            }
            
            
            if (!tested[k] && ag.ia[k] != dest) {

                tested[k] = true;
                
    			// do not include links into centroids in strategy unless its going into dest.
    			if ( ag.ib[k] < g.getNumCentroids() && ag.ib[k] != dest ) {
    				inStrategy[k] = false;
    				continue;
    			}
    			
    			
    			// get the highway network link index for the given transit network link index
    			m = ag.hwyLink[k];
			

				linkImped = ag.getLinkImped(k);
				
				// log some information about the starting condition of the candidate link being examined
				if ( debug ) {
					logger.info ("");
					
					logger.info ("k=" + k + ", ag.ia[k]=" + ag.ia[k] + "(g.an=" + indexNode[gia[m]] + "), ag.ib[k]=" + ag.ib[k] + "(g.bn=" + indexNode[gib[m]] + "), linkType=" + ag.linkType[k] + ", trRoute=" + ag.trRoute[k] + "(" + (ag.trRoute[k] >= 0 ? ag.tr.getLine(ag.trRoute[k]) : "aux") + ")" );
					logger.info ("nodeLabel[ag.ia=" + ag.ia[k] + "]=" + nodeLabel[ag.ia[k]]);
					logger.info ("nodeLabel[ag.ib=" + ag.ib[k] + "]=" + nodeLabel[ag.ib[k]]);
					logger.info ("nodeFreq[ag.ia=" + ag.ia[k] + "]=" + nodeFreq[ag.ia[k]]);
					logger.info ("nodeFreq[ag.ib=" + ag.ib[k] + "]=" + nodeFreq[ag.ib[k]]);
					logger.info ("ag.freq[k=" + k + "]=" + ag.freq[k]);
					logger.info ("linkImped(k=" + k + ")=" + linkImped);
					
				}

				
				// if the anode's label is at least as big as bnode's label + link's utility, the link is a candidate to be added to strategy; otherwise get next link. 
				if (nodeLabel[ag.ia[k]] >= (nodeLabel[ag.ib[k]] + linkImped)) {
					
					// alighting link
					if (ag.linkType[k] == AuxTrNet.ALIGHTING_TYPE) {

						nodeLabel[ag.ia[k]] = nodeLabel[ag.ib[k]] + linkImped;

						// the in-vehicle transit segment preceding this alighting link has index = k - 1.
						// the in-vehicle transit segment preceding this alighting link which has a dwell time factor has index = k - 4.
						// if the dwellTime for the in-vehicle segment is negative, this segment is the last one for the route,
						// so the dwell time for the in-vehicle segment will be determined by the following auxiliary link.
						if ( ag.dwellTime[k-1] < 0 ) {
							
							// find the auxilliary transit link following this alighting link in the strategy.
							// assign the dwell time for the in-vehicle link to the dwell time calculated for the auxiliary link
							start = ag.ipa[ag.ib[k]];
							j = ag.ib[k] + 1;
							while (ag.ipa[j] == -1)
								j++;
							end = ag.ipa[j];
							for (int i=start; i < end; i++) {
								j = ag.indexa[i];
								if (ag.linkType[j] == AuxTrNet.AUXILIARY_TYPE && inStrategy[j]) {
									// use dwell time factor from in-vehicle link preceding last in-vehicle link in route.
									ag.dwellTime[k-1] = gDist[ag.hwyLink[j]]*(-ag.dwellTime[k-1]);
									break;
								}
							}
							if ( ag.dwellTime[k-1] < 0 )
								ag.dwellTime[k-1] = 0;
							
						}
						
						inStrategy[k] = true;
						strategyOrderForLink[k] = inStrategyCount;
						orderInStrategy[inStrategyCount++] = k;
						updateEnteringLabels(ag.ia[k]);

					}
					// boarding link
					else if (ag.linkType[k] == AuxTrNet.BOARDING_TYPE) {

						if ( nodeFreq[ag.ia[k]] == 0.0 ) {
							
							// first transit boarding link considered from the current node
							nodeLabel[ag.ia[k]] = (AuxTrNet.ALPHA + ag.freq[k]*(nodeLabel[ag.ib[k]] + linkImped))/(nodeFreq[ag.ia[k]] + ag.freq[k]);
							nodeFreq[ag.ia[k]] = ag.freq[k];

						}
						else {
							
							// at least one transit boarding link from the current node exists in optimal strategy
							nodeLabel[ag.ia[k]] = (nodeFreq[ag.ia[k]]*nodeLabel[ag.ia[k]] + ag.freq[k]*(nodeLabel[ag.ib[k]] + linkImped))/(nodeFreq[ag.ia[k]] + ag.freq[k]);
							nodeFreq[ag.ia[k]] += ag.freq[k];
							
                            // use heap.add() to insert the relabelled node at the end and allow it to percolate to the right position.
                            candidateHeap.add(k);
						}
						
						inStrategy[k] = true;
						strategyOrderForLink[k] = inStrategyCount;
						orderInStrategy[inStrategyCount++] = k;
						updateEnteringLabels (ag.ia[k]);
							
					}
					// non-boarding link - either in-vehicle or auxilliary
					else {
						
						nodeLabel[ag.ia[k]] = nodeLabel[ag.ib[k]] + linkImped;

						inStrategy[k] = true;
						strategyOrderForLink[k] = inStrategyCount;
						orderInStrategy[inStrategyCount++] = k;
						updateEnteringLabels(ag.ia[k]);
					}
				}
				else {
					
					if (debug) logger.info ("link not included in strategy");
					inStrategy[k] = false;
					
				}


				// log some information about the ending condition of the candidate link being examined
				if ( debug && inStrategy[k] ) {
					
					logger.info ("");
					logger.info ("k=" + k + ", linkType=" + ag.linkType[k] + ", trRoute=" + ag.trRoute[k]);
					logger.info ("ag.ia[k]=" + ag.ia[k] + "(g.an=" + indexNode[gia[m]] + "), ag.ib[k]=" + ag.ib[k] + "(g.bn=" + indexNode[gib[m]] + ")");
					logger.info ("nodeLabel[ag.ia=" + ag.ia[k] + "]=" + nodeLabel[ag.ia[k]]);
					logger.info ("nodeLabel[ag.ib=" + ag.ib[k] + "]=" + nodeLabel[ag.ib[k]]);
					logger.info ("nodeFreq[ag.ia=" + ag.ia[k] + "]=" + nodeFreq[ag.ia[k]]);
					logger.info ("nodeFreq[ag.ib=" + ag.ib[k] + "]=" + nodeFreq[ag.ib[k]]);
					logger.info ("ag.freq[k=" + k + "]=" + ag.freq[k]);
					logger.info ("inStrategy[k=" + k + "]=" + inStrategy[k]);
					
				}
				
			}
			
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
			logger.info ("nodeAccWalkTime[" + node + "]=    " + String.format ("%10.5f", nodeAccWalkTime[node]));
			logger.info ("nodeEgrWalkTime[" + node + "]=    " + String.format ("%10.5f", nodeEgrWalkTime[node]));
			logger.info ("nodeTotWalkTime[" + node + "]=    " + String.format ("%10.5f", nodeTotWalkTime[node]));
			logger.info ("nodeFirstWaitTime[" + node + "]=  " + String.format ("%10.5f", nodeFirstWaitTime[node]));
			logger.info ("nodeTotalWaitTime[" + node + "]=  " + String.format ("%10.5f", nodeTotalWaitTime[node]));
			logger.info ("nodeDriveAccTime[" + node + "]=   " + String.format ("%10.5f", nodeDriveAccTime[node]));
			logger.info ("nodeInVehTime[" + node + "]=      " + String.format ("%10.5f", nodeInVehTime[node]));
			logger.info ("nodeCost[" + node + "]=           " + String.format ("%10.5f", nodeCost[node]));
			logger.info ("nodeBoardings[" + node + "]=      " + String.format ("%10.5f", nodeBoardings[node]));
			System.exit (-1);
			return (false);
		}
	}



	private int updateEnteringLabels (int currentNode) {
		// calculate linkLabels[] for use in ordering the contents of the heap.
		// linkLabel[k] is the cumulative utility from ia[k] to dest.

		int i, j, k, m;
		int start, end;
//		boolean debug = classDebug;
      boolean debug = true;
		double linkImped = 0.0;

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
			logger.info ("end=" + end + ", indexb[end]=" + (end < ag.indexb.length ? Integer.toString(ag.indexb[end]) : "null") + ", ia=" + (end < ag.indexb.length ? Integer.toString(ag.ia[ag.indexb[end]]) : "null") + ", ib=" + (end < ag.indexb.length ? Integer.toString(ag.ib[ag.indexb[end]]) : "null"));
		  	logger.info ("");
		}
		for (i=start; i < end; i++) {
			k = ag.indexb[i];
			m = ag.hwyLink[k];

			// if link k is a boarding link, but the in-vehicle link that follows it (link k+1) is not in the strategy,
			// don't add link k to the heap.
			if ( ag.linkType[k] == AuxTrNet.BOARDING_TYPE && !inStrategy[k+1] )
				continue;
				
			linkImped = ag.getLinkImped(k);
			linkLabel[k] = nodeLabel[ag.ib[k]] + linkImped;

			// if the anode's label is already smaller than the bnode's label plus the link impedance,
			// no need to add the link to the heap. 
			if ( nodeLabel[ag.ia[k]] < (nodeLabel[ag.ib[k]] + linkImped) )
				continue;

			if (debug)
				logger.info ("adding   " + i + ", indexb[i] or k=" + k + ", linkType=" + ag.linkType[k] + ", ia=" + ag.ia[k] + "(" + indexNode[gia[m]] + "), ib=" + ag.ib[k] + "(" + indexNode[gib[m]] + "), linkLabel[k]=" + String.format("%15.6f", linkLabel[k]) + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ag.ib[k]] + ", linkImped=" + linkImped);
            
			candidateHeap.add(k);

		}

		if (debug) candidateHeap.dataPrintSorted();
			
		return 0;
	}



	public void wtSkimsFromDest () {

		int i, k, m;
		int ia, ib;
//		double wait;

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

			

			if (debug) {
				logger.info ("");
				logger.info ("=====================================================================================================");
				logger.info ("k=" + k + ", i=" + i + ", m=" + m + ", ag.ia[k]=" + ag.ia[k] + ", ag.ib[k]=" + ag.ib[k] + ", ag.an[k]=" + (ag.ia[k] < indexNode.length ? indexNode[ag.ia[k]] : 0) + ", ag.bn[k]=" + (ag.ib[k] < indexNode.length ? indexNode[ag.ib[k]] : 0) + ", g.an[k]=" + indexNode[gia[m]] + ", g.bn[k]=" + indexNode[gib[m]] );
				logger.info ("=====================================================================================================");
				logger.info ("");
				logger.info (String.format("%-25s%15d", "first[ia]=", first[ia] ? 1 : 0) );
				logger.info (String.format("%-25s%15d", "ag.linkType[" + k + "]=", ag.linkType[k]) );
				logger.info (String.format("%-25s%15s", "ag.walkTime[" + k + "]=",  (ag.walkTime[k]        == -AuxTrNet.INFINITY ? "-Infinity" : (ag.walkTime[k]        == AuxTrNet.INFINITY ? "Infinity" : String.format ("%15.4f", ag.walkTime[k])))) );
				logger.info (String.format("%-25s%15s", "ag.invTime[" + k + "]=", (ag.invTime[k]         == -AuxTrNet.INFINITY ? "-Infinity" : (ag.invTime[k]         == AuxTrNet.INFINITY ? "Infinity" : String.format ("%15.4f", ag.invTime[k],15, 4)))) );
				logger.info (String.format("%-25s%15s", "ag.driveAccTime[" + k + "]=", (ag.driveAccTime[k]    == -AuxTrNet.INFINITY ? "-Infinity" : (ag.driveAccTime[k]    == AuxTrNet.INFINITY ? "Infinity" : String.format ("%15.4f", ag.driveAccTime[k],15, 4)))) );
				logger.info (String.format("%-25s%15s", "ag.cost[" + k + "]=", (ag.cost[k]            == -AuxTrNet.INFINITY ? "-Infinity" : (ag.cost[k]            == AuxTrNet.INFINITY ? "Infinity" : String.format ("%15.4f", ag.cost[k],15, 4)))) );
				logger.info (String.format("%-25s%15s", "ag.freq[" + k + "]=", (ag.freq[k]            == -AuxTrNet.INFINITY ? "-Infinity" : (ag.freq[k]            == AuxTrNet.INFINITY ? "Infinity" : String.format ("%15.4f", ag.freq[k],15, 4)))) );
				logger.info (String.format("%-25s%15s", "nodeFreq[" + ia + "]=", (nodeFreq[ia]          == -AuxTrNet.INFINITY ? "-Infinity" : (nodeFreq[ia]          == AuxTrNet.INFINITY ? "Infinity" : String.format ("%15.4f", nodeFreq[ia],15, 4)))) );
				logger.info (String.format("%-25s%15s", "nodeFreq[" + ib + "]=", (nodeFreq[ib]          == -AuxTrNet.INFINITY ? "-Infinity" : (nodeFreq[ib]          == AuxTrNet.INFINITY ? "Infinity" : String.format ("%15.4f", nodeFreq[ib],15, 4)))) );
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
				logger.info ( String.format("%-25s%15s", "nodeAccWalkTime[" + ia + "]=", (nodeAccWalkTime[ia]   == -AuxTrNet.INFINITY ? "-Infinity" : (nodeAccWalkTime[ia]   == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeAccWalkTime[ia])))) );
				logger.info ( String.format("%-25s%15s", "nodeAccWalkTime[" + ib + "]=", (nodeAccWalkTime[ib]   == -AuxTrNet.INFINITY ? "-Infinity" : (nodeAccWalkTime[ib]   == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeAccWalkTime[ib])))) );
				logger.info ( String.format("%-25s%15s", "nodeEgrWalkTime[" + ia + "]=", (nodeEgrWalkTime[ia]   == -AuxTrNet.INFINITY ? "-Infinity" : (nodeEgrWalkTime[ia]   == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeEgrWalkTime[ia])))) );
				logger.info ( String.format("%-25s%15s", "nodeEgrWalkTime[" + ib + "]=", (nodeEgrWalkTime[ib]   == -AuxTrNet.INFINITY ? "-Infinity" : (nodeEgrWalkTime[ib]   == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeEgrWalkTime[ib])))) );
				logger.info ( String.format("%-25s%15s", "nodeTotWalkTime[" + ia + "]=", (nodeTotWalkTime[ia]   == -AuxTrNet.INFINITY ? "-Infinity" : (nodeTotWalkTime[ia]   == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeTotWalkTime[ia])))) );
				logger.info ( String.format("%-25s%15s", "nodeTotWalkTime[" + ib + "]=", (nodeTotWalkTime[ib]   == -AuxTrNet.INFINITY ? "-Infinity" : (nodeTotWalkTime[ib]   == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeTotWalkTime[ib])))) );
				logger.info ( String.format("%-25s%15s", "nodeFirstWaitTime[" + ia + "]=", (nodeFirstWaitTime[ia] == -AuxTrNet.INFINITY ? "-Infinity" : (nodeFirstWaitTime[ia] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeFirstWaitTime[ia])))) );
				logger.info ( String.format("%-25s%15s", "nodeFirstWaitTime[" + ib + "]=", (nodeFirstWaitTime[ib] == -AuxTrNet.INFINITY ? "-Infinity" : (nodeFirstWaitTime[ib] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeFirstWaitTime[ib])))) );
				logger.info ( String.format("%-25s%15s", "nodeTotalWaitTime[" + ia + "]=", (nodeTotalWaitTime[ia] == -AuxTrNet.INFINITY ? "-Infinity" : (nodeTotalWaitTime[ia] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeTotalWaitTime[ia])))) );
				logger.info ( String.format("%-25s%15s", "nodeTotalWaitTime[" + ib + "]=", (nodeTotalWaitTime[ib] == -AuxTrNet.INFINITY ? "-Infinity" : (nodeTotalWaitTime[ib] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeTotalWaitTime[ib])))) );
				logger.info ( String.format("%-25s%15s", "nodeDriveAccTime[" + ia + "]=", (nodeDriveAccTime[ia]  == -AuxTrNet.INFINITY ? "-Infinity" : (nodeDriveAccTime[ia]  == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeDriveAccTime[ia])))) );
				logger.info ( String.format("%-25s%15s", "nodeDriveAccTime[" + ib + "]=", (nodeDriveAccTime[ib]  == -AuxTrNet.INFINITY ? "-Infinity" : (nodeDriveAccTime[ib]  == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeDriveAccTime[ib])))) );
				logger.info ( String.format("%-25s%15s", "nodeInVehTime[" + ia + "]=", (nodeInVehTime[ia]     == -AuxTrNet.INFINITY ? "-Infinity" : (nodeInVehTime[ia]     == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeInVehTime[ia])))) );
				logger.info ( String.format("%-25s%15s", "nodeInVehTime[" + ib + "]=", (nodeInVehTime[ib]     == -AuxTrNet.INFINITY ? "-Infinity" : (nodeInVehTime[ib]     == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeInVehTime[ib])))) );
				logger.info ( String.format("%-25s%15s", "nodeCost[" + ia + "]=", (nodeCost[ia]          == -AuxTrNet.INFINITY ? "-Infinity" : (nodeCost[ia]          == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeCost[ia])))) );
				logger.info ( String.format("%-25s%15s", "nodeCost[" + ib + "]=", (nodeCost[ib]          == -AuxTrNet.INFINITY ? "-Infinity" : (nodeCost[ib]          == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeCost[ib])))) );
				logger.info ( String.format("%-25s%15s", "nodeBoardings[" + ia + "]=", (nodeBoardings[ia]     == -AuxTrNet.INFINITY ? "-Infinity" : (nodeBoardings[ia]     == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeBoardings[ia])))) );
				logger.info ( String.format("%-25s%15s", "nodeBoardings[" + ib + "]=", (nodeBoardings[ib]     == -AuxTrNet.INFINITY ? "-Infinity" : (nodeBoardings[ib]     == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.4f", nodeBoardings[ib])))) );
				logger.info ( (nodeBoardings[ia] == 0) + ", " + nodeBoardings[ia] );
				logger.info ( (nodeBoardings[ib] == 0) + ", " + nodeBoardings[ib] );
			}

		}

	}



    public double[] loadOptimalStrategyDest (double[] tripColumn) {

        // tripColumn is the column of the trip table for the destination zone for this optimal strategy 
        int k, m;
        int count;
        double flow;
        boolean debug = false;
        
        int[] originsNotLoaded = new int[tripColumn.length];
        ArrayList origList = new ArrayList();
        
        tripsNotLoaded = 0;
        
        // the trips are loaded onto the network at the origin zone centroid nodes.
        for (int origTaz=0; origTaz < tripColumn.length; origTaz++) {
            
            // no intra-zonal trips assigned, so go to next orig if orig==dest.
            if ( origTaz == dest) continue;

            nodeFlow[origTaz] = tripColumn[origTaz];
            
            if ( tripColumn[origTaz] > 0.0 ) {
                origList.add(origTaz);
                originsNotLoaded[origTaz] = 1;
            }
        }


        // allocate an array to store boardings by route to be passed back to calling method.
        double[] routeBoardingsToDest = new double[ag.getMaxRoutes()];
        
        
        // loop through links in optimal strategy in reverse order and allocate
        // flow at the nodes to exiting links in the optimal strategy
        count = 0;
        for (int i=inStrategyCount; i >= 0; i--) {
            
            k = orderInStrategy[i];
            m = ag.hwyLink[k];

            if ( ag.linkType[k] == AuxTrNet.BOARDING_TYPE) {
                
                flow = (ag.freq[k]/nodeFreq[ag.ia[k]])*nodeFlow[ag.ia[k]];

                if ( flow > 0 ) {
                    ag.flow[k] = flow;
                    nodeFlow[ag.ib[k]] += flow;
                    routeBoardingsToDest[ag.trRoute[k]] += flow;
                }

            }
            else {

                flow = nodeFlow[ag.ia[k]];

                if ( flow > 0 ) {
                    if ( nodeLabel[ag.ib[k]] != AuxTrNet.INFINITY ) {
                        ag.flow[k] = flow;
                        nodeFlow[ag.ib[k]] += flow;
                        nodeFlow[ag.ia[k]] -= flow;
                        
                        if ( ag.ia[k] < g.getNumCentroids() )
                            originsNotLoaded[ag.ia[k]] = 0;
                    }
                }

            }
            
            
            
            if (debug) {
                logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + ag.trRoute[k] + ", ag.ia=" + ag.ia[k] + ", ag.ib="  + ag.ib[k] + ", g.an=" + indexNode[gia[m]] + ", g.bn=" + indexNode[gib[m]] + ", ag.linkType=" + ag.linkType[k] + ", ag.walkTime=" + ag.walkTime[k] + ", ag.invTime=" + ag.invTime[k] + ", ag.waitTime=" + ag.waitTime[k] + ", ag.flow[k]=" + ag.flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ag.ia[k]] + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ag.ib[k]] );
            }
            
            count++;
        
        }

        for (int origTaz=0; origTaz < tripColumn.length; origTaz++) {
            if ( originsNotLoaded[origTaz] == 1 )
                tripsNotLoaded += tripColumn[origTaz];
        }
        
        return routeBoardingsToDest;

    }


    public double getTripsNotLoaded () {
        return tripsNotLoaded;
    }
    

    public void getOptimalStrategyWtSkimsFromOrig (int startFromNode, int startToNode) {

        int k, m;
//        int start;
//        int end;
//        int check;
        int fromNodeIndex=0;
        int count;
        
//        double waitTime;
        double flow;
        
        boolean debug = classDebug;
//        boolean debug = true;
        


        if (startFromNode == indexNode[dest]) return;


        // find the link index of the first optimal strategy link exiting fromNode
        // allocate 1 trip to routes between fromNode and dest to track proportions allocated to multiple paths in strategy
        for (int i=inStrategyCount - 1; i >= 0; i--) {
            k = orderInStrategy[i];
            m = ag.hwyLink[k];
            if ( ag.ia[k] == nodeIndex[startFromNode] ) {
                fromNodeIndex = i;
                nodeFlow[ag.ia[k]] = 1.0;
                if (debug) {
                    logger.info ("");
                    logger.info ( "startFromNode=" + startFromNode + "(" + nodeIndex[startFromNode] + "), startToNode=" + startToNode + "(" + nodeIndex[startToNode] + "), fromNodeIndex=" + fromNodeIndex + ", i=" + i + ", k=" + k + ", m=" + m + ", ag.ia=" + ag.ia[k] + ", ag.ib=" + ag.ib[k] + ", g.an=" + indexNode[gia[m]] + ", g.bn=" + indexNode[gib[m]] + ", ag.linkType=" + ag.linkType[k] );
                }
                break;
            }
        }
        

        nodeFlow[nodeIndex[startFromNode]] = 1.0;
        
        
        // loop through links in optimal strategy starting at fromNode, stopping at dest
        count = 0;
        for (int i=fromNodeIndex; i >= 0; i--) {
            
            k = orderInStrategy[i];
            m = ag.hwyLink[k];
            
            
            int dummy = 0;
            if ( indexNode[gia[m]] == 24609 && indexNode[gib[m]] == 24969 ) {
                if ( ag.linkType[k] == 0 ) {
                    dummy = 1;
                }
                else if ( ag.linkType[k] == 1 ) {
                    dummy = 1;
                }
                else if ( ag.linkType[k] == 2 ) {
                    dummy = 1;
                }
                else {
                    dummy = 1;
                }
            }
            
            if (nodeFlow[ag.ia[k]] > 0.0) {
                
                if ( ag.linkType[k] == AuxTrNet.BOARDING_TYPE ) {
                    flow = (ag.freq[k]/nodeFreq[ag.ia[k]])*nodeFlow[ag.ia[k]];
                    ag.flow[k] = flow;
                    nodeFlow[ag.ib[k]] += flow;
                }
                else {
                    flow = nodeFlow[ag.ia[k]];
                    if ( nodeLabel[ag.ib[k]] != AuxTrNet.INFINITY ) {
                        ag.flow[k] += flow;
                        nodeFlow[ag.ib[k]] += flow;
                        //nodeFlow[ag.ia[k]] -= flow;
                    }
                    else {
                        dummy=1;
                    }
                }
                
                logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + ag.trRoute[k] + ", ag.ia=" + ag.ia[k] + ", ag.ib="  + ag.ib[k] + ", g.an=" + indexNode[gia[m]] + ", g.bn=" + indexNode[gib[m]] + ", ag.linkType=" + ag.linkType[k] + ", ag.walkTime=" + ag.walkTime[k] + ", ag.invTime=" + ag.invTime[k] + ", ag.waitTime=" + ag.waitTime[k] + ", ag.flow[k]=" + ag.flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ag.ia[k]] + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ag.ib[k]] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ag.ia[k]] + ", ag.freq[k]=" + ag.freq[k] + ", nodeFlow[ag.ia[k]]=" + nodeFlow[ag.ia[k]] + ", nodeFlow[ag.ib[k]]=" + nodeFlow[ag.ib[k]] );
                count++;

            }

        }
        

        // loop through links in optimal strategy that received flow and log some information 
        Integer tempNode = new Integer(0);
        ArrayList boardingNodes = new ArrayList();
        double inVehTime = 0.0f;
        double dwellTime = 0.0f;
        double walkTime = 0.0f;
        double wtAccTime = 0.0f;
        double wtEgrTime = 0.0f;
        double firstWait = 0.0f;
        double totalWait = 0.0f;
        double boardings = 0.0f;
        
        
        count = 0;
        if (debug)
            logger.info ( "\n\n\nlinks in strategy with flow:" );
        for (int i=inStrategyCount; i >= 0; i--) {
            
            k = orderInStrategy[i];
            m = ag.hwyLink[k];
            
            if (nodeFlow[ag.ia[k]] == 0.0)
                continue;
            
            logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + ag.trRoute[k] + ", ag.ia=" + ag.ia[k] + ", ag.ib="  + ag.ib[k] + ", g.an=" + indexNode[gia[m]] + ", g.bn=" + indexNode[gib[m]] + ", ag.linkType=" + ag.linkType[k] + ", ag.walkTime=" + ag.walkTime[k] + ", ag.invTime=" + ag.invTime[k] + ", ag.waitTime=" + ag.waitTime[k] + ", ag.flow[k]=" + ag.flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ag.ia[k]] + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ag.ib[k]] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ag.ia[k]] );
            
            
            if ( ag.linkType[k] == AuxTrNet.BOARDING_TYPE ) {
                
                tempNode = new Integer(ag.ia[k]);
                
                if ( firstWait == 0.0f ) {
                    boardings = ag.flow[k];
                    boardingNodes.add(tempNode);
                    firstWait = AuxTrNet.ALPHA/nodeFreq[ag.ia[k]];
                    totalWait = firstWait;
                }
                else {
                    boardings += ag.flow[k];
                    if ( !boardingNodes.contains(tempNode) ) {
                        totalWait += AuxTrNet.ALPHA/nodeFreq[ag.ia[k]];
                        boardingNodes.add(tempNode);
                    }
                }
                
            }
            else if ( ag.linkType[k] == AuxTrNet.IN_VEHICLE_TYPE ) {
                
                inVehTime += ag.invTime[k]*ag.flow[k];
                dwellTime += ag.dwellTime[k]*ag.flow[k];
                
            }
            else if ( ag.linkType[k] == AuxTrNet.AUXILIARY_TYPE ) {
                
                if ( ag.ia[k] == nodeIndex[startFromNode] )
                    wtAccTime = ag.walkTime[k]*ag.flow[k];
                else if ( ag.ib[k] == nodeIndex[startToNode] )
                    wtEgrTime = ag.walkTime[k]*ag.flow[k];
                else
                    walkTime += ag.walkTime[k]*ag.flow[k];
                
            }
                
            count++;

        }

        // linkFreqs were weighted by WAIT_COEFF, so unweight them to get actual first and total wait values
        firstWait /= AuxTrNet.WAIT_COEFF;
        totalWait /= AuxTrNet.WAIT_COEFF;

        
        logger.info ( "\n\n\ntransit skims from " + startFromNode + " to " + startToNode + ":" );
        logger.info ( "in-vehicle time  = " + inVehTime );
        logger.info ( "dwell time       = " + dwellTime );
        logger.info ( "firstWait time   = " + firstWait );
        logger.info ( "totalWait time   = " + totalWait );
        logger.info ( "wt access time   = " + wtAccTime );
        logger.info ( "wt egress time   = " + wtEgrTime );
        logger.info ( "other walk time  = " + walkTime );
        logger.info ( "total boardings  = " + boardings );
            

    }



	public Matrix[] getOptimalStrategySkimMatrices () {

		int j, k, m;
		int accessLinkIndex=-1;
		int start, end, check;
		double waitTime;
		
//		boolean debug = true;
		boolean debug = classDebug;
		
//		boolean firstBoard = false;


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
		logger.info ("driveStopChoice(" + String.format("%-5.2f", driveStopPcts) + "," + fromNode + "), stopCount=" + stopCount);
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
		logger.info (String.format("%5d", i) + String.format("%8d", stopNodes[i]) + String.format("%8d", indexNode[stopNodes[i]]) + String.format("%12.3f", stopTimes[i]) );
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
		logger.info (String.format("%5d", i) + String.format("%8d", mnlNodes[i]) + "(" + indexNode[mnlNodes[i]] + ")" + String.format("%12.3f", mnlTimes[i]) );
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
				 + (-AuxTrNet.WAIT_COEFF/AuxTrNet.OVT_COEFF)*(nodeTotalWaitTime[node] - nodeFirstWaitTime[node])
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
		logger.info ( String.format("%6s%10s%10s%10s%10s", "i", "avail[i]", "util[i]", "prop[i]", "cdf[i]") );
		for (int i=0; i < count; i++)
			logger.info ( String.format("%6d%10b%10.3f%10.3f%10.3f", i, avail[i], util[i], expUtil[i]/denominator, cdf[i]) );
		
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

		logger.info ( String.format("%9s%9s%15s%15s%15s%15s%15s%15s%15s%15s%15s", "int orig", "ext orig", "walk access", "walk egress", "walk total", "first wait", "total wait", "drive access", "in vehicle", "cost", "boardings") );

		for (i=0; i < g.getNumCentroids(); i++) {
		    ia = i;
		    if (ia > 0) {
				logger.info ( String.format("%9d%9s%15s%15s%15s%15s%15s%15s%15s%15s%15s", ia,
                    (ia <= g.getNodeCount() ? String.format("%s",indexNode[ia]) : " "),
					(nodeAccWalkTime[ia]   == -AuxTrNet.INFINITY ? "-Infinity" : (nodeAccWalkTime[ia] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.6f", nodeAccWalkTime[ia]))),
					(nodeEgrWalkTime[ia]   == -AuxTrNet.INFINITY ? "-Infinity" : (nodeEgrWalkTime[ia] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.6f", nodeEgrWalkTime[ia]))),
					(nodeTotWalkTime[ia]   == -AuxTrNet.INFINITY ? "-Infinity" : (nodeTotWalkTime[ia] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.6f", nodeTotWalkTime[ia]))),
					(nodeFirstWaitTime[ia] == -AuxTrNet.INFINITY ? "-Infinity" : (nodeFirstWaitTime[ia] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.6f", nodeFirstWaitTime[ia]/AuxTrNet.OVT_COEFF))),
					(nodeTotalWaitTime[ia] == -AuxTrNet.INFINITY ? "-Infinity" : (nodeTotalWaitTime[ia] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.6f", nodeTotalWaitTime[ia]/AuxTrNet.OVT_COEFF))),
					(nodeDriveAccTime[ia]  == -AuxTrNet.INFINITY ? "-Infinity" : (nodeDriveAccTime[ia] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.6f", nodeDriveAccTime[ia]))),
					(nodeInVehTime[ia]     == -AuxTrNet.INFINITY ? "-Infinity" : (nodeInVehTime[ia] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.6f", nodeInVehTime[ia]))),
					(nodeCost[ia]          == -AuxTrNet.INFINITY ? "-Infinity" : (nodeCost[ia] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.6f", nodeCost[ia]))),
					(nodeBoardings[ia]     == -AuxTrNet.INFINITY ? "-Infinity" : (nodeBoardings[ia] == AuxTrNet.INFINITY ? "Infinity" : String.format("%15.6f", nodeBoardings[ia])))) );
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
      
//                if (heapContents[ag.ia[x]] == 1) {
//                	addIndex = -1;
//    				for (int i = last; i >= 0; i--) {
//    				    if ( ag.ia[data[i]] == ag.ia[x] ) {
//    				    	addIndex = i;
//    				    	break;
//    				    }
//    				}
//
//    				if (addIndex < 0) {
//        				// not in the heap any longer, so add it at end
//	                	last++;
//	                	addIndex = last;
//	    				data[last] = x;
//    				}
//                }
//                else {
                	last++;
                	addIndex = last;
    				data[last] = x;
//                }

				percolateUp(addIndex);
//				heapContents[ag.ia[x]] = 1;
				
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

                logger.info( "Heap contents in natural order" );
                for (int i = 0; i <= last; i++) {
                    k = data[i];
                    m = ag.hwyLink[k];
                    logger.info ("i=" + i + ",k=" + k + ", ag.ia[k]=" + ag.ia[k] + "(g.an=" + indexNode[gia[m]] + "), ag.ib[k]=" + ag.ib[k] + "(g.bn=" + indexNode[gib[m]] + "), linkType=" + ag.linkType[k] + ", Route=" + ag.trRoute[k] + ", linkLabel[k]=" + String.format("%10.6f", linkLabel[k]) );
                }
            }

            //Print heap contents to console sorted by label
            public void dataPrintSorted() {
                int k, m;

                int[] values = new int[last+1];
                
                for (int i = 0; i <= last; i++) {
                    k = data[i];
                    values[i] = (int)(linkLabel[k]*100000);
                }
                
                int[] indices = com.pb.common.util.IndexSort.indexSort( values );

                logger.info( "Heap contents sorted by linklabel" );
                for (int i = 0; i < indices.length; i++) {
                    k = data[indices[i]];
                    m = ag.hwyLink[k];
                    logger.info ("i=" + i + ",k=" + k + ", ag.ia[k]=" + ag.ia[k] + "(g.an=" + indexNode[gia[m]] + "), ag.ib[k]=" + ag.ib[k] + "(g.bn=" + indexNode[gib[m]] + "), linkType=" + ag.linkType[k] + ", Route=" + ag.trRoute[k] + ", linkLabel[k]=" + String.format("%10.6f", linkLabel[k]) );
                }
            }
            
		}

}	
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
import com.pb.tlumip.ts.assign.TransitAssignAndSkimManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;



public class OptimalStrategy {


	protected static Logger logger = Logger.getLogger(OptimalStrategy.class);

	int IVT = TransitAssignAndSkimManager.SkimType.IVT.ordinal();      // in-vehicle time
	int FWT = TransitAssignAndSkimManager.SkimType.FWT.ordinal();      // first wait
	int TWT = TransitAssignAndSkimManager.SkimType.TWT.ordinal();      // total wait
    int ACC = TransitAssignAndSkimManager.SkimType.ACC.ordinal();      // access time
    int AUX = TransitAssignAndSkimManager.SkimType.AUX.ordinal();      // transfer time
    int EGR = TransitAssignAndSkimManager.SkimType.EGR.ordinal();      // egress time
    int HSR$ = TransitAssignAndSkimManager.SkimType.HSR$.ordinal();      // hsr fare
    int AIR$ = TransitAssignAndSkimManager.SkimType.AIR$.ordinal();      // air fare
    int RAIL$ = TransitAssignAndSkimManager.SkimType.RAIL$.ordinal();     // intercity rail fare
    int BUS$ = TransitAssignAndSkimManager.SkimType.BUS$.ordinal();      // intercity bus fare
    int TRAN$ = TransitAssignAndSkimManager.SkimType.TRAN$.ordinal();     // intracity transit fare
	public static final int NUM_SKIMS = 11;

	static final double COMPARE_EPSILON = 1.0e-07;

	static final double MIN_ALLOCATED_FLOW = 0.00001;
	
	static final int MAX_BOARDING_LINKS = 100;

//	static final double LATITUDE_PER_FEET  = 2.7;
//	static final double LONGITUDE_PER_FEET = 3.6;
	static final double LATITUDE_PER_FEET  = 1.0;
	static final double LONGITUDE_PER_FEET = 1.0;


    HashMap fareZones;
    HashMap transitFareLookupTable;
    
	NetworkHandlerIF nh;

	int dest;
    
    int auxNodeCount;
    int auxLinkCount;
	
	Heap candidateHeap;
	int[] heapContents;

	double[] nodeLabel, nodeFreq, linkLabel;
	boolean[] inStrategy;
	int[] orderInStrategy;
	int[] strategyOrderForLink;
	
	double[] nodeFlow;

    int[] ia = null;
    int[] ib = null;
    int[] ipa = null;
    int[] ipb = null;
    int[] indexa = null;
    int[] indexb = null;
    int[] hwyLink = null;
    int[] trRoute = null;
    double[] cost = null;
    double[] accessTime = null;
    double[] walkTime = null;
    double[] waitTime = null;
    double[] dwellTime = null;
    double[] layoverTime = null;
    double[] invTime = null;
    double[] freq = null;
    double[] flow = null;
    int[] linkType = null;
    char[] rteMode = null;
	
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
	
	
	
	public OptimalStrategy ( NetworkHandlerIF nh ) {

		this.nh = nh;
        
        
        auxNodeCount = nh.getAuxNodeCount();
        auxLinkCount = nh.getAuxLinkCount();
        
        
		nodeFlow = new double[auxNodeCount+1];
		nodeLabel = new double[auxNodeCount+1];
		nodeFreq = new double[auxNodeCount+1];
		linkLabel = new double[auxLinkCount+1];
		inStrategy = new boolean[auxLinkCount+1];
		orderInStrategy = new int[auxLinkCount+1];
		strategyOrderForLink = new int[auxLinkCount+1];
		
		//Create a new heap structure to sort candidate node labels
        //candidateHeap = new Heap(auxNodeCount+1);  // old Heap 
        candidateHeap = new Heap( auxLinkCount ); // new SortedSet
		heapContents = new int[auxNodeCount+1];

        ia = nh.getAuxIa();
        ib = nh.getAuxIb();
        ipa = nh.getAuxIpa();
        ipb = nh.getAuxIpb();
        indexa = nh.getAuxIndexa();
        indexb = nh.getAuxIndexb();
        hwyLink = nh.getAuxHwyLink();
        trRoute = nh.getLinkTrRoute();
        rteMode = nh.getRteMode();
        linkType = nh.getAuxLinkType();
        cost = nh.getCost();
        dwellTime = nh.getDwellTime();
        layoverTime = nh.getLayoverTime();
        waitTime = nh.getWaitTime(); 
        walkTime = nh.getWalkTime(); 
        invTime = nh.getInvTime();
        freq = nh.getAuxLinkFreq();
        flow = nh.getAuxLinkFlow();
        
		gia = nh.getIa();
		gib = nh.getIb();
		indexNode = nh.getIndexNode();
		nodeIndex = nh.getNodeIndex();
		gNodeX = nh.getNodeX();
		gNodeY = nh.getNodeY();
		gDist = nh.getDist();
		
	}


	private void initData() {
		Arrays.fill(nodeLabel, AuxTrNet.INFINITY);
		Arrays.fill(nodeFlow, 0.0);
		Arrays.fill(nodeFreq, 0.0);
		Arrays.fill(linkLabel, 0.0);
		Arrays.fill(inStrategy, false);
		Arrays.fill(orderInStrategy, 0);
		Arrays.fill(strategyOrderForLink, -1);

		inStrategyCount = 0;
		candidateHeap.clear();
	}



    
	// This method builds the optimal strategy sub-network for the destination taz passed in.
	// The sub-network is represented by the boolean link field inStrategy[] where true indicates
	// the link is part of the strategy and false indicates it is not.
	public int buildStrategy (int dest) {
		// dest is an internally numbered centroid number from highway network (g).

		int j, k, m, start, end;
        
        boolean debug = classDebug;
		
		double linkImped = 0.0;

		this.dest = dest;
		initData();

		
		nodeLabel[dest] = 0;
		nodeFreq[dest] = 0;
		updateEnteringLabels (dest);

		
		// set the access time array based on access mode
		if (nh.getAccessMode().equalsIgnoreCase("walk")) {
			accessTime = walkTime; 
		}
		else {
			accessTime = nh.getDriveAccTime(); 
		}


		if (debug)
		    logger.info ("building optimal strategy to " + dest + "(" + indexNode[dest] + ")");
		
        
        //while ((k = candidateHeap.remove()) != -1) {  //old Heap
        while ( candidateHeap.size() > 0 ) {

            HeapElement he = candidateHeap.getFirst();
            k = he.getIndex();
            
            if (ia[k] != dest && !inStrategy[k]) {

    			// do not include links into centroids in strategy unless its going into dest.
    			if ( ib[k] < nh.getNumCentroids() && ib[k] != dest ) {
    				inStrategy[k] = false;
    				continue;
    			}
    			
    			
				linkImped = nh.getLinkImped(k);
				
				// log some information about the starting condition of the candidate link being examined
				if ( debug ) {
					logger.info ("");
					
                    // get the highway network link index for the given transit network link index
                    m = hwyLink[k];
                

					logger.info ("k=" + k + ", ag.ia[k]=" + ia[k] + "(g.an=" + (m>=0 ? indexNode[gia[m]] : -1) + "), ag.ib[k]=" + ib[k] + "(g.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + "), linkType=" + linkType[k] + ", trRoute=" + trRoute[k] + "(" + (trRoute[k] >= 0 ? nh.getRouteName(trRoute[k]) : "aux") + ")" );
					logger.info ("nodeLabel[ag.ia=" + ia[k] + "]=" + nodeLabel[ia[k]]);
					logger.info ("nodeLabel[ag.ib=" + ib[k] + "]=" + nodeLabel[ib[k]]);
					logger.info ("nodeFreq[ag.ia=" + ia[k] + "]=" + nodeFreq[ia[k]]);
					logger.info ("nodeFreq[ag.ib=" + ib[k] + "]=" + nodeFreq[ib[k]]);
					logger.info ("ag.freq[k=" + k + "]=" + freq[k]);
					logger.info ("linkImped(k=" + k + ")=" + linkImped);
					
				}

				
				// if the anode's label is at least as big as bnode's label + link's utility, the link is a candidate to be added to strategy; otherwise get next link. 
				if (nodeLabel[ia[k]] >= (nodeLabel[ib[k]] + linkImped)) {
					
					// alighting link
					if (linkType[k] == AuxTrNet.ALIGHTING_TYPE) {

						nodeLabel[ia[k]] = nodeLabel[ib[k]] + linkImped;

						// the in-vehicle transit segment preceding this alighting link has index = k - 1.
						// the in-vehicle transit segment preceding this alighting link which has a dwell time factor has index = k - 4.
						// if the dwellTime for the in-vehicle segment is negative, this segment is the last one for the route,
						// so the dwell time for the in-vehicle segment will be determined by the following auxiliary link.
						if ( dwellTime[k-1] < 0 ) {
							
							// find the auxilliary transit link following this alighting link in the strategy.
							// assign the dwell time for the in-vehicle link to the dwell time calculated for the auxiliary link
							start = ipa[ib[k]];
							j = ib[k] + 1;
							while (ipa[j] == -1)
								j++;
							end = ipa[j];
							for (int i=start; i < end; i++) {
								j = indexa[i];
								if (linkType[j] == AuxTrNet.AUXILIARY_TYPE && inStrategy[j]) {
									// use dwell time factor from in-vehicle link preceding last in-vehicle link in route.
									dwellTime[k-1] = gDist[hwyLink[k-1]]*(-dwellTime[k-1]);
									break;
								}
							}
							if ( dwellTime[k-1] < 0 )
								dwellTime[k-1] = 0;
							
						}

						inStrategy[k] = true;
						strategyOrderForLink[k] = inStrategyCount;
                        orderInStrategy[inStrategyCount++] = k;
						updateEnteringLabels(ia[k]);

					}
					// boarding link
					else if (linkType[k] == AuxTrNet.BOARDING_TYPE) {

						if ( nodeFreq[ia[k]] == 0.0 ) {
							
							// first transit boarding link considered from the current node
							nodeLabel[ia[k]] = (AuxTrNet.ALPHA + freq[k]*(nodeLabel[ib[k]] + linkImped))/(nodeFreq[ia[k]] + freq[k]);
							nodeFreq[ia[k]] = freq[k];

						}
						else {
							
							// at least one transit boarding link from the current node exists in optimal strategy
							nodeLabel[ia[k]] = (nodeFreq[ia[k]]*nodeLabel[ia[k]] + freq[k]*(nodeLabel[ib[k]] + linkImped))/(nodeFreq[ia[k]] + freq[k]);
							nodeFreq[ia[k]] += freq[k];

                        }
						
						inStrategy[k] = true;
						strategyOrderForLink[k] = inStrategyCount;
						orderInStrategy[inStrategyCount++] = k;
						updateEnteringLabels (ia[k]);
							
					}
					// non-boarding link - either in-vehicle or auxilliary
					else {
						
						nodeLabel[ia[k]] = nodeLabel[ib[k]] + linkImped;

						inStrategy[k] = true;
						strategyOrderForLink[k] = inStrategyCount;
						orderInStrategy[inStrategyCount++] = k;
						updateEnteringLabels(ia[k]);
					}
				}
				else {
					
					if (debug) logger.info ("link not included in strategy");
					inStrategy[k] = false;
					
				}


				// log some information about the ending condition of the candidate link being examined
				if ( debug && inStrategy[k] ) {
					
                    // get the highway network link index for the given transit network link index
                    m = hwyLink[k];
                

					logger.info ("");
					logger.info ("k=" + k + ", linkType=" + linkType[k] + ", trRoute=" + trRoute[k]);
					logger.info ("ag.ia[k]=" + ia[k] + "(g.an=" + (m >= 0 ? indexNode[gia[m]] : -1) + "), ag.ib[k]=" + ib[k] + "(g.bn=" + ( m>=0 ? indexNode[gib[m]] : -1) + ")");
					logger.info ("nodeLabel[ag.ia=" + ia[k] + "]=" + nodeLabel[ia[k]]);
					logger.info ("nodeLabel[ag.ib=" + ib[k] + "]=" + nodeLabel[ib[k]]);
					logger.info ("nodeFreq[ag.ia=" + ia[k] + "]=" + nodeFreq[ia[k]]);
					logger.info ("nodeFreq[ag.ib=" + ib[k] + "]=" + nodeFreq[ib[k]]);
					logger.info ("ag.freq[k=" + k + "]=" + freq[k]);
					logger.info ("inStrategy[k=" + k + "]=" + inStrategy[k]);
					
				}
				
			}

		} // end of while heap not empty

		return 0;

	}



    private int updateEnteringLabels (int currentNode) {
        // calculate linkLabels[] for use in ordering the contents of the heap.
        // linkLabel[k] is the cumulative utility from ia[k] to dest.

        int i, j, k, m;
        int start, end;
        boolean debug = classDebug;
//      boolean debug = true;
        double linkImped = 0.0;

        if (debug) {
            logger.info ("");
            logger.info ("updateEnteringLabels(): currentNode = " + currentNode);
        }

        start = ipb[currentNode];
        if (start == -1) {
            return -1;
        }


        
        if (debug)
              logger.info ("start=" + start + ", indexb[start]=" + indexb[start] + ", ia=" + ia[indexb[start]] + ", ib=" + ib[indexb[start]] + ", an=" + (ia[indexb[start]] < indexNode.length ? indexNode[ia[indexb[start]]] : 0) + ", bn=" + (ib[indexb[start]] < indexNode.length ? indexNode[ib[indexb[start]]] : 0));
        j = currentNode + 1;
        while (ipb[j] == -1)
            j++;
        end = ipb[j];
        if (debug) {
            logger.info ("end=" + end + ", j=" + j);
            logger.info ("end=" + end + ", indexb[end]=" + (end < indexb.length ? Integer.toString(indexb[end]) : "null") + ", ia=" + (end < indexb.length ? Integer.toString(ia[indexb[end]]) : "null") + ", ib=" + (end < indexb.length ? Integer.toString(ib[indexb[end]]) : "null"));
            logger.info ("");
        }
        for (i=start; i < end; i++) {
            k = indexb[i];

            // if link k is a boarding link, but the in-vehicle link that follows it (link k+1) is not in the strategy,
            // don't add link k to the heap.
            if ( linkType[k] == AuxTrNet.BOARDING_TYPE && !inStrategy[k+1] )
                continue;
                
            linkImped = nh.getLinkImped(k);
            linkLabel[k] = nodeLabel[ib[k]] + linkImped;

            // if the anode's label is already smaller than the bnode's label plus the link impedance,
            // no need to add the link to the heap. 
            if ( nodeLabel[ia[k]] < (nodeLabel[ib[k]] + linkImped) )
                continue;

            if (debug) {
                m = hwyLink[k];
                logger.info ("adding   " + i + ", indexb[i] or k=" + k + ", linkType=" + linkType[k] + ", ia=" + ia[k] + "(" + (m>=0 ? indexNode[gia[m]] : -1) + "), ib=" + ib[k] + "(" + (m>=0 ? indexNode[gib[m]] : -1) + "), linkLabel[k]=" + String.format("%15.6f", linkLabel[k]) + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ib[k]] + ", linkImped=" + linkImped);
            }

            HeapElement he = new HeapElement(k, linkType[k], linkLabel[k]);
            
            if ( candidateHeap.contains(k))
                candidateHeap.remove(he);
            
            candidateHeap.add(he);

        }

        if (debug) candidateHeap.dataPrintSorted();
            
        return 0;
    }





    public double[] loadOptimalStrategyDest (double[] tripColumn) {

        // tripColumn is the column of the trip table for the destination zone for this optimal strategy 
        int k, m;
        int count;
        double linkFlow;
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
        double[] routeBoardingsToDest = new double[nh.getMaxRoutes()];
        
        
        // loop through links in optimal strategy in reverse order and allocate
        // flow at the nodes to exiting links in the optimal strategy
        count = 0;
        for (int i=inStrategyCount; i >= 0; i--) {
            
            k = orderInStrategy[i];
            m = hwyLink[k];

            if ( linkType[k] == AuxTrNet.BOARDING_TYPE) {
                
                linkFlow = (freq[k]/nodeFreq[ia[k]])*nodeFlow[ia[k]];

                if ( linkFlow > 0 ) {
                    flow[k] = linkFlow;
                    nodeFlow[ib[k]] += linkFlow;
                    routeBoardingsToDest[trRoute[k]] += linkFlow;
                }

            }
            else {

                linkFlow = nodeFlow[ia[k]];

                if ( linkFlow > 0 ) {
                    if ( nodeLabel[ib[k]] != AuxTrNet.INFINITY ) {
                        flow[k] = linkFlow;
                        nodeFlow[ib[k]] += linkFlow;
                        nodeFlow[ia[k]] -= linkFlow;
                        
                        if ( ia[k] < nh.getNumCentroids() )
                            originsNotLoaded[ia[k]] = 0;
                    }
                }

            }
            
            
            
            if (debug) {
                logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + trRoute[k] + ", ag.ia=" + ia[k] + ", ag.ib="  + ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", linkType=" + linkType[k] + ", ag.walkTime=" + walkTime[k] + ", invTime=" + invTime[k] + ", ag.waitTime=" + waitTime[k] + ", flow[k]=" + flow[k] + ", nodeLabel[ia[k]]=" + nodeLabel[ia[k]] + ", nodeLabel[ib[k]]=" + nodeLabel[ib[k]] );
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
    

//    public double[] getOptimalStrategyWtSkimsOrigDest (int startFromNode, int startToNode) {
//
//        // startFromNode and startToNode are externally numbered.
//        
//        int k, m;
//        int fromNodeIndex=-1;
//        int count;
//        
//        double linkFlow;
//        
//        boolean debug = classDebug;
////        boolean debug = true;
//        
//
//        double[] results = new double[6];
//        Arrays.fill (results, AuxTrNet.UNCONNECTED);
//        Arrays.fill (nodeFlow, 0.0);
//        Arrays.fill (flow, 0.0);
//
//        
//        if (startFromNode == indexNode[dest]) {
//            return results;
//        }
//
//
//        // find the link index of the first optimal strategy link exiting fromNode
//        // allocate 1 trip to routes between fromNode and dest to track proportions allocated to multiple paths in strategy
//        for (int i=inStrategyCount - 1; i >= 0; i--) {
//            k = orderInStrategy[i];
//            m = hwyLink[k];
//            if ( ia[k] == nodeIndex[startFromNode] ) {
//                fromNodeIndex = i;
//                nodeFlow[ia[k]] = 1.0;
//                if (debug) {
//                    logger.info ("");
//                    logger.info ( "startFromNode=" + startFromNode + "(" + nodeIndex[startFromNode] + "), startToNode=" + startToNode + "(" + nodeIndex[startToNode] + "), fromNodeIndex=" + fromNodeIndex + ", i=" + i + ", k=" + k + ", m=" + m + ", ag.ia=" + ia[k] + ", ag.ib=" + ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", linkType=" + linkType[k] );
//                }
//                break;
//            }
//        }
//        
//        if ( fromNodeIndex < 0 ) 
//            return results;
//
//        
//        // set one trip starting at the startFromNode
//        nodeFlow[nodeIndex[startFromNode]] = 1.0;
//        
//        
//        // loop through links in optimal strategy starting at the index where the startFromNode was found and assign flow to links in the strategy
//        count = 0;
//        for (int i=fromNodeIndex; i >= 0; i--) {
//            
//            k = orderInStrategy[i];
//            m = hwyLink[k];
//            
//            if (nodeFlow[ia[k]] > 0.0) {
//
//                
//                if ( linkType[k] == AuxTrNet.BOARDING_TYPE ) {
//                    linkFlow = (freq[k]/nodeFreq[ia[k]])*nodeFlow[ia[k]];
//                    flow[k] = linkFlow;
//                    nodeFlow[ib[k]] += linkFlow;
//                }
//                else {
//                    linkFlow = nodeFlow[ia[k]];
//                    if ( nodeLabel[ib[k]] != AuxTrNet.INFINITY ) {
//                        flow[k] += linkFlow;
//                        nodeFlow[ib[k]] += linkFlow;
//                    }
//                }
//
//                if ( debug )
//                    logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + trRoute[k] + ", ag.ia=" + ia[k] + ", ag.ib="  + ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", linkType=" + linkType[k] + ", ag.walkTime=" + walkTime[k] + ", invTime=" + invTime[k] + ", waitTime=" + waitTime[k] + ", flow[k]=" + flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ia[k]] + ", nodeLabel[ib[k]]=" + nodeLabel[ib[k]] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ia[k]] + ", ag.freq[k]=" + freq[k] + ", nodeFlow[agia]]=" + nodeFlow[ia[k]] + ", nodeFlow[ag.ib[k]]=" + nodeFlow[ib[k]] );
//                
//                count++;
//
//            }
//
//        }
//        
//
//        // loop through links in optimal strategy that received flow and log some information 
//        Integer tempNode = new Integer(0);
//        ArrayList boardingNodes = new ArrayList();
//        double inVehTime = 0.0;
//        double dwellTm = 0.0;
//        double walkTm = 0.0;
//        double wtAccTime = 0.0;
//        double wtEgrTime = 0.0;
//        double firstWait = 0.0;
//        double totalWait = 0.0;
//        double boardings = 0.0;
//        double alightings = 0.0;
//        double totalBoardings = 0.0;
//        double fare = 0.0;
//        
//        
//        count = 0;
//        if (debug)
//            logger.info ( "\n\n\nlinks in strategy with flow from origin toward destination:" );
//        for (int i=inStrategyCount; i >= 0; i--) {
//            
//            k = orderInStrategy[i];
//            m = hwyLink[k];
//            
//            if (nodeFlow[ia[k]] == 0.0)
//                continue;
//
//            if ( debug )
//                logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + trRoute[k] + ", ag.ia=" + ia[k] + ", ag.ib="  + ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", linkType=" + linkType[k] + ", ag.walkTime=" + walkTime[k] + ", invTime=" + invTime[k] + ", ag.waitTime=" + waitTime[k] + ", flow[k]=" + flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ia[k]] + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ib[k]] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ia[k]] );
//            
//            
//            if ( linkType[k] == AuxTrNet.BOARDING_TYPE ) {
//                
//                tempNode = new Integer(ia[k]);
//
//                // since we loaded 1 trip for computing skims, the fraction of the trip loading transit lines at a node
//                // is also a weight used to computed average waiting time at the node.
//                if ( firstWait == 0.0 ) {
//                    boardings = sumBoardingFlow (ia[k]);
//                    totalBoardings = boardings;
//                    boardingNodes.add(tempNode);
//                    firstWait = boardings*AuxTrNet.ALPHA/nodeFreq[ia[k]];
//                    totalWait = firstWait;
//                    fare = boardings*AuxTrNet.FARE;
//                }
//                else {
//                    if ( !boardingNodes.contains(tempNode) ) {
//                        boardings = sumBoardingFlow (ia[k]);
//                        totalBoardings += boardings;
//                        totalWait += boardings*AuxTrNet.ALPHA/nodeFreq[ia[k]];
//                        fare += boardings*AuxTrNet.TRANSFER_FARE;
//                        boardingNodes.add(tempNode);
//                    }
//                }
//                
//            }
//            else if ( linkType[k] == AuxTrNet.IN_VEHICLE_TYPE ) {
//                
//                inVehTime += invTime[k]*flow[k];
//                dwellTm += dwellTime[k]*flow[k];
//                
//            }
//            else if ( linkType[k] == AuxTrNet.AUXILIARY_TYPE ) {
//
//                // accumulate access walk time and total walk time
//                if ( firstWait == 0.0 )
//                    wtAccTime += walkTime[k]*flow[k];
//                else
//                    walkTm += walkTime[k]*flow[k];
//                
//            }
//                
//            count++;
//
//        }
//
//        // linkFreqs were weighted by WAIT_COEFF, so unweight them to get actual first and total wait values
//        firstWait /= AuxTrNet.WAIT_COEFF;
//        totalWait /= AuxTrNet.WAIT_COEFF;
//
//
//        
//
//        // loop through links in optimal strategy starting at the startToNode to accumulate egress time
//        count = 0;
//        double totalEgressFlow = 0.0;
//        if (debug)
//            logger.info ( "\n\n\nlinks in strategy with flow from destination toward origin:" );
//        for (int i=0; i < inStrategyCount; i++) {
//            
//            k = orderInStrategy[i];
//            m = hwyLink[k];
//            
//            if (nodeFlow[ia[k]] == 0.0)
//                continue;
//
//            if ( debug )
//                logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + trRoute[k] + ", ag.ia=" + ia[k] + ", ag.ib="  + ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", linkType=" + linkType[k] + ", ag.walkTime=" + walkTime[k] + ", invTime=" + invTime[k] + ", ag.waitTime=" + waitTime[k] + ", flow[k]=" + flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ia[k]] + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ib[k]] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ia[k]] );
//            
//            
//            if ( linkType[k] == AuxTrNet.AUXILIARY_TYPE ) {
//
//                // get total flow alighting to this node
//                alightings = sumAlightingFlow(ia[k]);
//                totalEgressFlow += alightings;
//                
//                // accumulate access walk time and total walk time
//                wtEgrTime += alightings*walkTime[k];
//                
//                
//                //break out of loop when all flow to destination is accounted for.
//                if ( 1.0 - totalEgressFlow < COMPARE_EPSILON )
//                    break;
//                
//            }
//                
//            count++;
//
//        }
//
//        walkTm -= wtEgrTime;
//        
//        
//        if ( debug ) {
//            logger.info ( "\n\n\ntransit skims from " + startFromNode + " to " + startToNode + ":" );
//            logger.info ( "in-vehicle time  = " + inVehTime );
//            logger.info ( "dwell time       = " + dwellTm );
//            logger.info ( "firstWait time   = " + firstWait );
//            logger.info ( "totalWait time   = " + totalWait );
//            logger.info ( "wt access time   = " + wtAccTime );
//            logger.info ( "wt egress time   = " + wtEgrTime );
//            logger.info ( "other walk time  = " + walkTm );
//            logger.info ( "total boardings  = " + totalBoardings );
//        }
//            
//        
//        results[0] = inVehTime;
//        results[1] = firstWait;
//        results[2] = totalWait + dwellTm;
//        results[3] = wtAccTime;
//        results[4] = totalBoardings;
//        results[5] = fare;
//
//        
//        return results;
//        
//    }



    public double[][] getOptimalStrategySkimsDest () {

        int k, m;
        int count;
        
        boolean debug = classDebug;
        

        // create temp arrays to hold rail ivt, bus dist, and tran ivt - to be used by fare calculation methods for fare tables
        double[] railDist = new double[auxNodeCount];
        double[] busDist = new double[auxNodeCount];
        double[] tranIvt = new double[auxNodeCount];
        
        
        double[][] nodeSkims = new double[NUM_SKIMS][auxNodeCount];
        double[][] skimResults = new double[NUM_SKIMS][nh.getNumCentroids()];
        for (k=0; k < NUM_SKIMS; k++) {
            Arrays.fill (nodeSkims[k], AuxTrNet.UNCONNECTED);
            Arrays.fill (skimResults[k], AuxTrNet.UNCONNECTED);
        }

        
        
        // check links entering dest.  If none are access links in the strategy, then dest is not transit accessible.
        boolean walkAccessAtDestination = false;
        int start = ipb[dest];
        if (start >= 0) {

            int j = dest + 1;
            while (ipb[j] == -1)
                j++;
            int end = ipb[j];
            
            for (int i=start; i < end; i++) {
                k = indexb[i];
                if ( linkType[k] == AuxTrNet.AUXILIARY_TYPE && inStrategy[k] ) {
                    walkAccessAtDestination = true;
                    break;
                }
            }
        }

        if ( !walkAccessAtDestination )
            return skimResults;
        
        
        
            
        // loop through links in optimal strategy in ascending order and accumulate skim component values at nodes weighted by link flows
        count = 0;
        for (int i=0; i < inStrategyCount; i++) {
            
            k = orderInStrategy[i];
            m = hwyLink[k];
            
            if ( debug )
                logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + trRoute[k] + ", ag.ia=" + ia[k] + ", ag.ib="  + ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", linkType=" + linkType[k] + ", ag.walkTime=" + walkTime[k] + ", invTime=" + invTime[k] + ", ag.waitTime=" + waitTime[k] + ", flow[k]=" + flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ia[k]] + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ib[k]] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ia[k]] + ", ag.freq[k]=" + freq[k] + ", nodeFlow[ag.ia[k]]=" + nodeFlow[ia[k]] + ", nodeFlow[ag.ib[k]]=" + nodeFlow[ib[k]] );
            
        
        
            if ( linkType[k] == AuxTrNet.BOARDING_TYPE ) {

                if ( nodeSkims[FWT][ib[k]] == 0.0 ) {
                    nodeSkims[FWT][ia[k]] = freq[k]*(AuxTrNet.ALPHA/nodeFreq[ia[k]])/nodeFreq[ia[k]];
                    nodeSkims[TWT][ia[k]] = freq[k]*(AuxTrNet.ALPHA/nodeFreq[ia[k]])/nodeFreq[ia[k]];
                    
                    // if link mode is air or hsr, accumulate fare.
                    if ( rteMode[k] == 'n' )
                        nodeSkims[AIR$][ia[k]] = cost[k];
                    else if ( rteMode[k] == 't' )
                        nodeSkims[HSR$][ia[k]] = cost[k];
                    
                }
                else {
                    nodeSkims[FWT][ia[k]] = nodeSkims[FWT][ib[k]] + freq[k]*AuxTrNet.ALPHA/nodeFreq[ia[k]];
                    nodeSkims[TWT][ia[k]] = nodeSkims[TWT][ib[k]] + freq[k]*AuxTrNet.ALPHA/nodeFreq[ia[k]];
                }
                nodeSkims[IVT][ia[k]] = nodeSkims[IVT][ib[k]];
                nodeSkims[ACC][ia[k]] = nodeSkims[ACC][ib[k]];
                nodeSkims[AUX][ia[k]] = nodeSkims[AUX][ib[k]];
                nodeSkims[EGR][ia[k]] = nodeSkims[EGR][ib[k]];
                
                railDist[ia[k]] = railDist[ib[k]];
                busDist[ia[k]] = busDist[ib[k]];
                tranIvt[ia[k]] = tranIvt[ib[k]];
            }
            else if ( linkType[k] == AuxTrNet.IN_VEHICLE_TYPE ) {
                
                nodeSkims[IVT][ia[k]] = nodeSkims[IVT][ib[k]] + invTime[k];
                nodeSkims[FWT][ia[k]] = nodeSkims[FWT][ib[k]];
                nodeSkims[TWT][ia[k]] = nodeSkims[TWT][ib[k]] + dwellTime[k];
                nodeSkims[ACC][ia[k]] = nodeSkims[ACC][ib[k]];
                nodeSkims[AUX][ia[k]] = nodeSkims[AUX][ib[k]];
                nodeSkims[EGR][ia[k]] = nodeSkims[EGR][ib[k]];
                
                // if link mode is intercity rail, accumulate dist.
                if ( rteMode[k] == 'm' ) {
                    railDist[ia[k]] = railDist[ib[k]] + gDist[m];
                    busDist[ia[k]] = busDist[ib[k]];
                    tranIvt[ia[k]] = tranIvt[ib[k]];
                }
                // if link mode is intercity bus, accumulate dist.
                else if ( rteMode[k] == 'i' ) {
                    railDist[ia[k]] = railDist[ib[k]];
                    busDist[ia[k]] = busDist[ib[k]] + gDist[m];
                    tranIvt[ia[k]] = tranIvt[ib[k]];
                }
                // if link mode is local bus, express bus, light rail, or commuter rail, accumulate ivt.
                else if ( rteMode[k] == 'b' || rteMode[k] == 'x' || rteMode[k] == 'l' || rteMode[k] == 'r' ) {
                    railDist[ia[k]] = railDist[ib[k]];
                    busDist[ia[k]] = busDist[ib[k]];
                    tranIvt[ia[k]] = tranIvt[ib[k]] + invTime[k];
                }
                    
            }
            else if ( linkType[k] == AuxTrNet.LAYOVER_TYPE ) {
                
                nodeSkims[IVT][ia[k]] = nodeSkims[IVT][ib[k]];
                nodeSkims[FWT][ia[k]] = nodeSkims[FWT][ib[k]];
                nodeSkims[TWT][ia[k]] = nodeSkims[TWT][ib[k]] + layoverTime[k];
                nodeSkims[ACC][ia[k]] = nodeSkims[ACC][ib[k]];
                nodeSkims[AUX][ia[k]] = nodeSkims[AUX][ib[k]];
                nodeSkims[EGR][ia[k]] = nodeSkims[EGR][ib[k]];
                    
                railDist[ia[k]] = railDist[ib[k]];
                busDist[ia[k]] = busDist[ib[k]];
                tranIvt[ia[k]] = tranIvt[ib[k]];
            }
            else if ( linkType[k] == AuxTrNet.ALIGHTING_TYPE ) {
                
                nodeSkims[IVT][ia[k]] = nodeSkims[IVT][ib[k]];
                nodeSkims[FWT][ia[k]] = nodeSkims[FWT][ib[k]];
                nodeSkims[TWT][ia[k]] = nodeSkims[TWT][ib[k]];
                nodeSkims[ACC][ia[k]] = nodeSkims[ACC][ib[k]];
                nodeSkims[AUX][ia[k]] = nodeSkims[AUX][ib[k]];
                nodeSkims[EGR][ia[k]] = nodeSkims[EGR][ib[k]];
                
                railDist[ia[k]] = railDist[ib[k]];
                busDist[ia[k]] = busDist[ib[k]];
                tranIvt[ia[k]] = tranIvt[ib[k]];
            }
            else if ( linkType[k] == AuxTrNet.AUXILIARY_TYPE ) {

                // if bnode is dest, initialize anode's egress walk time to that walk egress time and other skims to zero.
                if ( ib[k] < nh.getNumCentroids() ) {
                    nodeSkims[IVT][ia[k]] = 0.0;
                    nodeSkims[FWT][ia[k]] = 0.0;
                    nodeSkims[TWT][ia[k]] = 0.0;
                    nodeSkims[ACC][ia[k]] = 0.0;
                    nodeSkims[AUX][ia[k]] = 0.0;
                    nodeSkims[EGR][ia[k]] = walkTime[k];

                    railDist[ia[k]] = 0.0;
                    busDist[ia[k]] = 0.0;
                    tranIvt[ia[k]] = 0.0;
                }
                // anode is an origin node, set final skim values for that origin and add the access time
                else if ( ia[k] < nh.getNumCentroids() ) {
                    skimResults[IVT][ia[k]] = nodeSkims[IVT][ib[k]];
                    skimResults[FWT][ia[k]] = nodeSkims[FWT][ib[k]];
                    skimResults[TWT][ia[k]] = nodeSkims[TWT][ib[k]];
                    skimResults[ACC][ia[k]] = nodeSkims[ACC][ib[k]] + accessTime[k];
                    skimResults[AUX][ia[k]] = nodeSkims[AUX][ib[k]];
                    skimResults[EGR][ia[k]] = nodeSkims[EGR][ib[k]];

                    railDist[ia[k]] = railDist[ib[k]];
                    busDist[ia[k]] = busDist[ib[k]];
                    tranIvt[ia[k]] = tranIvt[ib[k]];
                }
                // link is a walk link, not connected to a centroid
                else {
                    nodeSkims[IVT][ia[k]] = nodeSkims[IVT][ib[k]];
                    nodeSkims[FWT][ia[k]] = nodeSkims[FWT][ib[k]];
                    nodeSkims[TWT][ia[k]] = nodeSkims[TWT][ib[k]];
                    nodeSkims[ACC][ia[k]] = nodeSkims[ACC][ib[k]];
                    nodeSkims[AUX][ia[k]] = nodeSkims[AUX][ib[k]] + walkTime[k];
                    nodeSkims[EGR][ia[k]] = nodeSkims[EGR][ib[k]];

                    railDist[ia[k]] = railDist[ib[k]];
                    busDist[ia[k]] = busDist[ib[k]];
                    tranIvt[ia[k]] = tranIvt[ib[k]];
                }
                
            }

        }
        
        count++;


        
        
        // linkFreqs were weighted by WAIT_COEFF, so unweight them to get actual first and total wait values
        for (int i=0; i < nh.getNumCentroids(); i++) {
            skimResults[FWT][i] /= AuxTrNet.WAIT_COEFF;
            skimResults[TWT][i] /= AuxTrNet.WAIT_COEFF;
        }

        

        for (int i=0; i < nh.getNumCentroids(); i++) {
            skimResults[RAIL$][i] = getIcRailFareMatrix ( i, railDist );
            skimResults[BUS$][i] = getIcBusFareMatrix ( i, busDist );
            skimResults[TRAN$][i] = getSkimTableLookupFare ( i, tranIvt, transitFareLookupTable );
        }

        return skimResults;
        
    }



    private double getSkimTableLookupFare ( int i, double[] skimTable, HashMap tazFareLookupTable ) {
        
        double fare = 0.0;
        
        if ( tazFareLookupTable != null ) {
            
            // if skim OD pair is connected by rail service, lookup fare for OD pair
            if ( skimTable[i] > 0 ) {
                
                // get the external origin and destination centroid numbers for the skim matrix indices
                int extOrigCent = indexNode[i];
                String origFareZone = (String)fareZones.get(extOrigCent);
                int extDestCent = indexNode[dest];
                String destFareZone = (String)fareZones.get(extDestCent);
                String key = String.format("%s_%s", origFareZone, destFareZone);
                
                try {
                    fare = (Float)tazFareLookupTable.get(key);
                }
                catch (RuntimeException e) {
                    logger.error( String.format("exception caught looking up intracity fare for,"));
                    logger.error( String.format("extOrigCent=%d, origFareZone=%s, extDestCent=%d, destFareZone=%s, key=%s", extOrigCent, origFareZone, extDestCent, destFareZone, key));
                }
            
            }
            
        }
        
        return fare;

    }


    private double getIcBusFareMatrix ( int i, double[] busDist ) {
        
        double fare = 0.0;

        // if skim OD pair is connected by rail service, calculate distance based bus fare for OD pair
        if ( busDist[i] > 0 ) {
            fare = 1.9694*Math.pow(busDist[i], -0.4994); 
        }
        
        return fare;
        
    }

    
    //TODO: implement function for rail
    private double getIcRailFareMatrix ( int i, double[] railDist ) {
        
        double fare = 0.0;

        // if skim OD pair is connected by rail service, calculate distance based rail fare for OD pair
        if ( railDist[i] > 0 ) {
            fare = 1.9694*Math.pow(railDist[i], -0.4994); 
        }
        
        return fare;
        
    }

    
    public void setTransitFareTables ( HashMap intracityFareTable, HashMap fareZonesMap ) {
        transitFareLookupTable = intracityFareTable;
        fareZones = fareZonesMap;
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



    
    

    // Inner classes

    public class HeapElement implements Comparable {
        
        int index;
        int type;
        double label;
        
        public HeapElement (int index, int type, double label) {
            this.index = index;
            this.type = type;
            this.label = label;
        }
        
        
        public int compareTo(Object obj) {
            
            int returnValue = 0;
            HeapElement el = (HeapElement)obj;
            
            if ( label > el.getLabel() )
                returnValue = 1;
            else if ( label < el.getLabel() )
                returnValue = -1;
            else {
                if ( type > el.getType() )
                    returnValue = 1;
                else if ( type < el.getType() )
                    returnValue = -1;
                else {
                    if ( index > el.getIndex() )
                        returnValue = 1;
                    else if ( index < el.getIndex() )
                        returnValue = -1;
                }
            }
            
            return returnValue;
        }

        public int getIndex() {
            return index;
        }
        
        public int getType() {
            return type;
        }
        
        public double getLabel() {
            return label;
        }
        
    }

    
    
    public class Heap {
        
        boolean[] inHeap = null;
        SortedSet elements = null;
        
        public Heap( int numLinks ) {
            elements = new TreeSet();
            inHeap = new boolean[numLinks];
        }

        public void clear() {
            elements.clear();
            Arrays.fill ( inHeap, false );
        }

        public int size() {
            return elements.size();
        }

        public void add( HeapElement el ) {
            int k = el.getIndex();
            inHeap[k] = true;
            elements.add(el);
        }
        
        public boolean remove( HeapElement el ) {
            int k = el.getIndex();
            inHeap[k] = false;
            return elements.remove(el);
        }
        
        public boolean contains( int k ) {
            return inHeap[k];
        }
        
        public HeapElement getFirst() {
            HeapElement el = (HeapElement)elements.first();
            if ( el != null )
                remove(el);
            return el;
        }

        public void dataPrintSorted() {

            logger.info( "Heap contents sorted by linklabel" );
            Iterator it = elements.iterator();

            int i=0;
            while ( it.hasNext() ) {
                HeapElement h = (HeapElement)it.next();
                int k = h.getIndex();
                int m = hwyLink[k];
                logger.info ("i=" + (i++) + ",k=" + k + ", ag.ia[k]=" + ia[k] + "(g.an=" + (m>=0 ? indexNode[gia[m]] : -1) + "), ag.ib[k]=" + ib[k] + "(g.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + "), linkType=" + linkType[k] + ", Route=" + trRoute[k] + ", linkLabel[k]=" + String.format("%10.6f", linkLabel[k]) );
            }
        }

    }

}	
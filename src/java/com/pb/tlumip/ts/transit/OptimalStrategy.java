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
    protected static Logger debugLogger = Logger.getLogger("debugLogger");
    //protected static Logger debugLogger = logger;

	int IVT = TransitAssignAndSkimManager.SkimType.IVT.ordinal();      // in-vehicle time
	int FWT = TransitAssignAndSkimManager.SkimType.FWT.ordinal();      // first wait
	int TWT = TransitAssignAndSkimManager.SkimType.TWT.ordinal();      // total wait
    int ACC = TransitAssignAndSkimManager.SkimType.ACC.ordinal();      // access time
    int EGR = TransitAssignAndSkimManager.SkimType.EGR.ordinal();      // egress time
    int AUX = TransitAssignAndSkimManager.SkimType.AUX.ordinal();      // transfer time
    int BRD = TransitAssignAndSkimManager.SkimType.BRD.ordinal();      // boardings
    int BRD_HSR = TransitAssignAndSkimManager.SkimType.BRD_HSR.ordinal();      // HSR routes available for OD
    int HSR$ = TransitAssignAndSkimManager.SkimType.HSR$.ordinal();    // hsr fare
    int HSR_IVT = TransitAssignAndSkimManager.SkimType.HSR_IVT.ordinal();    // hsr rail-only ivt
    int AIR$ = TransitAssignAndSkimManager.SkimType.AIR$.ordinal();    // air fare
    int RAIL$ = TransitAssignAndSkimManager.SkimType.RAIL$.ordinal();  // intercity rail fare
    int RAIL_IVT = TransitAssignAndSkimManager.SkimType.RAIL_IVT.ordinal();  // intercity rail rail-only ivt
    int BUS$ = TransitAssignAndSkimManager.SkimType.BUS$.ordinal();    // intercity bus fare
    int BUS_IVT = TransitAssignAndSkimManager.SkimType.BUS_IVT.ordinal();    // intercity bus bus-only ivt
    int TRAN$ = TransitAssignAndSkimManager.SkimType.TRAN$.ordinal();  // intracity transit fare
	public static final int NUM_SKIMS = 16;

	static final double COMPARE_EPSILON = 1.0e-07;

	static final double MIN_ALLOCATED_FLOW = 0.00001;
	
	static final int MAX_BOARDING_LINKS = 100;
    
    static final float MAX_TRANSFER_WALK_TIME = 20;
    static final float MAX_FIRST_WAIT_TIME = 60;
    static final float MAX_TOTAL_WAIT_TIME = 90;


    
    HashMap fareZones;
    HashMap transitFareLookupTable;
    

    NetworkHandlerIF nh;
    String identifier;
    
	int dest;
    
    int auxNodeCount;
    int auxLinkCount;
	
    int numCentroids;
    
	Heap candidateHeap;
	int[] heapContents;

	double[] nodeLabel, trfWalkLabel, nodeFreq, linkLabel;
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
    double[] drAccTime = null;
    double[] waitTime = null;
    double[] dwellTime = null;
    double[] layoverTime = null;
    double[] invTime = null;
    double[] freq = null;
    double[] flow = null;
    double[] rteHeadway = null;
    int[] linkType = null;
    char[] rteMode = null;
    String[] rteNames = null;
    
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
    //boolean classDebug = true;
	
	
	
	public OptimalStrategy ( NetworkHandlerIF nh, String identifier ) {
        
        this.nh = nh;
        this.identifier = identifier;
        

        numCentroids = nh.getNumCentroids();
        
        auxNodeCount = nh.getAuxNodeCount( identifier );
        auxLinkCount = nh.getAuxLinkCount( identifier );
        
        
		nodeFlow = new double[auxNodeCount+1];
        nodeLabel = new double[auxNodeCount+1];
        trfWalkLabel = new double[auxNodeCount+1];
		nodeFreq = new double[auxNodeCount+1];
		linkLabel = new double[auxLinkCount+1];
		inStrategy = new boolean[auxLinkCount+1];
		orderInStrategy = new int[auxLinkCount+1];
		strategyOrderForLink = new int[auxLinkCount+1];
		
		//Create a new heap structure to sort candidate node labels
        //candidateHeap = new Heap(auxNodeCount+1);  // old Heap 
        candidateHeap = new Heap( auxLinkCount ); // new SortedSet
		heapContents = new int[auxNodeCount+1];

        ia = nh.getAuxIa(identifier);
        ib = nh.getAuxIb(identifier);
        ipa = nh.getAuxIpa(identifier);
        ipb = nh.getAuxIpb(identifier);
        indexa = nh.getAuxIndexa(identifier);
        indexb = nh.getAuxIndexb(identifier);
        hwyLink = nh.getAuxHwyLink(identifier);
        trRoute = nh.getLinkTrRoute(identifier);
        rteMode = nh.getRteMode(identifier);
        rteNames = nh.getTransitRouteNames(identifier);
        linkType = nh.getAuxLinkType(identifier);
        cost = nh.getAuxCost(identifier);
        dwellTime = nh.getAuxDwellTime(identifier);
        layoverTime = nh.getAuxLayoverTime(identifier);
        waitTime = nh.getAuxWaitTime(identifier); 
        walkTime = nh.getAuxWalkTime(identifier); 
        drAccTime = nh.getAuxDriveAccTime(identifier); 
        invTime = nh.getAuxInvTime(identifier);
        freq = nh.getAuxLinkFreq(identifier);
        flow = nh.getAuxLinkFlow(identifier);
        rteHeadway = nh.getAuxRouteHeadway(identifier);
        
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
        Arrays.fill(trfWalkLabel, 0.0);
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
	public int buildStrategy ( int dest, String accessMode ) {
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
		if (accessMode.equalsIgnoreCase("walk")) {
			accessTime = walkTime; 
		}
		else {
			accessTime = drAccTime; 
		}


		if (debug)
		    debugLogger.info ("building optimal strategy to " + dest + "(" + indexNode[dest] + ")");
		
		
		ArrayList<Integer> linksInStrategyList = new ArrayList<Integer>();
		
        
        //while ((k = candidateHeap.remove()) != -1) {  //old Heap
        while ( candidateHeap.size() > 0 ) {

            HeapElement he = candidateHeap.getFirst();
            k = he.getIndex();
            
            
            int dummy=0;
            if ( ia[k] < indexNode.length && indexNode[ia[k]] == 24963 ){
                dummy = 1;
            }
            
            
            if (ia[k] != dest && !inStrategy[k]) {

    			// do not include links into centroids in strategy unless its going into dest.
    			if ( ib[k] < numCentroids && ib[k] != dest ) {
    				inStrategy[k] = false;
    				continue;
    			}
    			
    			
				linkImped = nh.getAuxLinkImped(identifier, k);
				
				// log some information about the starting condition of the candidate link being examined
				if ( debug ) {
					debugLogger.info ("");
					
                    // get the highway network link index for the given transit network link index
                    m = hwyLink[k];
                

					debugLogger.info ("k=" + k + ", ag.ia[k]=" + ia[k] + "(g.an=" + (m>=0 ? indexNode[gia[m]] : -1) + "), ag.ib[k]=" + ib[k] + "(g.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + "), linkType=" + linkType[k] + ", trRoute=" + trRoute[k] + "(" + (trRoute[k] >= 0 ? nh.getAuxRouteName(identifier, trRoute[k]) : "aux") + ")" );
					debugLogger.info ("nodeLabel[ag.ia=" + ia[k] + "]=" + nodeLabel[ia[k]]);
					debugLogger.info ("nodeLabel[ag.ib=" + ib[k] + "]=" + nodeLabel[ib[k]]);
					debugLogger.info ("nodeFreq[ag.ia=" + ia[k] + "]=" + nodeFreq[ia[k]]);
					debugLogger.info ("nodeFreq[ag.ib=" + ib[k] + "]=" + nodeFreq[ib[k]]);
					debugLogger.info ("ag.freq[k=" + k + "]=" + freq[k]);
					debugLogger.info ("linkImped(k=" + k + ")=" + linkImped);
					
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
                        linksInStrategyList.add(k);
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
                        linksInStrategyList.add(k);
						updateEnteringLabels (ia[k]);
							
					}
					// non-boarding link - either in-vehicle or auxilliary
					else {

                        nodeLabel[ia[k]] = nodeLabel[ib[k]] + linkImped;
                        
                        // do not add link to strategy if accumulated walk time is too large
                        if ( linkType[k] == AuxTrNet.AUXILIARY_TYPE ) 
                            trfWalkLabel[ia[k]] = trfWalkLabel[ib[k]] + walkTime[k];
                        else
                            trfWalkLabel[ia[k]] = trfWalkLabel[ib[k]];

                        
                        if ( trfWalkLabel[ia[k]] > MAX_TRANSFER_WALK_TIME ) {
                            inStrategy[k] = false;
                        }
                        else {
    						inStrategy[k] = true;
    						strategyOrderForLink[k] = inStrategyCount;
    						orderInStrategy[inStrategyCount++] = k;
                            linksInStrategyList.add(k);
    						updateEnteringLabels(ia[k]);
                        }
                        
					}
				}
				else {
					
					if (debug) debugLogger.info ("link not included in strategy");
					inStrategy[k] = false;
					
				}


				// log some information about the ending condition of the candidate link being examined
                if ( debug && inStrategy[k] ) {
					
                    // get the highway network link index for the given transit network link index
                    m = hwyLink[k];
                

					debugLogger.info ("");
					debugLogger.info ("k=" + k + ", linkType=" + linkType[k] + ", trRoute=" + trRoute[k]);
					debugLogger.info ("ag.ia[k]=" + ia[k] + "(g.an=" + (m >= 0 ? indexNode[gia[m]] : -1) + "), ag.ib[k]=" + ib[k] + "(g.bn=" + ( m>=0 ? indexNode[gib[m]] : -1) + ")");
					debugLogger.info ("nodeLabel[ag.ia=" + ia[k] + "]=" + nodeLabel[ia[k]]);
					debugLogger.info ("nodeLabel[ag.ib=" + ib[k] + "]=" + nodeLabel[ib[k]]);
					debugLogger.info ("nodeFreq[ag.ia=" + ia[k] + "]=" + nodeFreq[ia[k]]);
					debugLogger.info ("nodeFreq[ag.ib=" + ib[k] + "]=" + nodeFreq[ib[k]]);
					debugLogger.info ("ag.freq[k=" + k + "]=" + freq[k]);
					debugLogger.info ("inStrategy[k=" + k + "]=" + inStrategy[k]);
					
				}
				
			}

//            logger.info( "" );
//            logger.info( "Strategy Contents:" );
//            int cnt = 0;
//            for ( int el : linksInStrategyList ) {
//                int an = ia[el] < indexNode.length ? indexNode[ia[el]] : -1;
//                int bn = ib[el] < indexNode.length ? indexNode[ib[el]] : -1;
//                String name = trRoute[el] >= 0 ? rteNames[trRoute[el]] : "N/A";
//                logger.info( String.format("cnt=%d, k=%d, ia=%d, ib=%d, an=%d, bn=%d, linkType=%d, rte=%d, rteName=%s", cnt++, el, ia[el], ib[el], an, bn, linkType[el], trRoute[el], name ) );
//            }
//            logger.info( "" );
            
		} // end of while heap not empty

		return 0;

	}



    private int updateEnteringLabels (int currentNode) {
        // calculate linkLabels[] for use in ordering the contents of the heap.
        // linkLabel[k] is the cumulative utility from ia[k] to dest.

        int i, j, k, m;
        int start, end;
        //boolean debug = classDebug;
        boolean debug = true;
        double linkImped = 0.0;

        if (debug) {
            debugLogger.info ("");
            debugLogger.info ("updateEnteringLabels(): currentNode = " + currentNode);
        }

        start = ipb[currentNode];
        if (start == -1) {
            return -1;
        }


        
        if (debug)
              debugLogger.info ("start=" + start + ", indexb[start]=" + indexb[start] + ", ia=" + ia[indexb[start]] + ", ib=" + ib[indexb[start]] + ", an=" + (ia[indexb[start]] < indexNode.length ? indexNode[ia[indexb[start]]] : 0) + ", bn=" + (ib[indexb[start]] < indexNode.length ? indexNode[ib[indexb[start]]] : 0));
        j = currentNode + 1;
        while (ipb[j] == -1)
            j++;
        end = ipb[j];
        if (debug) {
            debugLogger.info ("end=" + end + ", j=" + j);
            debugLogger.info ("end=" + end + ", indexb[end]=" + (end < indexb.length ? Integer.toString(indexb[end]) : "null") + ", ia=" + (end < indexb.length ? Integer.toString(ia[indexb[end]]) : "null") + ", ib=" + (end < indexb.length ? Integer.toString(ib[indexb[end]]) : "null"));
            debugLogger.info ("");
        }
        for (i=start; i < end; i++) {
            k = indexb[i];

            // if link k is a boarding link, but the in-vehicle link that follows it (link k+1) is not in the strategy,
            // don't add link k to the heap.
            if ( linkType[k] == AuxTrNet.BOARDING_TYPE && !inStrategy[k+1] )
                continue;

            
            linkImped = nh.getAuxLinkImped(identifier, k);

            // if the anode's label is already smaller than the bnode's label plus the link impedance,
            // no need to add the link to the heap. 
            if ( nodeLabel[ia[k]] < (nodeLabel[ib[k]] + linkImped) )
                continue;

            
            if (linkType[k] == AuxTrNet.BOARDING_TYPE) {
                if ( nodeFreq[ia[k]] == 0.0 ) {
                    // first transit boarding link considered from the current node
                    linkLabel[k] = (AuxTrNet.ALPHA + freq[k]*(nodeLabel[ib[k]] + linkImped))/(nodeFreq[ia[k]] + freq[k]);
                }
                else {
                    // at least one transit boarding link from the current node exists in optimal strategy
                    linkLabel[k] = (nodeFreq[ia[k]]*nodeLabel[ia[k]] + freq[k]*(nodeLabel[ib[k]] + linkImped))/(nodeFreq[ia[k]] + freq[k]);
                }
            }
            else {
                linkLabel[k] = nodeLabel[ib[k]] + linkImped;
            }
            

            
            if (debug) {
                m = hwyLink[k];
                String name = "N/A";
                if ( trRoute[k] >= 0 )
                    name = String.format("%d_%s", trRoute[k], rteNames[trRoute[k]]);
                
                debugLogger.info ("adding   " + i + ", indexb[i] or k=" + k + ", linkType=" + linkType[k] + ", route=" + name + ", ia=" + ia[k] + "(" + ( ia[k] < indexNode.length ? indexNode[ia[k]] : -1) + "), ib=" + ib[k] + "(" + (ib[k] < indexNode.length ? indexNode[ib[k]] : -1) + "), linkLabel[k]=" + String.format("%15.6f", linkLabel[k]) + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ib[k]] + ", linkImped=" + linkImped);
            }

            HeapElement he = new HeapElement(k, linkType[k], linkLabel[k]);
            
            if ( candidateHeap.contains(k))
                candidateHeap.remove(he);
            
            candidateHeap.add(he);

        }

        if ( debugLogger.isDebugEnabled() )
            candidateHeap.dataPrintSorted();
            
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
        double[] routeBoardingsToDest = new double[AuxTrNet.MAX_ROUTES];
        
        
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
                        
                        if ( ia[k] < numCentroids )
                            originsNotLoaded[ia[k]] = 0;
                    }
                }

            }
            
            
            
            if (debug) {
                debugLogger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + trRoute[k] + ", ag.ia=" + ia[k] + ", ag.ib="  + ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", linkType=" + linkType[k] + ", ag.walkTime=" + walkTime[k] + ", invTime=" + invTime[k] + ", ag.waitTime=" + waitTime[k] + ", flow[k]=" + flow[k] + ", nodeLabel[ia[k]]=" + nodeLabel[ia[k]] + ", nodeLabel[ib[k]]=" + nodeLabel[ib[k]] );
            }
            
            count++;
        
        }

        for (int origTaz=0; origTaz < tripColumn.length; origTaz++) {
            if ( originsNotLoaded[origTaz] == 1 )
                tripsNotLoaded += tripColumn[origTaz];
        }
        
        return routeBoardingsToDest;

    }


    
    public void testLoadOptimalStrategyDest () {

        // tripColumn is the column of the trip table for the destination zone for this optimal strategy 
        int k, m;
        int count;
        double linkFlow;
        
        int orig = nodeIndex[TransitAssignAndSkimManager.TEST_ORIG];
                  
        // assign 1 trip to TransitAssignAndSkimManager.TEST_DEST for testing purposes.
        nodeFlow[orig] = 1;

        
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

                    // log boarding
                    logger.info ( String.format("orig=%d, dest=%d, board=%d, rte=%d, rteName=%s, flow=%.3f", + indexNode[orig], indexNode[dest], indexNode[ia[k]], trRoute[k], rteNames[trRoute[k]], flow[k] ) );
                    //logger.info ( "count=" + count++ + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + trRoute[k] + ", ag.ia=" + ia[k] + ", ag.ib="  + ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", linkType=" + linkType[k] + ", ag.walkTime=" + walkTime[k] + ", invTime=" + invTime[k] + ", ag.waitTime=" + waitTime[k] + ", flow[k]=" + flow[k] + ", nodeLabel[ia[k]]=" + nodeLabel[ia[k]] + ", nodeLabel[ib[k]]=" + nodeLabel[ib[k]] );
                }

            }
            else {

                linkFlow = nodeFlow[ia[k]];

                if ( linkFlow > 0 ) {
                    if ( nodeLabel[ib[k]] != AuxTrNet.INFINITY ) {
                        flow[k] = linkFlow;
                        nodeFlow[ib[k]] += linkFlow;
                        nodeFlow[ia[k]] -= linkFlow;

                        if ( linkType[k] == AuxTrNet.ALIGHTING_TYPE) {
                            // log alighting
                            logger.info ( String.format("orig=%d, dest=%d, alight=%d, rte=%d, rteName=%s, flow=%.3f", + indexNode[orig], indexNode[dest], indexNode[ib[k]], trRoute[k], rteNames[trRoute[k]], flow[k] ) );
                        }
                        //logger.info ( "count=" + count++ + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + trRoute[k] + ", ag.ia=" + ia[k] + ", ag.ib="  + ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", linkType=" + linkType[k] + ", ag.walkTime=" + walkTime[k] + ", invTime=" + invTime[k] + ", ag.waitTime=" + waitTime[k] + ", flow[k]=" + flow[k] + ", nodeLabel[ia[k]]=" + nodeLabel[ia[k]] + ", nodeLabel[ib[k]]=" + nodeLabel[ib[k]] );
                    }
                }

            }
            
        
        }

    }


    
    public void getOptimalStrategyLinks ( int orig ) {

        nodeFlow[orig] = 1.0;
        
        HashMap<String,Double> routeIvt = new HashMap<String,Double>();
        
        // loop through links in optimal strategy in reverse order and allocate
        // flow at the nodes to exiting links in the optimal strategy
        int count = 0;
        double linkFlow = 0.0;
        for (int i=inStrategyCount; i >= 0; i--) {
            
            int k = orderInStrategy[i];
            int m = hwyLink[k];

            if ( linkType[k] == AuxTrNet.BOARDING_TYPE) {
                
                linkFlow = (freq[k]/nodeFreq[ia[k]])*nodeFlow[ia[k]];

                if ( linkFlow > 0 ) {
                    flow[k] = linkFlow;
                    nodeFlow[ib[k]] += linkFlow;
                }

            }
            else {

                linkFlow = nodeFlow[ia[k]];

                if ( linkFlow > 0 ) {
                    if ( nodeLabel[ib[k]] != AuxTrNet.INFINITY ) {
                        flow[k] = linkFlow;
                        nodeFlow[ib[k]] += linkFlow;
                        nodeFlow[ia[k]] -= linkFlow;
                    }
                }

            }
            
            
            if ( linkFlow > 0 ) {
                double ivt = 0;
                String name = "N/A";
                if ( trRoute[k] >= 0 ) {
                    name = String.format("%d_%s", trRoute[k], rteNames[trRoute[k]]);
                    if ( routeIvt.containsKey(name) )
                        ivt = routeIvt.get(name);
                    ivt += linkFlow * invTime[k];
                    routeIvt.put(name, ivt);
                }
                logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", name=" + name + ", mode=" + rteMode[k] + ", trRoute=" + trRoute[k] + ", ag.ia=" + ia[k] + ", ag.ib="  + ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", linkType=" + linkType[k] + ", ag.walkTime=" + walkTime[k] + ", ag.drAccTime=" + drAccTime[k] + ", invTime=" + invTime[k] + ", ag.waitTime=" + waitTime[k] + ", flow[k]=" + flow[k] + ", nodeLabel[ia[k]]=" + nodeLabel[ia[k]] + ", nodeLabel[ib[k]]=" + nodeLabel[ib[k]] );
                count++;
            }
        
        }

        for ( String name : routeIvt.keySet() ){
            logger.info( String.format("Route %s has IVT = %.2f.", name, routeIvt.get(name)) );
        }
    }


    public double getTripsNotLoaded () {
        return tripsNotLoaded;
    }
    



    public double[][] getOptimalStrategySkimsDest () {

        int k, m;
        int count;
        
        boolean debug = classDebug;
        

        // create temp arrays to hold rail ivt, bus dist, and tran ivt - to be used by fare calculation methods for fare tables
        double[] railDist = new double[auxNodeCount];
        double[] busDist = new double[auxNodeCount];
        double[] tranIvt = new double[auxNodeCount];
        
        
        double[][] nodeSkims = new double[NUM_SKIMS][auxNodeCount];
        double[][] skimResults = new double[NUM_SKIMS][numCentroids];
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
                debugLogger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + trRoute[k] + ", ag.ia=" + ia[k] + ", ag.ib="  + ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", linkType=" + linkType[k] + ", ag.walkTime=" + walkTime[k] + ", invTime=" + invTime[k] + ", ag.waitTime=" + waitTime[k] + ", flow[k]=" + flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ia[k]] + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ib[k]] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ia[k]] + ", ag.freq[k]=" + freq[k] + ", nodeFlow[ag.ia[k]]=" + nodeFlow[ia[k]] + ", nodeFlow[ag.ib[k]]=" + nodeFlow[ib[k]] + ", nodeSkims[IVT][ia[k]]=" + nodeSkims[IVT][ia[k]] + ", nodeSkims[IVT][ib[k]]=" + nodeSkims[IVT][ib[k]] );
            
        
        
            if ( linkType[k] == AuxTrNet.BOARDING_TYPE ) {

                if ( nodeSkims[FWT][ib[k]] == 0.0 ) {
                    
                    nodeSkims[FWT][ia[k]] = freq[k]*(AuxTrNet.ALPHA/nodeFreq[ia[k]])/nodeFreq[ia[k]];
                    nodeSkims[TWT][ia[k]] = freq[k]*(AuxTrNet.ALPHA/nodeFreq[ia[k]])/nodeFreq[ia[k]];
                    nodeSkims[BRD][ia[k]] = freq[k]/nodeFreq[ia[k]];
                    
                    // if link mode is air or hsr, accumulate fare.
                    if ( rteMode[k] == 'n' )
                        nodeSkims[AIR$][ia[k]] = cost[k];
                    else if ( rteMode[k] == 't' )
                        nodeSkims[HSR$][ia[k]] = cost[k];
                    else if ( rteMode[k] == 'm' ) {
                        nodeSkims[BRD_HSR][ia[k]] ++;
                    }
                    
                }
                else {
                    
                    nodeSkims[FWT][ia[k]] = nodeSkims[FWT][ib[k]] + freq[k]*AuxTrNet.ALPHA/nodeFreq[ia[k]];
                    nodeSkims[TWT][ia[k]] = nodeSkims[TWT][ib[k]] + freq[k]*AuxTrNet.ALPHA/nodeFreq[ia[k]];
                    nodeSkims[BRD][ia[k]] = nodeSkims[BRD][ib[k]] + freq[k]/nodeFreq[ia[k]];

                    if ( rteMode[k] == 'm' )
                        nodeSkims[BRD_HSR][ia[k]] ++;

                }
                
                
                int dummy = 0;
                if ( nodeSkims[BRD_HSR][ia[k]] > 1 ) {
                    dummy = 1;
                }
                
                
                /*
                // loop over boarding links exiting ia[k] and sum 1/headway.
                double totalInverseHeadway = 0.0;
                start = ipa[ia[k]];
                if (start >= 0) {

                    int j = ia[k] + 1;
                    while (ipa[j] == -1)
                        j++;
                    int end = ipa[j];
                    
                    for (j=start; j < end; j++) {
                        int a = indexa[j];
                        if ( linkType[a] == AuxTrNet.BOARDING_TYPE ) {
                            totalInverseHeadway += 1.0/rteHeadway[a];
                        }
                    }
                }

                // calculate the loading proportion based on relative headway
                double loadingProportion = rteHeadway[k]/totalInverseHeadway;
                */
                
                
                nodeSkims[IVT][ia[k]] = nodeSkims[IVT][ib[k]];
                nodeSkims[ACC][ia[k]] = nodeSkims[ACC][ib[k]];
                nodeSkims[AUX][ia[k]] = nodeSkims[AUX][ib[k]];
                nodeSkims[EGR][ia[k]] = nodeSkims[EGR][ib[k]];

                nodeSkims[HSR_IVT][ia[k]] = nodeSkims[HSR_IVT][ib[k]];
                nodeSkims[RAIL_IVT][ia[k]] = nodeSkims[RAIL_IVT][ib[k]];
                nodeSkims[BUS_IVT][ia[k]] = nodeSkims[BUS_IVT][ib[k]];
                
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
                nodeSkims[BRD][ia[k]] = nodeSkims[BRD][ib[k]];
                nodeSkims[BRD_HSR][ia[k]] = nodeSkims[BRD_HSR][ib[k]];
                
                // if link mode is intercity rail, accumulate dist and ivt.
                if ( rteMode[k] == 'm' ) {
                    railDist[ia[k]] = railDist[ib[k]] + gDist[m];
                    busDist[ia[k]] = busDist[ib[k]];
                    tranIvt[ia[k]] = tranIvt[ib[k]];

                    nodeSkims[HSR_IVT][ia[k]] = nodeSkims[HSR_IVT][ib[k]] + invTime[k];
                }
                // if link mode is intercity bus, accumulate dist.
                else if ( rteMode[k] == 'i' ) {
                    railDist[ia[k]] = railDist[ib[k]];
                    busDist[ia[k]] = busDist[ib[k]] + gDist[m];
                    tranIvt[ia[k]] = tranIvt[ib[k]];
                    
                    nodeSkims[BUS_IVT][ia[k]] = nodeSkims[BUS_IVT][ib[k]] + invTime[k];
                }
                // if link mode is local bus, express bus, light rail, or commuter rail, accumulate ivt.
                else if ( rteMode[k] == 'b' || rteMode[k] == 'x' || rteMode[k] == 'l' || rteMode[k] == 'r' ) {
                    railDist[ia[k]] = railDist[ib[k]];
                    busDist[ia[k]] = busDist[ib[k]];
                    tranIvt[ia[k]] = tranIvt[ib[k]] + invTime[k];

                    if ( rteMode[k] == 'r' )
                        nodeSkims[RAIL_IVT][ia[k]] = nodeSkims[RAIL_IVT][ib[k]] + invTime[k];
                }
                    
            }
            else if ( linkType[k] == AuxTrNet.LAYOVER_TYPE ) {
                
                nodeSkims[IVT][ia[k]] = nodeSkims[IVT][ib[k]];
                nodeSkims[FWT][ia[k]] = nodeSkims[FWT][ib[k]];
                nodeSkims[TWT][ia[k]] = nodeSkims[TWT][ib[k]] + layoverTime[k];
                nodeSkims[ACC][ia[k]] = nodeSkims[ACC][ib[k]];
                nodeSkims[AUX][ia[k]] = nodeSkims[AUX][ib[k]];
                nodeSkims[EGR][ia[k]] = nodeSkims[EGR][ib[k]];
                nodeSkims[BRD][ia[k]] = nodeSkims[BRD][ib[k]];
                nodeSkims[BRD_HSR][ia[k]] = nodeSkims[BRD_HSR][ib[k]];
                    
                nodeSkims[HSR_IVT][ia[k]] = nodeSkims[HSR_IVT][ib[k]];
                nodeSkims[RAIL_IVT][ia[k]] = nodeSkims[RAIL_IVT][ib[k]];
                nodeSkims[BUS_IVT][ia[k]] = nodeSkims[BUS_IVT][ib[k]];

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
                nodeSkims[BRD][ia[k]] = nodeSkims[BRD][ib[k]];
                nodeSkims[BRD_HSR][ia[k]] = nodeSkims[BRD_HSR][ib[k]];
                
                nodeSkims[HSR_IVT][ia[k]] = nodeSkims[HSR_IVT][ib[k]];
                nodeSkims[RAIL_IVT][ia[k]] = nodeSkims[RAIL_IVT][ib[k]];
                nodeSkims[BUS_IVT][ia[k]] = nodeSkims[BUS_IVT][ib[k]];

                railDist[ia[k]] = railDist[ib[k]];
                busDist[ia[k]] = busDist[ib[k]];
                tranIvt[ia[k]] = tranIvt[ib[k]];
            }
            else if ( linkType[k] == AuxTrNet.AUXILIARY_TYPE ) {

                // if bnode is dest, initialize anode's egress walk time to that walk egress time and other skims to zero.
                if ( ib[k] < numCentroids ) {
                    nodeSkims[IVT][ia[k]] = 0.0;
                    nodeSkims[FWT][ia[k]] = 0.0;
                    nodeSkims[TWT][ia[k]] = 0.0;
                    nodeSkims[ACC][ia[k]] = 0.0;
                    nodeSkims[AUX][ia[k]] = 0.0;
                    nodeSkims[BRD][ia[k]] = 0.0;
                    nodeSkims[BRD_HSR][ia[k]] = 0.0;
                    nodeSkims[EGR][ia[k]] = walkTime[k];

                    nodeSkims[HSR_IVT][ia[k]] = 0.0;
                    nodeSkims[RAIL_IVT][ia[k]] = 0.0;
                    nodeSkims[BUS_IVT][ia[k]] = 0.0;

                    railDist[ia[k]] = 0.0;
                    busDist[ia[k]] = 0.0;
                    tranIvt[ia[k]] = 0.0;
                }
                // anode is an origin node, set final skim values for that origin and add the access time
                else if ( ia[k] < numCentroids ) {
                    skimResults[IVT][ia[k]] = nodeSkims[IVT][ib[k]];
                    skimResults[FWT][ia[k]] = nodeSkims[FWT][ib[k]];
                    skimResults[TWT][ia[k]] = nodeSkims[TWT][ib[k]];
                    skimResults[ACC][ia[k]] = nodeSkims[ACC][ib[k]] + accessTime[k];
                    skimResults[AUX][ia[k]] = nodeSkims[AUX][ib[k]];
                    skimResults[EGR][ia[k]] = nodeSkims[EGR][ib[k]];
                    skimResults[BRD][ia[k]] = nodeSkims[BRD][ib[k]];
                    skimResults[BRD_HSR][ia[k]] = nodeSkims[BRD_HSR][ib[k]];

                    skimResults[HSR_IVT][ia[k]] = nodeSkims[HSR_IVT][ib[k]];
                    skimResults[RAIL_IVT][ia[k]] = nodeSkims[RAIL_IVT][ib[k]];
                    skimResults[BUS_IVT][ia[k]] = nodeSkims[BUS_IVT][ib[k]];

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
                    nodeSkims[BRD][ia[k]] = nodeSkims[BRD][ib[k]];
                    nodeSkims[BRD_HSR][ia[k]] = nodeSkims[BRD_HSR][ib[k]];

                    nodeSkims[HSR_IVT][ia[k]] = nodeSkims[HSR_IVT][ib[k]];
                    nodeSkims[RAIL_IVT][ia[k]] = nodeSkims[RAIL_IVT][ib[k]];
                    nodeSkims[BUS_IVT][ia[k]] = nodeSkims[BUS_IVT][ib[k]];

                    railDist[ia[k]] = railDist[ib[k]];
                    busDist[ia[k]] = busDist[ib[k]];
                    tranIvt[ia[k]] = tranIvt[ib[k]];
                }
                
            }

            count++;

        }
        

        
        
        // linkFreqs were weighted by WAIT_COEFF, so unweight them to get actual first and total wait values
        for (int i=0; i < numCentroids; i++) {
            skimResults[FWT][i] /= AuxTrNet.WAIT_COEFF;
            skimResults[TWT][i] /= AuxTrNet.WAIT_COEFF;
            
            if ( skimResults[FWT][i] > MAX_FIRST_WAIT_TIME )
                skimResults[FWT][i] = MAX_FIRST_WAIT_TIME;
        
            if ( skimResults[TWT][i] > MAX_TOTAL_WAIT_TIME )
                skimResults[TWT][i] = MAX_TOTAL_WAIT_TIME;

        }

        

        for (int i=0; i < numCentroids; i++) {
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
    
                int extOrigCent = 0;
                int extDestCent = 0;
                String origFareZone = "";
                String destFareZone = "";
                String key = "";
                try {
                    // get the external origin and destination centroid numbers for the skim matrix indices
                    extOrigCent = indexNode[i];
                    origFareZone = (String)fareZones.get(extOrigCent);
                    extDestCent = indexNode[dest];
                    destFareZone = (String)fareZones.get(extDestCent);
                    key = String.format("%s_%s", origFareZone, destFareZone);
                }
                catch (RuntimeException e) {
                    logger.error( String.format("exception caught constructing fare lookup table key,"));
                    logger.error( String.format("i=%d, extOrigCent=%d, origFareZone=%s, extDestCent=%d, destFareZone=%s, key=%s", i, extOrigCent, origFareZone, extDestCent, destFareZone, key) );
                    if ( fareZones == null )
                        logger.error( "fareZones object is null" );
                    else
                        logger.error( String.format( "size of fareZones object is %d", fareZones.size() ) );
                    throw e;
                }
                
                try {
                    fare = (Float)tazFareLookupTable.get(key);
                }
                catch (RuntimeException e) {
                    logger.error( String.format("exception caught looking up intracity fare for,"));
                    logger.error( String.format("extOrigCent=%d, origFareZone=%s, extDestCent=%d, destFareZone=%s, key=%s", extOrigCent, origFareZone, extDestCent, destFareZone, key), e);
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
            fare = 0.6823*Math.pow(railDist[i], -0.2989); 
        }
        
        return fare;
        
    }

    
    public void setTransitFareTables ( HashMap intracityFareTable, HashMap fareZonesMap ) {
        transitFareLookupTable = intracityFareTable;
        fareZones = fareZonesMap;
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

            debugLogger.debug( "Heap contents sorted by linklabel" );
            Iterator it = elements.iterator();

            int i=0;
            while ( it.hasNext() ) {
                HeapElement h = (HeapElement)it.next();
                int k = h.getIndex();
                int m = hwyLink[k];
                debugLogger.info ("i=" + (i++) + ",k=" + k + ", ag.ia[k]=" + ia[k] + "(g.an=" + (m>=0 ? indexNode[gia[m]] : -1) + "), ag.ib[k]=" + ib[k] + "(g.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + "), linkType=" + linkType[k] + ", Route=" + trRoute[k] + ", linkLabel[k]=" + String.format("%10.6f", linkLabel[k]) );
            }
        }

    }

}	
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

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;



public class OptimalStrategy {


	protected static Logger logger = Logger.getLogger(OptimalStrategy.class);

	public static final int IVT = 0;
	public static final int FWT = 1;
	public static final int TWT = 2;
    public static final int ACC = 3;
    public static final int AUX = 4;
	public static final int BRD = 5;
    public static final int FAR = 6;
    public static final int HSR = 7;
	public static final int NUM_SKIMS = 8;

	static final double COMPARE_EPSILON = 1.0e-07;

	static final double MIN_ALLOCATED_FLOW = 0.00001;
	
	static final int MAX_BOARDING_LINKS = 100;

//	static final double LATITUDE_PER_FEET  = 2.7;
//	static final double LONGITUDE_PER_FEET = 3.6;
	static final double LATITUDE_PER_FEET  = 1.0;
	static final double LONGITUDE_PER_FEET = 1.0;


	AuxTrNet ag;
	NetworkHandlerIF nh;

	int dest;
	
	Heap candidateHeap;
	int[] heapContents;

	double[] nodeLabel, nodeFreq, linkLabel;
	boolean[] inStrategy;
	int[] orderInStrategy;
	int[] strategyOrderForLink;
	
	double[] nodeFlow;

    int[] alphaNumberArray = null;
    int[] zonesToSkim = null;
    int[] externalToAlphaInternal = null;
    int[] alphaExternalNumbers = null;
    
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
	
	
	
	public OptimalStrategy ( AuxTrNet ag ) {

		this.ag = ag;
		this.nh = ag.getHighwayNetworkHandler();
        
		nodeFlow = new double[ag.getAuxNodeCount()+1];
		nodeLabel = new double[ag.getAuxNodeCount()+1];
		nodeFreq = new double[ag.getAuxNodeCount()+1];
		linkLabel = new double[ag.getAuxLinkCount()+1];
		inStrategy = new boolean[ag.getAuxLinkCount()+1];
		orderInStrategy = new int[ag.getAuxLinkCount()+1];
		strategyOrderForLink = new int[ag.getAuxLinkCount()+1];
		
		//Create a new heap structure to sort candidate node labels
        //candidateHeap = new Heap(ag.getAuxNodeCount()+1);  // old Heap 
        candidateHeap = new Heap( ag.getAuxLinkCount() ); // new SortedSet
		heapContents = new int[ag.getAuxNodeCount()+1];

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



    
    public void initSkimMatrices ( String zoneCorrespondenceFile ) {

        // take a column of alpha zone numbers from a TableDataSet and puts them into an array for
        // purposes of setting external numbers.         */
        try {
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(zoneCorrespondenceFile));
            alphaNumberArray = table.getColumnAsInt( 1 );
        } catch (IOException e) {
            logger.fatal("Can't get zone numbers from zonal correspondence file");
            e.printStackTrace();
        }

    
        // define which of the total set of centroids are within the Halo area and should have skim trees built
        zonesToSkim = new int[nh.getMaxCentroid()+1];
        externalToAlphaInternal = new int[nh.getMaxCentroid()+1];
        alphaExternalNumbers = new int[alphaNumberArray.length+1];
        Arrays.fill ( zonesToSkim, 0 );
        Arrays.fill ( externalToAlphaInternal, -1 );
        for (int i=0; i < alphaNumberArray.length; i++) {
            zonesToSkim[alphaNumberArray[i]] = 1;
            externalToAlphaInternal[alphaNumberArray[i]] = i;
            alphaExternalNumbers[i+1] = alphaNumberArray[i];
        }

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
		accessTime = new double[ag.walkTime.length];
		if (ag.getAccessMode().equalsIgnoreCase("walk")) {
			accessTime = ag.walkTime; 
		}
		else {
			accessTime = ag.driveAccTime; 
		}


		if (debug)
		    logger.info ("building optimal strategy to " + dest + "(" + indexNode[dest] + ")");
		
        
        //while ((k = candidateHeap.remove()) != -1) {  //old Heap
        while ( candidateHeap.size() > 0 ) {

            HeapElement he = candidateHeap.getFirst();
            k = he.getIndex();
            
            if (ag.ia[k] != dest && !inStrategy[k]) {

    			// do not include links into centroids in strategy unless its going into dest.
    			if ( ag.ib[k] < nh.getNumCentroids() && ag.ib[k] != dest ) {
    				inStrategy[k] = false;
    				continue;
    			}
    			
    			
    			// get the highway network link index for the given transit network link index
    			m = ag.hwyLink[k];
			

				linkImped = ag.getLinkImped(k);
				
				// log some information about the starting condition of the candidate link being examined
				if ( debug ) {
					logger.info ("");
					
					logger.info ("k=" + k + ", ag.ia[k]=" + ag.ia[k] + "(g.an=" + (m>=0 ? indexNode[gia[m]] : -1) + "), ag.ib[k]=" + ag.ib[k] + "(g.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + "), linkType=" + ag.linkType[k] + ", trRoute=" + ag.trRoute[k] + "(" + (ag.trRoute[k] >= 0 ? ag.tr.getLine(ag.trRoute[k]) : "aux") + ")" );
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
									ag.dwellTime[k-1] = gDist[ag.hwyLink[k-1]]*(-ag.dwellTime[k-1]);
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
					logger.info ("ag.ia[k]=" + ag.ia[k] + "(g.an=" + (m >= 0 ? indexNode[gia[m]] : -1) + "), ag.ib[k]=" + ag.ib[k] + "(g.bn=" + ( m>=0 ? indexNode[gib[m]] : -1) + ")");
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
                logger.info ("adding   " + i + ", indexb[i] or k=" + k + ", linkType=" + ag.linkType[k] + ", ia=" + ag.ia[k] + "(" + (m>=0 ? indexNode[gia[m]] : -1) + "), ib=" + ag.ib[k] + "(" + (m>=0 ? indexNode[gib[m]] : -1) + "), linkLabel[k]=" + String.format("%15.6f", linkLabel[k]) + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ag.ib[k]] + ", linkImped=" + linkImped);

            HeapElement he = new HeapElement(k, ag.linkType[k], linkLabel[k]);
            
            if ( candidateHeap.contains(k))
                candidateHeap.remove(he);
            
            candidateHeap.add(he);

        }

        if (debug) candidateHeap.dataPrintSorted();
            
        return 0;
    }



    private double sumBoardingFlow (int ia) {
        // add up the flow allocated to boarding links exiting this internally numbered transit network node.

        int i, j, k;
        int start, end;
        double boardingFlow = 0.0;
        boolean debug = classDebug;


        // start is the pointer array index for links exiting ia.
        start = ag.ipa[ia];
        if (start == -1) {
            return -1;
        }
        if (debug)
            logger.info ("start=" + start + ", ia=" + ag.ia[ag.indexa[start]] + ", ib=" + ag.ib[ag.indexa[start]] + ", an=" + (ag.ia[ag.indexa[start]] < indexNode.length ? indexNode[ag.ia[ag.indexa[start]]] : 0) + ", bn=" + (ag.ib[ag.indexa[start]] < indexNode.length ? indexNode[ag.ib[ag.indexa[start]]] : 0));


        j = ia + 1;
        while (ag.ipa[j] == -1)
            j++;
        end = ag.ipa[j];
        if (debug) {
            logger.info ("end=" + end + ", j=" + j);
            logger.info ("end=" + end + ", indexa[end]=" + (end < ag.indexa.length ? Integer.toString(ag.indexa[end]) : "null") + ", ia=" + (end < ag.indexa.length ? Integer.toString(ag.ia[ag.indexa[end]]) : "null") + ", ib=" + (end < ag.indexa.length ? Integer.toString(ag.ib[ag.indexa[end]]) : "null"));
            logger.info ("");
        }
        
        
        for (i=start; i < end; i++) {

            k = ag.indexa[i];

            // if link k is a boarding link and it is in the strategy, sum its link flow.
            if ( ag.linkType[k] == AuxTrNet.BOARDING_TYPE && inStrategy[k] )
                boardingFlow += ag.flow[k];
                
        }

        return boardingFlow;
        
    }



    private double sumAlightingFlow (int ib) {
        // add up the flow allocated to alighting links entering this internally numbered transit network node.

        int i, j, k;
        int start, end;
        double alightingFlow = 0.0;
        boolean debug = classDebug;


        // start is the pointer array index for links exiting ia.
        start = ag.ipb[ib];
        if (start == -1) {
            return -1;
        }
        if (debug)
            logger.info ("start=" + start + ", ia=" + ag.ia[ag.indexb[start]] + ", ib=" + ag.ib[ag.indexb[start]] + ", an=" + (ag.ia[ag.indexb[start]] < indexNode.length ? indexNode[ag.ia[ag.indexb[start]]] : 0) + ", bn=" + (ag.ib[ag.indexb[start]] < indexNode.length ? indexNode[ag.ib[ag.indexb[start]]] : 0));


        j = ib + 1;
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

            // if link k is a boarding link and it is in the strategy, sum its link flow.
            if ( ag.linkType[k] == AuxTrNet.ALIGHTING_TYPE && inStrategy[k] )
                alightingFlow += ag.flow[k];
                
        }

        return alightingFlow;
        
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
                        
                        if ( ag.ia[k] < nh.getNumCentroids() )
                            originsNotLoaded[ag.ia[k]] = 0;
                    }
                }

            }
            
            
            
            if (debug) {
                logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + ag.trRoute[k] + ", ag.ia=" + ag.ia[k] + ", ag.ib="  + ag.ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", ag.linkType=" + ag.linkType[k] + ", ag.walkTime=" + ag.walkTime[k] + ", ag.invTime=" + ag.invTime[k] + ", ag.waitTime=" + ag.waitTime[k] + ", ag.flow[k]=" + ag.flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ag.ia[k]] + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ag.ib[k]] );
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
    

    public double[] getOptimalStrategyWtSkimsOrigDest (int startFromNode, int startToNode) {

        // startFromNode and startToNode are externally numbered.
        
        int k, m;
        int fromNodeIndex=-1;
        int count;
        
        double flow;
        
        boolean debug = classDebug;
//        boolean debug = true;
        

        double[] results = new double[6];
        Arrays.fill (results, AuxTrNet.UNCONNECTED);
        Arrays.fill (nodeFlow, 0.0);
        Arrays.fill (ag.flow, 0.0);

        
        if (startFromNode == indexNode[dest]) {
            return results;
        }


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
                    logger.info ( "startFromNode=" + startFromNode + "(" + nodeIndex[startFromNode] + "), startToNode=" + startToNode + "(" + nodeIndex[startToNode] + "), fromNodeIndex=" + fromNodeIndex + ", i=" + i + ", k=" + k + ", m=" + m + ", ag.ia=" + ag.ia[k] + ", ag.ib=" + ag.ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", ag.linkType=" + ag.linkType[k] );
                }
                break;
            }
        }
        
        if ( fromNodeIndex < 0 ) 
            return results;

        
        // set one trip starting at the startFromNode
        nodeFlow[nodeIndex[startFromNode]] = 1.0;
        
        
        // loop through links in optimal strategy starting at the index where the startFromNode was found and assign flow to links in the strategy
        count = 0;
        for (int i=fromNodeIndex; i >= 0; i--) {
            
            k = orderInStrategy[i];
            m = ag.hwyLink[k];
            
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
                    }
                }

                if ( debug )
                    logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + ag.trRoute[k] + ", ag.ia=" + ag.ia[k] + ", ag.ib="  + ag.ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", ag.linkType=" + ag.linkType[k] + ", ag.walkTime=" + ag.walkTime[k] + ", ag.invTime=" + ag.invTime[k] + ", ag.waitTime=" + ag.waitTime[k] + ", ag.flow[k]=" + ag.flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ag.ia[k]] + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ag.ib[k]] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ag.ia[k]] + ", ag.freq[k]=" + ag.freq[k] + ", nodeFlow[ag.ia[k]]=" + nodeFlow[ag.ia[k]] + ", nodeFlow[ag.ib[k]]=" + nodeFlow[ag.ib[k]] );
                
                count++;

            }

        }
        

        // loop through links in optimal strategy that received flow and log some information 
        Integer tempNode = new Integer(0);
        ArrayList boardingNodes = new ArrayList();
        double inVehTime = 0.0;
        double dwellTime = 0.0;
        double walkTime = 0.0;
        double wtAccTime = 0.0;
        double wtEgrTime = 0.0;
        double firstWait = 0.0;
        double totalWait = 0.0;
        double boardings = 0.0;
        double alightings = 0.0;
        double totalBoardings = 0.0;
        double fare = 0.0;
        
        
        count = 0;
        if (debug)
            logger.info ( "\n\n\nlinks in strategy with flow from origin toward destination:" );
        for (int i=inStrategyCount; i >= 0; i--) {
            
            k = orderInStrategy[i];
            m = ag.hwyLink[k];
            
            if (nodeFlow[ag.ia[k]] == 0.0)
                continue;

            if ( debug )
                logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + ag.trRoute[k] + ", ag.ia=" + ag.ia[k] + ", ag.ib="  + ag.ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", ag.linkType=" + ag.linkType[k] + ", ag.walkTime=" + ag.walkTime[k] + ", ag.invTime=" + ag.invTime[k] + ", ag.waitTime=" + ag.waitTime[k] + ", ag.flow[k]=" + ag.flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ag.ia[k]] + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ag.ib[k]] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ag.ia[k]] );
            
            
            if ( ag.linkType[k] == AuxTrNet.BOARDING_TYPE ) {
                
                tempNode = new Integer(ag.ia[k]);

                // since we loaded 1 trip for computing skims, the fraction of the trip loading transit lines at a node
                // is also a weight used to computed average waiting time at the node.
                if ( firstWait == 0.0 ) {
                    boardings = sumBoardingFlow (ag.ia[k]);
                    totalBoardings = boardings;
                    boardingNodes.add(tempNode);
                    firstWait = boardings*AuxTrNet.ALPHA/nodeFreq[ag.ia[k]];
                    totalWait = firstWait;
                    fare = boardings*AuxTrNet.FARE;
                }
                else {
                    if ( !boardingNodes.contains(tempNode) ) {
                        boardings = sumBoardingFlow (ag.ia[k]);
                        totalBoardings += boardings;
                        totalWait += boardings*AuxTrNet.ALPHA/nodeFreq[ag.ia[k]];
                        fare += boardings*AuxTrNet.TRANSFER_FARE;
                        boardingNodes.add(tempNode);
                    }
                }
                
            }
            else if ( ag.linkType[k] == AuxTrNet.IN_VEHICLE_TYPE ) {
                
                inVehTime += ag.invTime[k]*ag.flow[k];
                dwellTime += ag.dwellTime[k]*ag.flow[k];
                
            }
            else if ( ag.linkType[k] == AuxTrNet.AUXILIARY_TYPE ) {

                // accumulate access walk time and total walk time
                if ( firstWait == 0.0 )
                    wtAccTime += ag.walkTime[k]*ag.flow[k];
                else
                    walkTime += ag.walkTime[k]*ag.flow[k];
                
            }
                
            count++;

        }

        // linkFreqs were weighted by WAIT_COEFF, so unweight them to get actual first and total wait values
        firstWait /= AuxTrNet.WAIT_COEFF;
        totalWait /= AuxTrNet.WAIT_COEFF;


        

        // loop through links in optimal strategy starting at the startToNode to accumulate egress time
        count = 0;
        double totalEgressFlow = 0.0;
        if (debug)
            logger.info ( "\n\n\nlinks in strategy with flow from destination toward origin:" );
        for (int i=0; i < inStrategyCount; i++) {
            
            k = orderInStrategy[i];
            m = ag.hwyLink[k];
            
            if (nodeFlow[ag.ia[k]] == 0.0)
                continue;

            if ( debug )
                logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + ag.trRoute[k] + ", ag.ia=" + ag.ia[k] + ", ag.ib="  + ag.ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", ag.linkType=" + ag.linkType[k] + ", ag.walkTime=" + ag.walkTime[k] + ", ag.invTime=" + ag.invTime[k] + ", ag.waitTime=" + ag.waitTime[k] + ", ag.flow[k]=" + ag.flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ag.ia[k]] + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ag.ib[k]] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ag.ia[k]] );
            
            
            if ( ag.linkType[k] == AuxTrNet.AUXILIARY_TYPE ) {

                // get total flow alighting to this node
                alightings = sumAlightingFlow(ag.ia[k]);
                totalEgressFlow += alightings;
                
                // accumulate access walk time and total walk time
                wtEgrTime += alightings*ag.walkTime[k];
                
                
                //break out of loop when all flow to destination is accounted for.
                if ( 1.0 - totalEgressFlow < COMPARE_EPSILON )
                    break;
                
            }
                
            count++;

        }

        walkTime -= wtEgrTime;
        
        
        if ( debug ) {
            logger.info ( "\n\n\ntransit skims from " + startFromNode + " to " + startToNode + ":" );
            logger.info ( "in-vehicle time  = " + inVehTime );
            logger.info ( "dwell time       = " + dwellTime );
            logger.info ( "firstWait time   = " + firstWait );
            logger.info ( "totalWait time   = " + totalWait );
            logger.info ( "wt access time   = " + wtAccTime );
            logger.info ( "wt egress time   = " + wtEgrTime );
            logger.info ( "other walk time  = " + walkTime );
            logger.info ( "total boardings  = " + totalBoardings );
        }
            
        
        results[0] = inVehTime;
        results[1] = firstWait;
        results[2] = totalWait + dwellTime;
        results[3] = wtAccTime;
        results[4] = totalBoardings;
        results[5] = fare;

        
        return results;
        
    }



    public double[][] getOptimalStrategySkimsDest () {

        int k, m;
        int count;
        
        //double flow = 0.0;
        
        boolean debug = classDebug;
        

        
        
        double[][] nodeSkims = new double[NUM_SKIMS][ag.getAuxNodeCount()];
        double[][] skimResults = new double[NUM_SKIMS][nh.getNumCentroids()];
        for (k=0; k < NUM_SKIMS; k++) {
            Arrays.fill (nodeSkims[k], AuxTrNet.UNCONNECTED);
            Arrays.fill (skimResults[k], AuxTrNet.UNCONNECTED);
        }

//        Arrays.fill (ag.flow, 0.0);
//        for (int i=0; i < nh.getNumCentroids(); i++)
//            nodeFlow[i] = 1.0;
//        nodeFlow[dest] = 0.0;
        

        
        
        // check links entering dest.  If none are access links in the strategy, then dest is not transit accessible.
        boolean walkAccessAtDestination = false;
        int start = ag.ipb[dest];
        if (start >= 0) {

            int j = dest + 1;
            while (ag.ipb[j] == -1)
                j++;
            int end = ag.ipb[j];
            
            for (int i=start; i < end; i++) {
                k = ag.indexb[i];
                if ( ag.linkType[k] == AuxTrNet.AUXILIARY_TYPE && inStrategy[k] ) {
                    walkAccessAtDestination = true;
                    break;
                }
            }
        }

        if ( !walkAccessAtDestination )
            return skimResults;
        
        
        
        // loop through links in optimal strategy in reverse order and propagate 1 trip from each accessible origin through the optimal strategy to the destination. 
//        for (int i=inStrategyCount; i >= 0; i--) {
//            
//            k = orderInStrategy[i];
//            m = ag.hwyLink[k];
//            
//
//            if (nodeFlow[ag.ia[k]] > 0.0) {
//                
//                if ( ag.linkType[k] == AuxTrNet.BOARDING_TYPE ) {
//                    flow = (ag.freq[k]/nodeFreq[ag.ia[k]])*nodeFlow[ag.ia[k]];
//                    ag.flow[k] = flow;
//                    nodeFlow[ag.ib[k]] += flow;
//                }
//                else {
//                    flow = nodeFlow[ag.ia[k]];
//                    if ( nodeLabel[ag.ib[k]] != AuxTrNet.INFINITY ) {
//                        ag.flow[k] += flow;
//                        nodeFlow[ag.ib[k]] += flow;
//                    }
//                }
//
//            }
//            
//        }            
            
            

        // loop through links in optimal strategy in ascending order and accumulate skim component values at nodes weighted by link flows
        count = 0;
        for (int i=0; i < inStrategyCount; i++) {
            
            k = orderInStrategy[i];
            m = ag.hwyLink[k];
            
            if ( debug )
                logger.info ( "count=" + count + ", i=" + i + ", k=" + k + ", m=" + m + ", trRoute=" + ag.trRoute[k] + ", ag.ia=" + ag.ia[k] + ", ag.ib="  + ag.ib[k] + ", nh.an=" + (m>=0 ? indexNode[gia[m]] : -1) + ", nh.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + ", ag.linkType=" + ag.linkType[k] + ", ag.walkTime=" + ag.walkTime[k] + ", ag.invTime=" + ag.invTime[k] + ", ag.waitTime=" + ag.waitTime[k] + ", ag.flow[k]=" + ag.flow[k] + ", nodeLabel[ag.ia[k]]=" + nodeLabel[ag.ia[k]] + ", nodeLabel[ag.ib[k]]=" + nodeLabel[ag.ib[k]] + ", nodeFreq[ag.ia[k]]=" + nodeFreq[ag.ia[k]] + ", ag.freq[k]=" + ag.freq[k] + ", nodeFlow[ag.ia[k]]=" + nodeFlow[ag.ia[k]] + ", nodeFlow[ag.ib[k]]=" + nodeFlow[ag.ib[k]] );
            
        
        
            if ( ag.linkType[k] == AuxTrNet.BOARDING_TYPE ) {

                if ( nodeSkims[FWT][ag.ib[k]] == 0.0 ) {
                    nodeSkims[FWT][ag.ia[k]] = ag.freq[k]*(AuxTrNet.ALPHA/nodeFreq[ag.ia[k]])/nodeFreq[ag.ia[k]];
                    nodeSkims[TWT][ag.ia[k]] = ag.freq[k]*(AuxTrNet.ALPHA/nodeFreq[ag.ia[k]])/nodeFreq[ag.ia[k]];
                    nodeSkims[BRD][ag.ia[k]] = ag.freq[k]/nodeFreq[ag.ia[k]];
                    nodeSkims[FAR][ag.ia[k]] = ag.freq[k]*AuxTrNet.FARE/nodeFreq[ag.ia[k]];
                }
                else {
                    nodeSkims[FWT][ag.ia[k]] = nodeSkims[FWT][ag.ib[k]] + ag.freq[k]*AuxTrNet.ALPHA/nodeFreq[ag.ia[k]];
                    nodeSkims[TWT][ag.ia[k]] = nodeSkims[TWT][ag.ib[k]] + ag.freq[k]*AuxTrNet.ALPHA/nodeFreq[ag.ia[k]];
                    nodeSkims[BRD][ag.ia[k]] = nodeSkims[BRD][ag.ib[k]] + ag.freq[k]/nodeFreq[ag.ia[k]];
                    nodeSkims[FAR][ag.ia[k]] = nodeSkims[FAR][ag.ib[k]] + ag.freq[k]*AuxTrNet.TRANSFER_FARE/nodeFreq[ag.ia[k]];
                }
                nodeSkims[IVT][ag.ia[k]] = nodeSkims[IVT][ag.ib[k]];
                nodeSkims[AUX][ag.ia[k]] = nodeSkims[AUX][ag.ib[k]];
                nodeSkims[ACC][ag.ia[k]] = nodeSkims[ACC][ag.ib[k]];
                nodeSkims[HSR][ag.ia[k]] = nodeSkims[HSR][ag.ib[k]];
                
            }
            else if ( ag.linkType[k] == AuxTrNet.IN_VEHICLE_TYPE ) {
                
                nodeSkims[IVT][ag.ia[k]] = nodeSkims[IVT][ag.ib[k]] + ag.invTime[k];
                nodeSkims[FWT][ag.ia[k]] = nodeSkims[FWT][ag.ib[k]];
                nodeSkims[TWT][ag.ia[k]] = nodeSkims[TWT][ag.ib[k]] + ag.dwellTime[k];
                nodeSkims[AUX][ag.ia[k]] = nodeSkims[AUX][ag.ib[k]];
                nodeSkims[ACC][ag.ia[k]] = nodeSkims[ACC][ag.ib[k]];
                nodeSkims[BRD][ag.ia[k]] = nodeSkims[BRD][ag.ib[k]];
                nodeSkims[FAR][ag.ia[k]] = nodeSkims[FAR][ag.ib[k]];
                
                if ( ag.rteMode[k] == 'm' )
                    nodeSkims[HSR][ag.ia[k]] = nodeSkims[HSR][ag.ib[k]] + ag.invTime[k];
                else
                    nodeSkims[HSR][ag.ia[k]] = nodeSkims[HSR][ag.ib[k]];
                    
            }
            else if ( ag.linkType[k] == AuxTrNet.LAYOVER_TYPE ) {
                
                nodeSkims[IVT][ag.ia[k]] = nodeSkims[IVT][ag.ib[k]];
                nodeSkims[FWT][ag.ia[k]] = nodeSkims[FWT][ag.ib[k]];
                nodeSkims[TWT][ag.ia[k]] = nodeSkims[TWT][ag.ib[k]] + ag.layoverTime[k];
                nodeSkims[AUX][ag.ia[k]] = nodeSkims[AUX][ag.ib[k]];
                nodeSkims[ACC][ag.ia[k]] = nodeSkims[ACC][ag.ib[k]];
                nodeSkims[BRD][ag.ia[k]] = nodeSkims[BRD][ag.ib[k]];
                nodeSkims[FAR][ag.ia[k]] = nodeSkims[FAR][ag.ib[k]];
                    
            }
            else if ( ag.linkType[k] == AuxTrNet.ALIGHTING_TYPE ) {
                
                nodeSkims[IVT][ag.ia[k]] = nodeSkims[IVT][ag.ib[k]];
                nodeSkims[FWT][ag.ia[k]] = nodeSkims[FWT][ag.ib[k]];
                nodeSkims[TWT][ag.ia[k]] = nodeSkims[TWT][ag.ib[k]];
                nodeSkims[AUX][ag.ia[k]] = nodeSkims[AUX][ag.ib[k]];
                nodeSkims[ACC][ag.ia[k]] = nodeSkims[ACC][ag.ib[k]];
                nodeSkims[BRD][ag.ia[k]] = nodeSkims[BRD][ag.ib[k]];
                nodeSkims[FAR][ag.ia[k]] = nodeSkims[FAR][ag.ib[k]];
                nodeSkims[HSR][ag.ia[k]] = nodeSkims[HSR][ag.ib[k]];
                
            }
            else if ( ag.linkType[k] == AuxTrNet.AUXILIARY_TYPE ) {

                // if bnode is dest, initialize anode's total walk time to that walk egress time and other skims to zero.
                if ( ag.ib[k] < nh.getNumCentroids() ) {
                    nodeSkims[IVT][ag.ia[k]] = 0.0;
                    nodeSkims[FWT][ag.ia[k]] = 0.0;
                    nodeSkims[TWT][ag.ia[k]] = 0.0;
                    nodeSkims[AUX][ag.ia[k]] = ag.walkTime[k];
                    nodeSkims[ACC][ag.ia[k]] = 0.0;
                    nodeSkims[BRD][ag.ia[k]] = 0.0;
                    nodeSkims[FAR][ag.ia[k]] = 0.0;
                    nodeSkims[HSR][ag.ia[k]] = 0.0;
                }
                // anode is an origin node, set final skim values for that origin and add the access time
                else if ( ag.ia[k] < nh.getNumCentroids() ) {
                    skimResults[IVT][ag.ia[k]] = nodeSkims[IVT][ag.ib[k]];
                    skimResults[FWT][ag.ia[k]] = nodeSkims[FWT][ag.ib[k]];
                    skimResults[TWT][ag.ia[k]] = nodeSkims[TWT][ag.ib[k]];
                    skimResults[AUX][ag.ia[k]] = nodeSkims[AUX][ag.ib[k]];
                    skimResults[ACC][ag.ia[k]] = nodeSkims[ACC][ag.ib[k]] + accessTime[k];
                    skimResults[BRD][ag.ia[k]] = nodeSkims[BRD][ag.ib[k]];
                    skimResults[FAR][ag.ia[k]] = nodeSkims[FAR][ag.ib[k]];
                    skimResults[HSR][ag.ia[k]] = nodeSkims[HSR][ag.ib[k]];
                }
                // link is a walk link, not connected to a centroid
                else {
                    nodeSkims[IVT][ag.ia[k]] = nodeSkims[IVT][ag.ib[k]];
                    nodeSkims[FWT][ag.ia[k]] = nodeSkims[FWT][ag.ib[k]];
                    nodeSkims[TWT][ag.ia[k]] = nodeSkims[TWT][ag.ib[k]];
                    nodeSkims[AUX][ag.ia[k]] = nodeSkims[AUX][ag.ib[k]] + ag.walkTime[k];
                    nodeSkims[ACC][ag.ia[k]] = nodeSkims[ACC][ag.ib[k]] + ag.walkTime[k];
                    nodeSkims[BRD][ag.ia[k]] = nodeSkims[BRD][ag.ib[k]];
                    nodeSkims[FAR][ag.ia[k]] = nodeSkims[FAR][ag.ib[k]];
                    nodeSkims[HSR][ag.ia[k]] = nodeSkims[HSR][ag.ib[k]];
                }
                
            }

        }
        
        count++;


        
        
        // linkFreqs were weighted by WAIT_COEFF, so unweight them to get actual first and total wait values
        for (int i=0; i < nh.getNumCentroids(); i++) {
            skimResults[FWT][i] /= AuxTrNet.WAIT_COEFF;
            skimResults[TWT][i] /= AuxTrNet.WAIT_COEFF;
        }

        
        

        return skimResults;
        
    }



	public Matrix[] getOptimalStrategySkimMatrices () {

        // get skim values into 0-based double[][] dimensioned to number of actual zones including externals (2983)
        float[][][] zeroBasedFloatArrays = new float[NUM_SKIMS][][];
        double[][][] zeroBasedDoubleArray = new double[NUM_SKIMS][nh.getNumCentroids()][nh.getNumCentroids()];

		
		for (int dest=0; dest < nh.getNumCentroids(); dest++) {
		    
		    if ( dest % 100 == 0 ) {
		        logger.info ( "generating skims to zone " + dest + " at " + DateFormat.getDateTimeInstance().format(new Date()) );
		    }
		    
		    
			// build an optimal strategy for the specified destination node.
			if ( buildStrategy(dest) >= 0 ) {
			    
			    double[][] odSkimValues = getOptimalStrategySkimsDest();
                    
                for (int k=0; k < NUM_SKIMS; k++) {
                    for (int orig=0; orig < nh.getNumCentroids(); orig++)
                        zeroBasedDoubleArray[k][orig][dest] = odSkimValues[k][orig];
                    
				}
				
			}
			
		}
        

        for (int k=0; k < NUM_SKIMS; k++) {
            zeroBasedFloatArrays[k] = getZeroBasedFloatArray ( zeroBasedDoubleArray[k] );
        }
        

        String nameQualifier = null;
        String descQualifier = null;
        if ( ag.getTimePeriod().equalsIgnoreCase("peak") ) {
            nameQualifier = "p";
            descQualifier = "peak";
        }
        else {
            nameQualifier = "o";
            descQualifier = "offpeak";
        }

        if ( ag.getAccessMode().equalsIgnoreCase("walk") ) {
            nameQualifier = nameQualifier.concat("wt");
            descQualifier = descQualifier.concat(" walk-transit");
        }
        else {
            nameQualifier = nameQualifier.concat("dt");
            descQualifier = descQualifier.concat(" drive-transit");
        }
        

        Matrix[] skimMatrices = new Matrix[NUM_SKIMS];
        skimMatrices[IVT] = new Matrix( nameQualifier + "ivt", descQualifier + " in-vehicle time skims", zeroBasedFloatArrays[IVT] );
        skimMatrices[FWT] = new Matrix( nameQualifier + "fwt", descQualifier + " first wait time skims", zeroBasedFloatArrays[FWT] );
        skimMatrices[TWT] = new Matrix( nameQualifier + "twt", descQualifier + " total wait time skims", zeroBasedFloatArrays[TWT] );
        skimMatrices[AUX] = new Matrix( nameQualifier + "acc", descQualifier + " other/egress time skims", zeroBasedFloatArrays[ACC] );
        skimMatrices[ACC] = new Matrix( nameQualifier + "aux", descQualifier + " access time skims", zeroBasedFloatArrays[AUX] );
        skimMatrices[BRD] = new Matrix( nameQualifier + "brd", descQualifier + " boardings skims", zeroBasedFloatArrays[BRD] );
        skimMatrices[FAR] = new Matrix( nameQualifier + "far", descQualifier + " fare skims", zeroBasedFloatArrays[FAR] );
        skimMatrices[HSR] = new Matrix( nameQualifier + "hsr", descQualifier + " high speed rail ivt skims", zeroBasedFloatArrays[HSR] );
        
        for (int k=0; k < NUM_SKIMS; k++)
            skimMatrices[k].setExternalNumbers( alphaExternalNumbers );
        
		return skimMatrices;
		
	}

    
    private float[][] getZeroBasedFloatArray ( double[][] zeroBasedDoubleArray ) {

        int[] skimsInternalToExternal = indexNode;

        // convert the zero-based double[2983][2983] produced by the skimming procedure
        // to a zero-based float[2950][2950] for alpha zones to be written to skims file.
        float[][] zeroBasedFloatArray = new float[alphaNumberArray.length][alphaNumberArray.length];
        
        int exRow;
        int exCol;
        int inRow;
        int inCol;
        for (int i=0; i < zeroBasedDoubleArray.length; i++) {
            exRow = skimsInternalToExternal[i];
            if ( zonesToSkim[exRow] == 1 ) {
                inRow = externalToAlphaInternal[exRow];
                for (int j=0; j < zeroBasedDoubleArray[i].length; j++) {
                    exCol = skimsInternalToExternal[j];
                    if ( zonesToSkim[exCol] == 1 ) {
                        inCol = externalToAlphaInternal[exCol];
                        zeroBasedFloatArray[inRow][inCol] = (float)zeroBasedDoubleArray[i][j];
                    }
                }
            }
        }

        zeroBasedDoubleArray = null;

        return zeroBasedFloatArray;

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
                int m = ag.hwyLink[k];
                logger.info ("i=" + (i++) + ",k=" + k + ", ag.ia[k]=" + ag.ia[k] + "(g.an=" + (m>=0 ? indexNode[gia[m]] : -1) + "), ag.ib[k]=" + ag.ib[k] + "(g.bn=" + (m>=0 ? indexNode[gib[m]] : -1) + "), linkType=" + ag.linkType[k] + ", Route=" + ag.trRoute[k] + ", linkLabel[k]=" + String.format("%10.6f", linkLabel[k]) );
            }
        }

    }

}	
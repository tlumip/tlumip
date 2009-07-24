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
package com.pb.tlumip.ts;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.pb.common.util.IndexSort;

/**
 * Class for shortest path trees.
 *
 */

public class ShortestPathTreeH {

    private Logger logger = Logger.getLogger(ShortestPathTreeH.class);
    private Logger spDebugLogger = Logger.getLogger("spDebugLogger");

    static final double COMPARE_EPSILON = 1.0e-07;
    static final int MAX_PATH_LENGTH = 500;

    private int inOrigin;
    private int inDestination;

    private int[] ia;
    private int[] ib;
    private int[] ip;
    private int[] indexNode;
    private int[] nodeIndex;
    private int[] sortedLinkIndex;
    private int[][] turnPenaltyIndices;
    private float[][] turnPenaltyArray;
    private boolean[] centroid;
    private boolean[] validLink;
    private double[] linkCost;
	private double[] aonFlow;	
	
	
	private int[] nodeLabeled;
	private double[] nodeLabels;
	private int[] predecessorLink;

	private int numLinks;
	private int numNodes;
	private int numZones;

	private long initTime = 0;
	private long buildTime = 0;
	private long loadTime = 0;

	private Heap candidateHeap;
	private int[] heapContents;

    private boolean debug = false;
    
    

    public ShortestPathTreeH ( NetworkHandlerIF nh ) {

        numLinks = nh.getLinkCount();
        numNodes = nh.getNodeCount();
        numZones = nh.getNumCentroids();
        
        // store network fields in local arrays
        ia = nh.getIa();
        ib = nh.getIb();
        ip = nh.getIpa();
        sortedLinkIndex = nh.getSortedLinkIndexA();
        indexNode = nh.getIndexNode();
        nodeIndex = nh.getNodeIndex();
        centroid = nh.getCentroid();
        
        aonFlow = new double[numLinks];
        
        turnPenaltyIndices = nh.getTurnPenaltyIndices();
        turnPenaltyArray = nh.getTurnPenaltyArray();
        
        
        nodeLabeled = new int[numNodes+1];
        nodeLabels = new double[numNodes+1];

        //Create a new heap structure to sort candidate node labels
        candidateHeap = new Heap(numNodes+1);
        heapContents = new int[numNodes];
        
    }

    
    public ShortestPathTreeH ( int numLinks, int numNodes, int numZones, int[] ia, int[] ib, int[] ipa, int[] sortedLinkIndexA, int[] indexNode, int[] nodeIndex, boolean[] centroid, int[][] turnPenaltyIndices, float[][] turnPenaltyArray ) {

        this.numLinks = numLinks;
        this.numNodes = numNodes;
        this.numZones = numZones;
        
        // store network fields in local arrays
        this.ia = ia;
        this.ib = ib;
        this.ip = ipa;
        this.sortedLinkIndex = sortedLinkIndexA;
        this.indexNode = indexNode;
        this.nodeIndex = nodeIndex;
        this.centroid = centroid;
        
        aonFlow = new double[numLinks];
        
        this.turnPenaltyIndices = turnPenaltyIndices;
        this.turnPenaltyArray = turnPenaltyArray;

        nodeLabeled = new int[numNodes+1];
        nodeLabels = new double[numNodes+1];

        //Create a new heap structure to sort candidate node labels
        candidateHeap = new Heap(numNodes+1);
        heapContents = new int[numNodes];
        
    }

    
    private void initData() {

        long start = System.currentTimeMillis();

        Arrays.fill(nodeLabeled, 0);
        Arrays.fill(nodeLabels, 1.0e+99);
		nodeLabels[inOrigin] = 0.0;
		nodeLabeled[inOrigin] = 1;

		predecessorLink = new int[numNodes+1];
        Arrays.fill(predecessorLink, -1);

        candidateHeap.clear();

        initTime += (System.currentTimeMillis() - start);

    }


    public void buildTree(int inOrigin) {
        
		long start = System.currentTimeMillis();

		this.inOrigin = inOrigin;
		
        initData();

        // set labels for links eminating from the origin node
        setTreeRootLabels ( inOrigin );

        // continue labeling until candidateHeap is empty
		int k;
        while ((k = candidateHeap.remove()) >= 0) {
            setTreeRootLabels ( ib[k] );
			nodeLabeled[ib[k]] = 1;
        }

        buildTime += (System.currentTimeMillis() - start);
    }


    public boolean buildPath(int inOrigin, int inDestination) {
        int k;
        boolean debug = false;

        this.inOrigin = inOrigin;
        this.inDestination = inDestination;

        if (debug) {
            logger.debug ("building path from " + inOrigin + "(" + indexNode[inOrigin] + ")" + " to " + inDestination + "(" + indexNode[inDestination] + ")");
            spDebugLogger.info ("building path from " + inOrigin + "(" + indexNode[inOrigin] + ")" + " to " + inDestination + "(" + indexNode[inDestination] + ")");
        }
        initData();

        // set labels for links eminating from the origin node
        setPathRootLabels (inOrigin, inDestination);
        if (debug) candidateHeap.dataPrint();

        // continue labeling until candidateHeap is empty
        k = candidateHeap.remove();
        if (debug) {
            logger.debug ("removed k=" + k + ", ia=" + ia[k] + "(" + indexNode[ia[k]] + ")" + ", ib=" + ib[k] + "(" + indexNode[ib[k]] + ")");
            spDebugLogger.info ("removed k=" + k + ", ia=" + ia[k] + "(" + indexNode[ia[k]] + ")" + ", ib=" + ib[k] + "(" + indexNode[ib[k]] + ")");
        }
        while (ib[k] != inDestination) {
            setPathRootLabels (ib[k], inDestination);
            if (debug) candidateHeap.dataPrint();
            nodeLabeled[ib[k]] = 1;
            k = candidateHeap.remove();
            if (k == -1) {
                return false;
            }
            if (debug) {
                logger.debug ("removed k=" + k + ", ia=" + ia[k] + "(" + indexNode[ia[k]] + ")" + ", ib=" + ib[k] + "(" + indexNode[ib[k]] + ")");
                spDebugLogger.info ("removed k=" + k + ", ia=" + ia[k] + "(" + indexNode[ia[k]] + ")" + ", ib=" + ib[k] + "(" + indexNode[ib[k]] + ")");
            }
        }

        return true;
    }

    
    private void setTreeRootLabels ( int rootNode ) {
        // not setting a destination node will cause a shortest path tree to be built
        setRootLabels ( rootNode, -99999 );
    }
    
    
    private void setPathRootLabels ( int rootNode, int destNode ) {
        // setting an origin node and destination node will cause a shortest path to be built between them.
        // note that neither origin node or destination node is required to be a centroid node.
        setRootLabels ( rootNode, destNode );
    }
    
    
    private void setRootLabels ( int rootNode, int destNode ) {

        int k;
        double label;
        double turnPenalty;

        
//        if(logger.isDebugEnabled())
//            logger.debug ("rootNode=" + indexNode[rootNode] +"(external node label)" + ", ip[" + rootNode + "]=" + ip[rootNode] + ", ip[" + (rootNode+1) + "]=" + ip[(rootNode+1)]);


        // if the rootNode has no outgoing links, nothing to do
        int start = ip[rootNode];
        //if ( start == 0 && rootNode > 0 && rootNode != 22654 )
        //    start = 0;
        
        if ( start == 0 && rootNode > 0 )
            return;
        
        // determine the last index for outgoing links from this rootNode
        int offset = 1;
        int end = ip[rootNode + offset++];
        while ( end <= 0 )
            end = ip[rootNode + offset++];

//        if( debug )
//            spDebugLogger.info ("[rootNode]:  rootNode=" + indexNode[rootNode] +"(external node label)" + ", ip[" + rootNode + "]=" + ip[rootNode] + ", ip[" + (rootNode+1) + "]=" + ip[(rootNode+1)] + ", start=" + start + ", end=" + end + ", offset=" + offset );
        
        boolean first = true;
        for (int i=start; i < end; i++) {
            
            k = sortedLinkIndex[i];
            
//            if ( inOrigin == 1547 && indexNode[ib[k]] == 27746 ) {
//                debug = true;
//
//                if( first )
//                    spDebugLogger.info ("[rootNode]:  rootNode=" + indexNode[rootNode] +"(external node label)" + ", ip[" + rootNode + "]=" + ip[rootNode] + ", ip[" + (rootNode+1) + "]=" + ip[(rootNode+1)] + ", start=" + start + ", end=" + end + ", offset=" + offset );
//                
//                first = false;
//            }

            turnPenalty = 0.0;
            if ( turnPenaltyIndices != null && turnPenaltyIndices.length > 0 && predecessorLink[ia[k]] >= 0 )
                turnPenalty = getTurnPenalty( k, predecessorLink[ia[k]] );

//            if(logger.isDebugEnabled())
//                logger.debug ("i=" + i + ", k=" + k + ", ia[k=" + k + "]=" + ia[k] + ", ib[k=" + k + "]=" + ib[k] + ", an[k=" + k + "]=" + indexNode[ia[k]] + ", bn[k=" + k + "]=" + indexNode[ib[k]] + ", linkCost[k=" + k + "]=" + linkCost[k] +  ", nodeLabeled[ib[k]=" + ib[k] + "]=" +  nodeLabeled[ib[k]] +  ", nodeLabels[ib[k]=" + ib[k] + "]=" + nodeLabels[ib[k]] +  ", nodeLabels[ia[k]=" + ia[k] + "]=" + nodeLabels[ia[k]] + ", validLink[k=" + k + "]=" + validLink[k] + ", turnPenalty=" + turnPenalty);
//
//            if( debug )
//                spDebugLogger.info ("[forward star]:  i=" + i + ", k=" + k + ", ia[k=" + k + "]=" + ia[k] + ", ib[k=" + k + "]=" + ib[k] + ", an[k=" + k + "]=" + indexNode[ia[k]] + ", bn[k=" + k + "]=" + indexNode[ib[k]] + ", linkCost[k=" + k + "]=" + linkCost[k] +  ", nodeLabeled[ib[k]=" + ib[k] + "]=" +  nodeLabeled[ib[k]] +  ", nodeLabels[ib[k]=" + ib[k] + "]=" + nodeLabels[ib[k]] +  ", nodeLabels[ia[k]=" + ia[k] + "]=" + nodeLabels[ia[k]] + ", validLink[k=" + k + "]=" + validLink[k] + ", turnPenalty=" + turnPenalty);
            
            if ( validLink[k] && turnPenalty >= 0 ) {
                if (nodeLabeled[ib[k]] == 0) {
                    label = linkCost[k] + nodeLabels[ia[k]] + turnPenalty;
                    if (label - nodeLabels[ib[k]] < -COMPARE_EPSILON) {
                        nodeLabels[ib[k]] = label;
                        if (!centroid[k] || rootNode == inOrigin || ib[k] == destNode) {
                            candidateHeap.add(k);

//                            if (debug)
//                                candidateHeap.dataPrint( true );
                        }

                        predecessorLink[ib[k]] = k;

//                        if(logger.isDebugEnabled())
//                            logger.debug ("predecessor[" + indexNode[ib[k]] + "]=" + k + "   (" + indexNode[ia[k]] + "," + indexNode[ib[k]] + ")");
//                        
//                        if( debug )
//                            spDebugLogger.info ("[predecessor set]:  predecessor[" + indexNode[ib[k]] + "]=" + k + "   (" + indexNode[ia[k]] + "," + indexNode[ib[k]] + ")");
                        
                    }
                }
            }
        }
//        if(logger.isDebugEnabled()) {
//            logger.debug ("");
//        }

        if ( inOrigin != 1547 )
            debug = false;
    }



    /**
     * Load trips from the trip table row associated with the shortest
     * path tree origin
     */
    public double[] loadTree ( double[] tripRow, int userClass  ) {

        long start = System.currentTimeMillis();

        Arrays.fill (aonFlow, 0.0);
        
        int k;
        for (int j=0; j < numZones; j++) {
            if ( tripRow[j] > 0 && j != inOrigin ) {
                k = predecessorLink[j];
                if (k == -1) {
                    logger.info ("no path from " + indexNode[inOrigin] + " to " + indexNode[j] + " for userClass " + userClass);
                    continue;
                }
                aonFlow[k] += tripRow[j];
                while (ia[k] != inOrigin) {
                    k = predecessorLink[ia[k]];
                    aonFlow[k] += tripRow[j];
                }
            }
        }
        loadTime += (System.currentTimeMillis() - start);
        
        return aonFlow;
        
    }



    /**
     * For the user class and origin zone pair, get the trip table row from the DemandHandler
     * and buid and load the shortest path tree from the origin.
     * The loaded Aon link flows are then returned. 
     */
    public double[] buildAndLoadTrees ( int userClass, int origin, double[] originTrips  ) {

        int k;
        
        Arrays.fill (aonFlow, 0.0);
        

        // first build the shortest path tree for internal origin zone number z.
        buildTree( origin );

        
        // load these trips onto the links on routes from z to all destinations j, if there are trips from z to j. 
        for (int j=0; j < numZones; j++) {
            
            if ( originTrips[j] > 0 && j != origin ) {
                
                k = predecessorLink[j];
                if (k == -1) {
                    logger.info ("no path from " + indexNode[origin] + " to " + indexNode[j] + " for userClass " + userClass );
                    continue;
                }
                
                
                aonFlow[k] += originTrips[j];
                while (ia[k] != origin) {
                    k = predecessorLink[ia[k]];
                    aonFlow[k] += originTrips[j];
                }
            }
        }


        return aonFlow;
        
    }



    /**
     * Trace the shortest path tree to get all the skims for each attribute sent in to all
     * destination zones from this origin.
     */
    public double[][] getSkims ( double[][] linkAttributesToSkim ) {
        
        int p;
		double[][] skimTables = new double [linkAttributesToSkim.length][numZones];


        for (int k=0; k < linkAttributesToSkim.length; k++) {

	        for (int j=0; j < numZones; j++) {
	        	
	            if (j != inOrigin) {
	                p = predecessorLink[j];
	                if (p == -1) {
	                    //centroid is not connected
	                	skimTables[k][j] = Double.NEGATIVE_INFINITY;
	                    continue;
	                }
	                skimTables[k][j] += linkAttributesToSkim[k][p];
	                while (ia[p] != inOrigin) {
	                    p = predecessorLink[ia[p]];
	                    if (p == -1) {
	                        logger.error ("invalid predecessorLink: inOrigin=" + inOrigin + ", j=" + j + ", p=" + p);
	                        logger.error ("skimming attributes for table " + k);
	                        System.exit(-1);
	                    }
	                    
	                    skimTables[k][j] += linkAttributesToSkim[k][p];
	
	                }
	                
	            }
	            
	        }
        
        }

        return skimTables;
        
    }


    /**
     * Trace the shortest path tree to get distance skims to all
     * destination zones from this origin.
     */
    public double[] getSkim ( double[] linkAttributeToSkim ) {
        
        int k;
		double[] skim = new double [numZones];


        for (int j=0; j < numZones; j++) {
            if (j != inOrigin) {
                k = predecessorLink[j];
                if (k == -1) {
                    //centroid is not connected
                    skim[j] = Double.NEGATIVE_INFINITY;
                    continue;
                }
				skim[j] += linkAttributeToSkim[k];
                while (ia[k] != inOrigin) {
                    k = predecessorLink[ia[k]];
                    if (k == -1) {
                        logger.info ("invalid predecessorLink: inOrigin=" + inOrigin + ", j=" + j + ", k=" + k);
                        System.exit(-1);
                    }
                    
                   	skim[j] += linkAttributeToSkim[k];

                }
                
            }
        }

        return skim;
        
    }


	public void printPath ( int origin, int destination ) {
	    
		// origin and destination are external numbers
		double cumTime=0.0;

		int[] pathLinks = new int[5000];

		
		// first build the shortest path tree for this origin
		buildTree ( nodeIndex[origin] );
		
		
		int k = predecessorLink[nodeIndex[destination]];

		int count = 0;
		while (ia[k] != nodeIndex[origin]) {
			pathLinks[count++] = k;
			k = predecessorLink[ia[k]];
		}
		pathLinks[count++] = k;


		for (int i=count-1; i >= 0; i--) {
			k = pathLinks[i];
			cumTime += linkCost[k];
			logger.info ( String.format("%-15s%12.4f%12.4f", Integer.toString(indexNode[ia[k]]) + "," + Integer.toString(indexNode[ib[k]]), linkCost[k], cumTime) );
		}
	}


	public void setValidLinks ( boolean[] validLinks) {
		this.validLink = validLinks;
	}
    
    
	public void setLinkCost ( double[] linkCost) {
		this.linkCost = linkCost;
	}
    
    

    /**
     * Return the the int[] predecessorLink array that is current for
     * this ShortestPathTreeH object.
     */
    public int[] getPredecessorLink () {
        return predecessorLink;
    }

    
    
    /**
     * Returns an ArrayList of arrays containing (internal node id, shortest path cost), for links where one of the nodes has a nodeLabel
     * less than the threshold value, and the node is valid.
     */
    public ArrayList<double[]> getNodesWithinCost ( double costThreshold, boolean[] validNode ) {

        ArrayList<double[]> tempList = new ArrayList<double[]>();
        int[] tempData = new int[nodeLabels.length];
        
        int k = 0;
        for (int i=0; i < nodeLabels.length; i++) {
            if ( validNode[i] && nodeLabels[i] < costThreshold  ) {
                double[] nodeData = new double[2];
                nodeData[0] = i;
                nodeData[1] = nodeLabels[i];
                tempData[k++] = (int)(nodeLabels[i]*100000);
                tempList.add( nodeData );
            }
        }

        int[] sortData = new int[k];
        for (int i=0; i < k; i++)
            sortData[i] = tempData[i];
        
        // sort the node list by node label and return a sorted ArrayList with nodes sorted by shortest path cost from origin 
        int[] sortIndices = IndexSort.indexSort( sortData );
        ArrayList<double[]> nodeList = new ArrayList<double[]>(k);
        for (int i=0; i < tempList.size(); i++)
            nodeList.add( tempList.get(sortIndices[i]));
        
        return nodeList;
    }
    
    
    // return an ArrayList of arrays containing (internal node id, shortest path cost), for links where one of the nodes has a nodeLabel
    // greater than the min and less than the max threshold values, and the node is valid.
    public ArrayList<double[]> getNodesWithinCosts ( double minThreshold, double maxThreshold, boolean[] validNode ) {

        ArrayList<double[]> nodeList = new ArrayList<double[]>();
        
        for (int i=0; i < nodeLabels.length; i++) {
            
            if ( validNode[i] && nodeLabels[i] >= minThreshold && nodeLabels[i] < maxThreshold ) {
                double[] nodeData = new double[2];
                nodeData[0] = i;
                nodeData[1] = nodeLabels[i];
                nodeList.add( nodeData );
            }
        }

        return nodeList;
    }
    
    
    
    
    private float getTurnPenalty( int k, int predecessorLink ) {
        
        float penalty = 0.0f;
        
        if ( turnPenaltyIndices[predecessorLink] != null ) {
            
            for (int i=0; i < turnPenaltyIndices[predecessorLink].length; i++) {
                
                if ( turnPenaltyIndices[predecessorLink][i] == k ) {
                    penalty = turnPenaltyArray[predecessorLink][i];
                    break;
                }
                
            }

        }
        
        return penalty;
        
    }

    
    public int[] getNodeList () {

        int i, j, k, count;
        boolean debug = false;

        int[] pathLinks = new int[MAX_PATH_LENGTH];
        
        if (debug) {
            logger.debug ("");
            logger.debug (inDestination + "(" + indexNode[inDestination] + ")");

            k = predecessorLink[inDestination];
            logger.debug (ia[k] + "(" + indexNode[ia[k]] + ")");
            
            count = 0;
            while (ia[k] != inOrigin) {
                count++;
                k = predecessorLink[ia[k]];
                logger.debug (ia[k] + "(" + indexNode[ia[k]] + ")");
            }
            logger.debug (ia[k] + "(" + indexNode[ia[k]] + ")");
            if (count >= MAX_PATH_LENGTH)
                logger.debug (count + " links in path from " + inOrigin + "(" + indexNode[inOrigin] + ") to " + inDestination + "(" + indexNode[inDestination] + ")");
        }

        k = predecessorLink[inDestination];
        count = 0;
        while (ia[k] != inOrigin) {
            pathLinks[count++] = k;
            k = predecessorLink[ia[k]];
        }
        pathLinks[count++] = k;

        int[] nodes = new int[count + 1];

        j = 0;
        for (i=count-1; i >= 0; i--) {
            k = pathLinks[i];
            nodes[j++] = indexNode[ia[k]];
        }
        nodes[j] = indexNode[ib[k]];

        return nodes;
    }
    
    

    public int[] getLinkIdList () {

        int k;

        ArrayList<Integer> pathLinksList = new ArrayList<Integer>(MAX_PATH_LENGTH);
        
        k = predecessorLink[inDestination];
        while (ia[k] != inOrigin) {
            pathLinksList.add(k);
            k = predecessorLink[ia[k]];
        }
        pathLinksList.add(k);

        int[] pathLinks = new int[pathLinksList.size()];
        for ( int i=0; i < pathLinks.length; i++ )
            pathLinks[i] = (Integer)pathLinksList.get(i);
        
        return pathLinks;

    }
    
    

    
    
    
    
    /*-------------------- Inner class --------------------*/

    public class Heap {



        private int data[];
        private int last;


        public Heap(int size) {
            data = new int[size];
            last = -1;
        }


        public Heap(int initData[]) {
            data = new int[100];
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
            
            if(logger.isDebugEnabled()) {
                logger.debug("adding " + x + ", last=" + last + "   " + indexNode[ia[x]] + "   " + indexNode[ib[x]] + "   " + nodeLabels[ib[x]]);
            }
  
            if (heapContents[ib[x]] == 1) {
				for (int i = last; i >= 0; i--) {
				    if ( ib[data[i]] == ib[x] )
				        percolateUp(i);
				}
            }
            else {
				data[++last] = x;
				percolateUp(last);

				heapContents[ib[x]] = 1;
            }

            
			
        }


		public int remove() {
			if (last == -1) return -1;   // no item left
			int min = data[0];           // remove element at top of heap
			data[0] = data[last];        // move last element to top of heap

			if (last == 0) {
				last = -1;
				if(logger.isDebugEnabled()) {
                    logger.debug("remove " + min + ", last=" + last);
                }
				return min;
			}

			last--;                      // reduce heap size
			percolateDown(0);            // move element at top down

			if(logger.isDebugEnabled()) {
                logger.debug("remove " + min + ", last=" + last);
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
				if(logger.isDebugEnabled()) {
                    logger.debug("remove " + min + ", last=" + last);
                }
				return min;
			}

			last--;                      // reduce heap size
			percolateDown(i);            // move element at top down

			if(logger.isDebugEnabled()) {
                logger.debug("remove " + min + ", last=" + last);
            }
			return min;
		}


        //Let element move up and settle
        public void percolateUp(int idx) {
            if(logger.isDebugEnabled()) {
                logger.debug("pu " + idx);
            }
            if (idx == 0) return;
            int parentIdx = (idx - 1) / 2;
            int k = data[idx];                                  // added
            int kParent = data[parentIdx];          // added
			if(logger.isDebugEnabled()) {
				logger.debug("k=" + k + ", ib[k]=" + ib[k] + ", an[k]=" + indexNode[ia[k]] + ", bn[k]=" + indexNode[ib[k]] + ", nodeLabels[ib[k]]=" + nodeLabels[ib[k]]);
				logger.debug("kParent=" + kParent + ", ib[kParent]=" + ib[kParent] + ", an[kParent]=" + indexNode[ia[kParent]] + ", bn[kParent]=" + indexNode[ib[kParent]] + ", nodeLabels[ib[kParent]]=" + nodeLabels[ib[kParent]]);
				logger.debug("nodeLabels[ib[k]] - nodeLabels[ib[kParent]]=" + (nodeLabels[ib[k]] - nodeLabels[ib[kParent]]) );
			}

			if (nodeLabels[ib[k]] - nodeLabels[ib[kParent]] < -COMPARE_EPSILON) {
				swap(parentIdx, idx);           // move larger parent down
				percolateUp(parentIdx);
            }
			else if ((nodeLabels[ib[k]] - nodeLabels[ib[kParent]] <= COMPARE_EPSILON) && (indexNode[ib[k]] < indexNode[ib[kParent]])) {           // added
                swap(parentIdx, idx);           // move larger parent down
                percolateUp(parentIdx);
            }
        }


        public void percolateDown(int idx) {
            if(logger.isDebugEnabled()) {
                logger.debug("pd " + idx);
            }
            int childIdx = idx * 2 + 1;
            if (childIdx > last) return;
            int k = data[idx];
            int kChild = data[childIdx];
            int kChildp1 = data[childIdx+1];

			if(logger.isDebugEnabled()) {
				logger.debug("idx=" + idx + ", childIdx=" + childIdx + ", last=" + last);
				logger.debug("k=" + k + ", ib[k]=" + ib[k] + ", an[k]=" + indexNode[ia[k]] + ", bn[k]=" + indexNode[ib[k]] + ", nodeLabels[ib[k]]=" + nodeLabels[ib[k]]);
				logger.debug("kChild=" + kChild + ", ib[kChild]=" + ib[kChild] + ", an[kChild]=" + indexNode[ia[kChild]] + ", bn[kChild]=" + indexNode[ib[kChild]] + ", nodeLabels[ib[kChild]]=" + nodeLabels[ib[kChild]]);
				logger.debug("kChildp1=" + kChildp1 + ", ib[kChildp1]=" + ib[kChildp1] + ", an[kChildp1]=" + indexNode[ia[kChildp1]] + ", bn[kChildp1]=" + indexNode[ib[kChildp1]] + ", nodeLabels[ib[kChildp1]]=" + nodeLabels[ib[kChildp1]]);
				logger.debug("nodeLabels[ib[kChildp1]] - nodeLabels[ib[kChild]]=" + (nodeLabels[ib[kChildp1]] - nodeLabels[ib[kChild]]) );
				logger.debug("nodeLabels[ib[kChild]] - nodeLabels[ib[k]]=" + (nodeLabels[ib[kChild]] - nodeLabels[ib[k]]) );
			}
			if ((childIdx + 1) <= last && (nodeLabels[ib[kChildp1]] - nodeLabels[ib[kChild]] < -COMPARE_EPSILON)) {
                childIdx = childIdx + 1;
                kChild = data[childIdx];
            }
            else if ((childIdx + 1) <= last && (nodeLabels[ib[kChildp1]] - nodeLabels[ib[kChild]] <= COMPARE_EPSILON) && (indexNode[ib[kChildp1]] < indexNode[ib[kChild]])) {
                childIdx = childIdx + 1;
                kChild = data[childIdx];
            }
            
			if (nodeLabels[ib[kChild]] - nodeLabels[ib[k]] < -COMPARE_EPSILON) {
				swap(idx, childIdx);
				percolateDown(childIdx);
			}
            else if ((nodeLabels[ib[kChild]] - nodeLabels[ib[k]] <= COMPARE_EPSILON) && (indexNode[ib[kChild]] < indexNode[ib[k]])) {
                swap(idx, childIdx);
                percolateDown(childIdx);
            }
        }


        public void swap(int idx1, int idx2) {
            int temp = data[idx1];
            data[idx1] = data[idx2];
            data[idx2] = temp;
        }


		//Print heap contents to console (note in sorted order)
        public void dataPrint() {
            int k;

            for (int i = 0; i <= last; i++) {
                k = data[i];
                logger.debug("i=" + i + ", k=" + k + ", ib[k]=" + ib[k] + ", an=" + indexNode[ia[k]] + ", bn=" + indexNode[ib[k]] + ", nodeLabels[ib]=" + nodeLabels[ib[k]] + ", nodeLabeled[ib]=" + nodeLabeled[ib[k]]);
            }
            logger.debug("");
        }


        public void dataPrint( boolean isTrue ) {
            int k;

            for (int i = 0; i <= last; i++) {
                k = data[i];
                spDebugLogger.info("[heap]:  i=" + i + ", k=" + k + ", ib[k]=" + ib[k] + ", an=" + indexNode[ia[k]] + ", bn=" + indexNode[ib[k]] + ", nodeLabels[ib]=" + nodeLabels[ib[k]] + ", nodeLabeled[ib]=" + nodeLabeled[ib[k]]);
            }
            spDebugLogger.info("");
        }


        /*
        //Used to test as a stand alone class
        static final int initData[] =  { 10, 3, 5, 6, 2, 7, 1 };

        public static void main(String argv[]) {
        	logger.info("Initial data:");
            for (int i = 0; i < initData.length; i++) {
                logger.info(initData[i] + ", ");
            }
            logger.info();

            Heap heap = new Heap(initData);
            heap.dataPrint();

            heap.add(8);
            heap.dataPrint();

            int min = heap.removeMin();
            heap.dataPrint();

            heap.add(9);
            heap.dataPrint();

            heap.add(4);
            heap.dataPrint();

            min = heap.removeMin();
            heap.dataPrint();
        }
    */
    }

}

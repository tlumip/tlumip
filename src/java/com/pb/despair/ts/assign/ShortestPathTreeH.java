package com.pb.despair.ts.assign;

import java.util.Arrays;
import org.apache.log4j.Logger;

import com.pb.common.util.*;

/**
 * Class for shortest path trees.
 *
 */

public class ShortestPathTreeH {

	protected static Logger logger = Logger.getLogger("com.pb.despair.ts.assign");

    static final double COMPARE_EPSILON = 1.0e-07;

    Justify myFormat = new Justify();
    Network g;

    int inOrigin;

	int[] ia;
	int[] ib;
	int[] ip;
	int[] sortedLinkIndex;
	int[] indexNode;
	int[] nodeIndex;
	boolean[] centroid;
	boolean[] validLink;
	double[] linkCost;
	double[] aonFlow;	
	
	
    int[] nodeLabeled;
    double[] nodeLabels;
    int[] predecessorLink;


    long initTime = 0;
    long buildTime = 0;
    long loadTime = 0;

    Heap candidateHeap;
    int[] heapContents;


    public ShortestPathTreeH ( Network g ) {
        this.g = g;


		// store network fields in local arrays
		ia = g.getIa();
		ib = g.getIb();
		ip = g.getIpa();
		sortedLinkIndex = g.getSortedLinkIndexA();
		indexNode = g.getIndexNode();
		nodeIndex = g.getNodeIndex();
		centroid = g.getCentroid();
		
		aonFlow = new double[g.getLinkCount()];
        
        nodeLabeled = new int[g.getNodeCount()+1];
        nodeLabels = new double[g.getNodeCount()+1];

        //Create a new heap structure to sort candidate node labels
        candidateHeap = new Heap(g.getNodeCount()+1);
		heapContents = new int[g.getNodeCount()];

    }

    private void initData() {

        long start = System.currentTimeMillis();

        Arrays.fill(nodeLabeled, 0);
        Arrays.fill(nodeLabels, 1.0e+99);
		nodeLabels[inOrigin] = 0.0;
		nodeLabeled[inOrigin] = 1;

		predecessorLink = new int[g.getNodeCount()+1];
        Arrays.fill(predecessorLink, -1);

        candidateHeap.clear();

        initTime += (System.currentTimeMillis() - start);

    }


    public void buildTree(int inOrigin) {
        
		long start = System.currentTimeMillis();

		this.inOrigin = inOrigin;
		
        initData();

        // set labels for links eminating from the origin node
        setRootLabels (g, inOrigin);

        // continue labeling until candidateHeap is empty
		int k;
        while ((k = candidateHeap.remove()) >= 0) {

            setRootLabels (g, ib[k]);
			nodeLabeled[ib[k]] = 1;
			if(logger.isDebugEnabled()) {
                candidateHeap.dataPrint();
            }
        }

        buildTime += (System.currentTimeMillis() - start);
    }


    private void setRootLabels (Network g, int rootNode) {

        int k;
        double label;
        double turnPenalty;

        
		if(logger.isDebugEnabled()) {
            logger.debug ("rootNode=" + indexNode[rootNode] +"(external node label)" + ", ip[" + rootNode + "]=" + ip[rootNode] + ", ip[" + (rootNode+1) + "]=" + ip[(rootNode+1)]);
        }
		
        for (int i=ip[rootNode]; i < ip[rootNode+1]; i++) {
            
            k = sortedLinkIndex[i];
            
            
            turnPenalty = 0.0;
            if ( predecessorLink[ia[k]] >= 0)
                turnPenalty = g.getTurnPenalty( indexNode[ia[k]], indexNode[ia[predecessorLink[ia[k]]]], indexNode[ib[k]] );

			if(logger.isDebugEnabled()) {
				logger.debug ("i=" + i + ", k=" + k + ", ia[k=" + k + "]=" + ia[k] + ", ib[k=" + k + "]=" + ib[k] + ", an[k=" + k + "]=" + indexNode[ia[k]] + ", bn[k=" + k + "]=" + indexNode[ib[k]] + ", linkCost[k=" + k + "]=" + linkCost[k] +  ", nodeLabeled[ib[k]=" + ib[k] + "]=" +  nodeLabeled[ib[k]] +  ", nodeLabels[ib[k]=" + ib[k] + "]=" + nodeLabels[ib[k]] + ", validLink[k=" + k + "]=" + validLink[k] + ", turnPenalty=" + turnPenalty);
			}
            
			if ( validLink[k] && turnPenalty >= 0 ) {
                if (nodeLabeled[ib[k]] == 0) {
                    label = linkCost[k] + nodeLabels[ia[k]] + turnPenalty;
                    if (label - nodeLabels[ib[k]] < -COMPARE_EPSILON) {
                        nodeLabels[ib[k]] = label;
                        if (!centroid[k] || rootNode == inOrigin) {
                            candidateHeap.add(k);
                        }
                        predecessorLink[ib[k]] = k;
                        if(logger.isDebugEnabled()) {
                            logger.debug ("predecessor[" + indexNode[ib[k]] + "]=" + k + "   (" + indexNode[ia[k]] + "," + indexNode[ib[k]] + ")");
                        }
                    }
                }
            }
        }
        if(logger.isDebugEnabled()) {
            logger.debug ("");
        }
    }



    /**
     * Load trips from the trip table row associated with the shortest
     * path tree origin
     */
    public double[] loadTree ( double[] tripRow  ) {

        long start = System.currentTimeMillis();

        Arrays.fill (aonFlow, 0.0);
        
		int k;
        for (int j=0; j < g.getNumCentroids(); j++) {
            if ( tripRow[j] > 0 && j != inOrigin ) {
                k = predecessorLink[j];
                if (k == -1) {
                    logger.fatal ("inOrigin=" + inOrigin + ", j=" + j + ", k=" + k);
                    System.exit(-1);
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
     * Trace the shortest path tree to get distance skims to all
     * destination zones from this origin.
     */
    public double[] getSkim () {
        
        int k;
        double minSkim = 1.0e+99d;
		double[] skim = new double [g.getNumCentroids()];


        for (int j=0; j < g.getNumCentroids(); j++) {
            if (j != inOrigin) {
                k = predecessorLink[j];
                if (k == -1) {
                    logger.info ("invalid predecessorLink: inOrigin=" + inOrigin + ", j=" + j + ", k=" + k);
                    System.exit(-1);
                }
				skim[j] += linkCost[k];
                while (ia[k] != inOrigin) {
                    k = predecessorLink[ia[k]];
                    if (k == -1) {
                        logger.info ("invalid predecessorLink: inOrigin=" + inOrigin + ", j=" + j + ", k=" + k);
                        System.exit(-1);
                    }
					skim[j] += linkCost[k];
                }
                // keep track of minimum distance skim for use in setting intrazonal skim value.
                if (skim[j] < minSkim)
                    minSkim = skim[j];
            }
        }

/*        
        // intrazonal distance skim equals 1/2 the nearest neighbor skim.
		skim[inOrigin] = 0.5*minSkim;
*/
        
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
			logger.info (myFormat.left((Integer.toString(indexNode[ia[k]]) + "," + Integer.toString(indexNode[ib[k]])), 15) + myFormat.right(linkCost[k], 12, 4) + myFormat.right(cumTime, 12, 4));
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

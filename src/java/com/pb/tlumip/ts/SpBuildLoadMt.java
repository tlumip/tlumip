package com.pb.tlumip.ts;


import org.apache.log4j.Logger;

import com.pb.common.rpc.DBlockingQueue;


/**
 * Class for shortest path tree building and loading to be run
 * in parallel by multiple threads in a vm.
 *
 * The run method of this class will be executed by a thread.
 */
public class SpBuildLoadMt implements Runnable
{
    protected static Logger logger = Logger.getLogger(SpBuildLoadMt.class);
    
    private static Object objLock = new Object();

    private ShortestPathTreeH[] sp;
    private SpBuildLoadCommon spBuildLoadShared;
    
    private int numUserClasses;
    private int numLinks;

    
    // a distributed queue common to all instances of this class running in different
    // threads and possibly in different VMs hold packets of work elements to be processed, i.e. int[][userClass, originTaz].
    public SpBuildLoadMt ( SpBuildLoadCommon spBuildLoadShared ) {

        // SpBuildLoadCommon is a singleton created by the class which creates one or more instances of this class
        // and is therefore common to each of these instances.
        this.spBuildLoadShared = spBuildLoadShared;
        
        
        // get the network info needed to create ShortestPathTreeH objects.
        NetworkHandlerIF nh = spBuildLoadShared.getNetworkHandler();
        
        numUserClasses = nh.getNumUserClasses();
        numLinks = nh.getLinkCount();
        boolean[][] validLinksForClasses = nh.getValidLinksForAllClasses();
        double[] linkCost = nh.setLinkGeneralizedCost();
        
        // a ShortestPathTreeH object for each user class is created and initialized
        // so that any of them can be used to process work elements. 
        sp = new ShortestPathTreeH[numUserClasses];

        for (int i=0; i < numUserClasses; i++) {
            sp[i] = new ShortestPathTreeH( nh );
            sp[i].setValidLinks( validLinksForClasses[i] );
            sp[i].setLinkCost( linkCost );
        }
        
    }

    

    // this method is run by threads created by spBuildLoadHandlers.
    // it gets a packet of work elements, i.e. an array of user class and origin taz pairs, and
    // builds shorrtest path trees and loads trips from the trip table on links of those trees.
    // the method accumulates aon flows for links in the SpBuildLoadCommon class before exiting.
    public void run () {
        
        int userClass = 0;
        int origin = 0;
        double[] originTrips = null;
        
        int[][] workPacket = null;
        double[][] cumulativeAonFlowsThread = new double[numUserClasses][numLinks];
        

        // The objects in the workQueue are work packets of work elements: int[][2] arrays, with the first element
        // the user class index, and the second element the origin zone index.
        DBlockingQueue workList = spBuildLoadShared.getWorkList();
        

        while ( true ) {
            
            try {
                // The next line blocks until a work packet has been put on the queue.
                workPacket = (int[][])workList.take();
            }
            catch (Exception e) {
                logger.error("exception caught in SpBuildLoadMt.run() retrieving work packet from distributed queue.", e);
            }
            
            
            // process work elements in the packet
            for (int i=0; i < workPacket.length; i++) {
                userClass = workPacket[i][0];
                origin = workPacket[i][1];
                originTrips = spBuildLoadShared.getTripTableRow(userClass, origin);
                
                double[] aonFlows = sp[userClass].buildAndLoadTrees ( userClass, origin, originTrips );

                for (int k=0; k < numLinks; k++)
                    cumulativeAonFlowsThread[userClass][k] += aonFlows[k];
                
            }


            // at this point, the packet of shortest path trees taken by this thread have been loaded,
            // so update these acumulated link flows in the common results array for the VM this thread runs on.
            synchronized (objLock) {

                // get the results array from the shared object and update its values with
                // the aon flows calculated in this thread.
                double[][] cumulativeAonFlowsShared = spBuildLoadShared.getResultsArray();
                
                for (int m=0; m < cumulativeAonFlowsThread.length; m++)
                    for (int k=0; k < cumulativeAonFlowsThread[m].length; k++)
                        cumulativeAonFlowsShared[m][k] += cumulativeAonFlowsThread[m][k];
                
                // update completed packet count
                spBuildLoadShared.updateCompletedPacketCount();
            }
            
        }
        
    }

}

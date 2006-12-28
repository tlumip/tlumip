package com.pb.tlumip.ts;


import com.pb.common.rpc.DBlockingQueue;

import org.apache.log4j.Logger;


/**
 * Class for shortest path tree building and loading to be run
 * in parallel by multiple threads in a vm.
 *
 * The run method of this class will be executed by a thread.
 */
public class SpBuildLoadMt implements Runnable
{
    protected static Logger logger = Logger.getLogger(SpBuildLoadMt.class);
    
    private int threadId;
    private DBlockingQueue workQueue;
    
    private ShortestPathTreeH[] sp;
    private SpBuildLoadCommon spBuildLoadShared;
    
    private int numUserClasses;
    private int numLinks;

    
    // a distributed queue common to all instances of this class running in different
    // threads and possibly in different VMs hold packets of work elements to be
    // processed, i.e. int[][userClass, originTaz].
    public SpBuildLoadMt ( int threadId, SpBuildLoadCommon spBuildLoadShared, DBlockingQueue workQueue ) {
        
        this.threadId = threadId;
        
        // The objects in the workQueue are work packets of work elements: int[][2] arrays, with the first element
        // the user class index, and the second element the origin zone index.
        this.workQueue = workQueue;

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
    // this method trys to get packets from the workQueue until it gets a null packet, at which
    // time the workQueue has been emptied, and this method can finalize its results and return. 
    public void run () {
        
        int userClass = 0;
        int origin = 0;
        double[] originTrips = null;
        
        int[][] workPacket = null;
        double[][] cumulativeAonFlowsThread = new double[numUserClasses][numLinks];
        
        int count = 0;
        int packetCount = 0;

        double sum = 0.0;
        double totalFlow = 0.0;
        boolean workPacketsRemain = true;
        while ( workPacketsRemain ) {
            
            try {
                // The next line blocks until a work packet can be taken off the queue.
                workPacket = (int[][])workQueue.take();
            }
            catch (Exception e) {
                logger.error("exception caught in SpBuildLoadMt.run() retrieving work packet from distributed workQueue.", e);
            }
            
            
            // if the workPacket has length 0, there will be no more work elements in the queue,
            // so finalize results and return.
            if ( workPacket.length == 0 ) {
                workPacketsRemain = false;
                logger.info( " thread " + threadId + " on " + spBuildLoadShared.getHandlerName() + " took an empty packet." );
                continue;
            }
            
            packetCount++;
            
            // process work elements in the packet
            for (int i=0; i < workPacket.length; i++) {
                userClass = workPacket[i][0];
                origin = workPacket[i][1];
                originTrips = spBuildLoadShared.getTripTableRow(userClass, origin);
                
                sum = 0.0;
                for (int j=0; j < originTrips.length; j++)
                    sum += originTrips[j];
                
                double[] aonFlows = sp[userClass].buildAndLoadTrees ( userClass, origin, originTrips );

                for (int k=0; k < numLinks; k++) {
                    cumulativeAonFlowsThread[userClass][k] += aonFlows[k];
                    totalFlow += aonFlows[k];
                }
                      
                count++;
            }
            
            if ( logger.isDebugEnabled() )
                logger.debug( count + ": " + ", origin=" + origin + ", sum=" + sum + ", totalFlow=" + totalFlow );

        }

        spBuildLoadShared.setResultsArray( threadId, cumulativeAonFlowsThread );
        
        spBuildLoadShared.setPacketsCompletedByThread ( threadId, packetCount );
        
        logger.info( totalFlow + " total link flow from " + packetCount + " packets assigned by thread " + threadId + " on " + spBuildLoadShared.getHandlerName() );
        
    }

}

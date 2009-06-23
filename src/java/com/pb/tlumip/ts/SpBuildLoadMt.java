package com.pb.tlumip.ts;


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
    private int numUserClasses;
    private int numLinks;
    
    private SpBuildLoadCommon spBuildLoadShared;
    private ShortestPathTreeH[] sp;
    

    
    // a distributed queue common to all instances of this class running in different
    // threads and possibly in different VMs hold packets of work elements to be
    // processed, i.e. int[][userClass, originTaz].
    public SpBuildLoadMt ( int threadId, SpBuildLoadCommon spBuildLoadShared ) {
        
        this.threadId = threadId;
        
        // SpBuildLoadCommon is a singleton created by the class which creates one or more instances of this class
        // and is therefore common to each of these instances.
        this.spBuildLoadShared = spBuildLoadShared;
        
        
        this.numUserClasses = spBuildLoadShared.getNumUserClasses();
        this.numLinks = spBuildLoadShared.getNumLinks();
        
        // a ShortestPathTreeH object for each user class is created and initialized
        // so that any of them can be used to process work elements. 
        sp = spBuildLoadShared.getShortestPathTreeHObjects( threadId );

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
        
        double[][] cumulativeAonFlowsThread = new double[numUserClasses][numLinks];

        int[][] workElements = spBuildLoadShared.getWorkElements(this.threadId);
        
        double sum = 0.0;
        double totalFlow = 0.0;
        for ( int i=0; i < workElements.length; i++ ) {
            
            userClass = workElements[i][0];
            origin = workElements[i][1];
            
            originTrips = spBuildLoadShared.getElementDemand(this.threadId, i);
            
            sum = 0.0;
            for (int j=0; j < originTrips.length; j++)
                sum += originTrips[j];
            
            double[] aonFlows = sp[userClass].buildAndLoadTrees ( userClass, origin, originTrips );

            for (int k=0; k < numLinks; k++) {
                cumulativeAonFlowsThread[userClass][k] += aonFlows[k];
                totalFlow += aonFlows[k];
            }
                  
            spBuildLoadShared.setShortestPathTree( userClass, origin, sp[userClass].getPredecessorLink() );

            if ( logger.isDebugEnabled() )
                logger.debug( i + ": " + ", origin=" + origin + ", sum=" + sum + ", totalFlow=" + totalFlow );

        }

        spBuildLoadShared.setResultsArray( threadId, cumulativeAonFlowsThread );
        
        spBuildLoadShared.setPacketsCompletedByThread ( threadId, workElements.length );
        
        logger.info( totalFlow + " total link flow from " + workElements.length + " packets assigned by thread " + threadId + " on " + spBuildLoadShared.getHandlerName() );
        
    }

}

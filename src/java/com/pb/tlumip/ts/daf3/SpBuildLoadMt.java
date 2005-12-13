package com.pb.tlumip.ts.daf3;


import java.util.concurrent.BlockingQueue;

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
    
    protected static Object objLock = new Object();

    ShortestPathTreeH[] sp;
    AonBuildLoadCommon spBuildLoadShared;
    int numUserClasses;

    // Pass in the workList so all instances of this class running in different
    // threads see the same workList and block accordingly
    public SpBuildLoadMt ( double[] linkCost, boolean[][] validLinksForClasses, AonBuildLoadCommon spBuildLoadShared )
    {
        
        this.spBuildLoadShared = spBuildLoadShared;
        
        numUserClasses = validLinksForClasses.length; 
        
        sp = new ShortestPathTreeH[numUserClasses];

        for (int i=0; i < numUserClasses; i++) {
            sp[i] = new ShortestPathTreeH();
            sp[i].setValidLinks( validLinksForClasses[i] );
            sp[i].setLinkCost( linkCost );
        }
        
    }

    

    public void run ()
    {
        int userClass = 0;
        int origin = 0;
        boolean last = false;
        double totalLoadedLinkFlow = 0.0;

        int numLinks = spBuildLoadShared.getNumLinks();
        
        double[][] cumulativeAonFlowsThread = new double[numUserClasses][numLinks];
        
        while (! last) {

            // The objects in the workList are int[2] arrays, with the first element
            // the origin zone index, and the second element the user class index.
            BlockingQueue workList = spBuildLoadShared.getWorkList();
            
            int[] workObject = null;
            try {
            
                // The next line blocks until an item is put in the list and
                // assumes that no other waiting thread gets it first.
                workObject = (int[])workList.take();
                
                // If the origin zone index == -1, then no more work remains, and this thread can end.
                if ( workObject[0] == -1 ) {
                    last = true;
                }
                else {
                    origin = workObject[0];
                    userClass = workObject[1];
                }
                
            } catch (InterruptedException e) {
                logger.error ( "InterruptedException caught getting work object from work list in SpBuildLoadMt.run().", e);
                System.exit(-1);
            }

            double[] aonFlows = sp[userClass].buildAndLoadTrees ( userClass, origin );
            
            for (int k=0; k < numLinks; k++) {
                cumulativeAonFlowsThread[userClass][k] += aonFlows[k];
                totalLoadedLinkFlow += aonFlows[k];
            }

            if(logger.isDebugEnabled()) {
                logger.debug("Sum of aon link flows accumulated for origin = " + origin + ", userclass = " + userClass + " = " + totalLoadedLinkFlow );
            }

        }

        // at this point, all shortest path trees have been loaded, so update the loaded flow
        // array for flows generated in this thread.
        synchronized (objLock) {

            // get the results array from the shared object and update its values with
            // the aon flows calculated in this thread.
            double[][] cumulativeAonFlowsShared = spBuildLoadShared.getResultsArray();
            
            for (int m=0; m < cumulativeAonFlowsThread.length; m++)
                for (int k=0; k < cumulativeAonFlowsThread[m].length; k++)
                    cumulativeAonFlowsShared[m][k] += cumulativeAonFlowsThread[m][k];
            
            spBuildLoadShared.incrementThreadsFinished();
            
        }
        
    }

}

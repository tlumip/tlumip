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

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 6/30/2004
 */


import com.pb.tlumip.ts.daf3.AonBuildLoadCommon;
import com.pb.tlumip.ts.daf3.SpBuildLoadMt;

import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;



public class SpBuildLoadHandler {

    protected static Logger logger = Logger.getLogger(SpBuildLoadHandler.class);

    // set the frequency with which the shared class is polled to see if all threads have finshed their work.
    static final int POLLING_FREQUENCY_IN_SECONDS = 10;
    public static String remoteHandlerName = "spBuildLoadHandler";
    
    int numberOfThreads;
    


	public SpBuildLoadHandler() {

    }


    public double[][] getLoadedAonFlows ( int[][] workElements ) {


        // generate a NetworkHandler object to use for assignments and skimming
        NetworkHandlerIF nh = NetworkHandler.getInstance();
        
        // update the link costs based on current flows
        double[] linkCost = null;
        boolean[][] validLinksForClasses = null;
        try {
            validLinksForClasses = nh.getValidLinksForAllClasses();
            linkCost = nh.setLinkGeneralizedCost();
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught setting link generalized costs prior to loading aon flows in SpBuildLoadHandler.getLoadedAonFlows().", e );
            System.exit(1);
        }

        
        AonBuildLoadCommon buildLoadShared = AonBuildLoadCommon.getInstance();
        buildLoadShared.setup(validLinksForClasses);
        
        BlockingQueue workList = buildLoadShared.getWorkList();
        
        
        // put the origin, user class pairs into the workList.
        // put a -1, -1 pair for each thread to be started at the end of the workList.
        try {
            
            for (int i=0; i < workElements.length; i++)
                workList.put( workElements[i] );
        
            int[] endOfWorkMarker = { -1, -1 };
            for (int i=0; i < numberOfThreads; i++)
                workList.put( endOfWorkMarker );
        
        } catch (InterruptedException e) {
            logger.error ( "InterruptedException caught putting work object into workList in SpBuildLoadHandler.getLoadedAonFlows().", e);
            System.exit(-1);
        }

        
        // start the specified number of threads to build and load shortest path trees from the workList
        for (int i = 0; i < numberOfThreads; i++) {
            SpBuildLoadMt spMt = new SpBuildLoadMt( linkCost, validLinksForClasses, buildLoadShared );
            new Thread(spMt).start();
        }
        
        
        // wait here until all threads have indicated they are done.
        while ( buildLoadShared.getThreadsFinishedCount() < numberOfThreads ) {
            try {
                Thread.sleep( POLLING_FREQUENCY_IN_SECONDS*1000 );
            }
            catch (InterruptedException e){
                logger.error ( "InterruptedException thrown while waiting " + POLLING_FREQUENCY_IN_SECONDS + " seconds.", e);
            }
        }
        
        return buildLoadShared.getResultsArray();
        
    }

}

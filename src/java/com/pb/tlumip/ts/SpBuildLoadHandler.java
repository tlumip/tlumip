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


import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcHandler;

import com.pb.tlumip.ts.daf3.AonBuildLoadCommon;
import com.pb.tlumip.ts.daf3.SpBuildLoadMt;

import java.util.Vector;
import java.net.MalformedURLException;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;



public class SpBuildLoadHandler implements RpcHandler {

    protected static Logger logger = Logger.getLogger(SpBuildLoadHandler.class);

    // set the frequency with which the shared class is polled to see if all threads have finshed their work.
    static final int POLLING_FREQUENCY_IN_SECONDS = 10;
    public static String remoteHandlerName = "spBuildLoadHandler";
    
    int numberOfThreads;
    
    RpcClient networkHandlerClient;    

    


	public SpBuildLoadHandler() {

        String handlerName = null;
        
        try {
            
            //Create RpcClients this class connects to
            try {
                handlerName = NetworkHandler.remoteHandlerName;
                networkHandlerClient = new RpcClient( handlerName );
            }
            catch (MalformedURLException e) {
                logger.error ( "MalformedURLException caught in ShortestPathTreeH() while defining RpcClients.", e );
            }

        }
        catch ( Exception e ) {
            logger.error ( "Exception caught in ShortestPathTreeH().", e );
            System.exit(1);
        }

    }


    public Object execute (String methodName, Vector params) throws Exception {

        logger.info( "method name = SpBuildHandler.execute()." );

        if ( methodName.equalsIgnoreCase( "getLoadedAonFlows" ) ) {
            numberOfThreads = (Integer) params.get(0);
            int[][] workElements = (int[][]) params.get(1);
            logger.info( remoteHandlerName + " to build and load " + workElements.length + " shortest path trees on " + numberOfThreads + " threads." );
            return getLoadedAonFlows ( workElements );
        }
        else {
            logger.error ( "method name " + methodName + " called from remote client is not registered for remote method calls.", new Exception() );
            return 0;
        }
        
    }


    
    private double[][] getLoadedAonFlows ( int[][] workElements ) {


        // update the link costs based on current flows
        double[] linkCost = null;
        boolean[][] validLinksForClasses = null;
        try {
            validLinksForClasses = networkHandlerGetValidLinksForAllClassesRpcCall();
            linkCost = networkHandlerSetLinkGeneralizedCostRpcCall();
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

    
    
    private double[] networkHandlerSetLinkGeneralizedCostRpcCall() throws Exception {
        // g.setLinkGeneralizedCost()
        return (double[])networkHandlerClient.execute("networkHandler.setLinkGeneralizedCost", new Vector() );
    }

    private boolean[][] networkHandlerGetValidLinksForAllClassesRpcCall() throws Exception {
        // g.getValidLinksForAllClasses()
        return (boolean[][])networkHandlerClient.execute("networkHandler.getValidLinksForAllClasses", new Vector() );
    }

}

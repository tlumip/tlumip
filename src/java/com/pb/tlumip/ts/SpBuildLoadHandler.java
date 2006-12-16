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


import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

import com.pb.common.rpc.DafNode;

import org.apache.log4j.Logger;



public class SpBuildLoadHandler implements SpBuildLoadHandlerIF {

    protected static Logger logger = Logger.getLogger(SpBuildLoadHandler.class);

    private HashMap controlMap;
    private HashMap resultsMap;
    private BlockingQueue workQueue;
    
    private SpBuildLoadCommon spCommon = null;
    
    static String rpcConfigFile = null;
    
    private int numberOfThreads = java.lang.Runtime.getRuntime().availableProcessors();
    


	public SpBuildLoadHandler() {
    }

    
    // Factory Method to return either local or remote instance
    public static SpBuildLoadHandlerIF getInstance( String rpcConfigFile ) {
    
        SpBuildLoadHandler.rpcConfigFile = rpcConfigFile;
        
        if ( rpcConfigFile == null ) {

            // if rpc config file is null, then all handlers are local, so return local instance
            return new SpBuildLoadHandler();

        }
        else {
            
            // return either a local instance or an rpc instance depending on how the handler was defined.
            Boolean isLocal = DafNode.getInstance().isHandlerLocal( HANDLER_NAME );

            if ( isLocal == null )
                // handler name not found in config file, so create a local instance.
                return new SpBuildLoadHandler();
            else if ( isLocal )
                // handler name found in config file and is local, so create a local instance.
                return new SpBuildLoadHandler();
            else 
                // handler name found in config file but is not local, so create an rpc instance.
                return new SpBuildLoadHandlerRpc( rpcConfigFile );

        }
        
    }
    

    // Factory Method to return local instance only
    public static SpBuildLoadHandlerIF getInstance() {
        return new SpBuildLoadHandler();
    }
    
    
    
    public int setup(double[][][] tripTables, NetworkHandlerIF nh, BlockingQueue workQueue, HashMap controlMap, HashMap resultsMap ) {

        this.workQueue = workQueue;
        this.controlMap = controlMap;
        this.resultsMap = resultsMap;
        
        spCommon = SpBuildLoadCommon.getInstance();
        
        // ShortestPathHandlers assume that a NetworkHandler and a DemandHandler are running either in the same VM or as an RPC server
        spCommon.setup( numberOfThreads, nh.getNumUserClasses(), nh.getLinkCount(), tripTables, nh, controlMap );
        
        return 1;
    }


    public int start() {
        
        // start the specified number of threads to build and load shortest path trees from the workList
        for (int i = 0; i < numberOfThreads; i++) {
            SpBuildLoadMt spMt = new SpBuildLoadMt( i, spCommon, workQueue );
            new Thread(spMt).start();
        }
        
        // wait for a signal to be set in the controlMap, then update resultsMap.
        waitForSignalThenWriteResults();

        return 1;
        
    }
    

    private void waitForSignalThenWriteResults() {
        
        // wait here until all packets have been computed by distributed processes.
        // AonFlowHandler is watching the number of completed packets and will set a signal when they're all finished.
        try {
        
            boolean compileResults = false;
            while ( ! compileResults ) {
                compileResults = (Boolean)controlMap.get( AonFlowHandler.COMPILE_RESULTS_SIGNAL );
                try {
                    Thread.sleep( AonFlowHandler.POLLING_FREQUENCY_IN_SECONDS*1000 );
                }
                catch (InterruptedException e){
                    logger.error ( "", e);
                }
            }
            
            getResultsAndUpdateResultsMap();
            
        }
        catch (Exception e) {
            logger.error ( "Exception thrown while waiting " + AonFlowHandler.POLLING_FREQUENCY_IN_SECONDS + " seconds to see if COMPILE_RESULTS signal has been set.", e);
        }
        
        
        
        // at this point, all link flows have been written by this SpBuildLoadHandler to the resultsMap.
        // get the number of packets processed by this handler and add it to the COMPILED_RESULTS value
        // that other handlers are also updating so the AonFlowHandler will know when results for all
        // work packets have been updated to the resultsMap.
        try {
            int value = (Integer)controlMap.get( AonFlowHandler.NUM_COMPILED_RESULTS_SIGNAL ) + spCommon.getPacketsCompletedCount();
            controlMap.put( AonFlowHandler.NUM_COMPILED_RESULTS_SIGNAL, value );
        }
        catch (Exception e) {
            logger.error ("exception thrown updating number of packets completed count by this SpBuildLoadHandler into controlMap.", e);
        }

    }


    private void getResultsAndUpdateResultsMap() {
        
        // at this point, all work packets have been completed by threads created on this and possibly many other VMs.
        // the results accumulated in spBuildLoadCommon in this VM will be accumulated by AonFlowHanlder.
        
        double[][] aonFlows = spCommon.getResultsArray( 0 );
        for (int i=1; i < numberOfThreads; i++) {
            double[][] threadFlows = spCommon.getResultsArray( i );
            for (int m=0; m < aonFlows.length; m++) {
                for (int k=0; k < aonFlows[m].length; k++) {
                    aonFlows[m][k] += threadFlows[m][k];
                }
            }
        }
        
        double total = 0.0;
        for (int m=0; m < aonFlows.length; m++) {
            for (int k=0; k < aonFlows[m].length; k++) {
                total += aonFlows[m][k];
            }
        }
        logger.info( total + " total link flows assigned by SpBuildLoadHnandler." );
        

        int m=0;
        int k=0;
        String key = "";
        double value = 0.0;
        for (m=0; m < aonFlows.length; m++) {
            for (k=0; k < aonFlows[m].length; k++) {
                key = m + "_" + k;
                try {
                    value = (Double)resultsMap.get(key);
                }
                catch (Exception e) {
                    value = 0.0;
                }
                resultsMap.put(key, value + aonFlows[m][k]);
            }
        }
        
    }
    
}

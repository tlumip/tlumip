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


import org.apache.log4j.Logger;

import com.pb.common.rpc.DHashMap;



public class SpBuildLoadHandler {

    protected static Logger logger = Logger.getLogger(SpBuildLoadHandler.class);

    public static final String HANDLER_NAME = "spBuildLoadHandler";
    
    private DHashMap controlMap = new DHashMap( AonFlowHandler.CONTROL_MAP_NAME );
    private DHashMap resultsMap = new DHashMap( AonFlowHandler.RESULTS_MAP_NAME );
    
    private SpBuildLoadCommon spCommon = null;
    
    private int numberOfThreads = 1;
    


	public SpBuildLoadHandler() {

        // setup method sets up singleton class common to the objects created to run on multiple threads 
	    setup();
        
        
        // create the objects and start their run() methods 
        startThreads();
        
        
        // wait for a signal to be set in the controlMap, then update resultsMap.
        waitForSignalThenWriteResults();
        
    }

    
    private void setup() {

        spCommon = SpBuildLoadCommon.getInstance();
        
        // ShortestPathHandlers assume that a NetworkHandler and a DemandHandler are running either in the same VM or as an RPC server
        spCommon.setup( NetworkHandler.getInstance(), DemandHandler.getInstance() );
        
    }


    private void startThreads() {
        
        // start the specified number of threads to build and load shortest path trees from the workList
        for (int i = 0; i < numberOfThreads; i++) {
            SpBuildLoadMt spMt = new SpBuildLoadMt( spCommon );
            new Thread(spMt).start();
        }
        
    }
    


    private void waitForSignalThenWriteResults() {
        
        // wait here until all packets have been computed by distributed processes.
        // AonFlowHandler is watching the number of completed packets and will set a signal when they're all finished.
        try {
        
            while ( ! (Boolean)controlMap.get( AonFlowHandler.COMPILE_RESULTS_SIGNAL ) ) {
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
        double[][] aonFlows = spCommon.getResultsArray();

        int m=0;
        int k=0;
        String key = "";
        double value = 0.0;
        try {
            for (m=0; m < aonFlows.length; m++) {
                for (k=0; k < aonFlows[m].length; k++) {
                    key = m + "_" + k;
                    value = (Double)resultsMap.get(key);
                    resultsMap.put(key, value + aonFlows[m][k]);
                }
            }
        }
        catch (Exception e) {
            logger.error ("exception thrown putting flows into resultsMap in SpBuildLoadHandler.  m=" + m + ", k=" + k + ", aonFlows[m][k]=" + aonFlows[m][k] + ", key=" + key + ", value=" + value + ".", e);
        }
        
    }
    
}

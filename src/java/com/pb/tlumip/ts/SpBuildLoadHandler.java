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


import com.pb.common.rpc.DBlockingQueue;
import com.pb.common.rpc.DHashMap;
import com.pb.common.rpc.DafNode;
import com.pb.common.rpc.RpcException;

import org.apache.log4j.Logger;



public class SpBuildLoadHandler implements SpBuildLoadHandlerIF {

    protected static Logger logger = Logger.getLogger(SpBuildLoadHandler.class);

    private String rpcConfigFile;
    private static String handlerName;
    
    private DHashMap controlMap;
    private DBlockingQueue workQueue;
    
    private double[][] aonFlows = null;
    private SpBuildLoadCommon spCommon = null;
    
    private int numberOfThreads = java.lang.Runtime.getRuntime().availableProcessors();
//    private int numberOfThreads = 1;
    


    public SpBuildLoadHandler() {
        SpBuildLoadHandler.handlerName = "";
        this.rpcConfigFile = "";
    }

    public SpBuildLoadHandler(String rpcConfigFile, String handlerName) {
        this.rpcConfigFile = rpcConfigFile;
        SpBuildLoadHandler.handlerName = handlerName;
    }

    
    // Factory Method to return either local or remote instance
    public static SpBuildLoadHandlerIF getInstance( String rpcConfigFile, String handlerName ) {
        
        if ( rpcConfigFile == null ) {

            // if rpc config file is null, then all handlers are local, so return local instance
            return new SpBuildLoadHandler( rpcConfigFile, "local" );

        }
        else {
            
            // return either a local instance or an rpc instance depending on how the handler was defined.
            Boolean isLocal = DafNode.getInstance().isHandlerLocal( handlerName );

            if ( isLocal == null ) {
                // handler name not found in config file, so create a local instance.
                return new SpBuildLoadHandler( rpcConfigFile, "local" );
            }
            else { 
                // handler name found in config file; since the handler was loaded by the framework, create an rpc instance.
                SpBuildLoadHandler.handlerName = handlerName;
                return new SpBuildLoadHandlerRpc( rpcConfigFile, handlerName );
            }

        }
        
    }
    

    // Factory Method to return local instance only
    public static SpBuildLoadHandlerIF getInstance() {
        return new SpBuildLoadHandler("", "local");
    }
    
    
    
    public int setup( double[][][] tripTables ) {

        spCommon = SpBuildLoadCommon.getInstance();
        
        // define data structures used to manage the distribution of work
        workQueue = new DBlockingQueue( AonFlowHandler.WORK_QUEUE_NAME );
        controlMap = new DHashMap( AonFlowHandler.CONTROL_MAP_NAME );

        // get a NetworkHandler instance.
        // if the instance has a null Network object, a local instance is to be used, and it can be retrieved from the controlMap,
        // where it was stored by AonFlowHandler.
        NetworkHandlerIF nh = NetworkHandler.getInstance(rpcConfigFile);
        
        if ( nh.getNetwork() == null ) {
            
            try {
                nh = (NetworkHandlerIF)controlMap.get ( NetworkHandler.HANDLER_NAME );
            } catch (RpcException e) {
                logger.error ("exception thrown getting NetworkHandler handle from controlMap.", e);
            }

        }

        
        // ShortestPathHandlers assume that a NetworkHandler and a DemandHandler are running either in the same VM or as an RPC server
        spCommon.setup( numberOfThreads, tripTables, nh );
        
        return 1;
    }


    public int start() {
        
        // start the specified number of threads to build and load shortest path trees from the workList, then return
        for (int i = 0; i < numberOfThreads; i++) {
            SpBuildLoadMt spMt = new SpBuildLoadMt( handlerName, i, spCommon, workQueue );
            new Thread(spMt).start();
        }
        
        return 1;
        
    }
    
    
    public double[][] getResults() {

        // at this point, all work packets have been completed by threads created on this and
        // possibly many other VMs.  Combine the results from the threads used by this handler.
        // These results will then be accumulated by AonFlowHandler.
        
        aonFlows = spCommon.getResultsForThread( 0 );
        for (int i=1; i < numberOfThreads; i++) {
            double[][] threadFlows = spCommon.getResultsForThread( i );
            for (int m=0; m < aonFlows.length; m++) {
                for (int k=0; k < aonFlows[m].length; k++) {
                    aonFlows[m][k] += threadFlows[m][k];
                }
            }
        }

        return aonFlows;
        
    }
    
 
    public boolean handlerIsFinished() {
        // check to see if all handler threads have completed.
        boolean result = false;
        if ( spCommon.getNumberOfThreadsCompleted() == numberOfThreads )
            result = true;
        
        return result;
    }

    
}

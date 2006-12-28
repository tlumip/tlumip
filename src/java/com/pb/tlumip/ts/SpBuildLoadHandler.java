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
import com.pb.common.rpc.DafNode;

import org.apache.log4j.Logger;



public class SpBuildLoadHandler implements SpBuildLoadHandlerIF {

    protected static Logger logger = Logger.getLogger(SpBuildLoadHandler.class);

    
    private DBlockingQueue workQueue;
    
    private double[][] aonFlows = null;
    private SpBuildLoadCommon spCommon = null;
    
    private int numberOfThreads = java.lang.Runtime.getRuntime().availableProcessors();
//    private int numberOfThreads = 1;
    


    public SpBuildLoadHandler() {
    }


    
    // Factory Method to return either local or remote instance
    public static SpBuildLoadHandlerIF getInstance( String rpcConfigFile, String handlerName ) {
        
        if ( rpcConfigFile == null ) {

            // if rpc config file is null, then all handlers are local, so return local instance
            return new SpBuildLoadHandler();

        }
        else {
            
            // return either a local instance or an rpc instance depending on how the handler was defined.
            Boolean isLocal = DafNode.getInstance().isHandlerLocal( handlerName );

            if ( isLocal == null ) {
                // handler name not found in config file, so create a local instance.
                return new SpBuildLoadHandler();
            }
            else { 
                // handler name found in config file; since the handler was loaded by the framework, create an rpc instance.
                return new SpBuildLoadHandlerRpc( rpcConfigFile, handlerName );
            }

        }
        
    }
    

    // Factory Method to return local instance only
    public static SpBuildLoadHandlerIF getInstance() {
        return new SpBuildLoadHandler();
    }
    
    
    
    // this method is called by local instances of SpBuildLoadHandler.
    public int setup( String handlerName, String rpcConfigFile, NetworkHandlerIF nh, DemandHandlerIF dh ) {

        spCommon = SpBuildLoadCommon.getInstance();
        
        // define data structures used to manage the distribution of work
        workQueue = new DBlockingQueue( AonFlowHandler.WORK_QUEUE_NAME );

        // a local instance made this call and is loaded in the same VM as this instance, so NetworkHandler and DemandHandler handles are passed in
        // and can be passed on by this handler.
        spCommon.setup( handlerName, numberOfThreads, nh, dh );
        
        return 1;
    }

    

    // this method is called by instances of SpBuildLoadHandlerRPC.
    public int setup( String handlerName, String rpcConfigFile ) {

        spCommon = SpBuildLoadCommon.getInstance();
        
        // define data structures used to manage the distribution of work
        workQueue = new DBlockingQueue( AonFlowHandler.WORK_QUEUE_NAME );

        // an rpc instance made this call, so create new instances of NetworkHandler and DemandHandler, which should both also be rpc instances
        // and will be used to make remote calls to handlers loaded in other VMs.
        NetworkHandlerIF nh = NetworkHandler.getInstance(rpcConfigFile);
        
        DemandHandlerIF dh = DemandHandler.getInstance(rpcConfigFile);
        
        // setup the singleton common to threads started in parallel by this handler
        spCommon.setup( handlerName, numberOfThreads, nh, dh );
        
        return 1;
    }


    public int start() {
        
        // start the specified number of threads to build and load shortest path trees from the workList, then return
        for (int i = 0; i < numberOfThreads; i++) {
            SpBuildLoadMt spMt = new SpBuildLoadMt( i, spCommon, workQueue );
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

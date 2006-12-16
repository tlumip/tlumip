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
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.pb.common.rpc.DafNode;
import com.pb.common.util.ResourceUtil;

import org.apache.log4j.Logger;



public class AonFlowHandler implements AonFlowHandlerIF {

    protected static Logger logger = Logger.getLogger(AonFlowHandler.class);

    public static final String COMPILE_RESULTS_SIGNAL = "aonFlowHandlerCompileResults";
    public static final String NUM_COMPILED_RESULTS_SIGNAL = "aonFlowHandlerNumCompiledResults";
    public static final String NUM_ASSIGNED_PACKETS_NAME = "aonFlowHandlerNumPacketsAssigned";
    
    public static final int PACKET_SIZE = 100;

    // set the frequency with which the shared class is polled to see if all threads have finshed their work.
//    static final int POLLING_FREQUENCY_IN_SECONDS = 10;
    static final int POLLING_FREQUENCY_IN_SECONDS = 1;
    
    static String rpcConfigFile = null;
    
    HashMap componentPropertyMap;
    HashMap globalPropertyMap;

    ResourceBundle componentRb;
    ResourceBundle globalRb;
    
    int networkNumLinks;
    int networkNumUserClasses;
    int networkNumCentroids;
    String timePeriod;

    double[][] tripTableRowSums = null;
    
    NetworkHandlerIF nh;
    DemandHandlerIF dh;
    
	public AonFlowHandler() {
    }



    // Factory Method to return either local or remote instance
    public static AonFlowHandlerIF getInstance( String rpcConfigFile ) {
    
        // store config file name in this object so it can be retrieved by others if needed.
        AonFlowHandler.rpcConfigFile = rpcConfigFile;

        
        if ( rpcConfigFile == null ) {

            // if rpc config file is null, then all handlers are local, so return local instance
            return new AonFlowHandler();

        }
        else {
            
            // return either a local instance or an rpc instance depending on how the handler was defined.
            Boolean isLocal = DafNode.getInstance().isHandlerLocal( HANDLER_NAME );

            if ( isLocal == null )
                // handler name not found in config file, so create a local instance.
                return new AonFlowHandler();
            else if ( isLocal )
                // handler name found in config file and is local, so create a local instance.
                return new AonFlowHandler();
            else 
                // handler name found in config file but is not local, so create an rpc instance.
                return new AonFlowHandlerRpc( rpcConfigFile );

        }
        
    }
    

    // Factory Method to return local instance only
    public static AonFlowHandlerIF getInstance() {
        return new AonFlowHandler();
    }
    

    
    public boolean setup( ResourceBundle componentRb, ResourceBundle globalRb, NetworkHandlerIF nh, char[] highwayModeCharacters ) {

        this.componentRb = componentRb;
        this.globalRb = globalRb;

        componentPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( componentRb );
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( globalRb );

        this.nh = nh;
        networkNumLinks = nh.getLinkCount();
        networkNumCentroids = nh.getNumCentroids();
        networkNumUserClasses = nh.getNumUserClasses();
        this.timePeriod = nh.getTimePeriod();

        dh = DemandHandler.getInstance( rpcConfigFile );
        dh.setup( componentRb, globalRb, timePeriod, networkNumCentroids, networkNumUserClasses, nh.getNodeIndex(), nh.getAssignmentGroupMap(), highwayModeCharacters, nh.userClassesIncludeTruck() );
        dh.buildDemandObject();
        
        return true;
        
    }
    
    

    public double[][] getMulticlassAonLinkFlows () {

        double[][] aonFlow = null;
        
        // build and load shortest path trees for all zones, all user classes, and return aon link flows by user class
        aonFlow = calculateAonLinkFlows ( dh.getTripTableRowSums(), dh.getMulticlassTripTables() );
        
        return aonFlow;
        
    }

    
    
    private double[][] calculateAonLinkFlows ( double[][] tripTableRowSums, double[][][] tripTables ) {

        // define data structures used to manage the distribution of work
        HashMap controlMap = new HashMap();
        HashMap resultsMap = new HashMap();
        BlockingQueue workQueue = new LinkedBlockingQueue();

        
        int numPackets = putWorkPacketsOnQueue(workQueue, tripTableRowSums);

        // distribute work into packets and put on queue
        try {
            controlMap.put(COMPILE_RESULTS_SIGNAL, false);
            controlMap.put(NUM_ASSIGNED_PACKETS_NAME, numPackets);
        }
        catch ( Exception e ) {
            logger.error ("exception thrown seting COMPILE_RESULTS_SIGNAL to false and NUM_COMPLETED_PACKETS_NAME to 0 in controlMap.", e);
        }
        
        
        
        SpBuildLoadHandlerIF sp = SpBuildLoadHandler.getInstance( rpcConfigFile );
        sp.setup( tripTables, nh, workQueue, controlMap, resultsMap );
        sp.start();
        
        
        
        
        // wait here until all results from all packets have been transferred.
        try {
            while ( (Integer)controlMap.get( NUM_COMPILED_RESULTS_SIGNAL ) < numPackets ) {
                try {
                    Thread.sleep( POLLING_FREQUENCY_IN_SECONDS*1000 );
                }
                catch (InterruptedException e){
                    logger.error ( "InterruptedException thrown while waiting " + POLLING_FREQUENCY_IN_SECONDS + " seconds to see if all compiled results have been updated.", e);
                }
            }
        }
        catch ( Exception e ) {
            logger.error ("exception thrown checking NUM_COMPILED_RESULTS_SIGNAL < numPackets in controlMap.", e);
        }


        
        
        
        // resultsMap is complete, so get results.
        double[][] aonFlow = new double[networkNumUserClasses][networkNumLinks];
        
        int m=0;
        int k=0;
        String key = "";
        try {
            for (m=0; m < networkNumUserClasses; m++) {
                for (k=0; k < networkNumLinks; k++) {
                    key = m + "_" + k;
                    try {
                        aonFlow[m][k] = (Double)resultsMap.get(key);
                    }
                    catch (Exception e) {
                        aonFlow[m][k] = 0.0;
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error ("exception thrown retreiving final flows from resultsMap in AonFlowHandler.  m=" + m + ", k=" + k + ", aonFlow[m][k]=" + aonFlow[m][k] + ", key=" + key + ".", e);
        }

        return aonFlow;
        
    }

    
    
    // Work elements consist of a [user class, origin taz].
    // There will by numUserClasses*numCentroids total work elements.
    // Packets of work elements will be created  and placed on a distributed queue
    // to be processed concurrently if possible.
    private int putWorkPacketsOnQueue( BlockingQueue workQueue, double[][] tripTableRowSums )  {

        int numPackets = 0;
        
        int[][] workElements = new int[PACKET_SIZE][2];
        
        int k=0;
        for (int m=0; m < networkNumUserClasses; m++) {
            for (int i=0; i < networkNumCentroids; i++) {
            
                if (tripTableRowSums[m][i] > 0.0) {
                    workElements[k][0] = m;
                    workElements[k][1] = i;
                    k++;

                    if ( k == PACKET_SIZE ) {
                        
                        try {
                            workQueue.put(workElements);
                        }
                        catch ( InterruptedException e ) {
                            logger.error ("exception thrown putting work packets on workQueue.  m=" + m + ", i=" + i + ", k=" + k + ".", e);
                        }
                        
                        numPackets++;

                        workElements = new int[PACKET_SIZE][2];
                        k = 0;
                    }

                }
                
            }
        }

        if ( k > 0 ) {

            try {
                workQueue.put(workElements);
            }
            catch ( InterruptedException e ) {
                logger.error ("exception thrown putting work packets on workQueue.  m=" + networkNumUserClasses + ", i=" + networkNumCentroids + ", k=" + k + ".", e);
            }
            
            numPackets++;
        }

        return numPackets;
        
    }

}

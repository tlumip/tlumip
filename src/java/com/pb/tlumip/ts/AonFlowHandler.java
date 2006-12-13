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
import com.pb.common.rpc.RpcException;
import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;



public class AonFlowHandler implements AonFlowHandlerIF {

    protected static Logger logger = Logger.getLogger(AonFlowHandler.class);

    public static final String COMPILE_RESULTS_SIGNAL = "aonFlowHandlerCompileResults";
    public static final String NUM_COMPILED_RESULTS_SIGNAL = "aonFlowHandlerNumCompiledResults";
    public static final String NUM_COMPLETED_PACKETS_NAME = "aonFlowHandlerWorkCompletedQueue";
    
    public static final String WORK_QUEUE_NAME = "aonFlowHandlerWorkQueue";
    public static final String CONTROL_MAP_NAME = "aonFlowHandlerControlMap";
    public static final String RESULTS_MAP_NAME = "aonFlowHandlerResultsMap";
    
    public static final int PACKET_SIZE = 100;

    // set the frequency with which the shared class is polled to see if all threads have finshed their work.
    static final int POLLING_FREQUENCY_IN_SECONDS = 10;
    
    

    int numLinks;
    int numCentroids;
    int numUserClasses;
    int startOriginTaz;
    int lastOriginTaz;

    int[] ia;
    int[] indexNode;

    String componentPropertyName;
    String globalPropertyName;
    
	HashMap componentPropertyMap;
    HashMap globalPropertyMap;

    

	public AonFlowHandler() {
    }
    


    
    public boolean setup( HashMap componentPropertyMap, HashMap globalPropertyMap ) {
        
        this.componentPropertyMap = componentPropertyMap;
        this.globalPropertyMap = globalPropertyMap; 
        
        setNetworkParameters ();
        
        return true;
    }
    
    

    public boolean setup( ResourceBundle componentRb, ResourceBundle globalRb ) {
        
        this.componentPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( componentRb );
        this.globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( globalRb );

        setNetworkParameters ();

        return true;
    }
    
    
    
    public double[][] getMulticlassAonLinkFlows () {

        DemandHandlerIF dh = DemandHandler.getInstance();
        
        double[][] tripTableRowSums = null;
        
        // get the trip table row sums by user class
        try {
            tripTableRowSums = dh.getTripTableRowSums();
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught getting trip table row sums from DemandHandler.", e );
            System.exit(1);
        }
        
        // build and load shortest path trees for all zones, all user classes, and return aon link flows by user class
        double[][] aonFlow = null;
        try {
            aonFlow = calculateAonLinkFlows ( tripTableRowSums );
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught calculating AON link flows in AonFlowHandler.", e );
            System.exit(1);
        }
        
        return aonFlow;
        
    }


    
    private void setNetworkParameters () {
        
        // generate a NetworkHandler object to use for assignments and skimming
        NetworkHandlerIF nh = NetworkHandler.getInstance();

        startOriginTaz = 0;
        lastOriginTaz = nh.getNumCentroids();
        numLinks = nh.getLinkCount();
        numCentroids = nh.getNumCentroids();
        numUserClasses = nh.getNumUserClasses();
        
        ia = nh.getIa();
        indexNode = nh.getIndexNode();
        
    }
    
    

    private double[][] calculateAonLinkFlows ( double[][] tripTableRowSums ) throws RpcException {

        
        DHashMap workResultsMap = new DHashMap(RESULTS_MAP_NAME);
        DHashMap controlMap = new DHashMap(CONTROL_MAP_NAME);

        
        
        controlMap.put(COMPILE_RESULTS_SIGNAL, false);
        controlMap.put(NUM_COMPLETED_PACKETS_NAME, 0);
        
        // distribute work into packets and put on queue
        int numPackets = putWorkPacketsOnQueue(tripTableRowSums);
                
        
        
        // wait here until all packets have been processed.
        while ( (Integer)controlMap.get( NUM_COMPLETED_PACKETS_NAME ) < numPackets ) {
            try {
                Thread.sleep( POLLING_FREQUENCY_IN_SECONDS*1000 );
            }
            catch (InterruptedException e){
                logger.error ( "InterruptedException thrown while waiting " + POLLING_FREQUENCY_IN_SECONDS + " seconds to see if all packets have been handled.", e);
            }
        }
        controlMap.put(NUM_COMPILED_RESULTS_SIGNAL, 0);
        controlMap.put(COMPILE_RESULTS_SIGNAL, true);

        

        
        // wait here until all results from all packets have been transferred.
        while ( (Integer)controlMap.get( NUM_COMPILED_RESULTS_SIGNAL ) < numPackets ) {
            try {
                Thread.sleep( POLLING_FREQUENCY_IN_SECONDS*1000 );
            }
            catch (InterruptedException e){
                logger.error ( "InterruptedException thrown while waiting " + POLLING_FREQUENCY_IN_SECONDS + " seconds to see if all compiled results have been updated.", e);
            }
        }


        // resultsMap is complete, so get results.
        double[][] aonFlow = new double[numUserClasses][numLinks];
        
        int m=0;
        int k=0;
        String key = "";
        try {
            for (m=0; m < numUserClasses; m++) {
                for (k=0; k < numLinks; k++) {
                    key = m + "_" + k;
                    aonFlow[m][k] = (Double)workResultsMap.get(key);
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
    private int putWorkPacketsOnQueue( double[][] tripTableRowSums ) throws RpcException {

        DBlockingQueue workQueue = new DBlockingQueue(WORK_QUEUE_NAME);

        int numPackets = 0;
        
        int[][] workElements = new int[PACKET_SIZE][2];
        
        int k=0;
        for (int m=0; m < numUserClasses; m++) {
            for (int i=startOriginTaz; i < lastOriginTaz; i++) {
            
                if (tripTableRowSums[m][i] > 0.0) {
                    workElements[k][0] = m;
                    workElements[k][1] = i;
                    k++;

                    if ( k == PACKET_SIZE ) {
                        workQueue.put(workElements);
                        numPackets++;

                        workElements = new int[PACKET_SIZE][2];
                        k = 0;
                    }
                }
                
            }
        }

        if ( k > 0 ) {
            workQueue.put(workElements);
            numPackets++;
        }

        return numPackets;
        
    }

}

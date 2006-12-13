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


import org.apache.log4j.Logger;

import com.pb.common.rpc.DBlockingQueue;
import com.pb.common.rpc.DHashMap;
import com.pb.tlumip.ts.AonFlowHandler;

/**
 *   This class manages information that is common to all threads running SpBuildLoadMt.run() rethods on the vm from
 *   which an instance of this class was created.  Common information includes the workQueue, the results arrays in
 *   which results computed by the running threads are accumulated, and network and demand setup data.
 * 
 */

public class SpBuildLoadCommon {

    protected static Logger logger = Logger.getLogger(SpBuildLoadCommon.class);
    
    private static SpBuildLoadCommon instance = new SpBuildLoadCommon();

    private DBlockingQueue workQueue = new DBlockingQueue(AonFlowHandler.WORK_QUEUE_NAME);
    private DHashMap controlMap = new DHashMap( AonFlowHandler.CONTROL_MAP_NAME );
    
    private NetworkHandlerIF nh;
    private DemandHandlerIF dh;
    
    private double[][] cumulativeBuildLoadResults;
    
    private int packetsCompleted = 0;
    
    
    private SpBuildLoadCommon () {
    }

    
    /** Return instances of this class.
    *
    */
    public static SpBuildLoadCommon getInstance() {
        return instance;
    }

    
    /** setup data structures to be used by all threads
     *  working on building and loading aon link flows.
     */
    public void setup(NetworkHandlerIF nh, DemandHandlerIF dh) {

        // get a handle to a NetworkHandler object.
        this.nh = nh;
        
        // get a handle to a DemandHandler object.
        this.dh = dh;
        
       
        int numUserClasses = nh.getNumUserClasses();
        int numLinks = nh.getLinkCount();
        
        // declare an an array to be used by all threads for accumulating loaded aon link flows.
        cumulativeBuildLoadResults = new double[numUserClasses][numLinks];
        
        packetsCompleted = 0;
        
    }
    

    /** return the queue for holding work elements to be completed
     * *
     */
    public DBlockingQueue getWorkList() {
        return workQueue;
    }

    
    /** return the array for accumulating loaded aon link flows.
     * *
     */
    public double[][] getResultsArray() {
        return cumulativeBuildLoadResults;
    }


    /** return the row from the trip table for the specified user class and origin zone.
     * *
     */
    public double[] getTripTableRow( int m, int z ) {

        // get the user class m trips from zone z to all other zones (trip table row z).
        double[] tripTableRow = null;
        try {
            tripTableRow = dh.getTripTableRow( m, z );
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught getting trip table row for user class = " + m + ", origin zone index = " + z + ".", e ); 
            System.exit(1);
        }

        return tripTableRow;
    }


    /**
     * increment the count of packets completed by the worker threads
     */
    public void updateCompletedPacketCount() {

        packetsCompleted++;

        try {
            int value = (Integer)controlMap.get( AonFlowHandler.NUM_COMPLETED_PACKETS_NAME ) + 1;
            controlMap.put( AonFlowHandler.NUM_COMPLETED_PACKETS_NAME, value );
        }
        catch (Exception e) {
            logger.error ("exception thrown updating number of packets completed count by this SpBuildLoadHandler into controlMap.", e);
        }
        
    }
    
    
    public int getPacketsCompletedCount () {
        return packetsCompleted;
    }
    
    
    /** a NetworkHandler object is needed by the worker threads to create ShortestPathTreeH objects.
     * 
     */
    public NetworkHandlerIF getNetworkHandler() {
        return nh;
    }
    
}

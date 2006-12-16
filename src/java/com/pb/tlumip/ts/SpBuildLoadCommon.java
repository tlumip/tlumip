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


import java.util.HashMap;

import org.apache.log4j.Logger;

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

    private HashMap controlMap;
    
    private double[][][] cumulativeBuildLoadResults;
    private double[][][] tripTables;
    
    private int packetsCompleted = 0;
    
    private NetworkHandlerIF nh;
    
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
    public void setup(int numThreads, int numUserClasses, int numLinks, double[][][] tripTables, NetworkHandlerIF nh, HashMap controlMap ) {

        this.nh = nh;
        this.tripTables = tripTables;
        this.controlMap = controlMap;
        
        // declare an an array to be used by all threads for accumulating loaded aon link flows.
        cumulativeBuildLoadResults = new double[numThreads][numUserClasses][numLinks];
        
        packetsCompleted = 0;
        
    }
    

    /** return the array for accumulating loaded aon link flows.
     * *
     */
    public double[][] getResultsArray( int threadId ) {
        return cumulativeBuildLoadResults[threadId];
    }


    /** update the array afterr accumulating loaded aon link flows.
     * *
     */
    public void setResultsArray( int threadId, double[][] updatedArray ) {
        cumulativeBuildLoadResults[threadId] = updatedArray;
    }


    /** return the row from the trip table for the specified user class and origin zone.
     * *
     */
    public double[] getTripTableRow( int m, int z ) {
        return tripTables[m][z];
    }


    /**
     * increment the count of packets completed by the worker threads
     */
    public void updateCompletedPacketCount() {

        packetsCompleted++;

        int numPackets = (Integer)controlMap.get( AonFlowHandler.NUM_ASSIGNED_PACKETS_NAME );
        if ( packetsCompleted == numPackets ) {

            try {
                controlMap.put( AonFlowHandler.NUM_COMPILED_RESULTS_SIGNAL, 0 );
                controlMap.put( AonFlowHandler.COMPILE_RESULTS_SIGNAL, true );
            }
            catch ( Exception e ) {
                logger.error ("exception thrown setting COMPILE_RESULTS_SIGNAL to true and NUM_COMPILED_RESULTS_SIGNAL to 0 in controlMap.", e);
            }

        }

    }
    
    
    public int getPacketsLeft() {
        int numPackets = (Integer)controlMap.get( AonFlowHandler.NUM_ASSIGNED_PACKETS_NAME );
        return numPackets - packetsCompleted;
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

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


import java.util.Arrays;

import org.apache.log4j.Logger;


/**
 *   This class manages information that is common to all threads running SpBuildLoadMt.run() rethods on the vm from
 *   which an instance of this class was created.  Common information includes the workQueue, the results arrays in
 *   which results computed by the running threads are accumulated, and network setup data.
 * 
 */

public class SpBuildLoadCommon {

    protected static Logger logger = Logger.getLogger(SpBuildLoadCommon.class);
    
    private static SpBuildLoadCommon instance = new SpBuildLoadCommon();
    
    private String handlerName;


    private int[] packetsCompletedByThread;
    
    private double[][][] cumulativeBuildLoadResults;
    
    private NetworkHandlerIF nh;
    private DemandHandlerIF dh;
    
    private int numLinks;
    private int numUserClasses;
    private int numThreads;
    
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
    public void setup( String handlerName, int numThreads, NetworkHandlerIF nh, DemandHandlerIF dh ) {

        this.handlerName = handlerName;
        this.nh = nh;
        this.dh = dh;
        this.numThreads = numThreads;
        this.numUserClasses = nh.getNumUserClasses();
        this.numLinks = nh.getLinkCount();

        reset();
    }
    
    public void reset() {
        
        // declare an an array to be used by all threads for accumulating loaded aon link flows.
        cumulativeBuildLoadResults = new double[numThreads][numUserClasses][numLinks];
        
        packetsCompletedByThread = new int[numThreads];
        Arrays.fill ( packetsCompletedByThread, -1 );
        
    }
    

    /** return the array for accumulating loaded aon link flows.
     * *
     */
    public double[][] getResultsForThread( int threadId ) {
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
        double[] tripTableRow = dh.getTripTableRow(m, z);
        return tripTableRow;
    }

    
    /** set the number of packets handled by th thread.
     */
    public void setPacketsCompletedByThread ( int threadId, int packetsCompleted ) {
        packetsCompletedByThread[threadId] = packetsCompleted;
    }

    
    /** return the number of packets handled by the thread.
     */
    public int getpacketsCompletedByThread ( int threadId ) {
        return packetsCompletedByThread[threadId];
    }


    /** return the number of threads that have a non-negative packet count indicating they's finished.
     */
    public int getNumberOfThreadsCompleted () {

        int numThreadsCompleted = 0;
        for ( int i=0; i < packetsCompletedByThread.length; i++ ) {
            //logger.info( handlerName + " thread " + i + " completed " + packetsCompletedByThread[i] + " packets, numThreadsCompleted=" + numThreadsCompleted + ".");
            if ( packetsCompletedByThread[i] >= 0 )
                numThreadsCompleted++;
        }

        return numThreadsCompleted;
        
    }

    
    public String getHandlerName() {
        return handlerName;
    }
    
    
    /** a NetworkHandler object is needed by the worker threads to create ShortestPathTreeH objects.
     * 
     */
    public NetworkHandlerIF getNetworkHandler() {
        return nh;
    }
    
}

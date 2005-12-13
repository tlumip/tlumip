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
package com.pb.tlumip.ts.daf3;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

/**
 *   This class manages information that is common to all threads running on the vm from
 *   which an nstance of this class was created.
 * 
 */

public class AonBuildLoadCommon {

    protected static Logger logger = Logger.getLogger(AonBuildLoadCommon.class);
    
    private static AonBuildLoadCommon instance = new AonBuildLoadCommon();

    BlockingQueue workList;
    double[][] cumulativeAonFlowResults;
    
    boolean[][] validLinksForClasses;
    int numUserClasses;
    int numLinks;

    int threadsFinished;
    
    
    
    private AonBuildLoadCommon () {
    }

    
    /** Return instances of this class.
    *
    */
    public static AonBuildLoadCommon getInstance() {
        return instance;
    }

    
    /** setup data structures to be used by all threads
     *  working on building and loading aon link flows.
     * 
     * @param numUserClasses
     * @param numLinks
     */
    public void setup( boolean[][] validLinksForClasses ) {

        this.validLinksForClasses = validLinksForClasses;
        this.numUserClasses = validLinksForClasses.length;
        this.numLinks = validLinksForClasses[0].length;
        
        // use java 1.5 class to hold the work elements
        workList = new LinkedBlockingQueue();
        
        // declare an an array to be used by all threads for accumulating loaded aon link flows.
        cumulativeAonFlowResults = new double[numUserClasses][numLinks];
        
        // initialize the count of threads that have finished their work to 0.
        threadsFinished = 0;

    }
    

    /** return the queue for holding work elements to be completed
     * *
     */
    public BlockingQueue getWorkList() {
        return workList;
    }

    
    /** return the array for accumulating loaded aon link flows.
     * *
     */
    public double[][] getResultsArray() {
        return cumulativeAonFlowResults;
    }

    /** return the array of valid links by user classes over which shortest path trees can be built.
     * *
     */
    public boolean[][] getValidLinksForClasses() {
        return validLinksForClasses;
    }

    /** return the number of user classes for which aon link flows are accumulated.
     * *
     */
    public int getNumUserClasses() {
        return numUserClasses;
    }

    /** return the number of links for which aon link flows are accumulated.
     * *
     */
    public int getNumLinks() {
        return numLinks;
    }

    /** When a thread receives an end of workList marker, it increments this value.
     *  When this value is equal to the number of threads, the multithreaded procedure is fininshed.
     * *
     */
    public void incrementThreadsFinished() {
        threadsFinished++;
    }

    /** The controller code that started the threads watches this value to know when all threads
     *  have fininshed and therefore the multithreaded procedure is done.
     * *
     */
    public int getThreadsFinishedCount() {
        return threadsFinished;
    }

}

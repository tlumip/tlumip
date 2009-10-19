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


import com.pb.common.rpc.DafNode;

import org.apache.log4j.Logger;



public class AonFlowHandler implements AonFlowHandlerIF {

    protected static Logger logger = Logger.getLogger(AonFlowHandler.class);


    // set the frequency with which the shared class is polled to see if all threads have finshed their work.
//    static final int POLLING_FREQUENCY_IN_SECONDS = 10;
    static final int POLLING_FREQUENCY_IN_SECONDS = 1;
    
    static String rpcConfigFile = null;
    
    int networkNumLinks;
    int networkNumUserClasses;
    int networkNumCentroids;
    String timePeriod;

    NetworkHandlerIF nh;
    
    SpBuildLoadHandlerIF[] sp;
    
   
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
            else 
                // handler name found in config file but is not local, so create an rpc instance.
                return new AonFlowHandlerRpc( rpcConfigFile );

        }
        
    }
    

    // Factory Method to return local instance only
    public static AonFlowHandlerIF getInstance() {
        return new AonFlowHandler();
    }
    

    // this setup method called by methods running in the same VM as this object
    public boolean setup( String reportFileName, String rpcConfigFile, String demandOutputFileName, String sdtFileName, String ldtFileName, double ptSampleRate, String ctFileName, String etFileName, int startHour, int endHour, char[] highwayModeCharacters, NetworkHandlerIF nh ) {

        this.nh = nh;
        
        networkNumLinks = nh.getLinkCount();
        networkNumCentroids = nh.getNumCentroids();
        networkNumUserClasses = nh.getNumUserClasses();
        timePeriod = nh.getTimePeriod();
        float[] userClassPces = nh.getUserClassPces();


        logger.info( "requesting that demand matrices get built." );
        DemandHandlerIF dh = DemandHandler.getInstance( rpcConfigFile );
        dh.setup( userClassPces, demandOutputFileName, sdtFileName, ldtFileName, ptSampleRate, ctFileName, etFileName, startHour, endHour, timePeriod, networkNumCentroids, networkNumUserClasses, nh.getIndexNode(), nh.getNodeIndex(), nh.getAlphaDistrictIndex(), nh.getDistrictNames(), nh.getAssignmentGroupChars(), highwayModeCharacters, nh.userClassesIncludeTruck() );
        dh.buildHighwayDemandObject();
        dh.logDistrictReport();
        
        if ( reportFileName != null )
            dh.writeDistrictReport ( reportFileName );
        
        logger.info( "setting up SpBuildLoadHandlers." );
        sp = setupSpBuildLoadHandlers( dh.getTripTableRowSums(), dh.getMulticlassTripTables() );
        
        return true;
        
    }
    
    
    // this method called by methods running in a different VM and thus making a remote method call to setup this object
    public boolean setupRpc( String reportFileName, String rpcConfigFile, String demandOutputFileName, String sdtFileName, String ldtFileName, double ptSampleRate, String ctFileName, String etFileName, int startHour, int endHour, char[] highwayModeCharacters ) {

        nh = NetworkHandler.getInstance(rpcConfigFile);
        
        return setup( reportFileName, rpcConfigFile, demandOutputFileName, sdtFileName, ldtFileName, ptSampleRate, ctFileName, etFileName, startHour, endHour, highwayModeCharacters, nh );        
        
    }
    
    

    public double[][] getMulticlassAonLinkFlows () {

        // start the distributed handlers, and combine their results when they've all finished.
        logger.info( "AonFlowHandler starting all registered SpBuildLoadHandlers." );
        double[][] aonFlow = runSpBuildLoadHandlers();
        
        return aonFlow;
        
    }

    
    public double[][] getMulticlassAonLinkFlowsRpc () {

        double[][] aonFlow = getMulticlassAonLinkFlows();
        return aonFlow;
        
    }

    
    private double[][] runSpBuildLoadHandlers() {

        
        // start each handler working on the new workQueue
        for ( int i=0; i < sp.length; i++ ) {
            sp[i].start( nh.setLinkGeneralizedCost() );
        }


        // wait for all SpBuildLoadHandlers to ave indicated they are finished.
        logger.info( "AonFlowHandler waiting for all started SpBuildLoadHandlers to finish." );
        waitForAllHandlers();

        
        // all SpBuildLoadHandlers are finished, so get results.
        double[][] aonFlow = new double[networkNumUserClasses][networkNumLinks];
        for ( int i=0; i < sp.length; i++ ) {

            double[][] handlerResults = sp[i].getResults();
            for (int m=0; m < handlerResults.length; m++)
                for (int k=0; k < handlerResults[m].length; k++)
                    aonFlow[m][k] += handlerResults[m][k];
            
        }
        
        return aonFlow;
        
    }
    
    
    private SpBuildLoadHandlerIF[] setupSpBuildLoadHandlers( double[][] tripTableRowSums, double[][][] multiclassDemandMatrices ) {

        // get the specific handler names from the config file that begin with the SpBuildLoadHandler handler name.
        String[] spHandlerNames = null;
        if ( rpcConfigFile == null ) {
            logger.warn("Config file is null, creating handler on local VM");
            spHandlerNames = new String[1];
            spHandlerNames[0] = SpBuildLoadHandlerIF.HANDLER_NAME;
        }
        else {
            spHandlerNames = DafNode.getInstance().getHandlerNamesStartingWith( SpBuildLoadHandlerIF.HANDLER_NAME );

            if ( spHandlerNames == null ) {
                spHandlerNames = new String[1];
                spHandlerNames[0] = SpBuildLoadHandlerIF.HANDLER_NAME;
            }
        }

        // create an array of SpBuildLoadHandlers dimensioned to the number of handler names found
        sp = new SpBuildLoadHandlerIF[spHandlerNames.length];

        for ( int i=0; i < spHandlerNames.length; i++ )
            sp[i] = SpBuildLoadHandler.getInstance( rpcConfigFile, spHandlerNames[i] );

        
        // get number of threads for each handler and total number of threads over all handlers
        int totalThreads = 0;
        int[] handlerThreads = new int[sp.length];
        for ( int i=0; i < sp.length; i++ ) {
            handlerThreads[i] = sp[i].getNumberOfThreads();
            totalThreads += handlerThreads[i]; 
        }
            
        
        // get the list of [userclass, origin taz] work elements and distribute work elements by handler and threads in handlers
        int [][][][] workElementsArray = getWorkElementsArray( handlerThreads, totalThreads, tripTableRowSums );        
        
        // get the demand table row arrays associated with each work element
        double[][][][] workElementsDemand = getWorkElementDemand ( workElementsArray, multiclassDemandMatrices );
        
        
        
        // for each handler name, create a SpBuildLoadHandler, set it up, and start it running
        int returnCount = 0;
        for ( int i=0; i < spHandlerNames.length; i++ ) {
            returnCount += sp[i].setup( spHandlerNames[i], rpcConfigFile, workElementsArray[i], workElementsDemand[i], nh.getNumUserClasses(), nh.getLinkCount(), nh.getNodeCount(), nh.getNumCentroids(), nh.getIa(), nh.getIb(), nh.getIpa(), nh.getSortedLinkIndexA(), nh.getIndexNode(), nh.getNodeIndex(), nh.getCentroid(), nh.getValidLinksForAllClasses(), nh.setLinkGeneralizedCost(), nh.getTurnPenaltyIndices(), nh.getTurnPenaltyArray() );
        }


        return sp;
        
    }
    
    
    private void waitForAllHandlers() {
        
        // wait here until all distributed handlers have indicated they are finished.
        while ( getNumberOfHandlersCompleted () < sp.length ) {

            try {
                Thread.sleep( POLLING_FREQUENCY_IN_SECONDS*1000 );
            }
            catch (InterruptedException e){
                logger.error ( "exception thrown waiting for all SpBuildLoadHandlers to finish.", e);
            }
            
        }
        
    }


    
    private int getNumberOfHandlersCompleted () {

        int numHandlersCompleted = 0;
        for ( int i=0; i < sp.length; i++ ) {
            if ( sp[i].handlerIsFinished() )
                numHandlersCompleted++;
        }

        return numHandlersCompleted;
        
    }

    
    
    // Work elements consist of a [user class, origin taz].
    // There will by numUserClasses*numCentroids total potential work elements.
    // A work element will be created only if there is demand from the origin taz for the user class.
    // This list will be divided up among the available SpBuildLoadHandler worker threads.
    private int[][][][] getWorkElementsArray( int[] handlerThreads, int totalThreads, double[][] tripTableRowSums ) {

        // create an array of work elements to be split up and distributed to work handlers
        int[][] workElements = new int[networkNumUserClasses*networkNumCentroids][networkNumCentroids];

        int numberElements=0;
        for (int m=0; m < networkNumUserClasses; m++) {
            for (int i=0; i < networkNumCentroids; i++) {

                if (tripTableRowSums[m][i] > 0.0) {
                    workElements[numberElements][0] = m;
                    workElements[numberElements][1] = i;
                    numberElements++;

                }
                
            }
        }

        
        int numberElementsPerThread = numberElements/totalThreads;
        int remainder = numberElements - numberElementsPerThread*totalThreads;

        // return the work elements array - int[numHandlers][numThreads][numElements][2]
        int[][][][] returnElements = new int[handlerThreads.length][][][];

        int k = 0;
        int r = 0;
        for (int i=0; i < handlerThreads.length; i++) {
            
            returnElements[i] = new int[handlerThreads[i]][][];

            for (int j=0; j < handlerThreads[i]; j++) {
                int dimension = numberElementsPerThread;
                if ( r < remainder ) {
                    dimension++;
                    r++;
                }
                returnElements[i][j] = new int[dimension][2];

                for (int m=0; m < dimension; m++) {
                    returnElements[i][j][m][0] = workElements[k][0];
                    returnElements[i][j][m][1] = workElements[k][1];
                    k++;
                }
                
            }
            
        }
        
        return returnElements;
        
    }

    
    
    // create a ragged array of demand matrix rows of trips for each work element that will be distributed to handlers and subsequently handler threads.
    // the work elemnts don't change, so these can be set once in the SpBuildLoadHandlers and reused each time a new shortest path tree is loaded.
    double [][][][] getWorkElementDemand ( int[][][][] workElementArray, double[][][] multiclassDemandMatrices ) {
    
        double [][][][] demandPerElement = new double[workElementArray.length][][][];
        
        // loop over handlers
        for (int i=0; i < workElementArray.length; i++) {
            
            demandPerElement[i] = new double[workElementArray[i].length][][];
            
            // loop over threads per handler
            for (int j=0; j < workElementArray[i].length; j++) {
                
                demandPerElement[i][j] = new double[workElementArray[i][j].length][];

                // loop over work elements per thread per handler
                for (int m=0; m < workElementArray[i][j].length; m++) {
                    
                    int userclass =  workElementArray[i][j][m][0];
                    int origTaz =  workElementArray[i][j][m][1];

                    demandPerElement[i][j][m] = new double[multiclassDemandMatrices[userclass][origTaz].length];

                    // loop over columns in the demand matrix row associated with the userclass and origin taz defined in the work element
                    for (int c=0; c < multiclassDemandMatrices[userclass][origTaz].length; c++) {
                    
                        demandPerElement[i][j][m][c] = multiclassDemandMatrices[userclass][origTaz][c];
                        
                    }
                    
                }
                
            }
            
        }
        
        return demandPerElement;
        
    }
    

    public int[][][] getSavedShortestPathTrees () {
        
        int[][][] savedTrees = new int[networkNumUserClasses][networkNumCentroids][];
        
        // loop over shortest path tree handler objects
        for ( int h=0; h < sp.length; h++ ) {

            // get all the shortest path trees computed on this handler
            for (int m=0; m < networkNumUserClasses; m++) {
                for (int i=0; i < networkNumCentroids; i++) {
                    int[] pathTree = sp[h].getShortestPathTree( m, i );
                    if ( pathTree != null )
                        savedTrees[m][i] = pathTree;
                }
            }
            
        }
        
        return savedTrees;

    }
    
}

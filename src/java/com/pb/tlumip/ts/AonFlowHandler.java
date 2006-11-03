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


import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Logger;



public class AonFlowHandler {

    public static final int NUM_DISTRIBUTED_HANDLERS = 1;
    public static final int[] numberOfThreads = { 1 };
//    public static final int NUM_DISTRIBUTED_HANDLERS = 2;
//    public static final int[] numberOfThreads = { 2, 4 };

    
    public static String remoteHandlerName = "aonFlowHandler";
    
	protected static Logger logger = Logger.getLogger(AonFlowHandler.class);

//    private AonFlowResults flowResults = AonFlowResults.getInstance();

    int numLinks;
    int numCentroids;
    int numUserClasses;
    int lastOriginTaz;
    int startOriginTaz;
    int[] ia;
    int[] indexNode;

    String componentPropertyName;
    String globalPropertyName;
    
	HashMap componentPropertyMap;
    HashMap globalPropertyMap;

    


	public AonFlowHandler() {
    }
    


    
    public Object execute (String methodName, Vector params) throws Exception {
                  
        if ( methodName.equalsIgnoreCase( "setup" ) ) {
            HashMap componentPropertyMap = (HashMap)params.get(0);
            HashMap globalPropertyMap = (HashMap)params.get(1);
            setup( componentPropertyMap, globalPropertyMap );
            return 0;
        }
        else if ( methodName.equalsIgnoreCase( "getMulticlassAonLinkFlows" ) ) {
            return getMulticlassAonLinkFlows();
        }
        else {
            logger.error ( "method name " + methodName + " called from remote client is not registered for remote method calls.", new Exception() );
            return 0;
        }
        
    }
    

    
    
    public void setup( HashMap componentPropertyMap, HashMap globalPropertyMap ) {
        
        this.componentPropertyMap = componentPropertyMap;
        this.globalPropertyMap = globalPropertyMap; 
        
        getNetworkParameters ();
    }
    
    
    public void setup( ResourceBundle componentRb, ResourceBundle globalRb ) {
        
        this.componentPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( componentRb );
        this.globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( globalRb );

        getNetworkParameters ();
    }
    
    
    
    private void getNetworkParameters () {
        
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
    
    

    private double[][] getMulticlassAonLinkFlows () {

        DemandHandler dh = new DemandHandler();
        
        double[][] tripTableRowSums = null;
        
        // get the trip table row sums by user class
        try {
            tripTableRowSums = dh.getTripTableRowSums();
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }
        
        // build and load shortest path trees for all zones, all user classes, and return aon link flows by user class
        double[][] aonFlow = calculateAonLinkFlows ( tripTableRowSums );
        
        return aonFlow;
        
    }


    
    
    private double[][] calculateAonLinkFlows ( double[][] tripTableRowSums ) {

        int origin=0;
        
        int[][][] workElements = null;
        int[] workElementZoneList = new int[numUserClasses*numCentroids];
        int[] workElementUserClassList = new int[numUserClasses*numCentroids];

        
        int k=0;
        for (int m=0; m < numUserClasses; m++) {
            for (origin=startOriginTaz; origin < lastOriginTaz; origin++) {
            
                if (tripTableRowSums[m][origin] > 0.0) {
                    workElementZoneList[k] = origin;
                    workElementUserClassList[k] = m ;
                    k++;
                }

            }
        }

        
        // divide the work evenly among handlers for now.  First allocate workElements/handlers to
        // the first n-1 handlers and accumulate the number of work elements allocated.  Allocate
        // the remaining elements to the last handler.
        int[] numWorkElementsPerNode = new int[NUM_DISTRIBUTED_HANDLERS];
        int cumNumElements = 0;
        for (int i=0; i < NUM_DISTRIBUTED_HANDLERS - 1; i++) {
            numWorkElementsPerNode[i] = (int)(k/NUM_DISTRIBUTED_HANDLERS);
            cumNumElements += numWorkElementsPerNode[i]; 
        }
        numWorkElementsPerNode[NUM_DISTRIBUTED_HANDLERS - 1] = k - cumNumElements; 


        
        // dimension the work elements array from the number of work elemets added to the ArrayList.
        workElements = new int[NUM_DISTRIBUTED_HANDLERS][][];

        for (int n=0; n < NUM_DISTRIBUTED_HANDLERS; n++) {

            workElements[n] = new int[numWorkElementsPerNode[n]][2];
            
            for (int i=0; i < numWorkElementsPerNode[n]; i++) {
                workElements[n][i][0] = workElementZoneList[i];
                workElements[n][i][1] = workElementUserClassList[i];
            }
            
        }
        

        double[][][] returnedAonFlows = new double[NUM_DISTRIBUTED_HANDLERS][][];
        
        
        
        // create multiple handlers to distribute shortest path tree building and loading
        SpBuildLoadHandler[] spblh = new SpBuildLoadHandler[NUM_DISTRIBUTED_HANDLERS];
        for (int n=0; n < NUM_DISTRIBUTED_HANDLERS; n++)
            spblh[n] = new SpBuildLoadHandler();
        
        
        // send work elements arrays to each of the handlers
        try {
            for (int n=0; n < NUM_DISTRIBUTED_HANDLERS; n++) {
                logger.info( "SpBuildLoadHandler" + "_" + (n+1) + " sent " + numWorkElementsPerNode[n] + " userclass, origin zone pairs." );
                returnedAonFlows[n] = spblh[n].getLoadedAonFlows( workElements[n] );
            }
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }

        
        // initialize the AON Flow array
        double[][] aonFlow = new double[returnedAonFlows[0].length][returnedAonFlows[0][0].length];
        
        // collect the distributed results into an array of AON link flows by user class
        for (int n=0; n < NUM_DISTRIBUTED_HANDLERS; n++) {
            for (int m=0; m < returnedAonFlows[n].length; m++) {
                for (int j=0; j < returnedAonFlows[n][m].length; j++) {
                    aonFlow[m][j] += returnedAonFlows[n][m][j];
                }
            }
        }

        return aonFlow;
        
    }

    
    
    
/*
    private int networkHandlerGetNumCentroidsRpcCall() throws Exception {
        // g.getNumCentroids()
        return (Integer)networkHandlerClient.execute("networkHandler.getNumCentroids", new Vector());
    }

    private int networkHandlerGetLinkCountRpcCall() throws Exception {
        // g.getLinkCount()
        return (Integer)networkHandlerClient.execute("networkHandler.getLinkCount", new Vector() );
    }

    private int networkHandlerGetNumUserClassesRpcCall() throws Exception {
        // g.getNumUserClasses()
        return (Integer)networkHandlerClient.execute("networkHandler.getNumUserClasses", new Vector() );
    }

    private int[] networkHandlerGetIaRpcCall() throws Exception {
        // g.getIa()
        return (int[])networkHandlerClient.execute("networkHandler.getIa", new Vector() );
    }

    private int[] networkHandlerGetIndexNodeRpcCall() throws Exception {
        // g.getIndexNode()
        return (int[])networkHandlerClient.execute("networkHandler.getIndexNode", new Vector() );
    }

    
    
    
    private double[][] demandHandlerGetTripTableRowSumsRpcCall() throws Exception {
        return (double[][])demandHandlerClient.execute("demandHandler.getTripTableRowSums", new Vector() );
    }
    
    
    
    
    private double[][] spBuildLoadHandlerGetLoadedAonFlowsRpcCall( int n, int[][] workElements ) throws Exception {
        Vector params = new Vector();
        params.add( numberOfThreads[n] );
        params.add( workElements );
        return (double[][])spBuildLoadHandlerClient[n].execute( (SpBuildLoadHandler.remoteHandlerName + "_" + (n+1) + ".getLoadedAonFlows"), params );
    }
 
*/
}

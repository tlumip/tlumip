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


import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcException;
import com.pb.common.rpc.RpcHandler;
import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Logger;



public class AonFlowHandler implements RpcHandler {

    public static final int NUM_DISTRIBUTED_HANDLERS = 1;
    public static final int[] numberOfThreads = { 4 };

    
    public static String remoteHandlerName = "aonFlowHandler";
    
	protected static Logger logger = Logger.getLogger(AonFlowHandler.class);

//    private AonFlowResults flowResults = AonFlowResults.getInstance();

    RpcClient demandHandlerClient;    
    RpcClient networkHandlerClient;
    RpcClient[] spBuildLoadHandlerClient;

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

    ResourceBundle appRb;
    ResourceBundle globalRb;
    


	public AonFlowHandler() {

        String handlerName = null;
        
        spBuildLoadHandlerClient = new RpcClient[NUM_DISTRIBUTED_HANDLERS];

        try {
            
            //Create RpcClients this class connects to
            try {

                handlerName = NetworkHandler.remoteHandlerName;
                networkHandlerClient = new RpcClient( handlerName );

                handlerName = DemandHandler.remoteHandlerName;
                demandHandlerClient = new RpcClient( handlerName );
                
                handlerName = SpBuildLoadHandler.remoteHandlerName;
                for (int i=0; i < NUM_DISTRIBUTED_HANDLERS; i++)
                    spBuildLoadHandlerClient[i] = new RpcClient( handlerName + "_" + (i+1) );
                
            }
            catch (MalformedURLException e) {
            
                logger.error ( "MalformedURLException caught in ShortestPathTreeH() while defining RpcClients.", e );
            
            }

        }
        catch ( Exception e ) {
            logger.error ( "Exception caught in ShortestPathTreeH().", e );
            System.exit(1);
        }

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
        

        this.appRb = componentRb;
        this.globalRb = globalRb;
        
        this.componentPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( componentRb );
        this.globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( globalRb );

        getNetworkParameters ();
    }
    
    
    
    private void getNetworkParameters () {
        
        try {
            
            startOriginTaz = 0;
            lastOriginTaz = networkHandlerGetNumCentroidsRpcCall();
            numLinks = networkHandlerGetLinkCountRpcCall();
            numCentroids = networkHandlerGetNumCentroidsRpcCall();
            numUserClasses = networkHandlerGetNumUserClassesRpcCall();
            
            ia = networkHandlerGetIaRpcCall();
            indexNode = networkHandlerGetIndexNodeRpcCall();

        }
        catch ( RpcException e ) {
            logger.error ( "RpcException caught.", e );
            System.exit(1);
        }
        catch ( IOException e ) {
            logger.error ( "IOException caught.", e );
            System.exit(1);
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }
        
    }
    
    

    private double[][] getMulticlassAonLinkFlows () {

        double[][] tripTableRowSums = null;
        
        // get the trip table row sums by user class
        try {
            tripTableRowSums = demandHandlerGetTripTableRowSumsRpcCall();
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
        
        // initialize the AON Flow array to zero
        double[][] aonFlow = null;
        
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
        
        
        // send work elements arrays to each of the handlers
        try {
            for (int n=0; n < NUM_DISTRIBUTED_HANDLERS; n++) {
                logger.info( SpBuildLoadHandler.remoteHandlerName + "_" + (n+1) + " sent " + numWorkElementsPerNode[n] + " userclass, origin zone pairs." );
                aonFlow = spBuildLoadHandlerGetLoadedAonFlowsRpcCall( n, workElements[n] );
            }
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }
        
        return aonFlow;
        
    }

    
    
    

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
        Object obj = spBuildLoadHandlerClient[n].execute( (SpBuildLoadHandler.remoteHandlerName + "_" + (n+1) + ".getLoadedAonFlows"), params );
        return (double[][]) obj;
    }
    
}

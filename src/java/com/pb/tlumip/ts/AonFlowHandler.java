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

    public static String remoteHandlerName = "aonFlowHandler";
    
	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.ShortestPathTreeHandler");

//    private AonFlowResults flowResults = AonFlowResults.getInstance();

    RpcClient demandHandlerClient;    
    RpcClient networkHandlerClient;
    RpcClient shortestPathHandlerClient;

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
        
        try {
            
            //Create RpcClients this class connects to
            try {

                handlerName = NetworkHandler.remoteHandlerName;
                networkHandlerClient = new RpcClient( handlerName );

                handlerName = DemandHandler.remoteHandlerName;
                demandHandlerClient = new RpcClient( handlerName );
                
                handlerName = ShortestPathTreeHandler.remoteHandlerName;
                shortestPathHandlerClient = new RpcClient( handlerName );
                
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
        
        int[][] workElements = null;
        int[] workElementZoneList = new int[numUserClasses*numCentroids];
        int[] workElementClassList = new int[numUserClasses*numCentroids];

        
        int k=0;
        for (int m=0; m < numUserClasses; m++) {
            for (origin=startOriginTaz; origin < lastOriginTaz; origin++) {
            
                if (tripTableRowSums[m][origin] > 0.0) {
                    workElementZoneList[k] = origin;
                    workElementClassList[k] = m ;
                    k++;
                }

            }
        }

        
        // dimension the work elements array from the number of work elemets added to the ArrayList.
        workElements = new int[k][2];
        
        for (int i=0; i < workElements.length; i++) {
            workElements[i][0] = workElementZoneList[i];
            workElements[i][1] = workElementClassList[i];
        }
            
        
        try {
            logger.info( "generating aon link flows for " + k + " userclass, origin zone pairs." );
            aonFlow = shortestPathHandlerGetLoadedAonFlowsRpcCall( workElements );
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
    
    
    
    
    private double[][] shortestPathHandlerGetLoadedAonFlowsRpcCall( int[][] workElements ) throws Exception {
        Vector params = new Vector();
        params.add( workElements );
        return (double[][])shortestPathHandlerClient.execute("shortestPathTreeHandler.getLoadedAonFlows", params );
    }
    
}

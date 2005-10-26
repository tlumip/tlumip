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
import com.pb.tlumip.ts.daf3.ShortestPathTreeH;

import java.util.HashMap;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Logger;



public class ShortestPathTreeHandler implements RpcHandler {

    public static String remoteHandlerName = "shortestPathTreeHandler";
    public static String remoteHandlerNode = "tcp://192.168.1.214:6001";
    
	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.ShortestPathTreeHandler");

//    public static String nodeName;
//    public static int webPort = 8001;
//    public static int tcpPort = 8002;

    RpcClient demandHandlerClient;    
    RpcClient networkHandlerClient;    

    int numLinks;
    int numUserClasses;
    int lastOriginTaz;
    int startOriginTaz;
    

    String componentPropertyName;
    String globalPropertyName;
    
	HashMap componentPropertyMap;
    HashMap globalPropertyMap;

    ResourceBundle appRb;
    ResourceBundle globalRb;
    


	public ShortestPathTreeHandler() {

        String nodeName = null;
        String handlerName = null;
        
        try {
            
            //Need a config file to initialize a Daf node
//            DafNode.getInstance().init("sp-client", TS.tsRpcConfigFileName);

            //Create RpcClients this class connects to
            try {
                nodeName = NetworkHandler.remoteHandlerNode;
                handlerName = NetworkHandler.remoteHandlerName;
                networkHandlerClient = new RpcClient( handlerName );

                nodeName = DemandHandler.remoteHandlerNode;
                handlerName = DemandHandler.remoteHandlerName;
                demandHandlerClient = new RpcClient( handlerName );
            }
            catch (MalformedURLException e) {
            
                logger.error ( "MalformedURLException caught in ShortestPathTreeH() while defining RpcClients.", e );
            
            }

        }
//        catch ( RpcException e ) {
//            logger.error ( "RpcException caught in ShortestPathTreeH() establishing " + nodeName + " as the remote machine for running the " + handlerName + " object.", e );
//            System.exit(1);
//        }
//        catch ( IOException e ) {
//            logger.error ( "IOException caught in ShortestPathTreeH() establishing " + nodeName + " as the remote machine for running the " + handlerName + " object.", e );
//            System.exit(1);
//        }
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
        
    }
    
    
    public void setup( ResourceBundle componentRb, ResourceBundle globalRb ) {
        

        this.appRb = componentRb;
        this.globalRb = globalRb;
        
        this.componentPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( componentRb );
        this.globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( globalRb );

    }
    
    
    
    public void getNetworkParameters () {
        
        try {
            
            numLinks = networkHandlerGetLinkCountRpcCall();
            numUserClasses = networkHandlerGetNumUserClassesRpcCall();
            lastOriginTaz = networkHandlerGetNumCentroidsRpcCall();
            startOriginTaz = 0;

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

        boolean[] validLinksForClass = null;

        double[][] aonFlow = new double[numUserClasses][numLinks];
        
        for (int m=0; m < numUserClasses; m++)
            Arrays.fill (aonFlow[m], 0.0);

        
        try {
            
            // build shortest path tree object and set cost and valid link attributes for this user class.
            ShortestPathTreeH sp = new ShortestPathTreeH();
    
            // set the highway network attribute on which to skim the network
            double[] linkCost = networkHandlerSetLinkGeneralizedCostRpcCall();
            double[] aon;
        
            sp.setLinkCost( linkCost );
    
            for (int m=0; m < numUserClasses; m++) {
                
                validLinksForClass = networkHandlerGetValidLinksForClassRpcCall( m );
                sp.setValidLinks( validLinksForClass );

                for (int origin=startOriginTaz; origin < lastOriginTaz; origin++) {
                
                    double[] tripTableRow = demandHandlerGetTripTableRowRpcCall(m, origin);
                    
                    double tripTableRowSum = 0.0;
                    for (int j=0; j < tripTableRow.length; j++)
                        tripTableRowSum += tripTableRow[j];
    
    
                    if (origin % 500 == 0)
                        logger.info ("assigning origin zone index " + origin + ", user class index " + m);
    
                    if (tripTableRowSum > 0.0) {
                        
                        sp.buildTree ( origin );
                        aon = sp.loadTree ( tripTableRow, m );
    
                        for (int k=0; k < aon.length; k++)
                            aonFlow[m][k] += aon[k];
                        
                    }
                    
                }
                
            }
            
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

    private double[] networkHandlerSetLinkGeneralizedCostRpcCall() throws Exception {
        // g.setLinkGeneralizedCost()
        return (double[])networkHandlerClient.execute("networkHandler.setLinkGeneralizedCost", new Vector() );
    }

    private boolean[] networkHandlerGetValidLinksForClassRpcCall( int userClass ) throws Exception {
        // g.getValidLinksForClass( int i )
        Vector params = new Vector();
        params.add( userClass );
        return (boolean[])networkHandlerClient.execute("networkHandler.getValidLinksForClassInt", params );
    }


    
    private double[] demandHandlerGetTripTableRowRpcCall(int userClass, int row) throws Exception {
        Vector params = new Vector();
        params.add(userClass);
        params.add(row);
        return (double[])demandHandlerClient.execute("demandHandler.getTripTableRow", params );
    }
    
    
}

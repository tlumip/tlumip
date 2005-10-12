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


import com.pb.common.rpc.NodeConfig;
import com.pb.common.rpc.RPC;
import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcException;
import com.pb.common.rpc.RpcHandler;
import com.pb.common.util.Convert;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.ts.daf3.ShortestPathTreeH;

import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.Echo;
import org.apache.xmlrpc.SystemHandler;
import org.apache.xmlrpc.WebServer;



public class ShortestPathTreeHandler implements RpcHandler {

    public static String remoteHandlerAddress = "http://localhost:8001";
    
	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.ShortestPathTreeHandler");

    public static String nodeName;
    public static int webPort = 8001;
    public static int tcpPort = 8002;

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
	}


    
    public Object execute (String methodName, Vector params) throws Exception {
                  
        if ( methodName.equalsIgnoreCase( "setup" ) ) {
            HashMap componentPropertyMap = (HashMap)Convert.toObject((byte[])params.get(0));
            HashMap globalPropertyMap = (HashMap)Convert.toObject((byte[])params.get(1));
            String timePeriod = (String)params.get(2);
            setup( componentPropertyMap, globalPropertyMap );
            return 0;
        }
        else if ( methodName.equalsIgnoreCase( "getMulticlassAonLinkFlows" ) ) {
            
            return Convert.toBytes( getMulticlassAonLinkFlows() );
            
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
            ShortestPathTreeH sp = new ShortestPathTreeH( networkHandlerClient );
    
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
        return (double[])Convert.toObject( (byte[])networkHandlerClient.execute("networkHandler.setLinkGeneralizedCost", new Vector() ) );
    }

    private boolean[] networkHandlerGetValidLinksForClassRpcCall( int userClass ) throws Exception {
        // g.getValidLinksForClass( int i )
        Vector params = new Vector();
        params.add( userClass );
        return (boolean[])Convert.toObject( (byte[])networkHandlerClient.execute("networkHandler.getValidLinksForClassInt", params ) );
    }


    
    private double[] demandHandlerGetTripTableRowRpcCall(int userClass, int row) throws Exception {
        Vector params = new Vector();
        params.add(userClass);
        params.add(row);
        return (double[])Convert.toObject( (byte[])demandHandlerClient.execute("demandHandler.getTripTableRow", params ) );
    }
    
    
    
    public static void main(String[] args) {

        if (args.length < 2) {
            logger.error ("usage: java " + ShortestPathTreeHandler.class.getName() + " <node-name> <config-file>");
            return;
        }

        nodeName = args[0];

        RPC.init();
        //RPC.setDebug(true);

        try {
            
            //Read config file
            logger.info ("reading config file: " + args[1]);
            NodeConfig nodeConfig = new NodeConfig();
            nodeConfig.readConfig(new File(args[1]));

            //Create webserver - register default handlers
            WebServer webserver = new WebServer(webPort);
            webserver.addHandler("math", Math.class);
            webserver.addHandler("$default", new Echo());

            //Add SystemHandler, for multicall
            SystemHandler system = new SystemHandler();
            system.addDefaultSystemHandlers();
            webserver.addHandler("system", system);

            //Register handlers only for this node
            for (int i=0; i < nodeConfig.nHandlers; i++) {
                String name = nodeConfig._handlers[i].name;
                String node = nodeConfig._handlers[i].node;

                if (nodeName.equalsIgnoreCase(node)) {
                    Class clazz = Class.forName(nodeConfig._handlers[i].className);

                    logger.info ( "handler["+i+"]: " + name + "::" + clazz.getName() );
                    webserver.addHandler(name, clazz.newInstance());
                }
            }

            //Create webserver
            webserver.start();
            logger.info ( "Web server listening on " + webPort + "..." );
            
        }
        catch (Exception e) {
            logger.error ( "Exception caught in ShortestPathTreeHandler.main().", e );
        }
    }
    
}

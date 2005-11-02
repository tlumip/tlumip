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
import com.pb.common.rpc.RpcHandler;

import com.pb.tlumip.ts.daf3.ShortestPathTreeH;

import java.util.HashMap;
import java.net.MalformedURLException;
import java.util.Vector;

import org.apache.log4j.Logger;



public class ShortestPathTreeHandler implements RpcHandler {

    public static String remoteHandlerName = "shortestPathTreeHandler";
    
	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.ShortestPathTreeHandler");


    RpcClient networkHandlerClient;    

    ShortestPathTreeH[] sp = null;
    int numUserClasses = 0;

    double[] linkCost = null;
    
	HashMap componentPropertyMap;
    HashMap globalPropertyMap;
    


	public ShortestPathTreeHandler() {

        String handlerName = null;
        
        try {
            
            //Create RpcClients this class connects to
            try {
                handlerName = NetworkHandler.remoteHandlerName;
                networkHandlerClient = new RpcClient( handlerName );
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
            boolean[][] validLinksForClass = (boolean[][]) params.get(2);
            setup( componentPropertyMap, globalPropertyMap, validLinksForClass );
            return 0;
        }
        else if ( methodName.equalsIgnoreCase( "setLinkCostArray" ) ) {
            double[] linkCost = (double[]) params.get(0);
            setLinkCostArray(linkCost);
            return 0;
        }
        else if ( methodName.equalsIgnoreCase( "getPredecessorLinkArray" ) ) {
            int userClass = (Integer) params.get(0);
            int origin = (Integer) params.get(1);
            return getPredecessorLinkArray(userClass, origin);
        }
        else {
            logger.error ( "method name " + methodName + " called from remote client is not registered for remote method calls.", new Exception() );
            return 0;
        }
        
    }


    
    public void setup( HashMap componentPropertyMap, HashMap globalPropertyMap, boolean[][] validLinksForClasses ) {
        
        this.componentPropertyMap = componentPropertyMap;
        this.globalPropertyMap = globalPropertyMap; 
        
        numUserClasses = validLinksForClasses.length;
        
        
        // build shortest path tree object and set cost and valid link attributes for this user class.
        try {
            
            sp = new ShortestPathTreeH[numUserClasses];

            for (int i=0; i < numUserClasses; i++) {
                
                sp[i] = new ShortestPathTreeH();
                
                sp[i].setValidLinks( validLinksForClasses[i] );
                
            }

        }
        catch ( Exception e ) {
            logger.error ( "Exception caught setting up ShortestPathTreeH[] in ShortestPathTreeHandler.setup().", e );
            System.exit(1);
        }
        

    }


    private void setLinkCostArray ( double[] linkCost ) {
        
        // set the highway network attribute on which to build shortest paths over the network
        this.linkCost = linkCost;
        
        // set the highway network attribute on which to skim the network
        for (int i=0; i < numUserClasses; i++) {
            sp[i].setLinkCost( linkCost );
        }

    }
    
    
    private int[] getPredecessorLinkArray ( int userClass, int origin ) {

        sp[userClass].buildTree ( origin );
        return sp[userClass].getPredecessorLink();
        
    }

    
}

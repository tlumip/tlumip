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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Logger;



public class AonFlowHandlerRpc implements AonFlowHandlerIF {

	protected static Logger logger = Logger.getLogger(AonFlowHandlerRpc.class);

    RpcClient rc = null;

	


    public AonFlowHandlerRpc( String rpcConfigFileName ) {
        
        try {
            rc = new RpcClient(HANDLER_NAME);
        }
        catch (MalformedURLException e) {
            logger.error( "MalformedURLException caught in AonFlowHandlerRpc() while defining RpcClient for " + HANDLER_NAME + ".", e);
        }
        catch (Exception e) {
            logger.error( "Exception caught in AonFlowHandlerRpc() while defining RpcClient for " + HANDLER_NAME + ".", e);
        }
    }
    
    
   

    public boolean setup( ResourceBundle componentRb, ResourceBundle globalRb, NetworkHandlerIF nh, char[] highwayModeCharacters ) {

        boolean returnValue = false;
        try {
            Vector params = new Vector();
            params.add(componentRb);
            params.add(globalRb);
            params.add(nh);
            params.add(highwayModeCharacters);
            returnValue = (Boolean)rc.execute(HANDLER_NAME+".setup", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
        
    }
    
    
    
    
    public double[][] getMulticlassAonLinkFlows () {
        
        Vector params = new Vector();

        double[][] returnValue = null;
        try {
            returnValue = (double[][])rc.execute(HANDLER_NAME+".getMulticlassAonLinkFlows", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
        
    }

}

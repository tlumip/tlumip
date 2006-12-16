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

import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;

import com.pb.common.rpc.DafNode;
import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcException;

/**
 * @author   Jim Hicks  
 * @version  Sep 20, 2005
 */
public class SpBuildLoadHandlerRpc implements SpBuildLoadHandlerIF {

    protected static transient Logger logger = Logger.getLogger(SpBuildLoadHandlerRpc.class);

    RpcClient rc = null;



    public SpBuildLoadHandlerRpc( String rpcConfigFileName ) {
        
        try {
            
            // Need a config file to initialize a Daf node
            DafNode.getInstance().initClient(rpcConfigFileName);
            
            rc = new RpcClient(HANDLER_NAME);
        }
        catch (MalformedURLException e) {
            logger.error( "MalformedURLException caught in SpBuildLoadHandlerRpc() while defining RpcClient for " + HANDLER_NAME + ".", e);
        }
        catch (Exception e) {
            logger.error( "Exception caught in SpBuildLoadHandlerRpc() while defining RpcClient for " + HANDLER_NAME + ".", e);
        }
    }
    
    
    
    public int setup(double[][][] tripTables, NetworkHandlerIF nh, BlockingQueue workQueue, HashMap controlMap, HashMap resultsMap ) {

        int returnValue = -1;
        try {
            Vector params = new Vector();
            params.add(tripTables);
            params.add(nh);
            params.add(workQueue);
            params.add(controlMap);
            params.add(resultsMap);
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setup", params );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    
    public int start() {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".start", new Vector() );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

}

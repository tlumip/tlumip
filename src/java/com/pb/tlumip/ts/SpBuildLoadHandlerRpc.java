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
import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcException;
import com.pb.tlumip.ts.daf3.AonBuildLoadCommon;
import com.pb.tlumip.ts.daf3.SpBuildLoadMt;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;



public class SpBuildLoadHandlerRpc implements SpBuildLoadHandlerIF {

    protected static Logger logger = Logger.getLogger(SpBuildLoadHandlerRpc.class);

    public static final String HANDLER_NAME = "spBuildLoadHandler";
    
    RpcClient rc = null;

    // set the frequency with which the shared class is polled to see if all threads have finshed their work.
    static final int POLLING_FREQUENCY_IN_SECONDS = 10;
    
    int numberOfThreads;
    


	public SpBuildLoadHandlerRpc(String rpcConfigFileName) {

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


    public double[][] getLoadedAonFlows ( int[][] workElements ) {

        Vector params = new Vector();
        params.add(workElements);

        double[][] returnValue = null;
        try {
            returnValue = (double[][])rc.execute(HANDLER_NAME+".getLoadedAonFlows", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
        
    }

}

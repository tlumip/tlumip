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

import java.util.Vector;
import java.io.IOException;
import java.net.MalformedURLException;

import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcException;

import org.apache.log4j.Logger;

/**
 * @author   Jim Hicks  
 * @version  Sep 20, 2005
 */
public class SpBuildLoadHandlerRpc implements SpBuildLoadHandlerIF {

    protected static transient Logger logger = Logger.getLogger(SpBuildLoadHandlerRpc.class);

    RpcClient rc = null;
    String handlerName = null;



    public SpBuildLoadHandlerRpc( String rpcConfigFileName, String handlerName ) {
        
        this.handlerName = handlerName;
        
        try {
            rc = new RpcClient(handlerName);
        }
        catch (MalformedURLException e) {
            logger.error( "MalformedURLException caught in SpBuildLoadHandlerRpc() while defining RpcClient for " + handlerName + ".", e);
        }
        catch (Exception e) {
            logger.error( "Exception caught in SpBuildLoadHandlerRpc() while defining RpcClient for " + handlerName + ".", e);
        }
    }
    
    
    // when an instance of this rpc handler is used to call the setup method of an SpBuildLoadHandler running in
    // another VM, it is not necessary to send the NetworkHandler and DemandHandler object handles, so the alternate  
    // setup method is used.  
    public int setup( String handlerName, String rpcConfigFile, int[][][] workElements, double[][][] workElementsDemand, int numUserClasses, int numLinks, int numNodes, int numZones, int[] ia, int[] ib, int[] ipa, int[] sortedLinkIndexA, int[] indexNode, int[] nodeIndex, boolean[] centroid, boolean[][] validLinksForClasses, double[] linkCost ) {

        int returnValue = -1;
        try {
            Vector params = new Vector();
            params.add(handlerName);
            params.add(rpcConfigFile);
            params.add(workElements);
            params.add(workElementsDemand);
            params.add(numUserClasses);
            params.add(numLinks);
            params.add(numNodes);
            params.add(numZones);
            params.add(ia);
            params.add(ib);
            params.add(ipa);
            params.add(sortedLinkIndexA);
            params.add(indexNode);
            params.add(nodeIndex);
            params.add(validLinksForClasses);
            params.add(linkCost);
            returnValue = (Integer)rc.execute(handlerName+".setup", params );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
        
    }
    
    
    public int start( double[] linkCost ) {
        int returnValue = -1;
        try {
            Vector params = new Vector();
            params.add(linkCost);
            returnValue = (Integer)rc.execute(handlerName+".start", params );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    
    public double[][] getResults() {
        
        double[][] returnArray = null;
        
        try {
            returnArray = (double[][])rc.execute(handlerName+".getResults", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;

    }
    
    
    public boolean handlerIsFinished() {
        boolean returnValue = false;
        try {
            returnValue = (Boolean)rc.execute(handlerName+".handlerIsFinished", new Vector() );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    

    public int getNumberOfThreads() {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(handlerName+".getNumberOfThreads", new Vector() );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

}

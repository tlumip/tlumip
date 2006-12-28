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

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Vector;

import com.pb.common.rpc.DafNode;
import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcException;

import org.apache.log4j.Logger;



/**
 * @author    Jim Hicks
 * @version   1.0, 6/30/2004
 */
public class DemandHandlerRpc implements DemandHandlerIF, Serializable {

	protected static transient Logger logger = Logger.getLogger(DemandHandlerRpc.class);

    transient RpcClient rc = null;



    public DemandHandlerRpc( String rpcConfigFileName ) {
        
        try {
            
            // Need a config file to initialize a Daf node
            DafNode.getInstance().initClient(rpcConfigFileName);
            
            rc = new RpcClient(HANDLER_NAME);
        }
        catch (MalformedURLException e) {
            logger.error( "MalformedURLException caught in RpcDemandHandler() while defining RpcClient for " + HANDLER_NAME + ".", e);
        }
        catch (Exception e) {
            logger.error( "Exception caught in RpcDemandHandler() while defining RpcClient for " + HANDLER_NAME + ".", e);
        }
    }
    
    
   
    public boolean setup( HashMap componentPropertyMap, HashMap globalPropertyMap, String timePeriod, int numCentroids, int numUserClasses, int[] nodeIndexArray, HashMap assignmentGroupMap, char[] highwayModeCharacters, boolean userClassesIncludeTruck ) {

        Vector params = new Vector();
        params.add(componentPropertyMap);
        params.add(globalPropertyMap);
        params.add(timePeriod);
        params.add(numCentroids);
        params.add(numUserClasses);
        params.add(nodeIndexArray);
        params.add(assignmentGroupMap);
        params.add(highwayModeCharacters);
        params.add(userClassesIncludeTruck);

        boolean returnValue = false;
        try {
            returnValue = (Boolean)rc.execute(HANDLER_NAME+".setup", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
        
    }
    
    
    public boolean buildDemandObject() {
        
        boolean returnValue = false;
        try {
            returnValue = (Boolean)rc.execute(HANDLER_NAME+".buildDemandObject", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
        
    }
    
    
    public double[] getTripTableRow ( int userClass, int row ) {
        
        Vector params = new Vector();
        params.add(userClass);
        params.add(row);

        double[] returnValue = null;
        try {
            returnValue = (double[])rc.execute(HANDLER_NAME+".getTripTableRow", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }

        return returnValue;

    }
    
    
    public double[][][] getMulticlassTripTables () {

        double[][][] returnValue = null;
        try {
            returnValue = (double[][][])rc.execute(HANDLER_NAME+".getMulticlassTripTables", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }

        return returnValue;

    }
    
    
    public double[][] getTripTableRowSums () {

        double[][] returnValue = null;
        try {
            returnValue = (double[][])rc.execute(HANDLER_NAME+".getTripTableRowSums", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }

        return returnValue;

    }
    
}

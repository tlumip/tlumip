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

import java.util.HashMap;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Logger;



public class DemandHandlerRpc implements DemandHandlerIF {

	protected static Logger logger = Logger.getLogger(DemandHandlerRpc.class);

    public static final String HANDLER_NAME = "demandHandler";
    
    RpcClient rc = null;

    final char[] highwayModeCharacters = { 'a', 'd', 'e', 'f', 'g', 'h' };

    String componentPropertyName;
    String globalPropertyName;
    
	HashMap componentPropertyMap;
    HashMap globalPropertyMap;

    int networkNumCentroids;
    int networkNumUserClasses;
    int[] networkNodeIndexArray;
    boolean networkUserClassesIncludeTruck;
    HashMap networkAssignmentGroupMap;

    
	double[][][] multiclassTripTable = null;
    
    double[][] multiclassTripTableRowSums = null;

	


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
    
    
   

    public boolean setup( HashMap componentPropertyMap, HashMap globalPropertyMap, String timePeriod ) {
        
        Vector params = new Vector();
        params.add(componentPropertyMap);
        params.add(globalPropertyMap);
        params.add(timePeriod);

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
    
    
    public boolean setup( ResourceBundle componentRb, ResourceBundle globalRb, String timePeriod ) {
        
        Vector params = new Vector();
        params.add(componentRb);
        params.add(globalRb);
        params.add(timePeriod);

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
    
    
    public int setNetworkAttributes( int numCentroids, int numUserClasses, int[] nodeIndexArray, HashMap assignmentGroupMap, boolean userClassesIncludeTruck ) {
        
        Vector params = new Vector();
        params.add(numCentroids);
        params.add(numUserClasses);
        params.add(nodeIndexArray);
        params.add(assignmentGroupMap);
        params.add(userClassesIncludeTruck);

        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setNetworkAttributes", params);
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

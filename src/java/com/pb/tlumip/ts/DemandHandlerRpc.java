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
import java.util.ArrayList;
import java.util.Vector;

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
            rc = new RpcClient(HANDLER_NAME);
        }
        catch (MalformedURLException e) {
            logger.error( "MalformedURLException caught in RpcDemandHandler() while defining RpcClient for " + HANDLER_NAME + ".", e);
        }
        catch (Exception e) {
            logger.error( "Exception caught in RpcDemandHandler() while defining RpcClient for " + HANDLER_NAME + ".", e);
        }
    }
    
    
   
    public boolean setup( String ptFileName, String ctFileName, int startHour, int endHour, String timePeriod, int numCentroids, int numUserClasses, int[] nodeIndexArray, char[][] assignmentGroupChars, char[] highwayModeCharacters, boolean userClassesIncludeTruck ) {

        boolean returnValue = false;

        try {
            
            ArrayList nodeIndexArrayList = Util.intList( nodeIndexArray );
            ArrayList assignmentGroupCharsList = Util.char2List( assignmentGroupChars );
            ArrayList highwayModeCharacterList = Util.charList( highwayModeCharacters );
            
            Vector params = new Vector();
            params.add(ptFileName);
            params.add(ctFileName);
            params.add(startHour);
            params.add(endHour);
            params.add(timePeriod);
            params.add(numCentroids);
            params.add(numUserClasses);
            params.add(nodeIndexArrayList);
            params.add(assignmentGroupCharsList);
            params.add(highwayModeCharacterList);
            params.add(userClassesIncludeTruck);

            returnValue = (Boolean)rc.execute(HANDLER_NAME+".setupRpc", params);
            
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

        Vector returnList = null;
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getTripTableRowRpc", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }

        double[] returnArray = Util.vectorDouble( returnList );
        return returnArray;

    }
    
    
    public double[][][] getMulticlassTripTables () {

        Vector returnList = null;
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getMulticlassTripTablesRpc", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }

        double[][][] returnArray = Util.vectorDouble3( returnList );
        return returnArray;

    }
    
    
    public double[][] getTripTableRowSums () {

        Vector returnList = null;
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getTripTableRowSumsRpc", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }

        double[][] returnArray = Util.vectorDouble2( returnList );
        return returnArray;

    }
    
}

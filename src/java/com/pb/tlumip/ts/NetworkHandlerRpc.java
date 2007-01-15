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
 * @author   Jim Hicks  
 * @version  Sep 20, 2005
 */
public class NetworkHandlerRpc implements NetworkHandlerIF, Serializable {

    protected static transient Logger logger = Logger.getLogger(NetworkHandlerRpc.class);

    transient RpcClient rc = null;



    public NetworkHandlerRpc( String rpcConfigFileName ) {
        
        try {
            rc = new RpcClient(HANDLER_NAME);
        }
        catch (MalformedURLException e) {
            logger.error( "MalformedURLException caught in NetworkHandlerRpc() while defining RpcClient for " + HANDLER_NAME + ".", e);
        }
        catch (Exception e) {
            logger.error( "Exception caught in NetworkHandlerRpc() while defining RpcClient for " + HANDLER_NAME + ".", e);
        }
    }
    
    public int setRpcConfigFileName(String configFile) {
        int returnValue = -1;
        try {
            Vector params = new Vector();
            params.add(configFile);
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setRpcConfigFileName", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public String getRpcConfigFileName() {
        String returnValue = null;
        try {
            returnValue = (String)rc.execute(HANDLER_NAME+".getRpcConfigFileName", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public int getNumCentroids() {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getNumCentroids", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public int getMaxCentroid() {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getMaxCentroid", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public boolean[] getCentroid() {
        
        boolean[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getCentroidRpc", new Vector());
            returnArray = Util.vectorBoolean( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }
    
    public int getNodeCount() {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getNodeCount", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public int getLinkCount() {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getLinkCount", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public int getNumUserClasses() {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getNumUserClasses", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public String getTimePeriod () {
        String returnValue = null;
        try {
            returnValue = (String)rc.execute(HANDLER_NAME+".getTimePeriod", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public boolean userClassesIncludeTruck() {
        boolean returnValue = false;
        try {
            returnValue = (Boolean)rc.execute(HANDLER_NAME+".userClassesIncludeTruck", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public boolean[][] getValidLinksForAllClasses () {
        
        boolean[][] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getValidLinksForAllClassesRpc", new Vector());
            returnArray = Util.vectorBoolean2( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public boolean[] getValidLinksForClass ( int userClass ) {
        
        boolean[] returnArray = null;
        Vector returnList = null;
        
        try {
            Vector params = new Vector();
            params.add(userClass);
            returnList = (Vector)rc.execute(HANDLER_NAME+".getValidLinksForClassRpc", params);
            returnArray = Util.vectorBoolean( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }

        return returnArray;
        
    }

    public boolean[] getValidLinksForClass ( char modeChar ) {
        
        boolean[] returnArray = null;
        Vector returnList = null;
        
        try {
            Vector params = new Vector();
            params.add(modeChar);
            returnList = (Vector)rc.execute(HANDLER_NAME+".getValidLinksForClassRpc", params);
            returnArray = Util.vectorBoolean( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }
    
    public int[] getNodeIndex () {
        
        int[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getNodeIndexRpc", new Vector());
            returnArray = Util.vectorInt( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public int[] getLinkType () {
        
        int[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getLinkTypeRpc", new Vector());
            returnArray = Util.vectorInt( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public char[][] getAssignmentGroupChars() {
        
        char[][] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getAssignmentGroupCharsRpc", new Vector());
            returnArray = Util.vectorChar2( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public double[] getTransitTime () {
        
        double[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getTransitTimeRpc", new Vector());
            returnArray = Util.vectorDouble( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public double[] getCongestedTime () {
        
        double[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getCongestedTimeRpc", new Vector());
            returnArray = Util.vectorDouble( returnList );            
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public double[] getDist () {
        
        double[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getDistRpc", new Vector());
            returnArray = Util.vectorDouble( returnList );            
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public String getAssignmentResultsString () {
        String returnValue = "";
        try {
            returnValue = (String)rc.execute(HANDLER_NAME+".getAssignmentResultsString", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public String getAssignmentResultsTimeString () {
        String returnValue = "";
        try {
            returnValue = (String)rc.execute(HANDLER_NAME+".getAssignmentResultsTimeString", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public double[] setLinkGeneralizedCost () {
        
        double[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".setLinkGeneralizedCostRpc", new Vector());
            returnArray = Util.vectorDouble( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public int setFlows (double[][] flow) {
        
        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            ArrayList list = Util.double2List( flow );
            params.add( list );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setFlowsRpc", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnValue;
        
    }

    public int setVolau (double[] volau) {
        
        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            ArrayList list = Util.doubleList( volau );
            params.add( list );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setVolauRpc", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnValue;
        
    }
    
    public int setTimau (double[] timau) {
        
        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            ArrayList list = Util.doubleList( timau );
            params.add( list );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setTimauRpc", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnValue;
        
    }
    
    public int setVolCapRatios () {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setVolCapRatios", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public double applyLinkTransitVdf (int hwyLinkIndex, int transitVdfIndex ) {
        double returnValue = 0.0;
        try {
            Vector params = new Vector();
            params.add(hwyLinkIndex);
            params.add(transitVdfIndex);
            returnValue = (Double)rc.execute(HANDLER_NAME+".applyLinkTransitVdf", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    
    public int applyVdfs () {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".applyVdfs", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public int applyVdfIntegrals () {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".applyVdfIntegrals", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public double getSumOfVdfIntegrals () {
        double returnValue = -1.0;
        try {
            returnValue = (Double)rc.execute(HANDLER_NAME+".getSumOfVdfIntegrals", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }


    public int logLinkTimeFreqs () {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".logLinkTimeFreqs", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public char[] getUserClasses () {
        
        char[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getUserClassesRpc", new Vector());
            returnArray = Util.vectorChar( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }
    
    public int[] getIndexNode () {
        
        int[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getIndexNodeRpc", new Vector());
            returnArray = Util.vectorInt( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public String[] getMode () {
        
        String[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getModeRpc", new Vector());
            returnArray = Util.vectorString( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public double[] getNodeX () {
        
        double[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getNodeXRpc", new Vector());
            returnArray = Util.vectorDouble( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public double[] getNodeY () {
        
        double[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getNodeYRpc", new Vector());
            returnArray = Util.vectorDouble( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public double getWalkSpeed () {
        double returnValue = 0.0;
        try {
            returnValue = (Double)rc.execute(HANDLER_NAME+".getWalkSpeed", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public int[] getIa() {
        
        int[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getIaRpc", new Vector());
            returnArray = Util.vectorInt( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public int[] getIb() {
        
        int[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getIbRpc", new Vector());
            returnArray = Util.vectorInt( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public int[] getIpa() {
        
        int[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getIpaRpc", new Vector());
            returnArray = Util.vectorInt( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnArray;
        
    }

    public int[] getSortedLinkIndexA() {
        
        int[] returnArray = null;
        Vector returnList = null;
        
        try {
            returnList = (Vector)rc.execute(HANDLER_NAME+".getSortedLinkIndexARpc", new Vector());
            returnArray = Util.vectorInt( returnList );
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error( e );
        }
        
        return returnArray;
        
    }

    public int writeNetworkAttributes ( String fileName ) {
        int returnValue = -1;
        try {
            Vector params = new Vector();
            params.add(fileName);
            returnValue = (Integer)rc.execute(HANDLER_NAME+".writeNetworkAttributes", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public int checkForIsolatedLinks () {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".checkForIsolatedLinks", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public int buildNetworkObject ( String timePeriod, String[] propertyValues ) {
        
        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            ArrayList list = Util.stringList( propertyValues );
            params.add( timePeriod );
            params.add( list );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".buildNetworkObjectRpc", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnValue;
        
    }

}

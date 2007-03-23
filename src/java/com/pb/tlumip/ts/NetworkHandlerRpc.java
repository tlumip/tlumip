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
import java.net.MalformedURLException;
import java.util.Vector;

import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcException;

import org.apache.log4j.Logger;

/**
 * @author   Jim Hicks  
 * @version  Sep 20, 2005
 */
public class NetworkHandlerRpc implements NetworkHandlerIF {

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
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public void startDataServer() {
        try {
            rc.execute(HANDLER_NAME+".start", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
    }
    
    public void stopDataServer() {
        try {
            rc.execute(HANDLER_NAME+".shutdown", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
    }
    
    public boolean getStatus() {
        boolean returnValue = false;
        try {
            returnValue = (Boolean)rc.execute(HANDLER_NAME+".getStatus", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public String getRpcConfigFileName() {
        String returnValue = null;
        try {
            returnValue = (String)rc.execute(HANDLER_NAME+".getRpcConfigFileName", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public int getNumCentroids() {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getNumCentroids", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public int getMaxCentroid() {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getMaxCentroid", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public boolean[] getCentroid() {
        
        boolean[] returnArray = null;
        
        try {
            returnArray = (boolean[])rc.execute(HANDLER_NAME+".getCentroid", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }
    
    public int getNodeCount() {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getNodeCount", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public int getLinkCount() {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getLinkCount", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public int getLinkIndex(int an, int bn) {
        int returnValue = -1;
        try {
            Vector params = new Vector();
            params.add(an);
            params.add(bn);
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getLinkIndex", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public int getNumUserClasses() {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getNumUserClasses", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public String getTimePeriod () {
        String returnValue = null;
        try {
            returnValue = (String)rc.execute(HANDLER_NAME+".getTimePeriod", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }

    public boolean userClassesIncludeTruck() {
        boolean returnValue = false;
        try {
            returnValue = (Boolean)rc.execute(HANDLER_NAME+".userClassesIncludeTruck", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public boolean[][] getValidLinksForAllClasses () {
        
        boolean[][] returnArray = null;
        
        try {
            returnArray = (boolean[][])rc.execute(HANDLER_NAME+".getValidLinksForAllClasses", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public boolean[] getValidLinksForClass ( int userClass ) {
        
        boolean[] returnArray = null;
        
        try {
            Vector params = new Vector();
            params.add(userClass);
            returnArray = (boolean[])rc.execute(HANDLER_NAME+".getValidLinksForClass", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }

        return returnArray;
        
    }

    public boolean[] getValidLinksForClass ( char modeChar ) {
        
        boolean[] returnArray = null;
        
        try {
            Vector params = new Vector();
            params.add(modeChar);
            returnArray = (boolean[])rc.execute(HANDLER_NAME+".getValidLinksForClass", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }
    
    public int[] getNodeIndex () {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getNodeIndex", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public int[] getLinkType () {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getLinkType", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public int[] getVdfIndex () {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getVdfIndex", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public char[][] getAssignmentGroupChars() {
        
        char[][] returnArray = null;
        
        try {
            returnArray = (char[][])rc.execute(HANDLER_NAME+".getAssignmentGroupChars", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getTransitTime () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getTransitTime", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getFreeFlowTime () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getFreeFlowTime", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getFreeFlowSpeed () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getFreeFlowSpeed", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getLanes () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getLanes", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getCongestedTime () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getCongestedTime", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getCapacity () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getCapacity", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getOriginalCapacity () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getOriginalCapacity", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getTotalCapacity () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getTotalCapacity", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getDist () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getDist", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getToll () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getToll", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getVolau () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getVolau", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public String getAssignmentResultsString () {
        String returnValue = "";
        try {
            returnValue = (String)rc.execute(HANDLER_NAME+".getAssignmentResultsString", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public String getAssignmentResultsTimeString () {
        String returnValue = "";
        try {
            returnValue = (String)rc.execute(HANDLER_NAME+".getAssignmentResultsTimeString", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public double[] setLinkGeneralizedCost () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".setLinkGeneralizedCost", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public int setFlows (double[][] flow) {
        
        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            params.add( flow );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setFlows", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
        
    }

    public int setVolau (double[] volau) {
        
        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            params.add( volau );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setVolau", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
        
    }
    
    public int setTimau (double[] timau) {
        
        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            params.add( timau );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setTimau", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
        
    }
    
    public int setVolCapRatios () {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setVolCapRatios", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
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
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    
    public int applyVdfs () {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".applyVdfs", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public int applyVdfIntegrals () {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".applyVdfIntegrals", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public double getSumOfVdfIntegrals () {
        double returnValue = -1.0;
        try {
            returnValue = (Double)rc.execute(HANDLER_NAME+".getSumOfVdfIntegrals", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }


    public int logLinkTimeFreqs () {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".logLinkTimeFreqs", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public char[] getUserClasses () {
        
        char[] returnArray = null;
        
        try {
            returnArray = (char[])rc.execute(HANDLER_NAME+".getUserClasses", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }
    
    public int[] getIndexNode () {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getIndexNode", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public String[] getMode () {
        
        String[] returnArray = null;
        
        try {
            returnArray = (String[])rc.execute(HANDLER_NAME+".getMode", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public int[] getNodes () {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getNodes", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getNodeX () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getNodeX", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double[] getNodeY () {
        
        double[] returnArray = null;
        
        try {
            returnArray = (double[])rc.execute(HANDLER_NAME+".getNodeY", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public double getWalkSpeed () {
        double returnValue = 0.0;
        try {
            returnValue = (Double)rc.execute(HANDLER_NAME+".getWalkSpeed", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }

    public int[] getIa() {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getIa", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public int[] getIb() {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getIb", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public int[] getIpa() {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getIpa", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public int[] getSortedLinkIndexA() {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getSortedLinkIndexA", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
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
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public int checkForIsolatedLinks () {
        int returnValue = -1;
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".checkForIsolatedLinks", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public int buildNetworkObject ( String timePeriod, String[] propertyValues ) {
        
        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            params.add( timePeriod );
            params.add( propertyValues );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".buildNetworkObject", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
        
    }

}

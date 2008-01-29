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
    
    public int[] getExternalZoneLabels() {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getExternalZoneLabels", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }
    
    public String[] getDistrictNames () {
        String[] returnValue = null;
        try {
            returnValue = (String[])rc.execute(HANDLER_NAME+".getDistrictNames", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }

    public int[] getAlphaDistrictIndex () {
        int[] returnValue = null;
        try {
            returnValue = (int[])rc.execute(HANDLER_NAME+".getAlphaDistrictIndex", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
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
    
    public int getLinkIndexExitingNode(int an) {
        int returnValue = -1;
        try {
            Vector params = new Vector();
            params.add(an);
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getLinkIndexExitingNode", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public int[] getLinksExitingNode(int an) {
        int[] returnValue = null;
        try {
            Vector params = new Vector();
            params.add(an);
            returnValue = (int[])rc.execute(HANDLER_NAME+".getLinksExitingNode", params);
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
    
    public char[] getHighwayModeCharacters() {

        char[] returnArray = null;
        
        try {
            returnArray = (char[])rc.execute(HANDLER_NAME+".getHighwayModeCharacters", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;

    }
    
    public char[] getTransitModeCharacters() {

        char[] returnArray = null;
        
        try {
            returnArray = (char[])rc.execute(HANDLER_NAME+".getTransitModeCharacters", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;

    }
    
    public boolean[] getValidLinksForTransitPaths() {
    
        boolean[] returnArray = null;
        
        try {
            returnArray = (boolean[])rc.execute(HANDLER_NAME+".getValidLinksForTransitPaths", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;

    }

    public boolean[] getValidLinksForClassChar ( int modeChar ) {
        
        boolean[] returnArray = null;
        
        try {
            Vector params = new Vector();
            // can't send a char with xml-rpc so cast it as int
            params.add((int)modeChar);
            returnArray = (boolean[])rc.execute(HANDLER_NAME+".getValidLinksForClassChar", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }
    
    public int[] getOnewayLinksForClass ( int modeChar ) {
        
        int[] returnArray = null;
        
        try {
            Vector params = new Vector();
            // can't send a char with xml-rpc so cast it as int
            params.add((int)modeChar);
            returnArray = (int[])rc.execute(HANDLER_NAME+".getOnewayLinksForClass", params);
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

    public int[] getInternalNodeToNodeTableRow() {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getInternalNodeToNodeTableRow", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;

    }
    
    public double[] getCoordsForLink(int k) {
        
        double[] returnArray = null;
        
        try {
            Vector params = new Vector();
            params.add(k);
            returnArray = (double[])rc.execute(HANDLER_NAME+".getCoordsForLink", params);
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

    public int[] getTaz () {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getTaz", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public int[] getDrops () {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getDrops", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;
        
    }

    public int[] getUniqueIds () {
        
        int[] returnArray = null;
        
        try {
            returnArray = (int[])rc.execute(HANDLER_NAME+".getUniqueIds", new Vector());
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

    public int[][] getTurnPenaltyIndices () {

        int[][] returnArray = null;
        
        try {
            returnArray = (int[][])rc.execute(HANDLER_NAME+".getTurnPenaltyIndices", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnArray;

    }
    
    public float[][] getTurnPenaltyArray () {
    
        float[][] returnArray = null;
        
        try {
            returnArray = (float[][])rc.execute(HANDLER_NAME+".getTurnPenaltyArray", new Vector());
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
    
    public String getAssignmentResultsAnodeString () {
        String returnValue = "";
        try {
            returnValue = (String)rc.execute(HANDLER_NAME+".getAssignmentResultsAnodeString", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        return returnValue;
    }
    
    public String getAssignmentResultsBnodeString () {
        String returnValue = "";
        try {
            returnValue = (String)rc.execute(HANDLER_NAME+".getAssignmentResultsBnodeString", new Vector());
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
    
    public int linkSummaryReport ( double[][] flow ) {
        int returnValue = -1;
        try {
            Vector params = new Vector();
            params.add(flow);
            returnValue = (Integer)rc.execute(HANDLER_NAME+".linkSummaryReport", params);
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

    public int getExternalNode (int internalNode) {
        
        int returnValue = -1;
        
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getExternalNode", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
        
        
    }
        
    public int getInternalNode (int externalNode) {
        
        int returnValue = -1;
        
        try {
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getInternalNode", new Vector());
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
        
        
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
    
    public int setupHighwayNetworkObject ( String timePeriod, String[] propertyValues ) {
        
        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            params.add( timePeriod );
            params.add( propertyValues );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setupHighwayNetworkObject", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
        
    }

    
    
    
    
    public int setupTransitNetworkObject ( String identifier, String period, String accessMode, String auxTransitNetworkListingFileName, String transitRouteDataFilesDirectory, String[] d221Files, String[] rteTypes, int maxRoutes ) {

        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            params.add( period );
            params.add( accessMode );
            params.add( auxTransitNetworkListingFileName );
            params.add( transitRouteDataFilesDirectory );
            params.add( d221Files );
            params.add( rteTypes );
            params.add( maxRoutes );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setupTransitNetworkObject", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;

    }

    
    
    public int[] getAuxIa(String identifier) {

        int[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (int[])rc.execute(HANDLER_NAME+".getAuxIa", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;

    }
    
    public int[] getAuxIb( String identifier ) {

        int[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (int[])rc.execute(HANDLER_NAME+".getAuxIb", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;

    }
    
    public int[] getAuxIpa( String identifier ) {
        int[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (int[])rc.execute(HANDLER_NAME+".getAuxIpa", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;

    }
    
    public int[] getAuxIpb( String identifier ) {
        int[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (int[])rc.execute(HANDLER_NAME+".getAuxIpb", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;

    }
    
    public int[] getAuxIndexa( String identifier ) {
        int[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (int[])rc.execute(HANDLER_NAME+".getAuxIndexa", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;

    }
    
    public int[] getAuxIndexb( String identifier ) {
        int[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (int[])rc.execute(HANDLER_NAME+".getAuxIndexb", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;

    }
    
    public int[] getAuxHwyLink( String identifier ) {
        int[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (int[])rc.execute(HANDLER_NAME+".getAuxHwyLink", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;

    }
    
    
    
    
    
    public int[] getLinkTrRoute( String identifier ) {

        int[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (int[])rc.execute(HANDLER_NAME+".getLinkTrRoute", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;

    }
    
    public char[] getRteMode( String identifier ) {

        char[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (char[])rc.execute(HANDLER_NAME+".getRteMode", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;

    }

    public int[] getAuxLinkType( String identifier ) {
        int[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (int[])rc.execute(HANDLER_NAME+".getAuxLinkType", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }

    public double[] getAuxWalkTime( String identifier ) {
        double[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (double[])rc.execute(HANDLER_NAME+".getAuxWalkTime", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }
    
    public double[] getAuxWaitTime( String identifier ) {
        double[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (double[])rc.execute(HANDLER_NAME+".getAuxWaitTime", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }
    
    public double[] getAuxDriveAccTime( String identifier ) {
        double[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (double[])rc.execute(HANDLER_NAME+".getAuxDriveAccTime", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }
    
    public double[] getAuxDwellTime( String identifier ) {
        double[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (double[])rc.execute(HANDLER_NAME+".getAuxDwellTime", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }

    public double[] getAuxCost( String identifier ) {
        double[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (double[])rc.execute(HANDLER_NAME+".getAuxCost", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }

    public double[] getAuxLayoverTime( String identifier ) {
        double[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (double[])rc.execute(HANDLER_NAME+".getAuxLayoverTime", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }

    public double[] getAuxInvTime( String identifier ) {
        double[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (double[])rc.execute(HANDLER_NAME+".getAuxInvTime", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }

    public double[] getAuxLinkFreq( String identifier ) {
        double[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (double[])rc.execute(HANDLER_NAME+".getAuxLinkFreq", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }

    public double[] getAuxLinkFlow( String identifier ) {
        double[] returnValue = null;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (double[])rc.execute(HANDLER_NAME+".getAuxLinkFlow", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }

    public int getAuxNodeCount( String identifier ) {
        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getAuxNodeCount", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }

    public int getAuxLinkCount( String identifier ) {
        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getAuxLinkCount", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }

    public double getAuxLinkImped (String identifier, int k) {
        double returnValue = -1;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            params.add( k );
            returnValue = (Double)rc.execute(HANDLER_NAME+".getAuxLinkImped", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }
    
    public String getAuxRouteName(String identifier, int rte) {
        String returnValue = "";
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            params.add( rte );
            returnValue = (String)rc.execute(HANDLER_NAME+".getRouteName", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }
    
    public String getAuxRouteDescription(String identifier, int rte) {
        String returnValue = "";
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            params.add( rte );
            returnValue = (String)rc.execute(HANDLER_NAME+".getAuxRouteDescription", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }
    
    public char getAuxRouteMode(String identifier, int rte) {
        char returnValue = '0';
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            params.add( rte );
            returnValue = (Character)rc.execute(HANDLER_NAME+".getAuxRouteMode", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }
    
    public String getAuxRouteType(String identifier, int rte) {
        String returnValue = "";
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            params.add( rte );
            returnValue = (String)rc.execute(HANDLER_NAME+".getAuxRouteType", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }    
    
    public int getAuxNumRoutes(String identifier) {
        int returnValue = -1;
        
        try {
            Vector params = new Vector();
            params.add( identifier );
            returnValue = (Integer)rc.execute(HANDLER_NAME+".getAuxNumRoutes", params);
        } catch (RpcException e) {
            logger.error( e.getCause().getMessage(), e );
        } catch (IOException e) {
            logger.error( e.getCause().getMessage(), e );
        }
        
        return returnValue;
    }
    
    
    

    public String getAccessMode() {
        return null;
    }
    
    public String[] getTransitRouteNames() {
        return null;
    }
    
    public String[] getTransitRouteTypes() {
        return null;
    }
    
    public int[] getTransitRouteLinkIds(String rteName) {
        return null;
    }
    
    public int[] getStationDriveAccessNodes(int stationNode) {
        return null;
    }

    

}

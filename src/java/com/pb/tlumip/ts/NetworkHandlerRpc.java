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
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.pb.common.rpc.DafNode;
import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcException;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.ts.assign.Network;

/**
 * @author   Jim Hicks  
 * @version  Sep 20, 2005
 */
public class NetworkHandlerRpc implements NetworkHandlerIF {

    protected static transient Logger logger = Logger.getLogger(NetworkHandlerRpc.class);

    RpcClient rc = null;



    public NetworkHandlerRpc( String rpcConfigFileName ) {
        
        try {
            
            // Need a config file to initialize a Daf node
            DafNode.getInstance().initClient(rpcConfigFileName);
            
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
    
    public Network getNetwork() {
        Network returnValue = null;
        try {
            returnValue = (Network)rc.execute(HANDLER_NAME+".getNetwork", new Vector());
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
        boolean[] returnValue = null;
        try {
            returnValue = (boolean[])rc.execute(HANDLER_NAME+".getCentroid", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
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
        boolean[][] returnValue = null;
        try {
            returnValue = (boolean[][])rc.execute(HANDLER_NAME+".getValidLinksForAllClasses", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public boolean[] getValidLinksForClass ( int userClass ) {
        boolean[] returnValue = null;
        try {
            Vector params = new Vector();
            params.add(userClass);
            returnValue = (boolean[])rc.execute(HANDLER_NAME+".getValidLinksForClass", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public boolean[] getValidLinksForClass ( char modeChar ) {
        boolean[] returnValue = null;
        try {
            Vector params = new Vector();
            params.add(modeChar);
            returnValue = (boolean[])rc.execute(HANDLER_NAME+".getValidLinksForClass", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public int[] getNodeIndex () {
        int[] returnValue = null;
        try {
            returnValue = (int[])rc.execute(HANDLER_NAME+".getNodeIndex", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public int[] getLinkType () {
        int[] returnValue = null;
        try {
            returnValue = (int[])rc.execute(HANDLER_NAME+".getLinkType", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public HashMap getAssignmentGroupMap() {
        HashMap returnValue = null;
        try {
            returnValue = (HashMap)rc.execute(HANDLER_NAME+".getAssignmentGroupMap", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public double[] getTransitTime () {
        double[] returnValue = null;
        try {
            returnValue = (double[])rc.execute(HANDLER_NAME+".getTransitTime", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public double[] getCongestedTime () {
        double[] returnValue = null;
        try {
            returnValue = (double[])rc.execute(HANDLER_NAME+".getCongestedTime", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public double[] getDist () {
        double[] returnValue = null;
        try {
            returnValue = (double[])rc.execute(HANDLER_NAME+".getDist", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public double getFwOfValue( boolean[] validLinks, double[][] flow ) {
        double returnValue = -1.0;
        try {
            Vector params = new Vector();
            params.add( validLinks );
            params.add( flow );
            returnValue = (Double)rc.execute(HANDLER_NAME+".getFwOfValue", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public double[] setLinkGeneralizedCost () {
        double[] returnValue = null;
        try {
            returnValue = (double[])rc.execute(HANDLER_NAME+".setLinkGeneralizedCost", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public int setFlows (double[][] flow) {
        try {
            Vector params = new Vector();
            params.add(flow);
            rc.execute(HANDLER_NAME+".setFlows", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return 1;
    }

    public int setVolau (double[] volau) {
        try {
            Vector params = new Vector();
            params.add(volau);
            rc.execute(HANDLER_NAME+".setVolau", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return 1;
    }
    
    public int setVolCapRatios () {
        try {
            rc.execute(HANDLER_NAME+".setVolCapRatios", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return 1;
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
        try {
            rc.execute(HANDLER_NAME+".applyVdfs", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return 1;
    }
    
    public int applyVdfIntegrals () {
        try {
            rc.execute(HANDLER_NAME+".applyVdfIntegrals", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return 1;
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
        try {
            rc.execute(HANDLER_NAME+".logLinkTimeFreqs", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return 1;
    }
    
    public char[] getUserClasses () {
        char[] returnValue = null;
        try {
            returnValue = (char[])rc.execute(HANDLER_NAME+".getUserClasses", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }
    
    public int[] getIndexNode () {
        int[] returnValue = null;
        try {
            returnValue = (int[])rc.execute(HANDLER_NAME+".getIndexNode", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public String[] getMode () {
        String[] returnValue = null;
        try {
            returnValue = (String[])rc.execute(HANDLER_NAME+".getMode", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public double[] getNodeX () {
        double[] returnValue = null;
        try {
            returnValue = (double[])rc.execute(HANDLER_NAME+".getNodeX", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public double[] getNodeY () {
        double[] returnValue = null;
        try {
            returnValue = (double[])rc.execute(HANDLER_NAME+".getNodeY", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
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
        int[] returnValue = null;
        try {
            returnValue = (int[])rc.execute(HANDLER_NAME+".getIa", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public int[] getIb() {
        int[] returnValue = null;
        try {
            returnValue = (int[])rc.execute(HANDLER_NAME+".getIb", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public int[] getIpa() {
        int[] returnValue = null;
        try {
            returnValue = (int[])rc.execute(HANDLER_NAME+".getIpa", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return returnValue;
    }

    public int[] getSortedLinkIndexA() {
        int[] returnValue = null;
        try {
            returnValue = (int[])rc.execute(HANDLER_NAME+".getSortedLinkIndexA", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error( e );
        }
        return returnValue;
    }

    public int writeNetworkAttributes ( String fileName ) {
        try {
            Vector params = new Vector();
            params.add(fileName);
            rc.execute(HANDLER_NAME+".writeNetworkAttributes", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return 1;
    }
    
    public int checkForIsolatedLinks () {
        try {
            rc.execute(HANDLER_NAME+".checkForIsolatedLinks", new Vector());
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        return 1;
    }
    
    public int setup( String appPropertyName, String globalPropertyName, String assignmentPeriod ) {
        
        int returnValue = -1;
        try {
            Vector params = new Vector();
            params.add(ResourceUtil.getResourceBundleAsHashMap( appPropertyName ));
            params.add(ResourceUtil.getResourceBundleAsHashMap( globalPropertyName ));
            params.add(assignmentPeriod);
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setup", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnValue;
    }

    
    public int setup( ResourceBundle appRb, ResourceBundle globalRb, String assignmentPeriod ) {
        
        int returnValue = -1;
        try {
            Vector params = new Vector();
            params.add(ResourceUtil.changeResourceBundleIntoHashMap(appRb));
            params.add(ResourceUtil.changeResourceBundleIntoHashMap(globalRb));
            params.add(assignmentPeriod);
            returnValue = (Integer)rc.execute(HANDLER_NAME+".setup", params);
        } catch (RpcException e) {
            logger.error( e );
        } catch (IOException e) {
            logger.error(  e );
        }
        
        return returnValue;
    }

}

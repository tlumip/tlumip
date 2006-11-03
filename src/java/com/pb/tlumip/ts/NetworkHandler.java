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
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.pb.common.datafile.DataReader;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.ts.assign.Network;

/**
 * @author   Jim Hicks  
 * @version  Nov 1, 2006
 */
public class NetworkHandler implements NetworkHandlerIF {

    protected static transient Logger logger = Logger.getLogger(NetworkHandler.class);

    Network g = null;
    
    
    
    public NetworkHandler() {
    }

    
    // Factory Method to return either local or remote instance
    public static NetworkHandlerIF getInstance( String rpcConfigFile ) {
    
        // if false, remote method calls on networkHandler are made 
        boolean localFlag = false;
    
        //This method needs to be written
        //if (DafNode.getInstance().isHandlerLocal("networkHandler"))
        //    localFlag = true;
    
        if (localFlag == false && rpcConfigFile != null) {
            return new RpcNetworkHandler( rpcConfigFile );
        }
        else { 
            return new NetworkHandler();
        }
        
    }
    

    // Factory Method to return local instance only
    public static NetworkHandlerIF getInstance() {
        return new NetworkHandler();
    }
    

    public Network getNetwork() {
        return g;
    }
    
    public int getNumCentroids() {
        return g.getNumCentroids();
    }
    
    public int getMaxCentroid() {
        return g.getMaxCentroid();
    }
    
    public boolean[] getCentroid() {
        return g.getCentroid();
    }
    
    public int getNodeCount() {
        return g.getNodeCount();
    }
    
    public int getLinkCount() {
        return g.getLinkCount();
    }
    
    public int getNumUserClasses() {
        return g.getNumUserClasses();
    }
    
    public String getTimePeriod () {
        return g.getTimePeriod();
    }

    public boolean userClassesIncludeTruck() {
        return g.userClassesIncludeTruck();
    }
    
    public boolean[][] getValidLinksForAllClasses () {
        return g.getValidLinksForAllClasses ();
    }

    public boolean[] getValidLinksForClass ( int userClass ) {
        return g.getValidLinksForClass ( userClass );
    }

    public boolean[] getValidLinksForClass ( char modeChar ) {
        return g.getValidLinksForClass ( modeChar );
    }
    
    public int[] getNodeIndex () {
        return g.getNodeIndex();
    }

    public int[] getLinkType () {
        return g.getLinkType();
    }

    public HashMap getAssignmentGroupMap() {
        return g.getAssignmentGroupMap();
    }

    public double[] getCongestedTime () {
        return g.getCongestedTime();
    }

    public double[] getDist () {
        return g.getDist();
    }

    public double[] setLinkGeneralizedCost () {
        return g.setLinkGeneralizedCost ();
    }

    public int setFlows (double[][] flow) {
        g.setFlows( flow );
        return 1;
    }
    
    public int setVolau (double[] volau) {
        g.setVolau( volau );
        return 1;
    }
    
    public int setVolCapRatios () {
        g.setVolCapRatios ();
        return 1;
    }
    
    public int applyVdfs () {
        g.applyVdfs();
        return 1;
    }
    
    public int applyVdfIntegrals () {
        g.applyVdfIntegrals();
        return 1;
    }
    
    public double getSumOfVdfIntegrals () {
        return g.getSumOfVdfIntegrals();
    }
    
    public int logLinkTimeFreqs () {
        g.logLinkTimeFreqs();
        return 1;
    }
    
    public char[] getUserClasses () {
        return g.getUserClasses();
    }
    
    public int[] getIndexNode () {
        return g.getIndexNode();
    }
    
    public int[] getIa() {
        return g.getIa();
    }

    public int[] getIb() {
        return g.getIb();
    }

    public int[] getIpa() {
        return g.getIpa();
    }

    public int[] getSortedLinkIndexA() {
        return g.getSortedLinkIndexA();
    }

    public int writeNetworkAttributes ( String fileName ) {
        g.writeNetworkAttributes(fileName);
        return 1;
    }
    
    public int checkForIsolatedLinks () {
        g.checkForIsolatedLinks ();
        return 1;
    }
    
    public int setup( String appPropertyName, String globalPropertyName, String assignmentPeriod ) {
        
        HashMap componentPropertyMap = ResourceUtil.getResourceBundleAsHashMap( appPropertyName );
        HashMap globalPropertyMap = ResourceUtil.getResourceBundleAsHashMap( globalPropertyName );
        String timePeriod = assignmentPeriod;
        
        buildNetworkObject ( componentPropertyMap, globalPropertyMap, timePeriod );
        
        return g.getLinkCount();

    }

    
    public int setup( HashMap componentPropertyMap, HashMap globalPropertyMap, String assignmentPeriod ) {
        
        buildNetworkObject ( componentPropertyMap, globalPropertyMap, assignmentPeriod );
        
        return g.getLinkCount();
        
    }

    
    public int setup( ResourceBundle appRb, ResourceBundle globalRb, String assignmentPeriod ) {
        
        HashMap componentPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        HashMap globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);
        String timePeriod = assignmentPeriod;
        
        buildNetworkObject ( componentPropertyMap, globalPropertyMap, timePeriod );
        
        return g.getLinkCount();
        
    }

    
    public boolean buildNetworkObject ( HashMap componentPropertyMap, HashMap globalPropertyMap, String timePeriod ) {
        
        
        try {
            
            String networkDiskObjectFile = (String)componentPropertyMap.get("NetworkDiskObject.file");

            // if no network DiskObject file exists, no previous assignments
            // have been done, so build a new Network object which initialize 
            // the congested time field for computing time related skims.
            if ( networkDiskObjectFile == null ) {
                g = new Network( componentPropertyMap, globalPropertyMap, timePeriod );
                return true;
            }
            // otherwise, read the DiskObject file and use the congested time field
            // for computing time related skims.
            else {
                g = (Network) DataReader.readDiskObject ( networkDiskObjectFile, "highwayNetwork_" + timePeriod );
            }
            
            return true;
            
        }
        catch (Exception e){
            
            logger.info ( "error building " + timePeriod + " period highway network object in NetworkHandler.", e );
            return false;
            
        }
        
    }

}

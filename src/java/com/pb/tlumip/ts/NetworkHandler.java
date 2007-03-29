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

import com.pb.common.datafile.DataReader;
import com.pb.common.rpc.DafNode;

import com.pb.tlumip.ts.assign.Network;

import org.apache.log4j.Logger;

/**
 * @author   Jim Hicks  
 * @version  Nov 1, 2006
 */
public class NetworkHandler implements NetworkHandlerIF {

    protected static transient Logger logger = Logger.getLogger(NetworkHandler.class);

    static final int port = 6003;
    static final String dataServerName = "networkDataServer";
    
    Network g = null;
    NetworkDataServer ns = null;
    String rpcConfigFile = null;

    
    public NetworkHandler() {
        ns = NetworkDataServer.getInstance( this, port, dataServerName );
    }

    
    // Factory Method to return either local or remote instance
    public static NetworkHandlerIF getInstance( String rpcConfigFile ) {
    
        if ( rpcConfigFile == null ) {

            // if rpc config file is null, then all handlers are local, so return local instance
            return new NetworkHandler();

        }
        else {
            
            // return either a local instance or an rpc instance depending on how the handler was defined.
            Boolean isLocal = DafNode.getInstance().isHandlerLocal( HANDLER_NAME );

            if ( isLocal == null )
                // handler name not found in config file, so create a local instance.
                return new NetworkHandler();
            else 
                // handler name found in config file but is not local, so create an rpc instance.
                return new NetworkHandlerRpc( rpcConfigFile );

        }
        
    }
    

    // Factory Method to return local instance only
    public static NetworkHandlerIF getInstance() {
        return new NetworkHandler();
    }

    
    
    public boolean getStatus() {
        return true;
    }
    
    public void startDataServer() {
        ns.startServer();
    }
    
    public void stopDataServer() {
        ns.stopServer();
    }
    
    public int setRpcConfigFileName(String configFile) {
        this.rpcConfigFile = configFile;
        return 1;
    }

    public String getRpcConfigFileName() {
        return rpcConfigFile;
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
    
    public int getLinkIndex(int an, int bn) {
        return g.getLinkIndex(an,bn);
    }
    
    public int getNumUserClasses() {
        return g.getNumUserClasses();
    }
    
    public String getTimePeriod() {
        return g.getTimePeriod();
    }

    public boolean userClassesIncludeTruck() {
        return g.userClassesIncludeTruck();
    }
    
    public boolean[][] getValidLinksForAllClasses () {
        return g.getValidLinksForAllClasses();
    }

    public boolean[] getValidLinksForClass ( int userClass ) {
        return g.getValidLinksForClass ( userClass );
    }

    public boolean[] getValidLinksForClassChar ( int modeChar ) {
        // can't pass char in xml-rpc, so the remote call cast the char to int, and now we need to cast it back to char.
        return g.getValidLinksForClassChar ( (char)modeChar );
    }
    
    public int[] getNodeIndex () {
        return g.getNodeIndex();
    }

    public int[] getVdfIndex () {
        return g.getVdfIndex();
    }

    public int[] getLinkType () {
        return g.getLinkType();
    }

    public char[][] getAssignmentGroupChars() {
        return g.getAssignmentGroupChars();
    }

    public double[] getLanes () {
        return g.getLanes();
    }

    public double[] getCapacity () {
        return g.getCapacity();
    }

    public double[] getOriginalCapacity () {
        return g.getOriginalCapacity();
    }

    public double[] getTotalCapacity () {
        return g.getTotalCapacity();
    }

    public double[] getCongestedTime () {
        return g.getCongestedTime();
    }

    public double[] getTransitTime () {
        return g.getTransitTime();
    }

    public double[] getFreeFlowTime () {
        return g.getFreeFlowTime();
    }

    public double[] getFreeFlowSpeed () {
        return g.getFreeFlowSpeed();
    }

    public double[] getDist () {
        return g.getDist();
    }

    public double[] getToll () {
        return g.getToll();
    }

    public double[] getVolau () {
        return g.getVolau();
    }

    public String getAssignmentResultsString () {
        return g.getAssignmentResultsString();
    }
    
    public String getAssignmentResultsTimeString () {
        return g.getAssignmentResultsTimeString();
    }
    
    public double[] setLinkGeneralizedCost () {
        return g.setLinkGeneralizedCost ();
    }

    public int setFlows (double[][] flow) {
        g.setFlows( flow );

        double[] volau = new double[g.getLinkCount()];
        for (int i=0; i < flow.length; i++)
            for (int j=0; j < flow[i].length; j++)
                volau[j] += flow[i][j];
                
        g.setVolau(volau);
        
        return 1;
    }
    
    public int setVolau (double[] volau) {
        g.setVolau( volau );
        return 1;
    }
    
    public int setTimau (double[] timau) {
        g.setTimau( timau );
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
    
    public double applyLinkTransitVdf (int hwyLinkIndex, int transitVdfIndex ) {
        return g.applyLinkTransitVdf(hwyLinkIndex, transitVdfIndex);
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
    
    public char[] getUserClasses() {
        return g.getUserClasses();
    }
    
    public String[] getMode() {
        return g.getMode();
    }

    public int[] getIndexNode () {
        return g.getIndexNode();
    }
    
    public int[] getNodes() {
        return g.getNodes();
    }
    
    public double[] getNodeX() {
        return g.getNodeX();
    }
    
    public double[] getNodeY() {
        return g.getNodeY();
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

    public double getWalkSpeed () {
        return g.getWalkSpeed();
    }

    public int writeNetworkAttributes ( String fileName ) {
        g.writeNetworkAttributes(fileName);
        return 1;
    }
    
    public int checkForIsolatedLinks () {
        g.checkForIsolatedLinks ();
        return 1;
    }
    
    public int buildNetworkObject ( String timePeriod, String[] propertyValues  ) {
        
        try {
            
            String networkFileName = propertyValues[NETWORK_FILENAME_INDEX];
            String networkDiskObjectFileName = propertyValues[NETWORK_DISKOBJECT_FILENAME_INDEX];
            
            // if no network DiskObject file exists, no previous assignments
            // have been done, so build a new Network object which initialize 
            // the congested time field for computing time related skims.
            if ( networkDiskObjectFileName == null || networkDiskObjectFileName.equals("") ) {
                logger.info ( "building a new Network object from " + networkFileName + " for the " + timePeriod + " period." );
                g = new Network( timePeriod, propertyValues );
                return g.getLinkCount();
            }
            // otherwise, read the DiskObject file and use the congested time field
            // for computing time related skims.
            else {
                g = (Network) DataReader.readDiskObject ( networkDiskObjectFileName, "highwayNetwork_" + timePeriod );
            }
            
            return g.getLinkCount();
            
        }
        catch (Exception e){
            
            logger.error ( "error building " + timePeriod + " period highway network object in NetworkHandler.", e );
            return -1;
            
        }
        
    }

}

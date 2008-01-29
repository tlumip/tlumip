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

import com.pb.common.datafile.DataReader;
import com.pb.common.rpc.DafNode;

import com.pb.tlumip.ts.assign.Network;
import com.pb.tlumip.ts.transit.AuxTrNet;
import com.pb.tlumip.ts.transit.TrRoute;

import org.apache.log4j.Logger;

/**
 * @author   Jim Hicks  
 * @version  Nov 1, 2006
 */
public class NetworkHandler implements NetworkHandlerIF {

    protected static transient Logger logger = Logger.getLogger(NetworkHandler.class);

    Network g = null;
    
    HashMap transitNetworks; 
    
    ShortestPathTreeH sp = null;
    NetworkDataServer ns = null;
    String rpcConfigFile = null;

    
    public NetworkHandler() {
        ns = NetworkDataServer.getInstance( this, networkDataServerPort, dataServerName );
        transitNetworks = new HashMap();
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
    
    public int[] getExternalZoneLabels () {
        return g.getExternalZoneLabels();
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
    
    public int getLinkIndexExitingNode(int an) {
        int[] linksExiting = g.getLinksExitingNode(an);
        return linksExiting[0];
    }
    
    public int[] getLinksEnteringNode(int an) {
        int[] linksExiting = g.getLinksEnteringNode(an);
        return linksExiting;
    }
    
    public int[] getLinksExitingNode(int an) {
        int[] linksExiting = g.getLinksExitingNode(an);
        return linksExiting;
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
    
    public char[] getHighwayModeCharacters() {
        return g.getHighwayModeCharacters();
    }
    
    public char[] getTransitModeCharacters() {
        return g.getTransitModeCharacters();
    }
    
    public boolean[][] getValidLinksForAllClasses () {
        return g.getValidLinksForAllClasses();
    }

    public boolean[] getValidLinksForClass ( int userClass ) {
        return g.getValidLinksForClass ( userClass );
    }

    public int[] getOnewayLinksForClass ( int userClass ) {
        return g.getOnewayLinksForClass ( userClass );
    }

    public boolean[] getValidLinksForTransitPaths() {
        return g.getValidLinksForTransitPaths();
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

    public int[] getTaz () {
        return g.getTaz();
    }

    public int[] getDrops () {
        return g.getDrops();
    }

    public int[] getUniqueIds () {
        return g.getUniqueIds();
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

    public double[] getVolad () {
        return g.getVolad();
    }

    public int[][] getTurnPenaltyIndices () {
        return g.getTurnPenaltyIndices();
    }
    
    public float[][] getTurnPenaltyArray () {
        return g.getTurnPenaltyArray();
    }
    
    public String getAssignmentResultsString () {
        return g.getAssignmentResultsString();
    }
    
    public String getAssignmentResultsAnodeString () {
        return g.getAssignmentResultsAnodeString();
    }
    
    public String getAssignmentResultsBnodeString () {
        return g.getAssignmentResultsBnodeString();
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
    
    public int linkSummaryReport ( double[][] flow ) {
        g.linkSummaryReport( flow );
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
    
    public int getExternalNode (int internalNode) {
        return g.getExternalNode(internalNode);
    }
    
    public int getInternalNode (int externalNode) {
        return g.getInternalNode(externalNode);
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
    
    public int[] getInternalNodeToNodeTableRow () {
        return g.getInternalNodeToNodeTableRow();
    }
    
    public double[] getCoordsForLink(int k) {
        return g.getCoordsForLink(k);
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
        g.writeHighwayAsignmentResults(fileName);
        return 1;
    }
    
    public int checkForIsolatedLinks () {
        g.checkForIsolatedLinks ();
        return 1;
    }
    
    public int setupHighwayNetworkObject ( String timePeriod, String[] propertyValues  ) {
        
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




    public int setupTransitNetworkObject ( String identifier, String period, String accessMode, String auxTransitNetworkListingFileName, String transitNetworkListings, String[] d221Files, String[] rteTypes, int maxRoutes ) {
        
        // create transit routes object
        TrRoute tr = new TrRoute ( maxRoutes );

        //read transit route info from Emme/2 for d221 file for the specified time period
        tr.readTransitRoutes ( this, d221Files, rteTypes );
            
//        // associate transit segment node sequence with highway link indices
//        tr.getLinkIndices (this);


        if ( transitNetworkListings != null )
            tr.printTransitRouteFile ( transitNetworkListings );


        // create an auxilliary transit network object
        AuxTrNet ag = new AuxTrNet(this, tr);

        // build the auxilliary links for the given transit routes object
        ag.buildAuxTrNet ( accessMode );
        
        // define the forward star index arrays, first by anode then by bnode
        logger.info( "creating forward star representation for transit network.");
        ag.setForwardStarArrays ();
        logger.info( "creating backward star representation for transit network.");
        ag.setBackwardStarArrays ();


        // store the transit network built in a HashMap so that several transit network objects can exist in parallel.
        transitNetworks.put(identifier, ag);
        
        
        //ag.printAuxTrLinks (28, tr);
        if ( auxTransitNetworkListingFileName != null )
            ag.printAuxTranNetwork( auxTransitNetworkListingFileName );

        
        logger.info ( String.format("done creating %s transit network for %s period and %s access mode.", identifier, period, accessMode ) );

        
        return 1;
    }
    

    
    
    public int[] getAuxIa( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getIa();
    }
    
    public int[] getAuxIb( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getIb();
    }
    
    public int[] getAuxIpa( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getIpa();
    }
    
    public int[] getAuxIpb( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getIpb();
    }
    
    public int[] getAuxIndexa( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getIndexa();
    }
    
    public int[] getAuxIndexb( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getIndexb();
    }
    
    public int[] getAuxHwyLink( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getHwyLink();
    }

    
    public int[] getLinkTrRoute( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getLinkTrRoute();
    }
    
    public char[] getRteMode( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getRteMode();
    }

    public int[] getAuxLinkType( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getLinkType();
    }

    public double[] getAuxWalkTime( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getWalkTime();
    }
    
    public double[] getAuxWaitTime( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getWaitTime();
    }
    
    public double[] getAuxDriveAccTime( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getDriveAccTime();
    }
    
    public double[] getAuxDwellTime( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getDwellTime();
    }

    public double[] getAuxCost( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getCost();
    }

    public double[] getAuxLayoverTime( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getLayoverTime();
    }

    public double[] getAuxInvTime( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getInvTime();
    }

    public double[] getAuxLinkFreq( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getFreq();
    }

    public double[] getAuxLinkFlow( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getFlow();
    }

    public int getAuxNodeCount( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getAuxNodeCount();
    }

    public int getAuxLinkCount( String identifier ) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getAuxLinkCount();
    }

    public double getAuxLinkImped (String identifier, int k) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getLinkImped(k);
    }
    
    public String getAuxRouteName(String identifier, int rte) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getRouteName(rte);
    }
    
    public String getAuxRouteDescription(String identifier, int rte) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getRouteDescription(rte);
    }
    
    public char getAuxRouteMode(String identifier, int rte) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getRouteMode(rte);
    }
    
    public String getAuxRouteType(String identifier, int rte) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getRouteType(rte);
    }
    
    public int getAuxNumRoutes(String identifier) {
        AuxTrNet ag = (AuxTrNet)transitNetworks.get(identifier);
        return ag.getNumRoutes();
    }
    
    /*
    
    // Transit Network handling methods

    public String getAccessMode() {
        return ag.getAccessMode();
    }

    public String[] getTransitRouteNames() {
        if ( ag == null)
            return new String[0];
        
        String[] names = new String[ag.getNumRoutes()];
        String[] tempNames = ag.getRouteNames();
        for (int i=0; i < names.length; i++)
            names[i] = tempNames[i];
        return names;
    }
    
    public String[] getTransitRouteTypes() {
        String[] types = new String[ag.getNumRoutes()];
        String[] tempTypes = ag.getRouteTypes();
        for (int i=0; i < types.length; i++)
            types[i] = tempTypes[i];
        return types;
    }
    
    public int[] getTransitRouteLinkIds(String rteName) {
        return ag.getRouteLinkIds (rteName);
    }
    
    public int[] getStationDriveAccessNodes(int stationNode) {
        return ag.getStationDriveAccessNodes(stationNode);
    }

    public Vector getDriveAccessLinkCoords(Vector routeNames, Vector linkIds) {
        return ag.getDriveAccessLinkCoords(routeNames, linkIds);
    }
    
    public Vector getCentroidTransitDriveAccessLinkCoords(Vector zones) {
        return ag.getCentroidTransitDriveAccessLinkCoords(zones);       
    }
    
    
    */
    
    
    public int[] getShortestPathNodes(int startNode, int endNode) {
        
        if ( sp == null ) {
            sp = new ShortestPathTreeH( getLinkCount(), getNodeCount(), getNumCentroids(), getIa(), getIb(), getIpa(), getSortedLinkIndexA(), getIndexNode(), getNodeIndex(), getCentroid(), getTurnPenaltyIndices(), getTurnPenaltyArray() );

            // set the highway network attribute on which to skim the network
            sp.setLinkCost( setLinkGeneralizedCost() );
            
            // set the highway network valid links attribute for links which may appear in paths between unconnected highway network nodes in transit routes.
            sp.setValidLinks( getValidLinksForTransitPaths() );
        }
        
        sp.buildPath( startNode, endNode );
        int[] nodeList = sp.getNodeList();
        
        return nodeList;
    }
    
    public int[] getSpLinkInRouteIdList(int startNode, int endNode) {
        
        if ( sp == null ) {
            sp = new ShortestPathTreeH( getLinkCount(), getNodeCount(), getNumCentroids(), getIa(), getIb(), getIpa(), getSortedLinkIndexA(), getIndexNode(), getNodeIndex(), getCentroid(), getTurnPenaltyIndices(), getTurnPenaltyArray() );

            // set the highway network attribute on which to skim the network
            sp.setLinkCost( setLinkGeneralizedCost() );
            
            // set the highway network valid links attribute for links which may appear in paths between unconnected highway network nodes in transit routes.
            sp.setValidLinks( getValidLinksForTransitPaths() );
        }
        
        int[] nodeIndex = getNodeIndex();
        sp.buildPath( nodeIndex[startNode], nodeIndex[endNode] );
        int[] linkIdList = sp.getLinkIdList();
        
        return linkIdList;
    }

    
    public int[] getShortestPathLinks(int startNode, int endNode) {
        
        if ( sp == null ) {
            sp = new ShortestPathTreeH( getLinkCount(), getNodeCount(), getNumCentroids(), getIa(), getIb(), getIpa(), getSortedLinkIndexA(), getIndexNode(), getNodeIndex(), getCentroid(), getTurnPenaltyIndices(), getTurnPenaltyArray() );

            // set the highway network attribute on which to skim the network
            sp.setLinkCost( setLinkGeneralizedCost() );
            
            sp.setValidLinks( getValidLinksForClassChar( 'a' ) );

        }
        
        int[] ia = getIa();
        int[] ib = getIb();
        double[] dist = getDist();
        double[] time = getCongestedTime();
        
        int[] nodeIndex = getNodeIndex();
        int[] indexNode = getIndexNode();
        sp.buildPath( nodeIndex[startNode], nodeIndex[endNode] );
        int[] linkIdList = sp.getLinkIdList();
        
        for (int i=0; i < linkIdList.length; i++) {
            int k = linkIdList[i];
            logger.info( String.format("%3d %6d %6d %6d %8.2f %8.2f %8.2f", i, k, indexNode[ia[k]], indexNode[ib[k]], dist[k], time[k], (60.0*dist[k]/time[k]) ) );
        }
        
        return linkIdList;
    }

    
    public int[] getAlphaDistrictIndex () {
        return g.getAlphaDistrictArray();
    }
    
    public String[] getDistrictNames () {
        return g.getDistrictNames();
    }
    

}

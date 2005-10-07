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

import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.apache.xmlrpc.*;
import java.io.File;

import com.pb.common.datafile.DataReader;
import com.pb.common.datafile.DataWriter;
import com.pb.common.matrix.Matrix;
import com.pb.common.rpc.NodeConfig;
import com.pb.common.rpc.RPC;
import com.pb.common.rpc.RpcHandler;
import com.pb.common.util.Convert;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.ts.assign.Network;
import com.pb.tlumip.ts.assign.Skims;

/**
 * @author   Jim Hicks  
 * @version  Sep 20, 2005
 */
public class NetworkHandler implements RpcHandler {

    public static String remoteHandlerAddress = "http://localhost:6001";
    
    protected static transient Logger logger = Logger.getLogger("com.pb.tlumip.ts.NetworkHandler");

    public static String nodeName;
    public static int webPort = 6001;
    public static int tcpPort = 6002;


    HashMap componentPropertyMap;
    HashMap globalPropertyMap;
    String timePeriod;
    
    Network g = null;
    
    
    
    public NetworkHandler() {
    }

    
    
    public Object execute (String methodName, Vector params) throws Exception {
    
        if ( methodName.equalsIgnoreCase( "getNumCentroids" ) ) {
            return getNumCentroids();
        }
        else if ( methodName.equalsIgnoreCase( "getMaxCentroid" ) ) {
            return getMaxCentroid();
        }
        else if ( methodName.equalsIgnoreCase( "getNodeCount" ) ) {
            return getNodeCount();
        }
        else if ( methodName.equalsIgnoreCase( "getLinkCount" ) ) {
            return getLinkCount();
        }
        else if ( methodName.equalsIgnoreCase( "getNumUserClasses" ) ) {
            return getNumUserClasses();
        }
        else if ( methodName.equalsIgnoreCase( "getTimePeriod" ) ) {
            return getTimePeriod();
        }
        else if ( methodName.equalsIgnoreCase( "userClassesIncludeTruck" ) ) {
            return userClassesIncludeTruck();
        }
        else if ( methodName.equalsIgnoreCase( "getValidLinksForClassInt" ) ) {
            return getValidLinksForClass( (Integer)params.get(0) );
        }
        else if ( methodName.equalsIgnoreCase( "getValidLinksForClassChar" ) ) {
            return getValidLinksForClass( (Character)params.get(0) );
        }
        else if ( methodName.equalsIgnoreCase( "getNodeIndex" ) ) {
            return getNodeIndex();
        }
        else if ( methodName.equalsIgnoreCase( "getLinkType" ) ) {
            return getLinkType();
        }
        else if ( methodName.equalsIgnoreCase( "getAssignmentGroupMap" ) ) {
            return getAssignmentGroupMap();
        }
        else if ( methodName.equalsIgnoreCase( "getCongestedTime" ) ) {
            return getCongestedTime();
        }
        else if ( methodName.equalsIgnoreCase( "getDist" ) ) {
            return getDist();
        }
        else if ( methodName.equalsIgnoreCase( "setLinkGeneralizedCost" ) ) {
            return setLinkGeneralizedCost();
        }
        else if ( methodName.equalsIgnoreCase( "setFlows" ) ) {
            double[][] flows = (double[][])params.get(0);
            setFlows( flows );
            return null;
        }
        else if ( methodName.equalsIgnoreCase( "setVolau" ) ) {
            double[] volau = (double[])params.get(0);
            setVolau( volau );
            return null;
        }
        else if ( methodName.equalsIgnoreCase( "setVolCapRatios" ) ) {
            double[] volau = (double[])params.get(0);
            setVolCapRatios( volau );
            return null;
        }
        else if ( methodName.equalsIgnoreCase( "applyVdfs" ) ) {
            boolean[] validLinks = (boolean[])params.get(0);
            applyVdfs( validLinks );
            return null;
        }
        else if ( methodName.equalsIgnoreCase( "applyVdfIntegrals" ) ) {
            boolean[] validLinks = (boolean[])params.get(0);
            applyVdfIntegrals( validLinks );
            return null;
        }
        else if ( methodName.equalsIgnoreCase( "getSumOfVdfIntegrals" ) ) {
            boolean[] validLinks = (boolean[])params.get(0);
            return getSumOfVdfIntegrals( validLinks );
        }
        else if ( methodName.equalsIgnoreCase( "logLinkTimeFreqs" ) ) {
            boolean[] validLinks = (boolean[])params.get(0);
            logLinkTimeFreqs( validLinks );
            return null;
        }
        else if ( methodName.equalsIgnoreCase( "createSelectLinkAnalysisDiskObject" ) ) {
            double[] fwFlowProps = (double[])params.get(0);
            createSelectLinkAnalysisDiskObject( fwFlowProps );
            return null;
        }
        else if ( methodName.equalsIgnoreCase( "buildNetworkObject" ) ) {
            
            componentPropertyMap = (HashMap)Convert.toObject((byte[])params.get(0));
            globalPropertyMap = (HashMap)Convert.toObject((byte[])params.get(1));
            timePeriod = (String)params.get(2);
            return (Boolean)buildNetworkObject();
            
        }
        else {
            logger.error ( "method name " + methodName + " called from remote client is not registered for remote method calls.", new Exception() );
            return null;
        }
        
    }
    
    
    
    public int getLinkCount() {
        return g.getLinkCount();
    }
    
    public int getNodeCount() {
        return g.getNodeCount();
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
    
    public int getNumUserClasses() {
        return g.getNumUserClasses();
    }
    
    public char[] getUserClasses () {
        return g.getUserClasses();
    }
    
    public boolean userClassesIncludeTruck() {
        return g.userClassesIncludeTruck();
    }
    
    public HashMap getAssignmentGroupMap() {
        return g.getAssignmentGroupMap();
    }

    public String getTimePeriod () {
        return g.getTimePeriod();
    }

    public int[] getNodeIndex () {
        return g.getNodeIndex();
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

    public double[] getCongestedTime () {
        return g.getCongestedTime();
    }

    public double[] getDist () {
        return g.getDist();
    }

    public int[] getLinkType () {
        return g.getLinkType();
    }

    public boolean[] getValidLinksForClass ( int userClass ) {
        return g.getValidLinksForClass ( userClass );
    }

    public boolean[] getValidLinksForClass ( char modeChar ) {
        return g.getValidLinksForClass ( modeChar );
    }
    
    public double[] setLinkGeneralizedCost () {
        return g.setLinkGeneralizedCost ();
    }

    public void setFlows (double[][] flow) {
        g.setFlows( flow );
    }
    
    public void setVolau (double[] volau) {
        g.setVolau( volau );
    }
    
    public void logLinkTimeFreqs ( boolean[] validLinks ) {
        g.logLinkTimeFreqs( validLinks );
    }
    
    public void applyVdfs ( boolean[] validLinks ) {
        g.applyVdfs( validLinks );
    }
    
    public void applyVdfIntegrals ( boolean[] validLinks ) {
        g.applyVdfIntegrals( validLinks );
    }
    
    public double getSumOfVdfIntegrals ( boolean[] validLinks ) {
        return g.getSumOfVdfIntegrals( validLinks );
    }
    
    public void setVolCapRatios ( double[] volau ) {
        g.setVolCapRatios ( volau );
    }
    
    public void writeNetworkAttributes ( String fileName ) {
        g.writeNetworkAttributes(fileName);
    }
    
    public void checkForIsolatedLinks () {
        g.checkForIsolatedLinks ();
    }
    
    public void checkODConnectivity ( double[][][] trips ) {

        double[][] linkAttributes = new double[2][];
        linkAttributes[0] = getDist();
        linkAttributes[1] = getCongestedTime();
        
        char[] userClasses = g.getUserClasses();

        Skims skims = new Skims(this, componentPropertyMap, globalPropertyMap);

        
        for (int m=0; m < userClasses.length; m++) {

            double total = 0.0;
            for (int i=0; i < trips[m].length; i++)
                for (int j=0; j < trips[m][i].length; j++)
                    total += trips[m][i][j];
            
                    
            // log the average sov trip travel distance and travel time for this assignment
            logger.info("Generating Time and Distance peak skims for subnetwork " + userClasses[m] + " (class " + m + ") ...");
            
            if (total > 0.0) {

                Matrix[] skimMatrices = skims.getHwySkimMatrices( timePeriod, linkAttributes, userClasses[m] );

                logger.info( "Total " + timePeriod + " demand for subnetwork " + userClasses[m] + " (class " + m + ") = " + total + " trips."); 

                double[] distSummaries = skims.getAvgTripSkims ( trips[m], skimMatrices[0] );
                
                logger.info( "Average subnetwork " + userClasses[m] + " (class " + m + ") " + timePeriod + " trip travel distance = " + distSummaries[0] + " miles."); 
                logger.info( "Number of disconnected O/D pairs in subnetwork " + userClasses[m] + " (class " + m + ") based on distance = " + distSummaries[1]);

                double[] timeSummaries = skims.getAvgTripSkims ( trips[m], skimMatrices[1] );
                
                logger.info( "Average subnetwork " + userClasses[m] + " (class " + m + ") " + timePeriod + " trip travel time = " + timeSummaries[1] + " minutes."); 
                logger.info( "Number of disconnected O/D pairs in subnetwork " + userClasses[m] + " (class " + m + ") based on time = " + timeSummaries[1]);
                
            }
            else {
                
                logger.info("No demand for subnetwork " + userClasses[m] + " (class " + m + ") therefore, no average time or distance calculated.");
                
            }
                    
        }

    }
    
    
    
    public void checkNetworkForIsolatedLinks () {
        g.checkForIsolatedLinks ();
    }
    
    
    
    public void checkAllODPairsForNetworkConnectivity () {
        
        int numCentroids = getNumCentroids();
        int numUserClasses = getNumUserClasses();
        

        double[][][] dummyTripTable = new double[numUserClasses][numCentroids+1][numCentroids+1];
        for(int i=0; i < numUserClasses - 1; i++) {
            for(int j=0; j < numCentroids + 1; j++) {
                Arrays.fill(dummyTripTable[i][j], 1.0);
            }
        }
      
        checkODConnectivity(dummyTripTable);

    }
    
    
    
    public void checkODPairsWithTripsForNetworkConnectivity ( double[][][] multiclassTripTable ) {
        checkODConnectivity(multiclassTripTable);
    }

    
    
    
    public void createSelectLinkAnalysisDiskObject ( double[] fwFlowProps ) {
        
        // get the locations of the files for storing the network and assignment proportions
        String networkDiskObjectFile = (String)componentPropertyMap.get("NetworkDiskObject.file");
        String proportionsDiskObjectFile = (String)componentPropertyMap.get("ProportionsDiskObject.file");

            
        // write the network and saved proportions to DiskObject files for subsequent select link analysis
        if ( networkDiskObjectFile != null )
            DataWriter.writeDiskObject ( g, networkDiskObjectFile, "highwayNetwork_" + timePeriod );
        
        if ( proportionsDiskObjectFile != null )
            DataWriter.writeDiskObject ( fwFlowProps, proportionsDiskObjectFile, "fwProportions_" + timePeriod );
    
    }


    public void setup( String appPropertyName, String globalPropertyName, String assignmentPeriod ) {
        
        componentPropertyMap = ResourceUtil.getResourceBundleAsHashMap( appPropertyName );
        globalPropertyMap = ResourceUtil.getResourceBundleAsHashMap( globalPropertyName );
        timePeriod = assignmentPeriod;
        
        buildNetworkObject ();
        
    }

    
    public void setup( ResourceBundle appRb, ResourceBundle globalRb, String assignmentPeriod ) {
        
        componentPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);
        timePeriod = assignmentPeriod;
        
        buildNetworkObject ();
        
    }

    
    public boolean buildNetworkObject () {
        
        
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



    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("usage: java " + NetworkHandler.class.getName() + " <node-name> <config-file>");
            return;
        }

        nodeName = args[0];
        System.out.println("starting: " + nodeName);

        RPC.init();
        //RPC.setDebug(true);

        try {
            
            //Read config file
            System.out.println("reading config file: " + args[1]);
            NodeConfig nodeConfig = new NodeConfig();
            nodeConfig.readConfig(new File(args[1]));

            //Create webserver - register default handlers
            WebServer webserver = new WebServer(webPort);
            webserver.addHandler("math", Math.class);
            webserver.addHandler("$default", new Echo());

            //Add SystemHandler, for multicall
            SystemHandler system = new SystemHandler();
            system.addDefaultSystemHandlers();
            webserver.addHandler("system", system);

            //Register handlers only for this node
            for (int i=0; i < nodeConfig.nHandlers; i++) {
                String name = nodeConfig._handlers[i].name;
                String node = nodeConfig._handlers[i].node;

                if (nodeName.equalsIgnoreCase(node)) {
                    Class clazz = Class.forName(nodeConfig._handlers[i].className);

                    logger.info ( "handler["+i+"]: " + name + "::" + clazz.getName() );
                    webserver.addHandler(name, clazz.newInstance());
                }
            }

            //Create webserver
            webserver.start();
            logger.info ( "Web server listening on " + webPort + "..." );
            
        }
        catch (Exception e) {
            logger.error ( "Exception caught in NetworkHandler.main().", e );
        }
    }
    
}

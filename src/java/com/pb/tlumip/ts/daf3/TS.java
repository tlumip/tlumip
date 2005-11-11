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
package com.pb.tlumip.ts.daf3;

/**
 * 
 * @author Jim Hicks
 * @version 1.0, 6/30/2004
 */

import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.ShortestPathTreeHandler;

import com.pb.common.rpc.DafNode;
import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcException;
import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.util.Vector;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

public class TS {

    protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.daf3");

    HashMap tsPropertyMap;

    HashMap globalPropertyMap;

    ResourceBundle appRb;

    ResourceBundle globalRb;

    public static String tsRpcConfigFileName;

    String tsPropertyName;

    String globalPropertyName;

    String assignmentPeriod;

    RpcClient networkHandlerClient;

    RpcClient shortestPathHandlerClient;

    public TS(String appPropertyName, String globalPropertyName) {

        tsPropertyName = appPropertyName;
        this.globalPropertyName = globalPropertyName;

        tsPropertyMap = ResourceUtil
                .getResourceBundleAsHashMap(appPropertyName);
        globalPropertyMap = ResourceUtil
                .getResourceBundleAsHashMap(globalPropertyName);

        String handlerName = null;

        try {

            // Need a config file to initialize a Daf node
            DafNode.getInstance().init("ts-client", tsRpcConfigFileName);

            // Create RpcClients this class connects to
            try {
                handlerName = NetworkHandler.remoteHandlerName;
                networkHandlerClient = new RpcClient(handlerName);

                handlerName = ShortestPathTreeHandler.remoteHandlerName;
                shortestPathHandlerClient = new RpcClient(handlerName);
            } catch (MalformedURLException e) {

                logger
                        .error(
                                "MalformedURLException caught in TS() while defining RpcClients.",
                                e);

            }

        } catch (Exception e) {
            logger.error("Exception caught in TS().", e);
            System.exit(1);
        }

    }

    public TS(ResourceBundle appRb, ResourceBundle globalRb) {

        this.appRb = appRb;
        this.globalRb = globalRb;

        tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        globalPropertyMap = ResourceUtil
                .changeResourceBundleIntoHashMap(globalRb);

        String handlerName = null;

        try {

            // Need a config file to initialize a Daf node
            DafNode.getInstance().init("ts-client", tsRpcConfigFileName);

            // Create RpcClients this class connects to
            try {
                handlerName = NetworkHandler.remoteHandlerName;
                networkHandlerClient = new RpcClient(handlerName);

                handlerName = ShortestPathTreeHandler.remoteHandlerName;
                shortestPathHandlerClient = new RpcClient(handlerName);
            } catch (MalformedURLException e) {

                logger
                        .error(
                                "MalformedURLException caught in TS() while defining RpcClients.",
                                e);

            }

        } catch (Exception e) {
            logger.error("Exception caught in TS().", e);
            System.exit(1);
        }

    }

    public void runHighwayAssignment(String assignmentPeriod) {

        this.assignmentPeriod = assignmentPeriod;

        // define assignment related variables dependent on the assignment
        // period
        initializeHighwayAssignment(assignmentPeriod);

        // run the multiclass assignment for the time period
        multiclassEquilibriumHighwayAssignment(assignmentPeriod);

        // write the auto time and distance highway skim matrices to disk
        // writeHighwaySkimMatrices ( assignmentPeriod, 'a' );

        // if at some point in time we want to have truck specific highway
        // skims,
        // we'd create them here and would modify the the properties file to
        // include
        // class specific naming in skims file properties file keynames. We'd
        // also
        // modify the method above to distinguish the class id in addition to
        // period
        // and skim types.

    }

    private void initializeHighwayAssignment(String assignmentPeriod) {

        try {
            logger
                    .info("creating "
                            + assignmentPeriod
                            + " period NetworkHandler object for highway assignment at: "
                            + DateFormat.getDateTimeInstance().format(
                                    new Date()));
            networkHandlerSetupRpcCall();
        } catch (RpcException e) {
            logger.error("RpcException caught.", e);
            System.exit(1);
        } catch (IOException e) {
            logger.error("IOException caught.", e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Exception caught.", e);
            System.exit(1);
        }

        logger.info("NetworkHandler object successfully created at: "
                + DateFormat.getDateTimeInstance().format(new Date()));

    }

    private void multiclassEquilibriumHighwayAssignment(String assignmentPeriod) {

        long startTime = System.currentTimeMillis();

        String myDateString;

        // create Frank-Wolfe Algortihm Object
        myDateString = DateFormat.getDateTimeInstance().format(new Date());
        logger.info("creating + " + assignmentPeriod + " FW object at: "
                + myDateString);
        FW fw = new FW();
        fw.initialize(tsPropertyMap, globalPropertyMap);

        // Compute Frank-Wolfe solution
        myDateString = DateFormat.getDateTimeInstance().format(new Date());
        logger.info("starting + " + assignmentPeriod + " fw at: "
                + myDateString);
        fw.iterate();
        myDateString = DateFormat.getDateTimeInstance().format(new Date());
        logger.info("done with + " + assignmentPeriod + " fw at: "
                + myDateString);

        logger.info(assignmentPeriod + " highway assignment finished in "
                + ((System.currentTimeMillis() - startTime) / 60000.0)
                + " minutes.");

        String assignmentResultsFileName = null;

        if (assignmentPeriod.equalsIgnoreCase("peak")) {
            // get peak period definitions from property files
            assignmentResultsFileName = (String) tsPropertyMap
                    .get("peakOutput.fileName");
        } else if (assignmentPeriod.equalsIgnoreCase("offpeak")) {
            // get off-peak period definitions from property files
            assignmentResultsFileName = (String) tsPropertyMap
                    .get("offpeakOutput.fileName");
        }

        try {
            logger.info("Writing results file with " + assignmentPeriod
                    + " assignment results.");
            networkHandlerWriteNetworkAttributesRpcCall(assignmentResultsFileName);
        } catch (RpcException e) {
            logger.error("RpcException caught.", e);
            System.exit(1);
        } catch (IOException e) {
            logger.error("IOException caught.", e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Exception caught.", e);
            System.exit(1);
        }

        logger.info("\ndone with " + assignmentPeriod + " period assignment.");

    }

    public void writeHighwaySkimMatrix(String assignmentPeriod,
            String skimType, char modeChar) {

        logger.info("Writing " + assignmentPeriod
                + " time skim matrix for highway mode " + modeChar
                + " to disk...");
        long startTime = System.currentTimeMillis();

        Skims skims = new Skims(tsPropertyMap, globalPropertyMap);

        skims.writeHwySkimMatrix(assignmentPeriod, skimType, modeChar);

        logger.info("wrote the " + assignmentPeriod + " " + skimType
                + " skims for mode " + modeChar + " in "
                + ((System.currentTimeMillis() - startTime) / 1000.0)
                + " seconds");

    }

    public void writeHighwaySkimMatrices(String assignmentPeriod, char modeChar) {

        String[] skimTypeArray = { "time", "dist" };

        logger.info("Writing " + assignmentPeriod
                + " time and dist skim matrices for highway mode " + modeChar
                + " to disk...");
        long startTime = System.currentTimeMillis();

        Skims skims = new Skims(tsPropertyMap, globalPropertyMap);

        skims.writeHwySkimMatrices(assignmentPeriod, skimTypeArray, modeChar);

        logger.info("wrote the " + assignmentPeriod
                + " time and dist skims for mode " + modeChar + " in "
                + ((System.currentTimeMillis() - startTime) / 1000.0)
                + " seconds");

    }

    private void networkHandlerSetupRpcCall() throws Exception {
        // g.buildNetworkObject();

        Vector params = new Vector();
        params.addElement(tsPropertyMap);
        params.addElement(globalPropertyMap);
        params.addElement(assignmentPeriod);

        networkHandlerClient.execute("networkHandler.buildNetworkObject",
                params);
    }

    private void networkHandlerWriteNetworkAttributesRpcCall(
            String assignmentResultsFileName) throws Exception {
        // g.writeNetworkAttributes(String assignmentResultsFileName)
        Vector params = new Vector();
        params.addElement(assignmentResultsFileName);
        networkHandlerClient.execute("networkHandler.writeNetworkAttributes",
                params);
    }

    private boolean[][] networkHandlerGetValidLinksForAllClassesRpcCall()
            throws Exception {
        // g.getValidLinksForAllClasses()
        return (boolean[][]) networkHandlerClient.execute(
                "networkHandler.getValidLinksForAllClasses", new Vector());
    }

public static void main (String[] args) {
        
        String propertyFileName = null;
        
        switch ( args.length ) {
        
        case 0:
            System.out.println("usage: java " + TS.class.getName() + " <config-file> [connect url]");
            return;
            
        case 1:
            tsRpcConfigFileName = args[0];
            propertyFileName = "ts";
            break;

        case 2:
            tsRpcConfigFileName = args[0];
            propertyFileName = args[1];
            break;

        }
        
        
        TS tsTest = new TS( propertyFileName, "global" );

        // run peak highway assignment
		tsTest.runHighwayAssignment( "peak" );
// tsTest.runHighwayAssignment( "offpeak" );
		
		logger.info ("\ndone with TS run.");
		
    }}

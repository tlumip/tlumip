/*
 * Copyright 2006 PB Consult Inc.
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

import com.pb.common.rpc.DafNode;
import com.pb.common.util.DosCommand;
import com.pb.common.util.ResourceUtil;
import com.pb.models.reference.ModelComponent;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.util.ResourceBundle;


/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Mar 15, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class TSModelComponent extends ModelComponent {

    Logger logger = Logger.getLogger(TSModelComponent.class);
    
    static final String WINDOWS_CMD_TARGET = "windows.cmd";
    static final String PYTHON_CMD_TARGET = "python.cmd";
    static final String PYTHON_PROGRAM_TARGET = "python.source";
    static final int COUNT_YEAR = 1998;
    
    
    private String configFileName;
    private boolean dailyModel;

    TS ts;

    /**
     * If running in DAF mode, the configFileName will be something other than "null".
     * If the configFileName is null, then TS will run monolithically.
     * @param appRb is the TS component specific properties file ResourceBundle.
     * @param globalRb is the global model properties file ResourceBundle.
     * @param configFileName is the name of a DAF 3 configuration file (.groovy) that defines machine addresses and handler classes.
     * @param dailyModel is a Boolean that if true, causes all 4 assignment periods to be run.  If false or null, only amPeak and
     *        mdOffPeak periods are assigned.  FullModel runs are intended for base year and final year to produce full daily
     *        assignment results while intermediate model years require assignment procedures only for the purpose of producing
     *        representative peak and off-peak travel skim matrices for spatial models.
     */
    public TSModelComponent( ResourceBundle appRb, ResourceBundle globalRb, String configFileName, Boolean dailyModelFlag ){
		setResourceBundles(appRb, globalRb);    //creates a resource bundle as a class attribute called appRb.
        this.configFileName = configFileName;
        
        dailyModel = false;
        if ( dailyModelFlag != null ) {
            dailyModel = dailyModelFlag.booleanValue();
        }
        
    }

    
    public void startModel(int baseYear, int timeInterval){
        logger.info("Config file name: " + configFileName);
        if ( configFileName != null ) {
            try {
                DafNode.getInstance().initClient(configFileName);
            }
            catch (MalformedURLException e) {
                logger.error( "MalformedURLException caught initializing a DafNode.", e);
            }
            catch (Exception e) {
                logger.error( "Exception caught initializing a DafNode.", e);
            }

        }
        ts = new TS(appRb, globalRb);

        // amPeak and mdOffPeak periods are always run 
        assignAndSkimHwyAndTransit("ampeak");
        assignAndSkimHwyAndTransit("mdoffpeak");

        // pmPeak and ntOffPeak periods are run only if a daily model is required, and it's not SKIMS_ONLY mode.
        if ( dailyModel && ! ts.SKIM_ONLY ) {
            assignAndSkimHwyAndTransit("pmpeak");
            assignAndSkimHwyAndTransit("ntoffpeak");
        }

    }

    private void assignAndSkimHwyAndTransit(String period){

        NetworkHandlerIF nh = NetworkHandler.getInstance(configFileName);

        
        if ( nh.getStatus() ) {
            logger.info ( nh.getClass().getCanonicalName() + " instance created, and handler is active." );
        }

        try {
            nh.setRpcConfigFileName( configFileName );
            if ( ts.setupHighwayNetwork( nh, ResourceUtil.changeResourceBundleIntoHashMap(appRb), ResourceUtil.changeResourceBundleIntoHashMap(globalRb), period ) < 0 )
                throw new Exception();
            logger.info ("created " + period + " Highway NetworkHandler object: " + nh.getNodeCount() + " highway nodes, " + nh.getLinkCount() + " highway links." );
            
        }
        catch (Exception e) {
            logger.error ( "Exception caught setting up network in " + nh.getClass().getCanonicalName(), e );
            System.exit(-1);
        }


        // if SKIM_ONLY is false (set by skimOnly.flag TS property map key missing or set equal to false)
        // then skip the trip assignment step.
        if ( ! ts.SKIM_ONLY )
            ts.runHighwayAssignment( nh );
        

        
        //This will return a local network handler.  Jim needs to
        //test the remote network handler for transit skim building.
        NetworkHandlerIF nh_new = NetworkHandler.getInstance(null);
        try {
            if ( ts.setupHighwayNetwork( nh_new, ResourceUtil.changeResourceBundleIntoHashMap(appRb), ResourceUtil.changeResourceBundleIntoHashMap(globalRb), period ) < 0 )
                throw new Exception();
            logger.info ("created " + period + " Highway NetworkHandler object: " + nh_new.getNodeCount() + " highway nodes, " + nh_new.getLinkCount() + " highway links." );
        }
        catch (Exception e) {
            logger.error ( "Exception caught setting up network in " + nh_new.getClass().getCanonicalName(), e );
            System.exit(-1);
        }

        
        ts.loadAssignmentResults(nh_new, appRb);
        runLinkSummaries ( nh_new );
        
        
        char[] hwyModeChars = nh_new.getUserClasses();
        ts.writeHighwaySkimMatrices ( nh_new, hwyModeChars );


        ts.assignAndSkimTransit ( nh_new,  appRb, globalRb );
        
    }

        
    
    private void runLinkSummaries ( NetworkHandlerIF nh_new ) {
        
        String assignmentPeriod = nh_new.getTimePeriod();
        
        String linkSummaryFileName = null;

        // get output filename for link summary statustics report written by python program
        linkSummaryFileName = (String)appRb.getString( "linkSummary.fileName" );
        if ( linkSummaryFileName != null ) {

            int index = linkSummaryFileName.indexOf(".");
            if ( index < 0 ) {
                linkSummaryFileName += "_" + assignmentPeriod;
            }
            else {
                String extension = linkSummaryFileName.substring(index);
                linkSummaryFileName = linkSummaryFileName.substring(0, index);
                linkSummaryFileName += "_" + assignmentPeriod + extension;
            }

            String a2bFileName = (String)globalRb.getString( "alpha2beta.file" );
            String countsFileName = (String)appRb.getString( "counts.file" );
            
            String winCmdLocation = appRb.getString( WINDOWS_CMD_TARGET );
            
            String pythonCommand = appRb.getString( PYTHON_CMD_TARGET );
            String pythonSrc = appRb.getString( PYTHON_PROGRAM_TARGET );
            
            String commandString = String.format ( "%s %s %d %s %s %s %s %s %d", pythonCommand, pythonSrc, COUNT_YEAR, assignmentPeriod, linkSummaryFileName, a2bFileName, countsFileName, "localhost", NetworkHandlerIF.networkDataServerPort ); 

            // start data server that python program will use to generate link summaries.
            nh_new.startDataServer();
            
            // run python in an external dos command to generate link category summary reports file
            DosCommand.runDOSCommand ( winCmdLocation, commandString );

            // stop the data server.
            nh_new.stopDataServer();
            
        }

    }
        
}

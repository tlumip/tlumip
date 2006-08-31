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
package com.pb.tlumip.pt.daf;


import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.model.ModeChoiceLogsums;
import com.pb.tlumip.pt.CreateDestinationChoiceLogsums;
import com.pb.tlumip.pt.PTModelInputs;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * AggregateDestinationChoiceLogsumsTask
 *
 * @author Freedman
 * @version Aug 10, 2004
 * 
 */
public class AggregateDestinationChoiceLogsumsTask  extends MessageProcessingTask {
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.daf");
    protected static Object lock = new Object();
    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;
    protected static boolean initialized = false;
    boolean firstDCLogsum = true;
    CreateDestinationChoiceLogsums dcLogsumCalculator = new CreateDestinationChoiceLogsums();
//    protected static WorkLogsumMap logsumMap = new WorkLogsumMap();
    String fileWriterQueue = "FileWriterQueue";

    /**
     * Onstart method sets up model
     */
    public void onStart() {
        synchronized (lock) {
            logger.info( "***" + getName() + " started");
            //in cases where there are multiple tasks in a single vm, need to make sure only initilizing once!
            if (!initialized) {
                
                String scenarioName;
                int timeInterval;
                String pathToPtRb;
                String pathToGlobalRb;
                
                logger.info("Reading RunParams.properties file");
                ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
                scenarioName = ResourceUtil.getProperty(runParamsRb,"scenarioName");
                logger.info("\tScenario Name: " + scenarioName);
                timeInterval = Integer.parseInt(ResourceUtil.getProperty(runParamsRb,"timeInterval"));
                logger.info("\tTime Interval: " + timeInterval);
                pathToPtRb = ResourceUtil.getProperty(runParamsRb,"pathToAppRb");
                logger.info("\tResourceBundle Path: " + pathToPtRb);
                pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,"pathToGlobalRb");
                logger.info("\tResourceBundle Path: " + pathToGlobalRb);
                ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
                globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));

                PTModelInputs ptInputs = new PTModelInputs(ptRb,globalRb);
                logger.info("Setting up the aggregate mode choice model");
                ptInputs.setSeed(2002);
                ptInputs.getParameters();
                ptInputs.readSkims();
                ptInputs.readTazData();

                initialized = true;
            }

            logger.info( "***" + getName() + " finished onStart()");
        }
    }
    /**
     * A worker bee that will process a block of households.
     *
     */
    public void onMessage(Message msg) {
        logger.info("********" + getName() + " received messageId=" + msg.getId() +
            " message from=" + msg.getSender() + " at " + new Date());

         if (msg.getId().equals(MessageID.CREATE_DC_LOGSUMS)) 
            createDCLogsums(msg);
    }
    /**
     * Create destination choice aggregate logsums
     * @param msg
     */
    public void createDCLogsums(Message msg){
        if (firstDCLogsum) {
            logger.info(getName() + " is setting population, school occupation and collapsing employment " +
                    "in the ptModel tazs");
            dcLogsumCalculator.buildModel(PTModelInputs.tazs, PTModelInputs.getSkims());
            firstDCLogsum=false;
        }

        String purpose = String.valueOf(msg.getValue("purpose"));
        Integer segment = (Integer) msg.getValue("segment");

        String path = ResourceUtil.getProperty(ptRb, "mcLogsum.path");
        ModeChoiceLogsums mcl = new ModeChoiceLogsums(ptRb);
        mcl.readLogsums(purpose.charAt(0),segment.intValue());
        Matrix modeChoiceLogsum =mcl.getMatrix();

        if (purpose.equals("c")) {
            for (int i = 1; i <= 3; i++) {
                logger.info(getName() + " is calculating the DC Logsums for purpose c, market segment " + segment + " subpurpose " + i);
                
                //create a message to store the dc logsum vector
                Message dcLogsumMessage = createMessage();
                dcLogsumMessage.setId(MessageID.DC_LOGSUMS_CREATED);

                String dcPurpose = "c" + i;
                Matrix dcLogsumMatrix = (Matrix) dcLogsumCalculator.getDCLogsumVector(PTModelInputs.tazs,
                		PTModelInputs.tdpd, dcPurpose, segment.intValue(), modeChoiceLogsum);
                dcLogsumMessage.setValue("matrix", dcLogsumMatrix);
                sendTo(fileWriterQueue, dcLogsumMessage);
                
                //get the exponentiated utilities matrix and put it in another message
                Message dcExpUtilitiesMessage = createMessage();
                dcExpUtilitiesMessage.setId(MessageID.DC_EXPUTILITIES_CREATED);
                Matrix expUtilities = dcLogsumCalculator.getExpUtilities(dcPurpose, segment.intValue());
                dcExpUtilitiesMessage.setValue("matrix", expUtilities);
                sendTo(fileWriterQueue, dcExpUtilitiesMessage);
                
            }
        } else if (!purpose.equals("w")) {
            logger.info(getName() + " is calculating the DC Logsums for purpose " + purpose + ", market segment " + segment + " subpurpose 1");
            Message dcLogsumMessage = createMessage();
            dcLogsumMessage.setId(MessageID.DC_LOGSUMS_CREATED);
            Matrix dcLogsumMatrix = (Matrix) dcLogsumCalculator.getDCLogsumVector(PTModelInputs.tazs,
            		PTModelInputs.tdpd, purpose, segment.intValue(), modeChoiceLogsum);
            dcLogsumMessage.setValue("matrix", dcLogsumMatrix);
            sendTo(fileWriterQueue, dcLogsumMessage);
            
            //get the exponentiated utilities matrix and put it in another message
            Message dcExpUtilitiesMessage = createMessage();
            dcExpUtilitiesMessage.setId(MessageID.DC_EXPUTILITIES_CREATED);
            Matrix expUtilities = dcLogsumCalculator.getExpUtilities(purpose, segment.intValue());
            dcExpUtilitiesMessage.setValue("matrix", expUtilities);
            sendTo(fileWriterQueue, dcExpUtilitiesMessage);

        }

        modeChoiceLogsum = null;
    }

}

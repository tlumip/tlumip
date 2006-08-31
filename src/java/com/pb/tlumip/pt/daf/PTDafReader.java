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
/*
 * Created on May 20, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.tlumip.pt.daf;

import java.util.Date;
import java.util.ResourceBundle;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.model.SkimsInMemory;

/**
 * @author hansens
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PTDafReader extends MessageProcessingTask{

    SkimsInMemory skims;


    public void onStart() {
        logger.info( "***" + getName() + " started");

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
        
        ResourceBundle ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));

        ResourceBundle ptDafRb = ResourceUtil.getResourceBundle("ptdaf");
        final int NUMBER_OF_WORK_QUEUES = Integer.parseInt(ResourceUtil.getProperty(ptDafRb,"workQueues"));
        skims = new SkimsInMemory(globalRb);
        skims.readSkims(ptRb);
        for(int q=1;q<=NUMBER_OF_WORK_QUEUES;q++){
            Message skimsMessage = createMessage();
            skimsMessage.setId(MessageID.SKIMS);
            skimsMessage.setValue("skims",skims);
            String queueName = new String("WorkQueue"+q);
            if(logger.isDebugEnabled()) {
                logger.debug("Skims sent to "+queueName+" at "+ new Date());
            }
            sendTo(queueName,skimsMessage);
            if(logger.isDebugEnabled()) {
                logger.debug("Free memory after creating MC logsum: "+Runtime.getRuntime().freeMemory());   
            }
        }
        skims=null;
    }

    public void onMessage(Message msg) {
        logger.info( getName() + " received messageId=" + msg.getId() + " message from=" + msg.getSender() );
        sendTo(String.valueOf(msg.getValue("queue")),msg);        
    }
}

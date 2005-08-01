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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.pt.ActivityPurpose;

/**
 * This will serve up the mode choice logsum 
 * calculations to anyone listening to a
 * MCWorkQueue.  Once all work has been sent
 * out the task is complete and can be removed from 
 * the process.  It will receive a single message and that 
 * to start the mode choice logsum work.
 * msg.setId(MessageID.CREATE_MC_LOGSUMS)
 * 
 * Created on Jul 20, 2005 
 * @author Christi Willison
 */
public class MCServerTask extends MessageProcessingTask {
    
    protected static Logger logger = Logger.getLogger(MCServerTask.class);
    private ArrayList mcWorkQueueNames = new ArrayList();
    private int nMCWorkQueues;
    
    public void onStart() {
        //We need to get the list of mcWorkQueue names so that we know
        //who to send messages to.
        ResourceBundle rb = ResourceUtil.getResourceBundle("ptdaf");
        ArrayList queueList = ResourceUtil.getList(rb, "queueList");
        Iterator iter = queueList.iterator();
        while(iter.hasNext()){
            String temp = (String) iter.next();
            if(temp.startsWith("MCWorkQueue")){
                mcWorkQueueNames.add(temp);
                if(logger.isDebugEnabled()) logger.debug("Adding " + temp + " to list of Mode Choice Work Queues");
            }
        }
        nMCWorkQueues = mcWorkQueueNames.size();
    }
    
    public void onMessage(Message msg) {
        logger.info(getName() + " received messageId=" + msg.getId() +
                " message from=" + msg.getSender() + " @time=" + new Date());
        
        startMCLogsums();
    }
    
    /**
     * startMCLogsums - sends messages to the work queues to create MC Logsum matrices
     *
     */
    private void startMCLogsums() {
        logger.info("Creating tour mode choice logsums");
        int messageCounter = 1;
        //enter loop on purposes (skip home purpose)
        for (int purpose = 1; purpose < ActivityPurpose.ACTIVITY_PURPOSES.length; ++purpose) {
            char thisPurpose = ActivityPurpose.ACTIVITY_PURPOSES[purpose];

            //enter loop on segments
            for (int segment = 0; segment < Ref.TOTAL_SEGMENTS; ++segment) {
                Message mcLogsumMessage = createMessage();
                mcLogsumMessage.setId(MessageID.CREATE_MC_LOGSUM);
                mcLogsumMessage.setValue("purpose", new Character(thisPurpose));
                mcLogsumMessage.setValue("segment", new Integer(segment));

                String queueName = (String) mcWorkQueueNames.get(messageCounter%nMCWorkQueues);
                logger.info("Sending purpose: "+ thisPurpose + " segment: "+ segment + " to " + queueName);
                sendTo(queueName, mcLogsumMessage);
                messageCounter++;
            }
        }
        logger.info("Total Mode Choice Logsums Sent to Workers: "+ messageCounter);
    }

}

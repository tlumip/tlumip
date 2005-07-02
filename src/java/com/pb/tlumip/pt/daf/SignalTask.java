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

import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.daf.Message;
import com.pb.common.util.ResourceUtil;

import java.util.ResourceBundle;

/**
 * Author: willison
 * Date: Aug 16, 2004
 *
 * Created by IntelliJ
 */
public class SignalTask extends MessageProcessingTask {

    int nWorkQueues; //for PT we assume 1 work queue per worker node (with 2 workers per node pulling from the same queue)
    int workQueuesCount = 0;
    
    int nWorkers;
    int workersCount = 0;

    int nMCLogsums = Ref.TOTAL_SEGMENTS * Ref.MC_PURPOSES.length(); //should be 54
    int mcLogsumsCount = 0;

    int nDCLogsums = Ref.TOTAL_SEGMENTS * (Ref.MC_PURPOSES.length() +1);  //not doing 'w' but doing 'c0','c1','c2'
    int dcLogsumsCount = 0;                                               //should be 63

    int nHHs;
    int hhsCount = 0;

    int nPersons;
    int personsCount = 0;


    public void onStart(){
        ResourceBundle rb = ResourceUtil.getResourceBundle("ptdaf");
        int nNodes = Integer.parseInt(ResourceUtil.getProperty(rb,"nNodes"));
        this.nWorkQueues = nNodes-2; //reserve 1 node for master and 1 node for file writer
    }

    public void onMessage(Message msg){
        if(msg.getId().equals(MessageID.NUM_OF_WORK_QUEUES)){
            this.nWorkQueues = ((Integer)msg.getValue("nWorkQueues")).intValue();
            this.nWorkers = ((Integer)msg.getValue("nWorkers")).intValue();
        }else if(msg.getId().equals(MessageID.NODE_SETUP_DONE)){
            workQueuesCount++;
            if(workQueuesCount == nWorkQueues){
                PTServerTask.signalResultsProcessed();
            }
        }else if(msg.getId().equals(MessageID.NUM_OF_HHS)){
            this.nHHs = ((Integer)(msg.getValue("nHHs"))).intValue();
        }else if(msg.getId().equals(MessageID.NUM_OF_PERSONS)){
            this.nPersons = ((Integer)(msg.getValue("nPersons"))).intValue();
        }else if(msg.getId().equals(MessageID.MC_LOGSUMS_CREATED)){
            mcLogsumsCount++;
            if(mcLogsumsCount == nMCLogsums){
                PTServerTask.signalResultsProcessed();
            }
        }else if(msg.getId().equals(MessageID.TAZDATA_UPDATED)){
            workersCount++;
            if(workersCount == nWorkers){
                PTServerTask.signalResultsProcessed();
            }
        }
    }
}

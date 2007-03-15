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
package com.pb.tlumip.pt.daf;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageFactory;
import com.pb.models.pt.daf.LongDistanceWorker;
import com.pb.models.pt.daf.MessageID;
import com.pb.models.pt.ldt.LDTour;
import com.pb.models.pt.ldt.RunLDTModels;
import com.pb.tlumip.pt.ldt.RunTLUMIPLDTModels;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Mar 14, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class TLUMIPLongDistanceWorker extends LongDistanceWorker {

    public static void main(String[] args) {
        RunLDTModels ldtRunner = new RunTLUMIPLDTModels();

        // read households and create tours
        LDTour[] tours = ldtRunner.createToursFromHouseholdFile();

        // call the long-distance worker
        MessageFactory mFactory = MessageFactory.getInstance();
        Message msg = mFactory.createMessage();
        msg.setId(MessageID.HOUSEHOLDS_PROCESSED);
        msg.setValue("tours", tours);
        msg.setSender(RunLDTModels.class.toString());

        // initialize the queue and send the message to the queue
        LongDistanceWorker worker = new LongDistanceWorker();
        worker.onStart();
        worker.onMessage(msg);
    }
}

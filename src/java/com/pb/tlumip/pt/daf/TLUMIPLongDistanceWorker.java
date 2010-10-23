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
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PriceConverter;
import com.pb.models.pt.daf.LongDistanceWorker;
import com.pb.models.pt.daf.MessageID;
import com.pb.models.pt.ldt.LDTour;
import com.pb.models.pt.ldt.RunLDTModels;
import com.pb.tlumip.pt.PTOccupation;

import java.util.ResourceBundle;

/**
 * This a class that runs LDT as a stand-alone model
 * for the purpose of offline calibration.  
 * 
 * @arg binary flag indicating whether or not to run household level models
 * @arg name of resource bundle
 * 
 * Author: Christi Willison
 * Date: Mar 14, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class TLUMIPLongDistanceWorker extends LongDistanceWorker {

    public static void main(String[] args) {

        boolean runHouseholdLevelModels = false;
        if (args.length>0 && args[0].equals("1")) {
            runHouseholdLevelModels = true;
        } 
        
        String appRbName = args[1];
        ResourceBundle rb = ResourceUtil.getResourceBundle(appRbName);

        String globalRbName = args[2];
        ResourceBundle globalRb = ResourceUtil.getResourceBundle(globalRbName);

        //initialize price converter
        PriceConverter.getInstance(rb,globalRb);
                
        RunLDTModels ldtRunner = new RunLDTModels(PTOccupation.NONE);
        ldtRunner.setResourceBundles(rb, globalRb); 
        LDTour[] tours; 
        
        // run the household level models
        if (runHouseholdLevelModels) {
            ldtRunner.readTazData(); 
            ldtRunner.readDcLogsums();
            PTHousehold[] households = ldtRunner.runHouseholdLevelModels(true, true, 1990);
            tours = RunLDTModels.createLDTours(households); 
        } else {
            //read households and create tours
            //PTDataReader needs the base year in order to parse the age field.
            //In Ohio the default base year is 2000 but in Oregon it is 1990.
            tours = ldtRunner.createToursFromHouseholdFile(1990);
        }
        
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

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
package com.pb.tlumip.ts.daf;


import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.daf.Port;
import com.pb.common.daf.PortManager;
import com.pb.common.util.Justify;
import com.pb.tlumip.ts.assign.Network;

/**
 *   Distributed application server class used to manage the
 *   implementation of the TS components, some of which might
 *   themselves be distributed applications.
 */

public class TSControllerTask extends MessageProcessingTask{

	private static boolean LOGGING = true;
    static Logger logger = Logger.getLogger("com.pb.tlumip.ts.daf");

    public TSControllerTask () {
    }

    
    public void onStart() {

    	if (LOGGING)
			logger.info( getName() + " started" );

    }

    
    public void onMessage(Message msg) {
    	
    	if (LOGGING)
		    logger.info( this.name +  " onMessage() id=" + msg.getId() + ", sent by " + msg.getSender() + "." );

		if(msg.getId().equals(MessageID.TS_SOLVE_FW_ID)) {
/*
    		g = getNetworkObjectFromMessage( msg );
    		tripTable = getTripTableFromMessage( msg );

    		int firstOriginTaz = getFirstAssignmentTazFromMessage (msg );
    		int lastOriginTaz = getLastAssignmentTazFromMessage (msg );
    		int maxFwIteration = getMaxFwIterationNumberFromMessage (msg );

    		solveFrankWolfeAlgorithm ( g, tripTable, maxFwIteration, firstOriginTaz, lastOriginTaz );

		}
		else if(msg.getId().equals(MessageID.FINAL_AON_FLOW_RESULTS_ID)) {
			
			aonFlow = getMulticlassAonLinkFlows ( msg );
*/
		}

    }
    
    private void sendEquilibriumAssignmentMessage () {
    	
    	// establish that this controller task sends the message to the BuildLoadControllerQueue
        PortManager pManager = PortManager.getInstance();
        Port buildLoadInputPort = pManager.createPort( MessageID.AON_BUILD_LOAD_CONTROLLER_QUEUE );
        
    	// set the message id and trip table values needed by AonBuildLoadControllerTask to start Aon Build Load
    	Message startMsg = mFactory.createMessage();
    	startMsg.setId( MessageID.FW_BUILD_LOAD_START_ID );
//    	startMsg.setValue( MessageID.TRIPTABLE_KEY, tripTable );

    	// send the work message to the BuildLoadControllerQueue
    	buildLoadInputPort.send(startMsg);
    	
    }

    
 
}

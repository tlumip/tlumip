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
package com.pb.tlumip.ts.daf2;


import org.apache.log4j.Logger;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageFactory;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.daf.Port;
import com.pb.common.daf.PortManager;

import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.daf2.MessageID;

/**
 *   This task manages information that is common to all tasks running on the node.
 * 
 *   Only one of these tasks will be created for each computing node (ip address), and
 *   each of these tasks receives messages from its own queue.
 *   
 */

public class AonBuildLoadCommonTask extends MessageProcessingTask {

	private boolean LOGGING = true;
    static Logger logger = Logger.getLogger("com.pb.tlumip.ts.daf2");
    
    // get instances of the classes used to hold worker results
    private AonLinkFlowResults flowResults = AonLinkFlowResults.getInstance();
    private SavedShortestPaths pathResults = SavedShortestPaths.getInstance();
    private AonNetworkManager gManager = AonNetworkManager.getInstance(); 
    
    private int nodeId = -1;
    
    
	// create the message to be used to return results when they're requested
	MessageFactory mFactory = MessageFactory.getInstance();

	// establish that this controller task places work to be done on the BuildLoadQueue
    PortManager pManager = PortManager.getInstance();
    
    
    
    public AonBuildLoadCommonTask () {
    }

    
    public void onStart() {

    	if (LOGGING)
			logger.info( getName() + " started" );

    }

    
    public void onMessage(Message msg) {

        if (LOGGING)
		    logger.info( this.name +  " onMessage() id=" + msg.getId() + ", sent by " + msg.getSender() + "." );

        
        if(msg.getId().equals(MessageID.RETURN_AON_LINK_FLOWS_ID)) {
        	
        	sendAonFlowResultsMessage();    
        	
        }
        else if(msg.getId().equals(MessageID.RESET_WORK_ELEMENTS_COMPLETED_ID)) {
        	
        	flowResults.resetWorkElementsCompleted();    
        	
        }
        else if(msg.getId().equals(MessageID.RETURN_WORK_ELEMENTS_COMPLETED_ID)) {
        	
        	sendNumberOfWorkElementsCompletedMessage();    
        	
        }
        else if(msg.getId().equals(MessageID.FW_ASSIGNMENT_INFORMATION_ID)) {

        	// get the Network object and information about the assignment procedure from the message and store in gManager
        	nodeId = getNodeIdFromMessage( msg );
        	NetworkHandler g = getNetworkHandlerObjectFromMessage( msg );
    		int firstOriginTaz = getFirstAssignmentTazFromMessage (msg );
    		int lastOriginTaz = getLastAssignmentTazFromMessage (msg );
    		int fwIteration = getFwIterationNumberFromMessage (msg );
    		
        	gManager.setNetworkHandler( g );
        	gManager.setFirstTaz( firstOriginTaz );
        	gManager.setLastTaz( lastOriginTaz );
        	gManager.setFwIteration( fwIteration );
        	
        	// initialize the combined AON Flows array to zero
        	flowResults.initializeFlowArray( gManager.getNetworkHandler().getLinkCount(), gManager.getNetworkHandler().getNumUserClasses() );
        	
        }

    }

    
    private void sendAonFlowResultsMessage () {
    	
		// create the message to be used to return aon flow results
    	Message aonFlowMsg = mFactory.createMessage();

    	// return the total aon link flows accummulated by all the worker tasks on this node
    	aonFlowMsg.setId( MessageID.AON_FLOW_RESULTS_ID );
    	aonFlowMsg.setValue( MessageID.NODE_KEY, Integer.valueOf(nodeId) );
    	aonFlowMsg.setValue( MessageID.AON_FLOW_RESULT_VALUES_KEY, flowResults.getCombinedLinkFlows() );
    	
        Port resultsPort = pManager.createPort( MessageID.AON_BUILD_LOAD_CONTROLLER_QUEUE );
        resultsPort.send( aonFlowMsg );
    	
    }
    
    private void sendNumberOfWorkElementsCompletedMessage () {
    	
		// create the message used to return number of work elements completed by all the worker tasks on this node
    	Message workCompletedMsg = mFactory.createMessage();

    	// return the total aon link flows accummulated by all the worker tasks on this node
    	workCompletedMsg.setId( MessageID.NUMBER_OF_WORK_ELEMENTS_COMPLETED_ID );
    	workCompletedMsg.setValue( MessageID.NODE_KEY, Integer.valueOf(nodeId) );
    	workCompletedMsg.setValue( MessageID.NUMBER_OF_WORK_ELEMENTS_COMPLETED_KEY, Integer.valueOf(flowResults.getWorkElementsCompleted()) );
    	
        Port resultsPort = pManager.createPort( MessageID.AON_BUILD_LOAD_CONTROLLER_QUEUE );
        resultsPort.send( workCompletedMsg );
    	
    }
    
	private int getNodeIdFromMessage( Message msg ){
		return ((Integer)msg.getValue( MessageID.NODE_KEY )).intValue();
	}
    
	private NetworkHandler getNetworkHandlerObjectFromMessage( Message msg ){
		return (NetworkHandler)msg.getValue( MessageID.NETWORK_KEY );
	}

	private int getFwIterationNumberFromMessage( Message msg ){
		return ((Integer)msg.getValue( MessageID.FW_ITERATION_NUMBER_KEY )).intValue();
	}
    
	private int getFirstAssignmentTazFromMessage( Message msg ){
		return ((Integer)msg.getValue( MessageID.FIRST_TAZ_NUMBER_KEY )).intValue();
	}
    
	private int getLastAssignmentTazFromMessage( Message msg ){
		return ((Integer)msg.getValue( MessageID.LAST_TAZ_NUMBER_KEY )).intValue();
	}

}

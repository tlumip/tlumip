package com.pb.tlumip.ts.daf;


import org.apache.log4j.Logger;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageFactory;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.daf.Port;
import com.pb.common.daf.PortManager;

import com.pb.tlumip.ts.assign.Network;
import com.pb.tlumip.ts.daf.MessageID;

/**
 *   This task manages information that is common to all tasks running on the node.
 * 
 *   Only one of these tasks will be created for each computing node (ip address), and
 *   each of these tasks receives messages from its own queue.
 *   
 */

public class AonBuildLoadCommonTask extends MessageProcessingTask {

	private boolean LOGGING = true;
    static Logger logger = Logger.getLogger("com.pb.tlumip.ts.daf");
    
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
        	Network g = getNetworkObjectFromMessage( msg );
    		int firstOriginTaz = getFirstAssignmentTazFromMessage (msg );
    		int lastOriginTaz = getLastAssignmentTazFromMessage (msg );
    		int fwIteration = getFwIterationNumberFromMessage (msg );
    		
        	gManager.setNetwork( g );
        	gManager.setFirstTaz( firstOriginTaz );
        	gManager.setLastTaz( lastOriginTaz );
        	gManager.setFwIteration( fwIteration );
        	
        	// initialize the combined AON Flows array to zero
        	flowResults.initializeFlowArray( gManager.getNetwork().getLinkCount(), gManager.getNetwork().getNumUserClasses() );
        	
        }

    }

    
    private void sendAonFlowResultsMessage () {
    	
		// create the message to be used to return aon flow results
    	Message aonFlowMsg = mFactory.createMessage();

    	// return the total aon link flows accummulated by all the worker tasks on this node
    	aonFlowMsg.setId( MessageID.AON_FLOW_RESULTS_ID );
    	aonFlowMsg.setValue( MessageID.NODE_KEY, Integer.valueOf(nodeId) );
    	aonFlowMsg.setValue( MessageID.AON_FLOW_RESULT_VALUES_KEY, flowResults.getCombinedLinkFlows() );
    	
        Port resultsPort = pManager.createPort( "BuildLoadControllerQueue" );
        resultsPort.send( aonFlowMsg );
    	
    }
    
    private void sendNumberOfWorkElementsCompletedMessage () {
    	
		// create the message to be used to return aon flow results
    	Message workCompletedMsg = mFactory.createMessage();

    	// return the total aon link flows accummulated by all the worker tasks on this node
    	workCompletedMsg.setId( MessageID.NUMBER_OF_WORK_ELEMENTS_COMPLETED_ID );
    	workCompletedMsg.setValue( MessageID.NODE_KEY, Integer.valueOf(nodeId) );
    	workCompletedMsg.setValue( MessageID.NUMBER_OF_WORK_ELEMENTS_COMPLETED_KEY, Integer.valueOf(flowResults.getWorkElementsCompleted()) );
    	
        Port resultsPort = pManager.createPort( "BuildLoadControllerQueue" );
        resultsPort.send( workCompletedMsg );
    	
    }
    
	private int getNodeIdFromMessage( Message msg ){
		return ((Integer)msg.getValue( MessageID.NODE_KEY )).intValue();
	}
    
	private Network getNetworkObjectFromMessage( Message msg ){
		return (Network)msg.getValue( MessageID.NETWORK_KEY );
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

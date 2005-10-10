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
 *   Distributed application server class used for implementing a
 *   distributed version of the Frank-Wolfe Algorithm for highway
 *   user equilibrium trip assignment.  This server task class
 *   manages the distribution of the shortest path tree building
 *   and loading and the compilation of the loaded aon flow vectors
 *   for each user class.
 */

public class AonBuildLoadController extends MessageProcessingTask {

	private boolean LOGGING = true;
    static Logger logger = Logger.getLogger("com.pb.tlumip.ts.daf2");
    
    // define the time interval (in milliseconds) for polling worker nodes
    // for the number of work elements completed so far.
    private static final int POLLING_INTERVAL = 10000; 
    
	// create the MessageFactory used to generate outgoing messages
	MessageFactory mFactory = MessageFactory.getInstance();

	// create the PortManager factory used to connect to receiving queues
	PortManager pManager = PortManager.getInstance();
	
	
	int[] buildLoadRequestsHandled = new int[MessageID.AON_BUILD_LOAD_COMMON_QUEUES.length];
	int totalBuildLoadRequestsHandled = 0;
    
	double[][][] aonLinkFlowWorkerResults = new double[MessageID.AON_BUILD_LOAD_COMMON_QUEUES.length][][];
    int numberOfWorkerNodesThatReturnedResults = 0;
	
	
	
    public AonBuildLoadController () {
    }

    
    public void onStart() {

    	if (LOGGING)
			logger.info( getName() + " started" );

    }

    
    public void onMessage(Message msg) {

        if (LOGGING)
		    logger.info( this.name +  " onMessage() id=" + msg.getId() + ", sent by " + msg.getSender() + "." );


		
    	if( msg.getId().equals(MessageID.FW_BUILD_LOAD_START_ID) ) {

    		// get an instance of the AonNetworkManager which was initialized by the AonBuildLoadCommonTask
    	    AonNetworkManager gManager = AonNetworkManager.getInstance(); 

    		// get the assignment information needed from the AonNetworkManager
    	    NetworkHandler g = gManager.getNetworkHandler();
    		int firstOriginTaz = gManager.getFirstTaz();
    		int lastOriginTaz = gManager.getLastTaz();
    		int fwIteration = gManager.getFwIteration();

    		// get the information needed from the message
    		double[][][] tripTable = getTripTableFromMessage( msg );
		
    		
    		// do the work
    		calculateMulticlassAonLinkFlows ( g, tripTable, firstOriginTaz, lastOriginTaz, fwIteration );
    		
    	}
    	else if( msg.getId().equals(MessageID.NUMBER_OF_WORK_ELEMENTS_COMPLETED_ID) ) {
    		
    		// get the id of the worker node sending work elements completed info;
    		int nodeId = getNodeIdFromMessage( msg );
    		int workElementsCompleted = getNumberOfWorkElementsCompletedFromMessage( msg );
    		
    		buildLoadRequestsHandled[nodeId] = workElementsCompleted;
    		
    		totalBuildLoadRequestsHandled = 0;
    		for (int i=0; i < buildLoadRequestsHandled.length; i++)
    			totalBuildLoadRequestsHandled += buildLoadRequestsHandled[i];
    		
    	}
    	
    	else if( msg.getId().equals(MessageID.AON_FLOW_RESULTS_ID) ) {
    		
    		// get the id of the worker node sending work elements completed info;
    		int nodeId = getNodeIdFromMessage( msg );
    		double[][] workerFlowResults = getWorkerFlowResultsFromMessage( msg );
    		
    		aonLinkFlowWorkerResults[nodeId] = workerFlowResults;
    		
    		numberOfWorkerNodesThatReturnedResults++;    		
    	}
    	
    }

    
	private void calculateMulticlassAonLinkFlows ( NetworkHandler g, double[][][] tripTable, int firstOriginTaz, int lastOriginTaz, int fwIteration ) {

		int buildLoadRequestsSent = 0;
		
		// loop through userClasses and origin zones and build and load shortest path trees
		for (int m=0; m < g.getNumUserClasses(); m++) {

			for (int orig=firstOriginTaz; orig < lastOriginTaz; orig++) {
		    
				// If at least one destination zone has trips from the userclass/origin,
				// then build and load shortest path tree for the userClass/origin.
				for (int dest=0; dest < tripTable[m][orig].length; dest++) {
					if ( tripTable[m][orig][dest] > 0.0 ) {
						sendBuildLoadWorkMessage ( orig, m, tripTable[m][orig] );
						buildLoadRequestsSent++;
						break;
					}
				}
			    
			}
			
		}
		
		// wait here until the total number of build/load requests handled by worker tasks
		// equals the count of requests sent out.  The totalBuildLoadRequestsHandled value
		// gets updated as messages are retruned following these requests.  Just check the
		// status of total requests handled every POLLING_INTERVAL milliseconds.
		while ( totalBuildLoadRequestsHandled != buildLoadRequestsSent ) {
			sendReturnNumberOfWorkElementsCompletedMessages();
			try {
				Thread.sleep(POLLING_INTERVAL);
				logger.info ( "Shortest Path Tree Build/Load Requests Processed");
	    		for (int i=0; i < buildLoadRequestsHandled.length; i++)
	    			logger.info ( "worker node " + i + ": " + buildLoadRequestsHandled[i] );
    			logger.info ( "total for all nodes: " + totalBuildLoadRequestsHandled );
    			logger.info ( "total requests sent to all nodes: " + buildLoadRequestsSent );
			}
			catch (InterruptedException e){
				logger.error ( "InterruptedException thrown while waiting " + (POLLING_INTERVAL/1000) + " seconds.", e);
			}
		}
		
		
		// when all the requests have been handled, the assigned AonFlowResults can be requested
		sendReturnAonFlowResultsMessages();
		
		
		// wait here until each of the worker nodes has returned messages with their loaded
		// multiclass aon flow vectors.
		while ( numberOfWorkerNodesThatReturnedResults != MessageID.AON_BUILD_LOAD_COMMON_QUEUES.length ) {
		}

		
		// combine results from various worker nodes into one final results array
		double[][] finalFlowResults = new double[g.getNumUserClasses()][g.getLinkCount()];
		for (int i=0; i < aonLinkFlowWorkerResults.length; i++) {
			for (int j=0; j < aonLinkFlowWorkerResults[i].length; j++) {
				for (int k=0; k < aonLinkFlowWorkerResults[i][j].length; k++) {
					finalFlowResults[j][k] += aonLinkFlowWorkerResults[i][j][k];
				}
			}
		}
		
		
		// send final loaded aon link flow results back to FWAlgorithmControllerTask
		sendFinalAonFlowResultsMessage ( finalFlowResults );
		
	}
	
	
	

    private void sendBuildLoadWorkMessage ( int origin, int userClass, double[] tripTableRow ) {
    	
    	// establish that this controller task places work to be done on the BuildLoadWorkerQueue
        Port workerPort = pManager.createPort( MessageID.AON_BUILD_LOAD_WORKER_QUEUE );
        
    	// set the message id and values for the work to be done
    	Message workMsg = mFactory.createMessage();
    	workMsg.setId( MessageID.BUILDLOAD_WORK_ID );
    	workMsg.setValue( MessageID.BUILDLOAD_ROOT_ORIGIN_TAZ_KEY, Integer.valueOf(origin) );
    	workMsg.setValue( MessageID.BUILDLOAD_USER_CLASS_KEY, Integer.valueOf(userClass) );
    	workMsg.setValue( MessageID.BUILDLOAD_ROOT_ORIGIN_TRIPTABLE_ROW_KEY, tripTableRow );

    	// send the work message to the BuildLoadQueue
    	workerPort.send(workMsg);
    	
    }

    
    private void sendReturnNumberOfWorkElementsCompletedMessages () {
    	
    	// establish that this controller task sends the Assignment Information message to the BuildLoadCommonQueues for each node.
        PortManager pManager = PortManager.getInstance();
        
    	// create the message id to request the number of work elements completed
    	Message workCompletedMsg = mFactory.createMessage();
    	workCompletedMsg.setId( MessageID.RETURN_WORK_ELEMENTS_COMPLETED_ID );

    	// send the assignment info message to the BuildLoadCommonQueues on each node
    	for (int i=0; i < MessageID.AON_BUILD_LOAD_COMMON_QUEUES.length; i++) {
            Port buildLoadInputPort = pManager.createPort( MessageID.AON_BUILD_LOAD_COMMON_QUEUES[i] );
        	buildLoadInputPort.send(workCompletedMsg);
    	}
    	
    }
    

    private void sendReturnAonFlowResultsMessages () {
    	
    	// establish that this controller task sends the Assignment Information message to the BuildLoadCommonQueues for each node.
        PortManager pManager = PortManager.getInstance();
        
    	// create the message id to request the number of work elements completed
    	Message resultsMsg = mFactory.createMessage();
    	resultsMsg.setId( MessageID.RETURN_AON_LINK_FLOWS_ID );

    	// send the assignment info message to the BuildLoadCommonQueues on each node
    	for (int i=0; i < MessageID.AON_BUILD_LOAD_COMMON_QUEUES.length; i++) {
            Port buildLoadInputPort = pManager.createPort( MessageID.AON_BUILD_LOAD_COMMON_QUEUES[i] );
        	buildLoadInputPort.send(resultsMsg);
    	}
    	
    }
    

    private void sendFinalAonFlowResultsMessage ( double[][] finalFlowResults ) {
    	
    	// establish that this controller task sends the Assignment Information message to the BuildLoadCommonQueues for each node.
        PortManager pManager = PortManager.getInstance();
        
    	// create the message id to request the number of work elements completed
    	Message resultsMsg = mFactory.createMessage();
    	resultsMsg.setId( MessageID.FINAL_AON_FLOW_RESULTS_ID );
    	resultsMsg.setValue( MessageID.FINAL_AON_FLOW_RESULT_VALUES_KEY, finalFlowResults );

    	// send the final results info message to the FWAlgorithmQueue
        Port buildLoadResultsPort = pManager.createPort( MessageID.FW_ALGORITHM_QUEUE );
        buildLoadResultsPort.send(resultsMsg);
    	
    }
    

    private double[][][] getTripTableFromMessage ( Message msg ) {
		return (double[][][])msg.getValue( MessageID.TRIPTABLE_KEY );
	}

	private int getNodeIdFromMessage ( Message msg ) {
		return ((Integer)msg.getValue( MessageID.NODE_KEY )).intValue();
	}
    
    private int getNumberOfWorkElementsCompletedFromMessage ( Message msg ) {
		return ((Integer)msg.getValue( MessageID.NUMBER_OF_WORK_ELEMENTS_COMPLETED_KEY )).intValue();
	}

    private double[][] getWorkerFlowResultsFromMessage ( Message msg ) {
		return (double[][])msg.getValue( MessageID.AON_FLOW_RESULT_VALUES_KEY );
    }
    
}

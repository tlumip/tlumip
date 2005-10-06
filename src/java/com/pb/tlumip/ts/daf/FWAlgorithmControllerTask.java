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
package com.pb.tlumip.ts.dafv3;


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
import com.pb.tlumip.ts.dafv3.FW;

/**
 *   Distributed application server class used for implementing a
 *   distributed version of the Frank-Wolfe Algorithm for highway
 *   user equilibrium trip assignment.
 */

public class FWAlgorithmControllerTask extends MessageProcessingTask{

	private static boolean LOGGING = true;
    static Logger logger = Logger.getLogger("com.pb.tlumip.ts.daf");

    Justify myFormat = new Justify();

    double[][] aonFlow = null;
    double[] lambdas = null;
    double [] fwFlowProps;
    
    
    
    public FWAlgorithmControllerTask () {
    }


    public void onStart() {

    	if (LOGGING)
			logger.info( getName() + " started" );

    }


    public void onMessage(Message msg) {

    	Network g = null;
    	double[][][] tripTable = null;


    	if (LOGGING)
		    logger.info( this.name +  " onMessage() id=" + msg.getId() + ", sent by " + msg.getSender() + "." );


		if(msg.getId().equals(MessageID.TS_SOLVE_FW_ID)) {

    		g = getNetworkObjectFromMessage( msg );
    		tripTable = getTripTableFromMessage( msg );

    		int firstOriginTaz = getFirstAssignmentTazFromMessage (msg );
    		int lastOriginTaz = getLastAssignmentTazFromMessage (msg );
    		int maxFwIteration = getMaxFwIterationNumberFromMessage (msg );

    		solveFrankWolfeAlgorithm ( g, tripTable, maxFwIteration, firstOriginTaz, lastOriginTaz );

		}
		else if(msg.getId().equals(MessageID.FINAL_AON_FLOW_RESULTS_ID)) {
			
			aonFlow = getMulticlassAonLinkFlows ( msg );

		}

    }
    
 
    
    private void solveFrankWolfeAlgorithm ( Network g, double[][][] tripTable, int maxFwIteration, int firstOriginTaz, int lastOriginTaz ) {
    	
		double lub = 0.0;
	    double gap = 0.0;
	    double glb = 0.0;

		
	    FW fw = new FW();

		
		// determine which links are valid parts of paths for this skim
		boolean[] validLinksForClass = null;
		boolean[] validLinks = new boolean[g.getLinkCount()];
	    Arrays.fill (validLinks, false);
		for (int m=0; m < g.getNumUserClasses(); m++) {
			validLinksForClass = g.getValidLinksForClass(m);
			for (int k=0; k < g.getLinkCount(); k++)
				if (validLinksForClass[k])
					validLinks[k] = true;
		}

			
		double[][] flow = new double[g.getNumUserClasses()][g.getLinkCount()];

		for (int m=0; m < g.getNumUserClasses(); m++)
		    Arrays.fill (flow[m], 0.0);

		double[] volau = new double[g.getLinkCount()];
        
		lambdas = new double[maxFwIteration];
		
		
		
        
        // loop thru FW iterations
        for (int iter=0; iter < maxFwIteration; iter++) {
        	
            if(logger.isDebugEnabled()) {
                logger.debug("Frank-Wolfe Iteration = " + iter);
            }
            lambdas[iter] = 1.0;

            
            

            // build and load shortest path trees using distributed methods and get back the loaded aon flows.
            sendAssignmentInfoMessages ( g, iter, firstOriginTaz, lastOriginTaz );
            aonFlow = null;
            sendBuildLoadStartMessage ( tripTable );

            // wait until the aon flow vectors are completely loaded.  When they're loaded, they'll
            // be returned via a new message and retrieved into the aonFlow array.
            while ( aonFlow == null ) {
            }
            
			
            

            // use bisect to do Frank-Wolfe averaging -- returns true if exact solution
            if (iter > 0) {
                if ( fw.bisect ( iter, validLinks, aonFlow, flow ) ) {
                    logger.error ("Exact FW optimal solution found.  Unlikely, better check into this!");
                    iter = maxFwIteration;
                }
            }
            else {
				glb = Double.NEGATIVE_INFINITY;
				lub = 0.0;
				gap = 0.0;
            }


            // print assignment iterations report
            lub = fw.ofValue(validLinks, flow);
			gap = Math.abs(fw.ofGap( validLinks, aonFlow, flow ));
            if ( ( lub - gap ) > glb )
                glb = lub - gap;

            logger.info ("Iteration " + myFormat.right(iter, 3)
                                + "    Lambda= " + myFormat.right(lambdas[iter], 8, 4)
                                + "    LUB= "    + myFormat.right(lub, 16, 4)
                                + "    Gap= "    + myFormat.right(gap, 16, 4)
                                + "    GLB= "    + myFormat.right(glb, 16, 4)
                                + "    LUB-GLB= "    + myFormat.right(lub-glb, 16, 4)
                                + "    RelGap= " + myFormat.right(100.0*(lub - glb)/glb, 7, 4) + "%");


            // update link flows and times
			for (int k=0; k < volau.length; k++) {
				volau[k] = 0;
				for (int m=0; m < g.getNumUserClasses(); m++) {
					flow[m][k] = flow[m][k] + lambdas[iter]*(aonFlow[m][k] - flow[m][k]);
					volau[k] += flow[m][k];
				}
			}
			g.setFlows(flow);
			g.setVolau(volau);
            g.applyVdfs(validLinks);

            
			g.logLinkTimeFreqs (validLinks);
			
        } // end of FW iter loop




        fw.linkSummaryReport( flow );

        fwFlowProps = fw.getFWFlowProps();
        
        
        // Write a DiskObject out that contains the Network Object, the Frank Wolfe flow
        // proportions array, and the Frank Wolfe shortest path trees Disk Object Array.
        // A separate SelectLinkAnalysis class will utilize this information to perform
        // on-demand select link analysis after the trip assignment process has finished.
        //createSelectLinkAnalysisDiskObject();
        
        

        logger.info ("");
        logger.info (myFormat.right("iter", 5) + myFormat.right("lambdas", 12) + myFormat.right("Flow Props", 12));
        for (int i=0; i < maxFwIteration; i++)
            logger.info (myFormat.right(i, 5) + myFormat.right(lambdas[i], 12, 6) + myFormat.right(100.0*fwFlowProps[i], 12, 4) + "%");
        logger.info ("");

        String myDateString = DateFormat.getDateTimeInstance().format(new Date());
        logger.info ("done with Frank-Wolfe assignment: " + myDateString);
    	
    	
    }
    
    
    private void sendBuildLoadStartMessage ( double[][][] tripTable ) {
    	
    	// establish that this controller task sends the message to the BuildLoadControllerQueue
        PortManager pManager = PortManager.getInstance();
        Port buildLoadInputPort = pManager.createPort( MessageID.AON_BUILD_LOAD_CONTROLLER_QUEUE );
        
    	// set the message id and trip table values needed by AonBuildLoadControllerTask to start Aon Build Load
    	Message startMsg = mFactory.createMessage();
    	startMsg.setId( MessageID.FW_BUILD_LOAD_START_ID );
    	startMsg.setValue( MessageID.TRIPTABLE_KEY, tripTable );

    	// send the work message to the BuildLoadControllerQueue
    	buildLoadInputPort.send(startMsg);
    	
    }

    
    private void sendAssignmentInfoMessages ( Network g, int fwIteration, int firstOriginTaz, int lastOriginTaz ) {
    	
    	// establish that this controller task sends the Assignment Information message to the BuildLoadCommonQueues for each node.
        PortManager pManager = PortManager.getInstance();
        
    	// send the assignment info message to the BuildLoadCommonQueues on each node
    	for (int i=0; i < MessageID.AON_BUILD_LOAD_COMMON_QUEUES.length; i++) {

        	// set the message id and values to be sent to AonBuildLoadCommonTasks
        	Message assignInfoMsg = mFactory.createMessage();
        	assignInfoMsg.setId( MessageID.FW_ASSIGNMENT_INFORMATION_ID );
        	assignInfoMsg.setValue( MessageID.NODE_KEY, Integer.valueOf(i) );
        	assignInfoMsg.setValue( MessageID.NETWORK_KEY, g );
        	assignInfoMsg.setValue( MessageID.FW_ITERATION_NUMBER_KEY, Integer.valueOf(fwIteration) );
        	assignInfoMsg.setValue( MessageID.FIRST_TAZ_NUMBER_KEY, Integer.valueOf(firstOriginTaz) );
        	assignInfoMsg.setValue( MessageID.LAST_TAZ_NUMBER_KEY, Integer.valueOf(lastOriginTaz) );

    		Port buildLoadInputPort = pManager.createPort( MessageID.AON_BUILD_LOAD_COMMON_QUEUES[i] );
        	buildLoadInputPort.send(assignInfoMsg);
        	
    	}
    	    	
    }

    
    private double[][] getMulticlassAonLinkFlows ( Message msg ) {
		return (double[][])msg.getValue( MessageID.FINAL_AON_FLOW_RESULT_VALUES_KEY );
    }
	
	private Network getNetworkObjectFromMessage( Message msg ){
		return (Network)msg.getValue( MessageID.NETWORK_KEY );
	}

	private double[][][] getTripTableFromMessage( Message msg ){
		return (double[][][])msg.getValue( MessageID.TRIPTABLE_KEY );
	}

	private HashMap getTsPropertyMapFromMessage( Message msg ){
		return (HashMap)msg.getValue( MessageID.TS_PROPERTYMAP_KEY );
	}
	
	private int getMaxFwIterationNumberFromMessage( Message msg ){
		return ((Integer)msg.getValue( MessageID.MAX_FW_ITERATION_NUMBER_KEY )).intValue();
	}
    
	private int getFirstAssignmentTazFromMessage( Message msg ){
		return ((Integer)msg.getValue( MessageID.FIRST_TAZ_NUMBER_KEY )).intValue();
	}
    
	private int getLastAssignmentTazFromMessage( Message msg ){
		return ((Integer)msg.getValue( MessageID.LAST_TAZ_NUMBER_KEY )).intValue();
	}
    
}

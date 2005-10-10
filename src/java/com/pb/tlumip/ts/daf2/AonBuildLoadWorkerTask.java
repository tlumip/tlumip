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

import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.assign.Network;
import com.pb.tlumip.ts.assign.ShortestPathTreeH;
import com.pb.tlumip.ts.daf2.MessageID;

/**
 *   Distributed application worker class used for implementing a
 *   distributed version of the Frank-Wolfe Algorithm for highway
 *   user equilibrium trip assignment.  This worker task class
 *   computes shortest path trees for the origin zone and user class
 *   passed in and returns the loaded aon flow vector as well as
 *   optionally the shortest path tree.
 */

public class AonBuildLoadWorkerTask extends MessageProcessingTask {

	private boolean LOGGING = true;
    static Logger logger = Logger.getLogger("com.pb.tlumip.ts.daf2");
    
    
    AonLinkFlowResults flowResults = AonLinkFlowResults.getInstance();
    AonNetworkManager gManager = AonNetworkManager.getInstance();
    
    NetworkHandler g = gManager.getNetworkHandler();
    ShortestPathTreeH sp = createShortestPathTreeHObject( g );
    double[][] cummulativeAon = flowResults.getCombinedLinkFlows();


	MessageFactory mFactory = MessageFactory.getInstance();

	
    public AonBuildLoadWorkerTask () {
    }

    
    public void onStart() {

    	if (LOGGING)
			logger.info( getName() + " started ");

    }

    
    public void onMessage(Message msg) {

        double[] aon = null;

        
        
		if (LOGGING)
		    logger.info( this.name +  " onMessage() id=" + msg.getId() + ", sent by " + msg.getSender() + "." );


        if(msg.getId().equals(MessageID.BUILDLOAD_WORK_ID)) {

        	// get information about the tree to be built and loaded
    		int rootOrig = getRootOriginTazFromMessage ( msg );
    		int userClass = getUserClassFromMessage ( msg );
    		double[] rootOrigTrips = getTripTableRowFromMessage( msg );

        	
    		// calculate link generalized costs and return those costs as
    		// the link attribute on which shortest paths are calculated.
    		sp.setLinkCost( g.setLinkGeneralizedCost() );

    		// set the links which are valid to be used in shortest paths
    		sp.setValidLinks( g.getValidLinksForClass( userClass ) );
    		
    		// build the shortest path tree for the origin specified over the valid set
    		// of links for the userclass, and load the trip table row onto the shortest path links,
    		// returning the AON link flow vector for this origin zone.
        	aon = buildAndLoadShortestPathTree( rootOrig, userClass, rootOrigTrips );
        	
        	// add the aon flows for this origin zone to the others being accummulated by this worker task
        	for (int i=0; i < aon.length; i++)
        		cummulativeAon[userClass][i] += aon[i];
        	
        	
        	flowResults.incrementWorkElementsCompleted();
        	
        }
        
    }

    
	private double[] buildAndLoadShortestPathTree( int rootOrig, int userClass, double[] rootOrigTrips ){
    
		sp.buildTree ( rootOrig );
		return sp.loadTree ( rootOrigTrips, userClass );
	
	}

	
	private ShortestPathTreeH createShortestPathTreeHObject( NetworkHandler g ) {
	
		return new ShortestPathTreeH( g );

	}

	
	private int getRootOriginTazFromMessage( Message msg ){
		return ((Integer)msg.getValue( MessageID.BUILDLOAD_ROOT_ORIGIN_TAZ_KEY )).intValue();
	}
    
	private int getUserClassFromMessage( Message msg ){
		return ((Integer)msg.getValue( MessageID.BUILDLOAD_USER_CLASS_KEY )).intValue();
	}
    
	private double[] getTripTableRowFromMessage( Message msg ){
		return (double[])msg.getValue( MessageID.BUILDLOAD_ROOT_ORIGIN_TRIPTABLE_ROW_KEY );
	}
    
}

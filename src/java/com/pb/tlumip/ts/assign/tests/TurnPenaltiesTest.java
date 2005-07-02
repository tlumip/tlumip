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
package com.pb.tlumip.ts.assign.tests;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 5/7/2004
 */


import com.pb.tlumip.ts.assign.Network;
import com.pb.tlumip.ts.assign.ShortestPathTreeH;
import com.pb.common.util.ResourceUtil;

import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;



public class TurnPenaltiesTest {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.assign.tests");

    
	HashMap tsPropertyMap;
    HashMap globalPropertyMap;
	Network g = null;
	
	
	
	public TurnPenaltiesTest() {

		tsPropertyMap = ResourceUtil.getResourceBundleAsHashMap("tpTest");
        globalPropertyMap = ResourceUtil.getResourceBundleAsHashMap("global");

	}
    
	
	private void runTest (String period, float volumeFactor) {
        
		g = new Network( tsPropertyMap, globalPropertyMap, period, volumeFactor );
		logger.info ("done building Network object.");

		
		// build shortest path tree object and set cost and valid link attributes for this user class.
		ShortestPathTreeH sp = new ShortestPathTreeH( g );

		// set the highway network attribute on which to skim the network
		double[] linkCost = g.getCongestedTime();
		
		// set all links as valid for this test
		boolean[] validLinks = new boolean[g.getLinkCount()];
		Arrays.fill (validLinks, true);
   
		sp.setLinkCost( linkCost );
		sp.setValidLinks( validLinks );
		

		// build and print shortest path from 1 to 2
		sp.printPath( 1, 2 );
		
	}


    
	public static void main (String[] args) {
        
		long startTime = System.currentTimeMillis();
		
		TurnPenaltiesTest test = new TurnPenaltiesTest();
		test.runTest("peak", 0.5f);
        
		logger.info("TurnPenaltiesTest() finished in " +
			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");
	}


}

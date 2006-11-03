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


import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.NetworkHandlerIF;
import com.pb.tlumip.ts.assign.ShortestPathTreeH;
import com.pb.common.util.ResourceUtil;

import java.io.File;
import java.util.Arrays;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;



public class TurnPenaltiesTest {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.assign.tests");

    
    ResourceBundle rb;
    ResourceBundle globalRb;
	
	
	
	public TurnPenaltiesTest() {

        rb = ResourceUtil.getPropertyBundle( new File("/jim/tlumip/data/test/tpTest.properties") );
        globalRb = ResourceUtil.getPropertyBundle(new File("/jim/util/svn_workspace/projects/tlumip/config/global.properties"));

	}
    
	
	private void runTest (String period) {
        
        NetworkHandlerIF nh = NetworkHandler.getInstance();
        nh.setup( rb, globalRb, period );
		logger.info ("done building Network object.");

		
		// build shortest path tree object and set cost and valid link attributes for this user class.
		ShortestPathTreeH sp = new ShortestPathTreeH( nh );

		// set the highway network attribute on which to skim the network
		double[] linkCost = nh.getCongestedTime();
		
		// set all links as valid for this test
		boolean[] validLinks = new boolean[nh.getLinkCount()];
		Arrays.fill (validLinks, true);
   
		sp.setLinkCost( linkCost );
		sp.setValidLinks( validLinks );
		

		// build and print shortest path from 1 to 2
		sp.printPath( 1, 2 );
		
	}


    
	public static void main (String[] args) {
        
		long startTime = System.currentTimeMillis();
		
		TurnPenaltiesTest test = new TurnPenaltiesTest();
		test.runTest("peak");
        
		logger.info("TurnPenaltiesTest() finished in " +
			((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");
	}


}

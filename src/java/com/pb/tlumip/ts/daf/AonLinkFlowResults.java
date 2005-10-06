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
/*
 * Created on Jun 22, 2005
 *
 */
package com.pb.tlumip.ts.dafv3;

/**
 * @author Jim Hicks
 * 
 * An instance of this singleton class will exist in each AonBuildLoadWorkerTasks
 * and an instance will also exist in the AonBuildLoadCommonTask on each node.
 * This class holds a single copy of the Network object used by all worker tasks. 
 *
 */
final class AonLinkFlowResults {
	
    protected static Object objLock = new Object();

    private double[][] combinedAonFlows = null;
    private int workElementsCompleted = 0;
	
	private static AonLinkFlowResults instance = new AonLinkFlowResults();

    /** Keep this class from being created with "new".
    *
    */
	private AonLinkFlowResults() {
	}

    /** Return instances of this class.
    *
    */
	public static AonLinkFlowResults getInstance() {
		return instance;
	}

	
	public void initializeFlowArray ( int numLinks, int numClasses ) {
		combinedAonFlows = new double[numLinks][numClasses];
	}
	

	public void combineLinkFlows ( int userClass, double[] aonFlows ) {
		
        synchronized (objLock) {

        	for (int i=0; i < aonFlows.length; i++)
        		combinedAonFlows[userClass][i] += aonFlows[i];
        	
        }
        	
	}
	
	public double[][] getCombinedLinkFlows () {
		return combinedAonFlows;
	}
	
	public int getWorkElementsCompleted () {
		return this.workElementsCompleted;
	}
	
	public void resetWorkElementsCompleted () {
		this.workElementsCompleted = 0;
	}
	
	public void incrementWorkElementsCompleted () {
		this.workElementsCompleted ++;
	}
	
}

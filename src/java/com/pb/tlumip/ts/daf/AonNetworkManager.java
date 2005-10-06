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

import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.assign.Network;

/**
 * @author Jim Hicks
 * 
 * An instance of this singleton class will exist in each AonBuildLoadWorkerTasks
 * and an instance will also exist in the AonBuildLoadCommonTask on each node.
 * This class holds a single copy of the Network object used by all worker tasks on the node. 
 *
 */
final class AonNetworkManager {
	
    protected NetworkHandler g = null;
    protected int firstTaz = 0;
    protected int lastTaz = 0;
    protected int fwIteration = 0;
	
	private static AonNetworkManager instance = new AonNetworkManager();

    /** Keep this class from being created with "new".
    *
    */
	private AonNetworkManager() {
	}

    /** Return instances of this class.
    *
    */
	public static AonNetworkManager getInstance() {
		return instance;
	}

	
	public void setNetworkHandler ( NetworkHandler g ) {
		this.g = g;
	}
	
	public NetworkHandler getNetworkHandler () {
		return this.g;
	}
	
	public void setFirstTaz ( int firstTaz ) {
		this.firstTaz = firstTaz;
	}
	
	public int getFirstTaz () {
		return this.firstTaz;
	}
	
	public void setLastTaz ( int lastTaz ) {
		this.lastTaz = lastTaz;
	}
	
	public int getLastTaz () {
		return this.lastTaz;
	}
	
	public void setFwIteration ( int fwIteration ) {
		this.fwIteration = fwIteration;
	}
	
	public int getFwIteration () {
		return this.fwIteration;
	}
	
}

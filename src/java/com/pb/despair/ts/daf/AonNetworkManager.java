/*
 * Created on Jun 22, 2005
 *
 */
package com.pb.tlumip.ts.daf;

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
	
    protected Network g = null;
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

	
	public void setNetwork ( Network g ) {
		this.g = g;
	}
	
	public Network getNetwork () {
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

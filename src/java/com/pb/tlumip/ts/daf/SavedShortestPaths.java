/*
 * Created on Jun 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.pb.tlumip.ts.daf;

import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * @author Jim Hicks
 * 
 * An instance of this singleton class will exist in each AonBuildLoadWorkerTasks
 * which post paths to be saved by the worker classes.  An instance will also exist
 * in the AonBuildLoadResultsTask which will return saved paths to the AonBuildLoadController
 * upon its request. 
 *
 */
final class SavedShortestPaths {
	
    protected static Object objLock = new Object();

    static Logger logger = Logger.getLogger("com.pb.tlumip.ts.daf");
    protected HashMap storedPaths = new HashMap();
	
	private static SavedShortestPaths instance = new SavedShortestPaths();

    /** Keep this class from being created with "new".
    *
    */
	private SavedShortestPaths() {
	}

	
    /** Return instances of this class.
    *
    */
	public static SavedShortestPaths getInstance() {
		return instance;
	}

	
	public void initializePathHashMap () {
		storedPaths.clear();
	}
	

	public void storeShortestPathTree ( int[] predecessorLink, int numZones, int numClasses, int fwIter, int rootTaz, int userClass ) {
		
        // Calculate the index used for storing shortest path tree in a HashMap.
		// This HashMap will be retrieved by AonBuilLoadControllerTask and saved in a DiskObjectArray
	    Integer storedPathIndex = Integer.valueOf( fwIter*numZones*numClasses + rootTaz*numClasses + userClass );
	    
        synchronized (objLock) {

		    // store the shortest path tree for this iteration, origin zone, and user class
		    try {
		        storedPaths.put ( storedPathIndex, predecessorLink );
		    } catch (Exception e) {
		        logger.error ( "could not store shortest path tree using index=" + storedPathIndex + ", for iter=" + fwIter + ", for origin=" + rootTaz + ", and class=" + userClass, e );	        e.printStackTrace();
		        System.exit(1);
		    }
		        
        }
        	
	}
	
}

package com.pb.tlumip.ts;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

public interface SpBuildLoadHandlerIF {

    public static final String HANDLER_NAME = "spBuildLoadHandler";
    
    public int setup(double[][][] tripTables, NetworkHandlerIF nh, BlockingQueue workQueue, HashMap controlMap, HashMap resultsMap );
    public int start();
    
}

package com.pb.tlumip.ts;

public interface SpBuildLoadHandlerIF {

    public static final String HANDLER_NAME = "spBuildLoadHandler";
    
    public int setup(double[][][] tripTables );
    public int start();
    public double[][] getResults();
    public boolean handlerIsFinished();
    
}

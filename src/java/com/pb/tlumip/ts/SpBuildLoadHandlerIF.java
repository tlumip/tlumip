package com.pb.tlumip.ts;

public interface SpBuildLoadHandlerIF {

    public static final String HANDLER_NAME = "spBuildLoadHandler";
    
    public int setup( String handlerName, String rpcConfigFile, NetworkHandlerIF nh, DemandHandlerIF dh );
    public int setup( String handlerName, String rpcConfigFile );
    public int start();
    public double[][] getResults();
    public boolean handlerIsFinished();
    
}

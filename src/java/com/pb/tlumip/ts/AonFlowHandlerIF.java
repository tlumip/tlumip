package com.pb.tlumip.ts;

public interface AonFlowHandlerIF {

    public static String HANDLER_NAME = "aonFlowHandler";
    
    public boolean setup( String rpcConfigFile, String ptFileName, String ctFileName, int startHour, int endHour, char[] highwayModeCharacters, NetworkHandlerIF nh );
    public double[][] getMulticlassAonLinkFlows ();

}

package com.pb.tlumip.ts;

public interface AonFlowHandlerIF {

    public static String HANDLER_NAME = "aonFlowHandler";
    
    public boolean setup( String rpcConfigFile, String sdtFileName, String ldtFileName, String ctFileName, int startHour, int endHour, char[] highwayModeCharacters, NetworkHandlerIF nh );
    public double[][] getMulticlassAonLinkFlows ();

}

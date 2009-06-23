package com.pb.tlumip.ts;

public interface AonFlowHandlerIF {

    public static String HANDLER_NAME = "aonFlowHandler";
    
    public boolean setup( String reportFileName, String rpcConfigFile, String sdtFileName, String ldtFileName, double ptSampleRate, String ctFileName, String etFileName, int startHour, int endHour, char[] highwayModeCharacters, NetworkHandlerIF nh );
    public double[][] getMulticlassAonLinkFlows ();
    public int[][][] getSavedShortestPathTrees ();

}

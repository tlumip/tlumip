package com.pb.tlumip.ts;

import java.util.ResourceBundle;

public interface AonFlowHandlerIF {

    public static String HANDLER_NAME = "aonFlowHandler";
    
    public boolean setup( ResourceBundle componentRb, ResourceBundle globalRb, NetworkHandlerIF nh, char[] highwayModeCharacters );
    public double[][] getMulticlassAonLinkFlows ();
}

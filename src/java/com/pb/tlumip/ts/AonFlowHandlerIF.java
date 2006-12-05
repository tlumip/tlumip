package com.pb.tlumip.ts;

import java.util.HashMap;
import java.util.ResourceBundle;


public interface AonFlowHandlerIF {

    public boolean setup( HashMap componentPropertyMap, HashMap globalPropertyMap );
    public boolean setup( ResourceBundle componentRb, ResourceBundle globalRb );
    public double[][] getMulticlassAonLinkFlows ();
}

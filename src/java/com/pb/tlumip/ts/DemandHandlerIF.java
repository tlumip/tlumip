package com.pb.tlumip.ts;

import java.util.HashMap;
import java.util.ResourceBundle;


public interface DemandHandlerIF {

    public void setNetworkAttributes( int numCentroids, int numUserClasses, int[] nodeIndexArray, HashMap assignmentGroupMap, boolean userClassesIncludeTruck );
    public boolean setup( HashMap componentPropertyMap, HashMap globalPropertyMap, String timePeriod );
    public boolean setup( ResourceBundle componentRb, ResourceBundle globalRb, String timePeriod );
    public double[][][] getMulticlassTripTables ();
    public double[][] getTripTableRowSums ();

}

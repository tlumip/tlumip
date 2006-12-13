package com.pb.tlumip.ts;

import java.util.HashMap;
import java.util.ResourceBundle;


public interface DemandHandlerIF {

    public boolean setup( HashMap componentPropertyMap, HashMap globalPropertyMap, String timePeriod );
    public boolean setup( ResourceBundle componentRb, ResourceBundle globalRb, String timePeriod );
    public int setNetworkAttributes( int numCentroids, int numUserClasses, int[] nodeIndexArray, HashMap assignmentGroupMap, boolean userClassesIncludeTruck );
    public double[][][] getMulticlassTripTables();
    public double[][] getTripTableRowSums();
    public double[] getTripTableRow(int userClass, int row);
    
}

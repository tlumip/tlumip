package com.pb.tlumip.ts;

import java.util.HashMap;


public interface DemandHandlerIF {

    public static final String HANDLER_NAME = "demandHandler";
    
    public boolean setup( HashMap componentPropertyMap, HashMap globalPropertyMap, String timePeriod, int numCentroids, int numUserClasses, int[] nodeIndexArray, HashMap assignmentGroupMap, char[] highwayModeCharacters, boolean userClassesIncludeTruck );
    public boolean buildDemandObject();
    public double[][][] getMulticlassTripTables();
    public double[][] getTripTableRowSums();
    public double[] getTripTableRow(int userClass, int row);
    
}

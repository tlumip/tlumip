package com.pb.tlumip.ts;

public interface DemandHandlerIF {

    public static final String HANDLER_NAME = "demandHandler";
    
    public boolean setup( String ptFileName, String ctFileName, int startHour, int endHour, String timePeriod, int numCentroids, int numUserClasses, int[] nodeIndexArray, char[][] assignmentGroupChars, char[] highwayModeCharacters, boolean userClassesIncludeTruck );
    public boolean buildDemandObject();
    public double[][][] getMulticlassTripTables();
    public double[][] getTripTableRowSums();
    public double[] getTripTableRow(int userClass, int row);
    
}

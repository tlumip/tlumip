package com.pb.tlumip.ts;

import java.util.ArrayList;

public interface DemandHandlerIF {

    public static final String HANDLER_NAME = "demandHandler";
    
    public boolean setup( float[] userClassPces, String sdtFileName, String ldtFileName, double ptSampleRate, String ctFileName, String etFileName, int startHour, int endHour, String timePeriod, int numCentroids, int numUserClasses, int[] nodeIndexArray, int[] alphaDistrictArray, String[] alphaDistrictNames, char[][] assignmentGroupChars, char[] highwayModeCharacters, boolean userClassesIncludeTruck );
    public boolean buildHighwayDemandObject();
    public int logDistrictReport();
    public int writeDistrictReport ( String fileName );    
    public double[][][] getMulticlassTripTables();
    public double[][] getTripTableRowSums();
    public double[] getTripTableRow(int userClass, int row);
    public double[][] getTripTableForMode ( String tripMode );
    public double[][] getTripTablesForModes ( ArrayList<String> tripModes );
    
}

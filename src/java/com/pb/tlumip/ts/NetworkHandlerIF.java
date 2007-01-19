package com.pb.tlumip.ts;

public interface NetworkHandlerIF {

    public static final String HANDLER_NAME = "networkHandler";
    
    public static int NETWORK_FILENAME_INDEX = 0;
    public static int NETWORK_DISKOBJECT_FILENAME_INDEX = 1;
    public static int VDF_FILENAME_INDEX = 2;
    public static int VDF_INTEGRAL_FILENAME_INDEX = 3;
    public static int VOLUME_FACTOR_INDEX = 4;
    public static int ALPHA2BETA_FILENAME_INDEX = 5;
    public static int TURNTABLE_FILENAME_INDEX = 6;
    public static int NETWORKMODS_FILENAME_INDEX = 7;
    public static int USER_CLASSES_STRING_INDEX = 8;
    public static int TRUCKCLASS1_STRING_INDEX = 9;
    public static int TRUCKCLASS2_STRING_INDEX = 10;
    public static int TRUCKCLASS3_STRING_INDEX = 11;
    public static int TRUCKCLASS4_STRING_INDEX = 12;
    public static int TRUCKCLASS5_STRING_INDEX = 13;
    public static int WALK_SPEED_INDEX = 14;
    
    public static int NUMBER_OF_PROPERTY_VALUES = 15;
    
    
    public int setRpcConfigFileName(String configFile);
    public String getRpcConfigFileName();
    public int getNumCentroids();
    public int getMaxCentroid();
    public boolean[] getCentroid();
    public int getNodeCount();
    public int getLinkCount();
    public int getNumUserClasses();
    public String getTimePeriod ();
    public boolean userClassesIncludeTruck();
    public boolean[][] getValidLinksForAllClasses ();
    public boolean[] getValidLinksForClass ( int userClass );
    public boolean[] getValidLinksForClass ( char modeChar );
    public int[] getVdfIndex ();
    public int[] getNodeIndex ();
    public int[] getLinkType ();
    public char[][] getAssignmentGroupChars();
    public double[] getLanes();
    public double[] getCapacity();
    public double[] getCongestedTime();
    public double[] getFreeFlowTime();
    public double[] getFreeFlowSpeed();
    public double[] getTransitTime();
    public double[] getDist();
    public double[] getVolau();
    public double[] setLinkGeneralizedCost ();
    public int setFlows (double[][] flow);
    public int setVolau (double[] volau);
    public int setTimau (double[] timau);
    public int setVolCapRatios ();
    public double applyLinkTransitVdf (int hwyLinkIndex, int transitVdfIndex );
    public int applyVdfs ();
    public int applyVdfIntegrals ();
    public double getSumOfVdfIntegrals ();
    public int logLinkTimeFreqs ();
    public char[] getUserClasses ();
    public String[] getMode ();
    public int[] getIndexNode ();
    public int[] getNodes ();
    public double[] getNodeX ();
    public double[] getNodeY ();
    public int[] getIa();
    public int[] getIb();
    public int[] getIpa();
    public int[] getSortedLinkIndexA();
    public double getWalkSpeed ();
    public int writeNetworkAttributes ( String fileName );
    public int checkForIsolatedLinks ();
    public int buildNetworkObject ( String timePeriod, String[] propertyValues  );
    public String getAssignmentResultsString ();
    public String getAssignmentResultsTimeString ();
}

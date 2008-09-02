package com.pb.tlumip.ts;


public interface NetworkHandlerIF {

    public static final String HANDLER_NAME = "networkHandler";

    public static final int networkDataServerPort = 6003;
    public static final String dataServerName = "networkDataServer";
    
    
    public static int NETWORK_FILENAME_INDEX = 0;
    public static int NETWORK_DISKOBJECT_FILENAME_INDEX = 1;
    public static int VDF_FILENAME_INDEX = 2;
    public static int VDF_INTEGRAL_FILENAME_INDEX = 3;
    public static int VOLUME_FACTOR_INDEX = 4;
    public static int ALPHA2BETA_FILENAME_INDEX = 5;
    public static int TURNTABLE_FILENAME_INDEX = 6;
    public static int NETWORKMODS_FILENAME_INDEX = 7;
    public static int EXTRA_ATTRIBS_FILENAME_INDEX = 8;
    public static int USER_CLASSES_STRING_INDEX = 9;
    public static int TRUCKCLASS1_STRING_INDEX = 10;
    public static int TRUCKCLASS2_STRING_INDEX = 11;
    public static int TRUCKCLASS3_STRING_INDEX = 12;
    public static int TRUCKCLASS4_STRING_INDEX = 13;
    public static int TRUCKCLASS5_STRING_INDEX = 14;
    public static int WALK_SPEED_INDEX = 15;
    public static int USER_CLASS_PCES_STRING_INDEX = 16;

    public static int NUMBER_OF_PROPERTY_VALUES = 17;
    
    
    public void startDataServer();
    public void stopDataServer();
    public boolean getStatus();
    public int setRpcConfigFileName(String configFile);
    public String getRpcConfigFileName();
    public int getNumCentroids();
    public int getMaxCentroid();
    public boolean[] getCentroid();
    public int[] getExternalZoneLabels ();
    public int getNodeCount();
    public int getLinkCount();
    public int getLinkIndex(int an, int bn);
    public int getLinkIndexExitingNode(int an);
    public int[] getLinksExitingNode(int an);
    public int getNumUserClasses();
    public String getTimePeriod ();
    public boolean userClassesIncludeTruck();
    public char[] getHighwayModeCharacters();
    public char[] getTransitModeCharacters();
    public boolean[] getValidLinksForTransitPaths();
    public boolean[][] getValidLinksForAllClasses ();
    public boolean[] getValidLinksForClass ( int userClass );
    public boolean[] getValidLinksForClassChar ( int modeChar );
    public int[] getOnewayLinksForClass ( int userClass );
    public int[] getVdfIndex ();
    public int[] getNodeIndex ();
    public int[] getInternalNodeToNodeTableRow();
    public double[] getCoordsForLink(int k);
    public int[] getLinkType();
    public int[] getTaz();
    public int[] getDrops();
    public int[] getUniqueIds();
    public char[][] getAssignmentGroupChars();
    public float[] getUserClassPces();
    public double[] getLanes();
    public double[] getCapacity();
    public double[] getOriginalCapacity();
    public double[] getTotalCapacity();
    public double[] getCongestedTime();
    public double[] getFreeFlowTime();
    public double[] getFreeFlowSpeed();
    public double[] getTransitTime();
    public double[] getDist();
    public double[] getTotalLinkCost();
    public double[] getLinkAttribCosts( int userClass );
    public double[] getVolau();
    public int[][] getTurnPenaltyIndices ();
    public float[][] getTurnPenaltyArray ();
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
    public int linkSummaryReport ( double[][] flow );    
    public char[] getUserClasses ();
    public String[] getMode ();
    public int[] getIndexNode ();
    public int getExternalNode (int internalNode);
    public int getInternalNode (int externalNode);
    public int[] getNodes ();
    public double[] getNodeX ();
    public double[] getNodeY ();
    public int[] getIa();
    public int[] getIb();
    public int[] getIpa();
    public int[] getIpb();
    public int[] getSortedLinkIndexA();
    public int[] getSortedLinkIndexB();
    public double getWalkSpeed ();
    public int writeNetworkAttributes ( String fileName );
    public int checkForIsolatedLinks ();
    public String getAssignmentResultsString ();
    public String getAssignmentResultsAnodeString ();
    public String getAssignmentResultsBnodeString ();
    public String getAssignmentResultsTimeString ();
    
    public String[] getDistrictNames ();
    public int[] getAlphaDistrictIndex ();

    public int setupHighwayNetworkObject ( String timePeriod, String[] propertyValues  );
    public int setupTransitNetworkObject ( String identifier, String period, String accessMode, String auxTransitNetworkListingFileName, String transitRouteDataFilesDirectory, String[] d221Files, String[] rteTypes, int maxRoutes );

    
    public int[] getAuxIa(String identifier);
    public int[] getAuxIb(String identifier);
    public int[] getAuxIpa(String identifier);
    public int[] getAuxIpb(String identifier);
    public int[] getAuxIndexa(String identifier);
    public int[] getAuxIndexb(String identifier);
    public int[] getAuxHwyLink(String identifier);

    public int[] getLinkTrRoute(String identifier);
    public char[] getRteMode(String identifier);
    public int[] getAuxLinkType(String identifier);
    public double[] getAuxCost(String identifier);
    public double[] getAuxDwellTime(String identifier);
    public double[] getAuxLayoverTime(String identifier);
    public double[] getAuxWaitTime(String identifier);
    public double[] getAuxWalkTime(String identifier);
    public double[] getAuxDriveAccTime(String identifier);
    public double[] getAuxInvTime(String identifier);
    public double[] getAuxLinkFreq(String identifier);
    public double[] getAuxLinkFlow(String identifier);
    public int getAuxNodeCount( String identifier );
    public int getAuxLinkCount( String identifier );
    public double getAuxLinkImped (String identifier, int k);
    public String getAuxRouteName(String identifier, int rte);
    public String getAuxRouteDescription(String identifier, int rte);
    public char getAuxRouteMode(String identifier, int rte);
    public String getAuxRouteType(String identifier, int rte);
    public int getAuxNumRoutes(String identifier);
    
    
    /*
    public String getAccessMode();
    public String[] getTransitRouteNames();
    public String[] getTransitRouteTypes();
    public int[] getTransitRouteLinkIds(String rteName);
    public int[] getStationDriveAccessNodes(int stationNode);
    */
    
}

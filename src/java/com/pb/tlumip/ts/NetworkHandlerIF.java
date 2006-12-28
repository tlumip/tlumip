package com.pb.tlumip.ts;

import java.util.HashMap;
import java.util.ResourceBundle;

import com.pb.tlumip.ts.assign.Network;


public interface NetworkHandlerIF {

    public static final String HANDLER_NAME = "networkHandler";
    
    public int setRpcConfigFileName(String configFile);
    public String getRpcConfigFileName();
    public Network getNetwork();
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
    public int[] getNodeIndex ();
    public int[] getLinkType ();
    public HashMap getAssignmentGroupMap();
    public double[] getCongestedTime ();
    public double[] getTransitTime ();
    public double[] getDist ();
    public double[] setLinkGeneralizedCost ();
    public int setFlows (double[][] flow);
    public int setVolau (double[] volau);
    public int setVolCapRatios ();
    public double applyLinkTransitVdf (int hwyLinkIndex, int transitVdfIndex );
    public int applyVdfs ();
    public int applyVdfIntegrals ();
    public double getSumOfVdfIntegrals ();
    public int logLinkTimeFreqs ();
    public char[] getUserClasses ();
    public String[] getMode ();
    public int[] getIndexNode ();
    public double[] getNodeX ();
    public double[] getNodeY ();
    public int[] getIa();
    public int[] getIb();
    public int[] getIpa();
    public int[] getSortedLinkIndexA();
    public double getWalkSpeed ();
    public int writeNetworkAttributes ( String fileName );
    public int checkForIsolatedLinks ();
    public int setup( String appPropertyName, String globalPropertyName, String assignmentPeriod );
    public int setup( ResourceBundle appRb, ResourceBundle globalRb, String assignmentPeriod );

}

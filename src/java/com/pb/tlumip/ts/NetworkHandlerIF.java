package com.pb.tlumip.ts;

import java.util.HashMap;
import java.util.ResourceBundle;

import com.pb.tlumip.ts.assign.Network;


public interface NetworkHandlerIF {

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
    public double[] getDist ();
    public double[] setLinkGeneralizedCost ();
    public int setFlows (double[][] flow);
    public int setVolau (double[] volau);
    public int setVolCapRatios ();
    public int applyVdfs ();
    public int applyVdfIntegrals ();
    public double getSumOfVdfIntegrals ();
    public int logLinkTimeFreqs ();
    public char[] getUserClasses ();
    public int[] getIndexNode ();
    public int[] getIa();
    public int[] getIb();
    public int[] getIpa();
    public int[] getSortedLinkIndexA();
    public int writeNetworkAttributes ( String fileName );
    public int checkForIsolatedLinks ();
    public int setup( String appPropertyName, String globalPropertyName, String assignmentPeriod );
    public int setup( ResourceBundle appRb, ResourceBundle globalRb, String assignmentPeriod );

}

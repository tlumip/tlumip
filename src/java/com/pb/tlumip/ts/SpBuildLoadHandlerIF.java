package com.pb.tlumip.ts;

public interface SpBuildLoadHandlerIF {

    public static final String HANDLER_NAME = "spBuildLoadHandler";
    
    public int setup( String handlerName, String rpcConfigFile, int[][][] workElements, double[][][] workElementsDemand, int numUserClasses, int numLinks, int numNodes, int numZones, int[] ia, int[] ib, int[] ipa, int[] sortedLinkIndexA, int[] indexNode, int[] nodeIndex, boolean[] centroid, boolean[][] validLinksForClasses, double[] linkCost, int[][] turnPenaltyIndices, float[][] turnPenaltyArray );
    public int start( double[] linkCost );
    public double[][] getResults();
    public boolean handlerIsFinished();
    public int getNumberOfThreads();
    public int[] getShortestPathTree ( int userClassIndex, int internalOriginTazIndex );    
}

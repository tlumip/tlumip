/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tlumip.ts.daf3;

import com.pb.common.datafile.DiskObjectArray;
import com.pb.common.rpc.RpcClient;
import com.pb.common.rpc.RpcException;
import com.pb.tlumip.ts.DemandHandler;
import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.AonFlowHandler;
import com.pb.tlumip.ts.ShortestPathTreeHandler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;


public class FW {

    Logger logger = Logger.getLogger("com.pb.tlumip.ts.daf3.FW");

	static final int SIZEOF_INT = 4;

    HashMap appPropertyMap;
    HashMap globalPropertyMap;
	

    double [] lambdas;
    double [] fwFlowProps;
    
    int numAutoClasses;
    int numLinks;
    int startOriginTaz;
    int lastOriginTaz;;
	int maxFwIters;
	double fwGap;

    String timePeriod;

    DiskObjectArray fwPathsDoa = null;	

    RpcClient networkHandlerClient;    
    RpcClient demandHandlerClient;    
    RpcClient aonFlowHandlerClient;    
    RpcClient shortestPathHandlerClient;    
    

   
    public FW () {
    
        String handlerName = null;
        
        try {
        
            //Need a config file to initialize a Daf node
//            DafNode.getInstance().init("fw-client", TS.tsRpcConfigFileName);

            //Create RpcClients this class connects to
            try {
                handlerName = NetworkHandler.remoteHandlerName;
                networkHandlerClient = new RpcClient( handlerName );

                handlerName = DemandHandler.remoteHandlerName;
                demandHandlerClient = new RpcClient( handlerName );
                
                handlerName = AonFlowHandler.remoteHandlerName;
                aonFlowHandlerClient = new RpcClient( handlerName );
                
                handlerName = ShortestPathTreeHandler.remoteHandlerName;
                shortestPathHandlerClient = new RpcClient( handlerName );
            }
            catch (MalformedURLException e) {
            
                logger.error ( "MalformedURLException caught in FW() while defining RpcClients.", e );
            
            }

        }
        catch ( Exception e ) {
            logger.error ( "Exception caught in FW().", e );
            System.exit(1);
        }

    }


    public void initialize ( HashMap tsPropertyMap, HashMap globalPropertyMap ) {

        this.appPropertyMap = tsPropertyMap;
        this.globalPropertyMap = globalPropertyMap;

        
        maxFwIters = Integer.parseInt ( (String)appPropertyMap.get( "NUM_FW_ITERATIONS" ) );
        fwGap = Double.parseDouble ( (String)appPropertyMap.get( "FW_RELATIVE_GAP" ) );

        lambdas = new double[maxFwIters];
        fwFlowProps = new double[maxFwIters];

        try {
            
            // get network related variables needed in FW object
            startOriginTaz = 0;
            numLinks = networkHandlerGetLinkCountRpcCall();
            lastOriginTaz = networkHandlerGetNumCentroidsRpcCall();
            numAutoClasses = networkHandlerGetNumUserClassesRpcCall();
            timePeriod = networkHandlerGetTimePeriodRpcCall();

            // setup the demandHandler object 
            demandHandlerSetupRpcCall();
            
            // setup the ShortestPathTreeHandler that aonFlowHandler uses
            shortestPathHandlerSetupRpcCall();

            // setup the aonFlowHandlerClient object
            aonFlowHandlerSetupRpcCall();
            
            // create a DiskObjectArray for storing shortest path trees
            createDiskObjectArray();
        
        }
        catch ( RpcException e ) {
            logger.error ( "RpcException caught.", e );
            System.exit(1);
        }
        catch ( IOException e ) {
            logger.error ( "IOException caught.", e );
            System.exit(1);
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }

    }

    

    /**
     * Frank-Wolfe assignment procedure.
     */
	public void iterate () {

        
        int iterationsCompleted = 0;
        boolean converged = false;
		
        try {

            
            double lub = 0.0;
    	    double gap = 0.0;
    	    double glb = 0.0;
    
    		double[][] flow = new double[numAutoClasses][numLinks];

            
            
            // loop thru FW iterations
            for (int iter=0; iter < maxFwIters && !converged; iter++) {
            	
                if(logger.isDebugEnabled()) {
                    logger.debug("Iteration = " + iter);
                }
                lambdas[iter] = 1.0;
    

                double[] linkCost = networkHandlerSetLinkGeneralizedCostRpcCall();
                shortestPathHandlerSetLinkCostRpcCall( linkCost );
                    
            	double[][] aonFlow = aonFlowHandlerGetMulticlassAonLinkFlowsRpcCall();
    			
                
                if (iter > 0) {
                    // print assignment iterations report
                    lub = networkHandlerGetFwOfValueRpcCall ( flow );
                    gap = Math.abs( networkHandlerGetFwGapValueRpcCall( aonFlow, flow ));
                    if ( ( lub - gap ) > glb )
                        glb = lub - gap;

                    if ( Math.abs( (lub - glb)/glb ) < fwGap )
                        converged = true;
                    
                }
                else {
                    glb = Double.NEGATIVE_INFINITY;
                    lub = 0.0;
                    gap = 0.0;
                }

                logger.info ("Iteration " + String.format("%3d", iter)
                        + "    Lambda= " + String.format("%8.4f", lambdas[iter])
                        + "    LUB= "    + String.format("%16.4f", lub)
                        + "    Gap= "    + String.format("%16.4f", gap)
                        + "    GLB= "    + String.format("%16.4f", glb)
                        + "    LUB-GLB= "    + String.format("%16.4f", lub-glb)
                        + "    RelGap= " + String.format("%7.4f%%", 100.0*(lub - glb)/glb, 7, 4) );
    
    
                
                // use bisect to do Frank-Wolfe averaging -- returns true if exact solution
                lambdas[iter] = networkHandlerFwBisectRpcCall( iter, aonFlow, flow);
                if ( lambdas[iter] == -1.0 ) {
                    logger.error ("Exact FW optimal solution found.  Unlikely, better check into this!");
                    lambdas[iter] = 0.5;
                    iter = maxFwIters;
                }

                
                // sum total flow over all user classes for each link 
                for (int m=0; m < numAutoClasses; m++) {
                    for (int k=0; k < numLinks; k++) {
                        flow[m][k] = flow[m][k] + lambdas[iter]*( aonFlow[m][k] - flow[m][k] );
                    }
                }

                
                
                
//                networkHandlerLogLinkTimeFreqsRpcCall ();
    			
                iterationsCompleted++;
                
            } // end of FW iter loop
    
    
    
    
            networkHandlerLinkSummaryReportRpcCall( flow );
    
            fwFlowProps = getFWFlowProps();
            
            
            // Write a DiskObject out that contains the Network Object, the Frank Wolfe flow
            // proportions array, and the Frank Wolfe shortest path trees Disk Object Array.
            // A separate SelectLinkAnalysis class with utilize this information to perform
            // on-demand select link analysis after the trip assignment process has finished.
            networkHandlerCreateSelectLinkAnalysisDiskObjectRpcCall( fwFlowProps );
            
            
    
            logger.info ("");
            logger.info ( String.format( "%5s %12s %12s", "iter", "lambdas", "Flow Props" ) );
            for (int i=0; i < maxFwIters; i++)
                logger.info ( String.format("%6d %12.6f %12.4f%%", i, lambdas[i], 100.0*fwFlowProps[i]) );
            logger.info ("");

            String myDateString = DateFormat.getDateTimeInstance().format(new Date());
            logger.info ("done with Frank-Wolfe assignment: " + myDateString);

        
	    }
        catch ( RpcException e ) {
            logger.error ( "RpcException caught.", e );
            System.exit(1);
        }
        catch ( IOException e ) {
            logger.error ( "IOException caught.", e );
            System.exit(1);
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }
        
    }


	
    public double[] getFWFlowProps () {
        // Determine the proportions of O/D flow assigned during each FW iteration.

        double[] Proportions = new double[maxFwIters];

        for (int i=0; i < maxFwIters; i++) {
            Proportions[i] = lambdas[i];
            for (int k=i+1; k < maxFwIters; k++)
                Proportions[i] *= (1.0-lambdas[k]);
        }

        return(Proportions);
    }




	private void createDiskObjectArray() {
	    
	    // create a DiskObjectArray in which shortest path trees will be stored as the FW
	    // trip assignment model runs so that a select link analaysis procedure can be
	    // implemented post-assignment.
	    
        try {
	    
    	    // get the location of the file for storing paths
    		String diskObjectArrayFile = (String)appPropertyMap.get("PathsDiskObjectArray.file");
    
    		// create the object
    		if ( diskObjectArrayFile != null ) {
    
    			try{
    			    
    			    int numElements = maxFwIters*lastOriginTaz*numAutoClasses;
    			    int maxElementSize = SIZEOF_INT*(networkHandlerGetNodeCountRpcCall()+1) + 100;
    			    
    			    logger.info ("dimensions for paths DiskObjectArray: numElements=" + numElements + ", maxElementSize=" + maxElementSize );
    		
    			    fwPathsDoa = new DiskObjectArray( diskObjectArrayFile, numElements, maxElementSize );
    		
    			} catch(IOException e) {
    			    
    				logger.fatal("could not open disk object array file for storing shortest path trees.", e);
    				System.exit(1);
    				
    			}
    
    		}

	    }
        catch ( RpcException e ) {
            logger.error ( "RpcException caught.", e );
            System.exit(1);
        }
        catch ( IOException e ) {
            logger.error ( "IOException caught.", e );
            System.exit(1);
        }
        catch ( Exception e ) {
            logger.error ( "Exception caught.", e );
            System.exit(1);
        }

    }
	


    
    

    private int networkHandlerGetNumUserClassesRpcCall() throws Exception {
        // g.getNumUserClasses()
        return (Integer)networkHandlerClient.execute("networkHandler.getNumUserClasses", new Vector() );
    }

    private int networkHandlerGetNodeCountRpcCall() throws Exception {
        // g.getNodeCount()
        return (Integer)networkHandlerClient.execute("networkHandler.getNodeCount", new Vector() );
    }
    
    private int networkHandlerGetLinkCountRpcCall() throws Exception {
        // g.getLinkCount()
        return (Integer)networkHandlerClient.execute("networkHandler.getLinkCount", new Vector() );
    }

    private int networkHandlerGetNumCentroidsRpcCall() throws Exception {
        // g.getNumCentroids()
        return (Integer)networkHandlerClient.execute("networkHandler.getNumCentroids", new Vector());
    }

    private String networkHandlerGetTimePeriodRpcCall() throws Exception {
        // g.getTimePeriod()
        return (String)networkHandlerClient.execute("networkHandler.getTimePeriod", new Vector());
    }

    private double networkHandlerFwBisectRpcCall( int iter, double[][] aonFlow, double[][] flow) throws Exception {
        // g.getFwOfValue(boolean[] validLinks, double[][] flow)
        Vector params = new Vector();
        params.add( iter );
        params.add( aonFlow );
        params.add( flow );
        return (Double)networkHandlerClient.execute("networkHandler.fwBisect", params );
    }

    private double networkHandlerGetFwOfValueRpcCall( double[][] flow ) throws Exception {
        // g.getFwOfValue( double[][] flow )
        Vector params = new Vector();
        params.add( flow );
        return (Double)networkHandlerClient.execute("networkHandler.getFwOfValue", params );
    }

    private double networkHandlerGetFwGapValueRpcCall( double[][] aonFlow, double[][] flow ) throws Exception {
        // g.getFwGapValue( double[][] flow )
        Vector params = new Vector();
        params.add( aonFlow );
        params.add( flow );
        return (Double)networkHandlerClient.execute("networkHandler.getFwGapValue", params );
    }

    private void networkHandlerLinkSummaryReportRpcCall( double[][] flow ) throws Exception {
        // g.getFwGapValue( double[][] flow )
        Vector params = new Vector();
        params.add( flow );
        networkHandlerClient.execute("networkHandler.linkSummaryReport", params );
    }

    private boolean[][] networkHandlerGetValidLinksForAllClassesRpcCall() throws Exception {
        // g.getValidLinksForAllClasses()
        return (boolean[][])networkHandlerClient.execute("networkHandler.getValidLinksForAllClasses", new Vector() );
    }

    private void networkHandlerLogLinkTimeFreqsRpcCall() throws Exception {
        // g.logLinkTimeFreqs( boolean[] validLinks )
        networkHandlerClient.execute("networkHandler.logLinkTimeFreqs", new Vector() );
    }

    private void networkHandlerCreateSelectLinkAnalysisDiskObjectRpcCall( double[] fwFlowProps ) throws Exception {
        // g.createSelectLinkAnalysisDiskObject( double[] fwFlowProps )
        Vector params = new Vector();
        params.add( fwFlowProps );
        networkHandlerClient.execute("networkHandler.createSelectLinkAnalysisDiskObject", params);
    }

    private double[] networkHandlerSetLinkGeneralizedCostRpcCall() throws Exception {
        // g.setLinkGeneralizedCost()
        return (double[])networkHandlerClient.execute("networkHandler.setLinkGeneralizedCost", new Vector() );
    }

    
    
    
    
    
    private boolean demandHandlerSetupRpcCall() throws Exception {
        Vector params = new Vector();
        params.add( appPropertyMap );
        params.add( globalPropertyMap );
        params.add( timePeriod );
        return (Boolean)demandHandlerClient.execute("demandHandler.setup", params );
    }
    
    

    
    
    private void aonFlowHandlerSetupRpcCall() throws Exception {
        
        Vector params = new Vector();
        params.addElement( appPropertyMap );
        params.addElement( globalPropertyMap );
        aonFlowHandlerClient.execute("aonFlowHandler.setup", params );
    }
        
    private double[][] aonFlowHandlerGetMulticlassAonLinkFlowsRpcCall() throws Exception {
        return (double[][])aonFlowHandlerClient.execute("aonFlowHandler.getMulticlassAonLinkFlows", new Vector() );
    }

    
    
    
    private void shortestPathHandlerSetupRpcCall() throws Exception {
        Vector params = new Vector();
        params.addElement( appPropertyMap );
        params.addElement( globalPropertyMap );
        
        boolean[][] validLinksForClasses = networkHandlerGetValidLinksForAllClassesRpcCall();
        params.addElement( validLinksForClasses );

        shortestPathHandlerClient.execute("shortestPathTreeHandler.setup", params );
    }
    
    private void shortestPathHandlerSetLinkCostRpcCall( double[] linkCost ) throws Exception {
        Vector params = new Vector();
        params.addElement( linkCost );
        shortestPathHandlerClient.execute("shortestPathTreeHandler.setLinkCostArray", params );
    }
    

    
}

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
import com.pb.common.util.Justify;
import com.pb.common.util.Convert;
import com.pb.tlumip.ts.assign.Constants;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;


public class FW {

    Logger logger = Logger.getLogger("com.pb.tlumip.ts.daf3.FW");

    static Constants c = new Constants();

	static final int SIZEOF_INT = 4;
	static final int MAX_LINK_TYPE = 1000;

	HashMap propertyMap;
	
    Justify myFormat = new Justify();

    double [] lambdas;
    double [] fwFlowProps;
    
	double[] volau;
	int[] linkType;
    
    int numAutoClasses;
    int numLinks;
    int startOriginTaz;
    int lastOriginTaz;;
	int maxFwIters;
	double fwGap;

    String timePeriod;

    DiskObjectArray fwPathsDoa = null;	

    RpcClient networkHandlerClient;    
    

   
    public FW () {
    }


    public void initialize ( HashMap tsPropertyMap, RpcClient networkHandlerClient ) {

		this.propertyMap = tsPropertyMap;
        this.networkHandlerClient = networkHandlerClient;

        
        
        
        maxFwIters = Integer.parseInt ( (String)propertyMap.get( "NUM_FW_ITERATIONS" ) );
        fwGap = Double.parseDouble ( (String)propertyMap.get( "FW_RELATIVE_GAP" ) );

        lambdas = new double[maxFwIters];
        fwFlowProps = new double[maxFwIters];

        try {
            startOriginTaz = 0;
            numLinks = networkHandlerGetLinkCountRpcCall();
            lastOriginTaz = networkHandlerGetNumCentroidsRpcCall();
            numAutoClasses = networkHandlerGetNumUserClassesRpcCall();
    		timePeriod = networkHandlerGetTimePeriodRpcCall();

            // initialize assignment arrays prior to iterating
            linkType = networkHandlerGetLinkTypeRpcCall();
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

        
        
		volau = new double[numLinks];
		
		// create a DiskObjectArray for storing shortest path trees
		createDiskObjectArray();
    
    }


    /**
     * Frank-Wolfe assignment procedure.
     */
	public void iterate ( double[][][] tripTable ) {

		
        try {

            double lub = 0.0;
    	    double gap = 0.0;
    	    double glb = 0.0;
    
    
    		// determine which links are valid parts of paths for this skim
    		boolean[] validLinksForClass = null;
    		boolean[] validLinks = new boolean[numLinks];
    	    Arrays.fill (validLinks, false);
    		for (int m=0; m < numAutoClasses; m++) {
    			validLinksForClass = networkHandlerGetValidLinksForClassRpcCall( m );
    			for (int k=0; k < numLinks; k++)
    				if (validLinksForClass[k])
    					validLinks[k] = true;
    		}
    
    			
    		
    		double[][] flow = new double[numAutoClasses][numLinks];
    
    		for (int m=0; m < numAutoClasses; m++)
    		    Arrays.fill (flow[m], 0.0);
    
            
            // loop thru FW iterations
            for (int iter=0; iter < maxFwIters; iter++) {
            	
                if(logger.isDebugEnabled()) {
                    logger.debug("Iteration = " + iter);
                }
                lambdas[iter] = 1.0;
    
                
            	double[][] aonFlow = getMulticlassAonLinkFlows ( tripTable, iter );
    			
    
                // use bisect to do Frank-Wolfe averaging -- returns true if exact solution
                if (iter > 0) {
                    if ( bisect ( iter, validLinks, aonFlow, flow ) ) {
                        logger.error ("Exact FW optimal solution found.  Unlikely, better check into this!");
                        iter = maxFwIters;
                    }
                }
                else {
    				glb = Double.NEGATIVE_INFINITY;
    				lub = 0.0;
    				gap = 0.0;
                }
    
    
                // print assignment iterations report
                lub = ofValue(validLinks, flow);
    			gap = Math.abs(ofGap( validLinks, aonFlow, flow ));
                if ( ( lub - gap ) > glb )
                    glb = lub - gap;
    
                logger.info ("Iteration " + myFormat.right(iter, 3)
                                    + "    Lambda= " + myFormat.right(lambdas[iter], 8, 4)
                                    + "    LUB= "    + myFormat.right(lub, 16, 4)
                                    + "    Gap= "    + myFormat.right(gap, 16, 4)
                                    + "    GLB= "    + myFormat.right(glb, 16, 4)
                                    + "    LUB-GLB= "    + myFormat.right(lub-glb, 16, 4)
                                    + "    RelGap= " + myFormat.right(100.0*(lub - glb)/glb, 7, 4) + "%");
    
    
                // update link flows and times
    			for (int k=0; k < volau.length; k++) {
    				volau[k] = 0;
    				for (int m=0; m < numAutoClasses; m++) {
    					flow[m][k] = flow[m][k] + lambdas[iter]*(aonFlow[m][k] - flow[m][k]);
    					volau[k] += flow[m][k];
    				}
    			}
                networkHandlerSetFlowsRpcCall(flow);
                networkHandlerSetVolauRpcCall(volau);
                networkHandlerApplyVdfsRpcCall(validLinks);
    
                networkHandlerLogLinkTimeFreqsRpcCall (validLinks);
    			
    			
    			if ( Math.abs( (lub - glb)/glb ) < fwGap )
    				break;
    			
            } // end of FW iter loop
    
    
    
    
            linkSummaryReport( flow );
    
            fwFlowProps = getFWFlowProps();
            
            
            // Write a DiskObject out that contains the Network Object, the Frank Wolfe flow
            // proportions array, and the Frank Wolfe shortest path trees Disk Object Array.
            // A separate SelectLinkAnalysis class with utilize this information to perform
            // on-demand select link analysis after the trip assignment process has finished.
            networkHandlerCreateSelectLinkAnalysisDiskObjectRpcCall( fwFlowProps );
            
            
    
            logger.info ("");
            logger.info (myFormat.right("iter", 5) + myFormat.right("lambdas", 12) + myFormat.right("Flow Props", 12));
            for (int i=0; i < maxFwIters; i++)
                logger.info (myFormat.right(i, 5) + myFormat.right(lambdas[i], 12, 6) + myFormat.right(100.0*fwFlowProps[i], 12, 4) + "%");
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


	
	double[][] getMulticlassAonLinkFlows ( double[][][] tripTable, int iter ) {

        int storedPathIndex = 0;
        
        boolean[] validLinksForClass = null;

        double[][] aonFlow = new double[numAutoClasses][numLinks];
        
        for (int m=0; m < numAutoClasses; m++)
            Arrays.fill (aonFlow[m], 0.0);

        
        try {
            
    		// build shortest path tree object and set cost and valid link attributes for this user class.
    		ShortestPathTreeH sp = new ShortestPathTreeH( networkHandlerClient );
    
    		// set the highway network attribute on which to skim the network
    		double[] linkCost = networkHandlerSetLinkGeneralizedCostRpcCall();
    		double[] aon;
    	
    		sp.setLinkCost( linkCost );
    
    		for (int origin=startOriginTaz; origin < lastOriginTaz; origin++) {
    		    
    			for (int m=0; m < numAutoClasses; m++) {
    
    				double tripTableRowSum = 0.0;
    				for (int j=0; j < tripTable[m][origin].length; j++)
    					tripTableRowSum += tripTable[m][origin][j];
    
    				validLinksForClass = networkHandlerGetValidLinksForClassRpcCall( m );
    				sp.setValidLinks( validLinksForClass );
    
    
    				if (origin % 500 == 0)
    					logger.info ("assigning origin zone index " + origin + ", user class index " + m);
    
    				if (tripTableRowSum > 0.0) {
    					
    					sp.buildTree ( origin );
    					aon = sp.loadTree ( tripTable[m][origin], m );
    
    				    for (int k=0; k < aon.length; k++)
    				        aonFlow[m][k] += aon[k];
    				    
    				}
    			    
    				
    			    if ( fwPathsDoa != null ) {
    
    			        // calculate the index used for storing shortest path tree in DiskObjectArray
    				    storedPathIndex = iter*lastOriginTaz*numAutoClasses + origin*numAutoClasses + m;
    				    
    				    // store the shortest path tree for this iteration, origin zone, and user class
    				    try {
    				        int[] tempArray = sp.getPredecessorLink();
    				        fwPathsDoa.add( storedPathIndex, tempArray );
    				    } catch (Exception e) {
    				        logger.fatal ("could not store index=" + storedPathIndex + ", for iter=" + iter + ", for origin=" + origin + ", and class=" + m, e);
    				        System.exit(1);
    				    }
    			        
    			    }
    			    
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
        
        return aonFlow;
        
    }


    //Bisection routine to calculate opitmal lambdas during each frank-wolfe iteration.
    public boolean bisect ( int iter, boolean[] validLinks, double[][] aonFlow, double[][] flow ) {
        
        String myDateString = DateFormat.getDateTimeInstance().format(new Date());
        logger.info ("starting FW.bisect()" + myDateString);
 
        double x=0.0, xleft=0.0, xright=1.0, gap=0.0;

        int numBisectIterations = (int)(Math.log(1.0e-07)/Math.log(0.5) + 1.5);
        
        gap = ofGap( validLinks, aonFlow, flow );
        
        if (Math.abs(gap) <= 1.0e-07) {
            lambdas[iter] = 0.5;
            return(true);
        }
        else {
            if (gap <= 0)
                xleft = x;
            else
                xright = x;
            x = (xleft + xright)/2.0;
			if(logger.isDebugEnabled()) {
                logger.debug ("iter=" + iter + ", gap=" + gap + ", xleft=" + xleft + ", xright=" + xright + ", x=" + x);
            }

            for (int n=0; n < numBisectIterations; n++) {
                gap = bisectGap(x, validLinks, aonFlow, flow );
                if (gap <= 0)
                    xleft = x;
                else
                    xright = x;
                x = (xleft + xright)/2.0;
				if(logger.isDebugEnabled()) {
                    logger.debug ("iter=" + iter + ", n=" + n + ", gap=" + gap + ", xleft=" + xleft + ", xright=" + xright + ", x=" + x);
                }
            }
            lambdas[iter] = x;
            return(false);
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


    public double ofValue ( boolean[] validLinks, double[][] flow )  {

        double returnValue = -1;
        
        try {
            
            String myDateString = DateFormat.getDateTimeInstance().format(new Date());
            logger.info ("starting FW.ofValue()" + myDateString);
     
    		// sum total flow over all user classes for each link 
            for (int k=0; k < volau.length; k++) {
                volau[k] = 0.0;
                for (int m=0; m < numAutoClasses; m++)
                    volau[k] += flow[m][k];
            }
            
            networkHandlerSetVolauRpcCall(volau);
            networkHandlerSetVolCapRatiosRpcCall(volau);
            networkHandlerApplyVdfIntegralsRpcCall(validLinks);
    
            returnValue = networkHandlerGetSumOfVdfIntegralsRpcCall(validLinks);
        
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

        return returnValue;
        
    }



    public double ofGap ( boolean[] validLinks, double[][] aonFlow, double[][] flow ) {

        double returnValue = -1;
        
        try {
            
            String myDateString = DateFormat.getDateTimeInstance().format(new Date());
            logger.info ("starting FW.ofGap()" + myDateString);
     
    		double[] totAonFlow = new double[numLinks];
            
    		// sum total flow over all user classes for each link 
    		for (int k=0; k < volau.length; k++) {
    			volau[k] = 0.0;
    			totAonFlow[k] = 0.0;
    			for (int m=0; m < numAutoClasses; m++) {
    				volau[k] += flow[m][k];
    				totAonFlow[k] += aonFlow[m][k];
    			}
    		}
            
            networkHandlerSetVolauRpcCall(volau);
            networkHandlerApplyVdfsRpcCall(validLinks);
    		double[] cTime = networkHandlerGetCongestedTimeRpcCall();
    		
    		double gap = 0.0;
    		for (int k=0; k < volau.length; k++) {
                if ( validLinks[k] ) {
                    
                    if ( ! isValidDoubleValue(cTime[k]) )
                        logger.error( "invalid value for loaded link travel time on link " + k );
                    else if ( ! isValidDoubleValue(totAonFlow[k]) )
                        logger.error( "invalid value for all-or-nothing link flow on link " + k );
                    else if ( ! isValidDoubleValue(volau[k]) )
                        logger.error( "invalid value for loaded link flow on link " + k );
                
                	gap += cTime[k]*(totAonFlow[k] - volau[k]);
                }
    		}
    
            returnValue = gap;
        
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

        return returnValue;
        
    }


    public double bisectGap (double x, boolean[] validLinks, double[][] aonFlow, double[][] flow ) {

        double returnValue = -1;
        
        try {
            
            String myDateString = DateFormat.getDateTimeInstance().format(new Date());
            logger.info ("starting FW.bisectGap()" + myDateString);
     
    		double[] totAonFlow = new double[numLinks];
    		double[] totalFlow = new double[numLinks];
            
    		// sum total flow over all user classes for each link 
    		for (int k=0; k < totalFlow.length; k++) {
    			volau[k] = 0.0;
    			totAonFlow[k] = 0.0;
    			totalFlow[k] = 0.0;
    			for (int m=0; m < numAutoClasses; m++) {
    				totalFlow[k] += flow[m][k];
    				totAonFlow[k] += aonFlow[m][k];
    			}
    			volau[k] = totalFlow[k] + x*(totAonFlow[k] - totalFlow[k]);
    		}
            
            networkHandlerSetVolauRpcCall(volau);
            networkHandlerApplyVdfsRpcCall(validLinks);
            double[] cTime = networkHandlerGetCongestedTimeRpcCall();
    
            double gap = 0.0;
    		for (int k=0; k < cTime.length; k++)
                if ( validLinks[k] )
                	gap += cTime[k]*(totAonFlow[k] - volau[k]);
    
    
            returnValue = gap;
        
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

        return returnValue;
        
    }


    public void linkSummaryReport ( double[][] flow ) {
        double totalVol;
        double[][] volumeSum = new double[numAutoClasses][MAX_LINK_TYPE];

        for (int k=0; k < numLinks; k++)
            for (int m=0; m < numAutoClasses; m++)
                volumeSum[m][linkType[k]] += flow[m][k];

        logger.info("");
        logger.info("");
        logger.info("");
        logger.info("Link Type");
        for (int m=0; m < numAutoClasses; m++)
            logger.info(myFormat.right("Class " + Integer.toString(m) + " Volume", 24));
        logger.info("");
        for (int i=0; i < MAX_LINK_TYPE; i++) {
            totalVol = 0.0;
            for (int m=0; m < numAutoClasses; m++)
                totalVol += volumeSum[m][i];
            if (totalVol > 0.0) {
                logger.info (myFormat.left(i, 9));
                for (int m=0; m < numAutoClasses; m++)
                    logger.info (myFormat.right(volumeSum[m][i], 24, 4));
                logger.info("");
            }
        }
    }


	private void createDiskObjectArray() {
	    
	    // create a DiskObjectArray in which shortest path trees will be stored as the FW
	    // trip assignment model runs so that a select link analaysis procedure can be
	    // implemented post-assignment.
	    
        try {
	    
    	    // get the location of the file for storing paths
    		String diskObjectArrayFile = (String)propertyMap.get("PathsDiskObjectArray.file");
    
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
	
	private boolean isValidDoubleValue( double value ) {
		return ( value >= -Double.MAX_VALUE && value <= Double.MAX_VALUE );
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

    private int[] networkHandlerGetLinkTypeRpcCall() throws Exception {
        // g.getTimePeriod()
        return (int[])Convert.toObject( (byte[])networkHandlerClient.execute("networkHandler.getLinkType", new Vector() ) );
    }

    private boolean[] networkHandlerGetValidLinksForClassRpcCall( int userClass ) throws Exception {
        // g.getValidLinksForClass( int i )
        Vector params = new Vector();
        params.add( userClass );
        return (boolean[])Convert.toObject( (byte[])networkHandlerClient.execute("networkHandler.getValidLinksForClassInt", params ) );
    }

    private double[] networkHandlerGetCongestedTimeRpcCall() throws Exception {
        // g.getCongestedTime()
        return (double[])Convert.toObject( (byte[])networkHandlerClient.execute("networkHandler.getCongestedTime", new Vector() ) );
    }
    
    private void networkHandlerSetFlowsRpcCall( double[][] flows ) throws Exception {
        // g.setFlows( double[][] flows )
        Vector params = new Vector();
        params.add( flows );
        networkHandlerClient.execute("networkHandler.setFlows", params);
    }

    private void networkHandlerSetVolCapRatiosRpcCall( double[] volau ) throws Exception {
        // g.setVolCapRatios( double[] volau )
        Vector params = new Vector();
        params.add( volau );
        networkHandlerClient.execute("networkHandler.setVolCapRatios", params);
    }

    private void networkHandlerSetVolauRpcCall( double[] volau ) throws Exception {
        // g.setVolau( double[] volau )
        Vector params = new Vector();
        params.add( volau );
        networkHandlerClient.execute("networkHandler.setVolau", params);
    }

    private double[] networkHandlerSetLinkGeneralizedCostRpcCall() throws Exception {
        // g.setLinkGeneralizedCost()
        return (double[])Convert.toObject( (byte[])networkHandlerClient.execute("networkHandler.setLinkGeneralizedCost", new Vector() ) );
    }

    private void networkHandlerApplyVdfsRpcCall( boolean[] validLinks ) throws Exception {
        // g.applyVdfs( boolean[] validLinks )
        Vector params = new Vector();
        params.add( validLinks );
        networkHandlerClient.execute("networkHandler.applyVdfs", params);
    }

    private void networkHandlerApplyVdfIntegralsRpcCall( boolean[] validLinks ) throws Exception {
        // g.applyVdfIntegrals( boolean[] validLinks )
        Vector params = new Vector();
        params.add( validLinks );
        networkHandlerClient.execute("networkHandler.applyVdfIntegrals", params);
    }

    private double networkHandlerGetSumOfVdfIntegralsRpcCall( boolean[] validLinks ) throws Exception {
        // g.getSumOfVdfIntegrals( boolean[] validLinks )
        Vector params = new Vector();
        params.add( validLinks );
        return (Double)networkHandlerClient.execute("networkHandler.getSumOfVdfIntegrals", params);
    }
        
    private void networkHandlerLogLinkTimeFreqsRpcCall( boolean[] validLinks ) throws Exception {
        // g.logLinkTimeFreqs( boolean[] validLinks )
        Vector params = new Vector();
        params.add( validLinks );
        networkHandlerClient.execute("networkHandler.logLinkTimeFreqs", params);
    }

    private void networkHandlerCreateSelectLinkAnalysisDiskObjectRpcCall( double[] fwFlowProps ) throws Exception {
        // g.createSelectLinkAnalysisDiskObject( double[] fwFlowProps )
        Vector params = new Vector();
        params.add( fwFlowProps );
        networkHandlerClient.execute("networkHandler.createSelectLinkAnalysisDiskObject", params);
    }
        
}

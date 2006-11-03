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
package com.pb.tlumip.ts.assign;

import com.pb.common.datafile.DataWriter;
import com.pb.common.datafile.DiskObjectArray;
import com.pb.tlumip.ts.NetworkHandlerIF;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import org.apache.log4j.Logger;


public class FW {

    Logger logger = Logger.getLogger("com.pb.tlumip.ts.assign.FW");

    static Constants c = new Constants();

	static final int SIZEOF_INT = 4;
	static final int MAX_LINK_TYPE = 1000;

	HashMap propertyMap;
	
    NetworkHandlerIF nh;

    double [] lambdas;
    double [] fwFlowProps;
    
	double[] volau;
	int[] linkType;
    
    int numLinks;
    int numAutoClasses;
    String timePeriod;

 
    int startOriginTaz, lastOriginTaz;
	int maxFwIters;
	double fwGap;

    boolean[][] validLinksForClasses = null;
    
	DiskObjectArray fwPathsDoa = null;	

   
    public FW () {
    }


    public void initialize ( HashMap tsPropertyMap, NetworkHandlerIF nh ) {

		this.propertyMap = tsPropertyMap;
		this.nh = nh;
        
        maxFwIters = Integer.parseInt ( (String)propertyMap.get( "NUM_FW_ITERATIONS" ) );
        fwGap = Double.parseDouble ( (String)propertyMap.get( "FW_RELATIVE_GAP" ) );
        
        numLinks = nh.getLinkCount();
        
        numAutoClasses = nh.getUserClasses().length;
		timePeriod = nh.getTimePeriod();

		lambdas = new double[maxFwIters];
		fwFlowProps = new double[maxFwIters];

		startOriginTaz = 0;
        lastOriginTaz = nh.getNumCentroids();

    
		// initialize assignment arrays prior to iterating
		linkType = nh.getLinkType();

		
		volau = new double[numLinks];
	
        // get valid links arrays for each user class for use in assignment algorithm
        validLinksForClasses = new boolean[numAutoClasses][];
        for (int m=0; m < numAutoClasses; m++) {
            validLinksForClasses[m] = nh.getValidLinksForClass(m);
        }
        
		// create a DiskObjectArray for storing shortest path trees
		createDiskObjectArray();
    
    }


    /**
     * Frank-Wolfe assignment procedure.
     */
	public void iterate ( double[][][] tripTable ) {

		
		double lub = 0.0;
	    double gap = 0.0;
	    double glb = 0.0;


		// set validLinks true for a link if true for any of the classes of that link.
		boolean[] validLinks = new boolean[numLinks];
	    Arrays.fill (validLinks, false);
        for (int k=0; k < numLinks; k++) {
            for (int m=0; m < numAutoClasses; m++) {
				if (validLinksForClasses[m][k]) {
					validLinks[k] = true;
                    break;
                }
            }
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

            logger.info ("Iteration " + String.format("%3d", iter)
                                + "    Lambda= " + String.format("%8.4f", lambdas[iter])
                                + "    LUB= "    + String.format("%16.4f", lub)
                                + "    Gap= "    + String.format("%16.4f", gap)
                                + "    GLB= "    + String.format("%16.4f", glb)
                                + "    LUB-GLB= "    + String.format("%16.4f", lub-glb)
                                + "    RelGap= " + String.format("%7.4f%%", 100.0*(lub - glb)/glb, 7, 4) );


            // update link flows and times
			for (int k=0; k < volau.length; k++) {
				volau[k] = 0;
				for (int m=0; m < numAutoClasses; m++) {
					flow[m][k] = flow[m][k] + lambdas[iter]*(aonFlow[m][k] - flow[m][k]);
					volau[k] += flow[m][k];
				}
			}
			nh.setFlows(flow);
			nh.setVolau(volau);
            nh.applyVdfs();

            
			nh.logLinkTimeFreqs ();
			
			
			if ( Math.abs( (lub - glb)/glb ) < fwGap )
				break;
			
        } // end of FW iter loop




        linkSummaryReport( flow );

        fwFlowProps = getFWFlowProps();
        
        
        // Write a DiskObject out that contains the Network Object, the Frank Wolfe flow
        // proportions array, and the Frank Wolfe shortest path trees Disk Object Array.
        // A separate SelectLinkAnalysis class with utilize this information to perform
        // on-demand select link analysis after the trip assignment process has finished.
        createSelectLinkAnalysisDiskObject();
        
        

        logger.info ("");
        logger.info ( String.format( "%5s %12s %12s", "iter", "lambdas", "Flow Props" ) );
        for (int i=0; i < maxFwIters; i++)
            logger.info ( String.format("%6d %12.6f %12.4f%%", i, lambdas[i], 100.0*fwFlowProps[i]) );
        logger.info ("");

        String myDateString = DateFormat.getDateTimeInstance().format(new Date());
        logger.info ("done with Frank-Wolfe assignment: " + myDateString);

    }


	
	double[][] getMulticlassAonLinkFlows ( double[][][] tripTable, int iter ) {

	    int storedPathIndex = 0;
	    
		boolean[] validLinksForClass = null;

		// build shortest path tree object and set cost and valid link attributes for this user class.
		ShortestPathTreeH sp = new ShortestPathTreeH( nh );

		// set the highway network attribute on which to skim the network
		double[] linkCost = nh.setLinkGeneralizedCost();

		double[][] aonFlow = new double[numAutoClasses][numLinks];
		double[] aon;
	
		
		for (int m=0; m < numAutoClasses; m++)
		    Arrays.fill (aonFlow[m], 0.0);


		sp.setLinkCost( linkCost );

		for (int origin=startOriginTaz; origin < lastOriginTaz; origin++) {
		    
			for (int m=0; m < numAutoClasses; m++) {

				double tripTableRowSum = 0.0;
				for (int j=0; j < tripTable[m][origin].length; j++)
					tripTableRowSum += tripTable[m][origin][j];


				sp.setValidLinks( validLinksForClasses[m] );


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
				        logger.fatal ("could not store index=" + storedPathIndex + ", for iter=" + iter + ", for origin=" + origin + ", and class=" + m);
				        e.printStackTrace();
				        System.exit(1);
				    }
			        
			    }
			    
			}
			
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

        String myDateString = DateFormat.getDateTimeInstance().format(new Date());
        logger.info ("starting FW.ofValue()" + myDateString);
 
		// sum total flow over all user classes for each link 
        double total = 0.0;
        for (int k=0; k < volau.length; k++) {
            volau[k] = 0.0;
            for (int m=0; m < numAutoClasses; m++) {
                volau[k] += flow[m][k];
                total += volau[k]; 
            }
        }
        
		nh.setVolau(volau);
		nh.setVolCapRatios();
        nh.applyVdfIntegrals();

        return( nh.getSumOfVdfIntegrals() );
    }



    public double ofGap ( boolean[] validLinks, double[][] aonFlow, double[][] flow ) {

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
        
		nh.setVolau(volau);
		nh.applyVdfs();
		double[] cTime = nh.getCongestedTime();
		
		double gap = 0.0;
		for (int k=0; k < volau.length; k++) {
            if ( validLinks[k] ) {
            	gap += cTime[k]*(totAonFlow[k] - volau[k]);
            }
		}

        return(gap);
    }


    public double bisectGap (double x, boolean[] validLinks, double[][] aonFlow, double[][] flow ) {

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
        
		nh.setVolau(volau);
		nh.applyVdfs();
		double[] cTime = nh.getCongestedTime();

        double gap = 0.0;
		for (int k=0; k < cTime.length; k++)
            if ( validLinks[k] )
            	gap += cTime[k]*(totAonFlow[k] - volau[k]);


        return(gap);
    }


    public void linkSummaryReport ( double[][] flow ) {
        
        double totalVol;
        double[][] volumeSum = new double[numAutoClasses][Constants.MAX_LINK_TYPE];
        char[] autoClasses = nh.getUserClasses();

        for (int k=0; k < linkType.length; k++)
            for (int m=0; m < numAutoClasses; m++)
                volumeSum[m][linkType[k]] += flow[m][k];

        logger.info("");
        logger.info("");
        logger.info("");
        String logRecord = String.format("%-8s", "LinkType");
        for (int m=0; m < numAutoClasses; m++)
            logRecord.concat ( String.format("%12s", ("class " + autoClasses[m])) );
        logger.info( logRecord );
        for (int i=0; i < Constants.MAX_LINK_TYPE; i++) {
            totalVol = 0.0;
            for (int m=0; m < numAutoClasses; m++)
                totalVol += volumeSum[m][i];
            if (totalVol > 0.0) {
                logRecord = String.format("%-8d", i);
                for (int m=0; m < numAutoClasses; m++)
                    logRecord.concat ( String.format("%12.2f", volumeSum[m][i]) );
                logger.info( logRecord );
            }
        }
    }


	private void createDiskObjectArray() {
	    
	    // create a DiskObjectArray in which shortest path trees will be stored as the FW
	    // trip assignment model runs so that a select link analaysis procedure can be
	    // implemented post-assignment.
	    
	    
	    // get the location of the file for storing paths
		String diskObjectArrayFile = (String)propertyMap.get("PathsDiskObjectArray.file");

		// create the object
		if ( diskObjectArrayFile != null ) {

			try{
			    
			    int numElements = maxFwIters*lastOriginTaz*numAutoClasses;
			    int maxElementSize = SIZEOF_INT*(nh.getNodeCount()+1) + 100;
			    
			    logger.info ("dimensions for paths DiskObjectArray: numElements=" + numElements + ", maxElementSize=" + maxElementSize );
		
			    fwPathsDoa = new DiskObjectArray( diskObjectArrayFile, numElements, maxElementSize );
		
			} catch(IOException e) {
			    
				logger.fatal("could not open disk object array file for storing shortest path trees.");
				e.printStackTrace();
				System.exit(1);
				
			}

		}
	}


	private void createSelectLinkAnalysisDiskObject () {
	
		// get the locations of the files for storing the network and assignment proportions
		String networkDiskObjectFile = (String)propertyMap.get("NetworkDiskObject.file");
		String proportionsDiskObjectFile = (String)propertyMap.get("ProportionsDiskObject.file");

			
		// write the network and saved proportions to DiskObject files for subsequent select link analysis
		if ( networkDiskObjectFile != null )
		    DataWriter.writeDiskObject ( nh, networkDiskObjectFile, "highwayNetwork_" + timePeriod );
		
		if ( proportionsDiskObjectFile != null )
		    DataWriter.writeDiskObject ( fwFlowProps, proportionsDiskObjectFile, "fwProportions_" + timePeriod );
	
	}

	
	
//	private boolean isValidNonPositiveDoubleValue( double value ) {
//		return ( value >= -Double.MAX_VALUE && value <= 0.0 );
//	}
//
//	private boolean isValidNonNegativeDoubleValue( double value ) {
//		return ( value >= 0.0 && value <= Double.MAX_VALUE );
//	}
//
//	private boolean isValidDoubleValue( double value ) {
//		return ( value >= -Double.MAX_VALUE && value <= Double.MAX_VALUE );
//	}

}

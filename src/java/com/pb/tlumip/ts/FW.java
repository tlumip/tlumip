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
package com.pb.tlumip.ts;

import com.pb.common.datafile.DataWriter;
import com.pb.common.util.ResourceUtil;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;


public class FW {

    Logger logger = Logger.getLogger(FW.class);

    char[] highwayModeCharacters;
    
    ResourceBundle componentRb;
    ResourceBundle globalRb;
    
    HashMap appPropertyMap;
    HashMap globalPropertyMap;
	
    NetworkHandlerIF nh;
    
    double [] lambdas;
    
    int numAutoClasses;
    int numLinks;
	int maxFwIters;
    
    boolean[][] validLinksForClasses;
    
    double fwGap;

    String timePeriod;

    
   
    public FW () {
    }


    public void initialize ( ResourceBundle componentRb, ResourceBundle globalRb, NetworkHandlerIF nh, char[] highwayModeCharacters ) {

        this.componentRb = componentRb;
        this.globalRb = globalRb;
        this.nh = nh;
        this.highwayModeCharacters = highwayModeCharacters;
        
        this.appPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(componentRb);
        this.globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);
        
        maxFwIters = Integer.parseInt ( (String)appPropertyMap.get( "NUM_FW_ITERATIONS" ) );
        fwGap = Double.parseDouble ( (String)appPropertyMap.get( "FW_RELATIVE_GAP" ) );

        lambdas = new double[maxFwIters];

        // get network related variables needed in FW object
        numLinks = nh.getLinkCount();
        numAutoClasses = nh.getNumUserClasses();
        timePeriod = nh.getTimePeriod();
        validLinksForClasses = nh.getValidLinksForAllClasses();

    }

    

    /**
     * Frank-Wolfe assignment procedure.
     */
	public void iterate () {

        int iterationsCompleted = 0;
		
        try {
            
            double lub = 0.0;
    	    double gap = 0.0;
    	    double glb = 0.0;
    
    		double[][] flow = new double[numAutoClasses][numLinks];

            
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

            AonFlowHandlerIF ah = AonFlowHandler.getInstance( nh.getRpcConfigFileName() );
            ah.setup( componentRb, globalRb, nh, highwayModeCharacters );
            
            
            // loop thru FW iterations
            // loop thru FW iterations
            for (int iter=0; iter < maxFwIters; iter++) {
                
                if(logger.isDebugEnabled()) {
                    logger.debug("Iteration = " + iter);
                }
                lambdas[iter] = 1.0;

                long startTime = System.currentTimeMillis();
                double[][] aonFlow = ah.getMulticlassAonLinkFlows ();
                logger.info( ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds to build and loaded shortest path trees." );
                

                for (int i=0; i < aonFlow.length; i++) {
                    double tot = 0.0;
                    for (int k=0; k < aonFlow[i].length; k++)
                        tot += aonFlow[i][k];
                    logger.info( " flow[" + i + "]: " + tot);
                }
                    
                
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
                double[] totalLinkFlow = new double[numLinks];

                for (int k=0; k < totalLinkFlow.length; k++) {
                    totalLinkFlow[k] = 0;
                    for (int m=0; m < numAutoClasses; m++) {
                        flow[m][k] = flow[m][k] + lambdas[iter]*(aonFlow[m][k] - flow[m][k]);
                        totalLinkFlow[k] += flow[m][k];
                    }
                }
                nh.setFlows(flow);
                nh.setVolau(totalLinkFlow);
                nh.applyVdfs();

                
                //nh.logLinkTimeFreqs ();
                
                iterationsCompleted++;
                
                if ( Math.abs( (lub - glb)/glb ) < fwGap )
                    break;
                
            } // end of FW iter loop
    
    
    
    
            double[] fwFlowProps = getFWFlowProps();
            
            
            // Write a DiskObject out that contains the Network Object, the Frank Wolfe flow
            // proportions array, and the Frank Wolfe shortest path trees Disk Object Array.
            // A separate SelectLinkAnalysis class with utilize this information to perform
            // on-demand select link analysis after the trip assignment process has finished.
            createSelectLinkAnalysisDiskObject( fwFlowProps );
            
            
    
            logger.info ("");
            logger.info ( String.format( "%5s %12s %12s", "iter", "lambdas", "Flow Props" ) );
            for (int i=0; i < iterationsCompleted; i++)
                logger.info ( String.format("%6d %12.6f %12.4f%%", i, lambdas[i], 100.0*fwFlowProps[i]) );
            logger.info ("");

            String myDateString = DateFormat.getDateTimeInstance().format(new Date());
            logger.info ("done with Frank-Wolfe assignment: " + myDateString);

        
	    }
        catch ( Exception e ) {
            logger.error ( "Exception caught in FW.iterate().", e );
            System.exit(1);
        }
        
    }

    
    //Bisection routine to calculate opitmal lambdas during each frank-wolfe iteration.
    private boolean bisect ( int iter, boolean[] validLinks, double[][] aonFlow, double[][] flow ) {
        
        double x=0.0;
        double xleft=0.0;
        double xright=1.0;

        int numBisectIterations = (int)(Math.log(1.0e-07)/Math.log(0.5) + 1.5);
        double gap = ofGap( validLinks, aonFlow, flow );

        
        if ( Math.abs(gap) <= 1.0e-07 ) {
            
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

    

    private double ofValue ( boolean[] validLinks, double[][] flow )  {

        double total = 0.0;
        double[] totalLinkFlow = new double[numLinks];

        // sum total flow over all user classes for each link 
        for (int k=0; k < totalLinkFlow.length; k++) {
            totalLinkFlow[k] = 0.0;
            for (int m=0; m < numAutoClasses; m++) {
                totalLinkFlow[k] += flow[m][k];
                total += totalLinkFlow[k]; 
            }
        }
        
        nh.setVolau(totalLinkFlow);
        nh.setVolCapRatios();
        nh.applyVdfIntegrals();

        return( nh.getSumOfVdfIntegrals() );
    }



    private double ofGap ( boolean[] validLinks, double[][] aonFlow, double[][] flow ) {

        double[] totAonFlow = new double[numLinks];
        double[] totalLinkFlow = new double[numLinks];
        
        // sum total flow over all user classes for each link 
        for (int k=0; k < totalLinkFlow.length; k++) {
            totalLinkFlow[k] = 0.0;
            totAonFlow[k] = 0.0;
            for (int m=0; m < numAutoClasses; m++) {
                totalLinkFlow[k] += flow[m][k];
                totAonFlow[k] += aonFlow[m][k];
            }
        }
        
        nh.setVolau(totalLinkFlow);
        nh.applyVdfs();
        double[] cTime = nh.getCongestedTime();
        
        double gap = 0.0;
        for (int k=0; k < totalLinkFlow.length; k++) {
            if ( validLinks[k] ) {
                gap += cTime[k]*(totAonFlow[k] - totalLinkFlow[k]);
            }
        }

        return(gap);
    }


    private double bisectGap (double x, boolean[] validLinks, double[][] aonFlow, double[][] flow ) {

        double[] totAonFlow = new double[numLinks];
        double[] totalLinkFlow = new double[numLinks];
        
        // sum total flow over all user classes for each link 
        for (int k=0; k < totalLinkFlow.length; k++) {
            totAonFlow[k] = 0.0;
            totalLinkFlow[k] = 0.0;
            for (int m=0; m < numAutoClasses; m++) {
                totalLinkFlow[k] += flow[m][k];
                totAonFlow[k] += aonFlow[m][k];
            }
            totalLinkFlow[k] = totalLinkFlow[k] + x*(totAonFlow[k] - totalLinkFlow[k]);
        }
        
        nh.setVolau(totalLinkFlow);
        nh.applyVdfs();
        double[] cTime = nh.getCongestedTime();

        double gap = 0.0;
        for (int k=0; k < totalLinkFlow.length; k++)
            if ( validLinks[k] )
                gap += cTime[k]*(totAonFlow[k] - totalLinkFlow[k]);


        return(gap);
    }


	
    private double[] getFWFlowProps () {
        // Determine the proportions of O/D flow assigned during each FW iteration.

        double[] Proportions = new double[maxFwIters];

        for (int i=0; i < maxFwIters; i++) {
            Proportions[i] = lambdas[i];
            for (int k=i+1; k < maxFwIters; k++)
                Proportions[i] *= (1.0-lambdas[k]);
        }

        return(Proportions);
    }



    private void createSelectLinkAnalysisDiskObject ( double[] fwFlowProps ) {
        
        // get the locations of the files for storing the network and assignment proportions
        String networkDiskObjectFile = (String)appPropertyMap.get("NetworkDiskObject.file");
        String proportionsDiskObjectFile = (String)appPropertyMap.get("ProportionsDiskObject.file");

            
        // write the network and saved proportions to DiskObject files for subsequent select link analysis
        if ( networkDiskObjectFile != null )
            DataWriter.writeDiskObject ( nh, networkDiskObjectFile, "highwayNetwork_" + timePeriod );
        
        if ( proportionsDiskObjectFile != null )
            DataWriter.writeDiskObject ( fwFlowProps, proportionsDiskObjectFile, "fwProportions_" + timePeriod );
    
    }

    
}

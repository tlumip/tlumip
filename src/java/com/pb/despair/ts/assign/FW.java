package com.pb.despair.ts.assign;

import com.pb.common.datafile.DataWriter;
import com.pb.common.datafile.DiskObjectArray;
import com.pb.common.util.*;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;


public class FW {

    Logger logger = Logger.getLogger("com.pb.despair.ts.assign");

    static Constants c = new Constants();

	static final int SIZEOF_INT = 4;
	static final int MAX_LINK_TYPE = 1000;

	HashMap propertyMap;
	
    Network g;
    Justify myFormat = new Justify();
    TripData tl = new TripData();
    BitHash[] odList;
    int[][] odToList;

    double[][][] odTable;
    double [] lambdas;
    double [] fwFlowProps;
    
	double[][] aonFlow;
	double[][] flow;
	double[] volau;
	int[] linkType;
    
    int autoClass=0;
    int numAutoClasses;
    int totalTrips=0, totalIJs=0;
    static long totalTime=0;
    String timePeriod;

	boolean DEBUG = false;
 
    int START_ORIG, END_ORIG;
	int MAX_FW_ITERS;

	DiskObjectArray fwPathsDoa = null;	

   
    public FW ( HashMap propertyMap, Network g ) {

		this.propertyMap = propertyMap;
		this.g = g;
        
        MAX_FW_ITERS = Integer.parseInt ( (String)propertyMap.get( "NUM_FW_ITERATIONS" ) );
        numAutoClasses = Integer.parseInt ( (String)propertyMap.get( "NUM_AUTO_CLASSES" ) );
		timePeriod = g.getTimePeriod();

        odTable = new double[numAutoClasses][][];

		lambdas = new double[MAX_FW_ITERS];
		fwFlowProps = new double[MAX_FW_ITERS];

		START_ORIG = 0;
        END_ORIG = g.getNumCentroids();

    
		// initialize assignment arrays prior to iterating
		flow = g.getFlows();
		linkType = g.getLinkType();
		for (int m=0; m < numAutoClasses; m++)
			Arrays.fill(flow[m], 0.0);

		aonFlow = new double[numAutoClasses][g.getLinkCount()];
		volau = new double[g.getLinkCount()];
	
		
		// create a DiskObjectArray for storing shortest path trees
		createDiskObjectArray();
    
    }


    /**
     * Frank-Wolfe assignment procedure.
     */
	public void iterate ( double[][][] tripTable ) {

	    int storedPathIndex = 0;
	    
	    double lub = 0.0;
	    double gap = 0.0;
	    double glb = 0.0;


		// build shortest path tree object and set cost and valid link attributes for this user class.
		ShortestPathTreeH sp = new ShortestPathTreeH( g );

		// set the highway network attribute on which to skim the network
		double[] linkCost = g.getCongestedTime();
		
		double[] aon;
	
		// determine which links are valid parts of paths for this skim
		boolean[] validLinks = new boolean[g.getLinkCount()];
		Arrays.fill (validLinks, false);
		String[] mode = g.getMode();
		for (int i=0; i < validLinks.length; i++) {
		    if ( mode[i].indexOf('a') >= 0 )
		        validLinks[i] = true;
		}
   
		sp.setLinkCost( linkCost );
		sp.setValidLinks( validLinks );
		
		

		for (int m=0; m < numAutoClasses; m++)
		    Arrays.fill (flow[m], 0.0);

        
        // loop over FW iterations
        for (int iter=0; iter < MAX_FW_ITERS; iter++) {
            logger.fine("Iteration = " + iter);
            lambdas[iter] = 1.0;
            

			for (int m=0; m < numAutoClasses; m++)
			    Arrays.fill (aonFlow[m], 0.0);


			for (int origin=START_ORIG; origin < END_ORIG; origin++) {
			    
				for (int m=0; m < numAutoClasses; m++) {

					if (origin % 500 == 0)
						logger.info ("assigning origin " + origin);
				    sp.buildTree ( origin );
				    aon = sp.loadTree ( tripTable[m][origin] );
				    
				    for (int k=0; k < aon.length; k++)
				        aonFlow[m][k] += aon[k];
				    
				    
				    if ( fwPathsDoa != null ) {

				        // calculate the index used for storing shortest path tree in DiskObjectArray
					    storedPathIndex = iter*END_ORIG*numAutoClasses + origin*numAutoClasses + m;
					    
					    // store the shortest path tree for this iteration, origin zone, and user class
					    try {
					        int[] tempArray = sp.getPredecessorLink();
					        fwPathsDoa.add( storedPathIndex, tempArray );
					    } catch (Exception e) {
					        logger.severe ("could not store index=" + storedPathIndex + ", for iter=" + iter + ", for origin=" + origin + ", and class=" + m);
					        e.printStackTrace();
					        System.exit(1);
					    }
				        
				    }
				    
				}
			    
			}
            

            // use bisect to do Frank-Wolfe averaging -- returns true if exact solution
            if (iter > 0) {
                if ( bisect ( iter ) ) {
                    logger.severe ("Exact FW optimal solution found.  Unlikely, better check into this!");
                    iter = MAX_FW_ITERS;
                }
            }
            else {
				glb = Double.NEGATIVE_INFINITY;
				lub = 0.0;
				gap = 0.0;
            }


            // print assignment iterations report
            lub = ofValue();
			gap = Math.abs(ofGap());
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
				for (int m=0; m < numAutoClasses; m++) {
					flow[m][k] = flow[m][k] + lambdas[iter]*(aonFlow[m][k] - flow[m][k]);
					volau[k] += flow[m][k];
				}
			}
			g.setFlows(flow);
			g.setVolau(volau);
            g.applyVdfs();

			linkCost = g.getCongestedTime();
			sp.setLinkCost( linkCost );

			g.logLinkTimeFreqs ();
			
        } // end of FW iter loop




        linkSummaryReport();

        fwFlowProps = getFWFlowProps();
        
        
        // Write a DiskObject out that contains the Network Object, the Frank Wolfe flow
        // proportions array, and the Frank Wolfe shortest path trees Disk Object Array.
        // A separate SelectLinkAnalysis class with utilize this information to perform
        // on-demand select link analysis after the trip assignment process has finished.
        createSelectLinkAnalysisDiskObject();
        
        

        logger.info ("");
        logger.info (myFormat.right("iter", 5) + myFormat.right("lambdas", 12) + myFormat.right("Flow Props", 12));
        for (int i=0; i < MAX_FW_ITERS; i++)
            logger.info (myFormat.right(i, 5) + myFormat.right(lambdas[i], 12, 6) + myFormat.right(100.0*fwFlowProps[i], 12, 4) + "%");
        logger.info ("");

        String myDateString = DateFormat.getDateTimeInstance().format(new Date());
        logger.info ("done with Frank-Wolfe assignment: " + myDateString);

    }




    //Bisection routine to calculate opitmal lambdas during each frank-wolfe iteration.
    boolean bisect ( int iter ) {
        
        double x=0.0, xleft=0.0, xright=1.0, gap=0.0;

        int numBisectIterations = (int)(Math.log(1.0e-07)/Math.log(0.5) + 1.5);
        
        gap = ofGap();
        
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
			if (DEBUG) logger.info ("iter=" + iter + ", gap=" + gap + ", xleft=" + xleft + ", xright=" + xright + ", x=" + x);

            for (int n=0; n < numBisectIterations; n++) {
                gap = bisectGap(x);
                if (gap <= 0)
                    xleft = x;
                else
                    xright = x;
                x = (xleft + xright)/2.0;
				if (DEBUG) logger.info ("iter=" + iter + ", n=" + n + ", gap=" + gap + ", xleft=" + xleft + ", xright=" + xright + ", x=" + x);
            }
            lambdas[iter] = x;
            return(false);
        }
    }



    public double[] getFWFlowProps () {
        // Determine the proportions of O/D flow assigned during each FW iteration.

        double[] Proportions = new double[MAX_FW_ITERS];

        for (int i=0; i < MAX_FW_ITERS; i++) {
            Proportions[i] = lambdas[i];
            for (int k=i+1; k < MAX_FW_ITERS; k++)
                Proportions[i] *= (1.0-lambdas[k]);
        }

        return(Proportions);
    }


    double ofValue ()  {

		// sum total flow over all user classes for each link 
        for (int k=0; k < volau.length; k++) {
            volau[k] = 0.0;
            for (int m=0; m < numAutoClasses; m++)
                volau[k] += flow[m][k];
        }
        
		g.setVolau(volau);
        g.applyVdfIntegrals();

        return( g.getSumOfVdfIntegrals() );
    }



    double ofGap () {

		double[] totAonFlow = new double[g.getLinkCount()];
        
		// sum total flow over all user classes for each link 
		for (int k=0; k < volau.length; k++) {
			volau[k] = 0.0;
			totAonFlow[k] = 0.0;
			for (int m=0; m < numAutoClasses; m++) {
				volau[k] += flow[m][k];
				totAonFlow[k] += aonFlow[m][k];
			}
		}
        
		g.setVolau(volau);
		g.applyVdfs();
		double[] cTime = g.getCongestedTime();
		
		double gap = 0.0;
		for (int k=0; k < volau.length; k++)
            gap += cTime[k]*(totAonFlow[k] - volau[k]);

        return(gap);
    }


    double bisectGap (double x) {

		double[] totAonFlow = new double[g.getLinkCount()];
		double[] totalFlow = new double[g.getLinkCount()];
        
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
        
		g.setVolau(volau);
		g.applyVdfs();
		double[] cTime = g.getCongestedTime();

        double gap = 0.0;
		for (int k=0; k < cTime.length; k++)
            gap += cTime[k]*(totAonFlow[k] - volau[k]);
//            gap += cTime[k]*(totAonFlow[k] - totalFlow[k]);


        return(gap);
    }


    void linkSummaryReport () {
        double totalVol;
        double[][] volumeSum = new double[numAutoClasses][MAX_LINK_TYPE];

        for (int k=0; k < g.getLinkCount(); k++)
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
	    
	    
	    // get the location of the file for storing paths
		String diskObjectArrayFile = (String)propertyMap.get("PathsDiskObjectArray.file");

		// create the object
		if ( diskObjectArrayFile != null ) {

			try{
			    
			    int numElements = MAX_FW_ITERS*END_ORIG*numAutoClasses;
			    int maxElementSize = SIZEOF_INT*(g.getNodeCount()+1) + 100;
			    
			    logger.info ("dimensions for paths DiskObjectArray: numElements=" + numElements + ", maxElementSize=" + maxElementSize );
		
			    fwPathsDoa = new DiskObjectArray( diskObjectArrayFile, numElements, maxElementSize );
		
			} catch(IOException e) {
			    
				logger.severe("could not open disk object array file for storing shortest path trees.");
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
		    DataWriter.writeDiskObject ( g, networkDiskObjectFile, "highwayNetwork_" + timePeriod );
		
		if ( proportionsDiskObjectFile != null )
		    DataWriter.writeDiskObject ( fwFlowProps, proportionsDiskObjectFile, "fwProportions_" + timePeriod );
	
	}

}

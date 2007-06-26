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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;


import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.NetworkHandlerIF;



public class Skims {

	protected static Logger logger = Logger.getLogger(Skims.class);

	HashMap tsPropertyMap;
    HashMap globalPropertyMap;

	

    NetworkHandlerIF g;
    
	int[] alphaNumberArray = null;
	int[] betaNumberArray = null;
	int[] zonesToSkim = null;

    int[] externalToAlphaInternal = null;
    int[] alphaExternalNumbers = null;
	
    int numCentroids = 0;

    
    
    public Skims ( ResourceBundle tsRb, ResourceBundle globalRb, String timePeriod ) {

        NetworkHandlerIF nh = NetworkHandler.getInstance();
        setupNetwork( nh, tsPropertyMap, globalPropertyMap, timePeriod );


        initSkims ();

    }


    public Skims ( NetworkHandlerIF g, HashMap tsMap, HashMap globalMap ) {

        this.g = g;
        this.tsPropertyMap = tsMap;
        this.globalPropertyMap = globalMap;

        initSkims ();

    }



    private void setupNetwork ( NetworkHandlerIF nh, HashMap appMap, HashMap globalMap, String timePeriod ) {
        
        String networkFileName = (String)appMap.get("d211.fileName");
        String networkDiskObjectFileName = (String)appMap.get("NetworkDiskObject.file");
        
        String turnTableFileName = (String)appMap.get( "d231.fileName" );
        String networkModsFileName = (String)appMap.get( "d211Mods.fileName" );
        
        String vdfFileName = (String)appMap.get("vdf.fileName");
        String vdfIntegralFileName = (String)appMap.get("vdfIntegral.fileName");
        
        String a2bFileName = (String) globalMap.get( "alpha2beta.file" );
        
        // get peak or off-peak volume factor from properties file
        String volumeFactor="";
        if ( timePeriod.equalsIgnoreCase( "peak" ) )
            volumeFactor = (String)globalMap.get("AM_PEAK_VOL_FACTOR");
        else if ( timePeriod.equalsIgnoreCase( "offpeak" ) )
            volumeFactor = (String)globalMap.get("OFF_PEAK_VOL_FACTOR");
        else {
            logger.error ( "time period specifed as: " + timePeriod + ", but must be either 'peak' or 'offpeak'." );
            System.exit(-1);
        }
        
        String userClassesString = (String)appMap.get("userClass.modes");
        String truckClass1String = (String)appMap.get( "truckClass1.modes" );
        String truckClass2String = (String)appMap.get( "truckClass2.modes" );
        String truckClass3String = (String)appMap.get( "truckClass3.modes" );
        String truckClass4String = (String)appMap.get( "truckClass4.modes" );
        String truckClass5String = (String)appMap.get( "truckClass5.modes" );

        String walkSpeed = (String)globalMap.get( "WALK_MPH" );
        
        
        String[] propertyValues = new String[NetworkHandler.NUMBER_OF_PROPERTY_VALUES];
        
        propertyValues[NetworkHandlerIF.NETWORK_FILENAME_INDEX] = networkFileName;
        propertyValues[NetworkHandlerIF.NETWORK_DISKOBJECT_FILENAME_INDEX] = networkDiskObjectFileName;
        propertyValues[NetworkHandlerIF.VDF_FILENAME_INDEX] = vdfFileName;
        propertyValues[NetworkHandlerIF.VDF_INTEGRAL_FILENAME_INDEX] = vdfIntegralFileName;
        propertyValues[NetworkHandlerIF.ALPHA2BETA_FILENAME_INDEX] = a2bFileName;
        propertyValues[NetworkHandlerIF.TURNTABLE_FILENAME_INDEX] = turnTableFileName;
        propertyValues[NetworkHandlerIF.NETWORKMODS_FILENAME_INDEX] = networkModsFileName;
        propertyValues[NetworkHandlerIF.VOLUME_FACTOR_INDEX] = volumeFactor;
        propertyValues[NetworkHandlerIF.USER_CLASSES_STRING_INDEX] = userClassesString;
        propertyValues[NetworkHandlerIF.TRUCKCLASS1_STRING_INDEX] = truckClass1String;
        propertyValues[NetworkHandlerIF.TRUCKCLASS2_STRING_INDEX] = truckClass2String;
        propertyValues[NetworkHandlerIF.TRUCKCLASS3_STRING_INDEX] = truckClass3String;
        propertyValues[NetworkHandlerIF.TRUCKCLASS4_STRING_INDEX] = truckClass4String;
        propertyValues[NetworkHandlerIF.TRUCKCLASS5_STRING_INDEX] = truckClass5String;
        propertyValues[NetworkHandlerIF.WALK_SPEED_INDEX] = walkSpeed;
        
        nh.setupHighwayNetworkObject ( timePeriod, propertyValues );
        
    }
    
    
    private void initSkims () {

        numCentroids = g.getNumCentroids();
        
		// take a column of alpha zone numbers from a TableDataSet and puts them into an array for
	    // purposes of setting external numbers.	     */
		String zoneCorrespondenceFile = (String)globalPropertyMap.get("alpha2beta.file");
		try {
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(zoneCorrespondenceFile));
	        alphaNumberArray = table.getColumnAsInt( 1 );
	        betaNumberArray = table.getColumnAsInt( 2 );
        } catch (IOException e) {
            logger.fatal("Can't get zone numbers from zonal correspondence file", e);
        }

    
        // define which of the total set of centroids are within the Halo area and should have skim trees built
	    zonesToSkim = new int[g.getMaxCentroid()+1];
	    externalToAlphaInternal = new int[g.getMaxCentroid()+1];
	    alphaExternalNumbers = new int[alphaNumberArray.length+1];
		Arrays.fill ( zonesToSkim, 0 );
		Arrays.fill ( externalToAlphaInternal, -1 );
	    for (int i=0; i < alphaNumberArray.length; i++) {
	    	zonesToSkim[alphaNumberArray[i]] = 1;
	    	externalToAlphaInternal[alphaNumberArray[i]] = i;
	    	alphaExternalNumbers[i+1] = alphaNumberArray[i];
	    }

    }



    /**
	 * write out an alpha zone skim matrix
	 */
	public void writeHwySkimMatrix ( String assignmentPeriod, String skimType, char modeChar ) {

		String fileName = null;
		
		Matrix newSkimMatrix = getHwySkimMatrix ( assignmentPeriod, skimType, modeChar );
		
		
		// initialize the necessary values based on the assignmet period and type of skim to produce
        if ( assignmentPeriod.equalsIgnoreCase( "peak" ) ) {
        	if ( skimType.equalsIgnoreCase("time") ) {
        	    fileName = (String)tsPropertyMap.get( "pkHwyTimeSkim.fileName" );
        	}
            else if ( skimType.equalsIgnoreCase("dist") ) {
                fileName = (String)tsPropertyMap.get( "pkHwyDistSkim.fileName" );
            }
            else if ( skimType.equalsIgnoreCase("toll") ) {
                fileName = (String)tsPropertyMap.get( "pkHwyTollSkim.fileName" );
            }
        }
        else if ( assignmentPeriod.equalsIgnoreCase( "offpeak" ) ) {
        	if ( skimType.equalsIgnoreCase("time") ) {
        	    fileName = (String)tsPropertyMap.get( "opHwyTimeSkim.fileName" );
        	}
            else if ( skimType.equalsIgnoreCase("dist") ) {
                fileName = (String)tsPropertyMap.get( "opHwyDistSkim.fileName" );
            }
            else if ( skimType.equalsIgnoreCase("toll") ) {
                fileName = (String)tsPropertyMap.get( "opHwyTollSkim.fileName" );
            }
        }
        
        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(newSkimMatrix);
        
	}


    /**
	 * write out a set of alpha zone skim matrices
	 */
	public void writeHwySkimMatrices ( String assignmentPeriod, String[] skimType, char modeChar ) {

		String[] fileName = null;
		String[] matrixName = null;
		String[] matrixDescription = null;
		double[][] linkAttrib = null;

		linkAttrib = new double[skimType.length][];
		fileName = new String[skimType.length];
		matrixName = new String[skimType.length];
		matrixDescription = new String[skimType.length];
		
		for (int i=0; i < skimType.length; i++) {

			// initialize the necessary values based on the assignmet period and type of skim to produce
	        if ( assignmentPeriod.equalsIgnoreCase( "peak" ) ) {
	        	if ( skimType[i].equalsIgnoreCase("time") ) {
	        	    fileName[i] = (String)tsPropertyMap.get( "pkHwyTimeSkim.fileName" );
	        	    matrixName[i] = (String)tsPropertyMap.get( "pkHwyTimeSkim.matrixName" );
	        	    matrixDescription[i] = (String)tsPropertyMap.get( "pkHwyTimeSkim.matrixDescription" );

	        		linkAttrib[i] = g.getCongestedTime();
	        	}
                else if ( skimType[i].equalsIgnoreCase("dist") ) {
                    fileName[i] = (String)tsPropertyMap.get( "pkHwyDistSkim.fileName" );
                    matrixName[i] = (String)tsPropertyMap.get( "pkHwyDistSkim.matrixName" );
                    matrixDescription[i] = (String)tsPropertyMap.get( "pkHwyDistSkim.matrixDescription" );

                    linkAttrib[i] = g.getDist();
                }
                else if ( skimType[i].equalsIgnoreCase("toll") ) {
                    fileName[i] = (String)tsPropertyMap.get( "pkHwyTollSkim.fileName" );
                    matrixName[i] = (String)tsPropertyMap.get( "pkHwyTollSkim.matrixName" );
                    matrixDescription[i] = (String)tsPropertyMap.get( "pkHwyTollSkim.matrixDescription" );

                    linkAttrib[i] = g.getToll();
                }
	        }
	        else if ( assignmentPeriod.equalsIgnoreCase( "offpeak" ) ) {
	        	if ( skimType[i].equalsIgnoreCase("time") ) {
	        	    fileName[i] = (String)tsPropertyMap.get( "opHwyTimeSkim.fileName" );
	        	    matrixName[i] = (String)tsPropertyMap.get( "opHwyTimeSkim.matrixName" );
	        	    matrixDescription[i] = (String)tsPropertyMap.get( "opHwyTimeSkim.matrixDescription" );

	        		linkAttrib[i] = g.getCongestedTime();
	        	}
	        	else if ( skimType[i].equalsIgnoreCase("dist") ) {
	        	    fileName[i] = (String)tsPropertyMap.get( "opHwyDistSkim.fileName" );
	        	    matrixName[i] = (String)tsPropertyMap.get( "opHwyDistSkim.matrixName" );
	        	    matrixDescription[i] = (String)tsPropertyMap.get( "opHwyDistSkim.matrixDescription" );

	        		linkAttrib[i] = g.getDist();
	        	}
                else if ( skimType[i].equalsIgnoreCase("toll") ) {
                    fileName[i] = (String)tsPropertyMap.get( "opHwyTollSkim.fileName" );
                    matrixName[i] = (String)tsPropertyMap.get( "opHwyTollSkim.matrixName" );
                    matrixDescription[i] = (String)tsPropertyMap.get( "opHwyTollSkim.matrixDescription" );

                    linkAttrib[i] = g.getToll();
                }
	        }
			
		}
        
		Matrix[] newSkimMatrices = getHwySkimMatrices ( assignmentPeriod, linkAttrib, modeChar );
		
		for (int i=0; i < skimType.length; i++) {
			
			// write alpha zone skim matrix
	        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName[i]) );
	        mw.writeMatrix(newSkimMatrices[i]);
	        
	        // write beta zone skim matrix
	        writeBetaSkimMatrix ( assignmentPeriod, skimType[i], newSkimMatrices[i] );
	        	
		}
        
	}


    /**
	 * calculate the alpha zone skim matrices based on the set of lin attributes passed in and return the Matrix object array
	 */
	public Matrix[] getHwySkimMatrices ( String assignmentPeriod, double[][] linkAttribs, char modeChar ) {

		String matrixName = null;
		String matrixDescription = null;

		Matrix[] newSkimMatrices = new Matrix[linkAttribs.length];
		
		
		// set generalized cost as the link attribute by which to build shortest paths trees
		double[] linkCost = g.setLinkGeneralizedCost ();

		// set the highway network attribute on which to skim the network - congested time in this case
		boolean[] validLinks = g.getValidLinksForClassChar( modeChar );
		

		// get the skims as a double[][] array dimensioned to number of centroids (2984)
        double[][][] zeroBasedDoubleArrays = buildHwySkimMatrices( linkCost, linkAttribs, validLinks );

		for ( int i=0; i < linkAttribs.length; i++ ) {

	        // convert to a float[][] dimensioned to number of alpha zones (2950)
	        float[][] zeroBasedFloatArray = getZeroBasedFloatArray ( zeroBasedDoubleArrays[i] );

	        // define default names for matrices.  They can be set later if necessary
			matrixName = "table" + i;
			matrixDescription = "table" + i + ", mode " + modeChar;
			newSkimMatrices[i] = new Matrix( matrixName, matrixDescription, zeroBasedFloatArray );
			newSkimMatrices[i].setExternalNumbers( alphaExternalNumbers );
			
		}
		
        
        
        return newSkimMatrices;
        
	}


	public Matrix getHwySkimMatrix ( String assignmentPeriod, String skimType, char modeChar ) {

		String matrixName = null;
		String matrixDescription = null;

		// set generalized cost as the link attribute by which to build shortest paths trees
		double[] linkCost = g.setLinkGeneralizedCost ();

		// define a variable to hold the link attribute to be skimmed (e.g. time, dist, toll, genCost, ...)
		double[] linkAttrib = null;

		// set the highway network attribute on which to skim the network - congested time in this case
		boolean[] validLinks = g.getValidLinksForClassChar( modeChar );
		

		// initialize the necessary values based on the assignmet period and type of skim to produce
        if ( assignmentPeriod.equalsIgnoreCase( "peak" ) ) {
        	if ( skimType.equalsIgnoreCase("time") ) {
        	    matrixName = (String)tsPropertyMap.get( "pkHwyTimeSkim.matrixName" );
        	    matrixDescription = (String)tsPropertyMap.get( "pkHwyTimeSkim.matrixDescription" );

        		linkAttrib = g.getCongestedTime();
        	}
            else if ( skimType.equalsIgnoreCase("dist") ) {
                matrixName = (String)tsPropertyMap.get( "pkHwyTimeSkim.matrixName" );
                matrixDescription = (String)tsPropertyMap.get( "pkHwyTimeSkim.matrixDescription" );

                linkAttrib = g.getDist();
            }
            else if ( skimType.equalsIgnoreCase("toll") ) {
                matrixName = (String)tsPropertyMap.get( "pkHwyTollSkim.matrixName" );
                matrixDescription = (String)tsPropertyMap.get( "pkHwyTollSkim.matrixDescription" );

                linkAttrib = g.getToll();
            }
        }
        else if ( assignmentPeriod.equalsIgnoreCase( "offpeak" ) ) {
        	if ( skimType.equalsIgnoreCase("time") ) {
        	    matrixName = (String)tsPropertyMap.get( "opHwyTimeSkim.matrixName" );
        	    matrixDescription = (String)tsPropertyMap.get( "opHwyTimeSkim.matrixDescription" );

        		linkAttrib = g.getCongestedTime();
        	}
        	else if ( skimType.equalsIgnoreCase("dist") ) {
        	    matrixName = (String)tsPropertyMap.get( "opHwyTimeSkim.matrixName" );
        	    matrixDescription = (String)tsPropertyMap.get( "opHwyTimeSkim.matrixDescription" );

        		linkAttrib = g.getDist();
        	}
            else if ( skimType.equalsIgnoreCase("toll") ) {
                matrixName = (String)tsPropertyMap.get( "opHwyTollSkim.matrixName" );
                matrixDescription = (String)tsPropertyMap.get( "opHwyTollSkim.matrixDescription" );

                linkAttrib = g.getToll();
            }
        }
        

        
        // get the skims as a double[][] array dimensioned to number of centroids (2984)
        double[][] zeroBasedDoubleArray = buildHwySkimMatrix( linkCost, linkAttrib, validLinks );

        // convert to a float[][] dimensioned to number of alpha zones (2950)
        float[][] zeroBasedFloatArray = getZeroBasedFloatArray ( zeroBasedDoubleArray );

        Matrix newSkimMatrix = new Matrix( matrixName, matrixDescription, zeroBasedFloatArray );
	    newSkimMatrix.setExternalNumbers( alphaExternalNumbers );
        
        
        return newSkimMatrix;
        
	}


    /**
	 * calculate the beta zone skim matrix and return the beta zone Matrix object
	 */
	public Matrix getBetaSkimMatrix ( Matrix alphaZoneMatrix ) {

        // alphaNumberArray and betaNumberArray were read in from correspondence file and are zero-based
        AlphaToBeta a2b = new AlphaToBeta (alphaNumberArray, betaNumberArray);

        // create a MatrixCompression object and return the squeezed matrix
        MatrixCompression squeeze = new MatrixCompression(a2b);
        return squeeze.getCompressedMatrix( alphaZoneMatrix, "MEAN" );
        
	}


    
    /**
	 * write out beta zone time skim matrices and return the beta zone Matrix object
	 */
	public void writeBetaSkimMatrix ( String assignmentPeriod, String skimType, Matrix alphaZoneMatrix ) {

		String fileName = null;
		
		Matrix mSqueezed = getBetaSkimMatrix ( alphaZoneMatrix );

		
	    // create a Matrix from the peak alpha congested time skims array and write to disk
        if ( assignmentPeriod.equalsIgnoreCase( "peak" ) ) {
        	if ( skimType.equalsIgnoreCase("time") )
        	    fileName = (String)tsPropertyMap.get( "pkHwyTimeBetaSkim.fileName" );
            else if ( skimType.equalsIgnoreCase("dist") )
                fileName = (String)tsPropertyMap.get( "pkHwyDistBetaSkim.fileName" );
            else if ( skimType.equalsIgnoreCase("toll") )
                fileName = (String)tsPropertyMap.get( "pkHwyTollBetaSkim.fileName" );
        }
        else if ( assignmentPeriod.equalsIgnoreCase( "offpeak" ) ) {
        	if ( skimType.equalsIgnoreCase("time") )
        	    fileName = (String)tsPropertyMap.get( "opHwyTimeBetaSkim.fileName" );
            else if ( skimType.equalsIgnoreCase("dist") )
                fileName = (String)tsPropertyMap.get( "opHwyDistBetaSkim.fileName" );
            else if ( skimType.equalsIgnoreCase("toll") )
                fileName = (String)tsPropertyMap.get( "opHwyTollBetaSkim.fileName" );
        }
        
	    // create a squeezed beta skims Matrix from the peak alpha distance skims Matrix and write to disk
        if(fileName != null) {
            MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
            mw.writeMatrix(mSqueezed);
        }
        
        
	}


    
    private float[][] getZeroBasedFloatArray ( double[][] zeroBasedDoubleArray ) {

    	int[] skimsInternalToExternal = g.getIndexNode();

		// convert the zero-based double[2983][2983] produced by the skimming procedure
    	// to a zero-based float[2950][2950] for alpha zones to be written to skims file.
		float[][] zeroBasedFloatArray = new float[alphaNumberArray.length][alphaNumberArray.length];
		
        int exRow;
        int exCol;
        int inRow;
        int inCol;
		for (int i=0; i < zeroBasedDoubleArray.length; i++) {
			exRow = skimsInternalToExternal[i];
	    	if ( zonesToSkim[exRow] == 1 ) {
				inRow = externalToAlphaInternal[exRow];
                for (int j=0; j < zeroBasedDoubleArray[i].length; j++) {
                	exCol = skimsInternalToExternal[j];
	    	    	if ( zonesToSkim[exCol] == 1 ) {
	    				inCol = externalToAlphaInternal[exCol];
	    				zeroBasedFloatArray[inRow][inCol] = (float)zeroBasedDoubleArray[i][j];
	    	    	}
	    		}
	    	}
	    }

		zeroBasedDoubleArray = null;

	    return zeroBasedFloatArray;

    }


    /**
	 * build network skim arrays, return as double[][][].
	 * the highway network attributes on which to skim the network to produce multipe skim tables is passed in.
	 */
	private double[][][] buildHwySkimMatrices ( double[] linkCost, double[][] linkAttributes, boolean[] validLinks ) {
		
		
		double[][][] skimMatrices =  hwySkims ( linkCost, linkAttributes, validLinks, alphaNumberArray );
		int minI = 0;
        int minJ = 0;
		// set intrazonal values to 0.5*nearest neighbor
		for (int k=0; k < skimMatrices.length; k++) {
			
			for (int i=0; i < skimMatrices[k].length; i++) {
			
				// find minimum valued row element
				double minValue = Double.MAX_VALUE;
				for (int j=0; j < skimMatrices[k].length; j++) {
					if ( i != j && skimMatrices[k][i][j] != Double.NEGATIVE_INFINITY && skimMatrices[k][i][j] < minValue ){
						minValue = skimMatrices[k][i][j];
	                    minI = i;
	                    minJ = j;
	                }
				}
	
				// set intrazonal value
	            if(minValue < 0) {
	                logger.fatal("Hwy skim min value is " + minValue + "@ rowIndex " + minI + ", colIndex " + minJ);
	                logger.fatal("System will exit, no hwy skims have been written");
	                System.exit(10);
	            }
	
				if ( minValue < Double.MAX_VALUE )
					skimMatrices[k][i][i] = 0.5*minValue;
				else
					skimMatrices[k][i][i] = Double.NEGATIVE_INFINITY;

			}
		}
		
		return skimMatrices;
       
	}


    /**
	 * build network skim array, return as double[][].
	 * the highway network attribute on which to skim the network is passed in.
	 */
	private double[][] buildHwySkimMatrix ( double[] linkCost, double[] linkAttribute, boolean[] validLinks ) {
		
		
		double[][] skimMatrix =  hwySkim ( linkCost, linkAttribute, validLinks, alphaNumberArray );
		int minI = 0;
        int minJ = 0;
		// set intrazonal values to 0.5*nearest neighbor
		for (int i=0; i < skimMatrix.length; i++) {
			
			// find minimum valued row element
			double minValue = Double.MAX_VALUE;
			for (int j=0; j < skimMatrix.length; j++) {
				if ( i != j && skimMatrix[i][j] != Double.NEGATIVE_INFINITY && skimMatrix[i][j] < minValue ){
					minValue = skimMatrix[i][j];
                    minI = i;
                    minJ = j;
                }
			}

			// set intrazonal value
            if(minValue <= 0) {
                logger.fatal("Hwy skim min value is " + minValue + "@ rowIndex " + minI + ", colIndex " + minJ);
                logger.fatal("System will exit, no hwy skims have been written");
                System.exit(10);
            }

			if ( minValue < Double.MAX_VALUE )
				skimMatrix[i][i] = 0.5*minValue;
			else
				skimMatrix[i][i] = Double.NEGATIVE_INFINITY;

			
		}
		
		return skimMatrix;
       
	}


	/**
	 * highway network skimming procedure for generating multiple skim tables
	 */
	private double[][][] hwySkims ( double[] linkCost, double[][] linkAttributes, boolean[] validLinks, int[] alphaNumberArray ) {

	    int i;
	    
        double[][] tempMatrices;
        double[][][] skimMatrices = new double[linkAttributes.length][numCentroids][];

		
		// create a ShortestPathTreeH object
		ShortestPathTreeH sp = new ShortestPathTreeH( g );


		// build shortest path trees based on linkcost and summarize time, dist, and toll skims for each origin zone.		sp.setLinkCost( linkCost );
		sp.setValidLinks( validLinks );
        sp.setLinkCost( linkCost );
		
		
		// loop through the origin zones
		for (i=0; i < numCentroids; i++) {
			
			// build the shortest path tree
			sp.buildTree( i );
			
			// skim the shortest path tree for all the link attributes required
            tempMatrices = sp.getSkims( linkAttributes );
            for (int k=0; k < linkAttributes.length; k++)
                skimMatrices[k][i] = tempMatrices[k];
			
		}

		return skimMatrices;
        
	}


	/**
	 * highway network skimming procedure
	 */
	private double[][] hwySkim ( double[] linkCost, double[] linkAttribute, boolean[] validLinks, int[] alphaNumberArray ) {

	    int i;
	    
	    
	    
		double[][] skimMatrix = new double[numCentroids][];

		// create a ShortestPathTreeH object
		ShortestPathTreeH sp = new ShortestPathTreeH( g );


        // build shortest path trees based on linkcost and summarize time, dist, and toll skims for each origin zone.
		sp.setLinkCost( linkCost );
		sp.setValidLinks( validLinks );
		
		
		for (i=0; i < numCentroids; i++) {

			if (i % 500 == 0)
				logger.info ("shortest path tree for origin zone index " + i);

			sp.buildTree( i );
			skimMatrix[i] = sp.getSkim( linkAttribute );
		}

		return skimMatrix;
        
	}


	public Matrix getSqueezedMatrix (Matrix aMatrix) {
		
        // alphaNumberArray and betaNumberArray were read in from correspondence file and are zero-based
        AlphaToBeta a2b = new AlphaToBeta (alphaNumberArray, betaNumberArray);

        // create a MatrixCompression object and return the squeezed matrix
        MatrixCompression squeeze = new MatrixCompression(a2b);
        return squeeze.getCompressedMatrix(aMatrix, "MEAN");
        
	}
	
	

	/**
	 * get average SOV trip skim values
	 *   returns average skimm value weighted by trips and the number of o/d pairs with trips but disconnected skim value
	 */
	public double[] getAvgTripSkims ( double[][] trips, Matrix skimMatrix ) {

		double disconnected = 0;
		double tripSkim = 0.0;
		double totalTrips = 0.0;
		float matrixValue;
		
		for (int r=1; r <= skimMatrix.getRowCount(); r++) {
			for (int c=1; c <= skimMatrix.getColumnCount(); c++) {
				matrixValue = skimMatrix.getValueAt(r,c);
				int i = r - 1;
				int j = c - 1;
				if ( trips[i][j] > 0.0 ) {
					if ( matrixValue != Double.NEGATIVE_INFINITY ) {
						tripSkim += trips[i][j]*matrixValue;
						totalTrips += trips[i][j];
					}
					else {
						disconnected ++;
					}
				}
			}
		}

           	
		double[] results = new double[2];
		results[0] = tripSkim/totalTrips;
		results[1] = disconnected;
       
		return results;

	}


   

	public static void main(String[] args) {

    	
    	logger.info ("creating Skims object.");
        Skims s = new Skims ( ResourceBundle.getBundle("ts"), ResourceBundle.getBundle ("global"), "peak" );

    	logger.info ("skimming network and creating Matrix object for peak auto time.");
        Matrix m = s.getHwySkimMatrix ( "peak", "time", 'a' );
        m.logMatrixStatsToInfo();

    	logger.info ("squeezing the alpha matrix to a beta matrix.");
    	Matrix mSqueezed = s.getBetaSkimMatrix ( m );
        mSqueezed.logMatrixStatsToInfo();
        
    }
}

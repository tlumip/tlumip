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

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.model.WorldZoneExternalZoneUtil;
import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.NetworkHandlerIF;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.ResourceBundle;


public class Skims {

	protected static Logger logger = Logger.getLogger(Skims.class);

	HashMap tsPropertyMap;
    HashMap globalPropertyMap;

    ResourceBundle tsRb;
    ResourceBundle globalRb;

    NetworkHandlerIF nh;
    
    int[] tazToAlphaIndex = null;
    int[] alphaIndexToTaz = null;
    int[] alphaIndexToBeta = null;
    int[] zonesToAlpha = null;
    int[] alphaMatrixExternalNumbers = null;

	double[] skimTripFreqs = null;
    
    int numCentroids = 0;
    int maxCentroid = 0;

    
    
    public Skims ( ResourceBundle tsRb, ResourceBundle globalRb, String timePeriod ) {

        this.tsRb = tsRb;
        this.globalRb = globalRb;
        tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( tsRb );
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( globalRb );
        
        nh = NetworkHandler.getInstance();

        setupNetwork( tsPropertyMap, globalPropertyMap, timePeriod );

        initSkims ();

    }


    public Skims ( NetworkHandlerIF nh, ResourceBundle tsRb, ResourceBundle globalRb ) {


        this.tsRb = tsRb;
        this.globalRb = globalRb;
        this.nh = nh;
        tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( tsRb );
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap( globalRb );

        initSkims ();

    }



    private void setupNetwork ( HashMap appMap, HashMap globalMap, String timePeriod ) {
        
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

        int[] alphaNumberArray = null;
        int[] betaNumberArray = null;
        numCentroids = nh.getNumCentroids();
        maxCentroid = nh.getMaxCentroid();
        
        // read the list of alphas and corresponding betas from the alpha2beta.csv file.
		// take a column of alpha zone numbers from a TableDataSet and puts them into an array for
	    // purposes of setting external numbers.
		String zoneCorrespondenceFile = (String)globalPropertyMap.get("alpha2beta.file");
		try {
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            TableDataSet table = reader.readFile(new File(zoneCorrespondenceFile));
	        alphaNumberArray = table.getColumnAsInt( 1 );
	        betaNumberArray = table.getColumnAsInt( 2 );
        } catch (IOException e) {
            logger.fatal("Can't get zone numbers from zonal correspondence file", e);
        }

        // get the list of externals from the NetworkHandler.
        int[] externals = nh.getExternalZoneLabels();

        
        // zones to skim are indexed from 0,...,alphas+externals.
        // the initial double array of skim values computed will be ordered 0,...,alphas+externals, where indices refer to network internal centroid numbers.
        // the float array determined from this double array must have values rearranged so the row,column order matches the order of alphas+externals read from alpha2beta file.
        
        // make arrays to convert TAZ number to alpha zone array index and back 
        tazToAlphaIndex = new int[maxCentroid + 1];
        alphaIndexToTaz = new int[numCentroids];
        alphaIndexToBeta = new int[numCentroids];

        // create a external number array for the Matrix object to be created based on 1s indexing
        alphaMatrixExternalNumbers = new int[numCentroids + 1];
        
        for (int i=0; i < alphaNumberArray.length; i++) {
            alphaIndexToTaz[i] = alphaNumberArray[i];
            tazToAlphaIndex[alphaNumberArray[i]] = i;
            alphaIndexToBeta[i] = betaNumberArray[i];
            alphaMatrixExternalNumbers[i+1] = alphaNumberArray[i]; 
        }
        for (int i=0; i < externals.length; i++) {
            alphaIndexToTaz[alphaNumberArray.length+i] = externals[i];
            tazToAlphaIndex[externals[i]] = alphaNumberArray.length+i;
            alphaIndexToBeta[alphaNumberArray.length+i] = externals[i];
            alphaMatrixExternalNumbers[alphaNumberArray.length+1+i] = externals[i]; 
        }
        

    }





    /**
	 * write out a set of alpha zone skim matrices
	 */
	public void writeHwySkimMatrices ( String assignmentPeriod, String[] skimType, char modeChar ) {

        double[][] linkAttrib = new double[skimType.length][];
        String[] fileName = new String[skimType.length];
        String[] matrixName = new String[skimType.length];
        String[] matrixDescription = new String[skimType.length];
		
        // get the directory in which to write the skims files and the extension to use
        String skimsDirectory = (String)tsPropertyMap.get( "highwaySkims.directory" );
        String matrixExtension = (String) globalPropertyMap.get("matrix.extension");
        
        String tempName;
        String tempMatrixDescription;
        
		for (int i=0; i < skimType.length; i++) {

            // construct a filename based on assignment period, network mode skimmed, skim value:
            
            // get String associated with peak.identifier or offpeak.identifier from property file, and start filename with that String.
            String periodIdentifierString = assignmentPeriod + ".identifier";
            tempName = (String)tsPropertyMap.get( periodIdentifierString );
            tempMatrixDescription = assignmentPeriod + " ";

            // determine modeString, either "auto" or "truck", and if "truck", set numeric index for truck mode
            String modeString = "";
            int modeIndex = 0;
            if ( modeChar == 'a' ) {
                modeString = "auto";
            }
            else if ( modeChar == 'd' ) {
                modeString = "truck";
                modeIndex = 1;
            }
            else if ( modeChar == 'e' ) {
                modeString = "truck";
                modeIndex = 2;
            }
            else if ( modeChar == 'f' ) {
                modeString = "truck";
                modeIndex = 3;
            }
            else if ( modeChar == 'g' ) {
                modeString = "truck";
                modeIndex = 4;
            }
            else if ( modeChar == 'h' ) {
                modeString = "truck";
                modeIndex = 5;
            }
            
            // get String associated with "auto.identifier" or "truck.identifier", and if "truck.identifier", append numeric index.
            String modeIdentifierString = (String)tsPropertyMap.get( modeString + ".identifier" );
            if ( modeIndex > 0 )
                modeIdentifierString = String.format("%s%d", modeIdentifierString, modeIndex);
            
            // append the modeIdentifierString to the filename.
            tempName += modeIdentifierString;
            tempMatrixDescription = modeString + ( modeIndex == 0 ? " " : String.valueOf(modeIndex) + " " );
            
            // append "time", "dist", or "toll" fir the skim type
            tempName += skimType[i];
            tempMatrixDescription += skimType[i];

            

            // use tempName for both the file name and the matrix name

            fileName[i] = skimsDirectory + tempName + matrixExtension;
            matrixName[i] = tempName;
            matrixDescription[i] = tempMatrixDescription;
            if ( skimType[i].equalsIgnoreCase("time") ) {
                linkAttrib[i] = nh.getCongestedTime();
            }
            else if ( skimType[i].equalsIgnoreCase("dist") ) {
                linkAttrib[i] = nh.getDist();
            }
            else if ( skimType[i].equalsIgnoreCase("toll") ) {
                linkAttrib[i] = nh.getToll();
            }
			
		}

        
		Matrix[] newSkimMatrices = getHwySkimMatrices ( assignmentPeriod, linkAttrib, matrixName, matrixDescription, modeChar );
		
		for (int i=0; i < skimType.length; i++) {
			
            logger.info( String.format( "writing %s" + matrixExtension+ " skim matrix file.", matrixName[i] ) );
            
			// write alpha zone skim matrix
	        MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName[i]) );
	        mw.writeMatrix(newSkimMatrices[i]);
	        

            if ( modeChar == 'd' || modeChar == 'e' || modeChar == 'f' || modeChar == 'g' || modeChar == 'h' ) {
                
                logger.info( String.format( "writing beta%s" + matrixExtension + " skim matrix file.", matrixName[i] ) );

                int truckIndex = 1 + (modeChar - 'd');
                String periodIdentifierString = assignmentPeriod + ".identifier";
                tempName = (String)tsPropertyMap.get( periodIdentifierString );
                
                String betaFileName = skimsDirectory + "beta" + tempName + "trk" + String.valueOf(truckIndex) + skimType[i] + matrixExtension;
                writeBetaSkimMatrix ( newSkimMatrices[i], betaFileName );
                
            }

        }

        
	}


    /**
	 * calculate the alpha zone skim matrices based on the set of lin attributes passed in and return the Matrix object array
	 */
	public Matrix[] getHwySkimMatrices ( String assignmentPeriod, double[][] linkAttribs, String[] matrixName, String[] matrixDescription, char modeChar ) {

		Matrix[] newSkimMatrices = new Matrix[linkAttribs.length];
		
		
		// set generalized cost as the link attribute by which to build shortest paths trees
		double[] linkCost = nh.setLinkGeneralizedCost ();

		// set the highway network attribute on which to skim the network - congested time in this case
		boolean[] validLinks = nh.getValidLinksForClassChar( modeChar );
		

		// get the skims as a double[][] array dimensioned to number of centroids (alphas + externals)
        double[][][] zeroBasedDoubleArrays = buildHwySkimMatrices( linkCost, linkAttribs, validLinks );

		for ( int i=0; i < linkAttribs.length; i++ ) {

	        // convert to a float[][] dimensioned to number of alpha zones + externals
	        float[][] zeroBasedFloatArray = getZeroBasedFloatArray ( zeroBasedDoubleArrays[i] );

	        // define default names for matrices.  They can be set later if necessary
			newSkimMatrices[i] = new Matrix( matrixName[i], matrixDescription[i], zeroBasedFloatArray );
			newSkimMatrices[i].setExternalNumbers( alphaMatrixExternalNumbers );
			
		}
		
        
        
        return newSkimMatrices;
        
	}


    /**
	 * calculate the beta zone skim matrix and return the beta zone Matrix object
	 */
	public Matrix getBetaSkimMatrix ( Matrix alphaZoneMatrix ) {

        // alpha and beta arrays were read in from correspondence file and externals appended; also are zero-based.
        AlphaToBeta a2b = new AlphaToBeta (alphaIndexToTaz, alphaIndexToBeta);

        // create a MatrixCompression object and return the squeezed matrix
        MatrixCompression squeeze = new MatrixCompression(a2b);
        return squeeze.getCompressedMatrix( alphaZoneMatrix, "MEAN" );
        
	}


    
    /**
	 * write out beta zone time skim matrices and return the beta zone Matrix object
	 */
	public void writeBetaSkimMatrix ( Matrix alphaZoneMatrix, String betaFileName ) {

        // create a squeezed beta skims Matrix from the peak alpha distance skims Matrix and write to disk
        if( betaFileName != null ) {
            Matrix beta5000s = getBetaSkimMatrix ( alphaZoneMatrix );

            WorldZoneExternalZoneUtil wzUtil = new WorldZoneExternalZoneUtil(globalRb);
            Matrix beta6000s = wzUtil.createBeta6000Matrix(beta5000s);
            
            MatrixWriter mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(betaFileName) );
            mw.writeMatrix(beta6000s);
        }
        
	}


    
    private float[][] getZeroBasedFloatArray ( double[][] zeroBasedDoubleArray ) {

		// copy the double values to a float array, putting values in row,cols corresponding to the alpha+externals external number order.
		float[][] zeroBasedFloatArray = new float[numCentroids][numCentroids];

        int[] indexNode = nh.getIndexNode();
        
        for (int i=0; i < zeroBasedDoubleArray.length; i++) {
            // convert index to TAZ
            int exRow = indexNode[i];
            // convert TAZ to alphaIndex
            int inRow = tazToAlphaIndex[exRow];
            for (int j=0; j < zeroBasedDoubleArray[i].length; j++) {
                // convert index to TAZ
                int exCol = indexNode[j];
                // convert TAZ to alphaIndex
                int inCol = tazToAlphaIndex[exCol];
                
                // put value in return array by alpha zone indices for i,j.
                zeroBasedFloatArray[inRow][inCol] = (float)zeroBasedDoubleArray[i][j];
            }
        }
        
   	    return zeroBasedFloatArray;

    }


    /**
	 * build network skim arrays, return as double[][][].
	 * the highway network attributes on which to skim the network to produce multipe skim tables is passed in.
	 */
	private double[][][] buildHwySkimMatrices ( double[] linkCost, double[][] linkAttributes, boolean[] validLinks ) {
		
		
		double[][][] skimMatrices =  hwySkims ( linkCost, linkAttributes, validLinks );
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
	 * highway network skimming procedure for generating multiple skim tables
	 */
	private double[][][] hwySkims ( double[] linkCost, double[][] linkAttributes, boolean[] validLinks ) {

	    int i;
	    
        double[][] tempMatrices;
        double[][][] skimMatrices = new double[linkAttributes.length][numCentroids][];

		
		// create a ShortestPathTreeH object
		ShortestPathTreeH sp = new ShortestPathTreeH( nh );


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
	 * get average SOV trip skim values
	 *   returns average skimm value weighted by trips and the number of o/d pairs with trips but disconnected skim value
	 */
	public double[] getAvgTripSkims ( double[][] trips, Matrix skimMatrix, int[] indexNode ) {

		double disconnected = 0;
		double tripSkim = 0.0;
		double totalTrips = 0.0;
		float matrixValue;
		
        int maxElement = (int)skimMatrix.getMax() + 1;
        skimTripFreqs = new double[maxElement];
        
		for (int r=1; r <= skimMatrix.getRowCount(); r++) {
			for (int c=1; c <= skimMatrix.getColumnCount(); c++) {
				int i = r - 1;
				int j = c - 1;
                int iTaz = indexNode[i];
                int jTaz = indexNode[j];
                matrixValue = skimMatrix.getValueAt(iTaz,jTaz);
				if ( trips[i][j] > 0.0 ) {
					if ( matrixValue != Double.NEGATIVE_INFINITY ) {
						tripSkim += trips[i][j]*matrixValue;
						totalTrips += trips[i][j];
                        
                        skimTripFreqs[(int)matrixValue] += trips[i][j];
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


    public double[] getSkimTripFreqs () {
        return skimTripFreqs;
    }
   

	public static void main(String[] args) {

    	
    	logger.info ("creating Skims object.");
        Skims s = new Skims ( ResourceBundle.getBundle("ts"), ResourceBundle.getBundle ("global"), "peak" );

    	logger.info ("skimming network and creating Matrix object for peak auto time.");
        double[][] linkAttribs = new double[1][];
        linkAttribs[0] = s.nh.getCongestedTime();
        
        String[] matrixName = new String[1];
        matrixName[0] = "time";
        
        String[] matrixDescription = new String[1];
        matrixDescription[0] = "Skims.main() test time matrix";
        
        Matrix[] m = s.getHwySkimMatrices ( "peak", linkAttribs, matrixName, matrixDescription, 'a' );
        m[0].logMatrixStatsToInfo();

    	logger.info ("squeezing the alpha matrix to a beta matrix.");
    	Matrix mSqueezed = s.getBetaSkimMatrix( m[0] );
        mSqueezed.logMatrixStatsToInfo();
        
    }
}

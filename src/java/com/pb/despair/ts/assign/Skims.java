package com.pb.despair.ts.assign;

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
import com.pb.common.util.ResourceUtil;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.DataReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.MessageWindow;



public class Skims {

	protected static Logger logger = Logger.getLogger("com.pb.despair.ts.assign");

	MessageWindow mw;
	HashMap tsPropertyMap;
    HashMap globalPropertyMap;

    Network g;
    
	boolean useMessageWindow = false;
	

	int[] alphaNumberArray = null;
	int[] betaNumberArray = null;
	int[] zonesToSkim = null;

    int[] externalToAlphaInternal = null;
    int[] alphaExternalNumbers = null;
	
	Matrix newSkimMatrix;

    public Skims ( ResourceBundle tsRb, ResourceBundle globalRb, String timePeriod, float volumeFactor ) {

        tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(tsRb);
        globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

		String networkDiskObjectFile = (String)tsPropertyMap.get("NetworkDiskObject.file");
		
		// if no network DiskObject file exists, no previous assignments
		// have been done, so build a new Network object which initialize 
		// the congested time field for computing time related skims.
		if ( networkDiskObjectFile == null ) {
			g = new Network( tsPropertyMap, globalPropertyMap, timePeriod, volumeFactor );
		}
		// otherwise, read the DiskObject file and use the congested time field
		// for computing time related skims.
		else {
			g = (Network) DataReader.readDiskObject ( networkDiskObjectFile, "highwayNetwork_" + timePeriod );
		}
		

        initSkims ();

    }


    public Skims ( Network g, HashMap tsMap, HashMap globalMap ) {

        this.g = g;
        this.tsPropertyMap = tsMap;
        this.globalPropertyMap = globalMap;

        initSkims ();

    }



    private void initSkims () {

		// take a column of alpha zone numbers from a TableDataSet and puts them into an array for
	    // purposes of setting external numbers.	     */
		String zoneCorrespondenceFile = (String)globalPropertyMap.get("alpha2beta.file");
		try {
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(zoneCorrespondenceFile));
	        alphaNumberArray = table.getColumnAsInt( 1 );
	        betaNumberArray = table.getColumnAsInt( 2 );
        } catch (IOException e) {
            logger.fatal("Can't get zone numbers from zonal correspondence file");
            e.printStackTrace();
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
	 * write out peak and off-peak, alpha and beta SOV distance skim matrices
	 */
	public void writeSovDistSkimMatrices ( boolean[] validLinks ) {

		String fileName;
		MatrixWriter mw;
		Matrix mSqueezed;
		
		// set the highway network attribute on which to skim the network - distance in this case
		double[] linkCost = g.getDist();
		
        // get the skims as a double[][] array 
        double[][] zeroBasedDoubleArray = buildHwySkimMatrix( linkCost, validLinks );

        // copy the array to a ones-based float[][] for conversion to Matrix object
        float[][] zeroBasedFloatArray = getZeroBasedFloatArray ( zeroBasedDoubleArray );

	    // create a Matrix from the peak alpha distance skims array and write to disk
	    fileName = (String)tsPropertyMap.get( "pkHwyDistSkim.fileName" );
        newSkimMatrix = new Matrix( "pkdist", "Peak SOV Distance Skims", zeroBasedFloatArray );
	    newSkimMatrix.setExternalNumbersZeroBased( alphaNumberArray );
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(newSkimMatrix);

	    // create a squeezed beta skims Matrix from the peak alpha distance skims Matrix and write to disk
	    fileName = (String)tsPropertyMap.get( "pkHwyDistBetaSkim.fileName" );
        mSqueezed = getSqueezedMatrix(newSkimMatrix);
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(mSqueezed);
        
	    // create a Matrix from the off-peak alpha distance skims array and write to disk
	    fileName = (String)tsPropertyMap.get( "opHwyDistSkim.fileName" );
        newSkimMatrix = new Matrix( "opdist", "Off-peak SOV Distance Skims", zeroBasedFloatArray );
	    newSkimMatrix.setExternalNumbers( alphaExternalNumbers );
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(newSkimMatrix);

	}


    /**
	 * write out peak alpha and beta SOV time skim matrices
	 */
	public void writePeakSovTimeSkimMatrices ( boolean[] validLinks ) {

		String fileName;
		MatrixWriter mw;
		Matrix mSqueezed;
		
		// set the highway network attribute on which to skim the network - congested time in this case
		double[] linkCost = g.getCongestedTime();
		
        // get the skims as a double[][] array dimensioned to number of centroids (2984)
        double[][] zeroBasedDoubleArray = buildHwySkimMatrix( linkCost, validLinks );

        // convert to a float[][] dimensioned to number of alpha zones (2950)
        float[][] zeroBasedFloatArray = getZeroBasedFloatArray ( zeroBasedDoubleArray );

	    // create a Matrix from the peak alpha congested time skims array and write to disk
	    fileName = (String)tsPropertyMap.get( "pkHwyTimeSkim.fileName" );
        newSkimMatrix = new Matrix( "pktime", "Peak SOV Time Skims", zeroBasedFloatArray );
	    newSkimMatrix.setExternalNumbers( alphaExternalNumbers );
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(newSkimMatrix);

	    // create a squeezed beta skims Matrix from the peak alpha distance skims Matrix and write to disk
	    fileName = (String)tsPropertyMap.get( "pkHwyTimeBetaSkim.fileName" );
        mSqueezed = getSqueezedMatrix(newSkimMatrix);
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(mSqueezed);
        
	}


    /**
	 * write out off-peak alpha and beta SOV time skim matrices
	 */
	public void writeOffPeakSovTimeSkimMatrices ( boolean[] validLinks ) {

		String fileName;
		MatrixWriter mw;

		// set the highway network attribute on which to skim the network - free flow time in this case
		double[] linkCost = g.getCongestedTime();
		
        // get the skims as a double[][] array 
        double[][] zeroBasedDoubleArray = buildHwySkimMatrix( linkCost, validLinks );

        float[][] zeroBasedFloatArray = getZeroBasedFloatArray ( zeroBasedDoubleArray );

	    // create a Matrix from the off-peak alpha congested time skims array and write to disk
	    fileName = (String)tsPropertyMap.get( "opHwyTimeSkim.fileName" );
        newSkimMatrix = new Matrix( "optime", "Off-peak SOV Time Skims", zeroBasedFloatArray );
	    newSkimMatrix.setExternalNumbers( alphaExternalNumbers );
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(newSkimMatrix);

	}


    /**
	 * get peak alpha zone SOV distance skim matrix
	 */
	public Matrix getSovDistSkimAsMatrix (int userClass) {

		// set the highway network attribute on which to skim the network - distance in this case
		double[] linkCost = g.getDist();
		
        // get the skims as a double[][] array
		// skims are generated between all centroids in entire network (2985 total centroids)
        double[][] zeroBasedDoubleArray = buildHwySkimMatrix( linkCost, g.getValidLinkForClass(userClass) );

        float[][] zeroBasedFloatArray = getZeroBasedFloatArray ( zeroBasedDoubleArray );

	    // create a Matrix from the peak alpha distance skims array and return
        newSkimMatrix = new Matrix( "pkdist", "Peak SOV Distance Skims", zeroBasedFloatArray );
	    newSkimMatrix.setExternalNumbers( alphaExternalNumbers );

	    return newSkimMatrix;
	    
	}


    /**
	 * get peak alpha zone SOV distance skim array
	 */
	public double[][] getSovDistSkims ( boolean[] validLinks ) {

		// set the highway network attribute on which to skim the network - distance in this case
		double[] linkCost = g.getDist();
		
        // get the skims as a double[][] array 
        double[][] zeroBasedDoubleArray = buildHwySkimMatrix( linkCost, validLinks );

	    return zeroBasedDoubleArray;
	    
	}


    
    /**
	 * get average SOV trip travel skim values: (dist,time).
	 */
	public double[] getAvgSovTripSkims ( double[][] trips, boolean[] validLinks ) {

		// set the highway network attribute on which to skim the network - distance in this case
		double[] linkCost = g.getDist();
		
		// get the external zone number correspondence array
		int[] indexNode = g.getIndexNode();
		
		
        // get the skims as a double[][] array 
        double[][] zeroBasedDistArray = buildHwySkimMatrix( linkCost, validLinks );

        double disconnected = 0;
    	double tripMiles = 0.0;
        double totalTrips = 0.0;
        for (int i=0; i < zeroBasedDistArray.length; i++) {
            for (int j=0; j < zeroBasedDistArray.length; j++) {
            	if ( trips[i][j] > 0.0 ) {
                	if ( zeroBasedDistArray[i][j] != Double.NEGATIVE_INFINITY ) {
                		tripMiles += trips[i][j]*zeroBasedDistArray[i][j];
                    	totalTrips += trips[i][j];
                	}
                	else {
                		disconnected ++;
                	}
            	}
            }
        }


        
        
		// set the highway network attribute on which to skim the network - distance in this case
		linkCost = g.getCongestedTime();
		
        // get the skims as a double[][] array 
        double[][] zeroBasedTimeArray = buildHwySkimMatrix( linkCost, validLinks );

    	double tripMinutes = 0.0;
        for (int i=0; i < zeroBasedTimeArray.length; i++)
            for (int j=0; j < zeroBasedTimeArray.length; j++)
            	if ( zeroBasedTimeArray[i][j] != Double.NEGATIVE_INFINITY )
            		tripMinutes += trips[i][j]*zeroBasedTimeArray[i][j];

            	
            	
        double[] results = new double[3];
        results[0] = tripMiles/totalTrips;
        results[1] = tripMinutes/totalTrips;
        results[2] = disconnected;
        
        return results;

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
	 * build network skim array, return as double[][].
	 * the highway network attribute on which to skim the network is passed in.
	 */
	private double[][] buildHwySkimMatrix ( double[] linkCost, boolean[] validLinks ) {
		
		
		double[][] skimMatrix =  hwySkim ( linkCost, validLinks, alphaNumberArray );
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
	 * highway network skimming procedure
	 */
	private double[][] hwySkim ( double[] linkCost, boolean[] validLinks, int[] alphaNumberArray ) {

	    int i;
	    
	    
	    
		double[][] skimMatrix = new double[g.getNumCentroids()][];

		// create a ShortestPathTreeH object
		ShortestPathTreeH sp = new ShortestPathTreeH( g );


		// build shortest path trees and get distance skims for each origin zone.
		sp.setLinkCost( linkCost );
		sp.setValidLinks( validLinks );
		
		
		for (i=0; i < g.getNumCentroids(); i++) {
			if (useMessageWindow) mw.setMessage2 ( "Skimming shortest paths from zone " + (i+1) + " of " + alphaNumberArray.length + " zones." );

			if (i % 500 == 0)
				logger.info ("shortest path tree for origin zone index " + i);

			sp.buildTree( i );
			skimMatrix[i] = sp.getSkim();
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
	
	

	public static void main(String[] args) {

    	
    	ResourceBundle rb = ResourceUtil.getPropertyBundle( new File("/jim/util/svn_workspace/projects/tlumip/config/ts.properties") );
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File("/jim/util/svn_workspace/projects/tlumip/config/global.properties"));
    	logger.info ("creating Skims object.");
        Skims s = new Skims ( rb, globalRb, "peak", 0.5f );

    	logger.info ("skimming network and creating Matrix object for userclass 0 ('a').");
        Matrix m = s.getSovDistSkimAsMatrix(0);

    	logger.info ("squeezing the alpha matrix to a beta matrix.");
        Matrix mSqueezed = s.getSqueezedMatrix(m);

        
    }
}

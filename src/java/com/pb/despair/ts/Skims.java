package com.pb.despair.ts;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.JFrame;

import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixViewer;
import com.pb.common.util.ResourceUtil;

import com.pb.common.assign.Network;
import com.pb.common.assign.ShortestPathTreeH;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.DataReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.MessageWindow;



public class Skims {

	protected static Logger logger = Logger.getLogger("com.pb.despair.ts");

	MessageWindow mw;
	HashMap propertyMap;
	
	boolean useMessageWindow = true;
	
    Network g;

    int[] alphaNumberArray = null;
	int[] betaNumberArray = null;

	Matrix newSkimMatrix;

    public Skims (int timePeriod, ResourceBundle rb) {

        propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

		String networkDiskObjectFile = (String)propertyMap.get("NetworkDiskObject.file");
		
		// if no network DiskObject file exists, no previous assignments
		// have been done, so build a new Network object which initialize 
		// the congested time field for computing time related skims.
		if ( networkDiskObjectFile == null ) {
			g = new Network( propertyMap );
		}
		// otherwise, read the DiskObject file and use the congested time field
		// for computing time related skims.
		else {
			g = (Network) DataReader.readDiskObject ( networkDiskObjectFile, "highwayNetwork_" + timePeriod );
		}
		

	    // take a column of alpha zone numbers from a TableDataSet and puts them into an array for
	    // purposes of setting external numbers.	     */
		String zoneCorrespondenceFile = (String)propertyMap.get("zoneIndex.fileName");
		try {
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(zoneCorrespondenceFile));
	        alphaNumberArray = table.getColumnAsInt( 1 );
	        betaNumberArray = table.getColumnAsInt( 2 );
        } catch (IOException e) {
            logger.severe("Can't get zone numbers from zonal correspondence file");
            e.printStackTrace();
        }
	    

		if ( useMessageWindow ) {
			this.mw = new MessageWindow ( "Shortest Path Tree Skimming Progress" );
		}

    }




    /**
	 * return sov distance skims as Matrix
	 */
	public Matrix getSovDistSkimAsMatrix () {

        // get the skims as an array 
        float[][] zeroBasedFloatArray = getSovDistSkimAsFloatArray();

		float[][] onesBasedFloatArray = new float[zeroBasedFloatArray.length+1][zeroBasedFloatArray.length+1];
	    for (int i=1; i < onesBasedFloatArray.length; i++) {
           for (int j=1; j < onesBasedFloatArray[i].length; j++)
           	  onesBasedFloatArray[i][j] = zeroBasedFloatArray[i-1][j-1];
	    }
        
	    // create a Matrix from the skims array
	    newSkimMatrix = new Matrix( "", "SOV Distance Skims", onesBasedFloatArray );
	    newSkimMatrix.setExternalNumbersZeroBased( alphaNumberArray );
	    
	    return newSkimMatrix;
	}


    /**
	 * return sov time skims as Matrix
	 */
	public Matrix getSovTimeSkimAsMatrix () {

        // get the skims as an array 
        float[][] tempFloatArray = getSovTimeSkimAsFloatArray();
    
	    // create a Matrix from the skims array
        Matrix newMatrix = new Matrix(tempFloatArray);
        newMatrix.setExternalNumbers(alphaNumberArray);        
	    
	    return newMatrix;
	}


    /**
	 * return sov distance skims as float[][]
	 */
	public float[][] getSovDistSkimAsFloatArray () {

		double[][] tempDoubleArray = buildSovDistanceSkimMatrix();
		float[][] tempFloatArray = new float[tempDoubleArray.length][];
	    
	    
	    for (int i=0; i < tempDoubleArray.length; i++) {
           tempFloatArray[i] = new float[tempDoubleArray[i].length];
           for (int j=0; j < tempDoubleArray[i].length; j++)
               tempFloatArray[i][j] = (float)tempDoubleArray[i][j];
	    }
	    
	    return tempFloatArray;
	}


    /**
	 * return sov time skims as float[][]
	 */
	public float[][] getSovTimeSkimAsFloatArray () {

		double[][] tempDoubleArray = buildSovTimeSkimMatrix();
		float[][] tempFloatArray = new float[tempDoubleArray.length][];
	    
	    
	    for (int i=0; i < tempDoubleArray.length; i++) {
           tempFloatArray[i] = new float[tempDoubleArray[i].length];
           for (int j=0; j < tempDoubleArray[i].length; j++)
               tempFloatArray[i][j] = (float)tempDoubleArray[i][j];
	    }
	    
	    return tempFloatArray;
	}


	/**
	 * build sov distance skims as double[][]
	 */
	private double[][] buildSovDistanceSkimMatrix () {

		// set the highway network attribute on which to skim the network
		double[] linkCost = g.getDist();
		
		// specify which links are valid parts of paths for this skim matrix
		boolean[] validLinks = new boolean[g.getLinkCount()];
		Arrays.fill (validLinks, false);
		String[] mode = g.getMode();
		for (int i=0; i < validLinks.length; i++) {
			if ( mode[i].indexOf('a') >= 0 )
				validLinks[i] = true;
		}
		
		return hwySkim ( linkCost, validLinks );
       
	}


	/**
	 * build sov time skims as double[][]
	 */
	private double[][] buildSovTimeSkimMatrix () {

		// set the highway network attribute on which to skim the network
		double[] linkCost = g.getCongestedTime();
		
		// specify which links are valid parts of paths for this skim matrix
		boolean[] validLinks = new boolean[g.getLinkCount()];
		Arrays.fill (validLinks, false);
		String[] mode = g.getMode();
		for (int i=0; i < validLinks.length; i++) {
			if ( mode[i].indexOf('a') >= 0 )
				validLinks[i] = true;
		}
		
		return hwySkim ( linkCost, validLinks );
       
	}


	/**
	 * highway network skimming procedure
	 */
	private double[][] hwySkim ( double[] linkCost, boolean[] validLinks ) {

	    int i;
		double[][] skimMatrix = new double[g.getNumCentroids()][];

		// create a ShortestPathTreeH object
		ShortestPathTreeH sp = new ShortestPathTreeH( g );


		// build shortest path trees and get distance skims for each origin zone.
		sp.setLinkCost( linkCost );
		sp.setValidLinks( validLinks );
		
		
		for (i=0; i < g.getNumCentroids(); i++) {
			if (useMessageWindow) mw.setMessage2 ( "Skimming shortest paths from zone " + (i+1) + " of " + g.getNumCentroids() + " zones." );
			sp.buildTree( i );
			skimMatrix[i] = sp.getSkim();
		}

		return skimMatrix;
        
	}


	private Matrix getSqueezedMatrix (Matrix aMatrix) {
		
        // alphaNumberArray and betaNumberArray were read in from correspondence file and are zero-based
        AlphaToBeta a2b = new AlphaToBeta (alphaNumberArray, betaNumberArray);

        // create a MatrixCompression object and return the squeezed matrix
        MatrixCompression squeeze = new MatrixCompression(a2b);
        return squeeze.getCompressedMatrix(aMatrix, "MEAN");
        
	}
	
	

	public static void main(String[] args) {

    	
    	ResourceBundle rb = ResourceUtil.getPropertyBundle( new File("/jim/util/svn_workspace/projects/tlumip/config/skim.properties") );

    	logger.info ("creating Skims object.");
        Skims s = new Skims ( 0, rb );

    	logger.info ("skimming network and creating Matrix object.");
        Matrix m = s.getSovDistSkimAsMatrix();

    	logger.info ("squeezing the alpha matrix to a beta matrix.");
        Matrix mSqueezed = s.getSqueezedMatrix(m);

        
    	// use a MatrixViewer to examione the skims matrices created here
	    JFrame frame = new JFrame("MatrixViewer - " + m.getDescription());
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
        MatrixViewer matrixContentPane = new MatrixViewer( mSqueezed );

	    matrixContentPane.setOpaque(true); //content panes must be opaque
	    frame.setContentPane(matrixContentPane);
	
	    frame.pack();
	    frame.setVisible(true);

    }
}

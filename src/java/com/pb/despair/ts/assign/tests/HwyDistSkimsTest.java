package com.pb.despair.ts.assign.tests;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 5/7/2004
 */


import com.pb.despair.ts.assign.Network;
import com.pb.despair.ts.assign.Skims;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Logger;



public class HwyDistSkimsTest {

	protected static Logger logger = Logger.getLogger("com.pb.despair.ts.assign.tests");

	static final String DATABANK = "c:\\jim\\tlumip\\TLUMIPEmme2\\emme2ban";
	static final String CSVFILE = "c:\\jim\\tlumip\\TLUMIPEmme2\\sovDistSkimPk.csv";
    
	static ResourceBundle rb;	
	static HashMap propertyMap;
	static Network g = null;
	
	PrintWriter outStream = null;
	
	
	public HwyDistSkimsTest() {

    	rb = ResourceUtil.getPropertyBundle( new File("/jim/util/svn_workspace/projects/tlumip/config/ts.properties") );
		propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

	}
    
	
    public static void main (String[] args) {
    	
    	String period = "peak";
        
		long startTime = System.currentTimeMillis();
		
		HwyDistSkimsTest test = new HwyDistSkimsTest();
        
        g = new Network( propertyMap, period );
		logger.info ("done building Network object.");
        
        Skims sk = new Skims( g, propertyMap );
		logger.info ("done building Skims object.");


		Matrix distSkimMatrix = sk.getSovDistSkimAsMatrix();
		logger.info ("done computing peak sov distance skims.");

		
		MatrixReader mr = MatrixReader.createReader(MatrixType.EMME2, new File(DATABANK));
		Matrix databankMatrix = mr.readMatrix( "mf12");
		logger.info ("done reading peak sov distance skims from databank.");
		
		
		test.writeSkimsToCsv ( distSkimMatrix, databankMatrix );
		

		
		logger.info("HwyDistSkimsTest() finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");
    }


    
    private void writeSkimsToCsv ( Matrix distSkimMatrix, Matrix databankMatrix ) {
        

		int[] indexNode = g.getIndexNode();

		try {
			    
			// open output stream for .csv output file
			outStream = new PrintWriter (new BufferedWriter( new FileWriter(CSVFILE) ) );

			// write file header
			outStream.println( "orig,dest,modelSkim,emme2Skim,diff" );
			
			
			// write out .csv file records for selected o/d pairs
			int count=0;
			int diffCount = 0;
			int minR=0, maxR=0, minC=0, maxC=0;
			float diff = 0.0f;
			float maxDiff = 0.0f;
			float minDiff = 9999999.9f;
			for (int r=0; r < distSkimMatrix.getRowCount(); r++) {
			    
				for (int c=0; c < distSkimMatrix.getColumnCount(); c++) {

				    if ( r % 500 == 0 && c % 500 == 1 ) {
						logger.info ( r + "," + c + "," + indexNode[r] + "," + indexNode[c] + "," + distSkimMatrix.getValueAt(indexNode[r],indexNode[c]) +  "," + databankMatrix.getValueAt(indexNode[r],indexNode[c]) );
				    }

					diff = distSkimMatrix.getValueAt(indexNode[r],indexNode[c]) - databankMatrix.getValueAt(indexNode[r],indexNode[c]);
					
					count++;
					if ( Math.abs(diff) > 0.001 )
					    diffCount++;
					
					if (diff > maxDiff) {
					    maxDiff = diff;
					    maxR = r;
					    maxC = c;
					}
					else if (diff < minDiff) {
					    minDiff = diff;
					    minR = r;
					    minC = c;
					}
					
//					outStream.println( indexNode[r] + "," + indexNode[c] + "," + distSkimMatrix.getValueAt(indexNode[r],indexNode[c]) +  "," + databankMatrix.getValueAt(indexNode[r],indexNode[c]) + "," + diff );
				}
			    
			}
			
			if ( Math.abs(minDiff) > Math.abs(maxDiff) ) {
				maxDiff = Math.abs(minDiff);
				maxR = minR;
				maxC = minC;
			}
					    
			logger.info ( "Maximum difference=" + maxDiff + ", occurred at o,d=" + indexNode[maxR] + "," + indexNode[maxC] + "." );
			logger.info ( "computedSkim[" + indexNode[maxR] + "][" + indexNode[maxC] + "] = " + distSkimMatrix.getValueAt(indexNode[maxR],indexNode[maxC]) );
			logger.info ( "databankSkim[" + indexNode[maxR] + "][" + indexNode[maxC] + "] = " + databankMatrix.getValueAt(indexNode[maxR],indexNode[maxC]) );
			logger.info ( diffCount + " o/d pairs out of " + count + " are different by +/- 0.001 or more.");

			outStream.close();

        }
		catch (IOException e) {
		    
			logger.severe ("error occured writing to " + CSVFILE );
			   
		}
    }
    
}
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
package com.pb.tlumip.ts.assign.tests;

/**
 *
 * @author    Jim Hicks
 * @version   1.0, 5/7/2004
 */


import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.ts.NetworkHandler;
import com.pb.tlumip.ts.NetworkHandlerIF;
import com.pb.tlumip.ts.assign.Skims;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ResourceBundle;


public class HwyDistSkimsTest {

	protected static Logger logger = Logger.getLogger("com.pb.tlumip.ts.assign.tests");

	static final String DATABANK = "c:\\jim\\tlumip\\TLUMIPEmme2\\emme2ban";
	static final String CSVFILE = "c:\\jim\\tlumip\\TLUMIPEmme2\\sovDistSkimPk.csv";
    
	static HashMap tsPropertyMap;
    static HashMap globalPropertyMap;
	static NetworkHandlerIF nh = null;
	
    static ResourceBundle rb;
    static ResourceBundle globalRb;
    
	PrintWriter outStream = null;
	
	
	public HwyDistSkimsTest() {

    	rb = ResourceUtil.getPropertyBundle( new File("/jim/util/svn_workspace/projects/tlumip/config/ts.properties") );
		tsPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

        globalRb = ResourceUtil.getPropertyBundle( new File("/jim/util/svn_workspace/projects/tlumip/config/global.properties") );
		globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

	}
    
	
    public static void main (String[] args) {
    	
		long startTime = System.currentTimeMillis();
		
    	String period = "peak";
        
		HwyDistSkimsTest test = new HwyDistSkimsTest();
		
        nh = NetworkHandler.getInstance();
        test.setupNetwork( nh, tsPropertyMap, globalPropertyMap, period );

		logger.info ("done building Network object.");
        
        Skims sk = new Skims( nh, rb, globalRb );
		logger.info ("done building Skims object.");


        double[][] linkAttribs = new double[1][];
        linkAttribs[0] = nh.getDist();
        
        String[] matrixName = new String[1];
        matrixName[0] = "dist";
        
        String[] matrixDescription = new String[1];
        matrixDescription[0] = "Skims.main() test dist matrix";
        
        Matrix[] distSkimMatrix = sk.getHwySkimMatrices ( "peak", linkAttribs, matrixName, matrixDescription, 'a' );
		logger.info ("done computing peak sov distance skims.");

		
		MatrixReader mr = MatrixReader.createReader(MatrixType.EMME2, new File(DATABANK));
		Matrix databankMatrix = mr.readMatrix( "mf12");
		logger.info ("done reading peak sov distance skims from databank.");
		
		
		test.writeSkimsToCsv ( distSkimMatrix[0], databankMatrix );
		

		
		logger.info("HwyDistSkimsTest() finished in " +
			((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");
    }


    
    private void writeSkimsToCsv ( Matrix distSkimMatrix, Matrix databankMatrix ) {
        

		int[] indexNode = nh.getIndexNode();

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
		    
			logger.fatal ("error occured writing to " + CSVFILE );
            e.printStackTrace();

		}
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
            volumeFactor = (String)globalMap.get("am.peak.volume.factor");
        else if ( timePeriod.equalsIgnoreCase( "offpeak" ) )
            volumeFactor = (String)globalMap.get("offpeak.volume.factor");
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

        String walkSpeed = (String)globalMap.get( "sdt.walk.mph" );
        
        
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

}

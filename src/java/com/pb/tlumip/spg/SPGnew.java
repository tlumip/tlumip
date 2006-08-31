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
package com.pb.tlumip.spg;

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.TableDataSet;
import com.pb.models.synpop.SPG;
import com.pb.tlumip.model.IncomeSize;
import com.pb.tlumip.model.Industry;
import com.pb.tlumip.model.Occupation;

import java.util.HashMap;
import java.util.ResourceBundle;


/**
 * The SPG class is used to produce a set of synthetic households
 * from PUMS data consistent with employment forecasted by ED and
 * assigned to alpha zones.
 * 
 * The procedure is implemented in 2 parts:
 * SPG1 - extract a set of PUMS households consistent with ED employment forecasts
 * SPG2 - using output from PI, allocate households to alpha zones 
 *
 */

public class SPGnew extends SPG {

    public SPGnew ( String spgPropertyFileName, String globalPropertyFileName, String baseYear, String currentYear ) {
		
        super();
        
        HashMap spgPropertyMap = ResourceUtil.getResourceBundleAsHashMap( spgPropertyFileName );
        HashMap globalPropertyMap = ResourceUtil.getResourceBundleAsHashMap(globalPropertyFileName);

        spgNewInit( spgPropertyMap, globalPropertyMap, baseYear, currentYear );

    }
    

    public SPGnew ( ResourceBundle appRb, ResourceBundle globalRb, String baseYear, String currentYear ) {
        
        super();
        
        HashMap spgPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(appRb);
        HashMap globalPropertyMap = ResourceUtil.changeResourceBundleIntoHashMap(globalRb);

        spgNewInit( spgPropertyMap, globalPropertyMap, baseYear, currentYear );

    }

	
    private void spgNewInit( HashMap spgPropertyMap, HashMap globalPropertyMap, String baseYear, String currentYear ) {
        
        IncomeSize incSize = new IncomeSize( Double.parseDouble( (String)spgPropertyMap.get("convertTo2000Dollars") ) );
        Industry ind = new Industry( (String)globalPropertyMap.get("sw_pums_industry.correspondence.fileName"), baseYear );
        Occupation occ = new Occupation( (String)globalPropertyMap.get("sw_pums_occupation.correspondence.fileName"), baseYear );
        
        spgInit ( spgPropertyMap, globalPropertyMap, incSize, ind, occ, currentYear );
        
    }
    

	// the following main() is used to test the methods implemented in this object.
    public static void main (String[] args) {
        
		long startTime = System.currentTimeMillis();
		
        String which = args[0];
        String baseYear = args[1];  // in some calibration scenarios the base
                                    // year could be 1990, in other scenarios the base
                                    // year could be 2000.
        String tInterval = args[2]; // regardless of the base year, the first year simulation
                                    // will be t=1 so the first year employment data will either
                                    // be read from 1991 or 2001 depending on the baseyear argument,
                                    // and the PUMS data will be 1990 if t<10, otherwise it will be 2000.
        
        String currentYear = Integer.toString((Integer.valueOf(baseYear) + Integer.valueOf(tInterval)));
        
        String appPropertiesFile = args[3];
        String globalPropertiesFile = args[4];

        
        SPGnew testSPG = new SPGnew( appPropertiesFile, globalPropertiesFile, baseYear, currentYear );

        
        if(which.equals("spg1"))
		{
            testSPG.getHHAttributesFromPUMS( baseYear );
            testSPG.spg1(currentYear);
            TableDataSet table = testSPG.sumHouseholdsByIncomeSize();
            testSPG.writePiInputFile(table);
            testSPG.writeFreqSummaryToCsvFile();
            testSPG.writeFinalHhArrayToCsvFile();
            logger.info("SPG1 finished in " + ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");
            startTime = System.currentTimeMillis();
        
        }
        else {

            testSPG.spg2();
            logger.info("SPG2 finished in " + ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");
            startTime = System.currentTimeMillis();

            testSPG.writeHHOutputAttributesFromPUMS( baseYear );
            logger.info("writing SynPop files finished in " + ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");

        }
    }

}


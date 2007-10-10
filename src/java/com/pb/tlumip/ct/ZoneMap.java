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
package com.pb.tlumip.ct;

// ZoneMap.java provides storage for a matrix of beta-to-alpha zone equivalen-
// cies. It is used in DiscreteShipments (and possibly other places) to syn-
// thetically allocate the flows from a given beta zone to one of its
// constituent alpha zones. Most of the work takes place in the constructor,
// which reads the data and calculates cumulative probabilities for each beta
// zone (row). Only one public method is provided, which given a beta zone
// returns the alpha zone associated with it.
// @author "Rick Donnelly <rdonnelly@pbtfsc.com>"
// @version "0.9, 15/08/04"

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.AlphaToBeta;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class ZoneMap {
    protected static Logger logger = Logger.getLogger("com.pb.tlumip.ct");
    public static int HIGHEST_ALPHA_ZONENUMBER,
                       HIGHEST_BETA_ZONENUMBER ; //initialized after reading alphaToBeta.csv
    float[][] intensityMap;   // rows=beta zones, columns = alpha zones
    Random rn;

    public ZoneMap (File f, long randomSeed) {
        rn = new Random(randomSeed);
        AlphaToBeta a2b= new AlphaToBeta(f);
        HIGHEST_ALPHA_ZONENUMBER = a2b.getMaxAlphaZone();
        HIGHEST_BETA_ZONENUMBER = a2b.getMaxBetaZone();
        intensityMap = new float[HIGHEST_BETA_ZONENUMBER+1][HIGHEST_ALPHA_ZONENUMBER+1];

        fillIntensityMap(f);
        normaliseRows();
    }


    private void fillIntensityMap(File f){
        TableDataSet table = null;
        CSVFileReader reader = new CSVFileReader();
        try {
            table = reader.readFile(f);
        } catch (IOException e) {
            logger.fatal(f.getAbsolutePath() + " could not be found - check path");
            e.printStackTrace();
        }

        for(int r=1; r <= table.getRowCount(); r++){
            //read in the values of interest from the alpha2beta.csv file
            int azone = (int)table.getValueAt(r, "AZone");
            int bzone = (int)table.getValueAt(r, "BZone");
            float gridAcres = table.getValueAt(r, "GridAcres");
            String LUIntensity = table.getStringValueAt(r, "LUIntensityCode");

            //double check that you have valid values for these variables.
            if(azone <= 0 || bzone <=0 || gridAcres <= 0 || LUIntensity == null){
                logger.fatal("Incorrect value in the alpha2beta file - check row " + r);
                logger.fatal("zone numbers and gridAcres should be greater than 0 and" +
                        "LUIntensity string cannot be null");
            }

            //translate the LUIntensity string into a numeric value.
            int LUIntensityValue = 1;

            if (LUIntensity.equalsIgnoreCase("Low")) LUIntensityValue = 7;
            else if (LUIntensity.equalsIgnoreCase("Medium")) LUIntensityValue = 14;
            else if (LUIntensity.equalsIgnoreCase("High")) LUIntensityValue = 21;

            //fill in the intensity map with the appropriate values
            intensityMap[bzone][azone] = Math.max(1.0F, LUIntensityValue*gridAcres);
        }
    }

  // We'll eventually use a random number to choose from among candidate alpha
  // zones, so for each beta zone (row) we'll have to first normalise the
  // intensity of each alpha zone (column), calculating its cumulative value
  // along the way (such that the last non-zero entry equals 1.0).
  private void normaliseRows () {
    float rowTotal, cumulative;
    for (int p=1; p<HIGHEST_BETA_ZONENUMBER+1; p++) {
      rowTotal = 0.0F;
      for (int q=1; q<HIGHEST_ALPHA_ZONENUMBER+1; q++)
        rowTotal += intensityMap[p][q];
      if (rowTotal==0.0F) continue;   // beta zone must be unused (not an error)
      cumulative = 0.0F;
      for (int q=1; q<HIGHEST_ALPHA_ZONENUMBER+1; q++) {
        if (intensityMap[p][q]==0.0F) continue;
        // DEBUG:
        //System.out.print("p="+p+" q="+q+" im="+intensityMap[p][q]);
        intensityMap[p][q] = (intensityMap[p][q]/rowTotal)+cumulative;
        cumulative = intensityMap[p][q];
        // DEBUG:
        //System.out.println(" im'="+intensityMap[p][q]);
      }
    }
  }

    public float[][] getIntensityMap() {
        return intensityMap;
    }


  // This is the only public method in this class. The calling program supplies
  // the commodity code (ignored for now, eventually will use employment assoc-
  // iated with sector producing the commodity to weight the intensity) and
  // and betazone, and receives back the alpha zone to allocate the flows to.
  public int getAlphaZone (int betaZone, String commodityCode) {
    float r = rn.nextFloat();
    int result = -1;
    for (int q=1; q<HIGHEST_ALPHA_ZONENUMBER+1; q++)
      if (r<intensityMap[betaZone][q]) {
        result = q;
        break;
      }
    //DEBUG:
    //System.out.println("r="+r+" result="+result);
    return result;
  }


  public static void main (String[] args) {
    ZoneMap zm = new ZoneMap(new File("/temp/data/alpha2beta.csv"), 5910772L);
    int i;
    for (int k=0; k<20; k++) {
      i = zm.getAlphaZone(3157, "UNDEFINED");
    }

      //Test for Christi's system
//      File f = new File("/models/tlumip/scenario_pleaseWork/reference/alpha2beta.csv");
//      ZoneMap zm = new ZoneMap(f ,5910772L);
//      zm.fillIntensityMap(f);
//
//      float[][] intensityMap = zm.getIntensityMap();
//      float[][] intMap2 = zm.getIntensityMapTest();
//
//      for(int r=0; r<intensityMap.length; r++){
//          for(int c=0; c< intMap2[0].length; c++){
//              if(intensityMap[r][c] != intMap2[r][c]){
//                  logger.info("maps do not agree in cell (" + r + ", " + c + ")");
//              }
//          }
//      }


  }

}

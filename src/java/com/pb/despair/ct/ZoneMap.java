package com.pb.despair.ct;

// ZoneMap.java provides storage for a matrix of beta-to-alpha zone equivalen-
// cies. It is used in DiscreteShipments (and possibly other places) to syn-
// thetically allocate the flows from a given beta zone to one of its
// constituent alpha zones. Most of the work takes place in the constructor,
// which reads the data and calculates cumulative probabilities for each beta
// zone (row). Only one public method is provided, which given a beta zone
// returns the alpha zone associated with it.
// @author "Rick Donnelly <rdonnelly@pbtfsc.com>"
// @version "0.9, 15/08/04"

import java.util.Random;
import java.util.StringTokenizer;
import java.io.*;

public class ZoneMap {
  private static final int HIGHEST_ALPHA_ZONENUMBER = 4141,
    HIGHEST_BETA_ZONENUMBER = 4141;
  float[][] intensityMap;   // rows=beta zones, columns = alpha zones
  Random rn;

  ZoneMap (File f, long randomSeed) {
    rn = new Random(randomSeed);
    intensityMap =
      new float[HIGHEST_BETA_ZONENUMBER+1][HIGHEST_ALPHA_ZONENUMBER+1];
    for (int p=0; p<HIGHEST_BETA_ZONENUMBER+1; p++)
      for (int q=0; q<HIGHEST_ALPHA_ZONENUMBER+1; q++)
        intensityMap[p][q] = 0.0F;
    readAlpha2Beta(f);
    normaliseRows();
  }

  // Read the file with alpha zone attributes, which includes its parent beta
  // zone. This method of course depends on the assumed structure of the alpha
  // zone attribute file. Regularly check that this method and that file are
  // still in sync! (Better yet, replace this eventually with method that reads
  // the header and dynamically figures out which columns to read from.)
  private void readAlpha2Beta (File f) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
      StringTokenizer st;
      String s, LUIntensityCode;
      int alpha, beta;
      float gridAcres, LUIntensityValue;
      br.readLine();   // skip the header record
      while ((s = br.readLine()) != null) {
        if (s.startsWith("#")) continue;  // comment record
        // read the record contents, skipping data we don't care about
        st = new StringTokenizer(s, ",");
        alpha = Integer.parseInt(st.nextToken());
        beta = Integer.parseInt(st.nextToken());
        // (Eventually) add assertion here to check that alpha and beta values
        // are less than highest zone number assumed
        st.nextToken();   // state code
        st.nextToken();   // county name
        st.nextToken();   // FIPS code
        st.nextToken();   // PUMA1pct
        st.nextToken();   // PUMA5pct
        gridAcres = Float.parseFloat(st.nextToken());
        LUIntensityCode = st.nextToken();
        // Translate the land use intensity code into a numeric value
        LUIntensityValue = 1;  // default
        if (LUIntensityCode.equals("Low")) LUIntensityValue = 7;
        if (LUIntensityCode.equals("Medium")) LUIntensityValue = 14;
        if (LUIntensityCode.equals("High")) LUIntensityValue = 21;
        // And finally, weight the intensity by the size of the area, placing
        // the result in the mapping table
        intensityMap[beta][alpha] = Math.max(1.0F, LUIntensityValue*gridAcres);
        // DEBUG:
        //System.out.println("a="+alpha+" b="+beta+" ga="+gridAcres+" luic="+
        //  LUIntensityCode+" luiv="+LUIntensityValue+" ="+intensityMap[beta][alpha]);
      }
      br.close();
    } catch (IOException e) { e.printStackTrace(); }
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
  }

}

package com.pb.despair.ct;


import java.io.*;
import java.util.*;
public class ModalDistributions {
  private static boolean DEBUG = false;
  List modes;
  int modeSize;
  HashMap hm;

  ModalDistributions (String filename) {
    modes = new ArrayList();
    hm = new HashMap();
    readDistributions (new File(filename));
    modeSize = modes.size();
  }

  // Read triangular distributions for commodity and mode of transport
  private void readDistributions (File f) {
    try {
      String source = f.getAbsolutePath();
      if (DEBUG) {
        System.out.println("[# ModalDistributions.readDistributions()]");
        System.out.println("Reading data from "+source+" (LM: "+
          new Date(f.lastModified())+")");
      }
      BufferedReader br = new BufferedReader(new FileReader(source));
      String s, commodity, modeOfTransport;
      StringTokenizer st;
      double mean, alpha, beta, modeShare;
      while ((s = br.readLine()) != null) {
        if (s.startsWith("#")) continue;   // skip comments
        // Parse whitespace as well as commas as tokenizers, making , truck
        // the same as ,truck -- allowing the user to code the input file with
        // in just about any comma delimiter format they want
        st = new StringTokenizer(s, ", ");
        commodity = st.nextToken();
        modeOfTransport = st.nextToken();
        mean = Double.parseDouble(st.nextToken());
        alpha = Double.parseDouble(st.nextToken());  // minimum value
        beta = Double.parseDouble(st.nextToken());  // maximum value
        modeShare = Double.parseDouble(st.nextToken());
        if (modeShare==0.000) continue;   // why bother?
        if (!modes.contains(modeOfTransport)) modes.add(modeOfTransport);
        // Eventually we'll want to catch cases where a mode-commodity combo
        // has been defined twice. For now, let the program crash...
        hm.put(commodity+modeOfTransport,
          new TriangularDistribution(modeOfTransport, mean, alpha, beta,
            modeShare));
      }
      br.close();
      if (DEBUG) {
        System.out.println("Modes found: "+modes);
        System.out.println("HashMap hm:\n"+hm);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Given the interzonal distance between commodity origin (production) and destination
  // (consumption), return the mode of transport that will carry it
  public String selectMode (String commodity, double distance) {
    return selectMode (commodity, distance, Math.random());
  }
  public String selectMode (String commodity, double distance, double rn) {
    if (DEBUG) System.out.println("\n[# ModalDistributions.selectMode("+
      commodity+","+distance+","+rn+")]");
    int m;   // index variable for modes
    double[] d = new double[modeSize];
    // Read the share-weighted densities (height of distribution at the given
    // distance) for each of the modes
    String key;
    double dsum = 0.0;
    if (DEBUG) System.out.println("Before normalization:");
    for (m=0; m<modeSize; m++) {
      key = commodity+modes.get(m);
      if (DEBUG) System.out.print("   key="+key);
      // Some commodity-mode combinations may be undefined, in which case we'll set them
      // equal to zero
      if (hm.containsKey(key))
        d[m] = ((TriangularDistribution)hm.get(key)).scaledDensity(distance);
      else d[m] = 0.0;
      dsum += d[m];
      if (DEBUG) System.out.println(", d="+d[m]+" dsum="+dsum);
    }
    // If the sum of the densities is zero then the wheels fell off somewhere...
    if (dsum==0.0) {
      //System.out.println("Error: sum of densities is zero for "+
      //  "selectMode("+commodity+","+distance+","+rn+")");
      //System.exit(4);
      // Assign the default mode of truck
      return "UNKNOWN";
    }
    // Normalize the densities so that we can use them with a random draw on
    // (0,1) and put them in a cumulative distribution
    if (DEBUG) System.out.println("Cumulative normalized distribution:");
    double floor = 0.0;
    for (m=0; m<modeSize; m++) {
      floor = d[m] = floor+(d[m]/dsum);
      if (DEBUG) System.out.println("   mode="+(String)modes.get(m)+" cf="+
        d[m]);
    }
    // Finally, select the mode based on where the random draw falls in the
    // cumulative distribution
    String choice = (String)modes.get(0);  // Start by assuming its in first interval
    for (m=1; m<modeSize; m++)
      if ((rn>=d[m-1]) && (rn<=d[m])) {
        choice = (String)modes.get(m);
        break;
      }
    return choice;
  }

  public List getTransportModes() {
    return modes;
  }

  public static void main (String[] args) {
    DEBUG = true;    // Just for testing purposes, shouldn't be visible outside
    ModalDistributions md = new ModalDistributions("ModalDistributionParameters.txt");
    // Should select truck
    System.out.println("At distance=17 mode="+md.selectMode("SCTG35", 17));
    // Should select rail
    System.out.println("At distance=920 mode="+md.selectMode("SCTG27", 920, 0.9));
    // Should crash the program
    System.out.println("At distance=-1 mode="+md.selectMode("SCTG14", -1));

  }

}

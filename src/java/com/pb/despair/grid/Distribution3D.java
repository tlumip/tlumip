package com.pb.tlumip.grid;

import java.io.*;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Distribution3D {
  HashMap hm;
  String label, source;

  Distribution3D (String s) {
    hm = new HashMap();
    label = s;
    source = null;
  }

  private static boolean startsNumeric (String s) {
    boolean b = true;
    s = s.trim();
    char c = s.charAt(0);
    if (c<'0' | c>'9') b = false;
    return b;
  }

  public void readData (String filename) {
    int[] lower = { 1999, 1995, 1990, 1980, 1970, 1960, 1950, 1940, 1900 };
    int[] upper = { 2000, 1998, 1994, 1989, 1979, 1969, 1959, 1949, 1939 };
    source = filename;

    try {
      BufferedReader br = new BufferedReader(new FileReader(filename));
      String s;
      StringTokenizer st;
      int alphaZone;
      while ((s = br.readLine()) != null) {
        if (startsNumeric(s)) {
          st = new StringTokenizer(s, ",");
          File f = new File("c:/temp/DistributionFiles/Scratch.csv");
          BufferedWriter bw = new BufferedWriter(new FileWriter(f));
          alphaZone = Integer.parseInt(st.nextToken());
          int[] value = new int[upper.length];
          int totalValue = 0;
          for (int n=0; n<value.length; n++) {
            value[n] = Integer.parseInt(st.nextToken());
            totalValue += value[n];
            bw.write(lower[n]+","+upper[n]+","+value[n]);
            bw.newLine();
          }
          if (totalValue==0) System.err.println("Error: total=0 for zone "+
            alphaZone);
          bw.flush();
          bw.close();

          Integer i = new Integer(alphaZone);
          hm.put(i, new Distribution2D(""+alphaZone, f.getPath()));
        }
      }
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public double drawSample (int index, double r) {
    return drawSample(index, r, r);
  }
  public double drawSample (int index, double r1, double r2) {
    Integer i = new Integer(index);
    return ((Distribution2D)hm.get(i)).drawSample(r1, r2);
  }

  public static void main (String[] args) {
    Distribution3D d = new Distribution3D("3-dimensional test");
    d.readData("YearBuilt.txt");
    int targetZone = 4141;
    double r1, r2;
    for (int n=0; n<30; n++) {
      r1 = Math.random();
      r2 = Math.random();
      double dx = d.drawSample(targetZone, r1, r2);
      System.out.println("t="+targetZone+" r1="+(float)r1+" r2="+(float)r2+
        " -> "+dx+" -> "+(int)(dx+0.5));
    }
  }

}

package com.pb.tlumip.ct;

public class LinearFunction {
  double slope, intercept;
  String label;

  LinearFunction (String l, double s, double i) {
    slope = s;
    intercept = i;
    label = l;
  }

  // Transform annual value (dollars) to annual tons using CFS97 Table 6 relationships
  public double transform (double annualValue) {
    return (slope*annualValue)+intercept;
  }

  public String toString () { return label+": slope="+slope+" intercept="+intercept+"\n"; }

}

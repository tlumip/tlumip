package com.pb.despair.ct;

public class TriangularDistribution {

  public final String label;
  public final double mode, mean, alpha, beta, share;

  TriangularDistribution (String l, double m, double a, double b, double s) {
    label = l;
    mean = m;
    alpha = a;
    beta = b;
    share = s;
    double x = (mean*3)-alpha-beta;
    // In some cases this results in a negative value for mode, which obviously
    // doesn't make sense in this application
    if (x>0.0) mode = x;   // better than a negative value
    else mode = mean;
  }

  // Returns the raw density (normally shouldn't be used alone, as it has not
  // been factored by each modes share of the total ton-miles for that commodity
  public double density (double distance) {
    double d = 0.0;
    if ((distance>=alpha) & (distance<=beta)) {
      if (distance<=mode)
        d = (2*(distance-alpha))/((beta-alpha)*(mode-alpha));
      else d = (2*(beta-distance))/((beta-alpha)*(beta-mode));
    }
    return d;
  }

  // Returns the moment in the distribution multiplied by this modes share of
  // the commodity total ton miles (from CFS97 Table 6, with some imputations
  // as necessary)
  public double scaledDensity (double d) {
    return density(d)*(share*100.0);
  }

  public String toString () {
    return ("label="+label+" a="+alpha+" b="+beta+" c="+mode+" mean="+mean+
      " share="+share+"\n");
  }

  public static void main (String[] args) {
    System.out.println("  Test with values from SCTG20 trucks (STK):");
    TriangularDistribution td = new TriangularDistribution("SCTG20STK", 639, 0,
      2700, 0.8);
    System.out.println("    calculated mode="+td.mode);
    System.out.println("    density(1205)="+td.density(1205));
    System.out.println("    scaledDensity(1205)="+td.scaledDensity(1205));
    System.out.println("    density(2818)="+td.density(2818));   // Should crash

    System.out.println("  Test with values from SCTG35 private trucks (STP):");
    td = new TriangularDistribution("SCTG35STP", 44.0526, 8, 89, 0.43);
    System.out.println("    calculated mode="+td.mode);
    System.out.println("    density(52)="+td.density(52));
    System.out.println("    scaledDensity(52)="+td.scaledDensity(52));
    System.out.println("    density(4)="+td.density(4));   // Should be zero
  }

}

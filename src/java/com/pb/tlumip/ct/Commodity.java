package com.pb.tlumip.ct;

import java.text.DecimalFormat;

class Commodity {
  private double totalProduction, totalConsumption;

  Commodity () {
    totalProduction = 0.0;
    totalConsumption = 0.0;
  }
  public void addProduction (double d) { totalProduction += d; }

  public void addConsumption (double d) { totalConsumption += d; }

  public double getTotalProduction () { return totalProduction; }
  public double getTotalConsumption () { return totalConsumption; }

  public String toString() {
    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(1);
    return "p="+df.format(totalProduction)+" c="+
      df.format(totalConsumption)+"\n";
  }

}

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


import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
class ValueDensityFunction {

  HashMap hm;

  ValueDensityFunction (File f) {
    hm = new HashMap();
    readFunctionParameters(f);
  }

  public void readFunctionParameters (File f) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(f));
      StringTokenizer st;
      String s, commodity, modeOfTransport, key;
      double slope, intercept;

      while ((s = br.readLine()) != null) {
        if (s.startsWith("#")) continue;   // skip comments
        st = new StringTokenizer(s,", ");
        commodity = st.nextToken();
        modeOfTransport = st.nextToken();
        slope = Double.parseDouble(st.nextToken());
        intercept = Double.parseDouble(st.nextToken());
        key = commodity+modeOfTransport;
        hm.put(key, new LinearFunction(key, slope, intercept));
      }
      br.close();
    } catch (IOException e) { e.printStackTrace(); }
  }


  public double getAnnualTons (String key, double value) {
    // This would be simple, except that when we carried out the estimation we used the same
    // units as the CFS: millions of dollars and thousands of tons. Here we're being fed
    // raw dollars, and we want to return raw tons. Thus, we'll also have to scale the
    // arguments into the correct metrics before doing the transform.
    double scaledValue = value/1e6;   // convert value into millions of value
    double thousandTons = ((LinearFunction)hm.get(key)).transform(scaledValue);
    double tons = thousandTons*1000.0;
    if (tons<0) tons = Double.NaN;   // Sure to attract attention
    return tons;
  }


  public static void main (String[] args) {
    ValueDensityFunction vdf = new ValueDensityFunction(new File("ValueDensityParameters.txt"));
    //System.out.println(vdf.hm);

    // Let's take it out for a spin around the block
    String key;
    double annualValue;
    DecimalFormat df = new DecimalFormat();
    df.setGroupingSize(0);
    df.setMaximumFractionDigits(1); df.setMinimumFractionDigits(1);

    key = "SCTG23STK";
    System.out.print("function: "+((LinearFunction)vdf.hm.get(key)).toString());
    annualValue = 10248475;
    System.out.println("key="+key+" v="+df.format(annualValue)+" getAnnualTons()="+
      df.format(vdf.getAnnualTons(key, annualValue)));

    key = "SCTG02STK";
    System.out.print("function: "+((LinearFunction)vdf.hm.get(key)).toString());
    annualValue = 902177;
    System.out.println("key="+key+" v="+df.format(annualValue)+" getAnnualTons()="+
      df.format(vdf.getAnnualTons(key, annualValue)));
  }


}

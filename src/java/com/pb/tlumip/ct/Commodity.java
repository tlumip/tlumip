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

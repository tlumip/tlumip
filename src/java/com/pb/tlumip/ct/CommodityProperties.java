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

import java.util.Random;
class CommodityProperties {

  TriangularDistribution td;       // distribution to read shipment size from

  double averageShipmentSize,
    maximumShipmentSize,
    shipmentThreshold,
    transShipmentProbability,
    privateCarriageProbability,
    transShipmentProximity;

  CommodityProperties (double a, double m, double s, double t, double p, double z) {
    averageShipmentSize = a;
    maximumShipmentSize = m;
    shipmentThreshold = s;
    transShipmentProbability = t;
    privateCarriageProbability = p;
    transShipmentProximity = z;
    td = new TriangularDistribution(" ", averageShipmentSize, 1.0, maximumShipmentSize, 1.0);
  }

  // This is a kluge at the moment, until I figure out how to sample from a cumulative probability
  // function of triangular distribution, which will return much more robust estimate
  public double getShipmentSize (double randomNumber) {
    return (randomNumber+0.5)*averageShipmentSize;
  }

  public String toString () {
    return "a="+averageShipmentSize+
      " m="+maximumShipmentSize+
      " st="+shipmentThreshold+
      " tsp="+transShipmentProbability+
      " pcp="+privateCarriageProbability+
      " z="+transShipmentProximity+"\n";
  }

  public static void main (String[] args) {
    // Test with the values for SCTG 01 truck (SCTG01STK)...
    CommodityProperties cp = new CommodityProperties(26939, 30662, 0.167, 0.398, 0.406, 1.0);
    System.out.println(cp);
    Random rn = new Random(1957L);
    double d;
    for (int n=0; n<20; n++) {
      d = rn.nextDouble();
      System.out.println("n="+n+" d="+d+": "+cp.getShipmentSize(d));
    }
  }

}

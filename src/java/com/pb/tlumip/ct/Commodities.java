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
import java.util.StringTokenizer;
import java.util.HashMap;

class Commodities {

  HashMap cp;

  Commodities (File f) {
    cp = new HashMap();
    readCommodityProperties(f);
  }

  private void readCommodityProperties (File f) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
      String s;
      StringTokenizer st;
      while ((s = br.readLine()) != null) {
        if (s.startsWith("#")) continue;    // skip comment records
        st = new StringTokenizer(s, ", ");
        cp.put(st.nextToken(),   // commodity code
          new CommodityProperties(
            Double.parseDouble(st.nextToken()),    // average shipment size
            Double.parseDouble(st.nextToken()),    // maximum shipment size
            Double.parseDouble(st.nextToken()),    // shipment threshold
            Double.parseDouble(st.nextToken()),    // transshipment probability
            Double.parseDouble(st.nextToken()),    // private carriage probability
            Double.parseDouble(st.nextToken()) ) );  // transshipment proximity
      }
      br.close();
    } catch (IOException e) { e.printStackTrace(); }
  }

  public double getShipmentSize (String commodityCode, double randomNumber) {
    return ((CommodityProperties)cp.get(commodityCode)).getShipmentSize(randomNumber);
  }

  public double getShipmentThreshold (String commodityCode) {
    return ((CommodityProperties)cp.get(commodityCode)).shipmentThreshold;
  }

  public double getTransShipmentProbability (String commodityCode) {
    return ((CommodityProperties)cp.get(commodityCode)).transShipmentProbability;
  }


  public static void main (String[] args) {
    String testFilename = "CommodityProperties.txt";
    Commodities c = new Commodities(new File(testFilename));
    System.out.println(c.cp);
  }



}

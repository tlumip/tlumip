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

import com.pb.common.util.SeededRandom;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.StringTokenizer;

class TruckSelector {
  // Define default parameters
    protected Logger logger = Logger.getLogger("com.pb.tlumip.ct");
  private final static int HIGHEST_COMMODITY_NUMBER = 49,
    MAX_RANDOM_DRAWS = 300;

  // Define structures to hold the vehicle type attributes
  private String[] tf$label;
  private float[] tf$grossVehicleWeight;
  private float[] tf$payloadCapacity;
  private float[] tf$shiftMinutes;
  private float[] tf$averageDwellTimes;

  // Define structures to hold the probabilities of each of the vehicle types
  private boolean[] isDefined;
  private float[] pPrivateCarriage;
  private float[][] pTruckType;


  TruckSelector (File vehicleTypeAttributes, File commodityVehicleTypes) {

    readVehicleTypeAttributes(vehicleTypeAttributes);
    readCommodityVehicleTypes(commodityVehicleTypes);
  }
  
  private void readVehicleTypeAttributes (File f) {
    try {
      int bufferSize = 122434;
      BufferedReader br = new BufferedReader(new FileReader(f), bufferSize);
      String s;
      StringTokenizer st;
      logger.info("Reading " + f.getAbsolutePath() + " length: " + f.length());

      // First determine how many non-commented records are in the file
      br.mark(bufferSize-1);
      int records = 0;
      while ((s = br.readLine()) != null) {
        if (s.startsWith("#")) continue;
        ++records;
      }

      // Now we can initialize the vehicle type structures to hold the data we
      // are about to read
      tf$label = new String[records];
      tf$grossVehicleWeight = new float[records];
      tf$payloadCapacity = new float[records];
      tf$shiftMinutes = new float[records];
      tf$averageDwellTimes = new float[records];

      // And finally, fill the attribute table with data
      br.reset();
      int index = 0;
      while ((s = br.readLine()) != null) {
        if (s.startsWith("#")) continue;
        st = new StringTokenizer(s, " ,");
        tf$label[index] = st.nextToken();
        tf$grossVehicleWeight[index] = Float.parseFloat(st.nextToken());
        tf$payloadCapacity[index] = Float.parseFloat(st.nextToken());
        tf$shiftMinutes[index] = Float.parseFloat(st.nextToken());
        tf$averageDwellTimes[index] = Float.parseFloat(st.nextToken());
        index++;
      }
      br.close();
    } catch (IOException e) { e.printStackTrace(); }
  }

  private void readCommodityVehicleTypes (File f) {
    // Get ready to populate probabilities table
    int nTypes = tf$label.length;
    int nClasses = HIGHEST_COMMODITY_NUMBER;  // shorter handle
    int n, p;    // loop counters
    isDefined = new boolean[nClasses];  //initialized to all false
    pTruckType = new float[nClasses][nTypes]; //initialized to 0
    pPrivateCarriage = new float[nClasses];   //initialized to 0
    

    // Read the data from external file, Note that we assume that the columns
    // in this file are in the same order as the rows in the vehicle type
    // attributes. This code isn't sophisticated enough to catch misalignment;
    // this is where XML will ready shine for us...
    try {
      BufferedReader br = new BufferedReader(new FileReader(f));
      String s;
      StringTokenizer st;
      int commodity;
      logger.info("Reading " +  f.getAbsolutePath() + " length: " + f.length());  
      while ((s = br.readLine()) != null) {
        if (s.startsWith("#")) continue;   // plow past comments
        st = new StringTokenizer(s," ,");
        commodity = Integer.parseInt(st.nextToken());
        pPrivateCarriage[commodity] = Float.parseFloat(st.nextToken());
        for (n=0; n<nTypes; n++)
          pTruckType[commodity][n] = Float.parseFloat(st.nextToken());
        isDefined[commodity] = true;
      }
      br.close();
    } catch (IOException e) { e.printStackTrace(); }

    // Now we need to convert the raw probabilities (which might not even be
    // normalised yet) into cumulative probabilities
    float total;
    for (n=0; n<nClasses; n++)
      if (isDefined[n]) {
        total = 0.0f;
        for (p=0; p<nTypes; p++)  total += pTruckType[n][p];
        for (p=0; p<nTypes; p++)  pTruckType[n][p] /= total;
        for (p=1; p<nTypes; p++)  pTruckType[n][p] += pTruckType[n][p-1];
      }
  }


  

  private int getIndex (String truckType) {
    // You would think that just putting the labels into a hashmap and then
    // accessing the index number that way would be faster, but it inexplicably
    // turns out to be slower than this simple method...
    int result = -1;
    for (int n=0; n<tf$label.length; n++)
      if (tf$label[n].equals(truckType)) {
        result = n;
        break;
      }
    if (result<0) {
      System.err.println("Error: Unknown truckType="+truckType+
        " passed to getIndex()");
      System.exit(4);
    }
    return result;
  }

  public float getDwellTime (Truck3 t) {
    return tf$averageDwellTimes[getIndex( t.truckType )];
  }


  // Choose between private and for-hire carriage, which we do independently
  // of nextTruck() because all shipments for a given origin and commodity need
  // to have a consistent carrier type. The string that is returned is specific
  // to this implementation of the transitional model.
  public String nextCarrierType (int commodity) {
    String result = "STF";    // for-hire truck

    if (SeededRandom.getRandomFloat()<=pPrivateCarriage[commodity]) result = "STP";
    return result;
  }

  // Randomly generate trucks from commodity-specific distribution until you
  // get one that can hold the specified shipment.
  public Truck3 nextTruck (String carrierType, int commodity, int location,
      float shipmentWeight) {
    if (!isDefined[commodity]) return null;    // code an exception later on
    // Start by randomly choosing a truck from the cumulative probability
    // distribution associated with the given commodity
    int draws = 0, index = -1;
    do {
      float r = SeededRandom.getRandomFloat();
      // Since the upper ends of the cumulative probabilities are stored in the
      // arrays, we simply need to see if the random number is less or equal
      for (int p=0; p<pTruckType[commodity].length; p++)
        if (r<=pTruckType[commodity][p]) {
          index = p;
          break;
        }
      ++draws;    // How many selections have we made so far?
    } while (tf$payloadCapacity[index]<shipmentWeight & draws<=MAX_RANDOM_DRAWS);

    // It's possible that shipment is too heavy for any truck in the fleet (an
    // inconsistency in the shipment weight and vehicle payload distributions),
    // so we should check for that here...
    if (draws>=MAX_RANDOM_DRAWS) {    // Convert to exception later
        logger.error("Error: shipment w="+shipmentWeight+" for c="+ commodity+" too large for defined fleet, will place it in largest truck class");
        index = pTruckType[commodity].length-1;
    }

    // Create a new truck of the type drawn from the commodity-specific
    // distribution and send it on its merry way
    return new Truck3(tf$label[index], carrierType, location,
      tf$grossVehicleWeight[index], tf$payloadCapacity[index],
      tf$shiftMinutes[index]);
  }


  public static void main (String[] args) {
    TruckSelector ts = new TruckSelector(
      new File ("/temp/data/VehicleTypeAttributes.txt"),
      new File ("/temp/data/CommodityVehicleTypes.txt") );

    // Test nextTruck()
    Random r = new Random();
    int commodity = 10, location = 401;
    float pounds, base = 3000f, nr;
    String carrierType = "STP";
    Truck3 truck;
    for (int n=0; n<10; n++) {
      nr = r.nextFloat();
      pounds = base+(nr*(base/10.0f));
      System.err.print("nr="+nr+" p="+pounds+" > ");
      truck = ts.nextTruck(carrierType, commodity, location, pounds);
      System.err.println(truck);
    }

  }

}

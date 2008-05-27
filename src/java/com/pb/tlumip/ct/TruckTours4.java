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

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixReader;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

class TruckTours4 {
    protected static Logger logger = Logger.getLogger("com.pb.tlumip.ct");
    ResourceBundle rb;
    ResourceBundle globalRb;
    String inputPath;
    ArrayList shipments;
    ArrayList trucks;

    TruckTours4 (ResourceBundle rb, ResourceBundle globalRb, String ctInputPath) {
        this.rb = rb;
        this.globalRb = globalRb;
        this.inputPath = ctInputPath;
        this.shipments = new ArrayList();
        this.trucks = new ArrayList();

    }

    private void readShipments (File f) {
        logger.info("Starting readShipments()");
        int recordsRead = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
            StringTokenizer st;
            String s;
            int i = 0;   // number of records read
            while ((s = br.readLine()) != null) {
                if (s.startsWith("#")) continue;      // encountered a comment record
                st = new StringTokenizer(s, ",");
                ++recordsRead;
                String sctg = st.nextToken(); //sctg
                int obz = Integer.parseInt(st.nextToken());  //origin beta zone
                int oaz = Integer.parseInt(st.nextToken()); //origin alpha zone
                int dbz = Integer.parseInt(st.nextToken()); //destination beta zone
                int daz = Integer.parseInt(st.nextToken()); //destination alpha zone
                double pounds = Double.parseDouble(st.nextToken());  //pounds
                double value = Double.parseDouble(st.nextToken());  //value
                String mode = st.nextToken(); //mode of transport
                boolean transShipped = Boolean.valueOf(st.nextToken());  //transShipped
                String pattern = st.nextToken(); //transshipment pattern
                float alphaDistance =  Float.parseFloat(st.nextToken());
                shipments.add(new Shipment(sctg,obz,oaz,dbz,daz,pounds,value,mode,transShipped,pattern,alphaDistance));
            }
            br.close();
        } catch (IOException e) { e.printStackTrace(); }
        logger.info("readShipments(): "+recordsRead+" records read");
    }


  public void run (File f) {
      logger.info("Starting to load shipments onto trucks (TruckTours)");
      Date start = new Date();
      readShipments(f);
      int nShipments = shipments.size();
      logger.info("Starting sort...");
      Collections.sort(shipments, new ShipmentComparator());

      TruckSelector ts = new TruckSelector(new File (inputPath + "VehicleTypeAttributes.txt"),   // yet more parameters
                                new File(inputPath + "CommodityVehicleTypes.txt") );   // even more parameters

      String offPeakTimeFile = ResourceUtil.getProperty(rb, "alpha.op.time.skim");
      ZipMatrixReader zr = new ZipMatrixReader(new File(offPeakTimeFile + globalRb.getString("matrix.extension")));
      Matrix offPeakSkim = zr.readMatrix();

      // Generate the first truck (because everyone knows the chicken came before
      // the egg...)
      String commodity = ((Shipment)shipments.get(0)).getCommodityCode(), currentCommodity = commodity;
      int cint = Integer.parseInt(commodity);
      String currentCarrierType = ts.nextCarrierType(cint);
      int zone = ((Shipment)shipments.get(0)).getOriginAlphaZone();
      int currentZone = zone;
      Truck3 truck = ts.nextTruck(currentCarrierType, cint, zone,((Shipment)shipments.get(0)).getWeight());
      float dwellTime = ts.getDwellTime(truck);
      float travelTime = offPeakSkim.getValueAt( zone, ((Shipment)shipments.get(0)).getDestinationAlphaZone());


      truck.addShipment((Shipment)shipments.get(0), (dwellTime+travelTime));

    // Process each shipment in turn
    for (int j=1; j<nShipments; j++) {
        // Grab attributes of the next shipment
        zone = ((Shipment)shipments.get(j)).getOriginAlphaZone();
        commodity = ((Shipment)shipments.get(j)).getCommodityCode();
        // Convert string representation to integer for use later...
        cint = Integer.parseInt(commodity);
        dwellTime = ts.getDwellTime(truck);
        travelTime = offPeakSkim.getValueAt( zone, ((Shipment)shipments.get(0)).getDestinationAlphaZone());

      // Do we need a new truck? Handle the case that each zone,commodity pair
      // represents a separate notional shipper, as well as full trucks
      if (zone!=currentZone || !commodity.equals(currentCommodity) ||
        !truck.canHandleShipment(((Shipment)shipments.get(j)).getWeight(),(dwellTime+travelTime) )) {
        trucks.add(truck);
        currentCarrierType = ts.nextCarrierType(cint);
        truck = ts.nextTruck(currentCarrierType, cint, zone,
          ((Shipment)shipments.get(0)).getWeight());
        currentZone = zone;
        currentCommodity = commodity;
      }

      // Finally, add the shipment to the current truck
      truck.addShipment(((Shipment)shipments.get(j)), (dwellTime+travelTime));
    }

    // Tell us what you did on your way out the door
    logger.info(shipments.size()+" shipments loaded into "+trucks.size()+
      " trucks");
      logger.info("total runtime for Truck Tours: "+CTHelper.elapsedTime(start, new Date()));
  }


  public void writeTours (File f, boolean collapseIntrazonalTrips) {
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(f.getAbsolutePath()));
      int nTrucks = trucks.size();
      logger.info("Writing "+nTrucks+" truck tours to "+f);

      bw.write("origin,tripStartTime,duration,destination,tourMode,tripMode,"+
        "tripFactor,truckID,truckType,carrierType,commodity,weight,distance");
      bw.newLine();

      for (int n=0; n<nTrucks; n++)
        bw.write(((Truck3)trucks.get(n)).getTripList(n, collapseIntrazonalTrips));

      bw.flush();
      bw.close();
    } catch (IOException e) { e.printStackTrace(); }
  }


  public static void main (String[] args) {
    boolean collapseIntrazonalTrips = true;
    Date start = new Date();
    ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_CT/t1/ct/ct.properties"));
    ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_CT/t1/global.properties"));
        String inputPath = ResourceUtil.getProperty(rb, "ct.base.data");
      logger.info("input path: " + inputPath);
    String outputPath = ResourceUtil.getProperty(rb, "ct.current.data");
      logger.info("output path: " + outputPath);
   
    TruckTours4 truckTours = new TruckTours4(rb,globalRb, inputPath);
    truckTours.run(new File(outputPath + "DailyShipments.txt"));
    truckTours.writeTours(new File(outputPath + "TruckTrips.txt"), collapseIntrazonalTrips);
    logger.info("total time: "+CTHelper.elapsedTime(start, new Date()));
  }

}

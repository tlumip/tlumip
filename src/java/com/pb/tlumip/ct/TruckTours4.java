package com.pb.tlumip.ct;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixReader;
import com.pb.common.util.ResourceUtil;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

class TruckTours4 {
    protected static Logger logger = Logger.getLogger("com.pb.tlumip.ct");
    ResourceBundle rb;
    String inputPath;
    ArrayList shipments;
    ArrayList trucks;
    Random rn;
    long randomNumberSeed;


    TruckTours4 (ResourceBundle rb, String ctInputPath, long rns) {
        this.rb = rb;
        this.inputPath = ctInputPath;
        this.shipments = new ArrayList();
        this.trucks = new ArrayList();
        this.randomNumberSeed = rns;
        this.rn = new Random(randomNumberSeed);
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
                boolean transShipped = Boolean.valueOf(st.nextToken()).booleanValue();  //transShipped
                String pattern = st.nextToken(); //transshipment pattern
                shipments.add(new Shipment(sctg,obz,oaz,dbz,daz,pounds,value,mode,transShipped,pattern));
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

      TruckSelector ts = new TruckSelector(randomNumberSeed,new File (inputPath + "VehicleTypeAttributes.txt"),   // yet more parameters
                                new File(inputPath + "CommodityVehicleTypes.txt") );   // even more parameters

      String offPeakTimeFile = ResourceUtil.getProperty(rb, "optime.skim.file");
      ZipMatrixReader zr = new ZipMatrixReader(new File(offPeakTimeFile));
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

      bw.write("origin,tripStartTime,distance,destination,tourMode,tripMode,"+
        "tripFactor,truckID,truckType,carrierType,commodity,weight");
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
    ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_pleaseWork/t1/ct/ct.properties"));
    String inputPath = ResourceUtil.getProperty(rb, "ct.base.data");
      logger.info("input path: " + inputPath);
    String outputPath = ResourceUtil.getProperty(rb, "ct.current.data");
      logger.info("output path: " + outputPath);
    long randomSeed = Long.parseLong(ResourceUtil.getProperty(rb, "randomSeed"));
      logger.info("random seed: " + randomSeed);
    TruckTours4 truckTours = new TruckTours4(rb,inputPath,randomSeed);
    truckTours.run(new File(outputPath + "DailyShipments.txt"));
    truckTours.writeTours(new File(outputPath + "TruckTrips.txt"), collapseIntrazonalTrips);
    logger.info("total time: "+CTHelper.elapsedTime(start, new Date()));
  }

}

package com.pb.despair.ct;

import com.pb.common.util.ResourceUtil;
import com.pb.common.matrix.ZipMatrixReader;
import com.pb.common.matrix.Matrix;

import java.util.*;
import java.util.logging.Logger;
import java.io.*;

public class DiscreteShipments2 {
    protected static Logger logger = Logger.getLogger("com.pb.despair.ct");
    private final static int POUNDS_PER_TON = 2000,   // number of pounds in a short ton
                            INITIAL_ARRAYLIST_SIZE = 600000;
    private static Vector workspace = new Vector();
    private static ArrayList shipments= new ArrayList(INITIAL_ARRAYLIST_SIZE);

    ResourceBundle rb;
    Commodities cp;
    Random randomNumber;
    ZoneMap zoneMap;

   DiscreteShipments2(ResourceBundle rb, String ctInputs, long seed) {
       this.rb = rb;
        File alpha2betaFile = new File(ResourceUtil.getProperty(rb,"alpha2beta.file"));
        zoneMap = new ZoneMap(alpha2betaFile, seed);
        File commodityPropertiesFile = new File(ctInputs + "CommodityProperties.txt");
        this.cp = new Commodities(commodityPropertiesFile);
        this.randomNumber = new Random(seed);
    }

   private double[] getShipmentList (String c, double weeklyTons) {
     double pounds = weeklyTons*POUNDS_PER_TON, totalShipmentSize = 0.0;
     double residual = pounds;
     double d;  // size of the current shipment
     // Since we're re-using workspace continually, start out by emptying its current contents.
     // This might be unnecessary given that we keep track of its maximum index, but we'll make
     // the code correct for now.
     workspace.clear();
     while (residual>0.0) {
       // Determine shipment size by sampling from commodity-specific distribution
       d = cp.getShipmentSize(c, randomNumber.nextDouble());
       // Handle to possibility that the total OD flow is smaller than the shipment size sampled from
       // the survey distribution
       if (pounds<=d) d = pounds;
       // But otherwise, keep on sampling until we've exceeded the given tonnage (now in pounds)
       residual -= d;
       totalShipmentSize += d;
       workspace.add(new Double(d));
     }
     // Now we've generated a list of shipments but their total weight should be greater than the
     // given tonnage. So scale the weights we've generated to exactly conserve to OD flows.
     double scalingFactor = pounds/totalShipmentSize;
     double[] result = new double[workspace.size()];
     Double dx;
     for (int j=0; j<workspace.size(); j++) {
       dx = (Double)workspace.get(j);
       result[j] = dx.doubleValue()*scalingFactor;
     }
     // And finally, send back the appropriately sized array
     return result;
   }

   private int getTransShipmentZone (String c, int origin, int destination, double distance) {
     int transShipmentZone = -1;    // transshipment doesn't occur
     double d = cp.getTransShipmentProbability(c)*CTHelper.tanh(distance*0.01); 
     if (randomNumber.nextDouble()<=d) transShipmentZone = destination;
     return transShipmentZone;

   }

   private String commodityToString (int c) {
     String s = String.valueOf(c);
     if (c<10) s = "0"+s;
     return s;
   }

   public void run (File f) {
       logger.info("Starting the beta zone demand to alpha zone demand conversion process " +
               "(DiscreteShipments)");
       Date start = new Date();
      int weeklyShipments = 0;
     //Get the beta distance matrix so that we can get distances
       String distanceFile = ResourceUtil.getProperty(rb, "betapkdist.skim.file");
        ZipMatrixReader zr = new ZipMatrixReader(new File(distanceFile));
        Matrix distMatrix = zr.readMatrix();
     // Read each record from the weekly demand and process it
     try {
       DataInputStream di = new DataInputStream(new FileInputStream(f));
       String commodity, transShipmentPattern;
       int originAlpha, originBeta, destinationAlpha, destinationBeta, intWeeklyTons, transShipmentAlpha,
         transShipmentBeta;
       double weeklyTons, value = 0.0;
         float betaDist = 0.0f;

       boolean eof = false, transShipped = false;
       double[] shipmentList;
       try {
         while (!eof) {
           commodity = commodityToString(di.readInt());
           originBeta = di.readInt();
           destinationBeta = di.readInt();
           weeklyTons = di.readDouble();
           betaDist = distMatrix.getValueAt(originBeta,destinationBeta);

// We have a pathological case where commodity 00 is found. We need to fix FreightDemand, but for now
// just ignore them
           if (commodity.equals("00") || commodity.equals("16")) continue;

           shipmentList = getShipmentList(commodity, weeklyTons);
           for (int i=0; i<shipmentList.length; i++) {
             ++weeklyShipments;
             // Will it ship today?
             if (randomNumber.nextDouble()<=cp.getShipmentThreshold(commodity)) {
               //(1) Reset default non-attributed shipment properties
               transShipped = false;
               transShipmentPattern = "OD";

               //(2) Allocate the shipment to alpha zones within the given beta zones
               originAlpha = zoneMap.getAlphaZone(originBeta, commodity);
               destinationAlpha = zoneMap.getAlphaZone(destinationBeta, commodity);

               //(3) Determine whether transshipment occurs and if so, where
               transShipmentBeta = getTransShipmentZone(commodity, originBeta, destinationBeta, betaDist);

               if (transShipmentBeta>0) {
                 transShipmentAlpha = destinationBeta;   // Replace with search function
                 // Generate the trip from the transshipment point to final destination
                 transShipped = true;
                 transShipmentPattern = "XD";
                 shipments.add(new Shipment(commodity, transShipmentBeta, transShipmentAlpha,
                   destinationBeta, destinationAlpha, shipmentList[i], value, "STK", transShipped,
                   transShipmentPattern));
                 // Specify the destination end to be the transshipment point
                 destinationBeta = transShipmentBeta;
                 destinationAlpha = transShipmentAlpha;
                 transShipmentPattern = "OX";
               }
               // And finally, add the leg from origin to either ultimate destination or transshipment point
               shipments.add(new Shipment(commodity, originBeta, originAlpha, destinationBeta, destinationAlpha,
                 shipmentList[i], value, "STK", transShipped, transShipmentPattern));
             }
           }
         }
       } catch (EOFException e) { eof = true; }
       di.close();

     } catch (IOException e) { e.printStackTrace(); }
     int dailyShipments = shipments.size();
     double percent = ((double)dailyShipments/(double)weeklyShipments)*100.0;
     logger.info(dailyShipments + " daily shipments created (" + percent + "%  of " + weeklyShipments + " weekly shipments)");
       logger.info("total runtime for Discrete Shipments: "+CTHelper.elapsedTime(start, new Date()));
   }

   public void writeShipments (File f) {
     try {
       BufferedWriter bw = new BufferedWriter(new FileWriter(f.getAbsolutePath()));
       bw.write(Shipment.writeShipmentHeader()+"\n");
       int size = shipments.size();
       for (int n=0; n<size; n++) {
         bw.write(((Shipment)shipments.get(n)).writeShipment());
         bw.newLine();
       }
       bw.flush();
       bw.close();
     } catch (IOException e) { e.printStackTrace(); }
   }


   public static void main (String[] args) {
       Date start = new Date();
       ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_pleaseWork/t1/ct/ct.properties"));
       String inputPath = ResourceUtil.getProperty(rb,"ct.base.data");
       String outputPath = ResourceUtil.getProperty(rb,"ct.current.data");
       long randomSeed = Long.parseLong(ResourceUtil.getProperty(rb, "randomSeed"));
       DiscreteShipments2 ds = new DiscreteShipments2(rb,inputPath,randomSeed);
       ds.run(new File(outputPath + "WeeklyDemand.binary"));
       ds.writeShipments(new File(outputPath + "DailyShipments.txt"));
       logger.info("total time: "+CTHelper.elapsedTime(start, new Date()));
   }

}

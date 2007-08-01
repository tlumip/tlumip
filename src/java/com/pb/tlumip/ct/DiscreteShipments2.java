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
import com.pb.tlumip.model.WorldZoneExternalZoneUtil;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Vector;

public class DiscreteShipments2 {
    protected static Logger logger = Logger.getLogger("com.pb.tlumip.ct");
    private final static int POUNDS_PER_TON = 2000,   // number of pounds in a short ton
                            INITIAL_ARRAYLIST_SIZE = 600000;
    private static Vector workspace = new Vector();
    private static ArrayList shipments= new ArrayList(INITIAL_ARRAYLIST_SIZE);

    ResourceBundle ctRb;
    Commodities cp;
    Random randomNumber;
    ZoneMap zoneMap;
    WorldZoneExternalZoneUtil wzUtil;
    ResourceBundle globalRb;

   DiscreteShipments2(ResourceBundle appRb, ResourceBundle globalRb, String ctInputs, long seed) {
       this.ctRb = appRb;
       File alpha2betaFile = new File(ResourceUtil.getProperty(globalRb,"alpha2beta.file"));
       zoneMap = new ZoneMap(alpha2betaFile, seed);
       File commodityPropertiesFile = new File(ctInputs + "CommodityProperties.txt");
       this.cp = new Commodities(commodityPropertiesFile);
       this.randomNumber = new Random(seed);
       wzUtil = new WorldZoneExternalZoneUtil(globalRb);
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

   private int getTransShipmentZone (String c, int destination, double distance) {
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
        //The flows also include flows to the "world zones" so beta = beta + world zones
        //and alpha = alpha + external stations.
        logger.info("Starting the beta zone demand to alpha zone demand conversion process " +
               "(DiscreteShipments)");
        Date start = new Date();
        int weeklyShipments = 0;
        //Get the beta distance matrix so that we can get distances
        //Note: the beta skims also include the world zones so you can
        //get distances from beta to beta, or beta to world zone
        String distanceFile = ResourceUtil.getProperty(ctRb, "beta.pk.dist.skim");
        ZipMatrixReader zr = new ZipMatrixReader(new File(distanceFile));
        Matrix betaDistMatrix = zr.readMatrix();

        //Note: the alpha skims also include the external zones so you can
        //get distances from alpha to alpha, or alpha to external zone
        distanceFile = ctRb.getString("alpha.pk.dist.skim");
        zr = new ZipMatrixReader(new File(distanceFile));
        Matrix alphaDistMatrix = zr.readMatrix();

        // Read each record from the weekly demand and process it
        try {
            DataInputStream di = new DataInputStream(new FileInputStream(f));
            String commodity, transShipmentPattern;
            int originAlpha, originBeta, destinationAlpha, destinationBeta, transShipmentAlpha,
                transShipmentBeta;
            double weeklyTons, value = 0.0;

            boolean eof = false, transShipped;
            double[] shipmentList;
            try {
                while (!eof) {
                    commodity = commodityToString(di.readInt());
                    originBeta = di.readInt();
                    destinationBeta = di.readInt();
                    weeklyTons = di.readDouble();



                    shipmentList = getShipmentList(commodity, weeklyTons);
                    for (int i=0; i<shipmentList.length; i++) {
                        ++weeklyShipments;
                        // Will it ship today?
                        if (randomNumber.nextDouble()<= cp.getShipmentThreshold(commodity)) {
                            //(1) Reset default non-attributed shipment properties
                            transShipped = false;
                            transShipmentPattern = "OD";

                            //(2) Allocate the shipment to alpha zones within the given beta zones
                            if(wzUtil.isWorldZone(originBeta) || wzUtil.isWorldZone(destinationBeta)){  //must be an originWZ
                                //one of the ends is a world zone and therefore you have to determine
                                //the internal alpha zone first (either alphaOrigin or alphaDestination) before
                                //you can determine the appropriate external zone because it depends on distance
                                //to the alpha zone.
                                int[] aOaD = chooseAlphaODs(originBeta, destinationBeta, alphaDistMatrix, commodity);
                                originAlpha = aOaD[0];
                                destinationAlpha = aOaD[1];
                            }else{
                                originAlpha = zoneMap.getAlphaZone(originBeta, commodity);
                                destinationAlpha = zoneMap.getAlphaZone(destinationBeta, commodity);
                            }



                            float betaODDist = betaDistMatrix.getValueAt(originBeta,destinationBeta);

                            //(3) Determine whether transshipment occurs and if so, where
                            transShipmentBeta = getTransShipmentZone(commodity, destinationBeta, betaODDist);

                            if (transShipmentBeta>0) {
                                //TODO - Ask Rick about this change.  1.  is it supposed to be alpha and
                                //TODO 2.  if the transShipmentBeta is a world zone is it ok to move the
                                //TODO transShipment location to an external zone.
                                transShipmentAlpha = destinationAlpha;   // Replace with search function
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

    /*
    The user will pass in a world zone and the algorithm will decide which external
    zone to pick based on the distance from the world zone to the eligible external zones
     */
    public int[] chooseAlphaODs(int originBeta, int destinationBeta, Matrix alphaDistMatrix, String commodity){
        //first let's do a little sanity check
        if(wzUtil.isWorldZone(originBeta) && wzUtil.isWorldZone(destinationBeta))
            throw new RuntimeException("CT module does not handle flows from world zone " + originBeta + " to world zone " + destinationBeta);

        int alphaO = -1;
        int alphaD = -1;
        //Next figure out which end of the flow is a world zone and handle the other end first.
        if(wzUtil.isWorldZone(originBeta)){ //this means the origin is a world zone so deal with dest zone first
            alphaD = zoneMap.getAlphaZone(destinationBeta, commodity);
            List<Integer> eligibleZones = wzUtil.getExternalZonesConnectedTo(originBeta); //remember that originBeta = WZ
            float min = Float.MAX_VALUE;
            float dist;
            for(int zone : eligibleZones){ //find min distance from WZ to possible externals + external to alpha
                dist = wzUtil.getDistanceFromWorldZoneToEZone(originBeta, zone) + alphaDistMatrix.getValueAt(zone, alphaD);
                if(dist < min){
                    min = dist;
                    alphaO = zone;
                }
            }
            return new int[] {alphaO, alphaD};
        } else{  //this means the destination is a world zone so deal with the origin zone first
            alphaO = zoneMap.getAlphaZone(originBeta, commodity);
            List<Integer> eligibleZones = wzUtil.getExternalZonesConnectedTo(destinationBeta); //remember that destinationBeta = WZ
            float min = Float.MAX_VALUE;
            float dist;
            for(int zone : eligibleZones){ //find min distance from alpha to possible externals and then external to WZ
                dist = alphaDistMatrix.getValueAt(alphaO, zone) + wzUtil.getDistanceFromEZoneToWorldZone(zone, destinationBeta);
                if(dist < min){
                    min = dist;
                    alphaD = zone;
                }
            }
            return new int[] {alphaO, alphaD};
        }

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
       ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_aaaCurrentData/t1/ct/ct.properties"));
       ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_aaaCurrentData/t1/global.properties"));
       String inputPath = ResourceUtil.getProperty(rb,"ct.base.data");
       String outputPath = ResourceUtil.getProperty(rb,"ct.current.data");
       long randomSeed = Long.parseLong(ResourceUtil.getProperty(rb, "randomSeed"));
       DiscreteShipments2 ds = new DiscreteShipments2(rb,globalRb,inputPath,randomSeed);
       ds.run(new File(outputPath + "WeeklyDemand.binary"));
       ds.writeShipments(new File(outputPath + "DailyShipments.txt"));
       logger.info("total time: "+CTHelper.elapsedTime(start, new Date()));
   }

}

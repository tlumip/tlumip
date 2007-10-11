/*
 * Copyright 2006 PB Consult Inc.
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
package com.pb.tlumip.model;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeSet;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Jul 12, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class WorldZoneExternalZoneUtil {

    public final int LOCAL_MARKET_WORLD_ZONE;
    static final String WZ_COL_NAME = "WorldMarket";
    static final String EZ_COL_NAME = "ExternalStation";
    static final String FROM_DIST = "DistanceFromWorldMarket";
    static final String TO_DIST = "DistanceToWorldMarket";


    int[] worldZones;
    int nWorldZones;
    int MAX_WORLD_ZONE;

    int[] externalZones;
    int nExternalZones;
    int MAX_EXTERNAL_ZONE;
    TableDataSet tblWZEZDistances;

    HashMap<Integer, List<Integer>> externalZonesConnectedToWorldZone;
    HashMap<Integer, List<Integer>> worldZonesConnectedToExternalZone;
    HashMap<Integer, HashMap<Integer, float[]>> distanceEzoneFromToWzone;

    public WorldZoneExternalZoneUtil(ResourceBundle rb){

        LOCAL_MARKET_WORLD_ZONE = ResourceUtil.getIntegerProperty(rb, "local.market.world.zone",6006);

        CSVFileReader reader = new CSVFileReader();
        try {
            tblWZEZDistances = reader.readFile(new File(rb.getString("world.to.external.distances")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        TreeSet<Integer> set = new TreeSet<Integer>();
        for(int wz : tblWZEZDistances.getColumnAsInt(WZ_COL_NAME)){
            set.add(wz);
        }


        nWorldZones = set.size();
        MAX_WORLD_ZONE = set.last();
        worldZones = new int[set.size()];
        int index = 0;
        for(int zone : set){
            worldZones[index] = zone;
            index++;
        }


        set.clear();
        for(int wz : tblWZEZDistances.getColumnAsInt(EZ_COL_NAME)){
            set.add(wz);
        }

        nExternalZones = set.size();
        MAX_EXTERNAL_ZONE = set.last();
        externalZones = new int[set.size()];
        index = 0;
        for(int zone : set){
            externalZones[index] = zone;
            index++;
        }

        //populate the 2 hashmaps based on the rows of the table.
        externalZonesConnectedToWorldZone = new HashMap<Integer, List<Integer>>();
        worldZonesConnectedToExternalZone = new HashMap<Integer, List<Integer>>();
        distanceEzoneFromToWzone = new HashMap<Integer, HashMap<Integer, float[]>>();
        for(int r=1; r<tblWZEZDistances.getRowCount(); r++){
            Integer key = (int) tblWZEZDistances.getValueAt(r,WZ_COL_NAME);
            //map 1
            if(!externalZonesConnectedToWorldZone.containsKey(key)){
                externalZonesConnectedToWorldZone.put(key, new ArrayList<Integer>());
            }
            externalZonesConnectedToWorldZone.get(key).add((int) tblWZEZDistances.getValueAt(r,EZ_COL_NAME));

            //map 2
            if(!distanceEzoneFromToWzone.containsKey(key)){
                distanceEzoneFromToWzone.put(key, new HashMap<Integer, float[]>());
            }
            float[] fromDistToDist = new float[2];
            fromDistToDist[0] = tblWZEZDistances.getValueAt(r, FROM_DIST);
            fromDistToDist[1] = tblWZEZDistances.getValueAt(r, TO_DIST);
            distanceEzoneFromToWzone.get(key).put((int) tblWZEZDistances.getValueAt(r,EZ_COL_NAME), fromDistToDist);

            //map 3
            if(!worldZonesConnectedToExternalZone.containsKey((int) tblWZEZDistances.getValueAt(r,EZ_COL_NAME))){
                worldZonesConnectedToExternalZone.put((int) tblWZEZDistances.getValueAt(r,EZ_COL_NAME), new ArrayList<Integer>());
            }
            worldZonesConnectedToExternalZone.get((int) tblWZEZDistances.getValueAt(r,EZ_COL_NAME)).add(key);
        }



    }


    public int getHighestWZForCT(){
        if(MAX_WORLD_ZONE == LOCAL_MARKET_WORLD_ZONE ){
            return MAX_WORLD_ZONE - 1;
        }else return MAX_WORLD_ZONE;
    }

    public int[] getWorldZones(){
        return worldZones;
    }

    public int getNumberOfWorldZones(){
        return nWorldZones;
    }

    public int getNumberOfWZsForCT(){
        return nWorldZones - 1; //CT doesn't care about the local market zone - that is ET.
    }

    public int[] getWorldZonesForCT(){
        int[] wzs = new int[nWorldZones-1];
        int index = 0;
        for(int wz : worldZones){
            if(wz != LOCAL_MARKET_WORLD_ZONE){
                wzs[index] = wz;
                index++;
            }
        }
        return wzs;
    }

    public int[] getExternalZones(){
        return externalZones;
    }

    public int[] getExternalZonesForET() {
        int[] externalZonesForET = new int[externalZones.length-1];
        List<Integer> externalZoneList = new ArrayList<Integer>();
        for (int zone: externalZones) {
            if (zone != 5012) {
               externalZoneList.add(zone);
            }
        }

        for (int i = 0; i < externalZoneList.size(); i++) {
            externalZonesForET[i] = externalZoneList.get(i);
        }

        return externalZonesForET;
    }

    public int getNumberOfExternalZones(){
        return nExternalZones;
    }

    public List<Integer> getExternalZonesConnectedTo(int worldZone){
         return externalZonesConnectedToWorldZone.get(worldZone);
    }

    public List<Integer> getWorldZonesConnectedTo(int externalZone){
         return worldZonesConnectedToExternalZone.get(externalZone);
    }

    public float getDistanceFromWorldZoneToEZone(int fromWorldZone, int toEZone){
        return distanceEzoneFromToWzone.get(fromWorldZone).get(toEZone)[0];
    }

    public float getDistanceFromEZoneToWorldZone(int fromEZone, int toWorldZone){
        return distanceEzoneFromToWzone.get(toWorldZone).get(fromEZone)[1];
    }

    /**
     * if the zoneNumber passed in matches a zone in the array of worldzones
     * method will return true.  If the for loop exits without returning
     * this means the zone was not found and therefore the method returns false.
     * @param zoneNumber - zone to check against list of world zones
     * @return true if zone is a world zone.
     */
    public boolean isWorldZone(int zoneNumber){
        //
        for(int zone : worldZones){
            if (zone == zoneNumber) return true;
        }
        return false;
    }

    public Matrix createBeta6000Matrix(Matrix mtxBeta5000s){
        //figure out the external numbers
        int nBetas = mtxBeta5000s.getExternalNumbers().length - nExternalZones -1; //just betas
        int[] externalNumbers = new int[nBetas + nWorldZones + 1];

        System.arraycopy(mtxBeta5000s.getExternalNumbers(), 1, externalNumbers, 1, nBetas);
        System.arraycopy(worldZones, 0, externalNumbers, nBetas + 1, worldZones.length);

        //now move the values from mtxBeta5000s to mtxBeta6000s and add the worldZone distance
        //to the 6000 zones
        int row, col;
        Matrix mtxBeta6000s = new Matrix("beta6000s","distanceMtx", externalNumbers.length-1, externalNumbers.length-1 );
        mtxBeta6000s.setExternalNumbers(externalNumbers);

        //Set the values in the new matrix
        float distance;
        for(int r = 1; r < externalNumbers.length; r++){
            row = externalNumbers[r];
            for(int c=1; c < externalNumbers.length; c++){
                col = externalNumbers[c];
                //case 1: row is beta, col is beta
                if(!isWorldZone(row) && !isWorldZone(col)){
                    mtxBeta6000s.setValueAt(row, col, mtxBeta5000s.getValueAt(row, col));

                //case 2: row is beta, col is world zone
                }else if(!isWorldZone(row) && isWorldZone(col)){
                    float minDist = Float.MAX_VALUE;
                    List<Integer> eZones = getExternalZonesConnectedTo(col);
                    for(int zone : eZones){
                        distance = mtxBeta5000s.getValueAt(row, zone) + getDistanceFromEZoneToWorldZone(zone, col);
                        if(distance < minDist){
                            minDist = distance;
                        }
                    }
                    mtxBeta6000s.setValueAt(row, col, minDist);

                //case 3: row is world zone, col is beta
                } else if(isWorldZone(row) && !isWorldZone(col)){
                    float minDist = Float.MAX_VALUE;
                    List<Integer> eZones = getExternalZonesConnectedTo(row);
                    for(int zone: eZones){
                        distance = getDistanceFromWorldZoneToEZone(row,zone) + mtxBeta5000s.getValueAt(zone, col);
                        if(distance < minDist) minDist = distance;
                    }
                    mtxBeta6000s.setValueAt(row, col, minDist);

                //case 4: row and col are world zones
                } else {
                    distance = Float.MAX_VALUE;
                    mtxBeta6000s.setValueAt(row, col, distance);
                }
            }
        }
        return mtxBeta6000s;
    }


    public static void main(String[] args) {
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_aaaCurrentData/t1/global.properties"));
        WorldZoneExternalZoneUtil util = new WorldZoneExternalZoneUtil(globalRb);
        System.out.println("nWorldZones: " + util.nWorldZones);
        System.out.println("max WorldZone: " + util.MAX_WORLD_ZONE);
        for(int zone : util.worldZones){
            System.out.println(zone);
        }

        int[] wzs = util.getWorldZonesForCT();
        for(int zone : wzs){
            System.out.println(zone);
        }
    }
}

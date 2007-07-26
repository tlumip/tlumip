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
    static final String FROM_DIST = "From6000Distance";
    static final String TO_DIST = "To6000Distance";


    int[] worldZones;
    int nWorldZones;
    int MAX_WORLD_ZONE;

    int[] externalZones;
    int nExternalZones;
    int MAX_EXTERNAL_ZONE;
    TableDataSet tblWZEZDistances;

    HashMap<Integer, List<Integer>> externalZonesConnectedToWorldZone;

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

        externalZonesConnectedToWorldZone = new HashMap<Integer, List<Integer>>();
        Integer key;
        for(int r=1; r<tblWZEZDistances.getRowCount(); r++){
            key = (int) tblWZEZDistances.getValueAt(r,WZ_COL_NAME);
            if(!externalZonesConnectedToWorldZone.containsKey(key)){
                externalZonesConnectedToWorldZone.put(key, new ArrayList<Integer>());
            }
            externalZonesConnectedToWorldZone.get(key).add((int) tblWZEZDistances.getValueAt(r,EZ_COL_NAME));
        }

    }

    public int getHighestWZForCT(){
        if(MAX_WORLD_ZONE == LOCAL_MARKET_WORLD_ZONE ){
            return MAX_WORLD_ZONE - 1;
        }else return MAX_WORLD_ZONE;
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

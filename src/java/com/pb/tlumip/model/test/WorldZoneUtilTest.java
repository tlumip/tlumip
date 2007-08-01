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
package com.pb.tlumip.model.test;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.model.WorldZoneExternalZoneUtil;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.junit.Before;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Jul 31, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class WorldZoneUtilTest extends TestCase {

    private WorldZoneExternalZoneUtil wzUtil;
    private Matrix beta5000s;

    @Before
    public void setUp(){
        //For testing purposes write the 2 global props to cwd and then write the
        //WorldZoneExernalStationDistance.csv file to cwd.
        File tempFile = new File("global.properties");
        System.out.println("Writing " + tempFile.getAbsolutePath());
        try {
            FileWriter writer = new FileWriter(tempFile);
            writer.write("world.to.external.distances = ./WorldZoneExternalStationDistances.csv\n");
            writer.write("local.market.world.zone = 6006\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        tempFile = new File("WorldZoneExternalStationDistances.csv");
        System.out.println("Writing " + tempFile.getAbsolutePath());
        try {
            FileWriter writer = new FileWriter(tempFile);
            writer.write("WorldMarket,ExternalStation,DistanceFromWorldMarket,DistanceToWorldMarket\n");
            writer.write("6001,5002,240,240\n");
            writer.write("6001,5003,240,240\n");
            writer.write("6002,5004,800,800\n");
            writer.write("6003,5007,1200,1200\n");
            writer.write("6004,5010,460,460\n");
            writer.write("6005,5012,600,1500\n");
            writer.write("6006,5001,75,75\n");
            writer.write("6006,5002,75,75\n");
            writer.write("6006,5003,75,75\n");
            writer.write("6006,5004,75,75\n");
            writer.write("6006,5005,75,75\n");
            writer.write("6006,5006,75,75\n");
            writer.write("6006,5007,75,75\n");
            writer.write("6006,5008,75,75\n");
            writer.write("6006,5009,75,75\n");
            writer.write("6006,5010,75,75\n");
            writer.write("6006,5011,75,75\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File("global.properties"));
        wzUtil = new WorldZoneExternalZoneUtil(globalRb);

        int[] externals = new int[]{0,1,17,19,5001,5002,5003,5004,5005,5006,5007,5008,5009,5010,5011,5012};
        float[][] values = new float[][]{{10,20,30,0,200,300,400,0,0,0,0,0,0,0,0},
                                         {3,6,9,0,100,200,300,0,0,0,0,0,0,0,0},
                                         {5,10,15,0,50,150,250,0,0,0,0,0,0,0,0},
                                         {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                                         {201,101,51,0,10,20,30,0,0,0,0,0,0,0,0},
                                         {301,201,151,0,25,15,25,0,0,0,0,0,0,0,0},
                                         {401,301,251,0,35,30,20,0,0,0,0,0,0,0,0},
                                         {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                                         {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                                         {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                                         {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                                         {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                                         {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                                         {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                                         {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}};
        beta5000s = new Matrix(values);
        beta5000s.setExternalNumbers(externals);
    }

    public void testNumberOfWorldZones(){
        assertEquals(6, wzUtil.getNumberOfWorldZones());
    }

    public void testNumberOfWorldZonesForCT(){
        assertEquals(5, wzUtil.getNumberOfWZsForCT());
    }

    public void testNumberOfExternalZones(){
        assertEquals(12, wzUtil.getNumberOfExternalZones());
    }

    public void testIsWorldZoneTrue(){
        assertTrue(wzUtil.isWorldZone(6004));
    }

    public void testIsWorldZoneFalse(){
        assertFalse(wzUtil.isWorldZone(4141));
    }

    public void testExternalZonesConnectedToWorldZones(){
        ArrayList<Integer> expectedList = new ArrayList<Integer>();
        expectedList.add(5002);
        expectedList.add(5003);

        List actual = wzUtil.getExternalZonesConnectedTo(6001);

        assertTrue(actual.size() == expectedList.size() &&
                actual.contains(5002) && actual.contains(5003));
    }

    public void testWorldZoneConnectedToExternalZone(){

        ArrayList<Integer> expectedList = new ArrayList<Integer>();
        expectedList.add(6006);
        expectedList.add(6003);

        List actual = wzUtil.getWorldZonesConnectedTo(5007);

        assertTrue(actual.size() == expectedList.size() &&
                actual.contains(6003) && actual.contains(6006));
    }

    public void testDistanceFromWorldZoneToEZone(){
        assertEquals(600.0f, wzUtil.getDistanceFromWorldZoneToEZone(6005, 5012));
    }

    public void testDistanceFromEZoneToWorldZone(){
        assertEquals(1500.0f, wzUtil.getDistanceFromEZoneToWorldZone(5012, 6005));
    }

    public void testCreateBeta6000MatrixBetaToBeta(){
        Matrix beta6000s = wzUtil.createBeta6000Matrix(beta5000s);
        assertEquals(6.0f, beta6000s.getValueAt(17,17));
    }

    public void testCreateBeta6000MatrixBetaToWorld(){
        //check the distance from beta zone 19 to world zone 6001
        Matrix beta6000s = wzUtil.createBeta6000Matrix(beta5000s);

        //There are 2 external zones that connect to world zone 6001 - 5002 and 5003
        //so the distance from 19 to 6001 is the min((dist(19,5002) + dist(5002,6001)),(dist(19,5003) + dist(5003,6001)
        //dist(19,6001) = min((50 + 240), (150 + 240)) = 290
        assertEquals(290.0f, beta6000s.getValueAt(19,6001));
    }

    public void testCreateBeta6000MatrixWorldToBeta(){
        //check the distance from world zone 6002 to beta zone 1
        Matrix beta6000s = wzUtil.createBeta6000Matrix(beta5000s);

        //There is 1 external zone that connects to world zone 6002 - 5004
        //so the distance from 6002 to 1 is the min((dist(6002,5004) + dist(5004,1), Float.MAX_VALUE)
        //dist(6002,1) = min( 800 + 401, Float.MAX_VALUE) = 1201
        assertEquals(1201.0f, beta6000s.getValueAt(6002, 1));
    }



    public static void main(String[] args) {
        new TestRunner().doRun(new TestSuite(WorldZoneUtilTest.class));
    }

}

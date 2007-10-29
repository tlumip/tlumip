package com.pb.tlumip.et.test;
/**
 * ShipmentDetailTest is a class that tests the ShipmentDetail class.
 *
 * @author Kimberly Grommes
 * @version 1.0, Oct 29, 2007
 * Created by IntelliJ IDEA.
 */

import com.pb.tlumip.et.ShipmentDetail;
import junit.framework.TestCase;

public class ShipmentDetailTest extends TestCase {
    ShipmentDetail shipmentDetail;
    String commodity = "SCTG_01";
    int origin = 1;
    int destination = 5;
    String startTime = "0900";
    float numberOfTrucks = 7.47f;
    int truckClass=4;

    protected void setUp() {
        shipmentDetail = new ShipmentDetail(commodity, origin, destination, startTime, numberOfTrucks, truckClass);
    }

    public void testGetOrigination() throws Exception {
        assertEquals(origin, shipmentDetail.getOrigination());
    }

    public void testGetDestination() throws Exception {
        assertEquals(destination, shipmentDetail.getDestination());
    }

    public void testGetTimeOfDay() throws Exception {
        assertEquals(startTime, shipmentDetail.getTimeOfDay());
    }

    public void testGetNumberOfTrucks() throws Exception {
        assertEquals(numberOfTrucks, shipmentDetail.getNumberOfTrucks());
    }

    public void testGetCommodity() throws Exception {
        assertEquals(commodity, shipmentDetail.getCommodity());
    }

    public void testGetTruckClass() throws Exception {
        assertEquals(truckClass, shipmentDetail.getTruckClass());
    }
}
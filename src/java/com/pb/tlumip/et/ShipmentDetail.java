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
package com.pb.tlumip.et;

/**
 * ShipmentDetail is a class that ...
 *
 * @author Kimberly Grommes
 * @version 1.0, Sep 24, 2007
 *          Created by IntelliJ IDEA.
 */
public class ShipmentDetail {

    private int intOrigination;
    private int intDestination;
    private String strTimeOfDay;
    private float fltNumberOfTrucks;
    private String strTruckClass;
    private String strCommodity; //I don't think this is needed.

    public ShipmentDetail(String Commodity, int Origination, int Destination, String TimeOfDay, float NumberOfTrucks, String TruckClass) {
        intOrigination = Origination;
        intDestination = Destination;
        strTimeOfDay = TimeOfDay;
        fltNumberOfTrucks = NumberOfTrucks;
        strCommodity = Commodity;
        strTruckClass = TruckClass;
    }

    public int getOrigination() {
        return intOrigination;
    }

    public int getDestination() {
        return intDestination;
    }

    public String getTimeOfDay() {
        return strTimeOfDay;
    }

    public float getNumberOfTrucks() {
        return fltNumberOfTrucks;
    }

    public String getCommodity() {
        return strCommodity;
    }

    public String getTruckClass() {
        return strTruckClass;
    }
}

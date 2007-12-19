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

class Shipment {
    int originBetaZone, destinationBetaZone, originAlphaZone, destinationAlphaZone;
    double pounds, value;
    String sctg, modeOfTransport, transShipmentPattern;
    boolean transShipped;
    float alphaDistance;

    Shipment (String s, int ob, int oa, int db, int da, double p, double v, String m, boolean t, String tp, float alphaDist) {
        // Information about the shipment that we're given
        sctg = s;
        originBetaZone = ob;
        originAlphaZone = oa;
        destinationBetaZone = db;
        destinationAlphaZone = da;
        pounds = p;
        value = v;
        modeOfTransport = m;
        transShipped = t;
        transShipmentPattern = tp;
        alphaDistance = alphaDist;
    }

    public int getOriginAlphaZone () { return originAlphaZone; }

    public int getDestinationAlphaZone () { return destinationAlphaZone; }

    public String getCommodityCode () { return sctg; }

    public float getWeight () { return (float)pounds; }

    public float getAlphaDistance () { return alphaDistance; }

    public String toString () {
        return "s="+sctg+
                " ob="+originBetaZone+
                " oa="+originAlphaZone+
                " db="+destinationBetaZone+
                " da="+destinationAlphaZone+
                " p="+pounds+
                " v="+value+
                " m="+modeOfTransport+
                " ts="+transShipped+
                " tsp="+transShipmentPattern+
                " ad="+alphaDistance;
    }


    public static String writeShipmentHeader () {
        return "#sctg,originBetaZone,originAlphaZone,destinationBetaZone,destinationAlphaZone,pounds,value,"+
                "modeOfTransport,transShipped,transShipmentPattern,alphaDistance";
    }

    public String writeShipment () {
        return sctg+","+
                originBetaZone+","+
                originAlphaZone+","+
                destinationBetaZone+","+
                destinationAlphaZone+","+
                pounds+","+
                value+","+
                modeOfTransport+","+
                transShipped+","+
                transShipmentPattern+","+
                alphaDistance;
    }

    public static void main (String[] args) {
        Shipment s = new Shipment("10", 20, 30, 40, 50, 60.0, 70.0, "STK", false, "undefined", 10f);
        System.out.println(s);

    }

}

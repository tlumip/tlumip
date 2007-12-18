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



// This is a general class for a truck vehicle, which in the future might become
// an abstract class to allow for polymorphs. Right now it contains the truck
// attributes as well as the shipments it will carry on its journey. A synthetic
// process is used to tag departure time to each shipment, so that we can hand
// off a time-stamped trip list for assignment later.
// @author "Rick Donnelly <rdonnelly@pbtfsc.com>
// @version "1.0, 04/12/04"

import java.util.ArrayList;
import java.util.Random;

public class Truck3 {
  private static final int DEFAULT_ARRAYLIST_SIZE = 5;
  int currentZone;
  float payloadWeight, capacity, timeLeft, grossVehicleWeight;
  String truckType, carrierType;
  ArrayList shipments;
  ArrayList durations;

  Truck3 (String vt, String ct, int loc, float g, float c, float shiftLength) {
    shipments = new ArrayList(DEFAULT_ARRAYLIST_SIZE);
    durations = new ArrayList(DEFAULT_ARRAYLIST_SIZE);
    truckType = vt;
    carrierType = ct;
    currentZone = loc;
    payloadWeight = 0.0f;
    grossVehicleWeight = g;
    capacity = c;
    timeLeft = shiftLength;
  }

  public int getNumberOfShipments () { return shipments.size(); }

  public boolean canHandleShipment (float pounds, float timeRequired) {
    boolean reply = false;   // always a pessimist
    if ((timeLeft-timeRequired)>0 &
      (grossVehicleWeight+payloadWeight+pounds)<=capacity) reply = true;
    return reply;
  }

  public void addShipment (Shipment s, float timeRequired) {
    float pounds = (float)((Shipment)s).pounds;
    shipments.add(s);
    durations.add(new Integer((int)timeRequired));
    payloadWeight += pounds;
    timeLeft -= timeRequired;
  }

  // This is used for diagnostic work only; use writeTripList to export data to
  // TS component
  public String toString () {  
      String header = "vt="+truckType+" ct="+carrierType+" cz="+currentZone+
      " cap="+capacity+" pw="+
      payloadWeight+" tl="+timeLeft+" ns="+shipments.size();
    String s = new String();
    for (int n=0; n<shipments.size(); n++)
      s += "\n"+shipments.get(n);
    return header+s;
  }

  // Here is how to generate a starting time in the utter absence of real data
  private int getStartTime () {
    Random rn = new Random();
    int hour = rn.nextInt(13);
    int minute = rn.nextInt(59);
    return (hour*100)+minute;
  }


  // I quickly tired of the pain of using a native Java Date representation of
  // time (in hhmm format), so have devised a lightweight method that will take
  // a time in hhmm format and add a user-specified duration (in minutes) to
  // arrive at new time in hhmm format. It's faster than using SimpleDateFormat
  // and Date as well!
  private static int addMinutes (int startTime, int duration) {
    // Break time in hhmm into hh and mm parts and convert to minutes...
    int hour = startTime/100;
    int minute = startTime-(hour*100);
    int minutes = (hour*60)+minute+duration;  // ...add duration to it...
    // ...and then convert back to hhmm format
    hour = minutes/60;
    minute = minutes-(hour*60);
    return (hour*100)+minute;
  }


  // getTripList returns a string containing the tour of this truck, one trip
  // per record. It is formatted in the input format required by TS, although
  // with several optional fields to the right of the required data.
  // @params seqnum : truck number (simply echoed back in the trip list),
  //         consolidateIntrazonalTrips : if true will omit intrazonal trips
  public String getTripList (int seqnum, boolean consolidateIntrazonalTrips) {
    String s = "";
    int nShipments = shipments.size(), tourMode = 9, tripMode = 9;
    float tripFactor = 1.20f;
    // Assume that the optimization of the truck stops has left the truck at
    // the first shipment origin (so we don't need to move the truck there from
    // currentZone)
    int departureTime = getStartTime();
    int origin = ((Shipment)shipments.get(0)).getOriginAlphaZone();
    int destination;
    for (int n=0; n<nShipments; n++) {
      destination = ((Shipment)shipments.get(n)).getDestinationAlphaZone();
      // If the user hasn't asked for intrazonal trips then suppress them
      if (consolidateIntrazonalTrips & (origin==destination)) continue;
      s += origin+","+
           departureTime+","+
           durations.get(n)+","+
           destination+","+
           tourMode+","+
           tripMode+","+
           tripFactor+" ,"+
           seqnum+","+
           truckType+","+
           carrierType+","+
           ((Shipment)shipments.get(n)).getCommodityCode()+","+
           ((Shipment)shipments.get(n)).getWeight()+"\n";
      // Note that duration includes both travel and dwell time, so no need to
      // separately account of the latter here
      departureTime = addMinutes(departureTime, ((Integer)durations.get(n)).intValue());
      origin = ((Shipment)shipments.get(n)).getDestinationAlphaZone();
    }
    return s;
  }

}

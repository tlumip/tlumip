package com.pb.despair.ct;

class Shipment {
  int originBetaZone, destinationBetaZone, originAlphaZone, destinationAlphaZone;
  double pounds, value;
  String sctg, modeOfTransport, transShipmentPattern;
  boolean transShipped;

  Shipment (String s, int ob, int oa, int db, int da, double p, double v, String m, boolean t, String tp) {
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
  }

  public int getOriginAlphaZone () { return originAlphaZone; }

  public int getDestinationAlphaZone () { return destinationAlphaZone; }

  public String getCommodityCode () { return sctg; }

  public float getWeight () { return (float)pounds; }

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
      " tsp="+transShipmentPattern;
  }


  public static String writeShipmentHeader () {
    return "#sctg,originBetaZone,originAlphaZone,destinationBetaZone,destinationAlphaZone,pounds,value,"+
      "modeOfTransport,transShipped,transShipmentPattern";
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
      transShipmentPattern;
  }

  public static void main (String[] args) {
    Shipment s = new Shipment("10", 20, 30, 40, 50, 60.0, 70.0, "STK", false, "undefined");
    System.out.println(s);

  }

}

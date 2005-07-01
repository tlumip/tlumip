package com.pb.tlumip.ct;

import java.util.*;
class ShipmentComparator implements Comparator {

  // This was my original compare() method, which simply sorted by alpha zone number
  //public int compare (Object o1, Object o2) {
  //  int o1a = ((Shipment)o1).originAlphaZone;
  //  int o2a = ((Shipment)o2).originAlphaZone;
  //  return (o1a<o2a ? -1 : (o1a==o2a ? 0 : 1));
  //}

  private static boolean isLessThan (String s1, String s2) {
    boolean b = false;
    int result = s1.compareTo(s2);
    if (result<0) b = true;
    return b;
  }

  private static boolean isLessThanOrEqualTo (String s1, String s2) {
    boolean b = false;
    int result = s1.compareTo(s2);
    if (result<=0) b = true;
    return b;
  }

  public int compare (Object o1, Object o2) {

    // The goal here is to sort in origin, sctg, destination order
    int o1_orig = ((Shipment)o1).originAlphaZone, o2_orig = ((Shipment)o2).originAlphaZone,
        o1_dest = ((Shipment)o1).destinationAlphaZone, o2_dest = ((Shipment)o2).destinationAlphaZone;
    String o1_sctg = ((Shipment)o1).sctg, o2_sctg = ((Shipment)o2).sctg;

    // Are they equal?
    if ((o1_orig==o2_orig) & (o1_sctg.equals(o2_sctg)) & (o1_dest==o2_dest)) return 0;

    // If not, successively order them
    if (o1_orig<o2_orig) return -1;
    if ((o1_orig<=o2_orig) & isLessThan(o1_sctg,o2_sctg)) return -1;
    if ((o1_orig<=o2_orig) & isLessThanOrEqualTo(o1_sctg,o2_sctg) & (o1_dest<o2_dest)) return -1;

    return 1;     // method gets here only when o2 truly should be ordered before o1
  }

}

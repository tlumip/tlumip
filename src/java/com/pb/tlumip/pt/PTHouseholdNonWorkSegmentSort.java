package com.pb.tlumip.pt;

/** 
 * Allows sorting of household array by nonWorkSegment
 * 
 * @author Steve Hansen
 * @version 1.0 12/01/2003
 * 
 */
public class PTHouseholdNonWorkSegmentSort extends PTHousehold{

     
     public int compareTo(Object household){

        PTHouseholdNonWorkSegmentSort h = (PTHouseholdNonWorkSegmentSort)household;
        
          if(this.calcNonWorkLogsumSegment()<h.calcNonWorkLogsumSegment()) return -1;
          else if(this.calcNonWorkLogsumSegment()>h.calcNonWorkLogsumSegment()) return 1;
          else return 0;
    }
}
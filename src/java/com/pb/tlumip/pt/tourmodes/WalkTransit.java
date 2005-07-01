package com.pb.tlumip.pt.tourmodes;
import com.pb.tlumip.model.Mode;
import com.pb.tlumip.model.ModeType;
import com.pb.tlumip.model.TravelTimeAndCost;
import com.pb.tlumip.pt.PersonTourModeAttributes;
import com.pb.tlumip.pt.TourModeParameters;

import org.apache.log4j.Logger;

/**  
 * Walk-Transit mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */


public class WalkTransit extends Mode {
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
//     public boolean isAvailable=true;
//     public boolean hasUtility=false;
     
//     double utility=0;

     public WalkTransit(){
         isAvailable = true;
                 hasUtility = false;
                 utility = 0.0D;
          alternativeName=new String("WalkTransit");
          type=ModeType.WALKTRANSIT;
     }
     
    /** Calculates utility of walk-transit mode
     * 
     * @param inbound - In-bound TravelTimeAndCost
     * @param outbound - Outbound TravelTimeAndCost
     * @param c - TourModeParameters
     * @param p - PersonTourModeAttributes
     */
     public void calcUtility(TravelTimeAndCost inbound, TravelTimeAndCost outbound,
           TourModeParameters c, PersonTourModeAttributes p){

          if(inbound.walkTransitInVehicleTime==0.0) isAvailable=false;
          if(outbound.walkTransitInVehicleTime==0.0) isAvailable=false;
          
          if(isAvailable){
               time= (inbound.walkTransitInVehicleTime+outbound.walkTransitInVehicleTime
                 + inbound.walkTransitShortFirstWaitTime
                 + outbound.walkTransitShortFirstWaitTime
                 + inbound.walkTransitLongFirstWaitTime
                 + outbound.walkTransitLongFirstWaitTime
                 + inbound.walkTransitTransferWaitTime
                 + outbound.walkTransitTransferWaitTime
                 + inbound.walkTransitWalkTime+outbound.walkTransitWalkTime);

               utility=(
              c.ivt*(inbound.walkTransitInVehicleTime+outbound.walkTransitInVehicleTime)
            + c.shfwt*(inbound.walkTransitShortFirstWaitTime
                 +outbound.walkTransitShortFirstWaitTime)
            + c.lgfwt*(inbound.walkTransitLongFirstWaitTime
                 +outbound.walkTransitLongFirstWaitTime)
            + c.xwt*(inbound.walkTransitTransferWaitTime
                 +outbound.walkTransitTransferWaitTime)
            + c.wlk*(inbound.walkTransitWalkTime+outbound.walkTransitWalkTime)
            + c.opclow*((inbound.walkTransitFare+outbound.walkTransitFare)*p.inclow)
            + c.opcmed*((inbound.walkTransitFare+outbound.walkTransitFare)*p.incmed)
            + c.opchi*((inbound.walkTransitFare+outbound.walkTransitFare)*p.inchi)
            + c.wktaw0*p.auwk0 + c.wktaw1*p.auwk1 + c.wktaw2*p.auwk2
            + c.wktap0*p.aupr0 + c.wktap1*p.aupr1 + c.wktap2*p.aupr2
            + c.wktstp*p.totalStops
               );
               hasUtility=true;
          };
     };
     public double getUtility(){
          if(!hasUtility){
               logger.fatal("Error: Utility not calculated for "+alternativeName+"\n");
              //TODO - log this error to the node exception file
               System.exit(1);
          };
          return utility;
     };
     

};


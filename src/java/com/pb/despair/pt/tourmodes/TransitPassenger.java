package com.pb.despair.pt.tourmodes;
import com.pb.despair.model.Mode;
import com.pb.despair.model.ModeType;
import com.pb.despair.model.TravelTimeAndCost;
import com.pb.despair.pt.PersonTourModeAttributes;
import com.pb.despair.pt.TourModeParameters;

import org.apache.log4j.Logger;
/**  
 * Transit Passenger Mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */


public class TransitPassenger extends Mode {
    final static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
//     public boolean isAvailable=true;
//     public boolean hasUtility=false;
//     double utility=0;

     public TransitPassenger(){
         isAvailable = true;
         hasUtility = false;
         utility = 0.0D;
          alternativeName=new String("TransitPassenger");
          type=ModeType.TRANSITPASSENGER;
     }
     
    /** Calculates utility of transit-passenger mode
     * 
     * @param inbound - In-bound TravelTimeAndCost
     * @param outbound - Outbound TravelTimeAndCost
     * @param c - TourModeParameters
     * @param p - PersonTourModeAttributes
     */
    
     public void calcUtility(TravelTimeAndCost inbound, TravelTimeAndCost outbound,
           TourModeParameters c, PersonTourModeAttributes p){

          if(inbound.walkTransitInVehicleTime==0.0) isAvailable=false;
          if(outbound.sharedRide2Time==0.0) isAvailable=false;
          if(p.tourPurpose=='b') isAvailable=false;
     
          if(isAvailable){
               time=(inbound.walkTransitInVehicleTime+outbound.sharedRide2Time
                    +inbound.walkTransitShortFirstWaitTime
                    +inbound.walkTransitLongFirstWaitTime
                    +inbound.walkTransitTransferWaitTime
                    +inbound.walkTransitWalkTime
               );
               utility=(
              c.ivt*(inbound.walkTransitInVehicleTime+outbound.sharedRide2Time)
            + c.shfwt*inbound.walkTransitShortFirstWaitTime
            + c.lgfwt*inbound.walkTransitLongFirstWaitTime
            + c.xwt*inbound.walkTransitTransferWaitTime
            + c.wlk*inbound.walkTransitWalkTime
            + c.opclow*((inbound.walkTransitFare)*p.inclow)
            + c.opcmed*((inbound.walkTransitFare)*p.incmed)
            + c.opchi*((inbound.walkTransitFare)*p.inchi)
            + c.opcpas*(outbound.sharedRide2Cost)
            + c.trpaw0*p.auwk0 + c.trpaw1*p.auwk1 + c.trpaw2*p.auwk2
            + c.trpap0*p.aupr0 + c.trpap1*p.aupr1 + c.trpap2*p.aupr2
            + c.trpstp*p.totalStops
            + c.trphh1*p.size1
            + c.trphh2*p.size2
            + c.trphh3p*p.size3p
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


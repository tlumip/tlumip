package com.pb.despair.pt.tourmodes;
import com.pb.despair.model.Mode;
import com.pb.despair.model.ModeType;
import com.pb.despair.model.TravelTimeAndCost;
import com.pb.despair.pt.PersonTourModeAttributes;
import com.pb.despair.pt.TourModeParameters;
import com.pb.despair.pt.ZoneAttributes;

import org.apache.log4j.Logger;

/**  
 * Passenger Transit mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

public class PassengerTransit extends Mode {
    final static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
     //public boolean isAvailable=true;
     //public boolean hasUtility=false;
     
     //double utility=0;

     public PassengerTransit(){
          isAvailable = true;
          hasUtility = false;
          utility = 0.0D;
          alternativeName=new String("PassengerTransit");
          type=ModeType.PASSENGERTRANSIT;
     }
     
    /** Calculates utility of passenger-transit mode
     * 
     * @param inbound - In-bound TravelTimeAndCost
     * @param outbound - Outbound TravelTimeAndCost
     * @param z - ZoneAttributes (Currently only parking cost)
     * @param c - TourModeParameters
     * @param p - PersonTourModeAttributes
     */
     public void calcUtility(TravelTimeAndCost inbound, TravelTimeAndCost outbound,
           ZoneAttributes z,TourModeParameters c, PersonTourModeAttributes p){

          if(inbound.sharedRide2Time==0.0) isAvailable=false;
          if(outbound.walkTransitInVehicleTime==0.0) isAvailable=false;
          if(p.tourPurpose=='b') isAvailable=false;

          if(isAvailable){
               time=(outbound.walkTransitInVehicleTime+inbound.sharedRide2Time
                    +outbound.walkTransitShortFirstWaitTime
                    +outbound.walkTransitLongFirstWaitTime
                    +outbound.walkTransitTransferWaitTime
                    +outbound.walkTransitWalkTime
               );
               utility=(
              c.ivt*(outbound.walkTransitInVehicleTime+inbound.sharedRide2Time)
            + c.shfwt*outbound.walkTransitShortFirstWaitTime
            + c.lgfwt*outbound.walkTransitLongFirstWaitTime
            + c.xwt*outbound.walkTransitTransferWaitTime
            + c.wlk*outbound.walkTransitWalkTime
            + c.opclow*((outbound.walkTransitFare)*p.inclow)
            + c.opcmed*((outbound.walkTransitFare)*p.incmed)
            + c.opchi*((outbound.walkTransitFare)*p.inchi)
            + c.opcpas*(inbound.sharedRide2Cost)
            + c.opcpas*(z.parkingCost*(p.primaryDuration/60))           
            + c.ptraw0*p.auwk0 + c.ptraw1*p.auwk1 + c.ptraw2*p.auwk2
            + c.ptrap0*p.aupr0 + c.ptrap1*p.aupr1 + c.ptrap2*p.aupr2
            + c.ptrstp*p.totalStops
            + c.ptrhh1*p.size1
            + c.ptrhh2*p.size2
            + c.ptrhh3p*p.size3p
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


package com.pb.despair.pt.tripmodes;
import com.pb.despair.model.Mode;
import com.pb.despair.model.ModeType;
import com.pb.despair.model.TravelTimeAndCost;
import com.pb.despair.pt.Activity;
import com.pb.despair.pt.PersonTripModeAttributes;
import com.pb.despair.pt.TripModeParameters;
import com.pb.despair.pt.ZoneAttributes;

import org.apache.log4j.Logger;
/**  
 * Passenger (two person shared ride) mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class SharedRide2 extends Mode {
    final static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
     
//     public boolean isAvailable=true;
//     public boolean hasUtility=false;
//     double utility=0;
     public SharedRide2(){
         isAvailable = true;
                 hasUtility = false;
                 utility = 0.0D;
          alternativeName=new String("SharedRide2");
          type=ModeType.SHAREDRIDE2;
     }
     
    /** Calculates utility of two person shared ride mode
     * 
     * @param z - ZoneAttributes (Currently only parking cost)
     * @param c - TourModeParameters
     * @param p - PersonTourModeAttributes
     */
     
     public void calcUtility(TravelTimeAndCost tc, ZoneAttributes z,TripModeParameters c, PersonTripModeAttributes p, Mode tourMode,
          Activity thisActivity){

              hasUtility = false;
              utility=-999;
              isAvailable = true;

          if(tc.sharedRide2Time==0) isAvailable=false;
          if(tourMode.type!=ModeType.AUTODRIVER && tourMode.type!=ModeType.AUTOPASSENGER
             && tourMode.type!=ModeType.TRANSITPASSENGER && tourMode.type!=ModeType.PASSENGERTRANSIT)
               isAvailable=false;
               
          int autoDriver=0;
          if(tourMode.type==ModeType.AUTODRIVER)
               autoDriver=1;
               
          if(isAvailable){
               time=tc.sharedRide2Time;
               utility=(
                 c.ivt*tc.sharedRide2Time
                 + c.opcpas*tc.sharedRide2Cost
                 + c.opcpas*(z.parkingCost*(thisActivity.duration/60))
                 + c.sr2hh2*p.size2
                 + c.sr2hh3p*p.size3p
                  + c.driverSr2*autoDriver
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
     

}


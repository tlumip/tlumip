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
package com.pb.tlumip.pt.tourmodes;
import com.pb.tlumip.model.Mode;
import com.pb.tlumip.model.ModeType;
import com.pb.tlumip.model.TravelTimeAndCost;
import com.pb.tlumip.pt.PersonTourModeAttributes;
import com.pb.tlumip.pt.TourModeParameters;
import com.pb.tlumip.pt.ZoneAttributes;

import org.apache.log4j.Logger;
/** 
 * Passenger mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class AutoPassenger extends Mode {
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
    // public String alternativeName="AutoPassenger";

     //public boolean isAvailable=true;
    // public boolean hasUtility=false;
    // double utility=0;
     
    public AutoPassenger() {
        isAvailable = true;
        hasUtility = false;
        utility = 0.0D;
        alternativeName = new String("AutoPassenger");
        type = ModeType.AUTOPASSENGER;
    }

     /** Calculates Utility of Auto Passenger mode
      * 
      * @param inbound - In-bound TravelTimeAndCost
      * @param outbound - Outbound TravelTimeAndCost
      * @param z - ZoneAttributes (Currently only parking cost)
      * @param c - TourModeParameters
      * @param p - PersonTourModeAttributes
      */
     public void calcUtility(TravelTimeAndCost inbound, TravelTimeAndCost outbound,
          ZoneAttributes z,TourModeParameters c, PersonTourModeAttributes p){
               
          if(inbound.sharedRide2Time==0) isAvailable=false;
          if(outbound.sharedRide2Time==0) isAvailable=false;
               
          if(isAvailable){
               time=inbound.sharedRide2Time + outbound.sharedRide2Time;
               utility=(
                 c.ivt*(inbound.sharedRide2Time+outbound.sharedRide2Time)
                 + c.opcpas*(inbound.sharedRide2Cost+outbound.sharedRide2Cost)
                 + c.opcpas*(z.parkingCost*(p.primaryDuration/60))
                 + c.pasaw0*p.auwk0 + c.pasaw1*p.auwk1 + c.pasaw2*p.auwk2
                 + c.pasap0*p.aupr0 + c.pasap1*p.aupr1 + c.pasap2*p.aupr2
                 + c.passtp*p.totalStops
                 + c.pashh1*p.size1
                 + c.pashh2*p.size2
                 + c.pashh3p*p.size3p
               );
               hasUtility=true;
          };
               
     };
    /** get the utility of auto passenger */
     public double getUtility(){
          if(!hasUtility){
               logger.fatal("Error: Utility not calculated for "+alternativeName+"\n");
              //TODO - log this error to the node exception log file
               System.exit(1);
          };
          return utility;
     };
     
}


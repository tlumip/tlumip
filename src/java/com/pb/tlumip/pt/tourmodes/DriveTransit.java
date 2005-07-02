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

import org.apache.log4j.Logger;
/**  
 * Drive Transit mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class DriveTransit extends Mode {
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
//     public boolean isAvailable=true;
//     public boolean hasUtility=false;
//     double utility=0;


     public DriveTransit(){
          isAvailable = true;
          hasUtility = false;
          utility = 0.0D;
          alternativeName=new String("DriveTransit");
          type=ModeType.DRIVETRANSIT;
     }
     
     /** Calculate Drive Transit utility
      * 
      * @param inbound Inbound TravelTimeAndCost
      * @param outbound Outbound TravelTimeAndCost
      * @param c TourModeParameters
      * @param p PersonTourModeAttributes
      */
     public void calcUtility(TravelTimeAndCost inbound, TravelTimeAndCost outbound,
           TourModeParameters c, PersonTourModeAttributes p){

          //check transit availability
          if(inbound.driveTransitInVehicleTime==0) isAvailable=false;
          if(outbound.driveTransitInVehicleTime==0) isAvailable=false;
          
          //only available for work tours
          if(p.tourPurpose !='w') isAvailable=false;

          if(isAvailable){
               time=
                    (inbound.driveTransitInVehicleTime+outbound.driveTransitInVehicleTime
                 + inbound.driveTransitDriveTime+outbound.driveTransitDriveTime
                 + inbound.driveTransitShortFirstWaitTime
                 + outbound.driveTransitShortFirstWaitTime
                 + inbound.driveTransitLongFirstWaitTime
                 + outbound.driveTransitLongFirstWaitTime
                 + inbound.driveTransitTransferWaitTime
                 + outbound.driveTransitTransferWaitTime);

               utility=(
              c.ivt*(inbound.driveTransitInVehicleTime+outbound.driveTransitInVehicleTime)
            + c.dvt*(inbound.driveTransitDriveTime+outbound.driveTransitDriveTime)
            + c.shfwt*(inbound.driveTransitShortFirstWaitTime
                 + outbound.driveTransitShortFirstWaitTime)
            + c.lgfwt*(inbound.driveTransitLongFirstWaitTime
                 + outbound.driveTransitLongFirstWaitTime)
            + c.xwt*(inbound.driveTransitTransferWaitTime
                 + outbound.driveTransitTransferWaitTime)
            + c.wlk*(inbound.driveTransitWalkTime+outbound.driveTransitWalkTime)
            + c.opclow*((inbound.driveTransitFare+outbound.driveTransitFare)*p.inclow)
            + c.opcmed*((inbound.driveTransitFare+outbound.driveTransitFare)*p.incmed)
            + c.opchi*((inbound.driveTransitFare+outbound.driveTransitFare)*p.inchi)
            + c.opclow*((inbound.driveTransitDriveCost+outbound.driveTransitDriveCost)*p.inclow)
            + c.opcmed*((inbound.driveTransitDriveCost+outbound.driveTransitDriveCost)*p.incmed)
            + c.opchi*((inbound.driveTransitDriveCost+outbound.driveTransitDriveCost)*p.inchi)
            + c.drtaw0*p.auwk0 + c.drtaw1*p.auwk1 + c.drtaw2*p.auwk2
            + c.drtap0*p.aupr0 + c.drtap1*p.aupr1 + c.drtap2*p.aupr2
            + c.drtstp*p.totalStops
                  );
               hasUtility=true;
          };
     };
    /** Get drive transit utility */
     public double getUtility(){
          if(!hasUtility){
               logger.fatal("Error: Utility not calculated for "+alternativeName+"\n");
              //TODO - log this error to the node exception file
               System.exit(1);
          };
          return utility;
     };

};


package com.pb.despair.pt.tripmodes;
import com.pb.despair.model.Mode;
import com.pb.despair.model.ModeType;
import com.pb.despair.model.TravelTimeAndCost;
import com.pb.despair.pt.Activity;
import com.pb.despair.pt.PersonTripModeAttributes;
import com.pb.despair.pt.TripModeParameters;
import com.pb.despair.pt.ZoneAttributes;

import java.util.logging.Logger;
/** 
 * Driver alone mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class DriveAlone extends Mode {
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
     
//     public boolean isAvailable=true;
//     public boolean hasUtility=false;
     
 //    double utility=0;

     public DriveAlone(){
         isAvailable = true;
                hasUtility = false;
                utility = 0.0D;
          alternativeName=new String("DriveAlone");
          type=ModeType.DRIVEALONE;
     }
     
    /** Calculates utility of driving alone
     * 
     * @param inbound - In-bound TravelTimeAndCost
     * @param outbound - Outbound TravelTimeAndCost
     * @param z - ZoneAttributes (Currently only parking cost)
     * @param c - TourModeParameters
     * @param p - PersonTourModeAttributes
     */
     public void calcUtility(TravelTimeAndCost tc, ZoneAttributes z,TripModeParameters c, PersonTripModeAttributes p,Mode tourMode,
          Activity thisActivity){
               
               if(p.age<16) isAvailable=false;
               if(p.autos==0) isAvailable=false;
               if(tourMode.type!=ModeType.AUTODRIVER) isAvailable=false;
               
               if(isAvailable){
                    time=(tc.driveAloneTime);
                    utility=(
                      c.ivt*(tc.driveAloneTime )
                 + c.opclow*(tc.driveAloneCost*p.inclow)
                 + c.opcmed*(tc.driveAloneCost*p.incmed)
                 + c.opchi* (tc.driveAloneCost*p.inchi)
                 + c.pkglow*((z.parkingCost*(thisActivity.duration/60))*p.inclow)
                 + c.pkgmed*((z.parkingCost*(thisActivity.duration/60))*p.incmed)
                 + c.pkghi*((z.parkingCost*(thisActivity.duration/60))*p.inchi)
                    );
               hasUtility=true;
               };
     };
    /** Get drive alone utility */
     public double getUtility(){
          if(!hasUtility){
               logger.severe("Error: Utility not calculated for "+alternativeName+"\n");
               
                System.exit(1);
          };
          return utility;
     };
     

}


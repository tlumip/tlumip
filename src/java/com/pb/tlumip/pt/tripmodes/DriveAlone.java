package com.pb.tlumip.pt.tripmodes;

import com.pb.tlumip.model.Mode;
import com.pb.tlumip.model.ModeType;
import com.pb.tlumip.model.TravelTimeAndCost;
import com.pb.tlumip.pt.Activity;
import com.pb.tlumip.pt.PersonTripModeAttributes;
import com.pb.tlumip.pt.TripModeParameters;
import com.pb.tlumip.pt.ZoneAttributes;

import org.apache.log4j.Logger;
/** 
 * Driver alone mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class DriveAlone extends Mode {
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
     
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
     * @param z - ZoneAttributes (Currently only parking cost)
     * @param c - TourModeParameters
     * @param p - PersonTourModeAttributes
     */
     public void calcUtility(TravelTimeAndCost tc, ZoneAttributes z,TripModeParameters c, PersonTripModeAttributes p,Mode tourMode,
          Activity thisActivity){

              hasUtility = false;
              utility=-999;
              isAvailable = true;

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
               logger.fatal("Error: Utility not calculated for "+alternativeName+"\n");
              //TODO - log this error to the node exception file
               System.exit(1);
          };
          return utility;
     };
     

}


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
 * Driver alone mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class WalkTrip extends Mode {
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
     public WalkTrip(){
          alternativeName=new String("WalkTrip");
          type=ModeType.WALKTRIP;
     }

}


package com.pb.despair.pt.tripmodes;
import com.pb.despair.model.Mode;
import com.pb.despair.model.ModeType;

import java.util.logging.Logger;
/** 
 * Driver alone mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class DriveTransitTrip extends Mode {
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
    public DriveTransitTrip(){
          alternativeName=new String("DriveTransitTrip");
          type=ModeType.DRIVETRANSITTRIP;
     }
     
}


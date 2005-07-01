package com.pb.tlumip.pt.tripmodes;

import com.pb.tlumip.model.Mode;
import com.pb.tlumip.model.ModeType;


import org.apache.log4j.Logger;
/** 
 * Driver alone mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class WalkTrip extends Mode {
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
     public WalkTrip(){
          alternativeName=new String("WalkTrip");
          type=ModeType.WALKTRIP;
     }

}


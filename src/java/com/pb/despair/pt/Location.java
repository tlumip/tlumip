package com.pb.despair.pt;
import org.apache.log4j.Logger;
import java.io.Serializable;
/** 
 * A class that represents an activity location
 * 
 * @see OtherClasses
 * @author Joel Freedman
 * @version  1.0, 12/1/2003
 * 
 */
public class Location implements Serializable{
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
     //attributes
     public int zoneNumber;
     int gridCell;

     public Location(){
     
     }

     public void print(){
          logger.info("\tzoneNumber: "+zoneNumber);
          logger.info("\tgridCell:  "+gridCell);
     }
}

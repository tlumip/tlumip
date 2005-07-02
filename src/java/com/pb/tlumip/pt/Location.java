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
package com.pb.tlumip.pt;
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
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
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

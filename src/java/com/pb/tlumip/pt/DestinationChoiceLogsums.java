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
import com.borland.dx.dataset.TableDataSet;
import com.pb.common.datastore.DataManager;

import java.util.Hashtable;
import org.apache.log4j.Logger;

/** 
 * A class that contains a set of destination choice logsums
 * for a specific purpose and market segment
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class DestinationChoiceLogsums {
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
    public Hashtable logsums = new Hashtable();

    public DestinationChoiceLogsums(){
    }
    /** Reads DestinationChoiceLogsums from jDataStore
     * 
     * @param tableName Name of table to be read from JDataStore
     * @param purpose Trip purpose
     * @param segment Market segment
     */
    public void readFromJDataStore(String tableName, String purpose, int segment){
       if(logger.isDebugEnabled()) {
           logger.debug("Getting table: "+tableName+" "+purpose+" "+segment);
       }
       try {
                DataManager dm = new DataManager();  //Create a data manager, connect to default data-store
             TableDataSet table = dm.getTableDataSet(tableName);
 
            while (table.inBounds()) {

                 String thisPurpose = table.getString("Purpose");
                 if(thisPurpose.equals(purpose)==false){
                      table.next();
                      continue;
                 }
                 int thisSegment = table.getInt("Segment");
                 if(thisSegment!=segment){
                      table.next();
                      continue;
                 }
                     int zone = table.getInt("ZoneNumber");
                 double logsum = table.getDouble("Logsum");
                 
                 
//                 System.out.println("Found "+thisPurpose+" segment "+thisSegment+" zone "+zone+" logsum "+logsum);
                 logsums.put(new Integer(zone),new Double(logsum));
                 table.next();
                 
                         
            }           
            DataManager.closeTable (table);                            //Close table     
               dm.closeStore();                                                               
//        } catch (com.borland.dx.dataset.DataSetException e) {            
        } catch (Exception e) {            
              System.err.println("Error: printTable()");                   
            e.printStackTrace();                                         
        } 
     };                        
    
         
}

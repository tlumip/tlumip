package com.pb.despair.pt;
import com.borland.dx.dataset.TableDataSet;
import com.pb.common.datastore.DataManager;

import java.util.Hashtable;
import java.util.logging.Logger;

/** 
 * A class that contains a set of destination choice logsums
 * for a specific purpose and market segment
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class DestinationChoiceLogsums {
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
    boolean debug = false;
    public Hashtable logsums = new Hashtable();

    public DestinationChoiceLogsums(){
         logger.fine("New DC Logsums created");
    };
    /** Reads DestinationChoiceLogsums from jDataStore
     * 
     * @param tableName Name of table to be read from JDataStore
     * @param purpose Trip purpose
     * @param segment Market segment
     */
    public void readFromJDataStore(String tableName, String purpose, int segment){
       if(debug) logger.fine("Getting table: "+tableName+" "+purpose+" "+segment);
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

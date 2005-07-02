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
import com.pb.common.util.ResourceUtil;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import java.io.File;
import java.io.IOException;

import java.util.ResourceBundle;
import org.apache.log4j.Logger;
/**  
 * A class to access tour mode parameters from JDataStore
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class TourModeParametersData {
    
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.default");
    String tourModeParametersTableName="TourModeParameters";
    //DataManager dm = new DataManager();  //Create a data manager, connect to default data-store
     
     //a hashtable of taz objects
     private TourModeParameters[] tourModeParameters = new TourModeParameters[ActivityPurpose.ACTIVITY_PURPOSES.length];
     
     public TourModeParameters getTourModeParameters(int activityPurpose){
        return tourModeParameters[activityPurpose];
     }
     
     public TableDataSet loadTableDataSet(ResourceBundle rb, String fileName){

          if(logger.isDebugEnabled()) {
              logger.debug("Adding table TourModeParameters");
          }
          try {
          	  String file = ResourceUtil.getProperty(rb, fileName);
              CSVFileReader reader = new CSVFileReader();
              TableDataSet table = reader.readFile(new File(file));
             return table;
         } catch (IOException e) {
             logger.fatal("Can't find TourModeParameters input table " + fileName);
             e.printStackTrace();
         }
         return null;
          
     };     
     
     public void readData(ResourceBundle rb, String fileName){
          
          if(logger.isDebugEnabled()) {
              logger.debug("\nGetting table: "+tourModeParametersTableName);
          }
          TableDataSet table = loadTableDataSet(rb, fileName);
    try{          
    
          for(int rowNumber = 1; rowNumber <=table.getRowCount(); rowNumber++) {
                 
                TourModeParameters thisPurpose = new TourModeParameters();
                thisPurpose.purpose =     table.getStringValueAt(rowNumber, table.getColumnPosition("Purpose")); 
                thisPurpose.ivt =       table.getValueAt(rowNumber, table.getColumnPosition("ivt"));
                thisPurpose.wlk =          table.getValueAt(rowNumber, table.getColumnPosition("wlk"));
                thisPurpose.shfwt =     table.getValueAt(rowNumber, table.getColumnPosition("shfwt"));
                     thisPurpose.lgfwt =     table.getValueAt(rowNumber, table.getColumnPosition("lgfwt"));
                     thisPurpose.xwt =       table.getValueAt(rowNumber, table.getColumnPosition("xwt"));
                     thisPurpose.dvt =       table.getValueAt(rowNumber, table.getColumnPosition("dvt"));
                     thisPurpose.shwlk =     table.getValueAt(rowNumber, table.getColumnPosition("shwlk"));
                     thisPurpose.lgwlk =     table.getValueAt(rowNumber, table.getColumnPosition("lgwlk"));
                    thisPurpose.bmt =       table.getValueAt(rowNumber, table.getColumnPosition("bmt"));
                    thisPurpose.pkglow =    table.getValueAt(rowNumber, table.getColumnPosition("pkglow"));
                    thisPurpose.pkgmed =    table.getValueAt(rowNumber, table.getColumnPosition("pkgmed"));
                    thisPurpose.pkghi =     table.getValueAt(rowNumber, table.getColumnPosition("pkghi"));
                    thisPurpose.opclow =    table.getValueAt(rowNumber, table.getColumnPosition("opclow"));
                    thisPurpose.opcmed =    table.getValueAt(rowNumber, table.getColumnPosition("opcmed"));
                    thisPurpose.opchi =     table.getValueAt(rowNumber, table.getColumnPosition("opchi"));
                    thisPurpose.opcpas =    table.getValueAt(rowNumber, table.getColumnPosition("opcpas"));
                    thisPurpose.passtp =    table.getValueAt(rowNumber, table.getColumnPosition("passtp"));
                    thisPurpose.wlkstp =    table.getValueAt(rowNumber, table.getColumnPosition("wlkstp"));
                    thisPurpose.bikstp =    table.getValueAt(rowNumber, table.getColumnPosition("bikstp"));
                    thisPurpose.wktstp =    table.getValueAt(rowNumber, table.getColumnPosition("wktstp"));
                    thisPurpose.trpstp =    table.getValueAt(rowNumber, table.getColumnPosition("trpstp"));
                    thisPurpose.ptrstp =    table.getValueAt(rowNumber, table.getColumnPosition("ptrstp"));
                    thisPurpose.drtstp =    table.getValueAt(rowNumber, table.getColumnPosition("drtstp"));
                    thisPurpose.pasgt16 =   table.getValueAt(rowNumber, table.getColumnPosition("pasgt16"));
                    thisPurpose.pashh1 =    table.getValueAt(rowNumber, table.getColumnPosition("pashh1"));
                    thisPurpose.pashh2 =    table.getValueAt(rowNumber, table.getColumnPosition("pashh2"));
                    thisPurpose.pashh3p =   table.getValueAt(rowNumber, table.getColumnPosition("pashh3p"));
                    thisPurpose.trphh1 =    table.getValueAt(rowNumber, table.getColumnPosition("trphh1"));
                    thisPurpose.trphh2 =    table.getValueAt(rowNumber, table.getColumnPosition("trphh2"));
                    thisPurpose.trphh3p =   table.getValueAt(rowNumber, table.getColumnPosition("trphh3p"));
                    thisPurpose.ptrhh1 =    table.getValueAt(rowNumber, table.getColumnPosition("ptrhh1"));
                    thisPurpose.ptrhh2 =    table.getValueAt(rowNumber, table.getColumnPosition("ptrhh2"));
                    thisPurpose.ptrhh3p =   table.getValueAt(rowNumber, table.getColumnPosition("ptrhh3p"));
                    thisPurpose.dra1619 =   table.getValueAt(rowNumber, table.getColumnPosition("dra1619"));
                    thisPurpose.tra10 =     table.getValueAt(rowNumber, table.getColumnPosition("tra10"));
                    thisPurpose.noon =      table.getValueAt(rowNumber, table.getColumnPosition("noon"));
                    thisPurpose.drvaw0 =    table.getValueAt(rowNumber, table.getColumnPosition("drvaw0"));
                    thisPurpose.drvaw1 =    table.getValueAt(rowNumber, table.getColumnPosition("drvaw1"));
                    thisPurpose.drvaw2 =    table.getValueAt(rowNumber, table.getColumnPosition("drvaw2"));
                    thisPurpose.pasaw0 =    table.getValueAt(rowNumber, table.getColumnPosition("pasaw0"));
                    thisPurpose.pasaw1 =    table.getValueAt(rowNumber, table.getColumnPosition("pasaw1"));
                    thisPurpose.pasaw2 =    table.getValueAt(rowNumber, table.getColumnPosition("pasaw2"));
                    thisPurpose.wlkaw0 =    table.getValueAt(rowNumber, table.getColumnPosition("wlkaw0"));
                    thisPurpose.wlkaw1 =    table.getValueAt(rowNumber, table.getColumnPosition("wlkaw1"));
                    thisPurpose.wlkaw2 =    table.getValueAt(rowNumber, table.getColumnPosition("wlkaw2"));
                    thisPurpose.bikaw0 =    table.getValueAt(rowNumber, table.getColumnPosition("bikaw0"));
                    thisPurpose.bikaw1 =    table.getValueAt(rowNumber, table.getColumnPosition("bikaw1"));
                    thisPurpose.bikaw2 =    table.getValueAt(rowNumber, table.getColumnPosition("bikaw2"));
                    thisPurpose.wktaw0 =    table.getValueAt(rowNumber, table.getColumnPosition("wktaw0"));
                    thisPurpose.wktaw1 =    table.getValueAt(rowNumber, table.getColumnPosition("wktaw1"));
                    thisPurpose.wktaw2 =    table.getValueAt(rowNumber, table.getColumnPosition("wktaw2"));
                    thisPurpose.trpaw0 =    table.getValueAt(rowNumber, table.getColumnPosition("trpaw0"));
                    thisPurpose.trpaw1 =    table.getValueAt(rowNumber, table.getColumnPosition("trpaw1"));
                    thisPurpose.trpaw2 =    table.getValueAt(rowNumber, table.getColumnPosition("trpaw2"));
                    thisPurpose.ptraw0 =    table.getValueAt(rowNumber, table.getColumnPosition("ptraw0"));
                    thisPurpose.ptraw1 =    table.getValueAt(rowNumber, table.getColumnPosition("ptraw1"));
                    thisPurpose.ptraw2 =    table.getValueAt(rowNumber, table.getColumnPosition("ptraw2"));
                    thisPurpose.drtaw0 =    table.getValueAt(rowNumber, table.getColumnPosition("drtaw0"));
                    thisPurpose.drtaw1 =    table.getValueAt(rowNumber, table.getColumnPosition("drtaw1"));
                    thisPurpose.drtaw2 =    table.getValueAt(rowNumber, table.getColumnPosition("drtaw2"));
                    thisPurpose.drvap0 =    table.getValueAt(rowNumber, table.getColumnPosition("drvap0"));
                    thisPurpose.drvap1 =    table.getValueAt(rowNumber, table.getColumnPosition("drvap1"));
                    thisPurpose.drvap2 =    table.getValueAt(rowNumber, table.getColumnPosition("drvap2"));
                    thisPurpose.pasap0 =    table.getValueAt(rowNumber, table.getColumnPosition("pasap0"));
                    thisPurpose.pasap1 =    table.getValueAt(rowNumber, table.getColumnPosition("pasap1"));
                    thisPurpose.pasap2 =    table.getValueAt(rowNumber, table.getColumnPosition("pasap2"));
                thisPurpose.wlkap0 =    table.getValueAt(rowNumber, table.getColumnPosition("wlkap0"));
                thisPurpose.wlkap1 =    table.getValueAt(rowNumber, table.getColumnPosition("wlkap1"));
                thisPurpose.wlkap2 =    table.getValueAt(rowNumber, table.getColumnPosition("wlkap2"));
                thisPurpose.bikap0 =    table.getValueAt(rowNumber, table.getColumnPosition("bikap0"));
                thisPurpose.bikap1 =    table.getValueAt(rowNumber, table.getColumnPosition("bikap1"));
                thisPurpose.bikap2 =    table.getValueAt(rowNumber, table.getColumnPosition("bikap2"));
                thisPurpose.wktap0 =    table.getValueAt(rowNumber, table.getColumnPosition("wktap0"));
                thisPurpose.wktap1 =    table.getValueAt(rowNumber, table.getColumnPosition("wktap1"));
                thisPurpose.wktap2 =    table.getValueAt(rowNumber, table.getColumnPosition("wktap2"));
                thisPurpose.trpap0 =    table.getValueAt(rowNumber, table.getColumnPosition("trpap0"));
                thisPurpose.trpap1 =    table.getValueAt(rowNumber, table.getColumnPosition("trpap1"));
                thisPurpose.trpap2 =    table.getValueAt(rowNumber, table.getColumnPosition("trpap2"));
                thisPurpose.ptrap0 =    table.getValueAt(rowNumber, table.getColumnPosition("ptrap0"));
                thisPurpose.ptrap1 =    table.getValueAt(rowNumber, table.getColumnPosition("ptrap1"));
                thisPurpose.ptrap2 =    table.getValueAt(rowNumber, table.getColumnPosition("ptrap2"));
                thisPurpose.drtap0 =    table.getValueAt(rowNumber, table.getColumnPosition("drtap0"));
                thisPurpose.drtap1 =    table.getValueAt(rowNumber, table.getColumnPosition("drtap1"));
                thisPurpose.drtap2 =    table.getValueAt(rowNumber, table.getColumnPosition("drtap2"));
                thisPurpose.nestlow =   table.getValueAt(rowNumber, table.getColumnPosition("nestlow"));
                
                int purposeNumber = ActivityPurpose.getActivityPurposeValue(thisPurpose.purpose.charAt(0));                
                tourModeParameters[purposeNumber] = thisPurpose;                                              
            }                                                            
     } catch (Exception e) {            
           System.err.println("Error: printTable()");                   
         e.printStackTrace();                                                           
     }
     }                                                                          
    public static void main (String[] args) throws Exception {
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
         TourModeParametersData tmp = new TourModeParametersData();
         tmp.readData(rb,"tourModeParameters.file");
         System.exit(1);                                                  
    };                                                                   
                                                                         
}                                                                        

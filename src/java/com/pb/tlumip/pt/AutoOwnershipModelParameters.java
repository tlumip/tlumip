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
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;


/** A class that contains parameters for the Auto Ownership Model
 * 
 * @author Joel Freedman
 */
public class AutoOwnershipModelParameters {
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.AutoOwnershipModelParameters");
     String autoOwnershipParametersTableName="AutoOwnershipParameters";

    public double auto0Con;
    public double hhsize10;
    public double hhsize20;
    public double worker00;
    public double worker10;
    public double income10;
    public double income20;
    public double sfdwell0;
    public double tot25t0;
    public double auto1Con;
    public double hhsize11;
    public double hhsize21;
    public double hhsize31;
    public double worker01;
    public double worker11;
    public double income11;
    public double income21;
    public double income31;
    public double sfdwell1;
    public double tot25t1;
    public double auto2Con;
    public double hhsize22;
    public double hhsize32;
    public double worker12;
    public double worker22;
    public double worker32;
    public double income22;
    public double income32;
    public double income42;
    public double sfdwell2;

     public AutoOwnershipModelParameters(){
        
        auto0Con=0;
        hhsize10=0;
        hhsize20=0;
        worker00=0;
        worker10=0;
        income10=0;
        income20=0;
        sfdwell0=0;
        tot25t0=0;
        auto1Con=0;
        hhsize11=0;
        hhsize21=0;
        hhsize31=0;
        worker01=0;
        worker11=0;
        income11=0;
        income21=0;
        income31=0;
        sfdwell1=0;
        tot25t1=0;
        auto2Con=0;
        hhsize22=0;
        hhsize32=0;
        worker12=0;
        worker22=0;
        worker32=0;
        income22=0;
        income32=0;
        income42=0;
        sfdwell2=0;        
        };
   
   
     
        public static TableDataSet loadTableDataSet(ResourceBundle rb, String fileName){
            try {
               CSVFileReader reader = new CSVFileReader();
               String file = ResourceUtil.getProperty(rb, fileName);   
               TableDataSet table = reader.readFile(new File(file));
               return table;
            } catch (IOException e) {
               logger.fatal("Can't find Patterns input table " + fileName);
               e.printStackTrace();
              }
            return null;
        }
         
        public void readData(ResourceBundle rb, String fileName){
          
            if(logger.isDebugEnabled()) {
                logger.debug("Getting table: "+fileName);
            }
            TableDataSet table = loadTableDataSet(rb, fileName);
         
            for(int rowNumber=1;rowNumber<=table.getRowCount();rowNumber++) {
                auto0Con= table.getValueAt(rowNumber,table.getColumnPosition("auto0Con"));
                auto0Con=table.getValueAt(rowNumber,table.getColumnPosition("auto0Con"));
                hhsize10=table.getValueAt(rowNumber,table.getColumnPosition("hhsize10"));
                hhsize20=table.getValueAt(rowNumber,table.getColumnPosition("hhsize20"));
                worker00=table.getValueAt(rowNumber,table.getColumnPosition("worker00"));
                worker10=table.getValueAt(rowNumber,table.getColumnPosition("worker10"));
                income10=table.getValueAt(rowNumber,table.getColumnPosition("income10"));
                income20=table.getValueAt(rowNumber,table.getColumnPosition("income20"));
                sfdwell0=table.getValueAt(rowNumber,table.getColumnPosition("sfdwell0"));
                tot25t0=table.getValueAt(rowNumber,table.getColumnPosition("tot25t0"));
                auto1Con=table.getValueAt(rowNumber,table.getColumnPosition("auto1Con"));
                hhsize11=table.getValueAt(rowNumber,table.getColumnPosition("hhsize11"));
                hhsize21=table.getValueAt(rowNumber,table.getColumnPosition("hhsize21"));
                hhsize31=table.getValueAt(rowNumber,table.getColumnPosition("hhsize31"));
                worker01=table.getValueAt(rowNumber,table.getColumnPosition("worker01"));
                worker11=table.getValueAt(rowNumber,table.getColumnPosition("worker11"));
                income11=table.getValueAt(rowNumber,table.getColumnPosition("income11"));
                income21=table.getValueAt(rowNumber,table.getColumnPosition("income21"));
                income31=table.getValueAt(rowNumber,table.getColumnPosition("income31"));
                sfdwell1=table.getValueAt(rowNumber,table.getColumnPosition("sfdwell1"));
                tot25t1=table.getValueAt(rowNumber,table.getColumnPosition("tot25t1"));
                auto2Con=table.getValueAt(rowNumber,table.getColumnPosition("auto2Con"));
                hhsize22=table.getValueAt(rowNumber,table.getColumnPosition("hhsize22"));
                hhsize32=table.getValueAt(rowNumber,table.getColumnPosition("hhsize32"));
                worker12=table.getValueAt(rowNumber,table.getColumnPosition("worker12"));
                worker22=table.getValueAt(rowNumber,table.getColumnPosition("worker22"));
                worker32=table.getValueAt(rowNumber,table.getColumnPosition("worker32"));
                income22=table.getValueAt(rowNumber,table.getColumnPosition("income22"));
                income32=table.getValueAt(rowNumber,table.getColumnPosition("income32"));
                income42=table.getValueAt(rowNumber,table.getColumnPosition("income42"));
                sfdwell2=table.getValueAt(rowNumber,table.getColumnPosition("sfdwell2"));
            }      
        }                                                                  
}                                                                

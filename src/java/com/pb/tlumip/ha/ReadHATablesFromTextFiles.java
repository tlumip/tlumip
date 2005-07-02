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
package com.pb.tlumip.ha;

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.TableDataSet;

import java.util.ResourceBundle;

public class ReadHATablesFromTextFiles {
    public static void main(String[] args) {
        ResourceBundle rb = ResourceUtil.getResourceBundle( "ha" );
        String path = ResourceUtil.getProperty(rb, "population.path");

        TableDataSet householdFile = HAModel.reloadTableFromScratchFromTextFile(path, "SynPopH");
        boolean eof = false;
        do {
            for(int r=1;r<=householdFile.getRowCount();r++){
                int yrMoved = (int)householdFile.getValueAt(r,"YRMOVED");
                int newYrMoved = 0;
                switch (yrMoved) {
                    case 0:
                    case 1: newYrMoved = 1; break;
                    case 2: newYrMoved = 3; break;
                    case 3: newYrMoved = 8; break;
                    case 4: newYrMoved = 15; break;
                    case 5: newYrMoved = 25; break;
                    case 6:
                    default: newYrMoved = 35; break;
                }
                householdFile.setValueAt(r,householdFile.getColumnPosition("YRMOVED"), newYrMoved);
                if (r==householdFile.getRowCount()) eof=true;
            }
        } while (!eof);
        
        TableDataSet personFile = HAModel.reloadTableFromScratchFromTextFile(path,"SynPopP");
        eof=false;
        do {
            for(int r=1;r<=personFile.getRowCount();r++){
                int yrsSchool = (int)personFile.getValueAt(r,"YEARSCH");
                int age = (int)personFile.getValueAt(r,"AGE");
                int newYrSchool = 0;
                switch (yrsSchool) {
                    case 0:
                    case 1:
                    case 2:
                    case 3: newYrSchool = 0; break;
                    case 4: if (age <6) newYrSchool = 1;
                            else if (age > 9) newYrSchool = 4;
                            else newYrSchool = age-5;
                            break;
                    case 5:if (age <10) newYrSchool = 5;
                            else if (age > 13) newYrSchool = 8;
                            else newYrSchool = age-5;
                            break;
                    case 6: newYrSchool = 9; break;
                    case 7: newYrSchool = 10; break;
                    case 8:
                    case 9: newYrSchool = 11; break;
                    case 10: newYrSchool = 12; break;
                    case 11: newYrSchool = 12 + (int) Math.round(Math.random()*4); break;
                    case 12:
                    case 13:
                    case 16:
                    case 14: newYrSchool = 16; break;
                    case 15: newYrSchool = 18; break;
                    case 17: newYrSchool = 21; break;
                    default: newYrSchool = 12; break;
                }
                personFile.setValueAt(r,personFile.getColumnPosition("YEARSCH"), newYrSchool);
                if (r == personFile.getRowCount()) eof=true;
            }
        } while (!eof);
        System.exit(0);
    }
}

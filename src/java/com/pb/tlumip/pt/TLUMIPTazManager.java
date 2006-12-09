/*
 * Copyright 2006 PB Consult Inc.
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

import com.pb.models.pt.TazManager;
import com.pb.common.datafile.TableDataSet;
import com.pb.models.pt.Taz;

import java.util.ResourceBundle;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Oct 23, 2006
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class TLUMIPTazManager extends TazManager {

    public TLUMIPTazManager(){
        super();
    }



    public void setParkingCost(ResourceBundle rb, String fileName) {
        TableDataSet table = loadTableDataSet(rb, fileName);
        int workColumn = table.getColumnPosition("WorkParkingCost");
        int nonWorkColumn = table.getColumnPosition("NonWorkParkingCost");
        int aZoneColumn = table.getColumnPosition("ZoneNumber");
        for(int i=1;i<=table.getRowCount();i++){
            if(tazData.containsKey(new Integer((int)table.getValueAt(i,aZoneColumn)))){
                Taz thisTaz = tazData.get((int)table.getValueAt(i,aZoneColumn));
                thisTaz.workParkingCost = table.getValueAt(i,workColumn);
                thisTaz.nonWorkParkingCost = table.getValueAt(i,nonWorkColumn);
            }
        }
    }
}

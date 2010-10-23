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

import com.pb.common.datafile.TableDataSet;
import com.pb.models.pt.PriceConverter;
import com.pb.models.pt.Taz;
import com.pb.models.pt.TazManager;

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


    /**
     * Set parking costs.  Multiply costs by 100 to convert to cents.
     */

    public void setParkingCost(ResourceBundle appRb, ResourceBundle globalRb,String fileName) {
//        float conversionFactor = Float.parseFloat(globalRb.getString("cf.1990.to.2000.dollars"));
//        float conversionFactor = Float.parseFloat(globalRb.getString("convertTo2000Dollars"));
        PriceConverter priceConverter = PriceConverter.getInstance(appRb,globalRb);

        TableDataSet table = loadTableDataSet(globalRb, fileName);
        int workColumn = table.getColumnPosition("DayPark");
        int nonWorkColumn = table.getColumnPosition("HourPark");
        int aZoneColumn = table.getColumnPosition("Azone");

        for (int i = 1; i <= table.getRowCount(); i++) {
            if (tazData.containsKey((int)table.getValueAt(i, aZoneColumn))) {
                Taz thisTaz = tazData.get((int)table.getValueAt(i, aZoneColumn));
//                thisTaz.workParkingCost = (table.getValueAt(i, workColumn))*conversionFactor * 100;
//                thisTaz.nonWorkParkingCost = (table.getValueAt(i, nonWorkColumn))*conversionFactor * 100;
                //12/29/08 costs in dollars, not cents, now
//                thisTaz.workParkingCost = (table.getValueAt(i, workColumn))*conversionFactor;
//                thisTaz.nonWorkParkingCost = (table.getValueAt(i, nonWorkColumn))*conversionFactor;
                thisTaz.workParkingCost = priceConverter.convertPrice(table.getValueAt(i, workColumn),PriceConverter.ConversionType.PRICE);
                thisTaz.nonWorkParkingCost = priceConverter.convertPrice(table.getValueAt(i, nonWorkColumn),PriceConverter.ConversionType.PRICE);
            }
        }
    }

    public void addExternalZones(ResourceBundle globalRb){
    }
}

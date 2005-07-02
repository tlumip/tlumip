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
package com.pb.tlumip.pc.beans;

import java.util.ArrayList;

/**
 * Holds statistics for use in PC
 *
 * @author Christi Willison
 * @version Oct 29, 2003
 * Created by IntelliJ IDEA.
 */

public class StatisticBean {
    // will eventually get rid of these (along with
    //their getters and setters) but am not quite ready.
	private int iterationNumber = 0;
	private int commodityNumber = 0;
    private String commodityType = "";
    private String commodityType2 = "";
    private String commodityName = "";
	private double objFuncValue = 0.00;

    private ArrayList names = new ArrayList();
    private ArrayList values = new ArrayList();


    public void setValue(String fieldName, String value) {
        names.add(fieldName);
        values.add(value);
    }


    public String getValue(String fieldName) throws NoSuchFieldException {
        for(int i=0; i < names.size(); i++) {
            if( (names.get(i)).equals(fieldName) ) {
                return (String)values.get(i);
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    public int getIterationNumber() {
        return iterationNumber;

    }

    public void setIterationNumber(int iterationNumber) {
        this.iterationNumber = iterationNumber;
    }

    public int getCommodityNumber() {
        return commodityNumber;

    }

    public void setCommodityNumber(int commodityNumber) {
        this.commodityNumber = commodityNumber;
    }

    public String getCommodityType() {
        return commodityType;
    }

    public void setCommodityType(String commodityType) {
        this.commodityType = commodityType;
    }

    public String getCommodityType2() {
        return commodityType2;
    }

    public void setCommodityType2(String commodityType2) {
        this.commodityType2 = commodityType2;
    }

    public String getCommodityName() {
        return commodityName;
    }

    public void setCommodityName(String commodityName) {
        this.commodityName = commodityName;
    }

    public double getObjFuncValue() {
        return objFuncValue;
    }

    public void setObjFuncValue(double objFuncValue) {
        this.objFuncValue = objFuncValue;
    }

}

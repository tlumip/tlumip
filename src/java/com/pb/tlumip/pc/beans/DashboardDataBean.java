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
import java.util.HashMap;

/**
 * Holds data used in the dashboard displays
 *
 * @author Christi Willison
 * @version Oct 29, 2003
 * Created by IntelliJ IDEA.
 */
public class DashboardDataBean {

    protected HashMap map = new HashMap();

//    DashboardDataBeans can take a double or an int value but that value will
//    be stored as a double value because the Datasets formed from these values
//    require a double.

    public void setValue(String fieldName, double d) {
        map.put(fieldName,new Double(d));
    }

    public void setValue(String fieldName, int i) {
        map.put(fieldName,new Double(i));
    }

    public void setValue(String fieldName, String s){
        map.put(fieldName, s);
    }

    public double getDoubleValue(String fieldName) {
        return ((Double) map.get(fieldName)).doubleValue();
    }

    public String getStringValue(String fieldName){
        return (String) map.get(fieldName);
    }

}

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
package com.pb.tlumip.pc;

import com.pb.tlumip.pc.beans.DashboardDataBean;

import java.util.HashMap;

/**
 * Abstract DataCache used to hold Dashboard data
 *
 * @author Christi Willison
 * @version  Sep 29, 2003
 * Created by IntelliJ IDEA.
 */
public abstract class DataCache {

    protected HashMap map = new HashMap();


    public void addValue(DashboardDataBean bean){};

    public abstract DashboardDataBean[] getValues(String key);

    public void clearValues(String key){

    };
}

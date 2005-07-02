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
import com.pb.tlumip.pc.beans.StatisticBean;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * PI data cache used for commodity surplus graph
 *
 * @author Christi Willison
 * @version  Sep 29, 2003
 * Created by IntelliJ IDEA.
 */
public class PICommoditySurplusDataCache {

    protected HashMap surplusCache = new HashMap();
    private static PICommoditySurplusDataCache instance = new PICommoditySurplusDataCache();

    /** Keep this class from being created with "new".
     */
    private PICommoditySurplusDataCache() {
    }

    /** Return instances of this class.
     */
    public static PICommoditySurplusDataCache getInstance() {
        return instance;
    }

    public synchronized void addValue(DashboardDataBean bean) {
        String commodityName = bean.getStringValue(DashboardDataKey.COMMODITY_NAME);
        List beanList = getList(commodityName);
        beanList.add( bean );
    }

    public synchronized String[] getKeys(){
        return (String[]) surplusCache.keySet().toArray(new String[0]);
    }

    public synchronized DashboardDataBean[] getValues(String key) {
        List statList = getList(key);
        return (DashboardDataBean[]) statList.toArray( new DashboardDataBean[0] );
    }

    public synchronized void clearValues(String key) {
        surplusCache.remove(key);
    }

    private List getList(String key) {
        List beanList =  (List) surplusCache.get(key);

        if (beanList == null) {
            beanList = new ArrayList();
            surplusCache.put( key, beanList );
        }
        return beanList;
    }
}

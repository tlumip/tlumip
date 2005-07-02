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
 * PI data cache used for PI dashboard
 *
 * @author Christi Willison
 * @version  Sep 29, 2003
 * Created by IntelliJ IDEA.
 */
public class PICommodityPriceDataCache {

    protected HashMap priceCache = new HashMap();
    private static PICommodityPriceDataCache instance = new PICommodityPriceDataCache();

    /** Keep this class from being created with "new".
     */
    private PICommodityPriceDataCache() {
    }

    /** Return instances of this class.
     */
    public static PICommodityPriceDataCache getInstance() {
        return instance;
    }

    public synchronized void addValue(DashboardDataBean bean) {
        String commodityName = bean.getStringValue(DashboardDataKey.COMMODITY_NAME);
        List beanList = getList(commodityName);
        beanList.add( bean );
    }

    public synchronized DashboardDataBean[] getValues(String key) {
        List statList = getList(key);
        return (DashboardDataBean[]) statList.toArray( new DashboardDataBean[0] );
    }

    public synchronized String[] getKeys(){
        return (String[]) priceCache.keySet().toArray(new String[0]);
    }

    public synchronized void clearValues(String key) {
        priceCache.remove(key);
    }

    private List getList(String key) {
        List beanList =  (List) priceCache.get(key);

        if (beanList == null) {
            beanList = new ArrayList();
            priceCache.put( key, beanList );
        }
        return beanList;
    }
}

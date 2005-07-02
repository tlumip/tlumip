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
 * PI data cache used for floorspace graph
 *
 * @author Christi Willison
 * @version  Sep 29, 2003
 * Created by IntelliJ IDEA.
 */
public class PIIBSEFloorspaceDataCache {

    protected HashMap ibsefloorspaceCache = new HashMap();
    private static PIIBSEFloorspaceDataCache instance = new PIIBSEFloorspaceDataCache();

    /** Keep this class from being created with "new".
     */
    private PIIBSEFloorspaceDataCache() {
    }

    /** Return instances of this class.
     */
    public static PIIBSEFloorspaceDataCache getInstance() {
        return instance;
    }

    public synchronized void addValue(DashboardDataBean bean) {
        String commodityName = bean.getStringValue("commodityName");
        List beanList = getList(commodityName);
        beanList.add( bean );
    }

    public synchronized DashboardDataBean[] getValues(DashboardDataBean bean) {
        String commodityName="noName";
        commodityName=bean.getStringValue(DashboardDataKey.COMMODITY_NAME);
        List statList = getList(commodityName);
        return (DashboardDataBean[]) statList.toArray( new DashboardDataBean[0] );
    }

    public synchronized void clearValues(DashboardDataBean bean) {
        String commodityName="noName";
        ibsefloorspaceCache.remove(commodityName);
    }

    private List getList(String key) {
        List beanList =  (List) ibsefloorspaceCache.get(key);

        if (beanList == null) {
            beanList = new ArrayList();
            ibsefloorspaceCache.put( key, beanList );
        }
        return beanList;
    }
}

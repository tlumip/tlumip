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

import com.pb.tlumip.pc.beans.StatisticBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Stores StatBeans in a cache. StatBeans are indexed by the application name and the
 * type of statistic.
 *
 * @author    Tim Heier
 * @version   1.0, 11/24/2002
 */
public class StatCache {

    //Types of statistics
    public static final String ITERATION_SUMMARY = "ITERATION_SUMMARY";

    protected HashMap statCache = new HashMap();
    private static StatCache instance = new StatCache();

    /** Keep this class from being created with "new".
     */
    private StatCache() {
    }

    /** Return instances of this class.
     */
    public static StatCache getInstance() {
        return instance;
    }

    public synchronized StatisticBean[] getValues(String appName, String statType) {
        List statList = getList(appName, statType);
        return (StatisticBean[]) statList.toArray( new StatisticBean[0] );
    }

    public synchronized void addValue(String appName, String statType, StatisticBean bean) {
        List statList = getList(appName, statType);
        statList.add( bean );
    }

    public synchronized void clearValues(String appName, String statType) {
        statCache.remove( appName + "_" + statType );
    }

    private List getList(String appName, String statType) {
        List statList =  (List) statCache.get( appName + "_" + statType );

        if (statList == null) {
            statList = new ArrayList();
            statCache.put( appName + "_" + statType, statList );
        }
        return statList;
    }
}

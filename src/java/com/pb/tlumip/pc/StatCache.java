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

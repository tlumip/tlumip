package com.pb.despair.pc;

import com.pb.despair.pc.beans.DashboardDataBean;
import com.pb.despair.pc.beans.StatisticBean;

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

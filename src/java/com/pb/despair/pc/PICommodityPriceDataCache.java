package com.pb.despair.pc;

import com.pb.despair.pc.beans.DashboardDataBean;
import com.pb.despair.pc.beans.StatisticBean;

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

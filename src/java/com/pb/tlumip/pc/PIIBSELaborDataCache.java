package com.pb.tlumip.pc;

import com.pb.tlumip.pc.beans.DashboardDataBean;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * PI data cache used for labor data graph
 *
 * @author Christi Willison
 * @version  Sep 29, 2003
 * Created by IntelliJ IDEA.
 */
public class PIIBSELaborDataCache {

    protected HashMap ibselaborCache = new HashMap();
    private static PIIBSELaborDataCache instance = new PIIBSELaborDataCache();

    /** Keep this class from being created with "new".
     */
    private PIIBSELaborDataCache() {
    }

    /** Return instances of this class.
     */
    public static PIIBSELaborDataCache getInstance() {
        return instance;
    }

    public synchronized void addValue(DashboardDataBean bean) {
        String commodityName = "noName";
        commodityName = bean.getStringValue(DashboardDataKey.COMMODITY_NAME);
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
        commodityName=bean.getStringValue(DashboardDataKey.COMMODITY_NAME);
        ibselaborCache.remove(commodityName);
    }

    private List getList(String key) {
        List beanList =  (List) ibselaborCache.get(key);

        if (beanList == null) {
            beanList = new ArrayList();
            ibselaborCache.put( key, beanList );
        }
        return beanList;
    }
}

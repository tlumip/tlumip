package com.pb.despair.pc;

import com.pb.despair.pc.beans.DashboardDataBean;
import com.pb.despair.pc.beans.StatisticBean;

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

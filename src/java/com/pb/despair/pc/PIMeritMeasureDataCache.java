package com.pb.tlumip.pc;

import com.pb.tlumip.pc.beans.DashboardDataBean;
import com.pb.tlumip.pc.beans.StatisticBean;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * PI data cache used for merit measure graph
 *
 * @author Christi Willison
 * @version  Sep 29, 2003
 * Created by IntelliJ IDEA.
 */
public class PIMeritMeasureDataCache {

    protected ArrayList meritCache = new ArrayList();
    private static PIMeritMeasureDataCache instance = new PIMeritMeasureDataCache();

    /** Keep this class from being created with "new".
     */
    private PIMeritMeasureDataCache() {
    }

    /** Return instances of this class.
     */
    public static PIMeritMeasureDataCache getInstance() {
        return instance;
    }

    public synchronized void addValue(DashboardDataBean bean) {
        meritCache.add(bean);
    }

    public synchronized DashboardDataBean[] getValues() {
        return (DashboardDataBean[]) meritCache.toArray( new DashboardDataBean[0] );
    }

    public synchronized void clearValues() {
        meritCache.clear();
    }


}

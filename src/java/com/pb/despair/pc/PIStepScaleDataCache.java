package com.pb.despair.pc;

import com.pb.despair.pc.beans.DashboardDataBean;

import java.util.ArrayList;

/**
 * PI data cache used for step scale graph
 *
 * @author Christi Willison
 * @version Sep 29, 2003
 *          Created by IntelliJ IDEA.
 */
public class PIStepScaleDataCache {

    protected ArrayList stepscaleCache = new ArrayList();
    private static PIStepScaleDataCache instance = new PIStepScaleDataCache();

    /**
     * Keep this class from being created with "new".
     */
    private PIStepScaleDataCache() {
    }

    /**
     * Return instances of this class.
     */
    public static PIStepScaleDataCache getInstance() {
        return instance;
    }

    public synchronized void addValue(DashboardDataBean bean) {
        stepscaleCache.add(bean);
    }

    public synchronized DashboardDataBean[] getValues() {
        return (DashboardDataBean[]) stepscaleCache.toArray(new DashboardDataBean[0]);
    }

    public synchronized void clearValues() {
        stepscaleCache.clear();
    }


}

package com.pb.despair.pc;

import com.pb.despair.pc.beans.DashboardDataBean;

import java.util.HashMap;

/**
 * Abstract DataCache used to hold Dashboard data
 *
 * @author Christi Willison
 * @version  Sep 29, 2003
 * Created by IntelliJ IDEA.
 */
public abstract class DataCache {

    protected HashMap map = new HashMap();


    public void addValue(DashboardDataBean bean){};

    public abstract DashboardDataBean[] getValues(String key);

    public void clearValues(String key){

    };
}

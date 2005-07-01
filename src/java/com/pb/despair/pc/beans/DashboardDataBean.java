package com.pb.tlumip.pc.beans;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Holds data used in the dashboard displays
 *
 * @author Christi Willison
 * @version Oct 29, 2003
 * Created by IntelliJ IDEA.
 */
public class DashboardDataBean {

    protected HashMap map = new HashMap();

//    DashboardDataBeans can take a double or an int value but that value will
//    be stored as a double value because the Datasets formed from these values
//    require a double.

    public void setValue(String fieldName, double d) {
        map.put(fieldName,new Double(d));
    }

    public void setValue(String fieldName, int i) {
        map.put(fieldName,new Double(i));
    }

    public void setValue(String fieldName, String s){
        map.put(fieldName, s);
    }

    public double getDoubleValue(String fieldName) {
        return ((Double) map.get(fieldName)).doubleValue();
    }

    public String getStringValue(String fieldName){
        return (String) map.get(fieldName);
    }

}

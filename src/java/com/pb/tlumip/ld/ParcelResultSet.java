/*
 * Created on Dec 16, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.pb.osmp.ld;

import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ParcelResultSet {
    
    /**
     * @param set
     */
    public ParcelResultSet(ResultSet set) {
        mySet = set;
        zoneColumnName = "TAZ";
    }
    
    public ParcelResultSet(ResultSet set, String zoneColumnName){
        mySet = set;
        this.zoneColumnName = zoneColumnName;
    }
    
    final String zoneColumnName;

    private final ResultSet mySet;
    public ResultSet getSet() {
        return mySet;
    }

    private int tazColumn = -1;
    private int areaColumn = -1;
    private int sqFtColumn = -1;
    private int pecasTypeColumn = -1;

    int getTazColumn() throws SQLException {
        if (tazColumn<0) {
            tazColumn = mySet.findColumn(zoneColumnName); 
        }
        return tazColumn;
    }

    int getAreaColumn() throws SQLException {
        if (areaColumn<0) {
            areaColumn = mySet.findColumn("AREA"); 
        }
        return areaColumn;
    }
    int getSqFtColumn() throws SQLException {
        if (sqFtColumn<0) {
            sqFtColumn = mySet.findColumn("PECASSQFT"); 
        }
        return sqFtColumn;
    }
    int getPecasTypeColumn() throws SQLException {
        if (pecasTypeColumn<0) {
            pecasTypeColumn = mySet.findColumn("PECASTYPE"); 
        }
        return pecasTypeColumn;
    }
    
    void close() throws SQLException {
        mySet.close();
    }
}

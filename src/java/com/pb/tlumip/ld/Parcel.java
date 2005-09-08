/*
 * Created on Dec 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.pb.osmp.ld;

import java.sql.SQLException;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */


public class Parcel {
	
	private int taz;
	private float size;
	private float sqFtAssigned;
	private char currentCoverage;
	private boolean gotStuffFromSQL=false;
	
	public void sqlTableChanged() {
		gotStuffFromSQL=false;
	}
	
	int getTaz() throws SQLException {
		if (!gotStuffFromSQL) getVariables();
		return taz;
	};
    
    
    float getSize() throws SQLException {
		if (!gotStuffFromSQL) getVariables();
		return size;
    };

    float getSqFtAssigned() throws SQLException {
		if (!gotStuffFromSQL) getVariables();
		return sqFtAssigned;
    };
    
    char getCurrentCoverage() throws SQLException {
		if (!gotStuffFromSQL) getVariables();
		return currentCoverage;
    };

    
	private void getVariables() throws SQLException {
		synchronized (set) {
			set.getSet().absolute(place);
			taz= set.getSet().getInt(set.getTazColumn());
			size= (float) set.getSet().getDouble(set.getAreaColumn());
			sqFtAssigned = (float) set.getSet().getDouble(set.getSqFtColumn());
			String currentCoverageString = set.getSet().getString(set.getPecasTypeColumn());
            if (currentCoverageString == null) {
                currentCoverage = ' ';
            } else  if (currentCoverageString.length()==0) {
                currentCoverage = ' ';
            } else {
                currentCoverage = currentCoverageString.charAt(0);
            }
		}
		gotStuffFromSQL=true;
	};


    ParcelResultSet set;
    int place;
    /**
     * @param someParcelsSQL
     */
    public Parcel(ParcelResultSet s, int i) {
        set =s;
        place =i;
    }
    
    public Parcel(ParcelResultSet s) throws SQLException {
        set = s;
        place = set.getSet().getRow();
    }

    /**
     * @param string
     * @return
     */
    public String getValue(String string) throws SQLException {
        synchronized (set) {
            set.getSet().absolute(place);
            return set.getSet().getString(string);
        }
    }
    /**
     * @param string
     */
    public void setCurrentCoverage(char myCharCode) throws SQLException {
        try {
            synchronized (set) {
                set.getSet().absolute(place);
                String theString = new String(myCharCode+"");
                set.getSet().updateString(set.getPecasTypeColumn(),theString);
                set.getSet().updateRow();
            }
        } catch (SQLException e) {
            System.out.println("Problem in parcel "+this+ " trying to update coverage to "+myCharCode);
            throw e;
        }
        currentCoverage = myCharCode;
        revision++;
    }
    /**
     * @param amount
     */
    public void addSqFtAssigned(float amount) throws SQLException {
        synchronized (set) {
        	float newSqFt = getSqFtAssigned()+amount;
            set.getSet().absolute(place);
            set.getSet().updateDouble(set.getSqFtColumn(),newSqFt);
            set.getSet().updateRow();
			sqFtAssigned = newSqFt;
        }
        revision++;
        
    }
    

    int revision = 0;

	public int getRevision() {
		return revision;
	}

    /**
     * @param f
     */
    public void setSqFtAssigned(float f) throws SQLException {
        synchronized (set) {
            set.getSet().absolute(place);
            set.getSet().updateDouble(set.getSqFtColumn(),f);
            set.getSet().updateRow();
            sqFtAssigned = f;
        }
        revision++;
        
    }

}

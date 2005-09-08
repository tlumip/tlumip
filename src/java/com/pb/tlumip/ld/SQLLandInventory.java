/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
/*
 * Created on Jul 22, 2004
 *
 */
package com.pb.osmp.ld;

import com.pb.common.datafile.JDBCTableReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.sql.JDBCConnection;

import java.io.IOException;
import java.sql.*;


/**
 * @author jabraham
 * An inventory of land parcels or grid cells in a database
 */

public class SQLLandInventory extends LandInventory {
    

    final Connection conn;
    final Statement[] lastUsedStatements;
    final ResultSet[] lastUsedResultSets;
    final int[] zonesForLastUsedResultSets;
    final String tableName;
//    final Statement statement;
    static final int numberOfResultSetsToSave = 10;
    int zoneColumnNumber = -1;
    int coverageColumnNumber = -1;
    int quantityColumnNumber = -1;
    int zoningColumnNumber = -1;
    int yearBuiltColumnNumber = -1;
    int sizeColumnNumber = -1;

    private String zoneColumnName;
    private String coverageColumnName;
    private String quantityColumnName;
    private String zoningColumnName;
    private String yearBuiltColumnName;
    private String sizeColumnName;
    String landDatabaseDriver;
    String landDatabaseSpecifier;
    int maxZoneNumber = 4000;
    static final int maxCharCodes = 256;
    double[][] prices = new double[maxCharCodes][];
    
    
    public void setColumnNames(String zoneColumnName, String coverageColumnName, String quantityColumnName, String zoningColumnName, String yearBuiltColumnName, String sizeColumnName) {
        this.zoneColumnName = zoneColumnName;
        this.coverageColumnName = coverageColumnName;
        this.quantityColumnName=quantityColumnName;
        this.zoningColumnName=zoningColumnName;
        this.yearBuiltColumnName = yearBuiltColumnName;
        this.sizeColumnName = sizeColumnName;
        zoneColumnNumber = -1;
        coverageColumnNumber = -1;
        quantityColumnNumber = -1;
        zoningColumnNumber = -1;
        yearBuiltColumnNumber = -1;
        sizeColumnNumber = -1;
        
    }
    
    public void putPrice(int zoneNumber, char coverageType, double price) {
        if (zoneNumber >= maxZoneNumber) {
            for (int i=0;i<maxCharCodes;i++) {
                if (prices[i]!=null) {
                    double[] oldPrices = prices[i];
                    prices[i] = new double[zoneNumber+1];
                    System.arraycopy(oldPrices,0,prices[i],0,oldPrices.length);
                }
            }
            maxZoneNumber = zoneNumber;
        }
        if (prices[coverageType]==null) {
            prices[coverageType] = new double[maxZoneNumber+1];
        }
        prices[coverageType][zoneNumber]=price;
    }

    /**
     * @param landDatabaseDriver
     * @param landDatabaseSpecifier
     */
    public SQLLandInventory(String landDatabaseDriver, String landDatabaseSpecifier, String tableName) throws SQLException {
        this.landDatabaseDriver = landDatabaseDriver;
        this.landDatabaseSpecifier = landDatabaseSpecifier;
        this.tableName = tableName;
        try {
            Class.forName(landDatabaseDriver).newInstance();
            conn = DriverManager.getConnection(landDatabaseSpecifier);
        } catch (Exception e) {
            System.out.println("Error opening land database");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        lastUsedResultSets = new ResultSet[numberOfResultSetsToSave];
        lastUsedStatements = new Statement[numberOfResultSetsToSave];
        zonesForLastUsedResultSets = new int[numberOfResultSetsToSave];
    }
    
    private ResultSet getResultSet(int zoneNumber) {
        int i;
        for (i=0;i<zonesForLastUsedResultSets.length;i++) {
            if (zonesForLastUsedResultSets[i]==zoneNumber) {
                return lastUsedResultSets[i];
            }
            if (zonesForLastUsedResultSets[i]== 0) break;
        }
        ResultSet r;
        try {
            if (i>=zonesForLastUsedResultSets.length) {
                //gotta bump everyone down
                lastUsedResultSets[0].close();
                lastUsedStatements[0].close();
                System.arraycopy(lastUsedResultSets,1,lastUsedResultSets,0,lastUsedResultSets.length-1);
                System.arraycopy(zonesForLastUsedResultSets,1,zonesForLastUsedResultSets,0,zonesForLastUsedResultSets.length-1);
                System.arraycopy(lastUsedStatements,1,lastUsedStatements,0,lastUsedStatements.length-1);
                i=zonesForLastUsedResultSets.length-1;
            }
            String queryString = "SELECT * FROM "+tableName+" WHERE "+zoneColumnName+"="+zoneNumber;
            Statement statement = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
            if (statement.getMaxRows()!=0) {
                System.out.println("Max rows is set by default in statement .. attempting to remove maxrows limitation");
                statement.setMaxRows(0);
            }
            
            r = statement.executeQuery(queryString); 
            zonesForLastUsedResultSets[i] = zoneNumber;
            lastUsedResultSets[i] = r;
            lastUsedStatements[i] = statement;
            if (zoneColumnNumber == -1) {
                zoneColumnNumber = r.findColumn(zoneColumnName);
                coverageColumnNumber = r.findColumn(coverageColumnName);
                quantityColumnNumber = r.findColumn(quantityColumnName);
                zoningColumnNumber = r.findColumn(zoningColumnName);
                yearBuiltColumnNumber = r.findColumn(yearBuiltColumnName);
                sizeColumnNumber= r.findColumn(sizeColumnName);
                
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return r;
    }
    
    char[] tempCharArray = new char[1];

    public void putCoverage(long id1, long id2, char coverageChar) {
        ResultSet r = getResultSet((int) id1);
        try {
           r.absolute((int) id2);
           tempCharArray[0] = coverageChar;
           r.updateString(coverageColumnNumber,new String(tempCharArray));
           r.updateRow();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
    }

    public void putQuantity(long id1, long id2, float quantity) {
        ResultSet r = getResultSet((int) id1);
        try {
            r.absolute((int) id2);
            r.updateFloat(quantityColumnNumber,quantity);
            r.updateRow();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void putYearBuilt(long id1, long id2, int yearBuilt) {
        ResultSet r = getResultSet((int) id1);
        try {
            r.absolute((int) id2);
            r.updateInt(yearBuiltColumnNumber,yearBuilt);
            r.updateRow();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public int getYearBuilt(long id1, long id2) {
        ResultSet r = getResultSet((int) id1);
        try {
            r.absolute((int) id2);
            return r.getInt(yearBuiltColumnNumber);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public float getQuantity(long id1, long id2) {
        ResultSet r = getResultSet((int) id1);
        try {
            r.absolute((int) id2);
            return r.getFloat(quantityColumnNumber);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /* 
     * @return returns 'V' if coverage string is null or zero length
     *
     * 
     * @see com.pb.tlumip.ld.LandInventory#getCoverage(long, long)
     */
    public char getCoverage(long id1, long id2) {
        ResultSet r = getResultSet((int) id1);
        try {
            r.absolute((int) id2);
            String coverageString = r.getString(coverageColumnNumber);
            if (coverageString == null) return 'V';
            if (coverageString.length() == 0) return 'V';
            return coverageString.charAt(0);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public float getSize(long id1, long id2) {
        ResultSet r = getResultSet((int) id1);
        try {
            r.absolute((int) id2);
            return r.getFloat(sizeColumnNumber);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public short getZoning(long id1, long id2) {
        ResultSet r = getResultSet((int) id1);
        try {
            r.absolute((int) id2);
            return (short) r.getInt(zoningColumnNumber);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean isDevelopable(long id1, long id2) {
        return true;
    }

    /**
     * @param zoneNumber
     * @return
     */
    public long getParcelCount(long zoneNumber) {
        ResultSet r = getResultSet((int) zoneNumber);
        try {
            r.last();
            return r.getRow();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * @return
     */
    public TableDataSet summarizeInventory(String commodityNameTable, String commodityNameColumn) {
        String createTableString = 
            "SELECT "+tableName+"."+zoneColumnName+" AS FloorspaceZone, "+commodityNameTable+"."+commodityNameColumn+", "+tableName+"."+coverageColumnName+
            " AS pecastype, Sum("+tableName+"."+quantityColumnName+") AS Quantity INTO Floorspace "+
            "FROM "+commodityNameTable+" INNER JOIN "+tableName+" ON "+commodityNameTable+"."+coverageColumnName+" = "+tableName+"."+coverageColumnName+
            " GROUP BY "+tableName+"."+zoneColumnName+", "+commodityNameTable+"."+commodityNameColumn+", "+tableName+"."+coverageColumnName+";";
        Statement aNewStatement = null;
        try {
            aNewStatement = conn.createStatement();
        } catch (SQLException e) {
            throw new RuntimeException("Can't recreate statement to summarize floorspace inventory",e);
        }
        try {
            aNewStatement.execute("DROP TABLE FLOORSPACE;");
        } catch (SQLException e) {
            System.out.println("can't delete existing floorspace table "+e);
        }
        try {
            aNewStatement.execute(createTableString);
        } catch (SQLException e) {
            throw new RuntimeException("Can't create floorspace inventory table",e);
        }
        JDBCConnection jdbcConn = new JDBCConnection(landDatabaseSpecifier,landDatabaseDriver,"","");
        JDBCTableReader reader = new JDBCTableReader(jdbcConn);
        try {
            return reader.readTable("FLOORSPACE");
        } catch (IOException e) {
            throw new RuntimeException("Can't read in floorspace inventory",e);
        }
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.ld.LandInventory#getPrice(long, long, char)
     */
    public double getPrice(long id1, long id2, char coverageChar) {
        if (prices[coverageChar]==null) {
            throw new RuntimeException("No price set for coverage "+coverageChar+" zone number "+id1);
        }
        return prices[coverageChar][(int) id1];
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.ld.LandInventory#getLocalVacancyRate(long, long, char, double)
     */
    public double getLocalVacancyRate(long id1, long id2, char coverageChar, double radius) {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.ld.LandInventory#elementToString(long, long)
     */
    public String elementToString(long id1, long id2) {
        return id1+","+id2;
    }

}

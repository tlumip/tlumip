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
package com.pb.tlumip.ld;

import com.pb.common.datafile.TableDataSet;
import com.pb.models.pecas.DevelopmentTypeInterface;
import com.pb.models.pecas.ProductionActivity;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A class that represents a type of land development
 * @author John Abraham
 */
public class DevelopmentType implements DevelopmentTypeInterface{

    public final int developmentTypeNumber;
    public final int gridCode;
    public final String name;
    public String getName() {return name;}
    public final boolean vacantType;

    private double constructionCost;
    private double maintenanceCost;
    private double addASC=0;
    private double newASC=0;
    private double rentDiscount =0;

    
    
    public DevelopmentType(String name, int gridCode, boolean vacantType) {
        this.vacantType= vacantType;
        if (getAlreadyCreatedDevelopmentType(name) != null) {
            throw new RuntimeException("Can't create duplicate development types with the same name");
        }
        if (getAlreadyCreatedDevelopmentByCode(gridCode) != null) {
            throw new RuntimeException("Can't create duplicate development types with the same name");
        }
        developmentTypeNumber = createdDevelopmentTypes.size();
        this.name = name;
        this.gridCode = gridCode;
        createdDevelopmentTypes.add(this);
    }


    public DevelopmentType(String name, int gridCode) {
        this(name,gridCode,name.equalsIgnoreCase("Vacant"));
    }

   public int getID() {
    	return developmentTypeNumber;
    }
    
    public char getGridCode() {
        return (char) gridCode;
    }

    private static ArrayList createdDevelopmentTypes = new ArrayList();

    public static DevelopmentType getAlreadyCreatedDevelopmentType(int number) {
        if (createdDevelopmentTypes.size() > number) {
            return (DevelopmentType)createdDevelopmentTypes.get(number);
        } else {
            return null;
        }
    }

    public static DevelopmentType getAlreadyCreatedDevelopmentType(String name) {
        Iterator it = createdDevelopmentTypes.iterator();
        DevelopmentType theone;
        while (it.hasNext()) {
            theone = (DevelopmentType)it.next();
            if (theone.name.equals(name)) {
                return theone;
            }
        }
        return null;
    }


    public static DevelopmentType getAlreadyCreatedDevelopmentByCode(int gridCode) {
        Iterator it = createdDevelopmentTypes.iterator();
        DevelopmentType theone;
        while (it.hasNext()) {
            theone = (DevelopmentType)it.next();
            if (theone.gridCode == gridCode) {
                return theone;
            }
        }
        return null;
    }


    public String toString() { return "DevelopmentType " + name; };

    public int hashCode() { return developmentTypeNumber; };

    public boolean equals(Object o) {
        if (o instanceof DevelopmentType) {
            return developmentTypeNumber == ((DevelopmentType)o).developmentTypeNumber;
        };
        return false;
    }

    public double getConstructionCost(){ return constructionCost; }

    public void setConstructionCost(double constructionCost){ this.constructionCost = constructionCost; }

    public double getMaintenanceCost(){ return maintenanceCost; }

    public void setMaintenanceCost(double maintenanceCost){ this.maintenanceCost = maintenanceCost; }

    public static void setUpDevelopmentTypeUsage(TableDataSet ctab) {
        for (int row=1;row<=ctab.getRowCount();row++) {
                String name;
                name = ctab.getStringValueAt(row,"ActivityName");
                ProductionActivity pa = ProductionActivity.retrieveProductionActivity(name);
                if (pa == null) {
                    throw new Error("Invalid (not set up) production activity \""+name+"\" in DevelopmentTypeUsage.csv");
                }
                name = ctab.getStringValueAt(row,"DevelopmentTypeName");
                DevelopmentType dt = getAlreadyCreatedDevelopmentType(name);
                if (dt == null) {
                    throw new Error("Invalid (not set up) development type \""+name+"\" in DevelopmentTypeUsage.csv");
                }
                pa.allowIn(dt);
            }
    }


//    static TableDataSet createPriceUseTable() {
//        TableDataSet priceUseTable = new TableDataSet();
//        priceUseTable.setName("SpacePricesAndUse");
//        priceUseTable.addColumn("Time");
//        priceUseTable.addColumn("DevelopmentType", Variant.STRING);
//        priceUseTable.addColumn("TAZ", Variant.INT);
//        priceUseTable.addColumn("Price", Variant.DOUBLE);
//        priceUseTable.addColumn("Quantity",Variant.DOUBLE);
//        priceUseTable.addColumn("Use",Variant.DOUBLE);
//        priceUseTable.addColumn("Vacant",Variant.DOUBLE);
//        priceUseTable.restructure();
//        return priceUseTable;
//    }

//   static TableDataSet staticPriceUseTable = null;
//   static TableDataSet staticCurrentPricesTable = null;
//   static DataRow searchPrices = null;

//    static TableDataSet getPriceUseTable(DataManager dm) {
//      if (staticPriceUseTable != null) return staticPriceUseTable;
//      staticPriceUseTable = dm.getTableDataSet("SpacePricesAndUse");
//        if (staticPriceUseTable.getColumns().length == 0) {
//            staticPriceUseTable = createPriceUseTable(dm);
//        }
//        return staticPriceUseTable;
//    }

//    static TableDataSet getCurrentPricesTable(DataManager dm) {
//      if (staticCurrentPricesTable != null) return staticCurrentPricesTable;
//      staticCurrentPricesTable = dm.getTableDataSet("FloorspaceRents");
//      staticCurrentPricesTable.setSort(
//          new SortDescriptor(
//          new String[] { "Zone", "DevelopmentType"}));
//     searchPrices = new DataRow(staticCurrentPricesTable,
//            new String[] { "Zone", "DevelopmentType" });
//      
//      return staticCurrentPricesTable;
//    }


//    public static void writeStatusForAll(DataManager dm, float currentTime) {
//        TableDataSet pricesAndUseHistory = getPriceUseTable(dm);
//        TableDataSet currentPrices = getCurrentPricesTable(dm);
//        AbstractTAZ[] zones = AbstractTAZ.getAllZones();
//        Iterator it  = createdDevelopmentTypes.iterator();
//        while (it.hasNext()) {
//            DevelopmentType d = (DevelopmentType) it.next();
//            d.writeStatus(currentTime, pricesAndUseHistory, currentPrices, zones);
//        }
//    }

//    void writeStatus(float currentTime, DataManager dm) {
//        writeStatus(currentTime, getPriceUseTable(dm), getCurrentPricesTable(dm), AbstractTAZ.getAllZones());
//    }

//    void writeStatus(float currentTime, TableDataSet priceUseHistory, TableDataSet currentPrices, AbstractTAZ[] zones) {
//        AbstractTAZ.PriceVacancy pv;
//        for (int z = 0; z < zones.length; z++) {
//            pv = zones[z].getPriceVacancySize(this);
//            priceUseHistory.insertRow(false);
//            priceUseHistory.setFloat("Time", currentTime);
//            priceUseHistory.setString("DevelopmentType", name);
//            priceUseHistory.setInt("TAZ", zones[z].getZoneUserNumber());
//            priceUseHistory.setDouble("Price", pv.getPrice());
//            priceUseHistory.setDouble("Quantity",pv.getTotalSize());
//            priceUseHistory.setDouble("Use",pv.getTotalSize()-pv.getVacancy());
//            priceUseHistory.setDouble("Vacant",pv.getVacancy());
//            priceUseHistory.post();
//            try {
//                searchPrices.setInt("Zone", zones[z].getZoneUserNumber());
//                searchPrices.setString("DevelopmentType", this.name);
//                boolean found = currentPrices.locate(searchPrices, 0x20); // 0x20 is supposed to be
//                // com.borland.dx.dataset.Locate.FIRST but the Locate class def seems to be missing
//                if (!found) {
//                    currentPrices.insertRow(false);
//                    currentPrices.setString("DevelopmentType", name);
//                    currentPrices.setInt("Zone", zones[z].getZoneUserNumber());
//                    currentPrices.post();
//                }
//                currentPrices.setDouble("Price", pv.getPrice());
//            } catch (com.borland.dx.dataset.DataSetException e) {
//                System.err.println("Error adding price record " + this + " to table");
//                e.printStackTrace();
//            }
//            
//        }
//    }

//    public static void main(String[] args) {
//        DataManager dm = new DataManager();
///*        TableDataSet priceUseTable = dm.getTableDataSet("SpacePricesAndUse");
//        priceUseTable.addColumn("Quantity",Variant.DOUBLE);
//        priceUseTable.restructure();
//        priceUseTable.close(); */
//        createPriceUseTable(dm);
//        dm.closeStore();
//        System.exit(0);
//    }
    /**
     * Method getTransitionCoefficient.
     *@return double
     */
    public double getTransitionCoefficient(DevelopmentType newDT) {
            
        // TODO: read in transition coefficients
        if (newDT.isVacant()) {
            return 0.0*9691;
        }
        else return 0.0*9691;
    }

    /**
     * Method getAddCoefficient.
     * @return double
     */
    public double getAddASC() {
        return addASC;
    }

    /**
     * Method getKeepCoefficient.
     * @return double
     */
    public double getNewASC() {
        return newASC;
    }

    /**
     * Method isVacant.
     * @return boolean
     */
    public boolean isVacant() {
        return vacantType;
    }

    public void setAddASC(double addASC) {
        this.addASC = addASC;
    }

    public void setNewASC(double newASC) {
        this.newASC = newASC;
    }


    /* (non-Javadoc)
     * @see com.pb.tlumip.model.DevelopmentTypeInterface#setRentDiscount(double)
     */
    public void setAgeRentDiscount(double d) {
        rentDiscount = d;
        
    }


    /* (non-Javadoc)
     * @see com.pb.tlumip.model.DevelopmentTypeInterface#getRentDiscount(int)
     */
    public double getRentDiscountFactor(int age) {
        return Math.pow(1-rentDiscount,age);
    }

}


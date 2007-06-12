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
package com.pb.tlumip.ld;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.grid.GridManager;
import com.pb.models.pecas.AbstractZone;
import com.pb.models.pecas.DevelopmentTypeInterface;

/**
 * @author jabraham
 *
 */
public class GridLandInventory extends LandInventory {
    
    static final float gridSize = 9691;

    private GridManager coverageGrid;
    private GridManager zoningGrid;
    private GridManager yrBuiltGrid;
    private GridManager floorspaceGrid;
    private GridManager alphaZoneGrid;

    public void putCoverage(long id1, long id2, char coverageChar) {
        coverageGrid.putCellValue((int) id1,(int) id2,(int) coverageChar);

    }

    public void putQuantity(long id1, long id2, float quantity) {
        floorspaceGrid.putCellValue((int) id1, (int) id2, (int) (quantity));
    }

    public void putYearBuilt(long id1, long id2, int yearBuilt) {
        yrBuiltGrid.putCellValue((int) id1, (int) id2, yearBuilt);

    }

    public int getYearBuilt(long id1, long id2) {
        return yrBuiltGrid.getCellValue((int) id1, (int) id2);
    }

    public float getQuantity(long id1, long id2) {
        return (float) floorspaceGrid.getCellValue((int) id1, (int) id2);
    }

    public char getCoverage(long id1, long id2) {
        return (char) coverageGrid.getCellValue((int) id1, (int) id2);
    }

    /**
     * Method openGridFiles.
     */
    public void openGridFiles(String gridPath) {
        coverageGrid = new GridManager(gridPath+"coverage.grid","rw");
        zoningGrid = new GridManager(gridPath+"zoning.grid","r");
        yrBuiltGrid = new GridManager(gridPath+"yearBuilt.grid","rw");
        floorspaceGrid = new GridManager(gridPath+"floorspace.grid","rw");
        alphaZoneGrid = new GridManager(gridPath+"alphaZone.grid","r");
        
    }

    /**
     * Method openGridFiles.
     */
    public void closeGridFiles() {
        coverageGrid.close();
        zoningGrid.close();
        yrBuiltGrid.close();
        floorspaceGrid.close();
        alphaZoneGrid.close();
    }

    /**
     * Method getCoverageGrid.
     * @return GridManager
     */
    public GridManager getZoningGrid() {
        return zoningGrid;
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.ld.LandInventory#getSize(long, long)
     */
    public float getSize(long row, long col) {
        return gridSize;
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.ld.LandInventory#isDevelopable(long, long)
     */
    public boolean isDevelopable(long id1, long id2) {
        AbstractZone myTAZ = AbstractZone.findZoneByUserNumber(alphaZoneGrid.getCellValue((int) id1,(int) id2));
        if (myTAZ==null) return false;
        char zoning = (char) zoningGrid.getCellValue((int) id1,(int) id2);
        if (zoning == 'X') return false;
        return true;
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.ld.LandInventory#getZoning(long, long)
     */
    public short getZoning(long id1, long id2) {
        return (short) zoningGrid.getCellValue((int) id1,(int) id2);
    }

    /**
     * @return
     */
    public long getId1Extent() {
        return zoningGrid.getNrows();
    }

    /**
     * @param row
     * @return
     */
    public long getId2Extent(long row) {
        return zoningGrid.getNcols();
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.ld.LandInventory#summarizeInventory()
     */
    public TableDataSet summarizeInventory(String commodityNameTable, String commodityNameColumn) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.ld.LandInventory#getPrice(long, long, char)
     */
    public double getPrice(long id1, long id2, char coverageChar) {
        DevelopmentTypeInterface dt = DevelopmentType.getAlreadyCreatedDevelopmentByCode(coverageChar);
        AbstractZone t = TAZ.findZoneByUserNumber(alphaZoneGrid.getCellValue((int)id1, (int)id2));
        AbstractZone.PriceVacancy pv = t.getPriceVacancySize(dt);
        return pv.getPrice();
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.ld.LandInventory#getLocalVacancyRate(long, long, char, double)
     */
    public double getLocalVacancyRate(long id1, long id2, char coverageChar, double radius) {
        DevelopmentTypeInterface dt = DevelopmentType.getAlreadyCreatedDevelopmentByCode(coverageChar);
        AbstractZone t = TAZ.findZoneByUserNumber(alphaZoneGrid.getCellValue((int)id1, (int)id2));
        AbstractZone.PriceVacancy pv = t.getPriceVacancySize(dt);
        if (pv.getTotalSize()==0) return 0;
        return pv.getVacancy()/pv.getTotalSize();
    }

    /* (non-Javadoc)
     * @see com.pb.tlumip.ld.LandInventory#elementToString(long, long)
     */
    public String elementToString(long id1, long id2) {
        return id1+","+id2+" in zone "+alphaZoneGrid.getCellValue((int) id1, (int)id2);
    }
    

}

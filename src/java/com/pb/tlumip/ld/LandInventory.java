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
package com.pb.osmp.ld;

import com.pb.common.datafile.TableDataSet;


/**
 * A class that represents a the allowed zoning in a Grid Cell
 * @author John Abraham
 */
public abstract class LandInventory {
    
    public abstract void putCoverage(long id1, long id2,char coverageChar);
    public abstract void putQuantity(long id1, long id2,float quantity);
    public abstract void putYearBuilt(long id1, long id2, int yearBuilt);
    public abstract int getYearBuilt(long id1, long id2);
    public abstract float getQuantity(long id1, long id2);
    public abstract char getCoverage(long id1, long id2);
    public abstract float getSize(long id1, long id2);
    public abstract short getZoning(long id1, long id2);
    public abstract double getPrice(long id1, long id2, char coverageChar);
    public abstract double getLocalVacancyRate(long id1, long id2, char coverageChar,double radius);
    public abstract String elementToString(long id1, long id2);
    
    // some bits of land dont' need to be processed.  This can be used to check
    public abstract boolean isDevelopable(long id1, long id2);

    public abstract TableDataSet summarizeInventory(String commodityNameTable, String commodityNameColumn);


}

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
package com.pb.tlumip.ha;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.datastore.DataManager;
import com.pb.models.reference.ModelComponent;


/**
 * HAModel implements...
 *
 * @author   John Abraham
 * @version   1.0
 *
 */

public class HAModel extends ModelComponent {
    static TableDataSet reloadTableFromScratchFromTextFile(String path, String tableName) {
        //TODO rewrite this method to use com.pb.common.TableDataSet instead of Borland.TableDataSet
//        TableDataSet table = dm.getTableDataSet(tableName);
//        try {
//            table.empty();
//        } catch (com.borland.dx.dataset.DataSetException e) { };
//        DataManager.closeTable(table);
//        dm.deleteTable(tableName);
//        dm.loadTable(tableName, path + tableName, path + tableName);
//        table = dm.getTableDataSet(tableName); //Add a table to data-store
//        return table;

        return new TableDataSet();
    }

    static public final DataManager dm = new DataManager();

    public void startModel(int baseYear, int timeInterval){};
}

package com.pb.tlumip.ha;

import com.pb.common.datastore.DataManager;
import com.pb.common.datafile.TableDataSet;
import com.pb.tlumip.model.ModelComponent;


/**
 * HAModel implements...
 *
 * @author   John Abraham
 * @version   1.0
 *
 */

public class HAModel extends ModelComponent
{
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

    public void startModel(int timeInterval){};
}

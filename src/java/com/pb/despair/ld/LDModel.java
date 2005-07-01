package com.pb.tlumip.ld;

import com.pb.tlumip.model.ModelComponent;
import com.pb.common.datafile.TableDataSet;
import com.pb.models.pecas.AbstractTAZ;
import com.pb.models.pecas.DevelopmentTypeInterface;
import com.pb.models.pecas.GridCell;

/**
 * LDModel implements...
 * @author    John Abraham
 * @version   1.0, 3/31/2000
 */
public class LDModel extends ModelComponent {
    public static void setUpGridCells(TableDataSet gtab) {
        for(int r=1;r<=gtab.getRowCount();r++) {
            int zoneNumber = (int)gtab.getValueAt(r,"TAZ");
            AbstractTAZ zone = AbstractTAZ.findZoneByUserNumber(zoneNumber);
            if (zone == null) {
                throw new Error("Grid cell has undefined zone number " + zoneNumber);
            }
            DevelopmentTypeInterface currentDevelopment =
                DevelopmentType.getAlreadyCreatedDevelopmentType(gtab.getStringValueAt(r,"DevelopmentType"));
                if (currentDevelopment == null)
                    throw new Error("Incorrect Development Type " + gtab.getStringValueAt(r,"DevelopmentType") +
                    " in GridCell.csv");
            float amountDeveloped = gtab.getValueAt(r,"AmountOfDevelopment");
            float amountLand = gtab.getValueAt(r,"AmountOfLand");
            int age = (int)gtab.getValueAt(r,"Age");
            ZoningScheme zs = ZoningScheme.getAlreadyCreatedZoningScheme(gtab.getStringValueAt(r,"ZoningScheme"));
            if (zs == null)
                throw new Error("Incorrect zoning scheme in GridCells.csv: " + gtab.getStringValueAt(r,"ZoningScheme"));
            GridCell gc = new GridCell(zone, amountLand, currentDevelopment, amountDeveloped, age, zs);
        }
    }

    public static void writeUpdatedGridCells(TableDataSet gtab) {
        //TODO replace 'gtab.empty' with an equivalent com.pb.common.TableDataSet method.
//          gtab.empty();
          AbstractTAZ[] allZones = AbstractTAZ.getAllZones();
          for (int z=0;z<allZones.length; z++) {
               AbstractTAZ taz = allZones[z];
               taz.writeGridCells(gtab);
          }
    }


    public void startModel(int timeInterval){};

}

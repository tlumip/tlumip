package com.pb.tlumip.sl;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.NEW_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * @author crf <br/>
 *         Started: Dec 10, 2009 12:59:37 PM
 */
public abstract class TripClassifier {
    private static TableDataSet hhData = null;
    private static TableDataSet empData;

    protected Object extraData;

    public abstract double getOriginFactor(SelectLinkData sld, String ... data);
    public abstract double getDestinationFactor(SelectLinkData sld, String ... data);
    public abstract double getExternalExternalFactor(SelectLinkData sld, String externalIn, String externalOut);
    public abstract Object formExtraData(SelectLinkData sld, Object extraData, String ... data);
    public void setExtraData(SelectLinkData sld, String ... data) {
        extraData = formExtraData(sld,extraData,data);
    }
    
    //todo: add static method to create this from dsl

    private static void loadModelData(ResourceBundle rb) {
        try {
            NEW_CSVFileReader reader = new NEW_CSVFileReader();
            hhData = reader.readFile(new File(rb.getString("sdt.household.data")));
            hhData.buildIndex(1);
            empData = reader.readFile(new File(rb.getString("sdt.current.employment")));
            empData.buildIndex(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static TripClassifier getClassifier(ResourceBundle rb, String file) {
        if (hhData == null)
            loadModelData(rb);

        if (file.equals("SDT")) {
            return new StandardTripClassifier() {
                 public double getOriginFactor(SelectLinkData sld, String ... data) {
                     if (sld.getInteriorZones().contains("" + ((int) hhData.getIndexedValueAt(Integer.parseInt(data[0]),2)))) {
                         return 3.0; //todo: attach household zone counts
                     } else {
                         try {
                             float employment = empData.getIndexedValueAt(Integer.parseInt(data[9]),"Total");
                             return Math.log(employment);
                         } catch (IndexOutOfBoundsException e) {
                             System.out.println(e.getMessage());
                             return 1.0; //todo: is this right - this means external zone?
                         }
                     }
                }
            };
        }
        if (file.equals("LDT")) {
            return new StandardTripClassifier() {
                 public double getOriginFactor(SelectLinkData sld, String ... data) {
                     if (sld.getInteriorZones().contains("" + ((int) hhData.getIndexedValueAt(Integer.parseInt(data[0]),2)))) {
                         return 3.0; //todo: attach household zone counts
                     } else {
                         try {
                            float employment = empData.getIndexedValueAt(Integer.parseInt(data[7]),"Total");
                             return Math.log(employment);
                         } catch (IndexOutOfBoundsException e) {
                             System.out.println(e.getMessage());
                             return 1.0; //todo: is this right - this means external zone?
                         }
                     }
                }
            };
        }
        if (file.equals("CT")) {
            return new StandardTripClassifier() {
                 public double getOriginFactor(SelectLinkData sld, String ... data) {
                     int homeTaz = (Integer) ((Object[]) extraData)[0];
                     if (sld.getInteriorZones().contains("" + homeTaz)) {
                         return 3.0; //todo: attach household zone counts
                     } else {
                         try {
                             float employment = empData.getIndexedValueAt(Integer.parseInt(data[7]),"Total");
                             return Math.log(employment);
                         } catch (IndexOutOfBoundsException e) {
                             System.out.println(e.getMessage());
                             return 1.0; //todo: is this right - this means external zone?
                         }
                     }
                }

                public Object formExtraData(SelectLinkData sld, Object extraData, String ... data) {
                    if (extraData == null || !data[7].equals(((Object[]) extraData)[1]))
                        extraData = new Object[] {Integer.parseInt(data[0]),data[7]};
                    return extraData;
                }
            };
        }
        return new StandardTripClassifier();
    }

    static private class StandardTripClassifier extends TripClassifier {
        public double getOriginFactor(SelectLinkData sld, String ... data) {
            return 1.0;
        }

        public double getDestinationFactor(SelectLinkData sld, String ... data) {
            return getOriginFactor(sld,data);
        }

        public double getExternalExternalFactor(SelectLinkData sld, String externalIn, String externalOut) {
            return 1.0;
        }

        public Object formExtraData(SelectLinkData sld, Object extraData, String ... data) {
            return null;
        }

    }
}

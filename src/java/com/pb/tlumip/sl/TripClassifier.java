package com.pb.tlumip.sl;

import com.pb.common.datafile.NEW_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * @author crf <br/>
 *         Started: Dec 10, 2009 12:59:37 PM
 */
public abstract class TripClassifier {
    private static final Logger logger = Logger.getLogger(TripClassifier.class);

    private static TableDataSet hhData = null;
//    private static TableDataSet empData;
    private static TableDataSet swimScaling;
    private static double eeScalingFactor;

    protected Object extraData;

    public abstract double getOriginFactor(SelectLinkData sld, String ... data);
    public abstract double getDestinationFactor(SelectLinkData sld, String ... data);
    public abstract double getExternalExternalFactor(SelectLinkData sld, String externalIn, String externalOut);
    public abstract Object formExtraData(SelectLinkData sld, Object extraData, String ... data);
    public void setExtraData(SelectLinkData sld, String ... data) {
        extraData = formExtraData(sld,extraData,data);
    }

    private static void loadModelData(ResourceBundle rb) {
        try {
            NEW_CSVFileReader reader = new NEW_CSVFileReader();
//            logger.info("Reading swim scaling data.");
//            if (new File(rb.getString("sl.swim.scaling.file")).exists()) {
//                swimScaling = reader.readFile(new File(rb.getString("sl.swim.scaling.file")));
//                swimScaling.buildIndex(1);
//            }
            logger.info("Reading sdt household data.");
            hhData = reader.readFile(new File(rb.getString("sdt.household.data")));
            hhData.buildIndex(1);
            eeScalingFactor = 1.0; //ResourceUtil.getDoubleProperty(rb,"sl.ee.scaling.factor");
//            logger.info("Reading current employment data.");
//            empData = reader.readFile(new File(rb.getString("sdt.current.employment")));
//            empData.buildIndex(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //convenient to get this from here
    static int getOriginZone(int hhId, ResourceBundle rb) {
        if (hhData == null)
            loadModelData(rb);
        return ((int) hhData.getIndexedValueAt(hhId,"TAZ"));
    }

    static TripClassifier getClassifier(ResourceBundle rb, String file) {
        if (hhData == null)
            loadModelData(rb);

        if (file.equals("SDT")) {
            return new StandardTripClassifier() {
                 public double getOriginFactor(SelectLinkData sld, String ... data) {
                     int homeTaz = ((int) hhData.getIndexedValueAt(Integer.parseInt(data[0]),2));
                     if (sld.getInteriorZones().contains("" + homeTaz)) {
                         return swimScaling.getIndexedValueAt(homeTaz,"LOCALHHS") / swimScaling.getIndexedValueAt(homeTaz,"SWIMHHS");
                     } else {
                         //try to scale to destination, otherwise to origin otherwise ee
                         int eZone = Integer.parseInt(data[9]);
                         if (!sld.getInteriorZones().contains("" + eZone))
                             eZone = Integer.parseInt(data[8]);
                         if (!sld.getInteriorZones().contains("" + eZone))
                             return eeScalingFactor;
                         try {
                             return swimScaling.getIndexedValueAt(eZone,"LOCALEMP") / swimScaling.getIndexedValueAt(eZone,"SWIMEMP");
                         } catch (IndexOutOfBoundsException e) {
                             System.out.println("Bad swim scaling: " + e.getMessage());
                             return 1.0;
                         }
                     }
                }
            };
        }
        if (file.equals("LDT")) {
            return new StandardTripClassifier() {
                 public double getOriginFactor(SelectLinkData sld, String ... data) {
                     int homeTaz = ((int) hhData.getIndexedValueAt(Integer.parseInt(data[0]),2));
                     if (sld.getInteriorZones().contains("" + homeTaz)) {
                         return swimScaling.getIndexedValueAt(homeTaz,"LOCALHHS") / swimScaling.getIndexedValueAt(homeTaz,"SWIMHHS");
                     } else {
                         //try to scale to destination, otherwise to origin otherwise ee
                         int eZone = Integer.parseInt(data[7]);
                         if (!sld.getInteriorZones().contains("" + eZone))
                             eZone = Integer.parseInt(data[6]);
                         if (!sld.getInteriorZones().contains("" + eZone))
                             return eeScalingFactor;
                         try {
                         return swimScaling.getIndexedValueAt(eZone,"LOCALEMP") / swimScaling.getIndexedValueAt(eZone,"SWIMEMP");
                         } catch (IndexOutOfBoundsException e) {
                             System.out.println("Bad swim scaling: " + e.getMessage());
                             return 1.0;
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
                         return swimScaling.getIndexedValueAt(homeTaz,"LOCALHHS") / swimScaling.getIndexedValueAt(homeTaz,"SWIMHHS");
                     } else {
                         //try to scale to destination, otherwise to origin otherwise ee
                         //int eZone = Integer.parseInt(data[3]);
                         int eZone = Integer.parseInt(data[3]); //removed duration value
                         if (!sld.getInteriorZones().contains("" + eZone))
                             eZone = Integer.parseInt(data[0]);
                         if (!sld.getInteriorZones().contains("" + eZone))
                             return eeScalingFactor;
                         try {
                         return swimScaling.getIndexedValueAt(eZone,"LOCALEMP") / swimScaling.getIndexedValueAt(eZone,"SWIMEMP");
                         } catch (IndexOutOfBoundsException e) {
                             System.out.println("Bad swim scaling: " + e.getMessage());
                             return 1.0;
                         }
                     }
                 }
//                     if (sld.getInteriorZones().contains("" + homeTaz)) {
//                         return 3.0;
//                     } else {
//                         try {
//                             float employment = empData.getIndexedValueAt(Integer.parseInt(data[3]),"Total");
//                             return Math.log(employment);
//                         } catch (IndexOutOfBoundsException e) {
////                             System.out.println(e.getMessage());
//                             return 1.0;
//                         }
//                     }
//                }

                public Object formExtraData(SelectLinkData sld, Object extraData, String ... data) {
                    if (extraData == null || !data[7].equals(((Object[]) extraData)[1]))
                        extraData = new Object[] {Integer.parseInt(data[0]),data[7]};
                    return extraData;
                }
            };
        }
        if (file.equals("ET")) {
            return new StandardTripClassifier() {
                 public double getOriginFactor(SelectLinkData sld, String ... data) {
                     return eeScalingFactor; //ET should just be ee trips
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
            return eeScalingFactor;
        }

        public Object formExtraData(SelectLinkData sld, Object extraData, String ... data) {
            return null;
        }

    }
}

package com.pb.tlumip.sl;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TextFile;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixReader;
import com.pb.common.matrix.ZipMatrixWriter;
import com.pb.tlumip.ts.DemandHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author crf <br/>
 *         Started: Dec 1, 2009 1:34:52 PM
 */
public class TripSynthesizer {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TripSynthesizer.class);

    public static final String SL_AUTO_ASSIGN_CLASS = "a";
    public static final String SL_TRUCK_ASSIGN_CLASS = "d";

    public static final String AM_STRING_NAME = "ampeak";
    public static final String MD_STRING_NAME = "mdoffpeak";
    public static final String PM_STRING_NAME = "pmpeak";
    public static final String NT_STRING_NAME = "ntoffpeak";

    private final OdMatrixGroup autoMatrices;
    private final OdMatrixGroup truckMatrices;
    private final SelectLinkData autoSelectLinkData;
    private final SelectLinkData truckSelectLinkData;
    private final ResourceBundle rb;
    private Map<String,Integer> autoZoneMatrixMap;
    private Map<String,Integer> truckZoneMatrixMap;

    private boolean initialized = false;
    private final double[] factors;

    public TripSynthesizer(ResourceBundle rb, int autoGroups, int truckGroups) {
        logger.info("Initializing SL Synthesizer");
        this.rb = rb;
        String dataFile = rb.getString("sl.current.directory") + rb.getString("sl.output.file.select.link.results");
        autoSelectLinkData = new SelectLinkData(dataFile,SL_AUTO_ASSIGN_CLASS);
        truckSelectLinkData = new SelectLinkData(dataFile,SL_TRUCK_ASSIGN_CLASS);
        autoMatrices = new OdMatrixGroup(autoGroups);
        truckMatrices = new OdMatrixGroup(truckGroups);
        factors = formTripFactorLookup(rb);
    }

    //convenience for simplest use case with one class for auto or truck
    public TripSynthesizer(ResourceBundle rb) {
        this(rb,1,1);
    }

    public void synthesizeTrips() {
        if (!initialized)
            initializeMatrices();
        logger.info("Synthesizing SDT");
        synthesizeTrips(new SDTTripFile(rb),true);
        logger.info("Synthesizing LDT");
        synthesizeTrips(new LDTTripFile(rb),true);
        logger.info("Synthesizing CT");
        synthesizeTrips(new CTTripFile(rb),false);
        logger.info("Synthesizing ET");
        synthesizeTrips(new ETTripFile(rb),false);
    }

    public void writeSynthesizedTrips() {
        logger.info("Writing sl trips");
        String tazListFile = rb.getString("sl.taz.list.output.filename");
        //todo: deal with classes - constructor needs class names?
        for (Matrix[] m : autoMatrices.matrices) {
            String baseOutFile = rb.getString("sl.link.demand.output.filename").replace(DemandHandler.DEMAND_OUTPUT_MODE_STRING,SL_AUTO_ASSIGN_CLASS);
            for (int i = 0; i < 4; i++) {
                String outFile;
                switch (i) {
                    case 0 : outFile = baseOutFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,AM_STRING_NAME); break;
                    case 1 : outFile = baseOutFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,MD_STRING_NAME); break;
                    case 2 : outFile = baseOutFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,PM_STRING_NAME); break;
                    case 3 : outFile = baseOutFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,NT_STRING_NAME); break;
                    default : throw new RuntimeException("invalid time period: " + i);
                }
                ZipMatrixWriter writer = new ZipMatrixWriter(new File(outFile));
                writer.writeMatrix(m[i]);
            }
        }
        writeTazList(tazListFile,true);
        for (Matrix[] m : truckMatrices.matrices) {
            for (int i = 0; i < 4; i++) {
                String outFile = rb.getString("demand.output.filename").replace(DemandHandler.DEMAND_OUTPUT_MODE_STRING,SL_TRUCK_ASSIGN_CLASS).replace("demand","synthdemand");
                switch (i) {
                    case 0 : outFile = outFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,AM_STRING_NAME); break;
                    case 1 : outFile = outFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,MD_STRING_NAME); break;
                    case 2 : outFile = outFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,PM_STRING_NAME); break;
                    case 3 : outFile = outFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,NT_STRING_NAME); break;
                }
                ZipMatrixWriter writer = new ZipMatrixWriter(new File(outFile));
                writer.writeMatrix(m[i]);
            }
        }
        writeTazList(tazListFile,false);
    }

    private void writeTazList(String baseFile, boolean auto) {
        Map<String,Integer> tazMatrixMap = auto ? autoZoneMatrixMap : truckZoneMatrixMap;
        baseFile = baseFile.replace(DemandHandler.DEMAND_OUTPUT_MODE_STRING,auto ? SL_AUTO_ASSIGN_CLASS : SL_TRUCK_ASSIGN_CLASS);
        Map<Integer,String> matrixTazMap = new TreeMap<Integer,String>();
        for (String taz : tazMatrixMap.keySet())
            matrixTazMap.put(tazMatrixMap.get(taz),taz);
        TextFile file = new TextFile();
        file.addLine("Index,TAZ");
        for (int matrixId : matrixTazMap.keySet())
            file.addLine(matrixId + "," + matrixTazMap.get(matrixId));
        file.writeTo(baseFile);
    }

    private void initializeMatrices() {
        logger.info("Initializing SL matrices");
        for (int i = 0; i < 4; i++) {
            initializeMatrices(i,true);
            initializeMatrices(i,false);
        }
    }

    //returns mapping from zone/link name to matrix index
    private void initializeMatrices(int period, boolean auto) {
        SelectLinkData sld = auto ? autoSelectLinkData : truckSelectLinkData;
        OdMatrixGroup omg = auto ? autoMatrices : truckMatrices;
        // file name = demand_matrix_{MODE}_{PERIOD}.zmx
        String matrixFile = rb.getString("demand.output.filename");
        //note: the select link stuff essentially assumes 1 truck class, so this cannot handle multi truck assignment
        matrixFile = matrixFile.replace(DemandHandler.DEMAND_OUTPUT_MODE_STRING,auto ? SL_AUTO_ASSIGN_CLASS : SL_TRUCK_ASSIGN_CLASS);
        switch (period) {
            case 0 : matrixFile = matrixFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,AM_STRING_NAME); break;
            case 1 : matrixFile = matrixFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,MD_STRING_NAME); break;
            case 2 : matrixFile = matrixFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,PM_STRING_NAME); break;
            case 3 : matrixFile = matrixFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,NT_STRING_NAME); break;
        }
        Matrix demand = new ZipMatrixReader(new File(matrixFile)).readMatrix();

        //create external numbers and mapping to internal numbers
        List<String> extNums = formBaseExternalNumbers();
        for (String s : sld.getLinkList())
            extNums.add(s);
        Map<String,Integer> zoneMatrixMap = new HashMap<String, Integer>();
        int counter = 1;
        for (String s : extNums)
            zoneMatrixMap.put(s,counter++);
        logger.info("Ext numcount = " + extNums.size());
        Matrix newDemand = new Matrix(extNums.size(),extNums.size());
        float[][] values = newDemand.getValues();
        float[][] baseValues = demand.getValues();
        logger.info("base value count = " + demand.getRowCount());
        logger.info("value count = " + newDemand.getRowCount());

        for (int i = 0; i < demand.getRowCount(); i++)
            System.arraycopy(baseValues[i],0,values[i],0,baseValues[i].length);
        omg.initMatrix(newDemand,period);
        if (auto)
            autoZoneMatrixMap = zoneMatrixMap;
        else
            truckZoneMatrixMap = zoneMatrixMap;
    }

    private List<String> formBaseExternalNumbers() {
        TableDataSet taz;
        try {
            taz = new CSVFileReader().readFile(new File(rb.getString("sl.taz.data.filename")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<String> orderedTazs = new LinkedList<String>();
        for (int i = 1; i <= taz.getRowCount(); i++)
            if (!taz.getStringValueAt(i,"TYPE").equals("WM")) //todo: don't hardcode these two fields
                orderedTazs.add("" + ((int) taz.getValueAt(i,"AZONE")));
        return orderedTazs;
    }

    private void synthesizeTrips(TripFile tripFile, boolean autoClass) {
        SelectLinkData sld = autoClass ? autoSelectLinkData : truckSelectLinkData;
        OdMatrixGroup mg = autoClass ? autoMatrices : truckMatrices;
        Map<String,Integer> zoneMatrixMap = autoClass ? autoZoneMatrixMap : truckZoneMatrixMap;

        int originId = -1;
        int destId = -1;
        int counter = 0;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(tripFile.path));
            String line = reader.readLine();

            String[] header;
            header = line.trim().split(",");
            for (String h : header) {
                if (h.equals(tripFile.originField))
                    originId = counter;
                else if (h.equals(tripFile.destField))
                    destId = counter;
                counter++;
            }

            while ((line = reader.readLine()) != null) {
                String[] tripFileLine = line.trim().split(",");
                String origin = tripFileLine[originId];
                String dest = tripFileLine[destId];
                String od = SelectLinkData.formODLookup(origin,dest);
                if (!sld.containsOd(od))
                    continue;
                double trips = tripFile.getTripFromRecord(tripFileLine)*factors[tripFile.getModeIdFromRecord(tripFileLine)];
                if (trips == 0.0)
                    continue;
                Matrix ofInterest = mg.matrices[tripFile.classifier.getClass(tripFileLine)][tripFile.getTimePeriodFromRecord(tripFileLine)];
                int mo = zoneMatrixMap.get(origin);
                int md = zoneMatrixMap.get(dest);
                //subtract from original od in matrix
                List<SelectLinkData.LinkData> linkData = sld.getDataForOd(od);
                for (SelectLinkData.LinkData ld : linkData) {
                    int lid = zoneMatrixMap.get(ld.getMatrixEntryName());
                    double linkTrips = ld.getOdPercentage(od)*trips;
                    ofInterest.setValueAt(mo,md,(float) (ofInterest.getValueAt(mo,md)-linkTrips));
                    ofInterest.setValueAt(mo,lid,(float) (ofInterest.getValueAt(mo,lid)+linkTrips));
                    ofInterest.setValueAt(lid,md,(float) (ofInterest.getValueAt(lid,md)+linkTrips));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private class OdMatrixGroup {
        private final Matrix[][] matrices;

        private OdMatrixGroup(int classes) {
            matrices = new Matrix[classes][4];
        }

        void initMatrix(Matrix baseMatrix, int period) {
            for (int i = 0; i < matrices.length; i++) {
                Matrix m = new Matrix(baseMatrix.getRowCount(),baseMatrix.getColumnCount());
                float[][] values = m.getValues();
                float[][] baseValues = baseMatrix.getValues();

                for (int j = 0; j < baseMatrix.getRowCount(); j++)
                    System.arraycopy(baseValues[j],0,values[j],0,baseValues[j].length);
                matrices[i][period] = m;
            }
        }
    }

    public abstract static class TripFile {
        private final String path;
        private final String originField;
        private final String destField;
        private final TripClassifier classifier;

        private final int amStart;
        private final int amEnd;
        private final int mdEnd;
        private final int pmEnd;

        public TripFile(String path, String originField, String destField, TripClassifier classifier, ResourceBundle rb) {
            this.path = path;
            this.originField = originField;
            this.destField = destField;
            this.classifier = classifier;

            amStart = Integer.parseInt(rb.getString( "am.peak.start"));
            amEnd = Integer.parseInt(rb.getString( "am.peak.end"));
            mdEnd = Integer.parseInt(rb.getString( "md.offpeak.end"));
            pmEnd = Integer.parseInt(rb.getString( "pm.peak.end"));
        }

        abstract int getTripTimeFromRecord(String ... data);

        double getTripFromRecord(String ... data) {
            return 1;
        }

        public int getModeIdFromRecord(String ... data) {
            return 0;
        }

        public int getTimePeriodFromRecord(String ... data) {
            int timePeriod = getTripTimeFromRecord(data);
            if (timePeriod < amStart)
                return 3;
            else if (timePeriod < amEnd)
                return 0;
            else if (timePeriod < mdEnd)
                return 1;
            else if (timePeriod < pmEnd)
                return 2;
            else
                return 3;
        }
    }

    public static interface TripClassifier {
        int getClass(String ... data);
    }

    //todo: confirm this is correct   - right, I think
    private double[] formTripFactorLookup(ResourceBundle rb) {
        char[] modeChars = { 'a', 'd', 'e', 'f', 'g', 'h' }; //cribbed from ts.Network - could maybe eventually make that a static variable for access?
        String[] highwayModeCharacters = new String[modeChars.length];
        for (int i = 0; i < modeChars.length; i++)
            highwayModeCharacters[i] = ((Character) modeChars[i]).toString();

        String[] f = rb.getString("userClass.pces").trim().split("\\s");
        double[] tf = new double[f.length];
        int counter = 0;
        for (String s : f)
            tf[counter++] = Double.valueOf(s);

        double[] factorArray = new double[highwayModeCharacters.length];
        factorArray[0] = tf[0]; //auto factor
        for (int i = 1; i < factorArray.length; i++)
            for (String modeClass : rb.getString("truckClass" + i + ".modes").trim().split("\\s"))
                for (int j = 1; j < highwayModeCharacters.length; j++)
                    if (modeClass.equals(highwayModeCharacters[j]))
                        factorArray[j] = tf[i];
        return factorArray;
    }

    private class SDTTripFile extends TripFile {
        private final String daId;
        private final String sr2Id;
        private final String sr3Id;
        private final double daTrip = 1.0;
        private final double sr2Trip = 0.5;
        private final double sr3Trip = 1/ DemandHandler.AVERAGE_SR3P_AUTO_OCCUPANCY;

        private SDTTripFile(ResourceBundle rb) {
            super(rb.getString("sdt.person.trips"),"origin","destination",getSDTClassifier(),rb);
            this.daId = rb.getString("driveAlone.identifier");
            this.sr2Id = rb.getString("sharedRide2.identifier");
            this.sr3Id = rb.getString("sharedRide3p.identifier");
        }

        double getTripFromRecord(String ... data) {
            //0      1            2                  3       4               5            6         7            8        9         10       11          12               13         14          15          16      17  18     19
            //hhID	memberID	weekdayTour(yes/no)	tour#	subTour(yes/no)	tourPurpose	tourSegment	tourMode	origin	destination	time	distance	tripStartTime	tripEndTime	tripPurpose	tripMode	income	age	enroll	esr
            //driveAlone.identifier = da
            //sharedRide2.identifier = sr2
            //sharedRide3p.identifier
            String mode = data[15];
            if (mode.equalsIgnoreCase(daId)) return daTrip;
            else if (mode.equalsIgnoreCase(sr2Id)) return sr2Trip;
            else if (mode.equalsIgnoreCase(sr3Id)) return sr3Trip;
            return 0;
        }

        int getTripTimeFromRecord(String ... data) {
            return (int) Double.parseDouble(data[12]);
        }
    }

    private class LDTTripFile extends TripFile {
        private LDTTripFile(ResourceBundle rb) {
            //0        1         2       3        4          5              6      7                     8       9               10           11         12
            //hhID	memberID	tourID	income	tourPurpose	tourMode	origin	destination	distance	time	tripStartTime	tripPurpose	tripMode	vehicleTrip
            super(rb.getString("ldt.vehicle.trips"),"origin","destination",getLDTClassifier(),rb);
        }

        int getTripTimeFromRecord(String ... data) {
            return (int) Double.parseDouble(data[9]);
        }
    }

    private class CTTripFile extends TripFile {
        private CTTripFile(ResourceBundle rb) {
            //0          1                   2       3           4          5              6              7     8        9               10        11         12
            //origin	tripStartTime	duration	destination	tourMode	tripMode	tripFactor	truckID	truckType	carrierType	commodity	weight	distance
            super(rb.getString("ct.truck.trips"),"origin","destination",getCTClassifier(),rb);
        }

        int getTripTimeFromRecord(String ... data) {
            return (int) Double.parseDouble(data[1]);
        }

        public int getModeIdFromRecord(String ... data) {
            return Integer.parseInt(data[8].substring(3));
        }
    }

    private class ETTripFile extends TripFile {
        private ETTripFile(ResourceBundle rb) {
            //0          1                   2       3           4
            //origin	destination	tripStartTime	truckClass	truckVolume

            super(rb.getString("et.truck.trips"),"origin","destination",getETClassifier(),rb);
        }

        double getTripFromRecord(String ... data) {
            return Double.parseDouble(data[4]);
        }

        int getTripTimeFromRecord(String ... data) {
            return (int) Double.parseDouble(data[2]);
        }

        public int getModeIdFromRecord(String ... data) {
            return Integer.parseInt(data[3].substring(3));
        }
    }

    private TripClassifier getSDTClassifier() {
        return new TripClassifier() {
            public int getClass(String ... data) {
                return 0;
            }
        };
    }

    private TripClassifier getLDTClassifier() {
        return new TripClassifier() {
            public int getClass(String ... data) {
                return 0;
            }
        };
    }

    private TripClassifier getCTClassifier() {
        return new TripClassifier() {
            public int getClass(String ... data) {
                return 0;
            }
        };
    }

    private TripClassifier getETClassifier() {
        return new TripClassifier() {
            public int getClass(String ... data) {
                return 0;
            }
        };
    }
}

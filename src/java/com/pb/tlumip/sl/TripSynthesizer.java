package com.pb.tlumip.sl;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.*;
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

    private OdMatrixGroup.OdMatrixGroupCollection autoMatrices;
    private OdMatrixGroup.OdMatrixGroupCollection truckMatrices;
    private final SelectLinkData autoSelectLinkData;
    private final SelectLinkData truckSelectLinkData;
    private final ResourceBundle rb;
    private final boolean balanceOn;

    private final TripFile sdtTripFile;
    private final TripFile ldtTripFile;
    private final TripFile ctTripFile;
    private final TripFile etTripFile;

    private final double[] factors;

    public TripSynthesizer(ResourceBundle rb, SelectLinkData autoSelectLinkData, SelectLinkData truckSelectLinkData, TripClassifier sdtClassifier, TripClassifier ldtClassifier, TripClassifier ctClassifier, TripClassifier etClassifier) {
        logger.info("Initializing SL Synthesizer");
        this.rb = rb;
        this.autoSelectLinkData = autoSelectLinkData;
        this.truckSelectLinkData = truckSelectLinkData;
        factors = formTripFactorLookup(rb);

        sdtTripFile = new SDTTripFile(rb,sdtClassifier);
        ldtTripFile = new LDTTripFile(rb,ldtClassifier);
        ctTripFile = new CTTripFile(rb,ctClassifier);
        etTripFile = new ETTripFile(rb,etClassifier);

        balanceOn = false; //indicates if balancing on; if not, then only origin scaling factor will be used for autobalancing
                           //NOTE: I think balancing will not work, because there is not a consistent way to deal with weaving
    }

    OdMatrixGroup.OdMatrixGroupCollection getSynthesizedMatrices(boolean auto) {
        return auto ? autoMatrices : truckMatrices;
    }

    void synthesizeTrips() {
        initializeMatrices();
        logger.info("Synthesizing SDT");
        synthesizeTrips(sdtTripFile,true);
        logger.info("Synthesizing LDT");
        synthesizeTrips(ldtTripFile,true);
        logger.info("Synthesizing CT");
        synthesizeTrips(ctTripFile,false);
        logger.info("Synthesizing ET");
        synthesizeTrips(etTripFile,false);

        if (balanceOn) {
            for (String type : autoMatrices.keySet()) {
                logger.info("Balancing auto trips for " + type);
                autoMatrices.put(type,balanceTrips((OdMatrixGroup.OdMarginalMatrixGroup) autoMatrices.get(type)));
            }
            for (String type : autoMatrices.keySet()) {
                logger.info("Balancing truck trips for " + type);
                truckMatrices.put(type,balanceTrips((OdMatrixGroup.OdMarginalMatrixGroup) truckMatrices.get(type)));
            }
        }
    }

    private void initializeMatrices() {
        logger.info("Initializing SL matrices");
        OdMatrixGroup a = new OdMatrixGroup.OdMarginalMatrixGroup();
        OdMatrixGroup t = new OdMatrixGroup.OdMarginalMatrixGroup();
        for (int i = 0; i < 4; i++) {
            initializeMatrices(i,true,a);
            initializeMatrices(i,false,t);
        }
        autoMatrices = new OdMatrixGroup.OdMatrixGroupCollection(a);
        truckMatrices = new OdMatrixGroup.OdMatrixGroupCollection(t);
    }

    //returns mapping from zone/link name to matrix index
    private void initializeMatrices(int period, boolean auto, OdMatrixGroup omg) {
        SelectLinkData sld = auto ? autoSelectLinkData : truckSelectLinkData;

        //create external numbers and mapping to internal numbers
        List<String> extNums = formBaseExternalNumbers();
        for (String s : sld.getExternalStationList())
            extNums.add(s);
        Map<String,Integer> zoneMatrixMap = new HashMap<String, Integer>();
        for (String zone : sld.getExteriorZones())
            if (!extNums.remove(zone))
                System.out.println("Couldn't remove: " + zone);
        int counter = 1;
        for (String s : extNums)
            zoneMatrixMap.put(s,counter++);

        Matrix newDemand = new Matrix(extNums.size(),extNums.size());
        omg.initMatrix(newDemand,period);
        omg.setZoneMatrixMap(zoneMatrixMap);
    }

    /* Get taz numbers for SWIM zones */
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
        OdMatrixGroup.OdMatrixGroupCollection omc = autoClass ? autoMatrices : truckMatrices;
        Map<String,Integer> zoneMatrixMap = omc.getTemplate().getZoneMatrixMap();

        double tripsLostToWeaving = 0.0; //trips that can't be used because od has a weaving path

        Set<String> exteriorZones = sld.getExteriorZones();
        //crazy mapping: [Matrix : [od : [in/out/total : [link/(total in/out) : (link percentage)/(total trips)]]]]
        Map<Matrix,Map<String,Map<String,Map<String,Double>>>> eeTracker = new HashMap<Matrix,Map<String,Map<String,Map<String,Double>>>>();
        //matrix -> marginal mapping, needed for ee trips
        Map<Matrix,Matrix[]> marginalMap = new HashMap<Matrix,Matrix[]>();

        int originId = -1;
        int destId = -1;
        double tripCounter = 0;
        double eeTripCounter = 0;
        double iiTripCounter = 0;
        double eiTripCounter = 0;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(tripFile.path));
            String line = reader.readLine();

            //read header
            String[] header;
            int counter = 0;
            header = line.trim().split(",");
            for (String h : header) {
                if (h.equals(tripFile.originField))
                    originId = counter;
                else if (h.equals(tripFile.destField))
                    destId = counter;
                counter++;
            }

            //read trips
            counter = 0;
            while ((line = reader.readLine()) != null) {
                if (++counter % 100000 == 0 && counter < 1000000)
                    logger.info("\tProcessed " + counter + " Trips.");
                if (counter % 1000000 == 0)
                    logger.info("\tProcessed " + counter + " Trips.");

                String[] tripFileLine = line.trim().split(",");
                String origin = tripFileLine[originId];
                String dest = tripFileLine[destId];
                String od = SelectLinkData.formODLookup(origin,dest);
                tripFile.classifier.setExtraData(sld,tripFileLine);
                if (!sld.containsOd(od))
                    continue;

                //trips = trips from record * scaling factor from model * exogenous scaling factor
                double trips = tripFile.getTripFromRecord(tripFileLine)*factors[tripFile.getModeIdFromRecord(tripFileLine)];
                if (trips == 0.0)
                    continue;
                tripCounter += trips;
                OdMatrixGroup.OdMarginalMatrixGroup omg = (OdMatrixGroup.OdMarginalMatrixGroup) omc.get(tripFile.getNormalizedTripTypeFromRecord(tripFileLine));
                int period = tripFile.getTimePeriodFromRecord(tripFileLine);
                Matrix ofInterest = omg.getMatrix(period);
                ColumnVector originMarginals = omg.getOriginMarginals(period);
                RowVector destMarginals = omg.getDestinationMarginals(period);
                int mo = zoneMatrixMap.containsKey(origin) ? zoneMatrixMap.get(origin) : -1;
                int md = zoneMatrixMap.containsKey(dest) ? zoneMatrixMap.get(dest) : -1;

                // exogenous scaling factors for o/d marginals
                double originFactor = tripFile.classifier.getOriginFactor(sld,tripFileLine);
                //double destinationFactor = tripFile.classifier.getDestinationFactor(tripFileLine);
                //if not balancing, dest factor becomes origin factor
                double destinationFactor = balanceOn ? tripFile.classifier.getDestinationFactor(sld,tripFileLine) : originFactor;
                //next two numbers say how to factor the "link zone" trips in the marginals
//                double linkOriginFactor = 1.0;
//                double linkDestinationFactor = 1.0;

                if (sld.getWeavingZones().contains(od)) {
                    List<SelectLinkData.WeavingData> wds = sld.getWeavingData(od);
                    if (wds == null) {
                        logger.warn("Missing weaving data for " + od);
                        continue;
                    }
                    for (SelectLinkData.WeavingData wd : wds) {
                        double linkTrips = wd.getPercentage()*trips * (balanceOn ? 1.0 : originFactor);
                        List<String> links = wd.getFromNodeToNodes();
                        if (wd.isInvalid()) {
                            logger.warn("Skipping trips because of invalid path: " + linkTrips);
                            tripsLostToWeaving += linkTrips;
                            continue;
                        }
                        boolean skip = wd.isFirstLinkIn();  //skip trips that are outside the region
                        int lcounter = 0;
                        int lastId = mo;
                        for (String link : links) {
                            SelectLinkData.LinkData ld = wd.getRepresentativeLinkData(lcounter);
                            int lid = zoneMatrixMap.get(ld.getMatrixEntryName());
                            if (!skip) {
                                    ofInterest.setValueAt(lastId,lid,(float) (ofInterest.getValueAt(lastId,lid)+linkTrips));
                                if (balanceOn) {
                                    //todo - not clear that there is a consistent way to do this
                                }
                            }
                            skip ^= true; //skip every other link
                            lastId = lid;
                            lcounter++;
                        }
                        if (!skip)
                            ofInterest.setValueAt(lastId,md,(float) (ofInterest.getValueAt(lastId,md)+linkTrips));
                    }
                    continue;
                }

                //set interior/exterior
                boolean ee = exteriorZones.contains(origin) && exteriorZones.contains(dest);
                boolean ii = !exteriorZones.contains(origin) && !exteriorZones.contains(dest);
                if (ee)
                    eeTripCounter += trips;
                else if (ii)
                    iiTripCounter += trips;
                else
                    eiTripCounter += trips;

                //subtract from original od in matrix
                List<SelectLinkData.LinkData> linkData = sld.getDataForOd(od);
                for (SelectLinkData.LinkData ld : linkData) {
                    int lid = zoneMatrixMap.get(ld.getMatrixEntryName());
                    double linkTrips = ld.getOdPercentage(od)*trips * (balanceOn ? 1.0 : originFactor);
                    //marginals don't matter if not balancing, so ignore them
                    if (ii) {
                        if (ld.getIn()) {
                            ofInterest.setValueAt(lid,md,(float) (ofInterest.getValueAt(lid,md)+linkTrips));
                            if (balanceOn) {
                                originMarginals.setValueAt(lid,(float) (originMarginals.getValueAt(lid)+linkTrips*originFactor));
                                destMarginals.setValueAt(md,(float) (destMarginals.getValueAt(md)+linkTrips*destinationFactor));
                            }
                        } else {
                            ofInterest.setValueAt(mo,lid,(float) (ofInterest.getValueAt(mo,lid)+linkTrips));
                            if (balanceOn) {
                                originMarginals.setValueAt(mo,(float) (originMarginals.getValueAt(mo)+linkTrips*originFactor));
                                destMarginals.setValueAt(lid,(float) (destMarginals.getValueAt(lid)+linkTrips*destinationFactor));
                            }
                        }
                    } else {
                        if (exteriorZones.contains(origin) && !exteriorZones.contains(dest)) {
                            ofInterest.setValueAt(lid,md,(float) (ofInterest.getValueAt(lid,md)+linkTrips));
                            if (balanceOn) {
                                originMarginals.setValueAt(lid,(float) (originMarginals.getValueAt(lid)+linkTrips*originFactor));
                                destMarginals.setValueAt(md,(float) (destMarginals.getValueAt(md)+linkTrips*destinationFactor));
                            }
                        } else if (!exteriorZones.contains(origin) && exteriorZones.contains(dest)) {
                            ofInterest.setValueAt(mo,lid,(float) (ofInterest.getValueAt(mo,lid)+linkTrips));
                            if (balanceOn) {
                                originMarginals.setValueAt(mo,(float) (originMarginals.getValueAt(mo)+linkTrips*originFactor));
                                destMarginals.setValueAt(lid,(float) (destMarginals.getValueAt(lid)+linkTrips*destinationFactor));
                            }
                        }
                    }
                    if (ee) {
                        //put data into tracker
                        if (!eeTracker.containsKey(ofInterest)) {
                            eeTracker.put(ofInterest,new HashMap<String,Map<String,Map<String,Double>>>());
                            marginalMap.put(ofInterest,new Matrix[] {originMarginals,destMarginals});
                        }
                        Map<String,Map<String,Map<String,Double>>> eeSub = eeTracker.get(ofInterest);
                        if (!eeSub.containsKey(od)) {
                            Map<String,Map<String,Double>> subTracker = new HashMap<String,Map<String,Double>>();
                            subTracker.put("in",new HashMap<String,Double>());
                            subTracker.put("out",new HashMap<String,Double>());
                            subTracker.put("total",new HashMap<String,Double>());
                            subTracker.get("total").put("in",0.0);
                            subTracker.get("total").put("out",0.0);
                            eeSub.put(od,subTracker);
                        }
                        if (ld.getIn()) {
                            //originMarginals.setValueAt(lid,(float) (originMarginals.getValueAt(lid)+linkTrips*linkOriginFactor));
                            eeSub.get(od).get("in").put(ld.getMatrixEntryName(),ld.getOdPercentage(od));
                            eeSub.get(od).get("total").put("in",linkTrips+eeSub.get(od).get("total").get("in"));
                        } else {
                            //destMarginals.setValueAt(lid,(float) (destMarginals.getValueAt(lid)+linkTrips*linkDestinationFactor));
                            eeSub.get(od).get("out").put(ld.getMatrixEntryName(),ld.getOdPercentage(od));
                            eeSub.get(od).get("total").put("out",linkTrips+eeSub.get(od).get("total").get("out"));
                        }

                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //deal with ee trips
        for (Matrix ofInterest : eeTracker.keySet()) {
            Map<String,Map<String,Map<String,Double>>> eeSub = eeTracker.get(ofInterest);
            for (String od : eeSub.keySet()) {
                //check in/out total equality
                double totalTrips =  eeSub.get(od).get("total").get("in");
                if (totalTrips != eeSub.get(od).get("total").get("out"))
                    logger.warn("Total ee trips for od " + od + " not equal for in (" + eeSub.get(od).get("total").get("in") + ") and out (" + eeSub.get(od).get("total").get("out") + ").");

                for (String linkIn : eeSub.get(od).get("in").keySet()) {
                    double ip = eeSub.get(od).get("in").get(linkIn);
                    int origin = zoneMatrixMap.get(linkIn);
                    for (String linkOut : eeSub.get(od).get("out").keySet()) {
                        double trips = eeSub.get(od).get("out").get(linkOut)*ip*totalTrips*tripFile.classifier.getExternalExternalFactor(sld,linkIn,linkOut);
                        int dest = zoneMatrixMap.get(linkOut);
                        ofInterest.setValueAt(origin,dest,(float) (ofInterest.getValueAt(origin,dest)+trips));
                        if (balanceOn) {
                            ((ColumnVector) marginalMap.get(ofInterest)[0]).setValueAt(origin,(float) (((ColumnVector) marginalMap.get(ofInterest)[0]).getValueAt(origin)+trips));
                            ((RowVector) marginalMap.get(ofInterest)[1]).setValueAt(dest,(float) (((RowVector) marginalMap.get(ofInterest)[1]).getValueAt(dest)+trips));
                        }
                    }
                }
            }
        }

        logger.info("Total ee trips tallied: " + Math.round(eeTripCounter));
        logger.info("Total ei/ie trips tallied: " + Math.round(eiTripCounter));
        logger.info("Total ii trips tallied: " + Math.round(iiTripCounter));
        logger.info("Total trips tallied: " + Math.round(tripCounter));
        logger.info(String.format("Trips lost to weaving: %.2f (%.2f%%)",tripsLostToWeaving,tripsLostToWeaving / tripCounter * 100));
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
        abstract String getTripTypeFromRecord(String ... data);

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

        public String getNormalizedTripTypeFromRecord(String ... data) {
            return getTripTypeFromRecord(data).toLowerCase().replace(" ","_");
        }
    }

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

    private OdMatrixGroup balanceTrips(OdMatrixGroup.OdMarginalMatrixGroup trips) {
        OdMatrixGroup omg = new OdMatrixGroup();
        omg.setZoneMatrixMap(trips.getZoneMatrixMap());
        for (int p = 0; p < 4; p++) {
            ColumnVector originMarginal = trips.getOriginMarginals(p);
            RowVector destMarginal = trips.getDestinationMarginals(p);
            double originMarginalSum = originMarginal.getSum(); //0.0;
            double destMarginalSum = destMarginal.getSum(); //0.0;
            Matrix mat = trips.getMatrix(p);
            List<Integer> modelZones = new LinkedList<Integer>();
            List<Integer> linkZones = new LinkedList<Integer>();
            for (String zone : trips.getZoneMatrixMap().keySet()) {
                if (SelectLinkData.isLinkZone(zone))
                    linkZones.add(trips.getZoneMatrixMap().get(zone));
                else
                    modelZones.add(trips.getZoneMatrixMap().get(zone));
            }
            //zero out ii trips, and sum total trips for marginal check
            double tripSum = 0.0;
            for (int mz : modelZones) {
                for (int mz2 : modelZones)
                    mat.setValueAt(mz,mz2,0.0f);
                for (int lz : linkZones) {
                    tripSum += mat.getValueAt(mz,lz);
                    tripSum += mat.getValueAt(lz,mz);
                }
            }
            //have to sum link-link trips
            for (int lz1 : linkZones)
                for (int lz2 : linkZones)
                    tripSum += mat.getValueAt(lz1,lz2);
            //renormalize marginals
            if (Math.abs(tripSum - originMarginalSum) > 1.0) {
                logger.info("Trip sum and origin marginal sum mismatch for period " + p + ", will normalize: " + tripSum + " " + originMarginalSum);
                double originFactor = tripSum / originMarginalSum;
                for (int i = 1; i <= originMarginal.size(); i++)
                    originMarginal.setValueAt(i,(float) (originMarginal.getValueAt(i) * originFactor));
            }
            if (Math.abs(tripSum - destMarginalSum) > 1.0) {
                logger.info("Trip sum and destination marginal sum mismatch for period " + p + ", will normalize: " + tripSum + " " + destMarginalSum);
                double destFactor = tripSum / destMarginalSum;
                for (int i = 1; i <= destMarginal.size(); i++)
                    destMarginal.setValueAt(i,(float) (destMarginal.getValueAt(i) * destFactor));
            }
            logger.info("Origin marginal sum: " + originMarginalSum);
            logger.info("Destination marginal sum: " + destMarginalSum);
            logger.info("\tBalancing trips for period " + p);
            MatrixBalancerRM mb = new MatrixBalancerRM(mat,originMarginal,destMarginal,0.1,20,MatrixBalancerRM.ADJUST.NONE);
            omg.initMatrix(mb.balance(),p);
        }
        return omg;
    }

    private class SDTTripFile extends TripFile {
        private final String daId;
        private final String sr2Id;
        private final String sr3Id;
        private final double daTrip = 1.0;
        private final double sr2Trip = 0.5;
        private final double sr3Trip = 1/ DemandHandler.AVERAGE_SR3P_AUTO_OCCUPANCY;

        private SDTTripFile(ResourceBundle rb, TripClassifier classifier) {
            super(rb.getString("sdt.person.trips"),"origin","destination",classifier,rb);
            this.daId = rb.getString("driveAlone.identifier");
            this.sr2Id = rb.getString("sharedRide2.identifier");
            this.sr3Id = rb.getString("sharedRide3p.identifier");
        }

        double getTripFromRecord(String ... data) {
            //0      1            2                  3       4               5            6         7            8        9         10       11          12               13         14          15          16      17  18     19
            //hhID	memberID	weekdayTour(yes/no)	tour#	subTour(yes/no)	tourPurpose	tourSegment	tourMode	origin	destination	time	distance	tripStartTime	tripEndTime	tripPurpose	tripMode	income	age	enroll	esr
            String mode = data[15];
            if (mode.equalsIgnoreCase(daId)) return daTrip;
            else if (mode.equalsIgnoreCase(sr2Id)) return sr2Trip;
            else if (mode.equalsIgnoreCase(sr3Id)) return sr3Trip;
            return 0;
        }

        int getTripTimeFromRecord(String ... data) {
            return (int) Double.parseDouble(data[12]);
        }

        String getTripTypeFromRecord(String ... data) {
            return data[14];
        }
    }

    private class LDTTripFile extends TripFile {
        private LDTTripFile(ResourceBundle rb, TripClassifier classifier) {
            //0        1         2       3        4          5              6      7           8       9               10           11         12
            //hhID	memberID	tourID	income	tourPurpose	tourMode	origin	destination	distance	time	tripStartTime	tripPurpose	tripMode	vehicleTrip
            super(rb.getString("ldt.vehicle.trips"),"origin","destination",classifier,rb);
        }

        int getTripTimeFromRecord(String ... data) {
            return (int) Double.parseDouble(data[10]);
        }

        String getTripTypeFromRecord(String ... data) {
            return data[11];
        }
    }

    private class CTTripFile extends TripFile {
        private CTTripFile(ResourceBundle rb, TripClassifier classifier) {
            //0          1                   2       3           4          5              6              7     8        9               10        11         12
            //origin	tripStartTime	duration	destination	tourMode	tripMode	tripFactor	truckID	truckType	carrierType	commodity	weight	distance
            super(rb.getString("ct.truck.trips"),"origin","destination",classifier,rb);
        }

        int getTripTimeFromRecord(String ... data) {
            return (int) Double.parseDouble(data[1]);
        }

        public int getModeIdFromRecord(String ... data) {
            return Integer.parseInt(data[8].substring(3));
        }

        String getTripTypeFromRecord(String ... data) {
            return "";
        }
    }

    private class ETTripFile extends TripFile {
        private ETTripFile(ResourceBundle rb, TripClassifier classifier) {
            //0          1                   2       3           4
            //origin	destination	tripStartTime	truckClass	truckVolume
            super(rb.getString("et.truck.trips"),"origin","destination",classifier,rb);
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

        String getTripTypeFromRecord(String ... data) {
            return "";
        }
    }
}

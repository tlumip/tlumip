package com.pb.tlumip.sl;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TextFile;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author crf <br/>
 *         Started: Dec 1, 2009 4:29:05 PM
 */
public class SelectLinkData {
    private static Logger logger = Logger.getLogger(SelectLinkData.class);
    public static final String LINK_MATRIX_ENTRY_PREFIX = "_";

    public static boolean isLinkZone(String zoneName) {
        return zoneName.startsWith(LINK_MATRIX_ENTRY_PREFIX);
    }

    public static String formODLookup(String fromZone, String toZone) {
        return fromZone + " " + toZone;
    }

    public static String getZoneFromLookup(String odLookup, boolean origin) {
        return odLookup.split("\\s")[origin ? 0 : 1];
    }

    private Map<String,List<LinkData>> linkData = new HashMap<String,List<LinkData>>();
    private List<String> externalStationList;
    private Set<String> internalZones = null;
    private Set<String> externalZones = null;
    private Set<String> weavingZones = null;

    public SelectLinkData(String linkDataFile, String assignClass) {
        loadLinkData(linkDataFile,assignClass);
    }

    public boolean containsOd(String od) {
        return linkData.containsKey(od);
    }

    public List<LinkData> getDataForOd(String od) {
        return linkData.get(od);
    }

    public List<String> getExternalStationList() {
        return externalStationList;
    }

    private void loadLinkData(String linkDataFile,String assignClass) {
        String assignClassFieldName = "ASSIGNCLASS";
        String linkFieldName = "FROMNODETONODE";
        String directionFieldName = "DIRECTION";
        String originFieldName = "FROMZONE";
        String destFieldName = "TOZONE";
        String percentageFieldName = "PERCENT";
        String externalStationNumberFieldName = "STATIONNUMBER";
        TableDataSet ld;
        try {
            ld = new CSVFileReader().readFile(new File(linkDataFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String,LinkData> pureLinkData = new HashMap<String,LinkData>();

        for (int i = 1; i <= ld.getRowCount(); i++) {
            if (!ld.getStringValueAt(i,assignClassFieldName).equals(assignClass))
                continue;
            String fromNodeToNode = ld.getStringValueAt(i,linkFieldName);
            if (!pureLinkData.containsKey(fromNodeToNode))
                pureLinkData.put(fromNodeToNode,new LinkData(fromNodeToNode,ld.getStringValueAt(i,directionFieldName).equalsIgnoreCase("in"),(int) ld.getValueAt(i,externalStationNumberFieldName)));
            String origin = "" + ((int) ld.getValueAt(i,originFieldName));
            String dest = "" + ((int) ld.getValueAt(i,destFieldName));
            pureLinkData.get(fromNodeToNode).addOdPercentage(formODLookup(origin,dest),ld.getValueAt(i,percentageFieldName));
        }

        Set<String> links = new TreeSet<String>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                return ((Integer) getLinkExternalStationNumber(o1)).compareTo(getLinkExternalStationNumber(o2));
            }
        });

        for (String s : pureLinkData.keySet()) {
            LinkData ldata = pureLinkData.get(s);
            links.add(ldata.getMatrixEntryName());
            for (String id : ldata.odPercentages.keySet()) {
                if (!linkData.containsKey(id))
                    linkData.put(id,new LinkedList<LinkData>());
                linkData.get(id).add(ldata);
            }
        }

        externalStationList = new LinkedList<String>(links);
    }

    public void setInteriorExteriorZones() {
        //algorithm:
        //  1) got through all ods
        //  2) if od pair uses in and out links, skip it
        //  2a) aggregate link percentages, if > 2 then log as a weaving zone
        //  4) otherwise, depending on what it uses, set interior zone
        //  will miss interior zones which have no ie/ei trips but so will last algorithm, I think
        internalZones = new TreeSet<String>(); //sort them correctly
        Set<String> eeiiZones = new HashSet<String>(); //for checking
        Map<String,Double> weavingZones = new HashMap<String,Double>(); //in-out-in-... or out-in-out-...
        for (String od : linkData.keySet()) {
            byte mask = 0;
            double linkPercentage = 0.0;
            for (LinkData data : linkData.get(od)) {
                mask |= data.in ? 1 : 2;
                linkPercentage += data.getOdPercentage(od);
            }
            switch (mask) {
                case 1 : internalZones.add(getZoneFromLookup(od,false)); break;
                case 2 : internalZones.add(getZoneFromLookup(od,true)); break;
                default : if (linkPercentage > 2.01)  //for rounding errors, but want to capture small percentage weaving trips
                              weavingZones.put(od,linkPercentage);
                          else
                              eeiiZones.add(od);
            }
        }
        //write weaving zone data - will eventually work this out using Ben's code
        TextFile tf = new TextFile();
        tf.addLine("od,links,percentage");
        for (String od : weavingZones.keySet()) {
            Map<Double,List<String>> weavingLinks = new HashMap<Double,List<String>>();
            for (LinkData ld : linkData.get(od)) {
                double percentage = ld.getOdPercentage(od);
                if (!weavingLinks.containsKey(percentage))
                    weavingLinks.put(percentage,new LinkedList<String>());
                weavingLinks.get(percentage).add(ld.getFromNodeToNode());
            }
            for (double percentage : weavingLinks.keySet()) {
                StringBuffer sb = new StringBuffer();
                sb.append(od).append(",");
                Iterator<String> links = weavingLinks.get(percentage).iterator();
                sb.append(links.next());
                while(links.hasNext())
                    sb.append(";").append(links.next());
                tf.addLine(sb.append(",").append(percentage).toString());
            }
        }
        tf.writeTo("C:\\chris\\projects\\tlumip\\path_analysis\\sl_weaving.csv");

        //checking
        for (String od : eeiiZones)
            if (internalZones.contains(getZoneFromLookup(od,true)))
                if (internalZones.contains(getZoneFromLookup(od,false)))
                    logger.info("ii pair: " + od);
                else
                    logger.warn("Conflict for od " + od + "; set as ee/ii but found internal,external");
            else if (internalZones.contains(getZoneFromLookup(od,false)))
                logger.warn("Conflict for od " + od + "; set as ee/ii but found external,internal");

        //set external zones
        externalZones = new HashSet<String>();
        //add all zones
        for (String od : linkData.keySet()) {
            externalZones.add(getZoneFromLookup(od,true));
            externalZones.add(getZoneFromLookup(od,false));
        }
        //remove interior zones
        for (String zone : internalZones)
            externalZones.remove(zone);

        //log weaves
        for (String od : weavingZones.keySet())
            logger.debug("Weaving zone: " + od + " (link percentage: " + weavingZones.get(od) + ")");
        this.weavingZones = weavingZones.keySet();
    }

    public Set<String> getInteriorZones() {
        if (internalZones == null)
            setInteriorExteriorZones();
        return internalZones;
    }

    public Set<String> getInteriorZonesOrig() {
        Set<String> zones = new TreeSet<String>(); //sort them correctly
        //add all "in" points
        for (String od : linkData.keySet())
            for (LinkData data : linkData.get(od))
                if (data.in)
                    zones.add(getZoneFromLookup(od,false));
        //remove all "out" points
        for (String od : linkData.keySet())
            for (LinkData data : linkData.get(od))
                if (!data.in)
                    zones.remove(getZoneFromLookup(od,false));
        return zones;
    }

    public Set<String> getExteriorZones() {
        if (externalZones == null)
            setInteriorExteriorZones();
        return externalZones;  
//        Set<String> zones = new HashSet<String>();
//        //add all zones
//        for (String od : linkData.keySet()) {
//            zones.add(getZoneFromLookup(od,true));
//            zones.add(getZoneFromLookup(od,false));
//        }
//        //remove interior zones
//        for (String zone : getInteriorZones())
//            zones.remove(zone);
//        return zones;
    }

    public Set<String> getWeavingZones() {
        if (weavingZones == null)
            setInteriorExteriorZones();
        return weavingZones;
    }

    public static int getLinkExternalStationNumber(String externalStationName) {
        //this will work with external stations and regular zones, hence the check for the prefix
        return Integer.parseInt(externalStationName.startsWith(LINK_MATRIX_ENTRY_PREFIX) ? externalStationName.substring(LINK_MATRIX_ENTRY_PREFIX.length()) : externalStationName);
    }

    class LinkData {
        private final String fromNodeToNode;
        private final Map<String,Double> odPercentages;
        private final boolean in;
        private final int externalStation;

        public LinkData(String fromNodeToNode, boolean in, int externalStation) {
            this.fromNodeToNode = fromNodeToNode;
            this.in = in;
            this.externalStation = externalStation;
            odPercentages = new HashMap<String,Double>();
        }

        public void addOdPercentage(String od, double percentage) {
            odPercentages.put(od,percentage);
        }

        public String getFromNodeToNode() {
            return fromNodeToNode;
        }

        public String getMatrixEntryName() {
            return LINK_MATRIX_ENTRY_PREFIX + externalStation;
//            return LINK_MATRIX_ENTRY_PREFIX + fromNodeToNode;
        }

        public double getOdPercentage(String od) {
            return odPercentages.get(od);
        }


        public boolean getIn() {
            return in;
        }
    }
}

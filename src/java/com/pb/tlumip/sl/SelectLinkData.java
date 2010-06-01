package com.pb.tlumip.sl;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TextFile;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
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
    Map<String,List<WeavingData>> odWeavingMap = null;

    public SelectLinkData(String linkDataFile, String assignClass, ResourceBundle rb) {
        loadLinkData(linkDataFile,assignClass,rb);
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

    public List<WeavingData> getWeavingData(String od) {
        return odWeavingMap.get(od);
    }

    private void loadLinkData(String linkDataFile,String assignClass, ResourceBundle rb) {
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

        setInteriorExteriorZones(rb,assignClass);
    }

    private void setInteriorExteriorZones(ResourceBundle rb, String mode) {
        //algorithm:
        //  1) got through all ods
        //  2) if od pair uses in and out links, skip it
        //  2a) aggregate link percentages, if > 2 then log as a weaving zone
        //  4) otherwise, depending on what it uses, set interior zone
        //  will miss interior zones which have no ie/ei trips but so will last algorithm, I think
        internalZones = new TreeSet<String>(); //sort them correctly
        Set<String> eeiiZones = new HashSet<String>(); //for checking
        Map<String,Double> weavingZones = new HashMap<String,Double>(); //in-out-in-... or out-in-out-...

        //next two maps used for error reporting guess
        Map<String,Integer> internalZoneCounter = new HashMap<String,Integer>();
        Map<String,Integer> externalZoneCounter = new HashMap<String,Integer>(); //may mislabel external zones if ii, but only used if inconsistent, so ok

        for (String od : linkData.keySet()) {
            byte mask = 0;
            double linkPercentage = 0.0;
            for (LinkData data : linkData.get(od)) {
                mask |= data.in ? 1 : 2;
                linkPercentage += data.getOdPercentage(od);
            }
            String o = getZoneFromLookup(od,true);
            String d = getZoneFromLookup(od,false);
            if (!internalZoneCounter.containsKey(o)) {
                internalZoneCounter.put(o,0);
                externalZoneCounter.put(o,0);
            }
            if (!internalZoneCounter.containsKey(d)) {
                internalZoneCounter.put(d,0);
                externalZoneCounter.put(d,0);
            }
            switch (mask) {
                case 1 :
                    internalZones.add(d);
                    internalZoneCounter.put(d,internalZoneCounter.get(d)+1);
                    externalZoneCounter.put(o,externalZoneCounter.get(o)+1);
                    break;
                case 2 :
                    internalZones.add(getZoneFromLookup(od,true));
                    internalZoneCounter.put(o,internalZoneCounter.get(o)+1);
                    externalZoneCounter.put(d,externalZoneCounter.get(d)+1);
                    break;
                default :
                    if (linkPercentage > 2.01) { //for rounding errors, but want to capture small percentage weaving trips
                        weavingZones.put(od,linkPercentage);
                    } else {
                        eeiiZones.add(od);
                        externalZoneCounter.put(o,externalZoneCounter.get(o)+1);
                        externalZoneCounter.put(d,externalZoneCounter.get(d)+1);
                    }
            }
        }

        //checking
        Set<String> problemOds = new HashSet<String>();
        for (String od : eeiiZones) {
            if (internalZones.contains(getZoneFromLookup(od,true))) {
                if (internalZones.contains(getZoneFromLookup(od,false))) {
                    logger.info("ii pair: " + od);
                } else {
                    logger.warn("Conflict for od " + od + "; set as ee/ii but found internal,external");
                    problemOds.add(od);
                }
            } else if (internalZones.contains(getZoneFromLookup(od,false))) {
                logger.warn("Conflict for od " + od + "; set as ee/ii but found external,internal");
                problemOds.add(od);
            }
        }

        if (problemOds.size() > 0) {
            //need to do further checking to give better error logging
            Set<Integer> probablyProblems = new HashSet<Integer>();
            for (String od : problemOds) {
                for (String zone : new String[] {getZoneFromLookup(od,true),getZoneFromLookup(od,false)}) {
                    if (externalZoneCounter.get(zone) > 0 && internalZoneCounter.get(zone) > 0) {
                        boolean probablyEx = externalZoneCounter.get(zone) > internalZoneCounter.get(zone);
                        for (String candidateOd : linkData.keySet()) {
                            if (eeiiZones.contains(candidateOd))
                                continue; //problems are more apparent from non ee/ii zones
                            if (getZoneFromLookup(candidateOd,true).equals(zone)) {
                                for (LinkData ld : linkData.get(candidateOd))
                                    if ((probablyEx && !ld.in))
                                        probablyProblems.add(ld.externalStation);
                            } else if (getZoneFromLookup(candidateOd,false).equals(zone)) {
                                for (LinkData ld : linkData.get(candidateOd))
                                    if ((probablyEx && !ld.in))
                                        probablyProblems.add(ld.externalStation);
                            }
                        }
                    }
                }
            }
            StringBuilder sb = new StringBuilder("Probable problem links (check select link specification file):\n\t");
            for (Integer i : probablyProblems)
                sb.append(" ").append(i);
            logger.warn(sb.toString());
        }

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


        setWeavingData(rb,weavingZones,mode);
    }

    private void setWeavingData(ResourceBundle rb, Map<String,Double> weavingZones, String mode) {
        //log weaves
        for (String od : weavingZones.keySet())
            logger.debug("Weaving zone: " + od + " (link percentage: " + weavingZones.get(od) + ")");
        this.weavingZones = weavingZones.keySet();
        odWeavingMap = new HashMap<String,List<WeavingData>>();
        System.out.println("Weaving zone count: " + this.weavingZones.size());
        if (this.weavingZones.size() == 0)
            return; //no need to continue


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
//        String weavingFile = rb.getString("sl.current.directory") + rb.getString("sl.output.file.select.link.weaving").replace(".csv","_" + mode + ".csv");
        //todo: above line for testing - delete and uncomment next two lines and 5th line down and copyt file at end of method for production
        String weavingFile = rb.getString("sl.current.directory") + rb.getString("sl.output.file.select.link.weaving");
        tf.writeTo(weavingFile);

        //generate weaving paths
        SelectLink.runRScript("sl.select.link.node.sequence.r.file","generate select link weaving paths",rb,ResourceUtil.getProperty(rb,"sl.mode.key") + "=" + mode);


        CSVFileReader reader = new CSVFileReader();
        try {
            TableDataSet tds = reader.readFile(new File(weavingFile));
            for (int i = 1; i <= tds.getRowCount(); i++) {
                String od = tds.getStringValueAt(i,"od");
                if (!odWeavingMap.containsKey(od))
                    odWeavingMap.put(od,new LinkedList<WeavingData>());
                String[] links = tds.getStringValueAt(i,"links").split(";");
                String[] order = tds.getStringValueAt(i,"ordering").split("\\s");
                String[] orderedLinks = new String[links.length];
                for (int k = 0; k < orderedLinks.length; k++)
                    orderedLinks[Integer.parseInt(order[k])-1] = links[k];
                odWeavingMap.get(od).add(new WeavingData(od,(double) tds.getValueAt(i,"percentage"),orderedLinks));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        copyFile(new File(weavingFile), new File(weavingFile.replace(".csv","_" + mode + ".csv")));
    }

    private void copyFile(File sourceFile, File destFile) {

        FileChannel source = null;
        FileChannel destination = null;
        try {
            if(!destFile.exists())
                destFile.createNewFile();
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(source != null)
                try {
                source.close();
                } catch (IOException e) {/*swallow*/}
            if(destination != null)
                try {
                    destination.close();
                } catch (IOException e) {/*swallow*/}
        }
    }

    public Set<String> getInteriorZones() {
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
        return externalZones;
    }

    public Set<String> getWeavingZones() {
        return weavingZones;
    }

    public static int getLinkExternalStationNumber(String externalStationName) {
        //this will work with external stations and regular zones, hence the check for the prefix
        return Integer.parseInt(externalStationName.startsWith(LINK_MATRIX_ENTRY_PREFIX) ? externalStationName.substring(LINK_MATRIX_ENTRY_PREFIX.length()) : externalStationName);
    }

    //holds data for a given od and links>3 in path: need to know the order the links are traversed in the path, so this object'll hold this
    class WeavingData {
        private final String od;
        private final List<String> fromNodeToNodes;
        private final double percentage;
        private final LinkData[] linkDataList;
        private final boolean invalid;

        public WeavingData(String od, double percentage, String ... links) {
            this.od = od;
            this.percentage = percentage;
            linkDataList = new LinkData[links.length];

            boolean first = true;
            boolean in = false;
            boolean error = false;
            String message = "";
            String message2 = "";
            if (!linkData.containsKey(od)) {
                logger.warn("Weaving od not found: " + od);
                invalid = true;
            } else {
                for (int i = 0; i < links.length; i++) {
                    for (LinkData ld : linkData.get(od)) {
                        if (links[i].equals(ld.fromNodeToNode)) {
                            linkDataList[i] = ld;
                            if (first) {
                                first = false;
                                in = ld.getIn();
                            } else {
                                error |= in == ld.getIn(); //same as last - impossible for weaving
                                in = ld.getIn();
                                message += ",";
                                message2 += ",";
                            }
                            message += ld.getIn() ? "IN" : "OUT";
                            message2 += links[i];
                            break;
                        }
                    }
                }

                if (error)
                    logger.error("Invalid weaving path for od " + od + ": [" + message2 + "] is (" + message + ")");
                invalid = error;
            }

            fromNodeToNodes = Arrays.asList(links);
        }
        public String getOd() {
            return od;
        }

        public List<String> getFromNodeToNodes() {
            return fromNodeToNodes;
        }

        public double getPercentage() {
            return percentage;
        }

        public boolean isFirstLinkIn() {
            return linkDataList[0].getIn();
        }

        public LinkData getRepresentativeLinkData(int linkIndex) {
            return linkDataList[linkIndex];
        }

        public boolean isInvalid() {
            return invalid;
        }
    }

    //holds data about a link, including the od's whose paths it is used in (and the percentage of trips using this path)
    class LinkData {
        private final String fromNodeToNode;
        private final Map<String,Double> odPercentages; //map from od name to percentage of trips between o & d using link
        private final boolean in; //is incoming or outgoing - supplied externally
        private final int externalStation; //external station # used in output matrix

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
        }

        public double getOdPercentage(String od) {
            return odPercentages.get(od);
        }


        public boolean getIn() {
            return in;
        }
    }



//    private Map<String,List<LinkData>> linkData = new HashMap<String,List<LinkData>>();
//    private List<String> externalStationList;
//    private Set<String> internalZones = null;
//    private Set<String> externalZones = null;
//    private Set<String> weavingZones = null;

    void reconcileAgainstOtherSelectLinkData(SelectLinkData ... slds) {
        reconcileAgainstOtherSelectLinkData(true,slds);
    }

    private void reconcileAgainstOtherSelectLinkData(boolean recurse, SelectLinkData ... slds) {
        //method: go through each sld and add any missing zones, then copy back (if necessary)
        Set<String> ods = linkData.keySet();
        for (SelectLinkData sld : slds)
            for (String otherSldOd : sld.linkData.keySet())
                if (!ods.contains(otherSldOd)) {
                    linkData.put(otherSldOd,new LinkedList<LinkData>());
                    LinkData ld = sld.linkData.get(otherSldOd).get(0);
                    LinkData ldFake = new LinkData(ld.fromNodeToNode,ld.in,ld.externalStation); //clone, essentially
                    ldFake.addOdPercentage(otherSldOd,0.0); //nothing travels on this placeholder
                    linkData.get(otherSldOd).add(ldFake);
                    String o = getZoneFromLookup(otherSldOd,true);
                    String d = getZoneFromLookup(otherSldOd,false);
                    if (sld.externalZones.contains(o) && !externalZones.contains(o))
                        externalZones.add(o);                      
                    if (sld.externalZones.contains(d) && !externalZones.contains(d))
                        externalZones.add(d);
                    if (sld.internalZones.contains(o) && !internalZones.contains(o))
                        internalZones.add(o);                      
                    if (sld.internalZones.contains(d) && !internalZones.contains(d))
                        internalZones.add(d);
                    //refresh with new key set with added od
                    ods = linkData.keySet();
                }
//        for (String s : newOds.keySet()) System.out.println("Missing od: " + s);
//        System.out.println(newOds.size());
        
        //copy back - if recurse
        if (recurse)
            for (SelectLinkData sld : slds)
                sld.reconcileAgainstOtherSelectLinkData(false,this);

    }
}

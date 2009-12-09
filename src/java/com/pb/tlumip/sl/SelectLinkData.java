package com.pb.tlumip.sl;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author crf <br/>
 *         Started: Dec 1, 2009 4:29:05 PM
 */
public class SelectLinkData {

    public static String formODLookup(String fromZone, String toZone) {
        return fromZone + toZone;
    }

    private Map<String,List<LinkData>> linkData = new HashMap<String,List<LinkData>>();
    private List<String> linkList;

    public SelectLinkData(String linkDataFile, String assignClass) {
        loadLinkData(linkDataFile,assignClass);
    }

    public boolean containsOd(String od) {
        return linkData.containsKey(od);
    }

    public List<LinkData> getDataForOd(String od) {
        return linkData.get(od);
    }

    public List<String> getLinkList() {
        return linkList;
    }

    private void loadLinkData(String linkDataFile,String assignClass) {
        String assignClassFieldName = "ASSIGNCLASS";
        String linkFieldName = "FROMNODETONODE";
        String directionFieldName = "DIRECTION";
        String originFieldName = "FROMZONE";
        String destFieldName = "TOZONE";
        String percentageFieldName = "PERCENT";
        TableDataSet ld;
        try {
            ld = new CSVFileReader().readFile(new File(linkDataFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String,LinkData> pureLinkData = new HashMap<String,LinkData>();

        for (int i = 1; i <= ld.getRowCount(); i++) {
            if (ld.getStringValueAt(i,assignClassFieldName).equals(assignClass))
                continue;
            String fromNodeToNode = ld.getStringValueAt(i,linkFieldName);
            if (!pureLinkData.containsKey(fromNodeToNode))
                pureLinkData.put(fromNodeToNode,new LinkData(fromNodeToNode,ld.getStringValueAt(i,directionFieldName).equalsIgnoreCase("in")));
            String origin = "" + ((int) ld.getValueAt(i,originFieldName));
            String dest = "" + ((int) ld.getValueAt(i,destFieldName));
            pureLinkData.get(fromNodeToNode).addOdPercentage(formODLookup(origin,dest),ld.getValueAt(i,percentageFieldName));
        }

        Set<String> links = new TreeSet<String>();

        for (String s : pureLinkData.keySet()) {
            LinkData ldata = pureLinkData.get(s);
            links.add(ldata.getMatrixEntryName());
            for (String id : ldata.odPercentages.keySet()) {
                if (!linkData.containsKey(id))
                    linkData.put(id,new LinkedList<LinkData>());
                linkData.get(id).add(ldata);
            }
        }

        linkList = new LinkedList<String>(links);
    }

    class LinkData {
        private final String fromNodeToNode;
        private final Map<String,Double> odPercentages;
        private final boolean in;

        public LinkData(String fromNodeToNode, boolean in) {
            this.fromNodeToNode = fromNodeToNode;
            this.in = in;
            odPercentages = new HashMap<String,Double>();
        }

        public void addOdPercentage(String od, double percentage) {
            odPercentages.put(od,percentage);
        }

        public String getMatrixEntryName() {
            return "Link_" + fromNodeToNode;
        }

        public double getOdPercentage(String od) {
            return odPercentages.get(od);
        }

    }
}

package com.pb.tlumip.sl;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.*;
import com.pb.tlumip.sl.SelectLinkData.LinkData;
import com.pb.tlumip.ts.DemandHandler;

import java.io.*;
import java.util.*;

/**
 * @author crf <br/>
 *         Started: Dec 1, 2009 1:34:52 PM
 */
public class TripSynthesizer {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TripSynthesizer.class);

    private OdMatrixGroup.OdMatrixGroupCollection autoMatrices;
    private OdMatrixGroup.OdMatrixGroupCollection truckMatrices;
    private final Map<Integer,SelectLinkData> autoSelectLinkData;
    private final Map<Integer,SelectLinkData> truckSelectLinkData;
    private final ResourceBundle rb;
    private final boolean balanceOn;

    private final TripFile sdtTripFile;
    private final TripFile ldtTripFile;
    private final TripFile ctTripFile;
    private final TripFile etTripFile;
    private final TripFile ldtPersonStub;

    //private final double[] factors; - not needed; want vehicles, not auto-equivalent trips

    private String lastSdtTripHhId;
    private String lastSdtTripTourId;
    private String lastSdtTripPersonId;
    private String lastSdtTripType;

    public TripSynthesizer(ResourceBundle rb, Map<Integer,SelectLinkData> autoSelectLinkData, Map<Integer,SelectLinkData> truckSelectLinkData, TripClassifier sdtClassifier, TripClassifier ldtClassifier, TripClassifier ctClassifier, TripClassifier etClassifier) {
        logger.info("Initializing SL Synthesizer");
        this.rb = rb;
        this.autoSelectLinkData = autoSelectLinkData;
        this.truckSelectLinkData = truckSelectLinkData;
//        factors = formTripFactorLookup(rb);

        sdtTripFile = new SDTTripFile(rb,sdtClassifier);
        ldtTripFile = new LDTTripFile(rb,ldtClassifier);
        ctTripFile = new CTTripFile(rb,ctClassifier);
        etTripFile = new ETTripFile(rb,etClassifier);
        ldtPersonStub = new LDTPersonTripFileStub(rb,ldtClassifier);

        balanceOn = false; //indicates if balancing on; if not, then only origin scaling factor will be used for autobalancing
                           //NOTE: I think balancing will not work, because there is not a consistent way to deal with weaving
    }

    OdMatrixGroup.OdMatrixGroupCollection getSynthesizedMatrices(boolean auto) {
        return auto ? autoMatrices : truckMatrices;
    }

    private String buildSelectLinkTripFile(TripFile tf) {
        int i = tf.path.lastIndexOf(".");
        return tf.path.substring(0,i) + "_select_link" + tf.path.substring(i);
    }

    public void synthesizeTripsAndAppendToTripFile(Set<Integer> internalZones, String summaryFile) {
    	//write summary file
    	    	
    	TableDataSet slSummary;
        try {
            slSummary = new CSVFileReader().readFile(new File(summaryFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }    	
    	
        //Integer rowCount = slSummary.getRowCount();
        Map<String,List<Double>> slSummaryData = new HashMap<String,List<Double>>();

        for (int i = 1; i <= slSummary.getRowCount(); i++) {
        	String assignClass = slSummary.getStringValueAt(i, "ASSIGNCLASS");
        	String stationNumber = slSummary.getStringValueAt(i, "STATIONNUMBER");
        	String direction = slSummary.getStringValueAt(i, "DIRECTION");
        	Double autoOD = (double) slSummary.getValueAt(i, "AUTO_SL_OD");
        	Double truckOD = (double) slSummary.getValueAt(i, "TRUCK_SL_OD");
        	String strKey = assignClass + "," + stationNumber + "," + direction;
        	
        	if (!slSummaryData.containsKey(strKey))
        		slSummaryData.put(strKey, new ArrayList<Double>());
        	
        	slSummaryData.get(strKey).add(autoOD);
        	slSummaryData.get(strKey).add(truckOD);
        	slSummaryData.get(strKey).add(0.0); //2-SDT trips
        	slSummaryData.get(strKey).add(0.0); //3-LDT person trips
        	slSummaryData.get(strKey).add(0.0); //4-LDT vehicle trips
        	slSummaryData.get(strKey).add(0.0); //5-CT trips
        	slSummaryData.get(strKey).add(0.0); //6-ET trips
        	//logger.info(strKey);
        }
        
        logger.info("Synthesizing SDT");
        synthesizeTripsAndAppendToTripFile(sdtTripFile,true,buildSelectLinkTripFile(sdtTripFile),internalZones,slSummaryData,2);
        logger.info("Synthesizing LDT Vehicle");
        synthesizeTripsAndAppendToTripFile(ldtTripFile,true,buildSelectLinkTripFile(ldtTripFile),internalZones,slSummaryData,4);
        logger.info("Synthesizing LDT Person");
        //TripFile ldtPersonStub = new LDTPersonTripFileStub(rb,null);
        synthesizeTripsAndAppendToTripFile(ldtPersonStub,true,buildSelectLinkTripFile(ldtPersonStub),internalZones,slSummaryData,3);
        logger.info("Synthesizing CT");
        synthesizeTripsAndAppendToTripFile(ctTripFile,false,buildSelectLinkTripFile(ctTripFile),internalZones,slSummaryData,5);
        logger.info("Synthesizing ET");
        synthesizeTripsAndAppendToTripFile(etTripFile,false,buildSelectLinkTripFile(etTripFile),internalZones,slSummaryData,6);
        
        //close summary file
        //writer_summary.close();
        
    	String newHeader = "ASSIGNCLASS,STATIONNUMBER,DIRECTION,AUTO_SL_OD,TRUCK_SL_OD,SDT_PESON_TRIP,LDT_PERSON_TRIP,LDT_VEHICLE_TRIP,CT_TRIP,ET_TRIP";
    	
        PrintWriter writer = null;
        String newSummaryFile = summaryFile;
        //String newSummaryFile = "E:/Projects/Clients/ODOT/Model/tlumip_v26/root/scenario/outputs/t20/sl_summary_test.csv";
		try {
			writer = new PrintWriter(newSummaryFile);
			writer.println(newHeader); 
	        for (String key: slSummaryData.keySet()){
	        	String line = key;
	        	for (Double value: slSummaryData.get(key)){
	        		line = line + "," + value.intValue();
	        	}
	        	writer.write(line + "\n");
	        	//writer.write(key + "," + slSummaryData.get(key) + "\n");
	        }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        writer.flush();
        writer.close();
        
    }

    protected List<File> getSelectLinkTripFiles() {
        List<File> files = new LinkedList<File>();
        files.add(new File(buildSelectLinkTripFile(sdtTripFile)));
        files.add(new File(buildSelectLinkTripFile(ldtTripFile)));
        files.add(new File(buildSelectLinkTripFile(new LDTPersonTripFileStub(rb,null))));
        files.add(new File(buildSelectLinkTripFile(ctTripFile)));
        files.add(new File(buildSelectLinkTripFile(etTripFile)));
        return files;
    }

    void synthesizeTrips() {
        test(true);
        test(false);
        for (String link : f.keySet())
            System.out.println(link + ": " + f.get(link));
//        System.exit(0);

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
        Map<Integer,SelectLinkData> sld = auto ? autoSelectLinkData : truckSelectLinkData;
        Set<SelectLinkData> uniqueSld = new HashSet<>();
        for (SelectLinkData slData : sld.values())
            uniqueSld.add(slData);
        //create external numbers and mapping to internal numbers
        Set<String> extNums = new LinkedHashSet<String>(formBaseExternalNumbers());
        for (SelectLinkData slData : uniqueSld)
            for (String s : slData.getExternalStationList())
                extNums.add(s);
        Map<String,Integer> zoneMatrixMap = new HashMap<String, Integer>();
        Set<String> exteriorZones = new HashSet<>();
        for (SelectLinkData slData : uniqueSld)
            exteriorZones.addAll(slData.getExteriorZones());
        for (String zone : exteriorZones)
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
            if (!taz.getStringValueAt(i,"MPO").equals("WM")) //todo: don't hardcode these two fields
                orderedTazs.add("" + ((int) taz.getValueAt(i,"AZONE")));
        return orderedTazs;
    }

    Map<String,Double> f = new HashMap<String,Double>();
    private void test(boolean autoClass) {
        Map<Integer,SelectLinkData> sld = autoClass ? autoSelectLinkData : truckSelectLinkData;
        if (autoClass) {
        testm(sld.get(0),ZipMatrixReader.readMatrix(new File(rb.getString("sl.current.directory"),"demand_matrix_a_ampeak.zmx"),""));
        testm(sld.get(2),ZipMatrixReader.readMatrix(new File(rb.getString("sl.current.directory"),"demand_matrix_a_pmpeak.zmx"),""));
        testm(sld.get(1),ZipMatrixReader.readMatrix(new File(rb.getString("sl.current.directory"),"demand_matrix_a_mdoffpeak.zmx"),""));
        testm(sld.get(0),ZipMatrixReader.readMatrix(new File(rb.getString("sl.current.directory"),"demand_matrix_a_ntoffpeak.zmx"),""));
        } else {
        testm(sld.get(0),ZipMatrixReader.readMatrix(new File(rb.getString("sl.current.directory"),"demand_matrix_d_ampeak.zmx"),""));
        testm(sld.get(2),ZipMatrixReader.readMatrix(new File(rb.getString("sl.current.directory"),"demand_matrix_d_pmpeak.zmx"),""));
        testm(sld.get(1),ZipMatrixReader.readMatrix(new File(rb.getString("sl.current.directory"),"demand_matrix_d_mdoffpeak.zmx"),""));
        testm(sld.get(0),ZipMatrixReader.readMatrix(new File(rb.getString("sl.current.directory"),"demand_matrix_d_ntoffpeak.zmx"),""));
        }
    }
    private void testm(SelectLinkData sld,Matrix m) {
//        StringBuilder sb = new StringBuilder();
//        try{
//        FileReader reader = new FileReader(new File(rb.getString("sl.current.directory"),"_external row numbers"));
//        char[] buffer = new char[1024];
//        int amount;
//
//        while ((amount = reader.read(buffer)) > -1) {
//            sb.append(buffer,0,amount);
//        }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        String[] sexternals = sb.toString().trim().split(",");
//        int[] externals = new int[sexternals.length+1];
//        for (int i = 0; i < sexternals.length; i++)
//            externals[i+1] = Integer.parseInt(sexternals[i]);


        int[] extNum = m.getExternalNumbers();

        for (int i = 1; i < extNum.length; i++) {
            for (int j = 1; j < extNum.length; j++) {
                String od = "" + extNum[i] + " " + extNum[j];
                if (sld.containsOd(od)) {
                    for (SelectLinkData.LinkData ld : sld.getDataForOd(od)) {
                        String e = ld.getMatrixEntryName() + (ld.getIn() ? "_in" : "_out");
                        if (!f.containsKey(e))
                            f.put(e,0.0);
//                        f.put(e,f.get(e)+m.getValueAt(i,j));
                        f.put(e,f.get(e)+m.getValueAt(extNum[i],extNum[j]));
//                        if (e.equals("_17_in") && m.getValueAt(extNum[i],extNum[j]) > 0.0) {
//                            System.out.println("17: " + extNum[i] + " " + extNum[j] + " - " + m.getValueAt(extNum[i],extNum[j]));
//                        }
                    }
                }
            }
        }
    }

    private void synthesizeTrips(TripFile tripFile, boolean autoClass) {
        Map<Integer,SelectLinkData> sld = autoClass ? autoSelectLinkData : truckSelectLinkData;
        Set<SelectLinkData> uniqueSld = new HashSet<>();
        for (SelectLinkData slData : sld.values())
            uniqueSld.add(slData);

        OdMatrixGroup.OdMatrixGroupCollection omc = autoClass ? autoMatrices : truckMatrices;
        Map<String,Integer> zoneMatrixMap = omc.getTemplate().getZoneMatrixMap();

        double tripsLostToWeaving = 0.0; //trips that can't be used because od has a weaving path

        Set<String> exteriorZones = new HashSet<>();
        for (SelectLinkData slData : uniqueSld)
            exteriorZones.addAll(slData.getExteriorZones());
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

        String traceZone1 = "2";
        String traceZone2 = "3";
        String slOd = "_17";
        double[] slOdTraceIn = {0,0,0};
        double[] slOdTraceOut = {0,0,0};
        double[] tripsTrace = {0,0,0,0};
        double[] tripsCountedTrace = {0,0,0,0};
        boolean getTotalDemand = true;
        double[] totalDemand = {0,0,0,0};

        //initialize sdt info, even if not using them
        lastSdtTripHhId = lastSdtTripTourId = lastSdtTripType = lastSdtTripPersonId = "";

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
            logger.info("origin dest fields " + originId + " " + destId);

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
                if (origin.equals(traceZone1) && dest.equals(traceZone2)) {
//                    logger.info(Arrays.toString(tripFileLine));
                    tripsTrace[tripFile.getTimePeriodFromRecord(tripFileLine)] += tripFile.getTripFromRecord(tripFileLine);//*factors[tripFile.getModeIdFromRecord(tripFileLine)];
                }
                String od = SelectLinkData.formODLookup(origin,dest);
                int period = tripFile.getTimePeriodFromRecord(tripFileLine);
                SelectLinkData slData = sld.get(period);

                tripFile.classifier.setExtraData(slData,tripFileLine);

                if (getTotalDemand) {
                    totalDemand[tripFile.getTimePeriodFromRecord(tripFileLine)] += tripFile.getTripFromRecord(tripFileLine);//*factors[tripFile.getModeIdFromRecord(tripFileLine)];
                }

                //do this here to update last trip
                String omcType = (tripFile.getTourTypeFromRecord(tripFileLine) + "_" +
                                  tripFile.getOriginTripFromRecord(tripFileLine) + "_" +
                                  tripFile.getDestTripFromRecord(tripFileLine)).toLowerCase().replace(" ","_");
//                if (tripFileLine[0].equals("2137")) System.out.println(Arrays.toString(tripFileLine));
                lastSdtTripType = tripFile.getTripTypeFromRecord(tripFileLine); //for next trip
                if (!slData.containsOd(od))
                    continue;

                //trips = trips from record * scaling factor from model * exogenous scaling factor
                double trips = tripFile.getTripFromRecord(tripFileLine);//*factors[tripFile.getModeIdFromRecord(tripFileLine)];
                if (trips == 0.0)
                    continue;

                tripCounter += trips;
                //OdMatrixGroup.OdMarginalMatrixGroup omg = (OdMatrixGroup.OdMarginalMatrixGroup) omc.get(tripFile.getNormalizedTripTypeFromRecord(tripFileLine));
                if (omcType.equals("gradeschool_work_based_college") || omcType.equals("recreate_work_based_other"))
                    logger.info(Arrays.toString(tripFileLine));

                OdMatrixGroup.OdMarginalMatrixGroup omg = (OdMatrixGroup.OdMarginalMatrixGroup) omc.get(omcType);

                if (origin.equals(traceZone1) && dest.equals(traceZone2)) tripsCountedTrace[period] += trips;

                Matrix ofInterest = omg.getMatrix(period);
                PATripType tt = tripFile.getPATripTypeFromRecord(tripFileLine);
                Matrix paOfInterest = omc.get(tt.toString()).getMatrix(period);
                boolean endsAtHome = tripFile.tripEndsAtHome(tripFileLine);
                ColumnVector originMarginals = omg.getOriginMarginals(period);
                RowVector destMarginals = omg.getDestinationMarginals(period);
                int mo = zoneMatrixMap.containsKey(origin) ? zoneMatrixMap.get(origin) : -1;
                int md = zoneMatrixMap.containsKey(dest) ? zoneMatrixMap.get(dest) : -1;

                // exogenous scaling factors for o/d marginals
                double originFactor = tripFile.classifier.getOriginFactor(slData,tripFileLine);
                //double destinationFactor = tripFile.classifier.getDestinationFactor(tripFileLine);
                //if not balancing, dest factor becomes origin factor
                double destinationFactor = balanceOn ? tripFile.classifier.getDestinationFactor(slData,tripFileLine) : originFactor;
                //next two numbers say how to factor the "link zone" trips in the marginals
//                double linkOriginFactor = 1.0;
//                double linkDestinationFactor = 1.0;

                if (slData.getWeavingZones().contains(od)) {
                    List<SelectLinkData.WeavingData> wds = slData.getWeavingData(od);
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
                        Matrix paOfInterestTemp;
                        for (String link : links) {
                            SelectLinkData.LinkData ld = wd.getRepresentativeLinkData(lcounter);
                            int lid = zoneMatrixMap.get(ld.getMatrixEntryName());
                            if (!skip) {
                                ofInterest.setValueAt(lastId,lid,(float) (ofInterest.getValueAt(lastId,lid)+linkTrips));
                                if (tt == PATripType.HBW || tt == PATripType.HBO) { //if coming in, then first trip is home-based, if going out, last one is
                                    if ((endsAtHome && lcounter >= links.size()-2) ||
                                        (!endsAtHome && lcounter < 2))
                                        paOfInterestTemp = paOfInterest;
                                    else  //otherwise trip is non-home-based
                                        paOfInterestTemp = omc.get(PATripType.NHB.toString()).getMatrix(period);

                                } else {
                                    paOfInterestTemp = paOfInterest;
                                }
                                paOfInterestTemp.setValueAt(lastId,lid,(float) (paOfInterestTemp.getValueAt(lastId,lid)+linkTrips));
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
                List<SelectLinkData.LinkData> linkData = slData.getDataForOd(od);
                for (SelectLinkData.LinkData ld : linkData) {
                    int lid = zoneMatrixMap.get(ld.getMatrixEntryName());
                    if (slOd.equals(ld.getMatrixEntryName())) {
                        if (ld.getIn()) {
                            slOdTraceIn[0] += trips;
                            slOdTraceIn[1] += ld.getOdPercentage(od)*trips;
                            slOdTraceIn[2] += ld.getOdPercentage(od)*trips * (balanceOn ? 1.0 : originFactor);
                        } else {
                            slOdTraceOut[0] += trips;
                            slOdTraceOut[1] += ld.getOdPercentage(od)*trips;
                            slOdTraceOut[2] += ld.getOdPercentage(od)*trips * (balanceOn ? 1.0 : originFactor);
                        }
                    }
                    double linkTrips = ld.getOdPercentage(od)*trips * (balanceOn ? 1.0 : originFactor);
                    //marginals don't matter if not balancing, so ignore them
                    if (ii) {
                        if (ld.getIn()) {
                            ofInterest.setValueAt(lid,md,(float) (ofInterest.getValueAt(lid,md)+linkTrips));
                            paOfInterest.setValueAt(lid,md,(float) (paOfInterest.getValueAt(lid,md)+linkTrips));
                            if (balanceOn) {
                                originMarginals.setValueAt(lid,(float) (originMarginals.getValueAt(lid)+linkTrips*originFactor));
                                destMarginals.setValueAt(md,(float) (destMarginals.getValueAt(md)+linkTrips*destinationFactor));
                            }
                        } else {
                            ofInterest.setValueAt(mo,lid,(float) (ofInterest.getValueAt(mo,lid)+linkTrips));
                            paOfInterest.setValueAt(mo,lid,(float) (paOfInterest.getValueAt(mo,lid)+linkTrips));
                            if (balanceOn) {
                                originMarginals.setValueAt(mo,(float) (originMarginals.getValueAt(mo)+linkTrips*originFactor));
                                destMarginals.setValueAt(lid,(float) (destMarginals.getValueAt(lid)+linkTrips*destinationFactor));
                            }
                        }
                    } else {
                        if (exteriorZones.contains(origin) && !exteriorZones.contains(dest)) {
                            ofInterest.setValueAt(lid,md,(float) (ofInterest.getValueAt(lid,md)+linkTrips));
                            paOfInterest.setValueAt(lid,md,(float) (paOfInterest.getValueAt(lid,md)+linkTrips));
                            if (balanceOn) {
                                originMarginals.setValueAt(lid,(float) (originMarginals.getValueAt(lid)+linkTrips*originFactor));
                                destMarginals.setValueAt(md,(float) (destMarginals.getValueAt(md)+linkTrips*destinationFactor));
                            }
                        } else if (!exteriorZones.contains(origin) && exteriorZones.contains(dest)) {
                            ofInterest.setValueAt(mo,lid,(float) (ofInterest.getValueAt(mo,lid)+linkTrips));
                            paOfInterest.setValueAt(mo,lid,(float) (paOfInterest.getValueAt(mo,lid)+linkTrips));
                            if (balanceOn) {
                                originMarginals.setValueAt(mo,(float) (originMarginals.getValueAt(mo)+linkTrips*originFactor));
                                destMarginals.setValueAt(lid,(float) (destMarginals.getValueAt(lid)+linkTrips*destinationFactor));
                            }
                        }
                    }
                    if (ee) {
                        for (Matrix oi : new Matrix[] {ofInterest,paOfInterest}) {
                            //put data into tracker
                            if (!eeTracker.containsKey(oi)) {
                                eeTracker.put(oi,new HashMap<String,Map<String,Map<String,Double>>>());
                                if (oi == ofInterest)
                                    marginalMap.put(oi,new Matrix[] {originMarginals,destMarginals});
                            }
                            Map<String,Map<String,Map<String,Double>>> eeSub = eeTracker.get(oi);
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
                        double trips = eeSub.get(od).get("out").get(linkOut)*ip*totalTrips*tripFile.classifier.getExternalExternalFactor(sld.get(0),linkIn,linkOut); //sld not used here for now, so just pick ampeak
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
        logger.info("Trips trace for " + traceZone1 + "-" + traceZone2 + ": " + Arrays.toString(tripsTrace));
        logger.info("Trips counted trace for " + traceZone1 + "-" + traceZone2 + ": " + Arrays.toString(tripsCountedTrace));
        logger.info("Link " + slOd + " (in) trace: " + Arrays.toString(slOdTraceIn));
        logger.info("Link " + slOd + " (out) trace: " + Arrays.toString(slOdTraceOut));
        if (getTotalDemand)
            logger.info("Total trip demand: " + Arrays.toString(totalDemand));
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

        private String lastTripType;
        private long lastTourId = -1;

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
        abstract PATripType getPATripTypeFromRecord(String ... data);
        abstract boolean tripEndsAtHome(String ... data);
        abstract int getTourHome(String ... data);
        abstract long getTourId(String ... data);

        String getLastTripType() {
            return lastTripType;
        }

        void setLastTripType(String ... data) {
            long tourId = getTourId(data);
            if (tourId == lastTourId) {
                lastTripType = getTripTypeFromRecord(data);
            } else {
                lastTripType = "";
                lastTourId = tourId;
            }
        }

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

        public String getTourTypeFromRecord(String ... data) {
            return "";
        }

        public String getOriginTripFromRecord(String ... data) {
            return "";
        }

        public String getDestTripFromRecord(String ... data) {
            return "";
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
//            logger.warn("Unknown sdt mode for trip: " + mode);
            return 0;
        }

        int getTripTimeFromRecord(String ... data) {
            return (int) Double.parseDouble(data[12]);
        }

        String getTripTypeFromRecord(String ... data) {
            return data[14];
        }

        PATripType getPATripTypeFromRecord(String ... data) {
            int tourSegment = Integer.parseInt(data[6]);
            String tripPurpose = getTripTypeFromRecord(data);
            String tourPurpose = getTourTypeFromRecord(data);
            if (tourPurpose.equalsIgnoreCase("WORK") &&
                    (tourSegment == 0 || tripPurpose.equalsIgnoreCase("HOME")))
                return PATripType.HBW;
            //else if (!tourPurpose.equalsIgnoreCase("WORK_BASED") &&
            else if (data[4].equals("1") &&
                    (tourSegment == 0 || tripPurpose.equalsIgnoreCase("HOME")))
                return PATripType.HBO;
            return PATripType.NHB;
        }

        boolean tripEndsAtHome(String ... data) {
            return getTripTypeFromRecord(data).equalsIgnoreCase("HOME");
        }

        @Override
        int getTourHome(String... data) {
            return TripClassifier.getOriginZone(Integer.parseInt(data[0]),rb);
        }

        long getTourId(String ... data) {
            return ((long) Integer.parseInt(data[0])) * 1000 + Integer.parseInt(data[1])*10 + Integer.parseInt(data[3]);
        }

        private void updateSdtTripInfo(String ... data) {
            if (!lastSdtTripHhId.equals(data[0]) || !lastSdtTripPersonId.equals(data[1]) || !lastSdtTripTourId.equals(data[3])) {//new tour or hh
                //lastSdtTripType = data[4].equals("1") && getTourTypeFromRecord(data).equals("WORK_BASED") ? "WORK_BASED" : "HOME";
                lastSdtTripType = data[4].equals("1") ? getTourTypeFromRecord(data) : "HOME";
                lastSdtTripHhId = data[0];
                lastSdtTripPersonId = data[1];
                lastSdtTripTourId = data[3];
            }
        }

        public String getTourTypeFromRecord(String ... data) {
            return data[5];
        }

        public String getOriginTripFromRecord(String ... data) {
            updateSdtTripInfo(data);
            return lastSdtTripType;
        }

        public String getDestTripFromRecord(String ... data) {
            return getTripTypeFromRecord(data);
        }
    }

    private class LDTPersonTripFileStub extends TripFile {
        private final String daId;
        private final String sr2Id;
        private final String sr3Id;
        private final double daTrip = 1.0;
        private final double sr2Trip = 0.5;
        private final double sr3Trip = 1/ DemandHandler.AVERAGE_SR3P_AUTO_OCCUPANCY;
        private final ResourceBundle rb;
        
    	private LDTPersonTripFileStub(ResourceBundle rb, TripClassifier classifier) {
            //0        1         2       3        4          5              6      7           8         9               10           11         12
            //hhID	memberID	tourID	income	tourPurpose	tourMode	origin	destination	distance	time	tripStartTime	tripPurpose	tripMode	vehicleTrip
            //hhID  memberID    tourID income   tourPurpose tourMode    origin  destination distance    time    tripStartTime,tripPurpose,tripMode,vehicleTrip
            super(rb.getString("ldt.person.trips"),"origin","destination",classifier,rb);
            this.rb = rb;
            this.daId = rb.getString("driveAlone.identifier");
            this.sr2Id = rb.getString("sharedRide2.identifier");
            this.sr3Id = rb.getString("sharedRide3p.identifier");
        }

        double getTripFromRecord(String ... data) {
            String mode = data[12];
            if (mode.equalsIgnoreCase(daId)) return daTrip;
            else if (mode.equalsIgnoreCase(sr2Id)) return sr2Trip;
            else if (mode.equalsIgnoreCase(sr3Id)) return sr3Trip;
//            logger.warn("Unknown sdt mode for trip: " + mode);
            return 0;
        }

        int getTripTimeFromRecord(String ... data) {
            return (int) Double.parseDouble(data[10]);
        }

        String getTripTypeFromRecord(String ... data) {
            return data[11];
        }

        PATripType getPATripTypeFromRecord(String ... data) {
            String homeTaz = "" + TripClassifier.getOriginZone(Integer.parseInt(data[0]),rb);
            String workType = "WORKRELATED";
            if (data[6].equals(homeTaz) || data[7].equals(homeTaz))
                return data[11].equals(workType) ? PATripType.HBW : PATripType.HBO;
            return PATripType.NHB;
        }

        boolean tripEndsAtHome(String ... data) {
            String homeTaz = "" + TripClassifier.getOriginZone(Integer.parseInt(data[0]),rb);
            return data[7].equals(homeTaz);
        }

        public String getTourTypeFromRecord(String ... data) {
            return data[4];
        }

        public String getOriginTripFromRecord(String ... data) {
            return data[6].equals("" + TripClassifier.getOriginZone(Integer.parseInt(data[0]),rb)) ? "HOME" : getTripTypeFromRecord(data);
        }

        public String getDestTripFromRecord(String ... data) {
            return data[7].equals("" + TripClassifier.getOriginZone(Integer.parseInt(data[0]),rb)) ? "HOME" : getTripTypeFromRecord(data);
        }

        @Override
        int getTourHome(String... data) {
            return TripClassifier.getOriginZone(Integer.parseInt(data[0]),rb);
        }

        long getTourId(String ... data) {
            return ((long) Integer.parseInt(data[0])) * 1000 + Integer.parseInt(data[1])*10 + Integer.parseInt(data[2]);
        }
    }

    private class LDTTripFile extends TripFile {
        private final String daId;
        private final String sr2Id;
        private final String sr3Id;
        private final double daTrip = 1.0;
        private final double sr2Trip = 0.5;
        private final double sr3Trip = 1/ DemandHandler.AVERAGE_SR3P_AUTO_OCCUPANCY;
        private final ResourceBundle rb;
        private LDTTripFile(ResourceBundle rb, TripClassifier classifier) {
            //0        1         2       3        4          5              6      7           8       9               10           11         12
            //hhID	memberID	tourID	income	tourPurpose	tourMode	origin	destination	distance	time	tripStartTime	tripPurpose	tripMode	vehicleTrip
            super(rb.getString("ldt.vehicle.trips"),"origin","destination",classifier,rb);
            this.rb = rb;
            this.daId = rb.getString("driveAlone.identifier");
            this.sr2Id = rb.getString("sharedRide2.identifier");
            this.sr3Id = rb.getString("sharedRide3p.identifier");
        }

        double getTripFromRecord(String ... data) {
            String mode = data[12];
            if (mode.equalsIgnoreCase(daId)) return daTrip;
            else if (mode.equalsIgnoreCase(sr2Id)) return sr2Trip;
            else if (mode.equalsIgnoreCase(sr3Id)) return sr3Trip;
//            logger.warn("Unknown sdt mode for trip: " + mode);
            return 0;
        }

        int getTripTimeFromRecord(String ... data) {
            return (int) Double.parseDouble(data[10]);
        }

        String getTripTypeFromRecord(String ... data) {
            return data[11];
        }

        PATripType getPATripTypeFromRecord(String ... data) {
            String homeTaz = "" + TripClassifier.getOriginZone(Integer.parseInt(data[0]),rb);
            String workType = "WORKRELATED";
            if (data[6].equals(homeTaz) || data[7].equals(homeTaz))
                return data[11].equals(workType) ? PATripType.HBW : PATripType.HBO;
            return PATripType.NHB;
        }

        boolean tripEndsAtHome(String ... data) {
            String homeTaz = "" + TripClassifier.getOriginZone(Integer.parseInt(data[0]),rb);
            return data[7].equals(homeTaz);
        }

        public String getTourTypeFromRecord(String ... data) {
            return data[4];
        }

        public String getOriginTripFromRecord(String ... data) {
            return data[6].equals("" + TripClassifier.getOriginZone(Integer.parseInt(data[0]),rb)) ? "HOME" : getTripTypeFromRecord(data);
        }

        public String getDestTripFromRecord(String ... data) {
            return data[7].equals("" + TripClassifier.getOriginZone(Integer.parseInt(data[0]),rb)) ? "HOME" : getTripTypeFromRecord(data);
        }

        @Override
        int getTourHome(String... data) {
            return TripClassifier.getOriginZone(Integer.parseInt(data[0]),rb);
        }

        long getTourId(String ... data) {
            return ((long) Integer.parseInt(data[0])) * 1000 + Integer.parseInt(data[1])*10 + Integer.parseInt(data[2]);
        }
    }

    private class CTTripFile extends TripFile {
        private int currentTourOrigin = -1;
        private int lastTruck = -1;
        private CTTripFile(ResourceBundle rb, TripClassifier classifier) {
            //0          1                   2       3           4          5              6              7     8        9               10        11         12
            //origin	tripStartTime	duration	destination	tourMode	tripMode	tripFactor	truckID	truckType	carrierType	commodity	weight	distance
            //old above, new below
            //0          1                   2       3           4              5       6         7          8           9
            //origin	tripStartTime	destination	tourMode	tripMode	truckID	truckType	carrierType	commodity	weight
            super(rb.getString("ct.truck.trips"),"origin","destination",classifier,rb);
        }

        void updateCurrentTourOrigin(String ... data) {
            int truckId = Integer.parseInt(data[5]);
            if (lastTruck != truckId) {
                lastTruck = truckId;
                currentTourOrigin = Integer.parseInt(data[0]);
            }
        }

        long getTourId(String ... data) {
            return Integer.parseInt(data[5]);
        }

        @Override
        int getTourHome(String... data) {
            return currentTourOrigin;
        }

        int getTripTimeFromRecord(String ... data) {
            return (int) Double.parseDouble(data[1]);
        }

        public int getModeIdFromRecord(String ... data) {
            //return Integer.parseInt(data[8].substring(3));
            return Integer.parseInt(data[6].substring(3));
        }

        String getTripTypeFromRecord(String ... data) {
            return "";
        }

        PATripType getPATripTypeFromRecord(String ... data) {
            return PATripType.NHB;
        }

        boolean tripEndsAtHome(String ... data) {
            return false;
        }
    }

    private class ETTripFile extends TripFile {
        private ETTripFile(ResourceBundle rb, TripClassifier classifier) {
            //0          1                   2       3           4
            //origin	destination	tripStartTime	truckClass	truckVolume
            super(rb.getString("et.truck.trips"),"origin","destination",classifier,rb);
        }

        long getTourId(String ... data) {
            return -1;
        }

        @Override
        int getTourHome(String... data) {
            return Integer.parseInt(data[0]);
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

        PATripType getPATripTypeFromRecord(String ... data) {
            return PATripType.NHB;
        }

        boolean tripEndsAtHome(String ... data) {
            return false;
        }
    }

    private enum PATripType {
        HBW,HBO,NHB
    }

    private void synthesizeTripsAndAppendToTripFile(TripFile tripFile, boolean autoClass, String newFile, Set<Integer> internalZones, Map<String, List<Double>> slSummaryData, Integer fieldIndex) {
        Map<Integer,SelectLinkData> sld = autoClass ? autoSelectLinkData : truckSelectLinkData;
        Set<SelectLinkData> uniqueSld = new HashSet<>();
        for (SelectLinkData slData : sld.values())
            uniqueSld.add(slData);
        //create external numbers and mapping to internal numbers
        Set<String> extNums = new LinkedHashSet<String>(formBaseExternalNumbers());
        for (SelectLinkData slData : uniqueSld)
            for (String s : slData.getExternalStationList())
                extNums.add(s);
        Map<String,Integer> zoneMatrixMap = new HashMap<String, Integer>();
        Set<String> exteriorZones = new HashSet<>();
        for (SelectLinkData slData : uniqueSld)
            exteriorZones.addAll(slData.getExteriorZones());
        for (String zone : exteriorZones)
            if (!extNums.remove(zone))
                System.out.println("Couldn't remove: " + zone);
        int counter = 1;
        for (String s : extNums)
            zoneMatrixMap.put(s,counter++);
        Map<Integer,String> reverseZoneMatrixMap = new HashMap<Integer, String>();
        for (String s : zoneMatrixMap.keySet())
            reverseZoneMatrixMap.put(zoneMatrixMap.get(s),s);



        boolean ctTrips = tripFile instanceof CTTripFile;
        double tripsLostToWeaving = 0.0; //trips that can't be used because od has a weaving path
        int originId = -1;
        int destId = -1;
        double tripCounter = 0;
        double eeTripCounter = 0;
        double iiTripCounter = 0;
        double ieTripCounter = 0;
        double eiTripCounter = 0;
        //double[] tripSmmary = {0,0,0,0};
        //Map<String, Integer> tripSummary = new HashMap<String, Integer>();
        //String keySummary = assignClass + stationNum + direction;
        
        //set to write trips to log file
        int traceOrigin = -1;
        int traceDest = -1;
        if (rb.containsKey("sl.cttrips.trace.origin")) {
        	traceOrigin = Integer.parseInt(rb.getString("sl.cttrips.trace.origin"));
        }
        if (rb.containsKey("sl.cttrips.trace.destination")) {
        	traceDest = Integer.parseInt(rb.getString("sl.cttrips.trace.destination"));
        }
        
        PrintWriter writer = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(tripFile.path));
            String line = reader.readLine();
            writer = new PrintWriter(newFile);

            //read header
            String[] header;
            counter = 0;
            header = line.trim().split(",");
            for (String h : header) {
                if (h.equals(tripFile.originField))
                    originId = counter;
                else if (h.equals(tripFile.destField))
                    destId = counter;
                counter++;
            }
            logger.info("origin dest fields " + originId + " " + destId);

            String newHeader = line.trim() + ",EXTERNAL_ZONE_ORIGIN,EXTERNAL_ZONE_DESTINATION,SELECT_LINK_PERCENT,HOME_ZONE,FROM_TRIP_TYPE";
            writer.println(newHeader);
            
            //for summary
            String tod_string = null;
            Integer station = null;
            boolean direction = true;
            String dir_string = null;
            String strKey = null;
            Double totalTrips = null;

            //read trips
            counter = 0;
            while ((line = reader.readLine()) != null) {
                if (++counter % 100000 == 0 && counter < 1000000)
                    logger.info("\tProcessed " + counter + " Trips.");
                if (counter % 1000000 == 0)
                    logger.info("\tProcessed " + counter + " Trips.");

                String[] tripFileLine = line.trim().split(",");
                if (ctTrips)
                    ((CTTripFile) tripFile).updateCurrentTourOrigin(tripFileLine);
                String origin = tripFileLine[originId];
                String dest = tripFileLine[destId];
                SelectLinkData slData = sld.get(tripFile.getTimePeriodFromRecord(tripFileLine));                
                String od = SelectLinkData.formODLookup(origin,dest);
                String lastTripType = tripFile.getLastTripType();
                tripFile.setLastTripType(tripFileLine);
                
                //for summary file
                Integer tod = tripFile.getTimePeriodFromRecord(tripFileLine);
                
                if (tod==0)
                	tod_string = "peak";
                else if (tod==1)
                	tod_string = "offpeak";
                else if (tod==2)
                	tod_string = "pm";
                else
                	tod_string = "ni";
                
                //if od pair is not in the select link file
                if (!slData.containsOd(od)) {
                    try {
                        if (internalZones.contains(Integer.parseInt(origin)) && internalZones.contains(Integer.parseInt(dest)))
                            writer.println(line.trim() + "," + origin + "," + dest + ",1.0," + tripFile.getTourHome(tripFileLine) + "," + lastTripType);
                        
	                        //trace debugging ct trips
	                        if ( ctTrips & (Integer.parseInt(origin) == traceOrigin) & (Integer.parseInt(dest) == traceDest) ) {  
	                        	String result = "ctTrips trace not in slData origin=" + traceOrigin + " dest=" + traceDest;
	                            result = result + " " + line.trim() + "," + origin + "," + dest + ",1.0," + tripFile.getTourHome(tripFileLine) + "," + lastTripType;
	                        	logger.info(result);
	                        }
                        
                    } catch (NumberFormatException e) {
                        //ignore
                    }
                    continue;
                }
                
                // od pair is in the select link file
                double trips = tripFile.getTripFromRecord(tripFileLine);//*factors[tripFile.getModeIdFromRecord(tripFileLine)];

                tripCounter += trips;
                int mo = zoneMatrixMap.containsKey(origin) ? zoneMatrixMap.get(origin) : -1;
                int md = zoneMatrixMap.containsKey(dest) ? zoneMatrixMap.get(dest) : -1;

                //od pair is in weaving data
                List<String> additionalEntries = new LinkedList<String>();
                if (slData.getWeavingZones().contains(od)) {
                    List<SelectLinkData.WeavingData> wds = slData.getWeavingData(od);
                    if (wds == null) {
                        logger.warn("Missing weaving data for " + od);
                        continue;
                    }
                    for (SelectLinkData.WeavingData wd : wds) {
                        List<String> links = wd.getFromNodeToNodes();
                        if (wd.isInvalid()) {
                            logger.warn("Skipping trips because of invalid path: " + line.trim());
                            tripsLostToWeaving++;
                            continue;
                        }
                        boolean skip = wd.isFirstLinkIn();  //skip trips that are outside the region
                        int lcounter = 0;
                        int lastId = mo;
                        for (String link : links) {
                            SelectLinkData.LinkData ld = wd.getRepresentativeLinkData(lcounter);
                            int lid = zoneMatrixMap.get(ld.getMatrixEntryName());
                            
                            if (!skip && wd.getPercentage() > 0.0){
                            	additionalEntries.add("," + reverseZoneMatrixMap.get(lastId) + "," + reverseZoneMatrixMap.get(lid) + "," + wd.getPercentage() + "," + tripFile.getTourHome(tripFileLine) + "," + lastTripType);
                            	
                                direction = ld.getIn();
                                station = ld.getExternalStation();
                                
                                //set direction in string format
                                if (direction)
                                	dir_string = "IN";
                                else
                                	dir_string = "OUT";
                                
                                strKey = tod_string + "," + station + "," + dir_string;
                            	totalTrips = slSummaryData.get(strKey).get(fieldIndex);
                            	totalTrips += wd.getPercentage();
                            	slSummaryData.get(strKey).set(fieldIndex, totalTrips);                                
                                
                            }
                                
                            skip ^= true; //skip every other link
                            lastId = lid;
                            lcounter++;
                        }
                        
                        if (!skip && wd.getPercentage() > 0.0){
                            additionalEntries.add("," + reverseZoneMatrixMap.get(lastId) + "," + reverseZoneMatrixMap.get(md) + "," + wd.getPercentage() + "," + tripFile.getTourHome(tripFileLine) + "," + lastTripType);
                            SelectLinkData.LinkData ld = wd.getRepresentativeLinkData(0);
                            direction = ld.getIn();
                            station = ld.getExternalStation();
                            
                            //set direction in string format
                            if (direction)
                            	dir_string = "IN";
                            else
                            	dir_string = "OUT";
                            
                            strKey = tod_string + "," + station + "," + dir_string;
                        	totalTrips = slSummaryData.get(strKey).get(fieldIndex);
                        	totalTrips += wd.getPercentage();
                        	slSummaryData.get(strKey).set(fieldIndex, totalTrips);                             
                        }
                    }
                    for (String ae : additionalEntries)
                        writer.println(line.trim() + ae);

                    continue;
                }

                //set interior/exterior
                boolean ee = exteriorZones.contains(origin) && exteriorZones.contains(dest);
                boolean ii = !exteriorZones.contains(origin) && !exteriorZones.contains(dest);
                boolean ie = !exteriorZones.contains(origin) && exteriorZones.contains(dest);
                boolean ei = exteriorZones.contains(origin) && !exteriorZones.contains(dest);
                
                if (ee)
                    eeTripCounter += trips;
                	//station = reverseZoneMatrixMap.get(lid);
                else if (ii)
                    iiTripCounter += trips;
                else if (ie)
                	ieTripCounter += trips;
                else
                    eiTripCounter += trips;

                //subtract from original od in matrix
                List<SelectLinkData.LinkData> linkData = slData.getDataForOd(od);
                for (SelectLinkData.LinkData ld : linkData) {
                    if (!zoneMatrixMap.containsKey(ld.getMatrixEntryName()))
                        continue; //skip, because this class didn't use this external station
                    int lid = zoneMatrixMap.get(ld.getMatrixEntryName());
                    direction = ld.getIn();
                    station = ld.getExternalStation();
                    
                    //set direction in string format
                    if (direction)
                    	dir_string = "IN";
                    else
                    	dir_string = "OUT";

                    if (ld.getOdPercentage(od) > 0.0) {
                        if (ii) {
                            if (ld.getIn())
                                additionalEntries.add("," + reverseZoneMatrixMap.get(lid) + "," + reverseZoneMatrixMap.get(md) + "," + ld.getOdPercentage(od) + "," + tripFile.getTourHome(tripFileLine) + "," + lastTripType);
                             else
                                additionalEntries.add("," + reverseZoneMatrixMap.get(mo) + "," + reverseZoneMatrixMap.get(lid) + "," + ld.getOdPercentage(od) + "," + tripFile.getTourHome(tripFileLine) + "," + lastTripType);
                        } else {
                            if (exteriorZones.contains(origin) && !exteriorZones.contains(dest))
                                additionalEntries.add("," + reverseZoneMatrixMap.get(lid) + "," + reverseZoneMatrixMap.get(md) + "," + ld.getOdPercentage(od) + "," + tripFile.getTourHome(tripFileLine) + "," + lastTripType);
                            else if (!exteriorZones.contains(origin) && exteriorZones.contains(dest))
                                additionalEntries.add("," + reverseZoneMatrixMap.get(mo) + "," + reverseZoneMatrixMap.get(lid) + "," + ld.getOdPercentage(od) + "," + tripFile.getTourHome(tripFileLine) + "," + lastTripType);
                        }
                        if (ee) {
                            if (ld.getIn())  //don't double count, but still need to split trip across "outs"
                                for (SelectLinkData.LinkData ldo : linkData)
                                    if (!ldo.getIn())
                                        additionalEntries.add("," + reverseZoneMatrixMap.get(lid) + "," + ldo.getMatrixEntryName() + "," + ld.getOdPercentage(od)*ldo.getOdPercentage(od) + "," + tripFile.getTourHome(tripFileLine) + "," + lastTripType);
                        }
                                                
                        strKey = tod_string + "," + station + "," + dir_string;
                    	totalTrips = slSummaryData.get(strKey).get(fieldIndex);
                    	totalTrips += ld.getOdPercentage(od);
                    	slSummaryData.get(strKey).set(fieldIndex, totalTrips);
                    }
                }
                for (String ae : additionalEntries)
                    writer.println(line.trim() + ae);
                       
                //trace debugging ct trips
                if ( ctTrips & (Integer.parseInt(origin) == traceOrigin) & (Integer.parseInt(dest) == traceDest) ) {  
                	String result = "ctTrips trace origin=" + traceOrigin + " dest=" + traceDest + " " + line.trim();
                	for (String ae : additionalEntries)
                        result = result + ae;
                	logger.info(result);
                }
                
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null)
                writer.close();
        }

        logger.info("Total ee trips tallied: " + Math.round(eeTripCounter));
        logger.info("Total ie trips tallied: " + Math.round(ieTripCounter));
        logger.info("Total ei trips tallied: " + Math.round(eiTripCounter));
        logger.info("Total ii trips tallied: " + Math.round(iiTripCounter));
        logger.info("Total trips tallied: " + Math.round(tripCounter));
        logger.info(String.format("Trips lost to weaving: %.2f (%.2f%%)",tripsLostToWeaving,tripsLostToWeaving / tripCounter * 100));
    }
}

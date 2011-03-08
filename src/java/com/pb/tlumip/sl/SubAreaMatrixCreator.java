package com.pb.tlumip.sl;

import com.pb.common.matrix.CSVMatrixWriter;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixWriter;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.ts.DemandHandler;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * @author crf <br/>
 *         Started: Dec 9, 2009 8:57:53 PM
 */
public class SubAreaMatrixCreator {
    private static final Logger logger = org.apache.log4j.Logger.getLogger(SubAreaMatrixCreator.class);

    public static final String SL_AUTO_ASSIGN_CLASS = "a";
    public static final String SL_TRUCK_ASSIGN_CLASS = "d";
    public static final String AM_STRING_NAME = "ampeak";
    public static final String MD_STRING_NAME = "mdoffpeak";
    public static final String PM_STRING_NAME = "pmpeak";
    public static final String NT_STRING_NAME = "ntoffpeak";
    public static final String OUTPUT_MATRIX_TYPE_STRING = "{TYPE}";

    private final ResourceBundle rb;
    private final SelectLinkData autoSelectLinkData;
    private final SelectLinkData truckSelectLinkData;

    public SubAreaMatrixCreator(ResourceBundle rb) {
        this.rb = rb;
        String dataFile = rb.getString("sl.current.directory") + rb.getString("sl.output.file.select.link.results");
        autoSelectLinkData = new SelectLinkData(dataFile,SL_AUTO_ASSIGN_CLASS,rb);
        truckSelectLinkData = new SelectLinkData(dataFile,SL_TRUCK_ASSIGN_CLASS,rb);
        autoSelectLinkData.reconcileAgainstOtherSelectLinkData(truckSelectLinkData);
    }

    public void createSubAreaMatrices() {
        TripSynthesizer ts = synthesizeTrips();
        List<String> matrixNames = new LinkedList<String>();
        List<Matrix> matrices = new LinkedList<Matrix>();
        for (boolean auto : new boolean[] {true,false}) {
            OdMatrixGroup.OdMatrixGroupCollection omc = ts.getSynthesizedMatrices(auto);
            for (String type : omc.keySet()) {
                logger.info("Forming subarea matrices for " + (auto ? "auto " : "truck ") + type);
                OdMatrixGroup subAreaMatrices = formSubAreaMatrices(omc.get(type),auto ? autoSelectLinkData : truckSelectLinkData);
                int[] externals = getExternalNumbers(subAreaMatrices.getZoneMatrixMap());
                String baseOutFile = formOutputMatrixTemplateName(auto,type);
                //baseOutFile = baseOutFile.substring(baseOutFile.lastIndexOf('/')+1,baseOutFile.lastIndexOf('.'));
                for (int i = 0; i < 4; i++) {
                    String outFile;
                    switch (i) {
                        case 0 : outFile = baseOutFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,AM_STRING_NAME); break;
                        case 1 : outFile = baseOutFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,MD_STRING_NAME); break;
                        case 2 : outFile = baseOutFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,PM_STRING_NAME); break;
                        case 3 : outFile = baseOutFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,NT_STRING_NAME); break;
                        default : throw new RuntimeException("invalid time period: " + i);
                    }
                    subAreaMatrices.getMatrix(i).setExternalNumbers(externals);
                    matrixNames.add(outFile);
                    matrices.add(subAreaMatrices.getMatrix(i));
                }
            }
        }
        writeMatrices(rb.getString("sl.link.demand.output.filename"),matrixNames.toArray(new String[matrixNames.size()]),matrices.toArray(new Matrix[matrices.size()]));
//        for (boolean auto : new boolean[] {true,false}) {
//            OdMatrixGroup.OdMatrixGroupCollection omc = ts.getSynthesizedMatrices(auto);
//            for (String type : omc.keySet()) {
//                logger.info("Forming subarea matrices for " + (auto ? "auto " : "truck ") + type);
//                OdMatrixGroup subAreaMatrices = formSubAreaMatrices(omc.get(type),auto ? autoSelectLinkData : truckSelectLinkData);
//                writeSubAreaMatrices(subAreaMatrices,auto,type);
//            }
////            logger.info("Forming subarea matrices for " + (auto ? "auto" : "truck"));
////            OdMatrixGroup subAreaMatrices = formSubAreaMatrices(ts.getSynthesizedMatrices(auto),auto ? autoSelectLinkData : truckSelectLinkData);
////            writeSubAreaMatrices(subAreaMatrices,auto);
//        }
    }

    private TripSynthesizer synthesizeTrips() {
        TripSynthesizer ts = new TripSynthesizer(rb,autoSelectLinkData,truckSelectLinkData,
                TripClassifier.getClassifier(rb,"SDT"),
                TripClassifier.getClassifier(rb,"LDT"),
                TripClassifier.getClassifier(rb,"CT"),
                TripClassifier.getClassifier(rb,""));
        ts.synthesizeTrips();
        return ts;
    }

    private OdMatrixGroup formSubAreaMatrices(OdMatrixGroup odMatrices, SelectLinkData sld) {
        OdMatrixGroup outputGroup = new OdMatrixGroup();
        Map<String,Integer> zoneSubMatrixMap = new HashMap<String,Integer>();
        int zoneCount = 1;
        for (String zone : sld.getInteriorZones())
            zoneSubMatrixMap.put(zone,zoneCount++);
        for (String link :  sld.getExternalStationList())
            zoneSubMatrixMap.put(link,zoneCount++);
        zoneCount--; //decrement to match last count
        outputGroup.setZoneMatrixMap(zoneSubMatrixMap);

        Map<String,Integer> zoneMatrixMap = odMatrices.getZoneMatrixMap();

        Matrix subAreaDemandBase = new Matrix(zoneCount,zoneCount);
        //loop over each period
        for (int i = 0; i < 4; i++) {
            outputGroup.initMatrix(subAreaDemandBase,i);
            Matrix m = outputGroup.getMatrix(i);
            Matrix sourceMatrix = odMatrices.getMatrix(i);
            for (String origin : zoneSubMatrixMap.keySet()) {
                int o = zoneSubMatrixMap.get(origin);
                int so = zoneMatrixMap.get(origin);
                for (String dest : zoneSubMatrixMap.keySet()) {
                    int d = zoneSubMatrixMap.get(dest);
                    int sd = zoneMatrixMap.get(dest);
                    m.setValueAt(o,d,sourceMatrix.getValueAt(so,sd));
                }
            }
        }
        return outputGroup;
    }

//    private void writeSubAreaMatrices(OdMatrixGroup matrices, boolean auto) {
    private void writeSubAreaMatrices(OdMatrixGroup matrices, boolean auto, String type) {
        writeOdMatrix(matrices,auto,type);
//        writeTazList(matrices.getZoneMatrixMap(),auto);
    }

    private int[] getExternalNumbers(Map<String,Integer> tazMatrixMap) {
        Map<Integer,String> matrixTazMap = new TreeMap<Integer,String>();
        for (String taz : tazMatrixMap.keySet())
            matrixTazMap.put(tazMatrixMap.get(taz),taz);
        int[] externals = new int[matrixTazMap.size()+1];
        for (Integer i : matrixTazMap.keySet())
            externals[i] = SelectLinkData.getLinkExternalStationNumber(matrixTazMap.get(i));
        return externals;
    }

    private String formOutputMatrixTemplateName(boolean auto, String type) {
//        String typeString = type.toLowerCase().replace(" ","_");
        //return rb.getString("sl.link.demand.output.filename").replace(DemandHandler.DEMAND_OUTPUT_MODE_STRING,auto ? SL_AUTO_ASSIGN_CLASS : SL_TRUCK_ASSIGN_CLASS);
        return rb.getString("sl.link.demand.output.matrix.names").replace(DemandHandler.DEMAND_OUTPUT_MODE_STRING,(auto ? SL_AUTO_ASSIGN_CLASS : SL_TRUCK_ASSIGN_CLASS))
                                                                 .replace(OUTPUT_MATRIX_TYPE_STRING,type);

    }

    private void writeMatrices(String matrixFile, String[] names, Matrix[] matrices) {
        for (int i = 0; i < matrices.length; i++)
            matrices[i].setName(names[i]);
        CSVMatrixWriter writer = new CSVMatrixWriter(new File(matrixFile));
        writer.writeMatrices(null,matrices);
    }

//    private void writeOdMatrix(OdMatrixGroup matrices, boolean auto) {
    private void writeOdMatrix(OdMatrixGroup matrices, boolean auto, String type) {
        int[] externals = getExternalNumbers(matrices.getZoneMatrixMap());
        logger.info("Writing subarea trips for " + (auto ? "auto " : "truck ") + type);
        String baseOutFile = formOutputMatrixTemplateName(auto,type);
        for (int i = 0; i < 4; i++) {
            String outFile;
            switch (i) {
                case 0 : outFile = baseOutFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,AM_STRING_NAME); break;
                case 1 : outFile = baseOutFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,MD_STRING_NAME); break;
                case 2 : outFile = baseOutFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,PM_STRING_NAME); break;
                case 3 : outFile = baseOutFile.replace(DemandHandler.DEMAND_OUTPUT_TIME_PERIOD_STRING,NT_STRING_NAME); break;
                default : throw new RuntimeException("invalid time period: " + i);
            }
            matrices.getMatrix(i).setExternalNumbers(externals);
            ZipMatrixWriter writer = new ZipMatrixWriter(new File(outFile));
            writer.writeMatrix(matrices.getMatrix(i));
        }
    }

//    private void writeTazList(Map<String,Integer> tazMatrixMap, boolean auto) {
////        Map<String,Integer> tazMatrixMap = auto ? autoZoneMatrixMap : truckZoneMatrixMap;
//        String fileName = rb.getString("sl.taz.list.output.filename").replace(DemandHandler.DEMAND_OUTPUT_MODE_STRING,auto ? SL_AUTO_ASSIGN_CLASS : SL_TRUCK_ASSIGN_CLASS);
//        Map<Integer,String> matrixTazMap = new TreeMap<Integer,String>();
//        for (String taz : tazMatrixMap.keySet())
//            matrixTazMap.put(tazMatrixMap.get(taz),taz);
//        TextFile file = new TextFile();
//        file.addLine("Index,TAZ");
//        for (int matrixId : matrixTazMap.keySet())
//            file.addLine(matrixId + "," + matrixTazMap.get(matrixId));
//        file.writeTo(fileName);
//    }

    public static void main(String ... args) {
        long startTime = System.currentTimeMillis();
        //ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("C:\\transfers\\sl\\sl.properties"));
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File(args[0] + "\\sl.properties"));

//        String dataFile = rb.getString("sl.current.directory") + rb.getString("sl.output.file.select.link.results");
//        SelectLinkData autoSelectLinkData = new SelectLinkData(dataFile,SL_AUTO_ASSIGN_CLASS);
//        System.out.println(autoSelectLinkData.getInteriorZones().contains("2442"));
//        System.out.println(autoSelectLinkData.getExteriorZones().contains("2442"));



        SubAreaMatrixCreator samc = new SubAreaMatrixCreator(rb);
        samc.createSubAreaMatrices();
        logger.info("Total Time: " + ((System.currentTimeMillis() - startTime)/1000) + " seconds.");
    }
}
package com.pb.tlumip.sl;

import com.pb.tlumip.ao.ExternalProcess;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.pb.models.utils.StatusLogger;
import com.pb.common.util.ResourceUtil;

/**
 * @author crf <br/>
 *         Started: Nov 20, 2009 1:35:06 PM
 */
public class SelectLink {
    protected static Logger logger = Logger.getLogger(SelectLink.class);

    private final ResourceBundle rb;
    private final int year;

    public SelectLink(ResourceBundle rb, int year) {
        this.rb = rb;
        this.year = year;
    }
//    sl.select.link.data.python.file = build_select_link_matrices.py
//    sl.select.link.node.sequence.python.file = get_link_sequence.py
    protected static boolean visumMode(ResourceBundle rb) {
        return rb.containsKey("sl.visum.mode") ? Boolean.parseBoolean(rb.getString("sl.visum.mode")) : false;
    }

    private boolean visumMode() {
        return visumMode(rb);
    }

    public void runStages(String stagesToRun) {
        Set<Character> stageChars = new HashSet<Character>();
        for (char stageChar : stagesToRun.toCharArray())
            if (SelectLinkStage.isValidStageChar(stageChar))
                stageChars.add(stageChar);
            else
                throw new IllegalArgumentException("No select link stage defined for character: " + stageChar);
        //go through stages in enum order, as that defines correct order
        for (SelectLinkStage stage : SelectLinkStage.values()) {
            if (stageChars.contains(stage.getStageChar()))
                runStage(stage);
        }
    }

    private void runStage(SelectLinkStage stage) {
        switch (stage) {
//            case GENERATE_PATHS : generatePaths(); break;
            case GENERATE_SELECT_LINK_DATA : generateSelectLinkData(); break;
//            case CREATE_SUBAREA_MATRIX : createSubAreaMatrix(); break;
            case APPEND_SELECT_LINK_TO_TRIPS : appendSelectLinkDataToTrips(); break;
        }
    }

    private void generatePaths() {
        if (visumMode()) {
            //do nothing, visum assignment already has paths embedded in it
        } else {
            runRScript("sl.generate.paths.r.file","generate select link paths");
        }
    }

    private void generateSelectLinkData() {
        if (visumMode()) {
            runPythonScript("sl.select.link.data.python.file","generate select link data",
                    "ta.version.file",
                    "ta.peak.paths.version.file",
                    "ta.offpeak.paths.version.file",
                    "ta.pm.paths.version.file",
                    "ta.ni.paths.version.file",
                    "sl.input.file.select.links",
                    "sl.output.file.select.link.results",
                    "sl.visum.demand.segment.mapping");
        } else {
            runRScript("sl.select.link.data.r.file","generate select link data");
        }
    }

    private void createSubAreaMatrix() {
        SubAreaMatrixCreator samc = new SubAreaMatrixCreator(rb);
        samc.createSubAreaMatrices();
    }

    private void appendSelectLinkDataToTrips() {
        String dataFile = rb.getString("sl.current.directory") + rb.getString("sl.output.file.select.link.results");
        Map<Integer,SelectLinkData> autoSelectLinkData = new HashMap<>();
        Map<Integer,SelectLinkData> truckSelectLinkData = new HashMap<>();
        Map<String,SelectLinkData> finishedSelectLinkData = new HashMap<String,SelectLinkData>();
        if (rb.containsKey("sl.auto.classes")) {
            String[] autoSelectLinkDataClasses = rb.getString("sl.auto.classes").split(",");
            String[] truckSelectLinkDataClasses = rb.getString("sl.truck.classes").split(",");
            int counter = 0;
            for (String sldClass : autoSelectLinkDataClasses) {
                sldClass = sldClass.trim();
                if (!finishedSelectLinkData.containsKey(sldClass))
                    finishedSelectLinkData.put(sldClass,new SelectLinkData(dataFile,sldClass,rb));
                autoSelectLinkData.put(counter++,finishedSelectLinkData.get(sldClass));
            }
            counter = 0;
            for (String sldClass : truckSelectLinkDataClasses) {
                sldClass = sldClass.trim();
                if (!finishedSelectLinkData.containsKey(sldClass))
                    finishedSelectLinkData.put(sldClass,new SelectLinkData(dataFile,sldClass,rb));
                truckSelectLinkData.put(counter++,finishedSelectLinkData.get(sldClass));
            }
        } else {
            finishedSelectLinkData.put(SubAreaMatrixCreator.SL_AUTO_ASSIGN_CLASS,new SelectLinkData(dataFile,SubAreaMatrixCreator.SL_AUTO_ASSIGN_CLASS,rb));
            finishedSelectLinkData.put(SubAreaMatrixCreator.SL_TRUCK_ASSIGN_CLASS,new SelectLinkData(dataFile,SubAreaMatrixCreator.SL_TRUCK_ASSIGN_CLASS,rb));
            for (int i = 0; i < 4; i++) {
                autoSelectLinkData.put(i,finishedSelectLinkData.get(SubAreaMatrixCreator.SL_AUTO_ASSIGN_CLASS));
                truckSelectLinkData.put(i,finishedSelectLinkData.get(SubAreaMatrixCreator.SL_TRUCK_ASSIGN_CLASS));
            }
        }

        List<SelectLinkData> otherSlds = new LinkedList<SelectLinkData>(finishedSelectLinkData.values());
        SelectLinkData reconcileBase = otherSlds.remove(0);
        reconcileBase.reconcileAgainstOtherSelectLinkData(otherSlds.toArray(new SelectLinkData[otherSlds.size()]));

        TripSynthesizer ts = new TripSynthesizer(rb,autoSelectLinkData,truckSelectLinkData,null,null,null,null);
//                TripClassifier.getClassifier(rb,"SDT"),
//                TripClassifier.getClassifier(rb,"LDT"),
//                TripClassifier.getClassifier(rb,"CT"),
//                TripClassifier.getClassifier(rb,""));

        String internalZoneFile = null;
        for (Enumeration<String> e = rb.getKeys(); e.hasMoreElements();)
            if (e.nextElement().equals("sl.internal.zone.file"))
                internalZoneFile = rb.getString("sl.internal.zone.file");
        Set<Integer> internalZones = new HashSet<Integer>();
        if (internalZoneFile != null) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(internalZoneFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.length() > 0)
                        internalZones.add(Integer.parseInt(line));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        ts.synthesizeTripsAndAppendToTripFile(internalZones);
        bundleAndZipOutputs(ts);
        for (File f : ts.getSelectLinkTripFiles())
            f.delete();
    }

    private void bundleAndZipOutputs(TripSynthesizer ts) {
        List<File> filesToZip = new LinkedList<File>(ts.getSelectLinkTripFiles());
        filesToZip.add(new File(rb.getString("spg2.current.synpop.summary")));
        filesToZip.add(new File(rb.getString("sdt.current.employment")));
        filesToZip.add(new File(rb.getString("alpha2beta.file")));

        ZipOutputStream zos = null;
        try {
            String outputFile = rb.getString("sl.output.bundle.file");
            zos = new ZipOutputStream(new FileOutputStream(outputFile));
            logger.info("Bundling select link files to: " + outputFile);
            byte[] buffer = new byte[2048];
            for (File f : filesToZip) {
                logger.info("    " + f);
                InputStream is = null;
                try {
                    zos.putNextEntry(new ZipEntry(f.getName()));
                    is = new BufferedInputStream(new FileInputStream(f));
                    int count;
                    while ((count = is.read(buffer)) > -1)
                        zos.write(buffer,0,count);
                } finally {
                    if (is != null)
                        is.close();
                    zos.flush();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (zos != null) {
                try {
                    zos.flush();
                } catch (IOException e) {
                    //swallow
                }
                try {
                    zos.close();
                } catch (IOException e) {
                    //swallow
                }
            }
        }
    }

    void runPythonScript(String pythonScriptKey, String name, String ... additionalArgs) {
        logger.info("Starting " + name);
        StatusLogger.logText("sl",name + " started for " + year);
        runPythonScript(pythonScriptKey,name,rb,additionalArgs);
        StatusLogger.logText("sl",name + " finished.");
    }

    static void runPythonScript(String pythonScriptKey, String name, ResourceBundle rb, String ... additionalArgs) {
        //match the signature of the R method...
        String processProgram = rb.getString("python.visum.executable");
        List<String> args = new LinkedList<String>();
        args.add(rb.getString(pythonScriptKey));
        for (String additionalArg : additionalArgs)
            args.add(rb.containsKey(additionalArg) ? rb.getString(additionalArg) : additionalArg);
        new ExternalProcess(name,processProgram,args).startProcess();
    }

    void runRScript(String rScriptKey, String name, String ... additionalArgs) {
        logger.info("Starting " + name);
        StatusLogger.logText("sl",name + " started for " + year);
        runRScript(rScriptKey,name,rb,additionalArgs);
        StatusLogger.logText("sl",name + " finished.");
    }

    static void runRScript(String rScriptKey, String name, ResourceBundle rb, String ... additionalArgs) {

        String rFile = ResourceUtil.getProperty(rb,rScriptKey);
        String scenPathKey = ResourceUtil.getProperty(rb,"sl.scenario.path.key");
        String[] processArgs = new String[additionalArgs.length+5];
        processArgs[0] = "R";
        processArgs[1] = "--no-save";
        processArgs[2] = "<";
        processArgs[3] = ResourceUtil.getProperty(rb,"sl.code.path") + rFile;
        processArgs[4] = scenPathKey + "=" + ResourceUtil.getProperty(rb,"sl.current.directory");
        System.arraycopy(additionalArgs, 0, processArgs, 5, additionalArgs.length);
        ProcessBuilder pb = new ProcessBuilder(processArgs);
        pb.redirectErrorStream(true);
        final Process p;
        try {
            p = pb.start();
            //log error stream
            new Thread(new Runnable() {
                public void run() {
                    logInputStream(p.getErrorStream(),true);
                }
            }).start();
            logInputStream(p.getInputStream(),false);
            if (p.waitFor() != 0)
                slFatalError(name + " did not finish correctly");
        } catch (IOException e) {
            slFatalError("An IO exception occured while trying " + name,e);
        } catch (InterruptedException e) {
            slFatalError("Interrupted exception caught waiting for " + name + " to finish",e);
        }
        logger.info(name + " finished.");
    }


    private static void slFatalError(String message) {
        slFatalError(message,null);
    }

    private static void slFatalError(String message, Exception e) {
        if (e != null)
            logger.fatal(message, e);
        else
            logger.fatal(message);
        throw new RuntimeException(message);
    }

    private static void logInputStream(InputStream stream, boolean error) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try {
            String line;
            while ((line = reader.readLine()) != null)
                if (error)
                    logger.error(line);
                else
                    logger.info(line);
        } catch (IOException e) {
            logger.error("An IO exception occured while logging select link model output",e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                //swallow
            }
        }
    }

    public static void main(String ... args) throws IOException {
        long startTime = System.currentTimeMillis();
        String bpath = args[0].replace("\\","/");
        if (bpath.endsWith("/"))
            bpath = bpath.substring(0,bpath.length()-1);
        BufferedReader reader = new BufferedReader(new FileReader(new File(bpath,"sl.properties.template")));
        PrintWriter writer = new PrintWriter(new FileWriter(new File(bpath,"global.properties")));
        String line;
        while ((line = reader.readLine()) != null)
            writer.println(line.replace("@@base_path@@",bpath));
        reader.close();
        writer.close();
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File(bpath,"global.properties"));
        SelectLink sl = new SelectLink(rb,-1);
        sl.runStages(args[1]);
        logger.info("Total Time: " + ((System.currentTimeMillis() - startTime)/1000) + " seconds.");
    }
    

}

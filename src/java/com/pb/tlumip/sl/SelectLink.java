package com.pb.tlumip.sl;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.HashSet;

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
            case GENERATE_PATHS : generatePaths(); break;
            case GENERATE_SELECT_LINK_DATA : generateSelectLinkData(); break;
            case CREATE_SUBAREA_MATRIX : createSubAreaMatrix(); break;
            case APPEND_SELECT_LINK_TO_TRIPS : appendSelectLinkDataToTrips(); break;
        }
    }

    private void generatePaths() {
        runRScript("sl.generate.paths.r.file","generate select link paths");
    }

    private void generateSelectLinkData() {
        runRScript("sl.select.link.data.r.file","generate select link data");
    }

    private void createSubAreaMatrix() {
        SubAreaMatrixCreator samc = new SubAreaMatrixCreator(rb);
        samc.createSubAreaMatrices();
    }

    private void appendSelectLinkDataToTrips() {
        String dataFile = rb.getString("sl.current.directory") + rb.getString("sl.output.file.select.link.results");
        SelectLinkData autoSelectLinkData = new SelectLinkData(dataFile,SubAreaMatrixCreator.SL_AUTO_ASSIGN_CLASS,rb);
        SelectLinkData truckSelectLinkData = new SelectLinkData(dataFile,SubAreaMatrixCreator.SL_TRUCK_ASSIGN_CLASS,rb);
        autoSelectLinkData.reconcileAgainstOtherSelectLinkData(truckSelectLinkData);

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

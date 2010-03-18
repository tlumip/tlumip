package com.pb.tlumip.sl;

import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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

    private void runRScript(String rScriptKey, String name) {
        logger.info("Starting " + name);
        StatusLogger.logText("sl",name + " started for " + year);

        String rFile = ResourceUtil.getProperty(rb,rScriptKey);
        String scenPathKey = ResourceUtil.getProperty(rb,"sl.scenario.path.key");
        ProcessBuilder pb = new ProcessBuilder(
                "R","--no-save","<",
                ResourceUtil.getProperty(rb,"sl.code.path") + rFile,
                scenPathKey + "=" + ResourceUtil.getProperty(rb,"sl.current.directory"));
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
        StatusLogger.logText("sl",name + " finished.");
    }


    private void slFatalError(String message) {
        slFatalError(message,null);
    }

    private void slFatalError(String message, Exception e) {
        if (e != null)
            logger.fatal(message, e);
        else
            logger.fatal(message);
        throw new RuntimeException(message);
    }

    private void logInputStream(InputStream stream, boolean error) {
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


    

}

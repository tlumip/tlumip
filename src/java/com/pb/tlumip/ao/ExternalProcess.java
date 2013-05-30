package com.pb.tlumip.ao;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * The {@code ExternalProcess} ...
 *
 * @author crf
 *         Started 4/3/13 12:24 PM
 */
public class ExternalProcess {
    private static final Logger logger = Logger.getLogger(ExternalProcess.class);

    private final String processName;
    private final List<String> process;

    public ExternalProcess(String processName, String processProgram, List<String> processArguments) {
        this.processName = processName;
        process = new LinkedList<String>();
        process.add(processProgram);
        process.addAll(processArguments);
    }

    public ExternalProcess(String processName, String processProgram, String ... processArguments) {
        this(processName,processProgram,Arrays.asList(processArguments));
    }

    public void startProcess() {
        StringBuilder sb = new StringBuilder();
        for (String processPart : process)
            sb.append(processPart).append(' ');
        logger.info("Executing (" + processName + "): " + sb);
        ProcessBuilder pb = new ProcessBuilder(process);
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
                signalErrors();
        } catch (IOException e) {
            logger.error("An IO exception occurred while trying to run " + processName,e);
            signalErrors();
        } catch (InterruptedException e) {
            logger.error("Interrupted exception caught waiting for " + processName + " to finish",e);
            signalErrors();
        }
    }

    private void signalErrors() {
        logger.error("Errors occurred running " +  processName);
        throw new RuntimeException("Errors occured running " + processName + ". Check log files.");
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
            logger.error("An IO exception occurred while logging " + processName + " output",e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                //swallow
            }
        }
    }
}

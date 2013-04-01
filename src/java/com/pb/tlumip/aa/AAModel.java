package com.pb.tlumip.aa;

import com.pb.common.util.ResourceUtil;
import com.pb.models.reference.ModelComponent;
import com.pb.models.utils.StatusLogger;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ResourceBundle;

/**
 * The {@code AAModel} runs the AA model, which is a replacement for the PI module. Though AA is java, it's dependencies
 * and class versions are totally different from the rest of the java codebase. Additionally, the source java code for this
 * project is available from a different repository, and only to a select group of people. Because of this, the jar files
 * for this project need to maintained as a "black box" which may then be called. This leaves us two choices:
 * 
 * 1) Create a mock class (com.hbaspecto.oregon.pecas.aa.OregonAAControl) and call it directly from AO, making sure that
 *    all of the AA jars are listed before our normal classpath to get dependency resolution for AA correct.
 * 2) Have AO call the AA model as if it was from an external program (like the python ET module).
 * 
 * While (1) seems more elegant since it keeps all of the java stuff together, it is actually problematic because it is
 * unclear if/what code that AO needs will be superseded by the jars that are provided for AA. Since those AA jars are
 * not being checked against this code, trusting that their code that affects AO will not break things is not a viable
 * option. (1) could be made to work correctly if we did some fancy stuff with a custom classloader, but that is frankly
 * overkill for this. So we'll do (2).
 *
 * @author crf <br/>
 *         Started 8/23/11 2:20 PM
 */
public class AAModel extends ModelComponent {
    private static final Logger logger = Logger.getLogger(AAModel.class);

    public AAModel(ResourceBundle appRb, ResourceBundle globalRb){
		setResourceBundles(appRb, globalRb);    //creates a resource bundle as a class attribute called appRb.
    }

    public void startModel(int baseYear, int timeInterval) {
        logger.info("Starting AA Model.");
        StatusLogger.logText("aa", "AA started for year t" + timeInterval);

        String aaJavaCommand =  ResourceUtil.getProperty(appRb,"aa.command.java");
        String aaXmx =  ResourceUtil.getProperty(appRb,"aa.command.max.heap.size");
        String aaLog4j =  ResourceUtil.getProperty(appRb,"aa.command.log4j.config.file");
        String aaCp =  ResourceUtil.getProperty(appRb,"aa.command.classpath");
        String aaClass =  ResourceUtil.getProperty(appRb,"aa.command.class");

        //call is java -Xmx3000m -Dlog4j.configuration=log4j.xml -cp classpath com.hbaspecto.oregon.pecas.aa.OregonAAControl
        //log4j configuration should be console and status only so as not to double write logging
        ProcessBuilder pb = new ProcessBuilder(
                aaJavaCommand,
                "-Xmx" + aaXmx,
                "-Dlog4j.configuration=\"" + aaLog4j + "\"",
                "-cp",
                "\"" + aaCp + "\"",
                aaClass
        );

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
            int returnCode = p.waitFor();
            if (returnCode != 0 && returnCode != 2) //0 = normal, 2 = max iterations (blech)
                indicateAAErrors();
        } catch (IOException e) {
            logger.error("An IO exception occurred while trying to run AA model",e);
            indicateAAErrors();
        } catch (InterruptedException e) {
            logger.error("Interrupted exception caught waiting for AA model to finish",e);
            indicateAAErrors();
        }
        logger.info("Finishing AA Model.");
        StatusLogger.logText("aa","AA Done");
    }

    private void indicateAAErrors() {
        logger.error("Errors occurred running AA model.");
        throw new RuntimeException("Errors occured running the AA model. Check log files.");
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
            logger.error("An IO exception occurred while logging AA model output",e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                //swallow
            }
        }
    }
}

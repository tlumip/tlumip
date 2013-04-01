package com.pb.tlumip.ed;

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
 * The {@code NedModel} ...
 *
 * @author crf <br/>
 *         Started May 25, 2011 7:28:38 AM
 */
public class NEDModel extends ModelComponent {
    private static final Logger logger = Logger.getLogger(NEDModel.class);

    public NEDModel(ResourceBundle appRb, ResourceBundle globalRb){
		setResourceBundles(appRb, globalRb);    //creates a resource bundle as a class attribute called appRb.
    }

    public void startModel(int baseYear, int timeInterval) {
        logger.info("Starting NED Model.");
        StatusLogger.logText("ned","NED started for year t" + timeInterval);


        String nedPropertyFile = ResourceUtil.getProperty(appRb,"ned.property");
        String nedPythonCommand =  ResourceUtil.getProperty(appRb, "ned.python.command");
        String pythonExecutable = ResourceUtil.getProperty(appRb, "python.executable");

        ProcessBuilder pb = new ProcessBuilder(
                pythonExecutable,
                nedPythonCommand,
                nedPropertyFile);


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
                indicateNedErrors();
        } catch (IOException e) {
            logger.error("An IO exception occurred while trying to run NED model",e);
            indicateNedErrors();
        } catch (InterruptedException e) {
            logger.error("Interrupted exception caught waiting for NED model to finish",e);
            indicateNedErrors();
        }
        logger.info("Finishing NED Model.");
        StatusLogger.logText("ned","NED Done");
    }

    private void indicateNedErrors() {
        logger.error("Errors occurred running NED model.");
        throw new RuntimeException("Errors occured running the NED model. Check log files.");
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
            logger.error("An IO exception occurred while logging NED model output",e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                //swallow
            }
        }
    }
}

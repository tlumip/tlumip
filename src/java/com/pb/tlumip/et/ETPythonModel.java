package com.pb.tlumip.et;

import com.pb.models.reference.ModelComponent;
import com.pb.models.utils.StatusLogger;
import com.pb.common.util.ResourceUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

/**
 * @author crf <br/>
 *         Started: Jan 6, 2009 2:12:18 PM
 */
public class ETPythonModel extends ModelComponent  {
    private static final Logger logger = Logger.getLogger(ETPythonModel.class);

    public ETPythonModel(ResourceBundle appRb, ResourceBundle globalRb){
		setResourceBundles(appRb, globalRb);    //creates a resource bundle as a class attribute called appRb.
    }

    public void startModel(int baseYear, int timeInterval) {
        logger.info("Starting ET Model.");
        StatusLogger.logText("et","ET started for year t" + timeInterval);
        String etPythonCommand =  ResourceUtil.getProperty(appRb, "et.python.command");
        String etPropertyFile = ResourceUtil.getProperty(appRb, "et.property");

        ProcessBuilder pb = new ProcessBuilder(
                "python",
                etPythonCommand,
                etPropertyFile);
        pb.redirectErrorStream(true);
        final Process p;
//        System.out.println(Arrays.toString(pb.command().toArray(new String[0])));
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
                indicateEtErrors();
        } catch (IOException e) {
            logger.error("An IO exception occured while trying to run ET model",e);
            indicateEtErrors();
        } catch (InterruptedException e) {
            logger.error("Interrupted exception caught waiting for ET model to finish",e);
            indicateEtErrors();
        }
        logger.info("Finishing ET Model.");
        StatusLogger.logText("et","ET Done");
    }

    private void indicateEtErrors() {
        logger.error("Errors occurred running ET model.");
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
            logger.error("An IO exception occured while logging ET model output",e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                //swallow
            }
        }
    }
}

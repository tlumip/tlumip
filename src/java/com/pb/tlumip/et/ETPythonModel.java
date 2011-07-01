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
 * At 3/4/10 ABQ meeting it was decided by Rick, Tara, Erin, & Michalis, that ET would only cover EE trucks as
 * temporarily implemented (for Oregon Freight Plan Application).   All prior code ET code (by Kim Grommes in
 * java and Michalis Xyntarakis in python) would be dropped.  If it is desired to implement ET IE/EI flows to/from
 * World Market 6006 (PI buying flows), it is recommended to use CT with extra code written to assign ET IE/EI flows
 * to the closest small road ExternalStation.  If the python ET code is to be used, the growth rate methods and
 * calibration targets from the External Stations should be checked.
 *
 *  @author crf <br/>
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

    /* ***********start old python code******************* */
    /* Do not use this code; it was replaced by ET EE component implementation only.
       If this code were to be reused, the recent growth rate functionality should be checked and the targets
       (ExtSta counts) used in calibration should be verified against the PI module all commodity IE/EI flows for the
       share of CT vs. ET IE/EI shares. */

//        String etPythonCommand =  ResourceUtil.getProperty(appRb, "et.python.command");
//        String etPropertyFile = ResourceUtil.getProperty(appRb, "et.property");
//
//        ProcessBuilder pb = new ProcessBuilder(
//                "python",
//                etPythonCommand,
//                etPropertyFile);

    /* ***********end old python code******************* */


    /* ***********start new python code******************* */
    /* This code implements EE Truck Trips.  It starts with a seed OD matrix of EE flows between External Stations
       (developed from ….), which is IPFed to match marginals which are 2000 External Station counts with growth rates
       applied per properties files (rates vary for large and small roads). */

        String etProperties = ResourceUtil.getProperty(appRb,"et.property");
        String etPythonCommand =  ResourceUtil.getProperty(appRb, "et.python.command");
//        String etOut = ResourceUtil.getProperty(appRb,"et.truck.trips");
//        String etBasis = ResourceUtil.getProperty(appRb,"et.basis.matrix");
//        String etBasisYear = ResourceUtil.getProperty(appRb,"et.basis.year");

        ProcessBuilder pb = new ProcessBuilder(
                "python",
                etPythonCommand,
                etProperties,
                "" + timeInterval);

    /* ***********end new python code******************* */

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
                indicateEtErrors();
        } catch (IOException e) {
            logger.error("An IO exception occurred while trying to run ET model",e);
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
            logger.error("An IO exception occurred while logging ET model output",e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                //swallow
            }
        }
    }
}

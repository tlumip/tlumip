package com.pb.despair.ao;

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.TableDataSet;
import com.pb.despair.ed.EDSummarizer;

import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;
import java.io.File;

/**
 * Author: willison
 * Date: Dec 14, 2004
 * <p/>
 * Created by IntelliJ IDEA.
 */
public class CalibrationManager {
    private static Logger logger = Logger.getLogger("com.pb.despair.CalibrationManager");
    boolean debug = false;

    ResourceBundle rb;          //calibration.properties which will have the names of all calibration
                                //files for all applications
    String scenarioName;
    int timeInterval;
    int baseYear;

    boolean calibrateED,
            calibrateALD,
            calibrateSPG1,      //the calibration.properties will specify which applications we
            calibratePI,        //are currently calibrating.  The property "applications.in.calibration.mode"
            calibrateSPG2,      //will have a list of all apps that will be running
            calibratePT,        //in calibration mode (or user could specfiy "all") in
            calibrateCT,         //which case all of the booleans will be set to "true".
            calibrateTS;

     CalibrationFileWriter cFileWriter;

    public CalibrationManager(ResourceBundle rb, String scenarioName, int timeInterval, int baseYear){

        this.rb = rb;
        this.scenarioName = scenarioName;
        this.timeInterval = timeInterval;
        this.baseYear = baseYear;

        //Now read in the calibrate properties to see which applications we are calibrating
        specifyWhichApplicationsToCalibrate();

        //Just a check to see if things are working right
        if(debug){
            logCalibrationSettings();
        }

        //Initialize the CalibrationFileWriter
        cFileWriter = new CalibrationFileWriter(rb, scenarioName, timeInterval);

    }

    private void specifyWhichApplicationsToCalibrate(){

        ArrayList apps = ResourceUtil.getList(rb, "applications.in.calibration.mode");
        if(apps.size() == 0) logger.warn("The ao.properties file indicated" +
                "that calibration is required but no applications have been" +
                "listed to be calibrated.  Check ao.properties and calibration.properties");

        for(Iterator i = apps.iterator(); i.hasNext(); ){
            String app = (String) i.next();

            if(app.equalsIgnoreCase("all")){
                calibrateED = true;
                calibrateALD = true;
                calibrateSPG1 = true;
                calibratePI = true;
                calibrateSPG2 = true;
                calibratePT = true;
                calibrateCT = true;
                calibrateTS = true;
            }else if(app.equalsIgnoreCase("ed")){
                calibrateED = true;
            }else if(app.equalsIgnoreCase("ald")){
                calibrateALD = true;
            }else if(app.equalsIgnoreCase("spg1")){
                calibrateSPG1 = true;
            }else if(app.equalsIgnoreCase("pi")){
                calibratePI = true;
            }else if(app.equalsIgnoreCase("spg2")){
                calibrateSPG2 = true;
            }else if(app.equalsIgnoreCase("pt")){
                calibratePT = true;
            }else if(app.equalsIgnoreCase("ct")){
                calibrateCT = true;
            }else if(app.equalsIgnoreCase("ts")){
                calibrateTS = true;
            }

        }

    }

    private void logCalibrationSettings(){
        ArrayList apps = ResourceUtil.getList(rb, "applications.in.calibration.mode");
        logger.info("The properties file specifies that the following applications should " +
                "be run in calibration mode");
        for(Iterator i = apps.iterator(); i.hasNext(); ){
            String app = (String) i.next();
            logger.info("\t"+ app);
        }

        logger.info("The calibration settings are as follows..");
        logger.info("\tcalibrateED " + calibrateED);
        logger.info("\tcalibrateALD " + calibrateALD);
        logger.info("\tcalibrateSPG1 " + calibrateSPG1);
        logger.info("\tcalibratePI " + calibratePI);
        logger.info("\tcalibrateSPG2 " + calibrateSPG2);
        logger.info("\tcalibratePT " + calibratePT);
        logger.info("\tcalibrateCT " + calibrateCT);
        logger.info("\tcalibrateTS " + calibrateTS);

    }

    public boolean isThisApplicationBeingCalibrated(String appName){

        if(appName.equalsIgnoreCase("ed")){
                return calibrateED;
        }else if(appName.equalsIgnoreCase("ald")){
            return calibrateALD;
        }else if(appName.equalsIgnoreCase("spg1")){
            return calibrateSPG1;
        }else if(appName.equalsIgnoreCase("pi")){
            return calibratePI;
        }else if(appName.equalsIgnoreCase("spg2")){
            return calibrateSPG2;
        }else if(appName.equalsIgnoreCase("pt")){
            return calibratePT;
        }else if(appName.equalsIgnoreCase("ct")){
            return calibrateCT;
        }else if(appName.equalsIgnoreCase("ts")){
            return calibrateTS;
        }else {
            logger.warn("The application name passed in was not recognized and therefore " +
                    "the method returned false.  The application name must be one of the following: " +
                    "ed, ald, spg1, pi, spg2, pt, ct or ts");
            return false;
        }
    }

    public void produceCalibrationOutput(String appName, ResourceBundle appRB){
        if(appName.equalsIgnoreCase("ed")){
            if (calibrateED) {
                logger.info("Producing and Writing ED calibration output - please wait");
                TableDataSet[] edCalibrationDataSets = produceEDCalibrationOutputs(appRB);
                cFileWriter.writeCalibrationFiles(edCalibrationDataSets, appName);
            } else {
                logger.info("The calibration.properties file does not show that ED was in calibration" +
                        " mode.  Check calibration.properties file");
            }
        }else if(appName.equalsIgnoreCase("ald")){
            if (calibrateALD) {
                logger.info("Producing and Writing ALD calibration output - please wait");
            } else {
                logger.info("The calibration.properties file does not show that ALD was in calibration" +
                        " mode.  Check calibration.properties file");
            }
        }else if(appName.equalsIgnoreCase("spg1")){
            if (calibrateSPG1) {
                logger.info("Producing and Writing SPG1 calibration output - please wait");
            } else {
                logger.info("The calibration.properties file does not show that SPG1 was in calibration" +
                        " mode.  Check calibration.properties file");
            }
        }else if(appName.equalsIgnoreCase("pi")){
            if (calibratePI) {
                logger.info("Producing and Writing PI calibration output - please wait");
            } else {
                logger.info("The calibration.properties file does not show that PI was in calibration" +
                        " mode.  Check calibration.properties file");
            }
        }else if(appName.equalsIgnoreCase("spg2")){
            if (calibrateSPG2) {
                logger.info("Producing and Writing SPG2 calibration output - please wait");
            } else {
                logger.info("The calibration.properties file does not show that SPG2 was in calibration" +
                        " mode.  Check calibration.properties file");
            }
        }else if(appName.equalsIgnoreCase("pt")){
            if (calibratePT) {
                logger.info("Producing and Writing PT calibration output - please wait");
            } else {
                logger.info("The calibration.properties file does not show that PT was in calibration" +
                        " mode.  Check calibration.properties file");
            }
        }else if(appName.equalsIgnoreCase("ct")){
            if (calibrateCT) {
                logger.info("Producing and Writing CT calibration output - please wait");
            } else {
                logger.info("The calibration.properties file does not show that CT was in calibration" +
                        " mode.  Check calibration.properties file");
            }
        }else if(appName.equalsIgnoreCase("ts")){
            if (calibrateTS) {
                logger.info("Producing and Writing TS calibration output - please wait");
            } else {
                logger.info("The calibration.properties file does not show that TS was in calibration" +
                        " mode.  Check calibration.properties file");
            }
        }else {
            logger.error("The application name passed in was not recognized and therefore " +
                    "the method returned false.  The application name must be one of the following: " +
                    "ed, ald, spg1, pi, spg2, pt, ct or ts");
        }
    }

    private TableDataSet[] produceEDCalibrationOutputs(ResourceBundle appRb){

        ArrayList outputFilesToCreate = ResourceUtil.getList(rb,"ed.calibration.outputs");
        if(outputFilesToCreate == null || outputFilesToCreate.size() == 0) {
            logger.fatal("There are no calibration " +

                "outputs listed for ED, even though ED is supposedly being run in calibration mode " +
                "- check calibration.properties for existence of an 'ed.calibration.outputs' property");
            return null;
        }

        TableDataSet[] dataSets = new TableDataSet[outputFilesToCreate.size()];
        int count = 0;
        for(Iterator i = outputFilesToCreate.iterator(); i.hasNext(); ){
            String output = (String) i.next();
            ArrayList specialColHeaders = null;
            if(output.equalsIgnoreCase("oregon.activity")){
                //get the non-distinct column headers from the calibration.properties file
                specialColHeaders = ResourceUtil.getList(rb, "ed.oregon.activity.columns");
                if(specialColHeaders == null || specialColHeaders.size() == 0 ){
                    logger.fatal("The activity columns must be listed in the calibration.properties file" +
                            " in order to generate the oregon activity calibration file - check calibration.properties " +
                            "for a property called 'ed.oregon.activity.columns'");
                    return null;
                }
            }else if(output.equalsIgnoreCase("odot.employment")){
                //get the special column headers from the calibration.properties file
                specialColHeaders = ResourceUtil.getList(rb, "ed.odot.employment.columns");
                if(specialColHeaders == null || specialColHeaders.size() == 0 ){
                    logger.fatal("The odot employment columns must be listed in the calibration.properties file" +
                            " in order to generate the odot employment calibration file - check calibration.properties " +
                            "for a property called 'ed.odot.employment.columns'");
                    return null;
                }
            }

            dataSets[count] = EDSummarizer.summarize(output, timeInterval, baseYear, appRb, specialColHeaders);
            dataSets[count].setName(output);

            count++;
        }

        return dataSets;
    }

     public static void main(String[] args) {

         ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/repositories/projects/tlumip/config/calibration.properties"));

         CalibrationManager cManager = new CalibrationManager(rb, "pleaseWork", 5, 1990);
         cManager.specifyWhichApplicationsToCalibrate();

         cManager.logCalibrationSettings();

         logger.info("Is 'ts' being calibrated? ");
         logger.info(new Boolean(cManager.isThisApplicationBeingCalibrated("ts")).toString());

         logger.info("Is 'ha' being calibrated? ");
         logger.info(new Boolean(cManager.isThisApplicationBeingCalibrated("ha")).toString());

         ResourceBundle edRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_pleaseWork/t1/ed/ed.properties"));
         cManager.produceCalibrationOutput("ed", edRb);
     }
}

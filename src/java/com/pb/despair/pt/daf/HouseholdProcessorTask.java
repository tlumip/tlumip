package com.pb.despair.pt.daf;

/**
 * HouseholdProcessorTask
 *
 * @author Freedman
 * @version Aug 11, 2004
 * 
 */


import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.pt.*;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

import java.lang.Runtime;

import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class HouseholdProcessorTask extends MessageProcessingTask {

    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.daf");
    protected static Object lock = new Object();
    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;
    protected static boolean initialized = false;
    String fileWriterQueue = "FileWriterQueue";
    PTModel ptModel;
    DCExpUtilitiesManager expUtilitiesManager;

    /**
     * Onstart method sets up model
     */
    public void onStart() {
        synchronized (lock) {
            logger.info( "***" + getName() + " started");
            //in cases where there are multiple tasks in a single vm, need to make sure only initilizing once!
            if (!initialized) {
                //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.txt file
                //that was written by the Application Orchestrator
                BufferedReader reader = null;
                int timeInterval = -1;
                String pathToPtRb = null;
                String pathToGlobalRb = null;
                try {
                    logger.info("Reading RunParams.txt file");
                    reader = new BufferedReader(new FileReader(new File( Scenario.runParamsFileName )));
                    timeInterval = Integer.parseInt(reader.readLine());
                    logger.info("\tTime Interval: " + timeInterval);
                    pathToPtRb = reader.readLine();
                    logger.info("\tPT ResourceBundle Path: " + pathToPtRb);
                    pathToGlobalRb = reader.readLine();
                    logger.info("\tGlobal ResourceBundle Path: " + pathToGlobalRb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
                globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));

                PTModelInputs ptInputs = new PTModelInputs(ptRb);
                logger.info("Setting up the aggregate mode choice model");
                ptInputs.setSeed(2002);
                ptInputs.getParameters();
                ptInputs.readSkims(globalRb);
                ptInputs.readTazData();

                initialized = true;
            }

            logger.info( "***" + getName() + " finished onStart()");
            ptModel.stopDestinationChoiceModel.buildModel(PTModelInputs.tazs);
            ptModel.workBasedTourModel.buildModel(PTModelInputs.tazs);

            expUtilitiesManager = new DCExpUtilitiesManager(ptRb);
        }
    }
    /**
     * A worker bee that will process a block of households.
     *
     */
    public void onMessage(Message msg) {
        logger.info("********" + getName() + " received messageId=" + msg.getId() +
            " message from=" + msg.getSender() + " at " + new Date());

        if (msg.getId().equals(MessageID.PROCESS_HOUSEHOLDS)) {
                    householdBlockWorker(msg);
                }
        }
    /**
     * Process PT Models for one block of households
     * @param msg
     */
    public void householdBlockWorker(Message msg) {
        
        logger.fine(getName() + " free memory before running model: " +
            Runtime.getRuntime().freeMemory());

        PTHousehold[] households = (PTHousehold[]) msg.getValue("households");

        logger.info(getName() + " working on " + households.length + " households");

        //Run models for weekdays
        long startTime = System.currentTimeMillis();
        logger.info("\t"+ getName() + " running Weekday Pattern Model");
        households = ptModel.runWeekdayPatternModel(households);
        logger.fine("\t"+ getName() + " Time to run Weekday Pattern Model for " + households.length +
            " households = " +
            ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

        startTime = System.currentTimeMillis();
        logger.info("\t"+ getName() + " generating Tours based on Weekday,Weekend Patterns");
        households = ptModel.generateWeekdayTours(households);
        logger.fine("\t"+ getName() + " time to generate Weekday Tours for " + households.length +
            " households = " +
            ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");

        startTime = System.currentTimeMillis();
        logger.info("\t"+ getName() + " getting Logsums and Running WeekdayDurationDestinationModeChoiceModel for HH block");
        for (int i = 0; i < households.length; i++) {
            logger.fine("\t"+ getName() + " processing household: " + households[i].ID);
            int thisNonWorkSegment = households[i].calcNonWorkLogsumSegment();
            int thisWorkBasedSegment = households[i].calcWorkLogsumSegment();

            expUtilitiesManager.updateExpUtilities(thisWorkBasedSegment,thisNonWorkSegment);

            logger.fine("\t\t"+ getName() + " running WeekdayDurationDestinationModeChoiceModel");
            households[i] = (PTHousehold) ptModel.runWeekdayDurationDestinationModeChoiceModels(households[i],
                expUtilitiesManager).get(0);
        }

        logger.info("\t"+ getName() + " time to run duration destination mode choice model for " +
            households.length + " households = " +
            ((System.currentTimeMillis()-startTime) / 1000.0) + " seconds");

        if (PTModel.RUN_WEEKEND_MODEL) {
            logger.info("\t"+ getName() + " running Weekend Models");
            //Run models for weekends
            households = ptModel.runWeekendPatternModel(households);
            households = ptModel.generateWeekendTours(households);

            for (int i = 0; i < households.length; i++) {
                households[i] = (PTHousehold) ptModel.runWeekdayDurationDestinationModeChoiceModels(households[i],
                    expUtilitiesManager).get(0);
            }
        }

        //done, send results to queue
        logger.fine("Sending message to results queue.");
        msg.setId(MessageID.HOUSEHOLDS_PROCESSED);
        msg.setValue("households", households);
        sendTo("TaskMasterQueue", msg);
        logger.fine("Free memory after running model: " +
            Runtime.getRuntime().freeMemory());
    }

}

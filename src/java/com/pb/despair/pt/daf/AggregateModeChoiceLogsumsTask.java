package com.pb.despair.pt.daf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.Date;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.pt.ActivityPurpose;
import com.pb.despair.pt.CreateModeChoiceLogsums;
import com.pb.despair.pt.LogsumManager;
import com.pb.despair.pt.PTModelInputs;
import com.pb.despair.pt.TourModeChoiceModel;
import com.pb.despair.pt.TourModeParameters;

/**
 * AggregateModeChoiceLogsumsTask
 *
 * This class builds aggregate mode choice logsums for subsequent steps of the pt model and other
 * TLUMIP model components.
 * 
 * @author Freedman
 * @version Aug 10, 2004
 * 
 */
public class AggregateModeChoiceLogsumsTask  extends MessageProcessingTask {
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.daf");
    protected static Object lock = new Object();
    protected static ResourceBundle rb;
    protected static boolean initialized = false;
    CreateModeChoiceLogsums mcLogsumCalculator = new CreateModeChoiceLogsums();
    TourModeChoiceModel tmcm = new TourModeChoiceModel();
    LogsumManager logsumManager;
    String fileWriterQueue = "FileWriterQueue";

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
                String pathToRb = null;
                try {
                    logger.info("Reading RunParams.txt file");
                    reader = new BufferedReader(new FileReader(new File("/models/tlumip/daf/RunParams.txt")));
                    timeInterval = Integer.parseInt(reader.readLine());
                    logger.info("\tTime Interval: " + timeInterval);
                    pathToRb = reader.readLine();
                    logger.info("\tResourceBundle Path: " + pathToRb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                rb = ResourceUtil.getPropertyBundle(new File(pathToRb));

                PTModelInputs ptInputs = new PTModelInputs(rb);
                logger.info("Setting up the aggregate mode choice model");
                ptInputs.setSeed(2002);
                ptInputs.getParameters();
                ptInputs.readSkims();
                ptInputs.readTazData();
                initialized = true;
            }

            logsumManager = new LogsumManager(rb);
            logger.info( "***" + getName() + " finished onStart()");
        }
    }
    /**
     * A worker bee that will process a block of households.
     *
     */
    public void onMessage(Message msg) {
        logger.info("********" + getName() + " received messageId=" + msg.getId() +
            " message from=" + msg.getSender() + " at " + new Date());

        if (msg.getId().equals(MessageID.CREATE_MC_LOGSUMS))
            createMCLogsums(msg);
    }      
    
    /**
     * The work happens here: create mode choice logsums for the market segment and 
     * purpose in the message.
     * 
     * @param msg
     */
    public void createMCLogsums(Message msg) {
        logger.fine("Free memory before creating MC logsum: " +
            Runtime.getRuntime().freeMemory());

        String purpose = String.valueOf(msg.getValue("purpose"));
        Integer segment = (Integer) msg.getValue("segment");

        //Creating the ModeChoiceLogsum Matrix
        logger.info("Creating ModeChoiceLogsumMatrix for purpose: " + purpose +
            " segment: " + segment);

        TourModeParameters theseParameters = (TourModeParameters) PTModelInputs.tmpd.getTourModeParameters(ActivityPurpose.getActivityPurposeValue(
                    purpose.charAt(0)));
        long startTime = System.currentTimeMillis();
        Matrix m = mcLogsumCalculator.setModeChoiceLogsumMatrix(PTModelInputs.tazs,
                theseParameters, purpose.charAt(0), segment.intValue(),
                PTModelInputs.getSkims(), tmcm);
        logger.fine("Created ModeChoiceLogsumMatrix in " +
            ((System.currentTimeMillis() - startTime) / 1000) + " seconds.");

        //Sending message to TaskMasterQueue
        msg.setId(MessageID.MC_LOGSUMS_CREATED);
        msg.setValue("matrix", m);
        sendTo(fileWriterQueue, msg);
        m=null;
    }

}

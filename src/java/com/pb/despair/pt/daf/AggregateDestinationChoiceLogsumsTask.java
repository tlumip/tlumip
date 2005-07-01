package com.pb.tlumip.pt.daf;


import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.model.ModeChoiceLogsums;
import com.pb.tlumip.pt.CreateDestinationChoiceLogsums;
import com.pb.tlumip.pt.PTModelInputs;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * AggregateDestinationChoiceLogsumsTask
 *
 * @author Freedman
 * @version Aug 10, 2004
 * 
 */
public class AggregateDestinationChoiceLogsumsTask  extends MessageProcessingTask {
    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.daf");
    protected static Object lock = new Object();
    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;
    protected static boolean initialized = false;
    boolean firstDCLogsum = true;
    CreateDestinationChoiceLogsums dcLogsumCalculator = new CreateDestinationChoiceLogsums();
//    protected static WorkLogsumMap logsumMap = new WorkLogsumMap();
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

                PTModelInputs ptInputs = new PTModelInputs(ptRb,globalRb);
                logger.info("Setting up the aggregate mode choice model");
                ptInputs.setSeed(2002);
                ptInputs.getParameters();
                ptInputs.readSkims();
                ptInputs.readTazData();

                initialized = true;
            }

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

         if (msg.getId().equals(MessageID.CREATE_DC_LOGSUMS)) 
            createDCLogsums(msg);
    }
    /**
     * Create destination choice aggregate logsums
     * @param msg
     */
    public void createDCLogsums(Message msg){
        if (firstDCLogsum) {
            logger.info(getName() + " is setting population, school occupation and collapsing employment " +
                    "in the ptModel tazs");
            dcLogsumCalculator.buildModel(PTModelInputs.tazs, PTModelInputs.getSkims());
            firstDCLogsum=false;
        }

        String purpose = String.valueOf(msg.getValue("purpose"));
        Integer segment = (Integer) msg.getValue("segment");

        String path = ResourceUtil.getProperty(ptRb, "mcLogsum.path");
        ModeChoiceLogsums mcl = new ModeChoiceLogsums(ptRb);
        mcl.readLogsums(purpose.charAt(0),segment.intValue());
        Matrix modeChoiceLogsum =mcl.getMatrix();

        if (purpose.equals("c")) {
            for (int i = 1; i <= 3; i++) {
                logger.info(getName() + " is calculating the DC Logsums for purpose c, market segment " + segment + " subpurpose " + i);
                
                //create a message to store the dc logsum vector
                Message dcLogsumMessage = createMessage();
                dcLogsumMessage.setId(MessageID.DC_LOGSUMS_CREATED);

                String dcPurpose = "c" + i;
                Matrix dcLogsumMatrix = (Matrix) dcLogsumCalculator.getDCLogsumVector(PTModelInputs.tazs,
                		PTModelInputs.tdpd, dcPurpose, segment.intValue(), modeChoiceLogsum);
                dcLogsumMessage.setValue("matrix", dcLogsumMatrix);
                sendTo(fileWriterQueue, dcLogsumMessage);
                
                //get the exponentiated utilities matrix and put it in another message
                Message dcExpUtilitiesMessage = createMessage();
                dcExpUtilitiesMessage.setId(MessageID.DC_EXPUTILITIES_CREATED);
                Matrix expUtilities = dcLogsumCalculator.getExpUtilities(dcPurpose, segment.intValue());
                dcExpUtilitiesMessage.setValue("matrix", expUtilities);
                sendTo(fileWriterQueue, dcExpUtilitiesMessage);
                
            }
        } else if (!purpose.equals("w")) {
            logger.info(getName() + " is calculating the DC Logsums for purpose " + purpose + ", market segment " + segment + " subpurpose 1");
            Message dcLogsumMessage = createMessage();
            dcLogsumMessage.setId(MessageID.DC_LOGSUMS_CREATED);
            Matrix dcLogsumMatrix = (Matrix) dcLogsumCalculator.getDCLogsumVector(PTModelInputs.tazs,
            		PTModelInputs.tdpd, purpose, segment.intValue(), modeChoiceLogsum);
            dcLogsumMessage.setValue("matrix", dcLogsumMatrix);
            sendTo(fileWriterQueue, dcLogsumMessage);
            
            //get the exponentiated utilities matrix and put it in another message
            Message dcExpUtilitiesMessage = createMessage();
            dcExpUtilitiesMessage.setId(MessageID.DC_EXPUTILITIES_CREATED);
            Matrix expUtilities = dcLogsumCalculator.getExpUtilities(purpose, segment.intValue());
            dcExpUtilitiesMessage.setValue("matrix", expUtilities);
            sendTo(fileWriterQueue, dcExpUtilitiesMessage);

        }

        modeChoiceLogsum = null;
    }

}

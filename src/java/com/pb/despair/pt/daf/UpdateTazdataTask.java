package com.pb.despair.pt.daf;

/**
 * UpdateTazdataTask
 *
 * @author Freedman
 * @version Aug 11, 2004
 * 
 */


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.Date;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.pt.PTModelInputs;

public class UpdateTazdataTask extends MessageProcessingTask{
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.daf");
    protected static Object lock = new Object();
    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;
    protected static boolean initialized = false;
    String fileWriterQueue = "FileWriterQueue";

    //these arrays are to store the information necessary to update PTModelNew.tazData
    public static int[] householdsByTaz;
    public static int[] postSecOccup;
    public static int[] otherSchoolOccup;

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

        if (msg.getId().equals(MessageID.UPDATE_TAZDATA)){
                    updateTazData(msg);
                }
    }

        public void updateTazData(Message msg){
            logger.info("Updating TAZ info");
            householdsByTaz = new int[((int[])msg.getValue("householdsByTaz")).length];
            for(int i=0;i<((int[])msg.getValue("householdsByTaz")).length;i++){
                householdsByTaz[i] = ((int[])msg.getValue("householdsByTaz"))[i];
            }
            postSecOccup = new int[((int[])msg.getValue("postSecOccup")).length];
            for(int i=0;i<((int[])msg.getValue("postSecOccup")).length;i++){
                postSecOccup[i] = ((int[])msg.getValue("postSecOccup"))[i];
            }
            otherSchoolOccup = new int[((int[])msg.getValue("otherSchoolOccup")).length];
            for(int i=0;i<((int[])msg.getValue("otherSchoolOccup")).length;i++){
                otherSchoolOccup[i] = ((int[])msg.getValue("otherSchoolOccup"))[i];
            }

            PTModelInputs.tazs.setPopulation(householdsByTaz);
            PTModelInputs.tazs.setSchoolOccupation(otherSchoolOccup,postSecOccup);
            PTModelInputs.tazs.collapseEmployment(PTModelInputs.tdpd,PTModelInputs.sdpd);

            Message tazUpdatedMsg = createMessage();
            tazUpdatedMsg.setId(MessageID.TAZDATA_UPDATED);
            sendTo("TaskMasterQueue",tazUpdatedMsg);
        }

}

package com.pb.despair.pt.daf;

/**
 * WorkplaceLocationTask
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
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.TableDataSetLoader;
import com.pb.despair.model.ModeChoiceLogsums;
import com.pb.despair.pt.LaborFlows;
import com.pb.despair.pt.LogsumManager;
import com.pb.despair.pt.PTModelInputs;
import com.pb.despair.pt.PTPerson;
import com.pb.despair.pt.WorkplaceLocationModel;

public class WorkplaceLocationTask  extends MessageProcessingTask{

    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.daf");
    protected static Object lock = new Object();
    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;
    protected static boolean initialized = false;
    String fileWriterQueue = "FileWriterQueue";
    LogsumManager logsumManager;
//    protected static WorkLogsumMap logsumMap = new WorkLogsumMap();
    WorkplaceLocationModel workLocationModel = new WorkplaceLocationModel();

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

                PTModelInputs ptInputs = new PTModelInputs(ptRb, globalRb);
                logger.info("Setting up the workplace model");
                ptInputs.setSeed(2002);
                ptInputs.getParameters();
                ptInputs.readSkims();
                ptInputs.readTazData();
                LaborFlows lf = new LaborFlows(ptRb);
                lf.setZoneMap(TableDataSetLoader.loadTableDataSet(globalRb,"alpha2beta.file"));
                lf.readAlphaValues(TableDataSetLoader.loadTableDataSet(ptRb,"productionValues.file"),
                    TableDataSetLoader.loadTableDataSet(ptRb,"consumptionValues.file"));
                lf.readBetaLaborFlows();
                initialized = true;
            }

            logsumManager = new LogsumManager(ptRb);
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

        if (msg.getId().equals(MessageID.CALCULATE_WORKPLACE_LOCATIONS)) {
                    createLaborFlowMatrix(msg);
                } 
    }

        /**
         * Create labor flow matrices for a particular occupation, hh segment, and person array
         * @param msg
         */
        public void createLaborFlowMatrix(Message msg) {
            //getting message information
            logger.fine("Free memory before creating labor flow matrix: " +
                Runtime.getRuntime().freeMemory());


            Integer occupation = (Integer) msg.getValue("occupation");
            Integer segment = (Integer) msg.getValue("segment");
            PTPerson[] persons = (PTPerson[]) msg.getValue("persons");

            ModeChoiceLogsums mcl = new ModeChoiceLogsums(ptRb);
            mcl.readLogsums('w',segment.intValue());
            Matrix modeChoiceLogsum =mcl.getMatrix();

            //Write Labor Flow Probabilities to Disk
            logger.info("Calculating AZ Labor flows.");

            Matrix m = LaborFlows.calculateAlphaLaborFlowsMatrix(modeChoiceLogsum,
                    segment.intValue(), occupation.intValue());
            persons = calculateWorkplaceLocation(persons, m);

            Message laborFlowMessage = createMessage();
            laborFlowMessage.setId(MessageID.WORKPLACE_LOCATIONS_CALCULATED);
            laborFlowMessage.setValue("persons", persons);
            sendTo("TaskMasterQueue", laborFlowMessage);
            logger.fine("Free memory after creating labor flow matrix: " +
                Runtime.getRuntime().freeMemory());
            m = null;
        }

        /**
         * Calculate workplace locations for the array of persons given the logsum accessibility matrix
         * @param persons
         * @param logsumMatrix
         * @return
         */
        public PTPerson[] calculateWorkplaceLocation(PTPerson[] persons,Matrix logsumMatrix) {

            for (int p = 0; p < persons.length; p++) {
                if (persons[p].employed) {
                    persons[p].workTaz = workLocationModel.chooseWorkplace(logsumMatrix,
                            persons[p], PTModelInputs.tazs);

                    if (persons[p].worksTwoJobs) {
                        persons[p].workTaz2 = workLocationModel.chooseWorkplace(logsumMatrix,
                                persons[p], PTModelInputs.tazs);
                    }
                }
            }
            return persons;
        }


}

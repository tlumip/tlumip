/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tlumip.pt.daf;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.daf.LocalMessageQueuePort;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.model.ModeChoiceLogsums;
import com.pb.tlumip.pt.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;


/**
 * HouseholdWorker processes all messages sent by PTDafMaster
 *
 *
 * @author    Christi Willison
 * @version   3.0, 3/8/2005
 *
 */
public class HouseholdWorker extends MessageProcessingTask {
    Logger ptLogger = Logger.getLogger(HouseholdWorker.class);
    protected static Object lock = new Object();
    protected static boolean initialized = false;
    protected static boolean dcLoaded = false;
    protected static boolean CALCULATE_MCLOGSUMS = true;
    protected static ArrayList matricesToCollapse;
    protected static boolean CALCULATE_DCLOGSUMS = true;
    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;

    //these arrays are to store the information necessary to update PTModelNew.tazData
    public static int[] householdsByTaz;
    public static int[] postSecOccup;
    public static int[] otherSchoolOccup;

    static AlphaToBeta a2b = null;

    CreateModeChoiceLogsums mcLogsumCalculator;
    CreateDestinationChoiceLogsums dcLogsumCalculator;
    WorkplaceLocationModel workLocationModel;
    DCExpUtilitiesManager expUtilitiesManager;
    PTModel ptModel;
    PTResults ptResults; //used by models to write debug info if model is unsuccessful


    boolean firstHouseholdBlock = true;
    boolean firstDCLogsum = true;
    
    String matrixWriterQueue = "MatrixWriterQueue1";
    double durationTime;
    double primaryTime;
    double secondaryTime;
    double destZoneTime;
    double stopZoneTime;
    double tripModeTime;

    int currentWorkSegment = -1;
    int currentNonWorkSegment =-1;
    

    /**
     * Onstart method sets up model
     */
    public void onStart() {
        synchronized (lock) {
            ptLogger.info(getName() + ", Started");
            if (!initialized) {
                ptLogger.info(getName() + ", Initializing PT Model on Node");
                //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.properties file
                //that was written by the Application Orchestrator
                String scenarioName = null;
                int timeInterval = -1;
                String pathToPtRb = null;
                String pathToGlobalRb = null;
                
                ptLogger.info(getName() + ", Reading RunParams.properties file");
                ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
                scenarioName = ResourceUtil.getProperty(runParamsRb,"scenarioName");
                ptLogger.info(getName() + ", Scenario Name: " + scenarioName);
                timeInterval = Integer.parseInt(ResourceUtil.getProperty(runParamsRb,"timeInterval"));
                ptLogger.info(getName() + ", Time Interval: " + timeInterval);
                pathToPtRb = ResourceUtil.getProperty(runParamsRb,"pathToAppRb");
                ptLogger.info(getName() + ", ResourceBundle Path: " + pathToPtRb);
                pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,"pathToGlobalRb");
                ptLogger.info(getName() + ", ResourceBundle Path: " + pathToGlobalRb);
                
                ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
                globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));
                
                //set whether you want to calculate the dc and mode choice logsums
                //in production mode these should always be true.
                String dcLogsumBoolean = ResourceUtil.getProperty(ptRb, "calculate.dc.logsums");
                if(dcLogsumBoolean != null){
                    CALCULATE_DCLOGSUMS = new Boolean(dcLogsumBoolean).booleanValue();
                } //otherwise it has already been initialized to true.
                String mcLogsumBoolean = ResourceUtil.getProperty(ptRb, "calculate.mc.logsums");
                if(mcLogsumBoolean != null){
                    CALCULATE_MCLOGSUMS = new Boolean(mcLogsumBoolean).booleanValue();
                } //otherwise it has already been initialized to true

                //get the list of Matrices to collapse from properties file
                matricesToCollapse = ResourceUtil.getList(ptRb,"matrices.for.pi");

                PTModelInputs ptInputs = new PTModelInputs(ptRb,globalRb);
                ptLogger.info(getName() + ", Setting seed");
                ptInputs.setSeed(2002);
                ptLogger.info(getName() + ", Reading parameter files");
                ptInputs.getParameters();
                ptLogger.info(getName() + ", Reading skims into memory");
                ptInputs.readSkims();
                ptLogger.info(getName() + ", Reading taz data into memory");
                ptInputs.readTazData();

                LaborFlows lf = new LaborFlows(ptRb);
                TableDataSet alphaToBetaTable = loadTableDataSet(globalRb,"alpha2beta.file");
                a2b = new AlphaToBeta(alphaToBetaTable);
                lf.setZoneMap(alphaToBetaTable);

                ptLogger.info(getName() + ", Reading Labor Flows");
                lf.readAlphaValues(loadTableDataSet(ptRb,"productionValues.file"),
                                   loadTableDataSet(ptRb,"consumptionValues.file"));

                lf.readBetaLaborFlows();
                initialized = true;
                ptLogger.info(getName() + ", Finished initializing");
            }
            mcLogsumCalculator = new CreateModeChoiceLogsums();
            dcLogsumCalculator = new CreateDestinationChoiceLogsums();
            workLocationModel = new WorkplaceLocationModel(ptRb);
            expUtilitiesManager = new DCExpUtilitiesManager(ptRb);

            ptModel = new PTModel(ptRb, globalRb);
            ptResults = new PTResults(ptRb);
            
            //establish a connection between the workers
            //and the server and the file writer.
            Message masterInitMsg = mFactory.createMessage();
            masterInitMsg.setId("init");
            sendTo("TaskMasterQueue",masterInitMsg);
            Message mWriterInitMsg = mFactory.createMessage();
            mWriterInitMsg.setId("init");
            sendTo(matrixWriterQueue, mWriterInitMsg);
            Message rWriterInitMsg = mFactory.createMessage();
            rWriterInitMsg.setId("init");
            sendTo("ResultsWriterQueue", rWriterInitMsg);
            
            ptLogger.info(getName() + ", Finished onStart()");
//            
        }
    }

    /**
     * A worker bee that will process a block of households.
     *
     */
    public void onMessage(Message msg) {
        ptLogger.info(getName() + ", Received messageId=" + msg.getId() +
            " message from=" + msg.getSender() + " at " + new Date());

        if (msg.getId().equals(MessageID.CREATE_MC_LOGSUMS)) {
            if(CALCULATE_MCLOGSUMS)createMCLogsums(msg);
            else {
                msg.setId(MessageID.MC_LOGSUMS_CREATED);
                msg.setValue("matrix",null);
                sendTo(matrixWriterQueue, msg);

                String purpose = String.valueOf(msg.getValue("purpose"));
                Integer segment = (Integer) msg.getValue("segment");
                String purSeg = purpose + segment.toString();

                if (matricesToCollapse.contains(purSeg)) {
                    Message collapsedMsg = createMessage();
                    collapsedMsg.setId(MessageID.MC_LOGSUMS_COLLAPSED);
                    collapsedMsg.setValue("matrix",null);
                    sendTo(matrixWriterQueue, collapsedMsg);
                }
             }
        } else if (msg.getId().equals(MessageID.CALCULATE_WORKPLACE_LOCATIONS)) {
            createLaborFlowMatrix(msg);
        } else if (msg.getId().equals(MessageID.UPDATE_TAZDATA)){
            updateTazData(msg);
        } else if (msg.getId().equals(MessageID.CREATE_DC_LOGSUMS)) {
            if(CALCULATE_DCLOGSUMS) createDCLogsums(msg);
            else {
                if(String.valueOf(msg.getValue("purpose")).equals("c")){
                    for (int i = 1; i <= 3; i++) {
                        Message dcSchoolMessage = createMessage();
                        dcSchoolMessage.setId(MessageID.DC_LOGSUMS_CREATED);
                        dcSchoolMessage.setValue("matrix", null);
                        sendTo(matrixWriterQueue, dcSchoolMessage);

                        Message dcExpUtilitiesMessage = createMessage();
                        dcExpUtilitiesMessage.setId(MessageID.DC_EXPUTILITIES_CREATED);
                        dcExpUtilitiesMessage.setValue("matrix", null);
                        sendTo(matrixWriterQueue, dcExpUtilitiesMessage);
                    }
                }
                else {
                    Message dcMessage = createMessage();
                    dcMessage.setId(MessageID.DC_LOGSUMS_CREATED);
                    dcMessage.setValue("matrix", null);
                    sendTo(matrixWriterQueue, dcMessage);

                    Message dcExpUtilitiesMessage = createMessage();
                    dcExpUtilitiesMessage.setId(MessageID.DC_EXPUTILITIES_CREATED);
                    dcExpUtilitiesMessage.setValue("matrix", null);
                    sendTo(matrixWriterQueue, dcExpUtilitiesMessage);
                }
            }


        } else if (msg.getId().equals(MessageID.PROCESS_HOUSEHOLDS)) {
            if(ptLogger.isDebugEnabled()) {
                ptLogger.debug(getName() + ", Received HH Block, " + (Integer)msg.getValue("blockNumber"));
                if (defaultPort instanceof LocalMessageQueuePort){
                    ptLogger.debug(getName()+ ", Msgs in Queue " + ((LocalMessageQueuePort)defaultPort).getSize());
                }
            }
            householdBlockWorker(msg);
        } else{
            //do nothing - it was just an init message to establish the connection.
        }
    }

    public void createMCLogsums(Message msg) {
        String purpose = String.valueOf(msg.getValue("purpose"));
        Integer segment = (Integer) msg.getValue("segment");

        String purSeg = purpose + segment.toString();

        //Creating the ModeChoiceLogsum Matrix
        ptLogger.info(getName() + ", Creating ModeChoiceLogsumMatrix for purpose: " + purpose +
            " segment: " + segment);

        TourModeParameters theseParameters = (TourModeParameters) PTModelInputs.tmpd.getTourModeParameters(ActivityPurpose.getActivityPurposeValue(
                    purpose.charAt(0)));
        long startTime = System.currentTimeMillis();
        Matrix m = mcLogsumCalculator.setModeChoiceLogsumMatrix(PTModelInputs.tazs,
                theseParameters, purpose.charAt(0), segment.intValue(),
                PTModelInputs.getSkims(), new TourModeChoiceModel());
//        if(ptLogger.isDebugEnabled()) {
//            ptLogger.debug(getName() + ", Created ModeChoiceLogsumMatrix in " +
//            ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
//        }

        //Collapse the required matrices

        if (matricesToCollapse.contains(purSeg)) {
            ptLogger.info(getName() + ", Collapsing ModeChoiceLogsumMatrix for purpose: " + purpose +
                " segment: " + segment);
            collapseMCLogsums(m,a2b,matrixWriterQueue);
        }
        
        //Sending message to TaskMasterQueue
        msg.setId(MessageID.MC_LOGSUMS_CREATED);
        msg.setValue("matrix", m);
        try {
            sendTo(matrixWriterQueue, msg);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(10);
        }
    }
    
    /**
     * Collapse the logsums in the alpha zone matrix to beta zones.  The
     * collapsed matrix will be send to the matrixWriterQueue.
     * 
     * @param m  Logsum matrix
     * @param a2b AlphaToBeta mapping
     */
    public void collapseMCLogsums(Matrix m, AlphaToBeta a2b, String queueName){
        MatrixCompression mc = new MatrixCompression(a2b);
            
        Matrix compressedMatrix = mc.getCompressedMatrix(m,"MEAN");

        //Need to do a little work to get only the purpose/segment part out of the name
        String newName = m.getName();
        newName = newName.replaceAll("ls","betals");
//        if(ptLogger.isDebugEnabled()) {
//            ptLogger.debug(getName() + ", Old name: " + m.getName() + " New name: " + newName);
//        }
        compressedMatrix.setName(newName);
            
        //Sending message to TaskMasterQueue
        Message msg = createMessage();
        msg.setId(MessageID.MC_LOGSUMS_COLLAPSED);
        msg.setValue("matrix", compressedMatrix);
        try {
            sendTo(queueName, msg);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    /**
     * Create labor flow matrices for a particular occupation, hh segment, and person array
     * @param msg
     */
    public void createLaborFlowMatrix(Message msg) {
       
        //getting message information
        Integer occupation = (Integer) msg.getValue("occupation");
        Integer segment = (Integer) msg.getValue("segment");
        PTPerson[] persons = (PTPerson[]) msg.getValue("persons");
        
        ptLogger.info(getName() + ", Creating Labor Flow Matrix");
        if(ptLogger.isDebugEnabled()){
            ptLogger.debug(getName() + ", segment " + segment);
            ptLogger.debug(getName() + ", occupation " + occupation);
            ptLogger.debug(getName() + ", num persons " + persons.length);
//          ptLogger.debug(getName() + ", Free memory before creating labor flow matrix: " +
//          Runtime.getRuntime().freeMemory());
        }

        ModeChoiceLogsums mcl = new ModeChoiceLogsums(ptRb);
//        ptLogger.info(getName() + ", Reading logsum w"+ segment.intValue() + "ls.zip" );//BINARY-ZIP
//        mcl.readLogsums('w',segment.intValue());        //BINARY-ZIP
        if(ptLogger.isDebugEnabled()) {
            ptLogger.debug(getName() + ", Reading logsum w"+ segment.intValue() + "ls.binary" );
        }
        mcl.readBinaryLogsums('w',segment.intValue());
        Matrix modeChoiceLogsum = mcl.getMatrix();
        
        Matrix m = LaborFlows.calculateAlphaLaborFlowsMatrix(modeChoiceLogsum,
                segment.intValue(), occupation.intValue());
        persons = calculateWorkplaceLocation(persons, m);

        Message laborFlowMessage = createMessage();
        laborFlowMessage.setId(MessageID.WORKPLACE_LOCATIONS_CALCULATED);
        laborFlowMessage.setValue("persons", persons);
        sendTo("TaskMasterQueue", laborFlowMessage);
//        if(ptLogger.isDebugEnabled()) {
//            ptLogger.debug(getName() + ", Free memory after creating labor flow matrix: " +
//            Runtime.getRuntime().freeMemory());
//        }
//        m = null; //NULL
    }

    /**
     * Calculate workplace locations for the array of persons given the logsum accessibility matrix
     * (This method does not change the PTModelInputs.tazs attribute so it is OK to use it)
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

    public void updateTazData(Message msg){
        ptLogger.info(getName() + ", Updating TAZ info");
        householdsByTaz = (int[]) msg.getValue("householdsByTaz");
        postSecOccup = (int[])msg.getValue("postSecOccup");
        otherSchoolOccup = (int[])msg.getValue("otherSchoolOccup");

//        if(ptLogger.isDebugEnabled()) {
//            ptLogger.debug(getName() + ", Setting population, school occupation and collapsing employment " 
//                    + "in the PTModelInputs tazs");
//        }
                    
        PTModelInputs.tazs.setPopulation(householdsByTaz);
        PTModelInputs.tazs.setSchoolOccupation(otherSchoolOccup,postSecOccup);
        PTModelInputs.tazs.collapseEmployment(PTModelInputs.tdpd,PTModelInputs.sdpd);

        Message tazUpdatedMsg = createMessage();
        tazUpdatedMsg.setId(MessageID.TAZDATA_UPDATED);
        sendTo("TaskMasterQueue",tazUpdatedMsg);
    }

    /**
     * Create destination choice aggregate logsums
     * @param msg
     */
    public void createDCLogsums(Message msg){
        if (firstDCLogsum) {
            ptLogger.info(getName() + ", Setting population, school occupation and collapsing employment " +
                    "in the ptModel tazs");
            dcLogsumCalculator.buildModel(PTModelInputs.tazs, PTModelInputs.getSkims());
            firstDCLogsum=false;
        }


        String purpose = String.valueOf(msg.getValue("purpose"));
        Integer segment = (Integer) msg.getValue("segment");

        ModeChoiceLogsums mcl = new ModeChoiceLogsums(ptRb);
//        mcl.readLogsums(purpose.charAt(0),segment.intValue());   //BINARY-ZIP
        mcl.readBinaryLogsums(purpose.charAt(0),segment.intValue());
        Matrix modeChoiceLogsum =mcl.getMatrix();

        try {
            if (purpose.equals("c")) {
                for (int i = 1; i <= 3; i++) {
                    ptLogger.info(getName() + ", Calculating the DC Logsums for purpose c, market segment " + segment + " subpurpose " + i);

                    //create a message to store the dc logsum vector
                    Message dcLogsumMessage = createMessage();
                    dcLogsumMessage.setId(MessageID.DC_LOGSUMS_CREATED);
//
                    String dcPurpose = "c" + i;
                    dcLogsumCalculator.createNewExpMatrix(PTModelInputs.tazs); //create a new matrix
                    Matrix dcLogsumMatrix = (Matrix) dcLogsumCalculator.getDCLogsumVector(PTModelInputs.tazs,
                    		PTModelInputs.tdpd, dcPurpose, segment.intValue(), modeChoiceLogsum);
                    dcLogsumMessage.setValue("matrix", dcLogsumMatrix);
                    sendTo(matrixWriterQueue, dcLogsumMessage);

//                get the exponentiated utilities matrix and put it in another message
                    Message dcExpUtilitiesMessage = createMessage();
                    dcExpUtilitiesMessage.setId(MessageID.DC_EXPUTILITIES_CREATED);
                    Matrix expUtilities = dcLogsumCalculator.getExpUtilities(dcPurpose, segment.intValue());
                    dcExpUtilitiesMessage.setValue("matrix", expUtilities);
//                    Thread.sleep((long)(Math.random() * 10));
                    sendTo(matrixWriterQueue, dcExpUtilitiesMessage);

                }
            } else if (!purpose.equals("w")) {
                ptLogger.info(getName() + ", Calculating the DC Logsums for purpose " + purpose + ", market segment " + segment + " subpurpose 1");
                Message dcLogsumMessage = createMessage();
                dcLogsumMessage.setId(MessageID.DC_LOGSUMS_CREATED);

                dcLogsumCalculator.createNewExpMatrix(PTModelInputs.tazs);
                Matrix dcLogsumMatrix = (Matrix) dcLogsumCalculator.getDCLogsumVector(PTModelInputs.tazs,
                		PTModelInputs.tdpd, purpose, segment.intValue(), modeChoiceLogsum);
                dcLogsumMessage.setValue("matrix", dcLogsumMatrix);
                sendTo(matrixWriterQueue, dcLogsumMessage);

                //get the exponentiated utilities matrix and put it in another message
                Message dcExpUtilitiesMessage = createMessage();
                dcExpUtilitiesMessage.setId(MessageID.DC_EXPUTILITIES_CREATED);
                Matrix expUtilities = dcLogsumCalculator.getExpUtilities(purpose, segment.intValue());
                dcExpUtilitiesMessage.setValue("matrix", expUtilities);
//                Thread.sleep((long)(Math.random() * 10));
                sendTo(matrixWriterQueue, dcExpUtilitiesMessage);
//             dcLogsumCalculator.writeDestinationChoiceExpUtilitiesMatrix(rb);     //BINARY-ZIP
//            dcLogsumCalculator.writeDestinationChoiceExpUtilitiesBinaryMatrix(rb);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
//        modeChoiceLogsum = null; //NULL
     }

    /**
     * Process PT Models for one block of households
     * @param msg
     */
    public void householdBlockWorker(Message msg) {
        
        //data management if first block of households for this worker
        if (firstHouseholdBlock) {
            loadDCLogsums();
            workLocationModel = null;
            ptModel.buildLogitModels();
            firstHouseholdBlock = false;
        }

//        ptLogger.debug(getName() + ", Free memory before running model: " +
//            Runtime.getRuntime().freeMemory());

        PTHousehold[] households = (PTHousehold[]) msg.getValue("households");

        ptLogger.info(getName() + ", Working on " + households.length + " households");

        //Run models for weekdays
        long startTime = System.currentTimeMillis();
        ptLogger.info(getName() + ", Running Weekday Pattern Model");
        households = ptModel.runWeekdayPatternModel(households);
        double patternTime = (System.currentTimeMillis() - startTime) / 1000.0;
//        if(ptLogger.isDebugEnabled()) {
//            ptLogger.debug(getName() + ", Time to run Weekday Pattern Model for " + households.length +
//                    " households = " + patternTime + " seconds");
//        }
            

        startTime = System.currentTimeMillis();
        ptLogger.info(getName() + ", Generating Tours based on Weekday,Weekend Patterns");
        households = ptModel.generateWeekdayTours(households);
        double tourTime = (System.currentTimeMillis() - startTime) / 1000.0;
//        if(ptLogger.isDebugEnabled()) {
//            ptLogger.debug(getName() + ", Time to generate Weekday Tours for " + households.length +
//                    " households = " + tourTime + " seconds");
//        }
            

        startTime = System.currentTimeMillis();
        double readTime = 0.0;
        durationTime = 0.0;
        primaryTime = 0.0;
        secondaryTime = 0.0;
        destZoneTime = 0.0;
        stopZoneTime = 0.0;
        tripModeTime = 0.0;

        List returnValues = new ArrayList();
        ptLogger.info(getName() + ", Getting Logsums and Running WeekdayDurationDestinationModeChoiceModel");
        int thisNonWorkSegment =-1; //need to initialize for ptLogger statment
        int thisWorkSegment = -1; //need to initialize for ptLogger statement

        int wSegmentChange=0;
        int nwSegmentChange=0;
        int[] reads;
        for (int i = 0; i < households.length; i++) {
            thisNonWorkSegment = households[i].calcNonWorkLogsumSegment();
            thisWorkSegment = households[i].calcWorkLogsumSegment();
            long tempReadTime = System.currentTimeMillis();
            reads = expUtilitiesManager.updateExpUtilities(thisWorkSegment,thisNonWorkSegment);
            wSegmentChange += reads[0];  //will return a '1' if there was a read, otherwise 0
            nwSegmentChange += reads[1];
            readTime += ((System.currentTimeMillis() - tempReadTime)/1000.0);

            //ptLogger.debug(getName() + ", Running WeekdayDurationDestinationModeChoiceModel");
            returnValues = ptModel.runWeekdayDurationDestinationModeChoiceModels(households[i],
                    expUtilitiesManager);
            households[i] = (PTHousehold) returnValues.get(0);
            incrementModelTime((double[]) returnValues.get(1));
            incrementPrimaryModelSubTimes((double[]) returnValues.get(2));

        }
        double loopTime = ((System.currentTimeMillis() - startTime) / 1000.0);
//        if(ptLogger.isDebugEnabled()) {
//            ptLogger.debug(getName() + ", Time to run duration destination mode choice model for " +
//            households.length + " households: \n\t\tLoop Time: " +
//            loopTime + " seconds" + "\n\t\tDurationModel: " + durationTime +
//			"\n\t\tPrimaryTime: " + primaryTime + "\n\t\tSecondaryTime: " + secondaryTime);
//        }


        if (PTModel.RUN_WEEKEND_MODEL) {
            ptLogger.info(getName() + ", Running Weekend Models");
            //Run models for weekends
            households = ptModel.runWeekendPatternModel(households);
            households = ptModel.generateWeekendTours(households);

            for (int i = 0; i < households.length; i++) {
                households[i] = (PTHousehold) ptModel.runWeekendDurationDestinationModeChoiceModels(households[i],
                        expUtilitiesManager);
            }
        }
        //Worker has processed all the households.  It will send to ResultsWriter for writing
        double totalTime = patternTime + tourTime + loopTime;
        if(ptLogger.isDebugEnabled()) {
            ptLogger.debug(getName() + ", TIMING,"+ totalTime + "," + patternTime + "," + tourTime + "," + readTime + "," +
                    + durationTime + "," + primaryTime + "," + destZoneTime + "," + stopZoneTime + "," + tripModeTime + ","
                    + secondaryTime + "," + wSegmentChange + ","
                    + nwSegmentChange);
        }

        //notify the master that the households have been processed.  The message might also
        //have a 'sendMore' request in it which the master will honor.
        
        Message returnMsg = createMessage();
        returnMsg.setId(MessageID.HOUSEHOLDS_PROCESSED);
        returnMsg.setValue("blockNumber", (Integer)msg.getValue("blockNumber"));
        returnMsg.setValue("nHHs", new Integer(households.length));
        returnMsg.setValue("sendMore",(Integer)msg.getValue("sendMore"));
        returnMsg.setValue("WorkQueue", (String)msg.getValue("WorkQueue"));
        returnMsg.setValue("households", households);
        
        if(ptLogger.isDebugEnabled()) {
            ptLogger.debug(getName() + ", Sending household block " + returnMsg.getValue("blockNumber") + " to results queue.");
        }
        
        sendTo("ResultsWriterQueue", returnMsg);
    }

    private void loadDCLogsums() {
        synchronized (lock) {
            if (!dcLoaded) {
                ptLogger.info(getName() + ", Reading DC Logsums");
                PTModelInputs.readDCLogsums(ptRb);
            }
            dcLoaded = true;
        }
    }

    private TableDataSet loadTableDataSet(ResourceBundle rb,String pathName) {
        String path = ResourceUtil.getProperty(rb, pathName);
        try {
            String fullPath = path;
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(fullPath));
            return table;
            
        } catch (IOException e) {
            ptLogger.fatal(getName() + ", Can't find input table "+path);
            e.printStackTrace();
        }
        return null;
    }

    private void incrementModelTime (double[] times){
        incrementDurationTime(times[0]);
        incrementPrimaryTime(times[1]);
        incrementSecondaryTime(times[2]);
    }

    private void incrementDurationTime(double durationSecs){
        durationTime+=durationSecs;
    }

    private void incrementPrimaryTime(double primarySecs){
    	primaryTime+=primarySecs;
    }

    private void incrementSecondaryTime(double secondarySecs){
    	secondaryTime+=secondarySecs;
    }

    private void incrementPrimaryModelSubTimes(double[] subTimes){
        destZoneTime += subTimes[0];
        stopZoneTime += subTimes[1];
        tripModeTime += subTimes[2];
    }


}

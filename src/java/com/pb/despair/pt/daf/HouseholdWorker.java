package com.pb.despair.pt.daf;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.MatrixCollection;
import com.pb.common.util.ResourceUtil;

import com.pb.despair.model.ModeChoiceLogsums;
import com.pb.despair.pt.*;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

import java.lang.Runtime;

import java.util.Date;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * HouseholdWorker processes all messages sent by PTDafMaster
 *
 *
 * @author    Steve Hansen
 * @version   1.0, 5/5/2004
 *
 */
public class HouseholdWorker extends MessageProcessingTask {
    protected static Logger logger = Logger.getLogger("com.pb.despair.pt.daf");
    protected static Object lock = new Object();
    protected static boolean initialized = false;
    protected static boolean dcLoaded = false;
    protected static boolean CALCULATE_MCLOGSUMS = true;
    protected static ArrayList matricesToCollapse;
    protected static boolean CALCULATE_DCLOGSUMS = true;
    protected static ResourceBundle rb;

    //these arrays are to store the information necessary to update PTModelNew.tazData
    public static int[] householdsByTaz;
    public static int[] postSecOccup;
    public static int[] otherSchoolOccup;
    
    CreateModeChoiceLogsums mcLogsumCalculator = new CreateModeChoiceLogsums();
    CreateDestinationChoiceLogsums dcLogsumCalculator = new CreateDestinationChoiceLogsums();
    WorkplaceLocationModel workLocationModel = new WorkplaceLocationModel();
    DCExpUtilitiesManager expUtilitiesManager;
    PTModel ptModel;

    Matrix logsumMatrix;
    ModeChoiceLogsums workBasedModeChoiceLogsums;
    MatrixCollection nonWorkModeChoiceLogsums;

    // These variables keep track of the Matrices currently in memory
    int lastWorkplaceLocationSegment = -1;
    int lastWorkSegment = -1;
    int lastNonWorkSegment = -1;
    
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
    
    static AlphaToBeta a2b = null;

    PTResults results;


    /**
     * Onstart method sets up model
     */
    public void onStart() {
        synchronized (lock) {
            logger.info( "***" + getName() + " started");
            if (!initialized) {
                //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.txt file
                //that was written by the Application Orchestrator
                BufferedReader reader = null;
                String scenarioName = null;
                int timeInterval = -1;
                String pathToRb = null;
                try {
                    logger.info("Reading RunParams.txt file");
                    reader = new BufferedReader(new FileReader(new File( Scenario.runParamsFileName )));
                    scenarioName = reader.readLine();
                    logger.info("\tScenario Name: " + scenarioName);
                    timeInterval = Integer.parseInt(reader.readLine());
                    logger.info("\tTime Interval: " + timeInterval);
                    pathToRb = reader.readLine();
                    logger.info("\tResourceBundle Path: " + pathToRb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                rb = ResourceUtil.getPropertyBundle(new File(pathToRb));

                //set whether you want to calculate the dc and mode choice logsums
                //in production mode these should always be true.
                String dcLogsumBoolean = ResourceUtil.getProperty(rb, "calculate.dc.logsums");
                if(dcLogsumBoolean != null){
                    CALCULATE_DCLOGSUMS = new Boolean(dcLogsumBoolean).booleanValue();
                } //otherwise it has already been initialized to true.
                String mcLogsumBoolean = ResourceUtil.getProperty(rb, "calculate.mc.logsums");
                if(mcLogsumBoolean != null){
                    CALCULATE_MCLOGSUMS = new Boolean(mcLogsumBoolean).booleanValue();
                } //otherwise it has already been initialized to true

                //get the list of Matrices to collapse from properties file
                matricesToCollapse = ResourceUtil.getList(rb,"matrices.for.pi");

                PTModelInputs ptInputs = new PTModelInputs(rb);
                logger.info("Setting up the model");
                logger.info("\tsetting seed");
                ptInputs.setSeed(2002);
//                logger.info("\treading patterns files");   //not using the static copy at the moment 9/23/04
//                ptInputs.getPatterns();
                logger.info("\treading parameter files");
                ptInputs.getParameters();
                logger.info("\treading skims into memory");
                ptInputs.readSkims();
                logger.info("\treading taz data into memory");
                ptInputs.readTazData();

                LaborFlows lf = new LaborFlows(rb);
                TableDataSet alphaToBetaTable = loadTableDataSet(rb,"alphatobeta.file");
                a2b = new AlphaToBeta(alphaToBetaTable);
                lf.setZoneMap(alphaToBetaTable);

                logger.info("Reading Labor Flows");
                lf.readAlphaValues(loadTableDataSet(rb,"productionValues.file"),
                                   loadTableDataSet(rb,"consumptionValues.file"));

                lf.readBetaLaborFlows();
                initialized = true;
            }

            expUtilitiesManager = new DCExpUtilitiesManager(rb);

            ptModel = new PTModel(rb);

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
                String purpose = String.valueOf(msg.getValue("purpose"));
                Integer segment = (Integer) msg.getValue("segment");
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
            householdBlockWorker(msg);
        }
    }

    public void createMCLogsums(Message msg) {
        logger.fine("Free memory before creating MC logsum: " +
            Runtime.getRuntime().freeMemory());

        String purpose = String.valueOf(msg.getValue("purpose"));
        Integer segment = (Integer) msg.getValue("segment");

        String purSeg = purpose + segment.toString();

        //Creating the ModeChoiceLogsum Matrix
        logger.info("Creating ModeChoiceLogsumMatrix for purpose: " + purpose +
            " segment: " + segment);

        TourModeParameters theseParameters = (TourModeParameters) PTModelInputs.tmpd.getTourModeParameters(ActivityPurpose.getActivityPurposeValue(
                    purpose.charAt(0)));
        long startTime = System.currentTimeMillis();
        Matrix m = mcLogsumCalculator.setModeChoiceLogsumMatrix(PTModelInputs.tazs,
                theseParameters, purpose.charAt(0), segment.intValue(),
                PTModelInputs.getSkims(), new TourModeChoiceModel());
        logger.fine("Created ModeChoiceLogsumMatrix in " +
            ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
        
        //Collapse the required matrices

        if (matricesToCollapse.contains(purSeg)) {
            logger.info("Collapsing ModeChoiceLogsumMatrix for purpose: " + purpose +
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
        logger.info("Old name: " + m.getName() + " New name: " + newName);
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
        logger.fine("Free memory before creating labor flow matrix: " +
            Runtime.getRuntime().freeMemory());


        Integer occupation = (Integer) msg.getValue("occupation");
        Integer segment = (Integer) msg.getValue("segment");
        PTPerson[] persons = (PTPerson[]) msg.getValue("persons");

        ModeChoiceLogsums mcl = new ModeChoiceLogsums(rb);
//        logger.info("\t\t" + getName() + ": Reading logsum w"+ segment.intValue() + "ls.zip" );//BINARY-ZIP
//        mcl.readLogsums('w',segment.intValue());        //BINARY-ZIP
        logger.info("\t\t" + getName() + ": Reading logsum w"+ segment.intValue() + "ls.binary" );
        mcl.readBinaryLogsums('w',segment.intValue());
        Matrix modeChoiceLogsum = mcl.getMatrix();
        
        logger.info("\t\t" + getName() + " is calculating AZ Labor flows.");

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
        logger.info("Updating TAZ info");
        householdsByTaz = (int[]) msg.getValue("householdsByTaz");
        postSecOccup = (int[])msg.getValue("postSecOccup");
        otherSchoolOccup = (int[])msg.getValue("otherSchoolOccup");

        logger.info(getName() + " is setting population, school occupation and collapsing employment " +
                    "in the PTModelInputs tazs");
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
            logger.info(getName() + " is setting population, school occupation and collapsing employment " +
                    "in the ptModel tazs");
            dcLogsumCalculator.buildModel(PTModelInputs.tazs, PTModelInputs.getSkims());
            firstDCLogsum=false;
        }


        String purpose = String.valueOf(msg.getValue("purpose"));
        Integer segment = (Integer) msg.getValue("segment");

        String path = ResourceUtil.getProperty(rb, "mcLogsum.path");
        ModeChoiceLogsums mcl = new ModeChoiceLogsums(rb);
//        mcl.readLogsums(purpose.charAt(0),segment.intValue());   //BINARY-ZIP
        mcl.readBinaryLogsums(purpose.charAt(0),segment.intValue());
        Matrix modeChoiceLogsum =mcl.getMatrix();

        try {
            if (purpose.equals("c")) {
                for (int i = 1; i <= 3; i++) {
                    logger.info(getName() + " is calculating the DC Logsums for purpose c, market segment " + segment + " subpurpose " + i);

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
                    sendTo(matrixWriterQueue, dcExpUtilitiesMessage);

                }
            } else if (!purpose.equals("w")) {
                logger.info(getName() + " is calculating the DC Logsums for purpose " + purpose + ", market segment " + segment + " subpurpose 1");
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
                sendTo(matrixWriterQueue, dcExpUtilitiesMessage);
//             dcLogsumCalculator.writeDestinationChoiceExpUtilitiesMatrix(rb);     //BINARY-ZIP
//            dcLogsumCalculator.writeDestinationChoiceExpUtilitiesBinaryMatrix(rb);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        modeChoiceLogsum = null;
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

        logger.fine(getName() + " free memory before running model: " +
            Runtime.getRuntime().freeMemory());

        PTHousehold[] households = (PTHousehold[]) msg.getValue("households");

        logger.info(getName() + " working on " + households.length + " households");

        //Run models for weekdays
        long startTime = System.currentTimeMillis();
        logger.info("\t"+ getName() + " running Weekday Pattern Model");
        households = ptModel.runWeekdayPatternModel(households);
        double patternTime = (System.currentTimeMillis() - startTime) / 1000.0;
        logger.info("\t"+ getName() + " Time to run Weekday Pattern Model for " + households.length +
            " households = " + patternTime + " seconds");

        startTime = System.currentTimeMillis();
        logger.info("\t"+ getName() + " generating Tours based on Weekday,Weekend Patterns");
        households = ptModel.generateWeekdayTours(households);
        double tourTime = (System.currentTimeMillis() - startTime) / 1000.0;
        logger.info("\t"+ getName() + " time to generate Weekday Tours for " + households.length +
            " households = " + tourTime + " seconds");

        startTime = System.currentTimeMillis();
        double readTime = 0.0;
        durationTime = 0.0;
        primaryTime = 0.0;
        secondaryTime = 0.0;
        destZoneTime = 0.0;
        stopZoneTime = 0.0;
        tripModeTime = 0.0;

        List returnValues = new ArrayList();
        logger.info("\t\t"+ getName() + " getting Logsums and Running WeekdayDurationDestinationModeChoiceModel for HH block");
        int thisNonWorkSegment =-1; //need to initialize for logger statment
        int thisWorkSegment = -1; //need to initialize for logger statement

        int wSegmentChange=0;
        int nwSegmentChange=0;
        int[] reads;
        for (int i = 0; i < households.length; i++) {
            logger.fine("\t"+ getName() + " processing household: " + households[i].ID);
            thisNonWorkSegment = households[i].calcNonWorkLogsumSegment();
            thisWorkSegment = households[i].calcWorkLogsumSegment();
            long tempReadTime = System.currentTimeMillis();
            reads = expUtilitiesManager.updateExpUtilities(thisWorkSegment,thisNonWorkSegment);
            wSegmentChange += reads[0];  //will return a '1' if there was a read, otherwise 0
            nwSegmentChange += reads[1];
            readTime += ((System.currentTimeMillis() - tempReadTime)/1000.0);

            logger.fine("\t\t"+ getName() + " running WeekdayDurationDestinationModeChoiceModel");
            returnValues = ptModel.runWeekdayDurationDestinationModeChoiceModels(households[i],
                    expUtilitiesManager);
            households[i] = (PTHousehold) returnValues.get(0);
            incrementModelTime((double[]) returnValues.get(1));
            incrementPrimaryModelSubTimes((double[]) returnValues.get(2));

        }
        double loopTime = ((System.currentTimeMillis() - startTime) / 1000.0);
        logger.info("\t"+ getName() + ": Time to run duration destination mode choice model for " +
            households.length + " households: \n\t\t\tLoop Time: " +
            loopTime + " seconds" + "\n\t\t\tDurationModel: " + durationTime +
			"\n\t\t\tPrimaryTime: " + primaryTime + "\n\t\t\tSecondaryTime: " + secondaryTime);


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
        //Worker has processed all the households.  It will write out it's results to the local disk
        logger.info(getName() + " writing out household results to the worker file");
        startTime = System.currentTimeMillis();
//        results.writeResults(households);
        double writeTime = ((System.currentTimeMillis() - startTime)/1000.0);

        double totalTime = patternTime + tourTime + loopTime + writeTime;
        logger.info("TIMING," + getName() + "," + totalTime + "," + patternTime + "," + tourTime + "," + readTime + "," +
                    + durationTime + "," + primaryTime + "," + destZoneTime + "," + stopZoneTime + "," + tripModeTime + ","
                    + secondaryTime + "," + writeTime + "," + wSegmentChange + ","
                    + nwSegmentChange + "," + new Date());

        //notify the master that the households have been processed.  The message might also
        //have a 'sendMore' request in it which the master will honor.
        logger.info(getName() + " sending household count to results queue.");
        msg.setId(MessageID.HOUSEHOLDS_PROCESSED);
        msg.setValue("households",households);
        msg.setValue("nHHs", new Integer(households.length));
        sendTo("ResultsWriterQueue", msg);
//        logger.fine("Free memory after running model: " +
//            Runtime.getRuntime().freeMemory());
    }

    private void loadDCLogsums() {
        synchronized (lock) {
            if (!dcLoaded) {
                logger.info(getName() + " reading DC Logsums");
                PTModelInputs.readDCLogsums(rb);
            }
            dcLoaded = true;
        }
    }

    private static TableDataSet loadTableDataSet(ResourceBundle rb,String pathName) {
        String path = ResourceUtil.getProperty(rb, pathName);
        try {
            String fullPath = path;
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(fullPath));
            return table;
            
        } catch (IOException e) {
            logger.severe("Can't find input table "+path);
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

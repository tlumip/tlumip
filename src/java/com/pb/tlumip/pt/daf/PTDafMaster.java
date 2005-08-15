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
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.ObjectUtil;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.tlumip.pt.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;




/**
 *
 * PTDafMaster sends messages to work queues.
 *
 *
 * @author    Christi Willison
 * @version   1.0, 5/5/2004
 *
 */
public class PTDafMaster extends MessageProcessingTask {
    Logger ptDafMasterLogger = Logger.getLogger(PTDafMaster.class);
    static int NUMBER_OF_WORK_NODES;  //specified in the ptdaf_$SCENARIO_NAME.properties file
    static int MAXBLOCKSIZE;  //will be initialized through pt resource bundle as it may change depending
                                //on the scenario (ex. 5000 for pleaseWork, 8 for smallPop)
    static int TOTAL_COLLAPSED_MCLOGSUMS; // is set in the properties file, will be read in during onStart method
    static int MAXALPHAZONENUMBER; //will be set once the a2b object is read in (path is specified in global.properties).

    static int TOTALSEGMENTS; //is defined by the PTHousehold class and will be set after households are created
    static int TOTAL_MCLOGSUMS; // is number of activity purposes except 'home' * total segments

    static int TOTAL_DCLOGSUMS; //is total segments * the dc logsum activity purposes.length
    static int TOTAL_DCEXPUTILS; //is the same as the total number of dc logsums.

    static String DEBUG_FILES_PATH; //location of debug files, need to delete all files during onStart and report presence
                                                        //of any new files created during model run at the end of PT
    PTDataReader dataReader;

    PTHousehold[] households;
    PTPerson[] persons;

    //Counters that will be used to keep track of returning messages.
    int mcLogsumCount = 0;
    int mcCollapsedLogsumCount = 0;
    int dcLogsumCount = 0;
    int dcExpUtilCount = 0;
    int tazUpdateCount = 0;
    int personsWithWorkplaceCount = 0;
    int householdsProcessedCount = 0;

    
    int householdCounter = 0; /**counter to keep track of number of households that have been sent out for processing.*/
    int numHHBlocksLeftToProcess = 0; /**after initial blocks of households have been sent out, this number is calculated */
    int blockCounter = 0; /** Used to keep track of the total number of hh blocks sent to workers */

    ResourceBundle ptdafRb; //this will be read in after the scenarioName has been read in from RunParams.txt
    ResourceBundle ptRb; // this will be read in after pathToPtRb has been read in from RunParams.txt
    ResourceBundle globalRb; //this will be read in after pathToGlobalRb has been read in from RunParams.txt

    // variable that keeps track of last work queue
    int lastWorkQueue;
    ArrayList mcWorkQueues = new ArrayList();
    ArrayList dcWorkQueues = new ArrayList();
    ArrayList hhWorkQueues = new ArrayList();
    
    int nNodesInitialized = 0;


//    Read parameters
//     * Send out mode choice logsums to workers
//     * Read households
//     * Run household auto ownership model
//     * Read persons
//     * Set person attributes (Home TAZ, householdWorkLogsum) from household attributes
//     * Sort the person array by occupation (0->8) and householdWorkLogsumMarket (0->8)
    /**
     *
     */
    public void onStart() {
        ptDafMasterLogger.info("***" + getName() + " started");

        //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.txt file
        //that was written by the Application Orchestrator
        String scenarioName = null;
        int timeInterval = -1;
        String pathToPtRb = null;
        String pathToGlobalRb = null;

        ptDafMasterLogger.info("Reading RunParams.properties file");
        ResourceBundle runParamsRb = ResourceUtil.getPropertyBundle(new File(Scenario.runParamsFileName));
        scenarioName = ResourceUtil.getProperty(runParamsRb,"scenarioName");
        ptDafMasterLogger.info("\tScenario Name: " + scenarioName);
        timeInterval = Integer.parseInt(ResourceUtil.getProperty(runParamsRb,"timeInterval"));
        ptDafMasterLogger.info("\tTime Interval: " + timeInterval);
        pathToPtRb = ResourceUtil.getProperty(runParamsRb,"pathToAppRb");
        ptDafMasterLogger.info("\tResourceBundle Path: " + pathToPtRb);
        pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,"pathToGlobalRb");
        ptDafMasterLogger.info("\tResourceBundle Path: " + pathToGlobalRb);

        //Get the properties files.
        ptdafRb = ResourceUtil.getResourceBundle("ptdaf_"+scenarioName);
        ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
        globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));


        //Set class attributes based on those properties.
        // daf related properties
        NUMBER_OF_WORK_NODES = Integer.parseInt(ResourceUtil.getProperty(ptdafRb, "nWorkNodes"));
        lastWorkQueue = NUMBER_OF_WORK_NODES;
        if(ptDafMasterLogger.isDebugEnabled()) {
            ptDafMasterLogger.debug("Number of Worker Nodes: " + NUMBER_OF_WORK_NODES);
        }
        ArrayList queues = ResourceUtil.getList(ptdafRb, "queueList");
        if(ptDafMasterLogger.isDebugEnabled()) ptDafMasterLogger.debug("Total queues " + queues.size());
        Iterator iter = queues.iterator();
        while(iter.hasNext()){
            String queueName = (String) iter.next();
            if(ptDafMasterLogger.isDebugEnabled()) ptDafMasterLogger.debug("Queue Name : " + queueName);
            if(queueName.indexOf("MC")>=0) {
                mcWorkQueues.add(queueName);
                Message mcInitMsg = createMessage();
                mcInitMsg.setId("init");
                sendTo(queueName,mcInitMsg);
                if(ptDafMasterLogger.isDebugEnabled()) ptDafMasterLogger.debug(queueName + " added to mcWorkQueues");
            }
            if(queueName.indexOf("DC")>=0) {
                dcWorkQueues.add(queueName);
                Message dcInitMsg = createMessage();
                dcInitMsg.setId("init");
                sendTo(queueName,dcInitMsg);
                if(ptDafMasterLogger.isDebugEnabled()) ptDafMasterLogger.debug(queueName + " added to dcWorkQueues");
            }
            if(queueName.indexOf("HH")>=0) {
                hhWorkQueues.add(queueName);
                Message hhInitMsg = createMessage();
                hhInitMsg.setId("init");
                sendTo(queueName,hhInitMsg);
                if(ptDafMasterLogger.isDebugEnabled()) ptDafMasterLogger.debug(queueName + " added to hhWorkQueues");
            }
        }
        if(ptDafMasterLogger.isDebugEnabled()) {
            ptDafMasterLogger.debug("number of mcWorkQueues " + mcWorkQueues.size());
            ptDafMasterLogger.debug("number of dcWorkQueues " + dcWorkQueues.size());
            ptDafMasterLogger.debug("number of hhWorkQueues " + hhWorkQueues.size());
        }

        // pt related properties
        MAXBLOCKSIZE = Integer.parseInt(ResourceUtil.getProperty(ptRb,"max.block.size"));
        TOTAL_COLLAPSED_MCLOGSUMS = ResourceUtil.getList(ptRb,"matrices.for.pi").size();
        DEBUG_FILES_PATH = ResourceUtil.getProperty(ptRb, "debugFiles.path");
        if(ptDafMasterLogger.isDebugEnabled()) {
            ptDafMasterLogger.debug("Total Collapsed Mode Choice Logsums: " + TOTAL_COLLAPSED_MCLOGSUMS);
        }

        // global properties
        TableDataSet alphaToBetaTable = loadTableDataSet(globalRb,"alpha2beta.file");
        AlphaToBeta a2b = new AlphaToBeta(alphaToBetaTable);
        MAXALPHAZONENUMBER = a2b.getMaxAlphaZone();
        if(ptDafMasterLogger.isDebugEnabled()) {
            ptDafMasterLogger.debug("Max Alpha Zone Number: " + MAXALPHAZONENUMBER);
        }
        //Now define all the other static class attributes
        TOTALSEGMENTS = PTHousehold.NUM_WORK_SEGMENTS;
        TOTAL_MCLOGSUMS = (ActivityPurpose.ACTIVITY_PURPOSES.length -1) * TOTALSEGMENTS;
        TOTAL_DCLOGSUMS = ActivityPurpose.DC_LOGSUM_PURPOSES.length * TOTALSEGMENTS;
        TOTAL_DCEXPUTILS = TOTAL_DCLOGSUMS;
        if(ptDafMasterLogger.isDebugEnabled()) {
            ptDafMasterLogger.debug("Total Segments: " + TOTALSEGMENTS);
            ptDafMasterLogger.debug("Total Mode Choice Logsums: " + TOTAL_MCLOGSUMS);
            ptDafMasterLogger.debug("Total Destination Choice Logsums: " + TOTAL_DCLOGSUMS);
            ptDafMasterLogger.debug("Total Exponentiated Destination Choice Logsums: " + TOTAL_DCEXPUTILS);
        }

        //Check for existence of any files in the "debug" directory and delete them, new files
        // created during run will be reported on after the model has finished
        File debugDir = new File(DEBUG_FILES_PATH);
        File[] debugFiles = debugDir.listFiles();
        if(debugFiles.length != 0){
            for(int f=0;f<debugFiles.length; f++){
                debugFiles[f].delete();
                if(ptDafMasterLogger.isDebugEnabled()) ptDafMasterLogger.debug("File " + debugFiles[f] + " deleted");
            }
        }

        //Start mode choice logsums.  The workers will decide if the MC Logsums
        //actually need to be recalculated based on a class boolean.  If they
        //don't need to be calculated the workers will send back an empty message.
        //Either way, the Master will be able to go to the next model based
        //on the total "MCLogsumsCreated" messages coming back.
//        ptDafMasterLogger.info("Starting the Mode Choice Logsum calculations");
        startMCLogsums();

        //Read the SynPop data
        dataReader = new PTDataReader(ptRb, globalRb);
        ptDafMasterLogger.info("Adding synthetic population from database");
        households = dataReader.readHouseholds("households.file");
        ptDafMasterLogger.info("Total Number of HHs: " + households.length);
//        if(ptDafMasterLogger.isDebugEnabled()) {
//            ptDafMasterLogger.debug("Size of households: " + ObjectUtil.sizeOf(households));
//        }


        ptDafMasterLogger.info("Reading the Persons file");
        persons = dataReader.readPersons("persons.file");
        ptDafMasterLogger.info("Total Number of Persons: " + persons.length);
//        if(ptDafMasterLogger.isDebugEnabled()) {
//            ptDafMasterLogger.debug("Size of persons: " + ObjectUtil.sizeOf(persons));
//        }

        //add worker info to Households and add homeTAZ and hh work segment to persons
        //This method sorts households and persons by ID.  Leaves arrays in sorted positions.
        dataReader.addPersonInfoToHouseholdsAndHouseholdInfoToPersons(households, persons);

        //Persons must be read in and .addPersonInfo... must be called before running
        //auto ownership, otherwise PTHousehold.workers will be 0 and
        //the work segment will be calculated incorrectly.
        ptDafMasterLogger.info("Starting the AutoOwnershipModel");
        households = dataReader.runAutoOwnershipModel(households);

        PTSummarizer.summarizeHouseholds(households,ResourceUtil.getProperty(ptRb,"hhSummary.file"));
        PTSummarizer.summarizePersons(persons,ResourceUtil.getProperty(ptRb,"personSummary.file"));

        ptDafMasterLogger.info("Finished onStart()");
    }

    /**
     * Wait for message.
     * If message is MC_LOGSUMS_CREATED, startWorkplaceLocation
     * If message is WORKPLACE_LOCATIONS_CALCULATED, add the
     *    workers to the persons array, and check if done with all segments.
     *    If done,
     *       Set TazDataArrays, which will update the zone data in each
     *         node with the number of households and teachers in each TAZ.
     *       Add persons with workplace locations to households.
     *       Sort the household array by worklogsum segment and non-worklogsum segment.
     * If message is TAZDATA_UPDATED, check if all nodes have completed updating their data
     *    If done, startDCLogsums()
     * If message is DC_LOGSUMS_CREATED, check if all segments have been completed.
     *    If done, startProcessHouseholds(): Send out initial blocks of households to workers.
     * If message is HOUSEHOLDS_PROCESSED
     *    Send households to householdResults method, which will increment up householdsProcessedCount
     *       and send households for writing to results file.
     *    If households processed less than total households, sendMoreHouseholds()
     *
     *   @param msg
     */
    public void onMessage(Message msg) {
        ptDafMasterLogger.info(getName() + " received messageId=" + msg.getId() +
            " message from=" + msg.getSender() + " @time=" + new Date());

        if (msg.getId().equals(MessageID.MC_LOGSUMS_CREATED) ||
                    msg.getId().equals(MessageID.MC_LOGSUMS_COLLAPSED)) {
            if (msg.getId().equals(MessageID.MC_LOGSUMS_CREATED)) {
                mcLogsumCount++;
                ptDafMasterLogger.info("mcLogsumCount: " + mcLogsumCount);
            }else{
                mcCollapsedLogsumCount++;
                ptDafMasterLogger.info("mcCollapsedLogsumCount: " + mcCollapsedLogsumCount);
            }

            if (mcLogsumCount == TOTAL_MCLOGSUMS && mcCollapsedLogsumCount == TOTAL_COLLAPSED_MCLOGSUMS) {
                ptDafMasterLogger.info("ModeChoice Logsums completed.");
                createWorkplaceLocationMessages();
            }

        } else if (msg.getId().equals(MessageID.WORKPLACE_LOCATIONS_CALCULATED)) {
            addToPersonsArray(msg);
            ptDafMasterLogger.info("Persons with workplace location: " + personsWithWorkplaceCount);

            //if the persons in the person array is equal to the total number of persons,
            //we are done with the workplace location model
            if (personsWithWorkplaceCount == persons.length) {
                setTazDataArrays();
                households = dataReader.addPersonsToHouseholds(households,persons);
            }

        } else if (msg.getId().equals(MessageID.TAZDATA_UPDATED)) {

            tazUpdateCount++;
            if(ptDafMasterLogger.isDebugEnabled()) {
                ptDafMasterLogger.debug("tazUpdateCount: " + tazUpdateCount);
            }

            if (tazUpdateCount == NUMBER_OF_WORK_NODES) {
                ptDafMasterLogger.info("Taz data has been updated on all workers.");
                startDCLogsums();
            }

        } else if (msg.getId().equals(MessageID.DC_LOGSUMS_CREATED) ||
                msg.getId().equals(MessageID.DC_EXPUTILITIES_CREATED)) {

            if (msg.getId().equals(MessageID.DC_LOGSUMS_CREATED) ){
                dcLogsumCount++;
                if(ptDafMasterLogger.isDebugEnabled()) {
                    ptDafMasterLogger.debug("dcLogsumCount: " + dcLogsumCount);
                }
            } else {
                dcExpUtilCount++;
                if(ptDafMasterLogger.isDebugEnabled()) {
                    ptDafMasterLogger.debug("expUtilCount: " + dcExpUtilCount);
                }
            }

            if (dcLogsumCount == TOTAL_DCLOGSUMS  && dcExpUtilCount ==TOTAL_DCEXPUTILS) {
                ptDafMasterLogger.info("Destination Choice Logsums completed.");
                startProcessHouseholds();
            }

        } else if (msg.getId().equals(MessageID.HOUSEHOLDS_PROCESSED)) {
            householdsProcessedCount = householdsProcessedCount + ((Integer)msg.getValue("nHHs")).intValue();
            ptDafMasterLogger.info("Households processed so far: " + householdsProcessedCount);

            if ((((Integer) msg.getValue("sendMore")).intValue() == 1) &&
                    (householdCounter < households.length)) {
                sendMoreHouseholds(msg);
            }

            if (householdsProcessedCount == households.length) {
                Message allDone = createMessage();
                allDone.setId(MessageID.ALL_HOUSEHOLDS_PROCESSED);
                sendTo("ResultsWriterQueue",allDone);
            }

        } else if(msg.getId().equals(MessageID.ALL_FILES_WRITTEN)){
            //check to see if any debug files were created.  This indicates that there were tours that could
            //not find destinations or stops that couldn't find locations, etc.
            File debugDir = new File(DEBUG_FILES_PATH);
            File[] debugFiles = debugDir.listFiles();
            if(debugFiles.length != 0){
                ptDafMasterLogger.info("This run of the PTModel had unresolved problems.\n" +
                        "Please look at the files in  " + debugDir.getAbsolutePath() + " for further info.");
            }

            //Signal to the File Monitor that the model is finished.
            ptDafMasterLogger.info("Signaling to the File Monitor that the model is finished");
            File doneFile = new File(ResourceUtil.getProperty(ptRb,"done.file"));

            try {
                PrintWriter writer = new PrintWriter(new FileWriter(
                            doneFile));
                writer.println("pt daf is done." + new Date());
                writer.close();
                ptDafMasterLogger.info("pt daf is done.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else{
            //init message, don't do anything.
        }
    }

    /**
     * startMCLogsums - sends messages to the work queues to create MC Logsum matrices
     *
     */
    private void startMCLogsums() {
        ptDafMasterLogger.info("Creating tour mode choice logsums");
        int msgCounter = 0;
        //enter loop on purposes (skip home purpose)
        for (int purpose = 1; purpose < ActivityPurpose.ACTIVITY_PURPOSES.length; ++purpose) {
            char thisPurpose = ActivityPurpose.ACTIVITY_PURPOSES[purpose];

            //enter loop on segments
            for (int segment = 0; segment < TOTALSEGMENTS; ++segment) {
                Message mcLogsumMessage = createMessage();
                msgCounter++;
                mcLogsumMessage.setId(MessageID.CREATE_MC_LOGSUMS);
                mcLogsumMessage.setValue("purpose",
                    new Character(ActivityPurpose.ACTIVITY_PURPOSES[purpose]));
                mcLogsumMessage.setValue("segment", new Integer(segment));

//                String queueName = getQueueName();
                String queueName = getMCQueueName(msgCounter);
                mcLogsumMessage.setValue("queue", queueName);
                ptDafMasterLogger.info("Sending MC logsums to " + queueName + " : " +
                    thisPurpose + segment);
                sendTo(queueName, mcLogsumMessage);
            }
        }
        ptDafMasterLogger.info("Total Mode Choice Logsums Sent to Workers: "+ TOTAL_MCLOGSUMS);
    }

    /**
     * createWorkPlaceLocationMessages - sends messages to the work queues to create LaborFlowProbability matrices
     * iterates through all household and occupation segments,
     * bundles up workers in each combination of household and occupation
     * sends out messages: each node to run workplace location model on
     * one combination household segment/occupation group
     * Finally, add the unemployed persons to the person array.
     */
    private void createWorkplaceLocationMessages(){
        int msgCounter = 0;
        //Sort the person array by segment, occupation code so the first person will
        //have segment code 0, occupation code 0 followed by segment code 0, occupation code 1, etc.
        Arrays.sort(persons); //sorts persons by workSegment (0-8) and then by occupation code (0-8)

        //We want to find all persons that match a particular segment/occupation pair and send those
        //off to a worker to be processed.
        int index = 0; //index will keep track of where we are in the person array
        int nUnemployed = 0;
        int totalWorkers = 0;
        ArrayList unemployedPersonList = new ArrayList();
        while(index < persons.length){
            ArrayList personList = new ArrayList();
            int segment = persons[index].householdWorkSegment;
            int occupation = persons[index].occupation;
            int nPersons = 0;  //number of people in subgroup for the seg/occ pair.
            while(persons[index].householdWorkSegment == segment && persons[index].occupation == occupation){
                if(persons[index].employed){
                    if(persons[index].occupation == 0) ptDafMasterLogger.warn("Employed person has an occupation code of 'UNEMPLOYED'");
                    totalWorkers++;
                    nPersons++;
                    personList.add(persons[index]);
                    index++;
                }else{    //the person is unemployed - their occupation code may or may not be 0.
                    nUnemployed++;
                    unemployedPersonList.add(persons[index]);
                    index++; //go to next person
                }
                if(index == persons.length) break;  //the last person has been processed.
            }
            if(nPersons > 0){ //there were persons that matched the seg/occ pair (occ != 0)
                PTPerson[] personsSubset = new PTPerson[nPersons];
                personList.toArray(personsSubset);
                //create a message, set the occupation and segment
                Message laborFlowMessage = createMessage();
                msgCounter++;
                laborFlowMessage.setId(MessageID.CALCULATE_WORKPLACE_LOCATIONS);
                laborFlowMessage.setValue("segment", new Integer(segment));
                laborFlowMessage.setValue("occupation", new Integer(occupation));
                laborFlowMessage.setValue("persons", personsSubset);
                String queueName = getHHQueueName(msgCounter);
                ptDafMasterLogger.info("Sending Person Message to " + queueName + ": segment "
                        + segment + " - occupation " + occupation + ": total persons: " + nPersons);
                sendTo(queueName, laborFlowMessage);
            }
        }
        ptDafMasterLogger.info("Total persons: " + persons.length);
        ptDafMasterLogger.info("\tTotal unemployed persons: " + nUnemployed);
        ptDafMasterLogger.info("\tTotal working persons: " + totalWorkers);
        ptDafMasterLogger.info("Percent Unemployed: " + ((double)nUnemployed/persons.length)*100 + "%");
        personsWithWorkplaceCount = nUnemployed;   //used as a place holder for persons coming back from
                                                   //the workers.  They will be placed in the array
                                                   //starting at this number.

        //Once the entire list of persons has been processed, we need to put the unemployed
        // persons back into the persons array 
        Iterator iter = unemployedPersonList.iterator();
        while (iter.hasNext()){
            persons[nUnemployed-1] = (PTPerson) iter.next();  //the order doesn't matter so just start at
            nUnemployed--;                                //the array position corresponding to the nUnemployed-1
        }                                                 //and work backward.

        unemployedPersonList =  null;
    }

    /**
     * Add the workers from the persons array in the message to the persons array
     * @param msg
     */
    private void addToPersonsArray(Message msg) {
        PTPerson[] ps = (PTPerson[]) msg.getValue("persons");
        for (int i = 0; i < ps.length; i++) {
            persons[personsWithWorkplaceCount] = ps[i];
            personsWithWorkplaceCount++;
        }
    }

    /**
     * Count the number of households, pre and post-secondary teachers
     * in each taz and store in arrays.  Send the arrays to each node
     * to update the data in memory.
     *
     */
    private void setTazDataArrays() {
        ptDafMasterLogger.info("Sending message to workers to update TAZ data");
    	int[] householdsByTaz = new int[MAXALPHAZONENUMBER+1];
        int[] postSecOccup = new int[MAXALPHAZONENUMBER+1];
        int[] otherSchoolOccup = new int[MAXALPHAZONENUMBER+1];

        for(int p=0;p<persons.length;p++){
            if(persons[p].occupation==OccupationCode.P0ST_SEC_TEACHERS)
                postSecOccup[persons[p].workTaz]++;
            else if(persons[p].occupation==OccupationCode.OTHER_TEACHERS)
                otherSchoolOccup[persons[p].workTaz]++;
        }
        for(int h=0;h<households.length;h++)
            householdsByTaz[households[h].homeTaz]++;



        for (int q = 2; q <= NUMBER_OF_WORK_NODES+1; q++) {
            Message tazInfo = createMessage();
            tazInfo.setId(MessageID.UPDATE_TAZDATA);
            tazInfo.setValue("householdsByTaz",householdsByTaz);
            tazInfo.setValue("postSecOccup",postSecOccup);
            tazInfo.setValue("otherSchoolOccup",otherSchoolOccup);
//            String queueName = new String("WorkQueue" + q);
            String queueName = new String("HH_node"+ q + "WorkQueue");
            ptDafMasterLogger.info("Sending Message to" + queueName +
                        " to update TAZ info");

            sendTo(queueName, tazInfo);
        }
    }

    /**
     * Creates tour destination choice logsums for non-work purposes.
     * Sends messages to workers to create dc logsums for a given purpose
     * and market segment.
     *
     */
    private void startDCLogsums() {
        ptDafMasterLogger.info("Creating tour destination choice logsums");
        int msgCounter = 0;
        //enter loop on purposes - start at 2 because you don't need to create DC logsums for home or work purposes
        for (int purpose = 2; purpose < ActivityPurpose.ACTIVITY_PURPOSES.length; ++purpose) {
            char thisPurpose = ActivityPurpose.ACTIVITY_PURPOSES[purpose];

            //enter loop on segments
            for (int segment = 0; segment < TOTALSEGMENTS; ++segment) {
                Message dcLogsumMessage = createMessage();
                msgCounter++;
                dcLogsumMessage.setId(MessageID.CREATE_DC_LOGSUMS);
                dcLogsumMessage.setValue("purpose",
                    new Character(thisPurpose));
                dcLogsumMessage.setValue("segment", new Integer(segment));

//                String queueName = getQueueName();
                String queueName = getDCQueueName(msgCounter);
                dcLogsumMessage.setValue("queue", queueName);
                ptDafMasterLogger.info("Sending DC logsums to " + queueName + " : " +
                    thisPurpose + segment);
                sendTo(queueName, dcLogsumMessage);
            }
        }
    }
    
    /**
     * Starts the household processing by sending a specified number of household blocks to each work queue.
     * The second to last block will contain a message telling the worker node to send
     * a message back to PTDafMaster to send more households.
     *
     */
    private void startProcessHouseholds() {
        int msgCounter = 0;
        if(ptDafMasterLogger.isDebugEnabled()) {
            ptDafMasterLogger.debug("Processing households.");
        }
         //Sort hhs by work segment, non-work segment
         Arrays.sort(households);

        int numHHBlocksPerQueue = 20;
        

        //iterate through number of workers, 20 household blocks
        for (int q = 1; q <= hhWorkQueues.size(); q++) {
            for (int j = 0;j < numHHBlocksPerQueue; j++){ //consider: *Math.ceil(households.length / NUMBER_OF_WORK_QUEUES / MAXBLOCKSIZE)
                blockCounter++;
                //create an array of households
                PTHousehold[] householdBlock = new PTHousehold[MAXBLOCKSIZE];

                //fill it with households from the main household array
                for (int k = 0; k < MAXBLOCKSIZE; k++) {
                    if(householdCounter<households.length){
                        householdBlock[k] = (PTHousehold) households[householdCounter];
                        householdCounter++;
                    }
                }

                Message processHouseholds = createMessage();
                msgCounter++;
                processHouseholds.setId(MessageID.PROCESS_HOUSEHOLDS);
                processHouseholds.setValue("blockNumber" , new Integer(blockCounter));
                processHouseholds.setValue("households", householdBlock);

                //The "sendMore" key in the hashtable will be set to 1 for the second
                //to last block, else 0.
                if (j == (numHHBlocksPerQueue - 2)) {
                    processHouseholds.setValue("sendMore", new Integer(1));
                } else {
                    processHouseholds.setValue("sendMore", new Integer(0));
                }

                if(ptDafMasterLogger.isDebugEnabled()) {
                    ptDafMasterLogger.debug("sendMore: " +
                                    (Integer) processHouseholds.getValue("sendMore"));
                }

//                String queueName = new String("WorkQueue" + (q+1)); //queue numbering starts at 2 in ptdaf.properties
                String queueName = getHHQueueName(q);
                processHouseholds.setValue("WorkQueue", queueName);
                ptDafMasterLogger.debug("Sending HH Block "+ blockCounter + " to "  + queueName);
                sendTo(queueName, processHouseholds);
                if(ptDafMasterLogger.isDebugEnabled()) {
                    ptDafMasterLogger.debug("householdCounter = " + householdCounter);
                }
             }
        }
        
    }

    /**
     * Send more households to workers
     * 
     * @param msg
     */
    private void sendMoreHouseholds(Message msg) {
        
        int numHHBlocksLeftToProcess = (int) Math.ceil((households.length - householdCounter) / (double) MAXBLOCKSIZE);
        if(ptDafMasterLogger.isDebugEnabled()) {
            ptDafMasterLogger.debug("HHs left to process = " + (households.length - householdCounter));
            ptDafMasterLogger.debug("numHHBlocksLeft to process = " + (int) Math.ceil((households.length - householdCounter) / (double)MAXBLOCKSIZE));
        }
        //Send every worker who needs more work, either 20 new messages with households OR
        //divide up the number of blocks that are left and send an even number to all workers.
        int numOfHHBlocksPerQueue = Math.min(20, (int)Math.ceil(numHHBlocksLeftToProcess/(double)hhWorkQueues.size()));
        if(ptDafMasterLogger.isDebugEnabled()){
            ptDafMasterLogger.debug("num of HHBlocks per queue: " + numOfHHBlocksPerQueue);
        }
        for (int j = 0; j <numOfHHBlocksPerQueue; j++) {
            blockCounter++;
            int numOfHHsPerBlock = Math.min(MAXBLOCKSIZE, households.length - householdCounter);
            
            PTHousehold[] householdBlock = new PTHousehold[numOfHHsPerBlock];

            for (int k = 0; k < numOfHHsPerBlock; k++) {
                if(householdCounter<households.length){
                    householdBlock[k] = (PTHousehold) households[householdCounter];
                    householdCounter++;
                }
            }

            Message processHouseholds = createMessage();
            processHouseholds.setId(MessageID.PROCESS_HOUSEHOLDS);
            processHouseholds.setValue("blockNumber" , new Integer(blockCounter));
            processHouseholds.setValue("households", householdBlock);

            if (j == (numOfHHBlocksPerQueue - 2) || (numOfHHBlocksPerQueue == 1)) {
                processHouseholds.setValue("sendMore", new Integer(1));
            } else {
                processHouseholds.setValue("sendMore", new Integer(0));
            }

            String queueName = (String) msg.getValue("WorkQueue");
            processHouseholds.setValue("WorkQueue", queueName);
            
            if(ptDafMasterLogger.isDebugEnabled()) {
                ptDafMasterLogger.debug("Sending HH Block "+ blockCounter + " to"  + queueName);
                ptDafMasterLogger.debug("Num of HHs in Block: " + numOfHHsPerBlock);
                ptDafMasterLogger.debug("Send more: " + processHouseholds.getValue("sendMore"));
            }
            sendTo(queueName, processHouseholds);
            ptDafMasterLogger.info("householdCounter = " + householdCounter);
        }
    }

    /**
     * getQueueName() is used when spraying an equal number of messages accross all WorkQueues.
     */
//    private String getQueueName() {
//        String queue = null;
//
//        if (lastWorkQueue == NUMBER_OF_WORK_QUEUES+1) {
//            int thisWorkQueue = 2;
//            queue = new String("WorkQueue" + thisWorkQueue);
//            lastWorkQueue = thisWorkQueue;
//        } else {
//            int thisWorkQueue = lastWorkQueue + 1;
//            queue = new String("WorkQueue" + thisWorkQueue);
//            lastWorkQueue = thisWorkQueue;
//        }
//
//        return queue;
//    }
    
    private String getMCQueueName(int msgCount){
        String queueName = null;
        queueName = (String) mcWorkQueues.get((msgCount % mcWorkQueues.size()));
        ptDafMasterLogger.debug("Message " + msgCount + " being sent to " + queueName);
        return queueName;
    }
    
    private String getDCQueueName(int msgCount){
        String queueName = null;
        queueName = (String) dcWorkQueues.get((msgCount % dcWorkQueues.size()));
        ptDafMasterLogger.debug("Message " + msgCount + " being sent to " + queueName);
        return queueName;
    }
    
    private String getHHQueueName(int msgCount){
        String queueName = null;
        queueName = (String) hhWorkQueues.get((msgCount % hhWorkQueues.size()));
        return queueName;
    }
    
    private void readInPersonAndHouseholdData(){
//      Read the SynPop data
        dataReader = new PTDataReader(ptRb, globalRb);
        ptDafMasterLogger.info("Adding synthetic population from database");
        households = dataReader.readHouseholds("households.file");
        ptDafMasterLogger.info("Total Number of HHs: " + households.length);
        if(ptDafMasterLogger.isDebugEnabled()) {
            ptDafMasterLogger.debug("Size of households: " + ObjectUtil.sizeOf(households));
        }


        ptDafMasterLogger.info("Reading the Persons file");
        persons = dataReader.readPersons("persons.file");
        ptDafMasterLogger.info("Total Number of Persons: " + persons.length);
        if(ptDafMasterLogger.isDebugEnabled()) {
            ptDafMasterLogger.debug("Size of persons: " + ObjectUtil.sizeOf(persons));
        }

        //add worker info to Households and add homeTAZ and hh work segment to persons
        //This method sorts households and persons by ID.  Leaves arrays in sorted positions.
        dataReader.addPersonInfoToHouseholdsAndHouseholdInfoToPersons(households, persons);

        //Persons must be read in and .addPersonInfo... must be called before running
        //auto ownership, otherwise PTHousehold.workers will be 0 and
        //the work segment will be calculated incorrectly.
        ptDafMasterLogger.info("Starting the AutoOwnershipModel");
        households = dataReader.runAutoOwnershipModel(households);

        PTSummarizer.summarizeHouseholds(households,ResourceUtil.getProperty(ptRb,"hhSummary.file"));
        PTSummarizer.summarizePersons(persons,ResourceUtil.getProperty(ptRb,"personSummary.file"));
        
        startMCLogsums();
    }

    private TableDataSet loadTableDataSet(ResourceBundle rb,String pathName) {
        String path = ResourceUtil.getProperty(rb, pathName);
        try {
            String fullPath = path;
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(fullPath));
            return table;

        } catch (IOException e) {
            ptDafMasterLogger.fatal("Can't find input table "+path);
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {

        Logger ptDafMasterLogger = Logger.getLogger("com.pb.tlumip.pt");
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_pleaseWork/t1/pt/pt.properties"));
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_pleaseWork/t1/global.properties"));
        //Read the SynPop data
        PTDataReader dataReader = new PTDataReader(rb, globalRb);
        ptDafMasterLogger.info("Adding synthetic population from database");
        PTHousehold[] households = dataReader.readHouseholds("households.file");
        int totalHouseholds = households.length;
        ptDafMasterLogger.info("Total Number of HHs :" + totalHouseholds);

        ptDafMasterLogger.info("Reading the Persons file");
        PTPerson[] persons = dataReader.readPersons("persons.file");

        //add worker info to Households and add homeTAZ and hh work segment to persons
        //This method sorts households and persons by ID.  Leaves arrays in sorted positions.
        dataReader.addPersonInfoToHouseholdsAndHouseholdInfoToPersons(households,
                persons);

        //Sort hhs by work segment, non-work segment
        Arrays.sort(households);

        int[][] workByNonWork = PTSummarizer.summarizeHHsByWorkAndNonWorkSegments(households);
        System.out.println("work,0,1,2,3,4,5,6,7,8");
        String row = "";
        for(int r=0; r<workByNonWork.length; r++){
            row+=r+",";
            for(int c=0; c<workByNonWork[0].length; c++){
                row+= workByNonWork[r][c]+",";
            }
            System.out.println(row);
            row="";
        }
    }
}

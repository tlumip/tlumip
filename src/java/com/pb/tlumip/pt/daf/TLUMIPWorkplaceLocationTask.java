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
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.*;
import com.pb.models.pt.daf.MessageID;
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.models.reference.IndustryOccupationSplitIndustryReference;
import com.pb.tlumip.pt.LaborFlows;
import com.pb.tlumip.pt.PTOccupation;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * TLUMIPWorkplaceLocationTask is a class that ...
 *
 * @author Kimberly Grommes
 * @version 1.0, Feb 15, 2007
 *          Created by IntelliJ IDEA.
 */
public class TLUMIPWorkplaceLocationTask extends MessageProcessingTask {
    public static Logger wlLogger = Logger.getLogger(TLUMIPWorkplaceLocationTask.class);

    protected static final Object lock = new Object();
    protected static boolean initialized = false;
    protected static boolean dataRead = false;
    protected static boolean sensitivityTestingMode;

    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;
    protected static boolean CALCULATE_SDT;
    protected static String sendQueue = "TaskMasterQueue"; //default
    protected static int MAX_ALPHAZONE_NUMBER;
    protected static int BASE_YEAR;
    protected static IndustryOccupationSplitIndustryReference indOccSplitRef;
    protected static LaborFlows laborFlows;
    protected static SkimsInMemory skims;
    protected static String debugDirPath;
    protected static int[] aZones;

    PTOccupationReferencer occReferencer;
    long workplaceLocationModelSeed = Long.MIN_VALUE/243;

    public void onStart(){
        onStart(PTOccupation.NONE);
    }

    /**
     * Onstart method sets up model
     * @param occReferencer - project specific occupation names
     */
    public void onStart(PTOccupationReferencer occReferencer) {

        wlLogger.info(getName() + ", Started");
        // establish a connection between the workers
        // and the work server.
        Message checkInMsg = mFactory.createMessage();
        checkInMsg.setId(MessageID.WORKER_CHECKING_IN);
        checkInMsg.setValue("workQueueName", getName().trim().substring(0,2) + "_" + getName().trim().substring(12) + "WorkQueue");
        String queueName = "MS_node" + getName().trim().substring(12,13) + "WorkQueue";
        sendTo(queueName, checkInMsg);


        synchronized (lock) {
            if (!initialized) {
                wlLogger.info(getName() + ", Initializing PT Model on Node");
                // We need to read in the Run Parameters (timeInterval and
                // pathToResourceBundle) from the RunParams.properties file
                // that was written by the Application Orchestrator
                String scenarioName;
                int timeInterval;
                String pathToPtRb;
                String pathToGlobalRb;

                wlLogger.info(getName() + ", Reading RunParams.properties file");
                ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
                scenarioName = ResourceUtil.getProperty(runParamsRb,
                            "scenarioName");
                wlLogger.info(getName() + ", Scenario Name: " + scenarioName);
                BASE_YEAR = Integer.parseInt(ResourceUtil.getProperty(
                            runParamsRb, "baseYear"));
                wlLogger.info(getName() + ", Base Year: " + BASE_YEAR);
                timeInterval = Integer.parseInt(ResourceUtil.getProperty(
                            runParamsRb, "timeInterval"));
                wlLogger.info(getName() + ", Time Interval: " + timeInterval);
                pathToPtRb = ResourceUtil.getProperty(runParamsRb,
                            "pathToAppRb");
                wlLogger.info(getName() + ", ResourceBundle Path: "
                            + pathToPtRb);
                pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,
                            "pathToGlobalRb");
                wlLogger.info(getName() + ", ResourceBundle Path: "
                            + pathToGlobalRb);

                ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
                globalRb = ResourceUtil.getPropertyBundle(new File(
                                    pathToGlobalRb));
            
                //initialize price converter
                PriceConverter.getInstance(ptRb,globalRb);

                CALCULATE_SDT = ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.sdt", true);
                if(!CALCULATE_SDT) sendQueue="ResultsWriterQueue";

                //sensitivity testing is for Tlumip - added to code on 4/22/08 by Christi
                //The idea is that when in sensitivityTestMode we would allow the sequence of random numbers
                //to vary from run to run instead of fixing the seed (and thereby fixing the sequence of random numbers)
                //in order to be able to reproduce the results.
                sensitivityTestingMode = ResourceUtil.getBooleanProperty(ptRb, "pt.sensitivity.testing", false);
                wlLogger.info(getName() + ", Sensitivity Testing: " + sensitivityTestingMode);

                debugDirPath = ptRb.getString("sdt.debug.files");
                initialized = true;
            }

            this.occReferencer = occReferencer;

        }


        wlLogger.info(getName() + ", Finished onStart()");

    }

    /**
     * A worker bee that will calculate workplaces for a set of persons
     *
     */
    public void onMessage(Message msg) {
        wlLogger.info(getName() + ", Received messageId=" + msg.getId()
                + " message from=" + msg.getSender());

        if(msg.getId().equals(MessageID.CALCULATE_WORKPLACE_LOCATIONS)){
            readData();
            runWorkplaceLocationModel(msg);
        }
    }

    public void readData(){
        synchronized (lock) {
            if (!dataRead) {
                wlLogger.info(getName() + ", Reading Labor Flow Data on Node");

                //String refFile = globalRb.getString("industry.occupation.to.split.industry.correspondence");
//                indOccSplitRef = new IndustryOccupationSplitIndustryReference(refFile);
                indOccSplitRef = new IndustryOccupationSplitIndustryReference(IndustryOccupationSplitIndustryReference.getSplitCorrespondenceFilepath(globalRb));
                wlLogger.info("Reading alpha to beta file.");
                TableDataSet alphaToBetaTable = loadTableDataSet(globalRb,
                        "alpha2beta.file");
                String alphaName = globalRb.getString("alpha.name");
                String betaName = globalRb.getString("beta.name");
                AlphaToBeta a2b = new AlphaToBeta(alphaToBetaTable, alphaName, betaName);
                aZones = a2b.getAlphaExternals1Based();
                MAX_ALPHAZONE_NUMBER = a2b.getMaxAlphaZone();

                laborFlows = new LaborFlows(globalRb, ptRb, occReferencer);
                laborFlows.setZoneMap(a2b);
                laborFlows.readBetaLaborFlows(BASE_YEAR, indOccSplitRef.getOccupationLabelsByIndex());
                laborFlows.readAlphaValues();

                //The SkimReaderTask will read in the skims
                //prior to any other task being asked to do work.
                skims = SkimsInMemory.getSkimsInMemory();


               dataRead = true;
            }
        }
    }


     /**
     * Create labor flow matrices for a particular occupation, hh segment, and
     * person array and then determine the workplace locations.
     *
     * @param msg Message
     */
    public void runWorkplaceLocationModel(Message msg) {
        PTDataReader reader = new PTDataReader(ptRb, globalRb, occReferencer, BASE_YEAR);
        TourModeChoiceLogsumManager mcLogsums = new TourModeChoiceLogsumManager(globalRb, ptRb);

        int startRow = (Integer) msg.getValue("startRow");
        int endRow = (Integer) msg.getValue("endRow");
        int[] segmentByHhId = (int[]) msg.getValue("segmentByHhId");
        int[] homeTazByHhId = (int[]) msg.getValue("homeTazbyHhId");

        wlLogger.info(getName() + ", Running the WorkplaceLocationModel on rows " +
                startRow + " - " + endRow);
        PTPerson[] persons = reader.readPersonsForWorkplaceLocation(startRow, endRow);

        for(PTPerson person : persons){
            person.segment = (byte) segmentByHhId[person.hhID];
            person.homeTaz = (short) homeTazByHhId[person.hhID];
        }
         segmentByHhId = null;
         homeTazByHhId = null;

        //sort by occupation and segment
        Arrays.sort(persons); // sorts persons by workSegment (0-8) and then
                                // by occupation code (0-8)

        // We want to find all persons that match a particular segment/occupation
        // pair and process those and then do the next segement/occupation pair
        int index = 0; // index will keep track of where we are in the person array
        int nPersonsUnemployed = 0;
        int nPersonsWithWorkplace = 0;
        HashMap<String, int[]> workersByIndByTazId = new HashMap<String, int[]>(1000);
        HashMap<String, Short> workplaceByPersonId = new HashMap<String, Short>(1000000);


        int previousSegment = -1;
        Matrix logsums = null;
        Matrix propensity = null;
        Matrix flows;
        ArrayList<PTPerson> personList = new ArrayList<PTPerson>();
        while (index < persons.length) {
            int segment = persons[index].segment;
            Enum occupation = persons[index].occupation;
            if(segment != previousSegment){
                wlLogger.info(getName() + ", Finding workplaces for people in segment " + segment);
                logsums = mcLogsums.readLogsumMatrix(ActivityPurpose.WORK, segment);
                propensity = laborFlows.calculatePropensityMatrix(logsums, skims.pkDist);
                propensity = propensity.getSubMatrix(aZones);

                previousSegment = segment;
            }

            int nPersons = 0; // number of people in subgroup for the seg/occ pair.
            while (persons[index].segment == segment
                    && persons[index].occupation == occupation) {
                if (persons[index].employed) {
                    if (persons[index].occupation == occReferencer.getOccupation(0)) {
                        wlLogger.warn(getName() +  ", Employed person has NONE as their occupation code");
                    }
                    nPersons++;
                    personList.add(persons[index]);
                    index++;
                } else { // the person is unemployed - their occupation code may or may not be 0.
                    nPersonsUnemployed++;
                    index++; // go to next person
                }
                if (index == persons.length)
                    break; // the last person has been processed.
            }
            if (nPersons > 0) { // there were persons that matched the seg/occ
                                // pair (occ != 0)

                wlLogger.debug(getName() + ", Calculating Alpha Flows");
                flows = laborFlows.calculateAlphaLaborFlowsMatrix(propensity, logsums, skims.pkDist,
                        segment, occupation);

                wlLogger.debug(getName() + ", Finding Workplaces for " + nPersons + " persons");
                calculateWorkplaceLocation(personList, flows);

                wlLogger.debug(getName() + ", Storing results to send back to TaskMasterQueue");
                storeResultsInHashMaps(personList, workersByIndByTazId, workplaceByPersonId);

                nPersonsWithWorkplace += nPersons;
            }
            personList.clear();
        }

        wlLogger.info(getName() + ", Unemployed Persons: " + nPersonsUnemployed);
        wlLogger.info(getName() + ", Persons With Workplace Locations: " + nPersonsWithWorkplace);

        Message workLocations = createMessage();
        workLocations.setId(MessageID.WORKPLACE_LOCATIONS_CALCULATED);
        workLocations.setValue("empInTazs", workersByIndByTazId);
        workLocations.setValue("workplaceByPersonId", workplaceByPersonId);
        workLocations.setValue("nPersonsProcessed", persons.length);

        sendTo(sendQueue, workLocations);


    }

    /**
     * Calculate workplace locations for the array of persons given the logsum
     * accessibility matrix (This method does not change the tazs
     * attribute so it is OK to use it)
     *
     * @param persons persons
     * @param flows
     *            matrix
     *
     */
    public void calculateWorkplaceLocation(ArrayList<PTPerson> persons,
                                           Matrix flows) {
        Random random = new Random();
        for (PTPerson person : persons) {
            if(sensitivityTestingMode)
                random.setSeed(workplaceLocationModelSeed + System.currentTimeMillis() + person.randomSeed);
            else random.setSeed(workplaceLocationModelSeed + person.randomSeed);
            
            WorkplaceLocationModel.chooseWorkplace(flows,
                    person, random, debugDirPath, aZones);
        }

    }

    public void storeResultsInHashMaps(ArrayList<PTPerson> personsBeingProcessed, HashMap<String, int[]> workersByIndByTazId,
                                       HashMap<String, Short> workplaceByPersonId ){
        for(PTPerson person : personsBeingProcessed){
            String industryLabel = indOccSplitRef.getSplitIndustryLabelFromIndex(person.industry);
            int[] employmentByTaz = workersByIndByTazId.get(industryLabel);
            if(employmentByTaz == null){
                employmentByTaz = new int[MAX_ALPHAZONE_NUMBER + 1];
                employmentByTaz[person.workTaz] = 1;
                workersByIndByTazId.put(industryLabel, employmentByTaz);
            }else{
                employmentByTaz[person.workTaz]++;
                workersByIndByTazId.put(industryLabel, employmentByTaz);
            }
            workplaceByPersonId.put(person.hhID + "_" + person.memberID, person.workTaz);
        }
    }




    private TableDataSet loadTableDataSet(ResourceBundle rb, String pathName) {
        String path = ResourceUtil.getProperty(rb, pathName);
        try {
            CSVFileReader reader = new CSVFileReader();
            return reader.readFile(new File(path));

        } catch (IOException e) {
            wlLogger.fatal("Can't find input table " + path);
            throw new RuntimeException("Can't find input table " + path);
        }
     }

}

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

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.pt.LaborFlows;
import com.pb.tlumip.pt.PTModelInputs;

/**
 * 
 * 
 * Created on Jul 22, 2005 
 * @author Christi
 */
public class InitializeNode extends MessageProcessingTask {
    
    Logger logger = Logger.getLogger(HouseholdWorker.class);
    protected static Object lock = new Object();
    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;
    static AlphaToBeta a2b = null;
    /**
     * Onstart method sets up model
     */
    public void onStart(){
        logger.info("InitializeNode is waiting for an initialize message");
    }
    
    public void onMessage(Message msg) {
        logger.info( getName() + " received " + msg.getId() + " from" + msg.getSender() );
        
        //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.properties file
        //that was written by the Application Orchestrator
        String scenarioName = null;
        int timeInterval = -1;
        String pathToPtRb = null;
        String pathToGlobalRb = null;
        
        logger.info(getName() + ", Reading RunParams.properties file");
        ResourceBundle runParamsRb = ResourceUtil.getPropertyBundle(new File(Scenario.runParamsFileName));
        scenarioName = ResourceUtil.getProperty(runParamsRb,"scenarioName");
        logger.info(getName() + ", Scenario Name: " + scenarioName);
        timeInterval = Integer.parseInt(ResourceUtil.getProperty(runParamsRb,"timeInterval"));
        logger.info(getName() + ", Time Interval: " + timeInterval);
        pathToPtRb = ResourceUtil.getProperty(runParamsRb,"pathToAppRb");
        logger.info(getName() + ", ResourceBundle Path: " + pathToPtRb);
        pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,"pathToGlobalRb");
        logger.info(getName() + ", ResourceBundle Path: " + pathToGlobalRb);
        
        ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
        globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));
        
        

        PTModelInputs ptInputs = new PTModelInputs(ptRb,globalRb);
        logger.info(getName() + ", Setting seed");
        ptInputs.setSeed(2002);
        logger.info(getName() + ", Reading parameter files");
        ptInputs.getParameters();
        logger.info(getName() + ", Reading skims into memory");
        ptInputs.readSkims();
        logger.info(getName() + ", Reading taz data into memory");
        ptInputs.readTazData();

        LaborFlows lf = new LaborFlows(ptRb);
        TableDataSet alphaToBetaTable = loadTableDataSet(globalRb,"alpha2beta.file");
        a2b = new AlphaToBeta(alphaToBetaTable);
        lf.setZoneMap(alphaToBetaTable);

        logger.info(getName() + ", Reading Labor Flows");
        lf.readAlphaValues(loadTableDataSet(ptRb,"productionValues.file"),
                           loadTableDataSet(ptRb,"consumptionValues.file"));

        lf.readBetaLaborFlows();
        logger.info(getName() + ", Finished initializing");
        Message doneMsg = mFactory.createMessage();
        doneMsg.setId(MessageID.NODE_INITIALIZED);
        sendTo("TaskMasterQueue",doneMsg);
    }
    
    private TableDataSet loadTableDataSet(ResourceBundle rb,String pathName) {
        String path = ResourceUtil.getProperty(rb, pathName);
        try {
            String fullPath = path;
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(fullPath));
            return table;
            
        } catch (IOException e) {
            logger.fatal(getName() + ", Can't find input table "+path);
            e.printStackTrace();
        }
        return null;
    }
}

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
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.pt.ActivityPurpose;
import com.pb.tlumip.pt.CreateModeChoiceLogsums;
import com.pb.tlumip.pt.PTModelInputs;
import com.pb.tlumip.pt.TourModeChoiceModel;
import com.pb.tlumip.pt.TourModeParameters;

/**
 * This task will calculate a particular mode choice
 * logsum matrix based on the activity purpose and segment
 * that is sent in the message.  The task will also 
 * calculate a collapsed mode choice logsum if necessary.
 * 
 * The task will send the matrix to the PTDafWriter for
 * writing it to disk.  The Writer will then let the signal task
 * know that the calculation is complete.
 * 
 * Created on Jul 20, 2005 
 * @author Christi
 */
public class ModeChoiceLogsumTask extends MessageProcessingTask {
    
    protected static Logger logger = Logger.getLogger(ModeChoiceLogsumTask.class);
    protected static Object lock = new Object();
    protected static boolean initialized = false;
    protected static boolean CALCULATE_MCLOGSUMS = true;
    protected static ArrayList matricesToCollapse;
    protected static AlphaToBeta a2b = null;
    
    CreateModeChoiceLogsums mcLogsumCalculator = new CreateModeChoiceLogsums();
    
    String matrixWriterQueue = "MatrixWriterQueue1";
    
    public void onStart(){
        synchronized (lock) {
            logger.info(getName() + ", Started");
            if (!initialized) {
                logger.info(getName() + ", Initializing PT Model on Node");
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
                
                ResourceBundle ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
                ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));
                
                //set whether you want to calculate the mode choice logsums
                
                String mcLogsumBoolean = ResourceUtil.getProperty(ptRb, "calculate.mc.logsums");
                if(mcLogsumBoolean != null){
                    CALCULATE_MCLOGSUMS = new Boolean(mcLogsumBoolean).booleanValue();
                } //otherwise it has already been initialized to true

                //get the list of Matrices to collapse from properties file
                matricesToCollapse = ResourceUtil.getList(ptRb,"matrices.for.pi");
                
                TableDataSet alphaToBetaTable = loadTableDataSet(globalRb,"alpha2beta.file");
                a2b = new AlphaToBeta(alphaToBetaTable);
            }
        }
        
        
    }
    
    public void onMessage(Message msg){
        logger.info(getName() + " received messageId=" + msg.getId() +
                " message from=" + msg.getSender() + " @time=" + new Date());
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
            
    }
    
    public void createMCLogsums(Message msg) {
        String purpose = String.valueOf(msg.getValue("purpose"));
        Integer segment = (Integer) msg.getValue("segment");

        String purSeg = purpose + segment.toString();

        //Creating the ModeChoiceLogsum Matrix
        logger.info(getName() + ", Creating ModeChoiceLogsumMatrix for purpose: " + purpose +
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
            logger.info(getName() + ", Collapsing ModeChoiceLogsumMatrix for purpose: " + purpose +
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

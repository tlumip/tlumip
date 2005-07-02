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
package com.pb.tlumip.ald.daf;

import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.pt.daf.Scenario;

import java.io.*;
import java.util.ResourceBundle;
import java.util.Date;

/**
 * Author: willison
 * Date: Nov 23, 2004
 * <p/>
 * Created by IntelliJ IDEA.
 */
public class ALDWorkTask extends MessageProcessingTask {

    ResourceBundle rb;

    public void onStart(){
        logger.info( "***" + getName() + " started");
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

        /*  first we need to create the Strings that ALD uses as runtime arguments
            R expects 3 arguments:
                1. the path to the ald_inputs_XXX.R file which is located
                    in the same directory as the ald_XXX.R source code
                2.  the path to the input files (up to the scenario_Name directory)
                3.  the time interval (t) of the current simulation year
            Then we need the full path to the R code itself
        */
        String pathToRCode = ResourceUtil.getProperty(rb, "codePath");
        String pathToRCodeArg = "-" + pathToRCode;

        String pathToIOFiles = ResourceUtil.getProperty(rb, "filePath");
        String pathToIOFilesArg = "-" + pathToIOFiles;

        String yearArg = "-" + timeInterval;

        String rFileName = ResourceUtil.getProperty(rb, "nameOfRCode");
        String rCode = pathToRCode + rFileName;

        String rOut = pathToIOFiles + "t" + timeInterval +"/ald/ald.Rout";

        String execCommand = "R CMD BATCH "+ pathToRCodeArg + " "
                                + pathToIOFilesArg + " " + yearArg + " " + rCode + " " + rOut;
        logger.info("Executing "+execCommand);
        Runtime rt = Runtime.getRuntime();
        try {
            rt.exec(execCommand);

        } catch (IOException e) {
            e.printStackTrace();
        }

        
        logger.info("ALD is done - writing to the alddaf_done file");
        String doneFileName = ResourceUtil.getProperty(rb,"done.file.name");
        File doneFile = new File(pathToIOFiles + "t" + timeInterval + "/ald/" + doneFileName);
        logger.info("Writing to " + doneFile.getAbsolutePath());
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(
                        doneFile));
            writer.println("ALD is done." + new Date());
            writer.close();
            logger.info("ALD is done.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

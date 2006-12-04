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
package com.pb.tlumip.ald;

import com.pb.common.util.ResourceUtil;
import com.pb.tlumip.model.ModelComponent;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * The ALD model component is actually a R program
 * and will be run in R
 * This class will use the Java Runtime class to
 * call the R program.
 * 
 * @author Christi Willison
 * @version Jun 2, 2004
 */
public class ALDModel extends ModelComponent {
    Logger logger = Logger.getLogger("pb.com.tlumip.ald");
    Process process;

    public void startModel(int t){
        
        
        /*  first we need to create the Strings that ALD uses as runtime arguments
            R expects 3 arguments:
                1. the path to the ald_inputs_XXX.R file which is located
                    in the same directory as the ald_XXX.R source code
                2.  the path to the input files (up to the scenario_Name directory)
                3.  the time interval (t) of the current simulation year
            Then we need the full path to the R code itself
        */
        String pathToRCode = ResourceUtil.getProperty(appRb, "codePath");
        String pathToRCodeArg = "-" + pathToRCode;

        String pathToIOFiles = ResourceUtil.getProperty(appRb, "filePath");
        String pathToIOFilesArg = "-" + pathToIOFiles;

        String yearArg = "-" + t;

        String rFileName = ResourceUtil.getProperty(appRb, "nameOfRCode");
        String rCode = pathToRCode + rFileName;

        String rOut = pathToIOFiles + "t" + t +"/ald/ald.Rout";

        String execCommand = "R CMD BATCH "+ pathToRCodeArg + " "
                                + pathToIOFilesArg + " " + yearArg + " " + rCode + " " + rOut;
        logger.info("Executing "+execCommand);
        Runtime rt = Runtime.getRuntime();
        try {
//            process = rt.exec("cmd.exe /c " + execCommand);
//          First delete the indicator file (if there is one) that signals the 
            //java program that the R script has completed.
            File doneFile = new File(pathToIOFiles + "t" + t + "/ald/.RData");
            if(doneFile.exists()) doneFile.delete();
            
            process = rt.exec(execCommand);
            try {
                while(!doneFile.exists())
                    Thread.sleep(2000);
                //Will wait for the .RData file to appear before signaling 
                //that ALD is done.
                logger.info("ALD is done");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ALDModel ald = new ALDModel();
        ald.setApplicationResourceBundle(ResourceUtil.getResourceBundle("ald"));
        ald.startModel(1);
    }

}

/*
 * Copyright 2006 PB Consult Inc.
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
package com.pb.tlumip.pt.ldt;

import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.ldt.RunLDTModels;
import com.pb.tlumip.pt.PTOccupation;

import java.util.ResourceBundle;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Mar 14, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class RunTLUMIPLDTModels extends RunLDTModels {

    public RunTLUMIPLDTModels(){
        super(PTOccupation.NONE);
    }

    public static void main(String[] args) {

        boolean runTourLevelModels = false;
        boolean runHHLevelModels   = false;
        boolean runAutoOwnership   = false;
        boolean writeOnlyLDTHH     = false;

        if (args[0].equals("1")) {
            runHHLevelModels = true;
        } else if (args[0].equals("2")) {
            runHHLevelModels = true;
            runAutoOwnership = true;
        } else {
            runTourLevelModels = true;
        }

        if (args[1].equals("1")) {
            writeOnlyLDTHH = true;
        }

        // info
        logger.info("\n\nStarting LDT model run.");
        long startTime = System.currentTimeMillis();

        ResourceBundle appRb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
        RunLDTModels rm = new RunTLUMIPLDTModels();
        rm.setResourceBundles(appRb, globalRb);

        if (runHHLevelModels)   rm.runHouseholdLevelModels(runAutoOwnership, writeOnlyLDTHH);
        if (runTourLevelModels) rm.startModel(2000, 1);

        // clock time
        long timeSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long timeMinutes = timeSeconds / 60;
        long leftoverSeconds = timeSeconds - timeMinutes*60;
        logger.info("Finished running LDT models in: "
                + timeMinutes + ":" + leftoverSeconds + " minutes\n\n");
    }
}

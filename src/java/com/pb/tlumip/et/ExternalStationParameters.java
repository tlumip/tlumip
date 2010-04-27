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
package com.pb.tlumip.et;

import com.pb.common.datafile.TableDataSet;
import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * ExternalStationParameters is a utility class that provides details on the External Station File.
 *
 * @author Kimberly Grommes
 * @version 1.0, Oct 26, 2007
 *          Created by IntelliJ IDEA.
 */
@Deprecated
public class ExternalStationParameters {

    Logger logger = Logger.getLogger(ExternalStationParameters.class);

    private HashMap<String, Float> hshParameters;

    public ExternalStationParameters(TableDataSet ExternalStationParameters) {

        hshParameters = new HashMap<String, Float>();

        int[] intsExternalStations = ExternalStationParameters.getColumnAsInt("ExSta");
        ExternalStationParameters.buildIndex(1);

        String[] strsFactorsTemp = ExternalStationParameters.getColumnLabels();
        String[] strsFactors = new String[strsFactorsTemp.length-2];
        System.arraycopy(strsFactorsTemp, 2, strsFactors, 0, strsFactors.length);

        for (int intStation: intsExternalStations) {
            for (String strFactor: strsFactors) {
                String strHashKey = Integer.toString(intStation) + "_" + strFactor;
                float fltValue = ExternalStationParameters.getIndexedValueAt(intStation, strFactor);
                hshParameters.put(strHashKey, fltValue);
            }
        }

    }

    public float getValue(int Zone, String factorName) {
        String strHashedFactorName = Integer.toString(Zone) + "_" + factorName;
        if (hshParameters.containsKey(strHashedFactorName)) {
            return hshParameters.get(strHashedFactorName);
        } else {
            logger.info("Factor (" + factorName + ") for zone (" + Zone + ") requested from External Station Parameters does not exist.");
            return 0f;
        }
    }
}

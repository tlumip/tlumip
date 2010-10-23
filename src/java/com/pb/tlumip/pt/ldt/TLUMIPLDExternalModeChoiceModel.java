/*
 * Copyright  2006 PB Consult Inc.
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

import com.pb.common.model.ModelException;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.models.pt.ldt.LDExternalModeChoiceModel;
import com.pb.models.pt.ldt.LDTour;
import com.pb.models.pt.ldt.LDTourModeType;

import java.util.ResourceBundle;
import java.util.Random;

/**
 * Mode choice for long-distance tours with external destinations.  
 * 
 * @author Erhardt
 * @version 1.0 Mar 8, 2007
 *
 */
public class TLUMIPLDExternalModeChoiceModel extends LDExternalModeChoiceModel {

    private static String MATRIX_EXTENSION;
    /**
     * Default constructor. 
     *
     */
    public TLUMIPLDExternalModeChoiceModel() {
        super();
    }
    
    
    /**
     * Initializes class variables.  
     * 
     * @param ptRb
     */
    public void initialize(ResourceBundle ptRb, ResourceBundle globalRb){
        super.initialize(ptRb, globalRb);
        MATRIX_EXTENSION = globalRb.getString("matrix.extension");
        readParameters(); 
        
        // read the distance matrix
        String skimPath = ResourceUtil.getProperty(rb, "highway.assign.previous.skim.path");
        String[] fileNames = ResourceUtil.getArray(rb, "pt.car.offpeak.skims");
        distance = readTravelCost(skimPath + fileNames[1] + MATRIX_EXTENSION, "carOpDist");
    }
    
    
    /**
     * Draws a random number and monte carlo simulation chooses mode.
     * 
     * @param frequencies  The choice frequencies for the modes types.
     * @return The chosen mode.
     */
    private LDTourModeType chooseModeFromFrequency(float[] frequencies, double random) {

        double culmFreq = 0;
        LDTourModeType chosenMode = null;
        LDTourModeType[] mode = LDTourModeType.values();
        for (int i = 0; i < frequencies.length; ++i) {
            culmFreq += frequencies[i];
            if (random < culmFreq) {
                chosenMode = mode[i];
                break;
            }
        }

        // Make sure a pattern was chosen
        if (chosenMode == null) {
            String message = "No external mode chosen with a cumulative " + "probability of " + culmFreq
                    + " and a selector of " + random;
            logger.error(message);
            throw new ModelException(message);
        }
        return chosenMode;
    }
    
    /**
     * Choose a mode for the given tour.  
     */
    public LDTourModeType chooseMode(LDTour tour, boolean sensitivityTesting) {
        long seed = tour.hh.ID*100 + tour.person.memberID + tour.ID + ldExternalModeFixedSeed;
        if(sensitivityTesting) seed += System.currentTimeMillis();

        Random random = new Random();
        random.setSeed(seed);

        LDTourModeType chosenMode; 
        chosenMode = chooseModeFromFrequency(parameters[tour.purpose.ordinal()], random.nextDouble()); 
        return chosenMode;
    }

    
    /**
     * 
     * @param tour The tour of interest.  
     * @return The distance from the home zone to the destination zone.  
     */
    public float getDistance(LDTour tour) {
        float dist = distance.getValueAt(tour.homeTAZ, tour.destinationTAZ);
        return dist; 
    }
}

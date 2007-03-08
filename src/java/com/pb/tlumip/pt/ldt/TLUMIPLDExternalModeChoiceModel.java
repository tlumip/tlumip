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

import java.util.ResourceBundle;

import com.pb.models.pt.ldt.LDExternalModeChoiceModel;
import com.pb.models.pt.ldt.LDTour;
import com.pb.models.pt.ldt.LDTourModeType;

/**
 * TODO Implement this class!!!
 * 
 * @author Erhardt
 * @version 1.0 Mar 8, 2007
 *
 */
public class TLUMIPLDExternalModeChoiceModel extends LDExternalModeChoiceModel {

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
    public void initialize(ResourceBundle ptRb){
        super.initialize(ptRb);
    }
    
    
    /**
     * TODO Implement this method!!!
     *
     */
    public LDTourModeType chooseMode(LDTour tour) {

        LDTourModeType chosenMode = null; 
        return chosenMode;
    }

    
    /**
     * TODO Override in subclass.
     * 
     * @param tour The tour of interest.  
     * @return The distance from the home zone to the destination zone.  
     */
    public float getDistance(LDTour tour) {
        return 0; 
    }
}

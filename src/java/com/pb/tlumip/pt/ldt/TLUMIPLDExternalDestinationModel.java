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

import com.pb.models.pt.ldt.LDExternalDestinationModel;
import com.pb.models.pt.ldt.LDTour;

/**
 * TODO Need to implement this!!!
 * 
 * @author Erhardt
 * @version 1.0 Mar 8, 2007
 *
 */
public class TLUMIPLDExternalDestinationModel  extends LDExternalDestinationModel {
 
    /**
     * Default Constructor    
     */
    public TLUMIPLDExternalDestinationModel() {
        super();
    }
    
    /**
     * Initializes the class.  
     * 
     * @param globalRb
     * @param ptRb
     */
    public void initialize(ResourceBundle globalRb, ResourceBundle ptRb) {

        super.initialize(globalRb, ptRb);        
    }
    
    /**
     * TODO Need to implement this!!!
     * 
     * @return  The ID of the chosen TAZ.  
     */
    public int chooseTaz(LDTour tour) {
        
        return 0; 
    }
}

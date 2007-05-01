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

import com.pb.common.matrix.NDimensionalMatrix;
import com.pb.models.pt.ldt.LDTrip;

/**
 * @author Erhardt
 * @version 1.0 Apr 27, 2007
 *
 */
public class LDTTripAccumulator {

    NDimensionalMatrix trips; 
    
    LDTTripAccumulator() {        
    }
    
    
    
    private int getOriginIndex(LDTrip t) {
        return 0; 
    }
    
    private int getDestinationIndex(LDTrip t) {
        return 0; 
    }
           
    private int getModeIndex(LDTrip t) {
        return t.tripMode.ordinal(); 
    }
    
    private int getPeriodIndex(LDTrip t) {
        return 0; 
    }
}

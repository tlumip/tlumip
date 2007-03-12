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

import com.pb.models.pt.PTPerson;
import com.pb.models.pt.ldt.LDInternalExternalPersonAttributes;
import com.pb.tlumip.pt.PTOccupation;

/**
 * TODO Set occupation codes appropriately here!!!
 * 
 * @author Erhardt
 * @version 1.0 Mar 7, 2007
 *
 */
public class TLUMIPLDInternalExternalPersonAttributes extends LDInternalExternalPersonAttributes {

    public TLUMIPLDInternalExternalPersonAttributes(){
        super();
    }
    /**
     * Code the occupation, student, and age variables based on the attributes
     * of a single household member.
     *
     * @param p
     */
    public void codePersonAttributes(PTPerson p){

        persID = p.memberID;

        // set worker flag
        worker = 0;
        if (p.employed) worker = 1;

        // set age flags
        agelt25 = 0;
        age5564 = 0;
        age65p  = 0;
        if (p.age < 25) agelt25 = 1;
        else if (p.age >= 55 && p.age < 65) age5564 = 1;
        else if (p.age >= 65) age65p = 1;

        // set occupation flags
        occConstruct  = 0;
        occFinInsReal = 0;
        occPubAdmin   = 0;
        occEducation  = 0;
        occMedical    = 0;

        if (p.employed) {
            if (p.occupation == PTOccupation.HEALTH)                 occMedical   = 1; 
            else if (p.occupation == PTOccupation.POST_SECONDARY_ED) occEducation = 1;     
            else if (p.occupation == PTOccupation.OTHER_ED)          occEducation = 1;                 
        }
    }

}

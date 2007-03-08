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

import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;
import com.pb.models.pt.ldt.LDBinaryChoicePersonAttributes;
import com.pb.tlumip.pt.PTOccupation;

/**
 * TODO Need to set the occupation flags specific to TLUMIP here!
 * 
 * 
 * @author Erhardt
 * @version 1.0 Mar 7, 2007
 *
 */
public class TLUMIPLDBinaryChoicePersonAttributes extends LDBinaryChoicePersonAttributes {


    public TLUMIPLDBinaryChoicePersonAttributes(){
        super();
    }

    /**
     * Code the person attributes based on the attributes
     * of all household members.  Use this for the HOUSEHOLD LD
     * tour binary choice model.
     *
     * @param h
     */
    public void codePersonAttributes(PTHousehold h){
        PTPerson p;
        occ_ag_farm_mine=0;
        occ_manufactur=0;
        occ_trans_comm=0;
        occ_wholesale=0;
        occ_finance_re=0;
        occ_prof_sci=0;
        occ_other=0;
        schtype_college=0;
        male=0;
        age=0;
        age_sq=0;

        boolean ag_farm_mine=false;
        boolean manufactur=false;
        boolean trans_comm=false;
        boolean wholesale=false;
        boolean finance_re=false;
        boolean prof_sci=false;
        boolean other=false;
        boolean college=false;

        for(int i=0;i<h.persons.length;++i){
            p=h.persons[i];

//            if(p.occupation== PTOccupation.AGRICULTURE)
//                ag_farm_mine=true;
//            else if(p.occupation==PTOccupation.ASSEMBLY)
//                manufactur=true;
//            else if(p.occupation==PTOccupation.TRANSPORT)
//                trans_comm=true;
//            else if(p.occupation==PTOccupation.NON_RETAIL_SALES)
//                wholesale=true;
//            else if(p.occupation==PTOccupation.BUSINESS_FINANCE)
//                finance_re=true;
//            else if(p.occupation==PTOccupation.PROFESSIONAL)
//                prof_sci=true;
//            else if(p.occupation!=PTOccupation.NONE)
//                other=true;

            if(p.age>=18 && p.student)
                college=true;
        }

        if(ag_farm_mine)
            occ_ag_farm_mine=1;
        if(manufactur)
            occ_manufactur=1;
        if(trans_comm)
            occ_trans_comm=1;
        if(wholesale)
            occ_wholesale=1;
        if(finance_re)
            occ_finance_re=1;
        if(prof_sci)
            occ_prof_sci=1;
        if(other)
            occ_other=1;
        if(college)
            schtype_college=1;

    }

    /**
     * Code the occupation, student, and age variables based on the attributes
     * of a single household member.  Use this for the WORKRELATED AND OTHER
     * LD tour binary choice models.
     *
     * @param p
     */
    public void codePersonAttributes(PTPerson p){

        occ_ag_farm_mine=0;
        occ_manufactur=0;
        occ_trans_comm=0;
        occ_wholesale=0;
        occ_finance_re=0;
        occ_prof_sci=0;
        occ_other=0;
        schtype_college=0;
        male=0;
        age=0;
        age_sq=0;

//        if(p.occupation==PTOccupation.AGRICULTURE)
//            occ_ag_farm_mine=1;
//        else if(p.occupation==PTOccupation.ASSEMBLY)
//            occ_manufactur=1;
//        else if(p.occupation==PTOccupation.TRANSPORT)
//            occ_trans_comm=1;
//        else if(p.occupation==PTOccupation.NON_RETAIL_SALES)
//            occ_wholesale=1;
//        else if(p.occupation==PTOccupation.BUSINESS_FINANCE)
//            occ_finance_re=1;
//        else if(p.occupation==PTOccupation.PROFESSIONAL)
//            occ_prof_sci=1;
//        else if(p.occupation!=PTOccupation.NONE)
//            occ_other=1;

        if(p.age>=18 && p.student)
            schtype_college=1;

        if(p.female)
            male=1;

        age=p.age;
        age_sq=p.age^2;
    }

}

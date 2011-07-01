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
package com.pb.tlumip.pt;

import com.pb.common.math.MathUtil;
import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.StopDestinationParameters;
import com.pb.models.pt.Taz;
import static com.pb.models.pt.TourDestinationParameters.*;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Oct 24, 2006
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class TLUMIPTaz extends Taz {


    public void setTourSizeTerms(float[][] tdpd){
        if (employment.containsKey("RETAIL TRADE-Retail"))
            setTourSizeTermsPI(tdpd);
        else
            setTourSizeTermsAA(tdpd);
    }

    private void setTourSizeTermsPI(float[][] tdpd){
        float sizeTerm;

        ActivityPurpose[] purposes = ActivityPurpose.values();
        for(int i=0;i<purposes.length;i++){
            float destParams[] = tdpd[i];


            sizeTerm = (destParams[RETAIL] * employment.get("RETAIL TRADE-Retail"))
              + (destParams[OTHERSERVICES] * (employment.get("FIRE BUSINESS AND PROFESSIONAL SERVICES")  +
                                                    employment.get("PERSONAL AND OTHER SERVICES AND AMUSEMENTS")));
              sizeTerm += (destParams[HOUSEHOLDS] * households);
              sizeTerm += (destParams[GOVERNMENT] * (employment.get("GOVERNMENT ADMINISTRATION-Office") +
                                                employment.get("GOVERNMENT ADMINISTRATION-Government Support")));
              sizeTerm += (destParams[HEALTH] * (employment.get("HEALTH SERVICES-Office") + employment.get("HEALTH SERVICES-Institutional") +
                                         employment.get("HEALTH SERVICES-Hospital")));
              sizeTerm += (destParams[TRANSPORTATION] * (employment.get("TRANSPORT-Office") + employment.get("TRANSPORT-Depot")));
              sizeTerm += (destParams[HIGHEREDUCATION] * employment.get("HIGHER EDUCATION"));
              sizeTerm += (destParams[K12EDUCATION] * employment.get("LOWER EDUCATION-Grade School"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("ACCOMMODATIONS"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("AGRICULTURE AND MINING-Agriculture"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("AGRICULTURE AND MINING-Office"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("COMMUNICATIONS AND UTILITIES-Light Industry"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("COMMUNICATIONS AND UTILITIES-Office"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("CONSTRUCTION"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("ELECTRONICS AND INSTRUMENTS-Light Industry"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("ELECTRONICS AND INSTRUMENTS-Office"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("FOOD PRODUCTS-Heavy Industry"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("FOOD PRODUCTS-Light Industry"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("FOOD PRODUCTS-Office"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("FORESTRY AND LOGGING"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("HOMEBASED SERVICES"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("LOWER EDUCATION-Office"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("LUMBER AND WOOD PRODUCTS-Heavy Industry"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("LUMBER AND WOOD PRODUCTS-Office"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("OTHER DURABLES-Heavy Industry"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("OTHER DURABLES-Light Industry"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("OTHER DURABLES-Office"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("OTHER NON-DURABLES-Heavy Industry"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("OTHER NON-DURABLES-Light Industry"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("OTHER NON-DURABLES-Office"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("PULP AND PAPER-Heavy Industry"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("PULP AND PAPER-Office"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("RETAIL TRADE-Office"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("WHOLESALE TRADE-Office"));
              sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("WHOLESALE TRADE-Warehouse"));

            if (trace) {
                    logger.info("tour size term for purpose " + i + " taz "+ zoneNumber + ": "
                            +         (destParams[RETAIL]           + " * " + employment.get("RETAIL TRADE-Retail"))
                            + " + " + (destParams[OTHERSERVICES]    + " * " + (employment.get("FIRE BUSINESS AND PROFESSIONAL SERVICES")
                            + " + " +                                           employment.get("PERSONAL AND OTHER SERVICES AND AMUSEMENTS")))
                            + " + " + (destParams[HOUSEHOLDS]       + " * " + households)
                            + " + " + (destParams[GOVERNMENT]       + " * " + (employment.get("GOVERNMENT ADMINISTRATION-Office")
                            + " + " +                                            employment.get("GOVERNMENT ADMINISTRATION-Government Support")))
                            + " + " + (destParams[HEALTH]           + " * " + (employment.get("HEALTH SERVICES-Office") + employment.get("HEALTH SERVICES-Institutional")
                            + " + " +                                            employment.get("HEALTH SERVICES-Hospital")))
                            + " + " + (destParams[TRANSPORTATION]   + " * " + (employment.get("TRANSPORT-Office" + employment.get("TRANSPORT-Depot"))))
                            + " + " + (destParams[HIGHEREDUCATION]  + " * " + employment.get("HIGHER EDUCATION"))
                            + " + " + (destParams[K12EDUCATION]     + " * " + employment.get("LOWER EDUCATION-Grade School"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("ACCOMMODATIONS"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("AGRICULTURE AND MINING-Agriculture"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("AGRICULTURE AND MINING-Office"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("COMMUNICATIONS AND UTILITIES-Light Industry"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("COMMUNICATIONS AND UTILITIES-Office"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("CONSTRUCTION"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("ELECTRONICS AND INSTRUMENTS-Light Industry"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("ELECTRONICS AND INSTRUMENTS-Office"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("FOOD PRODUCTS-Heavy Industry"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("FOOD PRODUCTS-Light Industry"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("FOOD PRODUCTS-Office"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("FORESTRY AND LOGGING"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("HOMEBASED SERVICES"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("LOWER EDUCATION-Office"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("LUMBER AND WOOD PRODUCTS-Heavy Industry"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("LUMBER AND WOOD PRODUCTS-Office"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("OTHER DURABLES-Heavy Industry"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("OTHER DURABLES-Light Industry"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("OTHER DURABLES-Office"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("OTHER NON-DURABLES-Heavy Industry"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("OTHER NON-DURABLES-Light Industry"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("OTHER NON-DURABLES-Office"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("PULP AND PAPER-Heavy Industry"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("PULP AND PAPER-Office"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("RETAIL TRADE-Office"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("WHOLESALE TRADE-Office"))
                            + " + " + (destParams[OTHEREMPLOYMENT]  + " * " + employment.get("WHOLESALE TRADE-Warehouse")));                                              

                      logger.info("Size term: " + sizeTerm);
                }

                tourSizeTerm[i] = sizeTerm;
                if(sizeTerm>0)
                    tourLnSizeTerm[i] = MathUtil.log(sizeTerm);

                if(trace) logger.info("Log size term "+tourLnSizeTerm[i]);

        }
        tourSizeTermsSet = true;
    }//end setTourDestinationSizeTerms

    private void setTourSizeTermsAA(float[][] tdpd){
        float sizeTerm;

        ActivityPurpose[] purposes = ActivityPurpose.values();
        for(int i=0;i<purposes.length;i++){
            float destParams[] = tdpd[i];


            sizeTerm = 0.0f;
            sizeTerm += (destParams[HOUSEHOLDS] * households);
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("RES_agmin_ag"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("RES_forst_log"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("RES_offc_off"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("ENGY_elec_hi"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("ENGY_ngas_hi"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("ENGY_ptrl_hi"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("ENGY_offc_off"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("CNST_main_xxx"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("CNST_nres_xxx"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("CNST_othr_xxx"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("CNST_res_xxx"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("CNST_offc_off"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("MFG_food_hi"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("MFG_food_li"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("MFG_htec_hi"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("MFG_htec_li"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("MFG_hvtw_hi"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("MFG_hvtw_li"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("MFG_lvtw_hi"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("MFG_wdppr_hi"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("MFG_offc_off"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("WHSL_whsl_ware"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("WHSL_offc_off"));
            sizeTerm += (destParams[RETAIL] * employment.get("RET_auto_ret"));
            sizeTerm += (destParams[RETAIL] * employment.get("RET_stor_ret"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("RET_stor_off"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("RET_nstor_off"));
            sizeTerm += (destParams[TRANSPORTATION] * employment.get("TRNS_trns_ware"));
            sizeTerm += (destParams[TRANSPORTATION] * employment.get("TRNS_trns_off"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("INFO_info_off_li"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("INFO_info_off"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("UTL_othr_off_li"));
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("UTL_othr_off"));
            sizeTerm += (destParams[OTHERSERVICES] * employment.get("FIRE_fnin_off"));
            sizeTerm += (destParams[OTHERSERVICES] * employment.get("FIRE_real_off"));
            sizeTerm += (destParams[HEALTH] * employment.get("HLTH_hosp_hosp"));
            sizeTerm += (destParams[HEALTH] * employment.get("HLTH_care_inst"));
            sizeTerm += (destParams[HEALTH] * employment.get("HLTH_othr_off_li"));
            sizeTerm += (destParams[K12EDUCATION] * employment.get("K12_k12_k12"));
            sizeTerm += (destParams[K12EDUCATION] * employment.get("K12_k12_off"));
            sizeTerm += (destParams[HIGHEREDUCATION] * employment.get("HIED_hied_off_inst"));
            sizeTerm += (destParams[OTHERSERVICES] * employment.get("ENT_ent_ret"));
            sizeTerm += (destParams[OTHERSERVICES] * employment.get("HOSP_acc_acc"));
            sizeTerm += (destParams[OTHERSERVICES] * employment.get("HOSP_eat_ret_acc"));
            sizeTerm += (destParams[OTHERSERVICES] * employment.get("SERV_tech_off"));
            sizeTerm += (destParams[OTHERSERVICES] * employment.get("SERV_site_li"));
            sizeTerm += (destParams[OTHERSERVICES] * employment.get("SERV_home_xxx"));
            sizeTerm += (destParams[OTHERSERVICES] * employment.get("SERV_bus_off"));
            sizeTerm += (destParams[OTHERSERVICES] * employment.get("SERV_nonp_off_inst"));
            sizeTerm += (destParams[OTHERSERVICES] * employment.get("SERV_stor_ret"));
            sizeTerm += (destParams[GOVERNMENT] * employment.get("GOV_admn_gov"));
            sizeTerm += (destParams[GOVERNMENT] * employment.get("GOV_offc_off"));


            if (trace) {      
                    logger.info("tour size term for purpose " + i + " taz "+ zoneNumber + ": "
                            + " + " + destParams[HOUSEHOLDS]    + " * " + (households)
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("RES_agmin_ag"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("RES_forst_log"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("RES_offc_off"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("ENGY_elec_hi"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("ENGY_ngas_hi"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("ENGY_ptrl_hi"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("ENGY_offc_off"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("CNST_main_xxx"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("CNST_nres_xxx"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("CNST_othr_xxx"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("CNST_res_xxx"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("CNST_offc_off"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("MFG_food_hi"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("MFG_food_li"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("MFG_htec_hi"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("MFG_htec_li"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("MFG_hvtw_hi"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("MFG_hvtw_li"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("MFG_lvtw_hi"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("MFG_wdppr_hi"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("MFG_offc_off"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("WHSL_whsl_ware"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("WHSL_offc_off"))
                            + " + " + destParams[RETAIL]    + " * " + (employment.get("RET_auto_ret"))
                            + " + " + destParams[RETAIL]    + " * " + (employment.get("RET_stor_ret"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("RET_stor_off"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("RET_nstor_off"))
                            + " + " + destParams[TRANSPORTATION]    + " * " + (employment.get("TRNS_trns_ware"))
                            + " + " + destParams[TRANSPORTATION]    + " * " + (employment.get("TRNS_trns_off"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("INFO_info_off_li"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("INFO_info_off"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("UTL_othr_off_li"))
                            + " + " + destParams[OTHEREMPLOYMENT]    + " * " + (employment.get("UTL_othr_off"))
                            + " + " + destParams[OTHERSERVICES]    + " * " + (employment.get("FIRE_fnin_off"))
                            + " + " + destParams[OTHERSERVICES]    + " * " + (employment.get("FIRE_real_off"))
                            + " + " + destParams[HEALTH]    + " * " + (employment.get("HLTH_hosp_hosp"))
                            + " + " + destParams[HEALTH]    + " * " + (employment.get("HLTH_care_inst"))
                            + " + " + destParams[HEALTH]    + " * " + (employment.get("HLTH_othr_off_li"))
                            + " + " + destParams[K12EDUCATION]    + " * " + (employment.get("K12_k12_k12"))
                            + " + " + destParams[K12EDUCATION]    + " * " + (employment.get("K12_k12_off"))
                            + " + " + destParams[HIGHEREDUCATION]    + " * " + (employment.get("HIED_hied_off_inst"))
                            + " + " + destParams[OTHERSERVICES]    + " * " + (employment.get("ENT_ent_ret"))
                            + " + " + destParams[OTHERSERVICES]    + " * " + (employment.get("HOSP_acc_acc"))
                            + " + " + destParams[OTHERSERVICES]    + " * " + (employment.get("HOSP_eat_ret_acc"))
                            + " + " + destParams[OTHERSERVICES]    + " * " + (employment.get("SERV_tech_off"))
                            + " + " + destParams[OTHERSERVICES]    + " * " + (employment.get("SERV_site_li"))
                            + " + " + destParams[OTHERSERVICES]    + " * " + (employment.get("SERV_home_xxx"))
                            + " + " + destParams[OTHERSERVICES]    + " * " + (employment.get("SERV_bus_off"))
                            + " + " + destParams[OTHERSERVICES]    + " * " + (employment.get("SERV_nonp_off_inst"))
                            + " + " + destParams[OTHERSERVICES]    + " * " + (employment.get("SERV_stor_ret"))
                            + " + " + destParams[GOVERNMENT]    + " * " + (employment.get("GOV_admn_gov"))
                            + " + " + destParams[GOVERNMENT]    + " * " + (employment.get("GOV_offc_off")));

                      logger.info("Size term: " + sizeTerm);
                }

                tourSizeTerm[i] = sizeTerm;
                if(sizeTerm>0)
                    tourLnSizeTerm[i] = MathUtil.log(sizeTerm);

                if(trace) logger.info("Log size term "+tourLnSizeTerm[i]);

        }
        tourSizeTermsSet = true;
    }//end setTourDestinationSizeTerms

    public void setStopSizeTerms(float[][] params){
        if (employment.containsKey("RETAIL TRADE-Retail"))
            setStopSizeTermsPI(params);
        else
            setStopSizeTermsAA(params);
    }

    public void setStopSizeTermsPI(float[][] params){
        float sizeTerm;
        ActivityPurpose[] purposes = ActivityPurpose.values();
        for(int i=0;i<purposes.length;i++){

        	float[] p = params[i];

            sizeTerm =    (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("RETAIL TRADE-Retail"))
                        + (p[StopDestinationParameters.TOTAL_RETAIL] *   employment.get("FIRE BUSINESS AND PROFESSIONAL SERVICES"))
                        + (p[StopDestinationParameters.TOTAL_RETAIL] *   employment.get("PERSONAL AND OTHER SERVICES AND AMUSEMENTS"))
                        + (p[StopDestinationParameters.TOTAL_RETAIL] *   employment.get("ACCOMMODATIONS"))
                        + (p[StopDestinationParameters.RETAIL_PROD] *    employment.get("RETAIL TRADE-Retail"))
                        + (p[StopDestinationParameters.RETAIL_OTHSRVC] * employment.get("FIRE BUSINESS AND PROFESSIONAL SERVICES"))
                        + (p[StopDestinationParameters.RETAIL_OTHSRVC] * employment.get("PERSONAL AND OTHER SERVICES AND AMUSEMENTS"))
                        + (p[StopDestinationParameters.RETAIL_OTHSRVC] * employment.get("ACCOMMODATIONS"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("AGRICULTURE AND MINING-Agriculture"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("AGRICULTURE AND MINING-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("COMMUNICATIONS AND UTILITIES-Light Industry"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("COMMUNICATIONS AND UTILITIES-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("CONSTRUCTION"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("ELECTRONICS AND INSTRUMENTS-Light Industry"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("ELECTRONICS AND INSTRUMENTS-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("FOOD PRODUCTS-Heavy Industry"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("FOOD PRODUCTS-Light Industry"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("FOOD PRODUCTS-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("FORESTRY AND LOGGING"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("GOVERNMENT ADMINISTRATION-Government Support"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("GOVERNMENT ADMINISTRATION-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("HEALTH SERVICES-Hospital"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("HEALTH SERVICES-Institutional"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("HEALTH SERVICES-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("HIGHER EDUCATION"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("HOMEBASED SERVICES"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("LOWER EDUCATION-Grade School"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("LOWER EDUCATION-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("LUMBER AND WOOD PRODUCTS-Heavy Industry"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("LUMBER AND WOOD PRODUCTS-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("OTHER DURABLES-Heavy Industry"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("OTHER DURABLES-Light Industry"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("OTHER DURABLES-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("OTHER NON-DURABLES-Heavy Industry"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("OTHER NON-DURABLES-Light Industry"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("OTHER NON-DURABLES-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("PULP AND PAPER-Heavy Industry"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("PULP AND PAPER-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("RETAIL TRADE-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("RETAIL TRADE-Retail"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("TRANSPORT-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("WHOLESALE TRADE-Office"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("WHOLESALE TRADE-Warehouse"))
                        + (p[StopDestinationParameters.GRADESCHOOL] * employment.get("LOWER EDUCATION-Grade School"))
                        + (p[StopDestinationParameters.HHS] * households);


            stopSizeTerm[i] = sizeTerm;
            if (sizeTerm > 0)
                stopLnSizeTerm[i] = MathUtil.log(sizeTerm);
        }
        stopSizeTermsSet = true;
    }

    public void setStopSizeTermsAA(float[][] params){
        float sizeTerm;
        ActivityPurpose[] purposes = ActivityPurpose.values();
        for(int i=0;i<purposes.length;i++){

        	float[] p = params[i];

            sizeTerm = (p[StopDestinationParameters.HHS] * households)
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("RES_agmin_ag"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("RES_forst_log"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("RES_offc_off"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("ENGY_elec_hi"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("ENGY_ngas_hi"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("ENGY_ptrl_hi"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("ENGY_offc_off"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("CNST_main_xxx"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("CNST_nres_xxx"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("CNST_othr_xxx"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("CNST_res_xxx"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("CNST_offc_off"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("MFG_food_hi"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("MFG_food_li"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("MFG_htec_hi"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("MFG_htec_li"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("MFG_hvtw_hi"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("MFG_hvtw_li"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("MFG_lvtw_hi"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("MFG_wdppr_hi"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("MFG_offc_off"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("WHSL_whsl_ware"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("WHSL_offc_off"))
                    + (p[StopDestinationParameters.RETAIL_PROD] *     employment.get("RET_auto_ret"))
                    + (p[StopDestinationParameters.RETAIL_PROD] *     employment.get("RET_stor_ret"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("RET_stor_off"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("RET_nstor_off"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("TRNS_trns_ware"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("TRNS_trns_off"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("INFO_info_off_li"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("INFO_info_off"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("UTL_othr_off_li"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("UTL_othr_off"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("FIRE_fnin_off"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("FIRE_real_off"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("HLTH_hosp_hosp"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("HLTH_care_inst"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("HLTH_othr_off_li"))
                    + (p[StopDestinationParameters.GRADESCHOOL] *     employment.get("K12_k12_k12"))
                    + (p[StopDestinationParameters.GRADESCHOOL] *     employment.get("K12_k12_off"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("HIED_hied_off_inst"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("ENT_ent_ret"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("HOSP_acc_acc"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("HOSP_eat_ret_acc"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("SERV_tech_off"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("SERV_site_li"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("SERV_home_xxx"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("SERV_bus_off"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("SERV_nonp_off_inst"))
                    + (p[StopDestinationParameters.RETAIL_OTHSRVC] *     employment.get("SERV_stor_ret"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("GOV_admn_gov"))
                    + (p[StopDestinationParameters.NON_RETAIL] *     employment.get("GOV_offc_off"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("RET_auto_ret"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("RET_stor_ret"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("RET_stor_off"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("RET_nstor_off"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("FIRE_fnin_off"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("FIRE_real_off"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("ENT_ent_ret"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("HOSP_acc_acc"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("HOSP_eat_ret_acc"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("SERV_tech_off"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("SERV_site_li"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("SERV_home_xxx"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("SERV_bus_off"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("SERV_nonp_off_inst"))
                    + (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("SERV_stor_ret"));


            stopSizeTerm[i] = sizeTerm;
            if (sizeTerm > 0)
                stopLnSizeTerm[i] = MathUtil.log(sizeTerm);
        }
        stopSizeTermsSet = true;
    }
}

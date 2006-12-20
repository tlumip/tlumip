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
        float sizeTerm;
        for(String key: employment.keySet()){
            logger.info("key: "  + key);
       }

        ActivityPurpose[] purposes = ActivityPurpose.values();
        int lineNumber = 46;
        for(int i=0;i<purposes.length;i++){
                sizeTerm = 0.0f;
                float destParams[] = tdpd[i];
            try {
                lineNumber++;
                sizeTerm = (destParams[RETAIL] * employment.get("RETAIL TRADE-Retail"))
                  + (destParams[OTHERSERVICES] * (employment.get("FIRE BUSINESS AND PROFESSIONAL SERVICES")  +
                                                        employment.get("PERSONAL AND OTHER SERVICES AND AMUSEMENTS")));
                  lineNumber++;sizeTerm += (destParams[HOUSEHOLDS] * households);
                  lineNumber++;sizeTerm += (destParams[GOVERNMENT] * (employment.get("GOVERNMENT ADMINISTRATION-Office") +
                                                    employment.get("GOVERNMENT ADMINISTRATION-Government Support")));
                  lineNumber++;sizeTerm += (destParams[HEALTH] * (employment.get("HEALTH SERVICES-Office" + employment.get("HEALTH SERVICES-Institutional") +
                                             employment.get("HEALTH SERVICES-Hospital"))));
                  lineNumber++;sizeTerm += (destParams[TRANSPORTATION] * (employment.get("TRANSPORT-Office" + employment.get("TRANSPORT-Depot"))));
                  lineNumber++;sizeTerm += (destParams[HIGHEREDUCATION] * employment.get("HIGHER EDUCATION"));
                  lineNumber++;sizeTerm += (destParams[K12EDUCATION] * employment.get("LOWER EDUCATION-Grade School"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("ACCOMMODATIONS"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("AGRICULTURE AND MINING-Agriculture"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("AGRICULTURE AND MINING-Office"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("COMMUNICATIONS AND UTILITIES-Light Industry"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("COMMUNICATIONS AND UTILITIES-Office"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("CONSTRUCTION"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("ELECTRONICS AND INSTRUMENTS-Light Industry"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("ELECTRONICS AND INSTRUMENTS-Office"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("FOOD PRODUCTS-Heavy Industry"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("FOOD PRODUCTS-Light Industry"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("FOOD PRODUCTS-Office"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("FORESTRY AND LOGGING"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("HOMEBASED SERVICES"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("LOWER EDUCATION-Office"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("LUMBER AND WOOD PRODUCTS-Heavy Industry"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("LUMBER AND WOOD PRODUCTS-Office"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("OTHER DURABLES-Heavy Industry"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("OTHER DURABLES-Light Industry"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("OTHER DURABLES-Office"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("OTHER NON-DURABLES-Heavy Industry"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("OTHER NON-DURABLES-Light Industry"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("OTHER NON-DURABLES-Office"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("PULP AND PAPER-Heavy Industry"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("PULP AND PAPER-Office"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("RETAIL TRADE-Office"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("WHOLESALE TRADE-Office"));
                  lineNumber++;sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("WHOLESALE TRADE-Warehouse"));
            } catch (Exception e) {
                logger.info("Null pointer is in line: " + lineNumber);
            }

            if (trace) {
                    logger.info("tour size term for purpose " + i + " taz "+ zoneNumber + ": "
                            +         (destParams[RETAIL]           + " * " + employment.get("RETAIL TRADE-Retail"))
                            + " + " + (destParams[OTHERSERVICES]    + " * " + (employment.get("FIRE BUSINESS AND PROFESSIONAL SERVICES")
                            + " + " +                                           employment.get("PERSONAL AND OTHER SERVICES AND AMUSEMENTS")))
                            + " + " + (destParams[HOUSEHOLDS]       + " * " + households)
                            + " + " + (destParams[GOVERNMENT]       + " * " + (employment.get("GOVERNMENT ADMINISTRATION-Office")
                            + " + " +                                            employment.get("GOVERNMENT ADMINISTRATION-Government Support")))
                            + " + " + (destParams[HEALTH]           + " * " + (employment.get("HEALTH SERVICES-Office" + employment.get("HEALTH SERVICES-Institutional")
                            + " + " +                                            employment.get("HEALTH SERVICES-Hospital"))))
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

    public void setStopSizeTerms(float[][] params){
        float sizeTerm;
        ActivityPurpose[] purposes = ActivityPurpose.values();
        for(int i=0;i<purposes.length;i++){

        	float[] p = params[i];

            sizeTerm =  (p[StopDestinationParameters.TOTAL_RETAIL] *     employment.get("RETAIL TRADE-Retail"))
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
}

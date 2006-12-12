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

        ActivityPurpose[] purposes = ActivityPurpose.values();
        for(int i=0;i<purposes.length;i++){

                float destParams[] = tdpd[i];
                sizeTerm = (destParams[RETAIL] * employment.get("RETAIL TRADE-Office"))
                  + (destParams[OTHERSERVICES] * employment.get("OTHER DURABLES-Office"))
                  + (destParams[HOUSEHOLDS] * households)
                  + (destParams[GOVERNMENT] * employment.get("GOVERNMENT ADMINISTRATION-Office"))
                  + (destParams[HEALTH] * employment.get("HEALTH SERVICES-Office"))
                  + (destParams[TRANSPORTATION] * employment.get("TRANSPORT-Office"))
                  + (destParams[HIGHEREDUCATION] * employment.get("HIGHER EDUCATION"))
                  + (destParams[K12EDUCATION] * employment.get("LOWER EDUCATION-Grade School"))
                  + (destParams[OTHEREMPLOYMENT] * employment.get("AGRICULTURE AND MINING-Office"))
                  + (destParams[OTHEREMPLOYMENT] * employment.get("Primary Metal Products Production"))
                  + (destParams[OTHEREMPLOYMENT] * employment.get("OTHER DURABLES-Office"))
                  + (destParams[OTHEREMPLOYMENT] * employment.get("OTHER DURABLES-Heavy Industry"))
                  + (destParams[OTHEREMPLOYMENT] * employment.get("WHOLESALE TRADE-Warehouse"))
                  + (destParams[OTHEREMPLOYMENT] * employment.get("ACCOMMODATIONS"))
                  + (destParams[OTHEREMPLOYMENT] * employment.get("TRANSPORT-Depot"));

                if (trace) {
                    logger.info("tour size term for purpose " + i + " taz "+ zoneNumber + ": "
                            + destParams[RETAIL] + " * " + employment.get("RETAIL TRADE-Office")
                            + " + " + destParams[OTHERSERVICES] + " * " + employment.get("Other Services")
                            + " + " + destParams[HOUSEHOLDS] + " * " + households
                            + " + " + destParams[GOVERNMENT] + " * " + employment.get("Government and Other")
                            + " + " + destParams[HEALTH] + " * " + employment.get("Health Care")
                            + " + " + destParams[TRANSPORTATION] + " * " + employment.get("Transportation Handling")
                            + " + " + destParams[HIGHEREDUCATION] + " * " + employment.get("HIGHER EDUCATION")
                            + " + " + destParams[K12EDUCATION] + " * " + employment.get("LOWER EDUCATION-Grade School")
                            + " + " + destParams[OTHEREMPLOYMENT] + " * " + employment.get("Agriculture Forestry and Fisheries Production")
                            + " + " + destParams[OTHEREMPLOYMENT] + " * " + employment.get("Primary Metal Products Production")
                            + " + " + destParams[OTHEREMPLOYMENT] + " * " + employment.get("Light Industry Production")
                            + " + " + destParams[OTHEREMPLOYMENT] + " * " + employment.get("Heavy Industry Production")
                            + " + " + destParams[OTHEREMPLOYMENT] + " * " + employment.get("Wholesale Production")
                            + " + " + destParams[OTHEREMPLOYMENT] + " * " + employment.get("ACCOMMODATIONS")
                            + " + " + destParams[OTHEREMPLOYMENT] + " * " + employment.get("Transportation Equipment Production"));
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

            sizeTerm =  (p[StopDestinationParameters.TOTAL_RETAIL] * (employment.get("Retail Production") + employment.get("Other Services")))
                        + (p[StopDestinationParameters.RETAIL_PROD] * employment.get("Retail Production"))
                        + (p[StopDestinationParameters.RETAIL_OTHSRVC] * employment.get("Other Services"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("Agriculture Forestry and Fisheries Production"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("Primary Metal Products Production"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("Light Industry Production"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("Heavy Industry Production"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("Transportation Equipment Production"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("Wholesale Production"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("Construction"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("Health Care"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("Transportation Handling"))
                        + (p[StopDestinationParameters.NON_RETAIL] * employment.get("Government and Other"))
                        + (p[StopDestinationParameters.GRADESCHOOL] * employment.get("LOWER EDUCATION-Grade School"))
                        + (p[StopDestinationParameters.HHS] * households);


            stopSizeTerm[i] = sizeTerm;
            if (sizeTerm > 0)
                stopLnSizeTerm[i] = MathUtil.log(sizeTerm);
        }
        stopSizeTermsSet = true;
    }
}

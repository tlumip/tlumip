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
package com.pb.tlumip.pc;

/**
 * Enumeration of variables used in PC
 *
 * @author Christi Willison
 * @version  Sep 29, 2003
 * Created by IntelliJ IDEA.
 */


public class DashboardDataKey {

    public static final String MSG_ID = "Id";
    public static final String DASHBOARDDATA_BEAN = "DASHBOARDDATA_BEAN";

//    Field names for various DataBeans
    public static final String COMMODITY_NAME = "commodityName";
    public static final String COMMODITY_PRICE = "commodityPrice";
    public static final String COMMODITY_SURPLUS = "commoditySurplus";
    public static final String ITERATION_NUMBER = "iterationNumber";
    public static final String STEP_SCALE = "stepScale";
    public static final String MERIT_MEASURE = "meritMeasure";

//    Cache names for the various DataCaches
    public static final String MERIT_MEASURE_CACHE_NAME = "PIMeritMeasure";
    public static final String STEP_SCALE_CACHE_NAME = "PIStepScale";

}

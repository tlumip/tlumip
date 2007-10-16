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
package com.pb.tlumip.pt.ldt;

import com.pb.models.pt.ldt.LDInternalExternalModel;
import com.pb.tlumip.model.WorldZoneExternalZoneUtil;

import java.util.ArrayList;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Oct 11, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class TLUMIPLDInternalExternalModel extends LDInternalExternalModel {

    public TLUMIPLDInternalExternalModel(){
        super();
    }



    public void readExternalStations(){
        externalStations = new ArrayList<Integer>();

        WorldZoneExternalZoneUtil wzUtil = new WorldZoneExternalZoneUtil(globalRb);
        int[] externalZones = wzUtil.getExternalZones();
        for(int zone : externalZones){
            externalStations.add(zone);    
        }
    }



}

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
package com.pb.tlumip.ao;

import com.pb.tlumip.model.ModelProperties;
import java.util.ArrayList;

/**
 * 
 * 
 * @author  Christi Willison
 * @version Jan 7, 2004
 * Created by IntelliJ IDEA.
 */
public class AOProperties extends ModelProperties {
    //inherits a propertyMap from ModelProperties
    //but it needs a few other fields in addition.
    private ArrayList resourceList; //holds resource objects
    private Scenario scenario;   //contains the scenario description
    private ArrayList intervalUpdateList; //holds intervalUpdate objects

    public AOProperties(){
        resourceList = new ArrayList();
        scenario = new Scenario();
        intervalUpdateList = new ArrayList();
    }

    public void addResource(Resource resource){
        resourceList.add(resource);
    }

    public void addScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public void addIntervalUpdate(IntervalUpdate intervalUpdate){
        intervalUpdateList.add(intervalUpdate);
    }

    public ArrayList getResourceList() {
        return resourceList;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public ArrayList getIntervalUpdateList() {
        return intervalUpdateList;
    }

    public static void main (String[] args){}


}

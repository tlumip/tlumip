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
package com.pb.tlumip.seam;

import com.pb.models.reference.ModelComponent;
import com.pb.models.seam.SimpleEconomicAllocationModel;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.io.File;

/**
 * SEAMModel is a class that ...
 *
 * @author Kimberly Grommes
 * @version 1.0, Apr 30, 2007
 * Created by IntelliJ IDEA.
 */
public class SEAMModel extends ModelComponent {

    private static Logger logger = Logger.getLogger(SEAMModel.class);

    public void startModel(int BaseYear, int interval) {

        SimpleEconomicAllocationModel popSeam = new PopulationSEAM();
        popSeam.run();
        
    }

    public void Main(String[] args) {

        ModelComponent modelComponent = new SEAMModel();
        ResourceBundle globalRB = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_seam/t0/global.properties"));
        ResourceBundle appRB = ResourceUtil.getPropertyBundle(new File("/models/tlumip/scenario_seam/t1/seam/seam.properties"));

        modelComponent.setResourceBundles(appRB, globalRB);
        modelComponent.startModel(2000, 1);

    }
}

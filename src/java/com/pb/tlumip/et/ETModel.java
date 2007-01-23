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
package com.pb.tlumip.et;

import com.pb.tlumip.model.ModelComponent;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * ETModel is a class that ...
 * This is currently just a place holder for the real class.
 *
 * @author Kimberly Grommes
 * @version 1.0, Jan 23, 2007
 * Created by IntelliJ IDEA.
 */
public class ETModel extends ModelComponent {

    Logger logger = Logger.getLogger("ETModel.class");


    public ETModel(ResourceBundle appRb, ResourceBundle globalRb){
		setResourceBundles(appRb, globalRb);    //creates a resource bundle as a class attribute called appRb.

    }

	public void startModel(int t){
        logger.info("Starting ET Model.");
        logger.info("Finishing ET Model.");
    }
}

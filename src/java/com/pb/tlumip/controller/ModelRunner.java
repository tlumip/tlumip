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
package com.pb.tlumip.controller;

/**
 * This class runs the Despair model.
 *
 * @author    Tim Heier
 * @version   1.0, 7/17/2000
 *
 */

public class ModelRunner {

    public static void main (String[] args) {
        ModelRunner runner = new ModelRunner();
    }


    public ModelRunner() {
        loadProps();
        runModel();
    }


    public void loadProps() {
        System.out.println("Loading application properties");
    }


    public void runModel() {
        System.out.println("Running Despair Model");
    }
}

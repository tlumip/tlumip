package com.pb.tlumip.controller;

/**
 * This class runs the Despair model.
 *
 * @author    Tim Heier
 * @version   1.0, 7/17/2000
 *
 */

import com.pb.common.util.Debug;

public class ModelRunner {

    public static void main (String[] args) {
        ModelRunner runner = new ModelRunner();
    }


    public ModelRunner() {
        loadProps();
        runModel();
    }


    public void loadProps() {
        Debug.println(this, "Loading application properties");
    }


    public void runModel() {
        Debug.println(this, "Running Despair Model");
    }
}

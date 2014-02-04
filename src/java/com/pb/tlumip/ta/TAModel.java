package com.pb.tlumip.ta;

import com.pb.models.utils.StatusLogger;
import com.pb.tlumip.ao.ExternalProcess;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * The {@code TAModel} ...
 *
 * @author crf
 *         Started 4/24/13 8:07 AM
 */
public class TAModel {
    private static final Logger logger = Logger.getLogger(TAModel.class);


    public static enum TAMode {
        SWIM_INPUTS("inputs","SI"),
        HIGHWAY_ASSIGNMENT("highway","TA"),
        TRANSIT_ASSIGNMENT("transit","TR");

        private final String modeIdentifier;
        private final String shortModeName;

        private TAMode(String modeIdentifier, String shortModeName) {
            this.modeIdentifier = modeIdentifier;
            this.shortModeName = shortModeName;
        }
    }

    private final ResourceBundle properties;

    public TAModel(ResourceBundle properties) {
        this.properties = properties;
    }

    public void runModel(TAMode mode, int year) {
        String modeName = mode.shortModeName;
        logger.info("Starting " + modeName + " Model.");
        StatusLogger.logText(modeName.toLowerCase(),modeName + " started for year t" + year);

        if (mode == TAMode.HIGHWAY_ASSIGNMENT) //build demand matrices if running assignment
            new DemandBuilder(properties).writeDemandMatrices();
        String processProgram = properties.getString("python.visum.executable");
        List<String> args = new LinkedList<String>();
        args.add(properties.getString("ta.python.file"));
        args.add(properties.getString(modeName.toLowerCase() + ".property.file"));
        args.add(mode.modeIdentifier);
        new ExternalProcess("run " + mode,processProgram,args).startProcess();

        logger.info("Finishing " + modeName + " Model.");
        StatusLogger.logText(modeName.toLowerCase(),modeName + " Done");
    }

//    public void runAssignment(int year) {
//        logger.info("Starting TA Model.");
//        StatusLogger.logText("ta","TA started for year t" + year);
//
//        String processProgram = properties.getString("python.visum.executable");
//        List<String> args = new LinkedList<String>();
//        args.add(properties.getString("ta.python.file"));
//        args.add(properties.getString("ta.property.file"));
//        new ExternalProcess("run traffic assignment",processProgram,args).startProcess();
//
//        logger.info("Finishing TA Model.");
//        StatusLogger.logText("ta","TA Done");
//    }
}

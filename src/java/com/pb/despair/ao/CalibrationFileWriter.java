package com.pb.despair.ao;

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.CSVFileWriter;

import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;

/**
 * Author: willison
 * Date: Dec 14, 2004
 * This class will be called from AO when
 * calibration files need to be written.
 *
 * The actual methods to create the
 * module specific output
 * files will be in the respective
 * module code as a static class.
 *
 *
 * Created by IntelliJ IDEA.
 */
public class CalibrationFileWriter {

    protected static Logger logger = Logger.getLogger("com.pb.despair.ao");
    ResourceBundle rb;
    String calibrationOutputPath;
    String scenarioName;
    int timeInterval;

    public CalibrationFileWriter(ResourceBundle rb, String scenarioName, int timeInterval){
        this.rb = rb;
        this.calibrationOutputPath = ResourceUtil.getProperty(this.rb, "calibration.output.path");
        this.scenarioName = scenarioName;
        this.timeInterval = timeInterval;
    }

    public void writeCalibrationFiles(TableDataSet[] dataSets, String appName){
        //first create output path by concatinating the output path + scenarioName + timeInterval + application
        String outputPath = calibrationOutputPath + "scenario_"+ scenarioName + "/calibration/outputs/t" +
                timeInterval + "/" + appName + "/";

        for(int i=0; i< dataSets.length; i++){
            String fileName = ResourceUtil.getProperty(rb, (dataSets[i].getName()+ ".file"));
            String fullPath = outputPath+fileName;
            logger.fine("Full path to output file is: " + fullPath);
            writeCalibrationOutputFile(fullPath, dataSets[i]);
        }

    }

    private void writeCalibrationOutputFile(String outputFilePath, TableDataSet tableData){
        CSVFileWriter writer = new CSVFileWriter();
        try {
            writer.writeFile(tableData, new File(outputFilePath));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}

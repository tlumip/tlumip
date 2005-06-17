package com.pb.despair.calibrator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;



import org.apache.log4j.Logger;

import com.hbaspecto.calibrator.BatchFileRunner;
import com.hbaspecto.calibrator.CalibrateException;
import com.hbaspecto.calibrator.CalibrationStrategy;
import com.hbaspecto.calibrator.ModelInputsAndOutputs;
import com.hbaspecto.calibrator.ModelRunException;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.GeneralDecimalFormat;
import com.pb.common.datafile.TableDataSetCollection;
import com.pb.common.util.ResourceUtil;

/**
 * This class allows a user to start and stop
 * the PI Calibrator from the command line, eliminating
 * the need to use the swing application.  The user will
 * specify the strategy and the scenario location thru the 
 * "calibration.properties" file that must be in the classpath.
 * Output from the calibration process will be directed to 
 * the log4j logger which can be then be directed to an appender
 * thru the use of the log4j.xml configuration file. 
 * 
 * Created on Jun 15, 2005 
 * @author Christi
 */
public class CalibrationRunner {
    static Logger logger = Logger.getLogger(CalibrationRunner.class);

    public static void main(String[] args) {
        logger.info("Starting the Calibrator.");
        System.out.println("To STOP calibration you must create" +
        		" a file called STOP in the same directory as the PI/AA inputs and outputs");
        logger.info("All STOP files that currently exist will be deleted before calibration starts");
        ResourceBundle rb = ResourceUtil.getResourceBundle("calibration");
//        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("/tlumip_data/calibration.properties"));
        String stratPath = ResourceUtil.getProperty(rb, "strategiesPath"); //path to all .strat files
        //A list of all files will be output to the console and the user will select the 
        //strategy file that they want.
        logger.info("According to calibration.properties, the strategies are in " + stratPath);
        logger.info("Here is the list of strategies to choose from");
        String[] list = (new File(stratPath)).list();
        for(int i=0; i< list.length; i++){
            logger.info("    " + list[i]);
        }
        //Try to get the user to select a strategy from the list produced above.
        boolean loaded = false;
        String strategyName = null;
        CalibrationStrategy strat = null;
        BufferedReader stdin = new BufferedReader( new InputStreamReader(System.in));
        while (!loaded){
            while(strategyName == null){ //keep trying to get the user to input a strategy name
                try {
                    System.out.println("Type the name of the strategy you want to use.");
                    strategyName = stdin.readLine();
                    logger.info("You chose strategy: " + strategyName);
                } catch (IOException e) {
                    logger.info(e);
                    logger.info("Type in a strategy name from the list above");
                }
            }
            try {
                FileInputStream fis = new FileInputStream(stratPath + strategyName);
                ObjectInputStream in = new ObjectInputStream(fis);
                strat = (CalibrationStrategy) in.readObject();
                in.close();
                loaded=true;
                logger.info("Strategy Object Loaded");
            } catch (Exception e) {
                strategyName = null;
                logger.info("Error reading in Strategy Object - Check spelling");
                logger.info(e);
            }    
        }
       
       //Log the parameters and targets that are in the strategy that was just loaded
//       strat.log();
       
       //Read in the absolute path to piModel.sh and create a BatchFileRunner
       //that will actually start the PI/AA Model
       File piCmdFile = new File(ResourceUtil.getProperty(rb,"runnerFile"));
       File piInputsDir = piCmdFile.getParentFile(); //this will be the path to the input and output files
       logger.info("According to calibration.properties, you will be using " + piCmdFile + " to run PI/AA");
       if(!piCmdFile.exists()) logger.warn("However this file does not exist - check path in properties file");
       logger.info("The PI inputs and outputs will be going to the following directory: " + piInputsDir);
       if(!piInputsDir.exists() || !piInputsDir.canWrite()) logger.warn("However this directory either doesn't exist or cannot be written to");
       logger.info("Remember that to stop calibrating you must create a STOP file in this directory");
       File stopFile = new File(piInputsDir.getAbsolutePath() + "/STOP");
       logger.info("Checking for existence of " + stopFile);
       if(stopFile.exists()){
           logger.info("Found a STOP file - file will be deleted");
           stopFile.delete();
       }
       //Create readers and writers for the BatchFileRunner to use
       CSVFileReader r = new CSVFileReader();
       CSVFileWriter w = new CSVFileWriter();
       r.setMyDirectory(piInputsDir);
       w.setMyDirectory(piInputsDir);
       w.setMyDecimalFormat(new GeneralDecimalFormat("0.############E0",10000,.001));
       TableDataSetCollection myData = new TableDataSetCollection(r,w);

       ModelInputsAndOutputs y = new PecasDirectoryInputsAndOutputs(piInputsDir.getAbsolutePath(),myData);
       strat.setScenario(y);
       BatchFileRunner runner = new BatchFileRunner(piCmdFile, y);
       strat.setModelRunner(runner);
       
       //Start the calibration process
       try {
         strat.calibrate(stopFile);
       } catch (ModelRunException e) {
           e.printStackTrace();
       } catch (CalibrateException e) {
           e.printStackTrace();
       }
       
       //Save the strategy after the calibration run has finished.
       try {
           java.io.FileOutputStream fos = new java.io.FileOutputStream(stratPath + strategyName);
           java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(fos);
           out.writeObject(strat);
           out.flush();
           out.close();
           logger.info(strategyName + " has been saved to " + stratPath);
       } catch (java.io.IOException e) {
           logger.fatal(e);
           logger.fatal("Error Saving Strategy");
       }
       
    }
}

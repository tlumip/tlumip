package com.pb.despair.pi.daf;

import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.daf.Message;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.pi.OregonPIPProcessor;
import com.pb.despair.pi.PIPProcessor;
import com.pb.despair.pi.PIModel;
import com.pb.despair.pi.Commodity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.ResourceBundle;

/**
 * This class will calculate the composite buy and sell utilities
 * of a commodity in each zone when a message bearing a commodity
 * name is received.  It will return the buy and sell CUs in an array.
 * 
 * @author Christi Willison
 * @version Apr 27, 2004
 */
public class SetupWorkTask extends MessageProcessingTask {

    private int t;
    private ResourceBundle rb;
    private ResourceBundle pidafRb;
    String scenarioName = "pleaseWork";

    public void onStart() {
        logger.info("*******************************************************************************************");
            logger.info( "***" + getName() + " is starting...");
            
            pidafRb = ResourceUtil.getResourceBundle("pidaf_"+scenarioName);
            
            //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.txt file
        //that was written by the Application Orchestrator
        BufferedReader reader = null;
        String scenarioName = null;
        int timeInterval = -1;
        String pathToRb = null;
        try {
            logger.info("Reading RunParams.txt file");
            reader = new BufferedReader(new FileReader(new File((String)ResourceUtil.getProperty(pidafRb,"run.param.file"))));
            scenarioName = reader.readLine();
            logger.info("\tScenario Name: " + scenarioName);
            timeInterval = Integer.parseInt(reader.readLine());
            logger.info("\tTime Interval: " + timeInterval);
            pathToRb = reader.readLine();
            logger.info("\tResourceBundle Path: " + pathToRb);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File(pathToRb));
        this.rb = rb;
        this.t = timeInterval;
    }

    public void onMessage(Message msg) {
        logger.info( getName() + " received " + msg.getId() + " from" + msg.getSender() );
        logger.info("Reading data and setting up for PI run");
        long startTime = System.currentTimeMillis();
        //TODO get the PProcessor class name from the properties file and instantiate using Class.newInstance()
        PIPProcessor piReaderWriter = new OregonPIPProcessor(t,rb);
        piReaderWriter.setUpPi();
        logger.info("Setup is complete. Time in seconds: "+((System.currentTimeMillis()-startTime)/1000));
        Message doneMsg = mFactory.createMessage();
        sendTo("SetupResultsQueue",doneMsg);
    }


}
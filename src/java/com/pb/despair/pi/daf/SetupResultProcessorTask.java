package com.pb.despair.pi.daf;

import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.daf.Message;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.model.AbstractTAZ;
import com.pb.despair.pi.Commodity;
import com.pb.despair.pi.BuyingZUtility;
import com.pb.despair.pi.SellingZUtility;

import java.util.Iterator;
import java.util.ResourceBundle;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

/**
 * This class will extract the composite buy and sell utilities
 * of a commodity in each zone from a message and set those
 * values into the appropriate CommodityZUtility object that
 * exists in memory.
 *
 * @author Christi Willison
 * @version Apr 28, 2004
 */
public class SetupResultProcessorTask extends MessageProcessingTask {

    static int nodeCounter = 0;
    private int nWorkQueues = 0;

    public void onStart(){
        logger.info( "***" + getName() + " started");
        //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.txt file
        //that was written by the Application Orchestrator
        BufferedReader reader = null;
        String scenarioName = null;
        try {
            logger.info("Reading RunParams.txt file");
            reader = new BufferedReader(new FileReader(new File("/models/tlumip/daf/RunParams.txt")));
            scenarioName = reader.readLine();
            logger.info("\tScenario Name: " + scenarioName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ResourceBundle pidafRb = ResourceUtil.getResourceBundle("pidaf_"+scenarioName);
        nWorkQueues = (Integer.parseInt(ResourceUtil.getProperty(pidafRb,"nNodes"))-1);
    }

    public void onMessage(Message msg){
        logger.info( getName() + " received a message from " + msg.getSender() );
        nodeCounter++;
        if(nodeCounter == nWorkQueues){
            logger.info("Signaling that all nodes have been set up.");
            PIServerTask.signalResultsProcessed();
        }
     }
}

package com.pb.despair.pi.daf;

import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.daf.Message;
import com.pb.common.util.ResourceUtil;

import java.util.ResourceBundle;
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
    private ResourceBundle pidafRb;
    String scenarioName;

    public void onStart(){
        logger.info( "***" + getName() + " started");
        
        logger.info("Reading RunParams.properties file");
        ResourceBundle runParamsRb = ResourceUtil.getPropertyBundle(new File("/models/tlumip/daf/RunParams.properties"));
        scenarioName = ResourceUtil.getProperty(runParamsRb,"scenarioName");
        logger.info("\tScenario Name: " + scenarioName);

        pidafRb = ResourceUtil.getResourceBundle("pidaf_"+scenarioName);
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

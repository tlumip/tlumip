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
public class CUWorkTask extends MessageProcessingTask {
    private PIModel pi;
    boolean debug = false;

    public void onStart() {

        logger.info("*******************************************************************************************");
        logger.info( "***" + getName() + " is starting...");
        //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.txt file
        //that was written by the Application Orchestrator
        BufferedReader reader = null;
        int timeInterval = -1;
        String pathToRb = null;
        String scenarioName = null;
        try {
            logger.info("Reading RunParams.txt file");
            reader = new BufferedReader(new FileReader(new File("/models/tlumip/daf/RunParams.txt")));
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
        logger.info("  *" + getName() + " is creating a PIModel Object");
        pi = new PIModel(rb);
        logger.info("*******************************************************************************************");
    }

    public void onMessage(Message msg) {
        if(debug) logger.info( getName() + " received " + msg.getStringValue("Name") + " from" + msg.getSender() );
        String name = msg.getStringValue("Name");
        double[] prices = (double[])msg.getValue("Price");

        Commodity c = Commodity.retrieveCommodity(name);
        c.setPriceInAllExchanges(prices);
        double[][] compUtils = pi.calculateCompositeBuyAndSellUtilities(name);
        //compUtils[0][] = buy, compUtils[1][] = sell

        Message resultMsg = mFactory.createMessage();
        resultMsg.setId("Return CU values for "+name);
        resultMsg.setValue("Name",name);
        resultMsg.setValue("CompUtils",compUtils);
        sendTo("CUResultsQueue",resultMsg);
    }


}

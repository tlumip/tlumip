package com.pb.despair.pi.daf;

import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.daf.Message;
import com.pb.despair.pi.PIModel;
import com.pb.despair.pi.Commodity;


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
    private boolean firstMessage = true;

    public void onStart() {

        logger.info("*************************" + getName() + " has started ************");
    }

    public void onMessage(Message msg) {
        if(logger.isDebugEnabled()) {
            logger.debug( getName() + " received " + msg.getStringValue("Name") + " from" + msg.getSender() );
        }
        if(firstMessage){
            pi = new PIModel(SetupWorkTask.piRb, SetupWorkTask.globalRb);
            firstMessage = false;
        }
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

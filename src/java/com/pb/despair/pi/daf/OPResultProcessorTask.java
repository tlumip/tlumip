package com.pb.despair.pi.daf;

import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.daf.Message;
import com.pb.despair.model.AbstractTAZ;
import com.pb.despair.pi.Commodity;
import com.pb.despair.pi.BuyingZUtility;
import com.pb.despair.pi.SellingZUtility;

import java.util.Iterator;

/**
 * This class will receive a message from the CommodityOutputTask
 * that a commodity's buying and selling flows have been written out.  When
 * all commodities have been processed it will signal to the PIServerTask
 * that we are finished.
 *
 * @author Christi Willison
 * @version Apr 28, 2004
 */
public class OPResultProcessorTask extends MessageProcessingTask {

    static int commodityCounter = 0;
    static long processTime;
    boolean debug = false;

    public void onStart(){
        logger.info( "***" + getName() + " started");
    }

    public void onMessage(Message msg){
        if(commodityCounter == 0) processTime = System.currentTimeMillis();
        commodityCounter++;
        String name = msg.getStringValue("Name");
        if(debug) logger.info("Buying and Selling Flows have been written for " + name);
        //check to see if we have received all commmodity values.  If so, send the signal
        //queue a message that we are complete.
        if(commodityCounter >= Commodity.getAllCommodities().size()){
            commodityCounter=0;
            logger.info("Signaling that buying and selling flows have all been written.  Time in secs: "+ (System.currentTimeMillis()-processTime)/1000.0);
            PIServerTask.signalResultsProcessed();
        }
    }
}

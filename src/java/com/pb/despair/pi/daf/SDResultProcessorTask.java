package com.pb.despair.pi.daf;

import java.util.Iterator;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.despair.pi.Commodity;
import com.pb.despair.pi.Exchange;

/**
 * This class will extract the surplus and derivative of a
 * commodity in each exchange zone.  The surplus and derivative
 * will be set in the appropriate exchange zone.
 * 
 * @author Christi Willison
 * @version Apr 28, 2004
 */
public class SDResultProcessorTask extends MessageProcessingTask {
    
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
        double[][] surplusAndDeriv = (double[][])msg.getValue("SandD");

        Commodity c = Commodity.retrieveCommodity(name);
        Iterator exchanges = c.getAllExchanges().iterator();
        while(exchanges.hasNext()){
            Exchange ex = (Exchange)exchanges.next();
            int exIndex = ex.getExchangeLocationIndex();
            ex.setLastCalculatedSurplus(surplusAndDeriv[0][exIndex]);
            ex.setLastCalculatedDerivative(surplusAndDeriv[1][exIndex]);
            ex.setSurplusAndDerivativeValid(true);
        }
        if(debug) logger.info("Surplus and Derivative of "+name+" have been put into memory");
        //check to see if we have received all commmodity values.  If so, send the signal
        //queue a message that we are complete.
        if(commodityCounter >= Commodity.getAllCommodities().size()) {
            commodityCounter=0;
            logger.info("Signaling that S&D results have all been processed.  Time is secs: "+ (System.currentTimeMillis()-processTime)/1000.0);
            PIServerTask.signalResultsProcessed();
        }
    }
}

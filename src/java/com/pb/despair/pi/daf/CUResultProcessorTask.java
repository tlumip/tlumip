package com.pb.despair.pi.daf;

import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.daf.Message;
import com.pb.despair.pi.Commodity;
import com.pb.despair.pi.BuyingZUtility;
import com.pb.despair.pi.SellingZUtility;

import java.util.Iterator;

/**
 * This class will extract the composite buy and sell utilities
 * of a commodity in each zone from a message and set those
 * values into the appropriate CommodityZUtility object that
 * exists in memory.
 *
 * @author Christi Willison
 * @version Apr 28, 2004
 */
public class CUResultProcessorTask extends MessageProcessingTask {

    static int commodityCounter = 0;
    static long processTime;

    public void onStart(){
        logger.info( "***" + getName() + " started");
    }

    public void onMessage(Message msg){
        if(commodityCounter == 0) processTime = System.currentTimeMillis();
        commodityCounter++;
        String name = msg.getStringValue("Name");
        double[][] compUtils = (double[][])msg.getValue("CompUtils");

        Commodity c = Commodity.retrieveCommodity(name);
        
        //  just for testing May 27 2004-- remove these 4 lines
      //  if (name.equalsIgnoreCase("CONSTRUCTION")) {
        //    logger.info("CONSTRUCTION in zone "+AbstractTAZ.getZone(1).getZoneUserNumber()+" buying utility calculated is "+compUtils[0][1]);
        //    logger.info("CONSTRUCTION in zone "+AbstractTAZ.getZone(1).getZoneUserNumber()+" selling utility calculated is "+compUtils[1][1]);
       // }

        //we need to first set the buying comp util into the local memory
        Iterator buyZUtils = c.getBuyingTazZUtilities().values().iterator();
            while(buyZUtils.hasNext()){
                BuyingZUtility bzu = (BuyingZUtility)buyZUtils.next();
                int index = bzu.getTaz().getZoneIndex();
                bzu.setPricesFixed(true);
                bzu.setLastCalculatedUtility(compUtils[0][index]);
                bzu.setLastUtilityValid(true);
               }

        //and then the selling comp utilities into the local memory
        Iterator sellZUtils = c.getSellingTazZUtilities().values().iterator();
            while(sellZUtils.hasNext()){
                SellingZUtility szu =(SellingZUtility)sellZUtils.next();
                int index = szu.getTaz().getZoneIndex();
                szu.setPricesFixed(true);
                szu.setLastCalculatedUtility(compUtils[1][index]);
                szu.setLastUtilityValid(true);
               }
        if(logger.isDebugEnabled()) {
            logger.debug("Composite Utility of "+name+" has been put into memory");
        }
        //check to see if we have received all commmodity values.  If so, send the signal
        //queue a message that we are complete.
        if(commodityCounter >= Commodity.getAllCommodities().size()){
            commodityCounter=0;
            logger.info("Signaling that CU results have all been processed.  Time in secs: "+ (System.currentTimeMillis()-processTime)/1000.0);
            PIServerTask.signalResultsProcessed();
        }
    }
}

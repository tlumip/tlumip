package com.pb.despair.pi.daf;

import java.util.ResourceBundle;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.pi.PIModel;
import com.pb.despair.pi.PIPProcessor;

/**
 * This class will extract the surplus and derivative of a
 * commodity in each exchange zone.  The surplus and derivative
 * will be set in the appropriate exchange zone.
 * 
 * @author Christi Willison
 * @version Apr 28, 2004
 */
public class CommodityOutputTask extends MessageProcessingTask {

    private PIModel pi;
    private PIPProcessor pwriter;
    private ResourceBundle pidafRb;
    private boolean firstMessage = true;

    public void onStart(){

        logger.info("***************************" + getName() + " begin onStart() *****************************************");

        logger.info("***************************" + getName() + " end onStart()****************************************");
    }
    
    public void onMessage(Message msg){
        logger.info( getName() + " received " + msg.getStringValue("Name") + " from" + msg.getSender() );
        if(firstMessage){
            pi = new PIModel(SetupWorkTask.piRb, SetupWorkTask.globalRb);
//            pwriter = new OregonPIPProcessor(SetupWorkTask.timeInterval,SetupWorkTask.piRb,SetupWorkTask.globalRb);
            String pProcessorClass = ResourceUtil.getProperty(SetupWorkTask.piRb,"pprocessor.class");
            logger.info("ComodityOutputTask will be using the " + pProcessorClass + " for pre and post PI processing");
            Class ppClass = null;
            pwriter = null;
            try {
                ppClass = Class.forName(pProcessorClass);
                pwriter = (PIPProcessor) ppClass.newInstance();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                logger.fatal("Can't create new instance of PiPProcessor of type "+ppClass.getName());
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                logger.fatal("Can't create new instance of PiPProcessor of type "+ppClass.getName());
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            pwriter.setResourceBundles(SetupWorkTask.piRb, SetupWorkTask.globalRb);
            pwriter.setTimePeriod(SetupWorkTask.timeInterval);
            firstMessage = false;
        }
        String name = msg.getStringValue("Name");

        double[] tc = ((double[]) msg.getValue("TC")); //total consumption of commodity in zone

        double[] tp = ((double[]) msg.getValue("TP")); //total production of commodity in zone

        double[] dtc = ((double[]) msg.getValue("dTC")); //total derivative of consumption of commodity in zone

        double[] dtp = ((double[]) msg.getValue("dTP")); //total derivative of production of commodity in zone

        double[] price = ((double[]) msg.getValue("Price")); //price of commodity in zone

        pi.allocateQuantitiesToFlowsAndExchanges(name, tc, tp, dtc, dtp, price);

        pwriter.writeExchangeResults(name);
        pwriter.writeFlowZipMatrices(name,null);

        Message resultMsg = mFactory.createMessage();
        resultMsg.setId("FileWritten");
        resultMsg.setValue("Name",name);
        sendTo("OPResultsQueue",resultMsg);

    }
}

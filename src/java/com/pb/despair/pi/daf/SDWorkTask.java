package com.pb.despair.pi.daf;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.despair.pi.PIModel;

/**
 * @author Christi Willison
 * @version Apr 27, 2004
 */
public class SDWorkTask extends MessageProcessingTask {
    private PIModel pi;
    String scenarioName = "pleaseWork";
    private boolean firstMessage = true;

    public void onStart() {

        logger.info("******************" + getName() + " started ************************");
    }

    public void onMessage(Message msg){
        if(logger.isDebugEnabled()) {
            logger.debug( getName() + " received " + msg.getStringValue("Name") + " from" + msg.getSender() );
        }
        if(firstMessage){
            pi = new PIModel(SetupWorkTask.piRb, SetupWorkTask.globalRb);
            firstMessage = false;
        }

        String name = msg.getStringValue("Name");

        double[] tc = ((double[]) msg.getValue("TC")); //total consumption of commodity in zone

        double[] tp = ((double[]) msg.getValue("TP")); //total production of commodity in zone

        double[] dtc = ((double[]) msg.getValue("dTC")); //total derivative of consumption of commodity in zone

        double[] dtp = ((double[]) msg.getValue("dTP")); //total derivative of production of commodity in zone

        double[] price = ((double[]) msg.getValue("Price")); //price of commodity in zone

//        double[][] surplusAndDerivative = pi.calculateSurplusAndDerivatives(name,tc,tp,dtc,dtp,price);
        pi.allocateQuantitiesToFlowsAndExchanges(name, tc, tp, dtc, dtp, price);

        double[][] surplusAndDerivative = pi.calculateSurplusAndDerivatives(name);
//        surplusAndDerivative[0][]=surplus in each exchange, surplusAndDeriv[1][]=derivative in each exchange

        //create message to send to results queue
        Message resultMsg = mFactory.createMessage();
        resultMsg.setId("Return S and D values for "+name);
        resultMsg.setValue("Name",name);
        resultMsg.setValue("SandD",surplusAndDerivative);

        sendTo("SDResultsQueue",resultMsg);
    }
}

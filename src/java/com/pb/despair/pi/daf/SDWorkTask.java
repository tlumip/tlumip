package com.pb.despair.pi.daf;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.pi.PIModel;
import com.pb.despair.pi.Commodity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.ResourceBundle;

/**
 * @author Christi Willison
 * @version Apr 27, 2004
 */
public class SDWorkTask extends MessageProcessingTask {
    private PIModel pi;
    boolean debug = false;
    private ResourceBundle pidafRb;
    String scenarioName = "pleaseWork";

    public void onStart() {

        logger.info("*******************************************************************************************");
        logger.info( "***" + getName() + " is starting...");

        pidafRb = ResourceUtil.getResourceBundle("pidaf_"+scenarioName);
        //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.txt file
        //that was written by the Application Orchestrator
        BufferedReader reader = null;
        int timeInterval = -1;
        String pathToRb = null;
        String scenarioName = null;
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
        logger.info("  *" + getName() + " is creating a PIModel Object");
        pi = new PIModel(rb);
        logger.info("*******************************************************************************************");
    }

    public void onMessage(Message msg){

        String name = msg.getStringValue("Name");
        if(debug) logger.fine( getName() + " received " + msg.getStringValue("Name") + " from" + msg.getSender() );

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

package com.pb.despair.pi.daf;

import java.util.Iterator;
import java.util.ResourceBundle;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.util.ResourceUtil;
import com.pb.despair.pi.Commodity;
import com.pb.despair.pi.Exchange;
import com.pb.despair.pi.OregonPIPProcessor;
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
    String scenarioName = "pleaseWork";

    public void onStart(){

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
        logger.info("  *" + getName() + " is creating a PIPProcessor Object");
        pwriter = new OregonPIPProcessor(timeInterval, rb);
        logger.info("*******************************************************************************************");
    }
    
    public void onMessage(Message msg){
        String name = msg.getStringValue("Name");
        logger.info( getName() + " received " + msg.getStringValue("Name") + " from" + msg.getSender() );

        double[] tc = ((double[]) msg.getValue("TC")); //total consumption of commodity in zone

        double[] tp = ((double[]) msg.getValue("TP")); //total production of commodity in zone

        double[] dtc = ((double[]) msg.getValue("dTC")); //total derivative of consumption of commodity in zone

        double[] dtp = ((double[]) msg.getValue("dTP")); //total derivative of production of commodity in zone

        double[] price = ((double[]) msg.getValue("Price")); //price of commodity in zone

        pi.allocateQuantitiesToFlowsAndExchanges(name, tc, tp, dtc, dtp, price);

        //TODO get the PProcessor class name from the properties file and instantiate using Class.newInstance()
        pwriter.writeExchangeResults(name);
        pwriter.writeFlowZipMatrices(name);

        Message resultMsg = mFactory.createMessage();
        resultMsg.setId("FileWritten");
        resultMsg.setValue("Name",name);
        sendTo("OPResultsQueue",resultMsg);

    }
}

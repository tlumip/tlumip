package com.pb.despair.pi.daf;

import com.pb.common.daf.*;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.BooleanLock;
import com.pb.despair.pi.*;
import com.pb.despair.model.OverflowException;

import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.io.*;

/**
 * @author Christi Willison
 * @version Mar 18, 2004
 */
public class PIServerTask extends Task{

    Logger logger = Logger.getLogger("com.pb.common.despair.pi.daf");
    boolean debug = true;
    private PIModel pi;
    private int maxIterations = 300; //default value in case none is set in properties file
    private int nIterations = 0; //a counter to keep track of how many iterations it takes before the meritMeasure is within tolerance.
    protected static BooleanLock signal = new BooleanLock(false);
    OregonPIPProcessor piReaderWriter;
    String scenarioName = "pleaseWork";
    ResourceBundle piRb = null;
    ResourceBundle pidafRb = null;
    
    public void onStart(){
        logger.info( "***" + getName() + " started");
        logger.info("Reading data and setting up for PI run");
        long startTime = System.currentTimeMillis();

        pidafRb = ResourceUtil.getResourceBundle("pidaf_"+scenarioName);
        
        //We need to read in the Run Parameters (timeInterval and pathToResourceBundle) from the RunParams.txt file
        //that was written by the Application Orchestrator
        BufferedReader reader = null;
        int timeInterval = -1;
        String pathToRb = null;
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
        piRb = ResourceUtil.getPropertyBundle(new File(pathToRb));

        //TODO get the PProcessor class name from the properties file and instantiate using Class.newInstance()
        piReaderWriter = new OregonPIPProcessor(timeInterval, piRb);
        piReaderWriter.setUpPi();
        logger.info("Setup is complete. Time in seconds: "+((System.currentTimeMillis()-startTime)/1000));

        logger.info("*******************************************************************************************");
        logger.info("*   Beginning PI");
        pi = new PIModel(piRb);

        String maxIterationsString = ResourceUtil.getProperty(piRb, "pi.maxIterations");
        if (maxIterationsString == null) {
            logger.info("*   No 'pi.maxIterations' set in properties file -- using default");
        } else {
            int mi = Integer.valueOf(maxIterationsString).intValue();
            maxIterations = mi;
            logger.info("*   Maximum iteration set to " + mi);
        }
        if(maxIterations == 0) logger.warning("Max Iterations was set to 0 in properties file");
        logger.info("*******************************************************************************************");
    }

    public void doWork(){
        long startTime = System.currentTimeMillis();
        //Read in from the properties file the number of nodes
        //so that we know how many worker ports we need
        int nNodes = Integer.parseInt(ResourceUtil.getProperty(pidafRb,"nNodes"));
        int nWorkQueues = nNodes-1;

        PortManager pManager = PortManager.getInstance();
        MessageFactory mFactory = MessageFactory.getInstance();
        Message[] msgs;

        //Get ports to the worker queues to communicate with a queue
        Port[] SetupWorkPorts = new Port[nNodes-1];
        for(int i=0;i<nWorkQueues;i++){
            SetupWorkPorts[i] = pManager.createPort("SetupWorkQueue_"+ (i+1));
        }

        Port[] CUWorkPorts = new Port[nWorkQueues]; //the Server task will always be on node 0
        for(int i=0;i<nWorkQueues;i++){             //work queues will always start on node 1 (and numbered 1...n)
            CUWorkPorts[i] = pManager.createPort("CUWorkQueue_"+ (i+1)); //
        }

        Port[] SDWorkPorts = new Port[nNodes-1];
        for(int i=0;i<nWorkQueues;i++){
            SDWorkPorts[i] = pManager.createPort("SDWorkQueue_"+ (i+1));
        }

        Port[] OutputWorkPorts = new Port[nNodes-1];
        for(int i=0;i<nWorkQueues;i++){
            OutputWorkPorts[i] = pManager.createPort("OPWorkQueue_"+ (i+1));
        }

        //Set up PI on all nodes
        msgs = createSetupMessages(mFactory,nWorkQueues);
        sendWorkToWorkQueues(msgs,SetupWorkPorts,nWorkQueues);

        //Wait for a signal from SetupResultsProcessorTask that all nodes
        //are set up.
        long waitTime = System.currentTimeMillis();
        try {
            signal.waitUntilStateChanges(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("GO ON SIGNAL RECEIVED: Waited "+  (System.currentTimeMillis()-waitTime)/1000.0 + " secs for " +
                        "nodes to set up");

        /**************************************************************************************
         * Begin PI Model
         *************************************************************************************/

        boolean convergenceCriteriaMet;
        if(maxIterations == 0) convergenceCriteriaMet=true;
        else convergenceCriteriaMet = false;
        boolean nanPresent=false;
        double oldMeritMeasure=Double.POSITIVE_INFINITY; //sum of squares of the surplus over all commodities, all zones
        double newMeritMeasure;

        //First send a commodity name to the CUWorkQueues so that CUWorkTask can calculate the composite utilities
        //for that commodity - the values will be returned to the CUResultsQueue on the local node
        //and handled by the CUResultsProcessorTask.  CUResultsProcessor will send a msg to the Signal
        //Queue when the work is complete.
        msgs = createCUWorkMessages(mFactory);
        sendWorkToWorkQueues(msgs,CUWorkPorts,nWorkQueues);

        //Wait for a signal from CUResultsProcessorTask that all calcs
        //are complete and saved in the local node's memory (ie. node 0's memory)
        waitTime = System.currentTimeMillis();
        try {
            signal.waitUntilStateChanges(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("GO ON SIGNAL RECEIVED: Waited "+  (System.currentTimeMillis()-waitTime)/1000.0 + " secs for " +
                        "CU calculations");

        //Once the Composite Utility Calcs are complete and the results processed, the
        //total activity in each zone can be made locally
        nanPresent = nanPresent || pi.calculateTotalConsumptionAndProduction();

        //Now send a commodity name to the SDWorkQueues so that the SDWorkTask can calculate the surplus
        // and the derivatives of that commodity in each zone.  The values will be returned to the
        // SDResultsQueue on the local node and handled by the SDResultsProcessorTask.  SDResultsProcessor
        // will send a msg to the SignalQueue when the work is complete.
        msgs = createSDFlowWorkMessages(mFactory);
        sendWorkToWorkQueues(msgs,SDWorkPorts,nWorkQueues);

        //Again wait here until we get a signal from SDResultsProcessorTask that all calcs
        //are complete and saved in the local node's memory (ie. node 1's memory)
        waitTime = System.currentTimeMillis();
        try {
            signal.waitUntilStateChanges(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("GO ON SIGNAL RECEIVED: Waited "+ (System.currentTimeMillis()-waitTime)/1000.0 + " secs for workers " +
                        "to calculate Surplus and Derviative for all commodities");

        //All calculations are complete and the merit measure can be calculated locally.
        newMeritMeasure = Double.POSITIVE_INFINITY;
        if (!nanPresent) {
            try {
                newMeritMeasure = pi.calculateMeritMeasureWithoutLogging();
            } catch (OverflowException e) {
                nanPresent = true;
            }
        }

        if (nanPresent) {
            logger.severe("Initial prices cause overflow -- try again changing initial prices");
            throw new RuntimeException("Initial prices cause overflow -- try again changing initial prices");
        }

        /******************************************************************************************************
         *         Now we begin to iterate to find a solution.
        *******************************************************************************************************/
        while(!convergenceCriteriaMet){  //criteria includes meritMeasure and nIterations
            long iterationTime = System.currentTimeMillis();
            logger.info("*******************************************************************************************");
            logger.info("*   Starting iteration "+ (nIterations+1)+".  Merit measure is "+newMeritMeasure);
            logger.info("*******************************************************************************************");

            nanPresent = false;
            if(newMeritMeasure < pi.convergenceTolerance){
                convergenceCriteriaMet=true;
            }else if (newMeritMeasure/oldMeritMeasure < 1.0000000001 || (pi.getStepSize() <= pi.getMinimumStepSize() && newMeritMeasure != Double.POSITIVE_INFINITY)) {
                if (newMeritMeasure/oldMeritMeasure < 1.0000000001 ) {
                    // that worked -- we're getting somewhere
                    pi.increaseStepSize();
                    logger.info("!!  Improving -- increasing step size to "+pi.getStepSize());
                } else {
                    logger.info("!!  Not improving, but step size already at minimum "+pi.getStepSize());
                }
                pi.snapShotCurrentPrices();
                pi.calculateNewPrices();

                //Now send work to the CUWorkQueue (Composite Utilitiy calculations)
                msgs = createCUWorkMessages(mFactory);
                sendWorkToWorkQueues(msgs,CUWorkPorts,nWorkQueues);
                //Wait for a signal from CUResultProcessorTask that all calcs
                //are complete and saved in the local node's memory (ie. node 0's memory)
                waitTime = System.currentTimeMillis();
                try {
                    signal.waitUntilStateChanges(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("GO ON SIGNAL RECEIVED: Waited "+  (System.currentTimeMillis()-waitTime)/1000.0 + " secs for workers " +
                        "to calculate CUs for all commodities");

                //recalculate total activity in each zone
                nanPresent = nanPresent || pi.recalculateTotalConsumptionAndProduction();

                //send work to the SDWorkQueue (Surplus and Derivative calculations)
                msgs = createSDFlowWorkMessages(mFactory);
                sendWorkToWorkQueues(msgs,SDWorkPorts,nWorkQueues);
                //Wait for a signal from CUResultProcessorTask that all calcs
                //are complete and saved in the local node's memory (ie. node 0's memory)
                waitTime = System.currentTimeMillis();
                try {
                    signal.waitUntilStateChanges(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("GO ON SIGNAL RECEIVED: Waited "+ (System.currentTimeMillis()-waitTime)/1000.0 + " secs for workers " +
                        "to calculate Surplus and Derviative for all commodities");

                nIterations++;
                double tempMeritMeasure = newMeritMeasure;
                if (!nanPresent) {
                    try {
                        newMeritMeasure = pi.calculateMeritMeasureWithoutLogging(); //calculate surplus for each commodity as well as total surplus
                    } catch (OverflowException e) {
                        nanPresent = true;
                    }
                }
                if (!nanPresent) {
                    oldMeritMeasure = tempMeritMeasure;
                } else {
                    newMeritMeasure = Double.POSITIVE_INFINITY;
                }
            } else {
                if (nIterations == maxIterations-1) {
                    pi.backUpToLastValidPrices();
                    logger.warning("!!  Not Improving and at second last iteration -- backing up to last valid prices");
                } else if (newMeritMeasure == Double.POSITIVE_INFINITY && pi.getStepSize()<= pi.getMinimumStepSize()) {
                    pi.backUpToLastValidPrices();
                    nIterations = maxIterations-1;
                    logger.severe("!!  Can't get past infinity without going below minimum step size -- terminating at last valid prices");
                } else {
                    pi.decreaseStepSizeAndAdjustPrices();
                    logger.info("!!  Not Improving -- decreasing step size to "+pi.getStepSize());
                }
                //Now send work to the CUWorkQueue (Composite Utilitiy calculations)
                msgs = createCUWorkMessages(mFactory);
                sendWorkToWorkQueues(msgs,CUWorkPorts,nWorkQueues);
                //Wait for a signal from CUResultsProcessorTask that all calcs
                //are complete and saved in the local node's memory (ie. node 1's memory)
                waitTime = System.currentTimeMillis();
                try {
                    signal.waitUntilStateChanges(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("Waited "+  (System.currentTimeMillis()-waitTime)/1000.0 + " secs for workers " +
                        "to calculate CUs for all commodities");

                //recalculate total activity in each zone
                nanPresent = nanPresent || pi.recalculateTotalConsumptionAndProduction();

                //send work to the SDWorkQueues (Surplus and Derivative calculations)
                msgs = createSDFlowWorkMessages(mFactory);
                sendWorkToWorkQueues(msgs,SDWorkPorts,nWorkQueues);
                //Wait for a signal from CUResultsProcessorTask that all calcs
                //are complete and saved in the local node's memory (ie. node 1's memory)
                waitTime = System.currentTimeMillis();
                try {
                    signal.waitUntilStateChanges(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("Waited "+ (System.currentTimeMillis()-waitTime)/1000.0 + " secs for workers " +
                        "to calculate Surplus and Derviative for all commodities");

                nIterations++;
                try {
                    newMeritMeasure = pi.calculateMeritMeasureWithoutLogging(); //calculate surplus for each commodity as well as total surplus
                } catch (OverflowException e) {
                    nanPresent = true;
                }
                if (nanPresent) {
                    logger.warning("Overflow error, setting new merit measure to positive infinity");
                    newMeritMeasure = Double.POSITIVE_INFINITY;
                }
            }
            if(nIterations == maxIterations) {
                convergenceCriteriaMet=true;
                logger.severe("Terminating because maximum iterations reached -- did not converge to tolerance");
            }
            nanPresent = false;
            logger.info("*********************************************************************************************");
            logger.info("*   End of iteration "+ (nIterations)+".  Time in seconds: "+(System.currentTimeMillis()-iterationTime)/1000.0);
            logger.info("*********************************************************************************************");
        }
        String logStmt=null;
        if(nIterations == maxIterations) {
            logStmt = "*   PI has reached maxIterations. Time in seconds: ";
        }else{
            logStmt = "*   PI has reached equilibrium in "+nIterations+". Time in seconds: ";
        }

        logger.info("*********************************************************************************************");
        logger.info(logStmt+((System.currentTimeMillis()-startTime)/1000));
        logger.info("*   Final merit measure is "+ newMeritMeasure);
        logger.info("*********************************************************************************************");

        logger.info("Writing ActivityLocations.csv and ActivityLocations2.csv");
        piReaderWriter.writeLocationTables();// writes out ActivityLocations.csv and ActivityLocations2.csv
        logger.info("Writing ZonalMakeUse.csv");
        piReaderWriter.writeZonalMakeUseCoefficients(); //writes out ZonalMakeUse.csv
        logger.info("Writing CommodityZUtilities.csv");
        piReaderWriter.writeZUtilitiesTable(); //writes out CommodityZUtilities.csv
        logger.info("Writing ExchangeResults.csv");
        piReaderWriter.writeExchangeResults(); //write out ExchangeResults.csv (prices of all commodites at all exchanges)
        logger.info("Writing ActivitySummary.csv");
        piReaderWriter.writeActivitySummary();
        logger.info("Writing laborDollarProductionSum.csv, labordollarConsumptionSum.csv, etc");
        piReaderWriter.writeLaborConsumptionAndProductionFiles(); //writes out laborDollarProduction/Consumption and Sum files


        //Have the workers write the flows.
        msgs = createSDFlowWorkMessages(mFactory);
        sendWorkToWorkQueues(msgs, OutputWorkPorts, nWorkQueues);
        try {
            signal.waitUntilStateChanges(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Waited "+ (System.currentTimeMillis()-waitTime)/1000.0 + " secs for workers " +
                        "to output flows for all commodities");
        logger.info("Total Time in seconds: "+((System.currentTimeMillis()-startTime)/1000));
        File doneFile = new File(ResourceUtil.getProperty(piRb,"done.file"));
        if(!doneFile.exists()){
            try {
                doneFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.info("Done File has been written");
        return;
    }

    private Message[] createSetupMessages(MessageFactory mFactory, int nWorkQueues){
        Message[] messages =  new Message[nWorkQueues];
        for(int i=0;i<messages.length;i++){
            Message msg = mFactory.createMessage();
            msg.setId("SetupMessage");
            messages[i]=msg;
        }
        return messages;
    }


    private Message[] createCUWorkMessages(MessageFactory mFactory){
        Iterator commodities = Commodity.getAllCommodities().iterator();
        Message[] messages = new Message[Commodity.getAllCommodities().size()];
        int count=0;
        while (commodities.hasNext()) {
            Commodity c = (Commodity) commodities.next();
            String cName = c.getName();
            double[] prices = c.getPriceInAllExchanges();
            Message msg = mFactory.createMessage();
            msg.setId("CUWorkMessage");
            msg.setValue( "Name", cName );
            msg.setValue("Price",prices);
            msg.setValue("Iteration",new Integer(nIterations));
            messages[count]=msg;
            count++;
        }
        return messages;
    }

    private Message[] createSDFlowWorkMessages(MessageFactory mFactory){
        Iterator commodities = Commodity.getAllCommodities().iterator();
        Message[] messages = new Message[Commodity.getAllCommodities().size()];
        int count=0;
        while (commodities.hasNext()) {
            Commodity c = (Commodity) commodities.next();

            //get name
            String cName = c.getName();

            //get TC and dTC
            Iterator bUtils = c.getBuyingTazZUtilities().values().iterator();
            int nBUtils = c.getBuyingTazZUtilities().values().size();
            double[] TC =  new double[nBUtils];
            double[] dTC = new double[nBUtils];

            while(bUtils.hasNext()){
                BuyingZUtility bUtil = (BuyingZUtility)bUtils.next();
                int index = bUtil.getTaz().getZoneIndex();
                TC[index] = bUtil.getQuantity();
                dTC[index] = bUtil.getDerivative();
            }

            //get TP and dTP
            Iterator sUtils = c.getSellingTazZUtilities().values().iterator();
            int nSUtils = c.getSellingTazZUtilities().values().size();
            double[] TP =  new double[nSUtils];
            double[] dTP = new double[nSUtils];
            while(sUtils.hasNext()){
                SellingZUtility sUtil = (SellingZUtility)sUtils.next();
                int index = sUtil.getTaz().getZoneIndex();
                TP[index] = sUtil.getQuantity();
                dTP[index] = sUtil.getDerivative();
            }

            //get Price
            Iterator exchanges = c.getAllExchanges().iterator();
            double[] price = new double[nSUtils];
            while(exchanges.hasNext()){
                Exchange ex = (Exchange)exchanges.next();
                int index = ex.getExchangeLocationIndex();
                price[index] = ex.getPrice();
            }

            //Create message
            Message msg = mFactory.createMessage();
            msg.setId("SDWorkMessage_"+cName);
            msg.setValue( "Name", cName );
            msg.setValue("TC",TC);
            msg.setValue("TP",TP);
            msg.setValue("dTC",dTC);
            msg.setValue("dTP",dTP);
            msg.setValue("Price",price);

            messages[count]=msg;
            count++;
        }
        return messages;
    }

    private void sendWorkToWorkQueues(Message[] msgs, Port[] WorkPorts, int nWorkQueues){
        long sendTime = System.currentTimeMillis();
        for(int m=0;m<msgs.length;m++){
            //get a message from the array
            Message msg = msgs[m];
            int count = m+1;

            //Send the message
            if(debug) logger.info( getName() + " sent " + msg.getId() + " to " + WorkPorts[(count%nWorkQueues)].getName());
            WorkPorts[(count%nWorkQueues)].send(msg); //will cycle through the ports
                                                        //till all messages are sent
        }
        logger.info("All messages have been sent.  Time in secs: "+ (System.currentTimeMillis()-sendTime)/1000.0);
    }

    public static void signalResultsProcessed() {
        signal.flipValue();
    }

}

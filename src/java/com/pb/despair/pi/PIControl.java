package com.pb.despair.pi;

import java.util.ResourceBundle;

import com.pb.common.util.ResourceUtil;
import com.pb.despair.model.OverflowException;
import org.apache.log4j.Logger;

/**
 * This class runs PI.  It loads the data, instantiates the PIModel
 * and writes out the data at the end of PI.
 *
 * @author Christi Willison
 * @version Mar 16, 2004
 *
 */
public class PIControl {
    private static Logger logger = Logger.getLogger("com.pb.despair.pi");
    private int timePeriod;
    private ResourceBundle piRb;
    private ResourceBundle globalRb;
    private PIPProcessor piReaderWriter;

    public PIControl(Class pProcessorClass, ResourceBundle piRb, ResourceBundle globalRb){
        this.timePeriod = 1;
        this.piRb = piRb;
        this.globalRb = globalRb;
        try {
            piReaderWriter = (PIPProcessor) pProcessorClass.newInstance();
        } catch (InstantiationException e) {
            logger.fatal("Can't create new instance of PiPProcessor of type "+pProcessorClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.fatal("Can't create new instance of PiPProcessor of type "+pProcessorClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        piReaderWriter.setResourceBundles(this.piRb, this.globalRb);
        piReaderWriter.setTimePeriod(this.timePeriod);
    }

    public PIControl(Class pProcessorClass, ResourceBundle rb, int timePeriod){
        this.timePeriod = timePeriod;
        this.piRb = rb;
        try {
            piReaderWriter = (PIPProcessor) pProcessorClass.newInstance();
        } catch (InstantiationException e) {
            logger.fatal("Can't create new instance of PiPProcessor of type "+pProcessorClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.fatal("Can't create new instance of PiPProcessor of type "+pProcessorClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        piReaderWriter.setResourceBundles(this.piRb, this.globalRb);
        piReaderWriter.setTimePeriod(this.timePeriod);
    }

    public void readData(){
        logger.info("Reading data and setting up for PI run");
        long startTime = System.currentTimeMillis();
        piReaderWriter.doProjectSpecificInputProcessing();
        piReaderWriter.setUpPi();
        logger.info("Setup is complete. Time in seconds: "+((System.currentTimeMillis()-startTime)/1000.0));
        return;
    }
    
    public static boolean readFlag(ResourceBundle rb, String flagName, boolean defaultValue) {
        String stringVal = ResourceUtil.getProperty(rb, flagName);
        if (stringVal == null) return defaultValue;
        boolean retVal = defaultValue;
        if (defaultValue == true) {
            if (stringVal.equalsIgnoreCase("false")) {
                retVal = false;
            }
        } else {
            if (stringVal.equalsIgnoreCase("true")) {
                retVal = true;
            }
        }
        return retVal;
    }

    public void runPI(){
        logger.info("*******************************************************************************************");
        logger.info("*   Beginning PI");

        long startTime = System.currentTimeMillis();

        boolean convergenceCriteriaMet = false;
        int maxIterations = 300; //should be read in from file, need to create a method for this.
        boolean nanPresent=false;
        double oldMeritMeasure=Double.POSITIVE_INFINITY; //sum of squares of the surplus over all commodities, all zones
        double newMeritMeasure;
        int nIterations=0; //a counter to keep track of how many iterations it takes before the meritMeasure is within tolerance.

        PIModel pi = new PIModel(piRb, globalRb);
        double tolerance = pi.convergenceTolerance; 
        
        String maxIterationsString = ResourceUtil.getProperty(piRb, "pi.maxIterations");
        if (maxIterationsString == null) {
            logger.info("*   No 'pi.maxIterations' set in properties file -- using default");
        } else {
            int mi = Integer.valueOf(maxIterationsString).intValue();
            maxIterations = mi;
            logger.info("*   Maximum iteration set to " + mi);
        }
        if(maxIterations == 0) convergenceCriteriaMet=true;
        
        // flag as to whether to calculate average price
        boolean calcAvgPrice = readFlag(piRb, "pi.calculateAveragePrices", false);
        boolean calcDeltaUsingDerivatives = readFlag(piRb, "pi.useFullExchangeDerivatives", false);
        
        logger.info("*******************************************************************************************");

        //calculates CUBuy and CUSell for all commodities in all zones
        if (!nanPresent) nanPresent = pi.calculateCompositeBuyAndSellUtilities(); //this will be distributed
        //calculates TP,TC, dTP,dTC for all activities in all zones
        if (!nanPresent) nanPresent = pi.calculateTotalConsumptionAndProduction();
        //calculates the B qty and S qty for each commodity in all zones
        if (!nanPresent) nanPresent = pi.allocateQuantitiesToFlowsAndExchanges(); //this will be distributed
        newMeritMeasure = Double.POSITIVE_INFINITY;
        if (!nanPresent) {
            try {
                newMeritMeasure = pi.calculateMeritMeasureWithLogging();
            } catch (OverflowException e) {
                nanPresent = true;
            }
        } 
        
        if (nanPresent) {
            logger.fatal("Initial prices cause overflow -- try again changing initial prices");
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
            if(newMeritMeasure < tolerance){
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
                
                if (calcAvgPrice) {
                    pi.calculateNewPricesUsingBlockDerivatives(calcDeltaUsingDerivatives);
                } else {
                    pi.calculateNewPrices();
                }

                // this third option -- to use full derivatives -- doesn't fit 
                // into 2GB of memory, so hasn't been tested.  It would probaly
                // be very slow anyways.
                //pi.calculateNewPricesUsingFullDerivatives();
                
                if (!nanPresent)nanPresent = pi.calculateCompositeBuyAndSellUtilities(); //distributed
                if (!nanPresent)nanPresent = pi.recalculateTotalConsumptionAndProduction();
                if (!nanPresent)nanPresent = pi.allocateQuantitiesToFlowsAndExchanges(); //distributed
                nIterations++;
                double tempMeritMeasure = newMeritMeasure;
                if (!nanPresent) {
                    try {
                        newMeritMeasure = pi.calculateMeritMeasureWithLogging(); //calculate surplus for each commodity as well as total surplus
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
                    logger.warn("!!  Not Improving and at second last iteration -- backing up to last valid prices");
                } else if (newMeritMeasure == Double.POSITIVE_INFINITY && pi.getStepSize()<= pi.getMinimumStepSize()) {
                    pi.backUpToLastValidPrices();
                    nIterations = maxIterations-1;
                    logger.fatal("!!  Can't get past infinity without going below minimum step size -- terminating at last valid prices");
                    // TODO need to exit with a different error code if this happens -- this is worse than just not reaching our convergence criteria
                } else {
                    pi.decreaseStepSizeAndAdjustPrices();
                    logger.info("!!  Not Improving -- decreasing step size to "+pi.getStepSize());
                }

                nanPresent = nanPresent || pi.calculateCompositeBuyAndSellUtilities(); //distributed
                nanPresent = nanPresent || pi.recalculateTotalConsumptionAndProduction();
                nanPresent = nanPresent || pi.allocateQuantitiesToFlowsAndExchanges(); //distributed
                nIterations++;
                try {
                    newMeritMeasure = pi.calculateMeritMeasureWithLogging(); //calculate surplus for each commodity as well as total surplus
                } catch (OverflowException e) {
                    nanPresent = true;
                }
                if (nanPresent) {
                    logger.warn("Overflow error, setting new merit measure to positive infinity");
                    newMeritMeasure = Double.POSITIVE_INFINITY;
                }
            }
            if(nIterations == maxIterations) {
                convergenceCriteriaMet=true;
                logger.fatal("Terminating because maximum iterations reached -- did not converge to tolerance");
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
        logger.info(logStmt+((System.currentTimeMillis()-startTime)/1000.0));
        logger.info("*   Final merit measure is "+ newMeritMeasure);
        logger.info("*********************************************************************************************");

        return;
    }

    public void writeData(){
        logger.info("Writing out PI results - this takes up to 15 minutes");
        long startTime = System.currentTimeMillis();
        piReaderWriter.writeOutputs();
        logger.info("Output has been written. Time in seconds: "+((System.currentTimeMillis()-startTime)/1000.0));
        return;
    }

    public static void main(String[] args) {
        ResourceBundle piRb = ResourceUtil.getResourceBundle("pi");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
        String pProcessorClass = ResourceUtil.getProperty(piRb,"pprocessor.class");
        logger.info("PI will be using the " + pProcessorClass + " for pre and post PI processing");
        PIControl pi = null;
        try {
            pi = new PIControl(Class.forName(pProcessorClass),piRb, globalRb);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        pi.readData();
        pi.runPI();
        pi.writeData();
    }
}

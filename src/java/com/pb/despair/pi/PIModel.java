package com.pb.despair.pi;

import com.pb.common.util.ResourceUtil;
import com.pb.despair.model.AbstractTAZ;
import com.pb.despair.model.ModelComponent;
import com.pb.despair.model.OverflowException;
import com.pb.despair.model.ProductionActivity;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.logging.Logger;


/**
 * PIModel essentially has to: 1) Call migrationAndAllocation for each ProductionActivity for a given time step.
 * 2) if some Production Activities are modelled as being in equilibrium with each other, PIModel will need to adjust
 * prices/utilities and then call reMigrationAndReAllocation for those ProductionActivities repeatedly, adjusting prices until
 * an equilibrium is achived Note that different ProductionActivities could have different time steps.  Thus a
 * DisaggregateActivity could be simulated with a monthly time step with some prices adjusted after each month, while a set of
 * AggregateActivities are simulated as being in equilibrium at the end of each one-year time period.
 *
 * @author John Abraham
 */
public class PIModel extends ModelComponent {
    private static Logger logger = Logger.getLogger("com.pb.despair.pi");

    private double minimumStepSize = 1.0E-4; //default value
    private double stepSize=.5; //actual stepSize that will be used by PI, will go up or down depending on the
                                //merit measure.  It is changed via the "increaseStepSize"
                                // and "decreaseStepSizeAndAdjustPrices" method.
    private double maximumStepSize = 1.0; // default value

    // re-activate this code if we need some specific adjustments for specific stubborn commodities 
    // private double commoditySpecificScalingAdjustment = 0; //default value

    HashMap newPricesC = null; //a new HashMap will be created every time "calculateNewPrices" is called
    HashMap oldPricesC = new HashMap();

    public double convergenceTolerance;

    /* This constructor is only called by AO when running PI monolithically (which will not be called very often
    * if ever).  The properties are set by a call to the setApplicationResourceBundle method which is defined in the Model
    * Component class and then PIModel is started by a call to the 'startModel(timeInterval)' method which
    * calls PIControl
    */
    public PIModel(){

    }

    /*This constructor is used by PIControl and by PIDAF - The pi.properties file will not be
    * in the classpath when running
    * on the cluster (or even in the mono-version when running for mulitple years using AO)
    * and therefore we need to pass in the ResourceBundle to PIModel from within
    * the PIServerTask.  Because there may be more than 1 pi.properties file over the course of
    * a 30 year simulation it is not practical to include 30 different location in the classpath.
    * AO will locate the most recent pi.properties file based on the timeInterval and will write it's
    * absolute path location into a file called 'RunParameters.txt' where it will be read in by
    * PIServerTask in it's 'onStart( ) method and will subsequently be passed to PIModel during the
    * 'new PIModel(rb)' call.
    */
    public PIModel(ResourceBundle piRb, ResourceBundle globalRb){
        setResourceBundles(piRb, globalRb);
        String initialStepSizeString = ResourceUtil.getProperty(piRb, "pi.initialStepSize");
        if (initialStepSizeString == null) {
            logger.info("*   No pi.initialStepSize set in properties file -- using default");
        } else {
            double iss = Double.valueOf(initialStepSizeString).doubleValue();
            this.stepSize=iss; //set the stepSize to the initial value
            logger.info("*   Initial step size set to " + iss);
        }

        String minimumStepSizeString = ResourceUtil.getProperty(piRb, "pi.minimumStepSize");
        if (minimumStepSizeString == null) {
            logger.info("*   No pi.minimumStepSize set in properties file -- using default");
        } else {
            double mss = Double.valueOf(minimumStepSizeString).doubleValue();
            this.minimumStepSize=mss;
            logger.info("*   Minimum step size set to " + mss);
        }

        String maximumStepSizeString = ResourceUtil.getProperty(piRb, "pi.maximumStepSize");
        if (maximumStepSizeString == null) {
            logger.info("*   No pi.maximumStepSize set in properties file -- using default");
        } else {
            double mss = Double.valueOf(maximumStepSizeString).doubleValue();
            this.maximumStepSize=mss;
            logger.info("*   Maximum step size set to " + mss);
        }

        String convergedString = ResourceUtil.getProperty(piRb, "pi.converged");
        if (convergedString == null) {
            logger.info("*   No pi.converged set in properties file -- using default");
        } else {
            double converged = Double.valueOf(convergedString).doubleValue();
            this.convergenceTolerance = converged;
            logger.info("*   Convergence tolerance set to " + converged);
        }

        // reactivate this code for different step sizes for stubborn commodities
        //String commoditySpecificAdjustmentString = ResourceUtil.getProperty(rb, "pi.commoditySpecificAdjustment");
        //if (commoditySpecificAdjustmentString == null) {
        //    logger.info("*   No pi.commoditySpecificAdjustment set in properties file -- using default");
        //} else {
        //    double csa = Double.valueOf(commoditySpecificAdjustmentString).doubleValue();
        //    this.commoditySpecificScalingAdjustment = csa;
        //    logger.info("*   Commodity specific adjustment set to " + csa);
        //}

    }



    /* This method calculates  CUBuy and CUSell for each commodity in each zone.
    ** The values are set in the appropriate commodityZUtility object
    */
    public boolean calculateCompositeBuyAndSellUtilities(){
        logger.fine("Entering 'fixPricesAndConditionsForAllCommodities'");
        long startTime = System.currentTimeMillis();
        Commodity.unfixPricesAndConditionsForAllCommodities();
        boolean nanPresent=false;
        Iterator allOfUs = Commodity.getAllCommodities().iterator();
        while (allOfUs.hasNext()) {
            Commodity c = (Commodity) allOfUs.next();
            // TODO just for debugging create storage to save composite utilities calculated
            //double[][]compUtils = null;
            try {
                // TODO just for debugging save the calculated compUtils
                //compUtils = c.fixPricesAndConditionsAtNewValues();
                // TODO normally just do this"
                c.fixPricesAndConditionsAtNewValues();
            } catch (OverflowException e) {
                nanPresent = true;
                logger.warning("Overflow error in CUBuy, CUSell calcs");
            }

            //TODO just for testing -- remove this code after May 27 2004.
            //if (c.getName().equalsIgnoreCase("CONSTRUCTION")) {
             //   logger.info("CONSTRUCTION in zone "+AbstractTAZ.getZone(1).getZoneUserNumber()+" buying utility calculated is "+compUtils[0][1]);
            //    logger.info("CONSTRUCTION in zone "+AbstractTAZ.getZone(1).getZoneUserNumber()+" selling utility calculated is "+compUtils[1][1]);
            //}
            
           }
        logger.info("Composite buy and sell utilities have been calculated for all commodities. Time in seconds: "+((System.currentTimeMillis()-startTime)/1000.0));
        return nanPresent;
    }

    /*PI-DAF Method:  The following method will be called when PI is distributed and replaces the "calculateCompositeBuyAndSellUtilities()
    * method, that is used when PI is running on a single machine.
    */
    public double[][] calculateCompositeBuyAndSellUtilities(String name){
        double[][] compUtils = null; //compUtils[0][] = buy utils, compUtils[1][]= sell utils
        Commodity c = Commodity.retrieveCommodity(name);
        c.unfixPricesAndConditions();
        
        try {
            compUtils = c.fixPricesAndConditionsAtNewValues();
        } catch (OverflowException e) {
            logger.warning("Overflow error in CUBuy, CUSell calcs for commodity "+name);
            e.printStackTrace();
        }

        //TODO just for testing -- remove this code after May 27 2004.
        //if (name.equalsIgnoreCase("CONSTRUCTION")) {
        //    logger.info("CONSTRUCTION in zone "+AbstractTAZ.getZone(1).getZoneUserNumber()+" buying utility calculated is "+compUtils[0][1]);
        //    logger.info("CONSTRUCTION in zone "+AbstractTAZ.getZone(1).getZoneUserNumber()+" selling utility calculated is "+compUtils[1][1]);
        //}
        
        return compUtils;
    }

    /* This method calculates the TC, TP and derivative of TC and derivative of TP for each commodity in each zone.
    *  Along the way, it calculates several utilitiy functions.  The TC, TP and dTC, dTP are stored in the
    *  appropriate CommodityZUtilitiy object (quantity and derivative)
    */
    public boolean calculateTotalConsumptionAndProduction(){
        logger.fine("Beginning Activity Iteration: calling 'migrationAndAllocationWithOverflowTracking' for each activity");
        resetCommodityBoughtAndSoldQuantities();
        long startTime = System.currentTimeMillis();
        boolean nanPresent=false;
        int count=1;
        Iterator it = ProductionActivity.getAllProductionActivities().iterator();
        while (it.hasNext()) {
            AggregateActivity aa = (AggregateActivity) it.next();
            long activityStartTime = System.currentTimeMillis();
            try {
                aa.migrationAndAllocation(1.0, 0, 0);
            } catch (OverflowException e) {
                nanPresent = true;
                logger.warning("Overflow error in CUBuy, CUSell calcs");
            }
            logger.finer("Finished activity "+ count+ " in "+ (System.currentTimeMillis()-activityStartTime)/1000.0+ " seconds");
            count++;
        }
        logger.info("Finished all Activity allocation: Time in seconds: "+(System.currentTimeMillis()-startTime)/1000.0);
        return nanPresent;
    }

    /* This method calculates the TC, TP and derivative of TC and derivative of TP for each commodity in each zone.
    *  Along the way, it calculates several utilitiy functions.  The TC, TP and dTC, dTP are stored in the
    *  appropriate CommodityZUtilitiy object (quantity and derivative)
    */
    public boolean recalculateTotalConsumptionAndProduction(){
        logger.fine("Beginning Activity Iteration: calling 'reMigrationAndAllocationWithOverflowTracking' for each activity");
        resetCommodityBoughtAndSoldQuantities();
        long startTime = System.currentTimeMillis();
        boolean nanPresent=false;
        int count=1;
        Iterator it = ProductionActivity.getAllProductionActivities().iterator();
        while (it.hasNext()) {
            AggregateActivity aa = (AggregateActivity) it.next();
            long activityStartTime = System.currentTimeMillis();
            try {
                aa.reMigrationAndReAllocationWithOverflowTracking();
            } catch (OverflowException e) {
                nanPresent = true;
                logger.warning("Overflow error in CUBuy, CUSell calcs");
            }
            logger.finer("Finished activity "+ count+ " in "+ (System.currentTimeMillis()-activityStartTime)/1000.0+ " seconds");
            count++;
        }
        logger.info("Finished all Activity allocation: Time in seconds: "+(System.currentTimeMillis()-startTime)/1000.0);
        return nanPresent;
    }

    /* This method calculates the Buying and Selling quantities of each commodity produced or consumed in
    *  each zone that is allocated to a particular exchange zone for selling or buying.
    *  Bc,z,k and Sc,z,k.
    */
    public boolean allocateQuantitiesToFlowsAndExchanges(){
        logger.fine("Beginning 'allocateQuantitiesToFlowsAndExchanges'");
        long startTime = System.currentTimeMillis();
        boolean nanPresent=false;
        Commodity.clearAllCommodityExchangeQuantities();//iterates through the exchange objects inside the commodity
                                                        //objects and sets the sell, buy qtys and the derivatives to 0
        Iterator allComms = Commodity.getAllCommodities().iterator();
        int count=1;
        while(allComms.hasNext()){
            Commodity c = (Commodity) allComms.next();
            long activityStartTime = System.currentTimeMillis();
            for (int b = 0; b < 2; b++) {
                Hashtable ht;
                if (b == 0)
                    ht = c.getBuyingTazZUtilities();
                else
                    ht = c.getSellingTazZUtilities();
                Iterator it = ht.values().iterator();
                while(it.hasNext()){
                    CommodityZUtility czu = (CommodityZUtility) it.next();
                    try {
                        czu.allocateQuantityToFlowsAndExchanges();
                    } catch (OverflowException e) {
                        nanPresent = true;
                        logger.warning("Overflow error in Bc,z,k and Sc,z,k calculations");
                    }
                }
            }
            c.setFlowsValid(true);
            logger.finer("Finished allocating commodity "+ count + " in "+ (System.currentTimeMillis()-activityStartTime)/1000.0+ " seconds");
            count++;
        }
        logger.info("All commodities have been allocated.  Time in seconds: "+(System.currentTimeMillis()-startTime)/1000.0);
        return nanPresent;
    }

    /* PI-DAF Method: allocates the total production/consumption amount in a zone of a single commodity
    * to the exchange zone as Buying and Selling quantities (Bc,z,k and Sc,z,k).
    *
    */
    public Commodity allocateQuantitiesToFlowsAndExchanges(String name, double[] TCs, double[] TPs, double[] dTCs, double[] dTPs, double[] prices){
        boolean nanPresent=false;
        Commodity c = Commodity.retrieveCommodity(name);
        c.clearAllExchangeQuantities();//iterates through the exchange objects inside the commodity
                                       //object and sets the sell, buy qtys, surplus and the derivatives to 0

        c.setPriceInAllExchanges(prices);
        //Next go through and set the quantities and derivatives in the appropriate CommodityZUtility object
        //and then allocate those quantities to flows and exchanges.

        //First set TC, dTC and then calculate Bc,z,k
        Iterator bUtils = c.getBuyingTazZUtilities().values().iterator();
        while(bUtils.hasNext()){
           CommodityZUtility czu = (CommodityZUtility) bUtils.next();
           try {
               czu.setQuantity(TCs[czu.getTaz().getZoneIndex()]);
               czu.setDerivative(dTCs[czu.getTaz().getZoneIndex()]);
               czu.allocateQuantityToFlowsAndExchanges();
           } catch (OverflowException e) {
               nanPresent = true;
               logger.warning("Overflow error in total consumption or Bc,z,k calculations");
           }

        }

        //Next set TP, dTP and calculate Sc,z,k
        Iterator sUtils = c.getSellingTazZUtilities().values().iterator();
        while(sUtils.hasNext()){
           CommodityZUtility czu = (CommodityZUtility) sUtils.next();
           try {
               czu.setQuantity(TPs[czu.getTaz().getZoneIndex()]);
               czu.setDerivative(dTPs[czu.getTaz().getZoneIndex()]);
               czu.allocateQuantityToFlowsAndExchanges();
           } catch (OverflowException e) {
               nanPresent = true;
               logger.warning("Overflow error in total production or Sc,z,k calculations");
           }
        }
        c.setFlowsValid(true);
        return c;
    }

   /* PI-DAF Method:  This method (called by the SDWorkTask) 
    *  calculates the imports and exports in each exchange zone and returns the surplus and the derivative of
    *  the particular commodity in each exchange zone.
    */
    public double[][] calculateSurplusAndDerivatives(String name){

//       logger.info("Beginning 'calculateSurplusAndDervatives'");
//       long startTime = System.currentTimeMillis();
       Commodity c = Commodity.retrieveCommodity(name);
       double[][] sAndD = new double[2][c.getBuyingTazZUtilities().values().size()]; //sAndD[0][]= surplus in each exchange, sAndD[1] = derivative in each zone
       boolean nanPresent = false;

       //calulate the surplus and derivatives in each exchange.
       Iterator exchanges = c.getAllExchanges().iterator();
       while (exchanges.hasNext() && !nanPresent) {
            Exchange ex = (Exchange) exchanges.next();
            int eIndex=ex.getExchangeLocationIndex();
            double[] surplusAndDerivative = ex.exchangeSurplusAndDerivative();
            if (Double.isNaN(surplusAndDerivative[0]) || Double.isNaN(surplusAndDerivative[1]) ) {
                nanPresent = true;
                logger.warning("NaN present at "+ex);
                //TODO when distributed we have to deal with these - but what is the best way?
            }
            sAndD[0][eIndex]=surplusAndDerivative[0];
            sAndD[1][eIndex]=surplusAndDerivative[1];
        }

       //Now return the surplus and derivatives for a particular commodity in all exchange zones
       // to the SDWorkTask where the sAndD will be put into a message and sent to the SDResultsQueue.
//       logger.info("Surplus and Derivative in each exchange zone has been calculated for "+name+".  Time in seconds: "+(System.currentTimeMillis()-startTime)/1000.0);
       return sAndD;
    }


    public double calculateMeritMeasureWithoutLogging() throws OverflowException {
        boolean nanPresent = false;
        double meritMeasure = 0;
        Iterator commodities = Commodity.getAllCommodities().iterator();
        while (commodities.hasNext()) {
            Commodity c = (Commodity) commodities.next();
            Iterator exchanges = c.getAllExchanges().iterator();
            while (exchanges.hasNext() && !nanPresent) {
                Exchange ex = (Exchange) exchanges.next();
                double surplus = ex.exchangeSurplus();
                if (Double.isNaN(surplus)){
                    nanPresent = true;
                    throw new OverflowException("NaN present at "+ex);
                }
                meritMeasure += c.compositeMeritMeasureWeighting * c.compositeMeritMeasureWeighting * surplus * surplus;
            }
        }
        return meritMeasure;
    }

    public double calculateMeritMeasureWithLogging() throws OverflowException {
        boolean nanPresent = false;
        double meritMeasure = 0;
        Iterator commodities = Commodity.getAllCommodities().iterator();
        while (commodities.hasNext()) {
            double maxSurplus = 0;
            double maxSurplusSigned = 0;
            double maxPrice = Double.MIN_VALUE;
            double minPrice = Double.MAX_VALUE;
            double commodityMeritMeasure = 0;
            Exchange minPriceExchange = null;
            Exchange maxPriceExchange = null;

            Exchange maxExchange = null;
            Commodity c = (Commodity) commodities.next();
            logger.info(c.toString());
            Iterator exchanges = c.getAllExchanges().iterator();
            while (exchanges.hasNext() && !nanPresent) {
                Exchange ex = (Exchange) exchanges.next();
                double surplus = ex.exchangeSurplus();
                if (Math.abs(surplus) > maxSurplus) {
                    maxExchange = ex;
                    maxSurplus = Math.abs(surplus);
                    maxSurplusSigned = surplus;
                }
                if (ex.getPrice() > maxPrice) {
                    maxPrice = ex.getPrice();
                    maxPriceExchange = ex;
                }
                if (ex.getPrice() < minPrice) {
                    minPrice = ex.getPrice();
                    minPriceExchange = ex;
                }
                if (Double.isNaN(surplus)) {  /* || newPrice.isNaN()*/
                    nanPresent = true;
                    logger.warning("\t NaN present at "+ex);
                    throw new OverflowException("\t NaN present at "+ex);
                }
                meritMeasure += c.compositeMeritMeasureWeighting * c.compositeMeritMeasureWeighting * surplus * surplus;
                commodityMeritMeasure += surplus * surplus;
            }
            if (maxExchange != null) {
                logger.info("\t maxSurp: " + maxSurplusSigned + " in " + maxExchange + " planning DP from " + maxExchange.getPrice() /* + " to " + newPriceAtMaxExchange */);
            }
            if (maxPriceExchange != null) {
                logger.info("\t PMax " + maxPrice + " in " + maxPriceExchange);
            }
            if (minPriceExchange != null) {
                logger.info("\t PMin " + minPrice + " in " + minPriceExchange);
            }
            
            if (commodityMeritMeasure > c.oldMeritMeasure) {
                // reactivate this if you need separate step sizes for different commodities
                //c.scalingAdjustmentFactor *= (1 + commoditySpecificScalingAdjustment);
                logger.info("\t Meritmeasure " + commodityMeritMeasure + " NOT IMPROVING was " + c.oldMeritMeasure );
            } else {
                // reactivate this if you need separate step sizes for different commodities
                //c.scalingAdjustmentFactor /= (1 + commoditySpecificScalingAdjustment);
                logger.info("\t Meritmeasure " + commodityMeritMeasure + " (was " + c.oldMeritMeasure + ")");
            }
            c.oldMeritMeasure = commodityMeritMeasure;

        }
        return meritMeasure;

    }

    public void increaseStepSize(){
        final double increaseStepSizeMultiplier = 1.1;
        stepSize = stepSize * increaseStepSizeMultiplier;
        if (stepSize > maximumStepSize) stepSize = maximumStepSize;
    }

    public void decreaseStepSizeAndAdjustPrices(){
        final double decreaseStepSizeMultiplier = 0.5;
        stepSize = stepSize * decreaseStepSizeMultiplier;
        if(stepSize < minimumStepSize) stepSize = minimumStepSize;
        newPricesC = StepPartwayBackBetweenTwoOtherPrices(oldPricesC, newPricesC, decreaseStepSizeMultiplier);
        setExchangePrices(newPricesC);
    }
    
    public void backUpToLastValidPrices() {
        setExchangePrices(oldPricesC);
    }

    public void calculateNewPrices() {
        newPricesC = new HashMap();
        Iterator commodities = Commodity.getAllCommodities().iterator();
        while (commodities.hasNext()) {
            Commodity c = (Commodity) commodities.next();
            Iterator exchanges = c.getAllExchanges().iterator();
            while (exchanges.hasNext()) {
                Exchange ex = (Exchange) exchanges.next();
                double[] surplusAndDerivative = ex.exchangeSurplusAndDerivative();
                Double newPrice = new Double(ex.getPrice() - ((stepSize * surplusAndDerivative[0]) / surplusAndDerivative[1]));
                if (ex.monitor || Double.isNaN(surplusAndDerivative[0])) {
                    logger.info("Exchange:" + ex + " surplus:" + surplusAndDerivative[0] + " planning price change from " +
                            ex.getPrice() + " to " + newPrice);
                }
                newPricesC.put(ex, newPrice);
            }
        }
        setExchangePrices(newPricesC);
    }

    /**
     *
     */
    public void snapShotCurrentPrices() {
        Iterator commodities = Commodity.getAllCommodities().iterator();
        while (commodities.hasNext()) {
            Commodity c = (Commodity) commodities.next();
            Iterator exchanges = c.getAllExchanges().iterator();
            while (exchanges.hasNext()) {
                Exchange ex = (Exchange) exchanges.next();
                oldPricesC.put(ex,new Double(ex.getPrice()));
            }
        }
    }

    private static void setExchangePrices(HashMap prices) {
		Iterator it = Commodity.getAllCommodities().iterator();
		while (it.hasNext()) {
			Commodity c = (Commodity) it.next();
            c.unfixPricesAndConditions();
		}
        it = prices.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            Exchange x = (Exchange) e.getKey();
            Double price = (Double) e.getValue();
            x.setPrice(price.doubleValue());
        }
    }
    
    /**
     * For each price calculates the difference between the first one and the second one, 
     * and returns a new price that is a scaled back step
     * 
     * @param firstPrices Hashmap of first set of prices keyed by exchange
     * @param secondPrices Hashmap of second set of prices keyed by exchange
     * @param howFarBack how far back to step (0.5 is halfway back, 0.75 is 3/4 way back)
     * @return Hashmap of new prices in between the other two
     */
    private static HashMap StepPartwayBackBetweenTwoOtherPrices(HashMap firstPrices, HashMap secondPrices, double howFarBack) {
        HashMap newPrices = new HashMap();
        Iterator commodities = Commodity.getAllCommodities().iterator();
        while (commodities.hasNext()) {
            Commodity c = (Commodity) commodities.next();
            Iterator exchanges = c.getAllExchanges().iterator();
            while (exchanges.hasNext()) {
                Exchange ex = (Exchange) exchanges.next();
                Double aggressivePrice = (Double) secondPrices.get(ex);
                Double previousPrice = (Double) firstPrices.get(ex);
                double lessAggressivePrice = (aggressivePrice.doubleValue() - previousPrice.doubleValue()) *
                howFarBack + previousPrice.doubleValue();
                newPrices.put(ex, new Double(lessAggressivePrice));
                if (ex.monitor) {
                    logger.info("Exchange:" + ex + "  reducing amount of price change, changing p from " + aggressivePrice + " to " + lessAggressivePrice);
                }
            }
        }
        return newPrices;
    }
    
    /* This method just resets the quantities and derviative to 0 to prepare
    *  for the next iteration
    */
    private void resetCommodityBoughtAndSoldQuantities() {
        Iterator cit = Commodity.getAllCommodities().iterator();
        while (cit.hasNext()) {
            Commodity c = (Commodity) cit.next();
            for (int b = 0; b < 2; b++) {
                Hashtable ht;
                if (b == 0)
                    ht = c.buyingTazZUtilities;
                else
                    ht = c.sellingTazZUtilities;
                Iterator it = ht.values().iterator();
                while (it.hasNext()) {
                    CommodityZUtility czu = (CommodityZUtility) it.next();
                    try {
                        czu.setQuantity(0);
                        czu.setDerivative(0);
                    } catch (OverflowException e) {
                    }
                }
            }
        }
    }



/* --- Getters and Setters for PIModel properties ------------------ */

    public double getStepSize() {
        return stepSize;
    }

    public double getMinimumStepSize() {
        return minimumStepSize;
    }

    /* This method is called by AO.  Before startModel is called the most
    * current pi.properties file has been found in the tn subdirectories
    * and has been set by calling the setApplicationResourceBundle method (a ModelComponent method)
    * from AO.
    */
    public void startModel(int timeInterval){
        String pProcessorClass = ResourceUtil.getProperty(appRb,"pprocessor.class");
        logger.info("PI will be using the " + pProcessorClass + " for pre and post PI processing");
        PIControl pi = null;
        try {
            pi = new PIControl(Class.forName(pProcessorClass), appRb, globalRb);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        pi.readData();
        pi.runPI();
        pi.writeData();
    };




    ///////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////
    /****************************************************************************************************
    * ALL METHODS BELOW THIS LINE HAVE BEEN COMMENTED OUT BECAUSE THEY ARE NOT BEING USED (AS OF 3/18/04)
    * OR THEY HAVE BEEN MOVED TO OTHER CLASSES.
    *****************************************************************************************************/
        /* Main workhorse of PI.  Tries to determine equilibrium prices */
//    public void iterateAggregateFactors() {
//
//        int iterations = 0;
//        double oldMeritMeasure;
//        double newMeritMeasure = 0;
//        HashMap oldPrices = new HashMap();
//        HashMap currentPrices = new HashMap();
//        HashMap newPrices = new HashMap();
//        double stepSize =1;
//
//
//        stepSize = initialStepScaling;
//        logger.info("##### progress ##### entering iterateAggregateFactors");
//
//        //First calculate the total quantity of activity a in all zones,
//        //the total consumption and production of commodities in all zones
//        //and the total buy and sell qty of commodities in the exchanges
//        allocateCommoditiesAndActivitiesBasedOnInitialPrices(); //will throw a runtime exception if initial prices are
//                                                                //unreasonable.
//
//        logger.info("Assigning new prices to Commodities ");
//        long startTime = System.currentTimeMillis();
//        updateCurrentPrices(currentPrices);
//        newPrices = calculateNewPrices(stepSize);
//        try {
//            newMeritMeasure = calculateMeritMeasureWithoutLogging();
//        } catch (OverflowException e) {
//            logger.severe("Overflow with initial prices -- terminating");
//            throw new RuntimeException("Overflow with initial prices -- terminating");
//        }
//        logger.info("New prices have been defined. Time in seconds: "+(System.currentTimeMillis()-startTime)/1000.0);
//
//        logger.info("##### progress ##### entering do-loop in iterateAggregateFactors");
//        int numConverged = 0;
//        do {
//            boolean nanPresent = false;
//            iterations++;
//            logger.info("Starting iteration " + iterations);
//            long loopStartTime = System.currentTimeMillis();
//            // set the new prices;
//            setExchangePrices(newPrices);
//            // redo the spatial allocation with the new prices
//            logger.info("Starting commodity reallocation");
//            long allocationStartTime = System.currentTimeMillis();
//            nanPresent=reallocateCommoditiesAndActivitiesBasedOnFixedPrices();
//            logger.info("Finished allocation commodities. Time in seconds: "+ (System.currentTimeMillis()-allocationStartTime)/1000.0);
//
//            // figure out whether we've gotten anywhere, and figure out another tentative step too.
//            logger.info("Assigning new prices to Commodities if necessary ");
//            startTime = System.currentTimeMillis();
//            oldMeritMeasure = newMeritMeasure;
//            try {
//                newMeritMeasure = calculateMeritMeasureWithLogging();
//            } catch (OverflowException e) {
//                nanPresent = true;
//            }
//            oldPrices = currentPrices;
//            currentPrices = newPrices;
//            newPrices = calculateNewPrices(stepSize);
//            logger.info("Merit measures: old: " + oldMeritMeasure + " new: " + newMeritMeasure);
//            if (nanPresent) {
//                // big problems -- back UP!
//                stepSize *= decreaseStepSizeMultiplier;
//                logger.warning("NaN present -- back up all the way and do it again");
//                logger.warning("** decreasing step size to " + stepSize);
//                newPrices = (HashMap) oldPrices.clone();
//                newMeritMeasure = oldMeritMeasure;
//                currentPrices = oldPrices;
//                nanPresent = false;
//            } else if (newMeritMeasure > oldMeritMeasure && newMeritMeasure / oldMeritMeasure > 1.0000000001 && stepSize > getMinimumStepSize()) {
//                // going the wrong way...
//                // lets not be so agressive
//                stepSize *= decreaseStepSizeMultiplier;
//                logger.info("** decreasing step size to " + stepSize + " abandoning planned price changes");
//                // abandon the tentative price change, and, as well, back up the step size for our last price change
//                newPrices = StepPartwayBackBetweenTwoOtherPrices(oldPrices,currentPrices,decreaseStepSizeMultiplier);
//                // back everything else up
//                newMeritMeasure = oldMeritMeasure;
//                currentPrices = oldPrices;
//            } else {
//                if (newMeritMeasure / oldMeritMeasure > 0.999) numConverged++; else numConverged = 0;
//                stepSize *= increaseStepSizeMultiplier;
//                if (numConverged >= 300) logger.info("no progress -- stopping and claiming victory");
//                // just go and do it again
//            }
//            logger.info("New prices have been defined. Time in seconds: "+(System.currentTimeMillis()-startTime)/1000.0);
//
//            logger.info("Finishing iteration "+iterations+" Time in seconds: "+(System.currentTimeMillis()-loopStartTime)/1000.0);
//        } while (numConverged < 300 && newMeritMeasure > converged && iterations < maxIterations);
//        logger.info("##### progress ##### exiting do-loop in iterateAggregateFactors");
//        if (iterations >= maxIterations) logger.warning("stopping because max iterations reached");
//    }

    //public void runComponent() {
        // message #1.1 to disaggregateFactors:com.pb.despair.pi.DisaggregateActivity
        // disaggregateFactors.migrationAndAllocation(float, float, float);
        // message #1.2 to aggregateFactors:com.pb.despair.pi.AggregateActivity
        // aggregateFactors.migrationAndAllocation(float, float, float);
        // message #1.3 to thePAModel:com.pb.despair.pi.PIModel
        // this.iterateAggregateFactors();
    //}



//    public void getCoefficients(AppProperties props) {
//        increaseStepSizeMultiplier = Double.parseDouble(props.getProperty("increase.stepsize"));
//        decreaseStepSizeMultiplier = Double.parseDouble(props.getProperty("decrease.stepsize"));
//        setMinimumStepSize(Double.parseDouble(props.getProperty("minimum.stepsize")));
//        converged = Double.parseDouble(props.getProperty("converged"));
//        setInitialStepScaling(Double.parseDouble(props.getProperty("initial.stepsize")));
//        maxIterations = Integer.parseInt(props.getProperty("maximum.iterations"));
//        logger.info("Pi iteration parameters changed ...");
//        logger.info("increaseStepSize = " + increaseStepSizeMultiplier);
//        logger.info("decreaseStepSize = " + decreaseStepSizeMultiplier);
//        logger.info("minimumStepSize = " + getMinimumStepSize());
//        logger.info("converged = " + converged);
//        logger.info("initial step size = " + getInitialStepScaling());
//        logger.info("maximum iterations = " + maxIterations);
//    }

    /*DON'T MAKE CHANGES TO THIS METHOD.  WE HAVE MOVED ALL DATA WRITING METHODS
    * INTO THE PIDataWriter CLASS INSTEAD !!!
    */
    //public void writeZUtilitiesTable()

    /*DON'T MAKE CHANGES TO THIS METHOD.  WE HAVE MOVED ALL DATA WRITING METHODS
    * INTO THE PIDataWriter CLASS INSTEAD !!!
    */
    //public void writeLocationTable()
    /*DON'T MAKE CHANGES TO THIS METHOD.  WE HAVE MOVED ALL DATA WRITING METHODS
    * INTO THE PIDataWriter CLASS INSTEAD !!!
    */
    //public void writeExchangeResults()

    /*DON'T MAKE CHANGES TO THIS METHOD.  WE HAVE MOVED ALL DATA WRITING METHODS
    * INTO THE PIDataWriter CLASS INSTEAD !!!
    */
    //public void writeFlowTextFiles()

    //private void unfixPricesAndConditions() //not currently used

    //private void fixAllPricesAndConditions()//not currently used

//    public void allocateCommoditiesAndActivitiesBasedOnInitialPrices(){
//        try {
//            logger.info("Entering 'Commodity.fixPricesAndConditionsForAllCommodities'");
//            long startTime = System.currentTimeMillis();
//            Commodity.fixPricesAndConditionsForAllCommodities();//calculate all CUBuy and CUSell
//            logger.info("'Commodity.fixPricesAndConditionsForAllCommodities' is complete. Time in seconds: "+((System.currentTimeMillis()-startTime)/1000.0));
//
//            logger.info("Entering 'resetCommoditiesBoughtAndSoldQuantities'");
//            startTime = System.currentTimeMillis();
//            resetCommodityBoughtAndSoldQuantities();//setting sell qty and buy qty to 0 and setting derivative to 0
//                                                        // in the CommodityZUtility objects (all comms, all zones)
//            logger.info("'resetCommoditiesBoughtAndSoldQuantities' is complete. Time in seconds: "+((System.currentTimeMillis()-startTime)/1000.0));
//
//            logger.info("Beginning Activity Iteration: calling 'migrationAndAllocation' for each activity");
//            startTime = System.currentTimeMillis();
//            int count=1;
//            Iterator pit = ProductionActivity.getAllProductionActivities().iterator();
//            while (pit.hasNext()) {
//                AggregateActivity aa = (AggregateActivity) pit.next();
//                long activityStartTime = System.currentTimeMillis();
//                aa.migrationAndAllocation(1.0, 0, 0); //allocates activities to zones (timeStep=1, inMigration=0, outMigration=0
//                logger.info("Finished activity "+ count+ " in "+ (System.currentTimeMillis()-activityStartTime)/1000.0+ " seconds");
//                count++;                                //i.e. calculates Wa,z (total qty of activity aa in zone z),
//            }                                          // and calculates total production and consumption of each commodity
//                                                      //in each zone (TPc,z and TCc,z)
//            logger.info("All activity calculations are complete. Time in seconds: "+((System.currentTimeMillis()-startTime)/1000.0));
//
//            logger.info("Beginning Commodity Iteration: calling 'allocateQuantitiesToFlowsAndExchanges' for each commodity");
//            startTime = System.currentTimeMillis();
//            allocateQuantitiesToFlowsAndExchanges(); //calculates the buy and sell qtys (Sc,z,k & Bc,z,k)
//                                                    //flowing from the zones to the exchanges (+sell, -buy(flow is really
//                                                    //from exchanges to zones.))
//            logger.info("All commodity calculations are complete. Time in seconds: "+((System.currentTimeMillis()-startTime)/1000.0));
//
//       } catch (OverflowException e1) {
//                logger.severe("Overflow when running from starting prices -- change starting prices and retry");
//                e1.printStackTrace();
//                throw new RuntimeException("Overflow when running from starting prices -- change starting prices and retry");
//       }
//       Commodity.unfixPricesAndConditionsForAllCommodities();
//    }


}
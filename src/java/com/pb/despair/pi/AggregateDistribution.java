package com.pb.despair.pi;

import com.pb.despair.model.*;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

/**
 * Attributes (and methods) that relate to a particular ProductionActivity in a particular zone.  Includes the quantity, net
 * tax, and any constraints.  Also a zone-specific Utility. This implements the "Alternative" interface because the total
 * amount of activity is allocated amongst these with a logit model.
 *
 * @author John Abraham
 */
public class AggregateDistribution extends AmountInZone implements AggregateAlternative {
    protected static transient Logger logger =
            Logger.getLogger("com.pb.despair.pi");
    double derivative;
    ConsumptionFunction lastConsumptionFunction;
    ProductionFunction lastProductionFunction;

    static int numdebug = 0;

    public AggregateDistribution(ProductionActivity p, AbstractTAZ t) {
        super(p, t);
    }



    /**
     * Method updateLocationUtilityTerms.
     */
    public void updateLocationUtilityTerms(Writer w) {
        ConsumptionFunction cf = lastConsumptionFunction;
        ProductionFunction pf = lastProductionFunction;
        double[] buyingCommodityUtilities = new double[cf.size()];
        for (int c = 0; c < cf.size(); c++) {
            AbstractCommodity com = cf.commodityAt(c);
            if (com == null) {
                buyingCommodityUtilities[c] = 0;
            } else {
                try {
                    buyingCommodityUtilities[c] = cf.commodityAt(c).calcZUtility(getMyTaz(), false);
                } catch (OverflowException e1) {
                    buyingCommodityUtilities[c] = Double.NaN;
                }
            }
        }
        double[] sellingCommodityUtilities = new double[pf.size()];
        for (int c = 0; c < pf.size(); c++) {
            AbstractCommodity com = pf.commodityAt(c);
            if (com == null) {
                sellingCommodityUtilities[c] = 0;
            } else {
                try {
                    sellingCommodityUtilities[c] = pf.commodityAt(c).calcZUtility(getMyTaz(), true);
                } catch (OverflowException e1) {
                    sellingCommodityUtilities[c] = Double.NaN;
                }
            }
        }
        double productionUtility;
        double consumptionUtility;
        try {
            productionUtility = pf.overallUtility(sellingCommodityUtilities);
            consumptionUtility = cf.overallUtility(buyingCommodityUtilities);
        } catch (InvalidZUtilityError e) {
            logger.warning(this.toString());
            throw e;
        }
        try {
            w.write(productionUtility + ",");
            w.write(consumptionUtility + ",");
            double dispersionParameter = ((AggregateActivity) myProductionActivity).getLocationDispersionParameter();
            double sizeTerm = 1 / dispersionParameter * myProductionActivity.getSizeTermCoefficient() * Math.log(getAllocationSizeTerm());
            w.write(sizeTerm + ",");
            w.write(getLocationSpecificUtilityInclTaxes() + ",");
            w.write(productionUtility + consumptionUtility + getLocationSpecificUtilityInclTaxes() + sizeTerm + "\n");
        } catch (IOException e) {
            logger.severe("Error in writing commodityZUtilities");
            throw new RuntimeException(e);
        }
    }


    /**
     * Calculate a location disutility based on a different make and use table
     *
     * @param cf use table
     * @param pf make table
     */


    public double calcLocationUtility(ConsumptionFunction cf, ProductionFunction pf, double higherLevelDispersionParameter) throws OverflowException {
        // debug
        boolean debug = false;
/*        if (myProductionActivity.name.equals("StateLocalNonEd")) {
            int zoneUserNumber = myTaz.getZoneUserNumber();
            if (zoneUserNumber==1 || zoneUserNumber==104 || zoneUserNumber==2644) debug = true;
            debug = true;
        } */
        /* if (numdebug <100) {
             debug = true;
             numdebug++;
         } */
        return calcLocationUtilityDebug(cf, pf, debug, higherLevelDispersionParameter);
    }

    // debug June 6
    public double calcLocationUtilityDebug(ConsumptionFunction cf, ProductionFunction pf, boolean debug, double higherLevelDispersionParameter) throws OverflowException {
        lastConsumptionFunction = cf;
        lastProductionFunction = pf;
        
        if (debug) {
            // place to set a breakpoint
            lastConsumptionFunction=cf;
        }

        //TODO allow user to specify what to debug
        if (this.myProductionActivity.name.equals("WHOLESALE TRADE warehousing and transportation") && (myTaz.getZoneUserNumber()==874 || myTaz.getZoneUserNumber()==1394)) {
            debug = true;
        }
        double[] buyingCommodityUtilities = new double[cf.size()];
        for (int c = 0; c < cf.size(); c++) {
            AbstractCommodity com = cf.commodityAt(c);
            if (com == null) {
                buyingCommodityUtilities[c] = 0;
            } else {
                buyingCommodityUtilities[c] = cf.commodityAt(c).calcZUtility(getMyTaz(), false);
            }
        }
        double[] sellingCommodityUtilities = new double[pf.size()];
        for (int c = 0; c < pf.size(); c++) {
            AbstractCommodity com = pf.commodityAt(c);
            if (com == null) {
                sellingCommodityUtilities[c] = 0;
            } else {
                sellingCommodityUtilities[c] = pf.commodityAt(c).calcZUtility(getMyTaz(), true);
            }
        } //CUSellc,z and CUBuyc,z have now been calculated for the commodites made or used by the activity
        double productionUtility;
        double consumptionUtility;
        try {
            productionUtility = pf.overallUtility(sellingCommodityUtilities); //this is the value alphaProd*CUProda,z
            consumptionUtility = cf.overallUtility(buyingCommodityUtilities); //this is the value of alphaCons*CUConsa,z
        } catch (InvalidZUtilityError e) {
            logger.warning(this.toString());
            throw e;
        }
        if (debug) {
            logger.info(this + " production utility:" + pf.overallUtility(sellingCommodityUtilities) +
                    "  consumption utility:" + cf.overallUtility(buyingCommodityUtilities));
//            if (Double.isNaN(productionUtility)) {
            StringBuffer bob = new StringBuffer();
            bob.append("buying:");
            for (int c = 0; c < buyingCommodityUtilities.length; c++) bob.append(buyingCommodityUtilities[c] + " ");
            logger.info(bob.toString());
//            }
//            if (Double.isNaN(consumptionUtility)) {
            bob = new StringBuffer();
            bob.append("selling:");
            for (int c = 0; c < sellingCommodityUtilities.length; c++)
                bob.append(sellingCommodityUtilities[c] + " ");
            logger.info(bob.toString());
//            }
        }
        double noAltSpecificConstUtility = productionUtility +
                consumptionUtility;
        if (debug)
            logger.info("utility = " + noAltSpecificConstUtility + " + " + getLocationSpecificUtilityInclTaxes() +
                    " + " + myProductionActivity.getSizeTermCoefficient() * 1 / higherLevelDispersionParameter * Math.log(getAllocationSizeTerm()));
        return noAltSpecificConstUtility + getLocationSpecificUtilityInclTaxes() +
                1 / higherLevelDispersionParameter * myProductionActivity.getSizeTermCoefficient() * Math.log(getAllocationSizeTerm());
    }

    public double getUtility(double higherLevelDispersionParameter) throws ChoiceModelOverflowException {
        try {
            return calcLocationUtility(myProductionActivity.getConsumptionFunction(),
                    myProductionActivity.getProductionFunction(), higherLevelDispersionParameter);
        } catch (OverflowException e) {
            throw new ChoiceModelOverflowException(e.toString());
        }
        // message #1.2.3.1.1 to aggregateFactors:com.pb.despair.pi.AggregateActivity
        // ProductionFunction unnamed = aggregateFactors.getProductionFunction();
        // message #1.2.3.1.2 to aggregateFactors:com.pb.despair.pi.AggregateActivity
        // ConsumptionFunction unnamed = aggregateFactors.getConsumptionFunction();
        // message #1.2.3.1.3 to myTaz:com.pb.despair.pi.TAZ
        // double unnamed = myTaz.calcZUtility(com.pb.despair.pi.Commodity, boolean);
    }

/*    public double calcUtilityForPreferences(TravelPreferences tp, boolean withRouteChoice) {
        // message #1.2.3.1.1 to aggregateFactors:com.pb.despair.pi.AggregateActivity
        // ProductionFunction unnamed = aggregateFactors.getProductionFunction();
        // message #1.2.3.1.2 to aggregateFactors:com.pb.despair.pi.AggregateActivity
        // ConsumptionFunction unnamed = aggregateFactors.getConsumptionFunction();
        // message #1.2.3.1.3 to myTaz:com.pb.despair.pi.TAZ
        // double unnamed = myTaz.calcZUtilityForPreferences(com.pb.despair.pi.Commodity, boolean, TravelPreferences);
    } */

    //  public void setCommodityFlows() {
    //  }
    public void setCommoditiesBoughtAndSold() throws OverflowException {
        setCommoditiesBoughtAndSold(myProductionActivity.getConsumptionFunction(),
                myProductionActivity.getProductionFunction());
    }

    // void clearCommodityPurchaseMemory() {
    //     oldCommoditiesBought.clear();
    //     oldCommoditiesSold.clear();
    // }

    // so we can do remigration as well as migration, keep track of what we put where.
    //private Hashtable oldCommoditiesBought = new Hashtable();
    //private Hashtable oldCommoditiesSold = new Hashtable();

    public String toString() {
        return myProductionActivity + " in " + getMyTaz();
    };


    public void setAggregateQuantityWithErrorTracking(double amount, double derivative) throws OverflowException {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            logger.severe("amount in zone is NaN/Infinite " + this + " previous quantity:" + getQuantity() + " -- try less agressive step size...");
            logger.severe("following (between ***) is the utility calculation for this location");
            logger.severe("**********************************************");
            calcLocationUtilityDebug(lastConsumptionFunction, lastProductionFunction, true, ((AggregateActivity) myProductionActivity).getLocationDispersionParameter());
            logger.severe("**********************************************");
            throw new OverflowException("amount in zone is NaN/Infinite " + this + " previous quantity:" + getQuantity() + " -- try less agressive step size...");
        }
        setQuantity(amount);
        this.derivative = derivative;
        setCommoditiesBoughtAndSold();
    }


    public void setAggregateQuantity(double amount, double derivative) throws ChoiceModelOverflowException {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            logger.severe("amount in zone is NaN/Infinite " + this + " previous quantity:" + getQuantity() + " -- try less agressive step size...");
            logger.severe("following (between ***) is the utility calculation for this location");
            logger.severe("**********************************************");
            try {
                calcLocationUtilityDebug(lastConsumptionFunction, lastProductionFunction, true, ((AggregateActivity) myProductionActivity).getLocationDispersionParameter());
            } catch (OverflowException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            logger.severe("**********************************************");
            throw new Error("amount in zone is NaN/Infinite " + this + " previous quantity:" + getQuantity() + " -- try less agressive step size...");
        }
        setQuantity(amount);
        this.derivative = derivative;
        try {
            setCommoditiesBoughtAndSold();
        } catch (OverflowException e) {
            throw new ChoiceModelOverflowException(e.toString());
        }
    }

    public void setCommoditiesBoughtAndSold(ConsumptionFunction cf, ProductionFunction pf) throws OverflowException {
        // debug June 24 2002
        boolean debug = false;
        
        //TODO allow user to specify activiy-zone base debug logs.
        //if (myProductionActivity.name.equals("OTHER EDUCATION") && myTaz.getZoneUserNumber()>80 && myTaz.getZoneUserNumber()<90) debug = true;
        //if (myProductionActivity.name.equals("lt 10")) debug = true;
        if (getQuantity() == 0 && derivative == 0) return;
        
        // first get the relevent ZUtilities and calculate the buying and selling utility values
        double[] buyingCommodityUtilities = new double[cf.size()];
        CommodityZUtility[] buyingZUtilities = new CommodityZUtility[cf.size()];
        for (int c = 0; c < cf.size(); c++) {
            Commodity commodity = (Commodity) cf.commodityAt(c);
            if (commodity == null) buyingZUtilities[c] = null;
            else buyingZUtilities[c] = commodity.retrieveCommodityZUtility(getMyTaz(), false);
            if (commodity == null) buyingCommodityUtilities[c] = 0;
            else buyingCommodityUtilities[c] = commodity.calcZUtility(getMyTaz(), false);
        }
        double[] sellingCommodityUtilities = new double[pf.size()];
        CommodityZUtility[] sellingZUtilities = new CommodityZUtility[pf.size()];
        for (int c = 0; c < pf.size(); c++) {
            Commodity commodity = (Commodity) pf.commodityAt(c);
            if (commodity == null) sellingZUtilities[c] = null;
            else sellingZUtilities[c] = commodity.retrieveCommodityZUtility(getMyTaz(), true);
            if (commodity == null) sellingCommodityUtilities[c] = 0;
            else sellingCommodityUtilities[c] = commodity.calcZUtility(getMyTaz(), true);
        }
        // then figure out how much we want to buy and sell
        double[] buyingQuantities = cf.calcAmounts(buyingCommodityUtilities);
        double[] sellingQuantities = pf.calcAmounts(sellingCommodityUtilities);
        double[] buyingDerivatives = cf.amountsDerivatives(buyingCommodityUtilities);
        double[] sellingDerivatives = pf.amountsDerivatives(sellingCommodityUtilities);
        double[] buyingCompositeUtilityDerivatives = cf.overallUtilityDerivatives(buyingCommodityUtilities);
        double[] sellingCompositeUtilityDerivatives = pf.overallUtilityDerivatives(sellingCommodityUtilities);
        // then set the flows to move it through the network
        for (int c = 0; c < cf.size(); c++) {
            Commodity commodity = (Commodity) cf.commodityAt(c);
            if (commodity != null) {
                if (Double.isNaN(buyingQuantities[c]) || Double.isInfinite(buyingQuantities[c])) {
                    logger.severe("Error in consumption :" + myProductionActivity + " in " + myTaz + " consumes " + buyingQuantities[c] + " of " + commodity);
//                    throw new Error("Error: "+myProductionActivity+" in "+myTaz+" consumes "+buyingQuantities[c]+" of "+commodity);
                }

                // debug June 5 2002
                if (debug) {
                    logger.info("In zone " + myTaz.getZoneUserNumber() + " " + getQuantity() + " of " + myProductionActivity + " consumes " + buyingQuantities[c] * getQuantity() + " of " + commodity);
                }
                try {
                    if (getQuantity() != 0) {
                        buyingZUtilities[c].changeQuantityBy(getQuantity() * buyingQuantities[c]);
                    }
                    //TODO: check to see whether we should be changing the derivative or setting it.
                    if (derivative == 0) {
                        buyingZUtilities[c].changeDerivativeBy(getQuantity() * buyingDerivatives[c]);
                    } else if (getQuantity() == 0) {
                        buyingZUtilities[c].changeDerivativeBy(derivative * buyingCompositeUtilityDerivatives[c] * buyingQuantities[c]);
                    } else
                        buyingZUtilities[c].changeDerivativeBy(derivative * buyingCompositeUtilityDerivatives[c] * buyingQuantities[c] + getQuantity() * buyingDerivatives[c]);
                } catch (OverflowException e) {
                    logger.severe("Error: " + myProductionActivity + " in " + myTaz + " consumes " + buyingQuantities[c] + " of " + commodity + " leading to " + e);
                    e.printStackTrace();
                    throw new Error("Error: " + myProductionActivity + " in " + myTaz + " consumes " + buyingQuantities[c] + " of " + commodity + " leading to " + e);
                }

            }
        }
        for (int c = 0; c < pf.size(); c++) {
            Commodity commodity = (Commodity) pf.commodityAt(c);
            if (commodity != null) {
                if (Double.isNaN(sellingQuantities[c]) || Double.isInfinite(sellingQuantities[c])) {
                    logger.warning("Error in production: " + myProductionActivity + " in " + myTaz + " produces " + sellingQuantities[c] + " of " + commodity);
                }
                // debug June 5 2002
                if (debug) {
                    logger.info("In zone " + myTaz.getZoneUserNumber() + " " + getQuantity() + " of " + myProductionActivity + " produces " + sellingQuantities[c] * getQuantity() + " of " + commodity);
                }
                try {
                    if (getQuantity() != 0) {
                        sellingZUtilities[c].changeQuantityBy(getQuantity() * sellingQuantities[c]);
                    }
                    if (derivative == 0) {
                        sellingZUtilities[c].changeDerivativeBy(getQuantity() * sellingDerivatives[c]);
                    } else if (getQuantity() == 0) {
                        sellingZUtilities[c].changeDerivativeBy(derivative * sellingCompositeUtilityDerivatives[c] * sellingQuantities[c]);
                    } else
                    //TODO: check to see whether we should be changing the derivative or setting it.
                        sellingZUtilities[c].changeDerivativeBy(derivative * sellingCompositeUtilityDerivatives[c] * sellingQuantities[c] + getQuantity() * sellingDerivatives[c]);
                } catch (OverflowException e) {
                    logger.severe("Overflow Error: " + myProductionActivity + " in " + myTaz + " produces " + buyingQuantities[c] + " of " + commodity + " leading to " + e);
                    throw new Error("Error: " + myProductionActivity + " in " + myTaz + " produces " + buyingQuantities[c] + " of " + commodity + " leading to " + e);
                }

            }
        }
    }
}

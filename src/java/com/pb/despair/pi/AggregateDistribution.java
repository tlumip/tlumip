package com.pb.despair.pi;

import com.pb.despair.model.*;

import drasys.or.matrix.MatrixI;
import drasys.or.matrix.VectorI;

//import drasys.or.linear.algebra.Algebra;
//import drasys.or.linear.algebra.AlgebraException;
//import drasys.or.matrix.DenseMatrix;
//import drasys.or.matrix.DenseVector;
//import drasys.or.matrix.MatrixI;
//import drasys.or.matrix.SparseMatrix;
//import drasys.or.matrix.VectorI;

import java.io.IOException;
import java.io.Writer;

// backing up to ORObjects instead of MTJ because of MTJ Bugs
//import mt.DiagMatrix;
import org.apache.log4j.Logger;

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
    private double[] buyingCommodityUtilities;
    private CommodityZUtility[] buyingZUtilities;
    private CommodityZUtility[] sellingZUtilities;
    private double[] sellingCommodityUtilities;

    public AggregateDistribution(ProductionActivity p, AbstractTAZ t) {
        super(p, t);
    }
    
    private void initializeZUtilities() {
        buyingCommodityUtilities = new double[lastConsumptionFunction.size()];
        sellingCommodityUtilities = new double[lastProductionFunction.size()];
        buyingZUtilities = new CommodityZUtility[lastConsumptionFunction.size()];
        sellingZUtilities = new CommodityZUtility[lastProductionFunction.size()];
        for (int c = 0; c < lastConsumptionFunction.size(); c++) {
            Commodity com = (Commodity) lastConsumptionFunction.commodityAt(c);
            if (com == null) buyingZUtilities[c] = null;
            else buyingZUtilities[c] = com.retrieveCommodityZUtility(getMyTaz(), false);
            if (com == null) {
                buyingCommodityUtilities[c] = 0;
            } else {
                try {
                    buyingCommodityUtilities[c] = com.calcZUtility(getMyTaz(), false);
                } catch (OverflowException e1) {
                    buyingCommodityUtilities[c] = Double.NaN;
                }
            }
        }
        for (int c = 0; c < lastProductionFunction.size(); c++) {
            Commodity com = (Commodity) lastProductionFunction.commodityAt(c);
            if (com == null) sellingZUtilities[c] = null;
            else sellingZUtilities[c] = com.retrieveCommodityZUtility(getMyTaz(), true);
            if (com == null) {
                sellingCommodityUtilities[c] = 0;
            } else {
                try {
                    sellingCommodityUtilities[c] = com.calcZUtility(getMyTaz(), true);
                } catch (OverflowException e1) {
                    sellingCommodityUtilities[c] = Double.NaN;
                }
            }
        }
    }



    /**
     * Method updateLocationUtilityTerms.
     */
    public void updateLocationUtilityTerms(Writer w) {
        ConsumptionFunction cf = lastConsumptionFunction;
        ProductionFunction pf = lastProductionFunction;
        
        initializeZUtilities();
        
        double productionUtility;
        double consumptionUtility;

        try {
            productionUtility = pf.overallUtility(sellingCommodityUtilities);
            consumptionUtility = cf.overallUtility(buyingCommodityUtilities);
        } catch (InvalidZUtilityError e) {
            logger.error(this.toString());
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
            logger.fatal("Error in writing commodityZUtilities");
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
        
        initializeZUtilities();

        double productionUtility;
        double consumptionUtility;
        try {
            productionUtility = pf.overallUtility(sellingCommodityUtilities); //this is the value alphaProd*CUProda,z
            consumptionUtility = cf.overallUtility(buyingCommodityUtilities); //this is the value of alphaCons*CUConsa,z
        } catch (InvalidZUtilityError e) {
            logger.fatal(this.toString());
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
            throw new ChoiceModelOverflowException(e.toString());}
    }

    public void setCommoditiesBoughtAndSold() throws OverflowException {
        setCommoditiesBoughtAndSold(myProductionActivity.getConsumptionFunction(),
                myProductionActivity.getProductionFunction());
    }


    public String toString() {
        return myProductionActivity + " in " + getMyTaz();
    };


    public void setAggregateQuantityWithErrorTracking(double amount, double derivative) throws OverflowException {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            logger.fatal("amount in zone is NaN/Infinite " + this + " previous quantity:" + getQuantity() + " -- try less agressive step size...");
            logger.fatal("following (between ***) is the utility calculation for this location");
            logger.fatal("**********************************************");
            calcLocationUtilityDebug(lastConsumptionFunction, lastProductionFunction, true, ((AggregateActivity) myProductionActivity).getLocationDispersionParameter());
            logger.fatal("**********************************************");
            throw new OverflowException("amount in zone is NaN/Infinite " + this + " previous quantity:" + getQuantity() + " -- try less agressive step size...");
        }
        setQuantity(amount);
        this.derivative = derivative;
        setCommoditiesBoughtAndSold();
    }


    public void setAggregateQuantity(double amount, double derivative) throws ChoiceModelOverflowException {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            logger.fatal("amount in zone is NaN/Infinite " + this + " previous quantity:" + getQuantity() + " -- try less agressive step size...");
            logger.fatal("following (between ***) is the utility calculation for this location");
            logger.fatal("**********************************************");
            try {
                calcLocationUtilityDebug(lastConsumptionFunction, lastProductionFunction, true, ((AggregateActivity) myProductionActivity).getLocationDispersionParameter());
            } catch (OverflowException e) {
                e.printStackTrace();
            }
            logger.fatal("**********************************************");
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
        
        lastConsumptionFunction = cf;
        lastProductionFunction = pf;
        
        // first get the relevent ZUtilities and calculate the buying and selling utility values
        initializeZUtilities();

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
                    logger.fatal("Error in consumption :" + myProductionActivity + " in " + myTaz + " consumes " + buyingQuantities[c] + " of " + commodity);
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
                    if (derivative == 0) {
                        buyingZUtilities[c].changeDerivativeBy(getQuantity() * buyingDerivatives[c]);
                    } else if (getQuantity() == 0) {
                        buyingZUtilities[c].changeDerivativeBy(derivative * buyingCompositeUtilityDerivatives[c] * buyingQuantities[c]);
                    } else
                        buyingZUtilities[c].changeDerivativeBy(derivative * buyingCompositeUtilityDerivatives[c] * buyingQuantities[c] + getQuantity() * buyingDerivatives[c]);
                } catch (OverflowException e) {
                    logger.fatal("Error: " + myProductionActivity + " in " + myTaz + " consumes " + buyingQuantities[c] + " of " + commodity + " leading to " + e);
                    e.printStackTrace();
                    throw new Error("Error: " + myProductionActivity + " in " + myTaz + " consumes " + buyingQuantities[c] + " of " + commodity + " leading to " + e);
                }

            }
        }
        for (int c = 0; c < pf.size(); c++) {
            Commodity commodity = (Commodity) pf.commodityAt(c);
            if (commodity != null) {
                if (Double.isNaN(sellingQuantities[c]) || Double.isInfinite(sellingQuantities[c])) {
                    logger.fatal("Error in production: " + myProductionActivity + " in " + myTaz + " produces " + sellingQuantities[c] + " of " + commodity);
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
                        sellingZUtilities[c].changeDerivativeBy(derivative * sellingCompositeUtilityDerivatives[c] * sellingQuantities[c] + getQuantity() * sellingDerivatives[c]);
                } catch (OverflowException e) {
                    logger.fatal("Overflow Error: " + myProductionActivity + " in " + myTaz + " produces " + buyingQuantities[c] + " of " + commodity + " leading to " + e);
                    throw new Error("Error: " + myProductionActivity + " in " + myTaz + " produces " + buyingQuantities[c] + " of " + commodity + " leading to " + e);
                }

            }
        }
    }
    /**
     * @param averagePriceSurplusMatrix
     */
    private void allocateLocationChoiceAveragePriceDerivatives(double totalActivityQuantity, MatrixI averagePriceSurplusMatrix, VectorI locationChoiceDerivatives) throws OverflowException {
// this was the way with MTJ
//        private void allocateLocationChoiceAveragePriceDerivatives(double totalActivityQuantity, mt.Matrix averagePriceSurplusMatrix, mt.Vector locationChoiceDerivatives) throws OverflowException {
        
        ConsumptionFunction cf = lastConsumptionFunction;
        ProductionFunction pf = lastProductionFunction;
        
        initializeZUtilities();

        // then figure out how much we want to buy and sell
        double[] buyingQuantities = cf.calcAmounts(buyingCommodityUtilities);
        double[] sellingQuantities = pf.calcAmounts(sellingCommodityUtilities);

        //now multiply it by the location choice derivatives
        for (int c = 0; c < cf.size(); c++) {
            Commodity commodity = (Commodity) cf.commodityAt(c);
            if (commodity != null) {
                double buyingQuantity = -totalActivityQuantity*buyingQuantities[c];
                for (int c1=0;c1<locationChoiceDerivatives.size();c1++) {
                    // this is if the price of one commodity goes up the quantity of activity here will go
                    // down a bit, and hence to quantity bought/sold will also go down.
                    averagePriceSurplusMatrix.setElementAt(c,c1,
                            averagePriceSurplusMatrix.elementAt(c,c1)+buyingQuantity*locationChoiceDerivatives.elementAt(c1));
                }
            }
        }
        for (int c = 0; c < pf.size(); c++) {
            Commodity commodity = (Commodity) pf.commodityAt(c);
            if (commodity != null) {
                double sellingQuantity = totalActivityQuantity*sellingQuantities[c];
                for (int c1=0;c1<locationChoiceDerivatives.size();c1++) {
                    averagePriceSurplusMatrix.setElementAt(c,c1,
                    averagePriceSurplusMatrix.elementAt(c,c1)+sellingQuantity*locationChoiceDerivatives.elementAt(c1));
                }
           }
        }
    }

    

//    public void allocateLocationChoiceDerivatives(double totalActivityQuantity, MatrixI thePlaceToAddTheDerivatives, VectorI locationChoiceDerivatives, HashMap exchangeIndices) throws OverflowException {
//        
//        ConsumptionFunction cf = lastConsumptionFunction;
//        ProductionFunction pf = lastProductionFunction;
//        
//        initializeZUtilities();
//
//        // then figure out how much we want to buy and sell
//        double[] buyingQuantities = cf.calcAmounts(buyingCommodityUtilities);
//        double[] sellingQuantities = pf.calcAmounts(sellingCommodityUtilities);
//
//        // need these for later.
//        DenseMatrix locationDerivatives = new DenseMatrix(1,locationChoiceDerivatives.size());
//        locationDerivatives.setRow(0,locationChoiceDerivatives);
//        Algebra a = new Algebra();
//        
//        // then figure out the flows to move it through the network
//        for (int c = 0; c < cf.size(); c++) {
//            Commodity commodity = (Commodity) cf.commodityAt(c);
//            if (commodity != null) {
//                if (Double.isNaN(buyingQuantities[c]) || Double.isInfinite(buyingQuantities[c])) {
//                    logger.fatal("Error in consumption :" + myProductionActivity + " in " + myTaz + " consumes " + buyingQuantities[c] + " of " + commodity);
////                    throw new Error("Error: "+myProductionActivity+" in "+myTaz+" consumes "+buyingQuantities[c]+" of "+commodity);
//                }
//                DenseMatrix locationCausedDerivatives=null;
//                try {
//                    double buyingQuantity = -totalActivityQuantity*buyingQuantities[c];
//                    double[] buyingExchangeQuantities = buyingZUtilities[c].getExchangeProbabilities();
//                    for (int z=0;z<buyingExchangeQuantities.length;z++) {
//                        buyingExchangeQuantities[z]*=buyingQuantity;
//                    }
//                    locationCausedDerivatives = new DenseMatrix(buyingExchangeQuantities.length,1);
//                    locationCausedDerivatives.setColumn(0,new DenseVector(buyingExchangeQuantities));
//                    locationCausedDerivatives = a.multiply(locationCausedDerivatives,locationDerivatives);
//                            
//                } catch (AlgebraException e) {
//                    logger.fatal("problem trying to figure out derivatives due to location changes." + e);
//                    e.printStackTrace();
//                }
//                
//                // now add it to the big matrix in the right spot
//                Iterator exIt = commodity.getAllExchanges().iterator();
//                int[] indices = new int[locationCausedDerivatives.sizeOfRows()];
//                int index = 0;
//                while (exIt.hasNext()) {
//                   Exchange x = (Exchange) exIt.next();
//                   int globalIndex = ((Integer) exchangeIndices.get(x)).intValue();
//                   indices[index]=globalIndex;
//                }
//                for (int row =0;row<indices.length;row++) {
//                    for (int col=0;col<indices.length;col++) {
//                        thePlaceToAddTheDerivatives.setElementAt(indices[row],indices[col],
//                                thePlaceToAddTheDerivatives.elementAt(indices[row],indices[col])+
//                                     locationCausedDerivatives.elementAt(row,col));
//                    }
//                }
//            }
//        }
//        for (int c = 0; c < pf.size(); c++) {
//            Commodity commodity = (Commodity) pf.commodityAt(c);
//            if (commodity != null) {
//                if (Double.isNaN(sellingQuantities[c]) || Double.isInfinite(sellingQuantities[c])) {
//                    logger.warning("Error in production: " + myProductionActivity + " in " + myTaz + " produces " + sellingQuantities[c] + " of " + commodity);
//                }
//                DenseMatrix locationCausedDerivatives=null;
//                try {
//                    double sellingQuantity = totalActivityQuantity*sellingQuantities[c];
//                    double[] sellingExchangeQuantities = sellingZUtilities[c].getExchangeProbabilities();
//                    for (int z=0;z<sellingExchangeQuantities.length;z++) {
//                        sellingExchangeQuantities[z]*=sellingQuantity;
//                    }
//                    locationCausedDerivatives = new DenseMatrix(sellingExchangeQuantities.length,1);
//                    locationCausedDerivatives.setColumn(0,new DenseVector(sellingExchangeQuantities));
//                    locationCausedDerivatives = a.multiply(locationCausedDerivatives,locationDerivatives);
//               } catch (AlgebraException e) {
//                   logger.fatal("problem trying to figure out derivatives due to location changes." + e);
//                   e.printStackTrace();
//               }
//                   
//               
//               // now add it to the big matrix in the right spot
//               Iterator exIt = commodity.getAllExchanges().iterator();
//               int[] indices = new int[locationCausedDerivatives.sizeOfRows()];
//               int index = 0;
//               while (exIt.hasNext()) {
//                   Exchange x = (Exchange) exIt.next();
//                   int globalIndex = ((Integer) exchangeIndices.get(x)).intValue();
//                   indices[index]=globalIndex;
//               }
//               for (int row =0;row<indices.length;row++) {
//                   for (int col=0;col<indices.length;col++) {
//                       thePlaceToAddTheDerivatives.setElementAt(indices[row],indices[col],
//                               thePlaceToAddTheDerivatives.elementAt(indices[row],indices[col])+
//                               locationCausedDerivatives.elementAt(row,col));
//                   }
//               }
//              }
//        }
//    }
//
//    
//
//    /**
//     * @param firstDerivatives
//     * @param locationByPrice
//     * @param exchangeNumbering
//     */
//    public void addTwoComponentsOfDerivativesToMatrix(double activityAmount, MatrixI firstDerivatives, VectorI thisLocationByPrices, HashMap exchangeNumbering) {
//        if (lastConsumptionFunction == null) lastConsumptionFunction = myProductionActivity.getConsumptionFunction();
//        if (lastProductionFunction == null) lastProductionFunction = myProductionActivity.getProductionFunction();
//        try {
//            allocateLocationChoiceDerivatives(activityAmount, firstDerivatives, thisLocationByPrices, exchangeNumbering);
//        } catch (OverflowException e) {
//            e.printStackTrace();
//            throw new RuntimeException(e);
//        }
//        allocateProductionChoiceDerivatives(firstDerivatives,exchangeNumbering);
//    }
//


    /**
     * @param averagePriceSurplusMatrix
     * @param thisLocationByPrices
     */
    public void addTwoComponentsOfDerivativesToAveragePriceMatrix(double activityAmount, MatrixI averagePriceSurplusMatrix, VectorI thisLocationByPrices) {
        if (lastConsumptionFunction == null) lastConsumptionFunction = myProductionActivity.getConsumptionFunction();
        if (lastProductionFunction == null) lastProductionFunction = myProductionActivity.getProductionFunction();
         //moving activity around to places where production functinos are different
        try {
           allocateLocationChoiceAveragePriceDerivatives(activityAmount, averagePriceSurplusMatrix, thisLocationByPrices);
        } catch (OverflowException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        allocateProductionChoiceAveragePriceDerivatives(averagePriceSurplusMatrix);
    }
    
    /**
     * @param averagePriceSurplusMatrix
     */
    private void allocateProductionChoiceAveragePriceDerivatives(MatrixI averagePriceSurplusMatrix) {
        ConsumptionFunction cf = lastConsumptionFunction;
        ProductionFunction pf = lastProductionFunction;
        
        initializeZUtilities();
        
        
        // TODO make it work with other production and consumption functions
        if (! (cf instanceof LogitSubstitution)) throw new RuntimeException("full derivative calculations can only be used with LogitSubstitution right now");
        if (! (pf instanceof LogitSubstitution)) throw new RuntimeException("full derivative calculations can only be used with LogitSubstitution right now");
        LogitSubstitution cfl = (LogitSubstitution) cf;
        LogitSubstitution pfl = (LogitSubstitution) pf;
        
        // using DiagMatrix is too slow -- use double[] instead
        //DiagMatrix dSellingUtilitiesByDPrices = new DiagMatrix(pf.size());
        //DiagMatrix dBuyingUtilitiesByDPrices = new DiagMatrix(cf.size());
        
        double[] sellingPriceCoefficients = new double[pf.size()];
        double[] buyingPriceCoefficients = new double[cf.size()];
        
        
//        double[][] dSellingUtilitiesByDPrices = new double[pf.size()][firstDerivatives.sizeOfColumns()];
//        double[][] dBuyingUtilitiesByDPrices = new double[cf.size()][firstDerivatives.sizeOfColumns()];
        
        for (int c = 0; c < pf.size(); c++) {
            Commodity commodity = (Commodity) pf.commodityAt(c);
            if (commodity != null) {
                // this was this DiagMatrix
                //dSellingUtilitiesByDPrices.set(c,c,commodity.getSellingUtilityPriceCoefficient());
                sellingPriceCoefficients[c]=commodity.getSellingUtilityPriceCoefficient();
            }
        }
        
        for (int c = 0; c < cf.size(); c++) {
            Commodity commodity = (Commodity) cf.commodityAt(c);
            if (commodity != null) {
                // this was with DiagMatrix
                //dBuyingUtilitiesByDPrices.set(c,c,commodity.getBuyingUtilityPriceCoefficient());
                buyingPriceCoefficients[c] = commodity.getBuyingUtilityPriceCoefficient();
            }
        }
        // this is too slow too, use double[][] instead.
        //mt.DenseMatrix dSellingProductionByDPrices = new mt.DenseMatrix(pf.size(),pf.size());
        // mt.DenseMatrix productionDerivatives = new mt.DenseMatrix(pfl.productionUtilitiesDerivatives(sellingCommodityUtilities)); //0.94% of time here
        double[][] productionDerivatives = pfl.productionUtilitiesDerivatives(sellingCommodityUtilities);
        // multiplying by a diagonal is very slow in mtj; not (by default) smart enough to do it quickly.  
        // so instead we transpose, and premultiply, then transpose again.
        // but that's still too slow, so just do it in double[][] manually
        //dSellingUtilitiesByDPrices.mult(getQuantity(),productionDerivatives.transpose(),dSellingProductionByDPrices); //3.33 % of time here (0.25% of time with full commodity derivs)
        //dSellingProductionByDPrices.transpose();// 1.48% of time here

        // this is the original way
        //productionDerivatives.mult(getQuantity(),dSellingUtilitiesByDPrices,dSellingProductionByDPrices);
        double quantity = getQuantity();
        for (int row=0;row<productionDerivatives.length;row++) {
            for (int col=0;col<productionDerivatives[row].length;col++) {
                averagePriceSurplusMatrix.setElementAt(row,col,
                 averagePriceSurplusMatrix.elementAt(row,col)+productionDerivatives[row][col]*(quantity*sellingPriceCoefficients[col]));
            }
        }
        
        //mt.DenseMatrix dBuyingProductionByDPrices = new mt.DenseMatrix(cf.size(),cf.size());
        //mt.DenseMatrix consumptionDerivatives = new mt.DenseMatrix(cfl.productionUtilitiesDerivatives(buyingCommodityUtilities)); // 1.2% here
        double[][] consumptionDerivatives = cfl.productionUtilitiesDerivatives(buyingCommodityUtilities);
        
        // fancy way with transpose
        //dSellingUtilitiesByDPrices.mult(getQuantity(),consumptionDerivatives.transpose(),dBuyingProductionByDPrices); //3.27% of time here // 0.25% of time here
        //dBuyingProductionByDPrices.transpose(); // 1.5% of time here
        // original way
        //consumptionDerivatives.mult(getQuantity(),dBuyingUtilitiesByDPrices,dBuyingProductionByDPrices);
        for (int row=0;row<consumptionDerivatives.length;row++) {
            for (int col=0;col<consumptionDerivatives[row].length;col++) {
                averagePriceSurplusMatrix.setElementAt(row,col,
                averagePriceSurplusMatrix.elementAt(row,col)-consumptionDerivatives[row][col]*quantity*buyingPriceCoefficients[col]); 
            }
        }

        // put stuff in right place
        //averagePriceSurplusMatrix.add(dSellingProductionByDPrices);//1.3% of time here
        //averagePriceSurplusMatrix.add(dBuyingProductionByDPrices);
    }

//    /**
//     * @param firstDerivatives
//     * @param exchangeNumbering
//     */
//    private void allocateProductionChoiceDerivatives(MatrixI firstDerivatives, HashMap exchangeNumbering) {
//        ConsumptionFunction cf = lastConsumptionFunction;
//        ProductionFunction pf = lastProductionFunction;
//              
//        initializeZUtilities();
//        
//        
//        // TODO make it work with other production and consumption functions
//        if (! (cf instanceof LogitSubstitution)) throw new RuntimeException("full derivative calculations can only be used with LogitSubstitution right now");
//        if (! (pf instanceof LogitSubstitution)) throw new RuntimeException("full derivative calculations can only be used with LogitSubstitution right now");
//        LogitSubstitution cfl = (LogitSubstitution) cf;
//        LogitSubstitution pfl = (LogitSubstitution) pf;
//        
//        double[][] dSellingUtilitiesByDPrices = new double[pf.size()][firstDerivatives.sizeOfColumns()];
//        double[][] dBuyingUtilitiesByDPrices = new double[cf.size()][firstDerivatives.sizeOfColumns()];
//        
//        for (int c = 0; c < pf.size(); c++) {
//            Commodity commodity = (Commodity) pf.commodityAt(c);
//            if (commodity != null) {
//                double[] oneCommoditiesOverallUtilityDerivatives = sellingZUtilities[c].myFlows.getLogsumDerivativesWRTPrices(); 
//                Iterator exIt = commodity.getAllExchanges().iterator();
//                int exchangeIndex = 0;
//                while (exIt.hasNext()) {
//                    Exchange x = (Exchange) exIt.next();
//                    int index = ((Integer) exchangeNumbering.get(x)).intValue();
//                    dSellingUtilitiesByDPrices[c][index] = oneCommoditiesOverallUtilityDerivatives[exchangeIndex];
//                    exchangeIndex++;
//                }
//            }
//        }
//        
//        for (int c = 0; c < cf.size(); c++) {
//            Commodity commodity = (Commodity) cf.commodityAt(c);
//            if (commodity != null) {
//                double[] oneCommoditiesOverallUtilityDerivatives = buyingZUtilities[c].myFlows.getLogsumDerivativesWRTPrices(); 
//                Iterator exIt = commodity.getAllExchanges().iterator();
//                int exchangeIndex = 0;
//                while (exIt.hasNext()) {
//                    Exchange x = (Exchange) exIt.next();
//                    int index = ((Integer) exchangeNumbering.get(x)).intValue();
//                    dBuyingUtilitiesByDPrices[c][index] = oneCommoditiesOverallUtilityDerivatives[exchangeIndex];
//                    exchangeIndex++;
//                }
//            }
//        }
//        
//        MatrixI dSellingProductionByDPrices = new DenseMatrix(pfl.probabilitiesFullDerivatives(sellingCommodityUtilities,dSellingUtilitiesByDPrices));
//        MatrixI dBuyingConsumptionByDPrices = new DenseMatrix(cfl.probabilitiesFullDerivatives(buyingCommodityUtilities,dBuyingUtilitiesByDPrices));
//        
//        SparseMatrix diagonalMatrix = new SparseMatrix(firstDerivatives.sizeOfColumns(),firstDerivatives.sizeOfColumns());
//        DenseVector dv = new DenseVector(firstDerivatives.sizeOfColumns());
//        diagonalMatrix.setDiagonal(dv);
//        
//        diagonalMatrix.setElements(this.getQuantity());
//        Algebra a = new Algebra();
//        try {
//            dSellingProductionByDPrices = a.multiply(dSellingProductionByDPrices,diagonalMatrix);
//            diagonalMatrix.setElements(-this.getQuantity());
//            dBuyingConsumptionByDPrices = a.multiply(dBuyingConsumptionByDPrices,diagonalMatrix);
//        } catch (AlgebraException e) {
//            e.printStackTrace();
//            throw new RuntimeException(e);
//        }
//
//        //allocate amount across exchanges (each row in matrix gets expanded to a z rows
//        for (int c = 0; c < pf.size(); c++) {
//            Commodity commodity = (Commodity) pf.commodityAt(c);
//            if (commodity != null) {
//                double[] allocationProbabilities = sellingZUtilities[c].myFlows.getChoiceProbabilities();
//                for (int priceIndex=0;priceIndex<firstDerivatives.sizeOfColumns();priceIndex++) {
//                    Iterator exIt = commodity.getAllExchanges().iterator();
//                    int localNumber = 0;
//                    while (exIt.hasNext()) {
//                        Exchange x = (Exchange) exIt.next();
//                        int globalNumber = ((Integer) exchangeNumbering.get(x)).intValue();
//                        firstDerivatives.setElementAt(globalNumber,priceIndex,
//                                firstDerivatives.elementAt(globalNumber,priceIndex)+
//                                   dSellingProductionByDPrices.elementAt(c,priceIndex)*
//                                    allocationProbabilities[localNumber]);
//                        localNumber++;
//                    }
//                }
//            }
//        }
//                
//        for (int c = 0; c < cf.size(); c++) {
//            Commodity commodity = (Commodity) cf.commodityAt(c);
//            if (commodity != null) {
//                double[] allocationProbabilities = buyingZUtilities[c].myFlows.getChoiceProbabilities();
//                for (int priceIndex=0;priceIndex<firstDerivatives.sizeOfColumns();priceIndex++) {
//                    Iterator exIt = commodity.getAllExchanges().iterator();
//                    int localNumber = 0;
//                    while (exIt.hasNext()) {
//                        Exchange x = (Exchange) exIt.next();
//                        int globalNumber = ((Integer) exchangeNumbering.get(x)).intValue();
//                        firstDerivatives.setElementAt(globalNumber,priceIndex,
//                                firstDerivatives.elementAt(globalNumber,priceIndex)+
//                                dBuyingConsumptionByDPrices.elementAt(c,priceIndex)*
//                                allocationProbabilities[localNumber]);
//                        localNumber++;
//                    }
//                }
//            }
//        }
//    }
//


//    /**
//     * @param exchangeNumbering
//     * @return
//     */
//    public DenseVector calculateLocationUtilityDerivatives(HashMap exchangeNumbering) {
//        ConsumptionFunction cf = lastConsumptionFunction;
//        ProductionFunction pf = lastProductionFunction;
//        Algebra a = new Algebra();
//        try {
//        
//            initializeZUtilities();
//            double[] buyingCompositeUtilityDerivatives = cf.overallUtilityDerivatives(buyingCommodityUtilities);
//            double[] sellingCompositeUtilityDerivatives = pf.overallUtilityDerivatives(sellingCommodityUtilities);
//            DenseVector buying= new DenseVector(buyingCompositeUtilityDerivatives);
//            DenseVector selling =new DenseVector(sellingCompositeUtilityDerivatives);
//            
//            MatrixI buyingZUtilitiesWRTPrices = getBuyingZUtilitiesWRTPrices(exchangeNumbering);
//            MatrixI sellingZUtilitiesWRTPrices = getSellingZUtilitiesWRTPrices(exchangeNumbering);
//            
//            VectorI buyingOverallUtilityWRTPrices = a.multiply(buying,buyingZUtilitiesWRTPrices);
//            VectorI sellingOverallUtilityWRTPrices = a.multiply(selling,sellingZUtilitiesWRTPrices);
//            
//            return a.add(buyingOverallUtilityWRTPrices,sellingOverallUtilityWRTPrices);
//        } catch (AlgebraException e) {
//            e.printStackTrace();
//            throw new RuntimeException(e);
//        }
//        
//        
//    }
//
//    /**
//     * @param exchangeNumbering
//     * @return
//     */
//    private MatrixI getBuyingZUtilitiesWRTPrices(HashMap exchangeNumbering) {
//        DenseMatrix derivatives = new DenseMatrix(buyingZUtilities.length,exchangeNumbering.size());
//        for (int c=0;c<buyingZUtilities.length;c++) {
//            if (buyingZUtilities[c]!=null) {
//                double[] logsumDerivatives = buyingZUtilities[c].myFlows.getLogsumDerivativesWRTPrices();
//                Commodity com = buyingZUtilities[c].myCommodity;
//                Iterator exIt = com.getAllExchanges().iterator();
//                int localLocation = 0;
//                while (exIt.hasNext()) {
//                    Exchange x = (Exchange) exIt.next();
//                    int globalLocation = ((Integer) exchangeNumbering.get(x)).intValue();
//                    derivatives.setElementAt(c,globalLocation,logsumDerivatives[localLocation]);
//                    localLocation++;
//                }
//            }
//        }
//        return derivatives;
//    }
//
//    /**
//     * @param exchangeNumbering
//     * @return
//     */
//    private MatrixI getSellingZUtilitiesWRTPrices(HashMap exchangeNumbering) {
//        DenseMatrix derivatives = new DenseMatrix(sellingZUtilities.length,exchangeNumbering.size());
//        for (int c=0;c<sellingZUtilities.length;c++) {
//            if (sellingZUtilities[c]!=null) {
//                double[] logsumDerivatives = sellingZUtilities[c].myFlows.getLogsumDerivativesWRTPrices();
//                Commodity com = sellingZUtilities[c].myCommodity;
//                Iterator exIt = com.getAllExchanges().iterator();
//                int localLocation = 0;
//                while (exIt.hasNext()) {
//                    Exchange x = (Exchange) exIt.next();
//                    int globalLocation = ((Integer) exchangeNumbering.get(x)).intValue();
//                    derivatives.setElementAt(c,globalLocation,logsumDerivatives[localLocation]);
//                    localLocation++;
//                }
//            }
//        }
//        return derivatives;
//    }

    /**
     * @return
     */
    public double[] calculateLocationUtilityWRTAveragePrices() {
        ConsumptionFunction cf = lastConsumptionFunction;
        ProductionFunction pf = lastProductionFunction;
        initializeZUtilities();
        double[] buyingCompositeUtilityDerivatives = cf.overallUtilityDerivatives(buyingCommodityUtilities);
        double[] sellingCompositeUtilityDerivatives = pf.overallUtilityDerivatives(sellingCommodityUtilities);
        for(int i=0;i<buyingCompositeUtilityDerivatives.length;i++) {
            Commodity c = (Commodity) cf.commodityAt(i);
            double buyingPriceCoefficient = 0;
            if (c!=null) buyingPriceCoefficient = c.getBuyingUtilityPriceCoefficient();
            double sellingPriceCoefficient = 0;
            c = (Commodity) pf.commodityAt(i);
            if (c!=null) sellingPriceCoefficient = c.getSellingUtilityPriceCoefficient();
            buyingCompositeUtilityDerivatives[i] =
            buyingCompositeUtilityDerivatives[i] * buyingPriceCoefficient +
            sellingCompositeUtilityDerivatives[i] * sellingPriceCoefficient;
        }
        return buyingCompositeUtilityDerivatives;
    }
}

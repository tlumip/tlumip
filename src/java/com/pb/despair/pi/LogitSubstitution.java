package com.pb.despair.pi;

import com.pb.despair.model.AbstractCommodity;
import com.pb.despair.model.ConsumptionFunction;
import com.pb.despair.model.ProductionFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * This class calculates the overall utility of producing or
 * consuming given the ZUtilities of individual commodities.
 *
 * @author John Abraham
 */

public class LogitSubstitution implements ConsumptionFunction, ProductionFunction {

    private static Logger logger = Logger.getLogger("com.pb.despair.pi");

//    double denominator;  // for interim calculation results
    private double lambda;
    private double scaling;
    private double utilityOfNonModelledAlternative;
    boolean nonModelledDenominatorTerm = false;

    /**
     * @link aggregation
     */
    private ArrayList myCommodities = new ArrayList();
    private ArrayList sortedQuantitiesToUse = null;

    public static class Quantity {
        final public Commodity com;
        final public double minimum;
        final public double discretionary;
        final public double utilityScale;
        final public double utilityOffset;

        public String toString() {
            return (com.toString() + " " + minimum + "+" + discretionary + "* exp(lambda * " + discretionary + " *(" + utilityScale + "*u+" +
                    utilityOffset + "))/Sum(exp(lambda*discretionary*(scale*U+offset))))");
        }



        public Quantity(Commodity com, double minimum, double discretionary, double utilityScale,
                        double utilityOffset) {
            this.com = com;
            this.minimum = minimum;
            this.discretionary = discretionary;
            this.utilityScale = utilityScale;
            this.utilityOffset = utilityOffset;
        }
    }

    // count of debugging printouts
    int overallUtilityCalcPrint = -1;
    int amountsCalcPrint = -1;

    private double[][] utilityDerivatives;

    public LogitSubstitution(double scaling, double lambda) {
        this.lambda = lambda;
        this.scaling = scaling;
    }

    public double overallUtility(double[] individualCommodityUtilities) {
        //this method calculates the CUProda,z or CUConsa,z (later comments refer to the CUProda,z calc.)
        synchronized (this) {
            // synchronized so that we can rely on the denominator calculation
            // double[] amount = calcAmounts(individualCommodityUtilities);
            double utility = 0;
            boolean foundOne = false;
            double denominator = 0;
            for (int c = 0; c < sortedQuantitiesToUse.size(); c++) {
                Quantity q = (Quantity) sortedQuantitiesToUse.get(c);
                if (q != null) {
                    if (q.minimum != 0) {
                        //this is the sum of (MMinc,a * CUSellc,a,z) over c = sum(MMinc,a * (CUSellc,z * adj. factor + USellRefc,a)) over c
                        utility += q.minimum * (individualCommodityUtilities[c] * q.utilityScale + q.utilityOffset);
                    }
                    if (q.discretionary != 0) {
                        foundOne = true;
                        // this  is sum (e^(lambda*MDiscc,a*CUSellc,a,z)) over c = sum(e^(lambda*UIProdc,a,z))over c
                        denominator += Math.exp(lambda * q.discretionary * (individualCommodityUtilities[c] * q.utilityScale + q.utilityOffset));
                    }
                }
            }
            if (nonModelledDenominatorTerm) denominator += Math.exp(lambda * utilityOfNonModelledAlternative);
            //CUProda,z = sum(MMinc,a)over c + 1/lambda*ln(sum(e^(lambda*UIProdc,a,z) over c)
            if (foundOne) utility += 1 / lambda * Math.log(denominator);
            if (Double.isNaN(utility) || overallUtilityCalcPrint > 0) {
                logger.warn("building up a commodity utility to " + utility + ", details follow:");
                double[] amount = calcAmounts(individualCommodityUtilities);
                utility = 0;
                for (int c = 0; c < sortedQuantitiesToUse.size(); c++) {
                    Quantity q = (Quantity) sortedQuantitiesToUse.get(c);
                    if (q != null) {
                        utility += scaling * q.minimum * (individualCommodityUtilities[c] * q.utilityScale + q.utilityOffset);
                        logger.warn(q + " has quantity " + amount[c] + " and indiv utility " +
                                individualCommodityUtilities[c] + "; utility now is " + utility);
                    } else {
                        logger.warn("entry " + c + " is null -- no effect");
                    }
                }
            }
//            if (Double.isNaN(utility))
//                throw new InvalidZUtilityError(this.toString());
            if (overallUtilityCalcPrint > 0) overallUtilityCalcPrint--;
            //Returns alphaprod (or alphacons) * CUProda,z
            return utility * scaling;
        }
    }

    public double logitScaleRatio(boolean selling) {
        double totalScale = 0;
        double maxAdditionalScale = 0;
        for (int c = 0; c < sortedQuantitiesToUse.size(); c++) {
            Quantity q = (Quantity) sortedQuantitiesToUse.get(c);
            if (q != null) {
                double scale = q.com.getDefaultBuyingDispersionParameter();
                if (selling) scale = q.com.getDefaultSellingDispersionParameter();
                double minAddition = (q.minimum/scale)*(q.minimum/scale)*q.utilityScale*q.utilityScale;
                double maxAddition = ((q.minimum+q.discretionary)/scale)*((q.minimum+q.discretionary)/scale)*q.utilityScale*q.utilityScale;
                if ((maxAddition-minAddition)>maxAdditionalScale) maxAdditionalScale = maxAddition-minAddition;
                totalScale += minAddition;
            }
        }
        totalScale += maxAdditionalScale;
        double maxThisScale = 1/Math.sqrt(totalScale);
        if (this.lambda>maxThisScale) {
            logger.warn("Dispersion parameter for "+this+" is too high, maximum "+maxThisScale);
        }
        return lambda/maxThisScale;
    }

    public double[] overallUtilityDerivatives(double[] individualCommodityUtilities) {
        double[] d1 = new double[sortedQuantitiesToUse.size()];
        double[] d2 = new double[sortedQuantitiesToUse.size()];
        double denominator = 0;
        boolean foundOne = false;
        for (int c = 0; c < sortedQuantitiesToUse.size(); c++) {
            Quantity q = (Quantity) sortedQuantitiesToUse.get(c);
            if (q != null) {
                foundOne = true;
                if (q.minimum != 0) {
                    d1[c] = q.utilityScale * scaling * q.minimum;
                }
                if (q.discretionary != 0) {
                    foundOne = true;
                    d2[c] = Math.exp(lambda * q.discretionary * (individualCommodityUtilities[c] * q.utilityScale + q.utilityOffset));
                    denominator += d2[c];
                }
            }
        }
        if (nonModelledDenominatorTerm) denominator += Math.exp(lambda * utilityOfNonModelledAlternative);
        if (foundOne) {
            for (int c = 0; c < sortedQuantitiesToUse.size(); c++) {
                Quantity q = (Quantity) sortedQuantitiesToUse.get(c);
                if (q != null) {
                    if (q.discretionary != 0) {
                        d1[c] += q.utilityScale * scaling * d2[c] / denominator * q.discretionary;
                    }
                }
            }
        }
        return d1;
    }

    /**
     * This function calculates the amount of each commodity given the utility of each commodity.
     *
     * @param individualCommodityUtilities the utility of buying or selling each of the individual commodities that make up
     *                                     the list of commodities.   Length must match the length of the internal commodity list
     * @return an array specifying the amount of each commodity
     */

    public double[] calcAmounts(double[] individualCommodityUtilities) {
        double[] amounts = new double[sortedQuantitiesToUse.size()];
        if (individualCommodityUtilities.length != amounts.length) {
            throw new Error("Incorrect number of commodities for production/consumption function calculation");
        }
        Quantity q = null;
        double denominator = 0;
        boolean foundOne = false;
        for (int c = 0; c < amounts.length; c++) {

            // first use the amounts array to store the numerator of the logit choice
            q = (Quantity) sortedQuantitiesToUse.get(c);
            if (q == null) {
                amounts[c] = 0;
            } else {
                if (q.discretionary == 0) {
                    amounts[c] = 0;
                } else {
                    foundOne = true;
                    amounts[c] = Math.exp(lambda * q.discretionary * (individualCommodityUtilities[c] * q.utilityScale + q.utilityOffset));
                }

                denominator += amounts[c];
            }
        }
        // split equally if all alternatives are negative infinity
        // could try to use exceptions instead -- throw a NoAlternativeAvailable exception?
        if (nonModelledDenominatorTerm) denominator += Math.exp(lambda * utilityOfNonModelledAlternative);
        if (denominator == 0 && foundOne) {
            for (int c = 0; c < amounts.length; c++) {
                amounts[c] = 1;
                denominator += amounts[c];
            }
        }
        for (int c = 0; c < amounts.length; c++) {
            q = (Quantity) sortedQuantitiesToUse.get(c);
            if (q != null) {
                if (amountsCalcPrint > 0) logger.info(q + " has expQUtility " + amounts[c]);
                if (!foundOne) {
                    amounts[c] = q.minimum;
                    // no alternative producation choices
                } else {
                    amounts[c] = q.minimum + (q.discretionary * (amounts[c] / denominator));
                }
                if (amountsCalcPrint > 0) logger.info(" and amount " + amounts[c] + " at utility " + individualCommodityUtilities[c]);
            }
        }
        if (amountsCalcPrint > 0) --amountsCalcPrint;
        return amounts;
    }
    
//    public double[][] probabilitiesFullDerivatives(double[] individualCommodityUtilities, double[][] commodityUtilitiesDerivativeWRTPrices) {
//        return probabilitiesFullDerivativesMultiply(individualCommodityUtilities,productionUtilitiesDerivativeWRTPrices(commodityUtilitiesDerivativeWRTPrices));
//    }
  
//    private double[][] productionUtilitiesDerivativeWRTPrices(double[][] commodityUtilitiesDerivativeWRTPrices) {
//        double[] amounts = new double[sortedQuantitiesToUse.size()];
//        if (commodityUtilitiesDerivativeWRTPrices.length != amounts.length) {
//            throw new Error("Incorrect number of commodities for production/consumption function calculation");
//        }
//        double[][] utilityDerivatives = new double[amounts.length][commodityUtilitiesDerivativeWRTPrices[0].length];
//        Quantity q = null;
//        for (int c = 0; c < amounts.length; c++) {
//
//            // first use the amounts array to store the numerator of the logit choice
//            q = (Quantity) sortedQuantitiesToUse.get(c);
//            if (q != null) {
//                for (int p=0;p<commodityUtilitiesDerivativeWRTPrices[0].length;p++) {
//                    for (int i=0;i<utilityDerivatives.length;i++) {
//                        // TODO add in utility scaling effect
//                        utilityDerivatives[i][p] += q.minimum*commodityUtilitiesDerivativeWRTPrices[i][p];
//                    }
//                    utilityDerivatives[c][p]+= q.discretionary*commodityUtilitiesDerivativeWRTPrices[c][p];
//                }
//            }
//        }
//        return utilityDerivatives;
//    }
    
    /**
     * caution: the square array returned is a field of the class, so it can't be relied on 
     * to not change.  If you need a constant use System.arraycopy.
     * 
     * @param commodityUtilities
     * @return a square array of how the quantities produced or consumed change wrt a change in the commodityUtilities.
     */
    public double[][] derivativesOfQuantitiesWRTUtilities(double[] commodityUtilities) {
        // TODO check this calculation numerically.
        double[] amounts = new double[sortedQuantitiesToUse.size()];
        if (utilityDerivatives==null) {
            utilityDerivatives = new double[amounts.length][amounts.length];
        } else {
            if (utilityDerivatives.length!=amounts.length) {
                utilityDerivatives = new double[amounts.length][amounts.length];
            }
        }
        double[] probabilities = getExtraProductionProbabilities(commodityUtilities);
        Quantity q = null;
        for (int commodityIndex = 0; commodityIndex < amounts.length; commodityIndex++) {
            q = (Quantity) sortedQuantitiesToUse.get(commodityIndex);
            if (q != null) {
                for (int utilityIndex=0;utilityIndex<amounts.length;utilityIndex++) {
                    if (commodityIndex!=utilityIndex) {
                        Quantity q2 = (Quantity) sortedQuantitiesToUse.get(utilityIndex);
                        if (q2!=null) {
                            
                            // short way after algebraic simplification
                            utilityDerivatives[utilityIndex][commodityIndex] =
                                -lambda*q.discretionary*q2.discretionary*probabilities[commodityIndex]*probabilities[utilityIndex]*q.utilityScale;
                            // long way before algebraic simplification -- beware rounding errors
                            // the effect of losing the extra (minimum+discretionary) associated with this production option

//                            utilityDerivatives[utilityIndex][commodityIndex]=
//                                (q.minimum+q.discretionary)*(-lambda*q2.discretionary*probabilities[commodityIndex]*probabilities[utilityIndex]);
//                            for (int commodityIndex2=0; commodityIndex2<amounts.length;commodityIndex2++) {
//                                if (commodityIndex2==commodityIndex) {
//                                    // nothing extra
//                                } else if (commodityIndex2==utilityIndex) {
//                                    // the effect of getting more minimum in the option where utility goes up
//                                    utilityDerivatives[utilityIndex][commodityIndex]+=
//                                        lambda*q.minimum*q2.discretionary*(probabilities[commodityIndex2]*(1-probabilities[commodityIndex2]));
//                                } else {
//                                    // the effect of losing minimum in the options where share goes down
//                                    utilityDerivatives[utilityIndex][commodityIndex] +=
//                                        -lambda*q.minimum*q2.discretionary*probabilities[commodityIndex2]*probabilities[utilityIndex];
//                                }
//                            }
                        }
                    } else {
                        // short way after algebraic simplification
                        utilityDerivatives[utilityIndex][commodityIndex]=lambda*q.discretionary*q.discretionary*probabilities[commodityIndex]*(1-probabilities[commodityIndex])*q.utilityScale;
                        
                        // long way before algebraic simplification .. beware rounding errors
                        // the effect of getting extra (minimum+discretionary)
//                        utilityDerivatives[utilityIndex][commodityIndex]=
//                            (q.minimum+q.discretionary)*(lambda*q.discretionary*probabilities[commodityIndex]*(1-probabilities[commodityIndex]));
//                        for (int commodityIndex2=0; commodityIndex2<amounts.length;commodityIndex2++) {
//                            if (commodityIndex2 != commodityIndex) { 
//                                // the effect of losing minimum on the other options
//                                utilityDerivatives[utilityIndex][commodityIndex]+=
//                                    -lambda*q.minimum*q.discretionary*probabilities[commodityIndex]*probabilities[commodityIndex2];
//                            }
//                        }
                    }
                }
            }
        }
        return utilityDerivatives;
    }

    private double[] getExtraProductionProbabilities(double[] individualCommodityUtilities) {
        double[] probabilities = new double[sortedQuantitiesToUse.size()];
        if (individualCommodityUtilities.length != probabilities.length) {
            throw new Error("Incorrect number of commodities for production/consumption function calculation");
        }
        Quantity q = null;
        double denominator = 0;
        boolean foundOne = false;
        for (int c = 0; c < probabilities.length; c++) {

            // first use the amounts array to store the numerator of the logit choice
            q = (Quantity) sortedQuantitiesToUse.get(c);
            if (q == null) {
                probabilities[c] = 0;
            } else {
                if (q.discretionary == 0) {
                    probabilities[c] = 0;
                } else {
                    foundOne = true;
                    probabilities[c] = Math.exp(lambda * q.discretionary * (individualCommodityUtilities[c] * q.utilityScale + q.utilityOffset));
                }

                denominator += probabilities[c];
            }
        }
        if (nonModelledDenominatorTerm) denominator += Math.exp(lambda * utilityOfNonModelledAlternative);
        // split equally if all alternatives are negative infinity
        // could try to use exceptions instead -- throw a NoAlternativeAvailable exception?
        if (denominator == 0 && foundOne) {
            for (int c = 0; c < probabilities.length; c++) {
                probabilities[c] = 1;
                denominator += probabilities[c];
            }
        }
        for (int c = 0; c < probabilities.length; c++) {
            q = (Quantity) sortedQuantitiesToUse.get(c);
            if (q != null) {
                if (!foundOne) {
                    probabilities[c] = 0;
                    // no alternative producation choices
                } else {
                    probabilities[c] = probabilities[c] / denominator;
                }
            }
        }
        return probabilities;
        
        
    }
    
    private double[][] probabilitiesFullDerivativesMultiply(double[] individualCommodityUtilities, double[][] productionUtilitiesDerivativeWRTPrices) {
        double[][] derivatives = new double[sortedQuantitiesToUse.size()][productionUtilitiesDerivativeWRTPrices[0].length];
        double[] weights = new double[sortedQuantitiesToUse.size()];
        if (individualCommodityUtilities.length != derivatives.length) {
            throw new Error("Incorrect number of commodities for production/consumption function calculation");
        }
        Quantity q = null;
        double denom = 0;
        for (int c = 0; c < derivatives.length; c++) {

            // first use the amounts array to store the numerator of the logit choice
            q = (Quantity) sortedQuantitiesToUse.get(c);
            if (q == null) {
                weights[c] = 0;
            } else {
                if (q.discretionary == 0) {
                    weights[c] = 0;
                } else {
                    weights[c] = Math.exp(lambda * q.discretionary * (individualCommodityUtilities[c] * q.utilityScale + q.utilityOffset));
                }

                denom += weights[c];
            }
        }
        if (nonModelledDenominatorTerm) denom += Math.exp(lambda * utilityOfNonModelledAlternative);
        for (int c = 0; c < derivatives.length; c++) {
            q = (Quantity) sortedQuantitiesToUse.get(c);
            if (q != null) {
                if (denom == 0) {
                    for (int i=0;i<derivatives[c].length;i++) {
                        derivatives[c][i]=0;
                    }
                } else {
                    for (int i=0;i<derivatives[c].length;i++){
                        derivatives[c][i] = lambda * weights[c]/denom* productionUtilitiesDerivativeWRTPrices[c][i];
                        for (int j=0;j<productionUtilitiesDerivativeWRTPrices[c].length;j++) {
                            derivatives[c][i] -= lambda * weights[c]/denom*weights[j]/denom *productionUtilitiesDerivativeWRTPrices[c][j];
                        }
                    }
                }
            }
        }
        return derivatives;
    }

    
    public double[] amountsDerivatives(double[] individualCommodityUtilities) {
        double[] derivatives = new double[sortedQuantitiesToUse.size()];
        if (individualCommodityUtilities.length != derivatives.length) {
            throw new Error("Incorrect number of commodities for production/consumption function calculation");
        }
        Quantity q = null;
        double denom = 0;
        for (int c = 0; c < derivatives.length; c++) {

            // first use the amounts array to store the numerator of the logit choice
            q = (Quantity) sortedQuantitiesToUse.get(c);
            if (q == null) {
                derivatives[c] = 0;
            } else {
                if (q.discretionary == 0) {
                    derivatives[c] = 0;
                } else {
                    derivatives[c] = Math.exp(lambda * q.discretionary * (individualCommodityUtilities[c] * q.utilityScale + q.utilityOffset));
                }

                denom += derivatives[c];
            }
        }
        if (nonModelledDenominatorTerm) denom += Math.exp(lambda * utilityOfNonModelledAlternative);
        for (int c = 0; c < derivatives.length; c++) {
            q = (Quantity) sortedQuantitiesToUse.get(c);
            if (q != null) {
                if (denom == 0) {
                    derivatives[c] = 0;
                } else {
                    derivatives[c] = q.utilityScale * lambda * q.discretionary * q.discretionary * (derivatives[c] / denom) * (1 - (derivatives[c] / denom));
                }
            }
        }
        return derivatives;
    }


    public void sortToMatch(Collection commodityList) {
        sortedQuantitiesToUse = new ArrayList();
        Iterator commodityIterator = commodityList.iterator();
        Iterator quantityIterator = null;
        while (commodityIterator.hasNext()) {
            Commodity c = (Commodity) commodityIterator.next();
            quantityIterator = myCommodities.iterator();
            boolean found = false;
            while (quantityIterator.hasNext() && !found) {
                Quantity q = (Quantity) quantityIterator.next();
                if (q.com.equals(c)) {
                    sortedQuantitiesToUse.add(q);
                    found = true;
                }
            }
            if (!found) {
                //                    sortedQuantitiesToUse.add(new Quantity(c, 0, 0, 0, 0, 0));
                sortedQuantitiesToUse.add(null);
            }
        }
    }

    public int size() {
        return sortedQuantitiesToUse.size();
    }

    public void addCommodity(Quantity q) {
        myCommodities.add(q);
    }

    public AbstractCommodity commodityAt(int i) {
        Quantity q = (Quantity) sortedQuantitiesToUse.get(i);
        if (q == null) return null;
        return q.com;
    }

    /**
     * @param utilityOfNonModelledAlternativeParameter
     */
    public void addNonModelledAlternative(double utilityOfNonModelledAlternativeParameter) {
        utilityOfNonModelledAlternative = utilityOfNonModelledAlternativeParameter;
        nonModelledDenominatorTerm = true;
    }





}

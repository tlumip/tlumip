package com.pb.despair.model;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 
 */
public class LogitModel extends DiscreteChoiceModel implements Alternative {

    /**
     * The alternatives that need to be chosen between
     * @supplierCardinality 1..*
     * @clientCardinality *
     * @link aggregation
     * @associates <{com.pb.common.old.model.Alternative}>
     */
    private double dispersionParameter;
    private double constantUtility=0;
    protected ArrayList alternatives;


    public void allocateQuantity(double amount) throws ChoiceModelOverflowException {
        double[] probs = getChoiceProbabilities();
        for (int i=0;i<probs.length;i++) {
            Alternative a = alternativeAt(i);
            ((AggregateAlternative) a).setAggregateQuantity(amount*probs[i],amount*probs[i]*(1-probs[i])*dispersionParameter);
        }
    }

    /** @param a the alternative to add into the choice set */
    public void addAlternative(Alternative a) {
        alternatives.add(a);
    }

    public LogitModel() {
      alternatives = new ArrayList();
           dispersionParameter = 1.0;
    }
    //use this constructor if you know how many alternatives
    public LogitModel(int numberOfAlternatives) {
         alternatives = new ArrayList(numberOfAlternatives);
           dispersionParameter = 1.0;
    }


    /** @return the composite utility (log sum value) of all the alternatives */
    public double getUtility(double higherLevelDispersionParameter) throws ChoiceModelOverflowException {
        // message #1.2.3.1.3.2.1 to allExchanges:com.pb.despair.pa.Exchange
        // double unnamed = allExchanges.getPrice();
        double sum = 0;
        int i = 0;
        while (i<alternatives.size()) {
            sum += Math.exp(dispersionParameter * ((Alternative)alternatives.get(i)).getUtility(dispersionParameter));
            i++;
        }
        double bob = (1 / dispersionParameter) * Math.log(sum);
        if (Double.isNaN(bob)) {
            throw new ChoiceModelOverflowException("Overflow getting composite utility");
        }
        return bob+constantUtility;
    }

    public double getDispersionParameter() { return dispersionParameter; }

    public void setDispersionParameter(double dispersionParameter) { this.dispersionParameter = dispersionParameter; }

    public double[] getChoiceProbabilities() throws ChoiceModelOverflowException {
        synchronized(alternatives) {
            double[] weights = new double[alternatives.size()];
            double sum = 0;
            Iterator it = alternatives.listIterator();
            int i = 0;
            while (it.hasNext()) {
                Alternative a = (Alternative) it.next();
                double utility = a.getUtility(dispersionParameter);
                weights[i] = Math.exp(dispersionParameter * utility);
                if (Double.isNaN(weights[i])) {
                  throw new ChoiceModelOverflowException("NAN in weight for alternative "+a);
                }
                sum += weights[i];
                i++;
            }
            if (sum!=0) {
                    for (i = 0; i < weights.length; i++) {
                         weights[i] /= sum;
                    }
               }
               return weights;
        }
    }

     public Alternative alternativeAt(int i) { return (Alternative) alternatives.get(i);}// should throw an error if out of range


    /** Picks one of the alternatives based on the logit model probabilities */
    public Alternative monteCarloChoice() throws NoAlternativeAvailable, ChoiceModelOverflowException {
        synchronized(alternatives) {
            double[] weights = new double[alternatives.size()];
            double sum = 0;
            Iterator it = alternatives.listIterator();
            int i = 0;
            while (it.hasNext()) {
                double utility = ((Alternative)it.next()).getUtility(dispersionParameter);
                weights[i] = Math.exp(dispersionParameter * utility);
                if (Double.isNaN(weights[i])) {
                  throw new ChoiceModelOverflowException("in monteCarloChoice alternative was such that LogitModel weight was NaN");
                }
                sum += weights[i];
                i++;
            }
            if (sum==0) throw new NoAlternativeAvailable();
            double selector = Math.random() * sum;
            sum = 0;
            for (i = 0; i < weights.length; i++) {
                sum += weights[i];
                if (selector <= sum) return (Alternative)alternatives.get(i);
            }
            //yikes!
            throw new Error("Random Number Generator in Logit Model didn't return value between 0 and 1");
        }
    }
    
    /** Picks one of the alternatives based on the logit model probabilities;
          use this if you want to give method random number */
    public Alternative monteCarloChoice(double randomNumber) throws NoAlternativeAvailable, ChoiceModelOverflowException {
        synchronized(alternatives) {
            double[] weights = new double[alternatives.size()];
            double sum = 0;
            Iterator it = alternatives.listIterator();
            int i = 0;
            while (it.hasNext()) {
                double utility = ((Alternative)it.next()).getUtility(dispersionParameter);
                weights[i] = Math.exp(dispersionParameter * utility);
                if (Double.isNaN(weights[i])) {
                    throw new ChoiceModelOverflowException("in monteCarloChoice alternative was such that LogitModel weight was NaN");
                }
                sum += weights[i];
                i++;
            }
            if (sum==0) throw new NoAlternativeAvailable();
            double selector = randomNumber * sum;
            sum = 0;
            for (i = 0; i < weights.length; i++) {
                sum += weights[i];
                if (selector <= sum) return (Alternative)alternatives.get(i);
            }
            //yikes!
            throw new Error("Random Number Generator in Logit Model didn't return value between 0 and 1");
        }
    }




    public String toString() {
        StringBuffer altsString = new StringBuffer();
    	int alternativeCounter = 0;
        if (alternatives.size() > 5) { altsString.append("LogitModel with " + alternatives.size() + "alternatives {"); }
        else altsString.append("LogitModel, choice between ");
        Iterator it = alternatives.iterator();
        while (it.hasNext() && alternativeCounter < 5) {
            altsString.append(it.next());
            altsString.append(",");
            alternativeCounter ++;
        }
        if (it.hasNext()) altsString.append("...}"); else altsString.append("}");
        return new String(altsString);
    }

    public double getConstantUtility(){ return constantUtility; }

    public void setConstantUtility(double constantUtility){ this.constantUtility = constantUtility; }

    /**
     * Method arrayCoefficientSimplifiedChoice.
     * @param theCoefficients
     * @param theAttributes
     * @return int
     */
    public static int arrayCoefficientSimplifiedChoice(
        double[][] theCoefficients,
        double[] theAttributes) {

		double[] utilities = new double[theCoefficients.length];    
		int alt;
    	for (alt =0; alt < theCoefficients.length; alt++){
    		utilities[alt] = 0;
    		for (int c=0;c<theAttributes.length;c++) {
    			utilities[alt]+=theCoefficients[alt][c]*theAttributes[c];
    		}
    	}
    	int denominator = 0;
    	for (alt=0;alt<utilities.length;alt++) {
    		utilities[alt] = Math.exp(utilities[alt]);
    		denominator+=utilities[alt];
    	}
    	double selector = Math.random()*denominator;
    	double cumulator = 0;
    	for (alt=0;alt<utilities.length;alt++) {
    		cumulator += utilities[alt];
    		if (selector<=cumulator) return alt;
    	}
        // shouldn't happen
        return utilities.length-1;
    }

}



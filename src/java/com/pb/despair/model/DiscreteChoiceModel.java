package com.pb.despair.model;

import java.util.Random;

/**
 * 
 */
public abstract class DiscreteChoiceModel {
    /** Picks one of the alternatives based on the logit model probabilities */
    public abstract Alternative monteCarloChoice() throws NoAlternativeAvailable, ChoiceModelOverflowException ;

    /** Picks one of the alternatives based on the logit model probabilities and random number given*/
    public abstract Alternative monteCarloChoice(double r) throws NoAlternativeAvailable, ChoiceModelOverflowException ;

    public Alternative monteCarloElementalChoice() throws NoAlternativeAvailable, ChoiceModelOverflowException {
        Alternative a = monteCarloChoice();
        while (a instanceof DiscreteChoiceModel) {
            a = ((DiscreteChoiceModel) a).monteCarloChoice();
        }
        return a;
    }
    /** Use this method if you want to give a random number */
    public Alternative monteCarloElementalChoice(double r) throws NoAlternativeAvailable, ChoiceModelOverflowException {
        Alternative a = monteCarloChoice(r );
        Random newRandom = new Random(new Double(r*1000).longValue());
        while (a instanceof DiscreteChoiceModel) {
            a = ((DiscreteChoiceModel) a).monteCarloChoice(newRandom.nextDouble());
        }
        return a;
    }

    /** @param a the alternative to add into the choice set */
    public abstract void addAlternative(Alternative a);

     public abstract Alternative alternativeAt(int i);

    public abstract double[] getChoiceProbabilities() throws ChoiceModelOverflowException;

    abstract public void allocateQuantity(double amount) throws ChoiceModelOverflowException ;
    

}


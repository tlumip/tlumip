package com.pb.despair.model;

/**
 * 
 */
public interface AggregateAlternative extends Alternative {
    void setAggregateQuantity(double amount, double derivative) throws ChoiceModelOverflowException;
}

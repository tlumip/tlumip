package com.pb.despair.model;

/**
 *
 */
public interface Alternative {
    double getUtility(double dispersionParameterForSizeTermCalculation) throws ChoiceModelOverflowException ;
}

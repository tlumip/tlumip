package com.pb.despair.model;

/**
 * 
 */
public class FixedUtilityAlternative implements Alternative {
    public FixedUtilityAlternative(double utilityValue) {
      this.utilityValue=utilityValue;
    }

    public double getUtility(double dispersion) {return utilityValue;}

    public double getUtilityValue(){ return utilityValue; }

    public void setUtilityValue(double utilityValue){ this.utilityValue = utilityValue; }

    private double utilityValue;
    public String toString() {return "FixedUtility - "+utilityValue;};
}


package com.pb.despair.model;


/** A class that represents how the preferences for travel by different modes and different times of day
 *
 * @author J. Abraham
 */
public interface TravelUtilityCalculatorInterface {
   public double getUtility(int origin, int destination, TravelAttributesInterface travelConditions);

/**
 * @param fromZoneUserNumber
 * @param toZoneUserNumber
 * @param dt
 * @return
 */
   public double[] getUtilityComponents(int fromZoneUserNumber, int toZoneUserNumber,  TravelAttributesInterface travelConditions);
}

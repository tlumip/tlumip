package com.pb.tlumip.ts.old;

import com.pb.models.pecas.AbstractTAZ;

/**
 * A interface defining operations expected of different ways of summarizing travel conditions
 * @see TravelCharacteristicMatrix
 * @author John Abraham
 */
public interface SummaryOfTravelConditions   {


  /**
     * Figures out the attributes of getting from origin to destination.  Also includes info about the modes, time of day, etc that are being considered.
     */
  public ZPAttribute retrieveZPAttribute(AbstractTAZ o, AbstractTAZ d, boolean useRouteChoice);
} 

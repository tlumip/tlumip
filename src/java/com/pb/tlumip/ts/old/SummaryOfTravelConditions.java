/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
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

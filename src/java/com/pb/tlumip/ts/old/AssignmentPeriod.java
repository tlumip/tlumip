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

import java.util.Vector;
/**
 * An AssignmentPeriod is a loaded network.  The "links" attribute that it 
 * inherits points to a vector of LinkLoad objects.  This class 
 * is responsible for doing the actual assingment
 * @author John Abraham
 */
public class AssignmentPeriod extends Network   {

 /**
 * An attribute that represents the time period that is being loaded
 */
 public LoadPeriod myLoadPeriod;
 
 
 /**
 * The assignment period actually does the assignment, and so needs to update the summary information.
 *  
 *  This is a vector referencing the TravelCharacteristicMatrix for each mode and for this assignment period
 * @associates <{TravelCharacteristicMatrix}>
 * @supplierCardinality 1..*
 * 
 */

     private java.util.Vector characteristicsByMode;

          /**
         * These are all the trips that need to be loaded onto the network
         * @associates <{Trip}>
         * @supplierCardinality 1..* 
         */
        private Vector allTheTrips;
  /**
      * An operation that erases the load on all the links.*/
     void reSetNetwork( ) {
  }
    public String toString() {return "Assignment period for"+myLoadPeriod;};

     /**
     * Once all the Network and links and trips are set up, this routine will actually
     * do the equilibrium assignment algorithm
     */
     void doAssignment() {}

}

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
/**
 * @(#) LinkLoad.java
 */

package com.pb.tlumip.ts.old;
import com.pb.models.pecas.TravelUtilityCalculatorInterface;

/**
 * A class that represents the load on a link for a particular assignment period
 * see OtherClasses
 * @author your_name_here
 */
public class LinkLoad implements LinkInterface
{
        protected void setVolume() {
        }

        public void emptyLink() {
        }

        public double getUtility(TravelUtilityCalculatorInterface tp) {
          return 0;
        }

        public void addLoad(float howMuchToAdd) {
        }

        /**
         * The assignment period that the load occurs on.  This is the network that this LinkLoad is a part of.
         */
	protected AssignmentPeriod myAssignmentPeriod;

        /**
         * The link that the load is on
         * clientCardinality 0..*
         * supplierCardinality 1
         */
        private Link myLink;

        /**
         * this is private because changing the load on a link also implies that the travel time changes.
         */
        private float periodVolume;

        /**
         * supplierCardinality 1
         * supplierRole to
         */
        private NodeLoad to;

        /**
         * supplierCardinality 1
         * supplierRole from
         */
        private NodeLoad from;
    public String toString() {return "loaded "+myLink;};
}

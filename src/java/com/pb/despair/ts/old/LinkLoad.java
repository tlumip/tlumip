/**
 * @(#) LinkLoad.java
 */

package com.pb.despair.ts.old;
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

/** @(#) ZPAttribute.java */

package com.pb.tlumip.ts.old;



/**
 * A ZPAttribute is a class that contains the informatioin about the route choice between zone pairs on a congested network,
 * by a given mode and for a person with given TravelPreferences. <p>
 * This is the lightweight version, which only stores the final result.  Generally one would use the heavier weight version
 * FatZPAttribute to invistigate the paths, then use the supplied constructor to create a lightweight version.  An alternative
 * would be to to store the numbers (cost, times) directly in arrays.  This would save on memory and processing but would make
 * code maintenance and debugging harder. <p> Responsibilities: C:1 - find the path for a given zone pair.
 * @see <{com.pb.tlumip.ts.FatZPAttribute}>
 * @author J. Abraham
 */
public class ZPAttribute implements Cloneable {
    //        /**
    //         * @param m the mode that the route choice should use
    //         * @param tp the route choice parameters that determine the unique choice of route
    //         * @param ntwrk the congested network that the route flows through
    //         * @param o origin zone
    //         * @param d destination zone
    //         */
    //       public synchronized void findMinimumPath(com.pb.tlumip.pt.TravelPreferences tp, Mode m, AssignmentPeriod ntwrk,
    // UnitOfLand o, UnitOfLand d) {
    //                FatZPAttribute bob = new FatZPAttribute(
    //       }

    /**
     * This constructor creates a new ZPAttribute from a FatZPAttribute. It copies the necessary stuff out, but strips off all
     * the extra stuff that we don't want to store in-memory for long term.
     * @param zp the FatZPAttribute that needs to go on a diet for memory reasons
     */
    public ZPAttribute(FatZPAttribute zp) {
        cost = zp.cost;
        times = new float[zp.times.length];
        System.arraycopy(zp.times, 0, times, 0, times.length);
    }

    /**
     * This constructor creates a new ZPAttribute from a FatZPAttribute. It copies the necessary stuff out, but strips off all
     * the junk that we don't want to store in-memory for long term.
     * @param zp the FatZPAttribute that needs to go on a diet for memory reasons
     */
    protected Object clone() { return new ZPAttribute(this); }

    public ZPAttribute(ZPAttribute zp) {
        cost = zp.cost;
        times = new float[zp.times.length];
        System.arraycopy(zp.times, 0, times, 0, times.length);
    }

    public ZPAttribute(int howManyTimeFields) {
        times = new float[howManyTimeFields];
    }

    public ZPAttribute(float cost, float time) {
        this.cost = cost;
        this.times = new float[1];
        this.times[0] = time;
    }

    /** The distance associated with travelling over the associated route between the origin and destination */
    public float cost;

    /** The travel time associated with travelling over the associated route between the origin and destination */
    public float[] times;

    public String toString() {
        StringBuffer timesString = new StringBuffer();
        for (int i = 0; i < times.length; i++) {
            timesString.append(times[i]);
        }
        return "ZPAttribute, cost " + cost + " times " + timesString;
    };

    public float getCost(){
            return cost;
        }

    public float[] getTimes(){
            return times;
        }
}


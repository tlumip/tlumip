package com.pb.despair.model;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class represents a type of economic activity that occurs in the modelled area.
 * This is an abstract class -- all ProductionActivity is represented by one of the subclasses AggregateActivity or
 * DisaggregateActivity The main role of this class is to track the amount of activity in each zone and to allocate the
 * changes in the amount of activity in each zone over each time step.  As part of this, the class also needs to work with an
 * AssignmentPeriod to load a network with trips.
 * @see com.pb.despair.ha.Household, DisaggregateActivity, AggregateActivity, AssignmentPeriod
 * @author John E. Abraham
 */
public abstract class ProductionActivity {

    /** The textual name of the category of activity.  A unique identifier */
    public final String name;
    protected AmountInZone[] myDistribution;
    private double sizeTermCoefficient;
    private static final Vector allProductionActivities = new Vector();
    /**
     * @associates <{DevelopmentType}>
     * @supplierCardinality 1..*
     * @supplierRole allowedIn
     */
    protected Vector allowedIn = new Vector();

    public class CantRedoError extends Exception {
        public CantRedoError() { };

        public CantRedoError(String s) {
            super(s);
        };
    };
    
    public void setSizeTermsToZero() {
        for (int z=0;z<myDistribution.length;z++) {
            myDistribution[z].setAllocationSizeTerm(0);
        }
    }
    
    public void increaseSizeTerm(int zoneUserNumber, double sizeTermIncrement) {
        int zoneIndex = AbstractTAZ.findZoneByUserNumber(zoneUserNumber).getZoneIndex();
        myDistribution[zoneIndex].setAllocationSizeTerm(myDistribution[zoneIndex].getAllocationSizeTerm()+sizeTermIncrement);
    }

    /**
     * A ProductionActivity needs to set up its array of AggregateDistributions
     * to keep track of how much activity is in each zone.  To ensure that this
     * is done properly we make the constructor take as an argument the array of all zones.
     * @param allZones an array of all the TAZ zones in the system
     * @see <{com.pb.despair.model.AbstractTAZ}>
     */
    protected ProductionActivity(String name, AbstractTAZ[] allZones) {
        ProductionActivity oldOne = retrieveProductionActivity(name);
        if (oldOne != null) {
            throw new Error("Tried to create a duplicate ProductionActivity: " + name);
        }
        this.name = name;
        myDistribution = new AmountInZone[allZones.length];
        for (int z = 0; z < allZones.length; z++) {
            myDistribution[z] = new AmountInZone(this, allZones[z]);
        }
        allProductionActivities.add(this);
    }

    public static ProductionActivity retrieveProductionActivity(String name) {
        Iterator it = allProductionActivities.iterator();
        while (it.hasNext()) {
            ProductionActivity bob = (ProductionActivity)it.next();
            if (bob.name.equals(name)) {
                return bob;
            }
        }
        return null;
    }

    public Collection getAllowedIn() {
        return Collections.unmodifiableCollection(allowedIn);
    }

    public static Collection getAllProductionActivities() {
        return Collections.unmodifiableCollection(allProductionActivities);
    }

    public AmountInZone[] getMyDistribution() {
        return myDistribution;
    }

    /**
     * setDistribution sets the amount of activity in a zone.  The ProbabilityDensityFunction v can be used to specify
     * how this activity might not be homogeneous.
     * @param zone the zone in which the activity is being set
     * param v a statistical function for accounting for the variation within this category of activity, only used for
     * Disaggregate activities
     * @param quantity the amount of activity in the zone
     */
    public void setDistribution(AbstractTAZ zone, double quantity) {
        for (int z = 0; z < myDistribution.length; z++) {
            if (myDistribution[z].getMyTaz().equals(zone)) {
                myDistribution[z].setQuantity(quantity);
            }
        }
    }

    /**
     * setDistribution sets the amount of activity in a zone.  The ProbabilityDensityFunction v can be used to specify
     * how this activity might not be homogeneous.
     * @param zone the zone in which the activity is being set
     * param v a statistical function for accounting for the variation within this category of activity, only used for
     * Disaggregate activities
     * @param quantity the amount of activity in the zone
     */
    public void setDistribution(AbstractTAZ zone,  double quantity, double sizeTerm, double locationUtility) {
        for (int z = 0; z < myDistribution.length; z++) {
            if (myDistribution[z].getMyTaz().equals(zone)) {
                myDistribution[z].setQuantity(quantity);
                myDistribution[z].setLocationSpecificUtilityInclTaxes(locationUtility);
                myDistribution[z].setAllocationSizeTerm(sizeTerm);
            }
        }
    }

    /**
     * This is the main workhorse routine that allocates the regional production to zones.  PA calls this for each production
     * activity.  To simulate economic equilibrium, PIModel may have to call this function once, then repeatedly call
     * reMigrateAndReAllocate. Note, though, that not all activities are in spatial equilibrium.  For ODOT, for instance,
     * household transitions are not in equilibrium. This routine models a discrete time step.  It's important to be able to
     * adjust the time step if necessary, and in fact different things may need different time steps.  So these routines
     * should be careful to use this parameter.  A longer time step implies larger changes.
     * @param timeStep the amount of time that has passed since this function was last called.  If zero, then the
     * ProductionActivity is to redo the previous allocation
     * @param inMigration the amount of activity moving to the region
     * @param outMigration the amount of activity leaving the region.  Net migration is inMigration-outMigration.
     */
    abstract public void migrationAndAllocation(double timeStep, double inMigration, double outMigration) throws OverflowException ;

    /**
     * For certain types of ProductionActivity, including most AggregateActivities, there is a need to do the allocation, then
     * adjust the prices and redo the allocation repeatedly to find the economic equilibrium.  Thus PIModel needs to call
     * mygrationAndAllocation once, to set the in migration, out migration and time period and to reset the previous time
     * point.  Then PIModel can adjust the prices that result and have the ProductionActivity reallocate itself and redo the
     * migration in response to different prices by calling this method.
     * @exception CantRedoError not all types of ProductionActivity can redo their allocation.  Obviously, then, they can't be
     * modelled as being in spatial economic equilibrium
     */
    abstract public void reMigrationAndReAllocation() throws CantRedoError;

    public abstract double getUtility() throws OverflowException ;

    /**
     * @associates <{ProductionFunction}>
     * @supplierRole make table
     * @supplierCardinality 1
     * @clientCardinality 1..*
     */
    public abstract ProductionFunction getProductionFunction();

    /**
     * @associates <{ConsumptionFunction}>
     * @supplierRole use table
     * @clientCardinality 1..*
     */
    public abstract ConsumptionFunction getConsumptionFunction();

    public String toString() { return name; };

    public void disAllowIn(DevelopmentTypeInterface t) {
        if (allowedIn.contains(t)) {
            allowedIn.remove(t);
        }
    }

    public void allowIn(DevelopmentTypeInterface t) {
        if (!allowedIn.contains(t)) {
            allowedIn.add(t);
        }
    }

    public boolean isAllowedIn(DevelopmentTypeInterface t) {
        if (allowedIn.contains(t)) return true;
        return false;
    }

    public double getSizeTermCoefficient() {
        return sizeTermCoefficient;
    }

    public void setSizeTermCoefficient(double sizeTermCoefficient) {
        this.sizeTermCoefficient = sizeTermCoefficient;
    }

    public static void doReMigrationAndReAllocation() {
        Iterator it = allProductionActivities.iterator();
        while (it.hasNext()) {
            ProductionActivity p = (ProductionActivity)it.next();
            try {
                p.reMigrationAndReAllocation();
            } catch (CantRedoError e) { // hey, not everyone can do it...
            }
        }
    }



        /* hashCode and equals could operate on the name, but the constructor ensures that only one
        ProductionActivity with a given name can ever be created, so we can rely on the default hashCode
        and equals instead */

    // public int hashCode() {};
    //  public boolean equals(Object o);
    // public double calcUtilityForPreferences(com.pb.despair.pt.TravelPreferences p, boolean withRouteChoice){
    //}

}

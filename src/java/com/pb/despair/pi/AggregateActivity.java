package com.pb.despair.pi;

import com.pb.despair.model.*;

import org.apache.log4j.Logger;

/**
 * This is the class that represents a certain type of activity using aggregate quantities and prices.  There is no
 * microsimulation in this class
 *
 * @author J. Abraham
 */
public class AggregateActivity extends ProductionActivity {

    private static Logger logger = Logger.getLogger("com.pb.despair.pi");
    private double totalAmount;
    private ConsumptionFunction lnkConsumptionFunction;
    private ProductionFunction lnkProductionFunction;
    private double locationDispersionParameter;
    protected LogitModel logitModelOfZonePossibilities = new LogitModel();


    public AggregateActivity(String name, AbstractTAZ[] allZones) {
        super(name, allZones);

        // fix the amountInZone references to be objects of AggregateDistribution instead of just AmountInZone;
        logitModelOfZonePossibilities = new LogitModel();
        for (int z = 0; z < allZones.length; z++) {
            myDistribution[z] = new AggregateDistribution(this, allZones[z]);
            logitModelOfZonePossibilities.addAlternative((AggregateDistribution) myDistribution[z]);
        }

    }


    /**
     * This is the main workhorse routine that allocates the regional production to zones.  PA calls this for each production
     * activity.  To simulate economic equilibrium, PIModel may have to call this function once, then repeatedly call
     * reMigrateAndReAllocate. Note, though, that not all activities are in spatial equilibrium.  For ODOT, for instance,
     * household transitions are not in equilibrium. This routine models a discrete time step.  It's important to be able to
     * adjust the time step if necessary, and in fact different things may need different time steps.  So these routines
     * should be careful to use this parameter.  A longer time step implies larger changes.
     *
     * @param timeStep the amount of time that has passed since this function was last called.  If zero, then the
     *        ProductionActivity is to redo the previous allocation
     */
    public void migrationAndAllocation(double timeStep) throws ChoiceModelOverflowException {
        logitModelOfZonePossibilities.setDispersionParameter(getLocationDispersionParameter());
        if(logger.isDebugEnabled()) {
            logger.debug("total amount for " + this + " is " + getTotalAmount());
        }
        // message #1.2.3 to spatialAllocationLogitModel:com.pb.despair.utils.LogitModel
        // java.util.Vector unnamed = spatialAllocationLogitModel.getChoiceProbabilities();
        logitModelOfZonePossibilities.allocateQuantity(getTotalAmount());
/*		  ad.setQuantity(probs[i]*totalAmount);
		  ad.setCommoditiesBoughtAndSold();
		}                                  */
    }

    /* (non-Javadoc)
     * @see com.pb.despair.model.ProductionActivity#migrationAndAllocation(double, double, double)
     */
    public void migrationAndAllocation(double timeStep, double inMigration, double outMigration) throws OverflowException {
        setTotalAmount(getTotalAmount() + (inMigration - outMigration));
        try {
            migrationAndAllocation(timeStep);
        } catch (ChoiceModelOverflowException e) {
            throw new OverflowException(e.toString());
        }

    }

    /**
     * For certain types of ProductionActivity, including most AggregateActivities, there is a need to do the allocation, then
     * adjust the prices and redo the allocation repeatedly to find the economic equilibrium.  Thus PIModel needs to call
     * migrationAndAllocation once, to set the in migration, out migration and time period and to reset the previous time
     * point.  Then PIModel can adjust the prices that result and have the ProductionActivity reallocate itself and redo the
     * migration in response to different prices by calling this method.
     * 
     * @throws CantRedoError not all types of ProductionActivity can redo their allocation.  Obviously, then, they can't be
     *                       modelled as being in spatial economic equilibrium
     */
    public void reMigrationAndReAllocation() throws CantRedoError {
        try {
            reMigrationAndReAllocationWithOverflowTracking();
        } catch (OverflowException e) {
            throw new Error("OverflowException: " + e);
        }
    }

    public void reMigrationAndReAllocationWithOverflowTracking() throws OverflowException {
        logitModelOfZonePossibilities.setDispersionParameter(getLocationDispersionParameter());
        double[] probs;
        try {
            probs = logitModelOfZonePossibilities.getChoiceProbabilities();
        } catch (ChoiceModelOverflowException e) {
            throw new OverflowException(e.toString());
        }
        for (int i = 0; i < probs.length; i++) {
            Alternative a = logitModelOfZonePossibilities.alternativeAt(i);
            if (a instanceof AggregateDistribution) {
                ((AggregateDistribution) a).setAggregateQuantityWithErrorTracking(getTotalAmount() * probs[i], probs[i] * (1 - probs[i]) * logitModelOfZonePossibilities.getDispersionParameter() * getTotalAmount());
            } else if (a instanceof AggregateAlternative) {
                try {
                    ((AggregateAlternative) a).setAggregateQuantity(getTotalAmount() * probs[i], probs[i] * (1 - probs[i]) * logitModelOfZonePossibilities.getDispersionParameter() * getTotalAmount());
                } catch (ChoiceModelOverflowException e1) {
                    throw new OverflowException(e1.toString());
                }
            }
        }
    }

    public double getUtility() throws OverflowException {
        try {
            return logitModelOfZonePossibilities.getUtility(1);
        } catch (ChoiceModelOverflowException e) {
            throw new OverflowException(e.toString());
        }
    }

/*------------------------Getters and Setters---------------------------------------------------------*/

    public ConsumptionFunction getConsumptionFunction() {
        return lnkConsumptionFunction;
    }


    public ProductionFunction getProductionFunction() {
        return lnkProductionFunction;
    }

    public void setProductionFunction(ProductionFunction productionFunction) {
        lnkProductionFunction = productionFunction;
    }

    public void setConsumptionFunction(ConsumptionFunction consumptionFunction) {
        lnkConsumptionFunction = consumptionFunction;
    }

    public double getLocationDispersionParameter() {
        return locationDispersionParameter;
    }

    public void setLocationDispersionParameter(double locationDispersionParameter) {
        this.locationDispersionParameter = locationDispersionParameter;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    //    void clearCommodityPurchaseMemory() {
//		for (int i=0;i<myDistribution.length ;i++ ) {
//            AggregateDistribution ad = (AggregateDistribution) myDistribution[i];
    //           ad.clearCommodityPurchaseMemory();
    //       }
//    }
}


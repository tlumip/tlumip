package com.pb.despair.model;

import java.util.Vector;

/**
 * A class that represents an entitiy acting as a unit, e.g. a Firm or a Household
 * @see <{Household}>
 * @author J. Abraham,
 */
public abstract class EconomicUnit {
    /**
     * This function returns a vector of the RegularActivities that the EconomicUnit has to do.  These RegularActivities have
     * locations associated with them.  Thus the EconomicUnit generally tends to locate within a reasonable distance of the
     * RegularActivity locations.
     * @associates <{RegularActivity}>
     * @return a Vector of the regular activities
     */
    abstract public Vector getRegularActivities();

    /**
     * Each EconomicUnit is associated with a ProductionActivity in the economy.  The ProductionActivity is the aggregation
     * of all the EconomicUnits.
     */
    abstract public DisaggregateActivity getMyDisaggregateActivity();

    /**
     * Returns the amount of space needed by the EconomicUnit.  This routine eventually needs some parameters,
     * because the amount of space the EconomicUnit uses is dependent on, for instance, the price of that space,
     * as well as whether "in House" activities take the place of "consumption".
     */
    public abstract float spaceNeeded(DevelopmentTypeInterface dtype);

   public DevelopmentTypeInterface[] getAllowedInDevelopmentTypes() {
        Vector allowed = getMyDisaggregateActivity().allowedIn;
        DevelopmentTypeInterface[] bob = new DevelopmentTypeInterface[allowed.size()];
        return (DevelopmentTypeInterface[]) allowed.toArray(bob);
    }

    public boolean isAllowedIn(DevelopmentTypeInterface t) {
        return getMyDisaggregateActivity().isAllowedIn(t);
    }

    public ConsumptionFunction getConsumptionFunction() { return getMyDisaggregateActivity().getRepresentativeConsumptionFunction(); }


    public ProductionFunction getProductionFunction() { return getMyDisaggregateActivity().getRepresentativeProductionFunction(); }

    /**
     * This takes the economic unit its "births and deaths"
     * @param elapsedTime the amount of time that has elapsed
     * @return returns true if the economic unit disappears because of demographic changes (everyone dies, moves away or
     * the firm goes bankrupt)
     */
    protected abstract boolean demographicChanges(double elapsedTime);

    public int movingFlag = 0;
    public int secondaryMovingFlag = 0;
    public final static int MOVE_OUTSIDE_THE_REGION = 1;
    public final static int MOVE_INSIDE_THE_REGION = 2;

    /**
     * This is the function that takes the EconomicUnity through its spatial
     * transitions.  This part does the vacating spatial transitions, which may
     * put the household into the AllUnits moving pool.
     */
    abstract protected void decideActionsRegardingLocations(double elapsedTime);

    public void youHaveToLeave(GridCell.FloorspaceChunk s) {
        for (int i = 0; i < 2; i++) {
            if (primaryAndSecondaryLocation[i] == s) {
                getReadyToMoveOut(i);
            }
        }
        doAnyFlaggedMoves();
    }


    public void getReadyToMoveOut(int primaryOrSecondary) {
        if (primaryOrSecondary == 0) {
            if (Math.random() < getMovingOutsideTheRegionProbability()) {
                movingFlag = MOVE_OUTSIDE_THE_REGION;
            } else {
                movingFlag = MOVE_INSIDE_THE_REGION;
                DisaggregateActivity myPA = getMyDisaggregateActivity();
                myPA.addToMovingPool(this);
            }
        } else {
            secondaryMovingFlag = 1;
        }
    }


    public void doAnyFlaggedMoves() {
        if (movingFlag == MOVE_OUTSIDE_THE_REGION) {
            this.moveOutsideTheRegion();
            primaryAndSecondaryLocation[0] = null  ;
            movingFlag = 0;
        }
        if (movingFlag == MOVE_INSIDE_THE_REGION) {
            primaryAndSecondaryLocation[0].movingOut();
           primaryAndSecondaryLocation[0] = null;
           movingFlag = 0;
        }
        DisaggregateActivity myPA = getMyDisaggregateActivity();
        if (secondaryMovingFlag != 0) {
            primaryAndSecondaryLocation[1].movingOut();
           primaryAndSecondaryLocation[1] = null;
           secondaryMovingFlag = 0;
        }
    }

    public abstract double getMovingOutsideTheRegionProbability();

    synchronized void moveOutsideTheRegion() {
    	freeUpFloorspace();
        DisaggregateActivity myPA = getMyDisaggregateActivity();
        myPA.removeFromMovingPool(this);
        myPA.removeEconomicUnit(this);
        myPA.numberOfEconomicUnitsMovedOutsideTheRegion++;
    }

    public void freeUpFloorspace() {
        if (primaryAndSecondaryLocation[1] != null) {
            primaryAndSecondaryLocation[1].movingOut();
            primaryAndSecondaryLocation[1] = null;
        }
        if (primaryAndSecondaryLocation[0] != null) {
            primaryAndSecondaryLocation[0].movingOut();
            primaryAndSecondaryLocation[0] = null;
        } 
    }

    public AbstractTAZ getHomeZone() {
        if (primaryAndSecondaryLocation[0] == null) return null;
        return primaryAndSecondaryLocation[0].isLocatedWithin.getMyTAZ();
    }

    public AbstractTAZ getSecondaryZone() {
        if (primaryAndSecondaryLocation[1] == null) return null;
        return primaryAndSecondaryLocation[1].isLocatedWithin.getMyTAZ();
    }

    public GridCell getHomeGridCell() {
        if (primaryAndSecondaryLocation[0] == null) return null;
        return primaryAndSecondaryLocation[0].isLocatedWithin;
    }

    public GridCell.FloorspaceChunk getPrimaryLocation() {
        return primaryAndSecondaryLocation[0];
    }

    public GridCell.FloorspaceChunk getSecondaryLocation() {
        return primaryAndSecondaryLocation[1];
    }

    /**
     * This keeps track of the locations where the EconomicUnit "is".  The home base.  There can be up to
     * two home bases (e.g. for households, a home and a vacation property).
     * @supplierCardinality 0..2
     * @associates <{com.pb.despair.model.GridCell}>
     * @clientCardinality 0..*
     * @associationAsClass FloorspaceChunk
     * @undirected
     */
    protected GridCell.FloorspaceChunk[] primaryAndSecondaryLocation = new GridCell.FloorspaceChunk[2];

    /**  */
   // protected ConsumptionFunction myConsumptionFunction;

    /** @associates <{ProductionFunction}>*/
  //  protected ProductionFunction myProductionFunction;

    public EconomicUnit() {
        DisaggregateActivity myTypeOfThing = getMyDisaggregateActivity();
        if (myTypeOfThing == null) {
            throw new Error("Economic unit " + this + " doesn't have an associated DisaggregateActivity");
        }
    }
}


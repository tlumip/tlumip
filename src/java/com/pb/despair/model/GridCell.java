package com.pb.despair.model;

import java.util.Iterator;
import java.util.Vector;

/**
 * A class that represents a bit of space in a TAZ zones.  To ensure that the two-way association between
 * GridCells and their TAZ's are maintained, the constructors for GridCells automatically add themselves
 * in to the associated TAZ.
 * @see <{com.pb.despair.model.AbstractTAZ}>
 * @author John Abraham
 */
public class GridCell implements UnitOfLand {
    public static final double cellVacancyUtilityCoefficient = -1.0;
    public static final double zoneVacancyUtilityCoefficient = -1.0;
    public static final double rentPerAcreCoefficientOnCurrentDevelopment = 0.002;
    public static final double ageCoefficientToKeep = -0.2;
    public static final double keepConstant = 12.0;
    public static final double demolitionNotAddConstant = -5.0;
    public static final double profitPerAcreCoefficientOnNewDevelopment = .002;

    public GridCell(AbstractTAZ myTAZ, float amountOfLand, DevelopmentTypeInterface currentDevelopment,
        float amountOfDevelopedSpace, float age, ZoningSchemeInterface zs) {
            this.myTAZ = myTAZ;
            this.amountOfLand = amountOfLand;
            this.currentDevelopment = currentDevelopment;
            this.amountOfDevelopment = amountOfDevelopedSpace;
            this.vacantDevelopment = amountOfDevelopedSpace;
            // don't set the zoning to allow this development type, because some gridcells may be developed with
            // a no-longer permitted use.  Zoning refers to future development allowed, not current development
            myTAZ.addNewGridCell(this);
            this.age = age;
            this.zoningScheme = zs;
    }

    public double getPortionVacant() { return amountOfDevelopment != 0 ? vacantDevelopment / amountOfDevelopment : -1; }

    /**
     * @supplierCardinality 1
     * @associates <{com.pb.despair.model.AbstractTAZ}>
     */
    private AbstractTAZ myTAZ;

    /** @associates <{com.pb.despair.model.GridCell.FloorspaceChunk}> */
    protected Vector myFloorspaceChunks = new Vector(3);

    /**
     * The list of DevelopmentTypes that are allowed in the cell
     * @associates <{DevelopmentType}>
     * label zoning
     * @supplierCardinality *
     */
    protected ZoningSchemeInterface zoningScheme = null;

    /**
     * The current development type in the cell
     * label current development
     */
    protected DevelopmentTypeInterface currentDevelopment;

    /**
     * @supplierRole current use
     * @clientCardinality *
     */
    //protected ProductionActivity currentUse;
    float amountOfDevelopment;
    float amountOfLand;
    float vacantDevelopment;
    float age;

    /** This function returns the current development type */
    public com.pb.despair.model.DevelopmentTypeInterface getCurrentDevelopment() { return currentDevelopment; }

    /** This function finds a synthetic lot in the grid cell for the activity */
    public FloorspaceChunk giveMeSomeSpace(EconomicUnit forWhom) {
        float spaceNeeded = (float)forWhom.spaceNeeded(currentDevelopment);
// allow negative space        if (vacantDevelopment >= spaceNeeded) {
            FloorspaceChunk alot = new FloorspaceChunk(this);
            alot.setSize(spaceNeeded);
            vacantDevelopment -= spaceNeeded;
            myFloorspaceChunks.add(alot);
            alot.myTenant = forWhom;
            myTAZ.changeSpaceUse(currentDevelopment,spaceNeeded);
            return alot;

/* allow negative space       } else {
            return null;
        } */
    }

    private void tenantLeft(FloorspaceChunk lot) {
        vacantDevelopment += lot.getSize();
        if (myFloorspaceChunks.contains(lot)) {
            myTAZ.changeSpaceUse(currentDevelopment,-lot.getSize());
            myFloorspaceChunks.remove(lot);
        } else
            throw new Error("tried to remove a Lot from the wrong GridCell");
    }

    public float getVacantDevelopment() { return vacantDevelopment; }

    public AbstractTAZ getMyTAZ() {
        return myTAZ;
    }

    public String toString() { return "GridCell in TAZ " + myTAZ; };

    public float getAmountOfDevelopment() { return amountOfDevelopment; }

    private class DevelopmentAlternative implements Alternative {
        DevelopmentTypeInterface dt;
        boolean moreOfTheSame; // adding more floorspace of the same type
        double sizeTerm; // size term for change alternatives.

        DevelopmentAlternative(DevelopmentTypeInterface dt, boolean moreOfTheSame) {
            this.dt = dt;
            this.moreOfTheSame = moreOfTheSame;
        }

        public double getUtility(double higherLevelDispersionParameter) {
            AbstractTAZ.PriceVacancy priceVacancy = myTAZ.getPriceVacancySize(dt);
            // build up utility from parts.
            // first part is tendency to not develop when there's a lot of vacancy of that type
            double result = priceVacancy.getTotalSize() == 0 ? 0 :
                priceVacancy.getVacancy() / priceVacancy.getTotalSize() * zoneVacancyUtilityCoefficient;
                // second part is potential profits -- (price per square foot - cost per square foot)*number of square feet
                if (moreOfTheSame) {
                    result += rentPerAcreCoefficientOnCurrentDevelopment * priceVacancy.getPrice() *
                        amountOfDevelopment / amountOfLand;
                        double moreSpaceAvailable = zoningScheme.getAllowedFAR(dt) *
                        amountOfLand - amountOfDevelopment;
                        result += profitPerAcreCoefficientOnNewDevelopment * (priceVacancy.getPrice() - dt.getConstructionCost()) *
                        moreSpaceAvailable / amountOfLand;
            } else {
                result += profitPerAcreCoefficientOnNewDevelopment * (priceVacancy.getPrice() - dt.getConstructionCost()) *
                    zoningScheme.getAllowedFAR(dt);
            }
            // third part is demolition being rather unattractive generally.
            if (!moreOfTheSame) result += demolitionNotAddConstant;
            result += sizeTerm;
            return result;

           /* still need alternative specific constant, activity in the construction sector, growth rate in activities that use the development type
          region wide vacancies, interest rate... */
        }
    };


    private class NoChangeAlternative implements Alternative {
        public double getUtility(double higherLevelDispersionParameter) {
            AbstractTAZ.PriceVacancy priceVacancy = myTAZ.getPriceVacancySize(currentDevelopment);
            double portionVacant = getPortionVacant();
            double utility;
            if (portionVacant < 0 && priceVacancy.getTotalSize()>0) utility = priceVacancy.getVacancy() / priceVacancy.getTotalSize() * cellVacancyUtilityCoefficient;
            // if there's no vacancy information for the cell use the zone average number again
            else
                utility = portionVacant * cellVacancyUtilityCoefficient;
            // message #1.1.1 to aTAZ:com.pb.despair.TAZ
            // aTAZ.getPriceVacancySize(com.pb.despair.TAZ.PriceVacancy, com.pb.despair.ld.DevelopmentType, float);
            if (priceVacancy.getTotalSize()>0) utility += priceVacancy.getVacancy() / priceVacancy.getTotalSize()*zoneVacancyUtilityCoefficient;
            utility +=  priceVacancy.getPrice() *
                rentPerAcreCoefficientOnCurrentDevelopment * amountOfDevelopment / amountOfLand + age *
                ageCoefficientToKeep + keepConstant;
                if (Double.isNaN(utility)) {
                    System.out.println("hmm, no change alternative had NAN as utility...");
                    throw new Error("NAN for utility of no-change development alternative");
            }
            return utility;
        }
    };


    public void makeRedevelopmentDecision(double elapsedTime) {
        if (zoningScheme.size() > 0) // if we're not zoned for anything then can't do anything at all.
        {
            age += elapsedTime;
            LogitModel developChoice = new LogitModel();
            Alternative noChange = new NoChangeAlternative();

            /*{
                public double getUtility() {
                    AbstractTAZ.PriceVacancy priceVacancy = new AbstractTAZ.PriceVacancy();
                    myTAZ.getPriceVacancySize(priceVacancy, currentDevelopment);
                    double utility= getPortionVacant() * cellVacancyUtilityCoefficient +
                        // message #1.1.1 to aTAZ:com.pb.despair.TAZ
                        // aTAZ.getPriceVacancySize(com.pb.despair.TAZ.PriceVacancy, com.pb.despair.ld.DevelopmentType, float);
                        priceVacancy.vacancy / priceVacancy.totalSize * zoneVacancyUtilityCoefficient +
                        priceVacancy.price * rentPerAcreCoefficientOnCurrentDevelopment + age * ageCoefficientToKeep + keepConstant;
                    if (Double.isNaN(utility)) {
                              System.out.println("hmm, no change alternative had NAN as utility...");
                              throw new Error("NAN for utility of no-change development alternative");
                    }
                    return utility ;
                }
            };*/

            developChoice.addAlternative(noChange);
            LogitModel changeOptions = new LogitModel();
            Iterator it = zoningScheme.allowedDevelopmentTypes();
            while (it.hasNext()) {
                DevelopmentTypeInterface whatWeCouldBuild = (DevelopmentTypeInterface)it.next();
                Alternative aDevelopmentType;
                if (whatWeCouldBuild.equals(currentDevelopment)) {
                    aDevelopmentType = new DevelopmentAlternative(whatWeCouldBuild, true); // building more of the same
                    changeOptions.addAlternative(aDevelopmentType);
                }
                aDevelopmentType = new DevelopmentAlternative(whatWeCouldBuild, false); // rebuilding entirely
                changeOptions.addAlternative(aDevelopmentType);
            }
            // if, through the effect of the age of development, we virtually eliminate the possibility that
            // redevelopment will occur more than once in any of the time periods considered by this function,
            // then the "change" alternatives can be scaled with a size term reflecting the length of time considered.
            // This is because if twice as much time has elapsed then the choice to redevelop the land is twice as likely
            // to be made.
            changeOptions.setConstantUtility(Math.log(elapsedTime) / developChoice.getDispersionParameter());
            developChoice.addAlternative(changeOptions);
            Alternative a;
            try {
                a = developChoice.monteCarloElementalChoice();
            } catch (NoAlternativeAvailable e) {
                throw new Error("no reasonable development choices available for " + this);
            } catch (ChoiceModelOverflowException e) {
                e.printStackTrace();
                throw new Error("no reasonable development choices available for " + this);
            }
            if (a instanceof DevelopmentAlternative) {
                DevelopmentAlternative newType = ((DevelopmentAlternative)a);
                if (newType.moreOfTheSame) {
                    developMore();
                } else {
                    evictTenants();
                    redevelopAs(newType.dt);
                }
            }
        }
    }

    private void developMore() {
        LogitModel amountOfDevelopmentChoice = new LogitModel();
        double maxDevelopment = zoningScheme.getAllowedFAR(currentDevelopment) * amountOfLand;
        if (amountOfDevelopment >= maxDevelopment) return;
        for (double amount = amountOfDevelopment; amount <= maxDevelopment;
            amount += (maxDevelopment - amountOfDevelopment) / 100) {
                amountOfDevelopmentChoice.addAlternative(
                    new DevelopmentAmountAlternative(amount, 0));
                // amount of development is based on cost of construction vs tenants' willingness-to-pay for land vs. buildings.
                // that's going to be a bit hard to implement here.  We could just have to cost of construction going up with
                // FAR and the price per square foot going down with FAR.  Do it later...
        }
        DevelopmentAmountAlternative a;
        try {
            a = (DevelopmentAmountAlternative)amountOfDevelopmentChoice.monteCarloChoice();
        } catch (NoAlternativeAvailable e) {
            throw new Error("no additional development possible in " + this);
        } catch (ChoiceModelOverflowException e) {
            e.printStackTrace();
            throw new Error("no additional development possible in " + this);
        }
        age = (age * (float) amountOfDevelopment) / (float) a.amount; // blend the ages, age is now average age.
        vacantDevelopment += (a.amount - amountOfDevelopment);
        myTAZ.changeSpaceQuantity(currentDevelopment,(float) (a.amount-amountOfDevelopment));
        amountOfDevelopment = (float)a.amount;
    }

    private static class DevelopmentAmountAlternative extends FixedUtilityAlternative {
        double amount;

        DevelopmentAmountAlternative(double amount, double utility) {
            super(utility);
            this.amount = amount;
        }
    };


    /** preconditions evicted tenants */
    private void redevelopAs(DevelopmentTypeInterface newType) {
        myTAZ.changeSpaceQuantity(currentDevelopment,-amountOfDevelopment);
        currentDevelopment = newType;
        double maxDevelopment = zoningScheme.getAllowedFAR(newType) * amountOfLand;
        if (maxDevelopment <=0) {
            amountOfDevelopment = 0;
            vacantDevelopment = 0;
        } else {
                  LogitModel amountOfDevelopmentChoice = new LogitModel();
                  for (double amount = 0; amount <= maxDevelopment; amount += maxDevelopment / 100) {
                      amountOfDevelopmentChoice.addAlternative(
                          new DevelopmentAmountAlternative(amount, 0));
                      // amount of development is based on cost of construction vs tenants' willingness-to-pay for land vs. buildings.
                      // that's going to be a bit hard to implement here.  We could just have to cost of construction going up with
                      // FAR and the price per square foot going down with FAR.  Do it later...
                  }
                  DevelopmentAmountAlternative a;
                  try {
                      a = (DevelopmentAmountAlternative)amountOfDevelopmentChoice.monteCarloChoice();
                  } catch (NoAlternativeAvailable e) {
                      throw new Error("Can't seem to develop any space of type " + newType + " in cell " + this);
                  } catch (ChoiceModelOverflowException e) {
                    e.printStackTrace();
                    throw new Error("Can't seem to develop any space of type " + newType + " in cell " + this);
                  }
                  amountOfDevelopment = (float)a.amount;
                  vacantDevelopment = (float)a.amount;
                  myTAZ.changeSpaceQuantity(currentDevelopment,amountOfDevelopment);
        }
        age = 0;
    }

    public void evictTenants() {
        FloorspaceChunk[] homesAndBusinesses = new FloorspaceChunk[myFloorspaceChunks.size()];
        homesAndBusinesses = (FloorspaceChunk[]) myFloorspaceChunks.toArray(homesAndBusinesses);
        for (int i = 0; i < homesAndBusinesses.length; i++) {
            homesAndBusinesses[i].askToMoveOut();
        }
        myFloorspaceChunks.clear();
    }

    public double getAge() { return age; }

    public void setAge(float age) { this.age = age; }

    /**
     * A class that represents the amount of land that an EconomicUnit is using in a GridCell
     * @author J. Abraham
     */
    public class FloorspaceChunk {
        FloorspaceChunk(GridCell c) {
            isLocatedWithin = c;
        }

        /**
         * Somone is leaving this land behind, it needs to be made vacant somehow.  In ODOT this function should go to the
         * GridCell and/or TAZ and add the land to the total vacant land available.
         */
        public void movingOut() {
            if (isLocatedWithin == null) {
                throw new Error("Someone tried to move out twice");
            }
            isLocatedWithin.tenantLeft(this);
            isLocatedWithin = null;
            myTenant = null;
        }

        public float getSize() { return size; }

        public void setSize(float size) { this.size = size; }

        /** @associates <{com.pb.despair.model.GridCell}> */
        protected GridCell isLocatedWithin;
        protected EconomicUnit myTenant = null;
        private float size;

        public String toString() { return "FloorspaceChunk size" + size + " in GridCell " + isLocatedWithin; };

        public void askToMoveOut() {
            if (myTenant != null) {
                myTenant.youHaveToLeave(this);
            }
        }
        /**
         * Returns the isLocatedWithin.
         * @return GridCell
         */
        public GridCell getIsLocatedWithin() {
            return isLocatedWithin;
        }

    }
}

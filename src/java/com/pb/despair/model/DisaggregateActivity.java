
package com.pb.despair.model;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/** A class that represents activities that are microsimulated
 *
 * @see allHouseholds
 * @author John Abraham
 */
public abstract class DisaggregateActivity extends ProductionActivity {

    long numberOfEconomicUnitsMovedOutsideTheRegion=0;
    public DisaggregateActivity(String name, AbstractTAZ[] allZones) {
       super(name,allZones);
    }

    public void addEconomicUnit(EconomicUnit u) {
     // if (myEconomicUnits.contains(u)) throw new Error("Tried to add in a EconomicUnit that was already accounted for");
      myEconomicUnits.add(u);
    }

    public void removeEconomicUnit(EconomicUnit u) {
      myEconomicUnits.remove(u);
    }

    public ConsumptionFunction getConsumptionFunction() {
        return representativeConsumptionFunction;
    }

    public void setRepresentativeConsumptionFunction(ConsumptionFunction consumptionFunction) {
        representativeConsumptionFunction = consumptionFunction;
    }

    public ConsumptionFunction getRepresentativeConsumptionFunction() { return representativeConsumptionFunction; }

    /**
     * @supplierRole representative consumption function 
     */
    private ConsumptionFunction representativeConsumptionFunction;

    public ProductionFunction getRepresentativeProductionFunction() { return representativeProductionFunction; }

    /**
     * @supplierRole representative production function 
     */
    private ProductionFunction representativeProductionFunction;

    public void setRepresentativeProductionFunction(ProductionFunction representativeProductionFunction) {
        this.representativeProductionFunction = representativeProductionFunction;
    }

    public ProductionFunction getProductionFunction() { return representativeProductionFunction; }

    public List getEconomicUnits(){ return Collections.unmodifiableList(myEconomicUnits); }

    /**
     * @link aggregationByValue
     * @associates <{EconomicUnit}>
     */
    private Vector myEconomicUnits=new Vector();
    protected Vector movingPool = new Vector();
    protected Vector secondaryLocationMovingPool = new Vector();

    public boolean movingPoolContains(EconomicUnit u) {
      return movingPool.contains(u);
    }
    public boolean secondaryLocationMovingPoolContains(EconomicUnit u) {
      return secondaryLocationMovingPool.contains(u);
    }
    public void addToMovingPool(EconomicUnit u) {
      	// debug
      	if (movingPool.size()%100 ==0) System.out.println("moving pool size reached "+movingPool.size());
        movingPool.add(u);
    }
    public void removeFromMovingPool(EconomicUnit u) {
      movingPool.remove(u);
    }
    public String reportPools() {
        return "size of Moving Pool:" + movingPool.size() +"\nsize of Secondary Moving Pool:"+secondaryLocationMovingPool.size()+"\nNumberOfHouseholds:"+myEconomicUnits.size()+"\nHouseholds left:"+numberOfEconomicUnitsMovedOutsideTheRegion;
    }
    public void removeFromSecondaryLocationMovingPool(EconomicUnit u) {
      secondaryLocationMovingPool.remove(u);
    }
    public void addToSecondaryLocationMovingPool(EconomicUnit u) {
      secondaryLocationMovingPool.add(u);
    }
} /* end class DisaggregateActivity */

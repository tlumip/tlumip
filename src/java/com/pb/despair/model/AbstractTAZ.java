package com.pb.despair.model;


import com.pb.common.datafile.TableDataSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;

abstract public class AbstractTAZ implements UnitOfLand, Comparable {

    protected static transient Logger logger =
        Logger.getLogger("com.pb.despair.pi");
    
    //this could be much smaller as the max zone number is less than 5000
    private static final int maxZoneNumber=10000;

    protected AbstractTAZ(int i,int userNumber) {
        if (userNumber < 0 || userNumber >= maxZoneNumber) throw new RuntimeException("ZoneUserNumber "+userNumber+" too big, max set at "+maxZoneNumber);
        zoneIndex = i;
        allZones[i] = this;
        allZonesByUserNumber[userNumber] = this;
        priceVacancies = new PriceVacancy[0];
    }

    public static AbstractTAZ[] getAllZones() {
      AbstractTAZ[] otherZoneArray = new AbstractTAZ[allZones.length];
      System.arraycopy(allZones,0,otherZoneArray,0,allZones.length);
      return otherZoneArray;
    }
    
    public static AbstractTAZ getZone(int index) {
        return allZones[index];
    }

    public void writeGridCells(TableDataSet gtab) {
       Iterator it = myGridCells.iterator();
       int userID = getZoneUserNumber();
       while (it.hasNext()) {
           //TODO change this so that it uses the pb.common.datafile.TableDataSet class instead of the
           //TODO Borland tabledataset.
//          gtab.insertRow(false);
//          GridCell gc = (GridCell) it.next();
//          gtab.setInt("TAZ",userID);
//          gtab.setString("DevelopmentType",gc.currentDevelopment.getName());
//          gtab.setFloat("AmountOfDevelopment",gc.amountOfDevelopment);
//          gtab.setFloat("AmountOfLand",gc.amountOfLand);
//          gtab.setInt("Age",(int) gc.age);
//          gtab.setString("ZoningScheme",gc.zoningScheme.getName());
//          gtab.post();
      }
          
    }

    public void addNewGridCell(GridCell c) {
        myGridCells.add(c);
        changeSpaceQuantity(c.currentDevelopment, c.amountOfDevelopment);
        changeSpaceUse(c.currentDevelopment, c.amountOfDevelopment - c.vacantDevelopment);
    }

    private void checkSpaceAccountingArray(int dtID) {
    	if (priceVacancies.length <= dtID) {
    		PriceVacancy[] oldPriceVacancies = priceVacancies;
    		priceVacancies = new PriceVacancy[dtID+1];
    		System.arraycopy(oldPriceVacancies, 0, priceVacancies, 0, oldPriceVacancies.length);
    	}
    	if(priceVacancies[dtID] ==null) priceVacancies[dtID] = new PriceVacancy();
    }


    /**
     * Goes through the grid cells that have the development type and finds out how many vacant parcels
     * or vacant space there is.
     */
    public PriceVacancy getPriceVacancySize(DevelopmentTypeInterface dt) {
    	int dtID = dt.getID();
    	checkSpaceAccountingArray(dtID);
        return priceVacancies[dtID] ;
    }
        
    void updateVacancies(DevelopmentTypeInterface dt) {
    	int dtID = dt.getID();
    	checkSpaceAccountingArray(dtID);
        Iterator it = myGridCells.iterator();
        double oldVacancy = priceVacancies[dtID].vacancy;
        double oldSize = priceVacancies[dtID].totalSize;
        priceVacancies[dtID].vacancy = 0;
        priceVacancies[dtID].totalSize=0;
        while (it.hasNext()) {
            GridCell gc = (GridCell)it.next();
            if (gc.getCurrentDevelopment().equals(dt)) {
                priceVacancies[dtID].vacancy += gc.getVacantDevelopment();
                priceVacancies[dtID].totalSize += gc.getAmountOfDevelopment();
            }
        }
        if (Math.abs(priceVacancies[dtID].vacancy - oldVacancy) > .01) logger.warning("Warning -- vacancy of "+dt+" changed in zone "+this);
        if (Math.abs(priceVacancies[dtID].totalSize - oldSize)> 0.01) logger.warning("Warning -- size of "+dt+" changed in zone "+this);
    }

/*    static private double calcAveragePrice(DevelopmentTypeInterface dt) {
        double retval;
        Double thePrice = (Double)spacePrices.get(dt);
        if (thePrice == null) {
            double avg = 0;
            int numFound = 0;
            for (int z = 0; z < allZones.length; z++) {
                Double priceElsewhere = (Double)allZones[z].spacePrices.get(dt);
                if (priceElsewhere != null) {
                    avg = (avg * numFound + priceElsewhere.doubleValue()) / (numFound + 1);
                    numFound++;
                }
            }
            retval = avg;
        } else {
            retval = thePrice.doubleValue();
        }
        return retval;
    } */

    public GridCell.FloorspaceChunk assignAGridCell(EconomicUnit forMe, DevelopmentTypeInterface dt) throws CantFindRoomException {
        GridCell.FloorspaceChunk aLot = null;
        boolean found = false;
        PriceVacancy pv = getPriceVacancySize(dt);
        double gridCellSelectorVacancy = Math.random()*pv.vacancy;
        double accumulatedVacancy = 0;
        if (gridCellSelectorVacancy <=0) throw new CantFindRoomException("Zero vacancy for "+dt+" in "+this);
        Iterator gci = myGridCells.iterator();
        while (gci.hasNext()) {
            GridCell gc = (GridCell) gci.next();
            if (gc.currentDevelopment.equals(dt)) {
                accumulatedVacancy += gc.vacantDevelopment;
                if (accumulatedVacancy > gridCellSelectorVacancy) {
                    aLot = gc.giveMeSomeSpace(forMe);
                    if (aLot!=null) break;
                }
            }
        }
        if (aLot == null) {
            logger.warning("Cant find room in TAZ "+this);
   //         PriceVacancy pvbase = new PriceVacancy();
            logger.warning(forMe+ " needs "+forMe.spaceNeeded(dt)+" of "+dt);
            throw new CantFindRoomException("Can't find a suitable grid cell in TAZ " + this);
        }
        return aLot;
    }

    static public AbstractTAZ findZone(UnitOfLand l) {
        if (l instanceof AbstractTAZ) {
            return (AbstractTAZ)l;
        } else {
            if (l instanceof GridCell) {
                return ((GridCell)l).getMyTAZ();
            }
            return null;
        }
    }

    public int getZoneIndex() { return zoneIndex; }

    public abstract int getZoneUserNumber();

    public String toString() { return "TAZ:" + getZoneUserNumber(); };

    public int hashCode() { return zoneIndex; };

    public void gridCellDevelopmentUpdate(double elapsedTime) {
        // message #1.1 to eachGridCell:com.pb.despair.ld.GridCell
        // eachGridCell.makeRedevelopmentDecision();
          Iterator it = myGridCells.iterator();
          while (it.hasNext())
          {
               GridCell g = (GridCell) it.next();
               g.makeRedevelopmentDecision(elapsedTime);
          }
    }

    public static void createTazArray(int numZones) {
        allZones = new AbstractTAZ[numZones];
        allZonesByUserNumber = new AbstractTAZ[maxZoneNumber];
    }

    /** Update the price based on the vacancy rate. */
    public void updatePrice(DevelopmentTypeInterface dt, double newPrice) {
    	int dtID = dt.getID();
    	checkSpaceAccountingArray(dtID);
    	priceVacancies[dtID].price = newPrice;
    }

    public void changeSpaceUse(DevelopmentTypeInterface dt, float spaceChange) {
    	int dtID = dt.getID();
    	checkSpaceAccountingArray(dtID);
    	priceVacancies[dtID].vacancy-=spaceChange;
    }

    public void changeSpaceQuantity(DevelopmentTypeInterface dt, double spaceChange) {
    	int dtID = dt.getID();
    	checkSpaceAccountingArray(dtID);
    	priceVacancies[dtID].totalSize+=spaceChange;
    	priceVacancies[dtID].vacancy+=spaceChange;
    }


    public int compareTo(Object par1){
        if (! (par1 instanceof AbstractTAZ)) throw new ClassCastException("comparing non-TAZ to TAZ");
        AbstractTAZ other = (AbstractTAZ) par1;
        if (other.zoneIndex<zoneIndex ) return 1;
        if (other.zoneIndex == zoneIndex ) return 0;
        return -1;
    }

    public static AbstractTAZ findZoneByUserNumber(int zoneUserNumber) {
        if (zoneUserNumber <0 || zoneUserNumber >= maxZoneNumber) return null;
        return allZonesByUserNumber[zoneUserNumber];
/*        for (int z = 0; z<allZones.length; z++) {
            if (allZones[z].getZoneUserNumber() == zoneUserNumber) return allZones[z];
        }
        return null; */
    }

    /**
     * @associates <{GridCell}>
     * @supplierCardinality 1..*
     * @link aggregationByValue
     */
 //   protected HashSet myGridCells = new HashSet();
   protected ArrayList myGridCells = new ArrayList();
    private static AbstractTAZ[] allZones;
    private static AbstractTAZ[] allZonesByUserNumber;
    public final int zoneIndex;
    private PriceVacancy[] priceVacancies;
    static protected Random theRandom = new java.util.Random();

    /**
     * Goes through the grid cells that have the development type and finds out how many vacant parcels
     * or vacant space there is.
     */
    public static class PriceVacancy {
        private double price;
        private double vacancy;
        private double totalSize;

        public PriceVacancy() {
        };
        public String toString() {return "price:"+price+" vacnt:"+vacancy+" size:"+totalSize;}
        /**
         * Returns the price.
         * @return double
         */
        public double getPrice() {
            return price;
        }

        /**
         * Returns the totalSize.
         * @return double
         */
        public double getTotalSize() {
            return totalSize;
        }

        /**
         * Returns the vacancy.
         * @return double
         */
        public double getVacancy() {
            return vacancy;
        }

    };


    public static class CantFindRoomException extends Exception {
      public CantFindRoomException(String s) {
        super(s);
      }
    }
}

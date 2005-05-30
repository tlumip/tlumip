package com.pb.despair.ts.old;

import com.pb.despair.model.Mode;
import com.pb.models.pecas.UnitOfLand;
import com.pb.models.pecas.TravelUtilityCalculatorInterface;
import com.pb.models.pecas.AbstractTAZ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
//import com.pb.despair.pt.Mode;

/**
 * This is the class that stores the information on the attributes of travel by a mode over a congested network.  Those attributes are stored in its array of ZPAttributes.
 *  
 *  Responsibilities:
 *  C:1 - once the loaded network is established, it has to send a findMinimumPath message to each of its zone pairs.  It could do this all at once, or it
 *  could instead keep a cache of recently requested zone pairs, and recalculate others as they are needed.
 * 
 * E:1.1.5? - the log-sum of the zone pair disutilities needs to be calculated for certain trip types.
 * @author your_name_here
 */
public class TravelCharacteristicMatrix  implements SummaryOfTravelConditions {
    public TravelCharacteristicMatrix(UnitOfLand[] originsAndDestinationsP, Mode m, AssignmentPeriod ap, TravelUtilityCalculatorInterface tp) {
        originsAndDestinations = new ArrayList(originsAndDestinationsP.length);
        for (int i =0; i<originsAndDestinationsP.length;i++) {
            originsAndDestinations.add(originsAndDestinationsP[i]);
        }
        Collections.sort(originsAndDestinations);
        this.forMode = m;
        this.myAssignmentPeriod = ap;
        this.myTravelPreferences = tp;
        theMatrix = new ZPAttribute[originsAndDestinations.size()][originsAndDestinations.size()];
    }

  public Collection getOriginsAndDestinations(){ return originsAndDestinations; }

 // public void setOriginsAndDestinations(UnitOfLand[] originsAndDestinations){ this.originsAndDestinations = originsAndDestinations; }

  /**
   * If the load has changed on the AssignmentPeriod, the TravelCharacteristicMatrix needs to update itself.  It does this by asking each of its ZPAttributes to update themselves.  Note that the zpattributes might be stored directly in float arrays in this class instead of in instances of ZPAttribute (to reduce the memory and performance overheads of object creation and deletion), so "asking the ZPAttributes to findMinimumPath" might be a call to a static method, or might involve working with an already constructed ZPAttribute.
   * stereotype update
   */
  public void refreshTravelAttributes() {
  }

 private ZPAttribute getExistingZPData(UnitOfLand o, UnitOfLand d) {
        int onum = lookUpMatrixIndex(o);
        int dnum = lookUpMatrixIndex(d);
	    ZPAttribute zp= theMatrix[onum][dnum];
	    return zp;
	}

  private int lookUpMatrixIndex(UnitOfLand l) {
        int i = Collections.binarySearch(originsAndDestinations,l);
        if (i<0) {
            AbstractTAZ taz = AbstractTAZ.findZone(l);
            i = Collections.binarySearch(originsAndDestinations,taz);
        }
        if (i<0) throw new Error("No data for unit of land "+l+" in "+this);
        return i;
  }


  public void setZPAttribute(UnitOfLand o, UnitOfLand d, ZPAttribute a) {
    int onum = lookUpMatrixIndex(o);
    int dnum = lookUpMatrixIndex(d);
    theMatrix[onum][dnum] = a;
  }

	private FatZPAttribute doRouteChoiceForPair(UnitOfLand o, UnitOfLand d) {
	    FatZPAttribute zp = new FatZPAttribute(myTravelPreferences, forMode, myAssignmentPeriod, o, d);
		zp.findMinimumPath();
		return zp;
	}

  /**
   * This function is used to get the attributes of travel
   * between two zones.  The TravelCharacteristicMatrix could retrieve the attributes from its array, or it could calculate them on the fly.  Or it could keep a cache.  Lots of flexibility is established by putting the function here.
   */
  public ZPAttribute retrieveZPAttribute(AbstractTAZ o, AbstractTAZ d, boolean useRouteChoice) {
	if (!useRouteChoice) {
	    // eventually will go to the database
	    ZPAttribute zp= getExistingZPData(o,d);
	    return zp;
	} else {
	  FatZPAttribute zp = doRouteChoiceForPair(o,d);
	  return new ZPAttribute(zp);
	}
  }

  /**
   * Just makes a new FatZPAttribute after retrieving the ZPAttribute
   */
  public FatZPAttribute retrieveFatZPAttribute(UnitOfLand o, UnitOfLand d, boolean useRouteChoice) {
    if (!useRouteChoice)
    {
	   ZPAttribute thin = getExistingZPData(o,d);
		FatZPAttribute r = new FatZPAttribute(thin);
		r.origin = o;
		r.destination=o;
		r.myTravelCharacteristicMatrix = this;
		r.myTravelPreferences = myTravelPreferences;
		r.myMode = forMode;
		r.myAssignmentPeriod = myAssignmentPeriod;
		return r;
    } else {
	  FatZPAttribute zp = doRouteChoiceForPair(o,d);
	  return zp;
	}
  }

  /**
   * This function is used to get the attributes of travel
   * between two zones for someone with certain preferences.  This will find the person a specific path on the network between two zones given their travel conditions.
   *
   *  There is an obvious need for cross-functionality between this method and retrieveZPAttribute
   */
  public FatZPAttribute calcFatZPAttribute(UnitOfLand o, UnitOfLand d, TravelUtilityCalculatorInterface tp) {
         FatZPAttribute zp = new FatZPAttribute(tp, forMode, myAssignmentPeriod, o, d);
         zp.findMinimumPath();
         return zp;
  }

  /**
   * "What is theMatrix?"  theMatrix is the array of zpAttributes.
   * associates <{com.pb.despair.ts.ZPAttribute}>
   * @link aggregation
   * supplierCardinality n^2
   */
  protected ZPAttribute[][] theMatrix;
  protected Mode forMode;
  protected AssignmentPeriod myAssignmentPeriod;
  protected TravelUtilityCalculatorInterface myTravelPreferences;

  /**
   * The pointers to the originsAndDestinations that make up the rows and columns of the TravelCharacteristicMatirx.
   */
  private final ArrayList originsAndDestinations;
    public String toString() {return "TravelCharacteristicMatrix for "+forMode+" ,"+myAssignmentPeriod; };
	
}

package com.pb.despair.pi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;

import com.pb.common.matrix.Matrix;
import com.pb.despair.model.ChoiceModelOverflowException;
import com.pb.despair.model.AbstractCommodity;
import com.pb.despair.model.AbstractTAZ;
import com.pb.despair.model.OverflowException;

/**
 * These are the thingies (goods or services) that need to be transported on the network.  E.g. "labour", "manufactured
 * appliances", "personal grooming services", etc.  Not really commodities in the economic sense because they are
 * heterogeneous in the model.
 *
 * @author John Abraham
 */
public class Commodity extends AbstractCommodity {

    
    private boolean isFloorspaceCommodity = false;
    private static Logger logger = Logger.getLogger("com.pb.despair.pi");

    double oldMeritMeasure = 0.0;
    
    // reactivate scalingAdjustmentFactor if you need bigger step sizes for some stubborn commodities
    double scalingAdjustmentFactor = 1.0;
    double compositeMeritMeasureWeighting = 1.0;
    private double expectedPrice;
    /**
     * @associates <{BuyingZUtility}>
     * @supplierCardinality 1..*
     */
    Hashtable buyingTazZUtilities = new Hashtable();
    Hashtable sellingTazZUtilities = new Hashtable();
    public final char exchangeType;

    /** @associates <{Exchange}> */
    private ArrayList allExchanges = new ArrayList();
    private double buyingUtilitySizeCoefficient;
    private double sellingUtilitySizeCoefficient;
    private double buyingUtilityPriceCoefficient;
    private double sellingUtilityPriceCoefficient;
    private double buyingUtilityTransportCoefficient;
    private double sellingUtilityTransportCoefficient;
    private boolean flowsValid = false;

    static int numExchangeNotFoundErrors = 0;
    //private boolean pricesAndConditionsFixed = false;
    private double defaultBuyingDispersionParameter;
    private double defaultSellingDispersionParameter;

    static SingleParameterFunction zeroFunction = new SingleParameterFunction() {
        public double evaluate(double x) {
            return 0;
        }

        public double derivative(double x) {
            return 0;
        }
    };

    private Commodity(String name, char exchangeTypePar) {
        super(name);
        this.exchangeType = exchangeTypePar;
        if (exchangeType != 'c' && exchangeType != 'p' && exchangeType != 'a' && exchangeType != 's' && exchangeType != 'n') {
            throw new Error("Commodity " + name + " has invalid exchange type" + exchangeTypePar +
                    ": only c,p,a,n or s are allowed");
        }
    }
        
	public double[][] fixPricesAndConditionsAtNewValues() throws OverflowException {
        AbstractTAZ[] zones = AbstractTAZ.getAllZones();
        
        
       //  remove this debugging code of May 27 2004
       // if (this.name.equals("CONSTRUCTION")) {
            // we'll write out every single price to get to the bottom of this!
       //     StringBuffer pricesString = new StringBuffer();
       //     for (int z=0;z<zones.length;z++) {
       //         Exchange x = getExchange(z);
       //         if (x!=null) {
       //             pricesString.append(x.getPrice()+" ");
       //         }
       //     }
       //     logger.info(this+ " prices are now fixed at:"+pricesString);
       // }
        
        double[][] compositeUtilities = new double[2][zones.length]; //hold the CUBuys[0][] and CUSells[1][] for each zone

        //first do the CUBuy calculations
        boolean selling = false;
        for (int z = 0; z < zones.length; z++) {
            AbstractTAZ t = zones[z];

            //get the CUBuy zone utility object out of the CommodityZUtility hashtable
            CommodityZUtility czu = retrieveCommodityZUtility(t,selling);
            try {
                //This will return CUBuyc,z (or the BUc,z,k) which
                //is the utility of buying a commodity 'c' consumed in zone 'z'
                czu.setPricesFixed(true);
                compositeUtilities[0][z]= czu.getUtility(1);
            } catch (ChoiceModelOverflowException e) {
                throw new OverflowException(e.toString());
            }
        }

        //then do the CUSell calculations
        selling = true;
        for (int z = 0; z < zones.length; z++) {
            AbstractTAZ t = zones[z];
            //get the CUSell zone utility object out of the CommodityZUtility hashtable
            CommodityZUtility czu = retrieveCommodityZUtility(t,selling);
            try {
                //This will return CUSellc,z (or the SUc,z,k ) which
                //is the utility of selling a commodity 'c' produced in zone 'z'
                czu.setPricesFixed(true);
                compositeUtilities[1][z]=czu.getUtility(1);
            } catch (ChoiceModelOverflowException e) {
                throw new OverflowException(e.toString());
            }
        }
		return compositeUtilities;
	}
	
	public void unfixPricesAndConditions() {
        
        AbstractTAZ[] zones = AbstractTAZ.getAllZones();
        for (int s = 0; s < 2; s++) {
            boolean selling = true;
            if (s == 1) selling = false;
            for (int z = 0; z < zones.length; z++) {
                //get the CUSell or CUBuy utility calculator for each zone out of the CommodityZUtility hashtable
                CommodityZUtility czu = retrieveCommodityZUtility((TAZ) zones[z],selling);
                    czu.setPricesFixed(false);
            }
        }
	}

//Getters and Setters for the Commodity Parameters used in Utility calculations
//We could pass doubles instead of Parameter objects.
    public double getDefaultSellingDispersionParameter() {
        return defaultSellingDispersionParameter;
    }

    public double getDefaultBuyingDispersionParameter() {
        return defaultBuyingDispersionParameter;
    }

    public void setDefaultBuyingDispersionParameter(double defaultBuyingDispersionParameter) {
        this.defaultBuyingDispersionParameter = defaultBuyingDispersionParameter;
    }

    public void setDefaultSellingDispersionParameter(double defaultSellingDispersionParameter) {
        this.defaultSellingDispersionParameter = defaultSellingDispersionParameter;
    }

    public Hashtable getBuyingTazZUtilities() {
        return buyingTazZUtilities;
    }

    public Hashtable getSellingTazZUtilities() {
        return sellingTazZUtilities;
    }

    public static Commodity createOrRetrieveCommodity(String name, char exchangeTypePar) {
        Commodity c = retrieveCommodity(name);
        if (c == null) {
            c = new Commodity(name, exchangeTypePar);
        }
        return c;
    }

    public static Commodity retrieveCommodity(String name) {
        Commodity commodity = (Commodity)allCommoditiesHashmap.get(name);
        if (commodity == null) {
            return null;
        }
        else {
            return commodity;
        }
    }
    
	/**
     * This gets the ZUtility for a commodity in a zone, either the selling ZUtility or the buying ZUtility.
     *
     * @param t       the TAZ to get the buying or selling utility of
     * @param selling if true, get the selling utility.  Otherwise get the buying utility
     */
    public double calcZUtility(AbstractTAZ t, boolean selling) throws OverflowException {
        try {
            CommodityZUtility czu = retrieveCommodityZUtility(t, selling);
            return czu.getUtility(1);
        } catch (ChoiceModelOverflowException e) {
            throw new OverflowException(e.toString());
        }
    }


    public CommodityZUtility retrieveCommodityZUtility(AbstractTAZ t, boolean selling) {
        Hashtable ht;
        if (selling)
            ht = sellingTazZUtilities;
        else
            ht = buyingTazZUtilities;
        CommodityZUtility czu = (CommodityZUtility) ht.get(t);
        return czu;
    }
    
    public CommodityZUtility retrieveCommodityZUtility(int zoneNumber, boolean selling) {
        TAZ t = (TAZ) TAZ.findZoneByUserNumber(zoneNumber);
        return retrieveCommodityZUtility(t,selling);
    }

	/* This method is called by the Exchange constructor to add an Exchange
    *  to this Commodity's list of Exchanges
    */
    public void addExchange(Exchange ex) {
        allExchanges.add(ex);
    }

    public void addSellingZUtility(CommodityZUtility czu) {
        sellingTazZUtilities.put(czu.getTaz(), czu);
    }

    public void addBuyingZUtility(CommodityZUtility czu) {
        buyingTazZUtilities.put(czu.getTaz(), czu);
    }


    public List getAllExchanges() {
        return allExchanges;
    }

    public Exchange getExchange(int tazIndex) {
        return (Exchange) allExchanges.get(tazIndex);
    }

    public void setBuyingUtilityCoefficients(double size, double price, double transport) {
        buyingUtilitySizeCoefficient = size;
        buyingUtilityPriceCoefficient = price;
        buyingUtilityTransportCoefficient = transport;
    }

    public void setSellingUtilityCoefficients(double size, double price, double transport) {
        sellingUtilitySizeCoefficient = size;
        sellingUtilityPriceCoefficient = price;
        sellingUtilityTransportCoefficient = transport;
    }

    public double getBuyingUtilitySizeCoefficient() {
        return buyingUtilitySizeCoefficient;
    }

	public double getSellingUtilitySizeCoefficient() {
        return sellingUtilitySizeCoefficient;
    }

	public double getBuyingUtilityPriceCoefficient() {
        return buyingUtilityPriceCoefficient;
    }

	public double getSellingUtilityPriceCoefficient() {
			return sellingUtilityPriceCoefficient;
	}

	public double getBuyingUtilityTransportCoefficient() {
			return buyingUtilityTransportCoefficient;
	}

	public double getSellingUtilityTransportCoefficient() {
		return sellingUtilityTransportCoefficient;
	}

	public void clearAllExchangeQuantities() {
			Iterator it = getAllExchanges().iterator();
			while (it.hasNext()) {
				Exchange ex = (Exchange) it.next();
				ex.clearFlows();
			}
            setFlowsValid(false);
	}

	public static void clearAllCommodityExchangeQuantities() {
		Iterator it = getAllCommodities().iterator();
		while (it.hasNext()) {
			Commodity com = (Commodity) it.next();
			com.clearAllExchangeQuantities();
		}
	}



	public static void unfixPricesAndConditionsForAllCommodities() {
		Iterator allOfUs = getAllCommodities().iterator();
		while (allOfUs.hasNext()) {
			Commodity c = (Commodity) allOfUs.next();
			c.unfixPricesAndConditions();
		}
	}

    public void setFloorspaceCommodity(boolean isFloorspaceCommodity) {
        this.isFloorspaceCommodity = isFloorspaceCommodity;
    }

    public boolean isFloorspaceCommodity() {
        return isFloorspaceCommodity;
    }

    void setExpectedPrice(double expectedPrice) {
        this.expectedPrice = expectedPrice;
    }

    double getExpectedPrice() {
        return expectedPrice;
    }

    public void setCompositeMeritMeasureWeighting(double compositeMeritMeasureWeighting) {
        this.compositeMeritMeasureWeighting = compositeMeritMeasureWeighting;
    }

    public double[] getPriceInAllExchanges(){
        //exchanges are indexed by their location index which corresponds to a BetaZone index.
        //These index values could be greater than the number of exchange zones so you must
        //use the length of the buyingTazZUtility objects since their is one of these for each zone
        double[] prices = new double[buyingTazZUtilities.values().size()];

        Iterator exchanges = getAllExchanges().iterator();
        while(exchanges.hasNext()){
            Exchange ex = (Exchange)exchanges.next();
            prices[ex.exchangeLocationIndex]=ex.getPrice();
        }
        return prices;
    }

    public void setPriceInAllExchanges(double[] prices){
        Iterator exchanges = getAllExchanges().iterator();
        while(exchanges.hasNext()){
            Exchange ex = (Exchange)exchanges.next();
            ex.setPrice(prices[ex.exchangeLocationIndex]);
        }
        setFlowsValid(false);
        unfixPricesAndConditions();
    }

    void setFlowsValid(boolean flowsValid) {
        this.flowsValid = flowsValid;
    }

    boolean isFlowsValid() {
        return flowsValid;
    }

    /**
     * @return
     */
    public Matrix getBuyingFlowMatrix() {
        int nZones = TAZ.getAllZones().length;
        float[][] flows = new float[nZones][nZones];
        int[] zoneNumbers = new int[nZones+1];
        for (int exchange=0;exchange<nZones;exchange++) {//exchange zones
            Exchange ex = getExchange(exchange);
            if (ex!=null) {
                zoneNumbers[exchange+1] = TAZ.getZone(exchange).getZoneUserNumber();
                for (int consumption =0;consumption<nZones;consumption++) { //consumption zones
                    try {
                        flows[exchange][consumption]=(float) -ex.getFlowQuantity(consumption,'b');
                    } catch (InvalidFlowError e) {
                        // do nothing -- if there is no flow, leave the entry in the matrix as zero
                    }
                }
            }
        }
        
        Matrix m = new Matrix(flows);
        m.setExternalNumbers(zoneNumbers);
        return m;
    }

    /**
     * @return
     */
    public Matrix getSellingFlowMatrix() {
        int nZones = TAZ.getAllZones().length;
        float[][] flows = new float[nZones][nZones];
        int[] zoneNumbers = new int[nZones+1];
        for (int exchange=0;exchange<nZones;exchange++) {
            Exchange ex = getExchange(exchange);
            if (ex!=null) {
                zoneNumbers[exchange+1] = TAZ.getZone(exchange).getZoneUserNumber();
                for (int production =0;production<nZones;production++) {
                    try {
                        flows[production][exchange]=(float) ex.getFlowQuantity(production,'s');
                    } catch (InvalidFlowError e) {
                        // do nothing -- if there is no flow, leave the entry in the matrix as zero
                    }
                }
            }
        }
        
        Matrix m = new Matrix(flows);
        m.setExternalNumbers(zoneNumbers);
        return m;
    }

    /**
     * @return
     */
    public double[] getSurplusInAllExchanges() {
        double[] surplus = new double[getAllExchanges().size()];
        Iterator exIt = getAllExchanges().iterator();
        int exNum = 0;
        while (exIt.hasNext()) {
            Exchange x = (Exchange) exIt.next();
            surplus[exNum] = x.calculateSurplus();
            exNum++;
        }
        return surplus;
    }



 ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
 ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
 /***************************************************************************************************************
 * ALL METHODS BELOW THIS POINT HAVE BEEN COMMENTED OUT BECAUSE THEY ARE NOT CURRENTLY (3/18/04) BEING USED
  * OR THEY HAVE BEEN MOVED TO A NEW CLASS.
  *
  * ************************************************************************************************************/


//		/**
//		 * This gets the ZUtility for a commodity in a zone, either the selling ZUtility or the buying ZUtility.
//		 * @param selling if true, get the selling utility.  Otherwise get the buying utility
//		 */
  /*    public double calcZUtilityForPreferences(TAZ t, boolean selling, TravelUtilityCalculatorInterface tp,
		  boolean withRouteChoice) {
			  // message #1.2.5.1.1 to zUtilityOfLabourOrConsumption:com.pb.despair.pi.CommodityZutility
			  // double unnamed = zUtilityOfLabourOrConsumption.calcUtilityForPreferences(com.pb.despair.pt.TravelPreferences, boolean);
			  CommodityZUtility czu = retrieveCommodityZUtility(t, selling);
			  return czu.calcUtilityForPreferences(tp, withRouteChoice);
	  } */

//	 --Commented out by Inspection START (3/9/04 11:24 PM):
//		public CommodityZUtility retrieveSellingZUtilityForZone(TAZ z) {
//			return (CommodityZUtility) sellingTazZUtilities.get(z);
//		}
//	 --Commented out by Inspection STOP (3/9/04 11:24 PM)

//	 --Commented out by Inspection START (3/9/04 11:18 PM):
//		public CommodityZUtility retrieveBuyingZUtilityForZone(TAZ z) {
//			return (CommodityZUtility) buyingTazZUtilities.get(z);
//		}
//	 --Commented out by Inspection STOP (3/9/04 11:18 PM)

    






}

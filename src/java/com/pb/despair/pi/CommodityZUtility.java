package com.pb.despair.pi;

import com.pb.despair.model.Alternative;
import com.pb.despair.model.ChoiceModelOverflowException;
import com.pb.despair.model.OverflowException;
import com.pb.despair.model.TravelUtilityCalculatorInterface;

import java.util.logging.Logger;

/**
     * The Zutility of a commodity in a zone, whether it is the selling Zutility or the buying Zutility, is based on the
     * prices in the relevant exchange zones and the transport costs to the relevant exchange zones.
     * Generally (e.g. for ODOT), each commodity will either have one exchange zone for its buying zutility (and
     * multiple exchange zones for its selling zutility) or it will have one exchange zone for its selling zutility (and
     * multiple exchagne zone for its buying zutility).  Thus the cardinaility of this relationship will be 1 in about half of
     * the instances of Zutility objects, and N=number-of-zones in the remainder of the instances
     */

abstract public class CommodityZUtility implements Alternative {
    private static Logger logger = Logger.getLogger("com.pb.despair.pi");

    private double derivative = 0;
    private boolean pricesFixed = false;
    private boolean lastUtilityValid = false;
    private double myDispersionParameter;
    private TravelUtilityCalculatorInterface temporaryTravelPreferences = null;
    private boolean useRouteChoice = false;
    /**
     * This attribute represents the amount bought or sold in the zone.  Changing the quantity in the zone
     * does <b> not </b> automatically change the quantities in the flows and exchanges.  The method
     * allocateQuantityToFlowsAndExchanges must be called to update the amounts in the flows and exchanges
     *
     * @see CommodityZUtility#allocateQuantityToFlowsAndExchanges()
     * @see CommodityFlowArray
     * @see Exchange
     */
    private double quantity = 0; //TC or TP depending on whether the commZUtil is a buy or sell Utility
    protected Commodity myCommodity;
	private double lastCalculatedUtility; //CUBuy or CUSell depending on whether the commZUtil is a buy or sell util
    CommodityFlowArray myFlows;
    TAZ myTaz;
    private double lastHigherLevelDispersionParameter = 1;

    double getLastHigherLevelDispersionParameter() {
		return lastHigherLevelDispersionParameter;
    }
    // for profiling int utilityCalcCount = 0;


    /**
     * sets up one-way associations.  Should only be called by a subclass, which can inform the TAZ and the Commodity
     * to set up the other-way associations.
     */
    protected CommodityZUtility(Commodity c, TAZ z, int numZones, TravelUtilityCalculatorInterface tp) {
        myFlows = new CommodityFlowArray(this, tp);
        myCommodity = c;
        myTaz = z;
    }

    public abstract void addExchange(Exchange x);

    public abstract void addAllExchanges();

    public abstract void allocateQuantityToFlowsAndExchanges() throws OverflowException;

    /* calculates the CommodityUtility */
    public double getUtility(double higherLevelDispersionParameter) throws ChoiceModelOverflowException {
        if (pricesFixed &&lastUtilityValid) return lastCalculatedUtility;
        // no size terms at this level, so tracking higher level dispersion parameter is not strictly necessary
        // do it anyways just for fun.
        lastHigherLevelDispersionParameter = higherLevelDispersionParameter;
        //last calculatedUtility is either the SUc,z,k or CUSellc,z depending on the exhange type
        lastCalculatedUtility = myFlows.getUtility(higherLevelDispersionParameter);
        if (pricesFixed) lastUtilityValid = true;
        return lastCalculatedUtility;
    }
    
    public double[] getUtilityComponents(double higherLevelDispersionParameter) throws ChoiceModelOverflowException {
        lastHigherLevelDispersionParameter = higherLevelDispersionParameter;
        return myFlows.getUtilityComponents(higherLevelDispersionParameter);
    }

    public void changeDerivativeBy(double d) throws OverflowException {
        derivative += d;
        if (Double.isNaN(derivative) || Double.isInfinite(derivative)) {
            throw new OverflowException("Derivative is NaN/Infinite in " + this);
        }
    }

     public void changeQuantityBy(double change) throws OverflowException {
     // debug June 5 2002
    /* if (myCommodity.name.equals("FLR Agriculture") && myTaz.getZoneIndex()==726) {
            System.out.println(this + " quantity changing from "+this.quantity+" to "+(this.quantity+change));
        } */
        this.quantity += change;
        if (Double.isNaN(quantity) || Double.isInfinite(quantity)) {
            throw new OverflowException(" quantity is " + quantity + " in " + this);
        }
    }

    /* ------------------  GETTERS and SETTERS -----------------------------------------------------------*/
    public void setDerivative(double d) throws OverflowException {
//debug Feb232004
        if (derivative != 0) {
            logger.fine("resetting derivative in " + this);
        }
        derivative = d;
        if (Double.isNaN(d)) {
            logger.warning("Setting derivative to NaN for " + this);
            throw new OverflowException("Setting derivative to NaN for " + this);
        }
    }


    public double getDerivative() {
        return derivative;
    }

    public void setQuantity(double quantity) throws OverflowException {
        this.quantity = quantity;
        if (Double.isNaN(quantity) || Double.isInfinite(quantity)) {
            throw new OverflowException(" quantity is " + quantity + " in " + this);
        }
    }

    public double getQuantity() {
        return quantity;
    }

    public void setDispersionParameter(double dispersionParameter) {
        myDispersionParameter = dispersionParameter;
        myFlows.dispersionParameter = myDispersionParameter;
    }

    public double getDispersionParameter() {
        return myDispersionParameter;
    }

    public TravelUtilityCalculatorInterface getMyTravelPreferences() {
        if (temporaryTravelPreferences == null) {
            return myCommodity.getCommodityTravelPreferences();
        } else
            return temporaryTravelPreferences;
    }

    public boolean getUseRouteChoice() {
        return useRouteChoice;
    }

    public CommodityFlowArray getMyFlows() {
        return myFlows;
    }

    public Commodity getCommodity() {
        return myCommodity;
    }

    public TAZ getTaz() {
        return myTaz;
    }

    public String toString() {
        return myTaz + " " + myCommodity;
    }

    /**
     * @param b
     */
    public void setPricesFixed(boolean b) {
        if (pricesFixed == false || b == false) {
            lastUtilityValid  = false;
        }
        pricesFixed = b;
    }

    public void setLastCalculatedUtility(double lastCalculatedUtility) {
        this.lastCalculatedUtility = lastCalculatedUtility;
    }

    public void setLastUtilityValid(boolean b){
        lastUtilityValid = b;
    }
    /**
     * @return
     */
    public double[] getExchangeProbabilities() {
        return myFlows.getChoiceProbabilities();
    }



/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Has to use its logit model of exchange zones and inspect the knowledge of transport conditions to figure out the
     * composite utility of buying or selling
     */
/*    LogitModel getMyLogitModelOfExchangeAlternatives() {
        if (myLogitModelOfExchangeAlternatives == null) {
            myLogitModelOfExchangeAlternatives = new LogitModel();
            Iterator it = flows.values().iterator();
            while (it.hasNext()) {
                myLogitModelOfExchangeAlternatives.addAlternative((CommodityFlow)it.next());
                // message #1.2.3.1.3.1.1 to travelConditions:com.pb.despair.ts.TransportKnowledge with arguments: commodityTravelPreferences
                // Object unnamed = travelConditions.getRutilities(TravelPreferences);
                // message #1.2.3.1.3.1.2 to allExchanges:com.pb.despair.pi.Exchange
                // double unnamed = allExchanges.getPrice();
            }
            myLogitModelOfExchangeAlternatives.setDispersionParameter(getDispersionParameter());
        }
        return myLogitModelOfExchangeAlternatives;
    } */

    /**
     * Has to use its logit model of exchange zones and inspect the knowledge of transport conditions to figure out the
     * composite utility of buying or selling
     */
/*    public double calcUtilityForPreferences(TravelUtilityCalculatorInterface tp, boolean withRouteChoice) {
        double bob;
        synchronized(this) {
            temporaryTravelPreferences = tp;
            this.useRouteChoice = withRouteChoice;
            bob = getUtility();
            temporaryTravelPreferences = null;
            this.useRouteChoice = false;
        }
        return bob;
        // message #1.2.3.1.3.1.1 to travelConditions:com.pb.despair.ts.TransportKnowledge with arguments: commodityTravelPreferences
        // Object unnamed = travelConditions.getRutilities(TravelPreferences);
        // message #1.2.3.1.3.1.2 to allExchanges:com.pb.despair.pi.Exchange
        // double unnamed = allExchanges.getPrice();
    } */

/*    CommodityFlow monteCarloChoiceOfExchange() throws NoAlternativeAvailable {
        LogitModel exc = getMyLogitModelOfExchangeAlternatives();
        return (CommodityFlow)exc.monteCarloChoice();
    } */

/*    CommodityFlow monteCarloChoiceOfExchangeForPreferences(TravelUtilityCalculatorInterface tp) throws NoAlternativeAvailable {
        temporaryTravelPreferences = tp;
        LogitModel exc = getMyLogitModelOfExchangeAlternatives();
        CommodityFlow cf = (CommodityFlow)exc.monteCarloChoice();
        temporaryTravelPreferences = null;
        return cf;
    } */

    /*  public void addFlowIfNotAlreadyThere(CommodityFlow f) {
           if (!(flows.containsKey(f.theExchange))) flows.put(f.theExchange, f);
       } */

/*    public CommodityFlow findExistingCommodityFlow(Exchange ex) {
        return (CommodityFlow) flows.get(ex);
    }

/*    public CommodityFlow findExistingCommodityFlow(TAZ ex) {
        Iterator it = flows.iterator();
        while (it.hasNext()) {
            CommodityFlow cf = (CommodityFlow)it.next();
            if (cf.theExchange.exchangeLocation.equals(ex)) return cf;
        }
        return null;
    } */

    /*   for(int i=0;i<myFlows.theExchanges.length;i++) {
           if (myFlows.theExchanges[i]!=null) {
               try {
                 table.insertRow(false);
                 table.setString("Commodity", myCommodity.toString());
                 if (this instanceof BuyingZUtility) {
                       table.setInt("OriginZone",myFlows.theExchanges[i].exchangeLocation.getZoneUserNumber());
                       table.setInt("DestinationZone",myTaz.getZoneUserNumber());
                       table.setDouble("Quantity",-myFlows.quantities[i]);
                 } else {
                       table.setInt("OriginZone",myTaz.getZoneUserNumber());
                       table.setInt("DestinationZone",myFlows.theExchanges[i].exchangeLocation.getZoneUserNumber());
                       table.setDouble("Quantity",myFlows.quantities[i]);
                 }
                 table.post();
               } catch (com.borland.dx.dataset.DataSetException e) {
                   System.err.println("Error adding commodity flow to commodity flow table");
                   e.printStackTrace();
               }
           }
       }
   }  */

    /* has been moved to PIDataWriter */
    //void writeFlowsToFile(Writer f) throws IOException

    /**
     * Has been moved to PIDataWriter()
     */
    //public void updateTable(Writer w)

    /**
     * Has been moved to PIDataWriter()
     */
    //void updateFlowTable(TableDataSet table)


}

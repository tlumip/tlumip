package com.pb.despair.pi;

import org.apache.log4j.Logger;

import com.pb.despair.model.OverflowException;

/**
 * These are the exchanges, i.e. the market for a commodity in a zone.
 * Here are the commodity prices that need to be adjusted to acheive no shortage or no surplus in the zone at equilibrium
 *
 * @author John Abraham
 */
public class Exchange {

    private static Logger logger = Logger.getLogger("com.pb.despair.pi.exchange");

    public boolean monitor = false;
    /**
     * An attribute that represents the average price of the commodity when exchanged in the zone
     */
    private double price = 0;
    protected Commodity myCommodity;
    final int exchangeLocationIndex;
    final int exchangeLocationUserID;
    private double buyingFromExchangeDerivative = 0;
    private double sellingToExchangeDerivative = 0;

    /**
     * This is the flow of all of this commodity into and out of this exchange zone
     * Do NOT manually add flows to this vector.  The constructor for CommodityFlow
     * automatically adds itself into this vector.
     */
    protected final CommodityZUtility[] sellingToExchangeFlows;
    protected final CommodityZUtility[] buyingFromExchangeFlows;
    protected final double[] buyingQuantities;
    protected final double[] sellingQuantities;
    private double buyingSizeTerm;
    private double sellingSizeTerm;
    private SingleParameterFunction importFunction;
    private SingleParameterFunction exportFunction;
    private double lastCalculatedSurplus = 0;
    private double lastCalculatedDerivative = 0;
    private boolean surplusValid = false;
    private boolean derivativeValid = false;

    public Exchange(Commodity com, TAZ zone, int arraySize) {
        sellingToExchangeFlows = new CommodityZUtility[arraySize];
        buyingFromExchangeFlows = new CommodityZUtility[arraySize];
        buyingQuantities = new double[arraySize];
        sellingQuantities = new double[arraySize];
        this.myCommodity = com;
        this.exchangeLocationIndex = zone.zoneIndex;
        this.exchangeLocationUserID = zone.zoneUserNumber;
        com.addExchange(this);
    }

    void setFlowQuantity(int tazIndex, char selling, double quantity) throws OverflowException {
        if (Double.isNaN(quantity) || Double.isInfinite(quantity)) {
            throw new OverflowException("setting infinite or NaN flow quantity, exchange " + this + " buy/sell " + tazIndex + " quantity:" + quantity);
        }
        if (selling == 's') {
            if (sellingToExchangeFlows[tazIndex] == null)
                throw new Error("trying to set quantity for nonexistent flow " + this + " " + selling + " " + tazIndex);
            sellingQuantities[tazIndex] = quantity;
        } else {
            if (buyingFromExchangeFlows[tazIndex] == null)
                throw new Error("trying to set quantity for nonexistent flow " + this + " " + selling + " " + tazIndex);
            buyingQuantities[tazIndex] = quantity;
        }
    }

    void setFlowQuantityAndDerivative(int tazIndex, char selling, double quantity, double derivative) throws OverflowException {
        if (monitor) {
            logger.info("Setting flow quantity from " + this + " to taz with index " + tazIndex + " for " + selling + " to " + quantity + " with derivative " + derivative);
        }
        setFlowQuantity(tazIndex, selling, quantity);
        if (selling == 's') {
            setSellingToExchangeDerivative(getSellingToExchangeDerivative() + derivative);
        } else {
            setBuyingFromExchangeDerivative(getBuyingFromExchangeDerivative() + derivative);
        }
    }


    double getFlowQuantity(int tazIndex, char selling) {
        if (selling == 's') {
            if (sellingToExchangeFlows[tazIndex] == null)
                throw new InvalidFlowError("trying to get quantity for nonexistent flow " + this + " " + selling + " " + tazIndex);
            return sellingQuantities[tazIndex];
        } else {
            if (buyingFromExchangeFlows[tazIndex] == null)
                throw new InvalidFlowError("trying to get quantity for nonexistent flow " + this + " " + selling + " " + tazIndex);
            return buyingQuantities[tazIndex];
        }
    }



    /**
     * determine the net surplus in the zone, by summing all the selling quantities and subtracting the sum
     * of the buying quantities
     *
     * @return returns the net surplus or shortage in the zone at the given price
     */
    public double exchangeSurplus() {
        return exchangeSurplusAndDerivative()[0];
    } 


    /**
     * determine the net surplus in the zone, by summing all the selling quantities and subtracting the sum of the buying
     * quantities.  Also returns an approximation of the derivative of the surplus w.r.t. price
     *
     * @return returns the net surplus or shortage in the zone at the given price
     */
    public double[] exchangeSurplusAndDerivative() {
        if(surplusValid == true && derivativeValid == true) {
            double[] sAndD = new double[2];
            sAndD[0] = lastCalculatedSurplus;
            sAndD[1] = lastCalculatedDerivative;
            return sAndD;
        }
        // some debug code //

        /*    if (myCommodity.name.equals("HEALTH SERVICES") && exchangeLocation.getZoneIndex() == 0) {
               debug = true;
               System.out.println("debug info for exchange "+this+" with price "+price);
           } */

        double surplus = 0;
        double derivative = 0;
        CommodityZUtility cf = null;
        StringBuffer buyingString = null;
        StringBuffer sellingString = null;
        if(!myCommodity.isFlowsValid()) {
            logger.error("Calculating surplus for "+this+" when the flows are invalid");
            throw new Error("Calculating surplus for "+this+" when the flows are invalid");
        }
        if (monitor) {
            buyingString = new StringBuffer();
            sellingString = new StringBuffer();
        }
        for (int i = 0; i < buyingFromExchangeFlows.length; i++) {//all exchanges
            for (int b = 0; b < 2; b++) {
                double quantity;
                if (b == 0) {
                    cf = buyingFromExchangeFlows[i];
                    quantity = buyingQuantities[i]; //these are negative values
                    if (monitor && cf != null) buyingString.append(cf.getTaz().getZoneUserNumber() + ":" + quantity + " ");
                } else {
                    cf = sellingToExchangeFlows[i];
                    quantity = sellingQuantities[i];//these are positive values
                    if (monitor && cf != null) sellingString.append(cf.getTaz().getZoneUserNumber() + ":" + quantity + " ");
                }
                if (cf != null) {
                    if(logger.isDebugEnabled()) {
                        logger.debug("\t Commodity flow " + cf + " to exchange " + this + " quantity " + quantity);
                    }
                    surplus += quantity;
// old way          derivative += Math.abs(cf.getDispersionParameter() * 0.5 * quantity);
                }
            }
        }
        if (monitor) {
            logger.info("\t " + this + " buying quantities " + buyingString);
            logger.info("\t " + this + " selling quantities " + sellingString);
            logger.info("\t " + this + " price "+price);
        }
        derivative = getBuyingFromExchangeDerivative() + getSellingToExchangeDerivative();
        double[] importAndExport = importsAndExports(price);
        surplus += importAndExport[0] - importAndExport[1];
        derivative += importAndExport[2] - importAndExport[3];
        if(logger.isDebugEnabled() || monitor) {
            logger.info("\t " + "import:" + importAndExport[0] + " export:" + importAndExport[1]);
            logger.info("\t Total surplus = " + surplus);
        }

        // debug June 2 2002
        if (Double.isNaN(surplus) || Double.isInfinite(surplus) || Double.isNaN(derivative) || Double.isInfinite(derivative)) {
            logger.warn("\t Problem with Exchange surplus and/or derivative in " + this);
            logger.warn("\t surplus:" + surplus + " derivative:" + derivative + " buildup follows:");
            logger.warn(" price "+price);
            surplus = 0;
            derivative = 0;
            cf = null;
            for (int i = 0; i < buyingFromExchangeFlows.length; i++) {
                for (int b = 0; b < 2; b++) {
                    double quantity;
                    if (b == 0) {
                        cf = buyingFromExchangeFlows[i];
                        quantity = buyingQuantities[i];
                    } else {
                        cf = sellingToExchangeFlows[i];
                        quantity = sellingQuantities[i];
                    }
                    if (cf != null) {
                        logger.warn("\t Commodity flow " + cf + " to exchange " + this + " quantity " + quantity);
                        surplus += quantity;
                        derivative += Math.abs(cf.getDispersionParameter() * 0.5 * quantity);
                    }
                }
            }
            importAndExport = importsAndExports(price);
            logger.warn("\t imports:" + importAndExport[0] + " export:" + importAndExport[1]);
            surplus += importAndExport[0] - importAndExport[1];
            logger.warn("\t importDerivative:" + importAndExport[2] + " exportDerivative:" + importAndExport[3]);
            derivative += importAndExport[2] - importAndExport[3];
        }
        // end of June 2 2002 Debug


        double[] sAndD = new double[2];
        lastCalculatedSurplus = surplus;
        lastCalculatedDerivative = derivative;
        setSurplusAndDerivativeValid(true);

        sAndD[0] = lastCalculatedSurplus;
        sAndD[1] = lastCalculatedDerivative;
        return sAndD;
    }

    public double[] calculateSurplusAndDerviative(){
        double[] sAndD = new double[2];
        sAndD[0] = calculateSurplus();
        sAndD[1] = calculateDerviative();
        return sAndD;
    }

    public double calculateDerviative(){
        if(derivativeValid == true) return lastCalculatedDerivative;

        double derivative = 0;
        CommodityZUtility cf = null;
        StringBuffer buyingString = null;
        StringBuffer sellingString = null;
        if(!myCommodity.isFlowsValid()) {
            logger.error("Calculating derivative for "+this+" when the flows are invalid");
            throw new Error("Calculating derivative for "+this+" when the flows are invalid");
        }
        if (monitor) {
            buyingString = new StringBuffer();
            sellingString = new StringBuffer();
        }
        for (int i = 0; i < buyingFromExchangeFlows.length; i++) {//all exchanges
            for (int b = 0; b < 2; b++) {
                double quantity;
                if (b == 0) {
                    cf = buyingFromExchangeFlows[i];
                    quantity = buyingQuantities[i]; //these are negative values
                    if (monitor && cf != null) buyingString.append(cf.getTaz().getZoneUserNumber() + ":" + quantity + " ");
                } else {
                    cf = sellingToExchangeFlows[i];
                    quantity = sellingQuantities[i];//these are positive values
                    if (monitor && cf != null) sellingString.append(cf.getTaz().getZoneUserNumber() + ":" + quantity + " ");
                }
                if (cf != null) {
                    if(logger.isDebugEnabled()) {
                        logger.debug("\t Commodity flow " + cf + " to exchange " + this + " quantity " + quantity);
                    }
                 }
            }
        }
        if (monitor) {
            logger.info("\t " + this + " buying quantities " + buyingString);
            logger.info("\t " + this + " selling quantities " + sellingString);
            logger.info("\t " + this + " price "+price);
        }
        derivative = getBuyingFromExchangeDerivative() + getSellingToExchangeDerivative();
        double[] importAndExport = importsAndExports(price);
        derivative += importAndExport[2] - importAndExport[3];
        if(logger.isDebugEnabled() || monitor) {
            logger.info("\t " + "importDerivative:" + importAndExport[2] + " exportDerivative:" + importAndExport[3]);
            logger.info("\t Total derivative = " + derivative);
        }

        // debug June 2 2002
        if (Double.isNaN(derivative) || Double.isInfinite(derivative)) {
            logger.warn("\t Problem with Exchange derivative in " + this);
            logger.warn("\t derivative:" + derivative + " buildup follows:");
            logger.warn(" price "+price);
            derivative = 0;
            cf = null;
            for (int i = 0; i < buyingFromExchangeFlows.length; i++) {
                for (int b = 0; b < 2; b++) {
                    double quantity;
                    if (b == 0) {
                        cf = buyingFromExchangeFlows[i];
                        quantity = buyingQuantities[i];
                    } else {
                        cf = sellingToExchangeFlows[i];
                        quantity = sellingQuantities[i];
                    }
                    if (cf != null) {
                        logger.warn("\t Commodity flow " + cf + " to exchange " + this + " quantity " + quantity);
                        derivative += Math.abs(cf.getDispersionParameter() * 0.5 * quantity);
                    }
                }
            }
            importAndExport = importsAndExports(price);
            logger.warn("\t importDerivative:" + importAndExport[2] + " exportDerivative:" + importAndExport[3]);
            derivative += importAndExport[2] - importAndExport[3];
        }
        // end of June 2 2002 Debug


        lastCalculatedDerivative = derivative;
        setDerivativeValid(true);
        return derivative;
    }

    public double calculateSurplus(){
        if(surplusValid == true) return lastCalculatedSurplus;

        double surplus = 0.0;
        CommodityZUtility cf = null;
        StringBuffer buyingString = null;
        StringBuffer sellingString = null;
        if(!myCommodity.isFlowsValid()) {
            logger.error("Calculating surplus for "+this+" when the flows are invalid");
            throw new Error("Calculating surplus for "+this+" when the flows are invalid");
        }
        if (monitor) {
            buyingString = new StringBuffer();
            sellingString = new StringBuffer();
        }
        for (int i = 0; i < buyingFromExchangeFlows.length; i++) {//all exchanges
            for (int b = 0; b < 2; b++) {
                double quantity;
                if (b == 0) {
                    cf = buyingFromExchangeFlows[i];
                    quantity = buyingQuantities[i]; //these are negative values
                    if (monitor && cf != null) buyingString.append(cf.getTaz().getZoneUserNumber() + ":" + quantity + " ");
                } else {
                    cf = sellingToExchangeFlows[i];
                    quantity = sellingQuantities[i];//these are positive values
                    if (monitor && cf != null) sellingString.append(cf.getTaz().getZoneUserNumber() + ":" + quantity + " ");
                }
                if (cf != null) {
                    if(logger.isDebugEnabled()) {
                        logger.debug("\t Commodity flow " + cf + " to exchange " + this + " quantity " + quantity);
                    }
                    surplus += quantity;
                }
            }
        }
        if (monitor) {
            logger.info("\t " + this + " buying quantities " + buyingString);
            logger.info("\t " + this + " selling quantities " + sellingString);
            logger.info("\t " + this + " price "+price);
        }
        double[] importAndExport = importsAndExports(price);
        surplus += importAndExport[0] - importAndExport[1];
        if(logger.isDebugEnabled() || monitor) {
            logger.info("\t " + "import:" + importAndExport[0] + " export:" + importAndExport[1]);
            logger.info("\t Total surplus = " + surplus);
        }

        // debug June 2 2002
        if (Double.isNaN(surplus) || Double.isInfinite(surplus)) {
            logger.warn("\t Problem with Exchange surplus in " + this);
            logger.warn("\t surplus:" + surplus + " buildup follows:");
            logger.warn(" price "+price);
            surplus = 0;
            cf = null;
            for (int i = 0; i < buyingFromExchangeFlows.length; i++) {
                for (int b = 0; b < 2; b++) {
                    double quantity;
                    if (b == 0) {
                        cf = buyingFromExchangeFlows[i];
                        quantity = buyingQuantities[i];
                    } else {
                        cf = sellingToExchangeFlows[i];
                        quantity = sellingQuantities[i];
                    }
                    if (cf != null) {
                        logger.warn("\t Commodity flow " + cf + " to exchange " + this + " quantity " + quantity);
                        surplus += quantity;
                    }
                }
            }
            importAndExport = importsAndExports(price);
            logger.warn("\t imports:" + importAndExport[0] + " export:" + importAndExport[1]);
            surplus += importAndExport[0] - importAndExport[1];
         }
        // end of June 2 2002 Debug

        lastCalculatedSurplus = surplus;
        setSurplusValid(true);
        return surplus;
    }

    public void clearFlows() {
        for (int i = 0; i < buyingFromExchangeFlows.length; i++) {
            if (buyingFromExchangeFlows[i] != null) {
                buyingQuantities[i] = 0;
            }
        }
        for (int i = 0; i< sellingToExchangeFlows.length; i++) {
            if (sellingToExchangeFlows[i] != null) {
                sellingQuantities[i] = 0;
            }
        }
        setBuyingFromExchangeDerivative(0);
        setSellingToExchangeDerivative(0);
        setLastCalculatedSurplus(0);
        setLastCalculatedDerivative(0);
        setSurplusAndDerivativeValid(false);
    }

    public void setBuyingFromExchangeDerivative(double buyingFromExchangeDerivative) {
        this.buyingFromExchangeDerivative = buyingFromExchangeDerivative;
    }

    public double getBuyingFromExchangeDerivative() {
        return buyingFromExchangeDerivative;
    }

    public void setSellingToExchangeDerivative(double sellingToExchangeDerivative) {
        this.sellingToExchangeDerivative = sellingToExchangeDerivative;
    }

    public double getSellingToExchangeDerivative() {
        return sellingToExchangeDerivative;
    }

    public void setSurplusAndDerivativeValid(boolean b){
        this.surplusValid = b;
        this.derivativeValid = b;
    }

    public void setSurplusValid(boolean b){
        this.surplusValid = b;
    }

    public void setDerivativeValid(boolean b){
        this.derivativeValid = b;
    }

    public void setLastCalculatedSurplus(double surplus){
        this.lastCalculatedSurplus = surplus;
    }

    public void setLastCalculatedDerivative(double derivative){
        this.lastCalculatedDerivative = derivative;
    }

    public void setPrice(double newPrice) {
        price = newPrice;
    }

    public double getPrice() {
        return price;
    }

    public void addFlowIfNotAlreadyThere(CommodityZUtility f, boolean buying) {
        int tazIndex = f.myTaz.getZoneIndex();
        if (buying) {
            buyingFromExchangeFlows[tazIndex] = f;
            buyingQuantities[tazIndex] = 0;
        } else {
            sellingToExchangeFlows[tazIndex] = f;
            sellingQuantities[tazIndex] = 0;
        }
    }



    public String toString() {
        return "Exchange of " + myCommodity + " in " + exchangeLocationUserID;
        //return "Exchange " + exchangeLocationUserID;
    }

    public int hashCode() {
        return myCommodity.hashCode() ^ exchangeLocationIndex;
    }

    public double getBuyingSizeTerm() {
        return buyingSizeTerm;
    }

    public void setBuyingSizeTerm(double buyingSizeTerm) {
        this.buyingSizeTerm = buyingSizeTerm;
    }

    public double getSellingSizeTerm() {
        return sellingSizeTerm;
    }

    public void setSellingSizeTerm(double sellingSizeTerm) {
        this.sellingSizeTerm = sellingSizeTerm;
    }

    public SingleParameterFunction getImportFunction() {
        return importFunction;
    }

    public void setImportFunction(SingleParameterFunction importFunction) {
        this.importFunction = importFunction;
    }

    public SingleParameterFunction getExportFunction() {
        return exportFunction;
    }

    public void setExportFunction(SingleParameterFunction exportFunction) {
        this.exportFunction = exportFunction;
    }

    public int getExchangeLocationIndex() {
        return exchangeLocationIndex;
    }

    /**
     * Returns the amount of imports in an exchange zone given the price in the exchange zone, and the amount of exports in an
     * exchange zone given the price in the exchange zone.  This is a convenience routine for the exchange zone -- each
     * exchange zone can call this routine to determine the imports and exports in that zone. <p>
     * The default implementation uses a simple exponential routine
     *
     * @param price price for which the imports and exports are to be determined
     * @return first element is amount of imports, second is amount of export
     * @see Exchange#setImportFunction
     */
    public double[] importsAndExports(double price) {
        double[] impExp = new double[4];
        impExp[0] = importFunction.evaluate(price);
        impExp[1] = exportFunction.evaluate(price);
        impExp[2] = importFunction.derivative(price);
        impExp[3] = exportFunction.derivative(price);
        return impExp;
    }



    /**
     * @return
     */
    public double boughtTotal() {
            double total = 0;
            for (int i = 0; i < buyingFromExchangeFlows.length; i++) {
                if (buyingFromExchangeFlows[i] != null) total += buyingQuantities[i];
            }
            return -total;
    }

    /**
     * @return
     */
    public double soldTotal() {
        double total = 0;
        for (int i = 0; i < sellingToExchangeFlows.length; i++) {
            if (sellingToExchangeFlows[i] != null) total += sellingQuantities[i];
        }
        return total;
    }

}


package com.pb.despair.pi;

import com.pb.despair.model.ChoiceModelOverflowException;
import com.pb.despair.model.OverflowException;
import com.pb.despair.model.TravelUtilityCalculatorInterface;

import java.util.Iterator;


/**
 * This the the utility of buying or selling a commodity in a zone.  It is a function of the commodity prices in the exchange zones and the associated transport disutility.
 *
 * @author John Abraham
 */
public class SellingZUtility extends CommodityZUtility {

    public SellingZUtility(Commodity c, TAZ t, int numZones, TravelUtilityCalculatorInterface tp) {
        super(c, t, numZones, tp);
        c.addSellingZUtility(this);
     //   t.addSellingZUtility(this, c);
    }

    public String toString() {
        return "SellingZUtility" + super.toString();
    };

    public void allocateQuantityToFlowsAndExchanges() throws OverflowException {
        try {
            myFlows.setAggregateQuantity(getQuantity(), getDerivative());
        } catch (ChoiceModelOverflowException e) {
            throw new OverflowException(e.toString());
        }


    }

    public void addAllExchanges() {
        Iterator it = myCommodity.getAllExchanges().iterator();
        while (it.hasNext()) {
            Exchange x = (Exchange) it.next();
            x.addFlowIfNotAlreadyThere(this, false);
        }
    }

    public void addExchange(Exchange x) {
        x.addFlowIfNotAlreadyThere(this, false);
    }
}

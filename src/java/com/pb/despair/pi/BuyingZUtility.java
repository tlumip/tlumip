package com.pb.despair.pi;

import com.pb.despair.model.ChoiceModelOverflowException;
import com.pb.despair.model.OverflowException;
import com.pb.despair.model.TravelUtilityCalculatorInterface;

import java.util.Iterator;

/**
 * This the the utility of buying or selling a commodity in a zone.
 *
 * @author John Abraham
 */
public class BuyingZUtility extends CommodityZUtility {
    public BuyingZUtility(Commodity c, TAZ t, int numZones, TravelUtilityCalculatorInterface tp) {
        super(c, t, numZones, tp);
        c.addBuyingZUtility(this);
      //  t.addBuyingZUtility(this, c);
    }

    public String toString() {
        return "BuyingZUtility" + super.toString();
    };

    public void allocateQuantityToFlowsAndExchanges() throws OverflowException {
        try {
            //      myFlows.allocateQuantity(-getQuantity());
            myFlows.setAggregateQuantity(-getQuantity(), -getDerivative()); 
        } catch (ChoiceModelOverflowException e) {
            throw new OverflowException(e.toString());
        }
    }

    /**
     *
     */
    public void addAllExchanges() {
        Iterator it = myCommodity.getAllExchanges().iterator();
        while (it.hasNext()) {
            Exchange x = (Exchange) it.next();
            x.addFlowIfNotAlreadyThere(this, true);
        }
    }

    public void addExchange(Exchange x) {
        x.addFlowIfNotAlreadyThere(this, true);
    }
}


/*
 * Created on Jan 4, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.pi;

import java.util.Iterator;

import drasys.or.matrix.DenseVector;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TotalSurplusVector extends DenseVector {

    public TotalSurplusVector() {
        super(Commodity.getAllCommodities().size());
        int commodityIndex =0;
        Iterator comIt = Commodity.getAllCommodities().iterator();
        while (comIt.hasNext()) {
            double surplus = 0;
            Commodity c = (Commodity) comIt.next();
            Iterator exIt = c.getAllExchanges().iterator();
            while (exIt.hasNext()) {
                surplus += ((Exchange) exIt.next()).calculateSurplus();
            }
            setElementAt(commodityIndex,surplus);
            commodityIndex++;
        }
    }


}

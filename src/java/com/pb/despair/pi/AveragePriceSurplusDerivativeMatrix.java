/*
 * Created on Jan 3, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.pi;

import com.pb.despair.model.*;

import mt.*;

import java.util.*;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class AveragePriceSurplusDerivativeMatrix extends DenseMatrix {

    
    private static int numCommodities;
    private static int matrixSize=0;

    public static void calculateMatrixSize() {
        numCommodities = Commodity.getAllCommodities().size();
        matrixSize = numCommodities ;
    }

    public AveragePriceSurplusDerivativeMatrix() {
        super(matrixSize, matrixSize);
        if (matrixSize ==0) {
            throw new RuntimeException("Call BlockPriceSurplusMatrix.calculateMatrixSize() before instantiating BlockPriceSurplusMAtrix");
        }
        
        Iterator actIt = AggregateActivity.getAllProductionActivities().iterator();
        while (actIt.hasNext()) {
            ProductionActivity prodActivity = (ProductionActivity) actIt.next();
            if (prodActivity instanceof AggregateActivity) {
                AggregateActivity activity = (AggregateActivity) prodActivity;

                // build up relationship between average commodity price and total surplus
                DenseVector pl;
                DenseMatrix fpl;
                try {
                    pl= new DenseVector(activity.logitModelOfZonePossibilities.getChoiceProbabilities());
                    fpl = new DenseMatrix(activity.logitModelOfZonePossibilities.choiceProbabilityDerivatives());
                    
                } catch (ChoiceModelOverflowException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Can't solve for amounts in zone",e);
                }
                DenseMatrix dulbydprice = new DenseMatrix(fpl.numColumns(),numCommodities);
                dulbydprice.zero();
                int[] rows = new int[1];
                int[] columns = new int[numCommodities];
                double[][] valuesToAdd = new double[1][];
                for (int col=0;col<numCommodities;col++) {
                    columns[col] = col;
                }
                for (int location =0;location<pl.size();location++) {
                    rows[0] = location;
                    AggregateDistribution l = (AggregateDistribution) activity.logitModelOfZonePossibilities.alternativeAt(location);
                    valuesToAdd[0] = l.calculateLocationUtilityWRTAveragePrices();
                    dulbydprice.set(rows,columns,valuesToAdd);
                }
                DenseMatrix dLocationByDPrice = new DenseMatrix(AbstractTAZ.getAllZones().length,numCommodities);
                fpl.mult(dulbydprice,dLocationByDPrice);
                for (int location =0;location<pl.size();location++) {
                    mt.Vector dThisLocationByPrices = new DenseVector(numCommodities);
                    for (int i=0;i<dThisLocationByPrices.size();i++) {
                        dThisLocationByPrices.set(i,dLocationByDPrice.get(location,i));
                    }
                    AggregateDistribution l = (AggregateDistribution) activity.logitModelOfZonePossibilities.alternativeAt(location);
                    l.addTwoComponentsOfDerivativesToAveragePriceMatrix(activity.getTotalAmount(),this,dThisLocationByPrices); 
                }
            }
        }
        
        
        // add import/export effect to diagonal
        Iterator comIt = Commodity.getAllCommodities().iterator();
        int comNum = 0;
        while (comIt.hasNext()) {
            Commodity c = (Commodity) comIt.next();
            double derivative = 0;
            Iterator exIt = c.getAllExchanges().iterator();
            while (exIt.hasNext()) {
                Exchange x = (Exchange) exIt.next();
                double[] importsAndExports = x.importsAndExports(x.getPrice());
                derivative += importsAndExports[2] - importsAndExports[3];
            }
            add(comNum,comNum,derivative);
            comNum++;
        }
    }
}

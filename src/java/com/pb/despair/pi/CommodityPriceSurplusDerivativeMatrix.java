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
public class CommodityPriceSurplusDerivativeMatrix extends DenseMatrix {

    public final Commodity commodity; 

    public CommodityPriceSurplusDerivativeMatrix(Commodity c) {
        super(c.getAllExchanges().size()+1, c.getAllExchanges().size());
        commodity = c;
        
        Exchange[] exchanges = (Exchange[]) c.getAllExchanges().toArray(new Exchange[1]);
        AbstractTAZ[] allZones = AbstractTAZ.getAllZones();
        
        // need this for adding to the array
        int[] rows = new int[exchanges.length];
        for (int r=0;r<exchanges.length;r++) {
            rows[r] = r;
        }
        
        for (int z=0;z<allZones.length;z++) {
            // first add in the effect of exchange choice
            CommodityZUtility bzu = c.retrieveCommodityZUtility(allZones[z],false);
            CommodityZUtility szu = c.retrieveCommodityZUtility(allZones[z],true);
            Matrix bzuDerivatives = new DenseMatrix(bzu.myFlows.getChoiceDerivatives()); //6.67% of time here
            bzuDerivatives.scale(-bzu.getQuantity()*c.getBuyingUtilityPriceCoefficient()); //14.85% of time in this method
            
            
            Matrix szuDerivatives = new DenseMatrix(szu.myFlows.getChoiceDerivatives()); // 5.6 % of time here
            szuDerivatives.scale(szu.getQuantity()*c.getSellingUtilityPriceCoefficient()); // 5.46 % of time here
            
            //TODO check if we need to scale for the fact that the AVERAGE price goes down when one item goes down
            // TODO check if we need to scale for the fact that we're trying to solve (above average surplus)=0
            double[][] temp = new double[exchanges.length][exchanges.length];
            bzuDerivatives.get(rows,rows,temp); // 6.22 % of time in here
            add(rows,rows,temp); // 7.04% of time in here
            szuDerivatives.get(rows,rows,temp); // 6.33 % of time in here
            add(rows,rows,temp); // 6.98% of time in here
            
            // now add in the effect of changes in production, consumption and location
            DenseVector xProbabilities = new DenseVector(bzu.getExchangeProbabilities());
            // create a column matrix;
            DenseMatrix xProbabilitiesMatrix = new DenseMatrix(xProbabilities);
            DenseVector logSumDerivatives = new DenseVector(bzu.myFlows.getLogsumDerivativesWRTPrices());
            DenseMatrix logSumDerivativesMatrixTranspose = new DenseMatrix(logSumDerivatives);
            DenseMatrix logSumDerivativesMatrix = new DenseMatrix(1,logSumDerivatives.size());
            logSumDerivativesMatrixTranspose.transpose(logSumDerivativesMatrix);
            DenseMatrix exchangeQuantityChangeByPrice = new DenseMatrix(xProbabilities.size(),logSumDerivatives.size()); // 0.72% of time here
            xProbabilitiesMatrix.mult(-bzu.getDerivative(),logSumDerivativesMatrix,exchangeQuantityChangeByPrice); // 1.28% of time here
            exchangeQuantityChangeByPrice.get(rows,rows,temp); // 6.33 % of time here
            add(rows,rows,temp); // 6.87% of time here
            
            // now for selling
            xProbabilities = new DenseVector(szu.getExchangeProbabilities());
            // create a column matrix;
            xProbabilitiesMatrix = new DenseMatrix(xProbabilities);
            logSumDerivatives = new DenseVector(szu.myFlows.getLogsumDerivativesWRTPrices());
            logSumDerivativesMatrixTranspose = new DenseMatrix(logSumDerivatives);
            logSumDerivativesMatrix = new DenseMatrix(1,logSumDerivatives.size());
            logSumDerivativesMatrixTranspose.transpose(logSumDerivativesMatrix);
            exchangeQuantityChangeByPrice = new DenseMatrix(xProbabilities.size(),logSumDerivatives.size());
            xProbabilitiesMatrix.mult(szu.getDerivative(),logSumDerivativesMatrix,exchangeQuantityChangeByPrice); //.89% of time here
            exchangeQuantityChangeByPrice.get(rows,rows,temp); // 6.32% of time in here
            add(rows,rows,temp); //6.92% of time here 
            
        }
        
        // add in the effects of imports and exports
        Iterator exIt = c.getAllExchanges().iterator();
        int xNum = 0;
        while (exIt.hasNext()) {
            Exchange x = (Exchange) exIt.next();
            double[] importsAndExports = x.importsAndExports(x.getPrice());
            add(xNum,xNum,importsAndExports[2]-importsAndExports[3]);
            xNum++;
        }
        
        // now we need to add the additional equation that the sum of all the delta prices equals zero
        // TODO this last row should be scaled for the relative size of the units of price vs the units of commodity quantity.
        int row = numRows-1;
        for (int col=0;col<numColumns;col++) {
            set(row,col,1.0);
        }
    }
}

/*
 * Created on Jan 3, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.pi;

import com.pb.despair.model.*;

import drasys.or.linear.algebra.Algebra;
import drasys.or.linear.algebra.AlgebraException;
import drasys.or.matrix.DenseMatrix;
import drasys.or.matrix.DenseVector;
import drasys.or.matrix.MatrixI;

//import mt.*;

import java.util.*;

/**
 * @author jabraham
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class CommodityPriceSurplusDerivativeMatrix {

    public final double data[][];
    
    public final Commodity commodity;

    public static Algebra a = new Algebra();
    public final int size;

    public CommodityPriceSurplusDerivativeMatrix(Commodity c) {
        size = c.getAllExchanges().size();
        data = new double[size][size];
        
        commodity = c;

        //Exchange[] exchanges = (Exchange[]) c.getAllExchanges().toArray(new Exchange[1]);
        AbstractTAZ[] allZones = AbstractTAZ.getAllZones();

        // need this for adding to the array
        //int[] rows = new int[exchanges.length];
        //        for (int r=0;r<exchanges.length;r++) {
        //            rows[r] = r;
        //        }
        //        
        for (int z = 0; z < allZones.length; z++) {
//            try {
                // first add in the effect of exchange choice
                CommodityZUtility bzu = c.retrieveCommodityZUtility(allZones[z], false);
                CommodityZUtility szu = c.retrieveCommodityZUtility(allZones[z], true);
                if ((c.exchangeType != 'n' && c.exchangeType != 'c')) {
                    double[][] temp = bzu.myFlows.getChoiceDerivatives();
                    double scale = -bzu.getQuantity() * c.getBuyingUtilityPriceCoefficient();
                    for (int i = 0; i < temp.length; i++) {
                        for (int j = 0; j < temp[i].length; j++) {
                            data[i][j]+=temp[i][j]*scale;
                        }
                    }
                }
                //            MatrixI bzuDerivatives = new DenseMatrix(temp);
                if ((c.exchangeType != 'n' && c.exchangeType != 'p')) {
                    double[][] temp = szu.myFlows.getChoiceDerivatives();
                    double scale = szu.getQuantity() * c.getSellingUtilityPriceCoefficient();
                    for (int i = 0; i < temp.length; i++) {
                        for (int j = 0; j < temp[i].length; j++) {
                            data[i][j]+=temp[i][j]*scale;
                        }
                    }
                }

                // now add in the effect of changes in production, consumption
                // and location
                double[] xProbabilitiesDouble = bzu.getExchangeProbabilities();
                //DenseVector xProbabilities = new DenseVector(xProbabilitiesDouble);
                // create a column matrix;
                //DenseMatrix xProbabilitiesMatrix = new DenseMatrix(xProbabilities.size(),1);
                //xProbabilitiesMatrix.setColumn(0,xProbabilities);
                double[] logSumDerivativesDouble = bzu.myFlows.getLogsumDerivativesWRTPrices();
                //DenseVector logSumDerivatives = new DenseVector(logSumDerivativesDouble);
                //DenseMatrix logSumDerivativesMatrix = new DenseMatrix(1,logSumDerivatives.size());
                //logSumDerivativesMatrix.setRow(0,logSumDerivatives);
                //DenseMatrix exchangeQuantityChangeByPrice;

                //exchangeQuantityChangeByPrice = a.multiply(xProbabilitiesMatrix, logSumDerivativesMatrix);
                double bzuDerivative = bzu.getDerivative();
                for (int r = 0; r < xProbabilitiesDouble.length; r++) {
                    for (int col = 0; col < logSumDerivativesDouble.length; col++) {
//                        data[r][col]+=exchangeQuantityChangeByPrice.elementAt(r, col) * -bzu.getDerivative();
                        data[r][col]+=xProbabilitiesDouble[r]*logSumDerivativesDouble[col]* -bzuDerivative;
                    }
                }

                // now for selling
                xProbabilitiesDouble = szu.getExchangeProbabilities();
                //xProbabilities = new DenseVector(xProbabilitiesDouble);
                // create a column matrix;
                //xProbabilitiesMatrix = new DenseMatrix(xProbabilities.size(),1);
                //xProbabilitiesMatrix.setColumn(0,xProbabilities);
                logSumDerivativesDouble = szu.myFlows.getLogsumDerivativesWRTPrices();
                //logSumDerivatives = new DenseVector(logSumDerivativesDouble);
                //logSumDerivativesMatrix = new DenseMatrix(1,logSumDerivatives.size());
                //logSumDerivativesMatrix.setRow(0,logSumDerivatives);

                //exchangeQuantityChangeByPrice = a.multiply(xProbabilitiesMatrix, logSumDerivativesMatrix);
                double szuDerivative = szu.getDerivative();

                for (int r = 0; r < xProbabilitiesDouble.length; r++) {
                    for (int col = 0; col < logSumDerivativesDouble.length; col++) {
//                        data[r][col]+=exchangeQuantityChangeByPrice.elementAt(r, col) * szu.getDerivative();
                        data[r][col]+=xProbabilitiesDouble[r]*logSumDerivativesDouble[col] * szuDerivative;
                    }
                }
//            } catch (AlgebraException e) {
//                e.printStackTrace();
//                throw new RuntimeException(e);
//
//            }
        }

        // add in the effects of imports and exports
        Iterator exIt = c.getAllExchanges().iterator();
        int xNum = 0;
        while (exIt.hasNext()) {
            Exchange x = (Exchange) exIt.next();
            double[] importsAndExports = x.importsAndExports(x.getPrice());
            data[xNum][xNum]+= importsAndExports[2] - importsAndExports[3];
            //            add(xNum,xNum,importsAndExports[2]-importsAndExports[3]);
            xNum++;
        }

    }
}
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
public class CommodityPriceSurplusDerivativeMatrix extends DenseMatrix {

    public final Commodity commodity;

    public static Algebra a = new Algebra();

    public CommodityPriceSurplusDerivativeMatrix(Commodity c) {
        super(c.getAllExchanges().size(), c.getAllExchanges().size());
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
            try {
                // first add in the effect of exchange choice
                CommodityZUtility bzu = c.retrieveCommodityZUtility(allZones[z], false);
                CommodityZUtility szu = c.retrieveCommodityZUtility(allZones[z], true);
                if ((c.exchangeType != 'n' && c.exchangeType != 'c')) {
                    double[][] temp = bzu.myFlows.getChoiceDerivatives();
                    double scale = -bzu.getQuantity() * c.getBuyingUtilityPriceCoefficient();
                    for (int i = 0; i < temp.length; i++) {
                        for (int j = 0; j < temp[i].length; j++) {
                            setElementAt(i, j, elementAt(i, j) + temp[i][j] * scale);
                        }
                    }
                }
                //            MatrixI bzuDerivatives = new DenseMatrix(temp);
                if ((c.exchangeType != 'n' && c.exchangeType != 'p')) {
                    double[][] temp = szu.myFlows.getChoiceDerivatives();
                    double scale = szu.getQuantity() * c.getSellingUtilityPriceCoefficient();
                    for (int i = 0; i < temp.length; i++) {
                        for (int j = 0; j < temp[i].length; j++) {
                            setElementAt(i, j, elementAt(i, j) + temp[i][j] * scale);
                        }
                    }
                }
                //            MatrixI szuDerivatives= new DenseMatrix(temp);

                //TODO check if we need to scale for the fact that the AVERAGE
                // price goes down when one item goes down
                // TODO check if we need to scale for the fact that we're trying
                // to solve (above average surplus)

                //            double[][] temp = new
                // double[exchanges.length][exchanges.length];
                //            bzuDerivatives.get(rows,rows,temp); // 6.22 % of time in here
                //            add(rows,rows,temp); // 7.04% of time in here
                //            szuDerivatives.get(rows,rows,temp); // 6.33 % of time in here
                //            add(rows,rows,temp); // 6.98% of time in here

                // now add in the effect of changes in production, consumption
                // and location
                DenseVector xProbabilities = new DenseVector(bzu.getExchangeProbabilities());
                // create a column matrix;
                DenseMatrix xProbabilitiesMatrix = new DenseMatrix(xProbabilities.size(),1);
                xProbabilitiesMatrix.setColumn(0,xProbabilities);
//                DenseMatrix xProbabilitiesMatrix = new DenseMatrix(xProbabilities);
                DenseVector logSumDerivatives = new DenseVector(bzu.myFlows.getLogsumDerivativesWRTPrices());
                DenseMatrix logSumDerivativesMatrix = new DenseMatrix(1,logSumDerivatives.size());
                logSumDerivativesMatrix.setRow(0,logSumDerivatives);
//                DenseMatrix logSumDerivativesMatrixTranspose = new DenseMatrix(logSumDerivatives);
//                DenseMatrix logSumDerivativesMatrix = a.transpose(logSumDerivativesMatrixTranspose);
                //            DenseMatrix logSumDerivativesMatrix = new
                // DenseMatrix(1,logSumDerivatives.size());
                //            logSumDerivativesMatrixTranspose.transpose(logSumDerivativesMatrix);
                //            DenseMatrix exchangeQuantityChangeByPrice = new
                // DenseMatrix(xProbabilities.size(),logSumDerivatives.size());
                // // 0.72% of time here
                //            xProbabilitiesMatrix.mult(-bzu.getDerivative(),logSumDerivativesMatrix,exchangeQuantityChangeByPrice);
                // // 1.28% of time here
                DenseMatrix exchangeQuantityChangeByPrice;
                exchangeQuantityChangeByPrice = a.multiply(xProbabilitiesMatrix, logSumDerivativesMatrix);
                for (int r = 0; r < exchangeQuantityChangeByPrice.sizeOfRows(); r++) {
                    for (int col = 0; col < exchangeQuantityChangeByPrice.sizeOfColumns(); col++) {
                        setElementAt(r, col, elementAt(r, col) + exchangeQuantityChangeByPrice.elementAt(r, col) * -bzu.getDerivative());
                    }
                }
                //            exchangeQuantityChangeByPrice.get(rows,rows,temp); // 6.33 %
                // of time here
                //            add(rows,rows,temp); // 6.87% of time here

                // now for selling
                xProbabilities = new DenseVector(szu.getExchangeProbabilities());
                // create a column matrix;
                xProbabilitiesMatrix = new DenseMatrix(xProbabilities.size(),1);
                xProbabilitiesMatrix.setColumn(0,xProbabilities);
                logSumDerivatives = new DenseVector(szu.myFlows.getLogsumDerivativesWRTPrices());
                logSumDerivativesMatrix = new DenseMatrix(1,logSumDerivatives.size());
                logSumDerivativesMatrix.setRow(0,logSumDerivatives);
                //            new DenseMatrix(1,logSumDerivatives.size());
                //            logSumDerivativesMatrixTranspose.transpose(logSumDerivativesMatrix);
                //            exchangeQuantityChangeByPrice = new
                // DenseMatrix(xProbabilities.size(),logSumDerivatives.size());
                //            xProbabilitiesMatrix.mult(szu.getDerivative(),logSumDerivativesMatrix,exchangeQuantityChangeByPrice);
                // //.89% of time here
                exchangeQuantityChangeByPrice = a.multiply(xProbabilitiesMatrix, logSumDerivativesMatrix);
                //            exchangeQuantityChangeByPrice.get(rows,rows,temp); // 6.32%
                // of time in here
                //            add(rows,rows,temp); //6.92% of time here
                for (int r = 0; r < exchangeQuantityChangeByPrice.sizeOfRows(); r++) {
                    for (int col = 0; col < exchangeQuantityChangeByPrice.sizeOfColumns(); col++) {
                        setElementAt(r, col, elementAt(r, col) + exchangeQuantityChangeByPrice.elementAt(r, col) * szu.getDerivative());
                    }
                }
            } catch (AlgebraException e) {
                e.printStackTrace();
                throw new RuntimeException(e);

            }
        }

        // add in the effects of imports and exports
        Iterator exIt = c.getAllExchanges().iterator();
        int xNum = 0;
        while (exIt.hasNext()) {
            Exchange x = (Exchange) exIt.next();
            double[] importsAndExports = x.importsAndExports(x.getPrice());
            setElementAt(xNum, xNum, elementAt(xNum, xNum) + importsAndExports[2] - importsAndExports[3]);
            //            add(xNum,xNum,importsAndExports[2]-importsAndExports[3]);
            xNum++;
        }

    }
}
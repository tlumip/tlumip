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
import drasys.or.matrix.VectorI;

//import mt.*;

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
//                System.out.println(activity);

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
                DenseMatrix dulbydprice = new DenseMatrix(fpl.sizeOfColumns(),numCommodities);
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
                    //dulbydprice.set(rows,columns,valuesToAdd);
                    dulbydprice.setRow(location,new DenseVector(valuesToAdd[0]));
                }
                DenseMatrix dLocationByDPrice = new DenseMatrix(AbstractTAZ.getAllZones().length,numCommodities);
                Algebra a = new Algebra();
                try {
                    //fpl.mult(dulbydprice,dLocationByDPrice);
                    dLocationByDPrice = a.multiply(fpl,dulbydprice);
                } catch (AlgebraException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    throw new RuntimeException("Can't multiply matrices to figure out average price surplus",e1);
                }
                for (int location =0;location<pl.size();location++) {
                    VectorI dThisLocationByPrices = new DenseVector(numCommodities);
                    for (int i=0;i<dThisLocationByPrices.size();i++) {
                        dThisLocationByPrices.setElementAt(i,dLocationByDPrice.elementAt(location,i));
                    }
                    AggregateDistribution l = (AggregateDistribution) activity.logitModelOfZonePossibilities.alternativeAt(location);
                    l.addTwoComponentsOfDerivativesToAveragePriceMatrix(activity.getTotalAmount(),this,dThisLocationByPrices);
//                    System.out.println();
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
            setElementAt(comNum,comNum,
                    elementAt(comNum,comNum)+derivative);
            comNum++;
        }
    }
//    /* (non-Javadoc)
//     * @see drasys.or.matrix.MatrixI#setElementAt(int, int, double)
//     */
//    public void setElementAt(int arg0, int arg1, double arg2) {
//        if (arg0==62 && arg1==62) {
//            System.out.print("\t"+arg2);
//        }
//        super.setElementAt(arg0, arg1, arg2);
//    }
}

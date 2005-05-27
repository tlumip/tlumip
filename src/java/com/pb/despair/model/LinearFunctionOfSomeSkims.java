/*
 * Created on Mar 31, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.model;

import java.util.ArrayList;

import com.pb.despair.pi.SomeSkims;
import com.pb.despair.model.TravelAttributesInterface;
import com.pb.despair.model.TravelUtilityCalculatorInterface;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LinearFunctionOfSomeSkims
    implements TravelUtilityCalculatorInterface {
    
    SomeSkims lastSkims;
    
    ArrayList coefficientsList = new ArrayList();
    ArrayList namesList = new ArrayList();
    double[] coefficients;
    int[] matrixIndices;

    /* (non-Javadoc)
     * @see com.pb.despair.model.TravelUtilityCalculatorInterface#getUtility(com.pb.despair.model.TravelAttributesInterface)
     */
    public double getUtility(int origin, int destination, TravelAttributesInterface travelConditions) {
        if (travelConditions == lastSkims) {
            double utility = 0;
            for (int i=0;i<coefficients.length;i++) {
                utility += coefficients[i]*lastSkims.matrices[matrixIndices[i]].getValueAt(origin,destination);
            }
            return utility;
        }
        if (travelConditions instanceof SomeSkims) {
            lastSkims = (SomeSkims) travelConditions;
            matrixIndices = new int[namesList.size()];
            for (int i=0;i<namesList.size();i++) {
                matrixIndices[i] = lastSkims.getMatrixId((String) namesList.get(i));
            }
            return getUtility(origin, destination, lastSkims);
        }
        else throw new RuntimeException("Can't use LinearFunctionOfSomeSkims with travel attributes of type "+travelConditions.getClass());
    }
    
    public void addSkim(String name, double coefficient) {
        namesList.add(name);
        coefficientsList.add(new Double(coefficient));
        coefficients = new double[coefficientsList.size()];
        // store it in a double array for speed, and an ArrayList for resizing.
        for (int i=0;i<namesList.size();i++) {
            coefficients[i] = ((Double) (coefficientsList.get(i))).doubleValue();
        }
        
    }

    /* (non-Javadoc)
     * @see com.pb.despair.model.TravelUtilityCalculatorInterface#getUtilityComponents(int, int, com.pb.despair.model.TravelAttributesInterface)
     */
    public double[] getUtilityComponents(int origin, int destination, TravelAttributesInterface travelConditions) {
        if (travelConditions == lastSkims) {
            double[] components = new double[coefficients.length];
            for (int i=0;i<coefficients.length;i++) {
                components[i] = coefficients[i]*lastSkims.matrices[matrixIndices[i]].getValueAt(origin,destination);
            }
            return components;
        }
        if (travelConditions instanceof SomeSkims) {
            lastSkims = (SomeSkims) travelConditions;
            matrixIndices = new int[namesList.size()];
            for (int i=0;i<namesList.size();i++) {
                matrixIndices[i] = lastSkims.getMatrixId((String) namesList.get(i));
            }
            return getUtilityComponents(origin, destination, lastSkims);
        }
        else throw new RuntimeException("Can't use LinearFunctionOfSomeSkims with travel attributes of type "+travelConditions.getClass());
    }

}

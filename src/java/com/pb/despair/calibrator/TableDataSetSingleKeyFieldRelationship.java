/*
 * Created on Oct 18, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.tree.DefaultMutableTreeNode;

import com.hbaspecto.calibrator.Ratio;
import com.pb.common.datafile.TableDataSetIndexedValue;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TableDataSetSingleKeyFieldRelationship extends TableDataSetMultipleRatioRelationship {

    private String[] specificElementValues = new String[0];
    private String specificElementField = "";
    private boolean intKey = true;
    private double[] targetValues = new double[0];

    /**
     * 
     */
    public TableDataSetSingleKeyFieldRelationship() {
        super();
    }

    public static void main(String[] args) {
    }

    public DefaultMutableTreeNode getTargets() {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(this);
        TableDataSetIndexedValue denominatorValue = new TableDataSetIndexedValue(commonElements);
        //TableDataSetIndexedValue[] numeratorValues = new TableDataSetIndexedValue[specificElementValues.length];
        if (intKey) denominatorValue.addNewIntKey(specificElementField,specificElementValues);
        else denominatorValue.addNewStringKey(specificElementField,specificElementValues);
        TableDataSetTarget denominator = new TableDataSetTarget();
        denominator.setIndexedValue(denominatorValue);
        double totalValue =0;
        for (int n=0;n<targetValues.length;n++) {
            totalValue+= targetValues[n];
        }
        denominator.setTarget(totalValue);
        //Ratio[] ratios = new Ratio[specificElementValues.length];
        for (int n=0;n<specificElementValues.length;n++) {
            TableDataSetIndexedValue numeratorValue = new TableDataSetIndexedValue(commonElements);
            String[] oneString = new String[1];
            oneString[0] = specificElementValues[n];
            if (intKey) numeratorValue.addNewIntKey(specificElementField,oneString);
            else numeratorValue.addNewStringKey(specificElementField,oneString);
            TableDataSetTarget numerator = new TableDataSetTarget();
            numerator.setIndexedValue(numeratorValue);
            numerator.setTarget(targetValues[n]);
            Ratio ratio = new Ratio(numerator,denominator);
            ratio.setTarget(targetValues[n]/totalValue);
            ratio.setWeight(weight);
            top.add(new DefaultMutableTreeNode(ratio));
        }
        return top;
    }

    public JDialog makeDialog(JFrame f) {
        TableDataSetSingleKeyRelationshipPanel p = new TableDataSetSingleKeyRelationshipPanel();
        p.setMyModel(this);
        JDialog d = new JDialog();
        d.getContentPane().add(p);
        d.setSize(p.getPreferredSize());
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        return d;
    }

    /**
     * @return Returns the specificElementField.
     */
    public String getSpecificElementField() {
        return specificElementField;
    }

    /**
     * @param specificElementField The specificElementField to set.
     */
    public void setSpecificElementField(String specificElementField) {
        this.specificElementField = specificElementField;
    }

    /**
     * @return Returns the specificElementValues.
     */
    public String[] getSpecificElementValues() {
        if (specificElementValues == null) {
            specificElementValues = new String[0];
            targetValues = new double[0];
        }
        return specificElementValues;
    }

    /**
     * @param specificElementValues The specificElementValues to set.
     */
    public void setSpecificElementValues(String[] specificElementValues) {
        this.specificElementValues = specificElementValues;
    }

    /**
     * @return Returns the intKey.
     */
    public boolean isIntKey() {
        return intKey;
    }

    /**
     * @param intKey The intKey to set.
     */
    public void setIntKey(boolean intKey) {
        this.intKey = intKey;
    }

    void setTargetValues(double[] targetValues) {
        this.targetValues = targetValues;
    }

    double[] getTargetValues() {
        if (targetValues == null) {
            specificElementValues = new String[0];
            targetValues = new double[0];
        }
        return targetValues;
    }

    public Object clone() throws CloneNotSupportedException {
        TableDataSetSingleKeyFieldRelationship newOne = (TableDataSetSingleKeyFieldRelationship) super.clone();
        newOne.specificElementValues = (String[]) this.specificElementValues.clone();
        newOne.intKey = this.intKey;
        newOne.targetValues = (double[]) this.targetValues.clone();
        return newOne;
    }
}

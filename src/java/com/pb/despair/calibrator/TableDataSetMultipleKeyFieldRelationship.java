/*
 * Created on Oct 18, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.tree.DefaultMutableTreeNode;

import com.hbaspecto.calibrator.Ratio;
import com.hbaspecto.calibrator.Target;
import com.pb.common.datafile.TableDataSetIndexedValue;

/**
 * @author jabraham
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class TableDataSetMultipleKeyFieldRelationship extends TableDataSetMultipleRatioRelationship {

    private String[][] keysAndValues = new String[2][2];
    private boolean columnKeyIsInt = true;
    private boolean rowKeyIsInt = true;
    String columnKeyFieldName;

    /**
	 * @param columnKeyFieldName
	 *            The columnKeyFieldName to set.
	 */
    void setColumnKeyFieldName(String columnKeyFieldName) {
        this.columnKeyFieldName = columnKeyFieldName;
    }

    /**
	 * @return Returns the keysAndValues.
	 */
    String[][] getKeysAndValues() {
        return keysAndValues;
    }

    /**
	 * @param keysAndValues
	 *            The keysAndValues to set.
	 */
    void setKeysAndValues(String[][] keysAndValues) {
        this.keysAndValues = keysAndValues;
    }

    boolean isRowKeyIsInt() {
        return rowKeyIsInt;
    }

    void setRowKeyIsInt(boolean row1KeyIsInt) {
        this.rowKeyIsInt = row1KeyIsInt;
    }

    /**
	 *  
	 */
    public TableDataSetMultipleKeyFieldRelationship() {
        super();
    }

    public DefaultMutableTreeNode getTargets() {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(this);
        TableDataSetIndexedValue denominatorValue = new TableDataSetIndexedValue(commonElements);
        TableDataSetTarget[][] numeratorTargets = new TableDataSetTarget[keysAndValues.length - 1][keysAndValues[0].length - 1];
        String[] newKeys = new String[keysAndValues[0].length - 1];
        System.arraycopy(keysAndValues[0], 1, newKeys, 0, newKeys.length);
        if (isColumnKeyIsInt()) {
            denominatorValue.addNewIntKey(this.getColumnKeyFieldName(), newKeys);
        } else {
            denominatorValue.addNewStringKey(this.getColumnKeyFieldName(), newKeys);
        }
        newKeys = new String[keysAndValues.length - 1];
        for (int i = 1; i < keysAndValues.length; i++) {
            newKeys[i - 1] = keysAndValues[i][0];
        }
        if (isRowKeyIsInt()) {
            denominatorValue.addNewIntKey(keysAndValues[0][0], newKeys);
        } else {
            denominatorValue.addNewStringKey(keysAndValues[0][0], newKeys);
        }
        double denominatorTarget = 0;
        for (int row = 1; row < keysAndValues.length; row++) {
            for (int column = 1; column < keysAndValues[row].length; column++) {
                TableDataSetIndexedValue newOne = new TableDataSetIndexedValue(commonElements);
                String[] oneString = new String[1];
                oneString[0] = keysAndValues[row][0];
                if (isRowKeyIsInt()) {
                    newOne.addNewIntKey(keysAndValues[0][0], oneString);
                } else {
                    newOne.addNewStringKey(keysAndValues[0][0], oneString);
                }
                oneString[0] = keysAndValues[0][column];
                if (isColumnKeyIsInt()) {
                    newOne.addNewIntKey(getColumnKeyFieldName(), oneString);
                } else {
                    newOne.addNewStringKey(getColumnKeyFieldName(), oneString);
                }
                numeratorTargets[row - 1][column - 1] = new TableDataSetTarget();
                numeratorTargets[row - 1][column - 1].setIndexedValue(newOne);
                numeratorTargets[row - 1][column - 1].setTarget(getValue(row, column));
                denominatorTarget += getValue(row, column);
            }
        }
        Target denominator = null;
        TableDataSetTarget denominatorTDS = new TableDataSetTarget();
        denominatorTDS.setIndexedValue(denominatorValue);
        denominatorTDS.setTarget(denominatorTarget);
        denominator = denominatorTDS;
        if (isGenerateStringIndexedFloatTargets()) {
            denominator = new StringIndexedFloatTarget(denominatorTDS);
        }
        for (int row = 1; row < keysAndValues.length; row++) {
            DefaultMutableTreeNode rowNode = new DefaultMutableTreeNode(keysAndValues[0][0] + " " + keysAndValues[row][0]);
            for (int column = 1; column < keysAndValues[row].length; column++) {
                Target numerator = numeratorTargets[row-1][column-1];
                if (isGenerateStringIndexedFloatTargets()) {
                    numerator = new StringIndexedFloatTarget(numeratorTargets[row-1][column-1]);
                }
                Ratio ratio = new Ratio(numerator, denominator);
                ratio.setTarget(getValue(row, column) / denominatorTarget);
                ratio.setWeight(weight);
                rowNode.add(new DefaultMutableTreeNode(ratio));
            }
            top.add(rowNode);
        }
        return top;
    }

    /**
	 * @param row
	 * @param column
	 * @return
	 */
    private double getValue(int row, int column) {
        return Double.valueOf(keysAndValues[row][column]).doubleValue();
    }

    /**
	 * @return
	 */
    String getColumnKeyFieldName() {
        if (columnKeyFieldName == null) {
            columnKeyFieldName = "";
        }
        return columnKeyFieldName;
    }

    public JDialog makeDialog(JFrame f) {
        TableDataSetMultipleKeyRelationshipPanel p = new TableDataSetMultipleKeyRelationshipPanel();

        p.setMyModel(this);
        JDialog d = new JDialog();
        d.getContentPane().add(p);
        d.setSize(p.getPreferredSize());
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        return d;
    }

    public Object clone() throws CloneNotSupportedException {
        TableDataSetMultipleKeyFieldRelationship newOne = (TableDataSetMultipleKeyFieldRelationship) super.clone();
        newOne.keysAndValues = new String[this.keysAndValues.length][];
        for (int i = 0; i < newOne.keysAndValues.length; i++) {
            newOne.keysAndValues[i] = (String[]) this.keysAndValues[i].clone();
        }
        newOne.columnKeyIsInt = this.columnKeyIsInt;
        newOne.rowKeyIsInt = this.rowKeyIsInt;
        return newOne;
    }

    void setColumnKeyIsInt(boolean column1KeyIsInt) {
        this.columnKeyIsInt = column1KeyIsInt;
    }

    boolean isColumnKeyIsInt() {
        return columnKeyIsInt;
    }
}

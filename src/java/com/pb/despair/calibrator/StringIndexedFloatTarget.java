/*
 * Created on Jun 9, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.hbaspecto.calibrator.ModelInputsAndOutputs;
import com.hbaspecto.calibrator.TargetAdapter;
import com.hbaspecto.calibrator.TargetPanel;
import com.hbaspecto.util.StringArrayTableModel;
import com.pb.common.datafile.TableDataSetCollection;
import com.pb.common.datafile.TableDataSetIndexedValue;
import com.pb.common.matrix.StringIndexedNDimensionalMatrix;

/**
 * @author jabraham
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class StringIndexedFloatTarget extends TargetAdapter implements Serializable {

    private static Logger logger = Logger.getLogger("com.hbaspecto.calibrator");
    transient StringArrayTableModel stringKeysTableModel =null;

    private String[][] stringKeyNameValues;
    static final long serialVersionUID = 1822253333333324622L;
    transient StringIndexedNDimensionalMatrix myMatrix = null;
    private String fieldAndMatrixName;
    private String fileAndTableName;

    private boolean averageMode;
    transient boolean columnOrderOK = false;
    
    /**
     * @param fileAndTableName The fileAndTableName to set.
     */
    void setFileAndTableName(String fileAndTableName) {
        this.fileAndTableName = fileAndTableName;
        myMatrix = null;
        columnOrderOK = false;
    }

    /**
     * @return Returns the fileAndTableName.
     */
    String getFileAndTableName() {
        return fileAndTableName;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
    public Object clone() throws CloneNotSupportedException {
        StringIndexedFloatTarget newOne = (StringIndexedFloatTarget) super.clone();
        newOne.stringKeysTableModel = null;
        newOne.setStringKeyNameValues((String[][]) getStringKeyNameValues().clone());
        for (int i=0;i<getStringKeyNameValues().length;i++) {
            newOne.getStringKeyNameValues()[i] = (String[]) getStringKeyNameValues()[i].clone();
        }
        return newOne;
    }

    public StringIndexedFloatTarget(
        String fileName,
        String[][] stringKeyNameValues,
        String tableName) {
          this.fileAndTableName = fileName;
          this.fieldAndMatrixName = tableName;
          this.setStringKeyNameValues(stringKeyNameValues);
    }

    /**
     * 
     */
    public StringIndexedFloatTarget() {
        setStringKeyNameValues(new String[2][1]);
        myMatrix = null;
        fieldAndMatrixName = "";
        fileAndTableName = "";
    }

    /**
     * @param convertMe The TableDataSet target to be converted to use binary StringIndexedFloat files
     */
    public StringIndexedFloatTarget(TableDataSetTarget convertMe) {
        super(convertMe);
        String[][] oldStringKeyNameValues = convertMe.getIndexedValue().getStringKeyNameValues();
        String[][] intKeyNameValues = convertMe.getIndexedValue().getIntKeyNameValues();
        stringKeyNameValues = new String[oldStringKeyNameValues.length][oldStringKeyNameValues[0].length+intKeyNameValues[0].length];
        for (int i=0;i<stringKeyNameValues.length;i++) {
            stringKeyNameValues[i] = new String[oldStringKeyNameValues[0].length+intKeyNameValues[0].length];
            for (int j=0;j<stringKeyNameValues[i].length;j++) {
                if (j<oldStringKeyNameValues[i].length) {
                    stringKeyNameValues[i][j] = oldStringKeyNameValues[i][j];
                } else {
                    stringKeyNameValues[i][j] = intKeyNameValues[i][j-oldStringKeyNameValues[i].length];
                }
            }
        }
        averageMode = convertMe.getIndexedValue().getValueMode()==TableDataSetIndexedValue.AVERAGE_MODE;
        fileAndTableName = convertMe.getMyTableName();
        fieldAndMatrixName = convertMe.getMyFieldName();
    }

    StringArrayTableModel getStringKeysTableModel() {
        if (stringKeysTableModel == null) {
            stringKeysTableModel = new StringArrayTableModel(getStringKeyNameValues());
            stringKeysTableModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    setStringKeyNameValues(getStringKeysTableModel().getData());
                    columnOrderOK = false;
                }
            });

        }
        return stringKeysTableModel;
    }

    void setMyFieldAndMatrixName(String myFieldName) {
        if (myMatrix == null) {
            fieldAndMatrixName = myFieldName;
        } else {
            if (myMatrix.matrixName!= myFieldName) {
                fieldAndMatrixName = myFieldName;
                myMatrix = null;
                columnOrderOK = false;
            }
        }
    }

    String getMyFieldAndMatrixName() {
        return fieldAndMatrixName;
    }

    public String toString() {
        StringBuffer string = new StringBuffer();
        string.append ("T ");
        for (int i=0;i<getStringKeyNameValues()[0].length; i++) {
            string.append(getStringKeyNameValues()[0][i]);
            string.append(":");
            int j =1;
            for (j=1;j<getStringKeyNameValues().length && j<4;j++) {
                if (j!=1) string.append(",");
                string.append(getStringKeyNameValues()[j][i]);
            }
            if (j<getStringKeyNameValues().length) string.append("..."+(getStringKeyNameValues().length-j)+" more");
            string.append("  ");
        }
        return string.toString();
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see com.hbaspecto.calibrator.TargetAdapter#getValue(com.hbaspecto.calibrator.ModelInputsAndOutputs)
	 */
    public double getValue(ModelInputsAndOutputs y) {
        if (!(y instanceof PecasDirectoryInputsAndOutputs)) {
            logger.severe("StringIndexedFloatTargets can only work with model outputs of of type PecasDirectoryInputsAndOutputs");
            throw new RuntimeException("StringIndexedFloatTargets can only work with model outputs of of type PecasDirectoryInputsAndOutputs");
        }
        PecasDirectoryInputsAndOutputs t = (PecasDirectoryInputsAndOutputs) y;
        StringIndexedNDimensionalMatrix aOne = t.getMatrixByName(t.getDirectory()+File.separator+fileAndTableName+".bin",fieldAndMatrixName);
        if (aOne != myMatrix) {
            myMatrix = aOne;
            columnOrderOK = false;
        }
        if (!columnOrderOK) {
            for (int i =0; i<getStringKeyNameValues()[0].length;i++) {
                int column = myMatrix.getColumn(getStringKeyNameValues()[0][i]);
                if (column !=i) {
                    swapColumns(i,column);
                    i=-1;
                }
            }
        }
        columnOrderOK = true;
        double value =0;
        int num = 0;
        for (int j=1;j<getStringKeyNameValues().length;j++) {
            String[] keys = getStringKeyNameValues()[j];
            value += myMatrix.getValue(keys);
            num++;
        }
        if (averageMode) value/=num;
        return value;
    }

    /**
     * @param i
     * @param column
     */
    private void swapColumns(int column1, int column2) {
        for (int i=0;i<getStringKeyNameValues().length;i++) {
            String temp = getStringKeyNameValues()[i][column1];
            getStringKeyNameValues()[i][column1]=getStringKeyNameValues()[i][column2];
            getStringKeyNameValues()[i][column2]=temp;
        }
        if (stringKeysTableModel!=null) stringKeysTableModel.fireTableDataChanged();
    }

    /**
	 * This method was created in VisualAge.
	 * 
	 * @return javax.swing.JPanel
	 */
    public JPanel createUI() {
        StringIndexedFloatTargetPanel tp = new StringIndexedFloatTargetPanel(this);
        return tp;
    }


    public boolean goingToTakeALongTimeToGetValue() {
        return false;
        // TODO do something more intelligent
    }

    /* (non-Javadoc)
     * @see com.hbaspecto.calibrator.Target#queueUpToGetValue()
     */
    public void queueUpToGetValue() {
        // do nothing
    }

    void setStringKeyNameValues(String[][] stringKeyNameValues) {
        this.stringKeyNameValues = stringKeyNameValues;
        if (stringKeysTableModel!=null) stringKeysTableModel.fireTableStructureChanged();
        columnOrderOK = false;
    }

    String[][] getStringKeyNameValues() {
        if (stringKeyNameValues == null) {
            stringKeyNameValues = new String[1][2];
        }
        return stringKeyNameValues;
    }

    /* (non-Javadoc)
     * @see com.hbaspecto.calibrator.Target#getValueStatusString()
     */
    public String getValueStatusString() {
        if (getModelInputsAndOutputs() == null) return "modelInputsAndOutputs still null";
        if (!(getModelInputsAndOutputs() instanceof PecasDirectoryInputsAndOutputs)) {
            return "StringIndexedFloatTargets can only work with model outputs of of type PecasDirectoryInputsAndOutputs";
        }
        PecasDirectoryInputsAndOutputs t = (PecasDirectoryInputsAndOutputs) getModelInputsAndOutputs();
        StringIndexedNDimensionalMatrix aOne = null;
        try {
            aOne = t.getMatrixByName(t.getDirectory()+File.separator+fileAndTableName+".bin",fieldAndMatrixName);
        } catch (RuntimeException e) {
            return e.toString();
        }
        if (aOne == null) return ("Can't find matrix "+fileAndTableName+ " " +fieldAndMatrixName);
        if (aOne != myMatrix) {
            myMatrix = aOne;
            columnOrderOK = false;
        }
        if (!columnOrderOK) {
            for (int i =0; i<getStringKeyNameValues()[0].length;i++) {
                int column = myMatrix.getColumn(getStringKeyNameValues()[0][i]);
                if (column !=i) {
                    swapColumns(i,column);
                    i=-1;
                }
            }
        }
        columnOrderOK = true;
        double value =0;
        int num = 0;
        for (int j=1;j<getStringKeyNameValues().length;j++) {
            String[] keys = getStringKeyNameValues()[j];
            value += myMatrix.getValue(keys);
            num++;
        }
        if (num==0) return "no entries in matrix";
        if (averageMode) value/=num;
        return String.valueOf(value);
    }

}

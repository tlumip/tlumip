/*
 * Created on Jun 9, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.hbaspecto.calibrator.ModelInputsAndOutputs;
import com.hbaspecto.calibrator.Parameter;
import com.hbaspecto.util.StringArrayTableModel;
import com.pb.common.datafile.TableDataSetCollection;
import com.pb.common.datafile.TableDataSetIndexedValue;

/**
 * @author jabraham
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class TableDataSetParameter extends Parameter {
    
    TableDataSetIndexedValue theData = new TableDataSetIndexedValue();
    transient StringArrayTableModel stringKeysTableModel;
    transient StringArrayTableModel intKeysTableModel;

    static final long serialVersionUID = 1822253368524622L;

    public TableDataSetParameter(
        String tableName,
        String[] stringKeyNames,
        String[] intKeyNames,
        String[][] stringIndexValues,
        int[][] intIndexValues,
        String columnName) {

        theData = new TableDataSetIndexedValue(tableName,stringKeyNames,intKeyNames,stringIndexValues,intIndexValues,columnName);
    }

    public TableDataSetParameter() {
    }

    double perturbation = .01;

    public double retrieveValue(ModelInputsAndOutputs y) {
        if (!(y instanceof TableDataSetInputsAndOutputs)) {
            throw new RuntimeException("TableDataSetParameter can only work with ModelInputsAndOutputs of type TableDataSetCollection");
        }
        setValue(theData.retrieveValue(((TableDataSetInputsAndOutputs) y).myTableDataSetCollection));
        return value;
    }

    public void putValue(ModelInputsAndOutputs y) {
        if (!(y instanceof TableDataSetInputsAndOutputs)) {
            throw new RuntimeException("TableDataSetParameter can only work with ModelInputsAndOutputs of type TableDataSetInputsAndOutputs");
        }
        theData.putValue(((TableDataSetInputsAndOutputs) y).myTableDataSetCollection, (float) value);
    }

    public JPanel createUI() {
        TableDataSetIndexedValuePanel myPanel = new TableDataSetIndexedValuePanel();
        myPanel.setMyParam(theData);
        //TableDataSetParameterPanel myPanel = new TableDataSetParameterPanel();
        //myPanel.setMyParam(this);
        return myPanel;
    }

    public double perturbDown() {
        value -= perturbation;
        return -perturbation;
    }

    public double perturbUp() {
        value += perturbation;
        return perturbation;
    }

    void setMyTableName(String myTableName) {
        theData.setMyTableName(myTableName);
    }

    String getMyTableName() {
        return theData.getMyTableName();
    }

    public synchronized void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        getPropertyChange().addPropertyChangeListener(listener);
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        getPropertyChange().firePropertyChange(propertyName, oldValue, newValue);
    }

    protected java.beans.PropertyChangeSupport getPropertyChange() {
        if (propertyChange == null) {
            propertyChange = new java.beans.PropertyChangeSupport(this);
        };
        return propertyChange;
    }

    StringArrayTableModel getStringKeysTableModel() {
        if (stringKeysTableModel == null) {
            stringKeysTableModel = new StringArrayTableModel(theData.getStringKeyNameValues());
            stringKeysTableModel.addTableModelListener( new TableModelListener() {
              public void tableChanged(TableModelEvent e) {
                  theData.setStringKeyNameValues(getStringKeysTableModel().getData());
                }
              });
            
        }
        return stringKeysTableModel;
    }


    StringArrayTableModel getIntKeysTableModel() {
        if (intKeysTableModel == null) {
            intKeysTableModel = new StringArrayTableModel(theData.getIntKeyNameValues());
            intKeysTableModel.addTableModelListener( new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    try {
                        theData.setIntKeyNameValues(getIntKeysTableModel().getData());
                    } catch (NumberFormatException ee) {
                        // don't do anything if user puts a non-int in there temporarily
                    }
                }

            });
            
        }
        return intKeysTableModel;
    }

    void setMyFieldName(String myFieldName) {
        theData.setMyFieldName(myFieldName);
    }

    String getMyFieldName() {
        return theData.getMyFieldName();
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return ("P "+theData.toString());
    }
}

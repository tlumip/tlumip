/*
 * Created on Jun 9, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import java.util.*;
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

/**
 * @author jabraham
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class TableDataSetTarget extends TargetAdapter {

    private TableDataSetIndexedValue indexedValue = new TableDataSetIndexedValue();
    transient StringArrayTableModel stringKeysTableModel;
    transient StringArrayTableModel intKeysTableModel;

    static final long serialVersionUID = 18222525268524622L;


    /*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
    public Object clone() throws CloneNotSupportedException {
        TableDataSetIndexedValue newOne = (TableDataSetIndexedValue) super.clone();
        stringKeysTableModel = null;
        intKeysTableModel = null;
        return newOne;
    }

    public TableDataSetTarget(
        String tableName,
        String[] stringKeyNames,
        String[] intKeyNames,
        String[][] stringIndexValues,
        int[][] intIndexValues,
        String columnName) {

        indexedValue = new TableDataSetIndexedValue(tableName, stringKeyNames, intKeyNames, stringIndexValues, intIndexValues, columnName);
    }

    public TableDataSetTarget() {
        super();
    }

    //    public double retrieveValue(ModelInputsAndOutputs y) {
    //        if (!(y instanceof TableDataSetInputsAndOutputs)) {
    //            throw new RuntimeException("TableDataSetTarget can only work with
	// ModelInputsAndOutputs of type TableDataSetInputsAndOutputs");
    //        }
    //        return indexedValue.retrieveValue(((TableDataSetInputsAndOutputs)
	// y).myTableDataSetCollection);
    //    }

    void setMyTableName(String myTableName) {
        indexedValue.setMyTableName(myTableName);
    }

    String getMyTableName() {
        return indexedValue.getMyTableName();
    }

    StringArrayTableModel getStringKeysTableModel() {
        if (stringKeysTableModel == null) {
            stringKeysTableModel = new StringArrayTableModel(indexedValue.getStringKeyNameValues());
            stringKeysTableModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    indexedValue.setStringKeyNameValues(getStringKeysTableModel().getData());
                }
            });

        }
        return stringKeysTableModel;
    }

    StringArrayTableModel getIntKeysTableModel() {
        if (intKeysTableModel == null) {
            intKeysTableModel = new StringArrayTableModel(indexedValue.getIntKeyNameValues());
            intKeysTableModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    try {
                        indexedValue.setIntKeyNameValues(getIntKeysTableModel().getData());
                    } catch (NumberFormatException ee) {
                        // don't do anything if user puts a non-int in there
						// temporarily
                    }
                }

            });

        }
        return intKeysTableModel;
    }

    void setMyFieldName(String myFieldName) {
        indexedValue.setMyFieldName(myFieldName);
    }

    String getMyFieldName() {
        return indexedValue.getMyFieldName();
    }

    public String toString() {
        return ("T " + indexedValue.toString());
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see com.hbaspecto.calibrator.TargetAdapter#getValue(com.hbaspecto.calibrator.ModelInputsAndOutputs)
	 */
    public double getValue(ModelInputsAndOutputs y) {
        if (!(y instanceof TableDataSetInputsAndOutputs)) {
            throw new RuntimeException("TableDataSetTargets can only work with model outputs of of type TableDataSetInputsAndOutputs");
        }
        return indexedValue.retrieveValue(((TableDataSetInputsAndOutputs) y).myTableDataSetCollection);
    }

    /**
	 * This method was created in VisualAge.
	 * 
	 * @return javax.swing.JPanel
	 */
    public JPanel createUI() {
        TargetPanel t = new TargetPanel();
        t.setTheTarget(this);
        JPanel thePanel = new JPanel();
        thePanel.setLayout(new BoxLayout(thePanel, BoxLayout.LINE_AXIS));
        thePanel.add(t);
        TableDataSetIndexedValuePanel p = new TableDataSetIndexedValuePanel();
        p.setMyParam(indexedValue);
        thePanel.add(p);
        thePanel.setSize(600, 300);
        return thePanel;
    }

    void setIndexedValue(TableDataSetIndexedValue indexedValue) {
        this.indexedValue = indexedValue;
    }

    TableDataSetIndexedValue getIndexedValue() {
        return indexedValue;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see com.hbaspecto.calibrator.Target#queueUpToGetValue()
	 */
    public void queueUpToGetValue() {
        queueUpTargetRetrieval(this);

    }

    private static Thread targetRetrievalThread = null;
    private static Vector targetsToBeRetrieved = new Vector();
    private static void queueUpTargetRetrieval(TableDataSetTarget aTarget) {
        if (targetRetrievalThread == null) {
            targetRetrievalThread = new Thread() {
                public void run() {
                    while (true) {
                        TableDataSetTarget t = null;
                        synchronized(this) {
                            while (targetsToBeRetrieved.size() == 0) {
                                try {
                                    wait();
                                } catch (InterruptedException e) {
                                }
                            }
                            t = (TableDataSetTarget) targetsToBeRetrieved.get(0);
                            targetsToBeRetrieved.remove(t);
                        }
                        try {
                            if (t.getModelInputsAndOutputs() instanceof TableDataSetInputsAndOutputs) {
                               TableDataSetInputsAndOutputs tdsio = (TableDataSetInputsAndOutputs) t.getModelInputsAndOutputs();
                               t.getIndexedValue().retrieveValue(tdsio.myTableDataSetCollection);
                            }
                        } catch (Exception e) {
                            System.out.println("error "+e);
                            e.printStackTrace();
                        }
                    }
                }

            };
            targetRetrievalThread.start();
        }
        targetsToBeRetrieved.add(aTarget);
        synchronized (targetRetrievalThread) {
            targetRetrievalThread.notify();
        }
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see com.hbaspecto.calibrator.Target#goingToTakeALongTimeToGetValue()
	 */
    public boolean goingToTakeALongTimeToGetValue() {
        ModelInputsAndOutputs mio = getModelInputsAndOutputs();
        if (mio == null)
            return false;
        if (!(mio instanceof TableDataSetInputsAndOutputs))
            return false;
        TableDataSetCollection tdsc = ((TableDataSetInputsAndOutputs) mio).myTableDataSetCollection;
        return !(indexedValue.hasValidIndexes(tdsc));
    }

}

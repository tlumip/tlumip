/*
 * Created on Jun 9, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

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

    /*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
    protected Object clone() throws CloneNotSupportedException {
        TableDataSetParameter newOne = (TableDataSetParameter) super.clone();
        newOne.theData = (TableDataSetIndexedValue) this.theData.clone();
        stringKeysTableModel = null;
        intKeysTableModel = null;
        return newOne;
    }

    public TableDataSetParameter(
        String tableName,
        String[] stringKeyNames,
        String[] intKeyNames,
        String[][] stringIndexValues,
        int[][] intIndexValues,
        String columnName) {

        theData = new TableDataSetIndexedValue(tableName, stringKeyNames, intKeyNames, stringIndexValues, intIndexValues, columnName);
    }

    public TableDataSetParameter() {
    }

    double perturbation = .01;

    public double retrieveValue(ModelInputsAndOutputs y) {
        if (!(y instanceof PecasDirectoryInputsAndOutputs)) {
            throw new RuntimeException("TableDataSetParameter can only work with ModelInputsAndOutputs of type TableDataSetCollection");
        }
        setValue(theData.retrieveValue(((PecasDirectoryInputsAndOutputs) y).myTableDataSetCollection));
        return value;
    }

    public void putValue(ModelInputsAndOutputs y) {
        if (!(y instanceof PecasDirectoryInputsAndOutputs)) {
            throw new RuntimeException("TableDataSetParameter can only work with ModelInputsAndOutputs of type PecasDirectoryInputsAndOutputs");
        }
        theData.putValue(((PecasDirectoryInputsAndOutputs) y).myTableDataSetCollection, (float) value);
    }

    public JPanel createUI() {
        TableDataSetIndexedValuePanel myPanel = new TableDataSetIndexedValuePanel();
        myPanel.setMyParam(theData);
        JPanel perturbValuePanel = new JPanel();
        perturbValuePanel.setLayout(new GridLayout(2, 2));
        perturbValuePanel.add(new JLabel("Perturbation value"));
        JFormattedTextField perturbField = new JFormattedTextField(NumberFormat.getNumberInstance());
        perturbField.setColumns(5);
        perturbField.setValue(new Double(perturbation));
        perturbField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent arg0) {
                changeValue(arg0);
            }
            public void insertUpdate(DocumentEvent arg0) {
                changeValue(arg0);
            }
            public void removeUpdate(DocumentEvent arg0) {
                changeValue(arg0);
            }
            private void changeValue(DocumentEvent arg0) {
                Document doc = arg0.getDocument();
                String text = null;
                try {
                    text = doc.getText(doc.getStartPosition().getOffset(), doc.getLength());
                    perturbation = Double.valueOf(text).doubleValue();
                } catch (NumberFormatException e) {
                    // jsut don't change it until user enters correct value
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                    text = "";
                }
                System.out.println("perturbation now "+perturbation);
            }
        });
        perturbValuePanel.add(perturbField);
        
        perturbValuePanel.add(new JLabel("Coefficient value"));
        JFormattedTextField valueField = new JFormattedTextField(NumberFormat.getNumberInstance());
        valueField.setColumns(10);
        valueField.setValue(new Double(getValue()));
        valueField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent arg0) {
                changeValue(arg0);
            }
            public void insertUpdate(DocumentEvent arg0) {
                changeValue(arg0);
            }
            public void removeUpdate(DocumentEvent arg0) {
                changeValue(arg0);
            }
            private void changeValue(DocumentEvent arg0) {
                Document doc = arg0.getDocument();
                String text = null;
                try {
                    text = doc.getText(doc.getStartPosition().getOffset(), doc.getLength());
                    setValue(Double.valueOf(text).doubleValue());
                } catch (NumberFormatException e) {
                    // just don't change it until user enters valid number
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                    text = "";
                }
                System.out.println("coefficient value now "+getValue());
            }
        });
        perturbValuePanel.add(valueField);
        JPanel bothThings = new JPanel();
        bothThings.setLayout(new FlowLayout());
        bothThings.add(myPanel);
        bothThings.add(perturbValuePanel);
        bothThings.setPreferredSize(new Dimension(100, 100));
        return bothThings;
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
            stringKeysTableModel.addTableModelListener(new TableModelListener() {
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
            intKeysTableModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    try {
                        theData.setIntKeyNameValues(getIntKeysTableModel().getData());
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
        theData.setMyFieldName(myFieldName);
    }

    String getMyFieldName() {
        return theData.getMyFieldName();
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
    public String toString() {
        return ("P " + theData.toString());
    }
}

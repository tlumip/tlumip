/*
 * Created on Oct 19, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.hbaspecto.util.StringArrayTableModel;
import com.hbaspecto.util.TableTransferHandler;


/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TableDataSetMultipleKeyRelationshipPanel extends TableDataSetMultipleRatioRelationshipPanel implements ActionListener, DocumentListener{

    TableDataSetMultipleKeyFieldRelationship myModel;
    CrossTabKeyTable specificsTable;
    JTextField columnTitleFieldName;
    JCheckBox columnKeyIsInt;
    JCheckBox rowKeyIsInt;
    JButton setNumberOfRowsButton;
    JButton setNumberOfColumnsButton;
    JTextField numberOfRowsOrColumnsField;
    private JTextField nameField;

    
    /**
     * @return Returns the specificsTable.
     */
    CrossTabKeyTable getSpecificsTable() {
        if (specificsTable == null) {
            specificsTable = new CrossTabKeyTable(getMyModel2().getKeysAndValues());
        }
        return specificsTable;
    }

    /**
     * @return Returns the numberOfRowsOrColumnsField.
     */
    JTextField getNumberOfRowsOrColumnsField() {
        if (numberOfRowsOrColumnsField == null) {
            numberOfRowsOrColumnsField = new JTextField(10);
        }
        return numberOfRowsOrColumnsField;
    }

    /**
     * @return Returns the columnKeyIsInt.
     */
    JCheckBox getColumnKeyIsInt() {
        if (columnKeyIsInt == null) {
            columnKeyIsInt = new JCheckBox();
        }
        return columnKeyIsInt;
    }

    /**
     * @return Returns the columnTitleFieldName.
     */
    JTextField getColumnTitleFieldName() {
        if (columnTitleFieldName == null) {
            columnTitleFieldName = new JTextField(10);
        }
        return columnTitleFieldName;
    }

    /**
     * @return Returns the rowKeyIsInt.
     */
    JCheckBox getRowKeyIsInt() {
        if (rowKeyIsInt == null) {
            rowKeyIsInt = new JCheckBox();
        }
        return rowKeyIsInt;
    }

    /**
     * @return Returns the setNumberOfColumns.
     */
    JButton getSetNumberOfColumnsButton() {
        if (setNumberOfColumnsButton == null) {
            setNumberOfColumnsButton = new JButton();
            setNumberOfColumnsButton.setActionCommand("setNumberOfColumns");
            setNumberOfColumnsButton.setText("set number of colunns");
        }
        return setNumberOfColumnsButton;
    }

    /**
     * @return Returns the setNumberOfRows.
     */
    JButton getSetNumberOfRowsButton() {
        if (setNumberOfRowsButton == null) {
            setNumberOfRowsButton = new JButton();
            setNumberOfRowsButton.setActionCommand("setNumberOfRows");
            setNumberOfRowsButton.setText("set number of rows");
        }
        return setNumberOfRowsButton;
    }

    public TableDataSetMultipleKeyRelationshipPanel() {
        super();
        initialize();
    }



    public JFormattedTextField getWeightField() {
        if (weight == null) {
            weight = new JFormattedTextField(NumberFormat.getNumberInstance());
            weight.setColumns(5);
            weight.setValue(new Double(getMyModel().getWeight()));
            weight.getDocument().addDocumentListener(this);
        }
        return weight;
    }
    
    public JTextField getNameField() {
        if (nameField == null) {
            nameField = new JFormattedTextField();
            nameField.setColumns(5);
            nameField.setText(getMyModel().getName());
            nameField.getDocument().addDocumentListener(this);
        }
        return nameField;
    }
    
    public static void main(String[] args) {
        JFrame dialog = new JFrame();
        dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        TableDataSetMultipleKeyRelationshipPanel aPanel = new TableDataSetMultipleKeyRelationshipPanel();
        dialog.getContentPane().add(aPanel);
        dialog.setSize(aPanel.getPreferredSize());
        dialog.setVisible(true);
    }

    /**
     * @return Returns the myModel.
     */
    public TableDataSetMultipleRatioRelationship getMyModel() {
        if (myModel == null) {
            myModel = new TableDataSetMultipleKeyFieldRelationship();
            setMyModel(myModel);
        }
        return myModel;
    }
    public TableDataSetMultipleKeyFieldRelationship getMyModel2() {
        return (TableDataSetMultipleKeyFieldRelationship) getMyModel();
    }
    
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == getColumnKeyIsInt()) {
            getMyModel2().setColumnKeyIsInt(getColumnKeyIsInt().isSelected());
        }
        if (event.getSource() == getRowKeyIsInt()) {
            getMyModel2().setRowKeyIsInt(getRowKeyIsInt().isSelected());
        }
    }
    
    boolean initialized;
    void initialize() {
        if (!initialized) {
            initialized = true;
            super.initialize();
            JTable keysTable = new JTable(getSpecificsTable());
            keysTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            keysTable.setDragEnabled(true);
            keysTable.setTransferHandler(new TableTransferHandler());
            specificsPanel = new JPanel();
            specificsPanel.setLayout(new BorderLayout());
            
            JScrollPane keysPane = new JScrollPane(keysTable);
            keysPane.setPreferredSize(new Dimension(200,200));
            keysPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(1),"Key / Target"));
            specificsPanel.add(keysPane, BorderLayout.CENTER);
            getSetNumberOfRowsButton().addActionListener(specificsTable);
            getSetNumberOfColumnsButton().addActionListener(specificsTable);
            JPanel buttonPanel = new JPanel(new GridLayout(2,2));
            buttonPanel.add(getNumberOfRowsOrColumnsField());
            buttonPanel.add(new JLabel("Number"));
            buttonPanel.add(getSetNumberOfRowsButton());
            buttonPanel.add(getSetNumberOfColumnsButton());
            JPanel southPanel = new JPanel();
            southPanel.add(buttonPanel);
            JPanel jp = new JPanel(new GridLayout(2,2));
            jp.add(getColumnTitleFieldName());
            getColumnTitleFieldName().getDocument().addDocumentListener(this);
            jp.add(new JLabel("Key field for column key (first row keys this"));
            southPanel.add(jp);
            jp = new JPanel(new GridLayout(2,0));
            jp.add(getWeightField());
            jp.add(new JLabel("Weight"));
            southPanel.add(jp);
            jp = new JPanel(new GridLayout(2,2));
            jp.add(getRowKeyIsInt());
            jp.add(new JLabel("Row key is int (values in column 1)"));
            getRowKeyIsInt().addActionListener(this);
            jp.add(getColumnKeyIsInt());
            jp.add(new JLabel("Column key is int (values in row 1)"));
            getColumnKeyIsInt().addActionListener(this);
            southPanel.add(jp);
            jp = new JPanel(new FlowLayout());
            jp.add(getNameField());
            jp.add(new JLabel("Name of target set"));
            southPanel.add(jp);
            southPanel.setPreferredSize(new Dimension(500,200));
            specificsPanel.add(southPanel, BorderLayout.SOUTH);
            specificsPanel.setPreferredSize(new Dimension(500,500));
            this.add(specificsPanel);
        }

       }
    
    class CrossTabKeyTable extends StringArrayTableModel implements ActionListener {

        /**
         * 
         */
        CrossTabKeyTable() {
            super();
        }

        /**
         * @param d
         */
        CrossTabKeyTable(String[][] d) {
            super(d);
        }

        /* (non-Javadoc)
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals("setNumberOfRows")) {
            int numberOfRows = Integer.valueOf(getNumberOfRowsOrColumnsField().getText()).intValue();
            if (numberOfRows < 2) numberOfRows = 2;
            this.setRowCount(numberOfRows);
            getMyModel2().setKeysAndValues(this.getData());
        }
        if (event.getActionCommand().equals("setNumberOfColumns")) {
            int numberOfColumns = Integer.valueOf(getNumberOfRowsOrColumnsField().getText()).intValue();
            if (numberOfColumns < 2) numberOfColumns = 2;
            this.setColumnCount(numberOfColumns);
            getMyModel2().setKeysAndValues(this.getData());
           }
        
        }};


    /**
     * @param myModel The myModel to set.
     */
    public void setMyModel(TableDataSetMultipleKeyFieldRelationship myModel) {
        this.myModel = myModel;
        getWeightField().setValue(new Double(getMyModel().getWeight()));
        getColumnTitleFieldName().setText(myModel.getColumnKeyFieldName());
        getSpecificsTable().setData(myModel.getKeysAndValues());
        getColumnKeyIsInt().setSelected(myModel.isColumnKeyIsInt());
        getRowKeyIsInt().setSelected(myModel.isRowKeyIsInt());
        super.setMyModel(myModel);
    }

    public void changedUpdate(DocumentEvent event) {
        handleDocumentEvent(event);
    }

    public void insertUpdate(DocumentEvent event) {
        handleDocumentEvent(event);
    }

    public void removeUpdate(DocumentEvent event) {
        handleDocumentEvent(event);
    }
    
    private void handleDocumentEvent(DocumentEvent event) {
        if (event.getDocument() == getWeightField().getDocument()) {
            getMyModel().setWeight(((Number) getWeightField().getValue()).doubleValue());
        }
        if (event.getDocument() == getColumnTitleFieldName().getDocument()) {
            getMyModel2().setColumnKeyFieldName(getColumnTitleFieldName().getText());
        }
        if (event.getDocument() == getNameField().getDocument()) {
            getMyModel().setName(getNameField().getText());
        }
        
    }

}

    

package com.pb.despair.calibrator;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.hbaspecto.util.ColumnTitledStringArrayTableModel;
import com.hbaspecto.util.StringArrayTableModel;
import com.hbaspecto.util.TableTransferHandler;
import com.pb.common.datafile.TableDataSetIndexedValue;

public class TableDataSetIndexedValuePanel extends JPanel implements javax.swing.event.CaretListener, java.beans.PropertyChangeListener {
    private TableDataSetIndexedValue myParam = null;
    private JTextField tableNameField = new JTextField();
    private JTextField valueFieldNameField = new JTextField();
    private transient PropertyChangeSupport propertyChange = null;
    private JTable stringKeyTable;
    private JTable intKeyTable;

    public TableDataSetIndexedValuePanel() {
        super();
        initialize();
    }

    public TableDataSetIndexedValuePanel(java.awt.LayoutManager layout) {
        super(layout);
        initialize();
    }

    public TableDataSetIndexedValuePanel(java.awt.LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
        initialize();
    }

    public TableDataSetIndexedValuePanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        initialize();
    }

    public synchronized void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        getPropertyChange().addPropertyChangeListener(listener);
    }

    /**
	 * Method to handle events for the CaretListener interface.
	 * 
	 * @param e
	 *            javax.swing.event.CaretEvent
	 */
    public void caretUpdate(javax.swing.event.CaretEvent e) {
        if ((e.getSource() == getTableNameField())) {
            updateModelTableName();
        }
        if ((e.getSource() == getValueFieldNameField())) {
            updateModelFieldName();
        }
        // user code begin {2}
        // user code end
    }
    private void updateModelTableName() {
        getMyParam().setMyTableName(getTableNameField().getText());
    }

    private void updateModelFieldName() {
        getMyParam().setMyFieldName(getValueFieldNameField().getText());
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        getPropertyChange().firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
	 * Method generated to support the promotion of the JLabel1Text attribute.
	 * 
	 * @return java.lang.String
	 */
    public String getTableName() {
        return getTableNameField().getText();
    }

    private JTextField getTableNameField() {
        if (tableNameField == null) {
            tableNameField = new JTextField(10);
            tableNameField.setSize(50, 200);
        }
        return tableNameField;
    }

    private JTextField getValueFieldNameField() {
        if (valueFieldNameField == null) {
            valueFieldNameField = new JTextField(10);
            valueFieldNameField.setSize(200, 50);
        }
        return valueFieldNameField;
    }

    private void initConnections() {
        this.addPropertyChangeListener(this);
        getTableNameField().addCaretListener(this);
        getTableNameField().addPropertyChangeListener(this);
        getValueFieldNameField().addCaretListener(this);
        getValueFieldNameField().addPropertyChangeListener(this);
    }

    /**
	 * Initialize the class.
	 */
    private void initialize() {
        setName("TableDataSet Panel");
        setLayout(new java.awt.GridLayout());
        setPreferredSize(new Dimension(500, 300));
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Basics", getBasicsPanel());
        tabbedPane.addTab("Keys-Values", getKeysPanel());
        add(tabbedPane);
        //        add(getTableNameField(), getTableNameField().getName());
        //        add(getTableNameLabel());
        //        add(getStringKeyTable());
        //        add(getIntKeyTable());
        initConnections();
    }

    JRadioButton singleRowButton = null;
    JRadioButton sumButton = null;
    JRadioButton averageButton = null;

    private Box basicsPanel = null;
    private JPanel keysPanel = null;
    private JPanel intKeyPanel;
    private JPanel stringKeyPanel;
    public JComponent getBasicsPanel() {
        if (basicsPanel == null) {
            basicsPanel = Box.createVerticalBox();
            basicsPanel.add(new JLabel("Table name"));
            basicsPanel.add(getTableNameField(), getTableNameField().getName());
            basicsPanel.add(new JLabel("Field name containing parameter"));
            basicsPanel.add(getValueFieldNameField());
            ButtonGroup group = new ButtonGroup();
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new GridLayout(0, 3));
            singleRowButton = new JRadioButton("Single row");
            singleRowButton.setActionCommand("SINGLE");
            if (getMyParam().getValueMode() == myParam.SINGLE_VALUE_MODE)
                singleRowButton.setSelected(true);
            singleRowButton.setToolTipText("Index values must map to only one row in TableDataSet");
            group.add(singleRowButton);
            buttonPanel.add(singleRowButton);
            singleRowButton.addActionListener(getModeActionListener());
            sumButton = new JRadioButton("Sum / divide");
            sumButton.setActionCommand("SUM");
            sumButton.setToolTipText("Multiple matches are summed when retrieved, and value is divided by number of matches when saved");
            if (getMyParam().getValueMode() == myParam.SUM_MODE)
                singleRowButton.setSelected(true);
            group.add(sumButton);
            buttonPanel.add(sumButton);
            sumButton.addActionListener(getModeActionListener());
            averageButton = new JRadioButton("Average / same");
            averageButton.setActionCommand("AVERAGE");
            averageButton.setToolTipText("Multiple matches are averaged when retrieved, and value is placed in all matches when saved");
            if (getMyParam().getValueMode() == myParam.AVERAGE_MODE)
                singleRowButton.setSelected(true);
            group.add(averageButton);
            buttonPanel.add(averageButton);
            averageButton.addActionListener(getModeActionListener());
            basicsPanel.add(buttonPanel);
            basicsPanel.add(Box.createVerticalGlue());
            basicsPanel.setPreferredSize(new Dimension(300, 300));
        }
        return basicsPanel;
    }

    ActionListener modeActionListener = null;
    public ActionListener getModeActionListener() {
        if (modeActionListener == null) {
            modeActionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand().equals("SINGLE")) {
                        getMyParam().setValueMode(getMyParam().SINGLE_VALUE_MODE);
                        getAddKeyRowAction().setEnabled(false);
                    }
                    if (e.getActionCommand().equals("SUM")) {
                        getMyParam().setValueMode(getMyParam().SUM_MODE);
                        getAddKeyRowAction().setEnabled(true);
                    }
                    if (e.getActionCommand().equals("AVERAGE")) {
                        getMyParam().setValueMode(getMyParam().AVERAGE_MODE);
                        getAddKeyRowAction().setEnabled(true);
                    }
                }
            };
        }
        return modeActionListener;
    }

    public JPanel getKeysPanel() {
        if (keysPanel == null) {
            keysPanel = new JPanel();
            keysPanel.setLayout(new BoxLayout(keysPanel, BoxLayout.PAGE_AXIS));
            keysPanel.add(getStringKeyPanel());
            keysPanel.add(getIntKeyPanel());
            keysPanel.setPreferredSize(new Dimension(300, 300));
        }
        return keysPanel;
    }

    ColumnTitledStringArrayTableModel intKeysTableModel = null;

    ColumnTitledStringArrayTableModel getIntKeysTableModel() {
        if (intKeysTableModel == null) {
            intKeysTableModel = new ColumnTitledStringArrayTableModel(getMyParam().getIntKeyNameValues());
            intKeysTableModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    //TODO make this faster; if only some values have changed don't get all data over again
                    // e has info about what has actually changed
                    getMyParam().updateIntKeys(e,getIntKeysTableModel().getDataWithHeaders());
                    //getMyParam().setIntKeyNameValues(getIntKeysTableModel().getDataWithHeaders());
                }
            });
        }
        return intKeysTableModel;

    }

    ColumnTitledStringArrayTableModel stringKeysTableModel = null;

    ColumnTitledStringArrayTableModel getStringKeysTableModel() {
        if (stringKeysTableModel == null) {
            stringKeysTableModel = new ColumnTitledStringArrayTableModel(getMyParam().getStringKeyNameValues());
            stringKeysTableModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    getMyParam().setStringKeyNameValues(getStringKeysTableModel().getDataWithHeaders());
                }
            });
        }
        return stringKeysTableModel;

    }

    class AddIntKeyAction extends AbstractAction {
        AddIntKeyAction() {
            super("add key");
        }
        public void actionPerformed(ActionEvent arg0) {
            StringArrayTableModel intKeys = getIntKeysTableModel();
            intKeys.setColumnCount(intKeys.getColumnCount() + 1);
        }
    }

    DeleteKeyRowAction deleteKeyRowAction = null;
    DeleteKeyRowAction getDeleteKeyRowAction() {
        if (deleteKeyRowAction == null) {
            deleteKeyRowAction = new DeleteKeyRowAction();
        }
        if (getIntKeysTableModel().getRowCount() < 1) {
            deleteKeyRowAction.setEnabled(false);
        } else {
            deleteKeyRowAction.setEnabled(true);
        }
        return deleteKeyRowAction;
    }

    class DeleteKeyRowAction extends AbstractAction {
        DeleteKeyRowAction() {
            super("delete row");
        }
        public void actionPerformed(ActionEvent arg0) {
            StringArrayTableModel intKeys = getIntKeysTableModel();
            int rows = intKeys.getRowCount();
            if (rows > 1)
                intKeys.setRowCount(rows - 1);
            StringArrayTableModel stringKeys = getStringKeysTableModel();
            stringKeys.setRowCount(rows - 1);
            if (rows <= 2)
                getDeleteKeyRowAction().setEnabled(false);
        }
    }
    class DeleteIntKeyAction extends AbstractAction {
        DeleteIntKeyAction() {
            super("remove key");
        }

        public void actionPerformed(ActionEvent arg0) {
            StringArrayTableModel intKeys = getIntKeysTableModel();
            intKeys.setColumnCount(Math.max(0, intKeys.getColumnCount() - 1));
        }
    }

    class AddKeyAction extends AbstractAction {
        AddKeyAction() {
            super("add key");
        }

        public void actionPerformed(ActionEvent arg0) {
            TableDataSetIndexedValuePanel.this.showAddKeyDialog();
        }
    }
    
    AddKeyAction addKeyAction = null;
    AddKeyAction getAddKeyAction() {
        if (addKeyAction == null) {
            addKeyAction = new AddKeyAction();
        }
        return addKeyAction;
    }

    JDialog addKeyDialog = null;
    JTextField keyName = null;
    JTextField getKeyName() {
        if (keyName == null) {
            keyName = new JTextField(10);
        }
        return keyName;
    }

    JDialog getAddKeyDialog() {
        if (addKeyDialog == null) {
            addKeyDialog = new JDialog();
            addKeyDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            Container cp = addKeyDialog.getContentPane();
            cp.setLayout(new GridLayout(0,1));
            cp.add(new JLabel("Enter key name"));
            cp.add(getKeyName());
            JPanel buttons = new JPanel();
            JButton intKeyButton = new JButton("Int key");
            JButton stringKeyButton = new JButton("String key");
            buttons.add(intKeyButton);
            buttons.add(stringKeyButton);
            cp.add(buttons);
            stringKeyButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {
                    String keyField = keyName.getText();
                    ColumnTitledStringArrayTableModel tm = getStringKeysTableModel();
                    tm.setColumnCount(tm.getColumnCount() + 1);
                    tm.setColumnName(tm.getColumnCount() - 1, keyField);
                    addKeyDialog.setVisible(false);
                }
            });
            intKeyButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {
                    String keyField = keyName.getText();
                    ColumnTitledStringArrayTableModel tm = getIntKeysTableModel();
                    tm.setColumnCount(tm.getColumnCount() + 1);
                    tm.setColumnName(tm.getColumnCount() - 1, keyField);
                    addKeyDialog.setVisible(false);
                }
            });
            addKeyDialog.setModal(true);
            addKeyDialog.pack();
            
            
        }
    return addKeyDialog;

    }

    public void showAddKeyDialog() {
        getAddKeyDialog().setVisible(true);
    }

    class DeleteStringKeyAction extends AbstractAction {
        DeleteStringKeyAction() {
            super("remove key");
        }

        public void actionPerformed(ActionEvent arg0) {
            StringArrayTableModel stringKeys = getStringKeysTableModel();
            stringKeys.setColumnCount(Math.max(0, stringKeys.getColumnCount() - 1));
        }
    }

    AddKeyRowAction addKeyRowAction = null;
    AddKeyRowAction getAddKeyRowAction() {
        if (addKeyRowAction == null) {
            addKeyRowAction = new AddKeyRowAction();
        }
        if (getMyParam().getValueMode() == getMyParam().SINGLE_VALUE_MODE) {
            addKeyRowAction.setEnabled(false);
        }
        return addKeyRowAction;
    }

    class AddKeyRowAction extends AbstractAction {
        AddKeyRowAction() {
            super("add row");
        }
        public void actionPerformed(ActionEvent arg0) {
            StringArrayTableModel stringKeys = getStringKeysTableModel();
            int rows = stringKeys.getRowCount() + 1;
            stringKeys.setRowCount(rows);
            StringArrayTableModel intKeys = getIntKeysTableModel();
            intKeys.setRowCount(rows);
            getDeleteKeyRowAction().setEnabled(true);
        }
    }

    class DeleteStringRowAction extends AbstractAction {
        DeleteStringRowAction() {
            super("delete row");
        }
        public void actionPerformed(ActionEvent arg0) {
            StringArrayTableModel stringKeys = getStringKeysTableModel();
            int rows = stringKeys.getRowCount();
            if (rows > 2)
                stringKeys.setRowCount(stringKeys.getRowCount() - 1);
        }
    }

    /**
	 * @return
	 */
    private Component getIntKeyPanel() {
        if (intKeyPanel == null) {
            intKeyPanel = new JPanel();
            intKeyPanel.setLayout(new BorderLayout());
            intKeyPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "integer key fields with values"));
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(new JButton(getAddKeyAction()));
            buttonPanel.add(new JButton(new DeleteIntKeyAction()));
            buttonPanel.add(new JButton(getAddKeyRowAction()));
            buttonPanel.add(new JButton(getDeleteKeyRowAction()));
            JScrollPane scrollPane = new JScrollPane(getIntKeyTable());
            intKeyPanel.add(buttonPanel, BorderLayout.PAGE_START);
            intKeyPanel.add(scrollPane, BorderLayout.CENTER);
        }
        return intKeyPanel;
    }

    /**
	 * @return
	 */
    private Component getStringKeyPanel() {
        if (stringKeyPanel == null) {
            stringKeyPanel = new JPanel();
            stringKeyPanel.setLayout(new BorderLayout());
            stringKeyPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "string keys fields with values"));
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(new JButton(getAddKeyAction()));
            buttonPanel.add(new JButton(new DeleteStringKeyAction()));
            buttonPanel.add(new JButton(getAddKeyRowAction()));
            buttonPanel.add(new JButton(getDeleteKeyRowAction()));
            JScrollPane scrollPane = new JScrollPane(this.getStringKeyTable());
            stringKeyPanel.add(buttonPanel, BorderLayout.PAGE_START);
            stringKeyPanel.add(scrollPane, BorderLayout.CENTER);
        }
        return stringKeyPanel;
    }

    public static void main(java.lang.String[] args) {
        try {
            JDialog dialog = new JDialog();
            dialog.setLayout(new GridLayout(0, 2));
            dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            TableDataSetIndexedValuePanel aPanel = new TableDataSetIndexedValuePanel();
            dialog.add(aPanel);
            dialog.setSize(aPanel.getSize());
            dialog.setVisible(true);
        } catch (Throwable exception) {
            System.err.println("Exception occurred in main() of javax.swing.JPanel");
            exception.printStackTrace(System.out);
        }
    }

    public void propertyChange(java.beans.PropertyChangeEvent evt) {
        if ((evt.getSource() == getMyParam()) && (evt.getPropertyName().equals("tableName"))) {
            updateModelTableName();
        }
        if ((evt.getSource() == getMyParam()) && (evt.getPropertyName().equals("valueFieldName"))) {
            updateModelFieldName();
        }
    }

    public synchronized void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        getPropertyChange().removePropertyChangeListener(listener);
    }

    void setMyParam(TableDataSetIndexedValue myParam) {
        this.myParam = myParam;
        stringKeysTableModel = null;
        intKeysTableModel = null;
        getStringKeyTable().setModel(getStringKeysTableModel());
        getIntKeyTable().setModel(getIntKeysTableModel());
        getValueFieldNameField().setText(myParam.getMyFieldName());
        getTableNameField().setText(myParam.getMyTableName());
        switch (myParam.getValueMode()) {
            case TableDataSetIndexedValue.SINGLE_VALUE_MODE :
                singleRowButton.setSelected(true);
                break;
            case TableDataSetIndexedValue.SUM_MODE :
                sumButton.setSelected(true);
                break;
            case TableDataSetIndexedValue.AVERAGE_MODE :
                averageButton.setSelected(true);
                break;
        }
    }

    TableDataSetIndexedValue getMyParam() {
        if (myParam == null) {
            myParam = new TableDataSetIndexedValue();
            setMyParam(myParam);
        }
        return myParam;
    }

    protected PropertyChangeSupport getPropertyChange() {
        if (propertyChange == null) {
            propertyChange = new PropertyChangeSupport(this);
        }
        return propertyChange;
    }

    private JTable getStringKeyTable() {
        if (stringKeyTable == null) {
            stringKeyTable = new JTable(getStringKeysTableModel());
            stringKeyTable.setDragEnabled(true);
            stringKeyTable.setTransferHandler(new TableTransferHandler());
        }
        return stringKeyTable;
    }

    private JTable getIntKeyTable() {
        if (intKeyTable == null) {
            intKeyTable = new JTable(getIntKeysTableModel());
            intKeyTable.setDragEnabled(true);
            intKeyTable.setTransferHandler(new TableTransferHandler());
        }
        return intKeyTable;
    }

}
/*
 * Created on Nov 18, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.hbaspecto.calibrator.TargetPanel;
import com.hbaspecto.util.StringArrayTableModel;
import com.hbaspecto.util.TableTransferHandler;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class StringIndexedFloatTargetPanel extends JPanel {

    private StringIndexedFloatTarget myTarget;
    private JTextField matrixNameTextField;
    private JTextField fileNameTextField;
    JFormattedTextField weightTextField;
    
    JPanel essentialPanel;
    JPanel getEssentialPanel() {
        if (essentialPanel == null) {
            essentialPanel = new JPanel();
            essentialPanel.setLayout(new GridLayout(2,2));
            essentialPanel.add(new JLabel("Matrix name"));
            essentialPanel.add(getMatrixNameTextField());
            essentialPanel.add(new JLabel("File name"));
            essentialPanel.add(getFileNameTextField());
            
        }
        return essentialPanel;
    }
   
    boolean initialized;
    private void initialize() {
        if (!initialized) {
            setLayout(new BorderLayout());
            add(getEssentialPanel(), BorderLayout.NORTH);
            add(getStringKeyPanel(), BorderLayout.CENTER);
            TargetPanel theRegularStuff = new TargetPanel();
            theRegularStuff.setTheTarget(getMyTarget());
            add(theRegularStuff, BorderLayout.SOUTH);
            setSize(new Dimension(800,600));
            this.validate();
            getMatrixNameTextField().setText(getMyTarget().getMyFieldAndMatrixName());
            getFileNameTextField().setText(getMyTarget().getFileAndTableName());
        } 
        initialized=true;
    }
    
    class AddKeyAction extends AbstractAction {
        AddKeyAction() {
            super("add key");
        }

        public void actionPerformed(ActionEvent arg0) {
            showAddKeyDialog();
        }
    }
    
    public void showAddKeyDialog() {
        getAddKeyDialog().setVisible(true);
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
            JButton stringKeyButton = new JButton("String key");
            buttons.add(stringKeyButton);
            cp.add(buttons);
            stringKeyButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {
                    String keyField = keyName.getText();
                    StringArrayTableModel tm = getMyTarget().getStringKeysTableModel();
                    tm.setColumnCount(tm.getColumnCount() + 1);
                    tm.setValueAt(keyField, 0, tm.getColumnCount() - 1);
                    addKeyDialog.setVisible(false);
                }
            });
            addKeyDialog.setModal(true);
            addKeyDialog.pack();
            
            
        }
        return addKeyDialog;

    }
    
    
    AddKeyAction addKeyAction = null;
    AddKeyAction getAddKeyAction() {
        if (addKeyAction == null) {
            addKeyAction = new AddKeyAction();
        }
        return addKeyAction;
    }

    
    
    JPanel stringKeyPanel;
    class DeleteKeyAction extends AbstractAction {
        DeleteKeyAction() {
            super("remove key");
        }

        public void actionPerformed(ActionEvent arg0) {
            StringArrayTableModel stringKeys = getMyTarget().getStringKeysTableModel();
            stringKeys.setColumnCount(Math.max(0, stringKeys.getColumnCount() - 1));
        }
    }

    public static void main(java.lang.String[] args) {
        try {
            JDialog dialog = new JDialog();
            dialog.getContentPane().setLayout(new GridLayout(0, 2));
            dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            StringIndexedFloatTargetPanel aPanel = new StringIndexedFloatTargetPanel();
            dialog.getContentPane().add(aPanel);
            dialog.setSize(aPanel.getSize());
            dialog.setVisible(true);
        } catch (Throwable exception) {
            System.err.println("Exception occurred in main() of javax.swing.JPanel");
            exception.printStackTrace(System.out);
        }
    }

    /**
     * @return
     */
    private JPanel getStringKeyPanel() {
        if (stringKeyPanel == null) {
            stringKeyPanel = new JPanel();
            stringKeyPanel.setLayout(new BorderLayout());
            stringKeyPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "string keys fields with values"));
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(new JButton(getAddKeyAction()));
            buttonPanel.add(new JButton(new DeleteKeyAction()));
            buttonPanel.add(new JButton(getAddKeyRowAction()));
            buttonPanel.add(new JButton(getDeleteKeyRowAction()));
            JScrollPane scrollPane = new JScrollPane(this.getStringKeyTable());
            stringKeyPanel.add(buttonPanel, BorderLayout.PAGE_START);
            stringKeyPanel.add(scrollPane, BorderLayout.CENTER);
        }
        return stringKeyPanel;
    }
    
    class DeleteStringKeyAction extends AbstractAction {
        DeleteStringKeyAction() {
            super("remove key");
        }

        public void actionPerformed(ActionEvent arg0) {
            StringArrayTableModel stringKeys = getMyTarget().getStringKeysTableModel();
            stringKeys.setColumnCount(Math.max(0, stringKeys.getColumnCount() - 1));
        }
    }

    AddKeyRowAction addKeyRowAction = null;
    AddKeyRowAction getAddKeyRowAction() {
        if (addKeyRowAction == null) {
            addKeyRowAction = new AddKeyRowAction();
        }
        return addKeyRowAction;
    }

    class AddKeyRowAction extends AbstractAction {
        AddKeyRowAction() {
            super("add row");
        }
        public void actionPerformed(ActionEvent arg0) {
            StringArrayTableModel stringKeys = getMyTarget().getStringKeysTableModel();
            int rows = stringKeys.getRowCount() + 1;
            stringKeys.setRowCount(rows);
            deleteKeyRowAction.setEnabled(true);
        }
    }


    class DeleteKeyRowAction extends AbstractAction {
        DeleteKeyRowAction() {
            super("delete row");
        }
        public void actionPerformed(ActionEvent arg0) {
            StringArrayTableModel stringKeys = getMyTarget().getStringKeysTableModel();
            int rows = stringKeys.getRowCount();
            if (rows > 1)
                stringKeys.setRowCount(rows - 1);
            if (rows <= 2)
                getDeleteKeyRowAction().setEnabled(false);
        }
    }
    
    DeleteKeyRowAction deleteKeyRowAction = null;
    DeleteKeyRowAction getDeleteKeyRowAction() {
        if (deleteKeyRowAction == null) {
            deleteKeyRowAction = new DeleteKeyRowAction();
        }
        if (getMyTarget().getStringKeysTableModel().getRowCount() < 1) {
            deleteKeyRowAction.setEnabled(false);
        } else {
            deleteKeyRowAction.setEnabled(true);
        }
        return deleteKeyRowAction;
    }

    JTable stringKeyTable;

    private JTable getStringKeyTable() {
        if (stringKeyTable == null) {
            stringKeyTable = new JTable(myTarget.getStringKeysTableModel());
            stringKeyTable.setDragEnabled(true);
            stringKeyTable.setTransferHandler(new TableTransferHandler());
        }
        return stringKeyTable;
    }

    /**
     * @return
     */
    JTextField getMatrixNameTextField() {
        if (matrixNameTextField == null) {
            matrixNameTextField = new JTextField();
            matrixNameTextField.getDocument().addDocumentListener(new FieldDocumentListener(
            new FieldDocumentListener.FieldSetter() {
               public void setField(String r) {
                   getMyTarget().setMyFieldAndMatrixName(r);
               }
            }));
        }
        return matrixNameTextField;
    }

    
    /**
     * @return
     */
    JTextField getFileNameTextField() {
        if (fileNameTextField == null) {
            fileNameTextField = new JTextField();
            fileNameTextField.getDocument().addDocumentListener(new FieldDocumentListener(
                    new FieldDocumentListener.FieldSetter() {
                        public void setField(String r) {
                            getMyTarget().setFileAndTableName(r);
                        }
                    }));
        }
        return fileNameTextField;
    }
    /**
     * @return
     */
    JFormattedTextField getWeightTextField() {
        if (weightTextField == null) {
            weightTextField = new JFormattedTextField(NumberFormat.getNumberInstance());
            weightTextField.getDocument().addDocumentListener(new FieldDocumentListener(
                    new FieldDocumentListener.FieldSetter() {
                        public void setField(String r) {
                            getMyTarget().setWeight(Double.valueOf(r).doubleValue());
                        }
                    }));
        }
        return weightTextField;
    }
    /**
     * 
     */
    public StringIndexedFloatTargetPanel() {
        super();
        initialize();
    }

    /**
     * @param arg0
     */
    public StringIndexedFloatTargetPanel(boolean arg0) {
        super(arg0);
        initialize();
    }

    /**
     * @param arg0
     */
    public StringIndexedFloatTargetPanel(LayoutManager arg0) {
        super(arg0);
        initialize();
    }

    /**
     * @param arg0
     * @param arg1
     */
    public StringIndexedFloatTargetPanel(LayoutManager arg0, boolean arg1) {
        super(arg0, arg1);
        initialize();
    }

    /**
     * @param target
     */
    public StringIndexedFloatTargetPanel(StringIndexedFloatTarget target) {
        setMyTarget(target);
        initialize();
    }

    private void setMyTarget(StringIndexedFloatTarget myTarget) {
        this.myTarget = myTarget;
        initialize();
    }

    private StringIndexedFloatTarget getMyTarget() {
        if (myTarget== null) {
            myTarget = new StringIndexedFloatTarget();
        }
        return myTarget;
    }

}

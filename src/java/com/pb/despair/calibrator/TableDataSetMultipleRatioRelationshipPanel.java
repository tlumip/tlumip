/*
 * Created on Jun 17, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class TableDataSetMultipleRatioRelationshipPanel extends JPanel implements DocumentListener {
    
    /**
     * 
     */
    public TableDataSetMultipleRatioRelationshipPanel() {
        super();
        initialize();
    }
    
    TableDataSetIndexedValuePanel commonPanel;
    JPanel specificsPanel;
    
    public abstract JFormattedTextField getWeightField();
    public abstract JTextField getNameField();
    
    /**
     * @return Returns the myModel.
     */
    public abstract TableDataSetMultipleRatioRelationship getMyModel();
    
    
    JFormattedTextField weight ;
    
    
    boolean initialized;
    
    void initialize() {
        if (!initialized){
            this.setLayout(new GridLayout(0,2));
            JPanel leftSidePanel = new JPanel();
            leftSidePanel.setLayout(new BorderLayout());
            commonPanel = new TableDataSetIndexedValuePanel();
            commonPanel.setMyParam(getMyModel().getCommonElements());
            leftSidePanel.add(commonPanel, BorderLayout.CENTER);
            
            JPanel doStringIndices = new JPanel();
            doStringIndices.add(new JLabel("use StringIndexArray binary file"));
            doStringIndices.add(getUseStringIndexArrayCheckBox());
            leftSidePanel.add(doStringIndices, BorderLayout.SOUTH);
            this.setPreferredSize(new Dimension(600,500));
            this.add(leftSidePanel);
            initialized = true;
            getWeightField().getDocument().addDocumentListener(this);
            getNameField().getDocument().addDocumentListener(this);
        }
       }
    
    JCheckBox useStringIndexArrayCheckBox;
    /**
     * @return
     */
    private JCheckBox getUseStringIndexArrayCheckBox() {
        if (useStringIndexArrayCheckBox == null) {
            useStringIndexArrayCheckBox = new JCheckBox();
            useStringIndexArrayCheckBox.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                   getMyModel().setGenerateStringIndexedFloatTargets(useStringIndexArrayCheckBox.isSelected());
               }
            });
            useStringIndexArrayCheckBox.setSelected(getMyModel().isGenerateStringIndexedFloatTargets());
        }
        return useStringIndexArrayCheckBox;
    }
    public void setMyModel(TableDataSetMultipleRatioRelationship m) {
        commonPanel.setMyParam(m.getCommonElements());
        getNameField().setText(m.getName());
        getUseStringIndexArrayCheckBox().setSelected(getMyModel().isGenerateStringIndexedFloatTargets());
    }
}

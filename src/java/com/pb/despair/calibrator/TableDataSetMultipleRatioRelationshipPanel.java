/*
 * Created on Jun 17, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
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
            commonPanel = new TableDataSetIndexedValuePanel();
            commonPanel.setMyParam(getMyModel().getCommonElements());
            this.setPreferredSize(new Dimension(600,500));
            this.add(commonPanel);
            initialized = true;
            getWeightField().getDocument().addDocumentListener(this);
            getNameField().getDocument().addDocumentListener(this);
        }
       }
    
    public void setMyModel(TableDataSetMultipleRatioRelationship m) {
        commonPanel.setMyParam(m.getCommonElements());
        getNameField().setText(m.getName());
    }
}

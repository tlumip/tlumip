/*
 * Created on Jun 17, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import java.io.Serializable;

import com.hbaspecto.calibrator.DialogableTargetGroup;
import com.pb.common.datafile.TableDataSetIndexedValue;

/**
 * @author jabraham
 *
 */
public abstract class TableDataSetMultipleRatioRelationship implements DialogableTargetGroup, Serializable, Cloneable {

    TableDataSetIndexedValue commonElements = new TableDataSetIndexedValue();
    double weight;
    static final long serialVersionUID = 4517743004455337485L;
    private boolean generateStringIndexedFloatTargets = false;
    
    public String toString() {
        if (name!=null) {
            if (name.length()!=0) {
                return getName();
            }
        }
        return super.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        TableDataSetMultipleRatioRelationship newOne= (TableDataSetMultipleRatioRelationship) super.clone();
        newOne.commonElements = (TableDataSetIndexedValue) this.commonElements.clone();
        return newOne;
    }

    public TableDataSetMultipleRatioRelationship() {
    }

    /**
     * @return Returns the commonElements.
     */
    public TableDataSetIndexedValue getCommonElements() {
        return commonElements;
    }

    /**
     * @param commonElements The commonElements to set.
     */
    public void setCommonElements(TableDataSetIndexedValue commonElements) {
        this.commonElements = commonElements;
    }

    void setWeight(double weight) {
        this.weight = weight;
    }

    double getWeight() {
        return weight;
    }
    
    void setName(String name) {
        this.name = name;
    }

    String getName() {
        if (name == null) {
            name = new String();
        }
        return name;
    }

    void setGenerateStringIndexedFloatTargets(boolean generateStringIndexedFloatTargets) {
        this.generateStringIndexedFloatTargets = generateStringIndexedFloatTargets;
    }

    boolean isGenerateStringIndexedFloatTargets() {
        return generateStringIndexedFloatTargets;
    }

    private String name;
    
    

}

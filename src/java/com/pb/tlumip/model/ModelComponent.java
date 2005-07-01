package com.pb.tlumip.model;

import java.util.ResourceBundle;


/**
 * This is the base class for model components.
 *
 * @author    Tim Heier
 * @version   1.0, 4/15/2000
 *
 */

public abstract class ModelComponent
{

    //Name of model component
    String name;
    public ResourceBundle appRb; //this resource bundle holds application specific properties
    public ResourceBundle globalRb; //this resource bundle holds global definitions that are common
                                    //to several applications.

    public String getName() {
        return name;
    }
    
    
    public void setName(String name) {
        this.name = name;
    }

    abstract public void startModel(int timeInterval);

    public void setApplicationResourceBundle(ResourceBundle appRb){
        this.appRb = appRb;
    }

    public void setResourceBundles(ResourceBundle appRb, ResourceBundle globalRb){
        setApplicationResourceBundle(appRb);
        this.globalRb = globalRb;
    }


}

package com.pb.despair.model;

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
    public ResourceBundle resourceBundle;

    public String getName() {
        return name;
    }
    
    
    public void setName(String name) {
        this.name = name;
    }

    abstract public void startModel(int timeInterval);

    public void setProperties(ResourceBundle rb){
        this.resourceBundle = rb;
    }


}

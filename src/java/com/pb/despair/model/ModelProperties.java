package com.pb.despair.model;

import java.util.HashMap;

/**
 * Will store the model properties.  HashMap will be
 * populated by digesting a corresponding modelProperties.xml
 * file.
 * 
 * @author  Christi Willison
 * @version Jan 7, 2004
 * Created by IntelliJ IDEA.
 */
public abstract class ModelProperties {
    private HashMap propertyMap;

    public ModelProperties(){
        propertyMap = new HashMap();
    }

    public void setProperty(String name,String value){
        propertyMap.put(name,value);
    }

    public String getProperty(String name){
        return (String) propertyMap.get(name);
    }

    public HashMap getPropertyMap() {
        return propertyMap;
    }

}

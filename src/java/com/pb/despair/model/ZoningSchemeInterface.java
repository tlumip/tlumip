package com.pb.despair.model;

import java.util.Iterator;
//import com.pb.despair.ld.DevelopmentType;

/**
 * A class that represents a the allowed zoning in a Grid Cell
 * @author John Abraham
 */
public interface ZoningSchemeInterface {

    public abstract String getName();

    public void allowDevelopmentType(DevelopmentTypeInterface dt, double maxFloorAreaRatio, float fee);
    public void noLongerAllowDevelopmentType(DevelopmentTypeInterface dt);
    public double getAllowedFAR(DevelopmentTypeInterface dt);

    public Iterator allowedDevelopmentTypes();
    public int size();
}

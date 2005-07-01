package com.pb.tlumip.model;

//import com.pb.common.old.model.Alternative;
import com.pb.common.model.Alternative;
//import com.pb.common.model.ConcreteAlternative;
import java.io.Serializable;
import java.util.Collection;
import java.util.ArrayList;
import org.apache.log4j.Logger;


/** A class that represents mode of travel
 * 
 * @author Joel Freedman
 */
public abstract class Mode implements Serializable, Alternative{
    private static Logger logger = Logger.getLogger("com.pb.tlumip.pt");
    public Collection observers;
    public byte type;     
               
    //attributes of a mode include:
    public double time;          //unweighted, in minutes, of course
    public double cost;          //in cents, of course
    public boolean hasUtility;          //has the utility been calculated?
         
     
     public double utility;          //from mode choice
     public String alternativeName; //the name of the mode
     public boolean isAvailable;          //is it available?
     Object alternative;
     ArrayList alternativeObservers;
     public double constant;
     double expConstant;

    
   
    /**
    Get the utility of the alternative.
    @return Utility value.
    */
    public double getUtility(){
        return utility;
    }
    
    public void setConstant(double constant){
        this.constant = constant;
    }
    
    public double getConstant(){
        return constant;
    }
    public void setExpConstant(double expConstant){
        this.expConstant =expConstant;
    }
    public double getExpConstant(){
         return expConstant;
    }
    /**
    Set the utility of the alternative.
    @param util  Utility value.
    */
    public void setUtility(double util){
        utility=util;
        hasUtility=true;
    }    /**

    /** 
    Get the name of this alternative.
    @return The name of the alternative
    */
    public String getName(){
        return alternativeName;
    }
    /** 
    Set the name of this alternative.
    @param name The name of the alternative
    */
    public void setName(String alternativeName){
        this.alternativeName=alternativeName;
    }
    /** 
    Get the availability of this alternative.
    @return True if alternative is available
    */
    public boolean isAvailable(){
        return isAvailable;
    }
    /** 
    Set the availability of this alternative.
    @param available True if alternative is available
    */
    public void setAvailability(boolean available){
        isAvailable=available;
    }
    
    /**
    Set reference to actual alternative.
    @param n Name of alternative
    @param alt Actual alternative.
    */
    public void setAlternative(String n, Object alt){
        alternativeName=n;
        alternative=alt;
    }
    /**
    Get the actual alternative.
    @return Actual alternative.
    */
    public Object getAlternative(){
        return alternative;
    }

     
     public void print(){
         logger.info("");
          logger.info("Tour Mode: "+alternativeName+", travel time: "+time);
     }

    public String toString(){
        return alternativeName;
    }

}



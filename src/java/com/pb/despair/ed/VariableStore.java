
/**
 * Company:      ECONorthwest<p>
 */
package com.pb.tlumip.ed;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class VariableStore {
  Hashtable allVariables;
  Vector independantVariables;
  Vector dependantVariables;
  DataHound dh;

  public VariableStore() {
    allVariables = new Hashtable();
    independantVariables = new Vector();
    dependantVariables = new Vector();
    dh = new DataHound(EDControl.getEDDataAccess());
  }

  /**
   * Adds the variable to the list of variables if not already added.
   */
  void add(Variable v) {
    boolean addVar = true;
    if (allVariables.contains(v.getIdentifier())) {
      addVar = false;
    }
    if (addVar) {

      allVariables.put(v.getIdentifier(), v);
      if(v.isDependant()){
        dependantVariables.add(v);
      }else{
        independantVariables.add(v);
      }
    }
  }


  /**
   * Returns the variable in the store when given a name and year.
   */
  Variable getVariable(String name, int year) {
    return (Variable) allVariables.get((String)(name+year));
  }

  /**
   * Returns a vector of all independant variables in the store.
   */
  Vector getIndependantVariables() {
    return independantVariables;
  }

  Vector getDependantVariables() {
    return dependantVariables;
  }

  /**
   * Reads in the values of the independant variables from external source.
   */
  void getData() {
    if(allVariables.size() >= 0) {
      Enumeration e = independantVariables.elements();
      while (e.hasMoreElements()) {
        dh.getData((Variable) e.nextElement());
      }
    }
  }

  void printStore() {
      Enumeration e = allVariables.elements();
      while (e.hasMoreElements()) {
        Variable v = (Variable) e.nextElement();
        System.out.println("Variable Name: " + v.getName());
      }
      System.out.println("Variable store size: " + allVariables.size());
  }

  void setData() {
    Enumeration e = dependantVariables.elements();
    while (e.hasMoreElements()) {
      dh.setData((Variable) e.nextElement());
    }
  }



}

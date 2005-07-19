/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

/**
 * Company:      ECONorthwest<p>
 */
package com.pb.tlumip.ed;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;

public class VariableStore {
  protected static Logger logger = Logger.getLogger(VariableStore.class);
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
        if(logger.isDebugEnabled()) logger.debug("Variable Name: " + v.getName());
      }
      if(logger.isDebugEnabled()) logger.debug("Variable store size: " + allVariables.size());
  }

  void setData() {
    Enumeration e = dependantVariables.elements();
    while (e.hasMoreElements()) {
      dh.setData((Variable) e.nextElement());
    }
    dh.writeData();
  }



}

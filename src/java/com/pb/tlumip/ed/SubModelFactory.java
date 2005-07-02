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
 * Title:        null<p>
 * Description:  null<p>
 * Copyright:    null<p>
 * Company:      ECONorthwest<p>
 * @author
 * @version null
 */

package com.pb.tlumip.ed;
import com.pb.common.util.Debug;

import java.util.Vector;

public class SubModelFactory {

  private static String name;
  private static int order;
  private static String description;
  private static String submodel;
  private static VariableStore store;
  private static XMLModelSpecification m;

  static SubModel build(XMLModelSpecification msi, VariableStore vs) throws ModelReadingException, InvalidEquationException {
    store = vs;
    m = msi;
    name = m.getSubModelName();
    if(m.getSubModelOrder() != null) {
      order = Integer.parseInt(m.getSubModelOrder());
    }
    submodel= m.getSubModelType();
    Debug.println("  Building " + submodel + " submodel: " + name);
    if (submodel != null)
  	{
    	if(submodel.equals(TextParser.LINEAR)) {
			return buildLinearSubModel();
    	} else if (submodel.equals(TextParser.SIMPLE)){
			return buildSimpleFunctionsSubModel();
    	} else if(submodel.equals(TextParser.LOGIT)) {
    	//fill in later
    	}
  	}
    throw new ModelReadingException("Submodel", submodel);

  }//build

  private static SubModel buildLinearSubModel() throws ModelReadingException, InvalidEquationException {
      Vector vec = new Vector();
      LinearEquation le;
      addDependantVariablesToStore();
      while(m.nextEquationXML()) {
        le = (LinearEquation)EquationFactory.build(m, store);
        vec.add(le);
      }
      return new Linear(order, name, store ,vec);
  }

  private static SubModel buildSimpleFunctionsSubModel() throws ModelReadingException, InvalidEquationException {
    Vector vec = new Vector();
    Equation e;
    addDependantVariablesToStore();
    while(m.nextEquationXML()) {
      e = (Equation)EquationFactory.build(m,store);
      vec.add(e);
    }
    return new SimpleFunctions(order, name, store, vec);
  }

  private static void addDependantVariablesToStore() throws ModelReadingException {
    Variable v;
    String location;
    String name;
    int year;
    while(m.nextEquationXML()) {
      m.nextEquationElementXML();
      if(m.isVariableXML()) {
        name = m.getVariableName();
        year = EDControl.getCurrentYear();
        if(m.nextLagXML()) {
          year = year - Integer.parseInt(m.getLagXML());
        }
        //Get the variable if it's already been created.

        if(m.nextLocationXML()) {
          location = m.getLocationXML();
        } else {
          location = null;
        }

        if((location == null) ||(location.equals("default")) ) {
          location = EDControl.getDefaultDataLocation();
        }

        v = new Variable( name,location,year);
        v.setDependant(true);
        store.add(v);
      } else {
        throw new ModelReadingException("First element of an equation must be a variable.", "????");
      }
    }
    m.goToFirstEquation();
  }

}

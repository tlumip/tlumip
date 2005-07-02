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
 * LinearEquation is an equation that holds variables.
 */
package com.pb.tlumip.ed;
import java.util.Vector;

public class LinearEquation extends Equation {

  Vector terms;


  /**
   * Takes in the name and a vector of equations.
   */
  public LinearEquation(String name, Vector ee) throws InvalidEquationException {
    super(name, ee);
    terms = new Vector();
    makeLinear();
  }

  /**
   * Adds a vector of variables to this equation.
   */
  private void makeLinear() throws InvalidEquationException {
    Object temp;
    temp = equationElements.get(0);
    Parameter p = null;
    Variable v;
    //move the left hand variable to the right side of the equation.
    if(temp instanceof Variable) {
      terms.add(new LinearTerm(new Parameter(-1), (Variable) temp));
    }
    //insert the rest of the terms in the equation.
    for(int i=1; i < equationElements.size(); i++) {
//	for(int i=equationElements.size()-1; i >= 0 ; i--) {
      temp = equationElements.get(i);
      if(temp instanceof Parameter) {
        p = (Parameter) temp;
      } else if (temp instanceof Variable) {
        v = (Variable) temp;
		if (p == null) {
          terms.add(new LinearTerm(new Parameter(1),v));
        } else {
          terms.add(new LinearTerm(p,v));
        }
        p = null;
      }
    }
  }  //make linear


  Vector getLinearTerms() {
    return terms;
  }

  void simplify() {
    LinearTerm lt;
    for(int i = 0; i < terms.size(); i++) {
      lt = (LinearTerm)terms.get(i);
      if(lt.isDependant()){
        lt.switchSides();
      }
    }
  }


}

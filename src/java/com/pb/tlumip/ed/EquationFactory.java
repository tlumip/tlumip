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
 *
 * ED test is used in place of EDModel.java for now.
 */
package com.pb.tlumip.ed;

import java.util.Vector;

public class EquationFactory {

  private static String name;
  private static String equationtype;
  private static String description;
  private static VariableStore store;
  private static XMLModelSpecification m;

  /**
   * Builds equation object based on xml file.
   */
  static Equation build(XMLModelSpecification msi, VariableStore vs) throws ModelReadingException, InvalidEquationException {
    store = vs;
    m = msi;
    name = m.getEquationName();
    equationtype = m.getEquationType();
    System.err.println(equationtype);
    if(equationtype.equals(TextParser.LINEAR)) {
      return new LinearEquation(name, buildEquation());
    }
    else if(equationtype.equals(TextParser.LOGIT)) {

    } else if(equationtype.equals(TextParser.SIMPLE)) {
      return new Equation(name, buildEquation());
    }

    throw new ModelReadingException("Equation", equationtype);


  }//build

  private static Vector buildEquation() throws ModelReadingException, InvalidEquationException {
      Vector eq = new Vector();
      Object x;

      while(m.nextEquationElementXML()) {
        x = EquationElementFactory.build(m, store);
        eq.add(x);
      }
      return eq;
  }

}


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

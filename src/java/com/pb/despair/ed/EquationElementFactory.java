
/**
 * Company:      ECONorthwest<p>
 * Factory class that produces variables based on the values retrieved from
 * the model specification interface.
 */
package com.pb.despair.ed;
import com.pb.common.util.Debug;

public class EquationElementFactory {

  private static XMLModelSpecification m;
  private static VariableStore vs;
  /**
   * Called to build and return a variable.
   */
  protected static Object build(XMLModelSpecification msi, VariableStore vstore) throws ModelReadingException {
    m = msi;
    vs = vstore;

    Object o = null;
    //change this later
      if(m.isVariableXML()) {
        o = buildVariable();
      } else if(m.isParameterXML()) {
      	try{
        o = new Parameter(Double.parseDouble(m.getParameterXML()));
      	}catch (Exception e)
      	{
      		Debug.println("Error on value:" + m.getParameterXML());
      	}
      } else if(m.isOperatorXML()) {
      //Normally would catch and rethrow error. However, in this case,
      //the model reading exception makes more sense.
        try {
          o = new Operator(m.getOperatorXML());
        } catch (InvalidEquationException e) {
          throw new ModelReadingException("Operator", m.getOperatorXML());
        }
      } else {
        throw new ModelReadingException("EquationElement", "???");
      }

    return o;
  }//build

  /**
   * Builds a Variable.
   */
  private static Variable buildVariable() {
    Variable v;
    String location;
    String name = m.getVariableName();
    int year = EDControl.getCurrentYear();
    if(m.nextLagXML()) {
      year = year - Integer.parseInt(m.getLagXML());
    }
    //Get the variable if it's already been created.
    v = vs.getVariable(name, year);

    if (v == null) {
      if(!m.nextLocationXML()) {
        location = m.getLocationXML();
      } else {
        location = null;
      }

      if((location == null) ||(location.equals("default")) || (location.equals("")) ) {
        location = EDControl.getDefaultDataLocation();
      }

      v = new Variable( name,location,year);
      vs.add(v);
    }

    return v;
  }


}

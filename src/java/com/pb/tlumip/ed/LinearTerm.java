
/**
 * Title:        null<p>
 * Description:  null<p>
 * Copyright:    null<p>
 * Company:      ECONorthwest<p>
 * @author
 * @version null
 */
package com.pb.tlumip.ed;

public class LinearTerm {
  Variable v;
  Parameter p;
  boolean lhs;

  public LinearTerm(Parameter p, Variable v) {
    this.v = v;
    this.p = p;
    lhs = false;
  }

  Parameter getParameter() {
    return p;
  }

  double getParameterValue() {
    try {
      return p.getValue();
    } catch (UnknownValueException e) {
      //Parameters always have values due to constructor.
    }
    return 0;
  }

  Variable getVariable() {
    return v;
  }


  double getTermTotal() throws UnknownValueException {
    return p.getValue()*v.getValue();
  }

  boolean isDependant() {
    return v.isDependant();
  }

  void switchSides() {
    try {
      p.setValue(p.getValue()*-1);
      if(lhs) {
        lhs = false;
      } else {
        lhs = true;
      }
    } catch (UnknownValueException e) {}
  }

  boolean isLhs() {
    return lhs;
  }

}

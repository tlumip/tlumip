
/**
 * Title:        null<p>
 * Description:  null<p>
 * Copyright:    null<p>
 * Company:      ECONorthwest<p>
 * @author
 * @version null
 */
package com.pb.tlumip.ed;

import java.util.Vector;

class Equation {
  Vector equationElements;
  String name;

  Equation(String n, Vector ee) {
    name = n;
    equationElements = new Vector();
    equationElements = ee;
  }

  String getName() {
    return name;
  }

  Vector getEquationElements() {
    return equationElements;
  }

}

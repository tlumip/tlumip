
/**
 * Title:        null<p>
 * Description:  null<p>
 * Copyright:    null<p>
 * Company:      ECONorthwest<p>
 * @author
 * @version null
 */
package com.pb.despair.ed;

public class Parameter implements DoubleInterface{

  private double value;

  public Parameter(double v) {
    value = v;
  }

  public void setValue(double v) {
    value = v;
  }

  public double getValue() throws UnknownValueException {
    return value;
  }



}

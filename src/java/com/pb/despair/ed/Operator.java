
/**
 * Title:        null<p>
 * Description:  null<p>
 * Copyright:    null<p>
 * Company:      ECONorthwest<p>
 * @author
 * @version null
 */
package com.pb.despair.ed;

public class Operator {

  private String o;
  public final static String PLUS = "+";
  public final static String MULTIPLY = "*";
  public final static String MINUS = "-";
  public final static String NATURALLOG = "ln";
  public final static String EXP = "exp";
  public final static String DIVIDE = "/";
  public final static String POWER = "^";

  public Operator(String o) throws InvalidEquationException {
    if(o.equals(PLUS) || o.equals(MINUS) || o.equals(MULTIPLY) || o.equals(NATURALLOG) || o.equals(DIVIDE) || o.equals(POWER) || o.equals(EXP)) {
      this.o = o;
    } else {
      throw new InvalidEquationException("Invalid operator: " + o);
    }
  }

  protected String getOperator() {
    return o;
  }

}

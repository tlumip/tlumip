
/**
 * Title:        null<p>
 * Description:  null<p>
 * Copyright:    null<p>
 * Company:      ECONorthwest<p>
 * @author
 * @version null
 */
package com.pb.despair.ed;
import com.pb.common.util.Debug;

import java.util.Vector;

public class SimpleFunctions extends SubModel {

  Vector equations;

  /**
   * Constructor takes in all basic SubModel data.
   */
  public SimpleFunctions(int o, String n, VariableStore v, Vector e) {
    super(o,n,v);
    equations = e;
  }


  protected void solve() throws Exception {
    Equation e;
    for(int i = 0; i < equations.size(); i++) {
      e = (Equation)equations.get(i);
      Debug.println("  Solving for " + e.getName());
      SimpleSolver.solve(e);
    }
  }

  protected String getType() {
    return "SimpleFunctions";
  }



}


/**
 * Company:      ECONorthwest<p>
 * The linear model solves a system of linear equations using matrix algebra.
 * LinearSolver and Linearizer are used to help solve this SubModel.
 */

package com.pb.despair.ed;

import com.pb.common.util.Debug;

import java.util.Vector;

public class Linear extends SubModel {

  private Vector equations;
  private LinearSolver ls;

  /**
   * Takes in SubModel parameters and a vector of equations.
   */
  public Linear(int o, String n, VariableStore v, Vector e) {
    super(o,n, v);
    equations = e;
  }

  /**
   * Adds a linear equation to this SubModel.
   */
  void addEquation(LinearEquation e) {
    equations.add(e);
  }

  /**
   * Called to solve the system of linear equations.  Uses both the linearizer
   * object and the linear solver to do this.
   */
  protected void solve() throws Exception {
    Debug.println("  Linearizing...");
    Linearizer lin = new Linearizer(equations, super.getVariableStore());
    double [][] m = lin.getMArray();
    double [] y = lin.getBArray();
    Debug.println("  Finding solution...");
    ls = new LinearSolver(m, y);
    double[] solution = ls.solve();
    Debug.println("  Filling solution...");
    lin.fillSolution(solution);
  }

  protected String getType() {
    return "Linear";
  }

}

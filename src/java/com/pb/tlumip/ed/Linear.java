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
 * The linear model solves a system of linear equations using matrix algebra.
 * LinearSolver and Linearizer are used to help solve this SubModel.
 */

package com.pb.tlumip.ed;

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

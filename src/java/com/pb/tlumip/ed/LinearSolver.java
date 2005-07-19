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
 * Title:        null<p>
 * Description:  null<p>
 * Copyright:    null<p>
 * Company:      ECONorthwest<p>
 * @author
 * @version null
 */
package com.pb.tlumip.ed;
import org.apache.log4j.Logger;

import com.pb.common.util.Debug;
import drasys.or.linear.SingularException;
import drasys.or.linear.algebra.Algebra;
import drasys.or.linear.algebra.AlgebraException;
import drasys.or.linear.algebra.CroutPivot;
import drasys.or.matrix.*;
public class LinearSolver {

    protected static Logger logger = Logger.getLogger(LinearSolver.class);
    private double[] B;
    private double[][] A;
    private VectorI solution;
    private double[]solutionArray;
    private double[] CheckB;
    private double det;

  public LinearSolver(double[][] M, double[] Y) {
    B = Y;
    A = M;
  }

  public double[] solve() throws AlgebraException, SingularException {
    return solve(false);
  }

  public double[] solve(boolean c) throws AlgebraException, SingularException {
    if(logger.isDebugEnabled()) logger.debug("LinearSolver: solving...");
    if(logger.isDebugEnabled()) logger.debug("B vector:");
    IOObject.PrintSingleArray(B);
    if(logger.isDebugEnabled()) logger.debug("A matrix:");
    IOObject.PrintDoubleArray(A);
    VectorI v = new ContiguousVector(B);
    MatrixI m = new ColumnArrayMatrix(A);
    CroutPivot gl = new CroutPivot(m);
    solution = gl.solveEquations(v);
    solutionArray = solution.getArray();
    if(c) {
      check(m,solution,gl);
    }
    return solutionArray;
  }

  void check(MatrixI m, VectorI s, CroutPivot cp) throws AlgebraException {
    Algebra a = new Algebra();
    DenseVector dv = a.multiply(m,s);
    CheckB = dv.getArray();
    det = cp.computeDeterminate();
  }

}

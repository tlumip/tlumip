
/**
 * Title:        null<p>
 * Description:  null<p>
 * Copyright:    null<p>
 * Company:      ECONorthwest<p>
 * @author
 * @version null
 */
package com.pb.tlumip.ed;
import com.pb.common.util.Debug;
import drasys.or.linear.SingularException;
import drasys.or.linear.algebra.Algebra;
import drasys.or.linear.algebra.AlgebraException;
import drasys.or.linear.algebra.CroutPivot;
import drasys.or.matrix.*;
public class LinearSolver {

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
    Debug.println("LinearSolver: solving...");
    Debug.println("B vector:");
    IOObject.PrintSingleArray(B);
    Debug.println("A matrix:");
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

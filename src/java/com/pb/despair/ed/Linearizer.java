
/**
 * Company:      ECONorthwest<p>
 * Transforms a set of equation and variable objects into an array form
 * to be solved.
 */

package com.pb.despair.ed;
import com.pb.common.util.Debug;

import java.util.Vector;

public class Linearizer {

  private Vector LinearEquations;
  private Vector VarNames;
  private double[] b;
  private double[][] m;
  private VariableStore vs;
  private Vector dependants;

    /**
     * Accepts a vector of linear equations and the variable store from a
     * SubModels.
     */
  Linearizer(Vector les,VariableStore vs ) throws InvalidSubModelException, NotSquareMatrixException, InvalidEquationException  {
    this.vs = vs;
    this.dependants = vs.getDependantVariables();
    LinearEquations = les;
    VarNames = new Vector();
    b= new double[LinearEquations.size()];
    linearize();
  }

  /**
   * Returns the B array which is an array of constants.  These constants are
   * formed from summing known terms in each equation.
   */
  double[] getBArray() {
    return b;
  }

  /**
   * Returns M array which is an array of the unknown variable parameters.
   */
  double[][] getMArray() {
    return m;
  }

  /**
   * Returns variable names that were included in the M matrix.
   */
  Vector getVarNames() {
    return VarNames;
  }

  /**
   * Simplifies equations and transforms variable data and equations into
   * array form that can be handled by the LinearSolver class.
   */
  protected void linearize() throws NotSquareMatrixException, InvalidEquationException, InvalidSubModelException {
    simplify();
    setVarNames();
    //throw an exception for a non square matrix here.
    if(dependants.size() != LinearEquations.size()) {
      throw new NotSquareMatrixException("Number of variables = "+ VarNames.size() + ".  Number of equations = " + LinearEquations.size());
    }
    m = new double[LinearEquations.size()][LinearEquations.size()];
    insertParameterValues();
    insertConstantValues(); 
  }

  /**
   * Calls the method simplify on each linear equation in this submodel.
   */
  private void simplify() throws InvalidEquationException {
    Debug.println("    Simplifying equations...");
    int maxEquations = LinearEquations.size();
    for(int i= 0; i< maxEquations; i++) {
      ((LinearEquation)(LinearEquations.get(i))).simplify();
    }
  }

  /**
   * Sums all known variables on the right hand side of the equation to produce
   * a constant array used in solving the system of linear equations.
   */
  private void insertConstantValues()  {
    Debug.println("    Setting constant values...");
    LinearTerm lt;
    for(int i=0; i<LinearEquations.size(); i++) {
       LinearEquation l = (LinearEquation)LinearEquations.get(i);
       Vector terms = l.getLinearTerms();
       for(int j=0; j < terms.size(); j++) {
          lt = (LinearTerm) terms.get(j);
          if(!lt.isLhs()) {
            try {
              b[i] = lt.getTermTotal() +b[i];
            } catch(UnknownValueException e) {
              e.printStackTrace();
            }
          }
       }//variable loop
    }//equation loop
  }

/**
 * Takes all dependant variables from equations and inserts their parameters
 * in a matrix to be solved.
 */
  private void insertParameterValues() {
    Debug.println("    Setting parameter values...");
    LinearTerm lt;
    Variable v;
    for(int i=0; i<LinearEquations.size(); i++) {
       LinearEquation l = (LinearEquation)LinearEquations.get(i);
       Vector terms = l.getLinearTerms();
       for(int j=0; j < terms.size(); j++) {
          lt = (LinearTerm) terms.get(j);
          if(lt.isLhs()) {
            v = lt.getVariable();
            m[i][(VarNames.indexOf(v.getIdentifier()))] = lt.getParameterValue();
          }
       }//variable loop
    }//equation loop
  }//insertParameterValues()


  /**
   * Retrieves all the dependant variable names from the variable store and
   * places the names in a VarNames vector.  An
   * exception is thrown if two or more left hand side dependant variables are
   * the same.
   */
  private void setVarNames() throws InvalidSubModelException {
    Debug.println("    Setting variable names for matrix...");
    int size = dependants.size();
    for(int i=0; i< size; i++) {
      Variable v = (Variable)dependants.get(i);
      if(VarNames.contains(v.getIdentifier())) {
        throw new InvalidSubModelException("Repeated dependant variables in variable store.");
      }
			VarNames.add(v.getIdentifier());
    }
  }

  /**
   * Place the solution in each dependant variable.
   */
  protected void fillSolution(double[] d) {
    int size = VarNames.size();
    Variable v;
    for(int i = 0; i<size;i++) {
      ((Variable)dependants.get(i)).setValue(d[i]);
    }
  }

}

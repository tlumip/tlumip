package com.pb.despair.pi;

public abstract class NumericalDerivativeSingleParameterFunction implements SingleParameterFunction {
    double delta;
    public NumericalDerivativeSingleParameterFunction(double delta) {
        this.delta=delta;
    }

    public double derivative(double point){
          double perturbed = evaluate(point+delta);
          return (perturbed-evaluate(point))/delta;
    }
}

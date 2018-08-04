package bnw.abm.intg.util.math;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

/**
 * PolynomialFunction class with extra features like antiderivative
 * 
 * @author niroshan
 *
 */
public class ExtendedPolynomialFunction extends PolynomialFunction {
	public ExtendedPolynomialFunction(double[] c) {
		super(c);
	}

	/**
	 * Finds the antiderivative of the function
	 * 
	 * @return function representing anti derivative
	 */
	public ExtendedPolynomialFunction antiderivative() {
		double[] coefficients = this.getCoefficients();
		double[] newCoefficients = new double[coefficients.length + 1];
		newCoefficients[0] = 0;
		for (int i = 1; i < newCoefficients.length; i++) {
			newCoefficients[i] = coefficients[i - 1] / i;
		}
		ExtendedPolynomialFunction antiDerivateFunction = new ExtendedPolynomialFunction(newCoefficients);
		return antiDerivateFunction;
	}

	/**
	 * Calculates the sum of y values for given integer x range
	 * 
	 * @param from_x
	 *            starting x integer (inclusive)
	 * @param to_x
	 *            ending x integer (inclusive)
	 * @return sum of values returned by function
	 */
	public double resultsSum(int from_x, int to_x) {
		double resultsSum = 0;
		for (int i = from_x; i <= to_x; i++) {
			resultsSum += this.value(i);
		}
		return resultsSum;
	}
}

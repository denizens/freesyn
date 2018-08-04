package bnw.abm.intg.util.math;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Range;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

/**
 * Builds a polynomial function using a data set
 * 
 * @author niroshan
 *
 */
public class FunctionBuilder {

	/**
	 * Calculates number of degrees in a ordered numbers list
	 * 
	 * @param dataDistribution
	 *            numbers list
	 * @return number of degrees
	 */
	public static int calculateDegrees(List<Number> dataDistribution) {
		int degrees = 0;
		// We just need to know if current trend is - or +
		double trend = dataDistribution.get(1).doubleValue() - dataDistribution.get(0).doubleValue();
		if (trend == 0) {
			degrees = 0;
		} else {
			degrees = 1;
		}
		for (int i = 2; i < dataDistribution.size(); i++) {
			double prev = dataDistribution.get(i - 1).doubleValue();
			double curr = dataDistribution.get(i).doubleValue();
			if (trend * (curr - prev) < 0) {// value becomes negative if trend direction has changed
				degrees++;
				trend = curr - prev;
			}
		}

		return degrees;
	}

	/**
	 * Uses curve fitting approach to build a polynomial function representing x and y points
	 * 
	 * @param xValues
	 *            ordered list of x coordinates, that corresponds to yValues
	 * @param yValues
	 *            ordered list of y coordinates, that corresponds to xValues
	 * @return the constructed function
	 */
	public static ExtendedPolynomialFunction buildFunction(List<Number> xValues, List<Number> yValues) {
		int degrees = calculateDegrees(yValues);
		List<WeightedObservedPoint> dataPoints = new ArrayList<>();
		for (int i = 0; i < xValues.size(); i++) {
			dataPoints.add(new WeightedObservedPoint(1, xValues.get(i).doubleValue(), yValues.get(i).doubleValue()));
		}

		PolynomialCurveFitter polyCurveFitter = PolynomialCurveFitter.create(degrees);
		double[] coeficients = polyCurveFitter.fit(dataPoints);
		for (int i = 0; i < coeficients.length; i++) {
			coeficients[i] = Math.round(coeficients[i]);
		}
		ExtendedPolynomialFunction polyFunc = new ExtendedPolynomialFunction(coeficients);
		return polyFunc;
	}

	/**
	 * Builds a function of (c+mx) type for each pair of coordinate points
	 * 
	 * @param xValues
	 *            ordered list of x coordinates, that corresponds to yValues
	 * @param yValues
	 *            ordered list of y coordinates, that corresponds to xValues
	 * @return Map of functions representing each two points
	 */
	public static Map<Range<Double>, ExtendedPolynomialFunction> buildPairwiseFunctions(List<Number> xValues, List<Number> yValues) {
		Map<Range<Double>, ExtendedPolynomialFunction> functions = new HashMap<>();
		for (int i = 1; i < yValues.size(); i++) {
			double gradient = (yValues.get(i).doubleValue() - yValues.get(i - 1).doubleValue())
					/ (xValues.get(i).doubleValue() - xValues.get(i - 1).doubleValue());
			double yIntercept = yValues.get(i).doubleValue() - (gradient * xValues.get(i).doubleValue());
			functions.put(Range.between(yValues.get(i - 1).doubleValue(), yValues.get(i).doubleValue()), new ExtendedPolynomialFunction(
					new double[] { yIntercept, gradient }));
		}
		return functions;
	}

}
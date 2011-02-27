package com.sarxos.medusa.math;

/**
 * Mathematical basics core.
 * 
 * @author Bartosz Firyn (SarXos)
 */
public class M {

	public static double sum(double[] v) {
		double s = 0;
		for (int i = 0; i < v.length; i++) {
			s += v[i];
		}
		return s;
	}

	/**
	 * Return double vector derivative value.
	 * 
	 * @param v - input double vector
	 * @return Derivative
	 */
	public static double[] diff(double[] v) {
		double[] d = new double[v.length - 1];
		for (int i = 0; i < v.length - 1; i++) {
			d[i] = v[i + 1] - v[i];
		}
		return d;
	}

	/**
	 * Return minimum value for two double input arguments.
	 * 
	 * @param a - first argument to compare
	 * @param b - second argument to compare
	 * @return Will return a if and only if a &lt;= b, in other case return b
	 */
	public static double min(double a, double b) {
		return a <= b ? a : b;
	}

	/**
	 * Return maximum value for two double input arguments.
	 * 
	 * @param a - first argument to compare
	 * @param b - second argument to compare
	 * @return Will return a if and only if a &gt;= b, in other case return b
	 */
	public static double max(double a, double b) {
		return a >= b ? a : b;
	}

}

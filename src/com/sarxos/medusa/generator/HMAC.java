package com.sarxos.medusa.generator;

import static com.sarxos.medusa.market.SignalType.BUY;
import static com.sarxos.medusa.market.SignalType.SELL;
import static com.sarxos.medusa.market.SignalType.WAIT;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sarxos.medusa.market.AbstractGenerator;
import com.sarxos.medusa.market.Quote;
import com.sarxos.medusa.market.Signal;
import com.sarxos.medusa.market.Signal.Value;
import com.sarxos.medusa.market.SignalParameter;
import com.sarxos.medusa.math.MA;


/**
 * Hull Moving Averages Crossover
 * 
 * @author Bartosz Firyn (SarXos)
 * @see http://www.justdata.com.au/Journals/AlanHull/hull_ma.htm
 */
public class HMAC extends AbstractGenerator<Quote> {

	@SignalParameter
	private int A = 20;

	@SignalParameter
	private int B = 40;

	@SignalParameter
	private int C = 30;

	public HMAC() {
	}

	public HMAC(int A, int B, int C) {
		init(A, B, C);
	}

	public void init(int A, int B, int C) {
		if (A < 2) {
			throw new IllegalArgumentException("EMA period canot be less then 2");
		}
		if (B < 2) {
			throw new IllegalArgumentException("SMA period canot be less then 2");
		}
		if (C < 2) {
			throw new IllegalArgumentException("EMAD period canot be less then 2");
		}

		this.A = A;
		this.B = B;
		this.C = C;
	}

	@Override
	public List<Signal> generate(Quote[] data, int R) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Signal generate(Quote q) {

		// calculate necessary coefficients
		double f1 = MA.hma(q, A); // fast HMA
		double s1 = MA.hma(q, B); // slow HMA

		// required to find optimal position opening moment
		double f2 = MA.hma(q.prev(), A); // prev fast HMA
		double s2 = MA.hma(q.prev(), B); // prev slow HMA

		double dec = MA.emad(q, C);
		double dep = MA.emad(q.prev(), C);

		Signal signal = null;

		boolean buy = false;
		boolean sell = false;
		
		buy = f1 - s1 > 0 && f2 - s2 <= 0 && dec > 0;
		sell = f1 - s1 < 0 && f2 - s2 >= 0 || (dec < 0 && dep > 0);

		if (sell && f1 - f2 > 0) {
			sell = false;
		}

		if (buy) {
			signal = new Signal(q, BUY);
		} else if (sell) {
			signal = new Signal(q, SELL);
		} else {
			signal = new Signal(q, WAIT);
		}

		if (isOutputting()) {
			signal.addValue(new Value("FHMA", f1));
			signal.addValue(new Value("SHMA", s1));
		}

		return signal;
	}

	@Override
	public Map<String, String> getParameters() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("A", Integer.toString(A));
		params.put("B", Integer.toString(B));
		params.put("C", Integer.toString(C));
		return params;
	}

	@Override
	public void setParameters(Map<String, String> params) {
		int A = Integer.parseInt(params.get("A").toString());
		int B = Integer.parseInt(params.get("B").toString());
		int C = Integer.parseInt(params.get("C").toString());
		init(A, B, C);
	}

	// TODO: remove - this is for test purpose only
	public static void main(String[] args) {

		Map<String, String> params = new HashMap<String, String>();
		params.put("A", Integer.toString(20));
		params.put("B", Integer.toString(30));
		params.put("C", Integer.toString(40));

		Set<Entry<String, String>> entries = params.entrySet();
		Iterator<Entry<String, String>> ei = entries.iterator();
		Entry<String, String> entry = null;

		while (ei.hasNext()) {

			entry = ei.next();

			String n = entry.getKey();
			String v = entry.getValue();

			Method[] methods = HMAC.class.getDeclaredMethods();

			// Field field = null;
			// try {
			// field = MAVD2.class.getDeclaredField(n);
			// } catch (SecurityException e) {
			// e.printStackTrace();
			// } catch (NoSuchFieldException e) {
			// e.printStackTrace();
			// }
			//
			// if (field == null) {
			// continue;
			// }
			//
			// field.setAccessible(true);
			// field.set(this, v);
		}

	}
}

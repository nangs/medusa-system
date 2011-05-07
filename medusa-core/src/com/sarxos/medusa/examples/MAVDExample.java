package com.sarxos.medusa.examples;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.sarxos.medusa.data.QuotesRemoteReader;
import com.sarxos.medusa.data.QuotesReaderException;
import com.sarxos.medusa.generator.MAVD;
import com.sarxos.medusa.market.Index;
import com.sarxos.medusa.market.Quote;
import com.sarxos.medusa.market.Signal;
import com.sarxos.medusa.market.SignalGenerator;
import com.sarxos.medusa.market.SignalType;
import com.sarxos.medusa.util.StoqReader;


public class MAVDExample {

	public static void main(String[] args) throws QuotesReaderException {
		
		int days = 1500;
		
		QuotesRemoteReader<Index> reader = new StoqReader<Index>(Index.class);
		List<Index> data = reader.read(new File("data/bre_d.csv").toURI());
		
		SignalGenerator<Quote> gen = new MAVD(3, 14, 30);

		List<Signal> signals = gen.generate(data.toArray(new Quote[data.size()]), days);
		
		Signal signal = null;
		Date date = null;
		SignalType type = null;

		Iterator<Signal> iterator = signals.iterator();

		int wallet = 0;
		double initial = 10000;
		double cash = initial;
		
		Quote q = null;
		
		boolean transaction = false;
		
		while (iterator.hasNext()) {
			
			signal = iterator.next();
			
			date = signal.getDate();
			type = signal.getType();
			q = signal.getQuote();
			
			transaction = false;
			
			if (signal.getType() == SignalType.SELL) {
				if (wallet > 0) {
					double sell = q.getClose() * wallet;
					cash += sell - sell * 0.0028 - wallet * 0.01;
					wallet = 0;
					transaction = true;
				}
			} else if (signal.getType() == SignalType.BUY) {
				if (wallet == 0) {
					double buy = q.getClose() * 60;
					cash -= buy - buy * 0.0028 - 60 * 0.01;
					wallet = 60;
					transaction = true;
				}
			}

			if (transaction) {
				System.out.println(
						Quote.DATE_FORMAT.format(date) + " " + 
						(type == SignalType.BUY ? "B" : "S") + " " + 
						(int)cash + " " + signal.getLevel()
				);
			}
		}
		
		double sum = data.get(data.size() - 1).getClose() * wallet + cash;
		double income = sum - initial;
		
		System.out.println("=== income: " + (int)income);
		System.out.println("=== per day: " + (int)(income / days));
	}
}
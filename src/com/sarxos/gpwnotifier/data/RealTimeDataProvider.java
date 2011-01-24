package com.sarxos.gpwnotifier.data;

import com.sarxos.gpwnotifier.market.Quote;
import com.sarxos.gpwnotifier.market.Symbol;


public interface RealTimeDataProvider {

	/**
	 * @param symbol
	 * @return Quote for given symbol.
	 * @throws DataProviderException
	 */
	public Quote getQuote(Symbol symbol) throws DataProviderException;
	
	/**
	 * @param symbol - market symbol to check
	 * @return true if provider can serve data for symbol, false otherwise
	 */
	public boolean canServe(Symbol symbol);
}

package com.sarxos.gpwnotifier.trader;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.sarxos.gpwnotifier.market.Paper;
import com.sarxos.gpwnotifier.market.Position;
import com.sarxos.gpwnotifier.market.Quote;
import com.sarxos.gpwnotifier.market.Signal;
import com.sarxos.gpwnotifier.market.SignalGenerator;


/**
 * Decision maker class. Here is decided if I shall buy or
 * sell observed paper.
 * 
 * @author Bartosz Firyn (SarXos)
 */
public class DecisionMaker implements PriceListener {

	/**
	 * Price observer.
	 */
	private Observer observer = null;
	
	/**
	 * Signals generator.
	 */
	private SignalGenerator<Quote> generator = null; 
	
	/**
	 * Decision listeners (traders).
	 */
	private List<DecisionListener> listeners = new LinkedList<DecisionListener>();
	
	/**
	 * Current wallet position.
	 */
	private Position position = Position.SHORT;
	
	
	public DecisionMaker(Observer observer) {
		this.observer = observer;
		this.observer.addPriceListener(this);
	}

	@Override
	public void priceChange(PriceEvent pe) {

		Wallet wallet = Wallet.getInstance();
		Paper paper = wallet.getPaper(observer.getSymbol());
		Quote quote = pe.getQuote();
		
		Signal signal = generator.generate(quote);
		DecisionEvent de = new DecisionEvent(this, paper, signal.getType());
		
		notifyListeners(de);
	}
	
	/**
	 * Notify all listeners about price change.
	 * 
	 * @param de - decision event (buy or sell paper)
	 */
	protected void notifyListeners(DecisionEvent de) {
		
		DecisionListener listener = null;
		ListIterator<DecisionListener> i = listeners.listIterator();
		
		while (i.hasNext()) {
			try {
				listener = i.next();
				listener.decisionChange(de);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @return Decision listeners array.
	 */
	public DecisionListener[] getDecisionListeners() {
		return listeners.toArray(new DecisionListener[listeners.size()]);
	}
	
	/**
	 * 
	 * @param listener
	 * @return true if listener was added or false if it is already on the list 
	 */
	public boolean addDecisionListener(DecisionListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
		return false;
	}
	
	/**
	 * Remove particular decision listener.
	 * 
	 * @param listener - decision listener to remove
	 * @return true if listener list contained specified element
	 */
	public boolean removeDecisionListener(DecisionListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * @return Paper observer
	 */
	public Observer getObserver() {
		return observer;
	}

	/**
	 * Set new price observer. If other observer has been set,
	 * decision maker object will be removed from its listeners.
	 * 
	 * @param observer - new observer to set
	 */
	public void setObserver(Observer observer) {
		if (this.observer != null) {
			this.observer.removePriceListener(this);
		}
		this.observer = observer;
		this.observer.addPriceListener(this);
	}
	
	/**
	 * Set signal generator.
	 * 
	 * @param generator - new generator to set.
	 */
	public void setGenerator(SignalGenerator<Quote> generator) {
		this.generator = generator;
	}
	
	/**
	 * @return Signal generator.
	 */
	public SignalGenerator<Quote> getGenerator() {
		return generator;
	}

	/**
	 * @return Current position being set (long, short).
	 */
	public Position getPosition() {
		return position;
	}

	/**
	 * Set current position (long or short).
	 * @param position - position type
	 */
	public void setPosition(Position position) {
		if (position == null) {
			throw new IllegalArgumentException("Position cannot be null");
		}
		this.position = position;
	}
}

/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.ext.javax.sip.congestion;

import javolution.util.FastList;

import org.apache.log4j.Logger;
import org.mobicents.commons.congestion.CongestionListener;
import org.mobicents.commons.congestion.CongestionMonitor;
import org.mobicents.ext.javax.sip.SipStackExtension;

/**
 * This Congestion Monitor monitors whether or not the number of server transactions has crossed 
 * the serverTransactionsThreshold and notifies its listeners. 
 * If it has then it monitors if the number of server transactions has reduced
 * and come under the backToNormalServrTransactionsThreshold and notifies its listeners
 * 
 * @author jean.deruelle@gmail.com
 *
 */
public class ServerTransactionCongestionMonitor implements
		CongestionMonitor {

	private static final Logger logger = Logger.getLogger(ServerTransactionCongestionMonitor.class);

	private static final String SOURCE = "SERVER_TRANSACTION";

	protected SipStackExtension sipStack;
	private final FastList<CongestionListener> listeners = new FastList<CongestionListener>();

	private volatile boolean tooManyStx = false;

	private long backToNormalServerTransactionsThreshold;
	private long serverTransactionsThreshold;
	
	public ServerTransactionCongestionMonitor(SipStackExtension sipStack) {
		this.sipStack = sipStack;
	}
	
	/* (non-Javadoc)
	 * @see org.mobicents.commons.congestion.CongestionMonitor#monitor()
	 */
	public void monitor() {
		if(logger.isTraceEnabled()) {
			logger.trace("Number of Server Transactions used = " + sipStack.getNumberOfServerTransactions());
		}
		if (this.tooManyStx) {
			if (sipStack.getNumberOfServerTransactions() < this.backToNormalServerTransactionsThreshold) {
				logger.warn("Number of Server Transactions used: " + sipStack.getNumberOfServerTransactions() + " < to the back to normal server transactions : " + this.backToNormalServerTransactionsThreshold);
				this.tooManyStx = false;

				// Lets notify the listeners
				for (FastList.Node<CongestionListener> n = listeners.head(), end = listeners.tail(); (n = n.getNext()) != end;) {
					CongestionListener listener = n.getValue();
					listener.onCongestionFinish(SOURCE);
				}
			}
		} else {
			if(sipStack.getNumberOfServerTransactions() > serverTransactionsThreshold) {
				logger.warn("Number of Server Transactions used: " + sipStack.getNumberOfServerTransactions() + " > to the max server transactions : " + this.serverTransactionsThreshold);
				this.tooManyStx = true;

				// Lets notify the listeners
				for (FastList.Node<CongestionListener> n = listeners.head(), end = listeners.tail(); (n = n.getNext()) != end;) {
					CongestionListener listener = n.getValue();
					listener.onCongestionStart(SOURCE);
				}
			}
		}		
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.commons.congestion.CongestionMonitor#addCongestionListener(org.mobicents.commons.congestion.CongestionListener)
	 */
	@Override
	public void addCongestionListener(CongestionListener listener) {
		this.listeners.add(listener);
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.commons.congestion.CongestionMonitor#removeCongestionListener(org.mobicents.commons.congestion.CongestionListener)
	 */
	@Override
	public void removeCongestionListener(CongestionListener listener) {
		this.listeners.remove(listener);
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.commons.congestion.CongestionMonitor#getSource()
	 */
	@Override
	public String getSource() {
		return SOURCE;
	}

	/**
	 * @return the serverTransactionsThreshold
	 */
	public long getServerTransactionsThreshold() {
		return serverTransactionsThreshold;
	}

	/**
	 * @param serverTransactionsThreshold the serverTransactionsThreshold to set
	 */
	public void setServerTransactionsThreshold(long serverTransactionsThreshold) {
		this.serverTransactionsThreshold = serverTransactionsThreshold;
	}

	/**
	 * @return the backToNormalServerTransactionsThreshold
	 */
	public long getBackToNormalServerTransactionsThreshold() {
		return backToNormalServerTransactionsThreshold;
	}

	/**
	 * @param backToNormalServerTransactionsThreshold the backToNormalServerTransactionsThreshold to set
	 */
	public void setBackToNormalServerTransactionsThreshold(
			long backToNormalServerTransactionsThreshold) {
		this.backToNormalServerTransactionsThreshold = backToNormalServerTransactionsThreshold;
	}
}

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
 * This Congestion Monitor monitors whether or not the number of active dialogs has crossed 
 * the dialogsThreshold and notifies its listeners. 
 * If it has then it monitors if the active dialogs has reduced
 * and come under the backToNormalDialogsThreshold and notifies its listeners
 * 
 * @author jean.deruelle@gmail.com
 *
 */
public class DialogCongestionMonitor implements
		CongestionMonitor {

	private static final Logger logger = Logger.getLogger(DialogCongestionMonitor.class);

	private static final String SOURCE = "DIALOG";

	protected SipStackExtension sipStack;
	private final FastList<CongestionListener> listeners = new FastList<CongestionListener>();

	private volatile boolean tooManyDialogs = false;

	private long backToNormalDialogsThreshold;
	private long dialogsThreshold;
	
	public DialogCongestionMonitor(SipStackExtension sipStack) {
		this.sipStack = sipStack;
	}
	
	/* (non-Javadoc)
	 * @see org.mobicents.commons.congestion.CongestionMonitor#monitor()
	 */
	public void monitor() {
		if(logger.isTraceEnabled()) {
			logger.trace("Number of Dialogs used = " + sipStack.getNumberOfDialogs());
		}
		if (this.tooManyDialogs) {
			if (sipStack.getNumberOfDialogs() < this.backToNormalDialogsThreshold) {
				logger.warn("Number of Dialogs used: " + sipStack.getNumberOfDialogs() + " < to the back to normal dialogs : " + this.backToNormalDialogsThreshold);
				this.tooManyDialogs = false;

				// Lets notify the listeners
				for (FastList.Node<CongestionListener> n = listeners.head(), end = listeners.tail(); (n = n.getNext()) != end;) {
					CongestionListener listener = n.getValue();
					listener.onCongestionFinish(SOURCE);
				}
			}
		} else {			
			if(sipStack.getNumberOfDialogs() > dialogsThreshold) {
				logger.warn("Number of Dialogs used: " + sipStack.getNumberOfDialogs() + " > to the max dialog : " + this.dialogsThreshold);
				this.tooManyDialogs = true;

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
	 * @return the DialogsThreshold
	 */
	public long getDialogsThreshold() {
		return dialogsThreshold;
	}

	/**
	 * @param DialogsThreshold the DialogsThreshold to set
	 */
	public void setDialogsThreshold(long DialogsThreshold) {
		this.dialogsThreshold = DialogsThreshold;
	}

	/**
	 * @return the backToNormalDialogsThreshold
	 */
	public long getBackToNormalDialogsThreshold() {
		return backToNormalDialogsThreshold;
	}

	/**
	 * @param backToNormalDialogsThreshold the backToNormalDialogsThreshold to set
	 */
	public void setBackToNormalDialogsThreshold(
			long backToNormalDialogsThreshold) {
		this.backToNormalDialogsThreshold = backToNormalDialogsThreshold;
	}
}

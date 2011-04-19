/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
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

package gov.nist.javax.sip.stack;

import gov.nist.core.LogWriter;

import java.io.IOException;

import javax.sip.TransactionState;

import org.mobicents.ext.javax.sip.SipStackExtension;

/**
 * @author jean.deruelle@gmail.com
 *
 */
public class MobicentsSIPServerTransaction extends SIPServerTransaction {

	public MobicentsSIPServerTransaction(SIPTransactionStack sipStack,
			MessageChannel newChannelToUse) {
		super(sipStack, newChannelToUse);
	}

	@Override
	protected void map() {		
		if(((SipStackExtension)sipStack).isSendTryingRightAway()) {
			sendTryingRightAway();
		} else {
			super.map();
		}
	}
	
	protected void sendTryingRightAway() {
		// note that TRYING is a pseudo-state for invite transactions
        int realState = super.getRealState();

        if (realState < 0 || realState == TransactionState._TRYING) {
            // Also sent by intermediate proxies. 
            // null check added as the stack may be stopped. TRYING is not sent by reliable transports.
            if (isInviteTransaction() && !this.isMapped && sipStack.getTimer() != null ) {
                this.isMapped = true;
                // Schedule a timer to fire in 200 ms if the
                // TU did not send a trying in that time.
                if (sipStack.isLoggingEnabled(LogWriter.TRACE_DEBUG))
                    sipStack.getStackLogger().logDebug(" sending Trying current state = "
                            + realState);
                try {
                    sendMessage(getOriginalRequest()
                            .createResponse(100, "Trying"));
                    if (sipStack.isLoggingEnabled(LogWriter.TRACE_DEBUG))
                        sipStack.getStackLogger().logDebug(" trying sent "
                                + super.getRealState());
                } catch (IOException ex) {
                    if (sipStack.isLoggingEnabled())
                        sipStack.getStackLogger().logError("IO error sending  TRYING");
                }
                
            } else {
                isMapped = true;
            }
        }

        // Pull it out of the pending transactions list.
        sipStack.removePendingTransaction(this);
	}
	
	@Override
	protected void startTransactionTimer() {
		if(!((SipStackExtension)sipStack).isSendTryingRightAway() || getLastResponseStatusCode() != 100) {
			super.startTransactionTimer();
		}
	}
	
	public void startTransactionTimerForTrying() {
		super.startTransactionTimer();
	}

	@Override
	public void setRetransmitTimer(int retransmitTimer) {
		if (retransmitTimer <= 0)
            throw new IllegalArgumentException(
                    "Retransmit timer must be positive!");
		if (!((SipStackExtension)sipStack).isSendTryingRightAway() && this.transactionTimerStarted.get())
            throw new IllegalStateException(
                    "Transaction timer is already started");
        BASE_TIMER_INTERVAL = retransmitTimer;
	}

}

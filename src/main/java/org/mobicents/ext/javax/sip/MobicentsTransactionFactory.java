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

package org.mobicents.ext.javax.sip;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.MobicentsSIPClientTransaction;
import gov.nist.javax.sip.stack.MobicentsSIPServerTransaction;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.stack.SIPTransactionStack;

import javax.sip.SipStack;

/**
 * @author jean.deruelle@gmail.com
 *
 */
public class MobicentsTransactionFactory implements TransactionFactory {

	private SIPTransactionStack sipStack;

	public SIPClientTransaction createClientTransaction(SIPRequest sipRequest,
			MessageChannel encapsulatedMessageChannel) {
		MobicentsSIPClientTransaction ct = new MobicentsSIPClientTransaction(sipStack,
				encapsulatedMessageChannel);
        ct.setOriginalRequest(sipRequest);
        return ct;
	}

	public SIPServerTransaction createServerTransaction(
			MessageChannel encapsulatedMessageChannel) {
		return new MobicentsSIPServerTransaction(sipStack, encapsulatedMessageChannel);        
	}

	public void setSipStack(SipStack sipStack) {
		this.sipStack = (SIPTransactionStack) sipStack;
	}
	

}

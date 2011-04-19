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

import gov.nist.javax.sip.SipProviderImpl;

/**
 * Extensions that don't make it in JAIN SIP as they are too specific to Mobicents use cases.
 * The properties specified here allow a stack to provide its own transaction factory to create SIPServerTransaction and SIPClientTransaction extension objects
 * and allow to provide its on sip provider factory to allow creating SipProviderImpl extensions
 * 
 * @author jean.deruelle@gmail.com
 *
 */
public interface SipStackExtension {
	public static final String TRANSACTION_FACTORY_CLASS_NAME = "org.mobicents.ext.java.sip.TRANSACTION_FACTORY";
	public static final String SIP_PROVIDER_FACTORY_CLASS_NAME = "org.mobicents.ext.java.sip.SIP_PROVIDER_FACTORY";
	public static final String SEND_TRYING_RIGHT_AWAY = "org.mobicents.ext.java.sip.SEND_TRYING_RIGHT_AWAY";
	
	public void setSendTryingRightAway(boolean sendTryingRightAway);
	public boolean isSendTryingRightAway();
	
	public void addSipProvider(SipProviderImpl sipProvider);
	public void removeSipProvider(SipProviderImpl sipProvider);
}

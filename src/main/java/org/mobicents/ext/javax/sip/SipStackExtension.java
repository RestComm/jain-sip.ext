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

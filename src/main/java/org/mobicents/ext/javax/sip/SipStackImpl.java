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
package org.mobicents.ext.javax.sip;

import gov.nist.core.CommonLogger;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPServerTransaction;

import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.sip.PeerUnavailableException;
import javax.sip.ProviderDoesNotExistException;
import javax.sip.SipException;

/**
 * @author jean.deruelle@gmail.com
 *
 */
public class SipStackImpl extends gov.nist.javax.sip.SipStackImpl implements SipStackExtension, NotificationListener, SipStackImplMBean {
	private static StackLogger logger = CommonLogger.getLogger(SipStackImpl.class);
	public static String JAIN_SIP_MBEAN_NAME = "org.mobicents.jain.sip:type=sip-stack,name=";
	
	protected TransactionFactory transactionFactory = null;
	protected SipProviderFactory sipProviderFactory = null;
	protected boolean sendTryingRightAway;
	ObjectName oname = null;
	MBeanServer mbeanServer = null;
	boolean isMBeanServerNotAvailable = false;
	
	public SipStackImpl(Properties configurationProperties) throws PeerUnavailableException {
		super(configurationProperties);
		// allow the stack to provide its own SIPServerTransaction/SIPClientTransaction extension instances
		String transactionFactoryClassName = configurationProperties.getProperty(TRANSACTION_FACTORY_CLASS_NAME);
		if(transactionFactoryClassName != null) {
			try {
	            transactionFactory = (TransactionFactory) Class.forName(transactionFactoryClassName).newInstance();
	            transactionFactory.setSipStack(this);
	            if(logger.isLoggingEnabled(StackLogger.TRACE_INFO)) {
	            	logger.logInfo("SIP Stack TransactionFactory set to " + transactionFactoryClassName);
				}
	        } catch (Exception e) {
	            String errmsg = "The TransactionFactory class name: "
	                    + transactionFactoryClassName
	                    + " could not be instantiated. Ensure the " + TRANSACTION_FACTORY_CLASS_NAME + " property has been set correctly and that the class is on the classpath.";
	            throw new PeerUnavailableException(errmsg, e);
	        }
	    }
		// allow the stack to provide its own SipProviderImpl extension instances
	    String sipProviderFactoryClassName = configurationProperties.getProperty(SIP_PROVIDER_FACTORY_CLASS_NAME);
		if(sipProviderFactoryClassName != null) {
			try {
	            sipProviderFactory = (SipProviderFactory) Class.forName(sipProviderFactoryClassName).newInstance();
	            sipProviderFactory.setSipStack(this);
	            if(logger.isLoggingEnabled(StackLogger.TRACE_INFO)) {
	            	logger.logInfo("SIP Stack SipProviderFactory set to " + sipProviderFactoryClassName);
				}
	        } catch (Exception e) {
	            String errmsg = "The SipProviderFactory class name: "
	                    + sipProviderFactoryClassName
	                    + " could not be instantiated. Ensure the " + SIP_PROVIDER_FACTORY_CLASS_NAME + " property has been set correctly and that the class is on the classpath.";
	            throw new PeerUnavailableException(errmsg, e);
	        }
	    }
		
		this.sendTryingRightAway = Boolean.valueOf(
			configurationProperties.getProperty(SEND_TRYING_RIGHT_AWAY,"false")).booleanValue();
		if(logger.isLoggingEnabled(StackLogger.TRACE_INFO)) {
        	logger.logInfo("SIP Stack send trying right away " + sendTryingRightAway);
		}
	}
	
	@Override
	public void start() throws ProviderDoesNotExistException, SipException {
		super.start();
		String mBeanName=JAIN_SIP_MBEAN_NAME + stackName;
		try {
			oname = new ObjectName(mBeanName);
			if (getMBeanServer() != null && !getMBeanServer().isRegistered(oname)) {
				getMBeanServer().registerMBean(this, oname);				
			}
		} catch (Exception e) {
			logger.logError("Could not register the stack as an MBean under the following name", e);
			throw new SipException("Could not register the stack as an MBean under the following name " + mBeanName + ", cause: " + e.getMessage(), e);
		}		
	}
	
	@Override
	public void stop() {
		String mBeanName=JAIN_SIP_MBEAN_NAME + stackName;
		try {
			if (oname != null && getMBeanServer() != null && getMBeanServer().isRegistered(oname)) {
				getMBeanServer().unregisterMBean(oname);
			}
		} catch (Exception e) {
			logger.logError("Could not unregister the stack as an MBean under the following name" + mBeanName);
		}
		super.stop();
	}
	
	/**
	 * Get the current MBean Server.
	 * 
	 * @return
	 * @throws Exception
	 */
	public MBeanServer getMBeanServer() throws Exception {
		if (mbeanServer == null && !isMBeanServerNotAvailable) {
			try {
				mbeanServer = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);				
			} catch (Exception e) {
				logger.logStackTrace(StackLogger.TRACE_DEBUG);
				logger.logWarning("No Mbean Server available, so JMX statistics won't be available");
				isMBeanServerNotAvailable = true;
			}
		}
		return mbeanServer;
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	public void handleNotification(Notification notification, Object handback) {
		logger.setStackProperties(super.getConfigurationProperties());
	}
	
	@Override
	public SIPClientTransaction createClientTransaction(SIPRequest sipRequest,
			MessageChannel encapsulatedMessageChannel) {
		if(transactionFactory == null) {
			return super.createClientTransaction(sipRequest, encapsulatedMessageChannel);
		}
		return transactionFactory.createClientTransaction(sipRequest, encapsulatedMessageChannel);
	}
	
	@Override
	public SIPServerTransaction createServerTransaction(MessageChannel encapsulatedMessageChannel) {
		if(transactionFactory == null) {
			return super.createServerTransaction(encapsulatedMessageChannel);
		}
		return transactionFactory.createServerTransaction(encapsulatedMessageChannel);
	}

	
	public int getNumberOfClientTransactions() {		
		return getClientTransactionTableSize();
	}

	public int getNumberOfDialogs() {
		return dialogTable.size();	
	}
	
	public int getNumberOfEarlyDialogs() {
		return earlyDialogTable.size();	
	}

	public int getNumberOfServerTransactions() {
		return getServerTransactionTableSize();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.SipStackExtension#setSendTryingRightAway(boolean)
	 */
	public void setSendTryingRightAway(boolean sendTryingRightAway) {
		this.sendTryingRightAway = sendTryingRightAway;
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.SipStackExtension#isSendTryingRightAway()
	 */
	public boolean isSendTryingRightAway() {
		return sendTryingRightAway;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.SipStackExtension#addSipProvider(gov.nist.javax.sip.SipProviderImpl)
	 */
	public void addSipProvider(SipProviderImpl sipProvider) {
		sipProviders.add(sipProvider);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.SipStackExtension#removeSipProvider(gov.nist.javax.sip.SipProviderImpl)
	 */
	public void removeSipProvider(SipProviderImpl sipProvider) {
		sipProviders.remove(sipProvider);
	}
}

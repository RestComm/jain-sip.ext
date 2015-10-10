/*
 * TeleStax, Open Source Cloud Communications.
 * Copyright 2011-2013 and individual contributors by the @authors tag. 
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

import gov.nist.core.CommonLogger;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPMessageValve;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sip.SipStack;
import javax.sip.header.Header;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.mobicents.commons.congestion.CongestionListener;
import org.mobicents.ext.javax.sip.SipStackExtension;

/**
 * This is a congestion control valve that JSIP apps can use to stop traffic without breaking
 * existing dialogs when the number of
 * server transactions or dialogs or the memory or CPU reaches a given limit. <br/>
 * See {@link CongestionControlTimerTask} for the list of properties  
 * 
 * A background task gathers information about the current server congestion. The data collection interval  can be adjusted, and congestion control deactivated,  by setting the interval to 0 or a
 * negative value through org.mobicents.ext.javax.sip.congestion.CONGESTION_CONTROL_MONITOR_INTERVAL property
 * 
 * The drop policy is specified in org.mobicents.ext.javax.sip.congestion.DROP_RESPONSE_STATUS 
 * where "0" or negative value means silent drop and any positive number will be
 * interpreted as the status code of the error response that will be generated.
 * 
 * To enable this in your application you must specify this property:
 * gov.nist.javax.sip.SIP_MESSAGE_VALVE=org.mobicents.ext.javax.sip.congestion.CongestionControlMessageValve
 * 
 * It is advised to extend this class to add your application-specific control conditions or 
 * if you need to add any header to the response generated back (like Retry-After Header)
 * by overriding the modifyCongestionResponse(SIPResponse response). 
 * 
 * @author vladimirralev
 * @author jean.deruelle@gmail.com
 *
 */
public class CongestionControlMessageValve implements SIPMessageValve, CongestionListener {
	private static StackLogger logger = CommonLogger.getLogger(CongestionControlMessageValve.class);
	protected SipStackExtension sipStack;
	protected transient ScheduledFuture congestionControlTimerFuture;
	private long congestionControlMonitoringInterval; //30 sec
	//used for the congestion control mechanism
	private ScheduledThreadPoolExecutor congestionControlThreadPool = null;
    // High water mark for ServerTransaction Table
    // after which requests are dropped.
    protected int dropResponseStatus;
    private boolean rejectMessages;
    private List<String> blockedList = null;
    
	public boolean processRequest(SIPRequest request,
			MessageChannel messageChannel) {
		String requestMethod = request.getMethod();
		
		// We should not attempt to drop these requests because they actually free resources
		// which is our goal in congested mode
		boolean undropableMethod = requestMethod.equals(Request.BYE) 
		|| requestMethod.equals(Request.ACK) 
		|| requestMethod.equals(Request.PRACK) 
		|| requestMethod.equals(Request.CANCEL);
		
		if(!undropableMethod) {
			if (!securityCheck(request)){
				// we drop all requests from sip scanners
				return false;
			}
			if(rejectMessages) {
				// Allow directly any subsequent requests
				if(request.getToTag() != null) {
					return true;
				}
				if(dropResponseStatus > 0) {
					SIPResponse response = request.createResponse(dropResponseStatus);
					modifyCongestionResponse(response);
					try {
						messageChannel.sendMessage(response);
					} catch (IOException e) {
						logger.logError("Failed to send congestion control error response" + response, e);
					}
				}
				return false; // Do not pass this request to the pipeline
			}
		}
		return true; // OK, the processing of the request can continue
	}

	/**
     * @param request
     * @return
     */
    private boolean securityCheck(Request request) {
        //        User-Agent: sipcli/v1.8
        //        User-Agent: friendly-scanner
        //        To: "sipvicious" <sip:100@1.1.1.1>
        //        From: "sipvicious" <sip:100@1.1.1.1>;tag=3336353363346565313363340133313330323436343236
        //        From: "1" <sip:1@87.202.36.237>;tag=3e7a78de
        Header userAgentHeader = request.getHeader("User-Agent");
        Header toHeader = request.getHeader("To");
        Header fromHeader = request.getHeader("From");

        for (String blockedValue: blockedList){
            if(userAgentHeader != null && userAgentHeader.toString().toLowerCase().contains(blockedValue.toLowerCase())) {
                return false;
            } else if (toHeader != null && toHeader.toString().toLowerCase().contains(blockedValue.toLowerCase())) {
                return false;
            } else if (fromHeader != null && fromHeader.toString().toLowerCase().contains(blockedValue.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
	
	protected void modifyCongestionResponse(SIPResponse response) {
		//Default does nothing
		
	}

	public boolean processResponse(Response response,
			MessageChannel messageChannel) {
		return true;
	}

	public void destroy() {
		logger.logInfo("Destorying the congestion control valve " + this);
		if(congestionControlTimerFuture != null) {
			congestionControlTimerFuture.cancel(true);
		}
		if(congestionControlThreadPool != null) {
			congestionControlThreadPool.shutdownNow();
		}
		
	}

	public void init(SipStack stack) {
		sipStack = (SipStackExtension) stack;
		logger.logInfo("Initializing congestion control valve");
		String blockedValues = sipStack.getConfigurationProperties().getProperty("org.mobicents.ext.javax.sip.congestion.SIP_SCANNERS", "sipvicious,sipcli,friendly-scanner");
        blockedList = new ArrayList<String>(Arrays.asList(blockedValues.split(",")));
		String congestionControlMonitoringIntervalString = sipStack.getConfigurationProperties().getProperty("org.mobicents.ext.javax.sip.congestion.CONGESTION_CONTROL_MONITOR_INTERVAL", "30000");
		congestionControlMonitoringInterval = new Integer(congestionControlMonitoringIntervalString);
		if(congestionControlTimerFuture == null && congestionControlMonitoringInterval > 0) { 					
			String dropResponseStatusString = sipStack.getConfigurationProperties().getProperty("org.mobicents.ext.javax.sip.congestion.DROP_RESPONSE_STATUS", "503");
			dropResponseStatus = new Integer(dropResponseStatusString);
						
			congestionControlThreadPool = new ScheduledThreadPoolExecutor(2,
					new ThreadPoolExecutor.CallerRunsPolicy());
			congestionControlThreadPool.prestartAllCoreThreads();
			CongestionControlTimerTask congestionControlTimerTask = new CongestionControlTimerTask(this, sipStack);
			
				congestionControlTimerFuture = congestionControlThreadPool.scheduleWithFixedDelay(congestionControlTimerTask, 0, congestionControlMonitoringInterval, TimeUnit.MILLISECONDS);
			if(logger.isLoggingEnabled(CommonLogger.TRACE_INFO)) {
		 		logger.logInfo("Congestion control background task started and checking every " + congestionControlMonitoringInterval + " milliseconds.");
		 	}
		} else {
			if(logger.isLoggingEnabled(CommonLogger.TRACE_INFO)) {
		 		logger.logInfo("No Congestion control background task started since the checking interval is equals to " + congestionControlMonitoringInterval + " milliseconds.");
		 	}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.commons.congestion.CongestionListener#onCongestionStart(java.lang.String)
	 */
	public void onCongestionStart(String source) {
		this.rejectMessages = true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.commons.congestion.CongestionListener#onCongestionFinish(java.lang.String)
	 */
	public void onCongestionFinish(String source) {
		this.rejectMessages = false;
	}

	/**
	 * @return the rejectMessages
	 */
	public boolean isRejectMessages() {
		return rejectMessages;
	}
}

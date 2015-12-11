/*
* Conditions Of Use
*
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), an agency of the Federal Government.
* Pursuant to title 15 Untied States Code Section 105, works of NIST
* employees are not subject to copyright protection in the United States
* and are considered to be in the public domain.  As a result, a formal
* license is not needed to use the software.
*
* This software is provided by NIST as a service and is expressly
* provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
* OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
* AND DATA ACCURACY.  NIST does not warrant or make any representations
* regarding the use of the software or the results thereof, including but
* not limited to the correctness, accuracy, reliability or usefulness of
* the software.
*
* Permission to use this software is contingent upon your acceptance
* of the terms of this agreement
*
* .
*
*/
package org.mobicents.ext.javax.sip.timers;

import gov.nist.core.CommonLogger;
import gov.nist.core.NamingThreadFactory;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.stack.SIPStackTimerTask;
import gov.nist.javax.sip.stack.timers.SipTimer;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * https://github.com/Mobicents/jain-sip.ext/issues/4
 * Implementation of the SIP Timer based on io.netty.util.HashedWheelTimer
 * 
 * @author jean.deruelle@gmail.com
 *
 */
public class HashedWheelSipTimer implements SipTimer {
	private static StackLogger logger = CommonLogger.getLogger(HashedWheelSipTimer.class);
	protected SipStackImpl sipStackImpl;
	HashedWheelTimer hashedWheelSipTimer;
	protected AtomicBoolean started = new AtomicBoolean(false);
    
	public HashedWheelSipTimer() {
		hashedWheelSipTimer = new HashedWheelTimer(new NamingThreadFactory("hashed_wheel_sip_timer"), 50L, TimeUnit.MILLISECONDS);		
	}
	
	private class HashedWheelSipTimerTask implements TimerTask {
		private SIPStackTimerTask task;
		private Timeout timeout;
		private long period;

		public HashedWheelSipTimerTask(SIPStackTimerTask task, long period) {
			this.task= task;
			task.setSipTimerTask(this);
			this.period = period;
		}
		
		public void run(Timeout timeout) {
			 try {
				 // task can be null if it has been cancelled
				 if(task != null) {
					 task.runTask();					 
				 }
				 if(period > 0) {
					 timeout = hashedWheelSipTimer.newTimeout(this, period, TimeUnit.MILLISECONDS);
				 }
	        } catch (Exception e) {
	            logger.logError("SIP stack timer task failed due to exception:", e);
	        }
		}
		
		public boolean cancel() {
			if(task != null) {
				task.cleanUpBeforeCancel();
				task = null;
			}
			return timeout.cancel();
		}

		/**
		 * @return the timeout
		 */
		public Timeout getTimeout() {
			return timeout;
		}

		/**
		 * @param timeout the timeout to set
		 */
		public void setTimeout(Timeout timeout) {
			this.timeout = timeout;
		}
	}
	
	/* (non-Javadoc)
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#stop()
	 */
	public void stop() {
		started.set(false);
		hashedWheelSipTimer.stop();
		logger.logStackTrace(StackLogger.TRACE_DEBUG);
		if(logger.isLoggingEnabled(StackLogger.TRACE_INFO)) {
			logger.logInfo("the sip stack timer " + this.getClass().getName() + " has been stopped");
		}
	}

	/* (non-Javadoc)
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#schedule(gov.nist.javax.sip.stack.SIPStackTimerTask, long)
	 */
	public boolean schedule(SIPStackTimerTask task, long delay) {
		if(!started.get()) {
			throw new IllegalStateException("The SIP Stack Timer has been stopped, no new tasks can be scheduled !");
		}
		HashedWheelSipTimerTask timerTask = new HashedWheelSipTimerTask(task, -1);
		Timeout timeout = hashedWheelSipTimer.newTimeout(timerTask, delay, TimeUnit.MILLISECONDS);
		timerTask.setTimeout(timeout);
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#scheduleWithFixedDelay(gov.nist.javax.sip.stack.SIPStackTimerTask, long, long)
	 */
	public boolean scheduleWithFixedDelay(SIPStackTimerTask task, long delay,
			long period) {
		if(!started.get()) {
			throw new IllegalStateException("The SIP Stack Timer has been stopped, no new tasks can be scheduled !");
		}
		HashedWheelSipTimerTask timerTask = new HashedWheelSipTimerTask(task, period);
		Timeout timeout = hashedWheelSipTimer.newTimeout(timerTask, delay, TimeUnit.MILLISECONDS);
		timerTask.setTimeout(timeout);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#start(gov.nist.javax.sip.SipStackImpl, java.util.Properties)
	 */
	public void start(SipStackImpl sipStack, Properties configurationProperties) {
		sipStackImpl= sipStack;
		// TODO have a param in the stack properties to set the number of thread for the timer executor
		hashedWheelSipTimer.start();
		started.set(true);
		if(logger.isLoggingEnabled(StackLogger.TRACE_INFO)) {
			logger.logInfo("the sip stack timer " + this.getClass().getName() + " has been started");
		}
	}
	/*
	 * (non-Javadoc)
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#cancel(gov.nist.javax.sip.stack.SIPStackTimerTask)
	 */
	public boolean cancel(SIPStackTimerTask task) {
		return ((HashedWheelSipTimerTask)task.getSipTimerTask()).cancel();
	}

	/*
	 * (non-Javadoc)
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#isStarted()
	 */
	public boolean isStarted() {
		return started.get();
	}
	
}

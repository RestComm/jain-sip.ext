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

import gov.nist.core.CommonLogger;
import gov.nist.core.StackLogger;

import org.mobicents.commons.congestion.CPUProcessCongestionMonitor;
import org.mobicents.commons.congestion.CongestionListener;
import org.mobicents.commons.congestion.MemoryCongestionMonitor;
import org.mobicents.ext.javax.sip.SipStackExtension;

/**
 * Task responsible for monitoring at regular intervals whether or not the configured congestion control mechanisms have switched on or off.
 * The following congestion control mechanisms are available :
 * <ul>
 * 	<li>Memory : Monitor if the JVM Used Memory has crossed the org.mobicents.ext.javax.sip.congestion.MEMORY_THRESHOLD threshold 
 * or went back under org.mobicents.ext.javax.sip.congestion.BACK_TO_NORMAL_MEMORY_THRESHOLD threshold</li>
 * 	<li>CPU : Monitor if the Process Used CPU has crossed the org.mobicents.ext.javax.sip.congestion.CPU_THRESHOLD threshold 
 * or went back under org.mobicents.ext.javax.sip.congestion.BACK_TO_NORMAL_CPU_THRESHOLD threshold</li>
 * 	<li>Server Transactions: Monitor if the number of active Server Transactions has crossed the org.mobicents.ext.javax.sip.congestion.SERVER_TRANSACTIONS_THRESHOLD threshold 
 * or went back under org.mobicents.ext.javax.sip.congestion.BACK_TO_NORMAL_SERVER_TRANSACTIONS_THRESHOLD threshold</li>
 * 	<li>Dialogs: Monitor if the number of active Dialogs has crossed the org.mobicents.ext.javax.sip.congestion.DIALOGS_THRESHOLD threshold 
 * or went back under org.mobicents.ext.javax.sip.congestion.BACK_TO_NORMAL_DIALOGS_THRESHOLD threshold</li>
 * </ul>
 * 
 * <i>Note: Any congestion control mechanism can be disabled by setting their threshold properties to a negative value</i>
 * 
 * @author jean.deruelle@gmail.com
 *
 */
public class CongestionControlTimerTask implements Runnable {
	private static StackLogger logger = CommonLogger.getLogger(CongestionControlTimerTask.class);
	private MemoryCongestionMonitor memoryCongestionMonitor;
	private CPUProcessCongestionMonitor cpuProcessCongestionMonitor;
	private ServerTransactionCongestionMonitor maxServerTransactionsCongestionMonitor;
	private DialogCongestionMonitor dialogCongestionMonitor;
	
	public CongestionControlTimerTask(CongestionListener congestionListener, SipStackExtension sipStack) {
		String memoryThresholdString = sipStack.getConfigurationProperties().getProperty("org.mobicents.ext.javax.sip.congestion.MEMORY_THRESHOLD", "85");
		String backToNormalMemoryThresholdString = sipStack.getConfigurationProperties().getProperty("org.mobicents.ext.javax.sip.congestion.BACK_TO_NORMAL_MEMORY_THRESHOLD", "80");
		String cpuThresholdString = sipStack.getConfigurationProperties().getProperty("org.mobicents.ext.javax.sip.congestion.CPU_PROCESS_THRESHOLD", "85");
		String backToNormalCPUThresholdString = sipStack.getConfigurationProperties().getProperty("org.mobicents.ext.javax.sip.congestion.BACK_TO_NORMAL_CPU_PROCESS_THRESHOLD", "80");
		String serverTransactionsString = sipStack.getConfigurationProperties().getProperty("org.mobicents.ext.javax.sip.congestion.SERVER_TRANSACTIONS_THRESHOLD", "15000");
		String backToNormalServerTransactionsString = sipStack.getConfigurationProperties().getProperty("org.mobicents.ext.javax.sip.congestion.BACK_TO_NORMAL_SERVER_TRANSACTIONS_THRESHOLD", "10000");
		String dialogsString = sipStack.getConfigurationProperties().getProperty("org.mobicents.ext.javax.sip.congestion.DIALOGS_THRESHOLD", "200000");
		String backToNormalDialogsString = sipStack.getConfigurationProperties().getProperty("org.mobicents.ext.javax.sip.congestion.BACK_TO_NORMAL_DIALOGS_THRESHOLD", "180000");
		int memoryThreshold = new Integer(memoryThresholdString);
		int backToNormalMemoryThreshold = new Integer(backToNormalMemoryThresholdString);
		double cpuProcessThreshold = new Double(cpuThresholdString);
		double backToNormalCPUProcessThreshold = new Double(backToNormalCPUThresholdString);
		long maxServerTransactions = new Long(serverTransactionsString);
		long backToNormalMaxServerTransactions = new Long(backToNormalServerTransactionsString);
		long maxDialogs = new Long(dialogsString);
		long backToNormalDialogs = new Long(backToNormalDialogsString);
		
		if(memoryThreshold > 0) {
			memoryCongestionMonitor = new MemoryCongestionMonitor();
			memoryCongestionMonitor.setMemoryThreshold(memoryThreshold);
			memoryCongestionMonitor.setBackToNormalMemoryThreshold(backToNormalMemoryThreshold);
			memoryCongestionMonitor.addCongestionListener(congestionListener);
		}
		if(cpuProcessThreshold > 0) {
			cpuProcessCongestionMonitor = new CPUProcessCongestionMonitor();
			cpuProcessCongestionMonitor.setCPUThreshold(cpuProcessThreshold);
			cpuProcessCongestionMonitor.setBackToNormalCPUThreshold(backToNormalCPUProcessThreshold);
			cpuProcessCongestionMonitor.addCongestionListener(congestionListener);
		}
		if(maxServerTransactions > 0) {
			maxServerTransactionsCongestionMonitor = new ServerTransactionCongestionMonitor(sipStack);
			maxServerTransactionsCongestionMonitor.setServerTransactionsThreshold(maxServerTransactions);
			maxServerTransactionsCongestionMonitor.setBackToNormalServerTransactionsThreshold(backToNormalMaxServerTransactions);
			maxServerTransactionsCongestionMonitor.addCongestionListener(congestionListener);
		}
		if(maxDialogs > 0) {
			dialogCongestionMonitor = new DialogCongestionMonitor(sipStack);
			dialogCongestionMonitor.setDialogsThreshold(maxDialogs);
			dialogCongestionMonitor.setBackToNormalDialogsThreshold(backToNormalDialogs);
			dialogCongestionMonitor.addCongestionListener(congestionListener);
		}
	}
	
	public void run() {
		if(logger.isLoggingEnabled(CommonLogger.TRACE_TRACE)) {
			logger.logTrace("JAIN SIP Ext Congestion Control Timer Task now running");
		}
		if(memoryCongestionMonitor != null) {
			memoryCongestionMonitor.monitor();
		}
		if(cpuProcessCongestionMonitor != null) {
			cpuProcessCongestionMonitor.monitor();
		}
		if(maxServerTransactionsCongestionMonitor != null) {
			maxServerTransactionsCongestionMonitor.monitor();
		}
		if(dialogCongestionMonitor != null) {
			dialogCongestionMonitor.monitor();
		}
	}
}

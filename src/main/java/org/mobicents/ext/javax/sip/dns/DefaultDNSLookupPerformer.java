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

package org.mobicents.ext.javax.sip.dns;

import gov.nist.javax.sip.stack.HopImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.sip.ListeningPoint;
import javax.sip.address.Hop;

import org.apache.log4j.Logger;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * @author jean.deruelle@gmail.com
 *
 */
public class DefaultDNSLookupPerformer implements DNSLookupPerformer {
	private static final Logger logger = Logger.getLogger(DefaultDNSLookupPerformer.class);
	
	/* (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.dns.DNSLookupPerformer#performSRVLookup(org.xbill.DNS.Name)
	 */
	public List<Record> performSRVLookup(String replacement) {
		if(logger.isDebugEnabled()) {
			logger.debug("doing SRV lookup for replacement " + replacement);
		}
		Record[] srvRecords = null;
		try {
			srvRecords = new Lookup(replacement, Type.SRV).run();
		} catch (TextParseException e) {
			logger.error("Impossible to parse the parameters for dns lookup",e);
		}
		if(srvRecords != null && srvRecords.length > 0) {
			return Arrays.asList(srvRecords);	
		}
		return new ArrayList<Record>(0);
	}
	
	/* (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.dns.DNSLookupPerformer#performNAPTRLookup(java.lang.String, boolean, java.util.Set)
	 */
	public List<NAPTRRecord> performNAPTRLookup(String domain, boolean isSecure, Set<String> supportedTransports) {
		List<NAPTRRecord> records = new ArrayList<NAPTRRecord>();
		if(logger.isDebugEnabled()) {
			logger.debug("doing NAPTR lookup for domain " + domain + ", isSecure " + isSecure + ", supportedTransports " + supportedTransports.toString());
		}
		Record[] naptrRecords = null;
		try {
			naptrRecords = new Lookup(domain, Type.NAPTR).run();
		} catch (TextParseException e) {
			logger.warn("Couldn't parse domain " + domain, e);
		}	
		if(naptrRecords != null) {
			for (Record record : naptrRecords) {
				NAPTRRecord naptrRecord = (NAPTRRecord) record;
				if(isSecure) {
					// First, a client resolving a SIPS URI MUST discard any services that
					// do not contain "SIPS" as the protocol in the service field.
					if(naptrRecord.getService().startsWith(SERVICE_SIPS)) {
						records.add(naptrRecord);
					}
				} else {	
					// The converse is not true, however.
					if(!naptrRecord.getService().startsWith(SERVICE_SIPS) || 
							(naptrRecord.getService().startsWith(SERVICE_SIPS) && supportedTransports.contains(ListeningPoint.TLS))) {
						//A client resolving a SIP URI SHOULD retain records with "SIPS" as the protocol, if the client supports TLS
						if((naptrRecord.getService().contains(SERVICE_D2U) && supportedTransports.contains(ListeningPoint.UDP)) ||
								naptrRecord.getService().contains(SERVICE_D2T) && (supportedTransports.contains(ListeningPoint.TCP) || supportedTransports.contains(ListeningPoint.TLS))) {
							// Second, a client MUST discard any service fields that identify
							// a resolution service whose value is not "D2X", for values of X that
							// indicate transport protocols supported by the client.
							records.add(naptrRecord);
						} else if(naptrRecord.getService().equals(SERVICE_E2U)) {
							// ENUM support
							records.add(naptrRecord);
						}
					} 
				}				
			}
		}			
		return records;
	}

	/* (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.dns.DNSLookupPerformer#locateHopsForNonNumericAddressWithPort(java.lang.String, int, java.lang.String)
	 */
	public Queue<Hop> locateHopsForNonNumericAddressWithPort(String host, int port, String transport) {
		Queue<Hop> priorityQueue = new LinkedList<Hop>();
		
		try {
			Record[] aRecords = new Lookup(host, Type.A).run();
			if(logger.isDebugEnabled()) {
				logger.debug("doing A lookup for host:port/transport = " + host + ":" + port + "/" + transport);
			}
			if(aRecords != null && aRecords.length > 0) {
				for(Record aRecord : aRecords) {
					priorityQueue.add(new HopImpl(((ARecord)aRecord).getAddress().getHostAddress(), port, transport));
				}
			}	
		} catch (TextParseException e) {
			logger.warn("Couldn't parse domain " + host, e);
		}	
		try {
			final Record[] aaaaRecords = new Lookup(host, Type.AAAA).run();
			if(logger.isDebugEnabled()) {
				logger.debug("doing AAAA lookup for host:port/transport = " + host + ":" + port + "/" + transport);
			}
			if(aaaaRecords != null && aaaaRecords.length > 0) {
				for(Record aaaaRecord : aaaaRecords) {
					priorityQueue.add(new HopImpl(((AAAARecord)aaaaRecord).getAddress().getHostAddress(), port, transport));
				}
			}			
		} catch (TextParseException e) {
			logger.warn("Couldn't parse domain " + host, e);
		}	
		return priorityQueue;
	}
}

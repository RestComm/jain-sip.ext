/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.sip.ListeningPoint;

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
public class DNSLookupPerformer {
	private static final Logger logger = Logger.getLogger(DNSLookupPerformer.class);
	
	public static final String SERVICE_SIPS = "SIPS";
	public static final String SERVICE_D2U = "D2U";
	public static final String SERVICE_D2T = "D2T";
	
	
	public static List<NAPTRRecord> performNAPTRLookup(String domain, boolean isSecure, Set<String> supportedTransports) {
		List<NAPTRRecord> records = new ArrayList<NAPTRRecord>();
		
		try {
			Record[] naptrRecords = new Lookup(domain, Type.NAPTR).run();
			for (NAPTRRecord record : (NAPTRRecord[]) naptrRecords) {
				if(isSecure) {
					// First, a client resolving a SIPS URI MUST discard any services that
					// do not contain "SIPS" as the protocol in the service field.
					if(record.getService().startsWith(SERVICE_SIPS)) {
						records.add(record);
					}
				} else {	
					// The converse is not true, however.
					if(!record.getService().startsWith(SERVICE_SIPS) || 
							(record.getService().startsWith(SERVICE_SIPS) && supportedTransports.contains(ListeningPoint.TLS))) {
						//A client resolving a SIP URI SHOULD retain records with "SIPS" as the protocol, if the client supports TLS
						if((record.getService().contains(SERVICE_D2U) && supportedTransports.contains(ListeningPoint.UDP)) ||
								record.getService().contains(SERVICE_D2T) && (supportedTransports.contains(ListeningPoint.TCP) || supportedTransports.contains(ListeningPoint.TLS))) {
							// Second, a client MUST discard any service fields that identify
							// a resolution service whose value is not "D2X", for values of X that
							// indicate transport protocols supported by the client.
							records.add(record);
						}
					}
				}
			}
		} catch (TextParseException e) {
			logger.warn("Couldn't parse domain " + domain, e);
		}
		if(records.size() > 0) {
			java.util.Collections.sort(records, new NAPTRRecordComparator());
		}
		return records;
	}

	/**
	 * 
	 * @param host
	 * @return
	 */
	public static ARecord[] performALookup(String host) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 
	 * @param host
	 * @return
	 */
	public static AAAARecord[] performAAAALookup(String host) {
		// TODO Auto-generated method stub
		return null;
	}
}

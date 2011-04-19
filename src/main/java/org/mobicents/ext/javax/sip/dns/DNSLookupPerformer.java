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

import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.sip.address.Hop;

import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Record;

/**
 * Interface to implement for doing the DNS lookups, it uses DNS Java
 * 
 * @author jean.deruelle@gmail.com
 *
 */
public interface DNSLookupPerformer {

	public static final String SERVICE_SIPS = "SIPS";
	public static final String SERVICE_D2U = "D2U";
	public static final String SERVICE_E2U = "E2U+sip";
	public static final String SERVICE_D2T = "D2T";

	/**
	 * Performing the DNS SRV Lookup for a given Name
	 * @param replacement the replacement for which to perform the SRV lookup
	 * @return an unsorted list of SRV records
	 */
	List<Record> performSRVLookup(String replacement);

	/**
	 * Performing the DNS NAPTR Lookup for a given domain, whether or not it is secure and the supported transports
	 * @param domain the domain to resolve
	 * @param isSecure whether or not it is secure
	 * @param supportedTransports the transports supported locally
	 * @return an unsorted list of NAPTR Records
	 */
	List<NAPTRRecord> performNAPTRLookup(String domain,
			boolean isSecure, Set<String> supportedTransports);

	/**
	 * Perform the A and AAAA lookups for a given host, port and transport
	 * @param host the host
	 * @param port the port
	 * @param transport the transport
	 * @return an unsorted queue of Hops corresponding to the merge of A and AAAA lookup records found
	 */
	Queue<Hop> locateHopsForNonNumericAddressWithPort(
			String host, int port, String transport);

}
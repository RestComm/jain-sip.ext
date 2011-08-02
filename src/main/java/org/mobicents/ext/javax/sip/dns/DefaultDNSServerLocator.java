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

import gov.nist.javax.sip.address.AddressFactoryImpl;
import gov.nist.javax.sip.stack.HopImpl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sip.ListeningPoint;
import javax.sip.address.Hop;
import javax.sip.address.SipURI;
import javax.sip.address.TelURL;
import javax.sip.address.URI;

import org.apache.log4j.Logger;
import org.mobicents.ext.javax.sip.utils.Inet6Util;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;

/**
 * The Address resolver to resolve proxy domain to a hop to the outbound proxy server 
 * by doing SRV lookup of the host of the Hop as mandated by rfc3263. <br/>
 * 
 * some of the rfc3263 can hardly be implemented and NAPTR query can hardly be done 
 * since the stack populate port and transport automatically.
 * 
 * @author jean.deruelle@gmail.com
 *
 */
public class DefaultDNSServerLocator implements DNSServerLocator {
	private static final Logger logger = Logger.getLogger(DefaultDNSServerLocator.class);
	
	protected Set<String> supportedTransports;
	protected Set<String> localHostNames;
	private DNSLookupPerformer dnsLookupPerformer;
	
	public DefaultDNSServerLocator() {
		localHostNames = new CopyOnWriteArraySet<String>();
		dnsLookupPerformer = new DefaultDNSLookupPerformer();
		this.supportedTransports = new CopyOnWriteArraySet<String>();
	}
	
	/**
	 */
	public DefaultDNSServerLocator(Set<String> supportedTransports) {
		this();
		this.supportedTransports = new CopyOnWriteArraySet<String>(supportedTransports);
	}

	/* (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.dns.DNSServerLocator#locateHops(javax.sip.address.URI)
	 */
	public Queue<Hop> locateHops(URI uri) {
		SipURI sipUri = getSipURI(uri);
		if(sipUri != null) {
			return locateHopsForSipURI(sipUri);
		}
		
		return new LinkedList<Hop>();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.dns.DNSServerLocator#getSipURI(javax.sip.address.URI)
	 */
	public SipURI getSipURI(URI uri) {
		if(uri instanceof TelURL) {
			return lookupSipUri(((TelURL)uri).getPhoneNumber());
		} else if(uri.isSipURI() && ((SipURI)uri).getParameter("user") != null && ((SipURI)uri).getParameter("user").equalsIgnoreCase("phone")) {
			return lookupSipUri(((SipURI)uri).getUser());
		} else if (uri instanceof SipURI) {
			return (SipURI) uri;
		}
		return null;
	}
	
	/**
	 * The phone number is converted to a domain name
	 * then a corresponding NAPTR DNS lookup is done to find the SipURI
	 * @param phoneNumber phone number used to find the corresponding SipURI
	 * @return the SipURI found through ENUM for the given phone number
	 */
	public SipURI lookupSipUri(String phoneNumber) {
		
		String domainName = convertPhoneNumberToDomainName(phoneNumber);
		List<NAPTRRecord> naptrRecords = dnsLookupPerformer.performNAPTRLookup(domainName, false, supportedTransports);
		if(naptrRecords.size() > 0) {
			Collections.sort(naptrRecords, new NAPTRRecordComparator());
			for(NAPTRRecord naptrRecord : naptrRecords) {
				String regexp = naptrRecord.getRegexp().toString();
				if(logger.isDebugEnabled()) {
					logger.debug("regexp " + regexp + " found for phone number " + phoneNumber);
				}

				// http://code.google.com/p/mobicents/issues/detail?id=2774 : Fix For allowing REGEX 
				// Contribution from Oifa Yulian from Web Ukraine
				if(regexp.startsWith("!"))
					regexp=regexp.substring(1);
				
				if(regexp.endsWith("!"))
					regexp=regexp.substring(0,regexp.length()-1);
				
				String[] regexPortions=regexp.split("!");
				if(regexPortions.length==2) {
					if(regexPortions[1].startsWith("sip:")) {				
						String result = regexPortions[1];
						Pattern pattern = Pattern.compile(regexPortions[0]);
						Matcher regexMatcher = pattern.matcher(phoneNumber);
						if(regexMatcher.matches())
							for(int i=0; i<regexMatcher.groupCount(); i++) {
								String group = regexMatcher.group(i);
								if(logger.isDebugEnabled()) {
									logger.debug("group found " + group);
								}
								result = result.replace("\\\\" + (i+1), group);
							}
						
						try {
							return new AddressFactoryImpl().createSipURI(result);
						} 
						catch (ParseException e) {
							if(logger.isDebugEnabled()) {
								logger.debug("replacement " + result + " couldn't be parsed a valid sip uri : " + e.getMessage());
							}
						}
					} else {
						if(logger.isDebugEnabled()) {
							logger.debug("regexp seconf portion  " + regexPortions[1] + " does not start with sip:");
						}
					}
				} else {
					if(logger.isDebugEnabled()) {
						logger.debug("regexp " + regexp + " number of portions " + regexPortions.length);
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Convert Phone Number to a valid ENUM domain name to be used for NAPTR DNS lookup 
	 * @param phoneNumber phone number to convert
	 * @return the corresponding domain name
	 */
	private String convertPhoneNumberToDomainName(String phoneNumber) {
		char[] phoneNumberAsChar = phoneNumber.toCharArray();
		StringBuilder validPhoneNumber = new StringBuilder();
		for (char c : phoneNumberAsChar) {
			if(Character.isDigit(c)) {
				validPhoneNumber.append(c).append('.');
			}
		}
		return validPhoneNumber.reverse().append(".e164.arpa").substring(1);
	}

	public Queue<Hop> locateHopsForSipURI(SipURI sipURI) {		
		
		final String hopHost = sipURI.getHost();
		int hopPort = sipURI.getPort();
		final String hopTransport = sipURI.getTransportParam();	
		
		if(logger.isDebugEnabled()) {
			logger.debug("Resolving " + hopHost + " transport " + hopTransport);
		}
		// As per rfc3263 Section 4.2 
		// If TARGET is a numeric IP address, the client uses that address. 
		if(Inet6Util.isValidIP6Address(hopHost) 
				|| Inet6Util.isValidIPV4Address(hopHost)) {
			if(logger.isDebugEnabled()) {
				logger.debug("host " + hopHost + " is a numeric IP address, " +
						"no DNS SRV lookup to be done, using the hop given in param");
			}
			Queue<Hop> priorityQueue = new LinkedList<Hop>();
			String transport = hopTransport;
			if(transport == null) {
				transport = getDefaultTransportForSipUri(sipURI);
			}			 
			// If the URI also contains a port, it uses that port.  If no port is
			// specified, it uses the default port for the particular transport
			// protocol.numeric IP address, no DNS lookup to be done
			if(hopPort == -1) {		
				if(ListeningPoint.TLS.equalsIgnoreCase(transport) || (ListeningPoint.TCP.equalsIgnoreCase(transport) && sipURI.isSecure())) {
					hopPort = 5061;
				} else {
					hopPort = 5060;
				}
			}
			priorityQueue.add(new HopImpl(hopHost, hopPort, transport));
			return priorityQueue;
		} 
		
		// if the host belong to the local endpoint, server or container, it tries to resolve the ip address		
		if(localHostNames.contains(hopHost)) {
			if(logger.isDebugEnabled()) {
				logger.debug("host " + hopHost + " is a localhostName belonging to ourselves");
			}
			try {
				InetAddress ipAddress = InetAddress.getByName(hopHost);
				Queue<Hop> priorityQueue = new LinkedList<Hop>();
				priorityQueue.add(new HopImpl(ipAddress.getHostAddress(), hopPort, hopTransport));
				return priorityQueue;
			} catch (UnknownHostException e) {
				logger.warn(hopHost + " belonging to the container cannot be resolved");
			}			
		}
				
		// As per rfc3263 Section 4.2
		// If the TARGET was not a numeric IP address, and no port was present
		// in the URI, the client performs an SRV query
		return resolveHostByDnsSrvLookup(sipURI);
		
	}
	
	/**
	 * Resolve the Host by doing a SRV lookup on it 
	 * 
	 * @param sipUri
	 * @return 
	 */
	public Queue<Hop> resolveHostByDnsSrvLookup(SipURI sipURI) {		
		if(logger.isDebugEnabled()) {
			logger.debug("Resolving Hops for SipURI " + sipURI);
		}
		final String host = sipURI.getHost();
		final int port = sipURI.getPort();				
		String transport = sipURI.getTransportParam();
		
		NAPTRRecord naptrRecordOfTransportLookup = null;
		List<Record> srvRecordsOfTransportLookup = null;
		// Determine the transport to be used for a given SIP URI as defined by 
		// RFC 3263 Section 4.1 Selecting a Transport Protocol
		if(transport == null) {
			if(logger.isDebugEnabled()) {
				logger.debug("transport not specified, trying to resolve it for " + sipURI);
			}
			// Similarly, if no transport protocol is specified,
			// and the TARGET is not numeric, but an explicit port is provided, the
			// client SHOULD use UDP for a SIP URI, and TCP for a SIPS URI
			if(port != -1) {
				if(logger.isDebugEnabled()) {
					logger.debug("port not specified, trying to resolve it for " + sipURI);
				}
				transport = getDefaultTransportForSipUri(sipURI);
			} else {
				// Otherwise, if no transport protocol or port is specified, and the
				// target is not a numeric IP address, the client SHOULD perform a NAPTR
				// query for the domain in the URI.
				List<NAPTRRecord> naptrRecords = dnsLookupPerformer.performNAPTRLookup(host, sipURI.isSecure(), supportedTransports);
				
				if(naptrRecords == null || naptrRecords.size() == 0) {
					if(logger.isDebugEnabled()) {
						logger.debug("no NPATR records found, doing SRV queries for supported transports for " + sipURI);
					}
					// If no NAPTR records are found, the client constructs SRV queries for
					// those transport protocols it supports, and does a query for each.
					// Queries are done using the service identifier "_sip" for SIP URIs and
					// "_sips" for SIPS URIs
					Iterator<String> supportedTransportIterator = supportedTransports.iterator();
					while (supportedTransportIterator.hasNext() && transport == null) {
						 String supportedTransport = supportedTransportIterator
								.next().toLowerCase();
						String serviceIdentifier = "_sip._";
						if (sipURI.isSecure()) {
							serviceIdentifier = "_sips._";
						}
						if(logger.isDebugEnabled()) {
							logger.debug("no NPATR records found, doing SRV query for supported transport " + serviceIdentifier
									+ supportedTransport + "." + host + " for " + sipURI);
						}
						srvRecordsOfTransportLookup = dnsLookupPerformer.performSRVLookup(serviceIdentifier
								+ supportedTransport + "." + host);
						if (srvRecordsOfTransportLookup.size() > 0) {
							if(logger.isDebugEnabled()) {
								logger.debug("no NPATR records found, SRV query for supported transport " + serviceIdentifier
										+ supportedTransport + "." + host + " successful for " + sipURI);
							}
							// A particular transport is supported if the query is successful.  
							// The client MAY use any transport protocol it 
							// desires which is supported by the server => we use the first one
							transport = supportedTransport;
						}
					}
					if(transport == null) {
						if(logger.isDebugEnabled()) {
							logger.debug("no SRV records found for finding transport for " + sipURI);
						}
						// If no SRV records are found, the client SHOULD use TCP for a SIPS
						// URI, and UDP for a SIP URI
						transport = getDefaultTransportForSipUri(sipURI);
					}
				} else {					
					// Sorting the records
					java.util.Collections.sort(naptrRecords, new NAPTRRecordComparator());
					
					naptrRecordOfTransportLookup = naptrRecords.get(0);
					if(logger.isDebugEnabled()) {
						logger.debug("naptr records found for finding transport for " + sipURI);
					}
					String service = naptrRecordOfTransportLookup.getService();
					if(service.contains(DNSLookupPerformer.SERVICE_SIPS)) {
						transport = ListeningPoint.TLS;
					} else {
						if(service.contains(DNSLookupPerformer.SERVICE_D2U)) {
							transport = ListeningPoint.UDP;
						} else {
							transport = ListeningPoint.TCP;
						}
					}
				}
			}
		}
		transport = transport.toLowerCase();
		if(logger.isDebugEnabled()) {
			logger.debug("using transport "+ transport + " for " + sipURI);
		}
		// RFC 3263 Section 4.2
		// Once the transport protocol has been determined, the next step is to
		// determine the IP address and port.

		if(port != -1) {
			if(logger.isDebugEnabled()) {
				logger.debug("doing A and AAAA lookups since TARGET is not numeric and port is not null for " + sipURI);
			}
			// If the TARGET was not a numeric IP address, but a port is present in
			// the URI, the client performs an A or AAAA record lookup of the domain
			// name.  The result will be a list of IP addresses, each of which can
			// be contacted at the specific port from the URI and transport protocol
			// determined previously.  The client SHOULD try the first record.  If
			// an attempt should fail, based on the definition of failure in Section
			// 4.3, the next SHOULD be tried, and if that should fail, the next
			// SHOULD be tried, and so on.
			return dnsLookupPerformer.locateHopsForNonNumericAddressWithPort(host, port, transport);
		} else {						
			if(naptrRecordOfTransportLookup != null) {
				// If the TARGET was not a numeric IP address, and no port was present
				// in the URI, the client performs an SRV query on the record returned
				// from the NAPTR processing of Section 4.1, if such processing was
				// performed.
				if(logger.isDebugEnabled()) {
					logger.debug("performing SRV lookup on NAPTR replacement found earlier " + naptrRecordOfTransportLookup.getReplacement() + " for " + sipURI);
				}
				List<Record> srvRecords = dnsLookupPerformer.performSRVLookup(naptrRecordOfTransportLookup.getReplacement().toString());
				if (srvRecords.size() > 0) {
					return sortSRVRecords(host, transport, srvRecords);
				} else {
					if(logger.isDebugEnabled()) {
						logger.debug("doing A and AAAA lookups since SRV lookups returned no records for NAPTR replacement found earlier " + naptrRecordOfTransportLookup.getReplacement() + " for " + sipURI);
					}
					// If no SRV records were found, the client performs an A or AAAA record
					// lookup of the domain name.
					return dnsLookupPerformer.locateHopsForNonNumericAddressWithPort(host, port, transport);
				}
			} else if(srvRecordsOfTransportLookup == null || srvRecordsOfTransportLookup.size() == 0){
				// If it was not, because a transport was specified
				// explicitly, the client performs an SRV query for that specific
				// transport, using the service identifier "_sips" for SIPS URIs.  For a
				// SIP URI, if the client wishes to use TLS, it also uses the service
				// identifier "_sips" for that specific transport, otherwise, it uses "_sip"
				String serviceIdentifier = "_sip._";
				if ((sipURI.isSecure() && !transport.equalsIgnoreCase(ListeningPoint.UDP)) || transport.equalsIgnoreCase(ListeningPoint.TLS)) {
					serviceIdentifier = "_sips._";
				}
				if(logger.isDebugEnabled()) {
					logger.debug("performing SRV lookup because a transport was specified explicitly for " + sipURI);
				}
				List<Record> srvRecords = dnsLookupPerformer.performSRVLookup(serviceIdentifier + transport + "." + host);
				if (srvRecords == null || srvRecords.size() == 0) {
					if(logger.isDebugEnabled()) {
						logger.debug("doing A and AAAA lookups since SRV lookups returned no records and transport was specified explicitly for " + sipURI);
					}
					// If no SRV records were found, the client performs an A or AAAA record
					// lookup of the domain name.
					return dnsLookupPerformer.locateHopsForNonNumericAddressWithPort(host, port, transport);
				} else {
					return sortSRVRecords(host, transport, srvRecords);
				}
			} else {
				// If the NAPTR processing was not done because no NAPTR
				// records were found, but an SRV query for a supported transport
				// protocol was successful, those SRV records are selected
				return sortSRVRecords(host, transport, srvRecordsOfTransportLookup);
			}
		}			
	}

	/**
	 * @param host
	 * @param transport
	 * @param priorityQueue
	 * @param srvRecords
	 * @return
	 */
	private Queue<Hop> sortSRVRecords(final String host, String transport, List<Record> srvRecords) {
		Queue<Hop> priorityQueue = new LinkedList<Hop>();
		Collections.sort(srvRecords, new SRVRecordComparator());
		
		for (Record record : srvRecords) {
			SRVRecord srvRecord = (SRVRecord) record;
			int recordPort = srvRecord.getPort();						
			String resolvedName = srvRecord.getTarget().toString();
			try {
				String hostAddress= InetAddress.getByName(resolvedName).getHostAddress();
				if(logger.isDebugEnabled()) {
					logger.debug("Did a successful DNS SRV lookup for host:transport " +
							""+ host + "/" + transport +
							" , Host Name = " + resolvedName +
							" , Host IP Address = " + hostAddress + 
							", Host Port = " + recordPort);
				}				
				priorityQueue.add(new HopImpl(hostAddress, recordPort, transport));
			} catch (UnknownHostException e) {
				logger.error("Impossible to get the host address of the resolved name, " +
						"we are going to just use the domain name directly" + resolvedName, e);
			}
		}		
		
		return priorityQueue;
	}

	/**
	 * @param sipURI
	 * @return
	 */
	public String getDefaultTransportForSipUri(SipURI sipURI) {
		String transport;
		if(sipURI.isSecure()) {
			transport = ListeningPoint.TLS;
		} else {
			transport = ListeningPoint.UDP;
		}
		return transport;
	}
	
	
	/* (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.dns.DNSServerLocator#addLocalHostName(java.lang.String)
	 */
	public void addLocalHostName(String localHostName) {
		if(logger.isDebugEnabled()) {
			logger.debug("Adding localHostName "+ localHostName);
		}
		localHostNames.add(localHostName);
	}
	
	/* (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.dns.DNSServerLocator#removeLocalHostName(java.lang.String)
	 */
	public void removeLocalHostName(String localHostName) {
		if(logger.isDebugEnabled()) {
			logger.debug("Removing localHostName "+ localHostName);
		}
		localHostNames.remove(localHostName);
	}
	
	/* (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.dns.DNSServerLocator#addSupportedTransport(java.lang.String)
	 */
	public void addSupportedTransport(String supportedTransport) {
		if(logger.isDebugEnabled()) {
			logger.debug("Adding supportedTransport "+ supportedTransport);
		}
		supportedTransports.add(supportedTransport);
	}
	
	/* (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.dns.DNSServerLocator#removeSupportedTransport(java.lang.String)
	 */
	public void removeSupportedTransport(String supportedTransport) {
		if(logger.isDebugEnabled()) {
			logger.debug("Removing supportedTransport "+ supportedTransport);
		}
		supportedTransports.add(supportedTransport);
	}

	/* (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.dns.DNSServerLocator#setDnsLookupPerformer(org.mobicents.ext.javax.sip.dns.DefaultDNSLookupPerformer)
	 */
	public void setDnsLookupPerformer(DNSLookupPerformer dnsLookupPerformer) {
		this.dnsLookupPerformer = dnsLookupPerformer;
	}

	/* (non-Javadoc)
	 * @see org.mobicents.ext.javax.sip.dns.DNSServerLocator#getDnsLookupPerformer()
	 */
	public DNSLookupPerformer getDnsLookupPerformer() {
		return dnsLookupPerformer;
	}
}

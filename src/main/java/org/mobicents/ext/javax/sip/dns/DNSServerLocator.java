package org.mobicents.ext.javax.sip.dns;

import gov.nist.javax.sip.stack.HopImpl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.sip.ListeningPoint;
import javax.sip.address.Hop;
import javax.sip.address.SipURI;
import javax.sip.address.TelURL;
import javax.sip.address.URI;

import org.apache.log4j.Logger;
import org.mobicents.ext.javax.sip.utils.Inet6Util;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;

/**
 * The Address resolver to resolve proxy domain to a hop to the outbound proxy server 
 * by doing SRV lookup of the host of the Hop as mandated by rfc3263. <br/>
 * 
 * some of the rfc3263 can hardly be implemented and NAPTR query can hardly be done 
 * since the stack populate port and transport automatically.
 * 
 * @author M. Ranganathan
 * @author J. Deruelle
 *
 */
public class DNSServerLocator {
	private static final Logger logger = Logger.getLogger(DNSServerLocator.class);
	
	protected Set<String> supportedTransports;
	protected Set<String> localHostNames;
	private DNSLookupPerformer dnsLookupPerformer;
	
	/**
	 */
	public DNSServerLocator(Set<String> supportedTransports) {
		this.supportedTransports = new CopyOnWriteArraySet<String>(supportedTransports);
		localHostNames = new CopyOnWriteArraySet<String>();
		dnsLookupPerformer = new DNSLookupPerformer();
	}

	/**
	 * 
	 * @param uri
	 * @return
	 */
	public Queue<Hop> locateHops(URI uri) {
		if(uri.isSipURI()) {
			return locateHopsForSipURI((SipURI)uri);
		} else if(uri instanceof TelURL) {
			return locateHopsForTelURI((TelURL)uri);
		}
		return new LinkedList<Hop>();
	}
	
	public Queue<Hop> locateHopsForTelURI(TelURL telURL) {
		Queue<Hop> priorityQueue = new LinkedList<Hop>();
		return priorityQueue;
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
				if(ListeningPoint.TLS.equals(transport) || (ListeningPoint.TCP.equals(transport) && sipURI.isSecure())) {
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
								.next();
						String serviceIdentifier = "_sip._";
						if (sipURI.isSecure()) {
							serviceIdentifier = "_sips._";
						}
						if(logger.isDebugEnabled()) {
							logger.debug("no NPATR records found, doing SRV query for supported transport " + serviceIdentifier
									+ supportedTransport.toLowerCase() + "." + host + " for " + sipURI);
						}
						try {
							srvRecordsOfTransportLookup = dnsLookupPerformer.performSRVLookup(new Name(serviceIdentifier
									+ supportedTransport.toLowerCase() + "." + host));
						} catch (TextParseException e) {
							logger.error("Impossible to parse the parameters for dns lookup",e);
						}						
						if (srvRecordsOfTransportLookup.size() > 0) {
							if(logger.isDebugEnabled()) {
								logger.debug("no NPATR records found, SRV query for supported transport " + serviceIdentifier
										+ supportedTransport.toLowerCase() + "." + host + " successful for " + sipURI);
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
				List<Record> srvRecords = dnsLookupPerformer.performSRVLookup(naptrRecordOfTransportLookup.getReplacement());
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
				List<Record> srvRecords = null;
				try {
					srvRecords = dnsLookupPerformer.performSRVLookup(new Name(serviceIdentifier + transport + "." + host));
				} catch (TextParseException e) {
					logger.error("Impossible to parse the parameters for dns lookup",e);
				}	
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
			transport = ListeningPoint.TCP;
		} else {
			transport = ListeningPoint.UDP;
		}
		return transport;
	}
	
	
	public void addLocalHostName(String localHostName) {
		localHostNames.add(localHostName);
	}
	
	public void removeLocalHostName(String localHostName) {
		localHostNames.remove(localHostName);
	}
	
	public void addSupportedTransport(String supportedTransport) {
		supportedTransports.add(supportedTransport);
	}
	
	public void removeSupportedTransport(String supportedTransport) {
		supportedTransports.add(supportedTransport);
	}

	/**
	 * @param dnsLookupPerformer the dnsLookupPerformer to set
	 */
	public void setDnsLookupPerformer(DNSLookupPerformer dnsLookupPerformer) {
		this.dnsLookupPerformer = dnsLookupPerformer;
	}

	/**
	 * @return the dnsLookupPerformer
	 */
	public DNSLookupPerformer getDnsLookupPerformer() {
		return dnsLookupPerformer;
	}
}
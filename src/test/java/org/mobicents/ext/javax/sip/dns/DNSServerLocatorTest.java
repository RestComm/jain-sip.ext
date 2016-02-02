/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.ext.javax.sip.dns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import gov.nist.javax.sip.address.AddressFactoryImpl;
import gov.nist.javax.sip.stack.HopImpl;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.sip.ListeningPoint;
import javax.sip.address.AddressFactory;
import javax.sip.address.Hop;
import javax.sip.address.SipURI;
import javax.sip.address.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;

/**
 * @author jean.deruelle@gmail.com
 *
 */
public class DNSServerLocatorTest {
	AddressFactory addressFactory;
	DefaultDNSServerLocator dnsServerLocator;
	Set<String> supportedTransports;
	SipURI sipURI;
	String host = "telestax.com";
	public static final String LOCALHOST = "127.0.0.1";	
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		addressFactory = new AddressFactoryImpl();
		supportedTransports = new HashSet<String>();
		supportedTransports.add(ListeningPoint.UDP);
		supportedTransports.add(ListeningPoint.TCP);
		dnsServerLocator = new DefaultDNSServerLocator(supportedTransports);
		sipURI = addressFactory.createSipURI("jean",host);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link org.mobicents.ext.javax.sip.dns.DefaultDNSServerLocator#resolveHostByDnsSrvLookup(javax.sip.address.SipURI)}.
	 * @throws ParseException 
	 */
	@Test
	public void testGetDefaultTransportForSipUri() throws ParseException {
		sipURI.setPort(5080);
		assertEquals(ListeningPoint.UDP, dnsServerLocator.getDefaultTransportForSipUri(sipURI));
		sipURI.setSecure(true);
		assertEquals(ListeningPoint.TLS, dnsServerLocator.getDefaultTransportForSipUri(sipURI));
	}
	
	/**
	 * Non Regression test for https://code.google.com/p/jain-sip/issues/detail?id=162
	 * Test method for {@link org.mobicents.ext.javax.sip.dns.DefaultDNSServerLocator#resolveHostByAandAAAALookup(String, int, String)}.
	 */
	@Test
	public void testResolveHostByAandAAAALookupwithLocalHostNameMapping() {
		Set<String> ipAddress = new CopyOnWriteArraySet<String>();
		ipAddress.add("127.0.0.1");
		dnsServerLocator.mapLocalHostNameToIP("test.mobicents.org", ipAddress);
		Queue<Hop> hops = dnsServerLocator.resolveHostByAandAAAALookup("test.mobicents.org", -1, ListeningPoint.UDP);
		assertNotNull(hops);
		assertEquals(1, hops.size());
		assertEquals("127.0.0.1", hops.peek().getHost());
	}
	
	/**
	 * Test method for {@link org.mobicents.ext.javax.sip.dns.DefaultDNSServerLocator#locateHops(javax.sip.address.URI)}.
	 * @throws ParseException 
	 */
	@Test
	public void testRealExample() throws ParseException {
		Queue<Hop> hops = dnsServerLocator.locateHops(sipURI);
		assertNotNull(hops);
		assertTrue(hops.size() > 0);
	}
	
	@Test
	// https://code.google.com/p/sipservlets/issues/detail?id=236
    public void testResolveBadPhoneNumber() throws ParseException {
        String transport = ListeningPoint.UDP;
        int port = 5080;
        
        DNSLookupPerformer dnsLookupPerformer = mock(DefaultDNSLookupPerformer.class);
        dnsServerLocator.setDnsLookupPerformer(dnsLookupPerformer);
        LinkedList<Hop> mockedHops = new LinkedList<Hop>();
        mockedHops.add(new HopImpl(LOCALHOST, port, transport));
        when(dnsLookupPerformer.locateHopsForNonNumericAddressWithPort(host, port, transport.toLowerCase())).thenReturn(mockedHops);
        
        sipURI = addressFactory.createSipURI(null,host);
        sipURI.setParameter("user", "phone");
        try {
            SipURI result = dnsServerLocator.getSipURI(sipURI);
            fail("IllegalArgumentException should be thrown is there is not user part for an uri with a phone parameter");
        } catch (IllegalArgumentException e) {
            
        }        
    }
	
	@Test
	public void testResolveHostByAandAAAALookup() throws ParseException {
		String transport = ListeningPoint.UDP;
		int port = 5080;
		
		DNSLookupPerformer dnsLookupPerformer = mock(DefaultDNSLookupPerformer.class);
		dnsServerLocator.setDnsLookupPerformer(dnsLookupPerformer);
		LinkedList<Hop> mockedHops = new LinkedList<Hop>();
		mockedHops.add(new HopImpl(LOCALHOST, port, transport));
		when(dnsLookupPerformer.locateHopsForNonNumericAddressWithPort(host, port, transport.toLowerCase())).thenReturn(mockedHops);
		
		sipURI.setTransportParam(transport);
		sipURI.setPort(port);
		Queue<Hop> hops = dnsServerLocator.resolveHostByDnsSrvLookup(sipURI);
		assertNotNull(hops);
		assertEquals(1, hops.size());
		Hop hop = hops.poll();
		assertEquals(port, hop.getPort());
		assertEquals(transport, hop.getTransport());
		assertEquals(LOCALHOST, hop.getHost());
	}
	
	@Test
	public void testResolveHostByAandAAAALookupTimeout() throws ParseException {
		String transport = ListeningPoint.UDP;
		int port = 5080;
		
//		DNSLookupPerformer dnsLookupPerformer = mock(DefaultDNSLookupPerformer.class);
//		dnsServerLocator.setDnsLookupPerformer(dnsLookupPerformer);
//		LinkedList<Hop> mockedHops = new LinkedList<Hop>();
//		mockedHops.add(new HopImpl(LOCALHOST, port, transport));
//		when(dnsLookupPerformer.locateHopsForNonNumericAddressWithPort(host, port, transport.toLowerCase())).thenReturn(mockedHops);
		dnsServerLocator.getDnsLookupPerformer().setDNSTimeout(1);
		Queue<Hop> hops = dnsServerLocator.getDnsLookupPerformer().locateHopsForNonNumericAddressWithPort("dhsskfdkldjsmdjeoife.org", port, transport);
		assertNotNull(hops);
		assertEquals(0, hops.size());
	}
	
	@Test
	public void testResolveHostByAandAAAALookupCheckEmpty() throws ParseException {
		String transport = ListeningPoint.UDP;
		int port = 5080;
		
		DNSLookupPerformer dnsLookupPerformer = mock(DefaultDNSLookupPerformer.class);
		dnsServerLocator.setDnsLookupPerformer(dnsLookupPerformer);
		LinkedList<Hop> mockedHops = new LinkedList<Hop>();
		when(dnsLookupPerformer.locateHopsForNonNumericAddressWithPort(host, port, transport.toLowerCase())).thenReturn(mockedHops);
		
		sipURI.setTransportParam(transport);
		sipURI.setPort(port);
		Queue<Hop> hops = dnsServerLocator.resolveHostByDnsSrvLookup(sipURI);
		assertNotNull(hops);
		assertEquals(0, hops.size());
	}
	
	@Test
	public void testResolveHostNoPortButTransportSpecified() throws ParseException, TextParseException {
		String transport = ListeningPoint.UDP;
		
		DNSLookupPerformer dnsLookupPerformer = mock(DefaultDNSLookupPerformer.class);
		dnsServerLocator.setDnsLookupPerformer(dnsLookupPerformer);
		LinkedList<Record> mockedHops = new LinkedList<Record>();
		// mocking the name because localhost is not absolute and localhost. cannot be resolved 
		Name name = mock(Name.class);
		when(name.isAbsolute()).thenReturn(true);
		when(name.toString()).thenReturn("telestax.com");
		mockedHops.add(new SRVRecord(new Name("_sip._" + transport.toLowerCase() + "." + host + "."), DClass.IN, 1000L, 0, 0, 5060, name));
		when(dnsLookupPerformer.performSRVLookup("_sip._" + transport.toLowerCase() + "." + host)).thenReturn(mockedHops);
		
		Queue<Hop> mockedARecords = new LinkedList<Hop>();
		mockedARecords.add(new HopImpl(LOCALHOST, 5060, transport.toLowerCase()));
		when(dnsLookupPerformer.locateHopsForNonNumericAddressWithPort("telestax.com", 5060, transport.toLowerCase())).thenReturn(mockedARecords);
		
		sipURI.setTransportParam(transport);
		Queue<Hop> hops = dnsServerLocator.resolveHostByDnsSrvLookup(sipURI);
		assertNotNull(hops);
		assertTrue(hops.size() > 0);
		Hop hop = hops.poll();
		assertEquals(5060, hop.getPort());
		assertEquals(transport.toLowerCase(), hop.getTransport());
		assertEquals(LOCALHOST, hop.getHost());
	}
	
	@Test
	public void testResolveHostNoPortButTransportSpecifiedNoSRVFound() throws ParseException, TextParseException {
		String transport = ListeningPoint.UDP;
		sipURI.setHost("localhost");
		
		DNSLookupPerformer dnsLookupPerformer = mock(DefaultDNSLookupPerformer.class);
		dnsServerLocator.setDnsLookupPerformer(dnsLookupPerformer);
		LinkedList<Hop> mockedHops = new LinkedList<Hop>();
		mockedHops.add(new HopImpl(LOCALHOST, 5060, transport));
		when(dnsLookupPerformer.locateHopsForNonNumericAddressWithPort("localhost", -1, transport.toLowerCase())).thenReturn(mockedHops);
		
		sipURI.setTransportParam(transport);
		Queue<Hop> hops = dnsServerLocator.resolveHostByDnsSrvLookup(sipURI);
		assertNotNull(hops);
		assertTrue(hops.size() > 0);
		Hop hop = hops.poll();
		assertEquals(5060, hop.getPort());
		assertEquals(transport, hop.getTransport());
		assertEquals(LOCALHOST, hop.getHost());
	}
	
	@Test
	public void testResolveHostNoPortNoTransportSpecifiedNAPTRAndSRVFound() throws ParseException, TextParseException, UnknownHostException {
		String transport = ListeningPoint.UDP;
		
		DNSLookupPerformer dnsLookupPerformer = mock(DefaultDNSLookupPerformer.class);
		dnsServerLocator.setDnsLookupPerformer(dnsLookupPerformer);
		
		List<NAPTRRecord> mockedNAPTRRecords = new LinkedList<NAPTRRecord>();
		// mocking the name because localhost is not absolute and localhost. cannot be resolved 
		Name name = mock(Name.class);
		when(name.isAbsolute()).thenReturn(true);
		when(name.toString()).thenReturn("telestax.com");
		mockedNAPTRRecords.add(new NAPTRRecord(new Name(host + "."), DClass.IN, 1000, 0, 0, "s", "SIP+D2U", "", new Name("_sip._" + transport.toLowerCase() + "." + host + ".")));		
		when(dnsLookupPerformer.performNAPTRLookup(host, false, supportedTransports)).thenReturn(mockedNAPTRRecords);
		List<Record> mockedSRVRecords = new LinkedList<Record>();
		mockedSRVRecords.add(new SRVRecord(new Name("_sip._" + transport.toLowerCase() + "." + host + "."), DClass.IN, 1000L, 0, 0, 5060, name));
		when(dnsLookupPerformer.performSRVLookup("_sip._" + transport.toLowerCase() + "." + host + ".")).thenReturn(mockedSRVRecords);
		
		Queue<Hop> mockedARecords = new LinkedList<Hop>();
		mockedARecords.add(new HopImpl(LOCALHOST, 5060, transport.toLowerCase()));
		when(dnsLookupPerformer.locateHopsForNonNumericAddressWithPort("telestax.com", 5060, transport.toLowerCase())).thenReturn(mockedARecords);
		
		Queue<Hop> hops = dnsServerLocator.resolveHostByDnsSrvLookup(sipURI);
		assertNotNull(hops);
		assertTrue(hops.size() > 0);
		Hop hop = hops.poll();
		assertEquals(5060, hop.getPort());
		assertEquals(transport.toLowerCase(), hop.getTransport());
		assertEquals(LOCALHOST, hop.getHost());
	}
	
	@Test
	public void testResolveENUM() throws ParseException, TextParseException {
		DNSLookupPerformer dnsLookupPerformer = mock(DefaultDNSLookupPerformer.class);
		dnsServerLocator.setDnsLookupPerformer(dnsLookupPerformer);
		
		List<NAPTRRecord> mockedNAPTRRecords = new LinkedList<NAPTRRecord>();
		// mocking the name because localhost is not absolute and localhost. cannot be resolved 
		Name name = mock(Name.class);
		when(name.isAbsolute()).thenReturn(true);
		when(name.toString()).thenReturn("!^.*$!sip:jean@localhost!.");
		mockedNAPTRRecords.add(new NAPTRRecord(new Name("7.6.5.4.3.2.1.5.5.5.8.5.3.e164.arpa" + "."), DClass.IN, 1000, 0, 0, "s", "E2U+sip", "!^.*$!sip:jean@localhost!", name));		
		when(dnsLookupPerformer.performNAPTRLookup("7.6.5.4.3.2.1.5.5.5.8.5.3.e164.arpa", false, supportedTransports)).thenReturn(mockedNAPTRRecords);
		
		URI telURI = addressFactory.createTelURL("+358-555-1234567");
		SipURI resolvedSipURI = dnsServerLocator.getSipURI(telURI);
		assertNotNull(resolvedSipURI);
		assertEquals("sip:jean@localhost", resolvedSipURI.toString());
	}
	
	/*
	 * Non regression test for testing regex pattern in NAPTR for ENMU See http://www.ietf.org/mail-archive/web/enum/current/msg05060.html 
	 * and http://www.ietf.org/mail-archive/web/enum/current/msg05059.html
	 */
	@Test	
	public void testResolveENUMRegex() throws ParseException, TextParseException {
		DNSLookupPerformer dnsLookupPerformer = mock(DefaultDNSLookupPerformer.class);
		dnsServerLocator.setDnsLookupPerformer(dnsLookupPerformer);
		
		List<NAPTRRecord> mockedNAPTRRecords = new LinkedList<NAPTRRecord>();
		Name name = mock(Name.class);
		when(name.isAbsolute()).thenReturn(true);
		mockedNAPTRRecords.add(new NAPTRRecord(new Name("*.6.1.4.6.5.0.5.1.3.4.e164.arpa" + "."), DClass.IN, 1000, 0, 0, "u", "E2U+sip", "!^4315056416(.*)$!sip:\\\\1@enum.at!", name));		
		when(dnsLookupPerformer.performNAPTRLookup("3.1.6.1.4.6.5.0.5.1.3.4.e164.arpa", false, supportedTransports)).thenReturn(mockedNAPTRRecords);
		
		URI telURI = addressFactory.createTelURL("+431505641613");
		SipURI resolvedSipURI = dnsServerLocator.getSipURI(telURI);
		assertNotNull(resolvedSipURI);
		assertEquals("sip:431505641613@enum.at", resolvedSipURI.toString());
		
		mockedNAPTRRecords.clear();
		mockedNAPTRRecords.add(new NAPTRRecord(new Name("*.6.1.4.6.5.0.5.1.3.4.e164.arpa" + "."), DClass.IN, 1000, 0, 0, "u", "E2U+sip", "!^(4315056416)((.*))$!sip:\\\\2-extension-\\\\3@enum.at!", name));		
		when(dnsLookupPerformer.performNAPTRLookup("3.1.6.1.4.6.5.0.5.1.3.4.e164.arpa", false, supportedTransports)).thenReturn(mockedNAPTRRecords);
		
		telURI = addressFactory.createTelURL("+431505641613");
		resolvedSipURI = dnsServerLocator.getSipURI(telURI);
		assertNotNull(resolvedSipURI);
		assertEquals("sip:4315056416-extension-13@enum.at", resolvedSipURI.toString());
		
		mockedNAPTRRecords.clear();
		mockedNAPTRRecords.add(new NAPTRRecord(new Name("7.1.6.8.0.2.3.5.1.2.1.e164.arpa" + "."), DClass.IN, 1000, 0, 0, "u", "E2U+sip", "!^(.*)$!sip:\\\\1@example.net!", name));		
		when(dnsLookupPerformer.performNAPTRLookup("7.1.6.8.0.2.3.5.1.2.1.e164.arpa", false, supportedTransports)).thenReturn(mockedNAPTRRecords);
		
		telURI = addressFactory.createTelURL("+12153208617");
		resolvedSipURI = dnsServerLocator.getSipURI(telURI);
		assertNotNull(resolvedSipURI);
		assertEquals("sip:12153208617@example.net", resolvedSipURI.toString());
	}
	
	@Test
	public void testResolveENUMReal() throws ParseException, TextParseException {
		
		URI telURI = addressFactory.createTelURL("+437800047111");
		SipURI resolvedSipURI = dnsServerLocator.getSipURI(telURI);
		assertNotNull(resolvedSipURI);
		assertEquals("sip:enum-echo-test@sip.nemox.net", resolvedSipURI.toString());
	}

	@Test
	public void testResolveHostNoPortNoTransportSpecifiedNoNAPTRFound() throws ParseException, TextParseException {
		String transport = ListeningPoint.UDP;
		
		DNSLookupPerformer dnsLookupPerformer = mock(DefaultDNSLookupPerformer.class);
		dnsServerLocator.setDnsLookupPerformer(dnsLookupPerformer);
		
		// mocking the name because localhost is not absolute and localhost. cannot be resolved 
		Name name = mock(Name.class);
		when(name.isAbsolute()).thenReturn(true);
		when(name.toString()).thenReturn("telestax.com");
		List<Record> mockedSRVRecords = new LinkedList<Record>();
		mockedSRVRecords.add(new SRVRecord(new Name("_sip._" + transport.toLowerCase() + "." + host + "."), DClass.IN, 1000L, 0, 0, 5060, name));
		List<Record> mockedSRVRecordsTCP = new LinkedList<Record>();
		mockedSRVRecordsTCP.add(new SRVRecord(new Name("_sip._" + "tcp" + "." + host + "."), DClass.IN, 1000L, 0, 0, 5060, name));
		when(dnsLookupPerformer.performSRVLookup("_sip._" + transport.toLowerCase() + "." + host)).thenReturn(mockedSRVRecords);
		when(dnsLookupPerformer.performSRVLookup("_sip._" + "tcp" + "." + host)).thenReturn(mockedSRVRecordsTCP);
		
		Queue<Hop> mockedARecords = new LinkedList<Hop>();
		mockedARecords.add(new HopImpl(LOCALHOST, 5060, transport.toLowerCase()));
		when(dnsLookupPerformer.locateHopsForNonNumericAddressWithPort("telestax.com", 5060, transport.toLowerCase())).thenReturn(mockedARecords);
		
		Queue<Hop> hops = dnsServerLocator.resolveHostByDnsSrvLookup(sipURI);
		assertNotNull(hops);
		assertTrue(hops.size() > 0);
		Hop hop = hops.poll();
		assertEquals(5060, hop.getPort());
		assertEquals(transport.toLowerCase(), hop.getTransport());
		assertEquals(LOCALHOST, hop.getHost());
	}
	
	@Test
	public void testNAPTRComparator() throws TextParseException {
		List<NAPTRRecord> mockedNAPTRRecords = new LinkedList<NAPTRRecord>();
		// mocking the name because localhost is not absolute and localhost. cannot be resolved 
		Name name = mock(Name.class);
		when(name.isAbsolute()).thenReturn(true);
		when(name.toString()).thenReturn("localhost");
			
		mockedNAPTRRecords.add(new NAPTRRecord(new Name(host + "."), DClass.IN, 1000L, 90, 50, "s", "SIP+D2T", "", new Name("_sip._" + ListeningPoint.TCP.toLowerCase() + "." + host + ".")));	
		mockedNAPTRRecords.add(new NAPTRRecord(new Name(host + "."), DClass.IN, 1000L, 100, 50, "s", "SIP+D2U", "", new Name("_sip._" + ListeningPoint.UDP.toLowerCase() + "." + host + ".")));
		mockedNAPTRRecords.add(new NAPTRRecord(new Name(host + "."), DClass.IN, 1000L, 50, 50, "s", "SIPS+D2T", "", new Name("_sips._" + ListeningPoint.TLS.toLowerCase() + "." + host + ".")));
		
		// Sorting the records
		java.util.Collections.sort(mockedNAPTRRecords, new NAPTRRecordComparator());
		
		assertEquals("SIPS+D2T", mockedNAPTRRecords.get(0).getService());
		assertEquals("SIP+D2T", mockedNAPTRRecords.get(1).getService());
		assertEquals("SIP+D2U", mockedNAPTRRecords.get(2).getService());
	}
	
	@Test
	//Issue http://code.google.com/p/restcomm/issues/detail?id=3143
	public void testNAPTRPrefComparator() throws TextParseException {
		List<NAPTRRecord> mockedNAPTRRecords = new LinkedList<NAPTRRecord>();
		// mocking the name because localhost is not absolute and localhost. cannot be resolved 
		Name name = mock(Name.class);
		when(name.isAbsolute()).thenReturn(true);
		when(name.toString()).thenReturn("localhost");
			
		mockedNAPTRRecords.add(new NAPTRRecord(new Name(host + "."), DClass.IN, 1000L, 90, 50, "s", "SIP+D2T", "", new Name("_sip._" + ListeningPoint.TCP.toLowerCase() + "." + host + ".")));	
		mockedNAPTRRecords.add(new NAPTRRecord(new Name(host + "."), DClass.IN, 1000L, 90, 40, "s", "SIP+D2U", "", new Name("_sip._" + ListeningPoint.UDP.toLowerCase() + "." + host + ".")));
		
		// Sorting the records
		java.util.Collections.sort(mockedNAPTRRecords, new NAPTRRecordComparator());
				
		assertEquals("SIP+D2U", mockedNAPTRRecords.get(0).getService());
		assertEquals("SIP+D2T", mockedNAPTRRecords.get(1).getService());
	}
	
	@Test
	public void testSRVComparator() throws TextParseException {
		// mocking the name because localhost is not absolute and localhost. cannot be resolved 			
		List<SRVRecord> mockedSRVRecords = new LinkedList<SRVRecord>();
		mockedSRVRecords.add(new SRVRecord(new Name("_sip._" + ListeningPoint.TCP.toLowerCase() + "." + host + "."), DClass.IN, 1000L, 1, 9, 5060, new Name("old-slow-box.example.com.")));	
		mockedSRVRecords.add(new SRVRecord(new Name("_sip._" + ListeningPoint.TCP.toLowerCase() + "." + host + "."), DClass.IN, 1000L, 1, 10, 5060, new Name("old2-slow-box.example.com.")));
		mockedSRVRecords.add(new SRVRecord(new Name("_sip._" + ListeningPoint.TCP.toLowerCase() + "." + host + "."), DClass.IN, 1000L, 3, 9, 5060, new Name("new-fast2-box.example.com.")));
		mockedSRVRecords.add(new SRVRecord(new Name("_sip._" + ListeningPoint.TCP.toLowerCase() + "." + host + "."), DClass.IN, 1000L, 3, 9, 5060, new Name( "new-fast-box.example.com.")));		
				
		// Sorting the records
		java.util.Collections.sort(mockedSRVRecords, new SRVRecordComparator());
		
		assertEquals("old2-slow-box.example.com.", mockedSRVRecords.get(0).getTarget().toString());
		assertEquals("old-slow-box.example.com.", mockedSRVRecords.get(1).getTarget().toString());		
		assertEquals("new-fast-box.example.com.", mockedSRVRecords.get(2).getTarget().toString());
		assertEquals("new-fast2-box.example.com.", mockedSRVRecords.get(3).getTarget().toString());		
	}
}

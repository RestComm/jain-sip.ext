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

import static org.junit.Assert.assertEquals;
import gov.nist.javax.sip.address.AddressFactoryImpl;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import javax.sip.ListeningPoint;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jean.deruelle@gmail.com
 *
 */
public class DNSServerLocatorTest {
	AddressFactory addressFactory;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		addressFactory = new AddressFactoryImpl();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link org.mobicents.ext.javax.sip.dns.DNSServerLocator#resolveHostByDnsSrvLookup(javax.sip.address.SipURI)}.
	 * @throws ParseException 
	 */
	@Test
	public void testGetDefaultTransportForSipUri() throws ParseException {
		DNSServerLocator dnsServerLocator = new DNSServerLocator(new HashSet<String>());
		
		SipURI sipURI = addressFactory.createSipURI("jean","iptel.org:5080");
		assertEquals(ListeningPoint.UDP, dnsServerLocator.getDefaultTransportForSipUri(sipURI));
		sipURI.setSecure(true);
		assertEquals(ListeningPoint.TCP, dnsServerLocator.getDefaultTransportForSipUri(sipURI));
	}
	
	/**
	 * Test method for {@link org.mobicents.ext.javax.sip.dns.DNSServerLocator#resolveHostByDnsSrvLookup(javax.sip.address.SipURI)}.
	 * @throws ParseException 
	 */
	@Test
	public void test() throws ParseException {
		Set<String> supportedTransports = new HashSet<String>();
		supportedTransports.add(ListeningPoint.UDP);
		supportedTransports.add(ListeningPoint.TCP);
		DNSServerLocator dnsServerLocator = new DNSServerLocator(supportedTransports);
		
		SipURI sipURI = addressFactory.createSipURI("jean","iptel.org");
		dnsServerLocator.resolveHostByDnsSrvLookup(sipURI);
	}

}

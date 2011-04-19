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

import javax.sip.SipException;
import javax.sip.SipStack;
import javax.sip.address.Hop;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.RouteHeader;
import javax.sip.message.Request;

import gov.nist.core.CommonLogger;
import gov.nist.core.InternalErrorHandler;
import gov.nist.core.LogWriter;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.DefaultRouter;

/**
 * This custom router is the same as the DefaultRouter from jain sip except that it remove the first route if it contains
 * DNS_ROUTE param in the Route' SIP URI
 * 
 * @author jean.deruelle@gmail.com
 *
 */
public class DNSAwareRouter extends DefaultRouter {
	public static final String DNS_ROUTE = "dns_route";
	private static StackLogger logger = CommonLogger.getLogger(DNSAwareRouter.class);
	
	public DNSAwareRouter(SipStack sipStack, String defaultRoute) {
		super(sipStack, defaultRoute);
	}


	@Override
	public Hop getNextHop(Request request) throws SipException {
		SIPRequest sipRequest = (SIPRequest) request;

        RequestLine requestLine = sipRequest.getRequestLine();
        if (requestLine == null) {
            return getOutboundProxy();
        }
        javax.sip.address.URI requestURI = requestLine.getUri();
        if (requestURI == null)
            throw new IllegalArgumentException("Bad message: Null requestURI");

        RouteList routes = sipRequest.getRouteHeaders();

        /*
         * In case the topmost Route header contains no 'lr' parameter (which
         * means the next hop is a strict router), the implementation will
         * perform 'Route Information Postprocessing' as described in RFC3261
         * section 16.6 step 6 (also known as "Route header popping"). That is,
         * the following modifications will be made to the request:
         *
         * The implementation places the Request-URI into the Route header field
         * as the last value.
         *
         * The implementation then places the first Route header field value
         * into the Request-URI and removes that value from the Route header
         * field.
         *
         * Subsequently, the request URI will be used as next hop target
         */

        if (routes != null) {

            // to send the request through a specified hop the application is
            // supposed to prepend the appropriate Route header which.
            Route route = (Route) routes.getFirst();            
            URI uri = route.getAddress().getURI();
            if (uri.isSipURI()) {
                SipURI sipUri = (SipURI) uri;
                if(sipUri.getParameter(DNS_ROUTE) != null) {
                	if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG))
                        logger.logDebug("Removing Route added by container to conform to RFC 3263 " + route);
                	sipRequest.removeFirst(RouteHeader.NAME);
                }
                if (!sipUri.hasLrParam()) {

                    fixStrictRouting(sipRequest);
                    if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG))
                        logger
                                .logDebug("Route post processing fixed strict routing");
                }

                Hop hop = createHop(sipUri,request);
                if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG))
                    logger
                            .logDebug("NextHop based on Route:" + hop);
                return hop;
            } else {
                throw new SipException("First Route not a SIP URI");
            }

        } else if (requestURI.isSipURI()
                && ((SipURI) requestURI).getMAddrParam() != null) {
            Hop hop = createHop((SipURI) requestURI,request);
            if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG))
                logger
                        .logDebug("Using request URI maddr to route the request = "
                                + hop.toString());

            // JvB: don't remove it!
            // ((SipURI) requestURI).removeParameter("maddr");

            return hop;

        } else if (getOutboundProxy() != null) {
            if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG))
                logger
                        .logDebug("Using outbound proxy to route the request = "
                                + getOutboundProxy().toString());
            return getOutboundProxy();
        } else if (requestURI.isSipURI()) {
            Hop hop = createHop((SipURI) requestURI,request);
            if (hop != null && logger.isLoggingEnabled(LogWriter.TRACE_DEBUG))
                logger.logDebug("Used request-URI for nextHop = "
                        + hop.toString());
            else if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                logger
                        .logDebug("returning null hop -- loop detected");
            }
            return hop;

        } else {
            // The internal router should never be consulted for non-sip URIs.
            InternalErrorHandler.handleException("Unexpected non-sip URI",
                    this.logger);
            return null;
        }
	}
}

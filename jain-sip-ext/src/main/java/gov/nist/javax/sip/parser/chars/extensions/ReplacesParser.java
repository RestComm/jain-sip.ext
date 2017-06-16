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

package gov.nist.javax.sip.parser.chars.extensions;

import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.header.extensions.*;
import gov.nist.javax.sip.parser.*;

import java.text.ParseException;

// Parser for Replaces Header (RFC3891)
// Extension by pmusgrave
//
// Replaces        = "Replaces" HCOLON callid *(SEMI replaces-param)
// replaces-param  = to-tag / from-tag / early-flag / generic-param
// to-tag          = "to-tag" EQUAL token
// from-tag        = "from-tag" EQUAL token
// early-flag      = "early-only"
//
// TODO Should run a test case on early-only
//

public class ReplacesParser extends ParametersParser {

    /**
     * Creates new CallIDParser
     * @param callID message to parse
     */
    public ReplacesParser(String callID) {
        super(callID);
    }

    /**
     * Constructor
     * @param lexer Lexer to set
     */
    protected ReplacesParser(Lexer lexer) {
        super(lexer);
    }

    /**
     * parse the String message
     * @return SIPHeader (CallID object)
     * @throws ParseException if the message does not respect the spec.
     */
    public SIPHeader parse() throws ParseException {
        if (debug)
            dbg_enter("parse");
        try {
            headerName(TokenTypes.REPLACES_TO);

            Replaces replaces = new Replaces();
            this.lexer.SPorHT();
            String callId = lexer.byteStringNoSemicolon();
            this.lexer.SPorHT();
            super.parse(replaces);
            replaces.setCallId(callId);
            return replaces;
        } finally {
            if (debug)
                dbg_leave("parse");
        }
    }

    public static void main(String args[]) throws ParseException {
        String to[] =
            {   "Replaces: 12345th5z8z\n",
                "Replaces: 12345th5z8z;to-tag=tozght6-45;from-tag=fromzght789-337-2\n",
            };

        for (int i = 0; i < to.length; i++) {
            ReplacesParser tp = new ReplacesParser(to[i]);
            Replaces t = (Replaces) tp.parse();
            System.out.println("Parsing => " + to[i]);
            System.out.print("encoded = " + t.encode() + "==> ");
            System.out.println("callId " + t.getCallId() + " from-tag=" + t.getFromTag()
                    + " to-tag=" + t.getToTag()) ;

        }
    }

}

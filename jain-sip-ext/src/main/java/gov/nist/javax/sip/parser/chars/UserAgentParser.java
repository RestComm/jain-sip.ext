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

/*
* Conditions Of Use
*
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), an agency of the Federal Government.
* Pursuant to title 15 Untied States Code Section 105, works of NIST
* employees are not subject to copyright protection in the United States
* and are considered to be in the public domain.  As a result, a formal
* license is not needed to use the software.
*
* This software is provided by NIST as a service and is expressly
* provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
* OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
* AND DATA ACCURACY.  NIST does not warrant or make any representations
* regarding the use of the software or the results thereof, including but
* not limited to the correctness, accuracy, reliability or usefulness of
* the software.
*
* Permission to use this software is contingent upon your acceptance
* of the terms of this agreement
*
* .
*
*/
package gov.nist.javax.sip.parser.chars;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.UserAgent;

import java.text.ParseException;

/**
 * Parser for UserAgent header.
 *
 * @version 1.2 $Revision: 1.15 $ $Date: 2009/07/17 18:58:07 $
 *
 * @author Olivier Deruelle  <br/>
 * @author M. Ranganathan  <br/>
 *
 *
 */
public class UserAgentParser extends HeaderParser {

    /**
     * Constructor
     *
     * @param userAgent -
     *            UserAgent header to parse
     */
    public UserAgentParser(char[] userAgent) {
        super(userAgent);
    }

    /**
     * Constructor
     *
     * @param lexer -
     *            the lexer to use.
     */
    protected UserAgentParser(Lexer lexer) {
        super(lexer);
    }

    /**
     * parse the message. Note that we have losened up on the parsing quite a bit because
     * user agents tend to be very bad about specifying the user agent according to RFC.
     *
     * @return SIPHeader (UserAgent object)
     * @throws SIPParseException
     *             if the message does not respect the spec.
     */
    public SIPHeader parse() throws ParseException {
        if (debug)
            dbg_enter("UserAgentParser.parse");
        UserAgent userAgent = new UserAgent();
        try {
            headerName(TokenTypes.USER_AGENT);
            if (this.lexer.lookAhead(0) == '\n')
                throw createParseException("empty header");

            /*
             * BNF User-Agent = "User-Agent" HCOLON server-val *(LWS server-val)
             * server-val = product / comment product = token [SLASH
             * product-version] product-version = token
             */
            while (this.lexer.lookAhead(0) != '\n'
                    && this.lexer.lookAhead(0) != '\0') {

                if (this.lexer.lookAhead(0) == '(') {
                    String comment = this.lexer.comment();
                    userAgent.addProductToken('(' + comment + ')');
                } else {
                    // product = token [SLASHproduct-version]
                    // product-version = token
                    // The RFC Does NOT allow this space but we are generous in what we accept

                    this.getLexer().SPorHT();


                    String product = this.lexer.byteStringNoSlash();
                    if ( product == null ) throw createParseException("Expected product string");

                    StringBuilder productSb = new StringBuilder(product);
                    // do we possibily have the optional product-version?
                    if (this.lexer.peekNextToken().getTokenType() == TokenTypes.SLASH) {
                        // yes
                        this.lexer.match(TokenTypes.SLASH);
                        // product-version
                        // The RFC Does NOT allow this space but we are generous in what we accept
                        this.getLexer().SPorHT();

                        String productVersion = this.lexer.byteStringNoSlash();

                        if ( productVersion == null ) throw createParseException("Expected product version");

                        productSb.append("/");

                        productSb.append(productVersion);
                    }

                    userAgent.addProductToken(productSb.toString());
                }
                // LWS
                this.lexer.SPorHT();
            }
        } finally {
            if (debug)
                dbg_leave("UserAgentParser.parse");
        }

        return userAgent;
    }


      public static void main(String args[]) throws ParseException { String
      userAgent[] = { "User-Agent: Softphone/Beta1.5 \n", "User-Agent:Nist/Beta1 (beta version) \n", "User-Agent: Nist UA (beta version)\n",
      "User-Agent: Nist1.0/Beta2 Ubi/vers.1.0 (very cool) \n" ,
      "User-Agent: SJphone/1.60.299a/L (SJ Labs)\n",
      "User-Agent: sipXecs/3.5.11 sipXecs/sipxbridge (Linux)\n"};

      for (int i = 0; i < userAgent.length; i++ ) { UserAgentParser parser =
      new UserAgentParser(userAgent[i].toCharArray()); UserAgent ua= (UserAgent)
      parser.parse(); System.out.println("encoded = " + ua.encode()); }
       }

}

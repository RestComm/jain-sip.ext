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

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;

/**
 * Parser for the challenge portion of the authentication header.
 *
 * @version 1.2 $Revision: 1.8 $ $Date: 2009/07/17 18:57:58 $
 * @since 1.1
 *
 * @author Olivier Deruelle    <br/>
 *
 *
 */

public abstract class ChallengeParser extends HeaderParser {

    /**
     * Constructor
     * @param String challenge  message to parse to set
     */
    protected ChallengeParser(char[] challenge) {
        super(challenge);
    }

    /**
     * Constructor
     * @param String challenge  message to parse to set
     */
    protected ChallengeParser(Lexer lexer) {
        super(lexer);
    }

    /**
     * Get the parameter of the challenge string
     * @return NameValue containing the parameter
     */
    protected void parseParameter(AuthenticationHeader header)
        throws ParseException {

        if (debug)
            dbg_enter("parseParameter");
        try {
            NameValue nv = this.nameValue('=');
            header.setParameter(nv);
        } finally {
            if (debug)
                dbg_leave("parseParameter");
        }

    }

    /**
     * parser the String message.
     * @param header - header structure to fill in.
     * @throws ParseException if the message does not respect the spec.
     */
    public void parse(AuthenticationHeader header) throws ParseException {

        // the Scheme:
        this.lexer.SPorHT();
        lexer.match(TokenTypes.ID);
        Token type = lexer.getNextToken();
        this.lexer.SPorHT();
        header.setScheme(type.getTokenValue());

        // The parameters:
        try {
            while (lexer.lookAhead(0) != '\n') {
                this.parseParameter(header);
                this.lexer.SPorHT();
                char la = lexer.lookAhead(0);
                if (la == '\n' || la == '\0')
                    break;
                this.lexer.match(',');
                this.lexer.SPorHT();
            }
        } catch (ParseException ex) {
            throw ex;
        }
    }
}

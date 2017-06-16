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
import gov.nist.javax.sip.address.*;
import java.text.ParseException;

/**
 * Address parameters parser.
 *
 * @version 1.2 $Revision: 1.10 $ $Date: 2009/10/22 10:25:57 $
 * @author M. Ranganathan
 * @since 1.1
 *
 */
public class AddressParametersParser extends ParametersParser {

    protected AddressParametersParser(Lexer lexer) {
        super(lexer);
    }

    protected AddressParametersParser(char[] buffer) {
        super(buffer);
    }

    protected void parse(AddressParametersHeader addressParametersHeader)
        throws ParseException {
        dbg_enter("AddressParametersParser.parse");
        try {
            AddressParser addressParser = new AddressParser(this.getLexer());
            AddressImpl addr = addressParser.address(false);
            addressParametersHeader.setAddress(addr);
            lexer.SPorHT();
            char la = this.lexer.lookAhead(0);
            if ( this.lexer.hasMoreChars() &&
                 la != '\0' &&
                 la != '\n' &&
                 this.lexer.startsId()) {

                 super.parseNameValueList(addressParametersHeader);


            }  else super.parse(addressParametersHeader);

        } catch (ParseException ex) {
            throw ex;
        } finally {
            dbg_leave("AddressParametersParser.parse");
        }
    }
}

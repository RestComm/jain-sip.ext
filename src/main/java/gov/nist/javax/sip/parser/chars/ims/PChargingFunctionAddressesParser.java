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
/*******************************************
 * PRODUCT OF PT INOVACAO - EST DEPARTMENT *
 *******************************************/

package gov.nist.javax.sip.parser.chars.ims;

import gov.nist.core.NameValue;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PChargingFunctionAddresses;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;

import java.text.ParseException;


/**
 * P-Charging-Function-Addresses header parser.
 *
 * <p>Sintax (RFC 3455):</p>
 * <pre>
 * P-Charging-Addr    = "P-Charging-Function-Addresses" HCOLON
 *                      charge-addr-params
 *                      * (SEMI charge-addr-params)
 * charge-addr-params = ccf / ecf / generic-param
 * ccf                = "ccf" EQUAL gen-value
 * ecf                = "ecf" EQUAL gen-value
 * gen-value          = token / host / quoted-string
 * host               = hostname / IPv4address / IPv6reference
 * hostname           = *( domainlabel "." ) toplabel [ "." ]
 * domainlabel        = alphanum / alphanum *( alphanum / "-" ) alphanum
 * toplabel           = ALPHA / ALPHA *( alphanum / "-" ) alphanum
 * ipv6reference      = "[" IPv6address "]"
 *
 * </pre>
 *
 * @author ALEXANDRE MIGUEL SILVA SANTOS
 * @author aayush.bhatnagar: proposed change to allow duplicate ecf and ccf header parameters.
 */

public class PChargingFunctionAddressesParser
    extends ParametersParser
    implements TokenTypes {


    public PChargingFunctionAddressesParser(String charging) {

        super(charging);


    }


    protected PChargingFunctionAddressesParser(Lexer lexer) {
        super(lexer);

    }



    public SIPHeader parse() throws ParseException {


        if (debug)
            dbg_enter("parse");
        try {
            headerName(TokenTypes.P_CHARGING_FUNCTION_ADDRESSES);
            PChargingFunctionAddresses chargingFunctionAddresses = new PChargingFunctionAddresses();

            try {
                while (lexer.lookAhead(0) != '\n') {

                    this.parseParameter(chargingFunctionAddresses);
                    this.lexer.SPorHT();
                    char la = lexer.lookAhead(0);
                    if (la == '\n' || la == '\0')
                        break;

                    this.lexer.match(';');
                    this.lexer.SPorHT();
                }
            } catch (ParseException ex) {
                throw ex;
            }


            super.parse(chargingFunctionAddresses);
            return chargingFunctionAddresses;
        } finally {
            if (debug)
                dbg_leave("parse");
        }
    }

    protected void parseParameter(PChargingFunctionAddresses chargingFunctionAddresses) throws ParseException {

        if (debug)
            dbg_enter("parseParameter");
        try {

            NameValue nv = this.nameValue('=');
             
            //chargingFunctionAddresses.setParameter(nv);
            chargingFunctionAddresses.setMultiParameter(nv);

        } finally {
            if (debug)
                dbg_leave("parseParameter");
        }



    }






    /** Test program */

    public static void main(String args[]) throws ParseException {
        String r[] = {
                "P-Charging-Function-Addresses: ccf=\"test str\"; ecf=token\n",
                "P-Charging-Function-Addresses: ccf=192.1.1.1; ccf=192.1.1.2; ecf=192.1.1.3; ecf=192.1.1.4\n",
                "P-Charging-Function-Addresses: ccf=[5555::b99:c88:d77:e66]; ccf=[5555::a55:b44:c33:d22]; " +
                     "ecf=[5555::1ff:2ee:3dd:4cc]; ecf=[5555::6aa:7bb:8cc:9dd]\n"

                };


        for (int i = 0; i < r.length; i++ )
        {

            PChargingFunctionAddressesParser parser =
              new PChargingFunctionAddressesParser(r[i]);

            System.out.println("original = " + r[i]);

            PChargingFunctionAddresses chargAddr= (PChargingFunctionAddresses) parser.parse();
            System.out.println("encoded = " + chargAddr.encode());
        }


    }







}

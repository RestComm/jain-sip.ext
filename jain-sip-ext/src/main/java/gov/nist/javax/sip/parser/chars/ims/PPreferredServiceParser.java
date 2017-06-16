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

package gov.nist.javax.sip.parser.chars.ims;
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
import java.text.ParseException;

import javax.sip.InvalidArgumentException;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PPreferredService;
import gov.nist.javax.sip.header.ims.ParameterNamesIms;
import gov.nist.javax.sip.parser.chars.HeaderParser;
import gov.nist.javax.sip.parser.chars.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
/**
 *
 * @author aayush.bhatnagar
 * Rancore Technologies Pvt Ltd, Mumbai India.
 *
 * Parse this:
 * P-Preferred-Service: urn:urn-7:3gpp-service.exampletelephony.version1
 *
 */
public class PPreferredServiceParser extends HeaderParser implements TokenTypes{

    protected PPreferredServiceParser(Lexer lexer) {
        super(lexer);
    }

    public PPreferredServiceParser(char[] pps)
    {
        super(pps);
    }

    /**
     * "The URN consists of a hierarchical service identifier or application
     * identifier, with a sequence of labels separated by periods.The left-most label is
     * the most significant one and is called 'top-level service
     * identifier', while names to the right are called 'sub-services' or
     * 'sub-applications'.
     *
     * For any given service identifier, labels can be removed right-to-left and
     * the resulting URN is still valid, referring a more generic
     * service, with the exception of the top-level service identifier
     * and possibly the first sub-service or sub-application identifier.
     *
     *  Labels cannot be removed beyond a defined basic service, for
     *  example, the label w.x may define a service, but the label w may
     *  only define an assignment authority for assigning subsequent
     *  values and not define a service in its own right.  In other words,
     *  if a service identifier 'w.x.y.z' exists, the URNs 'w.x' and
     *  'w.x.y' are also valid service identifiers, but w may not be a
     *  valid service identifier if it merely defines who is responsible"
     *
     * TODO: PLEASE VALIDATE MY UNDERSTANDING OF THE ABOVE TEXT :)
     * @ranga: Please validate my understanding of the above text in the draft :)
     *         This last para is a little ambiguous.I will only check that atleast
     *         1 sub-service or 1 sub-application is present in the URN declaration.
     *         If not, I throw an exception. I thought of not throwing an exception
     *         and returning whatever was encoded..but the resultant encoding wont
     *         make sense. It would be something like-->
     *         urn:urn-7:3gpp-service OR urn:urn-7:3gpp-application alone with no sub-services
     *         or sub-applications. This is bound to cause an error at the recepient later.
     *
     * Sub-service and Application identifiers are not maintained by IANA and
     * are organization/application dependent (Section 8.2). So we cannot gurantee what lies
     * beyond the first sub-service or sub-application identifier. It should be the responsibility
     * of the application to make sense of the entire URN holistically. We can only check for the
     * standardized part as per the ABNF.
     */
    public SIPHeader parse() throws ParseException {
        if(debug)
            dbg_enter("PPreferredServiceParser.parse");
        try
        {

        this.lexer.match(TokenTypes.P_PREFERRED_SERVICE);
        this.lexer.SPorHT();
        this.lexer.match(':');
        this.lexer.SPorHT();

        PPreferredService pps = new PPreferredService();
        String urn = this.lexer.getBuffer();
        if(urn.contains(ParameterNamesIms.SERVICE_ID)){

           if(urn.contains(ParameterNamesIms.SERVICE_ID_LABEL))
                   {
                    String serviceID = urn.split(ParameterNamesIms.SERVICE_ID_LABEL+".")[1];

                     if(serviceID.trim().equals(""))
                        try {
                            throw new InvalidArgumentException("URN should atleast have one sub-service");
                        } catch (InvalidArgumentException e) {

                            e.printStackTrace();
                        }
                        else
                    pps.setSubserviceIdentifiers(serviceID);
                   }
           else if(urn.contains(ParameterNamesIms.APPLICATION_ID_LABEL))
              {
               String appID = urn.split(ParameterNamesIms.APPLICATION_ID_LABEL)[1];
               if(appID.trim().equals(""))
                    try {
                        throw new InvalidArgumentException("URN should atleast have one sub-application");
                    } catch (InvalidArgumentException e) {
                        e.printStackTrace();
                    }
                    else
                  pps.setApplicationIdentifiers(appID);
              }
           else
           {
               try {
                throw new InvalidArgumentException("URN is not well formed");

            } catch (InvalidArgumentException e) {
                e.printStackTrace();
                    }
                  }
          }

            super.parse();
            return pps;
        }
        finally{
            if(debug)
                dbg_enter("PPreferredServiceParser.parse");
        }

    }
}

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.systest.jaxws;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.ext.logging.Logging;

@WebService(name = "MyEchoService", targetNamespace = "urn:echo")
@Logging
public class EchoServiceImpl implements EchoService {
    public String echoException(String input) throws SOAPFaultException {

        SOAPFaultException ex;
        try {
            ex = wrapToSoapFault(new Exception("hello"));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return e.toString();
        }
        throw ex;

    }

    private SOAPFaultException wrapToSoapFault(Exception ex) throws Exception {
        SOAPFactory fac = null;
        try {
            fac = SOAPFactory.newInstance();
            String message = ex.getMessage();
            SOAPFault sf = fac.createFault(message, new QName(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE,
                                                              "Server"));
            sf.setFaultString("TestSOAPFaultException");
            // add detail makes CXF goes in a infinite loop
            sf.addDetail().addDetailEntry(new QName("urn:echo", "entry")).addTextNode("SOAPFaultException");

            return new SOAPFaultException(sf);
        } catch (Exception e2) {

            throw e2;
        }
    }
}

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
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

@WebService(endpointInterface = "org.apache.cxf.systest.jaxws.DocLitBareCodeFirstService",
            serviceName = "DocLitBareCodeFirstService",
            portName = "DocLitBareCodeFirstServicePort",
            targetNamespace = "http://cxf.apache.org/systest/jaxws/DocLitBareCodeFirstService")
public class DocLitBareCodeFirstServiceImpl implements DocLitBareCodeFirstService {

    public GreetMeResponse greetMe(GreetMeRequest gmr) {
        if ("fault".equals(gmr.getName())) {
            try { 
                SOAPFactory factory = SOAPFactory.newInstance();
                SOAPFault fault = factory.createFault("this is a fault string!",
                                                      new QName("http://foo", "FooCode"));
                fault.setFaultActor("mr.actor");
                fault.addDetail().addChildElement("test").addTextNode("TestText");
                throw new SOAPFaultException(fault);
            } catch (SOAPException ex) {
                throw new WebServiceException(ex);
            }
        } else if ("emptyfault".equals(gmr.getName())) {
            throw new RuntimeException("Empty!");
        }
        
        GreetMeResponse resp = new GreetMeResponse();
        resp.setName(gmr.getName());
        return resp;
    }

}

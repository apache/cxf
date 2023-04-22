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

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.jws.WebService;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.ext.logging.Logging;

@WebService(name = "MyEchoProxyService", targetNamespace = "urn:echo")
@Logging
public class EchoProxyServiceImpl implements EchoService {
    public String echoException(String input) throws SOAPFaultException {
        return input;
    }

    public String echoProxy(String address) throws SOAPFaultException {
        QName serviceName = new QName("urn:echo", "EchoServiceImplService");
        QName portName = new QName("urn:echo", "MyEchoServicePort");
        URL wsdlURL = null;
        try {
            wsdlURL = new URL(address);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Service service = Service.create(wsdlURL, serviceName);
        EchoService echoService = service.getPort(portName, EchoService.class);
        try {
            echoService.proxyException("input");
        } catch (SOAPFaultException se) {
            throw se;
        }
        return "DONE";
    }

    @Override
    public String proxyException(String input) throws SOAPFaultException {
        return input;
    }

}
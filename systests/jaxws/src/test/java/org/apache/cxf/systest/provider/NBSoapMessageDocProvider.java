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

package org.apache.cxf.systest.provider;

import java.io.InputStream;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

import org.apache.cxf.binding.soap.saaj.SAAJUtils;

@WebServiceProvider(portName = "SoapProviderPort", serviceName = "SOAPProviderService",
                    targetNamespace = "http://apache.org/hello_world_soap_http")
@ServiceMode(value = Service.Mode.MESSAGE)
public class NBSoapMessageDocProvider implements Provider<SOAPMessage> {
    private SOAPMessage sayHiResponse;

    public NBSoapMessageDocProvider() {

        try {
            MessageFactory factory = MessageFactory.newInstance();
            InputStream is = getClass().getResourceAsStream("resources/sayHiDocLiteralResp.xml");
            sayHiResponse = factory.createMessage(null, is);
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public SOAPMessage invoke(SOAPMessage request) {
        SOAPBody body = null;
        try {
            body = SAAJUtils.getBody(request);
        } catch (SOAPException e) {
            throw new RuntimeException("soap body expected");
        }
        if (body.getFirstChild() != null) {
            throw new RuntimeException("no body expected");
        }
        return sayHiResponse;
    }
}

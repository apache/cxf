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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jakarta.annotation.Resource;
import jakarta.xml.soap.Detail;
import jakarta.xml.soap.DetailEntry;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.helpers.DOMUtils;

//The following wsdl file is used.
//wsdlLocation = "/trunk/testutils/src/main/resources/wsdl/hello_world.wsdl"
@WebServiceProvider(portName = "SoapProviderPort", serviceName = "SOAPProviderService",
                    targetNamespace = "http://apache.org/hello_world_soap_http",
                    wsdlLocation = "/org/apache/cxf/systest/provider/hello_world_with_restriction.wsdl")
@ServiceMode(value = Service.Mode.MESSAGE)
public class HWSoapMessageDocProvider implements Provider<SOAPMessage> {

    private static QName sayHi = new QName("http://apache.org/hello_world_soap_http", "sayHi");
    private static QName greetMe = new QName("http://apache.org/hello_world_soap_http", "greetMe");

    @Resource
    WebServiceContext ctx;

    private SOAPMessage sayHiResponse;
    private SOAPMessage greetMeResponse;
    private SOAPMessage greetMeResponseExceedMaxLengthRestriction;

    public HWSoapMessageDocProvider() {

        try {
            MessageFactory factory = MessageFactory.newInstance();
            InputStream is = getClass().getResourceAsStream("resources/sayHiDocLiteralResp.xml");
            sayHiResponse = factory.createMessage(null, is);
            is.close();
            is = getClass().getResourceAsStream("resources/GreetMeDocLiteralResp.xml");
            greetMeResponse = factory.createMessage(null, is);
            is.close();
            is = getClass().getResourceAsStream("resources/GreetMeDocLiteralRespExceedLength.xml");
            greetMeResponseExceedMaxLengthRestriction = factory.createMessage(null, is);
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public SOAPMessage invoke(SOAPMessage request) {
        QName qn = (QName)ctx.getMessageContext().get(MessageContext.WSDL_OPERATION);
        if (qn == null) {
            throw new RuntimeException("No Operation Name");
        }

        SOAPMessage response = null;
        SOAPBody body = null;
        try {
            body = SAAJUtils.getBody(request);
        } catch (SOAPException e) {
            return null;
        }
        Node n = body.getFirstChild();

        while (n.getNodeType() != Node.ELEMENT_NODE) {
            n = n.getNextSibling();
        }
        if (n.getLocalName().equals(sayHi.getLocalPart())) {
            response = sayHiResponse;
        } else if (n.getLocalName().equals(greetMe.getLocalPart())) {
            Element el = DOMUtils.getFirstElement(n);
            String v = DOMUtils.getContent(el);
            if (v.contains("Return sayHi")) {
                response = sayHiResponse;
            } else if (v.contains("exceed maxLength")) {
                response = greetMeResponseExceedMaxLengthRestriction;
            } else if (v.contains("throwFault")) {
                try {
                    SOAPFactory f = SOAPFactory.newInstance();
                    SOAPFault soapFault = f.createFault();

                    soapFault.setFaultString("Test Fault String ****");

                    Detail detail = soapFault.addDetail();
                    detail = soapFault.getDetail();

                    QName qName = new QName("http://www.Hello.org/greeter", "TestFault", "ns");
                    DetailEntry de = detail.addDetailEntry(qName);

                    qName = new QName("http://www.Hello.org/greeter", "ErrorCode", "ns");
                    SOAPElement errorElement = de.addChildElement(qName);
                    errorElement.setTextContent("errorcode");
                    throw new SOAPFaultException(soapFault);
                } catch (SOAPException ex) {
                    //ignore
                }

            } else {
                response = greetMeResponse;
            }
        }
        return response;
    }
}

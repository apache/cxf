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
package org.apache.cxf.systest.jms.continuations;

import java.io.InputStream;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jakarta.annotation.Resource;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.helpers.DOMUtils;

@WebServiceProvider(serviceName = "HelloWorldService",
            portName = "HelloWorldPort",
            targetNamespace = "http://cxf.apache.org/hello_world_jms",
            wsdlLocation = "/org/apache/cxf/systest/jms/continuations/jms_test.wsdl")
@ServiceMode(value = Service.Mode.MESSAGE)
public class HWSoapMessageDocProvider implements Provider<SOAPMessage> {

    private static QName sayHi = new QName("http://apache.org/hello_world_soap_http", "sayHi");
    private static QName greetMe = new QName("http://apache.org/hello_world_soap_http", "greetMe");

    @Resource
    WebServiceContext ctx;

    private SOAPMessage sayHiResponse;
    private SOAPMessage greetMeResponse;

    public HWSoapMessageDocProvider() {

        try {
            MessageFactory factory = MessageFactory.newInstance();
            InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralResp.xml");
            greetMeResponse = factory.createMessage(null, is);
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public SOAPMessage invoke(SOAPMessage request) {
        try {
            final MessageContext messageContext = ctx.getMessageContext();

            ContinuationProvider contProvider =
                (ContinuationProvider) messageContext.get(ContinuationProvider.class.getName());
            final Continuation continuation = contProvider.getContinuation();

            if (continuation.isNew()) {
                continuation.suspend(5000);
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            continuation.resume();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                return null;
            } else if (!continuation.isResumed()) {
                continuation.reset();
                throw new RuntimeException("time out");
            } else {
                return resumeMessage(request);
            }
        } catch (SOAPFaultException e) {
            throw e;
        }

    }

    public SOAPMessage resumeMessage(SOAPMessage request) {
        if (IncomingMessageCounterInterceptor.getMessageCount() != 1) {
            throw new RuntimeException("IncomingMessageCounterInterceptor get invoked twice");
        }
        QName qn = (QName)ctx.getMessageContext().get(MessageContext.WSDL_OPERATION);
        if (qn == null) {
            throw new RuntimeException("No Operation Name");
        }

        SOAPMessage response = null;
        try {
            SOAPBody body = request.getSOAPBody();
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
                } else {
                    response = greetMeResponse;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return response;
    }
}

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
package org.apache.cxf.systest.handlers;

import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class TestMustUnderstandHandler<T extends SOAPMessageContext> extends TestHandlerBase implements
    SOAPHandler<T> {

    public TestMustUnderstandHandler() {
        super(true);
    }

    public boolean handleMessage(SOAPMessageContext ctx) {

        boolean continueProcessing = true;

        try {
            Object b = ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            boolean outbound = (Boolean)b;
            SOAPMessage msg = ctx.getMessage();
            if (isServerSideHandler()) {
                if (outbound) {
                    QName qname = new QName("http://cxf.apache.org/mu", "MU");
                    SOAPPart soapPart = msg.getSOAPPart();
                    SOAPEnvelope envelope = soapPart.getEnvelope();
                    SOAPHeader header = envelope.getHeader();
                    if (header == null) {
                        header = envelope.addHeader();
                    }


                    SOAPHeaderElement headerElement
                        = header.addHeaderElement(envelope.createName("MU", "ns1", qname.getNamespaceURI()));

                    // QName soapMustUnderstand = new QName("http://schemas.xmlsoap.org/soap/envelope/",
                    // "mustUnderstand");
                    Name name = SOAPFactory.newInstance()
                        .createName("mustUnderstand", "soap", "http://schemas.xmlsoap.org/soap/envelope/");
                    headerElement.addAttribute(name, "1");
                } else {
                    getHandlerInfoList(ctx).add(getHandlerId());
                }
            }
        } catch (SOAPException e) {
            e.printStackTrace();
        }
        return continueProcessing;
    }

    public String getHandlerId() {
        return "TestMustUnderstandHandler";
    }

    public Set<QName> getHeaders() {
        return null;
    }

    public void close(MessageContext messagecontext) {
    }

    public boolean handleFault(T messagecontext) {
        return true;
    }

}

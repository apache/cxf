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
package org.apache.cxf.ws.security.wss4j;

import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.WSSec;
import org.apache.wss4j.stax.ext.InboundWSSec;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventListener;

public class WSS4JStaxInInterceptor extends AbstractWSS4JStaxInterceptor {
    
    private final InboundWSSec inboundWSSec;
    
    public WSS4JStaxInInterceptor(WSSSecurityProperties securityProperties) throws WSSecurityException {
        super();
        setPhase(Phase.POST_STREAM);
        getAfter().add(StaxInInterceptor.class.getName());
        
        inboundWSSec = WSSec.getInboundWSSec(securityProperties);
    }

    @Override
    public void handleMessage(SoapMessage soapMessage) throws Fault {

        XMLStreamReader originalXmlStreamReader = soapMessage.getContent(XMLStreamReader.class);
        XMLStreamReader newXmlStreamReader;

        final List<SecurityEvent> incomingSecurityEventList = new LinkedList<SecurityEvent>();
        SecurityEventListener securityEventListener = new SecurityEventListener() {
            @Override
            public void registerSecurityEvent(SecurityEvent securityEvent) throws WSSecurityException {
                incomingSecurityEventList.add(securityEvent);
            }
        };
        soapMessage.getExchange().put(SecurityEvent.class.getName() + ".in", incomingSecurityEventList);

        try {
            @SuppressWarnings("unchecked")
            final List<SecurityEvent> requestSecurityEvents = 
                (List<SecurityEvent>) soapMessage.getExchange().get(SecurityEvent.class.getName() + ".out");
            newXmlStreamReader = 
                inboundWSSec.processInMessage(originalXmlStreamReader, requestSecurityEvents, securityEventListener);
            soapMessage.setContent(XMLStreamReader.class, newXmlStreamReader);

            // Warning: The exceptions which can occur here are not security relevant exceptions
            // but configuration-errors. To catch security relevant exceptions you have to catch 
            // them e.g.in the FaultOutInterceptor. Why? Because we do streaming security. This 
            // interceptor doesn't handle the ws-security stuff but just setup the relevant stuff
            // for it. Exceptions will be thrown as a wrapped XMLStreamException during further
            // processing in the WS-Stack.

        } catch (WSSecurityException e) {
            throw new SoapFault("unexpected service error", SoapFault.FAULT_CODE_SERVER);
        } catch (XMLStreamException e) {
            throw new SoapFault("unexpected service error", SoapFault.FAULT_CODE_SERVER);
        }
    }
}

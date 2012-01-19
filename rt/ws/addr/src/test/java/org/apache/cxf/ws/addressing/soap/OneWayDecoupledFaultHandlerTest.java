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
package org.apache.cxf.ws.addressing.soap;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class OneWayDecoupledFaultHandlerTest extends Assert {

    @Test
    public void testOnewayFault() {
        OneWayDecoupledFaultHandler handler = new OneWayDecoupledFaultHandler() {
            protected Destination createDecoupledDestination(Exchange exchange, EndpointReferenceType epr) {
                assertEquals("http://bar", epr.getAddress().getValue());
                return EasyMock.createMock(Destination.class);
            }    
        };
        
        SoapMessage message = new SoapMessage(new MessageImpl());
        QName qname = new QName("http://cxf.apache.org/mustunderstand", "TestMU");
        message.getHeaders().add(new Header(qname, new Object()));
        AddressingProperties maps = new AddressingPropertiesImpl();
        
        EndpointReferenceType faultTo = new EndpointReferenceType();
        faultTo.setAddress(new AttributedURIType());
        faultTo.getAddress().setValue("http://bar");
        maps.setFaultTo(faultTo);
        message.put(ContextUtils.getMAPProperty(false, false, false), 
                    maps);
        
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        exchange.setInMessage(message);
        exchange.setOneWay(true);
        
        handler.handleFault(message);
        assertTrue(message.getHeaders().isEmpty());
        assertFalse(exchange.isOneWay());
        assertSame(message, exchange.getOutMessage());
        assertNotNull(exchange.getDestination());
    }
    
}

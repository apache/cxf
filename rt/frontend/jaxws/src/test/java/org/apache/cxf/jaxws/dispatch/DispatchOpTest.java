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
package org.apache.cxf.jaxws.dispatch;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.AddressingFeature;

import org.w3c.dom.Document;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.AbstractJaxWsTest;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.jaxws.MessageReplayObserver;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.Destination;
import org.junit.Before;
import org.junit.Test;

public class DispatchOpTest extends AbstractJaxWsTest {
    private final QName serviceName = new QName("http://cxf.apache.org/test/dispatch", "DispatchTest");

    private final QName portName = new QName("http://cxf.apache.org/test/dispatch", "DispatchPort");

    private final String address = "http://localhost:9120/SoapContext/DispatchPort";

    private final QName operationName = new QName("http://cxf.apache.org/test/dispatch", "RequestResponseOperation");

    private final String wsdlResource = "/org/apache/cxf/jaxws/dispatch/DispatchTest.wsdl";

    private final String requestResource = "/org/apache/cxf/jaxws/dispatch/OperationRequest.xml";

    private final String responseResource = "/org/apache/cxf/jaxws/dispatch/OperationResponse.xml";

    private Destination d;

    @Before
    public void setUp() throws Exception {
        EndpointInfo ei = new EndpointInfo(null, "http://schemas.xmlsoap.org/soap/http");
        ei.setAddress(address);

        d = localTransport.getDestination(ei, bus);
    }

    @Test
    public void testResolveOperationWithSource() throws Exception {
        ServiceImpl service = 
            new ServiceImpl(getBus(), getClass().getResource(wsdlResource), serviceName, null);

        Dispatch<Source> disp = service.createDispatch(
                portName, Source.class, Service.Mode.PAYLOAD);
        disp.getRequestContext().put(MessageContext.WSDL_OPERATION, operationName);
        disp.getRequestContext().put(Dispatch.ENDPOINT_ADDRESS_PROPERTY, address);
        
        d.setMessageObserver(new MessageReplayObserver(responseResource));

        BindingOperationVerifier bov = new BindingOperationVerifier();
        ((DispatchImpl<?>)disp).getClient().getOutInterceptors().add(bov);

        Document doc = StaxUtils.read(getResourceAsStream(requestResource));
        DOMSource source = new DOMSource(doc);
        Source res = disp.invoke(source);
        assertNotNull(res);

        BindingOperationInfo boi = bov.getBindingOperationInfo();
        assertNotNull(boi);

        assertEquals(operationName, boi.getName());
    }
    
    @Test
    public void testResolveOperationWithSourceAndWSA() throws Exception {
        ServiceImpl service = 
            new ServiceImpl(getBus(), getClass().getResource(wsdlResource),
                    serviceName, null, new AddressingFeature());

        Dispatch<Source> disp = service.createDispatch(
                portName, Source.class, Service.Mode.PAYLOAD);
        disp.getRequestContext().put(MessageContext.WSDL_OPERATION, operationName);
        disp.getRequestContext().put(Dispatch.ENDPOINT_ADDRESS_PROPERTY, address);
        
        d.setMessageObserver(new MessageReplayObserver(responseResource));

        BindingOperationVerifier bov = new BindingOperationVerifier();
        ((DispatchImpl<?>)disp).getClient().getOutInterceptors().add(bov);

        Document doc = StaxUtils.read(getResourceAsStream(requestResource));
        DOMSource source = new DOMSource(doc);
        Source res = disp.invoke(source);
        assertNotNull(res);

        BindingOperationInfo boi = bov.getBindingOperationInfo();
        assertNotNull(boi);

        assertEquals(operationName, boi.getName());
    }
    
    private static class BindingOperationVerifier extends AbstractSoapInterceptor {
        BindingOperationInfo boi;
        BindingOperationVerifier() {
            super(Phase.POST_LOGICAL);
        }
        
        public void handleMessage(SoapMessage message) throws Fault {
            boi = message.getExchange().getBindingOperationInfo();
        }
        
        public BindingOperationInfo getBindingOperationInfo() {
            return boi;
        }
    }
}

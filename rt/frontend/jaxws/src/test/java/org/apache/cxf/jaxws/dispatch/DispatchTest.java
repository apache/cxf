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

import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Document;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.jaxws.AbstractJaxWsTest;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.jaxws.MessageReplayObserver;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.hello_world_soap_http.SOAPService;
import org.apache.hello_world_soap_http.types.SayHi;
import org.apache.hello_world_soap_http.types.SayHiResponse;
import org.junit.Before;
import org.junit.Test;

public class DispatchTest extends AbstractJaxWsTest {
    private final QName serviceName = new QName("http://apache.org/hello_world_soap_http", "SOAPService");

    private final QName portName = new QName("http://apache.org/hello_world_soap_http", "SoapPort");

    private final String address = "http://localhost:9000/SoapContext/SoapPort";

    private Destination d;

    @Before
    public void setUp() throws Exception {
        EndpointInfo ei = new EndpointInfo(null, "http://schemas.xmlsoap.org/soap/http");
        ei.setAddress(address);

        d = localTransport.getDestination(ei);
    }

    @Test
    public void testJAXB() throws Exception {
        d.setMessageObserver(new MessageReplayObserver("/org/apache/cxf/jaxws/sayHiResponse.xml"));

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);

        JAXBContext jc = JAXBContext.newInstance("org.apache.hello_world_soap_http.types");
        Dispatch<Object> disp = service.createDispatch(portName, jc, Service.Mode.PAYLOAD);

        SayHi s = new SayHi();

        Object response = disp.invoke(s);
        assertNotNull(response);
        assertTrue(response instanceof SayHiResponse);
    }

    @Test
    public void testDOMSource() throws Exception {
        ServiceImpl service = 
            new ServiceImpl(getBus(), getClass().getResource("/wsdl/hello_world.wsdl"), serviceName, null);

        Dispatch<Source> disp = service.createDispatch(portName, Source.class, Service.Mode.MESSAGE);
        disp.getRequestContext().put(Dispatch.ENDPOINT_ADDRESS_PROPERTY, address);

        d.setMessageObserver(new MessageReplayObserver("/org/apache/cxf/jaxws/sayHiResponse.xml"));

        Document doc = DOMUtils.readXml(getResourceAsStream("/org/apache/cxf/jaxws/sayHi2.xml"));
        DOMSource source = new DOMSource(doc);
        Source res = disp.invoke(source);
        assertNotNull(res);

    }

    @Test
    public void testHTTPBinding() throws Exception {
        ServiceImpl service = new ServiceImpl(getBus(), null, serviceName, null);
        service.addPort(portName, HTTPBinding.HTTP_BINDING, "local://foobar");
        Dispatch<Source> disp = service.createDispatch(portName, Source.class, Service.Mode.MESSAGE);
        assertTrue(disp.getBinding() instanceof HTTPBinding);
    }

    @Test
    public void testSOAPPBinding() throws Exception {
        ServiceImpl service = new ServiceImpl(getBus(), null, serviceName, null);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, "local://foobar");
        Dispatch<Source> disp = service.createDispatch(portName, Source.class, Service.Mode.MESSAGE);
        assertTrue(disp.getBinding() instanceof SOAPBinding);
    }

    @Test
    public void testSOAPPBindingNullMessage() throws Exception {
        d.setMessageObserver(new MessageReplayObserver("/org/apache/cxf/jaxws/sayHiResponse.xml"));

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);

        JAXBContext jc = JAXBContext.newInstance("org.apache.hello_world_soap_http.types");
        Dispatch<Object> disp = service.createDispatch(portName, jc, Service.Mode.PAYLOAD);
        try {
            // Send a null message
            disp.invoke(null);
        } catch (SOAPFaultException e) {
            //Passed
            return;
        }
        
        fail("SOAPFaultException was not thrown");
    }
    
    @Test
    // CXF-2822
    public void testInterceptorsConfiguration() throws Exception {
        String cfgFile = "org/apache/cxf/jaxws/dispatch/bus-dispatch.xml";
        Bus bus = new SpringBusFactory().createBus(cfgFile, true);
        ServiceImpl service = new ServiceImpl(bus, getClass().getResource("/wsdl/hello_world.wsdl"),
                                              serviceName, null);

        Dispatch<Source> disp = service.createDispatch(portName, Source.class, Service.Mode.MESSAGE);
        List<Interceptor<? extends Message>> interceptors = ((DispatchImpl)disp).getClient()
            .getInInterceptors();
        boolean exists = false;
        for (Interceptor<? extends Message> interceptor : interceptors) {
            if (interceptor instanceof LoggingInInterceptor) {
                exists = true;
            }
        }
        assertTrue("The LoggingInInterceptor is not configured to dispatch client", exists);
    }

    @Test
    public void testFindOperationWithSource() throws Exception {
        ServiceImpl service = 
            new ServiceImpl(getBus(), getClass().getResource("/wsdl/hello_world.wsdl"), serviceName, null);

        Dispatch<Source> disp = service.createDispatch(portName, Source.class, Service.Mode.MESSAGE);
        disp.getRequestContext().put(Dispatch.ENDPOINT_ADDRESS_PROPERTY, address);
        disp.getRequestContext().put("find.dispatch.operation", Boolean.TRUE);
        
        d.setMessageObserver(new MessageReplayObserver("/org/apache/cxf/jaxws/sayHiResponse.xml"));

        BindingOperationVerifier bov = new BindingOperationVerifier();
        ((DispatchImpl)disp).getClient().getOutInterceptors().add(bov);

        Document doc = DOMUtils.readXml(getResourceAsStream("/org/apache/cxf/jaxws/sayHi2.xml"));
        DOMSource source = new DOMSource(doc);
        Source res = disp.invoke(source);
        assertNotNull(res);

        BindingOperationInfo boi = bov.getBindingOperationInfo();
        assertNotNull(boi);
        BindingOperationInfo dboi = (BindingOperationInfo)boi.getProperty("dispatchToOperation");
        assertNotNull(dboi);

        assertEquals(new QName("http://apache.org/hello_world_soap_http", "sayHi"), dboi.getName());
    }
    
    private static class BindingOperationVerifier extends AbstractSoapInterceptor {
        BindingOperationInfo boi;
        public BindingOperationVerifier() {
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

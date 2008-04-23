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

package org.apache.cxf.jaxb;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.apache.hello_world_soap_http.types.GreetMe;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createNiceControl;

public class TestBase extends Assert {

    PhaseInterceptorChain chain;
    MessageImpl message;
    Bus bus;
    ServiceInfo serviceInfo;
    BindingInfo bindingInfo;
    Service service;
    EndpointInfo endpointInfo;
    EndpointImpl endpoint;
    BindingOperationInfo operation;

    @Before
    public void setUp() throws Exception {
        bus = BusFactory.newInstance().createBus();

        BindingFactoryManager bfm = bus.getExtension(BindingFactoryManager.class);

        IMocksControl control = createNiceControl();
        BindingFactory bf = control.createMock(BindingFactory.class);
        Binding binding = control.createMock(Binding.class);
        expect(bf.createBinding(null)).andStubReturn(binding);
        expect(binding.getInFaultInterceptors()).andStubReturn(new ArrayList<Interceptor>());
        expect(binding.getOutFaultInterceptors()).andStubReturn(new ArrayList<Interceptor>());
        
        bfm.registerBindingFactory("http://schemas.xmlsoap.org/wsdl/soap/", bf);

        String ns = "http://apache.org/hello_world_soap_http";
        WSDLServiceFactory factory = new WSDLServiceFactory(bus, getClass()
            .getResource("/org/apache/cxf/jaxb/resources/wsdl/hello_world.wsdl"),
                                                            new QName(ns, "SOAPService"));

        service = factory.create();
        endpointInfo = service.getEndpointInfo(new QName(ns, "SoapPort"));
        endpoint = new EndpointImpl(bus, service, endpointInfo);
        JAXBDataBinding db = new JAXBDataBinding();
        db.setContext(JAXBContext.newInstance(new Class[] {
            GreetMe.class,
            GreetMeResponse.class
        }));
        service.setDataBinding(db);

        operation = endpointInfo.getBinding().getOperation(new QName(ns, "greetMe"));
        operation.getOperationInfo().getInput().getMessagePartByIndex(0).setTypeClass(GreetMe.class);
        operation.getOperationInfo().getOutput()
            .getMessagePartByIndex(0).setTypeClass(GreetMeResponse.class);

        message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);

        exchange.put(Service.class, service);
        exchange.put(Endpoint.class, endpoint);
        exchange.put(Binding.class, endpoint.getBinding());
    }

    @After
    public void tearDown() throws Exception {
    }

    public InputStream getTestStream(Class<?> clz, String file) {
        return clz.getResourceAsStream(file);
    }

    public XMLStreamReader getXMLStreamReader(InputStream is) {
        return StaxUtils.createXMLStreamReader(is);
    }

    public XMLStreamWriter getXMLStreamWriter(OutputStream os) {
        return StaxUtils.createXMLStreamWriter(os);
    }

    public Method getTestMethod(Class<?> sei, String methodName) {
        Method[] iMethods = sei.getMethods();
        for (Method m : iMethods) {
            if (methodName.equals(m.getName())) {
                return m;
            }
        }
        return null;
    }
}

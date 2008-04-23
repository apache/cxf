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

package org.apache.cxf.binding.xml.interceptor;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.xml.XMLBindingFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLManagerImpl;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class TestBase extends Assert {

    protected PhaseInterceptorChain chain;

    protected Message xmlMessage;
    
    protected Bus bus;

    protected IMocksControl control;
    
    protected ServiceInfo serviceInfo;
    
    @Before
    public void setUp() throws Exception {
        SortedSet<Phase> phases = new TreeSet<Phase>();
        Phase phase1 = new Phase("phase1", 1);
        Phase phase2 = new Phase("phase2", 2);
        Phase phase3 = new Phase("phase3", 3);
        phases.add(phase1);
        phases.add(phase2);
        phases.add(phase3);
        chain = new PhaseInterceptorChain(phases);

        Exchange exchange = new ExchangeImpl();
        MessageImpl messageImpl = new MessageImpl();
        messageImpl.setInterceptorChain(chain);
        messageImpl.setExchange(exchange);
        xmlMessage = messageImpl;
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
    
    protected void common(String wsdl, QName portName, Class... jaxbClasses) throws Exception {
        control = EasyMock.createNiceControl();
        
        bus = control.createMock(Bus.class);
        EasyMock.expect(bus.getExtension(WSDLManager.class)).andStubReturn(new WSDLManagerImpl());
        
        BindingFactoryManager bindingFactoryManager = control.createMock(BindingFactoryManager.class);
        EasyMock.expect(bus.getExtension(BindingFactoryManager.class)).andStubReturn(bindingFactoryManager);
        DestinationFactoryManager dfm = control.createMock(DestinationFactoryManager.class);
        EasyMock.expect(bus.getExtension(DestinationFactoryManager.class)).andStubReturn(dfm);
        
        control.replay();        
        
        assertNotNull(bus.getExtension(WSDLManager.class));
        
        WSDLServiceFactory factory = 
            new WSDLServiceFactory(bus, getClass().getResource(wsdl),
                                   new QName(portName.getNamespaceURI(), "XMLService"));

        org.apache.cxf.service.Service service = factory.create();

        EndpointInfo epi = service.getEndpointInfo(portName);
        serviceInfo = epi.getService();
        assertNotNull(epi);
        Binding xmlBinding = new XMLBindingFactory().createBinding(epi.getBinding());

        control.reset();
        JAXBDataBinding db = new JAXBDataBinding();
        db.initialize(service);
        db.setContext(JAXBContext.newInstance(jaxbClasses));
        service.setDataBinding(db);

        Endpoint endpoint = control.createMock(EndpointImpl.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andStubReturn(epi);
        EasyMock.expect(endpoint.getBinding()).andStubReturn(xmlBinding);
        EasyMock.expect(endpoint.getService()).andStubReturn(service);

        control.replay();

        xmlMessage.getExchange().put(Endpoint.class, endpoint);
        xmlMessage.getExchange().put(org.apache.cxf.service.Service.class, service);
        

    }
}

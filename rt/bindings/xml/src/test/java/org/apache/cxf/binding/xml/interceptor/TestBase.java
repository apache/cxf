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

import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.xml.wsdl11.XMLWSDLExtensionLoader;
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
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLManagerImpl;
import org.apache.cxf.wsdl11.WSDLServiceFactory;

import org.junit.Before;

import static org.junit.Assert.assertNotNull;

public class TestBase {

    protected PhaseInterceptorChain chain;

    protected Message xmlMessage;

    protected ServiceInfo serviceInfo;

    @Before
    public void setUp() throws Exception {
        SortedSet<Phase> phases = new TreeSet<>();
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

    protected void common(String wsdl, QName portName, Class<?>... jaxbClasses) throws Exception {
        Bus bus = BusFactory.getDefaultBus();

        WSDLManagerImpl manager = new WSDLManagerImpl();
        XMLWSDLExtensionLoader loader = new XMLWSDLExtensionLoader(bus);
        loader.registerExtensors(manager);

        assertNotNull(bus.getExtension(WSDLManager.class));

        WSDLServiceFactory factory =
            new WSDLServiceFactory(bus, getClass().getResource(wsdl).toString(),
                                   new QName(portName.getNamespaceURI(), "XMLService"));

        org.apache.cxf.service.Service service = factory.create();

        EndpointInfo epi = service.getEndpointInfo(portName);
        assertNotNull(epi);
        serviceInfo = epi.getService();

        JAXBDataBinding db = new JAXBDataBinding();
        db.initialize(service);
        db.setContext(JAXBContext.newInstance(jaxbClasses));
        service.setDataBinding(db);

        Endpoint endpoint = new EndpointImpl(bus, service, epi);

        xmlMessage.getExchange().put(Endpoint.class, endpoint);
        xmlMessage.getExchange().put(org.apache.cxf.service.Service.class, service);
    }
}
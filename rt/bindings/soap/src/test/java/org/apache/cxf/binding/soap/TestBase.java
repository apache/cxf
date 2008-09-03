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

package org.apache.cxf.binding.soap;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.wsdl.Definition;
import javax.wsdl.Service;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class TestBase extends Assert {

    protected PhaseInterceptorChain chain;
    protected SoapMessage soapMessage;

    
    

    @Before
    public void setUp() throws Exception {
        SortedSet<Phase> phases = new TreeSet<Phase>();
        Phase phase1 = new Phase("phase1", 1);
        Phase phase2 = new Phase("phase2", 2);
        Phase phase3 = new Phase("phase3", 3);
        Phase phase4 = new Phase(Phase.WRITE_ENDING, 4);
        phases.add(phase1);
        phases.add(phase2);
        phases.add(phase3);
        phases.add(phase4);
        phases.add(new Phase(Phase.INVOKE, 5));
        chain = new PhaseInterceptorChain(phases);

        soapMessage = TestUtil.createEmptySoapMessage(Soap11.getInstance(), chain);
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

    public ServiceInfo getTestService(Class<?> clz) {
        // FIXME?!?!?!?? There should NOT be JAX-WS stuff here
        return null;
    }

    protected BindingInfo getTestService(String wsdlUrl, String port) throws Exception {
        ServiceInfo service = getMockedServiceModel(getClass().getResource(wsdlUrl).toString());
        assertNotNull(service);
        BindingInfo binding = service.getEndpoint(new QName(service.getName().getNamespaceURI(), port))
            .getBinding();
        assertNotNull(binding);
        return binding;
    }

    protected ServiceInfo getMockedServiceModel(String wsdlUrl) throws Exception {
        WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        Definition def = wsdlReader.readWSDL(wsdlUrl);

        IMocksControl control = EasyMock.createNiceControl();
        Bus bus = control.createMock(Bus.class);
        BindingFactoryManager bindingFactoryManager = control.createMock(BindingFactoryManager.class);
        DestinationFactoryManager dfm = control.createMock(DestinationFactoryManager.class);
        WSDLServiceBuilder wsdlServiceBuilder = new WSDLServiceBuilder(bus);

        Service service = null;
        for (Iterator<?> it = def.getServices().values().iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof Service) {
                service = (Service)obj;
                break;
            }
        }

        EasyMock.expect(bus.getExtension(BindingFactoryManager.class)).andReturn(bindingFactoryManager);
        EasyMock.expect(bus.getExtension(DestinationFactoryManager.class)).andStubReturn(dfm);
        control.replay();

        ServiceInfo serviceInfo = wsdlServiceBuilder.buildServices(def, service).get(0);
        serviceInfo.setProperty(WSDLServiceBuilder.WSDL_DEFINITION, null);
        serviceInfo.setProperty(WSDLServiceBuilder.WSDL_SERVICE, null);
        return serviceInfo;
    }
}

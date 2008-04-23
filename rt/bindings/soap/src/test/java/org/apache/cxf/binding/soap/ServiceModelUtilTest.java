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


import javax.wsdl.Definition;
import javax.wsdl.Service;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;


import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.expect;

public class ServiceModelUtilTest extends Assert {
    private static final String WSDL_PATH = "test-soap-header.wsdl";
    private Definition def;
    private Service service;
    private ServiceInfo serviceInfo;

    private IMocksControl control;
    private Bus bus;
    private BindingFactoryManager bindingFactoryManager;
    
    @Before
    public void setUp() throws Exception {
        String wsdlUrl = getClass().getResource(WSDL_PATH).toString();
        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        def = wsdlReader.readWSDL(wsdlUrl);

        WSDLServiceBuilder wsdlServiceBuilder = new WSDLServiceBuilder(bus);
        for (Service serv : CastUtils.cast(def.getServices().values(), Service.class)) {
            if (serv != null) {
                service = serv;
                break;
            }
        }

        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
        bindingFactoryManager = control.createMock(BindingFactoryManager.class);
        wsdlServiceBuilder = new WSDLServiceBuilder(bus);

        EasyMock.expect(bus.getExtension(BindingFactoryManager.class)).andReturn(bindingFactoryManager);

        DestinationFactoryManager dfm = control.createMock(DestinationFactoryManager.class);
        expect(bus.getExtension(DestinationFactoryManager.class)).andStubReturn(dfm);

        control.replay();
        serviceInfo = wsdlServiceBuilder.buildServices(def, service).get(0);
    }
    
    @After
    public void tearDown() throws Exception {
        
    }
    
    @Test
    public void testGetSchema() throws Exception {
        BindingInfo bindingInfo = null;
        bindingInfo = serviceInfo.getBindings().iterator().next();
        QName name = new QName(serviceInfo.getName().getNamespaceURI(), "inHeader");
        BindingOperationInfo inHeader = bindingInfo.getOperation(name);
        BindingMessageInfo input = inHeader.getInput();
        assertNotNull(input);
        assertEquals(input.getMessageInfo().getName().getLocalPart(), "inHeaderRequest");
        assertEquals(input.getMessageInfo().getName().getNamespaceURI(),
                     "http://org.apache.cxf/headers");
        assertEquals(input.getMessageInfo().getMessageParts().size(), 2);
        assertTrue(input.getMessageInfo().getMessageParts().get(0).isElement());
        assertEquals(
            input.getMessageInfo().getMessageParts().get(0).getElementQName().getLocalPart(), "inHeader");
        assertEquals(input.getMessageInfo().getMessageParts().get(0).getElementQName().getNamespaceURI(),
                     "http://org.apache.cxf/headers");
        
        assertTrue(input.getMessageInfo().getMessageParts().get(0).isElement());
        assertEquals(
            input.getMessageInfo().getMessageParts().get(1).getElementQName().getLocalPart(), "passenger");
        assertEquals(input.getMessageInfo().getMessageParts().get(1).getElementQName().getNamespaceURI(),
                     "http://mycompany.example.com/employees");
        assertTrue(input.getMessageInfo().getMessageParts().get(1).isElement());
        
        MessagePartInfo messagePartInfo = input.getMessageInfo().getMessageParts().get(0);
        SchemaInfo schemaInfo = ServiceModelUtil.getSchema(serviceInfo, messagePartInfo);
        assertEquals(schemaInfo.getNamespaceURI(), "http://org.apache.cxf/headers");
        
        messagePartInfo = input.getMessageInfo().getMessageParts().get(1);
        schemaInfo = ServiceModelUtil.getSchema(serviceInfo, messagePartInfo);
        assertEquals(schemaInfo.getNamespaceURI(), "http://mycompany.example.com/employees");
    }
}

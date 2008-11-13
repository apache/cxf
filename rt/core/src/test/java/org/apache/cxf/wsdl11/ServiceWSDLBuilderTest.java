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

package org.apache.cxf.wsdl11;

import java.util.Collection;
import java.util.logging.Logger;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class ServiceWSDLBuilderTest extends Assert {

    private static final Logger LOG = LogUtils.getLogger(ServiceWSDLBuilderTest.class);
    private static final String WSDL_PATH = "hello_world.wsdl";
    private static final String NO_BODY_PARTS_WSDL_PATH = "no_body_parts.wsdl";

    private Definition def;
    private Definition newDef;
    private Service service;

    private WSDLServiceBuilder wsdlServiceBuilder;
    private ServiceInfo serviceInfo;
    
    private IMocksControl control;
    private Bus bus;
    private BindingFactoryManager bindingFactoryManager;
    private DestinationFactoryManager destinationFactoryManager;
    private DestinationFactory destinationFactory;
    
    private void setupWSDL(String wsdlPath) throws Exception {
        String wsdlUrl = getClass().getResource(wsdlPath).toString();
        LOG.info("the path of wsdl file is " + wsdlUrl);
        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        def = wsdlReader.readWSDL(wsdlUrl);
        
        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
        bindingFactoryManager = control.createMock(BindingFactoryManager.class);
        destinationFactoryManager = control.createMock(DestinationFactoryManager.class);
        destinationFactory = control.createMock(DestinationFactory.class);
        wsdlServiceBuilder = new WSDLServiceBuilder(bus, false);

        for (Service serv : CastUtils.cast(def.getServices().values(), Service.class)) {
            if (serv != null) {
                service = serv;
                break;
            }
        }
        EasyMock.expect(bus.getExtension(WSDLManager.class)).andReturn(new WSDLManagerImpl()).anyTimes();
        
        EasyMock.expect(bus.getExtension(BindingFactoryManager.class)).andReturn(bindingFactoryManager);
        EasyMock.expect(bus.getExtension(DestinationFactoryManager.class))
            .andReturn(destinationFactoryManager);
        
        EasyMock.expect(destinationFactoryManager
                        .getDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/"))
            .andReturn(destinationFactory);

        control.replay();
        
        serviceInfo = wsdlServiceBuilder.buildServices(def, service).get(0);
        newDef = new ServiceWSDLBuilder(bus, serviceInfo).build();
    }
    
    @After
    public void tearDown() throws Exception {        
        control.verify();
        newDef = null;
    }
    
    @Test
    public void testNoBodyParts() throws Exception {
        setupWSDL(NO_BODY_PARTS_WSDL_PATH);
        QName messageName = new QName("urn:org:apache:cxf:no_body_parts/wsdl",
            "operation1Request");
        Message message = newDef.getMessage(messageName);
        Part part = message.getPart("mimeAttachment");
        assertNotNull(part.getTypeName());
    }
    
    @Test    
    public void testDefinition() throws Exception {
        setupWSDL(WSDL_PATH);
        assertEquals(newDef.getTargetNamespace(), "http://apache.org/hello_world_soap_http");
        Service serv = newDef.getService(new QName("http://apache.org/hello_world_soap_http",
                                                   "SOAPService"));
        assertNotNull(serv);
        assertNotNull(serv.getPort("SoapPort"));
    }
    
    @Test
    public void testPortType() throws Exception {
        setupWSDL(WSDL_PATH);
        assertEquals(1, newDef.getPortTypes().size());
        PortType portType = (PortType)newDef.getPortTypes().values().iterator().next();
        assertNotNull(portType);
        assertTrue(portType.getQName().equals(new QName(newDef.getTargetNamespace(), "Greeter")));
        
    }
    
    @Test
    public void testSayHiOperation() throws Exception {
        setupWSDL(WSDL_PATH);
        PortType portType = newDef.getPortType(new QName(newDef.getTargetNamespace(), 
            "Greeter"));
        Collection<Operation> operations =  
            CastUtils.cast(
                portType.getOperations(), Operation.class);
        
        assertEquals(4, operations.size());
        Operation sayHi = portType.getOperation("sayHi", "sayHiRequest", "sayHiResponse");
        assertNotNull(sayHi);
        assertEquals(sayHi.getName(), "sayHi");
        Input input = sayHi.getInput();
        assertNotNull(input);
        assertEquals("sayHiRequest", input.getName());
        Message message = input.getMessage();
        assertNotNull(message);
        assertEquals("sayHiRequest", message.getQName().getLocalPart());
        assertEquals(newDef.getTargetNamespace(), message.getQName().getNamespaceURI());
        assertEquals(1, message.getParts().size());
        assertEquals("in", message.getPart("in").getName());
        Output output = sayHi.getOutput();
        assertNotNull(output);
        assertEquals("sayHiResponse", output.getName());
        message = output.getMessage();
        assertNotNull(message);
        assertEquals("sayHiResponse", message.getQName().getLocalPart());
        assertEquals(newDef.getTargetNamespace(), message.getQName().getNamespaceURI());
        assertEquals(1, message.getParts().size());
        assertEquals("out", message.getPart("out").getName());
        assertEquals(0, sayHi.getFaults().size());
              
    }
    
    @Test
    public void testGreetMeOperation() throws Exception {
        setupWSDL(WSDL_PATH);
        PortType portType = newDef.getPortType(new QName(newDef.getTargetNamespace(), 
            "Greeter"));
        Operation greetMe = portType.getOperation("greetMe", "greetMeRequest", "greetMeResponse");
        assertNotNull(greetMe);
        assertEquals("greetMe", greetMe.getName());
        Input input = greetMe.getInput();
        assertNotNull(input);
        assertEquals("greetMeRequest", input.getName());
        Message message = input.getMessage();
        assertNotNull(message);
        assertEquals("greetMeRequest", message.getQName().getLocalPart());
        assertEquals(newDef.getTargetNamespace(), message.getQName().getNamespaceURI());
        assertEquals(1, message.getParts().size());
        assertEquals("in", message.getPart("in").getName());
        Output output = greetMe.getOutput();
        assertNotNull(output);
        assertEquals("greetMeResponse", output.getName());
        message = output.getMessage();
        assertNotNull(message);
        assertEquals("greetMeResponse", message.getQName().getLocalPart());
        assertEquals(newDef.getTargetNamespace(), message.getQName().getNamespaceURI());
        assertEquals(1, message.getParts().size());
        assertEquals("out", message.getPart("out").getName());
        assertEquals(0, greetMe.getFaults().size());
        
    }
    
    @Test
    public void testGreetMeOneWayOperation() throws Exception {
        setupWSDL(WSDL_PATH);
        PortType portType = newDef.getPortType(new QName(newDef.getTargetNamespace(), 
            "Greeter"));
        Operation greetMeOneWay = portType.getOperation("greetMeOneWay", "greetMeOneWayRequest", null);
        assertNotNull(greetMeOneWay);
        assertEquals("greetMeOneWay", greetMeOneWay.getName());
        Input input = greetMeOneWay.getInput();
        assertNotNull(input);
        assertEquals("greetMeOneWayRequest", input.getName());
        Message message = input.getMessage();
        assertNotNull(message);
        assertEquals("greetMeOneWayRequest", message.getQName().getLocalPart());
        assertEquals(newDef.getTargetNamespace(), message.getQName().getNamespaceURI());
        assertEquals(1, message.getParts().size());
        assertEquals("in", message.getPart("in").getName());
        Output output = greetMeOneWay.getOutput();
        assertNull(output);
        assertEquals(0, greetMeOneWay.getFaults().size());
    }
    
    @Test
    public void testPingMeOperation() throws Exception {
        setupWSDL(WSDL_PATH);
        PortType portType = newDef.getPortType(new QName(newDef.getTargetNamespace(), 
            "Greeter"));
        Operation pingMe = portType.getOperation("pingMe", "pingMeRequest", "pingMeResponse");
        assertNotNull(pingMe);
        assertEquals("pingMe", pingMe.getName());
        Input input = pingMe.getInput();
        assertNotNull(input);
        assertEquals("pingMeRequest", input.getName());
        Message message = input.getMessage();
        assertNotNull(message);
        assertEquals("pingMeRequest", message.getQName().getLocalPart());
        assertEquals(newDef.getTargetNamespace(), message.getQName().getNamespaceURI());
        assertEquals(1, message.getParts().size());
        assertEquals("in", message.getPart("in").getName());
        Output output = pingMe.getOutput();
        assertNotNull(output);
        assertEquals("pingMeResponse", output.getName());
        message = output.getMessage();
        assertNotNull(message);
        assertEquals("pingMeResponse", message.getQName().getLocalPart());
        assertEquals(newDef.getTargetNamespace(), message.getQName().getNamespaceURI());
        assertEquals(message.getParts().size(), 1);
        assertEquals("out", message.getPart("out").getName());
        assertEquals(1, pingMe.getFaults().size());
        Fault fault = pingMe.getFault("pingMeFault");
        assertNotNull(fault);
        assertEquals("pingMeFault", fault.getName());
        message = fault.getMessage();
        assertNotNull(message);
        assertEquals("pingMeFault", message.getQName().getLocalPart());
        assertEquals(newDef.getTargetNamespace(), message.getQName().getNamespaceURI());
        assertEquals(1, message.getParts().size());
        assertEquals("faultDetail", message.getPart("faultDetail").getName());
        assertNull(message.getPart("faultDetail").getTypeName());
    }
    
    @Test
    public void testBinding() throws Exception {
        setupWSDL(WSDL_PATH);
        assertEquals(newDef.getBindings().size(), 1);
        Binding binding = newDef.getBinding(new QName(newDef.getTargetNamespace(), "Greeter_SOAPBinding"));
        assertNotNull(binding);
        assertEquals(4, binding.getBindingOperations().size());
    }
    
    @Test
    public void testSchemas() throws Exception {
        setupWSDL(WSDL_PATH);
        Types types = newDef.getTypes();
        assertNotNull(types);
        Collection<ExtensibilityElement> schemas = 
            CastUtils.cast(types.getExtensibilityElements(), ExtensibilityElement.class);
        assertEquals(1, schemas.size());
        XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
        Element schemaElem = ((Schema)schemas.iterator().next()).getElement();
        assertEquals("http://apache.org/hello_world_soap_http/types",
                     schemaCollection.read(schemaElem).getTargetNamespace() 
                     );
    }
    
}

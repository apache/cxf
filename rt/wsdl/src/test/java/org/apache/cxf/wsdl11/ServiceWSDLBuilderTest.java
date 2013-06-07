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

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class ServiceWSDLBuilderTest extends Assert {

    private static final Logger LOG = LogUtils.getLogger(ServiceWSDLBuilderTest.class);
    private static final String WSDL_PATH = "hello_world.wsdl";
    private static final String NO_BODY_PARTS_WSDL_PATH = "no_body_parts.wsdl";
    private static final String WSDL_XSD_IMPORT_PATH = "hello_world_schema_import_test.wsdl";

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
        setupWSDL(wsdlPath, false);
    }
    
    private void setupWSDL(String wsdlPath, boolean doXsdImports) throws Exception {
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
        ServiceWSDLBuilder builder = new ServiceWSDLBuilder(bus, serviceInfo);
        builder.setUseSchemaImports(doXsdImports);
        builder.setBaseFileName("HelloWorld");
        newDef = builder.build(new HashMap<String, SchemaInfo>());
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
    public void testBindingWithDifferentNamespaceImport() throws Exception {
        setupWSDL("wsdl2/person.wsdl");
        assertEquals(newDef.getBindings().size(), 1);
        assertTrue(newDef.getNamespace("ns3").equals("http://cxf.apache.org/samples/wsdl-first"));
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
        XmlSchema newSchema = schemaCollection.read(schemaElem); 
        assertEquals("http://apache.org/hello_world_soap_http/types",
                     newSchema.getTargetNamespace() 
                     );
    }
    
    @Test
    public void testXsdImportMultipleSchemas() throws Exception {
        setupWSDL(WSDL_XSD_IMPORT_PATH, true);

        Types types = newDef.getTypes();
        assertNotNull(types);

        Collection<ExtensibilityElement> schemas = CastUtils.cast(types.getExtensibilityElements(),
                                                                  ExtensibilityElement.class);
        assertEquals(1, schemas.size());

        Schema schema = (Schema)schemas.iterator().next();

        assertEquals(1, schema.getImports().values().size());

        SchemaImport serviceTypesSchemaImport = getImport(schema.getImports(),
                "http://apache.org/hello_world_soap_http/servicetypes");

        Schema serviceTypesSchema = serviceTypesSchemaImport.getReferencedSchema();

        assertEquals(1, serviceTypesSchema.getImports().values().size());
        SchemaImport typesSchemaImport = getImport(serviceTypesSchema.getImports(),
                "http://apache.org/hello_world_soap_http/types");

        Schema typesSchema = typesSchemaImport.getReferencedSchema();
        
        Document doc = typesSchema.getElement().getOwnerDocument();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(outputStream, "utf-8");
        StaxUtils.writeNode(doc, writer, true);
        writer.close();

        // this is a test to make sure any embedded namespaces are properly included
        String savedSchema = new String(outputStream.toByteArray(), "UTF-8");
        assertTrue(savedSchema.contains("http://www.w3.org/2005/05/xmlmime"));
        
        SchemaImport types2SchemaImport = getImport(typesSchema.getImports(),
                "http://apache.org/hello_world_soap_http/types2");
        
        Schema types2Schema = types2SchemaImport.getReferencedSchema();
        assertNotNull(types2Schema);
    }
    
    private SchemaImport getImport(Map<?, ?> imps, String key) {
        List<SchemaImport> s1 = CastUtils.cast((List<?>)imps.get(key));
        return s1.get(0);
    }
    
}

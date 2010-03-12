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

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Service;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.catalog.CatalogWSDLLocator;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Test;

public class WSDLServiceBuilderTest extends Assert {
    // TODO: reuse the wsdl in testutils and add the parameter order into one of the wsdl
    private static final Logger LOG = LogUtils.getLogger(WSDLServiceBuilderTest.class);
    private static final String WSDL_PATH = "hello_world.wsdl";
    private static final String BARE_WSDL_PATH = "hello_world_bare.wsdl";
    private static final String IMPORT_WSDL_PATH = "hello_world_schema_import.wsdl";
    private static final String MULTIPORT_WSDL_PATH = "hello_world_multiporttype.wsdl";
    private static final String NO_BODY_PARTS_WSDL_PATH = "no_body_parts.wsdl";
    
    private static final String EXTENSION_NAMESPACE = "http://cxf.apache.org/extension/ns";
    private static final QName EXTENSION_ATTR_BOOLEAN = new QName(EXTENSION_NAMESPACE, "booleanAttr");
    private static final QName EXTENSION_ATTR_STRING = new QName(EXTENSION_NAMESPACE, "stringAttr");
    private static final QName EXTENSION_ELEM = new QName(EXTENSION_NAMESPACE, "stringElem");

    private Definition def;

    private Service service;

    private ServiceInfo serviceInfo;
    private List<ServiceInfo> serviceInfos;

    private IMocksControl control;

    private Bus bus;

    private BindingFactoryManager bindingFactoryManager;

    private DestinationFactoryManager destinationFactoryManager;

    public void setUpBasic() throws Exception {
        setUpWSDL(WSDL_PATH, 0);
    }

    private void setUpWSDL(String wsdl, int serviceSeq) throws Exception {
        URL url = getClass().getResource(wsdl);
        assertNotNull("could not find wsdl " + wsdl, url);
        String wsdlUrl = url.toString();
        LOG.info("the path of wsdl file is " + wsdlUrl);
        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        
        def = wsdlReader.readWSDL(new CatalogWSDLLocator(wsdlUrl));

        int seq = 0;
        for (Service serv : CastUtils.cast(def.getServices().values(), Service.class)) {
            if (serv != null) {
                service = serv;
                if (seq == serviceSeq) {
                    break;
                } else {
                    seq++;
                }
            }
        }

        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
        bindingFactoryManager = control.createMock(BindingFactoryManager.class);
        destinationFactoryManager = control.createMock(DestinationFactoryManager.class);
        DestinationFactory destinationFactory = control.createMock(DestinationFactory.class);

        WSDLServiceBuilder wsdlServiceBuilder = new WSDLServiceBuilder(bus);

        EasyMock.expect(bus.getExtension(BindingFactoryManager.class))
            .andReturn(bindingFactoryManager).anyTimes();
        
        EasyMock.expect(bus.getExtension(DestinationFactoryManager.class))
            .andReturn(destinationFactoryManager).atLeastOnce();
        
        EasyMock.expect(destinationFactoryManager
                        .getDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/"))
            .andReturn(destinationFactory).anyTimes();
        

        control.replay();
        serviceInfos = wsdlServiceBuilder.buildServices(def, service);
        serviceInfo = serviceInfos.get(0);

    }

    @Test
    public void testMultiPorttype() throws Exception {
        setUpWSDL(MULTIPORT_WSDL_PATH, 0);
        assertEquals(2, serviceInfos.size());
        control.verify();
    }

    @Test
    public void testServiceInfo() throws Exception {
        setUpBasic();
        assertEquals("SOAPService", serviceInfo.getName().getLocalPart());
        assertEquals("http://apache.org/hello_world_soap_http", serviceInfo.getName().getNamespaceURI());
        assertEquals("http://apache.org/hello_world_soap_http", serviceInfo.getTargetNamespace());
        assertTrue(serviceInfo.getProperty(WSDLServiceBuilder.WSDL_DEFINITION) == def);
        assertTrue(serviceInfo.getProperty(WSDLServiceBuilder.WSDL_SERVICE) == service);

        assertEquals("Incorrect number of endpoints", 1, serviceInfo.getEndpoints().size());
        EndpointInfo ei = serviceInfo.getEndpoint(new QName("http://apache.org/hello_world_soap_http",
                "SoapPort"));
        assertNotNull(ei);
        assertEquals("http://schemas.xmlsoap.org/wsdl/soap/", ei.getTransportId());
        assertNotNull(ei.getBinding());
        control.verify();
    }

    @Test
    public void testInterfaceInfo() throws Exception {
        setUpBasic();
        assertEquals("Greeter", serviceInfo.getInterface().getName().getLocalPart());
        control.verify();
    }

    @Test
    public void testOperationInfo() throws Exception {
        setUpBasic();
        QName name = new QName(serviceInfo.getName().getNamespaceURI(), "sayHi");
        assertEquals(4, serviceInfo.getInterface().getOperations().size());
        OperationInfo sayHi = serviceInfo.getInterface().getOperation(
                new QName(serviceInfo.getName().getNamespaceURI(), "sayHi"));
        assertNotNull(sayHi);
        assertEquals(sayHi.getName(), name);
        assertFalse(sayHi.isOneWay());
        assertTrue(sayHi.hasInput());
        assertTrue(sayHi.hasOutput());
        
        assertNull(sayHi.getParameterOrdering());

        name = new QName(serviceInfo.getName().getNamespaceURI(), "greetMe");
        OperationInfo greetMe = serviceInfo.getInterface().getOperation(name);
        assertNotNull(greetMe);
        assertEquals(greetMe.getName(), name);
        assertFalse(greetMe.isOneWay());
        assertTrue(greetMe.hasInput());
        assertTrue(greetMe.hasOutput());

        List<MessagePartInfo> inParts = greetMe.getInput().getMessageParts();
        assertEquals(1, inParts.size());
        MessagePartInfo part = inParts.get(0);
        assertNotNull(part.getXmlSchema());
        assertTrue(part.getXmlSchema() instanceof XmlSchemaElement);

        List<MessagePartInfo> outParts = greetMe.getOutput().getMessageParts();
        assertEquals(1, outParts.size());
        part = outParts.get(0);
        assertNotNull(part.getXmlSchema());
        assertTrue(part.getXmlSchema() instanceof XmlSchemaElement);

        assertTrue("greatMe should be wrapped", greetMe.isUnwrappedCapable());
        OperationInfo greetMeUnwrapped = greetMe.getUnwrappedOperation();

        assertNotNull(greetMeUnwrapped.getInput());
        assertNotNull(greetMeUnwrapped.getOutput());
        assertEquals("wrapped part not set", 1, greetMeUnwrapped.getInput().size());
        assertEquals("wrapped part not set", 1, greetMeUnwrapped.getOutput().size());
        assertEquals("wrapper part name wrong", "requestType", greetMeUnwrapped.getInput()
                .getMessagePartByIndex(0).getName().getLocalPart());
        assertEquals("wrapper part type name wrong", "MyStringType", greetMeUnwrapped.getInput()
                .getMessagePartByIndex(0).getTypeQName().getLocalPart());

        assertEquals("wrapper part name wrong", "responseType", greetMeUnwrapped.getOutput()
                .getMessagePartByIndex(0).getName().getLocalPart());
        assertEquals("wrapper part type name wrong", "string", greetMeUnwrapped.getOutput()
                .getMessagePartByIndex(0).getTypeQName().getLocalPart());

        name = new QName(serviceInfo.getName().getNamespaceURI(), "greetMeOneWay");
        OperationInfo greetMeOneWay = serviceInfo.getInterface().getOperation(name);
        assertNotNull(greetMeOneWay);
        assertEquals(greetMeOneWay.getName(), name);
        assertTrue(greetMeOneWay.isOneWay());
        assertTrue(greetMeOneWay.hasInput());
        assertFalse(greetMeOneWay.hasOutput());

        OperationInfo greetMeOneWayUnwrapped = greetMeOneWay.getUnwrappedOperation();
        assertNotNull(greetMeOneWayUnwrapped);
        assertNotNull(greetMeOneWayUnwrapped.getInput());
        assertNull(greetMeOneWayUnwrapped.getOutput());
        assertEquals("wrapped part not set", 1, greetMeOneWayUnwrapped.getInput().size());
        assertEquals(new QName("http://apache.org/hello_world_soap_http/types", "requestType"),
                     greetMeOneWayUnwrapped.getInput().getMessagePartByIndex(0).getConcreteName());

        name = new QName(serviceInfo.getName().getNamespaceURI(), "pingMe");
        OperationInfo pingMe = serviceInfo.getInterface().getOperation(name);
        assertNotNull(pingMe);
        assertEquals(pingMe.getName(), name);
        assertFalse(pingMe.isOneWay());
        assertTrue(pingMe.hasInput());
        assertTrue(pingMe.hasOutput());

        assertNull(serviceInfo.getInterface().getOperation(new QName("what ever")));
        control.verify();
    }

    @Test
    public void testBindingInfo() throws Exception {
        setUpBasic();
        BindingInfo bindingInfo = null;
        assertEquals(1, serviceInfo.getBindings().size());
        bindingInfo = serviceInfo.getBindings().iterator().next();
        assertNotNull(bindingInfo);
        assertEquals(bindingInfo.getInterface().getName().getLocalPart(), "Greeter");
        assertEquals(bindingInfo.getName().getLocalPart(), "Greeter_SOAPBinding");
        assertEquals(bindingInfo.getName().getNamespaceURI(), "http://apache.org/hello_world_soap_http");
        control.verify();
    }

    @Test
    public void testBindingOperationInfo() throws Exception {
        setUpBasic();
        BindingInfo bindingInfo = null;
        bindingInfo = serviceInfo.getBindings().iterator().next();
        Collection<BindingOperationInfo> bindingOperationInfos = bindingInfo.getOperations();
        assertNotNull(bindingOperationInfos);
        assertEquals(bindingOperationInfos.size(), 4);
        LOG.info("the binding operation is " + bindingOperationInfos.iterator().next().getName());

        QName name = new QName(serviceInfo.getName().getNamespaceURI(), "sayHi");
        BindingOperationInfo sayHi = bindingInfo.getOperation(name);
        assertNotNull(sayHi);
        assertEquals(sayHi.getName(), name);

        name = new QName(serviceInfo.getName().getNamespaceURI(), "greetMe");
        BindingOperationInfo greetMe = bindingInfo.getOperation(name);
        assertNotNull(greetMe);
        assertEquals(greetMe.getName(), name);

        name = new QName(serviceInfo.getName().getNamespaceURI(), "greetMeOneWay");
        BindingOperationInfo greetMeOneWay = bindingInfo.getOperation(name);
        assertNotNull(greetMeOneWay);
        assertEquals(greetMeOneWay.getName(), name);

        name = new QName(serviceInfo.getName().getNamespaceURI(), "pingMe");
        BindingOperationInfo pingMe = bindingInfo.getOperation(name);
        assertNotNull(pingMe);
        assertEquals(pingMe.getName(), name);
        control.verify();
    }

    @Test
    public void testBindingMessageInfo() throws Exception {
        setUpBasic();
        BindingInfo bindingInfo = null;
        bindingInfo = serviceInfo.getBindings().iterator().next();

        QName name = new QName(serviceInfo.getName().getNamespaceURI(), "sayHi");
        BindingOperationInfo sayHi = bindingInfo.getOperation(name);
        BindingMessageInfo input = sayHi.getInput();
        assertNotNull(input);
        assertEquals(input.getMessageInfo().getName().getLocalPart(), "sayHiRequest");
        assertEquals(input.getMessageInfo().getName().getNamespaceURI(),
                "http://apache.org/hello_world_soap_http");
        assertEquals(input.getMessageInfo().getMessageParts().size(), 1);
        assertEquals(input.getMessageInfo().getMessageParts().get(0).getName().getLocalPart(), "in");
        assertEquals(input.getMessageInfo().getMessageParts().get(0).getName().getNamespaceURI(),
                "http://apache.org/hello_world_soap_http");
        assertTrue(input.getMessageInfo().getMessageParts().get(0).isElement());
        QName elementName = input.getMessageInfo().getMessageParts().get(0).getElementQName();
        assertEquals(elementName.getLocalPart(), "sayHi");
        assertEquals(elementName.getNamespaceURI(), "http://apache.org/hello_world_soap_http/types");

        BindingMessageInfo output = sayHi.getOutput();
        assertNotNull(output);
        assertEquals(output.getMessageInfo().getName().getLocalPart(), "sayHiResponse");
        assertEquals(output.getMessageInfo().getName().getNamespaceURI(),
                "http://apache.org/hello_world_soap_http");
        assertEquals(output.getMessageInfo().getMessageParts().size(), 1);
        assertEquals(output.getMessageInfo().getMessageParts().get(0).getName().getLocalPart(), "out");
        assertEquals(output.getMessageInfo().getMessageParts().get(0).getName().getNamespaceURI(),
                "http://apache.org/hello_world_soap_http");
        assertTrue(output.getMessageInfo().getMessageParts().get(0).isElement());
        elementName = output.getMessageInfo().getMessageParts().get(0).getElementQName();
        assertEquals(elementName.getLocalPart(), "sayHiResponse");
        assertEquals(elementName.getNamespaceURI(), "http://apache.org/hello_world_soap_http/types");

        assertTrue(sayHi.getFaults().size() == 0);

        name = new QName(serviceInfo.getName().getNamespaceURI(), "pingMe");
        BindingOperationInfo pingMe = bindingInfo.getOperation(name);
        assertNotNull(pingMe);
        assertEquals(1, pingMe.getFaults().size());
        BindingFaultInfo fault = pingMe.getFaults().iterator().next();

        assertNotNull(fault);
        assertEquals(fault.getFaultInfo().getName().getLocalPart(), "pingMeFault");
        assertEquals(fault.getFaultInfo().getName().getNamespaceURI(),
                "http://apache.org/hello_world_soap_http");
        assertEquals(fault.getFaultInfo().getMessageParts().size(), 1);
        assertEquals(fault.getFaultInfo().getMessageParts().get(0).getName().getLocalPart(), "faultDetail");
        assertEquals(fault.getFaultInfo().getMessageParts().get(0).getName().getNamespaceURI(),
                "http://apache.org/hello_world_soap_http");
        assertTrue(fault.getFaultInfo().getMessageParts().get(0).isElement());
        elementName = fault.getFaultInfo().getMessageParts().get(0).getElementQName();
        assertEquals(elementName.getLocalPart(), "faultDetail");
        assertEquals(elementName.getNamespaceURI(), "http://apache.org/hello_world_soap_http/types");
        control.verify();
    }

    @Test
    public void testSchema() throws Exception {
        setUpBasic();
        SchemaCollection schemas = serviceInfo.getXmlSchemaCollection();
        assertNotNull(schemas);
        assertEquals(1, serviceInfo.getSchemas().size());
        SchemaInfo schemaInfo = serviceInfo.getSchemas().iterator().next();
        assertNotNull(schemaInfo);
        assertEquals(schemaInfo.getNamespaceURI(), "http://apache.org/hello_world_soap_http/types");
        assertEquals(schemas.read(schemaInfo.getElement()).getTargetNamespace(),
                "http://apache.org/hello_world_soap_http/types");
        // add below code to test the creation of javax.xml.validation.Schema
        // with schema in serviceInfo
        Schema schema = EndpointReferenceUtils.getSchema(serviceInfo);
        assertNotNull(schema);
        control.verify();
    }
    
    @Test
    public void testNoBodyParts() throws Exception {
        setUpWSDL(NO_BODY_PARTS_WSDL_PATH, 0);
        QName messageName = new QName("urn:org:apache:cxf:no_body_parts/wsdl",
                                      "operation1Request");
        MessageInfo mi = serviceInfo.getMessage(messageName);
        QName partName = new QName("urn:org:apache:cxf:no_body_parts/wsdl",
                                   "mimeAttachment");
        MessagePartInfo pi = mi.getMessagePart(partName);
        QName typeName = 
            new QName("http://www.w3.org/2001/XMLSchema",
                      "base64Binary");
        assertEquals(typeName, pi.getTypeQName());
        assertNull(pi.getElementQName());
    }

    @Test
    public void testBare() throws Exception {
        setUpWSDL(BARE_WSDL_PATH, 0);
        BindingInfo bindingInfo = null;
        bindingInfo = serviceInfo.getBindings().iterator().next();
        Collection<BindingOperationInfo> bindingOperationInfos = bindingInfo.getOperations();
        assertNotNull(bindingOperationInfos);
        assertEquals(bindingOperationInfos.size(), 1);
        LOG.info("the binding operation is " + bindingOperationInfos.iterator().next().getName());
        QName name = new QName(serviceInfo.getName().getNamespaceURI(), "greetMe");
        BindingOperationInfo greetMe = bindingInfo.getOperation(name);
        assertNotNull(greetMe);
        assertEquals("greetMe OperationInfo name error", greetMe.getName(), name);
        assertFalse("greetMe should be a Unwrapped operation ", greetMe.isUnwrappedCapable());
        
        assertNotNull(serviceInfo.getXmlSchemaCollection());
        control.verify();
    }

    @Test
    public void testImport() throws Exception {
        // rewrite the schema1.xsd to import schema2.xsd with absolute path.
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.parse(this.getClass().getResourceAsStream("./s1/s2/schema2.xsd"));
        Element schemaImport = null;
        
        Node node = doc.getFirstChild();
        while (node != null) {
            if (node instanceof Element) {
                schemaImport  = DOMUtils.getFirstElement(node);                
            }
            node = node.getNextSibling();
        }
        
        if (schemaImport == null) {
            fail("Can't find import element");
        }
        String filePath = this.getClass().getResource("./s1/s2/s4/schema4.xsd").toURI().getPath();
        String importPath = schemaImport.getAttributeNode("schemaLocation").getValue();
        if (!new URI(URLEncoder.encode(importPath, "utf-8")).isAbsolute()) {
            schemaImport.getAttributeNode("schemaLocation").setNodeValue("file:" + filePath);            
            String fileStr = this.getClass().getResource("./s1/s2/schema2.xsd").toURI().getPath();
            fileStr = URLDecoder.decode(fileStr, "utf-8");
            File file = new File(fileStr);
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fout = new FileOutputStream(file);
            XMLUtils.writeTo(doc, fout);
            fout.flush();
            fout.close();
        }
        setUpWSDL(IMPORT_WSDL_PATH, 0);
        assertNotNull(serviceInfo.getSchemas());
        Element ele = serviceInfo.getSchemas().iterator().next().getElement();
        assertNotNull(ele);
        Schema schema = EndpointReferenceUtils.getSchema(serviceInfo, null);        
        assertNotNull(schema);        
        control.verify();
    }

    
    @Test
    public void testDiffPortTypeNsImport() throws Exception {
        setUpWSDL("/DiffPortTypeNs.wsdl", 0);        
        doDiffPortTypeNsImport();
        setUpWSDL("/DiffPortTypeNs.wsdl", 1);
        doDiffPortTypeNsImport();
        control.verify();
    }
    
    private void doDiffPortTypeNsImport() {
        if (serviceInfo.getName().getLocalPart().endsWith("Rpc")) {
            String ns = serviceInfo.getInterface().getName().getNamespaceURI();
            OperationInfo oi = serviceInfo.getInterface().getOperation(new QName(ns, "NewOperationRpc"));
            assertNotNull(oi);
            ns = oi.getInput().getName().getNamespaceURI();
            MessagePartInfo mpi = oi.getInput().getMessagePart(new QName(ns, "NewOperationRequestRpc"));
            assertNotNull(mpi);                    
        } else {
            String ns = serviceInfo.getInterface().getName().getNamespaceURI();
            OperationInfo oi = serviceInfo.getInterface().getOperation(new QName(ns, "NewOperation"));
            assertNotNull(oi);
            ns = oi.getInput().getName().getNamespaceURI();
            MessagePartInfo mpi = oi.getInput().getMessagePart(new QName(ns, "NewOperationRequest"));
            assertNotNull(mpi);                    
        }
    }
    
    @Test
    public void testParameterOrder() throws Exception {
        String ns = "http://apache.org/hello_world_xml_http/bare";
        setUpWSDL("hello_world_xml_bare.wsdl", 0);
        
        OperationInfo operation = serviceInfo.getInterface().getOperation(new QName(ns, 
                                                                                    "testTriPart"));
        assertNotNull(operation);
        List<MessagePartInfo> parts = operation.getInput().getMessageParts();
        assertNotNull(parts);
        assertEquals(3, parts.size());
        assertEquals("in3", parts.get(0).getName().getLocalPart());
        assertEquals("in1", parts.get(1).getName().getLocalPart());
        assertEquals("in2", parts.get(2).getName().getLocalPart());
        
        List<String> order = operation.getParameterOrdering();
        assertNotNull(order);
        assertEquals(3, order.size());
        assertEquals("in1", order.get(0));
        assertEquals("in3", order.get(1));
        assertEquals("in2", order.get(2));
        
        parts = operation.getInput().getOrderedParts(order);
        assertNotNull(parts);
        assertEquals(3, parts.size());
        assertEquals("in1", parts.get(0).getName().getLocalPart());
        assertEquals("in3", parts.get(1).getName().getLocalPart());
        assertEquals("in2", parts.get(2).getName().getLocalPart());
        
        operation = serviceInfo.getInterface().getOperation(new QName(ns,
                                                                      "testTriPartNoOrder"));
        assertNotNull(operation);
        parts = operation.getInput().getMessageParts();
        assertNotNull(parts);
        assertEquals(3, parts.size());
        assertEquals("in3", parts.get(0).getName().getLocalPart());
        assertEquals("in1", parts.get(1).getName().getLocalPart());
        assertEquals("in2", parts.get(2).getName().getLocalPart());
        control.verify();
    }
    
    @Test
    public void testParameterOrder2() throws Exception {
        setUpWSDL("header2.wsdl", 0);
        String ns = "http://apache.org/header2";
        OperationInfo operation = serviceInfo.getInterface().getOperation(new QName(ns, "headerMethod"));
        assertNotNull(operation);
        List<MessagePartInfo> parts = operation.getInput().getMessageParts();
        assertNotNull(parts);
        assertEquals(2, parts.size());
        assertEquals("header_info", parts.get(0).getName().getLocalPart());
        assertEquals("the_request", parts.get(1).getName().getLocalPart());
        control.verify();        
    }

    @Test
    public void testExtensions() throws Exception {
        setUpWSDL("hello_world_ext.wsdl", 0);

        String ns = "http://apache.org/hello_world_soap_http";
        QName pingMeOpName = new QName(ns, "pingMe");
        QName greetMeOpName = new QName(ns, "greetMe");
        QName faultName = new QName(ns, "pingMeFault");

        // portType extensions

        InterfaceInfo ii = serviceInfo.getInterface();
        assertEquals(2, ii.getExtensionAttributes().size());
        assertNotNull(ii.getExtensionAttribute(EXTENSION_ATTR_BOOLEAN));
        assertNotNull(ii.getExtensionAttribute(EXTENSION_ATTR_STRING));
        assertEquals(1, ii.getExtensors(UnknownExtensibilityElement.class).size());
        assertEquals(EXTENSION_ELEM, ii.getExtensor(UnknownExtensibilityElement.class).getElementType());

        // portType/operation extensions
  
        OperationInfo oi = ii.getOperation(pingMeOpName);
        assertPortTypeOperationExtensions(oi, true);
        assertPortTypeOperationExtensions(ii.getOperation(greetMeOpName), false);
                
        // portType/operation/[input|output|fault] extensions
  
        assertPortTypeOperationMessageExtensions(oi, true, true, faultName);
        assertPortTypeOperationMessageExtensions(ii.getOperation(greetMeOpName), false, true, null);

        // service extensions

        assertEquals(1, serviceInfo.getExtensionAttributes().size());
        assertNotNull(serviceInfo.getExtensionAttribute(EXTENSION_ATTR_STRING));
        assertEquals(1, serviceInfo.getExtensors(UnknownExtensibilityElement.class).size());
        assertEquals(EXTENSION_ELEM,
            serviceInfo.getExtensor(UnknownExtensibilityElement.class).getElementType());
       
        // service/port extensions

        EndpointInfo ei = serviceInfo.getEndpoints().iterator().next();
        assertEquals(1, ei.getExtensionAttributes().size());
        assertNotNull(ei.getExtensionAttribute(EXTENSION_ATTR_STRING));
        assertEquals(1, ei.getExtensors(UnknownExtensibilityElement.class).size());
        assertEquals(EXTENSION_ELEM, ei.getExtensor(UnknownExtensibilityElement.class).getElementType());

        // binding extensions

        BindingInfo bi = ei.getBinding();
        // REVISIT: bug in wsdl4j?
        // getExtensionAttributes on binding element returns an empty map
        // assertEquals(1, bi.getExtensionAttributes().size());
        // assertNotNull(bi.getExtensionAttribute(EXTENSION_ATTR_STRING));
        assertEquals(1, bi.getExtensors(UnknownExtensibilityElement.class).size());
        assertEquals(EXTENSION_ELEM, bi.getExtensor(UnknownExtensibilityElement.class).getElementType());

        // binding/operation extensions
       
        BindingOperationInfo boi = bi.getOperation(pingMeOpName);
        assertBindingOperationExtensions(boi, true);
        assertBindingOperationExtensions(bi.getOperation(greetMeOpName), false);

        // binding/operation/[input|output|fault] extensions
  
        assertBindingOperationMessageExtensions(boi, true, true, faultName);
        assertBindingOperationMessageExtensions(bi.getOperation(greetMeOpName), false, true, null);
        control.verify();
        
    }

    private void assertPortTypeOperationExtensions(OperationInfo oi, boolean expectExtensions) {
        if (expectExtensions) {
            assertEquals(1, oi.getExtensionAttributes().size());
            assertNotNull(oi.getExtensionAttribute(EXTENSION_ATTR_STRING));
            assertEquals(1, oi.getExtensors(UnknownExtensibilityElement.class).size());
            assertEquals(EXTENSION_ELEM, oi.getExtensor(UnknownExtensibilityElement.class).getElementType());
        } else {
            assertNull(oi.getExtensionAttributes());
            assertNull(oi.getExtensionAttribute(EXTENSION_ATTR_STRING));
            assertNull(oi.getExtensors(UnknownExtensibilityElement.class));
            assertNull(oi.getExtensor(UnknownExtensibilityElement.class));
        }
    }

    private void assertBindingOperationExtensions(BindingOperationInfo boi, boolean expectExtensions) {
        if (expectExtensions) {
            assertEquals(1, boi.getExtensionAttributes().size());
            assertNotNull(boi.getExtensionAttribute(EXTENSION_ATTR_STRING));
            assertEquals(1, boi.getExtensors(UnknownExtensibilityElement.class).size());
            assertEquals(EXTENSION_ELEM, boi.getExtensor(UnknownExtensibilityElement.class).getElementType());
        } else {
            assertNull(boi.getExtensionAttributes());
            assertNull(boi.getExtensionAttribute(EXTENSION_ATTR_STRING));
            assertEquals(0, boi.getExtensors(UnknownExtensibilityElement.class).size());
            assertNull(boi.getExtensor(UnknownExtensibilityElement.class));
        }
    }

    private void assertPortTypeOperationMessageExtensions(OperationInfo oi, boolean expectExtensions,
        boolean hasOutput, QName fault) {

        MessageInfo mi = oi.getInput();
        if (expectExtensions) {
            assertEquals(1, mi.getExtensionAttributes().size());
            assertNotNull(mi.getExtensionAttribute(EXTENSION_ATTR_STRING));
            assertEquals(1, mi.getExtensors(UnknownExtensibilityElement.class).size());
            assertEquals(EXTENSION_ELEM, mi.getExtensor(UnknownExtensibilityElement.class).getElementType());
        } else {
            assertNull(mi.getExtensionAttributes());
            assertNull(mi.getExtensionAttribute(EXTENSION_ATTR_STRING));
            assertNull(mi.getExtensors(UnknownExtensibilityElement.class));
            assertNull(mi.getExtensor(UnknownExtensibilityElement.class));
        }
       
        if (hasOutput) {         
            mi = oi.getOutput();
            if (expectExtensions) {
                assertEquals(1, mi.getExtensionAttributes().size());
                assertNotNull(mi.getExtensionAttribute(EXTENSION_ATTR_STRING));
                assertEquals(1, mi.getExtensors(UnknownExtensibilityElement.class).size());
                assertEquals(EXTENSION_ELEM,
                    mi.getExtensor(UnknownExtensibilityElement.class).getElementType());
            } else {
                assertNull(mi.getExtensionAttributes());
                assertNull(mi.getExtensionAttribute(EXTENSION_ATTR_STRING));
                assertNull(mi.getExtensors(UnknownExtensibilityElement.class));
                assertNull(mi.getExtensor(UnknownExtensibilityElement.class));
            }
        }
        
        if (null != fault) { 
            FaultInfo fi = oi.getFault(fault);
            if (expectExtensions) {
                assertEquals(1, fi.getExtensionAttributes().size());
                assertNotNull(fi.getExtensionAttribute(EXTENSION_ATTR_STRING));
                assertEquals(1, fi.getExtensors(UnknownExtensibilityElement.class).size());
                assertEquals(EXTENSION_ELEM,
                    fi.getExtensor(UnknownExtensibilityElement.class).getElementType());
            } else {
                assertNull(fi.getExtensionAttributes());
                assertNull(fi.getExtensionAttribute(EXTENSION_ATTR_STRING));
                assertNull(fi.getExtensors(UnknownExtensibilityElement.class));
                assertNull(fi.getExtensor(UnknownExtensibilityElement.class));
            }
        } 
    }

    private void assertBindingOperationMessageExtensions(BindingOperationInfo boi, boolean expectExtensions,
        boolean hasOutput, QName fault) {

        BindingMessageInfo bmi = boi.getInput();
        if (expectExtensions) {
            // REVISIT: bug in wsdl4j?
            // getExtensionAttributes on binding/operation/input element returns an empty map
            // assertEquals(1, bmi.getExtensionAttributes().size());
            // assertNotNull(bmi.getExtensionAttribute(EXTENSION_ATTR_STRING));
            assertEquals(1, bmi.getExtensors(UnknownExtensibilityElement.class).size());
            assertEquals(EXTENSION_ELEM, bmi.getExtensor(UnknownExtensibilityElement.class).getElementType());
        } else {
            assertNull(bmi.getExtensionAttributes());
            assertNull(bmi.getExtensionAttribute(EXTENSION_ATTR_STRING));
            assertEquals(0, bmi.getExtensors(UnknownExtensibilityElement.class).size());
            assertNull(bmi.getExtensor(UnknownExtensibilityElement.class));
        }
       
        if (hasOutput) {         
            bmi = boi.getOutput();
            if (expectExtensions) {
                // REVISIT: bug in wsdl4j?
                // getExtensionAttributes on binding/operation/output element returns an empty map
                // assertEquals(1, bmi.getExtensionAttributes().size());
                // assertNotNull(bmi.getExtensionAttribute(EXTENSION_ATTR_STRING));
                assertEquals(1, bmi.getExtensors(UnknownExtensibilityElement.class).size());
                assertEquals(EXTENSION_ELEM,
                    bmi.getExtensor(UnknownExtensibilityElement.class).getElementType());
            } else {
                assertNull(bmi.getExtensionAttributes());
                assertNull(bmi.getExtensionAttribute(EXTENSION_ATTR_STRING));
                assertEquals(0, bmi.getExtensors(UnknownExtensibilityElement.class).size());
                assertNull(bmi.getExtensor(UnknownExtensibilityElement.class));
            }
        }
        
        if (null != fault) { 
            BindingFaultInfo bfi = boi.getFault(fault);
            if (expectExtensions) {
                assertEquals(1, bfi.getExtensionAttributes().size());
                assertNotNull(bfi.getExtensionAttribute(EXTENSION_ATTR_STRING));
                assertEquals(1, bfi.getExtensors(UnknownExtensibilityElement.class).size());
                assertEquals(EXTENSION_ELEM,
                    bfi.getExtensor(UnknownExtensibilityElement.class).getElementType());
            } else {
                assertNull(bfi.getExtensionAttributes());
                assertNull(bfi.getExtensionAttribute(EXTENSION_ATTR_STRING));
                assertNull(bfi.getExtensors(UnknownExtensibilityElement.class));
                assertNull(bfi.getExtensor(UnknownExtensibilityElement.class));
            }
        } 
    }


}

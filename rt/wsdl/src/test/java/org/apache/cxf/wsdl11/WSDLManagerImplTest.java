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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.staxutils.PropertiesExpandingStreamReader;
import org.apache.cxf.staxutils.XMLStreamReaderWrapper;
import org.apache.cxf.wsdl.WSDLManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WSDLManagerImplTest {

    @Test
    public void testBuildSimpleWSDL() throws Exception {
        String qname = "http://apache.org/hello_world_soap_http";
        String wsdlUrl = getClass().getResource("hello_world.wsdl").toString();

        WSDLManager builder = new WSDLManagerImpl();
        Definition def = builder.getDefinition(wsdlUrl);
        assertNotNull(def);

        Map<?, ?> services = def.getServices();
        assertNotNull(services);
        assertEquals(1, services.size());
        Service service = (Service)services.get(new QName(qname, "SOAPService"));
        assertNotNull(service);

        Map<?, ?> ports = service.getPorts();
        assertNotNull(ports);
        assertEquals(1, ports.size());
        Port port = service.getPort("SoapPort");
        assertNotNull(port);
    }

    @Test
    public void testBuildImportedWSDL() throws Exception {
        String wsdlUrl = getClass().getResource("hello_world_services.wsdl").toString();

        WSDLManager builder = new WSDLManagerImpl();
        Definition def = builder.getDefinition(wsdlUrl);

        assertNotNull(def);
        Map<?, ?> services = def.getServices();
        assertNotNull(services);
        assertEquals(1, services.size());

        String serviceQName = "http://apache.org/hello_world/services";
        Service service = (Service)services.get(new QName(serviceQName, "SOAPService"));
        assertNotNull(service);

        Map<?, ?> ports = service.getPorts();
        assertNotNull(ports);
        assertEquals(1, ports.size());
        Port port = service.getPort("SoapPort");
        assertNotNull(port);

        Binding binding = port.getBinding();
        assertNotNull(binding);
        QName bindingQName = new QName("http://apache.org/hello_world/bindings", "SOAPBinding");
        assertEquals(bindingQName, binding.getQName());
        PortType portType = binding.getPortType();
        assertNotNull(portType);
        QName portTypeQName = new QName("http://apache.org/hello_world", "Greeter");
        assertEquals(portTypeQName, portType.getQName());
        Operation op1 = portType.getOperation("sayHi", "sayHiRequest", "sayHiResponse");
        assertNotNull(op1);
        QName messageQName = new QName("http://apache.org/hello_world/messages", "sayHiRequest");
        assertEquals(messageQName, op1.getInput().getMessage().getQName());

        Part part = op1.getInput().getMessage().getPart("in");
        assertNotNull(part);
        assertEquals(new QName("http://apache.org/hello_world/types", "sayHi"), part.getElementName());
    }

    @Test
    public void testLocalNamespacedWSDL() throws Exception {
        String wsdlUrl = getClass().getResource("hello_world_local_nsdecl.wsdl").toString();

        WSDLManager builder = new WSDLManagerImpl();
        Definition def = builder.getDefinition(wsdlUrl);
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        builder.getWSDLFactory().newWSDLWriter().writeWSDL(def, bos);
    }

    @Test
    public void testXMLStreamReaderWrapper() throws Exception {
        final Map<String, String> map = new HashMap<>();
        map.put("org.apache.cxf.test.wsdl11.port", "99999");
        String wsdlUrl = getClass().getResource("hello_world_wrap.wsdl").toString();
        WSDLManagerImpl builder = new WSDLManagerImpl();
        builder.setXMLStreamReaderWrapper(new XMLStreamReaderWrapper() {
            @Override
            public XMLStreamReader wrap(XMLStreamReader reader) {
                return new PropertiesExpandingStreamReader(reader, map);
            }
        });
        Definition def = builder.getDefinition(wsdlUrl);
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        builder.getWSDLFactory().newWSDLWriter().writeWSDL(def, bos);
        assertTrue(bos.toString().contains("http://localhost:99999/SoapContext/SoapPort"));
    }

    @Test
    public void testRemoveDefinition() throws Exception {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        // Copy hello_world.wsdl so that we can delete it
        Path path1 = FileSystems.getDefault().getPath(basedir,
                "/src/test/resources/org/apache/cxf/wsdl11/hello_world.wsdl");
        Path path2 = FileSystems.getDefault().getPath(basedir, "/target/test-classes/hello_world2.wsdl");
        Files.copy(path1, path2);

        // Load the resource
        WSDLManager builder = new WSDLManagerImpl();
        Definition def = builder.getDefinition(path2.toString());
        assertNotNull(def);

        // Delete the resource
        Files.delete(path2);

        // Now load it again to test caching
        def = builder.getDefinition(path2.toString());
        assertNotNull(def);

        Map<?, ?> services = def.getServices();
        assertNotNull(services);
        assertEquals(1, services.size());
        String qname = "http://apache.org/hello_world_soap_http";
        Service service = (Service) services.get(new QName(qname, "SOAPService"));
        assertNotNull(service);

        // Now remove it
        builder.removeDefinition(def);

        // This time loading should fail as the original resource is removed
        try {
            builder.getDefinition(path2.toString());
            fail("Failure expected");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    @Test
    public void testRemoveDefinitionByURL() throws Exception {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        // Copy hello_world.wsdl so that we can delete it
        Path path1 = FileSystems.getDefault().getPath(basedir,
                "/src/test/resources/org/apache/cxf/wsdl11/hello_world.wsdl");
        Path path2 = FileSystems.getDefault().getPath(basedir, "/target/test-classes/hello_world2.wsdl");
        Files.copy(path1, path2);

        // Load the resource
        WSDLManager builder = new WSDLManagerImpl();
        Definition def = builder.getDefinition(path2.toString());
        assertNotNull(def);

        // Delete the resource
        Files.delete(path2);

        // Now load it again to test caching
        def = builder.getDefinition(path2.toString());
        assertNotNull(def);

        Map<?, ?> services = def.getServices();
        assertNotNull(services);
        assertEquals(1, services.size());
        String qname = "http://apache.org/hello_world_soap_http";
        Service service = (Service) services.get(new QName(qname, "SOAPService"));
        assertNotNull(service);

        // Now remove it
        builder.removeDefinition(path2.toString());

        // This time loading should fail as the original resource is removed
        try {
            builder.getDefinition(path2.toString());
            fail("Failure expected");
        } catch (NullPointerException ex) {
            // expected
        }
    }
}
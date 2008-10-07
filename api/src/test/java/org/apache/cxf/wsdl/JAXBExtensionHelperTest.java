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

package org.apache.cxf.wsdl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.xml.sax.InputSource;

import org.apache.cxf.abc.test.AnotherPolicyType;
import org.apache.cxf.abc.test.NewServiceType;
import org.apache.cxf.abc.test.TestPolicyType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JAXBExtensionHelperTest extends Assert {

    private WSDLFactory wsdlFactory;

    private WSDLReader wsdlReader;

    private Definition wsdlDefinition;

    private ExtensionRegistry registry;

    @Before
    public void setUp() throws Exception {

        wsdlFactory = WSDLFactory.newInstance();
        wsdlReader = wsdlFactory.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        registry = wsdlReader.getExtensionRegistry();
        if (registry == null) {
            registry = wsdlFactory.newPopulatedExtensionRegistry();
        }
    }

    @Test
    public void testAddTestExtension() throws Exception {

        JAXBExtensionHelper.addExtensions(registry, "javax.wsdl.Port",
                        "org.apache.cxf.abc.test.TestPolicyType");

        JAXBExtensionHelper.addExtensions(registry, "javax.wsdl.Port",
                        "org.apache.cxf.abc.test.AnotherPolicyType");

        JAXBExtensionHelper.addExtensions(registry, "javax.wsdl.Definition",
                        "org.apache.cxf.abc.test.NewServiceType");

        String file = this.getClass().getResource("/wsdl/test_ext.wsdl").toURI().toString();

        wsdlReader.setExtensionRegistry(registry);

        wsdlDefinition = wsdlReader.readWSDL(file);
        checkTestExt();
    }

    @Test
    public void testPrettyPrintXMLStreamWriter() throws Exception {
        JAXBExtensionHelper.addExtensions(registry, "javax.wsdl.Definition",
                        "org.apache.cxf.abc.test.NewServiceType");

        String file = this.getClass().getResource("/wsdl/test_ext.wsdl").toURI().toString();

        wsdlReader.setExtensionRegistry(registry);

        wsdlDefinition = wsdlReader.readWSDL(file);

        List extList = wsdlDefinition.getExtensibilityElements();
        NewServiceType newService = null;
        for (Object ext : extList) {
            if (ext instanceof NewServiceType) {
                newService = (NewServiceType) ext;
                break;
            }
        }

        assertNotNull("Could not find extension element NewServiceType", newService);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        JAXBExtensionHelper helper = new JAXBExtensionHelper(NewServiceType.class, null);
        helper.marshall(javax.wsdl.Definition.class,
                        new QName("http://cxf.apache.org/test/hello_world", "newService"),
                        newService,
                        new PrintWriter(stream),
                        wsdlDefinition,
                        registry);
        BufferedReader reader = new BufferedReader(new StringReader(new String(stream.toByteArray())));
        String actual = reader.readLine();
        int spaces = 0;
        while (actual != null) {
            if (!actual.endsWith("/>")) {
                if (!actual.contains("</")) {
                    spaces += 2;
                } else {
                    spaces -= 2;
                }
            }
            checkSpaces(actual, spaces);
            actual = reader.readLine();
        }
    }
    
    @Test
    public void testMappedNamespace() throws Exception {
        JAXBExtensionHelper.addExtensions(registry, "javax.wsdl.Port",
            "org.apache.cxf.abc.test.TestPolicyType",
            "http://cxf.apache.org/abc/test/remapped");

        JAXBExtensionHelper.addExtensions(registry, "javax.wsdl.Port",
            "org.apache.cxf.abc.test.AnotherPolicyType",
            "http://cxf.apache.org/abc/test/remapped");

        JAXBExtensionHelper.addExtensions(registry, "javax.wsdl.Definition",
            "org.apache.cxf.abc.test.NewServiceType",
            "http://cxf.apache.org/abc/test/remapped");

        String file = this.getClass().getResource("/wsdl/test_ext_remapped.wsdl").toURI().toString();
        wsdlReader.setExtensionRegistry(registry);

        wsdlDefinition = wsdlReader.readWSDL(file);
        checkTestExt();
        StringWriter out = new StringWriter();
        wsdlFactory.newWSDLWriter().writeWSDL(wsdlDefinition, out);
        wsdlDefinition = wsdlReader.readWSDL(null,
                                             new InputSource(new StringReader(out.toString())));
        checkTestExt();
    }
    
    private void checkTestExt() throws Exception {
        Service s = wsdlDefinition.getService(new QName("http://cxf.apache.org/test/hello_world",
            "HelloWorldService"));
        Port p = s.getPort("HelloWorldPort");
        List extPortList = p.getExtensibilityElements();
    
        TestPolicyType tp = null;
        AnotherPolicyType ap = null;
        for (Object ext : extPortList) {
            if (ext instanceof TestPolicyType) {
                tp = (TestPolicyType) ext;
            }
            if (ext instanceof AnotherPolicyType) {
                ap = (AnotherPolicyType) ext;
            }
        }
        assertNotNull("Could not find extension element TestPolicyType", tp);
        assertNotNull("Could not find extension element AnotherPolicyType", ap);
    
        assertEquals("Unexpected value for TestPolicyType intAttr", 30, tp.getIntAttr());
        assertEquals("Unexpected value for TestPolicyType stringAttr", "hello", tp.getStringAttr());
        assertTrue("Unexpected value for AnotherPolicyType floatAttr",
                   Math.abs(0.1F - ap.getFloatAttr()) < 0.5E-5);        
    }

    private void checkSpaces(String actual, int spaces) {
        String space = "";
        for (int i = 0; i < spaces; i++) {
            space += " ";
        }
        assertTrue("Indentation level not proper when marshalling a extension element;" + actual,
                   actual.startsWith(space));
    }

}

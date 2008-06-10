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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.wsdl11;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.PortType;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.apache.cxf.BusFactory;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSBinding;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JAXWSDefinitionBuilderTest extends Assert {
    private ToolContext env;

    @Before
    public void setUp() {
        env = new ToolContext();
    }

    @Test
    public void testCustomization() {
        env.put(ToolConstants.CFG_WSDLURL, getClass().getResource("resources/hello_world.wsdl").toString());
        env.put(ToolConstants.CFG_BINDING, getClass().getResource("resources/binding2.xml").toString());
        JAXWSDefinitionBuilder builder = new JAXWSDefinitionBuilder();
        builder.setContext(env);
        builder.setBus(BusFactory.getDefaultBus());
        builder.build();
        builder.customize();

        Definition customizedDef = builder.getWSDLModel();
        List defExtensionList = customizedDef.getExtensibilityElements();
        Iterator ite = defExtensionList.iterator();

        while (ite.hasNext()) {
            ExtensibilityElement extElement = (ExtensibilityElement)ite.next();
            JAXWSBinding binding = (JAXWSBinding)extElement;
            assertEquals("Customized package name does not been parsered", "com.foo", binding.getPackage());
            assertEquals("Customized enableAsync does not parsered", true, binding.isEnableAsyncMapping());
        }

        PortType portType = customizedDef.getPortType(new QName("http://apache.org/hello_world_soap_http",
                                                                "Greeter"));

        List portTypeList = portType.getExtensibilityElements();
        JAXWSBinding binding = (JAXWSBinding)portTypeList.get(0);

        assertEquals("Customized enable EnableWrapperStyle name does not been parsered", true, binding
            .isEnableWrapperStyle());

        List opList = portType.getOperations();
        Operation operation = (Operation)opList.get(0);
        List extList = operation.getExtensibilityElements();
        binding = (JAXWSBinding)extList.get(0);

        assertEquals("Customized method name does not parsered", "echoMeOneWay", binding.getMethodName());


        assertEquals("Customized parameter element name does not parsered", "tns:number1", binding
            .getJaxwsPara().getElementName());
        assertEquals("Customized parameter message name does not parsered", "greetMeOneWayRequest", binding
            .getJaxwsPara().getMessageName());
        assertEquals("customized parameter name does not parsered", "num1", binding.getJaxwsPara().getName());
    }



    @Test
    public void testCustomizationWithDifferentNS() {
        env.put(ToolConstants.CFG_WSDLURL, getClass().getResource("resources/hello_world.wsdl").toString());
        env.put(ToolConstants.CFG_BINDING, getClass().getResource("resources/binding3.xml").toString());
        JAXWSDefinitionBuilder builder = new JAXWSDefinitionBuilder();
        builder.setContext(env);
        builder.setBus(BusFactory.getDefaultBus());
        builder.build();
        builder.customize();

        Definition customizedDef = builder.getWSDLModel();
        List defExtensionList = customizedDef.getExtensibilityElements();
        Iterator ite = defExtensionList.iterator();

        while (ite.hasNext()) {
            ExtensibilityElement extElement = (ExtensibilityElement)ite.next();
            JAXWSBinding binding = (JAXWSBinding)extElement;
            assertEquals("Customized package name does not been parsered", "com.foo", binding.getPackage());
            assertEquals("Customized enableAsync does not parsered", true, binding.isEnableAsyncMapping());
        }

        PortType portType = customizedDef.getPortType(new QName("http://apache.org/hello_world_soap_http",
                                                                "Greeter"));

        List portTypeList = portType.getExtensibilityElements();
        JAXWSBinding binding = (JAXWSBinding)portTypeList.get(0);

        assertEquals("Customized enable EnableWrapperStyle name does not been parsered", true, binding
            .isEnableWrapperStyle());

        List opList = portType.getOperations();
        Operation operation = (Operation)opList.get(0);
        List extList = operation.getExtensibilityElements();
        binding = (JAXWSBinding)extList.get(0);

        assertEquals("Customized method name does not parsered", "echoMeOneWay", binding.getMethodName());


        assertEquals("Customized parameter element name does not parsered", "tns:number1", binding
            .getJaxwsPara().getElementName());
        assertEquals("Customized parameter message name does not parsered", "greetMeOneWayRequest", binding
            .getJaxwsPara().getMessageName());
        assertEquals("customized parameter name does not parsered", "num1", binding.getJaxwsPara().getName());
    }

    // tests the error case described in JIRA CXF-556
    @Test
    public void testCustomizationWhereURINotAnExactStringMatch() throws Exception  {
        // set up a URI with ./../wsdl11/hello_world.wsdl instead of
        // ./hello_world.wsdl

        env.put(ToolConstants.CFG_WSDLURL,
            new File(getClass().getResource("JAXWSDefinitionBuilderTest.class").toURI())
                .getParentFile().getParent() + "/wsdl11/resources/hello_world.wsdl");
        env.put(ToolConstants.CFG_BINDING,
            getClass().getResource("resources/cxf556_binding.xml").toString());

        JAXWSDefinitionBuilder builder = new JAXWSDefinitionBuilder();
        builder.setContext(env);
        builder.setBus(BusFactory.getDefaultBus());
        builder.build();

        // this call will fail before CXF-556
        builder.customize();

        assertTrue(true);
    }

    @Test
    public void testNoService() {
        env.put(ToolConstants.CFG_WSDLURL, getClass().getResource("resources/build.wsdl").toString());

        JAXWSDefinitionBuilder builder = new JAXWSDefinitionBuilder();
        builder.setContext(env);
        builder.setBus(BusFactory.getDefaultBus());
        builder.build();

        Definition def = builder.getWSDLModel();
        assertTrue(def.getServices().keySet().contains(new QName("http://apache.org/hello_world_soap_http", 
                                                                 "SOAPService")));
    }
}

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

package org.apache.cxf.tools.wsdlto.jaxws.wsdl11;

import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.xml.namespace.QName;

import org.apache.cxf.BusFactory;
import org.apache.cxf.bindings.xformat.XMLBindingMessageFormat;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.wsdl11.JAXWSDefinitionBuilder;
import org.apache.cxf.wsdl.JAXBExtensibilityElement;
import org.apache.cxf.wsdl.http.AddressType;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JAXWSDefinitionBuilderTest {
    private ToolContext env;

    @Before
    public void setUp() {
        env = new ToolContext();
    }


    @Test
    public void testBuildDefinitionWithXMLBinding() {
        String qname = "http://apache.org/hello_world_xml_http/bare";
        String wsdlUrl = getClass().getResource("resources/hello_world_xml_bare.wsdl").toString();

        JAXWSDefinitionBuilder builder = new JAXWSDefinitionBuilder();
        builder.setBus(BusFactory.getDefaultBus());
        builder.setContext(env);
        Definition def = builder.build(wsdlUrl);
        assertNotNull(def);

        Map<?, ?> services = def.getServices();
        assertNotNull(services);
        assertEquals(1, services.size());
        Service service = (Service)services.get(new QName(qname, "XMLService"));
        assertNotNull(service);

        Map<?, ?> ports = service.getPorts();
        assertNotNull(ports);
        assertEquals(1, ports.size());
        Port port = service.getPort("XMLPort");
        assertNotNull(port);

        assertEquals(1, port.getExtensibilityElements().size());
        Object obj = port.getExtensibilityElements().get(0);
        if (obj instanceof JAXBExtensibilityElement) {
            obj = ((JAXBExtensibilityElement)obj).getValue();
        }
        assertTrue(obj.getClass().getName() + " is not an AddressType",
                   obj instanceof AddressType);

        Binding binding = port.getBinding();
        assertNotNull(binding);
        assertEquals(new QName(qname, "Greeter_XMLBinding"), binding.getQName());

        BindingOperation operation = binding.getBindingOperation("sayHi", null, null);
        assertNotNull(operation);

        BindingInput input = operation.getBindingInput();
        assertNotNull(input);
        assertEquals(1, input.getExtensibilityElements().size());
        obj = input.getExtensibilityElements().get(0);
        if (obj instanceof JAXBExtensibilityElement) {
            obj = ((JAXBExtensibilityElement)obj).getValue();
        }
        assertTrue(obj.getClass().getName() + " is not an XMLBindingMessageFormat",
                   obj instanceof XMLBindingMessageFormat);
    }

}
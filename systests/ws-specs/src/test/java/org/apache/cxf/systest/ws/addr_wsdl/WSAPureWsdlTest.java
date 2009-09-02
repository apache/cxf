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

package org.apache.cxf.systest.ws.addr_wsdl;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.systest.ws.AbstractWSATestBase;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersPortType;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WSAPureWsdlTest extends AbstractWSATestBase {

    private final QName serviceName = new QName("http://apache.org/cxf/systest/ws/addr_feature/",
                                                "AddNumbersService");

    @Before
    public void setUp() throws Exception {
        createBus();
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testBasicInvocation() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getPort();

        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                                                        "http://localhost:9094/jaxws/add");

        assertEquals(3, port.addNumbers(1, 2));

        String base = "http://apache.org/cxf/systest/ws/addr_feature/AddNumbersPortType/";
        String expectedOut = base + "addNumbersRequest</Action>";
        String expectedIn = base + "addNumbersResponse</Action>";

        assertTrue(output.toString().indexOf(expectedOut) != -1);
        assertTrue(input.toString().indexOf(expectedIn) != -1);
    }

    private AddNumbersPortType getPort() {
        URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers.wsdl");
        assertNotNull("WSDL is null", wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        return service.getAddNumbersPort();
    }
}
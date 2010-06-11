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

package org.apache.cxf.systest.resolver;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.endpoint.ServiceContractResolverRegistry;
import org.apache.cxf.endpoint.ServiceContractResolverRegistryImpl;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_soap_http.Greeter;
import org.junit.BeforeClass;
import org.junit.Test;

public class JarResolverTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    private final QName serviceName = new QName("http://apache.org/hello_world_soap_http", "SOAPService");

    private final QName portName = new QName("http://apache.org/hello_world_soap_http", "SoapPort");
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
        
    }
    @Test
    public void testResolver() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        createBus();
        assertNotNull(bus);
        ServiceContractResolverRegistryImpl registry = 
            new ServiceContractResolverRegistryImpl();
        registry.setBus(bus);
        registry.init();
        assertNotNull(bus.getExtension(ServiceContractResolverRegistry.class));

        JarServiceContractResolver resolver = new JarServiceContractResolver();
        registry.register(resolver);

        Service service = Service.create(serviceName);
        //service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING,
        //                "http://localhost:9000/SoapContext/SoapPort"); 
        Greeter greeter = service.getPort(portName,  Greeter.class);
        updateAddressPort(greeter, PORT);

        String resp = greeter.sayHi();
        assertNotNull(resp);
    }
}

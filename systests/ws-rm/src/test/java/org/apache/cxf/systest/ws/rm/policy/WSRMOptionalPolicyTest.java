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
package org.apache.cxf.systest.ws.rm.policy;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WSRMOptionalPolicyTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    private static final URL WSDL = GreetingService.class.getResource("greeting.wsdl");
    private static final QName SERVICE = new QName("http://ws.samples.apache.org/", "GreetingService");
    private static final String ENDPOINT = "http://localhost:" + PORT + "/GreetingServer";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testBasicConnection() throws Exception {
        Greeting service = createService();
        assertEquals("Hello, cxf!", service.hello("cxf"));
        assertEquals("Goodbye, cxf!", service.goodbye("cxf"));
    }

    private static Greeting createService() {
        Greeting service = Service.create(WSDL, SERVICE).getPort(Greeting.class);
        Map<String, Object> context = ((BindingProvider)service).getRequestContext();
        context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT);
        return service;
    }

}

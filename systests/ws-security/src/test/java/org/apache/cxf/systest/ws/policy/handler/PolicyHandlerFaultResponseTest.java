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
package org.apache.cxf.systest.ws.policy.handler;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PolicyHandlerFaultResponseTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    private final QName serviceName = new QName("http://handler.policy.ws.systest.cxf.apache.org/",
                                                "HelloPolicyService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));

    }

    @Test
    public void testFaultResponse() throws Exception {
        String address = "http://localhost:" + PORT + "/policytest";
        URL wsdlURL = new URL(address + "?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        service
            .addPort(new QName("http://handler.policy.ws.systest.cxf.apache.org/", "HelloPolicyServicePort"),
                     SOAPBinding.SOAP11HTTP_BINDING, address);
        HelloService port = service.getPort(new QName("http://handler.policy.ws.systest.cxf.apache.org/",
                                                      "HelloPolicyServicePort"), HelloService.class);
        Map<String, Object> context = ((BindingProvider)port).getRequestContext();
        context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);

        context.put(SecurityConstants.CALLBACK_HANDLER, new CommonPasswordCallback());
        context.put(SecurityConstants.SIGNATURE_PROPERTIES, "alice.properties");
        context.put(SecurityConstants.SIGNATURE_USERNAME, "alice");

        try {
            port.checkHello("input");
            fail("Exception is expected");
        } catch (MyFault e) {
            assertEquals("Fault is not expected", "myMessage", e.getMessage());
        }

    }

}

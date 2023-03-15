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

package org.apache.cxf.systest.ws.addressing;

import jakarta.jws.WebService;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests the addition of WS-Addressing Message Addressing Properties
 * in the non-decoupled case.
 */
public class NonDecoupledTest extends MAPTestBase {
    static final String PORT = allocatePort(NonDecoupledTest.class);
    static final String ADDRESS = "http://localhost:" + PORT + "/SoapContext/SoapPort";

    private static final String CONFIG =
        "org/apache/cxf/systest/ws/addressing/wsa_interceptors.xml";

    public String getConfigFileName() {
        return CONFIG;
    }

    public String getPort() {
        return PORT;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, null,
                                new String[] {ADDRESS, GreeterImpl.class.getName()}, true));
    }

    @WebService(serviceName = "SOAPServiceAddressing",
                portName = "SoapPort",
                endpointInterface = "org.apache.hello_world_soap_http.Greeter",
                targetNamespace = "http://apache.org/hello_world_soap_http",
                wsdlLocation = "testutils/hello_world.wsdl")
    public static class GreeterImpl extends org.apache.cxf.systest.ws.addressing.AbstractGreeterImpl {
        GreeterImpl() {
            super(true);
        }
    }
    public String getAddress() {
        return ADDRESS;
    }


    @Test
    public void foo() {

    }

}


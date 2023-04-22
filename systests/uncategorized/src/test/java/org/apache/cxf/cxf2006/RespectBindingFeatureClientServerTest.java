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

package org.apache.cxf.cxf2006;

import javax.xml.namespace.QName;

import jakarta.xml.ws.RespectBindingFeature;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.ServerLauncher;
import org.apache.hello_world_rpclit.GreeterRPCLit;
import org.apache.hello_world_rpclit.SOAPServiceRPCLit;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RespectBindingFeatureClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    private final QName portName = new QName("http://apache.org/hello_world_rpclit", "SoapPortRPCLit");
    private SOAPServiceRPCLit service = new SOAPServiceRPCLit();
    private ServerLauncher serverLauncher;

    @After
    public void tearDown() throws Exception {
        if (null != serverLauncher) {
            serverLauncher.signalStop();
            serverLauncher.stopServer();
        }
    }

    private void startServers(String wsdlLocation) throws Exception {
        String[] args = new String[] {wsdlLocation};

        serverLauncher = new ServerLauncher(Server.class.getName(), null, args, true);
        boolean isServerReady = serverLauncher.launchServer();

        assertTrue("server did not launch correctly", isServerReady);
        createStaticBus();
    }

    @Test
    public void testRespectBindingFeature() throws Exception {
        startServers("/wsdl_systest/cxf2006.wsdl");

        try {
            GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class,
                                                    new RespectBindingFeature(true));
            updateAddressPort(greeter, PORT);
            greeter.greetMe("hello");
            fail("WebServiceException is expected");
        } catch (jakarta.xml.ws.WebServiceException ex) {
            assertTrue("RespectBindingFeature message is expected: " + ex.getMessage(),
                       ex.getMessage().indexOf("extension with required=true attribute") > -1);
        }
    }

    @Test
    public void testOperationRespectBindingFeature() throws Exception {
        startServers("/wsdl_systest/cxf_operation_respectbing.wsdl");

        try {
            GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class,
                                                    new RespectBindingFeature(true));
            updateAddressPort(greeter, PORT);
            greeter.greetMe("hello");
            fail("WebServiceException is expected");
        } catch (jakarta.xml.ws.WebServiceException ex) {
            assertTrue("RespectBindingFeature message is expected: " + ex.getMessage(),
                       ex.getMessage().indexOf("extension with required=true attribute") > -1);
        }
    }

    @Test
    public void testOperationInputRespectBindingFeature() throws Exception {
        startServers("/wsdl_systest/cxf_operation_input_respectbing.wsdl");

        try {
            GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class,
                                                    new RespectBindingFeature(true));
            updateAddressPort(greeter, PORT);
            greeter.greetMe("hello");
            fail("WebServiceException is expected");
        } catch (jakarta.xml.ws.WebServiceException ex) {
            assertTrue("RespectBindingFeature message is expected: " + ex.getMessage(),
                       ex.getMessage().indexOf("extension with required=true attribute") > -1);
        }
    }

    @Test
    public void testOperationOutputRespectBindingFeature() throws Exception {
        startServers("/wsdl_systest/cxf_operation_output_respectbing.wsdl");

        try {
            GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class,
                                                    new RespectBindingFeature(true));
            updateAddressPort(greeter, PORT);
            greeter.greetMe("hello");
            fail("WebServiceException is expected");
        } catch (jakarta.xml.ws.WebServiceException ex) {
            assertTrue("RespectBindingFeature message is expected: " + ex.getMessage(),
                       ex.getMessage().indexOf("extension with required=true attribute") > -1);
        }
    }

    @Test
    public void testRespectBindingFeatureFalse() throws Exception {
        startServers("/wsdl_systest/cxf2006.wsdl");

        GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class,
                                                new RespectBindingFeature(false));
        updateAddressPort(greeter, PORT);
        assertEquals("Bonjour", greeter.sayHi());
    }

}

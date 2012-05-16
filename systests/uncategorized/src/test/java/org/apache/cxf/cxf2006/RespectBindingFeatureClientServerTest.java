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
import javax.xml.ws.RespectBindingFeature;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_rpclit.GreeterRPCLit;
import org.apache.hello_world_rpclit.SOAPServiceRPCLit;
import org.junit.BeforeClass;
import org.junit.Test;

public class RespectBindingFeatureClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    private final QName portName = new QName("http://apache.org/hello_world_rpclit", "SoapPortRPCLit");
    private SOAPServiceRPCLit service = new SOAPServiceRPCLit();

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }

    @Test
    public void testRespectBindingFeature() throws Exception {
        try {
            GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class,
                                                    new RespectBindingFeature(true));
            updateAddressPort(greeter, PORT);
            greeter.greetMe("hello");
            fail("WebServiceException is expected");
        } catch (Exception ex) {
            assertTrue("WebServiceException is expected", ex instanceof javax.xml.ws.WebServiceException);
            assertTrue("RespectBindingFeature message is expected: " + ex.getMessage(),
                       ex.getMessage().indexOf("extension with required=true attribute") > -1);
        }
    }

    @Test
    public void testRespectBindingFeatureFalse() throws Exception {

        GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class,
                                                new RespectBindingFeature(false));
        updateAddressPort(greeter, PORT);
        assertEquals("Bonjour" , greeter.sayHi());
    }

}

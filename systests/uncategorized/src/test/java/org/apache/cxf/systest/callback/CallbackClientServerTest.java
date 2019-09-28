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

package org.apache.cxf.systest.callback;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.callback.SOAPService;
import org.apache.callback.ServerPortType;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CallbackClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    public static final String CB_PORT = allocatePort(CallbackClientServerTest.class);

    private static final QName SERVICE_NAME
        = new QName("http://apache.org/callback", "SOAPService");

    private static final QName PORT_NAME
        = new QName("http://apache.org/callback", "SOAPPort");


    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }

    @Test
    public void testCallback() throws Exception {


        Object implementor = new CallbackImpl();
        String address = "http://localhost:" + CB_PORT + "/CallbackContext/CallbackPort";
        Endpoint ep = Endpoint.publish(address, implementor);

        URL wsdlURL = getClass().getResource("/wsdl/basic_callback_test.wsdl");

        SOAPService ss = new SOAPService(wsdlURL, SERVICE_NAME);
        ServerPortType port = ss.getPort(PORT_NAME, ServerPortType.class);
        updateAddressPort(port, PORT);


        EndpointReference w3cEpr = ep.getEndpointReference();
        String resp = port.registerCallback((W3CEndpointReference)w3cEpr);
        assertEquals("registerCallback called", resp);
        ep.stop();
    }


}

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

package org.apache.cxf.systest.ws.addr_responses;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.systest.ws.AbstractWSATestBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WSAResponsesClientServerTest extends AbstractWSATestBase {
    static final String PORT = allocatePort(Server.class);
    @Before
    public void setUp() throws Exception {
        createBus();
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testWSAResponses() throws Exception {
        this.setupInLogging();
        this.setupOutLogging();
        URL wsdlURL = new URL("http://localhost:" + PORT + "/wsa/responses?wsdl");
        QName serviceQName = new QName("http://cxf.apache.org/systest/wsa/responses", "HelloService");
        HelloService service = new HelloService(wsdlURL, serviceQName);
        try {
            service.getHelloPort().sayHi("helloWorld");
            fail("Expect exception");
        } catch (SOAPFaultException e) {
            String expectedDetail = "A header representing a Message Addressing Property is not valid";
            assertTrue("Expect fault detail : " + expectedDetail,
                       e.getMessage().indexOf(expectedDetail) > -1);
        }
    }

}

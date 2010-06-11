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

package org.apache.cxf.systest.versioning;

import java.lang.reflect.UndeclaredThrowableException;

import javax.xml.namespace.QName;


import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.hello_world_mixedstyle.Greeter;
import org.apache.hello_world_mixedstyle.SOAPService;
import org.apache.hello_world_mixedstyle.types.GreetMe1;
import org.apache.hello_world_mixedstyle.types.GreetMeResponse;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientServerVersioningTest extends AbstractClientServerTestBase {
    private static final String PORT = Server.PORT;
    private final QName portName = new QName("http://apache.org/hello_world_mixedstyle", "SoapPort");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }
    
    @Test
    public void testVersionBasedRouting() throws Exception {

        SOAPService service = new SOAPService();
        assertNotNull(service);

        try {
            Greeter greeter = service.getPort(portName, Greeter.class);
            updateAddressPort(greeter, PORT);

            GreetMe1 request = new GreetMe1();
            request.setRequestType("Bonjour");
            GreetMeResponse greeting = greeter.greetMe(request);
            assertNotNull("no response received from service", greeting);
            assertEquals("Hello Bonjour version1", greeting.getResponseType());

            String reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals("Bonjour version2", reply);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

}

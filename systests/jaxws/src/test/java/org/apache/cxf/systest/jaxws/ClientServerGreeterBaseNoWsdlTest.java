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

package org.apache.cxf.systest.jaxws;

import java.lang.reflect.UndeclaredThrowableException;


import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClientServerGreeterBaseNoWsdlTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(ServerGreeterBaseNoWsdl.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(ServerGreeterBaseNoWsdl.class, true));
    }

    @Test
    public void testInvocation() throws Exception {

        GreeterService service = new GreeterService();
        assertNotNull(service);

        try {
            Greeter greeter = service.getGreeterPort();
            updateAddressPort(greeter, PORT);
            String greeting = greeter.greetMe("Bonjour");
            assertNotNull("no response received from service", greeting);
            assertEquals("Hello Bonjour", greeting);

        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

}

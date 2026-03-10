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

package org.apache.cxf.systest.http;


import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPException;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HTTPConduitIoExceptionsTest extends AbstractBusClientServerTestBase {
    @Rule public final ExpectedException exception = ExpectedException.none();

    private final QName serviceName =
        new QName("http://apache.org/hello_world", "SOAPService");
      
    private final QName mortimerQ =
        new QName("http://apache.org/hello_world", "Mortimer");

    public HTTPConduitIoExceptionsTest() {
    }


    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(BadServer.class, true));
    }

    @Test
    public void testNoIoExceptions() throws Exception {
        final Greeter greeter = getGreeter();

        try (Client client = (Client)greeter) {
            client.getRequestContext().put(HTTPConduit.NO_IO_EXCEPTIONS, true);
            
            exception.expect(SOAPFaultException.class);
            exception.expectMessage("Go away");
            
            greeter.sayHi();
        }
    }

    @Test
    public void testServiceUnavailable() throws Exception {
        final Greeter greeter = getGreeter();

        exception.expect(WebServiceException.class);
        exception.expectCause(new TypeSafeMatcher<Throwable>() {
            private static final String message = "HTTP response '503: Service Unavailable' when "
                + "communicating with http://localhost:" + BadServer.PORT + "/Mortimer";

            @Override
            public void describeTo(Description description) {
                description
                    .appendValue(HTTPException.class)
                    .appendText(" and message ")
                    .appendValue(message);
            }

            @Override
            protected boolean matchesSafely(Throwable item) {
                return item instanceof HTTPException && item.getMessage().equals(message);
            }
        });
        
        
        greeter.sayHi();
    }

    @Test
    public void testNotFound() throws Exception {
        final Greeter greeter = getGreeter();

        exception.expect(WebServiceException.class);
        exception.expectCause(new TypeSafeMatcher<Throwable>() {
            private static final String message = "HTTP response '404: Not Found' when "
                + "communicating with http://localhost:" + BadServer.PORT + "/Mortimer";

            @Override
            public void describeTo(Description description) {
                description
                    .appendValue(HTTPException.class)
                    .appendText(" and message ")
                    .appendValue(message);
            }

            @Override
            protected boolean matchesSafely(Throwable item) {
                return item instanceof HTTPException && item.getMessage().equals(message);
            }
        });
        
        
        greeter.greetMe("Test");
    }
    
    private Greeter getGreeter() throws MalformedURLException {
        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter mortimer = service.getPort(mortimerQ, Greeter.class);
        assertNotNull("Port is null", mortimer);
        updateAddressPort(mortimer, BadServer.PORT);

        return mortimer;
    }
}


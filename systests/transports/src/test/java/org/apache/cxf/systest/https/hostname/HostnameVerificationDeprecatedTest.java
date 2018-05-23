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

package org.apache.cxf.systest.https.hostname;

import java.net.URL;

import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * A test for hostname verification when the Java system property "java.protocol.handler.pkgs" is set to 
 * "com.sun.net.ssl.internal.www.protocol". This means that com.sun.net.ssl.HostnameVerifier is used 
 * instead of the javax version.
 */
public class HostnameVerificationDeprecatedTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(HostnameVerificationDeprecatedServer.class);
    static final String PORT2 = allocatePort(HostnameVerificationDeprecatedServer.class, 2);

    @BeforeClass
    public static void startServers() throws Exception {
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(HostnameVerificationDeprecatedServer.class, true)
        );
    }

    @AfterClass
    public static void cleanup() throws Exception {
        System.clearProperty("java.protocol.handler.pkgs");
        stopAllServers();
    }

    // Here we expect an exception, as the default hostname verifier contributed by CXF will object to the
    // fact that the server certificate does not have "CN=localhost".
    @org.junit.Test
    public void testLocalhostNotMatching() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HostnameVerificationDeprecatedTest.class.getResource("hostname-client-bethal.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on the hostname verification");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // No Subject Alternative Name, but the CN matches ("localhost"), so the default HostnameVerifier
    // should work fine
    @org.junit.Test
    public void testNoSubjectAlternativeNameCNMatch() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HostnameVerificationDeprecatedTest.class.getResource("hostname-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT2);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
}

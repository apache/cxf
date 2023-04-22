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

package org.apache.cxf.transport.udp;

import java.net.NetworkInterface;
import java.util.Enumeration;

import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.hello_world.Greeter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class UDPTransportTest {
    private static final String PORT = TestUtil.getPortNumber(UDPTransportTest.class);
    private static Server server;

    static class GreeterImpl implements Greeter {
        private String myName = "defaultGreeter";

        public String greetMe(String me) {
            return "Hello " + me;
        }
        public String sayHi() {
            return "Bonjour from " + myName;
        }
        public void pingMe() {
            throw new UnsupportedOperationException();
        }
    }


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setAddress("udp://:" + PORT);
        factory.setServiceClass(Greeter.class);
        factory.setServiceBean(new GreeterImpl());
        // factory.setFeatures(Collections.singletonList(new LoggingFeature()));
        server = factory.create();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testSimpleUDP() throws Exception {
        JaxWsProxyFactoryBean fact = new JaxWsProxyFactoryBean();
        fact.setAddress("udp://localhost:" + PORT);
        Greeter g = fact.create(Greeter.class);
        for (int x = 0; x < 5; x++) {
            final String message = Integer.toString(x);
            assertTrue(g.greetMe(message).endsWith(message));
        }

        ((java.io.Closeable)g).close();
    }

    @Test
    public void testBroadcastUDP() throws Exception {
        // Disable the test on Redhat Enterprise Linux which doesn't enable the UDP broadcast by default
        if ("Linux".equals(System.getProperties().getProperty("os.name"))
            && System.getProperties().getProperty("os.version").indexOf("el") > 0) {
            System.out.println("Skipping broadcast test for REL");
            return;
        }

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        int count = 0;
        if (interfaces != null) {
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                count++;
            }
        }
        if (count == 0) {
            //no non-loopbacks, cannot do broadcasts
            System.out.println("Skipping broadcast test");
            return;
        }

        JaxWsProxyFactoryBean fact = new JaxWsProxyFactoryBean();
        fact.setAddress("udp://:" + PORT + "/foo");
        Greeter g = fact.create(Greeter.class);
        assertEquals("Hello World", g.greetMe("World"));

        ((java.io.Closeable)g).close();
    }

    @Test
    public void testLargeRequest() throws Exception {
        JaxWsProxyFactoryBean fact = new JaxWsProxyFactoryBean();
        fact.setAddress("udp://localhost:" + PORT);
        Greeter g = fact.create(Greeter.class);
        StringBuilder b = new StringBuilder(100000);
        for (int x = 0; x < 6500; x++) {
            b.append("Hello ");
        }
        assertEquals("Hello " + b.toString(), g.greetMe(b.toString()));

        ((java.io.Closeable)g).close();
    }

    @Test
    public void testFailure() throws Exception {
        if ("Mac OS X".equals(System.getProperties().getProperty("os.name")) && !JavaUtils.isJava11Compatible()) {
            //Seems to fail fairly consistently on OSX on Java 8 with newer versions of OSX
            // java11 seems to be OK
            System.out.println("Skipping failure test for OSX");
            return;
        }
        
        JaxWsProxyFactoryBean fact = new JaxWsProxyFactoryBean();
        fact.setAddress("udp://localhost:" + PORT);
        Greeter g = fact.create(Greeter.class);
        try {
            g.pingMe();
            fail("Expected SOAPFaultException");
        } catch (SOAPFaultException ex) {
            //expected
        }
    }
}

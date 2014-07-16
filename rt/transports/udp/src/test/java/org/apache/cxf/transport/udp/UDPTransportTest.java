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

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.GreeterImpl;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class UDPTransportTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(UDPTransportTest.class);
    private static Server server; 

    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        createStaticBus();
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(getStaticBus());
        factory.setAddress("udp://:" + PORT);
        factory.setServiceBean(new GreeterImpl());
        server = factory.create();
    }
    
    @AfterClass 
    public static void shutdown() throws Exception {
        server.stop();
    }

    @Test
    public void testSimpleUDP() throws Exception {
        JaxWsProxyFactoryBean fact = new JaxWsProxyFactoryBean(); 
        fact.setAddress("udp://localhost:" + PORT);
        Greeter g = fact.create(Greeter.class);
        for (int x = 0; x < 5; x++) {
            assertEquals("Hello World", g.greetMe("World"));
        }
               
        ((java.io.Closeable)g).close();
    }
    @Test
    public void testBroadcastUDP() throws Exception {
        // Disable the test on Redhat Enterprise Linux which doesn't enable the UDP broadcast by default
        if (System.getProperties().getProperty("os.name").equals("Linux") 
            && System.getProperties().getProperty("os.version").indexOf("el") > 0) {
            System.out.println("Skipping broadcast test for REL");
            return;
        }
        
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        int count = 0;
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                continue;  
            }
            count++;
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
}

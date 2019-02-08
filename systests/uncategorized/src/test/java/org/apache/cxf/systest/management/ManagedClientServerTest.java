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

package org.apache.cxf.systest.management;

import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.jmx.InstrumentationManagerImpl;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.hello_world_soap_http.SOAPService;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ManagedClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(ManagedClientServerTest.class);
    public static final String JMX_PORT = allocatePort(ManagedClientServerTest.class, 1);


    private final QName portName =
        new QName("http://apache.org/hello_world_soap_http",
                  "SoapPort");

    public static class Server extends AbstractBusTestServerBase {
        Endpoint ep;
        protected void run() {
            Object implementor = new GreeterImpl();
            ep = Endpoint.publish("http://localhost:" + PORT + "/SoapContext/SoapPort",
                implementor);
        }
        public void tearDown() {
            ep.stop();
        }

    }

    @BeforeClass
    public static void startServers() throws Exception {
        createStaticBus("org/apache/cxf/systest/management/managed-spring.xml");
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testManagedEndpoint() throws Exception {
        Bus bus = getStaticBus();
        BusFactory.setDefaultBus(bus);
        InstrumentationManager im = bus.getExtension(InstrumentationManager.class);
        assertNotNull(im);
        InstrumentationManagerImpl impl = (InstrumentationManagerImpl)im;
        assertTrue(impl.isEnabled());
        assertNotNull(impl.getMBeanServer());

        MBeanServer mbs = im.getMBeanServer();
        ObjectName name = new ObjectName(ManagementConstants.DEFAULT_DOMAIN_NAME
                                         + ":type=Bus.Service.Endpoint,*");
        Set<?> s = mbs.queryNames(name, null);
        assertEquals(1, s.size());
        name = (ObjectName)s.iterator().next();

        Object val = mbs.invoke(name, "getState", new Object[0], new String[0]);
        assertEquals("Service should have been started.", "STARTED", val);

        SOAPService service = new SOAPService();
        assertNotNull(service);

        Greeter greeter = service.getPort(portName, Greeter.class);
        updateAddressPort(greeter, PORT);
        String response = new String("Bonjour");
        String reply = greeter.sayHi();
        assertNotNull("no response received from service", reply);
        assertEquals(response, reply);

        mbs.invoke(name, "stop", new Object[0], new String[0]);

        val = mbs.getAttribute(name, "State");

        assertEquals("Service should have been stopped.", "STOPPED", val);

        try {
            reply = greeter.sayHi();
            fail("Endpoint should not be active at this point.");
        } catch (Exception ex) {
            //Expected
        }

        mbs.invoke(name, "start", new Object[0], new String[0]);

        val = mbs.invoke(name, "getState", new Object[0], new String[0]);
        assertEquals("Service should have been started.", "STARTED", val);

        reply = greeter.sayHi();
        assertNotNull("no response received from service", reply);
        assertEquals(response, reply);

        mbs.invoke(name, "destroy", new Object[0], new String[0]);

        try {
            mbs.getMBeanInfo(name);
            fail("destroy failed to unregister MBean.");
        } catch (InstanceNotFoundException e) {
            // Expected
        }
    }
}

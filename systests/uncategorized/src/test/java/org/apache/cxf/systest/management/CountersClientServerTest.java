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

import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.counters.CounterRepository;
import org.apache.cxf.management.jmx.InstrumentationManagerImpl;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.hello_world_soap_http.SOAPService;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CountersClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(CountersClientServerTest.class);
    public static final String JMX_PORT = allocatePort(CountersClientServerTest.class, 1);

    private final QName portName =
        new QName("http://apache.org/hello_world_soap_http",
                  "SoapPort");

    public static class Server extends AbstractBusTestServerBase {
        Endpoint ep;
        protected void run() {
            Object implementor = new GreeterImpl();
            ep = Endpoint.publish("http://localhost:" + PORT + "/SoapContext/SoapPort", implementor);
        }
        public void tearDown() {
            ep.stop();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        createStaticBus("org/apache/cxf/systest/management/counter-spring.xml");
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testCountersWithInstrumentationManager() throws Exception {
        // create Client with other bus
        Bus bus = getStaticBus();
        BusFactory.setDefaultBus(bus);
        bus.getExtension(WorkQueueManager.class);

        CounterRepository cr = bus.getExtension(CounterRepository.class);
        InstrumentationManager im = bus.getExtension(InstrumentationManager.class);
        assertNotNull(im);
        InstrumentationManagerImpl impl = (InstrumentationManagerImpl)im;
        assertTrue(impl.isEnabled());
        assertNotNull(impl.getMBeanServer());

        MBeanServer mbs = im.getMBeanServer();
        ObjectName name = new ObjectName(ManagementConstants.DEFAULT_DOMAIN_NAME
            + ":" + ManagementConstants.BUS_ID_PROP + "=cxf" + bus.hashCode() + ",*");

        SOAPService service = new SOAPService();
        assertNotNull(service);

        Greeter greeter = service.getPort(portName, Greeter.class);
        updateAddressPort(greeter, PORT);

        String response = new String("Bonjour");
        String reply = greeter.sayHi();

        //assertNotNull("no response received from service", reply);
        //assertEquals(response, reply);

        assertEquals("The Counters are not create yet", 4, cr.getCounters().size());
        Set<?> counterNames = mbs.queryNames(name, null);
        assertEquals("The Counters are not export to JMX: " + counterNames,
                     4 + 3, counterNames.size());

        ObjectName sayHiCounter = new ObjectName(
            ManagementConstants.DEFAULT_DOMAIN_NAME + ":operation=\"sayHi\",*");

        Set<?> s = mbs.queryNames(sayHiCounter, null);
        Iterator<?> it = s.iterator();

        while (it.hasNext()) {
            ObjectName counterName = (ObjectName)it.next();
            Object val = mbs.getAttribute(counterName, "NumInvocations");
            assertEquals("Wrong Counters Number of Invocations", val, 1);
        }

        reply = greeter.sayHi();
        assertNotNull("no response received from service", reply);
        assertEquals(response, reply);

        s = mbs.queryNames(sayHiCounter, null);
        it = s.iterator();

        while (it.hasNext()) {
            ObjectName counterName = (ObjectName)it.next();
            Object val = mbs.getAttribute(counterName, "NumInvocations");
            assertEquals("Wrong Counters Number of Invocations", val, 2);
        }

        greeter.greetMeOneWay("hello");
        for (int count = 0; count < 10; count++) {
            if (6 != cr.getCounters().size()) {
                Thread.sleep(100);
            } else {
                break;
            }
        }
        assertEquals("The Counters are not create yet", 6, cr.getCounters().size());
        for (int count = 0; count < 10; count++) {
            if (10 > mbs.queryNames(name, null).size()) {
                Thread.sleep(100);
            } else {
                break;
            }
        }
        counterNames = mbs.queryNames(name, null);
        assertEquals("The Counters are not export to JMX " + counterNames, 6 + 4, counterNames.size());

        ObjectName greetMeOneWayCounter = new ObjectName(
            ManagementConstants.DEFAULT_DOMAIN_NAME + ":operation=\"greetMeOneWay\",*");

        s = mbs.queryNames(greetMeOneWayCounter, null);
        it = s.iterator();

        while (it.hasNext()) {
            ObjectName counterName = (ObjectName)it.next();
            Object val = mbs.getAttribute(counterName, "NumInvocations");
            assertEquals("Wrong Counters Number of Invocations", val, 1);
        }
    }
}

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

package org.apache.cxf.systest.lifecycle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jws.WebService;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientLifeCycleListener;
import org.apache.cxf.endpoint.ClientLifeCycleManager;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.apache.cxf.endpoint.ServerLifeCycleManager;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.greeter_control.ControlImpl;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.hello_world_soap_http.Greeter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LifeCycleTest {
    public static final String PORT1 = TestUtil.getPortNumber(LifeCycleTest.class, 1);
    public static final String PORT2 = TestUtil.getPortNumber(LifeCycleTest.class, 2);
    public static final String PORT3 = TestUtil.getPortNumber(LifeCycleTest.class, 3);
    public static final String PORT4 = TestUtil.getPortNumber(LifeCycleTest.class, 4);

    private static final int RECURSIVE_LIMIT = 3;
    private static final String[] ADDRESSES =
    {"http://localhost:" + PORT1 + "/SoapContext/SoapPort",
     "http://localhost:" + PORT2 + "/SoapContext/SoapPort",
     "http://localhost:" + PORT3 + "/SoapContext/SoapPort",
     "http://localhost:" + PORT4 + "/SoapContext/SoapPort"};
    private static final String CONFIG =
        "org/apache/cxf/systest/lifecycle/cxf.xml";

    private Bus bus;
    private ServerLifeCycleManager manager;
    private int recursiveCount;
    private Endpoint[] recursiveEndpoints;
    private Map<String, Integer> startNotificationMap;
    private Map<String, Integer> stopNotificationMap;

    @Before
    public void setUp() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus(CONFIG);
        BusFactory.setDefaultBus(bus);
        manager = bus.getExtension(ServerLifeCycleManager.class);
        recursiveCount = 0;
        recursiveEndpoints = new Endpoint[RECURSIVE_LIMIT];
        startNotificationMap = new HashMap<>();
        stopNotificationMap = new HashMap<>();
    }

    @After
    public void tearDown() throws Exception {
        bus.shutdown(true);
    }

    @Test
    public void testClientLifecycle() throws Exception {
        final AtomicBoolean created = new AtomicBoolean();
        final AtomicBoolean destroyed = new AtomicBoolean();

        bus.getExtension(ClientLifeCycleManager.class)
            .registerListener(new ClientLifeCycleListener() {
                public void clientCreated(Client client) {
                    created.set(true);
                }

                public void clientDestroyed(Client client) {
                    destroyed.set(true);
                }
            });

        org.apache.hello_world_soap_http.SOAPService service
            = new org.apache.hello_world_soap_http.SOAPService();

        Greeter client = service.getSoapPort();
        ((BindingProvider)client).getRequestContext()
            .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                 ADDRESSES[0]);
        assertTrue("clientCreated not called", created.get());
        client = null;
        int count = 0;
        while (count < 10 && !destroyed.get()) {
            System.gc();
            System.runFinalization();
            count++;
            if (count > 5) {
                Thread.sleep(100);
            }
        }
        assertTrue("clientDestroyed not called", destroyed.get());

        created.set(false);
        destroyed.set(false);

        client = service.getSoapPort();
        ((BindingProvider)client).getRequestContext()
            .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                 ADDRESSES[0]);
        assertTrue("clientCreated not called", created.get());
        ((java.io.Closeable)client).close();
        assertTrue("clientDestroyed not called", destroyed.get());
    }

    @Test
    public void testRecursive() {
        assertNotNull("unexpected non-null ServerLifeCycleManager", manager);

        manager.registerListener(new ServerLifeCycleListener() {
            public void startServer(Server server) {
                String address =
                    server.getEndpoint().getEndpointInfo().getAddress();
                verifyNotification(startNotificationMap, address, 0);
                updateMap(startNotificationMap, address);
                if (recursiveCount < RECURSIVE_LIMIT) {
                    recursiveEndpoints[recursiveCount++] =
                        Endpoint.publish(ADDRESSES[recursiveCount],
                                         new GreeterImpl());
                }
            }
            public void stopServer(Server server) {
                String address =
                    server.getEndpoint().getEndpointInfo().getAddress();
                verifyNotification(stopNotificationMap, address, 0);
                updateMap(stopNotificationMap, address);
                if (recursiveCount > 0) {
                    recursiveEndpoints[--recursiveCount].stop();
                }
            }
        });

        Endpoint.publish(ADDRESSES[0], new GreeterImpl()).stop();
        for (int i = 0; i < ADDRESSES.length; i++) {
            verifyNotification(startNotificationMap, ADDRESSES[i], 1);
            verifyNotification(stopNotificationMap, ADDRESSES[i], 1);
        }
    }

    @Test
    public void testGetActiveFeatures() {
        assertNotNull("unexpected non-null ServerLifeCycleManager", manager);

        manager.registerListener(new ServerLifeCycleListener() {
            public void startServer(Server server) {
                org.apache.cxf.endpoint.Endpoint endpoint
                    = server.getEndpoint();
                updateMap(startNotificationMap,
                          endpoint.getEndpointInfo().getAddress());
                String portName =
                    endpoint.getEndpointInfo().getName().getLocalPart();
                if ("SoapPort".equals(portName)) {

                    List<Feature> active = endpoint.getActiveFeatures();
                    assertNotNull(active);
                    assertEquals(1, active.size());
                    assertTrue(active.get(0) instanceof WSAddressingFeature);
                    assertSame(active.get(0),
                               AbstractFeature.getActive(active,
                                                         WSAddressingFeature.class));
                } else {
                    List<Feature> active = endpoint.getActiveFeatures();
                    assertNotNull(active);
                    assertEquals(0, active.size());
                    assertNull(AbstractFeature.getActive(active,
                                                         WSAddressingFeature.class));
                }
            }
            public void stopServer(Server server) {
                updateMap(stopNotificationMap,
                          server.getEndpoint().getEndpointInfo().getAddress());
            }
        });

        Endpoint greeter = Endpoint.publish(ADDRESSES[0], new GreeterImpl());
        Endpoint control = Endpoint.publish(ADDRESSES[1], new ControlImpl());
        greeter.stop();
        control.stop();
        for (int i = 0; i < 2; i++) {
            verifyNotification(startNotificationMap, ADDRESSES[i], 1);
            verifyNotification(stopNotificationMap, ADDRESSES[i], 1);
        }
    }

    private void verifyNotification(Map<String, Integer> notificationMap,
                                    String address,
                                    int expected) {
        synchronized (notificationMap) {
            Integer count = notificationMap.get(address);
            if (expected == 0) {
                assertNull("unexpected prior notification for: " + address, count);
            } else {
                assertEquals("unexpected prior notification for: " + address,
                             expected,
                             count.intValue());
            }
        }
    }

    private void updateMap(Map<String, Integer> notificationMap, String address) {
        synchronized (notificationMap) {
            Integer count = notificationMap.get(address);
            if (count != null) {
                notificationMap.put(address,
                                    Integer.valueOf(count.intValue() + 1));
            } else {
                notificationMap.put(address,
                                    Integer.valueOf(1));
            }
        }
    }


    @WebService(serviceName = "SOAPServiceAddressing",
                portName = "SoapPort",
                endpointInterface = "org.apache.hello_world_soap_http.Greeter",
                targetNamespace = "http://apache.org/hello_world_soap_http",
                wsdlLocation = "wsdl/hello_world.wsdl")
    public class GreeterImpl extends org.apache.hello_world_soap_http.BaseGreeterImpl {

    }
}
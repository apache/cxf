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

package org.apache.cxf.systest.clustering;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.clustering.FailoverTargetSelector;
import org.apache.cxf.clustering.RandomStrategy;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.ClusteredGreeterService;
import org.apache.cxf.greeter_control.Control;
import org.apache.cxf.greeter_control.ControlService;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.PingMeFault;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPException;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.soap.MAPCodec;
import org.apache.cxf.wsdl.WSDLManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests failover within a static cluster.
 */
public class FailoverTest extends AbstractBusClientServerTestBase {
    public static final String PORT_0 = allocatePort(Server.class, 0);
    public static final String PORT_A = allocatePort(Server.class, 1);
    public static final String PORT_B = allocatePort(Server.class, 2);
    public static final String PORT_C = allocatePort(Server.class, 3);
    public static final String PORT_D = allocatePort(Server.class, 4);
    public static final String PORT_E = allocatePort(Server.class, 5);


    protected static final String REPLICA_A =
        "http://localhost:" + PORT_A + "/SoapContext/ReplicatedPortA";
    protected static final String REPLICA_B =
        "http://localhost:" + PORT_B + "/SoapContext/ReplicatedPortB";
    protected static final String REPLICA_C =
        "http://localhost:" + PORT_C + "/SoapContext/ReplicatedPortC";
    protected static final String REPLICA_D =
        "http://localhost:" + PORT_D + "/SoapContext/ReplicatedPortD";
    protected static final String REPLICA_E =
        "http://localhost:" + PORT_E + "/SoapContext/ReplicatedPortE";
    private static final Logger LOG =
        LogUtils.getLogger(FailoverTest.class);
    private static final String FAILOVER_CONFIG =
        "org/apache/cxf/systest/clustering/failover.xml";
    private static String wsdlLocation = ClusteredGreeterService.WSDL_LOCATION.toString();

    protected Bus bus;
    protected Greeter greeter;
    protected List<String> targets;
    protected Control control;
    private MAPAggregator mapAggregator;
    private MAPCodec mapCodec;


    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(Server.class));
    }

    protected String getConfig() {
        return FAILOVER_CONFIG;
    }

    @Before
    public void setUp() {
        targets = new ArrayList<>();
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus(getConfig());
        BusFactory.setDefaultBus(bus);

        updateWsdlExtensors("9051", PORT_A);
        updateWsdlExtensors("9052", PORT_B);
        updateWsdlExtensors("9053", PORT_C);
        updateWsdlExtensors("9055", PORT_E);
    }

    @After
    public void tearDown() {
        if (null != control) {
            for (String address : targets) {
                assertTrue("Failed to stop greeter",
                           control.stopGreeter(address));
            }
        }
        targets = null;
        if (bus != null) {
            bus.shutdown(true);
            bus = null;
        }
    }

    @Test
    public void testNoFailoverAcrossBindings() throws Exception {
        startTarget(REPLICA_D);
        setupGreeter();

        try {
            greeter.greetMe("fred");
            fail("expected exception");
        } catch (Exception e) {
            verifyCurrentEndpoint(REPLICA_A);
        }
    }

    @Test
    public void testRevertExceptionOnUnsucessfulFailover() throws Exception {
        startTarget(REPLICA_B);
        startTarget(REPLICA_C);
        setupGreeter();
        stopTarget(REPLICA_C);
        stopTarget(REPLICA_B);

        try {
            greeter.greetMe("fred");
            fail("expected exception");
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            // failover attempt bails after retried invocations on
            // started & stopped replicas B & C fail with HTTP 404
            // indicated by a thrown IOException("Not found"),
            // in which case we should revert back to the original
            // java.net.ConnectionException on the unavailable
            // replica A

            boolean isOrig = cause instanceof ConnectException;
            if (!isOrig) {
                //depending on the order of the tests,
                //the port COULD have been created, but no service deployed
                isOrig = cause instanceof HTTPException
                    && cause.getMessage().contains("SoapContext/ReplicatedPortA")
                    && cause.getMessage().contains("404:");
            }

            if (!isOrig) {
                cause.printStackTrace();
            }
            assertTrue("should revert to original exception when no failover: "
                       + cause,
                       isOrig);

            // similarly the current endpoint referenced by the client
            // should also revert back to the original replica A
            //
            verifyCurrentEndpoint(REPLICA_A);
        }
    }

    @Test
    public void testInitialFailoverOnPrimaryReplicaUnavailable() throws Exception {
        startTarget(REPLICA_C);
        setupGreeter();
        String response = null;
        response = greeter.greetMe("fred");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_C));
        verifyCurrentEndpoint(REPLICA_C);

        response = greeter.greetMe("joe");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_C));
    }

    @Test
    public void testNoFailoverOnApplicationFault() throws Exception {
        startTarget(REPLICA_C);
        setupGreeter();

        greeter.pingMe();
        verifyCurrentEndpoint(REPLICA_C);

        startTarget(REPLICA_B);

        try {
            greeter.pingMe();
        } catch (PingMeFault pmf) {
            verifyCurrentEndpoint(REPLICA_C);
        }
    }

    @Test
    public void testFailoverOnCurrentReplicaDeath() throws Exception {
        startTarget(REPLICA_C);
        setupGreeter();
        String response = null;

        response = greeter.greetMe("fred");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_C));
        verifyCurrentEndpoint(REPLICA_C);

        startTarget(REPLICA_B);
        stopTarget(REPLICA_C);

        response = greeter.greetMe("joe");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_B));
        verifyCurrentEndpoint(REPLICA_B);
    }

    @Test
    public void testNoFailbackWhileCurrentReplicaLive() throws Exception {
        startTarget(REPLICA_C);
        setupGreeter();
        String response = null;

        response = greeter.greetMe("fred");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_C));
        verifyCurrentEndpoint(REPLICA_C);

        startTarget(REPLICA_A);
        response = greeter.greetMe("joe");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_C));
        verifyCurrentEndpoint(REPLICA_C);

        startTarget(REPLICA_B);
        response = greeter.greetMe("bob");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_C));
        verifyCurrentEndpoint(REPLICA_C);

        stopTarget(REPLICA_B);
        response = greeter.greetMe("john");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_C));
        verifyCurrentEndpoint(REPLICA_C);

        stopTarget(REPLICA_A);
        response = greeter.greetMe("mike");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_C));
        verifyCurrentEndpoint(REPLICA_C);
    }

    @Test
    public void testEndpointSpecificInterceptorsDoNotPersistAcrossFailover()
        throws Exception {
        startTarget(REPLICA_A);
        setupGreeter();
        String response = null;

        enableWSAForCurrentEndpoint();

        response = greeter.greetMe("fred");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_A));
        assertTrue("response expected to include WS-A messageID",
                   response.indexOf("message: urn:uuid") != -1);
        verifyCurrentEndpoint(REPLICA_A);
        assertTrue("expected WSA enabled for current endpoint",
                   isWSAEnabledForCurrentEndpoint());

        stopTarget(REPLICA_A);
        startTarget(REPLICA_C);

        response = greeter.greetMe("mike");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_C));
        assertTrue("response not expected to include WS-A messageID",
                   response.indexOf("message: urn:uuid") == -1);
        verifyCurrentEndpoint(REPLICA_C);
        assertFalse("unexpected WSA enabled for current endpoint",
                    isWSAEnabledForCurrentEndpoint());
    }

    @Test
    public void testDefaultSequentialStrategy() throws Exception {
        strategyTest(REPLICA_B, REPLICA_C, REPLICA_A, false);
    }

    @Test
    public void testExplicitSequentialStrategy() throws Exception {
        strategyTest(REPLICA_A, REPLICA_C, REPLICA_B, false);
    }

    @Test
    public void testRandomStrategy() throws Exception {
        strategyTest(REPLICA_A, REPLICA_B, REPLICA_C, true);
    }

    protected Greeter getGreeter(String type) throws Exception {
        if (REPLICA_A.equals(type)) {
            Greeter g = new ClusteredGreeterService().getReplicatedPortA();
            updateAddressPort(g, PORT_A);
            updateWsdlExtensors("9051", PORT_A);
            return g;
        } else if (REPLICA_B.equals(type)) {
            Greeter g = new ClusteredGreeterService().getReplicatedPortB();
            updateAddressPort(g, PORT_B);
            updateWsdlExtensors("9052", PORT_B);
            return g;
        } else if (REPLICA_C.equals(type)) {
            Greeter g = new ClusteredGreeterService().getReplicatedPortC();
            updateAddressPort(g, PORT_C);
            updateWsdlExtensors("9053", PORT_C);
            return g;
        }

        Greeter g = new ClusteredGreeterService().getReplicatedPortE();
        updateAddressPort(g, PORT_E);
        updateWsdlExtensors("9055", PORT_E);
        return g;
    }
    protected void strategyTest(String activeReplica1,
                              String activeReplica2,
                              String inactiveReplica,
                              boolean expectRandom) throws Exception {
        startTarget(activeReplica1);
        startTarget(activeReplica2);
        boolean randomized = false;
        String prevEndpoint = null;
        for (int i = 0; i < 20; i++) {
            Greeter g = getGreeter(inactiveReplica);
            verifyStrategy(g, expectRandom
                              ? RandomStrategy.class
                              : SequentialStrategy.class);
            String response = g.greetMe("fred");
            assertNotNull("expected non-null response", response);
            String currEndpoint = getCurrentEndpoint(g);
            if (!(prevEndpoint == null || currEndpoint.equals(prevEndpoint))) {
                randomized = true;
            }
            prevEndpoint = currEndpoint;
        }
        stopTarget(activeReplica1);
        stopTarget(activeReplica2);
        assertEquals("unexpected random/sequential distribution of failovers",
                     expectRandom,
                     randomized);
    }

    protected void startTarget(String address) throws Exception {
        ControlService cs = new ControlService();
        control = cs.getControlPort();
        updateAddressPort(control, PORT_0);

        LOG.info("starting replicated target: " + address);
        assertTrue("Failed to start greeter", control.startGreeter(address));
        targets.add(address);
    }

    protected void stopTarget(String address) {
        if (control != null
            && targets.contains(address)) {
            LOG.info("starting replicated target: " + address);
            assertTrue("Failed to start greeter", control.stopGreeter(address));
            targets.remove(address);
        }
    }

    protected void verifyCurrentEndpoint(String replica) {
        assertEquals("unexpected current endpoint",
                     replica,
                     getCurrentEndpoint(greeter));
    }

    protected String getCurrentEndpoint(Object proxy) {
        return ClientProxy.getClient(proxy).getEndpoint().getEndpointInfo().getAddress();
    }

    protected void setupGreeter() throws Exception {
        ClusteredGreeterService cs = new ClusteredGreeterService();
        // REVISIT: why doesn't the generic (i.e. non-Port-specific)
        // Service.getPort() load the <jaxws:client> configuration?
        greeter = cs.getReplicatedPortA();
        updateAddressPort(greeter, PORT_A);
        assertTrue("unexpected conduit selector: "
                   + ClientProxy.getClient(greeter).getConduitSelector().getClass().getName(),
                   ClientProxy.getClient(greeter).getConduitSelector()
                   instanceof FailoverTargetSelector);

        updateWsdlExtensors("9051", PORT_A);
        updateWsdlExtensors("9052", PORT_B);
        updateWsdlExtensors("9053", PORT_C);
        updateWsdlExtensors("9055", PORT_E);
    }

    protected void verifyStrategy(Object proxy, Class<?> clz) {
        ConduitSelector conduitSelector =
            ClientProxy.getClient(proxy).getConduitSelector();
        if (conduitSelector instanceof FailoverTargetSelector) {
            Object strategy =
                ((FailoverTargetSelector)conduitSelector).getStrategy();
            assertTrue("unexpected strategy", clz.isInstance(strategy));
        } else {
            fail("unexpected conduit selector: " + conduitSelector);
        }
    }

    protected void enableWSAForCurrentEndpoint() {
        Endpoint provider = ClientProxy.getClient(greeter).getEndpoint();
        mapAggregator = new MAPAggregator();
        mapCodec = MAPCodec.getInstance(ClientProxy.getClient(greeter).getBus());
        provider.getInInterceptors().add(mapAggregator);
        provider.getInInterceptors().add(mapCodec);

        provider.getOutInterceptors().add(mapAggregator);
        provider.getOutInterceptors().add(mapCodec);

        provider.getInFaultInterceptors().add(mapAggregator);
        provider.getInFaultInterceptors().add(mapCodec);

        provider.getOutFaultInterceptors().add(mapAggregator);
        provider.getOutFaultInterceptors().add(mapCodec);
    }

    protected boolean isWSAEnabledForCurrentEndpoint() {
        Endpoint provider = ClientProxy.getClient(greeter).getEndpoint();
        boolean enabledIn =
            provider.getInInterceptors().contains(mapAggregator)
            && provider.getInInterceptors().contains(mapCodec)
            && provider.getInFaultInterceptors().contains(mapAggregator)
            && provider.getInFaultInterceptors().contains(mapCodec);
        boolean enabledOut =
            provider.getOutInterceptors().contains(mapAggregator)
            && provider.getOutInterceptors().contains(mapCodec)
            && provider.getOutFaultInterceptors().contains(mapAggregator)
            && provider.getOutFaultInterceptors().contains(mapCodec);
        return enabledIn && enabledOut;
    }


    /**
     * Exchange the port number in all service addresses on the bus.
     * @param port1 current port
     * @param port2 new port
     */
    private void updateWsdlExtensors(String port1, String port2) {
        try {
            Definition def = bus.getExtension(WSDLManager.class)
                .getDefinition(wsdlLocation);
            Map<?, ?> map = def.getAllServices();
            for (Object o : map.values()) {
                Service service = (Service)o;
                Map<?, ?> ports = service.getPorts();
                for (Object p : ports.values()) {
                    Port port = (Port)p;
                    List<?> l = port.getExtensibilityElements();
                    for (Object e : l) {
                        if (e instanceof SOAPAddress) {
                            String add = ((SOAPAddress)e).getLocationURI();
                            int idx = add.indexOf(":" + port1);
                            if (idx != -1) {
                                add = add.substring(0, idx) + ":" + port2
                                    + add.substring(idx + port1.length() + 1);
                                ((SOAPAddress)e).setLocationURI(add);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

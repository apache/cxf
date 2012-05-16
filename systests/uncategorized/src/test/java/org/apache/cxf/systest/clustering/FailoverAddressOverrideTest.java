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
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.clustering.AbstractStaticFailoverStrategy;
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
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.soap.MAPCodec;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests failover within a static cluster.
 */
public class FailoverAddressOverrideTest extends AbstractBusClientServerTestBase {
    public static final String PORT_0 = allocatePort(Server.class, 0);
    public static final String PORT_A = allocatePort(Server.class, 1);
    public static final String PORT_B = allocatePort(Server.class, 2);
    public static final String PORT_C = allocatePort(Server.class, 3);
    public static final String PORT_D = allocatePort(Server.class, 4);
    public static final String PORT_EXTRA = allocatePort(Server.class, 99);
    
    
    protected static final String REPLICA_A =
        "http://localhost:" + PORT_A + "/SoapContext/ReplicatedPortA";
    protected static final String REPLICA_B =
        "http://localhost:" + PORT_B + "/SoapContext/ReplicatedPortB"; 
    protected static final String REPLICA_C =
        "http://localhost:" + PORT_C + "/SoapContext/ReplicatedPortC"; 
    protected static final String REPLICA_D =
        "http://localhost:" + PORT_D + "/SoapContext/ReplicatedPortD"; 

    private static final Logger LOG =
        LogUtils.getLogger(FailoverAddressOverrideTest.class);
    private static final String FAILOVER_CONFIG =
        "org/apache/cxf/systest/clustering/failover_address_override.xml";

    protected Bus bus;
    protected Greeter greeter;
    protected List<String> targets;
    private Control control;
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
        SpringBusFactory bf = new SpringBusFactory();    
        bus = bf.createBus(getConfig());
        BusFactory.setDefaultBus(bus);
        targets = new ArrayList<String>();
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
    public void testOverriddenSequentialStrategy() throws Exception {
        startTarget(REPLICA_C);
        setupGreeterA();
        verifyStrategy(greeter, SequentialStrategy.class, 3);
        String response = greeter.greetMe("fred");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_C));
        verifyCurrentEndpoint(REPLICA_C);
        stopTarget(REPLICA_C);
    }
    
    @Test
    public void testOverriddenRandomStrategy() throws Exception {
        startTarget(REPLICA_B);
        setupGreeterC();
        verifyStrategy(greeter, RandomStrategy.class, 3);
        String response = greeter.greetMe("fred");
        assertNotNull("expected non-null response", response);
        assertTrue("response from unexpected target: " + response,
                   response.endsWith(REPLICA_B));
        verifyCurrentEndpoint(REPLICA_B);
        stopTarget(REPLICA_B);
    }

    @Test
    public void testUnreachableAddresses() throws Exception {
        startTarget(REPLICA_A);
        setupGreeterB();
        verifyStrategy(greeter, SequentialStrategy.class, 2);
        try {
            greeter.greetMe("fred");
            fail("expected exception");
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (!(cause instanceof ConnectException)) {
                if (cause.getMessage() != null 
                    && cause.getMessage().contains("404")) {
                    return;
                }
                throw e;
            }
        }
        stopTarget(REPLICA_A);
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

    protected void setupGreeterA() throws Exception {
        greeter = new ClusteredGreeterService().getReplicatedPortA();
        updateAddressPort(greeter, PORT_A);
        verifyConduitSelector(greeter);
    }

    protected void setupGreeterB() throws Exception {
        greeter = new ClusteredGreeterService().getReplicatedPortB();
        updateAddressPort(greeter, PORT_B);
        verifyConduitSelector(greeter);
    }

    protected void setupGreeterC() throws Exception {
        greeter = new ClusteredGreeterService().getReplicatedPortC();
        updateAddressPort(greeter, PORT_C);
        verifyConduitSelector(greeter);
    }
        
    protected void verifyConduitSelector(Greeter g) {
        assertTrue("unexpected conduit slector",
                   ClientProxy.getClient(g).getConduitSelector()
                   instanceof FailoverTargetSelector);
    }

    protected void verifyStrategy(Object proxy, Class<?> clz, int count) {
        ConduitSelector conduitSelector =
            ClientProxy.getClient(proxy).getConduitSelector();
        if (conduitSelector instanceof FailoverTargetSelector) {
            AbstractStaticFailoverStrategy strategy = 
                (AbstractStaticFailoverStrategy)
                    ((FailoverTargetSelector)conduitSelector).getStrategy();
            assertTrue("unexpected strategy", clz.isInstance(strategy));
            List<String> alternates = strategy.getAlternateAddresses(null);
            assertNotNull("expected alternate addresses", alternates);
            assertEquals("unexpected alternate addresses", count, alternates.size());
        } else {
            fail("unexpected conduit selector: " + conduitSelector);
        }
    }

    protected void enableWSAForCurrentEndpoint() {
        Endpoint provider = ClientProxy.getClient(greeter).getEndpoint();
        mapAggregator = new MAPAggregator();
        mapCodec = new MAPCodec();
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
}

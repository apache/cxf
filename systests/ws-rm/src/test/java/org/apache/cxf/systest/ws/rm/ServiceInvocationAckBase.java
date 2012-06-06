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
package org.apache.cxf.systest.ws.rm;

import java.net.MalformedURLException;
import java.util.logging.Logger;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.greeter_control.Control;
import org.apache.cxf.greeter_control.ControlService;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.greeter_control.types.FaultLocation;
import org.apache.cxf.interceptor.ServiceInvokerInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RetransmissionQueue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the acknowledgement delivery back to the non-decoupled port when there is some
 * error at the provider side and how its behavior is affected by the robust in-only mode setting.
 */
public abstract class ServiceInvocationAckBase extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    
    private static final Logger LOG = LogUtils.getLogger(ServiceInvocationAckBase.class);

    private static final String CONTROL_PORT_ADDRESS = 
        "http://localhost:" + PORT + "/SoapContext/ControlPort";

    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            SpringBusFactory factory = new SpringBusFactory();
            Bus bus = factory.createBus();
            BusFactory.setDefaultBus(bus);
            setBus(bus);

            ControlImpl implementor = new ControlImpl();
            implementor.setAddress("http://localhost:" + PORT + "/SoapContext/GreeterPort");
            GreeterImpl greeterImplementor = new GreeterImpl();
            implementor.setImplementor(greeterImplementor);
            Endpoint.publish(CONTROL_PORT_ADDRESS, implementor);
            LOG.fine("Published control endpoint.");
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }
    
    private Bus controlBus;
    private Control control;
    private Bus greeterBus;
    private Greeter greeter;
    

    @BeforeClass
    public static void startServers() throws Exception {
        TestUtilities.setKeepAliveSystemProperty(false);
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }
    
    @AfterClass
    public static void cleanup() {
        TestUtilities.recoverKeepAliveSystemProperty();
    }
    
    @After
    public void tearDown() {
        if (null != greeter) {
            assertTrue("Failed to stop greeter.", control.stopGreeter(null));
            greeterBus.shutdown(true);
            greeterBus = null;
        }
        if (null != control) {  
            assertTrue("Failed to stop greeter", control.stopGreeter(null));
            controlBus.shutdown(true);
        }
    }

    protected void setupGreeter() throws Exception {
    }
    
    @Test
    public void testDefaultInvocationHandling() throws Exception {
        setupGreeter();

        control.setRobustInOnlyMode(false);
        
        FaultLocation location = new org.apache.cxf.greeter_control.types.ObjectFactory()
            .createFaultLocation();
        location.setPhase(Phase.INVOKE);
        location.setBefore(ServiceInvokerInterceptor.class.getName());
        
        RMManager manager = greeterBus.getExtension(RMManager.class);

        // the message is acked and the invocation takes place
        greeter.greetMeOneWay("one");
        waitForEmpty(manager.getRetransmissionQueue());
    
        control.setFaultLocation(location);

        // the invocation fails but the message is acked because the delivery succeeds
        greeter.greetMeOneWay("two");
        waitForEmpty(manager.getRetransmissionQueue());
    }
    
    

    @Test
    public void testRobustInvocationHandling() throws Exception {
        setupGreeter();

        control.setRobustInOnlyMode(true);
        
        FaultLocation location = new org.apache.cxf.greeter_control.types.ObjectFactory()
            .createFaultLocation();
        location.setPhase(Phase.INVOKE);
        location.setBefore(ServiceInvokerInterceptor.class.getName());
        
        RMManager manager = greeterBus.getExtension(RMManager.class);

        
        // the message is acked and the invocation takes place
        greeter.greetMeOneWay("one");
        waitForEmpty(manager.getRetransmissionQueue());

        control.setFaultLocation(location);

        // the invocation fails but the message is acked because the delivery succeeds
        greeter.greetMeOneWay("two");
        waitForNotEmpty(manager.getRetransmissionQueue());

        location.setPhase(null);
        control.setFaultLocation(location);

        // the retransmission succeeds and the invocation succeeds, the message is acked
        waitForEmpty(manager.getRetransmissionQueue());
        
    }

    private void waitForNotEmpty(RetransmissionQueue retransmissionQueue) throws Exception {
        long start = System.currentTimeMillis();
        while (true) {
            Thread.sleep(100);
            if (!retransmissionQueue.isEmpty()) {
                return;
            }
            long total = System.currentTimeMillis() - start;
            if (total > 10000L) {
                fail("RetransmissionQueue must not be empty");
            }
        }
    }

    private void waitForEmpty(RetransmissionQueue retransmissionQueue) throws Exception {
        long start = System.currentTimeMillis();
        while (true) {
            Thread.sleep(100);
            if (retransmissionQueue.isEmpty()) {
                return;
            }
            long total = System.currentTimeMillis() - start;
            if (total > 10000L) {
                fail("RetransmissionQueue must be empty");
            }
        }
    }

    protected void setupGreeter(String cfgResource) throws NumberFormatException, MalformedURLException {
        
        SpringBusFactory bf = new SpringBusFactory();
        
        controlBus = bf.createBus();
        BusFactory.setDefaultBus(controlBus);

        ControlService cs = new ControlService();
        control = cs.getControlPort();
        updateAddressPort(control, PORT);
        
        assertTrue("Failed to start greeter", control.startGreeter(cfgResource));
        
        greeterBus = bf.createBus(cfgResource);
        BusFactory.setDefaultBus(greeterBus);
        LOG.fine("Initialised greeter bus with configuration: " + cfgResource);
        
        GreeterService gs = new GreeterService();

        greeter = gs.getGreeterPort();
        updateAddressPort(greeter, PORT);
        LOG.fine("Created greeter client.");

    }
}

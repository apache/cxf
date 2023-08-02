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

import java.util.logging.Logger;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.rm.RMManager;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the addition of WS-RM properties to application messages and the
 * exchange of WS-RM protocol messages.
 */
public class RobustServiceWithFaultTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    public static final String GREETMEONEWAY_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeOneWayRequest";
    private static final Logger LOG = LogUtils.getLogger(RobustServiceWithFaultTest.class);

    private static RobustOneWayPropertySetter robustSetter;
    private static GreeterCounterImpl serverGreeter;
    private Greeter greeter;


    public static class Server extends AbstractBusTestServerBase {
        Endpoint ep;
        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            // use a at-most-once server with sync ack processing
            Bus bus = bf.createBus("/org/apache/cxf/systest/ws/rm/atmostonce.xml");
            BusFactory.setDefaultBus(bus);
            setBus(bus);
            bus.getExtension(RMManager.class).getConfiguration().setAcknowledgementInterval(Long.valueOf(0));

            serverGreeter = new GreeterCounterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";

            robustSetter = new RobustOneWayPropertySetter();
            bus.getInInterceptors().add(robustSetter);

            // publish this robust oneway endpoint
            ep = Endpoint.create(serverGreeter);
            // leave the robust prop untouched, as it will be set per call later

            ep.publish(address);
            LOG.info("Published greeter endpoint.");
        }
        public void tearDown() {
            ep.stop();
            ep = null;
        }

    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testRobustWithSomeFaults() throws Exception {
        LOG.fine("Creating greeter client");
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus("/org/apache/cxf/systest/ws/rm/seqlength1.xml");
        // set the client retry interval much shorter than the slow processing delay
        RMManager manager = bus.getExtension(RMManager.class);
        manager.getConfiguration().setBaseRetransmissionInterval(Long.valueOf(5000));

        BusFactory.setDefaultBus(bus);
        GreeterService gs = new GreeterService();
        greeter = gs.getGreeterPort();
        updateAddressPort(greeter, PORT);

        LOG.fine("Invoking greeter");
        greeter.greetMeOneWay("one");
        Thread.sleep(3000);

        // invoked once
        assertEquals("not invoked once", 1, serverGreeter.getCount());
        assertTrue("still in retransmission", manager.getRetransmissionQueue().isEmpty());

        LOG.fine("Invoking greeter and raising a fault");
        serverGreeter.setThrowAlways(true);

        greeter.greetMeOneWay("two");
        Thread.sleep(3000);

        // still invoked once
        assertEquals("not invoked once", 1, serverGreeter.getCount());
        assertTrue("still in retransmission", manager.getRetransmissionQueue().isEmpty());

        LOG.fine("Invoking robust greeter and raising a fault");
        robustSetter.setRobust(true);
        greeter.greetMeOneWay("three");
        Thread.sleep(3000);

        // still invoked once
        assertEquals("not invoked once", 1, serverGreeter.getCount());
        assertFalse("no message in retransmission", manager.getRetransmissionQueue().isEmpty());

        LOG.fine("Stop raising a fault and let the retransmission succeeds");
        serverGreeter.setThrowAlways(false);
        Thread.sleep(8000);

        // invoked twice
        assertEquals("not invoked twice", 2, serverGreeter.getCount());
        assertTrue("still in retransmission", manager.getRetransmissionQueue().isEmpty());
    }

    private static final class GreeterCounterImpl extends GreeterImpl {
        private int count;
        private boolean throwAlways;

        public void greetMeOneWay(String arg0) {
            if (throwAlways) {
                throw new RuntimeException("invocation exception");
            }
            super.greetMeOneWay(arg0);
            count++;
        }

        public int getCount() {
            return count;
        }

        @Override
        public void setThrowAlways(boolean t) {
            throwAlways = t;
            super.setThrowAlways(t);
        }
    }

    static class RobustOneWayPropertySetter extends AbstractPhaseInterceptor<Message> {
        private boolean robust;

        RobustOneWayPropertySetter() {
            super(Phase.RECEIVE);
        }

        public void setRobust(boolean robust) {
            this.robust = robust;
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            message.put(Message.ROBUST_ONEWAY, robust);
        }
    }
}

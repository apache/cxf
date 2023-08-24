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
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.rm.RMConfiguration;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.manager.RetryPolicyType;
import org.apache.cxf.ws.rm.persistence.jdbc.RMTxStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the redelivery of the message upon a delivery error.
 */
public class RedeliveryTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    private static final Logger LOG = LogUtils.getLogger(RedeliveryTest.class);

    private static GreeterRecorderImpl serverGreeter;
    private static Bus serverBus;
    private Greeter greeter;


    public static class Server extends AbstractBusTestServerBase {
        String port;
        String pfx;
        Endpoint ep;

        public Server(String[] args) {
            port = args[0];
            pfx = args[1];
        }

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            // use a at-most-once server with sync ack processing
            System.setProperty("db.name", pfx + "-server");
            serverBus = bf.createBus("/org/apache/cxf/systest/ws/rm/sync-ack-persistent-server.xml");
            System.clearProperty("db.name");
            BusFactory.setDefaultBus(serverBus);
            RMManager manager = serverBus.getExtension(RMManager.class);
            RMConfiguration cfg = manager.getConfiguration();
            cfg.setAcknowledgementInterval(0L);

            RetryPolicyType rp = new RetryPolicyType();
            rp.setMaxRetries(-1);
            serverBus.getExtension(RMManager.class).getDestinationPolicy().setRetryPolicy(rp);
            serverGreeter = new GreeterRecorderImpl();
            String address = "http://localhost:" + port + "/SoapContext/GreeterPort";

            // publish this robust oneway endpoint
            ep = Endpoint.create(serverGreeter);
            ep.publish(address);
            LOG.info("Published greeter endpoint.");
            BusFactory.setDefaultBus(null);
            BusFactory.setThreadDefaultBus(null);
        }
        public void tearDown() {
            ep.stop();
            ep = null;
        }

        public static void main(String[] args) {
            try {
                Server s = new Server(args);
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        RMTxStore.deleteDatabaseFiles("redlv-server", true);
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, null, new String[]{PORT, "redlv"}, true));
    }

    @AfterClass
    public static void cleanUpDerby() throws Exception {
        RMTxStore.deleteDatabaseFiles("redlv-server", true);
    }

    @Test
    public void testAutomaticRedeliveryAfterError() throws Exception {
        LOG.fine("Creating greeter client");
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus("/org/apache/cxf/systest/ws/rm/rminterceptors.xml");
        // set the client retry interval much shorter than the slow processing delay
        RMManager manager = bus.getExtension(RMManager.class);
        RMConfiguration cfg = manager.getConfiguration();
        cfg.setBaseRetransmissionInterval(3000L);

        BusFactory.setDefaultBus(bus);
        GreeterService gs = new GreeterService();
        greeter = gs.getGreeterPort();
        updateAddressPort(greeter, PORT);

        assertNull("last greeted by none", serverGreeter.getValue());

        LOG.fine("Invoking greeter for one");
        greeter.greetMeOneWay("one");
        LOG.fine("Wait for 4 secs ...");
        Thread.sleep(4000);

        assertEquals("last greeted by one", "one", serverGreeter.getValue());
        assertTrue("retransmission running", manager.getRetransmissionQueue().isEmpty());

        LOG.fine("Activating the error trigger and invoking greeter for two");
        serverGreeter.setThrowAlways(true);
        greeter.greetMeOneWay("two");
        LOG.fine("Wait for 4 secs ...");
        Thread.sleep(4000);

        RMManager serverManager = serverBus.getExtension(RMManager.class);

        assertEquals("last greeted by one", "one", serverGreeter.getValue());
        assertTrue("retransmission running", manager.getRetransmissionQueue().isEmpty());
        assertFalse("redelivery not running", serverManager.getRedeliveryQueue().isEmpty());

        LOG.fine("Deactivating the error trigger and wait for 9 secs ...");
        serverGreeter.setThrowAlways(false);
        Thread.sleep(9000);

        assertEquals("last greeted by two", "two", serverGreeter.getValue());
        assertTrue("redelivery running", serverManager.getRedeliveryQueue().isEmpty());
    }

    private static final class GreeterRecorderImpl extends GreeterImpl {
        private String value;
        private boolean ex;

        public void greetMeOneWay(String arg0) {
            if (ex) {
                throw new RuntimeException("intentional exception");
            }
            super.greetMeOneWay(arg0);
            value = arg0;
        }

        public String getValue() {
            return value;
        }

        @Override
        public void setThrowAlways(boolean b) {
            super.setThrowAlways(b);
            ex = b;
        }
    }
}

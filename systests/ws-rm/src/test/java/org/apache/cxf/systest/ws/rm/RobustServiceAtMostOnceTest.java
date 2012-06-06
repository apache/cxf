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

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.rm.RMManager;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the addition of WS-RM properties to application messages and the
 * exchange of WS-RM protocol messages.
 */
public class RobustServiceAtMostOnceTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class); 
    public static final String GREETMEONEWAY_ACTION 
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeOneWayRequest";
    private static final Logger LOG = LogUtils.getLogger(RobustServiceAtMostOnceTest.class);
    
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
            bus.getExtension(RMManager.class).getRMAssertion().getAcknowledgementInterval()
                .setMilliseconds(0L);

            // add some intentional processing delay at inbound
            SlowProcessingSimulator sps = new SlowProcessingSimulator();
            sps.setAction("http://cxf.apache.org/greeter_control/Greeter/greetMeOneWayRequest");
            sps.setDelay(10000L);
            bus.getInInterceptors().add(sps);
            serverGreeter = new GreeterCounterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
            
            // publish this robust oneway endpoint
            ep = Endpoint.create(serverGreeter);
            ep.getProperties().put(Message.ROBUST_ONEWAY, Boolean.TRUE);
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
    public void testRobustAtMostOnceWithSlowProcessing() throws Exception {
        LOG.fine("Creating greeter client");
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus("/org/apache/cxf/systest/ws/rm/seqlength1.xml");
        // set the client retry interval much shorter than the slow processing delay
        RMManager manager = bus.getExtension(RMManager.class); 
        manager.getRMAssertion().getBaseRetransmissionInterval()
            .setMilliseconds(3000L);

        BusFactory.setDefaultBus(bus);
        GreeterService gs = new GreeterService();
        greeter = gs.getGreeterPort();
        updateAddressPort(greeter, PORT);
        
        LOG.fine("Invoking greeter");
        greeter.greetMeOneWay("one");
        Thread.sleep(10000);
        
        assertEquals("invoked too many times", 1, serverGreeter.getCount());
        assertTrue("still in retransmission", manager.getRetransmissionQueue().isEmpty());
    }

    private static class GreeterCounterImpl extends GreeterImpl {
        private int count;

        public void greetMeOneWay(String arg0) {
            super.greetMeOneWay(arg0);
            count++;
        }
        
        public int getCount() {
            return count;
        }
    }
}

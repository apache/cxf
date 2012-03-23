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

import java.io.OutputStream;
import java.util.logging.Logger;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMManager;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the decoupling the soap fault handling if the fault occurs after 
 * the message is queued and retransmission is scheduled.
 */
public class RetransmissionQueueTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    public static final String DECOUPLE_PORT = allocatePort("decoupled.port");

    private static final Logger LOG = LogUtils.getLogger(RetransmissionQueueTest.class);
    private Bus bus;

    public static class Server extends AbstractBusTestServerBase {
      
        protected void run()  {            
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus("/org/apache/cxf/systest/ws/rm/message-loss.xml");
            BusFactory.setDefaultBus(bus);
            LoggingInInterceptor in = new LoggingInInterceptor();
            bus.getInInterceptors().add(in);
            bus.getInFaultInterceptors().add(in);
            LoggingOutInterceptor out = new LoggingOutInterceptor();
            bus.getOutInterceptors().add(out);
            bus.getOutFaultInterceptors().add(out);
            
            GreeterImpl implementor = new GreeterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
            
            Endpoint ep = Endpoint.create(implementor);
            ep.publish(address);

            LOG.info("Published greeter endpoint.");
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
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(Server.class));
    }
            
    @Test
    public void testDecoupleFaultHandling() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus("/org/apache/cxf/systest/ws/rm/message-loss.xml");
        BusFactory.setDefaultBus(bus);
        LoggingInInterceptor in = new LoggingInInterceptor();
        bus.getInInterceptors().add(in);
        bus.getInFaultInterceptors().add(in);
        LoggingOutInterceptor out = new LoggingOutInterceptor();
        bus.getOutInterceptors().add(out);
        // an interceptor to simulate a transmission error
        TransmissionErrorSimulator tes = new TransmissionErrorSimulator();
        bus.getOutInterceptors().add(tes);
        
        bus.getOutFaultInterceptors().add(out);
        
        bus.getExtension(RMManager.class).getRMAssertion().getBaseRetransmissionInterval()
        .setMilliseconds(new Long(5000));
        
        GreeterService gs = new GreeterService();
        final Greeter greeter = gs.getGreeterPort();
        updateAddressPort(greeter, PORT);
        LOG.fine("Created greeter client.");
       
        ConnectionHelper.setKeepAliveConnection(greeter, true);
        RMManager manager = bus.getExtension(RMManager.class);
        
        try {
            greeter.greetMeOneWay("oneway");            
        } catch (Exception e) {
            // no exception shall be thrown when the message is queued for retransmission
            fail("fault thrown after queued for retransmission: " + e);
        }
        
        // the message shall be in the queue
        assertFalse("RetransmissionQueue empty", manager.getRetransmissionQueue().isEmpty());
        
        tes.setWorking(true);

        long wait = 10000;
        while (wait > 0) {
            long start = System.currentTimeMillis();
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ex) {
                // ignore
            }
            wait -= System.currentTimeMillis() - start;
        }

        // the message shall no longer be in the queue
        assertTrue("RetransmissionQueue not empty", manager.getRetransmissionQueue().isEmpty());
    }

    /*
     * an interceptor to trigger an error occuring at the sending phase.
     */
    static class TransmissionErrorSimulator extends AbstractPhaseInterceptor<Message> {
        private boolean working;
        
        /**
         * @param phase
         */
        public TransmissionErrorSimulator() {
            super(Phase.PREPARE_SEND);
            addAfter(MessageSenderInterceptor.class.getName());
        }

        /* (non-Javadoc)
         * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
         */
        public void handleMessage(Message message) throws Fault {
            // let the create sequence message to succeed so that a valid sequence is created
            AddressingProperties maps =
                RMContextUtils.retrieveMAPs(message, false, true);
            if (maps != null 
                && maps.getAction() != null
                && RMConstants.getCreateSequenceAction().equals(maps.getAction().getValue())) {
                // spare the message
            } else if (!working) {
                // triggers a simulated error
                try {
                    message.getContent(OutputStream.class).close();
                } catch (Exception e) {
                    //
                }
            }
        }
        
        /**
         * @return the working
         */
        public boolean isWorking() {
            return working;
        }

        /**
         * @param working the working to set
         */
        public void setWorking(boolean working) {
            this.working = working;
        }
    }

}

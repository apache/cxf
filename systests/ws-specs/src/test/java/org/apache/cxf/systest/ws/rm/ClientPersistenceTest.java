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

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.systest.ws.policy.GreeterImpl;
import org.apache.cxf.systest.ws.util.InMessageRecorder;
import org.apache.cxf.systest.ws.util.MessageFlow;
import org.apache.cxf.systest.ws.util.MessageRecorder;
import org.apache.cxf.systest.ws.util.OutMessageRecorder;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.rm.DestinationSequence;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.persistence.jdbc.RMTxStore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the addition of WS-RM properties to application messages and the
 * exchange of WS-RM protocol messages.
 */
public class ClientPersistenceTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class); 
    public static final String GREETMEONEWAY_ACTION 
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeOneWayRequest";
    public static final String GREETME_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeRequest";
    private static final Logger LOG = LogUtils.getLogger(ClientPersistenceTest.class);
    
    private Greeter greeter;
    private OutMessageRecorder out;
    private InMessageRecorder in;

    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus("/org/apache/cxf/systest/ws/rm/persistent.xml");
            BusFactory.setDefaultBus(bus);
            
            LoggingInInterceptor logIn = new LoggingInInterceptor();
            bus.getInInterceptors().add(logIn);
            LoggingOutInterceptor logOut = new LoggingOutInterceptor();
            bus.getOutFaultInterceptors().add(logOut);
            bus.getOutFaultInterceptors().add(logOut);
            
            bus.getExtension(RMManager.class).getRMAssertion().getBaseRetransmissionInterval()
                .setMilliseconds(new BigInteger("60000"));
            
            GreeterImpl implementor = new GreeterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
            Endpoint ep = Endpoint.create(implementor);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("schema-validation-enabled", Boolean.TRUE);
            ep.setProperties(properties);
            ep.publish(address);
            LOG.info("Published greeter endpoint.");
        }

        public static void main(String[] args) {
            try {
                RMTxStore.deleteDatabaseFiles();
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
        RMTxStore.deleteDatabaseFiles(RMTxStore.DEFAULT_DATABASE_NAME, false);
        String derbyHome = System.getProperty("derby.system.home");
        try {
            if (derbyHome == null) {
                System.setProperty("derby.system.home", "derby-server");
            } else {
                System.setProperty("derby.system.home", derbyHome + "-server");                
            }
            assertTrue("server did not launch correctly", launchServer(Server.class));
        } finally {
            if (derbyHome == null) {
                System.clearProperty("derby.system.home");
            } else {
                System.setProperty("derby.system.home", derbyHome);
            }
        }
        RMTxStore.deleteDatabaseFiles();
    }
    
    @AfterClass
    public static void tearDownOnce() {
        RMTxStore.deleteDatabaseFiles(RMTxStore.DEFAULT_DATABASE_NAME, false);
    }

    @Test 
    public void testRecovery() throws Exception {
        startClient();
        populateStore();
        verifyStorePopulation();
        stopClient();
        startClient();
        recover();
        verifyRecovery();
    }
    
    void startClient() throws Exception {
        LOG.fine("Creating greeter client");
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus("/org/apache/cxf/systest/ws/rm/persistent.xml");
        BusFactory.setDefaultBus(bus);

        GreeterService gs = new GreeterService();
        greeter = gs.getGreeterPort();
        updateAddressPort(greeter, PORT);
        ((BindingProvider)greeter).getRequestContext().put("schema-validation-enabled", Boolean.TRUE);

        out = new OutMessageRecorder();
        in = new InMessageRecorder();

        bus.getOutInterceptors().add(out);
        bus.getInInterceptors().add(in);
    }

    void populateStore() throws Exception {
        
        bus.getExtension(RMManager.class).getRMAssertion().getBaseRetransmissionInterval()
            .setMilliseconds(new BigInteger("60000"));
        bus.getOutInterceptors().add(new MessageLossSimulator());
                
        greeter.greetMeOneWay("one");
        greeter.greetMeOneWay("two");
        greeter.greetMeOneWay("three");
        greeter.greetMeOneWay("four");
        
        MessageFlow mf = new MessageFlow(out.getOutboundMessages(), in.getInboundMessages());

        assertNotNull(mf);
        awaitMessages(5, 3);
        
        mf.verifyMessages(5, true);
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(),
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3", "4"}, true);
        mf.verifyAcknowledgements(new boolean[5], true);


        mf.verifyMessages(3, false);
        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(),
                                        RMConstants.getSequenceAcknowledgmentAction(),
                                        RMConstants.getSequenceAcknowledgmentAction()};
        mf.verifyActions(expectedActions, false);
        mf.verifyAcknowledgements(new boolean[] {false, true, true}, false);        
    }
    
    void verifyStorePopulation() {
        
        RMManager manager = bus.getExtension(RMManager.class);
        assertNotNull(manager);
        
        RMStore store = manager.getStore();
        assertNotNull(store);
        
        Client client = ClientProxy.getClient(greeter);
        String id = RMUtils.getEndpointIdentifier(client.getEndpoint());
        
        Collection<DestinationSequence> dss =
            store.getDestinationSequences(id);
        assertEquals(1, dss.size());
        
        Collection<SourceSequence> sss =
            store.getSourceSequences(id);
        assertEquals(1, sss.size());
        
        Collection<RMMessage> msgs = 
            store.getMessages(sss.iterator().next().getIdentifier(), true);
        assertEquals(2, msgs.size());  
        
        msgs = 
            store.getMessages(sss.iterator().next().getIdentifier(), false);
        assertEquals(0, msgs.size());  
    }
    
    void stopClient() {
        // ClientProxy.getClient(greeter).destroy();
        bus.shutdown(true);
    }
      
    void recover() throws Exception {
        
        // do nothing - resends should happen in the background  
       
        Thread.sleep(5000);
        LOG.info("Recovered messages should have been resent by now.");
 
    }
    
    void verifyRecovery() throws Exception {
        
        RMManager manager = bus.getExtension(RMManager.class);
        assertNotNull(manager);
        
        RMStore store = manager.getStore();
        assertNotNull(store);
        
        Client client = ClientProxy.getClient(greeter);
        String id = RMUtils.getEndpointIdentifier(client.getEndpoint());
        
        Collection<DestinationSequence> dss =
            store.getDestinationSequences(id);
        assertEquals(1, dss.size());
        
        Collection<SourceSequence> sss =
            store.getSourceSequences(id);
        assertEquals(1, sss.size());
        
        int i = 0;
        while (store.getMessages(sss.iterator().next().getIdentifier(), true).size() > 0 && i < 10) {
            Thread.sleep(200);
            i++;
        }
       
        assertEquals(0, store.getMessages(sss.iterator().next().getIdentifier(), true).size());
        assertEquals(0, store.getMessages(sss.iterator().next().getIdentifier(), false).size());        
    }
    
    private void awaitMessages(int nExpectedOut, int nExpectedIn) {
        awaitMessages(nExpectedOut, nExpectedIn, 10000);
    }
    
    private void awaitMessages(int nExpectedOut, int nExpectedIn, int timeout) {
        MessageRecorder mr = new MessageRecorder(out, in);
        mr.awaitMessages(nExpectedOut, nExpectedIn, timeout);
    }

}

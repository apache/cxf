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
import java.util.logging.Logger;

import javax.xml.ws.Response;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.Control;
import org.apache.cxf.greeter_control.ControlService;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.greeter_control.types.GreetMeResponse;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.systest.ws.util.InMessageRecorder;
import org.apache.cxf.systest.ws.util.MessageFlow;
import org.apache.cxf.systest.ws.util.MessageRecorder;
import org.apache.cxf.systest.ws.util.OutMessageRecorder;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.persistence.jdbc.RMTxStore;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the addition of WS-RM properties to application messages and the
 * exchange of WS-RM protocol messages.
 */
public class ServerPersistenceTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    public static final String DECOUPLE_PORT = allocatePort("decoupled.port");

    public static final String GREETMEONEWAY_ACTION 
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeOneWayRequest";
    public static final String GREETME_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeRequest";
    private static final String GREETME_RESPONSE_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeResponse";

    private static final Logger LOG = LogUtils.getLogger(ServerPersistenceTest.class);
    private static final String CFG = "/org/apache/cxf/systest/ws/rm/persistent.xml";
    private static final String SERVER_LOSS_CFG 
        = "/org/apache/cxf/systest/ws/rm/persistent-message-loss-server.xml";

    private OutMessageRecorder out;
    private InMessageRecorder in;

    @BeforeClass
    public static void startServers() throws Exception {
        RMTxStore.deleteDatabaseFiles();
        String derbyHome = System.getProperty("derby.system.home"); 
        try {
            System.setProperty("derby.system.home", derbyHome + "-server");
            RMTxStore.deleteDatabaseFiles();
        } finally {
            if (derbyHome != null) {
                System.setProperty("derby.system.home", derbyHome);
            } else {
                System.clearProperty("derby.system.home");
            }
        }
        boolean inProcess = "Windows 2000".equals(System.getProperty("os.name"));
        assertTrue("server did not launch correctly", launchServer(Server.class, inProcess));  
    }
    
    @Test 
    public void testRecovery() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus();
        BusFactory.setDefaultBus(bus);        
        LOG.fine("Created bus " + bus + " with default cfg");
        ControlService cs = new ControlService();
        Control control = cs.getControlPort();

        updateAddressPort(control, PORT);
        
        assertTrue("Failed to start greeter", control.startGreeter(SERVER_LOSS_CFG)); 
        LOG.fine("Started greeter server.");
        
        Bus greeterBus = new SpringBusFactory().createBus(CFG);
        LOG.fine("Created bus " + greeterBus + " with cfg : " + CFG);        
        BusFactory.setDefaultBus(greeterBus);
        
        // avoid early client resends
        greeterBus.getExtension(RMManager.class).getRMAssertion().getBaseRetransmissionInterval()
            .setMilliseconds(new BigInteger("60000"));
        GreeterService gs = new GreeterService();
        Greeter greeter = gs.getGreeterPort();
        updateAddressPort(greeter, PORT);
        
        LOG.fine("Created greeter client.");
 
        ConnectionHelper.setKeepAliveConnection(greeter, true);

        Client c = ClientProxy.getClient(greeter);
        HTTPConduit hc = (HTTPConduit)(c.getConduit());
        HTTPClientPolicy cp = hc.getClient();
        cp.setDecoupledEndpoint("http://localhost:" + DECOUPLE_PORT + "/decoupled_endpoint");

        out = new OutMessageRecorder();
        in = new InMessageRecorder();

        greeterBus.getOutInterceptors().add(out);
        greeterBus.getInInterceptors().add(in);
        
        LOG.fine("Configured greeter client.");

        Response<GreetMeResponse> responses[] = cast(new Response[3]);
        
        responses[0] = greeter.greetMeAsync("one");
        responses[1] = greeter.greetMeAsync("two");
        responses[2] = greeter.greetMeAsync("three");
        
        verifyMissingResponse(responses);
        control.stopGreeter(SERVER_LOSS_CFG);
        LOG.fine("Stopped greeter server");
       
        out.getOutboundMessages().clear();
        in.getInboundMessages().clear();
        
        control.startGreeter(CFG);
        String nl = System.getProperty("line.separator");
        LOG.fine("Restarted greeter server" + nl + nl);
        
        verifyServerRecovery(responses);
        
        greeterBus.shutdown(true);
        
        control.stopGreeter(CFG);
        bus.shutdown(true);
    }
    
    void verifyMissingResponse(Response<GreetMeResponse> responses[]) throws Exception {
        awaitMessages(4, 6, 20000);

        // wait another while to prove that response to second request is indeed lost
        Thread.sleep(1000);
        int nDone = 0;
        for (int i = 0; i < responses.length; i++) {
            if (responses[i].isDone()) {
                nDone++;
            }
        }
        
        assertEquals("Unexpected number of responses already received.", 2, nDone);
        
        MessageFlow mf = new MessageFlow(out.getOutboundMessages(), in.getInboundMessages());
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(),
                                                 GREETME_ACTION,
                                                 GREETME_ACTION,
                                                 GREETME_ACTION};
        mf.verifyActions(expectedActions, true);
        // mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);
        // mf.verifyAcknowledgements(new boolean[] {false, false, true, false}, true);
        
        mf.verifyPartialResponses(4);
        mf.purgePartialResponses();
        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(),
                                        GREETME_RESPONSE_ACTION,
                                        GREETME_RESPONSE_ACTION};
        mf.verifyActions(expectedActions, false);
        // mf.verifyMessageNumbers(new String[] {null, "1", "3"}, false);
        // mf.verifyAcknowledgements(new boolean[] {false, true, true}, false);    
    }
    
    void verifyServerRecovery(Response<GreetMeResponse> responses[]) throws Exception {
   
        // wait until all messages have received their responses
        int nDone = 0;
        long waited = 0;
        while (waited < 5000) {
            for (int i = 0; i < responses.length; i++) {
                if (responses[i].isDone()) {
                    nDone++;
                }
            }
            if (nDone == 3) {
                break;
            }
            Thread.sleep(500);
            nDone = 0;
        }
        
        assertEquals("Not all responses have been received.", 3, nDone);

        // verify that all inbound messages are resent responses
        
        synchronized (this) {
            MessageFlow mf = new MessageFlow(out.getOutboundMessages(), in.getInboundMessages());
            int nOut = out.getOutboundMessages().size();
            int nIn = in.getInboundMessages().size();
            assertEquals("Unexpected outbound message(s)", 0, nOut);
            assertTrue(nIn >= 1);
            String[] expectedActions = new String[nIn];
            for (int i = 0; i < nIn; i++) {
                expectedActions[i] = GREETME_RESPONSE_ACTION;
            }
            mf.verifyActions(expectedActions, false);
        }
    }
  
    
    protected void awaitMessages(int nExpectedOut, int nExpectedIn) {
        awaitMessages(nExpectedOut, nExpectedIn, 10000);
    }
    
    private void awaitMessages(int nExpectedOut, int nExpectedIn, int timeout) {
        MessageRecorder mr = new MessageRecorder(out, in);
        mr.awaitMessages(nExpectedOut, nExpectedIn, timeout);
    }
    
    @SuppressWarnings("unchecked")
    <T> Response<T>[] cast(Response[] val) {
        return (Response<T>[])val;
    }

}

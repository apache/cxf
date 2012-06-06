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

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Endpoint;

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
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.systest.ws.util.MessageFlow;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.recorders.InMessageRecorder;
import org.apache.cxf.testutil.recorders.MessageRecorder;
import org.apache.cxf.testutil.recorders.OutMessageRecorder;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.VersionTransformer.Names200408;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.cxf.ws.rm.RMException;
import org.apache.cxf.ws.rm.RMManager;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests control of WS-RM protocol variations on the client, and of the server responses matching whichever
 * variation is used by the client.
 */
public class ProtocolVariationsTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(ProtocolVariationsTest.class);

    private static final Logger LOG = LogUtils.getLogger(ProtocolVariationsTest.class);
    private static final String GREETME_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeRequest";
    private static final String GREETME_RESPONSE_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeResponse";

    private static String decoupledEndpoint;
    private static int decoupledCount = 1;
    private Bus controlBus;
    private Control control;
    private Bus greeterBus;
    private Greeter greeter;
    private OutMessageRecorder outRecorder;
    private InMessageRecorder inRecorder;
    private Dispatch<DOMSource> dispatch;

    public static class Server extends AbstractBusTestServerBase {
        Endpoint ep;
        
        protected void run() {
            SpringBusFactory factory = new SpringBusFactory();
            Bus bus = factory.createBus();
            BusFactory.setDefaultBus(bus);
            setBus(bus);

            //System.out.println("Created control bus " + bus);
            ControlImpl implementor = new ControlImpl();
            implementor.setDbName("pvt-server");
            implementor.setAddress("http://localhost:" + PORT + "/SoapContext/GreeterPort");
            GreeterImpl greeterImplementor = new GreeterImpl();
            implementor.setImplementor(greeterImplementor);
            ep = Endpoint.publish("http://localhost:" + PORT + "/SoapContext/ControlPort", implementor);
            BusFactory.setDefaultBus(null);
            BusFactory.setThreadDefaultBus(null); 
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

            
    
    @After
    public void tearDown() throws Exception {
        try {
            stopClient();
            stopControl();
        } catch (Throwable t) {
            //ignore
        }
        Thread.sleep(100);
    }
    
    @Test
    public void testDefault() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", false);

        assertEquals("ONE", greeter.greetMe("one"));
        assertEquals("TWO", greeter.greetMe("two"));
        assertEquals("THREE", greeter.greetMe("three"));

        verifyTwowayNonAnonymous(Names200408.WSA_NAMESPACE_NAME, RM10Constants.INSTANCE);
    }
    
    @Test
    public void testRM10WSA200408() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", false);
        
        // same as default, but explicitly setting the WS-Addressing namespace
        Client client = ClientProxy.getClient(greeter);
        client.getRequestContext().put(RMManager.WSRM_WSA_VERSION_PROPERTY, Names200408.WSA_NAMESPACE_NAME);

        assertEquals("ONE", greeter.greetMe("one"));
        assertEquals("TWO", greeter.greetMe("two"));
        assertEquals("THREE", greeter.greetMe("three"));

        verifyTwowayNonAnonymous(Names200408.WSA_NAMESPACE_NAME, RM10Constants.INSTANCE);
    }
    
    @Test
    public void testRM10WSA15() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", false);
        
        // WS-RM 1.0, but using the WS-A 1.0 namespace
        Client client = ClientProxy.getClient(greeter);
        client.getRequestContext().put(RMManager.WSRM_WSA_VERSION_PROPERTY, Names.WSA_NAMESPACE_NAME);

        assertEquals("ONE", greeter.greetMe("one"));
        assertEquals("TWO", greeter.greetMe("two"));
        assertEquals("THREE", greeter.greetMe("three"));

        verifyTwowayNonAnonymous(Names.WSA_NAMESPACE_NAME, RM10Constants.INSTANCE);
    }
    
    @Test
    public void testRM11() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", false);
        
        // WS-RM 1.1 and WS-A 1.0
        Client client = ClientProxy.getClient(greeter);
        client.getRequestContext().put(RMManager.WSRM_VERSION_PROPERTY, RM11Constants.NAMESPACE_URI);
        client.getRequestContext().put(RMManager.WSRM_WSA_VERSION_PROPERTY, Names.WSA_NAMESPACE_NAME);

        assertEquals("ONE", greeter.greetMe("one"));
        assertEquals("TWO", greeter.greetMe("two"));
        assertEquals("THREE", greeter.greetMe("three"));

        verifyTwowayNonAnonymous(Names.WSA_NAMESPACE_NAME, RM11Constants.INSTANCE);
    }
    
    @Test
    public void testInvalidRM11WSA200408() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", false);
        
        // WS-RM 1.1, but using the WS-A 1.0 namespace
        Client client = ClientProxy.getClient(greeter);
        client.getRequestContext().put(RMManager.WSRM_VERSION_PROPERTY, RM11Constants.NAMESPACE_URI);
        client.getRequestContext().put(RMManager.WSRM_WSA_VERSION_PROPERTY, Names200408.WSA_NAMESPACE_NAME);

        try {
            greeter.greetMe("one");
            fail("invalid namespace combination");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RMException);
            // verify a partial error text match to exclude an unexpected exception
            // (see UNSUPPORTED_NAMESPACE in Messages.properties)
            final String text = Names200408.WSA_NAMESPACE_NAME + " is not supported";
            assertTrue(e.getCause().getMessage() != null 
                       && e.getCause().getMessage().indexOf(text) > 0);
        }
        
    }
    
    @Test
    public void testDefaultDecoupled() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", true);

        assertEquals("ONE", greeter.greetMe("one"));
        assertEquals("TWO", greeter.greetMe("two"));
        assertEquals("THREE", greeter.greetMe("three"));

        verifyTwowayNonAnonymous(Names200408.WSA_NAMESPACE_NAME, RM10Constants.INSTANCE);
    }
    
    @Test
    public void testRM10WSA200408Decoupled() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", true);
        
        // same as default, but explicitly setting the WS-Addressing namespace
        Client client = ClientProxy.getClient(greeter);
        client.getRequestContext().put(RMManager.WSRM_WSA_VERSION_PROPERTY, Names200408.WSA_NAMESPACE_NAME);

        assertEquals("ONE", greeter.greetMe("one"));
        assertEquals("TWO", greeter.greetMe("two"));
        assertEquals("THREE", greeter.greetMe("three"));

        verifyTwowayNonAnonymous(Names200408.WSA_NAMESPACE_NAME, RM10Constants.INSTANCE);
    }
    
    @Test
    public void testRM10WSA15Decoupled() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", true);
        
        // WS-RM 1.0, but using the WS-A 1.0 namespace
        Client client = ClientProxy.getClient(greeter);
        client.getRequestContext().put(RMManager.WSRM_WSA_VERSION_PROPERTY, Names.WSA_NAMESPACE_NAME);

        assertEquals("ONE", greeter.greetMe("one"));
        assertEquals("TWO", greeter.greetMe("two"));
        assertEquals("THREE", greeter.greetMe("three"));

        verifyTwowayNonAnonymous(Names.WSA_NAMESPACE_NAME, RM10Constants.INSTANCE);
    }
    
    @Test
    public void testRM11Decoupled() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", true);
        
        // WS-RM 1.1 and WS-A 1.0
        Client client = ClientProxy.getClient(greeter);
        client.getRequestContext().put(RMManager.WSRM_VERSION_PROPERTY, RM11Constants.NAMESPACE_URI);
        client.getRequestContext().put(RMManager.WSRM_WSA_VERSION_PROPERTY, Names.WSA_NAMESPACE_NAME);

        assertEquals("ONE", greeter.greetMe("one"));
        assertEquals("TWO", greeter.greetMe("two"));
        assertEquals("THREE", greeter.greetMe("three"));

        verifyTwowayNonAnonymous(Names.WSA_NAMESPACE_NAME, RM11Constants.INSTANCE);
    }

    private void verifyTwowayNonAnonymous(String wsaUri, RMConstants consts) throws Exception {
    
        // CreateSequence and three greetMe messages

        awaitMessages(4, 4);
        
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), wsaUri, consts.getWSRMNamespace());
        
        
        mf.verifyMessages(4, true);
        String[] expectedActions = new String[] {consts.getCreateSequenceAction(), 
                                                 GREETME_ACTION,
                                                 GREETME_ACTION, 
                                                 GREETME_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);
        mf.verifyLastMessage(new boolean[] {false, false, false, false}, true);
        mf.verifyAcknowledgements(new boolean[] {false, false, true, true}, true);

        // createSequenceResponse plus 3 greetMeResponse messages
        // the first response should not include an acknowledgement, the other three should

        mf.verifyMessages(4, false);

        expectedActions = new String[] {consts.getCreateSequenceResponseAction(), 
                                        GREETME_RESPONSE_ACTION, 
                                        GREETME_RESPONSE_ACTION, 
                                        GREETME_RESPONSE_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, false);
        mf.verifyLastMessage(new boolean[4], false);
        mf.verifyAcknowledgements(new boolean[] {false, true, true, true}, false);
    }
    
    // --- test utilities ---
    
    private void init(String cfgResource, boolean useDecoupledEndpoint) {
        
        SpringBusFactory bf = new SpringBusFactory();
        initControl(bf, cfgResource);
        initGreeterBus(bf, cfgResource);
        initProxy(useDecoupledEndpoint, null);
    }
    
    private void initControl(SpringBusFactory bf, String cfgResource) {
        controlBus = bf.createBus();
        BusFactory.setDefaultBus(controlBus);

        ControlService cs = new ControlService();
        control = cs.getControlPort();
        try {
            updateAddressPort(control, PORT);
        } catch (Exception ex) {
            //ignore
        }
        
        assertTrue("Failed to start greeter", control.startGreeter(cfgResource));        
    }
    
    private void initGreeterBus(SpringBusFactory bf,
                                String cfgResource) {
        greeterBus = bf.createBus(cfgResource);
        BusFactory.setDefaultBus(greeterBus);
        LOG.fine("Initialised greeter bus with configuration: " + cfgResource);

        outRecorder = new OutMessageRecorder();
        greeterBus.getOutInterceptors().add(outRecorder);
        inRecorder = new InMessageRecorder();
        greeterBus.getInInterceptors().add(inRecorder);
    }

    private void initProxy(boolean useDecoupledEndpoint, Executor executor) {        
        GreeterService gs = new GreeterService();

        if (null != executor) {
            gs.setExecutor(executor);
        }
   
        greeter = gs.getGreeterPort();
        try {
            updateAddressPort(greeter, PORT);
        } catch (Exception e) {
            //ignore
        }
        LOG.fine("Created greeter client.");

        ConnectionHelper.setKeepAliveConnection(greeter, true);

        if (useDecoupledEndpoint) {
            initDecoupledEndpoint(ClientProxy.getClient(greeter));
        }
    }

    private void initDecoupledEndpoint(Client c) {
        // programatically configure decoupled endpoint that is guaranteed to
        // be unique across all test cases
        decoupledEndpoint = "http://localhost:" 
            + allocatePort("decoupled-" + decoupledCount++) + "/decoupled_endpoint";

        HTTPConduit hc = (HTTPConduit)(c.getConduit());
        HTTPClientPolicy cp = hc.getClient();
        cp.setDecoupledEndpoint(decoupledEndpoint);

        LOG.fine("Using decoupled endpoint: " + cp.getDecoupledEndpoint());
    }
    
    private void stopClient() {
        if (null != greeterBus) {
            
            //ensure we close the decoupled destination of the conduit,
            //so that release the port if the destination reference count hit zero
            if (greeter != null) {
                ClientProxy.getClient(greeter).getConduit().close();
            }
            if (dispatch != null) {
                ((DispatchImpl)dispatch).getClient().getConduit().close();
            }
            greeterBus.shutdown(true);
            greeter = null;
            dispatch = null;
            greeterBus = null;
        }
    }

    private void stopControl() {
        if (null != control) {  
            assertTrue("Failed to stop greeter", control.stopGreeter(null));
            controlBus.shutdown(true);
        }
    }
    
    private void awaitMessages(int nExpectedOut, int nExpectedIn) {
        awaitMessages(nExpectedOut, nExpectedIn, 10000);
    }
    
    private void awaitMessages(int nExpectedOut, int nExpectedIn, int timeout) {
        MessageRecorder mr = new MessageRecorder(outRecorder, inRecorder);
        mr.awaitMessages(nExpectedOut, nExpectedIn, timeout);
    }
}

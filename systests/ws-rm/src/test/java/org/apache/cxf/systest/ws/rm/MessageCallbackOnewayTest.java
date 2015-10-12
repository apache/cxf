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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Provider;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.ServiceMode;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.rm.MessageCallback;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMMessageConstants;

import org.junit.After;
import org.junit.Test;

/**
 * Tests the operation of MessageCallback for one-way messages to the server.
 */
public class MessageCallbackOnewayTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(MessageCallbackOnewayTest.class);
    private static final String GREETER_ADDRESS  = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
    private static final Long RETRANSMISSION_INTERVAL = new Long(2000);

    private static final Logger LOG = LogUtils.getLogger(MessageCallbackOnewayTest.class);

    private Bus serverBus;
    private Endpoint endpoint;
    private Bus greeterBus;
    private Greeter greeter;
    private RecordingMessageCallback callback;
    
    @After
    public void tearDown() throws Exception {
        try {
            stopClient();
        } catch (Throwable t) {
            //ignore
        }
        try {
            stopServer();
        } catch (Throwable t) {
            //ignore
        }
        Thread.sleep(100);
    }
    
    /**
     * Checks that all callbacks are received, that messages are accepted in order, and that each message is accepted
     * before it is acknowledged (order of acknowledgements doesn't really matter).
     */
    private void verifyCallbacks() {
        List<Callback> cbs = callback.getCallbacks();
        Set<Long> acks = new HashSet<Long>();
        long nextNum = 1;
        for (Callback cb: cbs) {
            if (cb.isAccept()) {
                assertEquals(nextNum++, cb.getMsgNumber());
            } else {
                assertTrue(cb.getMsgNumber() < nextNum);
                Long num = Long.valueOf(cb.getMsgNumber());
                assertFalse(acks.contains(num));
                acks.add(num);
            }
        }
    }

    @Test    
    public void testAtLeastOnce() throws Exception {
        testOnewayAtLeastOnce(null);
    }
    
    @Test    
    public void testAtLeastOnceAsyncExecutor() throws Exception {
        testOnewayAtLeastOnce(Executors.newSingleThreadExecutor());
    } 

    private void testOnewayAtLeastOnce(Executor executor) throws Exception {
        init("org/apache/cxf/systest/ws/rm/atleastonce.xml", executor);
        
        greeterBus.getOutInterceptors().add(new MessageLossSimulator());
        RMManager manager = greeterBus.getExtension(RMManager.class);
        manager.getConfiguration().setBaseRetransmissionInterval(RETRANSMISSION_INTERVAL);
        String[] callArgs = new String[] {"one", "two", "three", "four"};
        for (int i = 0; i < callArgs.length; i++) {
            greeter.greetMeOneWay(callArgs[i]);
        }
        callback.waitDone(8, 3000, 60000);
        verifyCallbacks();
    }

    @Test    
    public void testAtMostOnce() throws Exception {
        testOnewayAtMostOnce(null);
    }
    
    @Test    
    public void testAtMostOnceAsyncExecutor() throws Exception {
        testOnewayAtMostOnce(Executors.newSingleThreadExecutor());
    } 

    private void testOnewayAtMostOnce(Executor executor) throws Exception {
        init("org/apache/cxf/systest/ws/rm/atmostonce.xml", executor);
        
        greeterBus.getOutInterceptors().add(new MessageLossSimulator());
        RMManager manager = greeterBus.getExtension(RMManager.class);
        manager.getConfiguration().setBaseRetransmissionInterval(RETRANSMISSION_INTERVAL);
        String[] callArgs = new String[] {"one", "two", "three", "four"};
        for (int i = 0; i < callArgs.length; i++) {
            greeter.greetMeOneWay(callArgs[i]);
        }
        
        callback.waitDone(8, 3000, 60000);
        verifyCallbacks();
    }

    @Test    
    public void testExactlyOnce() throws Exception {
        testOnewayExactlyOnce(null);
    }
    
    @Test    
    public void testExactlyOnceAsyncExecutor() throws Exception {
        testOnewayExactlyOnce(Executors.newSingleThreadExecutor());
    } 

    private void testOnewayExactlyOnce(Executor executor) throws Exception {
        init("org/apache/cxf/systest/ws/rm/exactlyonce.xml", executor);
        
        greeterBus.getOutInterceptors().add(new MessageLossSimulator());
        RMManager manager = greeterBus.getExtension(RMManager.class);
        manager.getConfiguration().setBaseRetransmissionInterval(RETRANSMISSION_INTERVAL);
        String[] callArgs = new String[] {"one", "two", "three", "four"};
        for (int i = 0; i < callArgs.length; i++) {
            greeter.greetMeOneWay(callArgs[i]);
        }
        
        callback.waitDone(8, 3000, 60000);
        verifyCallbacks();
    }

    @Test    
    public void testExactlyOnceInOrder() throws Exception {
        testOnewayExactlyOnceInOrder(null);
    }
    
    @Test    
    public void testExactlyOnceInOrderAsyncExecutor() throws Exception {
        testOnewayExactlyOnceInOrder(Executors.newSingleThreadExecutor());
    }

    private void testOnewayExactlyOnceInOrder(Executor executor) throws Exception {
        init("org/apache/cxf/systest/ws/rm/exactlyonce-inorder.xml", executor);
        
        greeterBus.getOutInterceptors().add(new MessageLossSimulator());
        RMManager manager = greeterBus.getExtension(RMManager.class);
        manager.getConfiguration().setBaseRetransmissionInterval(RETRANSMISSION_INTERVAL);
        String[] callArgs = new String[] {"one", "two", "three", "four"};
        for (int i = 0; i < callArgs.length; i++) {
            greeter.greetMeOneWay(callArgs[i]);
        }
        
        callback.waitDone(8, 3000, 60000);
        verifyCallbacks();
    }

    // --- test utilities ---

    private void init(String cfgResource, Executor executor) {
        SpringBusFactory bf = new SpringBusFactory();
        initServer(bf, cfgResource);
        initGreeterBus(bf, cfgResource);
        initProxy(executor);
    }
    
    private void initServer(SpringBusFactory bf, String cfgResource) {
        String derbyHome = System.getProperty("derby.system.home"); 
        try {
            synchronized (GreeterProvider.CALL_ARGS) {
                GreeterProvider.CALL_ARGS.clear();
            }
            System.setProperty("derby.system.home", derbyHome + "-server");   
            serverBus = bf.createBus(cfgResource);
            BusFactory.setDefaultBus(serverBus);
            LOG.info("Initialised bus " + serverBus + " with cfg file resource: " + cfgResource);
            LOG.info("serverBus inInterceptors: " + serverBus.getInInterceptors());
            endpoint = Endpoint.publish(GREETER_ADDRESS, new GreeterProvider());
        } finally {
            if (derbyHome != null) {
                System.setProperty("derby.system.home", derbyHome);
            } else {
                System.clearProperty("derby.system.home");
            }
        }
    }
    
    private void initGreeterBus(SpringBusFactory bf,
                                String cfgResource) {
        greeterBus = bf.createBus(cfgResource);
        BusFactory.setDefaultBus(greeterBus);
        LOG.fine("Initialised greeter bus with configuration: " + cfgResource);
    }

    private void initProxy(Executor executor) {        
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

        ConnectionHelper.setKeepAliveConnection(greeter, false);
        
        callback = new RecordingMessageCallback();
        ((BindingProvider)greeter).getRequestContext().put(RMMessageConstants.RM_CLIENT_CALLBACK, callback);
    }
    
    private void stopClient() {
        if (null != greeterBus) {
            
            //ensure we close the decoupled destination of the conduit,
            //so that release the port if the destination reference count hit zero
            if (greeter != null) {
                ClientProxy.getClient(greeter).getConduit().close();
            }
            greeterBus.shutdown(true);
            greeter = null;
            greeterBus = null;
        }
    }
    
    private void stopServer() {
        if (null != endpoint) {
            LOG.info("Stopping Greeter endpoint");
            endpoint.stop();
        } else {
            LOG.info("No endpoint active.");
        }
        endpoint = null;
        if (null != serverBus) {
            serverBus.shutdown(true);
            serverBus = null;
        }
    }

    @WebService(serviceName = "GreeterService",
                portName = "GreeterPort",
                targetNamespace = "http://cxf.apache.org/greeter_control",
                wsdlLocation = "/wsdl/greeter_control.wsdl")
    @ServiceMode(Mode.PAYLOAD)
    public static class GreeterProvider implements Provider<Source> {
        
        public static final List<String> CALL_ARGS = new ArrayList<String>();

        public Source invoke(Source obj) {

            Node el;
            try {
                el = StaxUtils.read(obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (el instanceof Document) {
                el = ((Document)el).getDocumentElement();
            }
            
            Map<String, String> ns = new HashMap<String, String>();
            ns.put("ns", "http://cxf.apache.org/greeter_control/types");
            XPathUtils xp = new XPathUtils(ns);
            String s = (String)xp.getValue("/ns:greetMe/ns:requestType",
                                           el,
                                           XPathConstants.STRING);

            if (s == null || "".equals(s)) {
                s = (String)xp.getValue("/ns:greetMeOneWay/ns:requestType",
                                        el,
                                        XPathConstants.STRING);
                synchronized (CALL_ARGS) {
                    CALL_ARGS.add(s);
                }
                return null;
            } else {
                synchronized (CALL_ARGS) {
                    CALL_ARGS.add(s);
                }
                String resp =
                    "<greetMeResponse "
                        + "xmlns=\"http://cxf.apache.org/greeter_control/types\">"
                        + "<responseType>" + s.toUpperCase() + "</responseType>"
                    + "</greetMeResponse>";
                return new StreamSource(new StringReader(resp));
            }
        }
    }
    
    private static class RecordingMessageCallback implements MessageCallback {
        
        private List<Callback> callbacks = new ArrayList<Callback>();
        
        @Override
        public void messageAccepted(String seqId, long msgNum) {
            synchronized (callbacks) {
                callbacks.add(new Callback(true, msgNum));
                callbacks.notifyAll();
            }
        }

        @Override
        public void messageAcknowledged(String seqId, long msgNum) {
            synchronized (callbacks) {
                callbacks.add(new Callback(false, msgNum));
                callbacks.notifyAll();
            }
        }
        
        public List<Callback> getCallbacks() {
            return callbacks;
        }
        
        /**
         * Wait for expected number of callbacks.
         * 
         * @param count expected number of callbacks
         * @param delay extra time to wait after expected number received (in case more are coming)
         * @param timeout maximum time to wait, in milliseconds
         */
        public void waitDone(int count, int delay, long timeout) {
            long start = System.currentTimeMillis();
            synchronized (callbacks) {
                while (callbacks.size() < count) {
                    long remain = start + timeout - System.currentTimeMillis();
                    if (remain <= 0) {
                        fail("Expected " + count + " callbacks, only got " + callbacks.size()); 
                    }
                    try {
                        callbacks.wait(remain);
                    } catch (InterruptedException e) { /* ignored */ }
                }
                try {
                    callbacks.wait((long)delay);
                } catch (InterruptedException e) { /* ignored */ }
                if (callbacks.size() > count) {
                    fail("Expected " + count + " callbacks, got " + callbacks.size()); 
                }
            }
        }
    }
    
    private static class Callback {
        private final boolean accept;
        private final long msgNumber;
        
        Callback(boolean acc, long msgNum) {
            accept = acc;
            msgNumber = msgNum;
        }

        public boolean isAccept() {
            return accept;
        }

        public long getMsgNumber() {
            return msgNumber;
        }
    }
}

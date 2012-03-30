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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
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
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.rm.RMManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the operation of InOrder delivery assurance for one-way messages to the server.
 */
public class DeliveryAssuranceOnewayTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(DeliveryAssuranceOnewayTest.class);
    private static final String GREETER_ADDRESS 
        = "http://localhost:" + PORT + "/SoapContext/GreeterPort";

    private static final Logger LOG = LogUtils.getLogger(DeliveryAssuranceOnewayTest.class);

    private Bus serverBus;
    private Endpoint endpoint;
    private Bus greeterBus;
    private Greeter greeter;


    @BeforeClass
    public static void setProps() throws Exception {
        TestUtilities.setKeepAliveSystemProperty(false);
    }
    
    @AfterClass
    public static void cleanup() {
        TestUtilities.recoverKeepAliveSystemProperty();
    }
            
    
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
        manager.getRMAssertion().getBaseRetransmissionInterval().setMilliseconds(new Long(2000));
        String[] callArgs = new String[] {"one", "two", "three", "four"};
        for (int i = 0; i < callArgs.length; i++) {
            greeter.greetMeOneWay(callArgs[i]);
        }
        
        awaitMessages(callArgs.length + 2, 3000, 60000);
        List<String> actualArgs = GreeterProvider.CALL_ARGS;
        assertTrue("Too few messages", callArgs.length <= actualArgs.size());
        for (int i = 0; i < callArgs.length; i++) {
            boolean match = false;
            for (int j = 0; j < actualArgs.size(); j++) {
                if (actualArgs.get(j).equals(callArgs[i])) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                fail("No match for request " + callArgs[i]);
            }
        }
        
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
        manager.getRMAssertion().getBaseRetransmissionInterval().setMilliseconds(new Long(2000));
        String[] callArgs = new String[] {"one", "two", "three", "four"};
        for (int i = 0; i < callArgs.length; i++) {
            greeter.greetMeOneWay(callArgs[i]);
        }
        
        awaitMessages(callArgs.length, 3000, 60000);
        List<String> actualArgs = GreeterProvider.CALL_ARGS;
        assertTrue("Too many messages", callArgs.length >= actualArgs.size());
        for (int i = 0; i < actualArgs.size() - 1; i++) {
            for (int j = i + 1; j < actualArgs.size(); j++) {
                if (actualArgs.get(j).equals(actualArgs.get(i))) {
                    fail("Message received more than once " + callArgs[i]);
                }
            }
        }
        
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
        manager.getRMAssertion().getBaseRetransmissionInterval().setMilliseconds(new Long(2000));
        String[] callArgs = new String[] {"one", "two", "three", "four"};
        for (int i = 0; i < callArgs.length; i++) {
            greeter.greetMeOneWay(callArgs[i]);
        }
        
        awaitMessages(callArgs.length, 3000, 60000);
        List<String> actualArgs = GreeterProvider.CALL_ARGS;
        assertEquals("Wrong message count", callArgs.length, actualArgs.size());
        for (int i = 0; i < callArgs.length; i++) {
            boolean match = false;
            for (int j = 0; j < actualArgs.size(); j++) {
                if (actualArgs.get(j).equals(callArgs[i])) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                fail("No match for request " + callArgs[i]);
            }
        }
        
    }

    @Test    
    public void testInOrder() throws Exception {
        testOnewayInOrder(null);
    }
    
    @Test    
    public void testInOrderAsyncExecutor() throws Exception {
        testOnewayInOrder(Executors.newSingleThreadExecutor());
    } 

    private void testOnewayInOrder(Executor executor) throws Exception {
        init("org/apache/cxf/systest/ws/rm/inorder.xml", executor);
        
        greeterBus.getOutInterceptors().add(new MessageLossSimulator());
        RMManager manager = greeterBus.getExtension(RMManager.class);
        manager.getRMAssertion().getBaseRetransmissionInterval().setMilliseconds(new Long(2000));
        String[] callArgs = new String[] {"one", "two", "three", "four"};
        for (int i = 0; i < callArgs.length; i++) {
            greeter.greetMeOneWay(callArgs[i]);
        }
        
        awaitMessages(callArgs.length - 2, 3000, 60000);
        List<String> actualArgs = GreeterProvider.CALL_ARGS;
        int argNum = 0;
        for (String actual : actualArgs) {
            while (argNum < callArgs.length && !actual.equals(callArgs[argNum])) {
                argNum++;
            }
            assertTrue("Message out of order", argNum < callArgs.length);
        }
    }

    @Test    
    public void testAtMostOnceInOrder() throws Exception {
        testOnewayAtMostOnceInOrder(null);
    }
    
    @Test    
    public void testAtMostOnceInOrderAsyncExecutor() throws Exception {
        testOnewayAtMostOnceInOrder(Executors.newSingleThreadExecutor());
    } 

    private void testOnewayAtMostOnceInOrder(Executor executor) throws Exception {
        init("org/apache/cxf/systest/ws/rm/atmostonce-inorder.xml", executor);
        
        greeterBus.getOutInterceptors().add(new MessageLossSimulator());
        RMManager manager = greeterBus.getExtension(RMManager.class);
        manager.getRMAssertion().getBaseRetransmissionInterval().setMilliseconds(new Long(2000));
        String[] callArgs = new String[] {"one", "two", "three", "four"};
        for (int i = 0; i < callArgs.length; i++) {
            greeter.greetMeOneWay(callArgs[i]);
        }
        
        awaitMessages(callArgs.length - 2, 3000, 60000);
        List<String> actualArgs = GreeterProvider.CALL_ARGS;
        assertTrue("Too many messages", callArgs.length >= actualArgs.size());
        int argNum = 0;
        for (String actual : actualArgs) {
            while (argNum < callArgs.length && !actual.equals(callArgs[argNum])) {
                argNum++;
            }
            assertTrue("Message out of order", argNum < callArgs.length);
        }
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
        manager.getRMAssertion().getBaseRetransmissionInterval().setMilliseconds(new Long(2000));
        String[] callArgs = new String[] {"one", "two", "three", "four"};
        for (int i = 0; i < callArgs.length; i++) {
            greeter.greetMeOneWay(callArgs[i]);
        }
        
        awaitMessages(callArgs.length, 3000, 60000);
        List<String> actualArgs = GreeterProvider.CALL_ARGS;
        assertEquals("Wrong number of messages", callArgs.length, actualArgs.size());
        int argNum = 0;
        for (String actual : actualArgs) {
            while (argNum < callArgs.length && !actual.equals(callArgs[argNum])) {
                argNum++;
            }
            assertTrue("Message out of order", argNum < callArgs.length);
        }
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

        ConnectionHelper.setKeepAliveConnection(greeter, true);
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
    
    /**
     * @param nExpectedIn number of messages to wait for
     * @param delay added delay before return (in case more are coming)
     * @param timeout maximum time to wait for expected messages
     */
    private void awaitMessages(int nExpectedIn, int delay, int timeout) {
        int waited = 0;
        int nIn = 0;
        while (waited <= timeout) {                
            synchronized (GreeterProvider.CALL_ARGS) {
                nIn = GreeterProvider.CALL_ARGS.size();
            }
            if (nIn >= nExpectedIn) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // ignore
            }
            waited += 100;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            // ignore
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
                el = XMLUtils.fromSource(obj);
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
}

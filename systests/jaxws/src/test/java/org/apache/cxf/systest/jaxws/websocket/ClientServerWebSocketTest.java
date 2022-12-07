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

package org.apache.cxf.systest.jaxws.websocket;

import java.io.Closeable;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Response;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.NoSuchCodeLitFault;
import org.apache.hello_world_soap_http.SOAPService;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClientServerWebSocketTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);

    static final Logger LOG = LogUtils.getLogger(ClientServerWebSocketTest.class);
    private final QName serviceName = new QName("http://apache.org/hello_world_soap_http",
                                                "SOAPService");
    private final QName portName = new QName("http://apache.org/hello_world_soap_http",
                                             "SoapPort");

    @BeforeClass
    public static void startServers() throws Exception {
        // set up configuration to enable schema validation
        //System.setProperty("org.apache.cxf.transport.websocket.atmosphere.disabled", "true");
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }

    @Test
    public void testBasicConnection() throws Exception {

        SOAPService service = new SOAPService();

        Greeter greeter = service.getPort(portName, Greeter.class);
        updateGreeterAddress(greeter, PORT);

        try {
            String reply = greeter.greetMe("test");
            assertNotNull("no response received from service", reply);
            assertEquals("Hello test", reply);

            reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals("Bonjour", reply);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
        BindingProvider bp = (BindingProvider)greeter;
        Map<String, Object> responseContext = bp.getResponseContext();
        Integer responseCode = (Integer) responseContext.get(Message.RESPONSE_CODE);
        assertEquals(200, responseCode.intValue());
    }

    @Test
    public void testBasicConnection2() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);

        //getPort only passing in SEI
        Greeter greeter = service.getPort(Greeter.class);
        updateGreeterAddress(greeter, PORT);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        try {
            for (int idx = 0; idx < 5; idx++) {
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                String exResponse = response1 + idx;
                assertEquals(exResponse, greeting);

                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);

                greeter.greetMeOneWay("Milestone-" + idx);
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void testTimeoutConfigutation() throws Exception {

        SOAPService service = new SOAPService();

        Greeter greeter = service.getPort(portName, Greeter.class);
        updateGreeterAddress(greeter, PORT);

        ((jakarta.xml.ws.BindingProvider)greeter).getRequestContext().put("jakarta.xml.ws.client.receiveTimeout",
                                                                        "1");
        try {
            greeter.greetMe("test");
            // remove fail() check to let this test pass in the powerful machine
        } catch (Throwable ex) {
            Object cause = null;
            if (ex.getCause() != null) {
                cause = ex.getCause();
            }
            assertTrue("Timeout cause is expected", cause instanceof java.net.SocketTimeoutException);
        }
    }

    @Test
    public void testBasicConnectionAndOneway() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);

        Greeter greeter = service.getPort(portName, Greeter.class);
        updateGreeterAddress(greeter, PORT);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        try {
            for (int idx = 0; idx < 1; idx++) {
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                String exResponse = response1 + idx;
                assertEquals(exResponse, greeting);

                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);

                greeter.greetMeOneWay("Milestone-" + idx);



            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void testFaults() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        ExecutorService ex = Executors.newFixedThreadPool(1);
        service.setExecutor(ex);
        assertNotNull(service);

        String noSuchCodeFault = "NoSuchCodeLitFault";
        String badRecordFault = "BadRecordLitFault";

        Greeter greeter = service.getPort(portName, Greeter.class);
        updateGreeterAddress(greeter, PORT);

        for (int idx = 0; idx < 2; idx++) {
            try {
                greeter.testDocLitFault(noSuchCodeFault);
                fail("Should have thrown NoSuchCodeLitFault exception");
            } catch (NoSuchCodeLitFault nslf) {
                assertNotNull(nslf.getFaultInfo());
                assertNotNull(nslf.getFaultInfo().getCode());
            }

            try {
                greeter.testDocLitFault(badRecordFault);
                fail("Should have thrown BadRecordLitFault exception");
            } catch (BadRecordLitFault brlf) {
                BindingProvider bp = (BindingProvider)greeter;
                Map<String, Object> responseContext = bp.getResponseContext();
                String contentType = (String) responseContext.get(Message.CONTENT_TYPE);
                assertEquals("text/xml; charset=utf-8", contentType.toLowerCase());
                Integer responseCode = (Integer) responseContext.get(Message.RESPONSE_CODE);
                assertEquals(500, responseCode.intValue());
                assertNotNull(brlf.getFaultInfo());
                assertEquals("BadRecordLitFault", brlf.getFaultInfo());
            }

        }

    }

    @Test
    public void testBasicAuth() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        Greeter greeter = service.getPort(portName, Greeter.class);
        updateGreeterAddress(greeter, PORT);

        try {
            //try the jaxws way
            BindingProvider bp = (BindingProvider)greeter;
            bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "BJ");
            bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "pswd");
            String s = greeter.greetMe("secure");
            assertEquals("Hello BJ", s);
            bp.getRequestContext().remove(BindingProvider.USERNAME_PROPERTY);
            bp.getRequestContext().remove(BindingProvider.PASSWORD_PROPERTY);
            ((Closeable)greeter).close();

            greeter = service.getPort(portName, Greeter.class);
            updateGreeterAddress(greeter, PORT);
            //try setting on the conduit directly
            Client client = ClientProxy.getClient(greeter);
            HTTPConduit httpConduit = (HTTPConduit)client.getConduit();
            AuthorizationPolicy policy = new AuthorizationPolicy();
            policy.setUserName("BJ2");
            policy.setPassword("pswd");
            httpConduit.setAuthorization(policy);

            s = greeter.greetMe("secure");
            ((Closeable)greeter).close();
            assertEquals("Hello BJ2", s);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void testAsyncPollingCall() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        Greeter greeter = service.getPort(portName, Greeter.class);
        updateGreeterAddress(greeter, PORT);

        long before = System.currentTimeMillis();

        long delay = 3000;
        Response<GreetMeLaterResponse> r1 = greeter.greetMeLaterAsync(delay);
        Response<GreetMeLaterResponse> r2 = greeter.greetMeLaterAsync(delay);

        long after = System.currentTimeMillis();

        assertTrue("Duration of calls exceeded " + (2 * delay) + " ms", after - before < (2 * delay));

        // first time round, responses should not be available yet
        assertFalse("Response already available.", r1.isDone());
        assertFalse("Response already available.", r2.isDone());

        // after three seconds responses should be available
        long waited = 0;
        while (waited < (delay + 1000)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
               // ignore
            }
            if (r1.isDone() && r2.isDone()) {
                break;
            }
            waited += 500;
        }
        assertTrue("Response is  not available.", r1.isDone());
        assertTrue("Response is  not available.", r2.isDone());
    }

    @Test
    public void testAsyncSynchronousPolling() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);

        final String expectedString = new String("Hello, finally!");

        class Poller extends Thread {
            Response<GreetMeLaterResponse> response;
            int tid;

            Poller(Response<GreetMeLaterResponse> r, int t) {
                response = r;
                tid = t;
            }
            public void run() {
                if (tid % 2 > 0) {
                    while (!response.isDone()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                            // ignore
                        }
                    }
                }
                GreetMeLaterResponse reply = null;
                try {
                    reply = response.get();
                } catch (Exception ex) {
                    fail("Poller " + tid + " failed with " + ex);
                }
                assertNotNull("Poller " + tid + ": no response received from service", reply);
                String s = reply.getResponseType();
                assertEquals(expectedString, s);
            }
        }

        Greeter greeter = service.getPort(portName, Greeter.class);
        updateGreeterAddress(greeter, PORT);
        long before = System.currentTimeMillis();


        long delay = 3000;

        Response<GreetMeLaterResponse> response = greeter.greetMeLaterAsync(delay);
        long after = System.currentTimeMillis();

        assertTrue("Duration of calls exceeded " + delay + " ms", after - before < delay);

        // first time round, responses should not be available yet
        assertFalse("Response already available.", response.isDone());


        Poller[] pollers = new Poller[4];
        for (int i = 0; i < pollers.length; i++) {
            pollers[i] = new Poller(response, i);
        }
        for (Poller p : pollers) {
            p.start();
        }

        for (Poller p : pollers) {
            p.join();
        }


    }

    static class MyHandler implements AsyncHandler<GreetMeLaterResponse> {
        static int invocationCount;
        private String replyBuffer;

        public void handleResponse(Response<GreetMeLaterResponse> response) {
            invocationCount++;
            try {
                GreetMeLaterResponse reply = response.get();
                replyBuffer = reply.getResponseType();
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
        }

        String getReplyBuffer() {
            return replyBuffer;
        }
    }

    private void updateGreeterAddress(Greeter greeter, String port) {
        ((BindingProvider)greeter).getRequestContext()
            .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                 "ws://localhost:" + PORT + "/SoapContext/SoapPort");
    }

}

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

package org.apache.cxf.systest.jaxws;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.dynamic.DynamicClientFactory;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.DocLitBare;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.NoSuchCodeLitFault;
import org.apache.hello_world_soap_http.SOAPService;
import org.apache.hello_world_soap_http.SOAPServiceBogusAddressTest;
import org.apache.hello_world_soap_http.SOAPServiceDocLitBare;
import org.apache.hello_world_soap_http.SOAPServiceMultiPortTypeTest;
import org.apache.hello_world_soap_http.types.BareDocumentResponse;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientServerTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String BARE_PORT = allocatePort(Server.class, 1);
    static final String BOGUS_REAL_PORT = allocatePort(Server.class, 2);

    static final String BOGUS_PORT = allocatePort(Server.class, 3);
    static final String PUB_PORT = allocatePort(Server.class, 4);
    
    

    static final Logger LOG = LogUtils.getLogger(ClientServerTest.class);
    private final QName serviceName = new QName("http://apache.org/hello_world_soap_http",
                                                "SOAPService");    
    private final QName portName = new QName("http://apache.org/hello_world_soap_http",
                                             "SoapPort");
    
    private final QName fakePortName = new QName("http://apache.org/hello_world_soap_http",
                                                 "FakePort");
    
    
    private final QName portName1  = new QName("http://apache.org/hello_world_soap_http",
                                               "SoapPort2");


    @BeforeClass
    public static void startServers() throws Exception {                    
        // set up configuration to enable schema validation
        URL url = ClientServerTest.class.getResource("fault-stack-trace.xml");
        assertNotNull("cannot find test resource", url);
        defaultConfigFileName = url.toString();

        assertTrue("server did not launch correctly", launchServer(Server.class));
    }
    @Test
    public void testCXF2419() throws Exception {
        org.apache.cxf.hello_world.elrefs.SOAPService serv 
            = new org.apache.cxf.hello_world.elrefs.SOAPService();
        org.apache.cxf.hello_world.elrefs.Greeter g = serv.getSoapPort();
        updateAddressPort(g, PORT);
        assertEquals("Hello CXF", g.greetMe("CXF"));
    }
    
    @Test
    public void testBase64() throws Exception  {
        URL wsdl = getClass().getResource("/wsdl/others/dynamic_client_base64.wsdl");
        assertNotNull(wsdl);
        String wsdlUrl = null;
        wsdlUrl = wsdl.toURI().toString();
        CXFBusFactory busFactory = new CXFBusFactory(); 
        Bus bus = busFactory.createBus();
        DynamicClientFactory dynamicClientFactory = DynamicClientFactory.newInstance(bus);
        Client client = dynamicClientFactory.createClient(wsdlUrl);
        assertNotNull(client);
    }
    
    @Test
    public void testJaxWsDynamicClient() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/others/dynamic_client_base64.wsdl");
        assertNotNull(wsdl);
        String wsdlUrl = null;
        wsdlUrl = wsdl.toURI().toString();
        CXFBusFactory busFactory = new CXFBusFactory(); 
        Bus bus = busFactory.createBus();
        org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory dynamicClientFactory = 
            org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory.newInstance(bus);
        Client client = dynamicClientFactory.createClient(wsdlUrl);
        assertNotNull(client);
    }
        
    @Test
    public void testBasicConnection() throws Exception {

        SOAPService service = new SOAPService();
        assertNotNull(service);

        Greeter greeter = service.getPort(portName, Greeter.class);
        updateAddressPort(greeter, PORT);
        try {
            greeter.greetMe("test");
            
            String reply = greeter.sayHi();
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
    public void testTimeoutConfigutation() throws Exception {

        SOAPService service = new SOAPService();
        assertNotNull(service);

        Greeter greeter = service.getPort(portName, Greeter.class);
        updateAddressPort(greeter, PORT);
        ((javax.xml.ws.BindingProvider)greeter).getRequestContext().put("javax.xml.ws.client.receiveTimeout",
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
    public void testNillable() throws Exception {
        SOAPService service = new SOAPService();
        assertNotNull(service);

        Greeter greeter = service.getPort(portName, Greeter.class);
        updateAddressPort(greeter, PORT);

        try {
            String reply = greeter.testNillable("test", 100);
            assertEquals("test", reply);
            reply = greeter.testNillable(null, 100);
            assertNull(reply);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }

    }
    
    @Test
    public void testAddPortWithSpecifiedSoap12Binding() throws Exception {
        Service service = Service.create(serviceName);
        service.addPort(fakePortName, javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING, 
                        "http://localhost:" + PORT + "/SoapContext/SoapPort");
        Greeter greeter = service.getPort(fakePortName, Greeter.class);

        String response = new String("Bonjour");
        try {
            greeter.greetMe("test");
            String reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals(response, reply);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
        
        
    }
    
    @Test
    public void testAddPortWithSpecifiedSoap11Binding() throws Exception {
        Service service = Service.create(serviceName);
        service.addPort(fakePortName, javax.xml.ws.soap.SOAPBinding.SOAP11HTTP_BINDING, 
            "http://localhost:" + PORT + "/SoapContext/SoapPort");
        Greeter greeter = service.getPort(fakePortName, Greeter.class);

        String response = new String("Bonjour");
        try {
            greeter.greetMe("test");
            String reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals(response, reply);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
        
        
    }


    
    
    @Test
    public void testAddPort() throws Exception {
        Service service = Service.create(serviceName);
        service.addPort(fakePortName, "http://schemas.xmlsoap.org/soap/", 
                        "http://localhost:" + PORT + "/SoapContext/SoapPort");
        Greeter greeter = service.getPort(fakePortName, Greeter.class);

        String response = new String("Bonjour");
        try {
            greeter.greetMe("test");
            String reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals(response, reply);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    @Test
    public void testGetPortOneParam() throws Exception {

        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        Service service = Service.create(url, serviceName);
        
        Greeter greeter = service.getPort(Greeter.class);
        String response = new String("Bonjour");
         
        try {
            ((BindingProvider)greeter).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                     "http://localhost:" + PORT + "/SoapContext/SoapPort");
            greeter.greetMe("test");
            String reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals(response, reply);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void testDocLitBareConnection() throws Exception {
        
        SOAPServiceDocLitBare service = new SOAPServiceDocLitBare();
        assertNotNull(service);

        DocLitBare greeter = service.getPort(portName1, DocLitBare.class);
        updateAddressPort(greeter, BARE_PORT);
        try {
       
            BareDocumentResponse bareres = greeter.testDocLitBare("MySimpleDocument");
            assertNotNull("no response for operation testDocLitBare", bareres);
            assertEquals("CXF", bareres.getCompany());
            assertTrue(bareres.getId() == 1);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
        
    @Test
    public void testBasicConnectionAndOneway() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);
        
        Greeter greeter = service.getPort(portName, Greeter.class);
        updateAddressPort(greeter, PORT);
        
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
    public void testBasicConnection2() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);
        
        //getPort only passing in SEI
        Greeter greeter = service.getPort(Greeter.class);
        ((BindingProvider)greeter).getRequestContext()
            .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                 "http://localhost:" + PORT + "/SoapContext/SoapPort");
        
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
    public void testAsyncPollingCall() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        
        assertNotNull(service);
        
        Greeter greeter = service.getPort(portName, Greeter.class);
        
        assertNotNull(service);
        updateAddressPort(greeter, PORT);

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
        
        Greeter greeter = (Greeter)service.getPort(portName, Greeter.class);
        updateAddressPort(greeter, PORT);
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
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } catch (ExecutionException ex) {
                ex.printStackTrace();
            }            
        } 
        
        String getReplyBuffer() {
            return replyBuffer;
        }
    }
    
    @Test
    public void testAsyncCallUseProperAssignedExecutor() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        
        class TestExecutor implements Executor {
            
            private int count;
            
            public void execute(Runnable command) {
                count++;
                LOG.info("asyn call time " + count);
                command.run();
            }
            
            public int getCount() {
                return count;
            }
        }
        Executor executor = new TestExecutor();
        service.setExecutor(executor);
        assertNotNull(service);
        assertSame(executor, service.getExecutor());
        
        
        assertEquals(((TestExecutor)executor).getCount(), 0);
        Greeter greeter = service.getPort(portName, Greeter.class);
        updateAddressPort(greeter, PORT);
        List<Response<GreetMeResponse>> responses = new ArrayList<Response<GreetMeResponse>>();
        for (int i = 0; i < 5; i++) {
            responses.add(greeter.greetMeAsync("asyn call" + i));
        }
        //wait for all the responses
        for (Response<GreetMeResponse> resp : responses) {
            resp.get();
        }
            
        assertEquals(5, ((TestExecutor)executor).getCount());
    }

    
    @Test
    public void testAsyncCallWithHandler() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);
        
        MyHandler h = new MyHandler();
        MyHandler.invocationCount = 0;

        String expectedString = new String("Hello, finally!");
        try {
            Greeter greeter = (Greeter)service.getPort(portName, Greeter.class);
            updateAddressPort(greeter, PORT);
            long before = System.currentTimeMillis();
            long delay = 3000;
            Future<?> f = greeter.greetMeLaterAsync(delay, h);
            long after = System.currentTimeMillis();
            assertTrue("Duration of calls exceeded " + delay + " ms", after - before < delay);
            // first time round, responses should not be available yet
            assertFalse("Response already available.", f.isDone());


            int i = 0;
            while (!f.isDone() && i < 50) {
                Thread.sleep(100);
                i++;
            }
            assertEquals("callback was not executed or did not return the expected result",
                         expectedString, h.getReplyBuffer());
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
        assertEquals(1, MyHandler.invocationCount);       
        
    }

    @Test
    public void testAsyncCallWithHandlerAndMultipleClients() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        
        assertNotNull(service);
        
        final MyHandler h = new MyHandler();
        MyHandler.invocationCount = 0;

        final String expectedString = new String("Hello, finally!");
        
        class Poller extends Thread {
            Future<?> future;
            int tid;
            
            Poller(Future<?> f, int t) {
                future = f;
                tid = t;
            }
            public void run() {
                if (tid % 2 > 0) {
                    while (!future.isDone()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                            // ignore
                            ex.printStackTrace();
                        }
                    }
                }
                try {
                    future.get();
                } catch (Exception ex) {
                    fail("Poller " + tid + " failed with " + ex);
                }
                assertEquals("callback was not executed or did not return the expected result",
                             expectedString, h.getReplyBuffer());
            }
        }
        
        Greeter greeter = service.getPort(portName, Greeter.class);
        updateAddressPort(greeter, PORT);

        long before = System.currentTimeMillis();
        long delay = 3000;
        Future<?> f = greeter.greetMeLaterAsync(delay, h);
        long after = System.currentTimeMillis();
        assertTrue("Duration of calls exceeded " + delay + " ms", after - before < delay);
        // first time round, responses should not be available yet
        assertFalse("Response already available.", f.isDone());
        
        Poller[] pollers = new Poller[4];
        for (int i = 0; i < pollers.length; i++) {
            pollers[i] = new Poller(f, i);
        }
        for (Poller p : pollers) {            
            p.start();
        }
        
        for (Poller p : pollers) {
            p.join();
        }
        assertEquals(1, MyHandler.invocationCount);   
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
        updateAddressPort(greeter, PORT);
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
                assertEquals("text/xml; charset=utf-8", contentType);
                Integer responseCode = (Integer) responseContext.get(Message.RESPONSE_CODE);
                assertEquals(500, responseCode.intValue());                
                assertNotNull(brlf.getFaultInfo());
                assertEquals("BadRecordLitFault", brlf.getFaultInfo());
            }
                        
        }

    }

    @Test
    public void testFaultStackTrace() throws Exception {
        System.setProperty("cxf.config.file.url", 
                getClass().getResource("fault-stack-trace.xml").toString());
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        SOAPService service = new SOAPService(wsdl, serviceName);
        ExecutorService ex = Executors.newFixedThreadPool(1);
        service.setExecutor(ex);
        assertNotNull(service);        
        Greeter greeter = service.getPort(portName, Greeter.class);
        updateAddressPort(greeter, PORT);
        try {
            // trigger runtime exception throw of implementor method
            greeter.testDocLitFault("");
            fail("Should have thrown Runtime exception");
        } catch (WebServiceException e) {
            assertEquals("can't get back original message", "Unknown source", e.getCause().getMessage());
            assertTrue(e.getCause().getStackTrace().length > 0);            
        }
    }
    
    @Test
    public void testGetSayHi() throws Exception {
        HttpURLConnection httpConnection = 
            getHttpConnection("http://localhost:" + PORT + "/SoapContext/SoapPort/sayHi");
        httpConnection.connect(); 
        
        httpConnection.connect();
        
        assertEquals(200, httpConnection.getResponseCode());
        
        assertEquals("text/xml; charset=utf-8", httpConnection.getContentType());
        assertEquals("OK", httpConnection.getResponseMessage());
        
        InputStream in = httpConnection.getInputStream();
        assertNotNull(in);        
       
        Document doc = XMLUtils.parse(in);
        assertNotNull(doc);
        
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("soap", Soap11.SOAP_NAMESPACE);
        ns.put("ns2", "http://apache.org/hello_world_soap_http/types");
        XPathUtils xu = new XPathUtils(ns);
        Node body = (Node) xu.getValue("/soap:Envelope/soap:Body", doc, XPathConstants.NODE);
        assertNotNull(body);
        String response = (String) xu.getValue("//ns2:sayHiResponse/ns2:responseType/text()", 
                                               body, 
                                               XPathConstants.STRING);
        assertEquals("Bonjour", response);
    }

    @Test
    public void testGetGreetMe() throws Exception {
        HttpURLConnection httpConnection = 
            getHttpConnection("http://localhost:" + PORT 
                              + "/SoapContext/SoapPort/greetMe/requestType/cxf");    
        httpConnection.connect();        
        
        assertEquals(200, httpConnection.getResponseCode());
    
        assertEquals("text/xml; charset=utf-8", httpConnection.getContentType());
        assertEquals("OK", httpConnection.getResponseMessage());
        
        InputStream in = httpConnection.getInputStream();
        assertNotNull(in);
        
        Document doc = XMLUtils.parse(in);
        assertNotNull(doc);
        
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("soap", Soap11.SOAP_NAMESPACE);
        ns.put("ns2", "http://apache.org/hello_world_soap_http/types");
        XPathUtils xu = new XPathUtils(ns);
        Node body = (Node) xu.getValue("/soap:Envelope/soap:Body", doc, XPathConstants.NODE);
        assertNotNull(body);
        String response = (String) xu.getValue("//ns2:greetMeResponse/ns2:responseType/text()", 
                                               body, 
                                               XPathConstants.STRING);
        assertEquals("Hello cxf", response);
    }
    
    @Test
    public void testGetWSDL() throws Exception {
        String url = "http://localhost:" + PORT + "/SoapContext/SoapPort?wsdl";
        HttpURLConnection httpConnection = getHttpConnection(url);    
        httpConnection.connect();        
        
        assertEquals(200, httpConnection.getResponseCode());
    
        assertEquals("text/xml", httpConnection.getContentType());
        assertEquals("OK", httpConnection.getResponseMessage());
        
        InputStream in = httpConnection.getInputStream();
        assertNotNull(in);
        
        Document doc = XMLUtils.parse(in);
        assertNotNull(doc);
       
                
    }
    
    @Test
    public void testGetGreetMeFromQuery() throws Exception {
        String url = "http://localhost:" + PORT + "/SoapContext/SoapPort/greetMe?requestType=" 
            + URLEncoder.encode("cxf (was CeltixFire)", "UTF-8"); 
        
        HttpURLConnection httpConnection = getHttpConnection(url);    
        httpConnection.connect();        
        
        assertEquals(200, httpConnection.getResponseCode());
    
        assertEquals("text/xml; charset=utf-8", httpConnection.getContentType());
        assertEquals("OK", httpConnection.getResponseMessage());
        
        InputStream in = httpConnection.getInputStream();
        assertNotNull(in);
        
        Document doc = XMLUtils.parse(in);
        assertNotNull(doc);
        
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("soap", Soap11.SOAP_NAMESPACE);
        ns.put("ns2", "http://apache.org/hello_world_soap_http/types");
        XPathUtils xu = new XPathUtils(ns);
        Node body = (Node) xu.getValue("/soap:Envelope/soap:Body", doc, XPathConstants.NODE);
        assertNotNull(body);
        String response = (String) xu.getValue("//ns2:greetMeResponse/ns2:responseType/text()", 
                                               body, 
                                               XPathConstants.STRING);
        assertEquals("Hello cxf (was CeltixFire)", response);
    }
    
    @Test
    public void testBasicAuth() throws Exception {
        Service service = Service.create(serviceName);
        service.addPort(fakePortName, "http://schemas.xmlsoap.org/soap/", 
                        "http://localhost:" + PORT + "/SoapContext/SoapPort");
        Greeter greeter = service.getPort(fakePortName, Greeter.class);

        try {
            //try the jaxws way
            BindingProvider bp = (BindingProvider)greeter;
            bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "BJ");
            bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "pswd");
            String s = greeter.greetMe("secure");
            assertEquals("Hello BJ", s);
            bp.getRequestContext().remove(BindingProvider.USERNAME_PROPERTY);
            bp.getRequestContext().remove(BindingProvider.PASSWORD_PROPERTY);
            
            //try setting on the conduit directly
            Client client = ClientProxy.getClient(greeter);
            HTTPConduit httpConduit = (HTTPConduit)client.getConduit();
            AuthorizationPolicy policy = new AuthorizationPolicy();
            policy.setUserName("BJ2");
            policy.setPassword("pswd");
            httpConduit.setAuthorization(policy);
            
            s = greeter.greetMe("secure");
            assertEquals("Hello BJ2", s);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    

    @Test
    public void testBogusAddress() throws Exception {
        String realAddress = "http://localhost:" + BOGUS_REAL_PORT + "/SoapContext/SoapPort";
        SOAPServiceBogusAddressTest service = new SOAPServiceBogusAddressTest();
        Greeter greeter = service.getSoapPort();
        try {
            greeter.greetMe("test");
            fail("Should fail");
        } catch (WebServiceException f) {
            // expected
        }

        
        
        BindingProvider bp = (BindingProvider)greeter;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                   realAddress);
        greeter.greetMe("test");

        //should persist
        greeter.greetMe("test");

        bp.getRequestContext().remove(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        
        try {
            greeter.greetMe("test");
            fail("Should fail");
        } catch (WebServiceException f) {
            // expected
        }

        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                   realAddress);
        String reply = greeter.sayHi();
        assertNotNull("no response received from service", reply);
        assertEquals("Bonjour", reply);
                                   
    }

    /*
    @Test
    public void testDynamicClientFactory2() throws Exception {
        String wsdlUrl = "http://sdpwsparam.strikeiron.com/sdpNFLTeams?WSDL";

        //TODO test fault exceptions 
        DynamicClientFactory dcf = DynamicClientFactory.newInstance();
        Client client = dcf.createClient(wsdlUrl);
        Object o = Class.forName("com.strikeiron.GetTeamInfoByCity", true, 
                                 Thread.currentThread().getContextClassLoader()).newInstance();
        Object[] result = client.invoke("GetTeamInfoByCity", "a", "b", "New England");
        
        
        //System.out.println(Arrays.asList(result));
        
    }
    */
    @Test
    public void testDynamicClientFactory() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        String wsdlUrl = null;
        wsdlUrl = wsdl.toURI().toString();

        //TODO test fault exceptions 
        DynamicClientFactory dcf = DynamicClientFactory.newInstance();
        Client client = dcf.createClient(wsdlUrl, serviceName, portName);
        updateAddressPort(client, PORT);
        client.invoke("greetMe", "test");        
        Object[] result = client.invoke("sayHi");
        assertNotNull("no response received from service", result);
        assertEquals("Bonjour", result[0]);
        //TODO: the following isn't a real test. We need to test against a service
        // that would actually notice the difference. At least it ensures that 
        // specifying the property does not explode.
        Map<String, Object> jaxbContextProperties = new HashMap<String, Object>();
        jaxbContextProperties.put("com.sun.xml.bind.defaultNamespaceRemap", "uri:ultima:thule");
        dcf.setJaxbContextProperties(jaxbContextProperties);
        client = dcf.createClient(wsdlUrl, serviceName, portName);
        updateAddressPort(client, PORT);
        client.invoke("greetMe", "test");        
        result = client.invoke("sayHi");
        assertNotNull("no response received from service", result);
        assertEquals("Bonjour", result[0]);
    }
    
    @Test
    public void testMultiPorts() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        QName sname = new QName("http://apache.org/hello_world_soap_http",
                                "SOAPServiceMultiPortTypeTest");
        SOAPServiceMultiPortTypeTest service = new SOAPServiceMultiPortTypeTest(wsdl, sname);
        DocLitBare b = service.getDocLitBarePort();
        updateAddressPort(b, PORT);
        BareDocumentResponse resp = b.testDocLitBare("CXF");
        assertNotNull(resp);
        assertEquals("CXF", resp.getCompany());
        
        Greeter g = service.getGreeterPort();
        updateAddressPort(g, PORT);
        String result = g.greetMe("CXF");
        assertEquals("Hello CXF", result);
    }

    @Test
    public void testProxy() throws Exception {
        InvocationHandler handler = new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // TODO Auto-generated method stub
                return null;
            }
            
        };
        Object implementor4 = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                     new Class<?>[] {DocLitWrappedCodeFirstService.class},
                                                     handler);
        Endpoint.publish("http://localhost:" + PUB_PORT + "/DocLitWrappedCodeFirstService/", implementor4);
        URL url = new URL("http://localhost:" + PUB_PORT + "/DocLitWrappedCodeFirstService/?wsdl");
        InputStream ins = url.openStream();
        ins.close();
    }
    
}

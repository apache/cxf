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

package org.apache.cxf.systest.http.jaxws;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jakarta.jws.WebService;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Response;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPBinding;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.AbstractGreeterImpl;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.types.GreetMeResponse;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.http.HTTPConduit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXWSAsyncClientTest  extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static ScheduledExecutorService executor;
    
    public static class Server extends AbstractBusTestServerBase {

        protected void run()  {
            GreeterImpl implementor = new GreeterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
            jakarta.xml.ws.Endpoint.publish(address, implementor);
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

        @WebService(serviceName = "BasicGreeterService",
                    portName = "GreeterPort",
                    endpointInterface = "org.apache.cxf.greeter_control.Greeter",
                    targetNamespace = "http://cxf.apache.org/greeter_control",
                    wsdlLocation = "testutils/greeter_control.wsdl")
        public class GreeterImpl extends AbstractGreeterImpl {
            @Override
            public String greetMe(String arg) {
                if ("timeout".equalsIgnoreCase(arg)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // Do nothing
                    }
                }
                
                return super.greetMe(arg);
            }
        }
    }


    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        executor = Executors.newScheduledThreadPool(5);
    }

    @AfterClass
    public static void stopServers() throws Exception {
        stopAllServers();
        if (executor != null) {
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void testAsyncClient() throws Exception {
        // setup the feature by using JAXWS front-end API
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress("http://localhost:" + PORT + "/SoapContext/GreeterPort");
        factory.setServiceClass(Greeter.class);
        Greeter proxy = factory.create(Greeter.class);

        Response<GreetMeResponse>  response = proxy.greetMeAsync("cxf");
        int waitCount = 0;
        while (!response.isDone() && waitCount < 15) {
            Thread.sleep(1000);
            waitCount++;
        }
        
        assertTrue("Response still not received.", response.isDone());
        assertThat(response.get().getResponseType(), equalTo("CXF"));
    }
    
    @Test
    public void testAsyncClientChunking() throws Exception {
        // setup the feature by using JAXWS front-end API
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress("http://localhost:" + PORT + "/SoapContext/GreeterPort");
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.setServiceClass(Greeter.class);
        Greeter proxy = factory.create(Greeter.class);

        Client client = ClientProxy.getClient(proxy);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.getClient().setAllowChunking(true);

        final char[] bytes = new char [32 * 1024];
        final Random random = new Random();
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (char)(random.nextInt(26) + 'a');
        }

        final String greeting = new String(bytes);
        Response<GreetMeResponse>  response = proxy.greetMeAsync(greeting);
        int waitCount = 0;
        while (!response.isDone() && waitCount < 15) {
            Thread.sleep(1000);
            waitCount++;
        }
        
        assertTrue("Response still not received.", response.isDone());
        assertThat(response.get().getResponseType(), equalTo(greeting.toUpperCase()));
    }

    @Test
    public void testTimeout() throws Exception {
        // setup the feature by using JAXWS front-end API
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress("http://localhost:" + PORT + "/SoapContext/GreeterPort");
        factory.setServiceClass(Greeter.class);
        Greeter proxy = factory.create(Greeter.class);
        
        HTTPConduit cond = (HTTPConduit)((Client)proxy).getConduit();
        cond.getClient().setReceiveTimeout(500);

        try {
            proxy.greetMeAsync("timeout").get();
            fail("Should have faulted");
        } catch (SOAPFaultException ex) {
            fail("should not be a SOAPFaultException");
        } catch (ExecutionException ex) {
            //expected
            assertTrue(ex.getCause().getClass().getName(),
                       ex.getCause() instanceof java.net.ConnectException
                       || ex.getCause() instanceof java.net.SocketTimeoutException);
        }
    }
    
    /**
     * Not 100% reproducible but used to sporadically fail with:
     * 
     * java.util.concurrent.ExecutionException: jakarta.xml.ws.soap.SOAPFaultException: 
     *   Cannot invoke "org.w3c.dom.Node.getOwnerDocument()" because "nd" is null
     *   at java.base/java.util.concurrent.CompletableFuture.reportGet(CompletableFuture.java:396)
     *   at java.base/java.util.concurrent.CompletableFuture.get(CompletableFuture.java:2073)
     *   at org.apache.cxf.endpoint.ClientCallback.get(ClientCallback.java:139)
     *   at org.apache.cxf.jaxws.JaxwsResponseCallback.get(JaxwsResponseCallback.java:48)
     *   at org.apache.cxf.systest.hc5.jaxws.JAXWSAsyncClientTest.testAsyncWsdl(JAXWSAsyncClientTest.java:193)
     *
     * @see https://issues.apache.org/jira/browse/CXF-9007
     */
    @Test
    public void testAsyncWsdl() throws Exception {
        final URL wsdlUrl = getClass().getClassLoader().getResource("greeting.wsdl");

        final Service service = Service.create(wsdlUrl, new QName("http://apache.org/hello_world", "SOAPService"));

        service.addPort(new QName("http://apache.org/hello_world", "Greeter"), SOAPBinding.SOAP11HTTP_BINDING,
                "http://localhost:" + PORT + "/SoapContext/GreeterPort");

        final Dispatch<SOAPMessage> client = service.createDispatch(
                new QName("http://apache.org/hello_world", "Greeter"),
                SOAPMessage.class,
                Service.Mode.MESSAGE
        );

        final List<Future<Response<SOAPMessage>>> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            tasks.add(executor.submit(() -> client.invokeAsync(buildMessage())));
        }

        for (Future<Response<SOAPMessage>> task : tasks) {
            final SOAPMessage result = task.get(5, TimeUnit.SECONDS).get();
            verifyResult(result);
        }
    }

    private static void verifyResult(SOAPMessage message) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        
        String output =  null;
        try (StringWriter writer = new StringWriter()) {
            final DOMSource source = new DOMSource(message.getSOAPBody().extractContentAsDocument());
            transformer.transform(source, new StreamResult(writer));
            output = writer.getBuffer().toString().replaceAll("\n|\r", "");
        }

        assertThat(output, containsString("HELLO"));
    }

    private static SOAPMessage buildMessage() throws SOAPException, IOException {
        String soapMessage = 
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soapenv:Header/>"
            + "  <soapenv:Body>"
            + "    <ns2:greetMe xmlns:ns2=\"http://cxf.apache.org/greeter_control/types\">"
            + "        <ns2:requestType>Hello</ns2:requestType>"
            + "    </ns2:greetMe>"
            + "  </soapenv:Body>"
            + "</soapenv:Envelope>";

        SOAPMessage message = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(soapMessage.getBytes())) {
            message = MessageFactory.newInstance().createMessage(null, bis);
            message.saveChanges();
        }

        return message;
    }
}

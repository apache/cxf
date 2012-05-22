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

package org.apache.cxf.systest.dispatch;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Future;


import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Response;
import javax.xml.ws.Service;


import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.hello_world_soap_http.SOAPService;
import org.junit.BeforeClass;
import org.junit.Test;

public class DispatchClientServerWithMalformedResponseTest extends AbstractBusClientServerTestBase {

    private static final QName SERVICE_NAME 
        = new QName("http://apache.org/hello_world_soap_http", "SOAPDispatchService");
    private static final QName PORT_NAME 
        = new QName("http://apache.org/hello_world_soap_http", "SoapDispatchPort");

    private static String greeterPort = 
        TestUtil.getPortNumber(DispatchClientServerWithMalformedResponseTest.class); 
    private int asyncHandlerInvokedCount;
    
    public static class Server extends AbstractBusTestServerBase {        
        Endpoint ep;
        protected void run() {
            setBus(BusFactory.getDefaultBus());
            Object implementor = new GreeterImpl();
            String address = "http://localhost:"
                + TestUtil.getPortNumber(DispatchClientServerWithMalformedResponseTest.class)
                + "/SOAPDispatchService/SoapDispatchPort";
            ep = Endpoint.publish(address, implementor);
        }
        @Override
        public void tearDown() {
            ep.stop();
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
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }
    
    @org.junit.Before
    public void setUp() {
        BusFactory.setThreadDefaultBus(getStaticBus());
        BusFactory.getThreadDefaultBus().getOutInterceptors().add(new LoggingOutInterceptor());
        BusFactory.getThreadDefaultBus().getInInterceptors().add(new LoggingInInterceptor());
        BusFactory.getThreadDefaultBus().getInInterceptors().add(new MalformedResponseInterceptor());
    }
    
    private void waitForFuture(Future<?> fd) throws Exception {
        int count = 0;
        while (!fd.isDone()) {
            ++count;
            if (count > 500) {
                fail("Did not finish in 10 seconds");
            }
            Thread.sleep(20);
        }
    }
    
   
    @Test
    public void testSOAPMessageWithMalformedResponse() throws Exception {

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, SERVICE_NAME);
        assertNotNull(service);

        Dispatch<SOAPMessage> disp = service
            .createDispatch(PORT_NAME, SOAPMessage.class, Service.Mode.MESSAGE);
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:" 
                                     + greeterPort
                                     + "/SOAPDispatchService/SoapDispatchPort");
        
        

        // Test async callback
        InputStream is3 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq3.xml");
        SOAPMessage soapReqMsg3 = MessageFactory.newInstance().createMessage(null, is3);
        assertNotNull(soapReqMsg3);
        TestSOAPMessageHandler tsmh = new TestSOAPMessageHandler();
        Future<?> f = disp.invokeAsync(soapReqMsg3, tsmh);
        assertNotNull(f);
        waitForFuture(f);
        assertEquals("AsyncHandler shouldn't get invoked more than once", asyncHandlerInvokedCount, 1);
       
    }
    
   

    class TestSOAPMessageHandler implements AsyncHandler<SOAPMessage> {

        String replyBuffer;

        public void handleResponse(Response<SOAPMessage> response) {
            try {
                asyncHandlerInvokedCount++;
                SOAPMessage reply = response.get();
                replyBuffer = reply.getSOAPBody().getTextContent();
            } catch (Exception e) {
                //
            }
        }

        public String getReplyBuffer() {
            return replyBuffer;
        }
    }

}

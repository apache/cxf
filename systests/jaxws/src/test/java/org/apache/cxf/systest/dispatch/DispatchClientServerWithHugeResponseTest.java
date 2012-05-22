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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Response;
import javax.xml.ws.Service;


import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.hello_world_soap_http.SOAPService;
import org.junit.BeforeClass;
import org.junit.Test;

public class DispatchClientServerWithHugeResponseTest extends AbstractBusClientServerTestBase {

    private static final QName SERVICE_NAME 
        = new QName("http://apache.org/hello_world_soap_http", "SOAPDispatchService");
    private static final QName PORT_NAME 
        = new QName("http://apache.org/hello_world_soap_http", "SoapDispatchPort");

    private static String greeterPort = 
        TestUtil.getPortNumber(DispatchClientServerWithHugeResponseTest.class); 
    
    public static class Server extends AbstractBusTestServerBase {        

        protected void run() {
            Object implementor = new GreeterImpl();
            String address = "http://localhost:"
                + TestUtil.getPortNumber(DispatchClientServerWithHugeResponseTest.class)
                + "/SOAPDispatchService/SoapDispatchPort";
            Endpoint.publish(address, implementor);
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
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }
    
    @org.junit.Before
    public void setUp() {
        StaxUtils.setInnerElementCountThreshold(12);
        StaxUtils.setInnerElementLevelThreshold(12);
        BusFactory.getDefaultBus().getOutInterceptors().add(new LoggingOutInterceptor());
        BusFactory.getDefaultBus().getInInterceptors().add(new LoggingInInterceptor());
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        StaxUtils.setInnerElementCountThreshold(-1);
        StaxUtils.setInnerElementLevelThreshold(-1);
    }   
   
    @Test
    public void testStackOverflowErrorForSOAPMessageWithHugeResponse() throws Exception {
        HugeResponseInterceptor hugeResponseInterceptor = 
            new HugeResponseInterceptor(ResponseInterceptorType.overflow);
        BusFactory.getDefaultBus().getInInterceptors().add(hugeResponseInterceptor);
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
        
        

        InputStream is3 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq3.xml");
        SOAPMessage soapReqMsg3 = MessageFactory.newInstance().createMessage(null, is3);
        assertNotNull(soapReqMsg3);
        Response<SOAPMessage> response = disp.invokeAsync(soapReqMsg3);
        try {
            response.get(300, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            fail("We should not have encountered a timeout, " 
                + "should get some exception tell me stackoverflow");
        } catch (Throwable e) {
            assertTrue(e.getCause() instanceof StackOverflowError);
        } finally {
            BusFactory.getDefaultBus().getInInterceptors().remove(hugeResponseInterceptor);
        }
        
    }
     
    @Test
    public void testThresholdfForSOAPMessageWithHugeResponse() throws Exception {
        HugeResponseInterceptor hugeResponseInterceptor = 
            new HugeResponseInterceptor(ResponseInterceptorType.ElementLevelThreshold);
        BusFactory.getDefaultBus().getInInterceptors().add(hugeResponseInterceptor);
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
        
        

        InputStream is3 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq3.xml");
        SOAPMessage soapReqMsg3 = MessageFactory.newInstance().createMessage(null, is3);
        assertNotNull(soapReqMsg3);
        Response<SOAPMessage> response = disp.invokeAsync(soapReqMsg3);
        try {
            response.get(300, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            fail("We should not have encountered a timeout, " 
                + "should get some exception tell me stackoverflow");
        } catch (Throwable e) {
            assertTrue(e.getCause().getMessage().startsWith("reach the innerElementLevelThreshold"));
        } finally {
            BusFactory.getDefaultBus().getInInterceptors().remove(hugeResponseInterceptor);
        }
        
    }

    @Test
    public void testElementCountThresholdfForSOAPMessageWithHugeResponse() throws Throwable {
        HugeResponseInterceptor hugeResponseInterceptor = 
            new HugeResponseInterceptor(ResponseInterceptorType.ElementCountThreshold);
        BusFactory.getDefaultBus().getInInterceptors().add(hugeResponseInterceptor);
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
        
        

        InputStream is3 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq3.xml");
        SOAPMessage soapReqMsg3 = MessageFactory.newInstance().createMessage(null, is3);
        assertNotNull(soapReqMsg3);
        Response<SOAPMessage> response = disp.invokeAsync(soapReqMsg3);
        try {
            response.get(300, TimeUnit.SECONDS);
            fail("should catch exception");
        } catch (TimeoutException te) {
            fail("We should not have encountered a timeout, " 
                + "should get some exception tell me stackoverflow");
        } catch (Throwable e) {
            if (e.getCause() == null) {
                throw e;
            }
            if (e.getCause().getMessage() == null) {
                throw e;
            }
            assertTrue(e.getCause().getMessage().startsWith("reach the innerElementCountThreshold"));
        } finally {
            BusFactory.getDefaultBus().getInInterceptors().remove(hugeResponseInterceptor);
        }
        
    }
}

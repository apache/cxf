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

package org.apache.cxf.systests.forked.dispatch;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.namespace.QName;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Response;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.hello_world_soap_http.SOAPService;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DispatchClientServerWithHugeResponseTest extends AbstractBusClientServerTestBase {

    private static final QName SERVICE_NAME
        = new QName("http://apache.org/hello_world_soap_http", "SOAPDispatchService");
    private static final QName PORT_NAME
        = new QName("http://apache.org/hello_world_soap_http", "SoapDispatchPort");

    private static String greeterPort =
        TestUtil.getPortNumber(DispatchClientServerWithHugeResponseTest.class);

    public static class Server extends AbstractBusTestServerBase {
        Endpoint ep;

        protected void run() {
            Object implementor = new GreeterImpl();
            String address = "http://localhost:"
                + TestUtil.getPortNumber(DispatchClientServerWithHugeResponseTest.class)
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
        System.setProperty("org.apache.cxf.staxutils.innerElementLevelThreshold", "12");
        // Just enough so that we can read the WSDL but fail the response
        System.setProperty("org.apache.cxf.staxutils.innerElementCountThreshold", "35");

        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @org.junit.Before
    public void setUp() throws Exception {
        createBus();
        getBus().getOutInterceptors().add(new LoggingOutInterceptor());
        getBus().getInInterceptors().add(new LoggingInInterceptor());
    }

    @org.junit.AfterClass
    public static void cleanUp() throws Exception {
        System.clearProperty("org.apache.cxf.staxutils.innerElementLevelThreshold");
        System.clearProperty("org.apache.cxf.staxutils.innerElementCountThreshold");
    }

    @Test
    public void testStackOverflowErrorForSOAPMessageWithHugeResponse() throws Exception {
        HugeResponseInterceptor hugeResponseInterceptor =
            new HugeResponseInterceptor(ResponseInterceptorType.overflow);
        getBus().getInInterceptors().add(hugeResponseInterceptor);
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


        InputStream is3 = getClass().getResourceAsStream("GreetMeDocLiteralReq3.xml");
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
            assertTrue(e.getCause() instanceof StackOverflowError);
        } finally {
            getBus().getInInterceptors().remove(hugeResponseInterceptor);
        }

    }

    @Test
    public void testThresholdfForSOAPMessageWithHugeResponse() throws Exception {
        HugeResponseInterceptor hugeResponseInterceptor =
            new HugeResponseInterceptor(ResponseInterceptorType.ElementLevelThreshold);
        getBus().getInInterceptors().add(hugeResponseInterceptor);
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


        InputStream is3 = getClass().getResourceAsStream("GreetMeDocLiteralReq3.xml");
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
            Throwable t = e.getCause();
            if (t instanceof SOAPFaultException) {
                SoapFault sf = (SoapFault)t.getCause();
                if (sf.getCause() == null) {
                    throw e;
                }
                t = sf.getCause();
            }
            if (t.getMessage() == null) {
                throw e;
            }

            String msg = t.getMessage();
            assertTrue(msg, msg.startsWith("reach the innerElementLevelThreshold")
                       || msg.contains("Maximum Element Depth limit"));
        } finally {
            getBus().getInInterceptors().remove(hugeResponseInterceptor);
        }

    }

    @Test
    public void testElementCountThresholdfForSOAPMessageWithHugeResponse() throws Throwable {
        HugeResponseInterceptor hugeResponseInterceptor =
            new HugeResponseInterceptor(ResponseInterceptorType.ElementCountThreshold);
        getBus().getInInterceptors().add(hugeResponseInterceptor);
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

        InputStream is3 = getClass().getResourceAsStream("GreetMeDocLiteralReq3.xml");
        SOAPMessage soapReqMsg3 = MessageFactory.newInstance().createMessage(null, is3);
        assertNotNull(soapReqMsg3);
        Response<SOAPMessage> response = disp.invokeAsync(soapReqMsg3);
        try {
            response.get(300, TimeUnit.SECONDS);
            fail("should catch exception");
        } catch (TimeoutException te) {
            fail("We should not have encountered a timeout, "
                + "should get some exception tell me stackoverflow");
        } catch (ExecutionException e) {
            if (e.getCause() == null) {
                throw e;
            }
            Throwable t = e.getCause();
            if (t instanceof SOAPFaultException) {
                SoapFault sf = (SoapFault)t.getCause();
                if (sf.getCause() == null) {
                    throw e;
                }
                t = sf.getCause();
            }
            if (t.getMessage() == null) {
                throw e;
            }

            String msg = t.getMessage();
            assertTrue(msg, msg.startsWith("reach the innerElementCountThreshold")
                       || msg.contains("Maximum Number of Child Elements"));
        } finally {
            getBus().getInInterceptors().remove(hugeResponseInterceptor);
        }

    }
}

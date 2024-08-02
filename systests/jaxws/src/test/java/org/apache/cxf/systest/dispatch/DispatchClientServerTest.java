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
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;

import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Response;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.soap.Addressing;
import jakarta.xml.ws.soap.AddressingFeature;
import jakarta.xml.ws.soap.SOAPBinding;
import jakarta.xml.ws.soap.SOAPFaultException;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import jakarta.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.BaseGreeterImpl;
import org.apache.hello_world_soap_http.SOAPService;
import org.apache.hello_world_soap_http.types.GreetMe;
import org.apache.hello_world_soap_http.types.GreetMeLater;
import org.apache.hello_world_soap_http.types.GreetMeResponse;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DispatchClientServerTest extends AbstractBusClientServerTestBase {

    private static final QName SERVICE_NAME
        = new QName("http://apache.org/hello_world_soap_http", "SOAPDispatchService");
    private static final QName PORT_NAME
        = new QName("http://apache.org/hello_world_soap_http", "SoapDispatchPort");

    private static String greeterPort = TestUtil.getPortNumber(DispatchClientServerTest.class);


    @WebService(serviceName = "SOAPService",
        portName = "SoapPort",
        endpointInterface = "org.apache.hello_world_soap_http.Greeter",
        targetNamespace = "http://apache.org/hello_world_soap_http",
        wsdlLocation = "testutils/hello_world.wsdl")
    @Addressing
    public static class GreeterImpl extends BaseGreeterImpl {
    }

    public static class Server extends AbstractBusTestServerBase {
        Endpoint ep;

        protected void run() {
            setBus(BusFactory.getDefaultBus());
            Object implementor = new GreeterImpl();
            String address = "http://localhost:"
                + TestUtil.getPortNumber(DispatchClientServerTest.class)
                + "/SOAPDispatchService/SoapDispatchPort";
            ep = Endpoint.create(SOAPBinding.SOAP11HTTP_BINDING, implementor);
            Map<String, Object> properties = new HashMap<>();
            Map<String, String> nsMap = new HashMap<>();
            nsMap.put("gmns", "http://apache.org/hello_world_soap_http/types");
            properties.put("soap.env.ns.map", nsMap);
            properties.put("disable.outputstream.optimization", "true");

            ep.setProperties(properties);
            ep.publish(address);
            BusFactory.setDefaultBus(null);
            BusFactory.setThreadDefaultBus(null);
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
        createStaticBus();
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
    public void testTimeout() throws Exception {
        //CXF-2384
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        //pick one of the other service/ports that would have an address
        //without a service running
        QName otherServiceName = new QName("http://apache.org/hello_world_soap_http",
                "SOAPProviderService");
        QName otherPortName = new QName("http://apache.org/hello_world_soap_http", "SoapProviderPort");


        SOAPService service = new SOAPService(wsdl, otherServiceName);
        assertNotNull(service);

        Dispatch<SOAPMessage> disp = service
            .createDispatch(otherPortName, SOAPMessage.class, Service.Mode.MESSAGE);
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:"
                                     + TestUtil.getPortNumber("fake-port")
                                     + "/SOAPDispatchService/SoapDispatchPort");

        DispatchImpl<?> dispImpl = (DispatchImpl<?>)disp;
        HTTPConduit cond = (HTTPConduit)dispImpl.getClient().getConduit();
        cond.getClient().setConnectionTimeout(500);

        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        SOAPMessage soapReqMsg = MessageFactory.newInstance().createMessage(null, is);
        assertNotNull(soapReqMsg);

        try {
            disp.invoke(soapReqMsg);
            fail("Should have faulted");
        } catch (SOAPFaultException ex) {
            fail("should not be a SOAPFaultException");
        } catch (WebServiceException ex) {
            //expected
            assertTrue(ex.getCause().getClass().getName(),
                       ex.getCause() instanceof java.net.ConnectException
                       || ex.getCause() instanceof java.net.SocketTimeoutException
                       || ex.getCause()  instanceof HttpConnectTimeoutException);
        }
        
        try {            
            //create a really big message to make sure the write gets called
            Iterator<jakarta.xml.soap.Node> nodes = soapReqMsg.getSOAPBody().getChildElements();
            while (nodes.hasNext()) {
                jakarta.xml.soap.Node nd = nodes.next();
                if (nd instanceof SOAPElement) {
                    SOAPElement se = (SOAPElement)nd;
                    for (int x = 0; x < 100; x++) {
                        se.addTextNode("TestSoapMessageTestSoapMessageTestSoapMessageTestSoapMessage");
                    }
                }
            }            
            
            disp.invoke(soapReqMsg);
            fail("Should have faulted");
        } catch (SOAPFaultException ex) {
            fail("should not be a SOAPFaultException");
        } catch (WebServiceException ex) {
            //expected
            assertTrue(ex.getCause().getClass().getName(),
                       ex.getCause() instanceof java.net.ConnectException
                       || ex.getCause() instanceof java.net.SocketTimeoutException
                       || ex.getCause()  instanceof HttpConnectTimeoutException);
        }
        
        dispImpl.close();

    }
    @Test
    public void test404() throws Exception {
        //CXF-2384
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        //pick one of the other service/ports that would have an address
        //without a service running
        QName otherServiceName = new QName("http://apache.org/hello_world_soap_http",
                "SOAPProviderService");
        QName otherPortName = new QName("http://apache.org/hello_world_soap_http", "SoapProviderPort");


        SOAPService service = new SOAPService(wsdl, otherServiceName);
        assertNotNull(service);

        Dispatch<SOAPMessage> disp = service
            .createDispatch(otherPortName, SOAPMessage.class, Service.Mode.MESSAGE);
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:"
                                     + greeterPort
                                     + "/SomePlaceWithNoServiceRunning/SoapDispatchPort");

        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        SOAPMessage soapReqMsg = MessageFactory.newInstance().createMessage(null, is);
        assertNotNull(soapReqMsg);

        try {
            disp.invoke(soapReqMsg);
            fail("Should have faulted");
        } catch (SOAPFaultException ex) {
            fail("should not be a SOAPFaultException");
        } catch (WebServiceException ex) {
            //expected
            assertTrue(ex.getCause().getClass().getName(),
                       ex.getCause() instanceof java.io.IOException);
        }

    }

    @Test
    public void testSOAPMessageInvokeToOneWay() throws Exception {
        SOAPService service = new SOAPService(null, SERVICE_NAME);
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING,
                        "http://localhost:" + greeterPort
                        + "/SOAPDispatchService/SoapDispatchPort");
        assertNotNull(service);

        Dispatch<SOAPMessage> disp = service
            .createDispatch(PORT_NAME, SOAPMessage.class, Service.Mode.MESSAGE);

        //message is "one way", but there really isn't a way for us to know that
        //as we don't have a wsdl or other source of operation information to
        //compare the payload to.
        InputStream is1 = getClass().getResourceAsStream("resources/GreetMe1WDocLiteralReq2.xml");
        SOAPMessage soapReqMsg1 = MessageFactory.newInstance().createMessage(null, is1);
        assertNotNull(soapReqMsg1);

        //Version 1:
        //we'll just call invoke
        disp.invoke(soapReqMsg1);

        //Version 2:
        //We want to handle things asynchronously
        AsyncHandler<SOAPMessage> callback = new AsyncHandler<SOAPMessage>() {
            public void handleResponse(Response<SOAPMessage> res) {
                synchronized (this) {
                    notifyAll();
                }
            }
        };
        synchronized (callback) {
            disp.invokeAsync(soapReqMsg1, callback);
            callback.wait();
        }
    }
    @Test
    public void testSOAPMessage() throws Exception {

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

        // Test request-response
        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        SOAPMessage soapReqMsg = MessageFactory.newInstance().createMessage(null, is);
        assertNotNull(soapReqMsg);
        SOAPMessage soapResMsg = disp.invoke(soapReqMsg);

        assertNotNull(soapResMsg);
        String expected = "Hello TestSOAPInputMessage";
        assertEquals("Response should be : Hello TestSOAPInputMessage", expected,
                     DOMUtils.getContent(SAAJUtils.getBody(soapResMsg)
                                             .getFirstChild().getFirstChild()).trim());

        // Test oneway
        InputStream is1 = getClass().getResourceAsStream("resources/GreetMe1WDocLiteralReq2.xml");
        SOAPMessage soapReqMsg1 = MessageFactory.newInstance().createMessage(null, is1);
        assertNotNull(soapReqMsg1);
        disp.invokeOneWay(soapReqMsg1);

        // Test async polling
        InputStream is2 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq2.xml");
        SOAPMessage soapReqMsg2 = MessageFactory.newInstance().createMessage(null, is2);
        assertNotNull(soapReqMsg2);

        Response<?> response = disp.invokeAsync(soapReqMsg2);
        SOAPMessage soapResMsg2 = (SOAPMessage)response.get();
        assertNotNull(soapResMsg2);
        String expected2 = "Hello TestSOAPInputMessage2";
        assertEquals("Response should be : Hello TestSOAPInputMessage2", expected2,
                     DOMUtils.getContent(SAAJUtils.getBody(soapResMsg2).getFirstChild().getFirstChild()));

        // Test async callback
        InputStream is3 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq3.xml");
        SOAPMessage soapReqMsg3 = MessageFactory.newInstance().createMessage(null, is3);
        assertNotNull(soapReqMsg3);
        TestSOAPMessageHandler tsmh = new TestSOAPMessageHandler();
        Future<?> f = disp.invokeAsync(soapReqMsg3, tsmh);
        assertNotNull(f);
        waitForFuture(f);

        String expected3 = "Hello TestSOAPInputMessage3";
        assertEquals("Response should be : Hello TestSOAPInputMessage3",
                     expected3, tsmh.getReplyBuffer().trim());

    }

    @Test
    public void testCreateDispatchWithEPR() throws Exception {

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, SERVICE_NAME);
        assertNotNull(service);

        W3CEndpointReferenceBuilder builder = new  W3CEndpointReferenceBuilder();
        builder.address("http://localhost:"
                        + greeterPort
                        + "/SOAPDispatchService/SoapDispatchPort");
        builder.serviceName(SERVICE_NAME);
        builder.endpointName(PORT_NAME);
        W3CEndpointReference w3cEpr = builder.build();
        Dispatch<SOAPMessage> disp = service
            .createDispatch(w3cEpr, SOAPMessage.class, Service.Mode.MESSAGE);
        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        SOAPMessage soapReqMsg = MessageFactory.newInstance().createMessage(null, is);
        assertNotNull(soapReqMsg);
        SOAPMessage soapResMsg = disp.invoke(soapReqMsg);

        assertNotNull(soapResMsg);
        String expected = "Hello TestSOAPInputMessage";
        assertEquals("Response should be : Hello TestSOAPInputMessage", expected,
                     DOMUtils.getAllContent(SAAJUtils.getBody(soapResMsg)).trim());
    }

    @Test
    public void testDOMSourceMESSAGE() throws Exception {
        /*URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);*/
        Service service = Service.create(SERVICE_NAME);
        assertNotNull(service);
        service.addPort(PORT_NAME, "http://schemas.xmlsoap.org/soap/",
                        "http://localhost:"
                        + greeterPort
                        + "/SOAPDispatchService/SoapDispatchPort");

        Dispatch<DOMSource> disp = service.createDispatch(PORT_NAME, DOMSource.class, Service.Mode.MESSAGE);

        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        SOAPMessage soapReqMsg = MessageFactory.newInstance().createMessage(null, is);
        DOMSource domReqMsg = new DOMSource(soapReqMsg.getSOAPPart());
        assertNotNull(domReqMsg);


        DOMSource domResMsg = disp.invoke(domReqMsg);
        assertNotNull(domResMsg);
        String expected = "Hello TestSOAPInputMessage";

        assertEquals("Response should be : Hello TestSOAPInputMessage", expected,
                     DOMUtils.getAllContent(domResMsg.getNode().getFirstChild()).trim());

        Element el = (Element)domResMsg.getNode().getFirstChild();
        assertEquals("gmns", el.lookupPrefix("http://apache.org/hello_world_soap_http/types"));
        assertEquals("http://apache.org/hello_world_soap_http/types",
                     el.lookupNamespaceURI("gmns"));

        // Test invoke oneway
        InputStream is1 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq1.xml");
        SOAPMessage soapReqMsg1 = MessageFactory.newInstance().createMessage(null, is1);
        DOMSource domReqMsg1 = new DOMSource(soapReqMsg1.getSOAPPart());
        assertNotNull(domReqMsg1);
        disp.invokeOneWay(domReqMsg1);

        // Test async polling
        InputStream is2 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq2.xml");
        SOAPMessage soapReqMsg2 = MessageFactory.newInstance().createMessage(null, is2);
        DOMSource domReqMsg2 = new DOMSource(soapReqMsg2.getSOAPPart());
        assertNotNull(domReqMsg2);

        Response<DOMSource> response = disp.invokeAsync(domReqMsg2);
        DOMSource domRespMsg2 = response.get();
        assertNotNull(domReqMsg2);
        String expected2 = "Hello TestSOAPInputMessage2";
        assertEquals("Response should be : Hello TestSOAPInputMessage2", expected2, domRespMsg2.getNode()
            .getFirstChild().getTextContent().trim());

        // Test async callback
        InputStream is3 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq3.xml");
        SOAPMessage soapReqMsg3 = MessageFactory.newInstance().createMessage(null, is3);
        DOMSource domReqMsg3 = new DOMSource(soapReqMsg3.getSOAPPart());
        assertNotNull(domReqMsg3);

        TestDOMSourceHandler tdsh = new TestDOMSourceHandler();
        Future<?> fd = disp.invokeAsync(domReqMsg3, tdsh);
        assertNotNull(fd);
        waitForFuture(fd);
        String expected3 = "Hello TestSOAPInputMessage3";
        assertEquals("Response should be : Hello TestSOAPInputMessage3", expected3,
                     tdsh.getReplyBuffer().trim());
    }

    @Test
    public void testDOMSourcePAYLOAD() throws Exception {

        /*URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);*/
        Service service = Service.create(SERVICE_NAME);
        assertNotNull(service);
        service.addPort(PORT_NAME, "http://schemas.xmlsoap.org/soap/",
                        "http://localhost:"
                        + greeterPort + "/SOAPDispatchService/SoapDispatchPort");

        Dispatch<DOMSource> disp = service.createDispatch(PORT_NAME, DOMSource.class, Service.Mode.PAYLOAD);

        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        SOAPMessage soapReqMsg = MessageFactory.newInstance().createMessage(null, is);
        DOMSource domReqMsg = new DOMSource(soapReqMsg.getSOAPBody().extractContentAsDocument());
        assertNotNull(domReqMsg);

        // invoke
        DOMSource domResMsg = disp.invoke(domReqMsg);

        assertNotNull(domResMsg);
        String expected = "Hello TestSOAPInputMessage";

        Node node = domResMsg.getNode();
        assertNotNull(node);
        if (node instanceof Document) {
            node = ((Document)node).getDocumentElement();
        }
        String content = node.getTextContent();
        assertNotNull(content);
        assertEquals("Response should be : Hello TestSOAPInputMessage", expected, content.trim());

        InputStream is1 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq1.xml");
        SOAPMessage soapReqMsg1 = MessageFactory.newInstance().createMessage(null, is1);
        DOMSource domReqMsg1 = new DOMSource(soapReqMsg1.getSOAPBody().extractContentAsDocument());
        assertNotNull(domReqMsg1);
        // invokeOneWay
        disp.invokeOneWay(domReqMsg1);

        InputStream is2 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq2.xml");
        SOAPMessage soapReqMsg2 = MessageFactory.newInstance().createMessage(null, is2);
        DOMSource domReqMsg2 = new DOMSource(soapReqMsg2.getSOAPBody().extractContentAsDocument());
        assertNotNull(domReqMsg2);
        // invokeAsync
        Response<DOMSource> response = disp.invokeAsync(domReqMsg2);
        DOMSource domRespMsg2 = response.get();
        assertNotNull(domRespMsg2);
        String expected2 = "Hello TestSOAPInputMessage2";

        node = domRespMsg2.getNode();
        assertNotNull(node);
        if (node instanceof Document) {
            node = ((Document)node).getDocumentElement();
        }
        content = node.getTextContent();
        assertNotNull(content);

        assertEquals("Response should be : Hello TestSOAPInputMessage2", expected2, content.trim());

        InputStream is3 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq3.xml");
        SOAPMessage soapReqMsg3 = MessageFactory.newInstance().createMessage(null, is3);
        DOMSource domReqMsg3 = new DOMSource(soapReqMsg3.getSOAPBody().extractContentAsDocument());
        assertNotNull(domReqMsg3);
        // invokeAsync with AsyncHandler
        TestDOMSourceHandler tdsh = new TestDOMSourceHandler();
        Future<?> fd = disp.invokeAsync(domReqMsg3, tdsh);
        assertNotNull(fd);
        waitForFuture(fd);
        String expected3 = "Hello TestSOAPInputMessage3";
        assertEquals("Response should be : Hello TestSOAPInputMessage3",
                     expected3, tdsh.getReplyBuffer().trim());
    }

    @Test
    public void testJAXBObjectPAYLOAD() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, SERVICE_NAME);
        assertNotNull(service);

        JAXBContext jc = JAXBContext.newInstance("org.apache.hello_world_soap_http.types");
        Dispatch<Object> disp = service.createDispatch(PORT_NAME, jc, Service.Mode.PAYLOAD);
        doJAXBPayload(disp);
    }
    @Test
    public void testJAXBObjectPAYLOADFromEPR() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, SERVICE_NAME);
        assertNotNull(service);

        W3CEndpointReferenceBuilder builder = new  W3CEndpointReferenceBuilder();
        builder.address("http://localhost:"
                        + greeterPort
                        + "/SOAPDispatchService/SoapDispatchPort");
        builder.serviceName(SERVICE_NAME);
        builder.endpointName(PORT_NAME);
        W3CEndpointReference w3cEpr = builder.build();
        JAXBContext jc = JAXBContext.newInstance("org.apache.hello_world_soap_http.types");
        Dispatch<Object> disp = service.createDispatch(w3cEpr, jc, Service.Mode.PAYLOAD, new AddressingFeature());
        doJAXBPayload(disp);
    }
    private void doJAXBPayload(Dispatch<Object> disp) throws Exception {
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:"
                                     + greeterPort
                                     + "/SOAPDispatchService/SoapDispatchPort");

        String expected = "Hello Jeeves";
        GreetMe greetMe = new GreetMe();
        greetMe.setRequestType("Jeeves");

        Object response = disp.invoke(greetMe);
        assertNotNull(response);
        String responseValue = ((GreetMeResponse)response).getResponseType();
        assertEquals("Expected string, " + expected, expected, responseValue);

        // Test oneway
        disp.invokeOneWay(greetMe);

        // Test async polling
        Response<Object> response2 = disp.invokeAsync(greetMe);
        assertNotNull(response2);
        GreetMeResponse greetMeResponse = (GreetMeResponse)response2.get();
        String responseValue2 = greetMeResponse.getResponseType();
        assertEquals("Expected string, " + expected, expected, responseValue2);

        // Test async callback
        TestJAXBHandler tjbh = new TestJAXBHandler();
        Future<?> fd = disp.invokeAsync(greetMe, tjbh);
        assertNotNull(fd);
        waitForFuture(fd);

        String responseValue3 = ((GreetMeResponse)tjbh.getResponse()).getResponseType();
        assertEquals("Expected string, " + expected, expected, responseValue3);

        org.apache.hello_world_soap_http.types.TestDocLitFault fr =
            new org.apache.hello_world_soap_http.types.TestDocLitFault();
        fr.setFaultType(BadRecordLitFault.class.getSimpleName());

        tjbh = new TestJAXBHandler();
        fd = disp.invokeAsync(fr, tjbh);
        waitForFuture(fd);
        try {
            fd.get();
            fail("did not get expected exception");
        } catch (ExecutionException ex) {
            //expected
        }

        GreetMeLater later = new GreetMeLater();
        later.setRequestType(1000);

        HTTPClientPolicy pol = new HTTPClientPolicy();
        pol.setReceiveTimeout(100);
        disp.getRequestContext().put(HTTPClientPolicy.class.getName(), pol);
        Response<Object> o = disp.invokeAsync(later);
        try {
            Object o2 = o.get(10, TimeUnit.SECONDS);
            fail("Should have gotten a SocketTimeoutException: " + o2);
        } catch (TimeoutException tex) {
            // this is ok
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof SocketTimeoutException
                       || ex.getCause() instanceof HttpTimeoutException);
        }

        later.setRequestType(20000);
        pol.setReceiveTimeout(20000);
        disp.getRequestContext().put(HTTPClientPolicy.class.getName(), pol);
        o = disp.invokeAsync(later);
        try {
            o.get(100, TimeUnit.MILLISECONDS);
            fail("Should have gotten a SocketTimeoutException");
        } catch (TimeoutException ex) {
            //ignore - expected
        }

    }

    @Test
    public void testJAXBObjectPAYLOADWithFeature() throws Exception {
        // Save initial counts
        int initialCount = TestDispatchFeature.getCount();
        int initialInInterceptorCount = TestDispatchFeature.getInInterceptorCount();
        int initialOutInterceptorCount = TestDispatchFeature.getOutInterceptorCount();

        createBus("org/apache/cxf/systest/dispatch/client-config.xml");
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        String bindingId = "http://schemas.xmlsoap.org/wsdl/soap/";
        String endpointUrl = "http://localhost:" + greeterPort + "/SOAPDispatchService/SoapDispatchPort";

        Service service = Service.create(wsdl, SERVICE_NAME);
        service.addPort(PORT_NAME, bindingId, endpointUrl);
        assertNotNull(service);

        JAXBContext jc = JAXBContext.newInstance("org.apache.hello_world_soap_http.types");
        Dispatch<Object> disp = service.createDispatch(PORT_NAME, jc, Service.Mode.PAYLOAD);
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:"
                                     + greeterPort
                                     + "/SOAPDispatchService/SoapDispatchPort");

        String expected = "Hello Jeeves";
        GreetMe greetMe = new GreetMe();
        greetMe.setRequestType("Jeeves");

        Object response = disp.invoke(greetMe);
        assertNotNull(response);
        String responseValue = ((GreetMeResponse)response).getResponseType();
        assertEquals("Expected string, " + expected, expected, responseValue);

        assertEquals("Feature should be applied", initialCount + 1, TestDispatchFeature.getCount());
        assertEquals("Feature based interceptors should be added",
                    initialCount + 1, TestDispatchFeature.getCount());
        assertEquals("Feature based In interceptors has be added to in chain.",
                    initialInInterceptorCount + 1, TestDispatchFeature.getInInterceptorCount());
        assertEquals("Feature based interceptors has to be added to out chain.",
                    initialOutInterceptorCount + 1, TestDispatchFeature.getOutInterceptorCount());
        bus.shutdown(true);
    }

    @Test
    public void testSAXSourceMESSAGE() throws Exception {

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, SERVICE_NAME);
        assertNotNull(service);

        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        InputSource inputSource = new InputSource(is);
        SAXSource saxSourceReq = new SAXSource(inputSource);
        assertNotNull(saxSourceReq);

        Dispatch<SAXSource> disp = service.createDispatch(PORT_NAME, SAXSource.class, Service.Mode.MESSAGE);
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:"
                                     + greeterPort
                                     + "/SOAPDispatchService/SoapDispatchPort");
        SAXSource saxSourceResp = disp.invoke(saxSourceReq);
        assertNotNull(saxSourceResp);
        String expected = "Hello TestSOAPInputMessage";
        assertTrue("Expected: " + expected, StaxUtils.toString(saxSourceResp).contains(expected));

        // Test oneway
        InputStream is1 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq1.xml");
        InputSource inputSource1 = new InputSource(is1);
        SAXSource saxSourceReq1 = new SAXSource(inputSource1);
        assertNotNull(saxSourceReq1);
        disp.invokeOneWay(saxSourceReq1);

        // Test async polling
        InputStream is2 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq2.xml");
        InputSource inputSource2 = new InputSource(is2);
        SAXSource saxSourceReq2 = new SAXSource(inputSource2);
        assertNotNull(saxSourceReq2);

        Response<SAXSource> response = disp.invokeAsync(saxSourceReq2);
        SAXSource saxSourceResp2 = response.get();
        assertNotNull(saxSourceResp2);
        String expected2 = "Hello TestSOAPInputMessage2";
        assertTrue("Expected: " + expected, StaxUtils.toString(saxSourceResp2).contains(expected2));

        // Test async callback
        InputStream is3 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq3.xml");
        InputSource inputSource3 = new InputSource(is3);
        SAXSource saxSourceReq3 = new SAXSource(inputSource3);
        assertNotNull(saxSourceReq3);
        TestSAXSourceHandler tssh = new TestSAXSourceHandler();
        Future<?> fd = disp.invokeAsync(saxSourceReq3, tssh);
        assertNotNull(fd);
        waitForFuture(fd);

        String expected3 = "Hello TestSOAPInputMessage3";
        SAXSource saxSourceResp3 = tssh.getSAXSource();
        assertNotNull(saxSourceResp3);
        assertTrue("Expected: " + expected, StaxUtils.toString(saxSourceResp3).contains(expected3));
    }

    @Test
    public void testSAXSourceMESSAGEWithSchemaValidation() throws Exception {

        URL wsdl = getClass().getResource("/org/apache/cxf/systest/provider/hello_world_with_restriction.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, SERVICE_NAME);
        assertNotNull(service);

        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        InputSource inputSource = new InputSource(is);
        SAXSource saxSourceReq = new SAXSource(inputSource);
        assertNotNull(saxSourceReq);

        Dispatch<SAXSource> disp = service.createDispatch(PORT_NAME, SAXSource.class, Service.Mode.MESSAGE);
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:"
                                     + greeterPort
                                     + "/SOAPDispatchService/SoapDispatchPort");
        disp.getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE);
        SAXSource saxSourceResp = disp.invoke(saxSourceReq);
        assertNotNull(saxSourceResp);
        String expected = "Hello TestSOAPInputMessage";
        assertTrue("Expected: " + expected, StaxUtils.toString(saxSourceResp).contains(expected));

        is = getClass().getResourceAsStream("resources/GreetMeDocLiteralReqWithExceedMaxLength.xml");
        inputSource = new InputSource(is);
        saxSourceReq = new SAXSource(inputSource);
        assertNotNull(saxSourceReq);

        try {
            disp.invoke(saxSourceReq);
            fail("Should have thrown an exception");
        } catch (Exception ex) {
            // expected
            assertTrue(ex.getMessage().contains("cvc-maxLength-valid"));
        }
    }

    @Test
    public void testSAXSourcePAYLOAD() throws Exception {

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, SERVICE_NAME);
        assertNotNull(service);

        Dispatch<SAXSource> disp = service.createDispatch(PORT_NAME, SAXSource.class, Service.Mode.PAYLOAD);
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:"
                                     + greeterPort
                                     + "/SOAPDispatchService/SoapDispatchPort");

        // Test request-response
        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralSOAPBodyReq.xml");
        InputSource inputSource = new InputSource(is);
        inputSource.setPublicId(getClass()
                                    .getResource("resources/GreetMeDocLiteralSOAPBodyReq.xml").toString());
        inputSource.setSystemId(inputSource.getPublicId());
        SAXSource saxSourceReq = new SAXSource(inputSource);
        assertNotNull(saxSourceReq);
        SAXSource saxSourceResp = disp.invoke(saxSourceReq);
        assertNotNull(saxSourceResp);
        String expected = "Hello TestSOAPInputMessage";
        assertTrue("Expected: " + expected, StaxUtils.toString(saxSourceResp).contains(expected));

        // Test oneway
        InputStream is1 = getClass().getResourceAsStream("resources/GreetMeDocLiteralSOAPBodyReq1.xml");
        InputSource inputSource1 = new InputSource(is1);
        inputSource1.setPublicId(getClass()
                                 .getResource("resources/GreetMeDocLiteralSOAPBodyReq1.xml").toString());
        inputSource1.setSystemId(inputSource1.getPublicId());
        SAXSource saxSourceReq1 = new SAXSource(inputSource1);
        assertNotNull(saxSourceReq1);
        disp.invokeOneWay(saxSourceReq1);

        // Test async polling
        InputStream is2 = getClass().getResourceAsStream("resources/GreetMeDocLiteralSOAPBodyReq2.xml");
        InputSource inputSource2 = new InputSource(is2);
        inputSource2.setPublicId(getClass()
                                 .getResource("resources/GreetMeDocLiteralSOAPBodyReq2.xml").toString());
        inputSource2.setSystemId(inputSource2.getPublicId());
        SAXSource saxSourceReq2 = new SAXSource(inputSource2);
        assertNotNull(saxSourceReq2);
        Response<SAXSource> response = disp.invokeAsync(saxSourceReq2);
        SAXSource saxSourceResp2 = response.get();
        assertNotNull(saxSourceResp2);
        String expected2 = "Hello TestSOAPInputMessage2";
        assertTrue("Expected: " + expected, StaxUtils.toString(saxSourceResp2).contains(expected2));

        // Test async callback
        InputStream is3 = getClass().getResourceAsStream("resources/GreetMeDocLiteralSOAPBodyReq3.xml");
        InputSource inputSource3 = new InputSource(is3);
        inputSource3.setPublicId(getClass()
                                 .getResource("resources/GreetMeDocLiteralSOAPBodyReq3.xml").toString());
        inputSource3.setSystemId(inputSource3.getPublicId());
        SAXSource saxSourceReq3 = new SAXSource(inputSource3);
        assertNotNull(saxSourceReq3);

        TestSAXSourceHandler tssh = new TestSAXSourceHandler();
        Future<?> fd = disp.invokeAsync(saxSourceReq3, tssh);
        assertNotNull(fd);
        waitForFuture(fd);

        String expected3 = "Hello TestSOAPInputMessage3";
        SAXSource saxSourceResp3 = tssh.getSAXSource();
        assertNotNull(saxSourceResp3);
        assertTrue("Expected: " + expected, StaxUtils.toString(saxSourceResp3).contains(expected3));
    }

    @Test
    public void testStreamSourceMESSAGE() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, SERVICE_NAME);
        assertNotNull(service);
        Dispatch<StreamSource> disp = service.createDispatch(PORT_NAME, StreamSource.class,
                                                             Service.Mode.MESSAGE);
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:"
                                     + greeterPort
                                     + "/SOAPDispatchService/SoapDispatchPort");

        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        StreamSource streamSourceReq = new StreamSource(is);
        assertNotNull(streamSourceReq);
        StreamSource streamSourceResp = disp.invoke(streamSourceReq);
        assertNotNull(streamSourceResp);
        String expected = "Hello TestSOAPInputMessage";
        assertTrue("Expected: " + expected, StaxUtils.toString(streamSourceResp).contains(expected));

        InputStream is1 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq1.xml");
        StreamSource streamSourceReq1 = new StreamSource(is1);
        assertNotNull(streamSourceReq1);
        disp.invokeOneWay(streamSourceReq1);

        InputStream is2 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq2.xml");
        StreamSource streamSourceReq2 = new StreamSource(is2);
        assertNotNull(streamSourceReq2);
        Response<StreamSource> response = disp.invokeAsync(streamSourceReq2);
        StreamSource streamSourceResp2 = response.get();
        assertNotNull(streamSourceResp2);
        String expected2 = "Hello TestSOAPInputMessage2";
        assertTrue("Expected: " + expected, StaxUtils.toString(streamSourceResp2).contains(expected2));

        InputStream is3 = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq3.xml");
        StreamSource streamSourceReq3 = new StreamSource(is3);
        assertNotNull(streamSourceReq3);
        TestStreamSourceHandler tssh = new TestStreamSourceHandler();
        Future<?> fd = disp.invokeAsync(streamSourceReq3, tssh);
        assertNotNull(fd);
        waitForFuture(fd);

        String expected3 = "Hello TestSOAPInputMessage3";
        StreamSource streamSourceResp3 = tssh.getStreamSource();
        assertNotNull(streamSourceResp3);
        assertTrue("Expected: " + expected, StaxUtils.toString(streamSourceResp3).contains(expected3));
    }

    @Test
    public void testStreamSourcePAYLOAD() throws Exception {

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, SERVICE_NAME);
        assertNotNull(service);
        Dispatch<StreamSource> disp = service.createDispatch(PORT_NAME, StreamSource.class,
                                                             Service.Mode.PAYLOAD);
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:"
                                     + greeterPort
                                     + "/SOAPDispatchService/SoapDispatchPort");

        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralSOAPBodyReq.xml");
        StreamSource streamSourceReq = new StreamSource(is);
        assertNotNull(streamSourceReq);
        StreamSource streamSourceResp = disp.invoke(streamSourceReq);
        assertNotNull(streamSourceResp);
        String expected = "Hello TestSOAPInputMessage";
        assertTrue("Expected: " + expected, StaxUtils.toString(streamSourceResp).contains(expected));

        InputStream is1 = getClass().getResourceAsStream("resources/GreetMeDocLiteralSOAPBodyReq1.xml");
        StreamSource streamSourceReq1 = new StreamSource(is1);
        assertNotNull(streamSourceReq1);
        disp.invokeOneWay(streamSourceReq1);

        // Test async polling
        InputStream is2 = getClass().getResourceAsStream("resources/GreetMeDocLiteralSOAPBodyReq2.xml");
        StreamSource streamSourceReq2 = new StreamSource(is2);
        assertNotNull(streamSourceReq2);
        Response<StreamSource> response = disp.invokeAsync(streamSourceReq2);
        StreamSource streamSourceResp2 = response.get();
        assertNotNull(streamSourceResp2);
        String expected2 = "Hello TestSOAPInputMessage2";
        assertTrue("Expected: " + expected, StaxUtils.toString(streamSourceResp2).contains(expected2));

        // Test async callback
        InputStream is3 = getClass().getResourceAsStream("resources/GreetMeDocLiteralSOAPBodyReq3.xml");
        StreamSource streamSourceReq3 = new StreamSource(is3);
        assertNotNull(streamSourceReq3);

        TestStreamSourceHandler tssh = new TestStreamSourceHandler();
        Future<?> fd = disp.invokeAsync(streamSourceReq3, tssh);
        assertNotNull(fd);
        waitForFuture(fd);

        String expected3 = "Hello TestSOAPInputMessage3";
        StreamSource streamSourceResp3 = tssh.getStreamSource();
        assertNotNull(streamSourceResp3);
        assertTrue("Expected: " + expected, StaxUtils.toString(streamSourceResp3).contains(expected3));
    }


    @Test
    public void testStreamSourcePAYLOADWithSchemaValidation() throws Exception {

        URL wsdl = getClass().getResource("/org/apache/cxf/systest/provider/hello_world_with_restriction.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, SERVICE_NAME);
        assertNotNull(service);
        Dispatch<StreamSource> disp = service.createDispatch(PORT_NAME, StreamSource.class,
                                                             Service.Mode.PAYLOAD);
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:"
                                     + greeterPort
                                     + "/SOAPDispatchService/SoapDispatchPort");
        disp.getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE);
        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralSOAPBodyReqExceedMaxLength.xml");
        StreamSource streamSourceReq = new StreamSource(is);
        assertNotNull(streamSourceReq);
        try {
            disp.invoke(streamSourceReq);
            fail("Should have thrown an exception");
        } catch (Exception ex) {
            // expected
            assertTrue(ex.getMessage().contains("cvc-maxLength-valid"));
        }

        is = getClass().getResourceAsStream("resources/GreetMeDocLiteralSOAPBodyReq.xml");
        streamSourceReq = new StreamSource(is);
        assertNotNull(streamSourceReq);
        StreamSource streamSourceResp = disp.invoke(streamSourceReq);
        assertNotNull(streamSourceResp);
        String expected = "Hello TestSOAPInputMessage";
        assertTrue("Expected: " + expected, StaxUtils.toString(streamSourceResp).contains(expected));
    }

    @Test
    public void testStAXSourcePAYLOAD() throws Exception {

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        Service service = Service.create(wsdl, SERVICE_NAME);
        assertNotNull(service);

        Dispatch<StAXSource> disp = service.createDispatch(PORT_NAME, StAXSource.class, Service.Mode.PAYLOAD);
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:"
                                     + greeterPort
                                     + "/SOAPDispatchService/SoapDispatchPort");
        QName opQName = new QName("http://apache.org/hello_world_soap_http", "greetMe");
        disp.getRequestContext().put(MessageContext.WSDL_OPERATION, opQName);

        // Test request-response
        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralSOAPBodyReq.xml");
        StAXSource staxSourceReq = new StAXSource(StaxUtils.createXMLStreamReader(is));
        assertNotNull(staxSourceReq);
        Source resp = disp.invoke(staxSourceReq);
        assertNotNull(resp);
        assertTrue(resp instanceof StAXSource);
        String expected = "Hello TestSOAPInputMessage";
        String actual = StaxUtils.toString(StaxUtils.read(resp));
        assertTrue("Expected: " + expected, actual.contains(expected));
    }

    class TestSOAPMessageHandler implements AsyncHandler<SOAPMessage> {

        String replyBuffer;

        public void handleResponse(Response<SOAPMessage> response) {
            try {
                SOAPMessage reply = response.get();
                replyBuffer = DOMUtils.getContent(SAAJUtils.getBody(reply).getFirstChild().getFirstChild());
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        public String getReplyBuffer() {
            return replyBuffer;
        }
    }

    // REVISIT: Exception handling?
    class TestDOMSourceHandler implements AsyncHandler<DOMSource> {

        String replyBuffer;

        public void handleResponse(Response<DOMSource> response) {
            try {
                DOMSource reply = response.get();
                replyBuffer = DOMUtils.getChild(reply.getNode(), Node.ELEMENT_NODE).getTextContent();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        public String getReplyBuffer() {
            return replyBuffer;
        }
    }

    // REVISIT: Exception handling?
    class TestJAXBHandler implements AsyncHandler<Object> {

        Object reply;

        public void handleResponse(Response<Object> response) {
            try {
                //response context shouldn't be empty even with fault response message
                assertTrue(response.getContext().size() != 0);
                reply = response.get();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        public Object getResponse() {
            return reply;
        }
    }

    // REVISIT: Exception handling?
    class TestSAXSourceHandler implements AsyncHandler<SAXSource> {

        SAXSource reply;

        public void handleResponse(Response<SAXSource> response) {
            try {
                reply = response.get();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public SAXSource getSAXSource() {
            return reply;
        }
    }

    // REVISIT: Exception handling?
    class TestStreamSourceHandler implements AsyncHandler<StreamSource> {

        StreamSource reply;

        public void handleResponse(Response<StreamSource> response) {
            try {
                reply = response.get();

            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        public StreamSource getStreamSource() {
            return reply;
        }
    }
}

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

package org.apache.cxf.systest.handlers;

import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.activation.DataSource;
import javax.activation.URLDataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.http.HTTPException;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.handlers.AddNumbersService;
import org.apache.handlers.types.AddNumbersResponse;
import org.apache.handlers.types.ObjectFactory;
import org.apache.hello_world_xml_http.wrapped.XMLService;
import org.junit.BeforeClass;
import org.junit.Test;


public class DispatchHandlerInvocationTest extends AbstractBusClientServerTestBase {
    private static String addNumbersAddress
        = "http://localhost:" + TestUtil.getPortNumber(HandlerServer.class, 1)
            + "/handlers/AddNumbersService/AddNumbersPort";
    private static String greeterAddress = "http://localhost:"
            +  TestUtil.getPortNumber(HandlerServer.class, 2) + "/XMLService/XMLDispatchPort";

    private final QName serviceName = new QName("http://apache.org/handlers", "AddNumbersService");
    private final QName portName = new QName("http://apache.org/handlers", "AddNumbersPort");

    private final QName portNameXML = new QName("http://apache.org/hello_world_xml_http/wrapped",
                                                "XMLDispatchPort");
    

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(HandlerServer.class));
    }

    @Test
    public void testInvokeWithJAXBPayloadMode() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/addNumbers.wsdl");
        assertNotNull(wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull(service);

        JAXBContext jc = JAXBContext.newInstance("org.apache.handlers.types");
        Dispatch<Object> disp = service.createDispatch(portName, jc, Service.Mode.PAYLOAD);
        setAddress(disp, addNumbersAddress);
        
        TestHandler handler = new TestHandler();
        TestSOAPHandler soapHandler = new TestSOAPHandler();
        addHandlersProgrammatically(disp, handler, soapHandler);

        org.apache.handlers.types.AddNumbers req = new org.apache.handlers.types.AddNumbers();
        req.setArg0(10);
        req.setArg1(20);
        ObjectFactory factory = new ObjectFactory();
        JAXBElement e = factory.createAddNumbers(req);

        JAXBElement response = (JAXBElement)disp.invoke(e);
        assertNotNull(response);
        AddNumbersResponse value = (AddNumbersResponse)response.getValue();
        assertEquals(222, value.getReturn());
    }
    
    @Test
    public void testInvokeWithJAXBUnwrapPayloadMode() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/addNumbers.wsdl");
        assertNotNull(wsdl);

        org.apache.cxf.systest.handlers.AddNumbersServiceUnwrap service = 
            new org.apache.cxf.systest.handlers.AddNumbersServiceUnwrap(wsdl, serviceName);
        assertNotNull(service);

        JAXBContext jc = JAXBContext.newInstance(
            org.apache.cxf.systest.handlers.types.AddNumbers.class,
            org.apache.cxf.systest.handlers.types.AddNumbersResponse.class);

        Dispatch<Object> disp = service.createDispatch(portName, jc, Service.Mode.PAYLOAD);
        setAddress(disp, addNumbersAddress);
        
        TestHandler handler = new TestHandler();
        TestSOAPHandler soapHandler = new TestSOAPHandler();
        addHandlersProgrammatically(disp, handler, soapHandler);

        org.apache.cxf.systest.handlers.types.AddNumbers req = 
            new org.apache.cxf.systest.handlers.types.AddNumbers();
        req.setArg0(10);
        req.setArg1(20);
        
        org.apache.cxf.systest.handlers.types.AddNumbersResponse response = 
            (org.apache.cxf.systest.handlers.types.AddNumbersResponse)disp.invoke(req);
        assertNotNull(response);
        assertEquals(222, response.getReturn());
    }

    @Test
    public void testInvokeWithDOMSourcMessageMode() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/addNumbers.wsdl");
        assertNotNull(wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull(service);

        Dispatch<DOMSource> disp = service.createDispatch(portName, DOMSource.class, Mode.MESSAGE);
        setAddress(disp, addNumbersAddress);

        TestHandler handler = new TestHandler();
        TestSOAPHandler soapHandler = new TestSOAPHandler();
        addHandlersProgrammatically(disp, handler, soapHandler);
        InputStream is = this.getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapReq = factory.createMessage(null, is);
        DOMSource domReqMessage = new DOMSource(soapReq.getSOAPPart());

        DOMSource response = disp.invoke(domReqMessage);
        //XMLUtils.writeTo(response, System.out);
        assertNotNull(response);
    }

    @Test
    public void testInvokeWithDOMSourcPayloadMode() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/addNumbers.wsdl");
        assertNotNull(wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull(service);

        Dispatch<DOMSource> disp = service.createDispatch(portName, DOMSource.class, Mode.PAYLOAD);
        setAddress(disp, addNumbersAddress);

        TestHandler handler = new TestHandler();
        TestSOAPHandler soapHandler = new TestSOAPHandler();
        addHandlersProgrammatically(disp, handler, soapHandler);

        InputStream is2 =  this.getClass().getResourceAsStream("resources/GreetMeDocLiteralReqPayload.xml");
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapReq = factory.createMessage(null, is2);
        DOMSource domReqMessage = new DOMSource(soapReq.getSOAPPart());

        DOMSource response = disp.invoke(domReqMessage);
        assertNotNull(response);
    }

    @Test
    public void testInvokeWithSOAPMessageMessageMode() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/addNumbers.wsdl");
        assertNotNull(wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull(service);

        Dispatch<SOAPMessage> disp = service.createDispatch(portName, SOAPMessage.class, Mode.MESSAGE);
        setAddress(disp, addNumbersAddress);

        TestHandler handler = new TestHandler();
        TestSOAPHandler soapHandler = new TestSOAPHandler();
        addHandlersProgrammatically(disp, handler, soapHandler);

        InputStream is2 =  this.getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapReq = factory.createMessage(null, is2);

        SOAPMessage response = disp.invoke(soapReq);
        assertNotNull(response);
        //response.writeTo(System.out);
    }

    @Test
    public void testInvokeWithSOAPMessagePayloadMode() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/addNumbers.wsdl");
        assertNotNull(wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull(service);

        Dispatch<SOAPMessage> disp = service.createDispatch(portName, SOAPMessage.class, Mode.PAYLOAD);
        setAddress(disp, addNumbersAddress);

        TestHandler handler = new TestHandler();
        TestSOAPHandler soapHandler = new TestSOAPHandler();
        addHandlersProgrammatically(disp, handler, soapHandler);

        InputStream is2 =  this.getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapReq = factory.createMessage(null, is2);

        try {
            disp.invoke(soapReq);
            fail("Did not get expected exception");
        } catch (SOAPFaultException e) {
            assertTrue("Did not get expected exception message: " + e.getMessage(),  e.getMessage()
                       .indexOf("is not valid in PAYLOAD mode with SOAP/HTTP binding") > -1);
        }
    }

    @Test
    public void testInvokeWithDOMSourcMessageModeXMLBinding() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/addNumbers.wsdl");
        assertNotNull(wsdl);

        XMLService service = new XMLService();
        assertNotNull(service);

        Dispatch<DOMSource> disp = service.createDispatch(portNameXML, DOMSource.class, Mode.MESSAGE);
        setAddress(disp, addNumbersAddress);

        TestHandlerXMLBinding handler = new TestHandlerXMLBinding();
        addHandlersProgrammatically(disp, handler);

        InputStream is = getClass().getResourceAsStream("/messages/XML_GreetMeDocLiteralReq.xml");
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapReq = factory.createMessage(null, is);
        DOMSource domReqMessage = new DOMSource(soapReq.getSOAPPart());

        DOMSource response = disp.invoke(domReqMessage);
        assertNotNull(response);
    }

    @Test
    public void testInvokeWithDOMSourcPayloadModeXMLBinding() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/addNumbers.wsdl");
        assertNotNull(wsdl);

        XMLService service = new XMLService();
        assertNotNull(service);

        Dispatch<DOMSource> disp = service.createDispatch(portNameXML, DOMSource.class, Mode.PAYLOAD);
        setAddress(disp, addNumbersAddress);

        TestHandlerXMLBinding handler = new TestHandlerXMLBinding();
        addHandlersProgrammatically(disp, handler);

        InputStream is = getClass().getResourceAsStream("/messages/XML_GreetMeDocLiteralReq.xml");
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapReq = factory.createMessage(null, is);
        DOMSource domReqMessage = new DOMSource(soapReq.getSOAPPart());

        DOMSource response = disp.invoke(domReqMessage);
        assertNotNull(response);
    }

    @Test
    public void testInvokeWithDataSourcMessageModeXMLBinding() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/addNumbers.wsdl");
        assertNotNull(wsdl);

        XMLService service = new XMLService();
        assertNotNull(service);

        Dispatch<DataSource> disp = service.createDispatch(portNameXML, DataSource.class, Mode.MESSAGE);
        setAddress(disp, addNumbersAddress);

        TestHandlerXMLBinding handler = new TestHandlerXMLBinding();
        addHandlersProgrammatically(disp, handler);

        URL is = getClass().getResource("/messages/XML_GreetMeDocLiteralReq.xml");
        DataSource ds = new URLDataSource(is);

        try {
            disp.invoke(ds);
            fail("Did not get expected exception");
        } catch (HTTPException e) {
            //expected
        }
    }

    @Test
    public void testInvokeWithDataSourcPayloadModeXMLBinding() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/addNumbers.wsdl");
        assertNotNull(wsdl);

        XMLService service = new XMLService();
        assertNotNull(service);

        Dispatch<DataSource> disp = service.createDispatch(portNameXML, DataSource.class, Mode.PAYLOAD);
        setAddress(disp, addNumbersAddress);

        TestHandlerXMLBinding handler = new TestHandlerXMLBinding();
        addHandlersProgrammatically(disp, handler);

        URL is = getClass().getResource("/messages/XML_GreetMeDocLiteralReq.xml");
        DataSource ds = new URLDataSource(is);

        try {
            disp.invoke(ds);
            fail("Did not get expected exception");
        } catch (HTTPException e) {
            assertEquals(e.getCause().getMessage(),
                         "DataSource is not valid in PAYLOAD mode with XML/HTTP binding.");
        }
    }

    @Test
    public void testInvokeWithJAXBMessageModeXMLBinding() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/addNumbers.wsdl");
        assertNotNull(wsdl);

        XMLService service = new XMLService();
        assertNotNull(service);

        JAXBContext jc = JAXBContext.newInstance("org.apache.hello_world_xml_http.wrapped.types");
        Dispatch<Object> disp = service.createDispatch(portNameXML, jc, Mode.MESSAGE);
        setAddress(disp, greeterAddress);

        TestHandlerXMLBinding handler = new TestHandlerXMLBinding();
        addHandlersProgrammatically(disp, handler);

        org.apache.hello_world_xml_http.wrapped.types.GreetMe req =
            new org.apache.hello_world_xml_http.wrapped.types.GreetMe();
        req.setRequestType("tli");

        Object response = disp.invoke(req);
        assertNotNull(response);
        org.apache.hello_world_xml_http.wrapped.types.GreetMeResponse value =
            (org.apache.hello_world_xml_http.wrapped.types.GreetMeResponse)response;
        assertEquals("Hello tli", value.getResponseType());
    }

    @Test
    public void testInvokeWithJAXBPayloadModeXMLBinding() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/addNumbers.wsdl");
        assertNotNull(wsdl);

        XMLService service = new XMLService();
        assertNotNull(service);

        JAXBContext jc = JAXBContext.newInstance("org.apache.hello_world_xml_http.wrapped.types");
        Dispatch<Object> disp = service.createDispatch(portNameXML, jc, Mode.PAYLOAD);
        setAddress(disp, greeterAddress);

        TestHandlerXMLBinding handler = new TestHandlerXMLBinding();
        addHandlersProgrammatically(disp, handler);

        org.apache.hello_world_xml_http.wrapped.types.GreetMe req =
            new org.apache.hello_world_xml_http.wrapped.types.GreetMe();
        req.setRequestType("tli");

        Object response = disp.invoke(req);
        assertNotNull(response);
        org.apache.hello_world_xml_http.wrapped.types.GreetMeResponse value =
            (org.apache.hello_world_xml_http.wrapped.types.GreetMeResponse)response;
        assertEquals("Hello tli", value.getResponseType());
    }

    public void addHandlersProgrammatically(BindingProvider bp, Handler...handlers) {
        List<Handler> handlerChain = bp.getBinding().getHandlerChain();
        assertNotNull(handlerChain);
        for (Handler h : handlers) {
            handlerChain.add(h);
        }
        bp.getBinding().setHandlerChain(handlerChain);
    }

    class TestHandler implements LogicalHandler<LogicalMessageContext> {
        public boolean handleMessage(LogicalMessageContext ctx) {
            try {
                Boolean outbound = (Boolean)ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
                if (outbound) {
                    LogicalMessage msg = ctx.getMessage();
                    JAXBContext jaxbContext = JAXBContext
                        .newInstance(ObjectFactory.class,
                                     org.apache.hello_world_xml_http.wrapped.types.ObjectFactory.class);

                    Object payload = ((JAXBElement)msg.getPayload(jaxbContext)).getValue();
                    org.apache.handlers.types.AddNumbers req =
                        (org.apache.handlers.types.AddNumbers)payload;

                    assertEquals(10, req.getArg0());
                    assertEquals(20, req.getArg1());
                    
                    req.setArg0(11);
                    req.setArg1(21);
                    ObjectFactory of = new ObjectFactory();
                    of.createAddNumbers(req);
                    msg.setPayload(of.createAddNumbers(req), jaxbContext);
                    
                } else {
                    LogicalMessage msg = ctx.getMessage();
                    JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
                    Object payload = ((JAXBElement)msg.getPayload(jaxbContext)).getValue();
                    org.apache.handlers.types.AddNumbersResponse res =
                        (org.apache.handlers.types.AddNumbersResponse)payload;

                    assertEquals(333, res.getReturn());
                    
                    res.setReturn(222);
                    
                    ObjectFactory of = new ObjectFactory();
                    msg.setPayload(of.createAddNumbersResponse(res), jaxbContext);                     
                    
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
            return true;
        }
        public boolean handleFault(LogicalMessageContext ctx) {
            return true;
        }
        public void close(MessageContext arg0) {
        }
    }

    class TestHandlerXMLBinding implements LogicalHandler<LogicalMessageContext> {
        public boolean handleMessage(LogicalMessageContext ctx) {
            LogicalMessage msg = ctx.getMessage();

            Source payload = msg.getPayload();
            assertNotNull(payload);

            return true;
        }
        public boolean handleFault(LogicalMessageContext ctx) {
            return true;
        }
        public void close(MessageContext arg0) {
        }
    }

    class TestSOAPHandler implements SOAPHandler<SOAPMessageContext> {
        public boolean handleMessage(SOAPMessageContext ctx) {
            try {
                Boolean outbound = (Boolean)ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
                if (outbound) {
                    SOAPMessage msg = ctx.getMessage();
                    /*
                     * System.out.println("-----------soap---------");
                     * msg.writeTo(System.out);
                     * System.out.println("-----------soap---------");
                     */

                    SOAPEnvelope env = msg.getSOAPPart().getEnvelope();
                    SOAPBody body = env.getBody();
                    Iterator it = body.getChildElements();
                    while (it.hasNext()) {
                        
                        Object elem = it.next();
                        if (elem instanceof SOAPElement) {

                            Iterator it2 = ((SOAPElement)elem).getChildElements();
                            while (it2.hasNext()) {
                                Object elem2 = it2.next();
                                if (elem2 instanceof SOAPElement) {
                                    String value = ((SOAPElement)elem2).getValue();
                                    String name = ((SOAPElement)elem2).getLocalName();
                                    if (name.indexOf("arg0") >= 0 && value.equalsIgnoreCase("11")) {
                                        value = "12";
                                        ((SOAPElement)elem2).setValue(value);
                                    }
                                    if (name.indexOf("arg1") >= 0 && value.equalsIgnoreCase("21")) {
                                        value = "22";
                                        ((SOAPElement)elem2).setValue(value);
                                    }
                                }
                            }
                        }
                    }
                    msg.saveChanges();           
                } else {
                    SOAPMessage msg = ctx.getMessage();
                    /*
                     * System.out.println("-----------soap---------");
                     * msg.writeTo(System.out);
                     * System.out.println("-----------soap---------");
                     */

                    SOAPEnvelope env = msg.getSOAPPart().getEnvelope();
                    SOAPBody body = env.getBody();
                    Iterator it = body.getChildElements();
                    while (it.hasNext()) {
                        
                        Object elem = it.next();
                        if (elem instanceof SOAPElement) {

                            Iterator it2 = ((SOAPElement)elem).getChildElements();
                            while (it2.hasNext()) {
                                Object elem2 = it2.next();
                                if (elem2 instanceof SOAPElement) {
                                    String value = ((SOAPElement)elem2).getValue();
                                    String name = ((SOAPElement)elem2).getLocalName();
                                    if (name.indexOf("return") >= 0 && value.equalsIgnoreCase("264")) {
                                        value = "333";
                                        ((SOAPElement)elem2).setValue(value);
                                    }
                                }
                            }
                        }
                    }
                    msg.saveChanges();                     
                }
/*                SOAPMessage msg = ctx.getMessage();
                //msg.writeTo(System.out);
                assertNotNull(msg);*/
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }

            return true;
        }
        
        public final Set<QName> getHeaders() {
            return null;
        }
        public boolean handleFault(SOAPMessageContext ctx) {
            return true;
        }
        public void close(MessageContext arg0) {
        }
    }
}

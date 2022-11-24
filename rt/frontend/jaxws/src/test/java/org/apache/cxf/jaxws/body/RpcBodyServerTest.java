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


package org.apache.cxf.jaxws.body;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.AbstractJaxWsTest;
import org.apache.header_test.rpc.SOAPRPCHeaderService;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RpcBodyServerTest extends AbstractJaxWsTest {

    @Before
    public void setUp() throws Exception {
        BusFactory.setDefaultBus(getBus());

        String address = "http://localhost:9105/SoapBodyRPCContext/SoapBodyRPCPort";
        Endpoint.publish(address, new RPCGreeterImpl());
    }

    @Test
    public void testRPCInBodyObject() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapbody_rpc.wsdl");
        assertNotNull(wsdl);

        SOAPRPCHeaderService service = new SOAPRPCHeaderService(
                                                                wsdl,
                                                                new QName(
                                                                          "http://apache.org/body_test/rpc",
                                                                          "SOAPRPCBodyService"));
        assertNotNull(service);
        Dispatch<SOAPMessage> dispatch = service
            .createDispatch(new QName("http://apache.org/body_test/rpc", "SoapRPCBodyPort"),
                    jakarta.xml.soap.SOAPMessage.class, Service.Mode.MESSAGE);

        MessageFactory factory = MessageFactory.newInstance();
        InputStream is = getClass().getClassLoader()
            .getResourceAsStream("./soapbody_rpc_provider/sayHelloMsg.xml");
        SOAPMessage inMessage = factory.createMessage(null, is);
        SOAPMessage response = dispatch.invoke(inMessage);
        is.close();

        try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
            response.writeTo(bout);
            assertTrue(new String(bout.toByteArray()).contains("Yes!"));
        }
    }

    /**
     * When arguments are of primitive type, the invocation fails (since 'null' 
     * placeholders could not be converted to primitive ones).
     */
    @Test(expected = SOAPFaultException.class)
    public void testRPCInBodyPrimitive() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapbody_rpc.wsdl");
        assertNotNull(wsdl);

        SOAPRPCHeaderService service = new SOAPRPCHeaderService(
                                                                wsdl,
                                                                new QName(
                                                                          "http://apache.org/body_test/rpc",
                                                                          "SOAPRPCBodyService"));
        assertNotNull(service);
        Dispatch<SOAPMessage> dispatch = service
            .createDispatch(new QName("http://apache.org/body_test/rpc", "SoapRPCBodyPort"),
                    jakarta.xml.soap.SOAPMessage.class, Service.Mode.MESSAGE);

        MessageFactory factory = MessageFactory.newInstance();
        InputStream is = getClass().getClassLoader()
            .getResourceAsStream("./soapbody_rpc_provider/sayHello1Msg.xml");
        SOAPMessage inMessage = factory.createMessage(null, is);
        dispatch.invoke(inMessage);
        is.close();
    }

    @Test
    public void testRPCInBodyNoArguments() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapbody_rpc.wsdl");
        assertNotNull(wsdl);

        SOAPRPCHeaderService service = new SOAPRPCHeaderService(
                                                                wsdl,
                                                                new QName(
                                                                          "http://apache.org/body_test/rpc",
                                                                          "SOAPRPCBodyService"));
        assertNotNull(service);
        Dispatch<SOAPMessage> dispatch = service
            .createDispatch(new QName("http://apache.org/body_test/rpc", "SoapRPCBodyPort"),
                    jakarta.xml.soap.SOAPMessage.class, Service.Mode.MESSAGE);

        MessageFactory factory = MessageFactory.newInstance();
        InputStream is = getClass().getClassLoader()
            .getResourceAsStream("./soapbody_rpc_provider/sayHello2Msg.xml");
        SOAPMessage inMessage = factory.createMessage(null, is);
        SOAPMessage response = dispatch.invoke(inMessage);
        is.close();

        try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
            response.writeTo(bout);
            assertTrue(new String(bout.toByteArray()).contains("Yes!"));
        }
    }
}

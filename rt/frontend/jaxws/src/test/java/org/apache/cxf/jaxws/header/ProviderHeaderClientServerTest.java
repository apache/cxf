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


package org.apache.cxf.jaxws.header;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.AbstractJaxWsTest;
import org.apache.header_test.rpc.SOAPRPCHeaderService;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProviderHeaderClientServerTest extends AbstractJaxWsTest {

    @Before
    public void setUp() throws Exception {
        BusFactory.setDefaultBus(getBus());

        TestRPCHeaderProvider implementor = new TestRPCHeaderProvider();
        String address = "http://localhost:9104/SoapHeaderRPCContext/SoapHeaderRPCPort";
        Endpoint.publish(address, implementor);
    }

    @Test
    public void testRPCInHeader() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader_rpc.wsdl");
        assertNotNull(wsdl);

        SOAPRPCHeaderService service = new SOAPRPCHeaderService(
                                                                wsdl,
                                                                new QName(
                                                                          "http://apache.org/header_test/rpc",
                                                                          "SOAPRPCHeaderService"));
        assertNotNull(service);
        Dispatch<SOAPMessage> dispatch = service
            .createDispatch(new QName("http://apache.org/header_test/rpc", "SoapRPCHeaderPort"),
                            jakarta.xml.soap.SOAPMessage.class, Service.Mode.MESSAGE);

        MessageFactory factory = MessageFactory.newInstance();
        InputStream is = getClass().getClassLoader()
            .getResourceAsStream("./soapheader_rpc_provider/sayHelloMsg.xml");
        SOAPMessage inMessage = factory.createMessage(null, is);
        SOAPMessage response = dispatch.invoke(inMessage);
        is.close();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        response.writeTo(bout);
        assertTrue(new String(bout.toByteArray()).contains("part/header"));

    }

}

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

import java.util.Map;

import org.w3c.dom.Document;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import org.apache.cxf.Bus;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.WSDLGetUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class WsdlGetUtilsTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(WsdlGetUtilsTest.class);

    @WebService(targetNamespace = "org.apache.cxf.ws.WsdlTest")
    public static class StuffImpl {
        @WebMethod
        public void doStuff() {
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        createStaticBus();
    }

    @Test
    public void testNewDocumentIsCreatedForEachWsdlRequest() {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceBean(new StuffImpl());
        factory.setAddress("http://localhost:" + PORT + "/Stuff");
        Server server = factory.create();

        try {
            Message message = new MessageImpl();
            Exchange exchange = new ExchangeImpl();
            exchange.put(Bus.class, getBus());
            exchange.put(Service.class, server.getEndpoint().getService());
            exchange.put(Endpoint.class, server.getEndpoint());
            message.setExchange(exchange);

            Map<String, String> map = UrlUtils.parseQueryString("wsdl");
            String baseUri = "http://localhost:" + PORT + "/Stuff";
            String ctx = "/Stuff";

            WSDLGetUtils utils = new WSDLGetUtils();
            Document doc = utils.getDocument(message, baseUri, map, ctx, server.getEndpoint().getEndpointInfo());

            Document doc2 = utils.getDocument(message, baseUri, map, ctx, server.getEndpoint().getEndpointInfo());

            assertFalse(doc == doc2);
        } finally {
            server.stop();
        }
    }
}

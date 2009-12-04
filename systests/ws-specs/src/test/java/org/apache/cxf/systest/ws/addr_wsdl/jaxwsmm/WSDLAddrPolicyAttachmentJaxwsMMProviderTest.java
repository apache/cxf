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

package org.apache.cxf.systest.ws.addr_wsdl.jaxwsmm;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.policy.PolicyInInterceptor;
import org.apache.cxf.ws.policy.PolicyOutInterceptor;

import org.junit.BeforeClass;
import org.junit.Test;

import messaging.AsyncMessaging;
import messaging.AsyncMessagingService;

public class WSDLAddrPolicyAttachmentJaxwsMMProviderTest extends AbstractBusClientServerTestBase {

    private static final Logger LOG = LogUtils.getLogger(WSDLAddrPolicyAttachmentJaxwsMMProviderTest.class);

    private static final String ADDRESS = "http://localhost:9000/AsyncMessagingServiceProvider";
    private static final String WSDL_ADDRESS = ADDRESS + "?wsdl";
    private static final QName ENDPOINT_NAME = new QName("http://messaging/", "AsyncMessagingService");

    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus("org/apache/cxf/systest/ws/addr_wsdl/jaxwsmm/server.xml");

            JaxWsServerFactoryBean serviceFactory = new JaxWsServerFactoryBean();
            serviceFactory.setBus(bus);
            serviceFactory.setServiceClass(MessageProviderWithAddressingPolicy.class);
            serviceFactory.setWsdlLocation("wsdl_systest_wsspec/addr-jaxwsmm.wsdl");
            serviceFactory.setAddress(ADDRESS);
            org.apache.cxf.endpoint.Server provider = serviceFactory.create();
            EndpointInfo ei = provider.getEndpoint().getEndpointInfo();
            LOG.info("Started server at: " + ei.getAddress());

            testInterceptors(bus);
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

    private static void testInterceptors(Bus b) {
        boolean hasServerIn = false;
        boolean hasServerOut = false;
        List<Interceptor<? extends Message>> inInterceptors = b.getInInterceptors();
        for (Interceptor<? extends Message> i : inInterceptors) {
            if (i instanceof PolicyInInterceptor) {
                hasServerIn = true;
            }
        }
        assertTrue(hasServerIn);

        for (Interceptor<? extends Message> i : b.getOutInterceptors()) {
            if (i instanceof PolicyOutInterceptor) {
                hasServerOut = true;
            }
        }
        assertTrue(hasServerOut);
    }

    @Test
    public void testUsingAddressing() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        bus = bf.createBus("org/apache/cxf/systest/ws/policy/addr-inline-policy-old.xml");

        BusFactory.setDefaultBus(bus);

        URL wsdlURL = new URL(WSDL_ADDRESS);

        AsyncMessagingService ams = new AsyncMessagingService(wsdlURL, ENDPOINT_NAME);
        AsyncMessaging am = ams.getAsyncMessagingImplPort();

        ConnectionHelper.setKeepAliveConnection(am, true);
        testInterceptors(bus);

        // oneway
        am.deliver("This is a test");
        am.deliver("This is another test");
    }
}

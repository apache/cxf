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

package org.apache.cxf.systest.ws.rm.sec;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.types.GreetMe;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.rm.RMManager;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests the correct interaction of ws-rm calls with security.when policy validator verifies the calls.
 */
public class WSRMWithWSSecurityPolicyTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    private static final Logger LOG = LogUtils.getLogger(WSRMWithWSSecurityPolicyTest.class);

    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus("/org/apache/cxf/systest/ws/rm/sec/server-policy.xml");
            BusFactory.setDefaultBus(bus);
            setBus(bus);
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testWithSecurityInPolicy() throws Exception {
        LOG.fine("Creating greeter client");

        try (ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext("org/apache/cxf/systest/ws/rm/sec/client-policy.xml")) {

            Bus bus = (Bus)context.getBean("bus");
            BusFactory.setDefaultBus(bus);
            BusFactory.setThreadDefaultBus(bus);

            Greeter greeter = (Greeter)context.getBean("GreeterCombinedClient");

            RMManager manager = bus.getExtension(RMManager.class);
            boolean empty = manager.getRetransmissionQueue().isEmpty();
            assertTrue("RetransmissionQueue is not empty", empty);

            LOG.fine("Invoking greeter");
            greeter.greetMeOneWay("one");

            Thread.sleep(3000);

            empty = manager.getRetransmissionQueue().isEmpty();
            assertTrue("RetransmissionQueue not empty", empty);

        }
    }

    @Test
    public void testContextProperty() throws Exception {
        try (ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("org/apache/cxf/systest/ws/rm/sec/client-policy.xml")) {
            Bus bus = (Bus)context.getBean("bus");
            BusFactory.setDefaultBus(bus);
            BusFactory.setThreadDefaultBus(bus);
            Greeter greeter = (Greeter)context.getBean("GreeterCombinedClientNoProperty");
            Client client = ClientProxy.getClient(greeter);
            QName operationQName = new QName("http://cxf.apache.org/greeter_control", "greetMe");
            BindingOperationInfo boi = client.getEndpoint().getBinding().getBindingInfo().getOperation(operationQName);
            Map<String, Object> invocationContext = new HashMap<>();
            Map<String, Object> requestContext = new HashMap<>();
            Map<String, Object> responseContext = new HashMap<>();
            invocationContext.put(Client.REQUEST_CONTEXT, requestContext);
            invocationContext.put(Client.RESPONSE_CONTEXT, responseContext);

            requestContext.put(SecurityConstants.USERNAME, "Alice");
            requestContext.put(SecurityConstants.CALLBACK_HANDLER,
                "org.apache.cxf.systest.ws.rm.sec.UTPasswordCallback");
            requestContext.put(SecurityConstants.ENCRYPT_PROPERTIES, "bob.properties");
            requestContext.put(SecurityConstants.ENCRYPT_USERNAME, "bob");
            requestContext.put(SecurityConstants.SIGNATURE_PROPERTIES, "alice.properties");
            requestContext.put(SecurityConstants.SIGNATURE_USERNAME, "alice");
            RMManager manager = bus.getExtension(RMManager.class);
            boolean empty = manager.getRetransmissionQueue().isEmpty();
            assertTrue("RetransmissionQueue is not empty", empty);
            GreetMe param = new GreetMe();
            param.setRequestType("testContextProperty");
            Object[] answer = client.invoke(boi, new Object[]{param}, invocationContext);
            Assert.assertEquals("TESTCONTEXTPROPERTY", answer[0].toString());
            Thread.sleep(5000);
            empty = manager.getRetransmissionQueue().isEmpty();
            assertTrue("RetransmissionQueue not empty", empty);
        }
    }

}

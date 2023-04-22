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

package org.apache.cxf.systest.ws.rm;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.message.Message;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests the addition of WS-RM properties to application messages and the
 * exchange of WS-RM protocol messages.
 */
public class DecoupledClientServerOnewayTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    public static final String DECOUPLE_PORT = allocatePort(DecoupledClientServerOnewayTest.class);

    private static final Logger LOG = LogUtils.getLogger(DecoupledClientServerOnewayTest.class);

    public static class Server extends AbstractBusTestServerBase {
        Endpoint ep;
        protected void run()  {
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus("/org/apache/cxf/systest/ws/rm/exactlyonce-inorder-decoupled.xml");
            BusFactory.setDefaultBus(bus);
            setBus(bus);
            LoggingInInterceptor in = new LoggingInInterceptor();
            in.setPrettyLogging(true);
            bus.getInInterceptors().add(in);
            bus.getInFaultInterceptors().add(in);
            LoggingOutInterceptor out = new LoggingOutInterceptor();
            out.setPrettyLogging(true);
            bus.getOutInterceptors().add(out);
            bus.getOutFaultInterceptors().add(out);

            GreeterImpl implementor = new GreeterImpl();
            implementor.useLastOnewayArg(true);
            implementor.setDelay(5000);
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";

            ep = Endpoint.create(implementor);
            Map<String, Object> properties = new HashMap<>();
            properties.put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE);
            ep.setProperties(properties);
            ep.publish(address);

            LOG.info("Published greeter endpoint.");
        }
        public void tearDown() {
            ep.stop();
            ep = null;
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, true));
    }

    @Test
    public void testDecoupledOneway() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus("/org/apache/cxf/systest/ws/rm/exactlyonce-inorder-decoupled.xml");
        BusFactory.setDefaultBus(bus);
        LoggingInInterceptor in = new LoggingInInterceptor();
        in.setPrettyLogging(true);
        bus.getInInterceptors().add(in);
        bus.getInFaultInterceptors().add(in);
        LoggingOutInterceptor out = new LoggingOutInterceptor();
        out.setPrettyLogging(true);
        bus.getOutInterceptors().add(out);
        bus.getOutFaultInterceptors().add(out);

        GreeterService gs = new GreeterService();
        final Greeter greeter = gs.getGreeterPort();
        updateAddressPort(greeter, PORT);
        ((BindingProvider)greeter).getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED,
                                                           Boolean.TRUE);
        LOG.fine("Created greeter client.");

        ConnectionHelper.setKeepAliveConnection(greeter, true);
        greeter.greetMeOneWay("oneway");

    }
}

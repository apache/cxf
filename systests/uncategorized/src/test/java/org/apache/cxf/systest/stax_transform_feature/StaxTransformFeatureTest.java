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

package org.apache.cxf.systest.stax_transform_feature;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.interceptor.transform.TransformInInterceptor;
import org.apache.cxf.interceptor.transform.TransformOutInterceptor;
import org.apache.cxf.systest.interceptor.GreeterImpl;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests whether the stax transformer is correctly engaged and it does not interfere with logging.
 * This test uses a simple transformation. More complex transformation tests are found in the api package.
 *
 */
public class StaxTransformFeatureTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    private static final Logger LOG = LogUtils.getLogger(StaxTransformFeatureTest.class);
    private static final String GREETER_PORT_ADDRESS = "http://localhost:" + PORT + "/SoapContext/GreeterPort";

    private static TestLoggingEventSender serverlogIn = new TestLoggingEventSender();
    private static TestLoggingEventSender serverlogOut = new TestLoggingEventSender();
    private static TransformInInterceptor servertransIn = new TransformInInterceptor();
    private static TransformOutInterceptor servertransOut = new TransformOutInterceptor();

    private Greeter greeter;


    public static class Server extends AbstractBusTestServerBase {

        Endpoint ep;
        protected void run() {
            SpringBusFactory factory = new SpringBusFactory();
            Bus bus = factory.createBus();
            BusFactory.setDefaultBus(bus);
            setBus(bus);

            bus.getInInterceptors().add(new LoggingInInterceptor(serverlogIn));
            bus.getOutInterceptors().add(new LoggingOutInterceptor(serverlogOut));
            bus.getOutFaultInterceptors().add(new LoggingOutInterceptor(serverlogOut));


            Map<String, String> inElements = new HashMap<>();
            inElements.put("{http://cxf.apache.org/greeter_control/types}dontPingMe",
                           "{http://cxf.apache.org/greeter_control/types}pingMe");
            servertransIn.setInTransformElements(inElements);
            bus.getInInterceptors().add(servertransIn);


            Map<String, String> outElements = new HashMap<>();
            outElements.put("{http://cxf.apache.org/greeter_control/types}faultDetail",
                "{http://cxf.apache.org/greeter_control/types}noFaultDetail");
            servertransOut.setOutTransformElements(outElements);
            bus.getOutInterceptors().add(servertransOut);
            bus.getOutFaultInterceptors().add(servertransOut);

            GreeterImpl implementor = new GreeterImpl();
            ep = Endpoint.publish(GREETER_PORT_ADDRESS, implementor);
            LOG.fine("Published control endpoint.");
        }

        public void tearDown() {
            ep.stop();
            ep = null;
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
        // force the info logging for this test
        LOG.setLevel(Level.INFO);
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @AfterClass
    public static void reset() {
        Bus b = BusFactory.getDefaultBus(false);
        if (b == null) {
            b = BusFactory.getThreadDefaultBus(false);
        }
        if (b == null) {
            b = BusFactory.getDefaultBus();
        }
        b.shutdown(true);
    }

    @After
    public void tearDown() throws Exception {
        if (null != greeter) {
            ((java.io.Closeable)greeter).close();
        }
    }

    @Test
    public void testTransformWithLogging() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);

        TestLoggingEventSender logIn = new TestLoggingEventSender();
        bus.getInInterceptors().add(new LoggingInInterceptor(logIn));
        bus.getInFaultInterceptors().add(new LoggingInInterceptor(logIn));
        TestLoggingEventSender logOut = new TestLoggingEventSender();
        bus.getOutInterceptors().add(new LoggingOutInterceptor(logOut));
        bus.getOutFaultInterceptors().add(new LoggingOutInterceptor(logOut));

        TransformInInterceptor transIn = new TransformInInterceptor();
        Map<String, String> inElements = new HashMap<>();
        inElements.put("{http://cxf.apache.org/greeter_control/types}noFaultDetail",
            "{http://cxf.apache.org/greeter_control/types}faultDetail");
        bus.getInInterceptors().add(transIn);

        TransformOutInterceptor transOut = new TransformOutInterceptor();
        Map<String, String> outElements = new HashMap<>();
        outElements.put("{http://cxf.apache.org/greeter_control/types}pingMe",
            "{http://cxf.apache.org/greeter_control/types}dontPingMe");
        transOut.setOutTransformElements(outElements);

        bus.getOutInterceptors().add(transOut);
        bus.getOutFaultInterceptors().add(transOut);

        GreeterService gs = new GreeterService();
        greeter = gs.getGreeterPort();

        updateAddressPort(greeter, PORT);
        LOG.fine("Created greeter client.");

        // ping 1: request-response transformation
        greeter.pingMe();
        verifyPayload(logOut.getMessage(), "dontPingMe");
        verifyPayload(logIn.getMessage(), "pingMeResponse");
        verifyPayload(serverlogIn.getMessage(), "dontPingMe");
        verifyPayload(serverlogOut.getMessage(), "pingMeResponse");

        serverlogOut.cleaerMessage();
        serverlogIn.cleaerMessage();
        logOut.cleaerMessage();
        logIn.cleaerMessage();

        // ping 2: request-fault transformation
        try {
            greeter.pingMe();
            fail("Ping should have failed");
        } catch (Exception e) {
            assertEquals("Pings succeed only every other time.", e.getMessage());
        }
        verifyPayload(logOut.getMessage(), "dontPingMe");
        verifyPayload(logIn.getMessage(), "noFaultDetail");
        verifyPayload(serverlogIn.getMessage(), "dontPingMe");
        verifyPayload(serverlogOut.getMessage(), "noFaultDetail");

        // ping 3: idle
        greeter.pingMe();

        serverlogOut.cleaerMessage();
        serverlogIn.cleaerMessage();
        logOut.cleaerMessage();
        logIn.cleaerMessage();

        // ping 4: request-fault transformation with skipOnFault
        transOut.setSkipOnFault(true);
        servertransOut.setSkipOnFault(true);
        try {
            greeter.pingMe();
            fail("Ping should have failed");
        } catch (Exception e) {
            assertEquals("Pings succeed only every other time.", e.getMessage());
        }
        verifyPayload(logOut.getMessage(), "dontPingMe");
        verifyPayload(logIn.getMessage(), "faultDetail");
        verifyPayload(serverlogIn.getMessage(), "dontPingMe");
        verifyPayload(serverlogOut.getMessage(), "faultDetail");

        bus.shutdown(true);
    }

    private void verifyPayload(String m, String value) {
        assertNotNull("message not logged", m);
        // the entire soap envelope is logged
        assertTrue(m, m.indexOf("<soap:Envelope") >= 0 && m.indexOf("</soap:Envelope>") > 0);
        // the transformed body is logged
        assertTrue(value + " must be found in payload: " + m, m.indexOf(value) > 0);
    }

    static class TestLoggingEventSender implements LogEventSender {
        private String logMessage;

        public String getMessage() {
            return logMessage;
        }
        public void cleaerMessage() {
            logMessage = null;
        }
        @Override
        public void send(LogEvent event) {
            logMessage = event.getPayload();
        }
    }


}

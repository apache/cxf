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

package org.apache.cxf.systest.ws.policy;

import java.io.Closeable;
import java.util.logging.Logger;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.greeter_control.BasicGreeterService;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.PingMeFault;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.systest.ws.util.MessageFlow;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.testutil.recorders.InMessageRecorder;
import org.apache.cxf.testutil.recorders.MessageRecorder;
import org.apache.cxf.testutil.recorders.OutMessageRecorder;
import org.apache.cxf.ws.addressing.VersionTransformer.Names200408;
import org.apache.cxf.ws.rm.RM10Constants;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the use of the WS-Policy Framework to automatically engage WS-RM
 * in response to Policies defined for the endpoint via an external policy attachment.
 */
public class RMPolicyTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    public static final String TEMPDIR = FileUtils.getDefaultTempDir().toURI().toString();

    private static final Logger LOG = LogUtils.getLogger(RMPolicyTest.class);
    private static final String GREETMEONEWAY_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeOneWayRequest";
    private static final String GREETME_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeRequest";
    private static final String GREETME_RESPONSE_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeResponse";
    private static final String PINGME_ACTION = "http://cxf.apache.org/greeter_control/Greeter/pingMeRequest";
    private static final String PINGME_RESPONSE_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/pingMeResponse";
    private static final String GREETER_FAULT_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/pingMe/Fault/faultDetail";

    public static class Server extends AbstractBusTestServerBase {
        String tmpDir = TEMPDIR;
        Endpoint ep;
        public Server() {
        }
        public Server(String dir) {
            tmpDir = dir;
        }
        public Server(String[] args) {
            tmpDir = args[0];
        }
        protected void run()  {
            System.setProperty("temp.location", tmpDir);
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus("org/apache/cxf/systest/ws/policy/rm.xml");
            BusFactory.setDefaultBus(bus);
            setBus(bus);
            LoggingInInterceptor in = new LoggingInInterceptor();
            bus.getInInterceptors().add(in);
            LoggingOutInterceptor out = new LoggingOutInterceptor();
            bus.getOutInterceptors().add(out);
            bus.getOutFaultInterceptors().add(out);

            GreeterImpl implementor = new GreeterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
            ep = Endpoint.publish(address, implementor);
            LOG.info("Published greeter endpoint.");
        }
        public void tearDown() {
            ep.stop();
            ep = null;
        }

        public static void main(String[] args) {
            try {
                if (args.length == 0) {
                    args = new String[] {TEMPDIR};
                }
                Server s = new Server(args[0]);
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
        TestUtil.getNewPortNumber("decoupled");
        PolicyTestHelper.updatePolicyRef("rm-external.xml", ":9020", ":" + PORT);
        System.setProperty("temp.location", TEMPDIR);

        assertTrue("server did not launch correctly", launchServer(Server.class, null,
                                                                   new String[] {TEMPDIR}));
    }

    @Test
    public void testUsingRM() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus("org/apache/cxf/systest/ws/policy/rm.xml");
        BusFactory.setDefaultBus(bus);
        OutMessageRecorder outRecorder = new OutMessageRecorder();
        bus.getOutInterceptors().add(outRecorder);
        InMessageRecorder inRecorder = new InMessageRecorder();
        bus.getInInterceptors().add(inRecorder);

        BasicGreeterService gs = new BasicGreeterService();
        final Greeter greeter = gs.getGreeterPort();
        updateAddressPort(greeter, PORT);
        LOG.fine("Created greeter client.");

        ConnectionHelper.setKeepAliveConnection(greeter, true);

        // two-way

        assertEquals("CXF", greeter.greetMe("cxf"));

        // oneway

        greeter.greetMeOneWay("CXF");

        // exception

        try {
            greeter.pingMe();
        } catch (PingMeFault ex) {
            fail("First invocation should have succeeded.");
        }

        try {
            greeter.pingMe();
            fail("Expected PingMeFault not thrown.");
        } catch (PingMeFault ex) {
            assertEquals(2, ex.getFaultInfo().getMajor());
            assertEquals(1, ex.getFaultInfo().getMinor());
        }

        MessageRecorder mr = new MessageRecorder(outRecorder, inRecorder);
        mr.awaitMessages(5, 4, 5000);

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages(),
            Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);


        mf.verifyMessages(5, true);
        String[] expectedActions = new String[] {RM10Constants.INSTANCE.getCreateSequenceAction(),
                                                 GREETME_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 PINGME_ACTION,
                                                 PINGME_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3", "4"}, true);
        mf.verifyLastMessage(new boolean[] {false, false, false, false, false}, true);
        mf.verifyAcknowledgements(new boolean[] {false, false, true, false, true}, true);

        mf.verifyMessages(4, false);
//        mf.verifyMessages(9, false);
//        mf.verifyPartialResponses(5);
//        mf.purgePartialResponses();

        expectedActions = new String[] {
            RM10Constants.INSTANCE.getCreateSequenceResponseAction(),
            GREETME_RESPONSE_ACTION,
            PINGME_RESPONSE_ACTION,
            GREETER_FAULT_ACTION
        };
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, false);
        mf.verifyLastMessage(new boolean[] {false, false, false, false}, false);
        mf.verifyAcknowledgements(new boolean[] {false, true, true, true}, false);
        ((Closeable)greeter).close();

    }
}

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

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.PingMeFault;
import org.apache.cxf.greeter_control.ReliableGreeterService;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.systest.ws.util.MessageFlow;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.testutil.recorders.MessageRecorder;
import org.apache.cxf.ws.rm.RM10Constants;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the use of the WS-Policy Framework to automatically engage WS-RM 1.0 in response to Policies defined for the
 * endpoint via an direct attachment to the wsdl.
 */
public class RM10PolicyWsdlTest extends RMPolicyWsdlTestBase {

    public static final String PORT = allocatePort(Server.class);

    private static final Logger LOG = LogUtils.getLogger(RM10PolicyWsdlTest.class);

    public static class Server extends ServerBase {

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

        @Override
        protected String getConfigPath() {
            return "org/apache/cxf/systest/ws/policy/rm10wsdl_server.xml";
        }
    }


    @BeforeClass
    public static void startServers() throws Exception {
        TestUtil.getNewPortNumber("decoupled");
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testUsingRM() throws Exception {
        setUpBus(PORT);
        ReliableGreeterService gs = new ReliableGreeterService();
        Greeter greeter = gs.getGreeterPort();
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
//        mr.awaitMessages(5, 9, 5000);

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
                                         inRecorder.getInboundMessages(),
                                         "http://schemas.xmlsoap.org/ws/2004/08/addressing",
                                         "http://schemas.xmlsoap.org/ws/2005/02/rm");


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

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

package org.apache.cxf.systest.ws.wssec11;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.cxf.systest.ws.wssec11.server.Server11;
import org.apache.cxf.systest.ws.wssec11.server.Server11Restricted;
import org.apache.cxf.systest.ws.wssec11.server.StaxServer11;
import org.apache.cxf.systest.ws.wssec11.server.StaxServer11Restricted;
import org.apache.cxf.test.TestUtilities;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertTrue;

/**
 * This class runs the first half of the tests, as having all in
 * the one class causes an out of memory problem in eclipse.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class WSSecurity111Test extends WSSecurity11Common {
    private static boolean unrestrictedPoliciesInstalled;

    static {
        unrestrictedPoliciesInstalled = TestUtilities.checkUnrestrictedPoliciesInstalled();
    };

    final TestParam test;

    public WSSecurity111Test(TestParam type) {
        this.test = type;
    }

    static class TestParam {
        final String prefix;
        final boolean streaming;
        final String port;

        TestParam(String p, String port, boolean b) {
            prefix = p;
            this.port = port;
            streaming = b;
        }
        public String toString() {
            return prefix + ":" + port + ":" + (streaming ? "streaming" : "dom");
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        if (unrestrictedPoliciesInstalled) {
            assertTrue(
                    "Server failed to launch",
                    // run the server in the same process
                    // set this to false to fork
                    launchServer(Server11.class, true)
            );
            assertTrue(
                       "Server failed to launch",
                       // run the server in the same process
                       // set this to false to fork
                       launchServer(StaxServer11.class, true)
            );
        } else {
            assertTrue(
                    "Server failed to launch",
                    // run the server in the same process
                    // set this to false to fork
                    launchServer(Server11Restricted.class, true)
            );
            assertTrue(
                       "Server failed to launch",
                       // run the server in the same process
                       // set this to false to fork
                       launchServer(StaxServer11Restricted.class, true)
            );
        }
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam> data() {
        String domPort = null;
        if (unrestrictedPoliciesInstalled) {
            domPort = Server11.PORT;
        } else {
            domPort = Server11Restricted.PORT;
        }

        String staxPort = null;
        if (unrestrictedPoliciesInstalled) {
            staxPort = StaxServer11.PORT;
        } else {
            staxPort = StaxServer11Restricted.PORT;
        }

        return Arrays.asList(new TestParam[] {
            new TestParam("A", domPort, false),
            new TestParam("A-NoTimestamp", domPort, false),
            new TestParam("AD", domPort, false),
            new TestParam("A-ES", domPort, false),
            new TestParam("AD-ES", domPort, false),
            new TestParam("UX", domPort, false),
            new TestParam("UX-NoTimestamp", domPort, false),
            new TestParam("UXD", domPort, false),
            new TestParam("UX-SEES", domPort, false),
            new TestParam("UXD-SEES", domPort, false),

            new TestParam("A", domPort, true),
            new TestParam("A-NoTimestamp", domPort, true),
            new TestParam("AD", domPort, true),
            new TestParam("A-ES", domPort, true),
            new TestParam("AD-ES", domPort, true),
            new TestParam("UX", domPort, true),
            new TestParam("UX-NoTimestamp", domPort, true),
            new TestParam("UXD", domPort, true),
            new TestParam("UX-SEES", domPort, true),
            new TestParam("UXD-SEES", domPort, true),

            new TestParam("A", staxPort, false),
            new TestParam("A-NoTimestamp", staxPort, false),
            new TestParam("AD", staxPort, false),
            new TestParam("A-ES", staxPort, false),
            new TestParam("AD-ES", staxPort, false),
            new TestParam("UX", staxPort, false),
            new TestParam("UX-NoTimestamp", staxPort, false),
            new TestParam("UXD", staxPort, false),
            new TestParam("UX-SEES", staxPort, false),
            new TestParam("UXD-SEES", staxPort, false),

            new TestParam("A", staxPort, true),
            new TestParam("A-NoTimestamp", staxPort, true),
            new TestParam("AD", staxPort, true),
            new TestParam("A-ES", staxPort, true),
            new TestParam("AD-ES", staxPort, true),
            new TestParam("UX", staxPort, true),
            new TestParam("UX-NoTimestamp", staxPort, true),
            new TestParam("UXD", staxPort, true),
            new TestParam("UX-SEES", staxPort, true),
            new TestParam("UXD-SEES", staxPort, true),
        });
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @Test
    public void testClientServer() throws IOException {
        if (!unrestrictedPoliciesInstalled) {
            System.out.println("Not running as there is a problem with 1.6 jdk and restricted jars");
            return;
        }

        runClientServer(test.prefix, test.port, unrestrictedPoliciesInstalled, test.streaming);
    }

}

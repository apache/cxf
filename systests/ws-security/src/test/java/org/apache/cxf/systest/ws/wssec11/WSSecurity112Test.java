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

import org.apache.cxf.systest.ws.wssec11.server.Server12;
import org.apache.cxf.systest.ws.wssec11.server.Server12Restricted;
import org.apache.cxf.systest.ws.wssec11.server.StaxServer12;
import org.apache.cxf.systest.ws.wssec11.server.StaxServer12Restricted;
import org.apache.cxf.test.TestUtilities;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertTrue;

/**
 * This class runs the second half of the tests, as having all in
 * the one class causes an out of memory problem in eclipse
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class WSSecurity112Test extends WSSecurity11Common {
    private static boolean unrestrictedPoliciesInstalled;

    static {
        unrestrictedPoliciesInstalled = TestUtilities.checkUnrestrictedPoliciesInstalled();
    };

    final TestParam test;

    public WSSecurity112Test(TestParam type) {
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
                    launchServer(Server12.class, true)
            );
            assertTrue(
                       "Server failed to launch",
                       // run the server in the same process
                       // set this to false to fork
                       launchServer(StaxServer12.class, true)
            );
        } else {
            assertTrue(
                    "Server failed to launch",
                    // run the server in the same process
                    // set this to false to fork
                    launchServer(Server12Restricted.class, true)
            );
            assertTrue(
                       "Server failed to launch",
                       // run the server in the same process
                       // set this to false to fork
                       launchServer(StaxServer12Restricted.class, true)
            );
        }
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam> data() {
        if (unrestrictedPoliciesInstalled) {
            return Arrays.asList(new TestParam[] {
                new TestParam("X", Server12.PORT, false),
                new TestParam("X-NoTimestamp", Server12.PORT, false),
                new TestParam("X-AES128", Server12.PORT, false),
                new TestParam("X-AES256", Server12.PORT, false),
                new TestParam("X-TripleDES", Server12.PORT, false),
                new TestParam("XD", Server12.PORT, false),
                new TestParam("XD-ES", Server12.PORT, false),
                new TestParam("XD-SEES", Server12.PORT, false),

                new TestParam("X", StaxServer12.PORT, false),
                new TestParam("X-NoTimestamp", StaxServer12.PORT, false),
                new TestParam("X-AES128", StaxServer12.PORT, false),
                new TestParam("X-AES256", StaxServer12.PORT, false),
                new TestParam("X-TripleDES", StaxServer12.PORT, false),
                new TestParam("XD", StaxServer12.PORT, false),
                new TestParam("XD-ES", StaxServer12.PORT, false),
                new TestParam("XD-SEES", StaxServer12.PORT, false),
            });
        }
        return Arrays.asList(new TestParam[] {
            new TestParam("X", Server12Restricted.PORT, false),
            new TestParam("X-NoTimestamp", Server12Restricted.PORT, false),
            new TestParam("XD", Server12Restricted.PORT, false),
            new TestParam("XD-ES", Server12Restricted.PORT, false),
            new TestParam("XD-SEES", Server12Restricted.PORT, false),

            new TestParam("X", StaxServer12Restricted.PORT, false),
            new TestParam("X-NoTimestamp", StaxServer12Restricted.PORT, false),
            new TestParam("XD", StaxServer12Restricted.PORT, false),
            new TestParam("XD-ES", StaxServer12Restricted.PORT, false),
            new TestParam("XD-SEES", StaxServer12Restricted.PORT, false),
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

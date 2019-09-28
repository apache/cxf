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

package org.apache.cxf.systest.http_jetty;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.assertTrue;

/**
 * Tests thread pool config.
 */

public class JettyCachedOutDigestAuthTest extends JettyDigestAuthTest {
    private static String oldThreshold;

    @BeforeClass
    public static void startServers() throws Exception {
        oldThreshold = System.getProperty("org.apache.cxf.io.CachedOutputStream.Threshold");
        // forces the CacheOutputStream to use temporary file caching
        System.setProperty("org.apache.cxf.io.CachedOutputStream.Threshold", "8");
        assertTrue("server did not launch correctly",
                   launchServer(JettyDigestServer.class, true));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (oldThreshold == null) {
            System.clearProperty("org.apache.cxf.io.CachedOutputStream.Threshold");
        } else {
            System.setProperty("org.apache.cxf.io.CachedOutputStream.Threshold", oldThreshold);
        }
    }

}

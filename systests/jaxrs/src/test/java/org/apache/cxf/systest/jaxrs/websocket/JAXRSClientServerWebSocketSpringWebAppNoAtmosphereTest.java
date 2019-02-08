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

package org.apache.cxf.systest.jaxrs.websocket;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * JAXRSClientServerWebSocketSpringWebAppTest without atmosphere
 */
public class JAXRSClientServerWebSocketSpringWebAppNoAtmosphereTest extends JAXRSClientServerWebSocketSpringWebAppTest {
    private static final String PORT = BookServerWebSocket.PORT2_WAR;

    @BeforeClass
    public static void startServers() throws Exception {
        System.setProperty("org.apache.cxf.transport.websocket.atmosphere.disabled", "true");
        startServers(PORT);
    }

    @AfterClass
    public static void cleanup() {
        //System.clearProperty("org.apache.cxf.transport.websocket.atmosphere.disabled");
    }

    protected String getPort() {
        return PORT;
    }
}

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

package org.apache.cxf.systest.http_undertow.websocket;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.junit.BeforeClass;



/**
 * JAXRSClientServerWebSocket test with jaxrs:server using the embedded undertow server.
 */
public class JAXRSClientServerWebSocketSpringTest extends JAXRSClientServerWebSocketTest {
    private static final String PORT = BookServerWebSocket.PORT_SPRING;
    @BeforeClass
    public static void startServers() throws Exception {

        @SuppressWarnings({ "unused", "resource" })
        ApplicationContext appctxt =
            new ClassPathXmlApplicationContext(
                JAXRSClientServerWebSocketSpringTest.class.getResource(
                    "/jaxrs_websocket/beans-embedded.xml").toString());

    }

    protected String getPort() {
        return PORT;
    }

}

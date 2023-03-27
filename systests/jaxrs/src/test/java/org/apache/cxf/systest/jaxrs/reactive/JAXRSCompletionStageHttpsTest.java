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

package org.apache.cxf.systest.jaxrs.reactive;

import java.util.List;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;

import org.junit.BeforeClass;

import static org.junit.Assert.assertTrue;

public class JAXRSCompletionStageHttpsTest extends AbstractJAXRSCompletionStageTest {
    public static final String PORT = CompletableFutureHttpsServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        createStaticBus("org/apache/cxf/systest/jaxrs/reactive/reactive-https-server.xml");
        assertTrue("server did not launch correctly",
                   launchServer(CompletableFutureHttpsServer.class, true));
    }

    @Override
    protected String getAddress() {
        return "https://localhost:" + PORT; 
    }
    

    @Override
    protected WebClient createWebClient(String address) {
        return WebClient.create(address, List.of(), "org/apache/cxf/systest/jaxrs/reactive/reactive-https-client.xml");
    }
}

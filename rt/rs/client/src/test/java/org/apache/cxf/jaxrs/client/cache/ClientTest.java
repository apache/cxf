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
package org.apache.cxf.jaxrs.client.cache;


import java.util.HashMap;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClientTest {
    public static final String ADDRESS = "local://transport";

    @Test
    public void testClientClosed() {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget target = client.target(ADDRESS);
            client.close();
            target.resolveTemplatesFromEncoded(new HashMap<String, Object>());
            fail("IllegalStateException is expected");
        } catch (java.lang.IllegalStateException e) {
            assertTrue(e.getMessage().contains("client is closed"));
        }
    }
}
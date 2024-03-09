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

package org.apache.cxf.systest.jaxrs;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.Headers;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class JAXRSClientServerEmptyBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = EmptyBookServer.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(EmptyBookServer.class, true));

        final Bus bus = createStaticBus();
        bus.setProperty(Headers.SET_EMPTY_REQUEST_CT_PROPERTY, false);
    }

    @Test
    public void testContentTypeEmptyGet() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/";
        WebClient wc = WebClient.create(address);
        final Response r = wc.get();
        assertThat(r.getStatus(), equalTo(204));
    }
    
    @Test
    public void testContentTypeEmptyDelete() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/";
        WebClient wc = WebClient.create(address);
        final Response r = wc.delete();
        assertThat(r.getStatus(), equalTo(204));
    }
}

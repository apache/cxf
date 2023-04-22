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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JAXRSClientServerSubBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerSub.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookServerSub.class, true));
        createStaticBus();
    }

    @Test
    public void testGetChapterFromBookSubObject() throws Exception {
        WebClient wc =
            WebClient.create("http://localhost:" + PORT + "/bookstore/booksubresourceobject/chaptersobject/sub/1");
        Chapter c = wc.accept("application/xml").get(Chapter.class);
        assertNotNull(c);
    }

    @Test
    public void testSubresourceLocatorFromBookSubObject() throws Exception {
        final Client c = ClientBuilder.newClient();

        // There are two matching endpoints with different HTTP methods:
        //  - POST for BookSubObject /consumeslocator"
        //  - GET for BookStore /{id}
        // The test verifies that in this case for the POST method the correct subresource
        // locator is picked.
        final WebTarget wc = c.target("http://localhost:" + PORT + "/bookstore/consumeslocator");
        try (Response r = wc.request().header("Content-Type", MediaType.APPLICATION_ATOM_XML).post(null)) {
            assertThat(r.getStatus(), equalTo(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode()));
            assertThat(Integer.toString(r.getStatus()), equalTo(r.readEntity(String.class)));
        }
    }
}

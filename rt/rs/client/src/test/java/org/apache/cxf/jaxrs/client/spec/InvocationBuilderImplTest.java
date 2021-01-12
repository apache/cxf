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
package org.apache.cxf.jaxrs.client.spec;

import java.io.IOException;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InvocationBuilderImplTest {

    public static class TestFilter implements ClientRequestFilter {

        /** {@inheritDoc}*/
        @Override
        public void filter(ClientRequestContext context) throws IOException {
            MultivaluedMap<String, Object> headers = context.getHeaders();
            StringBuilder entity = new StringBuilder();
            for (String key : headers.keySet()) {
                entity.append(key).append('=').append(headers.getFirst(key)).append(';');
            }
            context.abortWith(Response.ok(entity.toString()).build());
        }
        
    }

    @Test
    public void testHeadersMethod() {
        // the javadoc for the Invocation.Builder.headers(MultivaluedMap) method says that
        // invoking this method should remove all previously existing headers
        Client client = ClientBuilder.newClient().register(TestFilter.class);
        Builder builder = client.target("http://localhost:8080/notReal").request();
        builder.header("Header1", "a");
        builder.header("UnexpectedHeader", "should be removed");
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.putSingle("Header1", "b");
        builder.headers(map);

        Response response = builder.get();
        String sentHeaders = response.readEntity(String.class);
        assertTrue(sentHeaders.contains("Header1=b"));
        assertFalse(sentHeaders.contains("UnexpectedHeader"));

        // If value is null then all current headers of the same name 
        // should be removed.
        builder.header("Header1", null);
        builder.header("Header2", "b");
        response = builder.get();
        sentHeaders = response.readEntity(String.class);
        assertTrue(sentHeaders.contains("Header2=b"));
        assertFalse(sentHeaders.contains("Header1"));
        
        // null headers map should clear all headers
        builder.headers(null);
        response = builder.get();
        assertEquals("", response.readEntity(String.class));
    }
}

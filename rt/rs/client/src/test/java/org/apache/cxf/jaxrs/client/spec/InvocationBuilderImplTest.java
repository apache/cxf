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
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.Headers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InvocationBuilderImplTest {

    private static final String FILTER_PROPS_KEY = "jaxrs.filter.properties";

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

    /**
     * CXF-9235: Invocation.Builder.property() must write the property value into BOTH:
     *
     * (a) the nested "jaxrs.filter.properties" sub-map inside the ClientConfiguration
     *     request context — this is what context.getProperty() reads inside a
     *     ClientRequestFilter (via MessagePropertyHolder / Exchange.get(PROPERTY_KEY)).
     *
     * (b) the flat top-level ClientConfiguration request context — this is what the
     *     HTTP transport reads via Message.getContextualProperty() in
     *     Headers.setProtocolHeadersInConnection().
     *
     * Before the fix only (a) was written.  The transport (b) path was silently missing,
     * meaning a property like "set.content.type.for.empty.request" set on the
     * Invocation.Builder had no effect on the conduit.
     */
    @Test
    public void testPropertyWrittenToBothFilterPropsAndFlatContext() {
        Client client = ClientBuilder.newClient().register(TestFilter.class);
        Builder builder = client.target("http://localhost:8080/notReal").request();

        // ---- set ----
        builder.property(Headers.SET_EMPTY_REQUEST_CT_PROPERTY, Boolean.FALSE);

        InvocationBuilderImpl builderImpl = (InvocationBuilderImpl) builder;
        Map<String, Object> requestContext =
                WebClient.getConfig(builderImpl.getWebClient()).getRequestContext();

        // (a) Must be in the nested filterProps sub-map that a ClientRequestFilter reads.
        //     This path was already written before the fix; we guard it stays working.
        Map<String, Object> filterProps =
                CastUtils.cast((Map<?, ?>) requestContext.get(FILTER_PROPS_KEY));
        assertTrue("jaxrs.filter.properties sub-map must exist after Builder.property()",
                filterProps != null && filterProps.containsKey(Headers.SET_EMPTY_REQUEST_CT_PROPERTY));
        assertEquals("Value in filterProps must match what was set",
                Boolean.FALSE, filterProps.get(Headers.SET_EMPTY_REQUEST_CT_PROPERTY));

        // (b) Must also be present flat in the top-level context — the path the HTTP
        //     transport reads via Message.getContextualProperty().  This was the bug in
        //     CXF-9235: only (a) was written, so the transport never saw the property.
        assertEquals("Flat request context must contain the property for the transport layer (CXF-9235)",
                Boolean.FALSE, requestContext.get(Headers.SET_EMPTY_REQUEST_CT_PROPERTY));

        // ---- remove (null value) ----
        builder.property(Headers.SET_EMPTY_REQUEST_CT_PROPERTY, null);

        // (a) removed from filterProps
        assertNull("Null value must remove property from filterProps sub-map",
                filterProps.get(Headers.SET_EMPTY_REQUEST_CT_PROPERTY));

        // (b) removed from flat context
        assertFalse("Null value must remove property from the flat transport context (CXF-9235)",
                requestContext.containsKey(Headers.SET_EMPTY_REQUEST_CT_PROPERTY));
    }
}

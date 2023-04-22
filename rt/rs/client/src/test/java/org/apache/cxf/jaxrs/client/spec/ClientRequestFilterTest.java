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

import jakarta.annotation.Priority;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Response;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClientRequestFilterTest {

    public static class NonStringObject {
        private String theString;

        NonStringObject(String s) {
            theString = s;
        }

        @Override
        public String toString() {
            return "hello " + theString;
        }
    }

    @Priority(1)
    public static class AddNonStringHeaderFilter implements ClientRequestFilter {

        /** {@inheritDoc}*/
        @Override
        public void filter(ClientRequestContext context) throws IOException {
            context.getHeaders().add("MyHeader", new NonStringObject("rabbit"));
        }
    }

    @Priority(2)
    public static class TestHeaderFilter implements ClientRequestFilter {

        /** {@inheritDoc}*/
        @Override
        public void filter(ClientRequestContext context) throws IOException {
            Object o = context.getHeaders().getFirst("MyHeader");
            Response r = (o instanceof String) ? Response.ok(o).build() : Response.serverError().build();
            context.abortWith(r);
        }
    }

    @Test
    public void testHeaderObjectInFilterIsConvertedToString() {
        Response response = ClientBuilder.newClient()
                                         .register(AddNonStringHeaderFilter.class)
                                         .register(TestHeaderFilter.class)
                                         .target("http://localhost:8080/notReally")
                                         .request()
                                         .get();
        assertEquals(200, response.getStatus());
        assertEquals("hello rabbit", response.readEntity(String.class));
    }
}
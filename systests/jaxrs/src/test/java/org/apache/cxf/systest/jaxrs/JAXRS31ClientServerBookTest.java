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

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.ContextResolver;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static jakarta.ws.rs.RuntimeType.CLIENT;
import static jakarta.ws.rs.RuntimeType.SERVER;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

public class JAXRS31ClientServerBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServer31.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(BookServer31.class, true));
    }

    @Test
    public final void shouldUseApplicationProvidedJsonbInstance() throws URISyntaxException {
        final String origin = String.format("Origin(%d)", ThreadLocalRandom.current().nextInt(1000));
        try (Client client = ClientBuilder
                .newBuilder()
                .register(new CustomJsonbProvider(CLIENT))
                .build()) {

            final Book book = new Book();
            book.setName(origin);

            final URI effectiveUri = UriBuilder
                .fromUri("http://localhost:" + PORT + "/")
                .path("bookstore")
                .build();

            final Book response = client
                .target(effectiveUri)
                .request(APPLICATION_JSON_TYPE)
                .buildPost(Entity.entity(book, APPLICATION_JSON_TYPE))
                .invoke(Book.class);

            final String expectedWaypoints = String.join(",", origin,
                "CustomSerializer(CLIENT)",
                "CustomDeserializer(SERVER)",
                "BookResource",
                "CustomSerializer(SERVER)",
                "CustomDeserializer(CLIENT)");

            assertThat(response.getName(), is(expectedWaypoints));
        }
    }
    
    public static final class BookServer31 extends AbstractServerTestServerBase {
        public static final String PORT = allocatePort(BookServer31.class);

        @Override
        protected Server createServer(Bus bus) throws Exception {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStoreResource.class);
            sf.setProvider(new CustomJsonbProvider(SERVER));
            sf.setAddress("http://localhost:" + PORT + "/");
            return sf.create();
        }

        public static void main(String[] args) throws Exception {
            new BookServer31().start();
        }
    }

    @Path("/bookstore")
    public static class BookStoreResource {
        @POST
        @Consumes(APPLICATION_JSON)
        @Produces(APPLICATION_JSON)
        public Book echo(final Book book) {
            book.setName(String.join(",", book.getName(), "BookResource"));
            return book;
        }
    }

    public static final class CustomJsonbProvider implements ContextResolver<Jsonb> {
        private final RuntimeType runtimeType;

        private CustomJsonbProvider(final RuntimeType runtimeType) {
            this.runtimeType = runtimeType;
        }

        public Jsonb getContext(final Class<?> type) {
            if (!Book.class.isAssignableFrom(type)) {
                return null;
            }

            return JsonbBuilder.create(new JsonbConfig()
                .withSerializers(new CustomSerializer())
                .withDeserializers(new CustomDeserializer()));
        }

        private final class CustomSerializer implements JsonbSerializer<Book> {
            @Override
            public void serialize(final Book book, final JsonGenerator generator, final SerializationContext ctx) {
                generator.writeStartObject();
                generator.write("name", String.format("%s,CustomSerializer(%s)", book.getName(),
                    CustomJsonbProvider.this.runtimeType));
                generator.writeEnd();
            }
        }

        private final class CustomDeserializer implements JsonbDeserializer<Book> {
            @Override
            public Book deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
                final Book book = new Book();
                book.setName(String.format("%s,CustomDeserializer(%s)", parser.getObject().getString("name"), 
                    CustomJsonbProvider.this.runtimeType));
                return book;
            }
        }
    }
}

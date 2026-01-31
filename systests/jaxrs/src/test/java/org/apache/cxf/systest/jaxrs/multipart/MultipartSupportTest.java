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

package org.apache.cxf.systest.jaxrs.multipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Adapted from ee.jakarta.tck.ws.rs.jaxrs31.ee.multipart.MultipartSupportIT
 */
public class MultipartSupportTest extends AbstractBusClientServerTestBase {
    public static final String PORT = MultipartSupportServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(MultipartSupportServer.class, true));
    }
    
    public static final class MultipartSupportServer extends AbstractServerTestServerBase {
        public static final String PORT = allocatePort(MultipartSupportServer.class);

        @Override
        protected Server createServer(Bus bus) throws Exception {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(TestResource.class);
            sf.setAddress("http://localhost:" + PORT + "/");
            return sf.create();
        }

        public static void main(String[] args) throws Exception {
            new MultipartSupportServer().start();
        }
    }

    private static InputStream xmlFile() {
        final String xml = "<root>" + System.lineSeparator() 
            + "  <mid attr1=\"value1\" attr2=\"value2\">" + System.lineSeparator()
            + "    <inner attr3=\"value3\"/>" + System.lineSeparator()
            + "  </mid>" + System.lineSeparator()
            + "  <mid attr1=\"value4\" attr2=\"value5\">" + System.lineSeparator()
            + "    <inner attr3=\"value6\"/>" + System.lineSeparator()
            + "  </mid>" + System.lineSeparator()
            + "</root>";
        return new ByteArrayInputStream(xml.getBytes());
    }

    @Test
    public void basicTest() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final List<EntityPart> multipart = List.of(
                    EntityPart.withName("octet-stream")
                        .content("test string".getBytes(StandardCharsets.UTF_8))
                        .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                        .build(),
                    EntityPart.withName("file")
                        .content("test file", xmlFile())
                        .mediaType(MediaType.APPLICATION_XML)
                        .build()
                );

            try (Response response = client.target("http://localhost:" + PORT + "/test/basicTest")
                    .request(MediaType.MULTIPART_FORM_DATA_TYPE)
                    .post(Entity.entity(new GenericEntity<>(multipart) { }, MediaType.MULTIPART_FORM_DATA))) {

                Assert.assertEquals(200, response.getStatus());
                final List<EntityPart> entityParts = response.readEntity(new GenericType<>() {
                });

                if (entityParts.size() != 3) {
                    Assert.fail("Expected 3 entries, received " + entityParts.size() + '.');
                }

                EntityPart part = find(entityParts, "received-string");
                Assert.assertNotNull(part);
                Assert.assertEquals("test string", part.getContent(String.class));

                // The javadoc for EntityPart.getContent(Class<T> type) states:
                // "Subsequent invocations will result in an {@code IllegalStateException}. 
                // Likewise this method will throw an {@code IllegalStateException} if it is 
                // called after calling {@link #getContent} or {@link #getContent(GenericType)}.
                try {
                    part.getContent(String.class);
                    Assert.fail("IllegalStateException is expected when getContent() is invoked more than once.");
                } catch (IllegalStateException e) {
                    // expected exception
                } catch (Throwable t) {
                    Assert.fail("Incorrect Throwable received: " + t);
                }
                
                part = find(entityParts, "received-file");
                Assert.assertNotNull(part);
                Assert.assertEquals("test file", part.getFileName().get());
                Assert.assertTrue(part.getContent(String.class).contains("value6"));

                part = find(entityParts, "added-input-stream");
                Assert.assertNotNull(part);
                Assert.assertEquals("Add part on return.", part.getContent(String.class));

                // Check headers.  Should be 5:  Content-Disposition, Content-Type,
                // and the 2 headers that were added.
                if ((part.getHeaders() == null) || (part.getHeaders().size() != 4)) {
                    Assert.fail("Expected 4 headers, received " + part.getHeaders().size());
                }

                Assert.assertEquals("Test1", part.getHeaders().get("Header1").get(0));
                Assert.assertEquals("Test2", part.getHeaders().get("Header2").get(0));
            }
        }
    }

    @Test
    public void multiFormParamTest() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final List<EntityPart> multipart = List.of(
                    EntityPart.withName("entity-part")
                            .content("test entity part")
                            .mediaType(MediaType.TEXT_PLAIN_TYPE)
                            .build(),
                    EntityPart.withName("string-part")
                            .content("test string")
                            .mediaType(MediaType.TEXT_PLAIN_TYPE)
                            .build(),
                    EntityPart.withName("input-stream-part")
                            .content("test input stream".getBytes(StandardCharsets.UTF_8))
                            .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                            .build()
                );

            try (Response response = client.target("http://localhost:" + PORT + "/test/multi-form-param")
                    .request(MediaType.MULTIPART_FORM_DATA_TYPE)
                    .post(Entity.entity(new GenericEntity<>(multipart) { }, MediaType.MULTIPART_FORM_DATA))) {
                Assert.assertEquals(200, response.getStatus());
                final List<EntityPart> entityParts = response.readEntity(new GenericType<>() { });
                if (entityParts.size() != 3) {
                    Assert.fail("Expected 3 entries, received " + entityParts.size() + '.');
                }
                verifyEntityPart(entityParts, "received-entity-part", "test entity part");
                verifyEntityPart(entityParts, "received-string", "test string");
                verifyEntityPart(entityParts, "received-input-stream", "test input stream");
            }
        }
    }
    
    @Test
    public void singleFormParamTest() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final List<EntityPart> multipart = List.of(
                    EntityPart.withName("entity-part")
                        .content("test entity part")
                        .mediaType(MediaType.TEXT_PLAIN_TYPE)
                        .build()
                );

            try (Response response = client.target("http://localhost:" + PORT + "/test/single-form-param")
                    .request(MediaType.MULTIPART_FORM_DATA_TYPE)
                    .post(Entity.entity(new GenericEntity<>(multipart) { }, MediaType.MULTIPART_FORM_DATA))) {
                Assert.assertEquals(200, response.getStatus());
                final List<EntityPart> entityParts = response.readEntity(new GenericType<>() { });
                if (entityParts.size() != 1) {
                    Assert.fail("Expected 1 entries, received " + entityParts.size() + '.');
                }
                verifyEntityPart(entityParts, "received-entity-part", "test entity part");
            }
        }
    }
    
    @Test
    public void singleTest() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final List<EntityPart> multipart = List.of(
                    EntityPart.withName("entity-part")
                        .content(new ByteArrayInputStream("test entity part".getBytes(StandardCharsets.UTF_8)))
                        .mediaType(MediaType.TEXT_PLAIN_TYPE)
                        .build()
                );

            try (Response response = client.target("http://localhost:" + PORT + "/test/single-param")
                    .request(MediaType.MULTIPART_FORM_DATA_TYPE)
                    .post(Entity.entity(new GenericEntity<>(multipart) { }, MediaType.MULTIPART_FORM_DATA))) {
                Assert.assertEquals(200, response.getStatus());
                final List<EntityPart> entityParts = response.readEntity(new GenericType<>() { });
                if (entityParts.size() != 1) {
                    Assert.fail("Expected 1 entries, received " + entityParts.size() + '.');
                }
                verifyEntityPart(entityParts, "received-entity-part", "test entity part");
            }
        }
    }
    
    @Test
    public void fileTest() throws Exception {
        final String resource = "/org/apache/cxf/systest/jaxrs/resources/add_book.txt";
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            final byte[] content = is.readAllBytes();
            try (Client client = ClientBuilder.newClient()) {
                final List<EntityPart> multipart = List.of(
                        EntityPart.withFileName("file-part")
                            .content(new ByteArrayInputStream(content))
                            .mediaType(MediaType.APPLICATION_XML_TYPE)
                            .build()
                    );
    
                try (Response response = client.target("http://localhost:" + PORT + "/test/file-param")
                        .request(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .post(Entity.entity(new GenericEntity<>(multipart) { }, MediaType.MULTIPART_FORM_DATA))) {
                    Assert.assertEquals(200, response.getStatus());
                    final List<EntityPart> entityParts = response.readEntity(new GenericType<>() { });
                    if (entityParts.size() != 1) {
                        Assert.fail("Expected 1 entries, received " + entityParts.size() + '.');
                    }
                    verifyEntityPart(entityParts, "received-file-part", new String(content, StandardCharsets.UTF_8));
                }
            }
        }
    }

    private static void verifyEntityPart(final List<EntityPart> parts, final String name, final String text)
            throws IOException {
        final EntityPart part = find(parts, name);
        Assert.assertNotNull(part);
        Assert.assertEquals(text, part.getContent(String.class));
    }
    
    private static String toString(final InputStream in) throws IOException {
        // try-with-resources fails here due to a bug in the
        //noinspection TryFinallyCanBeTryWithResources
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[32];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return out.toString(StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    private static EntityPart find(final Collection<EntityPart> parts, final String name) {
        for (EntityPart part : parts) {
            if (name.equals(part.getName())) {
                return part;
            }
        }
        return null;
    }

    @Path("/test")
    public static class TestResource {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/basicTest")
        public Response basicTest(final List<EntityPart> parts) throws IOException {
            final List<EntityPart> multipart = List.of(
                    EntityPart.withName("received-string")
                        .content(find(parts, "octet-stream").getContent(byte[].class))
                        .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                        .build(),
                    EntityPart.withName("received-file")
                        .content(find(parts, "file").getFileName().get(), find(parts, "file").getContent())
                        .mediaType(MediaType.APPLICATION_XML)
                        .build(),
                    EntityPart.withName("added-input-stream")
                        .content(new ByteArrayInputStream("Add part on return.".getBytes()))
                        .mediaType("text/asciidoc")
                        .header("Header1", "Test1")
                        .header("Header2", "Test2")
                        .build());
            return Response.ok(new GenericEntity<>(multipart) { }, MediaType.MULTIPART_FORM_DATA).build();
        }

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/multi-form-param")
        public Response multipleFormParamTest(@FormParam("string-part") final String string,
                @FormParam("entity-part") final EntityPart entityPart,
                @FormParam("input-stream-part") final InputStream in) throws IOException {
            final List<EntityPart> multipart = List.of(
                    EntityPart.withName("received-entity-part")
                        .content(entityPart.getContent(String.class))
                        .mediaType(entityPart.getMediaType())
                        .fileName(entityPart.getFileName().orElse(null))
                        .build(),
                    EntityPart.withName("received-input-stream")
                        .content(MultipartSupportTest.toString(in).getBytes(StandardCharsets.UTF_8))
                        .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                        .build(),
                    EntityPart.withName("received-string")
                        .content(string)
                        .mediaType(MediaType.TEXT_PLAIN_TYPE)
                        .build());
            return Response.ok(new GenericEntity<>(multipart) { }, MediaType.MULTIPART_FORM_DATA).build();
        }

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/single-form-param")
        public Response singleFormParamTest(@FormParam("entity-part") final EntityPart entityPart) throws IOException {
            final List<EntityPart> multipart = List.of(
                    EntityPart.withName("received-entity-part")
                        .content(entityPart.getContent(String.class))
                        .mediaType(entityPart.getMediaType())
                        .fileName(entityPart.getFileName().orElse(null))
                        .build());
            return Response.ok(new GenericEntity<>(multipart) { }, MediaType.MULTIPART_FORM_DATA).build();
        }

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/single-param")
        public Response singleParamTest(final List<EntityPart> parts) throws IOException {
            final List<EntityPart> multipart = List.of(
                    EntityPart.withName("received-entity-part")
                        .content(find(parts, "entity-part").getContent(byte[].class))
                        .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                        .build());
            return Response.ok(new GenericEntity<>(multipart) { }, MediaType.MULTIPART_FORM_DATA).build();
        }
        
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.MULTIPART_FORM_DATA)
        @Path("/file-param")
        public Response fileParamTest(@FormParam("file") final EntityPart filePart) throws IOException {
            final List<EntityPart> multipart = List.of(
                    EntityPart.withFileName("received-file-part")
                        .content(filePart.getContent(byte[].class))
                        .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                        .build());
            return Response.ok(new GenericEntity<>(multipart) { }, MediaType.MULTIPART_FORM_DATA).build();
        }
    }
}
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

import java.io.File;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.util.List;

import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.utils.ParameterizedListType;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSEntityPartTest extends AbstractBusClientServerTestBase {
    public static final String PORT = EntityPartServer.PORT;
    public static final String PORTINV = allocatePort(JAXRSEntityPartTest.class, 1);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(EntityPartServer.class, true));
        createStaticBus();
    }

    @Test
    public void testUploadImage() throws Exception {
        final File file = new File(getClass().getResource("/org/apache/cxf/systest/jaxrs/resources/java.jpg")
                .toURI().getPath());
        final String address = "http://localhost:" + PORT + "/bookstore/books/images";
        final WebClient client = WebClient.create(address);
        client.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.MULTIPART_FORM_DATA);

        try (InputStream is = Files.newInputStream(file.toPath())) {
            final EntityPart part = EntityPart
                .withFileName(file.getName())
                .content(is)
                .build();

            try (Response response = client.postCollection(List.of(part), EntityPart.class)) {
                assertThat(response.getStatus(), equalTo(200));

                @SuppressWarnings("unchecked")
                final List<EntityPart> parts = (List<EntityPart>) response
                    .readEntity(new GenericType<>(new ParameterizedListType(EntityPart.class)));

                assertThat(parts, hasSize(1));
                assertThat(parts.get(0), is(not(nullValue())));

                assertThat(parts.get(0).getFileName().isPresent(), is(true));
                assertThat(parts.get(0).getFileName().get(), equalTo(part.getFileName().get()));

                assertArrayEquals(IOUtils.readBytesFromStream(parts.get(0).getContent()), 
                    IOUtils.readBytesFromStream(Files.newInputStream(file.toPath())));
            }
        }
    }
    
    @Test
    public void testBookJSONForm() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jsonform";
        doAddFormBook(address, "attachmentFormJson", "gazetteer", MediaType.APPLICATION_JSON, 200);
    }

    @Test
    public void testBookJSONJAXBForm() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jsonjaxbform";
        
        final WebClient client = WebClient.create(address);
        client.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_XML);

        try (InputStream is1 = 
                getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/attachmentFormJaxb");
            InputStream is2 = 
                getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/attachmentFormJson")) {

            try (Response response = client.post(new SequenceInputStream(is1, is2))) {
                assertThat(response.getStatus(), equalTo(200));

                try (InputStream expected = getClass().getResourceAsStream("resources/expected_add_book.txt")) {
                    assertEquals(stripXmlInstructionIfNeeded(IOUtils.toString(expected)),
                       stripXmlInstructionIfNeeded(response.readEntity(String.class)));
                }
            }
        }
    }

    @Test
    public void testBookJaxbForm() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/jaxbform";
        doAddFormBook(address, "attachmentFormJaxb", "bookXML", MediaType.APPLICATION_XML, 200);
    }

    private void doAddFormBook(String address, String resourceName, 
            String name, String mt, int status) throws Exception {

        final WebClient client = WebClient.create(address);
        client.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_XML);

        try (InputStream is = 
                getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/" + resourceName)) {
            
            try (Response response = client.post(is)) {
                assertThat(response.getStatus(), equalTo(200));

                try (InputStream expected = getClass().getResourceAsStream("resources/expected_add_book.txt")) {
                    assertEquals(stripXmlInstructionIfNeeded(IOUtils.toString(expected)),
                       stripXmlInstructionIfNeeded(response.readEntity(String.class)));
                }
            }
        }
    }
        
    private static String stripXmlInstructionIfNeeded(String str) {
        if (str != null && str.startsWith("<?xml")) {
            int index = str.indexOf("?>");
            str = str.substring(index + 2);
        }
        return str;
    }
}

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.MultipartProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(value = org.junit.runners.Parameterized.class)
public class JAXRSClientChunkingTest extends AbstractBusClientServerTestBase {
    private static final String PORT = allocatePort(FileStoreServer.class);
    private final Boolean chunked;

    public JAXRSClientChunkingTest(Boolean chunked) {
        this.chunked = chunked;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(new FileStoreServer(PORT)));
        createStaticBus();
    }
    
    @Parameters(name = "{0}")
    public static Collection<Boolean> data() {
        return Arrays.asList(new Boolean[] {Boolean.FALSE, Boolean.TRUE});
    }

    @Test
    public void testMultipartChunking() {
        final String url = "http://localhost:" + PORT + "/file-store";
        final WebClient webClient = WebClient.create(url, List.of(new MultipartProvider())).query("chunked", chunked);
        WebClient.getConfig(webClient).getHttpConduit().getClient().setAllowChunking(chunked);

        try {
            final String filename = "keymanagers.jks";
            final MultivaluedMap<String, String> headers = new MetadataMap<>();
            headers.add("Content-ID", filename);
            headers.add("Content-Type", "application/binary");
            headers.add("Content-Disposition", "attachment; filename=" + chunked + "_" + filename);
            final Attachment att = new Attachment(getClass().getResourceAsStream("/" + filename), headers);
            final MultipartBody entity = new MultipartBody(att);
            try (Response response = webClient.header("Content-Type", "multipart/form-data").post(entity)) {
                assertThat(response.getStatus(), equalTo(201));
            }
        } finally {
            webClient.close();
        }
    }
    
    @Test
    public void testStreamChunking() throws IOException {
        final String url = "http://localhost:" + PORT + "/file-store/stream";
        final WebClient webClient = WebClient.create(url).query("chunked", chunked);
        WebClient.getConfig(webClient).getHttpConduit().getClient().setAllowChunking(chunked);

        final byte[] bytes = new byte [32 * 1024];
        final Random random = new Random();
        random.nextBytes(bytes);

        try (InputStream in = new ByteArrayInputStream(bytes)) {
            final Entity<InputStream> entity = Entity.entity(in, MediaType.APPLICATION_OCTET_STREAM);
            try (Response response = webClient.post(entity)) {
                assertThat(response.getStatus(), equalTo(200));
                assertThat(response.getHeaderString("Transfer-Encoding"), equalTo(chunked ? "chunked" : null));
                assertThat(response.getEntity(), not(equalTo(null)));
            }
        } finally {
            webClient.close();
        }
    }
}

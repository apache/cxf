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

package org.apache.cxf.systest.hc5.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.MultipartProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.asyncclient.hc5.AsyncHTTPConduit;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(value = org.junit.runners.Parameterized.class)
public class JAXRSAsyncClientChunkingTest extends AbstractBusClientServerTestBase {
    private static final String PORT = allocatePort(FileStoreServer.class);
    private final Boolean chunked;
    private final Boolean autoRedirect;
    private final ConcurrentMap<String, AtomicInteger> ids = new ConcurrentHashMap<>();

    public JAXRSAsyncClientChunkingTest(Boolean chunked, Boolean autoRedirect) {
        this.chunked = chunked;
        this.autoRedirect = autoRedirect;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(new FileStoreServer(PORT)));
        createStaticBus();
    }
    
    @Parameters(name = "chunked {0}, auto-redirect {1}")
    public static Collection<Boolean[]> data() {
        return Arrays.asList(new Boolean[][] {
            {Boolean.FALSE /* chunked */, Boolean.FALSE /* autoredirect */},
            {Boolean.FALSE /* chunked */, Boolean.TRUE /* autoredirect */},
            {Boolean.TRUE /* chunked */, Boolean.FALSE /* autoredirect */},
            {Boolean.TRUE /* chunked */, Boolean.TRUE /* autoredirect */},
        });
    }

    @Test
    public void testMultipartChunking() {
        final String url = "http://localhost:" + PORT + "/file-store";
        final WebClient webClient = WebClient.create(url, List.of(new MultipartProvider())).query("chunked", chunked);

        final ClientConfiguration config = WebClient.getConfig(webClient);
        config.getBus().setProperty(AsyncHTTPConduit.USE_ASYNC, true);
        config.getHttpConduit().getClient().setAllowChunking(chunked);
        config.getHttpConduit().getClient().setAutoRedirect(autoRedirect);
        configureLogging(config);

        final String filename = "Morpit.jks";
        try {
            final MultivaluedMap<String, String> headers = new MetadataMap<>();
            headers.add("Content-ID", filename);
            headers.add("Content-Type", "application/binary");
            headers.add("Content-Disposition", "attachment; filename=" + chunked + "_" + autoRedirect + "_" + filename);
            final Attachment att = new Attachment(getClass().getResourceAsStream("/keys/" + filename), headers);
            final MultipartBody entity = new MultipartBody(att);
            try (Response response = webClient.header("Content-Type", MediaType.MULTIPART_FORM_DATA).post(entity)) {
                assertThat(response.getStatus(), equalTo(201));
                assertThat(response.getHeaderString("Transfer-Encoding"), equalTo(chunked ? "chunked" : null));
                assertThat(response.getEntity(), not(equalTo(null)));
            }
        } finally {
            webClient.close();
        }

        assertRedirect(chunked + "_" + autoRedirect + "_" + filename);
    }

    @Test
    public void testMultipartChunkingAsync() throws InterruptedException, ExecutionException, TimeoutException {
        final String url = "http://localhost:" + PORT + "/file-store";
        final WebClient webClient = WebClient.create(url, List.of(new MultipartProvider())).query("chunked", chunked);

        final ClientConfiguration config = WebClient.getConfig(webClient);
        config.getBus().setProperty(AsyncHTTPConduit.USE_ASYNC, true);
        config.getHttpConduit().getClient().setAllowChunking(chunked);
        config.getHttpConduit().getClient().setAutoRedirect(autoRedirect);
        configureLogging(config);

        final String filename = "Morpit.jks";
        try {
            final MultivaluedMap<String, String> headers = new MetadataMap<>();
            headers.add("Content-ID", filename);
            headers.add("Content-Type", "application/binary");
            headers.add("Content-Disposition", "attachment; filename=" + chunked
                +  "_" + autoRedirect + "_async_" + filename);
            final Attachment att = new Attachment(getClass().getResourceAsStream("/keys/" + filename), headers);
            final Entity<MultipartBody> entity = Entity.entity(new MultipartBody(att), 
                    MediaType.MULTIPART_FORM_DATA_TYPE);
            try (Response response = webClient.header("Content-Type", MediaType.MULTIPART_FORM_DATA).async()
                    .post(entity).get(10, TimeUnit.SECONDS)) {
                assertThat(response.getStatus(), equalTo(201));
                assertThat(response.getHeaderString("Transfer-Encoding"), equalTo(chunked ? "chunked" : null));
                assertThat(response.getEntity(), not(equalTo(null)));
            }
        } finally {
            webClient.close();
        }

        assertRedirect(chunked + "_" + autoRedirect + "_" + filename);
    }

    @Test
    public void testStreamChunking() throws IOException {
        final String url = "http://localhost:" + PORT + "/file-store/stream";
        final WebClient webClient = WebClient.create(url).query("chunked", chunked);
        
        final ClientConfiguration config = WebClient.getConfig(webClient);
        config.getBus().setProperty(AsyncHTTPConduit.USE_ASYNC, true);
        config.getHttpConduit().getClient().setAllowChunking(chunked);
        config.getHttpConduit().getClient().setAutoRedirect(autoRedirect);
        configureLogging(config);

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

        assertNoDuplicateLogging();
    }

    @Test
    public void testStreamChunkingAsync() throws IOException, InterruptedException,
            ExecutionException, TimeoutException {
        final String url = "http://localhost:" + PORT + "/file-store/stream";
        final WebClient webClient = WebClient.create(url).query("chunked", chunked);
        
        final ClientConfiguration config = WebClient.getConfig(webClient);
        config.getBus().setProperty(AsyncHTTPConduit.USE_ASYNC, true);
        config.getHttpConduit().getClient().setAllowChunking(chunked);
        config.getHttpConduit().getClient().setAutoRedirect(autoRedirect);
        configureLogging(config);

        final byte[] bytes = new byte [32 * 1024];
        final Random random = new Random();
        random.nextBytes(bytes);

        try (InputStream in = new ByteArrayInputStream(bytes)) {
            final Entity<InputStream> entity = Entity.entity(in, MediaType.APPLICATION_OCTET_STREAM);
            try (Response response = webClient.async().post(entity).get(10, TimeUnit.SECONDS)) {
                assertThat(response.getStatus(), equalTo(200));
                assertThat(response.getHeaderString("Transfer-Encoding"), equalTo(chunked ? "chunked" : null));
                assertThat(response.getEntity(), not(equalTo(null)));
            }
        } finally {
            webClient.close();
        }

        assertNoDuplicateLogging();
    }

    private void assertRedirect(String filename) {
        final String url = "http://localhost:" + PORT + "/file-store/redirect";

        final WebClient webClient = WebClient.create(url, List.of(new MultipartProvider()))
                .query("chunked", chunked)
                .query("filename", filename);

        final ClientConfiguration config = WebClient.getConfig(webClient);
        config.getBus().setProperty(AsyncHTTPConduit.USE_ASYNC, true);
        config.getHttpConduit().getClient().setAllowChunking(chunked);
        config.getHttpConduit().getClient().setAutoRedirect(autoRedirect);
        configureLogging(config);

        try {
            try (Response response = webClient.get()) {
                if (autoRedirect) {
                    assertThat(response.getStatus(), equalTo(200));
                    assertThat(response.getHeaderString("Transfer-Encoding"), equalTo(chunked ? "chunked" : null));
                    assertThat(response.getEntity(), not(equalTo(null)));
                } else {
                    assertThat(response.getStatus(), equalTo(303));
                    assertThat(response.getHeaderString("Location"),
                        startsWith("http://localhost:" + PORT + "/file-store"));
                }
            }
        } finally {
            webClient.close();
        }

        assertNoDuplicateLogging();
    }

    private void assertNoDuplicateLogging() {
        ids.forEach((id, counter) -> assertThat("Duplicate client logging for message " + id,
            counter.get(), equalTo(1)));
    }

    private void configureLogging(final ClientConfiguration config) {
        final LoggingOutInterceptor out = new LoggingOutInterceptor();
        out.setShowMultipartContent(false);

        final LoggingInInterceptor in = new LoggingInInterceptor() {
            @Override
            protected void logging(Logger logger, Message message) {
                super.logging(logger, message);
                final String id = (String) message.get(LoggingMessage.ID_KEY);
                ids.computeIfAbsent(id, key -> new AtomicInteger()).incrementAndGet(); 
            }
        };
        in.setShowBinaryContent(false);

        config.getInInterceptors().add(in);
        config.getOutInterceptors().add(out);
    }
}

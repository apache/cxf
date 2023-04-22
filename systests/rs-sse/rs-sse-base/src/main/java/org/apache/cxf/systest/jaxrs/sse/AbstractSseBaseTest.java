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
package org.apache.cxf.systest.jaxrs.sse;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

public abstract class AbstractSseBaseTest extends AbstractBusClientServerTestBase {
    private final ObjectMapper mapper = new ObjectMapper();

    protected String toJson(final String name, final Integer id) throws JsonProcessingException {
        return mapper.writeValueAsString(new Book(name, id));
    }

    protected WebClient createWebClient(final String url, final String media) {
        final List< ? > providers = Arrays.asList(new JacksonJsonProvider());

        final WebClient wc = WebClient
            .create("http://localhost:" + getPort() + url, providers)
            .accept(media);

        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(8000L);
        return wc;
    }

    protected WebClient createWebClient(final String url) {
        return createWebClient(url, MediaType.SERVER_SENT_EVENTS);
    }

    protected WebTarget createWebTarget(final String url) {
        return ClientBuilder
            .newClient()
            .property("http.receive.timeout", 8000)
            .register(JacksonJsonProvider.class)
            .target("http://localhost:" + getPort() + url);
    }
    
    protected void awaitEvents(int timeout, final Collection<?> events, int size) throws InterruptedException {
        final int sleep = timeout / 10;
        
        for (int i = 0; i < timeout; i += sleep) {
            if (events.size() == size) {
                break;
            } else {
                Thread.sleep(sleep);
            }
        }
    }

    protected abstract int getPort();
}

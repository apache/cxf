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

package demo.jaxrs.tracing.client;

import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import brave.httpclient.TracingHttpClientBuilder;
import demo.jaxrs.tracing.server.CatalogTracing;

public final class ApacheHttpClient {
    private ApacheHttpClient() {
    }

    public static void main(final String[] args) throws Exception {
        try (final CatalogTracing tracing = new CatalogTracing("catalog-client")) {
            final CloseableHttpClient httpclient = TracingHttpClientBuilder
                    .create(tracing.getHttpTracing())
                    .build();
        
            final HttpGet request = new HttpGet("http://localhost:9000/catalog");
            request.setHeader("Accept", "application/json");
            
            final HttpResponse response = httpclient.execute(request);
            System.out.println(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
            
            httpclient.close();
        }
    }
}

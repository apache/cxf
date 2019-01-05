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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.util.EntityUtils;

import brave.httpasyncclient.TracingHttpAsyncClientBuilder;
import demo.jaxrs.tracing.server.CatalogTracing;

public final class ApacheHttpAsyncClient {
    private ApacheHttpAsyncClient() {
    }

    public static void main(final String[] args) throws Exception {
        try (final CatalogTracing tracing = new CatalogTracing("catalog-client")) {
            final CloseableHttpAsyncClient httpclient = TracingHttpAsyncClientBuilder
                .create(tracing.getHttpTracing())
                .build();
        
            final HttpGet request = new HttpGet("http://localhost:9000/catalog");
            request.setHeader("Accept", "application/json");
            
            httpclient.start();
            final Future<HttpResponse> response = httpclient.execute(request,
                new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(HttpResponse result) {
                        try {
                            System.out.println(EntityUtils.toString(result.getEntity(), StandardCharsets.UTF_8));
                        } catch (final IOException ex) {
                            System.out.println(ex);
                        }
                    }

                    @Override
                    public void failed(Exception ex) {
                        System.out.println(ex);
                    }

                    @Override
                    public void cancelled() {
                    }
                }
            );
            
            response.get(1000, TimeUnit.MILLISECONDS);
            httpclient.close();
        }
    }
}
